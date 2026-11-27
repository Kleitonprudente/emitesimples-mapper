package br.com.emitesimples.financeiroop;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.financeiro.entity.CobrancaDestinoEntity;
import br.com.emitesimples.financeiro.entity.CobrancaEntity;
import br.com.emitesimples.financeiro.entity.ConfiguracaoFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.CredencialCobrancaEntity;
import br.com.emitesimples.financeiro.entity.MovimentoEntity;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.financeiroop.GatewayDeCobranca.CobrancaParaCriar;
import br.com.emitesimples.financeiroop.GatewayDeCobranca.Credencial;
import br.com.emitesimples.financeiroop.GatewayDeCobranca.GatewayIndisponivel;
import br.com.emitesimples.financeiroop.GatewayDeCobranca.Pagador;
import br.com.emitesimples.financeiroop.GatewayDeCobranca.RespostaDaCobranca;
import br.com.emitesimples.plataforma.SegredoService;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Cobranca por gateway: boleto, Pix e link (docs/financeiro/README.md, 9.5, Fase 7; referencia §9.2).
 * <ul>
 * <li>Credencial por conta: provedor, identificacao no provedor (unica no sistema), token e segredo do webhook NO COFRE (SegredoService);
 * o segredo do webhook e gerado aqui e mostrado uma vez so (a URL do webhook); COBRANCADESTINO guarda so o hash dele.</li>
 * <li>Criar (critico, so ADMINISTRADOR, ou automatico ao lancar): grava CRIANDO, chama o provedor FORA da transacao, uma vez so.
 * Resposta: AGUARDANDO com os dados de pagamento; recusa: RECUSADA com a frase nossa pelo codigo; sem resposta: INCERTA.</li>
 * <li>Sincronizar (unico que muda o estado depois de criada; webhook, botao Atualizar e varredor): consulta o provedor fora da
 * transacao; PAGA da a baixa pelo livro (idempotente por idBaixaMovimento), o excedente vira juros, a tarifa vira saida avulsa;
 * pagamento fora do esperado vira PAGA_PARA_REVISAR e nao baixa.</li>
 * <li>Cancelar o titulo ou estornar a baixa cancela as cobrancas vivas (e a tela avisa para cancelar no portal do provedor).</li>
 * </ul>
 */
@ApplicationScoped
public class CobrancasService {

    private static final Logger LOG = Logger.getLogger(CobrancasService.class);
    static final Set<String> VIVAS = Set.of("CRIANDO", "AGUARDANDO", "INCERTA");
    static final Set<String> TIPOS = Set.of("BOLETO", "PIX", "LINK");
    static final Set<String> PROVEDORES = Set.of("FALSO");
    static final BigDecimal VALOR_MINIMO = new BigDecimal("30.00");
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Frase nossa por codigo de recusa do provedor (nunca o texto do provedor como mensagem principal). */
    static final Map<String, String> MOTIVOS = Map.of(
            "VALOR_MINIMO", "O provedor não aceita cobrança deste valor.",
            "DOCUMENTO_INVALIDO", "O CPF ou CNPJ do cliente foi recusado pelo provedor: confira o cadastro.",
            "VENCIMENTO_INVALIDO", "O vencimento foi recusado pelo provedor.",
            "CREDENCIAL_INVALIDA", "A integração foi recusada pelo provedor: confira o token na integração de cobrança.");

    public record ConfigurarCredencial(String provedor, String endpoint, String sellerId, String token, Boolean ativo) {
    }

    public record CredencialDto(String id, String idConta, String nomeConta, String provedor, String endpoint, String sellerId, boolean temToken,
            boolean ativo) {
    }

    /** Resposta da configuracao: {@code urlDoWebhook} so vem quando o segredo acabou de ser gerado (uma vez so). */
    public record CredencialConfigurada(CredencialDto credencial, String urlDoWebhook) {
    }

    public record CriarCobranca(String tipo, LocalDate vencimento) {
    }

    public record CobrancaDto(String id, String idTitulo, String idConta, String tipo, String estado, BigDecimal valor, LocalDate vencimento,
            String linhaDigitavel, String codigoDeBarras, String urlDoBoleto, String pixCopiaECola, String urlDoPix, String urlDoLink,
            LocalDateTime pagoEm, BigDecimal valorPago, BigDecimal tarifa, String motivo, String respostaDoProvedor, LocalDateTime ultimaConsultaEm,
            LocalDateTime dataCriacao, boolean viva) {
    }

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    @Inject
    JsonWebToken jwt;

    @Inject
    SegredoService segredos;

    @Inject
    ContextoTenantBanco contexto;

    @Inject
    LivroDeMovimentos livro;

    @Inject
    Instance<GatewayDeCobranca> gateways;

    // ------------------------------------------------------------------------------------------------------------ credencial

    public List<CredencialDto> credenciais() {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery("select c from CredencialCobrancaEntity c where c.empresa.id = :e "
                + "order by c.conta.nome", CredencialCobrancaEntity.class).setParameter("e", empresa).getResultList().stream()
                .map(CobrancasService::dto).toList());
    }

    public CredencialConfigurada configurar(String idConta, ConfigurarCredencial c) {
        String empresa = escopo.exigir("empresa_id");
        if (c == null) {
            throw invalido("corpo", "Informe os dados da integração.");
        }
        List<Map<String, String>> f = new ArrayList<>();
        if (c.provedor() == null || !PROVEDORES.contains(c.provedor())) {
            f.add(erro("provedor", "Escolha o provedor de cobrança."));
        }
        if (c.endpoint() != null && !c.endpoint().isBlank() && (!c.endpoint().startsWith("https://") || c.endpoint().length() > 255)) {
            f.add(erro("endpoint", "O endereço da API precisa começar com https:// (até 255 caracteres)."));
        }
        String seller = c.sellerId() == null ? "" : c.sellerId().strip();
        if (seller.isEmpty() || seller.length() > 100) {
            f.add(erro("sellerId", "Informe a identificação da conta no provedor (até 100 caracteres)."));
        }
        if (c.token() != null && c.token().length() > 2000) {
            f.add(erro("token", "Token longo demais."));
        }
        if (!f.isEmpty()) {
            throw invalido(f);
        }
        String segredoNovo;
        CredencialCobrancaEntity cred;
        try {
            Object[] r = QuarkusTransaction.requiringNew().call(() -> {
                ContaFinanceiraEntity conta = conta(empresa, idConta);
                CredencialCobrancaEntity x = em.createQuery("select c from CredencialCobrancaEntity c where c.conta.id = :c",
                        CredencialCobrancaEntity.class).setParameter("c", conta.getId()).getResultStream().findFirst().orElse(null);
                var agora = AuthService.agora();
                String gerado = null;
                if (x == null) {
                    x = new CredencialCobrancaEntity();
                    x.setEmpresa(em.getReference(EmpresaEntity.class, empresa));
                    x.setConta(conta);
                    x.setDataCriacao(agora);
                    gerado = aleatorio(32);
                    x.setReferenciaSegredoWebhook(segredos.guardar(gerado));
                }
                x.setProvedor(c.provedor());
                x.setEndpoint(c.endpoint() == null || c.endpoint().isBlank() ? null : c.endpoint().strip());
                x.setSellerId(seller);
                x.setAtivo(c.ativo() == null || c.ativo());
                if (c.token() != null && !c.token().isBlank()) {
                    if (x.getReferenciaToken() == null) {
                        x.setReferenciaToken(segredos.guardar(c.token()));
                    } else {
                        segredos.substituir(x.getReferenciaToken(), c.token());
                    }
                }
                x.setDataAtualizacao(agora);
                em.persist(x);
                em.flush();
                if (gerado != null) {
                    CobrancaDestinoEntity d = new CobrancaDestinoEntity();
                    d.setCredencial(x);
                    d.setIdEmpresa(empresa);
                    d.setSegredoHash(sha256(gerado));
                    d.setDataCriacao(agora);
                    d.setDataAtualizacao(agora);
                    em.persist(d);
                    em.flush();
                }
                return new Object[] {x, gerado};
            });
            cred = (CredencialCobrancaEntity) r[0];
            segredoNovo = (String) r[1];
        } catch (PersistenceException e) {
            throw new ApiException(409, "conflito", "Esta identificação no provedor já está ligada a outra conta.",
                    Map.of("campos", List.of(erro("sellerId", "Esta identificação no provedor já está ligada a outra conta."))));
        }
        return new CredencialConfigurada(dto(cred), segredoNovo == null ? null : "/api/webhooks/cobranca/" + segredoNovo);
    }

    // ------------------------------------------------------------------------------------------------------------ criar

    public CobrancaDto criar(String idTitulo, CriarCobranca pedido) {
        String empresa = escopo.exigir("empresa_id");
        if (pedido == null || pedido.tipo() == null || !TIPOS.contains(pedido.tipo())) {
            throw invalido("tipo", "Escolha boleto, Pix ou link de pagamento.");
        }
        return criar(empresa, idTitulo, pedido.tipo(), pedido.vencimento(), jwt.getSubject());
    }

    CobrancaDto criar(String empresa, String idTitulo, String tipo, LocalDate vencimentoPedido, String usuario) {
        Object[] r = QuarkusTransaction.requiringNew().call(() -> reservar(empresa, idTitulo, tipo, vencimentoPedido, usuario));
        CobrancaEntity c = (CobrancaEntity) r[0];
        Credencial cred = (Credencial) r[1];
        CobrancaParaCriar pedido = (CobrancaParaCriar) r[2];
        RespostaDaCobranca resp;
        try {
            resp = gateway(cred.provedor()).criar(cred, pedido); // FORA da transacao, uma vez so
        } catch (GatewayIndisponivel e) {
            LOG.warnf("Cobranca %s sem resposta do provedor: INCERTA.", c.getId());
            return QuarkusTransaction.requiringNew().call(() -> mudar(c.getId(), x -> {
                x.setEstado("INCERTA");
                x.setMotivo("O provedor não respondeu. Confira no portal antes de gerar outra.");
            }));
        }
        return QuarkusTransaction.requiringNew().call(() -> mudar(c.getId(), x -> aplicarCriacao(x, resp)));
    }

    private Object[] reservar(String empresa, String idTitulo, String tipo, LocalDate vencimentoPedido, String usuario) {
        TituloEntity t = titulo(empresa, idTitulo, true);
        if (!"RECEBER".equals(t.getTipo())) {
            throw conflito("cobranca_so_a_receber", "Só se gera cobrança de título a receber.");
        }
        if (t.getAprovadoEm() == null) {
            throw conflito("titulo_sem_aprovacao", "Este título está aguardando aprovação: aprove-o antes de gerar a cobrança.");
        }
        if (!Set.of("ABERTO", "PARCIAL").contains(t.getSituacao()) || Boolean.TRUE.equals(t.getParaRevisao())) {
            throw conflito("titulo_sem_saldo", "Este título não aceita cobrança (pago, cancelado ou para revisão).");
        }
        BigDecimal saldo = Dinheiro.normalizar(t.getValor()).subtract(Dinheiro.normalizar(t.getQuitado()));
        if (!"LINK".equals(tipo) && saldo.compareTo(VALOR_MINIMO) < 0) {
            throw invalido("tipo", "Boleto e Pix só a partir de " + Dinheiro.formatar(VALOR_MINIMO) + ".");
        }
        LocalDate hoje = TitulosService.hoje();
        LocalDate venc = switch (tipo) {
            case "PIX" -> hoje;
            case "LINK" -> vencimentoPedido == null ? hoje.plusDays(7) : vencimentoPedido;
            default -> vencimentoPedido == null ? (t.getVencimento().isBefore(hoje) ? hoje : t.getVencimento()) : vencimentoPedido;
        };
        if ("BOLETO".equals(tipo) && venc.isBefore(hoje)) {
            throw invalido("vencimento", "O vencimento do boleto precisa ser de hoje em diante.");
        }
        if ("LINK".equals(tipo) && (venc.isBefore(hoje.plusDays(1)) || venc.isAfter(hoje.plusDays(30)))) {
            throw invalido("vencimento", "O link de pagamento vale de 1 a 30 dias.");
        }
        CredencialCobrancaEntity cred = credencialPara(empresa, t);
        PessoaEntity p = t.getIdPessoa() == null ? null : em.find(PessoaEntity.class, t.getIdPessoa());
        if (p == null) {
            throw conflito("titulo_sem_cliente", "Este título não tem cliente: informe o cliente antes de gerar a cobrança.");
        }
        ConfiguracaoFinanceiraEntity cfg = em.createQuery("select c from ConfiguracaoFinanceiraEntity c where c.empresa.id = :e",
                ConfiguracaoFinanceiraEntity.class).setParameter("e", empresa).getResultStream().findFirst().orElse(null);
        var agora = AuthService.agora();
        CobrancaEntity c = new CobrancaEntity();
        c.setEmpresa(t.getEmpresa());
        c.setTitulo(t);
        c.setConta(cred.getConta());
        c.setProvedor(cred.getProvedor());
        c.setTipo(tipo);
        c.setEstado("CRIANDO");
        c.setValor(saldo);
        c.setVencimento(venc);
        c.setMultaPercentual(t.getMultaPercentual() != null ? t.getMultaPercentual() : cfg == null ? null : cfg.getMultaPercentual());
        c.setJurosMensalPercentual(t.getJurosMensalPercentual() != null ? t.getJurosMensalPercentual()
                : cfg == null ? null : cfg.getJurosMensalPercentual());
        c.setIdCriadoPor(usuario);
        c.setDataCriacao(agora);
        c.setDataAtualizacao(agora);
        try {
            em.persist(c);
            em.flush();
        } catch (PersistenceException e) {
            throw conflito("cobranca_viva", "Este título já tem uma cobrança em andamento.");
        }
        Credencial credencial = new Credencial(cred.getProvedor(), cred.getEndpoint(), cred.getSellerId(),
                cred.getReferenciaToken() == null ? null : segredos.ler(cred.getReferenciaToken()));
        CobrancaParaCriar pedido = new CobrancaParaCriar(c.getId(), tipo, saldo, venc, c.getMultaPercentual(), c.getJurosMensalPercentual(),
                t.getDescricao(), new Pagador(p.getNome(), p.getDocumento(), p.getEmail()));
        return new Object[] {c, credencial, pedido};
    }

    /** A conta padrao da forma prevista com integracao ativa; senao a unica conta com integracao ativa; com duas ou mais, recusa. */
    private CredencialCobrancaEntity credencialPara(String empresa, TituloEntity t) {
        List<CredencialCobrancaEntity> ativas = em.createQuery("select c from CredencialCobrancaEntity c where c.empresa.id = :e "
                + "and c.ativo = true and c.conta.ativo = true", CredencialCobrancaEntity.class).setParameter("e", empresa).getResultList();
        if (ativas.isEmpty()) {
            throw conflito("sem_integracao", "Nenhuma conta tem integração de cobrança ativa: configure em Financeiro > Integração de cobrança.");
        }
        String forma = t.getFormaPrevista();
        for (CredencialCobrancaEntity c : ativas) {
            ContaFinanceiraEntity k = c.getConta();
            if ("BOLETO".equals(forma) && Boolean.TRUE.equals(k.getPadraoBoleto()) || "PIX".equals(forma) && Boolean.TRUE.equals(k.getPadraoPix())) {
                return c;
            }
        }
        if (ativas.size() == 1) {
            return ativas.get(0);
        }
        throw conflito("integracao_ambigua", "Há mais de uma conta com cobrança e nenhuma é a padrão desta forma: marque a conta padrão.");
    }

    private void aplicarCriacao(CobrancaEntity c, RespostaDaCobranca r) {
        if ("RECUSADA".equals(r.situacao())) {
            c.setEstado("RECUSADA");
            c.setMotivo(MOTIVOS.getOrDefault(r.codigoRecusa(), "O provedor recusou a cobrança. Confira os dados e tente de novo."));
            c.setRespostaDoProvedor(filtrar(r.textoDoProvedor()));
            return;
        }
        c.setEstado("AGUARDANDO");
        dadosDePagamento(c, r);
    }

    private static void dadosDePagamento(CobrancaEntity c, RespostaDaCobranca r) {
        c.setIdExterno(r.idExterno());
        c.setLinhaDigitavel(cortar(r.linhaDigitavel(), 60));
        c.setCodigoDeBarras(cortar(r.codigoDeBarras(), 60));
        c.setUrlDoBoleto(soHttps(r.urlDoBoleto()));
        c.setPixCopiaECola(cortar(r.pixCopiaECola(), 500));
        c.setUrlDoPix(soHttps(r.urlDoPix()));
        c.setUrlDoLink(soHttps(r.urlDoLink()));
    }

    // ------------------------------------------------------------------------------------------------------------ sincronizar

    /** Botao "Atualizar situacao" (qualquer papel que escreve). */
    public CobrancaDto atualizar(String idCobranca) {
        String empresa = escopo.exigir("empresa_id");
        QuarkusTransaction.requiringNew().run(() -> cobranca(empresa, idCobranca, false));
        return sincronizar(empresa, idCobranca);
    }

    /** Consulta o provedor (fora da transacao) e aplica o resultado (dentro, com cobranca e titulo travados). */
    CobrancaDto sincronizar(String empresa, String idCobranca) {
        Object[] r = QuarkusTransaction.requiringNew().call(() -> {
            CobrancaEntity c = cobranca(empresa, idCobranca, false);
            CredencialCobrancaEntity cred = em.createQuery("select c from CredencialCobrancaEntity c where c.conta.id = :k",
                    CredencialCobrancaEntity.class).setParameter("k", c.getConta().getId()).getResultStream().findFirst().orElse(null);
            Credencial credencial = cred == null ? null : new Credencial(cred.getProvedor(), cred.getEndpoint(), cred.getSellerId(),
                    cred.getReferenciaToken() == null ? null : segredos.ler(cred.getReferenciaToken()));
            return new Object[] {c.getEstado(), c.getIdExterno(), c.getTipo(), c.getProvedor(), credencial};
        });
        String estado = (String) r[0];
        if (!VIVAS.contains(estado) || r[1] == null && !"INCERTA".equals(estado)) {
            return QuarkusTransaction.requiringNew().call(() -> dto(cobranca(empresa, idCobranca, false)));
        }
        if (r[1] == null) {
            return QuarkusTransaction.requiringNew().call(() -> dto(cobranca(empresa, idCobranca, false))); // incerta sem id: so o portal resolve
        }
        RespostaDaCobranca resp;
        try {
            resp = gateway((String) r[3]).consultar((Credencial) r[4], (String) r[1], (String) r[2]);
        } catch (GatewayIndisponivel e) {
            return QuarkusTransaction.requiringNew().call(() -> mudar(idCobranca, x -> x.setUltimaConsultaEm(AuthService.agora())));
        }
        return QuarkusTransaction.requiringNew().call(() -> aplicarConsulta(empresa, idCobranca, resp));
    }

    private CobrancaDto aplicarConsulta(String empresa, String idCobranca, RespostaDaCobranca r) {
        CobrancaEntity c = cobranca(empresa, idCobranca, true);
        c.setUltimaConsultaEm(AuthService.agora());
        c.setDataAtualizacao(AuthService.agora());
        if (!VIVAS.contains(c.getEstado())) {
            return dto(c); // outro caminho ja resolveu
        }
        switch (r.situacao()) {
            case "PAGA" -> pagar(c, r);
            case "CANCELADA" -> c.setEstado("CANCELADA");
            case "EXPIRADA" -> c.setEstado("EXPIRADA");
            case "AGUARDANDO" -> {
                c.setEstado("AGUARDANDO");
                if (c.getIdExterno() == null) {
                    dadosDePagamento(c, r);
                }
            }
            default -> LOG.debugf("Cobranca %s: situacao %s do provedor ignorada.", c.getId(), r.situacao());
        }
        em.flush();
        return dto(c);
    }

    /** Pagamento confirmado: baixa pelo livro (uma vez so), excedente como juros, tarifa como saida; fora do esperado, revisar. */
    private void pagar(CobrancaEntity c, RespostaDaCobranca r) {
        c.setPagoEm(r.pagoEm() == null ? AuthService.agora() : r.pagoEm());
        c.setValorPago(r.valorPago());
        c.setMeioPago(cortar(r.meioPago(), 10));
        c.setTarifa(r.tarifa());
        if (c.getIdBaixaMovimento() != null) {
            c.setEstado("PAGA");
            return;
        }
        TituloEntity t = em.find(TituloEntity.class, c.getTitulo().getId(), LockModeType.PESSIMISTIC_WRITE);
        BigDecimal saldo = Dinheiro.normalizar(t.getValor()).subtract(Dinheiro.normalizar(t.getQuitado()));
        BigDecimal pago = r.valorPago() == null ? null : Dinheiro.normalizar(r.valorPago());
        if (pago == null || pago.compareTo(saldo) < 0 || saldo.signum() <= 0 || !Set.of("ABERTO", "PARCIAL").contains(t.getSituacao())) {
            c.setEstado("PAGA_PARA_REVISAR"); // nao baixa sozinho
            c.setMotivo("Pagamento fora do esperado (valor ou situação do título): confira e dê a baixa à mão.");
            return;
        }
        String forma = "BOLETO".equals(c.getTipo()) ? "BOLETO" : "PIX".equals(c.getTipo()) ? "PIX" : "CARTAO".equals(r.meioPago()) ? "CARTAO" : "PIX";
        MovimentoEntity baixa = livro.baixarPeloGateway(t, c.getConta(), c.getPagoEm().toLocalDate(), forma, saldo, pago.subtract(saldo));
        em.flush();
        c.setIdBaixaMovimento(baixa.getId());
        if (r.tarifa() != null && r.tarifa().signum() > 0) {
            MovimentoEntity tarifa = livro.tarifaDoGateway(c.getConta(), c.getPagoEm().toLocalDate(), r.tarifa(),
                    "Tarifa da cobrança: " + t.getDescricao());
            em.flush();
            c.setIdTarifaMovimento(tarifa.getId());
        }
        c.setEstado("PAGA");
    }

    // ------------------------------------------------------------------------------------------------------------ descartar, listar

    /** Descartar: so incerta (conferida no portal) ou recusada. */
    public CobrancaDto descartar(String idCobranca) {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> {
            CobrancaEntity c = cobranca(empresa, idCobranca, true);
            if (!Set.of("INCERTA", "RECUSADA").contains(c.getEstado())) {
                throw conflito("cobranca_nao_descartavel", "Só se descarta cobrança incerta ou recusada.");
            }
            c.setEstado("DESCARTADA");
            c.setDataAtualizacao(AuthService.agora());
            em.flush();
            return dto(c);
        });
    }

    public List<CobrancaDto> doTitulo(String idTitulo) {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> {
            titulo(empresa, idTitulo, false);
            return em.createQuery("select c from CobrancaEntity c where c.titulo.id = :t order by c.dataCriacao desc", CobrancaEntity.class)
                    .setParameter("t", idTitulo).getResultList().stream().map(CobrancasService::dto).toList();
        });
    }

    /** Dentro da transacao de quem cancela o titulo ou estorna a baixa: as cobrancas vivas viram CANCELADA. Devolve quantas. */
    int cancelarVivas(String idTitulo) {
        return em.createQuery("update CobrancaEntity c set c.estado = 'CANCELADA', c.dataAtualizacao = :a where c.titulo.id = :t "
                + "and c.estado in ('CRIANDO', 'AGUARDANDO', 'INCERTA')").setParameter("a", AuthService.agora()).setParameter("t", idTitulo)
                .executeUpdate();
    }

    // ------------------------------------------------------------------------------------------------------------ automatica, webhook, varredor

    /**
     * Cobranca automatica ao lancar (regra do sistema): titulo a receber, aprovado, forma prevista boleto ou Pix, empresa com integracao.
     * Roda depois do commit do lancamento, uma tentativa so; falha nunca derruba o lancamento (fica registrada na cobranca ou no log).
     */
    void automatica(String empresa, List<TitulosService.TituloDto> titulos, String usuario) {
        for (TitulosService.TituloDto t : titulos) {
            if (!"RECEBER".equals(t.tipo()) || !t.aprovado() || !Set.of("BOLETO", "PIX").contains(t.formaPrevista())) {
                continue;
            }
            try {
                criar(empresa, t.id(), t.formaPrevista(), null, usuario);
            } catch (ApiException e) {
                LOG.infof("Cobranca automatica do titulo %s nao gerada: %s", t.id(), e.getCodigo());
            } catch (RuntimeException e) {
                LOG.warnf("Cobranca automatica do titulo %s falhou (%s).", t.id(), e.getClass().getSimpleName());
            }
        }
    }

    /** Webhook do provedor: so IDENTIFICA a cobranca e manda consultar (nunca e fonte de verdade). Segredo desconhecido: false. */
    public boolean webhook(String segredo, String idExterno) {
        if (segredo == null || segredo.length() > 100) {
            return false;
        }
        String hash = sha256(segredo);
        String empresa = contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select d.idEmpresa from CobrancaDestinoEntity d where d.segredoHash = :h", String.class).setParameter("h", hash)
                .getResultStream().findFirst().orElse(null)));
        if (empresa == null) {
            return false;
        }
        if (idExterno == null || idExterno.length() > 100) {
            return true;
        }
        contexto.rodarComoEmpresa(empresa, () -> {
            String id = QuarkusTransaction.requiringNew().call(() -> em.createQuery("select c.id from CobrancaEntity c where c.empresa.id = :e "
                    + "and c.idExterno = :x", String.class).setParameter("e", empresa).setParameter("x", idExterno).getResultStream()
                    .findFirst().orElse(null));
            if (id != null) {
                sincronizar(empresa, id);
            }
        });
        return true;
    }

    /** Varredor: Pix e link vivos a cada passada; boleto so se a ultima consulta tem mais de 12 horas. Devolve quantas consultou. */
    public int varrer() {
        LocalDateTime boleto = AuthService.agora().minusHours(12);
        List<Object[]> vivas = contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select c.empresa.id, c.id from CobrancaEntity c where c.estado in ('AGUARDANDO', 'INCERTA') and c.idExterno is not null and "
                        + "(c.tipo <> 'BOLETO' or c.ultimaConsultaEm is null or c.ultimaConsultaEm < :b)", Object[].class)
                .setParameter("b", boleto).setMaxResults(500).getResultList()));
        int n = 0;
        for (Object[] v : vivas) {
            try {
                contexto.rodarComoEmpresa((String) v[0], () -> sincronizar((String) v[0], (String) v[1]));
                n++;
            } catch (RuntimeException e) {
                LOG.warnf("Varredor: cobranca %s nao sincronizada (%s).", v[1], e.getClass().getSimpleName());
            }
        }
        return n;
    }

    // ------------------------------------------------------------------------------------------------------------ apoio

    private GatewayDeCobranca gateway(String provedor) {
        for (GatewayDeCobranca g : gateways) {
            if (g.provedor().equals(provedor)) {
                return g;
            }
        }
        throw new GatewayIndisponivel("provedor sem adaptador: " + provedor);
    }

    private CobrancaDto mudar(String id, java.util.function.Consumer<CobrancaEntity> f) {
        CobrancaEntity c = em.find(CobrancaEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        f.accept(c);
        c.setDataAtualizacao(AuthService.agora());
        em.flush();
        return dto(c);
    }

    private CobrancaEntity cobranca(String empresa, String id, boolean travar) {
        CobrancaEntity c = id == null || id.length() > 36 ? null
                : travar ? em.find(CobrancaEntity.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(CobrancaEntity.class, id);
        if (c == null || !empresa.equals(c.getEmpresa().getId())) {
            throw new ApiException(404, "nao_encontrado", "Cobrança não encontrada.");
        }
        return c;
    }

    private TituloEntity titulo(String empresa, String id, boolean travar) {
        TituloEntity t = id == null || id.length() > 36 ? null
                : travar ? em.find(TituloEntity.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(TituloEntity.class, id);
        if (t == null || t.getEmpresa() == null || !empresa.equals(t.getEmpresa().getId())) {
            throw new ApiException(404, "nao_encontrado", "Título não encontrado.");
        }
        return t;
    }

    private ContaFinanceiraEntity conta(String empresa, String id) {
        ContaFinanceiraEntity c = id == null || id.length() > 36 ? null : em.find(ContaFinanceiraEntity.class, id);
        if (c == null || !empresa.equals(c.getEmpresa().getId())) {
            throw new ApiException(404, "nao_encontrado", "Conta não encontrada.");
        }
        return c;
    }

    /** Texto do provedor sem CPF/CNPJ, e-mail, telefone e links, ate 200 caracteres (referencia §9.2). */
    static String filtrar(String texto) {
        if (texto == null) {
            return null;
        }
        String s = texto.replaceAll("https?://\\S+", "[link]")
                .replaceAll("[\\w.+-]+@[\\w-]+\\.[\\w.]+", "[e-mail]")
                .replaceAll("\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}", "[documento]")
                .replaceAll("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}", "[documento]")
                .replaceAll("\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}", "[telefone]");
        return cortar(s, 200);
    }

    private static String soHttps(String url) {
        return url != null && url.startsWith("https://") && url.length() <= 500 ? url : null;
    }

    private static String cortar(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }

    static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String aleatorio(int bytes) {
        byte[] b = new byte[bytes];
        ALEATORIO.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }

    private static ApiException invalido(String campo, String mensagem) {
        return invalido(List.of(erro(campo, mensagem)));
    }

    private static ApiException invalido(List<Map<String, String>> campos) {
        return new ApiException(400, "requisicao_invalida", campos.get(0).get("mensagem"), Map.of("campos", campos));
    }

    private static ApiException conflito(String codigo, String mensagem) {
        return new ApiException(409, codigo, mensagem);
    }

    private static CredencialDto dto(CredencialCobrancaEntity c) {
        return new CredencialDto(c.getId(), c.getConta().getId(), c.getConta().getNome(), c.getProvedor(), c.getEndpoint(), c.getSellerId(),
                c.getReferenciaToken() != null, Boolean.TRUE.equals(c.getAtivo()));
    }

    static CobrancaDto dto(CobrancaEntity c) {
        return new CobrancaDto(c.getId(), c.getTitulo().getId(), c.getConta().getId(), c.getTipo(), c.getEstado(), c.getValor(), c.getVencimento(),
                c.getLinhaDigitavel(), c.getCodigoDeBarras(), c.getUrlDoBoleto(), c.getPixCopiaECola(), c.getUrlDoPix(), c.getUrlDoLink(),
                c.getPagoEm(), c.getValorPago(), c.getTarifa(), c.getMotivo(), c.getRespostaDoProvedor(), c.getUltimaConsultaEm(),
                c.getDataCriacao(), VIVAS.contains(c.getEstado()));
    }
}
