package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.gateway.IntegraNotasCliente;
import br.com.emitesimples.gateway.IntegraNotasCliente.RetornoNfse;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.nfseconfig.CertificadoSituacao;
import br.com.emitesimples.nfseemissao.NfseEmissaoDtos.*;
import br.com.emitesimples.plataforma.IntegraNotasConfigService;
import br.com.emitesimples.plataforma.SegredoService;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Emissao de NFS-e da empresa da sessao: cria (numeracao atomica + snapshot do servico), envia ao gateway FORA de transacao,
 * reenvia (mesmo numero), consulta e lista. Ver README, secao "Emissao de NFS-e". Escopo: toda consulta filtra por
 * {@code empresa.id} explicitamente (o administrador da plataforma com empresa na sessao e irrestrito na RLS).
 */
@ApplicationScoped
public class NfseEmissaoService {

    private static final Logger LOG = Logger.getLogger(NfseEmissaoService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    static final BigDecimal VALOR_MIN = new BigDecimal("0.01");
    static final BigDecimal VALOR_MAX = new BigDecimal("9999999999999.99");
    static final long MAX_NUMERO = 999_999_999L;

    @ConfigProperty(name = "emitesimples.integranotas.enviar-tributos-totais", defaultValue = "false")
    boolean enviarTributosTotais;

    @ConfigProperty(name = "emitesimples.nfse.permitir-emissao-producao", defaultValue = "false")
    boolean permitirProducao;

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    @Inject
    IntegraNotasConfigService config;

    @Inject
    SegredoService segredos;

    @Inject
    IntegraNotasCliente cliente;

    @Inject
    JsonWebToken jwt;

    @Inject
    LimiteEnvioNfse limite;

    @Inject
    NfseAcompanhamentoService acompanhamento;

    @Inject
    NfseChamadaLog chamadas;

    /** Campos ja validados; {@code null} = nao informado (na criacao, so info/discriminacao podem faltar). */
    private record Campos(String pessoaId, String servicoId, BigDecimal valor, String info, String discriminacao) {
    }

    private record Pre(NfsePayloadMapper.Montado montado, String tokenRef, String ambiente) {
    }

    // ------------------------------------------------------------------------------------------- criar / enviar

    public NotaDetalhe criar(CriarEntrada in) {
        limite.consumir(jwt.getSubject());
        String emp = empresaDaSessao();
        if (in == null) {
            throw invalido(List.of(erro("corpo", "Informe os dados da nota.")));
        }
        Campos c = validar(in.pessoaId(), in.servicoId(), in.valorServicos(), in.informacoesComplementares(),
                in.discriminacao(), true);
        String ativo = config.ambienteAtivo();
        String usuarioId = jwt.getSubject();
        String notaId = QuarkusTransaction.requiringNew().call(() -> criarNota(emp, c, ativo, usuarioId));
        return enviarAoGateway(emp, notaId);
    }

    public NotaDetalhe enviar(String id, EnviarEntrada in) {
        limite.consumir(jwt.getSubject());
        String emp = empresaDaSessao();
        Campos c = in == null ? new Campos(null, null, null, null, null)
                : validar(in.pessoaId(), in.servicoId(), in.valorServicos(), in.informacoesComplementares(),
                        in.discriminacao(), false);
        String ativo = config.ambienteAtivo();
        String usuarioId = jwt.getSubject();
        QuarkusTransaction.requiringNew().run(() -> prepararReenvio(emp, id, c, ativo, usuarioId));
        return enviarAoGateway(emp, id);
    }

    private String criarNota(String emp, Campos c, String ativo, String usuarioId) {
        EmpresaEntity e = em.createQuery("select e from EmpresaEntity e where e.id = :id", EmpresaEntity.class)
                .setParameter("id", emp).setLockMode(LockModeType.PESSIMISTIC_WRITE) // numeracao atomica: FOR UPDATE
                .getResultStream().findFirst().orElseThrow(() -> naoEncontrado("Empresa não encontrada."));
        preCondicoes(e, ativo);
        PessoaEntity p = pessoa(emp, c.pessoaId());
        ServicoEntity s = servico(emp, c.servicoId());
        tomadorCompleto(p);
        String discriminacao = discriminacaoFinal(c.discriminacao(), s);
        long proximo = (e.getUltimoNumeroNfse() == null ? 0 : e.getUltimoNumeroNfse()) + 1L;
        if (proximo > MAX_NUMERO) {
            throw new ApiException(409, "numeracao_esgotada", "A numeração da série chegou ao limite (999999999).");
        }
        var agora = AuthService.agora();
        e.setUltimoNumeroNfse((int) proximo);
        e.setDataAtualizacao(agora);
        NotaEntity n = NotaEntity.builderOf()
                .withNumero(String.valueOf(proximo)).withSerie(e.getSerieNfse()).withAmbiente(e.getAmbiente())
                .withDataEmissao(LocalDateTime.now(NfsePayloadMapper.SAO_PAULO))
                .withDataCompetencia(LocalDate.now(NfsePayloadMapper.SAO_PAULO))
                .withInformacoesComplementares(c.info() == null || c.info().isBlank() ? null : c.info().strip())
                .withEstado("processando").withDataHoraAprovacao(agora)
                .withEmpresa(e).withPessoa(p).withServico(s)
                .withPreparadoPorUsuario(usuario(usuarioId)).withAprovadoPor(usuario(usuarioId))
                .withDataCriacao(agora).withDataAtualizacao(agora)
                .build();
        snapshot(n, s, discriminacao, c.valor());
        em.persist(n);
        em.flush();
        return n.getId();
    }

    private void prepararReenvio(String emp, String id, Campos c, String ativo, String usuarioId) {
        NotaEntity n = acompanhamento.carregar(emp, id, true);
        if (n == null) {
            throw naoEncontrado("Nota não encontrada.");
        }
        switch (n.getEstado()) {
            case "rascunho", "rejeitada" -> {
            }
            case "processando", "emitida_offline" -> throw new ApiException(409, "nota_em_processamento",
                    "A nota já está em processamento. Use Atualizar para ver o resultado.");
            case "em_conflito" -> throw new ApiException(409, "nota_em_conflito",
                    "A nota está em conflito no gateway. Não reenvie; use Atualizar.");
            default -> throw new ApiException(409, "nota_nao_editavel", "Esta nota não pode mais ser alterada nem reenviada.");
        }
        EmpresaEntity e = em.find(EmpresaEntity.class, emp);
        preCondicoes(e, ativo);
        PessoaEntity p = c.pessoaId() != null ? pessoa(emp, c.pessoaId()) : n.getPessoa();
        tomadorCompleto(p);
        n.setPessoa(p);
        if (c.servicoId() != null) {
            ServicoEntity s = servico(emp, c.servicoId());
            n.setServico(s);
            snapshot(n, s, discriminacaoFinal(c.discriminacao(), s), c.valor() != null ? c.valor() : n.getValorServicos());
        } else {
            if (c.valor() != null) {
                n.setValorServicos(c.valor());
            }
            if (c.discriminacao() != null) {
                n.setDiscriminacao(discriminacaoFinal(c.discriminacao(), n.getServico()));
            }
        }
        if (c.info() != null) {
            n.setInformacoesComplementares(c.info().isBlank() ? null : c.info().strip());
        }
        var agora = AuthService.agora();
        n.setEstado("processando");
        n.setCodigoRetorno(null);
        n.setMensagemRetorno(null);
        n.setErrosRetorno(null);
        n.setDataEmissao(LocalDateTime.now(NfsePayloadMapper.SAO_PAULO));
        n.setDataHoraAprovacao(agora);
        n.setAprovadoPor(usuario(usuarioId));
        n.setDataAtualizacao(agora);
        em.flush();
    }

    /** Copia do servico para a nota (alterar o cadastro depois nao muda a nota). */
    private static void snapshot(NotaEntity n, ServicoEntity s, String discriminacao, BigDecimal valor) {
        n.setCodigoServico(s.getCodigoServico());
        n.setCodigoTributacaoMunicipio(s.getCodigoTributacaoMunicipio());
        n.setCodigoNbs(s.getCodigoNbs());
        n.setDescricao(s.getDescricaoCurta());
        n.setDiscriminacao(discriminacao);
        n.setAliquotaIss(s.getAliquotaIss());
        n.setIssRetido(s.getIssRetido() == null ? Boolean.FALSE : s.getIssRetido());
        n.setTipoOperacaoIss(s.getTipoOperacaoIss() == null ? 1 : s.getTipoOperacaoIss());
        n.setCstPisCofins(s.getCstPisCofins() == null || s.getCstPisCofins().isBlank() ? "00" : s.getCstPisCofins());
        n.setValorServicos(valor);
    }

    private void preCondicoes(EmpresaEntity e, String ativo) {
        if (!Boolean.TRUE.equals(e.getNfseHabilitada()) || e.getSerieNfse() == null) {
            throw new ApiException(409, "nfse_nao_habilitada", "A NFS-e não está habilitada para esta empresa.");
        }
        if (IntegraNotasConfigService.PRODUCAO.equals(ativo) && !permitirProducao) {
            throw new ApiException(409, "emissao_producao_bloqueada",
                    "A emissão em produção está bloqueada nesta fase. Use o ambiente de homologação.");
        }
        if (e.getReferenciaTokenEmpresa() == null || !"cadastrado".equals(e.getStatusCadastro())) {
            throw new ApiException(409, "emitente_nao_cadastrado",
                    "O emitente ainda não foi cadastrado no IntegraNotas. Habilite a NFS-e da empresa.");
        }
        if (!ativo.equals(e.getAmbiente())) {
            throw new ApiException(409, "emitente_de_outro_ambiente",
                    "O emitente foi cadastrado em outro ambiente do IntegraNotas. Habilite a NFS-e novamente.");
        }
        if (!CertificadoSituacao.VALIDO.equals(CertificadoSituacao.situacao(e))) {
            throw new ApiException(409, "certificado_nao_valido",
                    "O certificado digital da empresa não está válido. Envie o certificado em Configuração.");
        }
    }

    // ------------------------------------------------------------------------------------------- gateway

    private NotaDetalhe enviarAoGateway(String emp, String notaId) {
        Pre pre = QuarkusTransaction.requiringNew().call(() -> {
            NotaEntity n = acompanhamento.carregar(emp, notaId, false);
            EmpresaEntity e = em.find(EmpresaEntity.class, emp);
            return new Pre(NfsePayloadMapper.montar(n, n.getPessoa(), enviarTributosTotais), e.getReferenciaTokenEmpresa(),
                    n.getAmbiente());
        });
        RetornoNfse r = null;
        String falha = null;
        long ini = System.nanoTime();
        try {
            String token = segredos.ler(pre.tokenRef());
            String urlBase = config.urlBase(pre.ambiente());
            ini = System.nanoTime();
            r = cliente.enviarNfse(urlBase, token, pre.montado().corpo());
        } catch (ApiException ex) {
            falha = ex.getCodigo();
        } catch (RuntimeException ex) {
            LOG.warnf("NFS-e: cofre/config indisponivel no envio (empresa=%s, %s)", emp, ex.getClass().getSimpleName());
            falha = "integranotas_indisponivel";
        }
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - ini);
        var m = pre.montado();
        chamadas.registrar(emp, notaId, "enviar_nfse", "POST", "/v2/nfse", "v2", r == null ? null : r.httpStatus(),
                r == null ? null : r.codigo(), r == null ? falha : r.mensagem(), ms, r == null ? null : r.chave(),
                m.identificacao(), m.resumo(), pre.ambiente());
        RetornoNfse retorno = r;
        String falhaFinal = falha;
        NfseRetorno.Efeito efeito = QuarkusTransaction.requiringNew().call(() -> {
            NotaEntity n = acompanhamento.carregar(emp, notaId, true);
            if (n == null || !"processando".equals(n.getEstado())) {
                return NfseRetorno.Efeito.NENHUM;
            }
            if (retorno == null) {
                NfseRetorno.aplicarFalhaDeEnvio(n, falhaFinal);
                return NfseRetorno.Efeito.VOLTOU_RASCUNHO;
            }
            return NfseRetorno.aplicarEnvio(n, retorno);
        });
        if (efeito == NfseRetorno.Efeito.CONSULTAR_AGORA) {
            try {
                acompanhamento.consultar(emp, notaId);
            } catch (RuntimeException ex) {
                LOG.infof("NFS-e: consulta imediata nao concluida (empresa=%s)", emp);
            }
        }
        return detalhe(emp, notaId);
    }

    public NotaDetalhe atualizar(String id) {
        String emp = empresaDaSessao();
        NotaDetalhe d = detalhe(emp, id);
        if (!d.podeAtualizar()) {
            return d;
        }
        acompanhamento.consultar(emp, id);
        return detalhe(emp, id);
    }

    // ------------------------------------------------------------------------------------------- leitura

    private static final String SELECT = "select n.id, n.numero, n.serie, n.estado, n.dataEmissao, n.valorServicos, p.id, p.nome, "
            + "p.documento, s.id, s.nome, n.chave, n.numeroNfse, n.codigoRetorno, n.mensagemRetorno, "
            + "case when n.pdf is null then false else true end, n.dataCriacao, n.informacoesComplementares, n.discriminacao, "
            + "n.descricao, n.codigoServico, n.errosRetorno, n.linkPdf, n.codigoVerificacao, n.dataHoraEvento "
            + "from NotaEntity n join n.pessoa p join n.servico s where n.empresa.id = :emp";

    public List<NotaItem> listar(int from, int to, String estado, String busca) {
        String emp = empresaDaSessao();
        if (from < 0 || to < from) {
            throw invalido(List.of(erro("to", "Informe um intervalo válido (from <= to).")));
        }
        int limiteItens = (int) Math.min(100L, (long) to - from + 1);
        return QuarkusTransaction.requiringNew().call(() -> {
            TypedQuery<Object[]> q = em.createQuery(SELECT + filtros(estado, busca)
                    + " order by n.dataCriacao desc, n.id desc", Object[].class);
            parametros(q, emp, estado, busca);
            q.setFirstResult(from).setMaxResults(limiteItens);
            List<NotaItem> r = new ArrayList<>();
            for (Object[] l : q.getResultList()) {
                r.add(item(l));
            }
            return r;
        });
    }

    public Contagem contar(String estado, String busca) {
        String emp = empresaDaSessao();
        return QuarkusTransaction.requiringNew().call(() -> {
            TypedQuery<Long> q = em.createQuery("select count(n) from NotaEntity n join n.pessoa p where n.empresa.id = :emp"
                    + filtros(estado, busca), Long.class);
            parametros(q, emp, estado, busca);
            return new Contagem(q.getSingleResult());
        });
    }

    private static String filtros(String estado, String busca) {
        StringBuilder sb = new StringBuilder();
        if (estado != null && !estado.isBlank()) {
            sb.append(" and n.estado = :estado");
        }
        if (busca != null && !busca.isBlank()) {
            sb.append(" and (n.numero like :b escape '!' or lower(p.nome) like :bn escape '!' or n.chave like :b escape '!')");
        }
        return sb.toString();
    }

    private static void parametros(TypedQuery<?> q, String emp, String estado, String busca) {
        q.setParameter("emp", emp);
        if (estado != null && !estado.isBlank()) {
            q.setParameter("estado", estado.strip());
        }
        if (busca != null && !busca.isBlank()) {
            String b = busca.strip().replace("!", "!!").replace("%", "!%").replace("_", "!_");
            q.setParameter("b", "%" + b + "%");
            q.setParameter("bn", "%" + b.toLowerCase() + "%");
        }
    }

    public NotaDetalhe detalhe(String emp, String id) {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<Object[]> l = em.createQuery(SELECT + " and n.id = :id", Object[].class).setParameter("emp", emp)
                    .setParameter("id", id).getResultList();
            if (l.isEmpty()) {
                throw naoEncontrado("Nota não encontrada.");
            }
            return detalhe(l.get(0));
        });
    }

    public NotaDetalhe detalhe(String id) {
        return detalhe(empresaDaSessao(), id);
    }

    public byte[] pdf(String id) {
        String emp = empresaDaSessao();
        List<byte[]> l = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select n.pdf from NotaEntity n where n.id = :id and n.empresa.id = :emp", byte[].class)
                .setParameter("id", id).setParameter("emp", emp).getResultList());
        if (l.isEmpty()) {
            throw naoEncontrado("Nota não encontrada.");
        }
        if (l.get(0) == null) {
            throw new ApiException(404, "pdf_indisponivel", "O PDF desta nota não está disponível.");
        }
        return l.get(0);
    }

    private static NotaItem item(Object[] l) {
        return new NotaItem((String) l[0], (String) l[1], (String) l[2], (String) l[3], (LocalDateTime) l[4],
                (BigDecimal) l[5], new PessoaResumo((String) l[6], (String) l[7], (String) l[8]),
                new ServicoResumo((String) l[9], (String) l[10]), (String) l[11], (String) l[12], (Integer) l[13],
                (String) l[14], Boolean.TRUE.equals(l[15]), (LocalDateTime) l[16]);
    }

    private static NotaDetalhe detalhe(Object[] l) {
        NotaItem i = item(l);
        String estado = i.estado();
        boolean editavel = "rascunho".equals(estado) || "rejeitada".equals(estado);
        boolean atualizar = NfseRetorno.emAndamento(estado) && i.chave() != null;
        return new NotaDetalhe(i.id(), i.numero(), i.serie(), estado, i.dataEmissao(), i.valorServicos(), i.pessoa(),
                i.servico(), i.chave(), i.numeroNfse(), i.codigoRetorno(), i.mensagemRetorno(), i.temPdf(),
                i.dataCriacao(), (String) l[17], (String) l[18], (String) l[19], (String) l[20], erros((String) l[21]),
                (String) l[22], (String) l[23], (LocalDateTime) l[24], editavel, editavel, atualizar);
    }

    /** Parse tolerante do texto salvo: array JSON de {campo, erro, descricao}; qualquer outro texto vira uma descricao. */
    static List<ErroRetorno> erros(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            JsonNode raiz = JSON.readTree(texto);
            if (raiz != null && raiz.isArray()) {
                List<ErroRetorno> r = new ArrayList<>();
                for (JsonNode e : raiz) {
                    r.add(new ErroRetorno(NfseRetorno.texto(e, "campo"), NfseRetorno.texto(e, "erro"),
                            NfseRetorno.texto(e, "descricao")));
                }
                return r.isEmpty() ? null : r;
            }
        } catch (java.io.IOException | RuntimeException e) {
            // cai no texto simples
        }
        return List.of(new ErroRetorno(null, null, texto.strip()));
    }

    // ------------------------------------------------------------------------------------------- validacao

    private Campos validar(String pessoaId, String servicoId, BigDecimal valor, String info, String discriminacao,
            boolean criacao) {
        List<Map<String, String>> f = new ArrayList<>();
        if (criacao && (pessoaId == null || pessoaId.isBlank())) {
            f.add(erro("pessoaId", "Informe o tomador."));
        }
        if (criacao && (servicoId == null || servicoId.isBlank())) {
            f.add(erro("servicoId", "Informe o serviço."));
        }
        if (valor == null) {
            if (criacao) {
                f.add(erro("valorServicos", "Informe o valor do serviço."));
            }
        } else if (valor.compareTo(VALOR_MIN) < 0 || valor.compareTo(VALOR_MAX) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            f.add(erro("valorServicos", "Informe um valor de 0,01 a 9999999999999,99 com até 2 casas decimais."));
        }
        if (info != null && info.length() > 2000) {
            f.add(erro("informacoesComplementares", "Use no máximo 2000 caracteres."));
        }
        if (discriminacao != null && discriminacao.length() > 2000) {
            f.add(erro("discriminacao", "Use no máximo 2000 caracteres."));
        }
        if (!f.isEmpty()) {
            throw invalido(f);
        }
        return new Campos(blank(pessoaId), blank(servicoId), valor == null ? null : valor.setScale(2), info, discriminacao);
    }

    private static String discriminacaoFinal(String informada, ServicoEntity s) {
        String d = informada != null && !informada.isBlank() ? informada.strip() : s.getDiscriminacaoPadrao();
        if (d == null || d.isBlank()) {
            throw new ApiException(400, "discriminacao_obrigatoria",
                    "Informe a discriminação do serviço (o serviço não tem uma padrão).",
                    Map.of("campos", List.of(erro("discriminacao", "Informe a discriminação do serviço."))));
        }
        d = d.strip();
        if (d.length() > 2000) {
            throw invalido(List.of(erro("discriminacao", "Use no máximo 2000 caracteres.")));
        }
        return d;
    }

    private PessoaEntity pessoa(String emp, String id) {
        return buscar("select p from PessoaEntity p where p.id = :id and p.empresa.id = :e", PessoaEntity.class, id, emp,
                "Tomador não encontrado.");
    }

    private ServicoEntity servico(String emp, String id) {
        ServicoEntity s = buscar("select s from ServicoEntity s where s.id = :id and s.empresa.id = :e", ServicoEntity.class,
                id, emp, "Serviço não encontrado.");
        if (!Boolean.TRUE.equals(s.getAtivo())) {
            throw new ApiException(409, "servico_inativo", "O serviço está inativo. Ative-o ou escolha outro.");
        }
        return s;
    }

    private <T> T buscar(String jpql, Class<T> tipo, String id, String emp, String msg) {
        if (id == null || id.length() > 36) {
            throw naoEncontrado(msg);
        }
        return em.createQuery(jpql, tipo).setParameter("id", id).setParameter("e", emp).getResultStream().findFirst()
                .orElseThrow(() -> naoEncontrado(msg));
    }

    private static void tomadorCompleto(PessoaEntity p) {
        List<Map<String, String>> f = NfsePayloadMapper.tomadorIncompleto(p);
        if (!f.isEmpty()) {
            throw new ApiException(422, "tomador_incompleto",
                    "Complete o cadastro do tomador (documento e endereço) para emitir a nota.", Map.of("campos", f));
        }
    }

    private UsuarioEntity usuario(String id) {
        return id == null ? null : em.getReference(UsuarioEntity.class, id);
    }

    private String empresaDaSessao() {
        String id = escopo.empresaIdOuNulo();
        if (id == null) {
            throw new ApiException(403, "empresa_nao_selecionada",
                    "Esta operação vale para a empresa da sessão. Entre na empresa para continuar.");
        }
        return id;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    private static ApiException naoEncontrado(String msg) {
        return new ApiException(404, "nao_encontrado", msg);
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }

    private static ApiException invalido(List<Map<String, String>> campos) {
        return new ApiException(400, "requisicao_invalida", "Há campos inválidos na requisição.", Map.of("campos", campos));
    }
}
