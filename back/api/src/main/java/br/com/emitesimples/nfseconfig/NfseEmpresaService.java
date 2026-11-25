package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.gateway.IntegraNotasCliente;
import br.com.emitesimples.gateway.IntegraNotasCliente.EmitenteNovo;
import br.com.emitesimples.gateway.WebhookToken;
import br.com.emitesimples.plataforma.IntegraNotasConfigService;
import br.com.emitesimples.plataforma.SegredoService;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import java.util.regex.Pattern;
import org.jboss.logging.Logger;

/**
 * Habilitacao da NFS-e da EMPRESA DA SESSAO e cadastro do emitente no IntegraNotas. A empresa e sempre a do token (o corpo
 * nunca informa empresa); a RLS do banco vale por cima. A Empresa e escrita a mao (a escrita gerada /api/Empresa e so da
 * plataforma).
 *
 * <p>Fluxo de "habilitar": (1) transacao curta com lock na linha: valida e decide se precisa criar o emitente; (2) chamada
 * externa FORA de qualquer transacao do banco; (3) token do emitente no cofre; (4) transacao curta com lock: grava o
 * resultado. PUTs concorrentes da mesma empresa se serializam num mutex em memoria (faixas de locks por hash do id) ALEM do
 * lock da linha; com varias instancias do back o mutex nao vale entre elas (ver README, riscos).
 */
@ApplicationScoped
public class NfseEmpresaService {

    private static final Logger LOG = Logger.getLogger(NfseEmpresaService.class);
    static final Pattern SERIE = Pattern.compile("^[0-9]{1,5}$");
    static final long MAX_NUMERO = 999_999_999L;

    /**
     * {@code avisos} so aparece quando ha algo a avisar (campo opcional): {@code certificado_nao_enviado} (certificado guardado
     * que nao chegou ao gateway: falha de rede/token, tente de novo) e {@code certificado_recusado} (o gateway recusou o
     * certificado; veja {@code certificadoSituacao}).
     */
    public record NfseDto(boolean habilitada, String serie, int ultimoNumero, long proximoNumero, String ambiente,
                          String statusCadastro, LocalDateTime dataCadastroGateway, boolean emitenteTokenConfigurado,
                          String certificadoSituacao, LocalDate validadeCertificado, boolean integracaoDisponivel,
                          @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> avisos) {
    }

    public record NfseEntrada(Boolean habilitada, String serie, Long ultimoNumero) {
    }

    /** O que a validacao decidiu: criar o emitente (dados) ou nao (null). */
    private record Plano(EmitenteNovo dados, String ambiente, boolean garantir, String refToken, String documento) {
    }

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
    WebhookToken webhookToken;

    @Inject
    TravaEmpresa trava;

    @Inject
    CertificadoEnvioService certificados;

    // ------------------------------------------------------------------------------------------- GET

    public NfseDto obter() {
        String empresaId = empresaDaSessao();
        boolean integracao = integracaoDisponivel(config.obter());
        return QuarkusTransaction.requiringNew().call(() -> dto(carregar(empresaId, false), integracao));
    }

    // ------------------------------------------------------------------------------------------- PUT

    public NfseDto atualizar(NfseEntrada in) {
        String empresaId = empresaDaSessao();
        if (in == null || in.habilitada() == null) {
            throw invalido(List.of(erro("habilitada", "Informe se a NFS-e fica habilitada (true ou false).")));
        }
        return trava.executar(empresaId, () -> in.habilitada() ? habilitar(empresaId, in) : desabilitar(empresaId));
    }

    private NfseDto desabilitar(String empresaId) {
        boolean integracao = integracaoDisponivel(config.obter());
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = carregar(empresaId, true);
            if (Boolean.TRUE.equals(e.getNfseHabilitada())) {
                e.setNfseHabilitada(false);
                e.setDataAtualizacao(AuthService.agora());
            }
            return dto(e, integracao);
        });
    }

    private NfseDto habilitar(String empresaId, NfseEntrada in) {
        String serie = validarSerie(in.serie());
        long ultimo = validarUltimoNumero(in.ultimoNumero());
        IntegraNotasConfigService.Atual cfg = config.obter();
        boolean integracao = integracaoDisponivel(cfg);

        Plano plano = QuarkusTransaction.requiringNew().call(() -> planejar(empresaId, serie, ultimo, cfg, integracao));

        List<String> avisos = new ArrayList<>();
        String novoSegredo = plano.garantir() ? garantirEmitente(empresaId, plano, avisos) : null;
        NfseDto gravado;
        try {
            gravado = gravar(empresaId, serie, ultimo, plano.ambiente(), novoSegredo, plano.garantir(), integracao);
        } catch (RuntimeException e) {
            if (novoSegredo != null) {
                // o emitente JA existe no gateway: o segredo fica no cofre (recuperavel pelo suporte); nada do valor no log
                LOG.errorf("NFS-e: emitente criado mas a gravacao falhou (empresa=%s, segredo=%s, %s)", empresaId,
                        novoSegredo, e.getClass().getSimpleName());
            }
            throw e;
        }
        return enviarCertificadoPendente(empresaId, gravado, integracao, avisos);
    }

    // ------------------------------------------------------------------------------------------- garantir o emitente

    /**
     * Garante o emitente no gateway ANTES de habilitar. Com token no cofre para o ambiente: sincroniza os dados
     * ({@code PUT /v1/emitente}); token recusado -> tenta recuperar pela lista da softhouse. Sem token: cria
     * ({@code POST /v1/soft/emitente}); se o CNPJ ja existe no gateway -> recupera o token pela lista e sincroniza. O webhook
     * (best-effort) e registrado depois, so quando o emitente nao foi criado agora (o POST ja leva o webhook).
     *
     * @return id do segredo do emitente NOVO criado agora (a gravar junto com a habilitacao) ou null (nada a gravar: o token
     *         ja estava no cofre ou foi registrado na recuperacao)
     */
    private String garantirEmitente(String empresaId, Plano p, List<String> avisos) {
        String ambiente = p.ambiente();
        String urlBase = config.urlBase(ambiente);
        if (p.refToken() != null) {
            String tokenEmitente = null;
            try {
                tokenEmitente = segredos.ler(p.refToken());
            } catch (RuntimeException e) {
                LOG.warnf("NFS-e: token do emitente ilegivel no cofre (empresa=%s, %s)", empresaId, e.getClass().getSimpleName());
            }
            if (tokenEmitente != null) {
                try {
                    cliente.atualizarEmitente(urlBase, tokenEmitente, p.dados());
                    enviarWebhook(urlBase, tokenEmitente, p.dados().webhook(), avisos);
                    return null;
                } catch (ApiException falha) {
                    if (!"integranotas_token_recusado".equals(falha.getCodigo())
                            || config.tokenSofthouse(ambiente).isEmpty()) {
                        throw falha; // validacao recusada, indisponivel, resposta invalida: nao habilita
                    }
                    LOG.infof("NFS-e: token do emitente recusado, recuperando pela lista (empresa=%s)", empresaId);
                }
            } else if (config.tokenSofthouse(ambiente).isEmpty()) {
                throw naoConfigurada();
            }
            recuperar(empresaId, p, urlBase, avisos);
            return null;
        }
        String tokenSofthouse = config.tokenSofthouse(ambiente).orElseThrow(NfseEmpresaService::naoConfigurada);
        try {
            return segredos.guardar(cliente.criarEmitente(urlBase, tokenSofthouse, p.dados()).token());
        } catch (ApiException falha) {
            if (IntegraNotasCliente.emitenteJaExiste(falha)) {
                LOG.infof("NFS-e: emitente ja existe no gateway, recuperando pela lista (empresa=%s)", empresaId);
                recuperar(empresaId, p, urlBase, avisos);
                return null;
            }
            marcarErro(empresaId, falha.getCodigo());
            throw falha;
        }
    }

    /**
     * Recupera o token do emitente que ja existe no gateway (lista da softhouse), registra no cofre (substitui o anterior) e
     * sincroniza os dados. Falha de validacao na sincronizacao bloqueia (o token ja fica registrado). Nao achou, veio sem
     * token ou a listagem falhou: 409 {@code emitente_existente_sem_token} e statusCadastro=erro.
     */
    private void recuperar(String empresaId, Plano p, String urlBase, List<String> avisos) {
        String ambiente = p.ambiente();
        String tokenSofthouse = config.tokenSofthouse(ambiente).orElseThrow(NfseEmpresaService::naoConfigurada);
        Optional<String> achado;
        try {
            achado = cliente.tokenDoEmitente(urlBase, tokenSofthouse, p.documento());
        } catch (ApiException falha) {
            marcarErro(empresaId, falha.getCodigo());
            throw semToken();
        }
        if (achado.isEmpty()) {
            marcarErro(empresaId, "emitente_nao_listado");
            throw semToken();
        }
        String tokenEmitente = achado.get();
        String segredo = segredos.guardar(tokenEmitente);
        registrarToken(empresaId, segredo, ambiente);
        cliente.atualizarEmitente(urlBase, tokenEmitente, p.dados());
        enviarWebhook(urlBase, tokenEmitente, p.dados().webhook(), avisos);
    }

    /** Webhook e best-effort: falha NAO bloqueia, so vira aviso. */
    private void enviarWebhook(String urlBase, String tokenEmitente, String webhook, List<String> avisos) {
        if (webhook == null) {
            return;
        }
        try {
            cliente.atualizarWebhook(urlBase, tokenEmitente, webhook);
        } catch (ApiException falha) {
            LOG.infof("NFS-e: webhook nao atualizado no gateway (codigo=%s)", falha.getCodigo());
            avisos.add("webhook_nao_atualizado");
        }
    }

    /** Registra o token recuperado: referencia, status, data, ambiente (e remove o segredo substituido). */
    private void registrarToken(String empresaId, String novoSegredo, String ambiente) {
        String antigo = QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = carregar(empresaId, true);
            String a = e.getReferenciaTokenEmpresa();
            aplicarToken(e, novoSegredo, ambiente, AuthService.agora());
            return a;
        });
        if (antigo != null && !antigo.equals(novoSegredo)) {
            segredos.remover(antigo);
        }
    }

    private static void aplicarToken(EmpresaEntity e, String novoSegredo, String ambiente, LocalDateTime agora) {
        e.setReferenciaTokenEmpresa(novoSegredo);
        e.setStatusCadastro(StatusCadastro.CADASTRADO);
        e.setDataCadastroGateway(agora);
        e.setAmbiente(ambiente);
        e.setDataAtualizacao(agora);
        if (CertificadoSituacao.guardado(e) && CertificadoSituacao.VALIDO.equals(e.getSituacaoCertificado())) {
            // emitente novo/recuperado: ele ainda nao conhece o certificado
            e.setSituacaoCertificado(CertificadoSituacao.NAO_ENVIADO);
        }
    }

    /**
     * Habilitou com sucesso: se ha certificado guardado que o gateway ainda nao aceitou, envia agora. Falha NAO desfaz a
     * habilitacao: o estado volta no DTO (situacao do certificado) e em {@code avisos}.
     */
    private NfseDto enviarCertificadoPendente(String empresaId, NfseDto gravado, boolean integracao, List<String> avisos) {
        boolean pendente = QuarkusTransaction.requiringNew().call(() -> CertificadoSituacao.pendenteDeEnvio(carregar(empresaId, false)));
        if (pendente) {
            var desfecho = certificados.enviarGuardado(empresaId);
            if (!desfecho.enviado()) {
                String c = desfecho.falha().getCodigo();
                avisos.add(c.endsWith("_gateway") ? "certificado_recusado" : "certificado_nao_enviado");
            }
        } else if (avisos.isEmpty()) {
            return gravado;
        }
        return QuarkusTransaction.requiringNew().call(() -> dto(carregar(empresaId, false), integracao, avisos));
    }

    /** Transacao 1 (com lock): valida tudo e decide se o emitente precisa ser garantido no gateway. */
    private Plano planejar(String empresaId, String serie, long ultimo, IntegraNotasConfigService.Atual cfg,
            boolean integracao) {
        EmpresaEntity e = carregar(empresaId, true);
        String ambiente = cfg.ambienteAtivo();
        // garantir: habilitando de novo (false->true), sem emitente cadastrado ou em outro ambiente
        boolean garantir = !jaCadastrado(e, ambiente) || !Boolean.TRUE.equals(e.getNfseHabilitada());
        String ref = ambiente.equals(e.getAmbiente()) ? e.getReferenciaTokenEmpresa() : null;
        if (garantir && ref == null && !integracao) {
            throw naoConfigurada();
        }
        List<Map<String, String>> faltas = camposFaltantes(e);
        if (!faltas.isEmpty()) {
            throw new ApiException(422, "cadastro_incompleto", "Complete os dados da empresa para habilitar a NFS-e.",
                    Map.of("campos", faltas));
        }
        validarNumeracao(e, serie, ultimo);
        if (!garantir) {
            return new Plano(null, ambiente, false, null, null);
        }
        return new Plano(montarEmitente(e, webhookDe(e, cfg)), ambiente, true, ref, digitos(e.getDocumento()));
    }

    /** Transacao 2 (falha do gateway): so marca erro; nunca derruba um emitente que ja estava cadastrado no ambiente. */
    private void marcarErro(String empresaId, String codigo) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                EmpresaEntity e = carregar(empresaId, true);
                e.setStatusCadastro(StatusCadastro.ERRO);
                e.setDataAtualizacao(AuthService.agora());
            });
        } catch (RuntimeException ex) {
            LOG.warnf("NFS-e: nao foi possivel marcar statusCadastro=erro (empresa=%s, %s)", empresaId,
                    ex.getClass().getSimpleName());
        }
        LOG.infof("NFS-e: cadastro do emitente falhou (empresa=%s, codigo=%s)", empresaId, codigo);
    }

    /** Transacao 3 (com lock): grava habilitacao, numeracao e, se criou, a referencia do token e o status. */
    /**
     * {@code confirmado}: o emitente acabou de ser garantido no gateway (criado, recuperado ou sincronizado com o token do
     * cofre). Sem token novo, marca {@code CADASTRADO} assim mesmo: cobre o token do emitente informado pela plataforma
     * ({@code TokenEmitenteAdminResource}), que fica {@code NAO_CADASTRADO} ate a primeira sincronizacao.
     */
    private NfseDto gravar(String empresaId, String serie, long ultimo, String ambiente, String novoSegredo,
            boolean confirmado, boolean integracao) {
        String[] antigo = new String[1];
        NfseDto dto = QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = carregar(empresaId, true);
            validarNumeracao(e, serie, ultimo);
            var agora = AuthService.agora();
            if (novoSegredo != null) {
                antigo[0] = e.getReferenciaTokenEmpresa();
                aplicarToken(e, novoSegredo, ambiente, agora);
            } else if (confirmado && e.getReferenciaTokenEmpresa() != null && ambiente.equals(e.getAmbiente())
                    && !StatusCadastro.CADASTRADO.equals(e.getStatusCadastro())) {
                e.setStatusCadastro(StatusCadastro.CADASTRADO);
                if (e.getDataCadastroGateway() == null) {
                    e.setDataCadastroGateway(agora);
                }
            }
            e.setNfseHabilitada(true);
            e.setSerieNfse(serie);
            e.setUltimoNumeroNfse((int) ultimo);
            e.setDataAtualizacao(agora);
            return dto(e, integracao);
        });
        if (antigo[0] != null) {
            segredos.remover(antigo[0]); // token do emitente de outro ambiente, substituido
        }
        return dto;
    }

    // ------------------------------------------------------------------------------------------- regras

    private static boolean jaCadastrado(EmpresaEntity e, String ambienteAtivo) {
        return StatusCadastro.CADASTRADO.equals(e.getStatusCadastro()) && e.getReferenciaTokenEmpresa() != null
                && ambienteAtivo.equals(e.getAmbiente());
    }

    private static boolean integracaoDisponivel(IntegraNotasConfigService.Atual cfg) {
        return IntegraNotasConfigService.PRODUCAO.equals(cfg.ambienteAtivo()) ? cfg.tokenProducaoConfigurado()
                : cfg.tokenHomologacaoConfigurado();
    }

    static String validarSerie(String serie) {
        String s = serie == null ? null : serie.strip();
        if (s == null || !SERIE.matcher(s).matches()) {
            throw invalido(List.of(erro("serie", "Informe a série com 1 a 5 dígitos.")));
        }
        return s;
    }

    static long validarUltimoNumero(Long n) {
        if (n == null || n < 0 || n > MAX_NUMERO) {
            throw invalido(List.of(erro("ultimoNumero", "Informe o último número emitido, de 0 a 999999999.")));
        }
        return n;
    }

    /**
     * Numeracao nunca anda para tras: o ultimo numero nao pode ser menor que o atual nem que o maior numero de nota da
     * empresa na serie. Mudar de serie so se a empresa nao tem nenhuma nota (a numeracao da nova serie e livre).
     */
    private void validarNumeracao(EmpresaEntity e, String serie, long ultimo) {
        String serieAtual = e.getSerieNfse();
        if (serieAtual != null && !serieAtual.equals(serie)) {
            if (contarNotas(e.getId()) > 0) {
                throw numeracaoInvalida("Não é possível mudar a série: a empresa já tem notas.");
            }
            return;
        }
        int atual = e.getUltimoNumeroNfse() == null ? 0 : e.getUltimoNumeroNfse();
        if (ultimo < atual) {
            throw numeracaoInvalida("O último número não pode ser menor que o atual (" + atual + ").");
        }
        long maior = maiorNumeroDeNota(e.getId(), serie);
        if (ultimo < maior) {
            throw numeracaoInvalida("O último número não pode ser menor que o maior número de nota já emitido (" + maior + ").");
        }
    }

    private long contarNotas(String empresaId) {
        return ((Number) em.createNativeQuery("select count(*) from nota where empresa_id = ?1")
                .setParameter(1, empresaId).getSingleResult()).longValue();
    }

    private long maiorNumeroDeNota(String empresaId, String serie) {
        Object r = em.createNativeQuery("select max(cast(numero as bigint)) from nota where empresa_id = ?1 and serie = ?2 "
                + "and numero ~ '^[0-9]{1,18}$'").setParameter(1, empresaId).setParameter(2, serie).getSingleResult();
        return r == null ? 0 : ((Number) r).longValue();
    }

    static List<Map<String, String>> camposFaltantes(EmpresaEntity e) {
        List<Map<String, String>> f = new ArrayList<>();
        if (vazio(e.getRazaoSocial()) || e.getRazaoSocial().strip().length() < 2) {
            f.add(erro("razaoSocial", "Informe a razão social."));
        }
        if (vazio(e.getNomeFantasia()) || e.getNomeFantasia().strip().length() < 2) {
            f.add(erro("nomeFantasia", "Informe o nome fantasia."));
        }
        String doc = digitos(e.getDocumento());
        if (doc.length() != 11 && doc.length() != 14) {
            f.add(erro("documento", "Informe o CNPJ (14 dígitos) ou o CPF (11 dígitos)."));
        }
        Integer crt = e.getRegimeTributario();
        if (crt == null || crt < 1 || crt > 4) {
            f.add(erro("regimeTributario", "Informe o regime tributário."));
        }
        String fone = digitos(e.getTelefone());
        if (fone.length() < 6 || fone.length() > 14) {
            f.add(erro("telefone", "Informe o telefone com 6 a 14 dígitos."));
        }
        if (vazio(e.getLogradouro())) {
            f.add(erro("logradouro", "Informe o logradouro."));
        }
        if (vazio(e.getNumeroEndereco())) {
            f.add(erro("numeroEndereco", "Informe o número do endereço."));
        }
        if (vazio(e.getBairro()) || e.getBairro().strip().length() < 2) {
            f.add(erro("bairro", "Informe o bairro."));
        }
        if (digitos(e.getCep()).length() != 8) {
            f.add(erro("cep", "Informe o CEP com 8 dígitos."));
        }
        MunicipioEntity m = e.getMunicipio();
        if (m == null || vazio(m.getNome()) || digitos(m.getCodigoIbge()).length() != 7 || vazio(m.getUf())
                || m.getUf().strip().length() != 2) {
            f.add(erro("municipio", "Informe o município (com código IBGE e UF)."));
        }
        return f;
    }

    /** URL do webhook desta empresa (null se nao ha URL publica configurada). */
    private String webhookDe(EmpresaEntity e, IntegraNotasConfigService.Atual cfg) {
        if (cfg.urlPublicaWebhook() == null) {
            return null;
        }
        String webhook = webhookToken.url(cfg.urlPublicaWebhook(), e.getId());
        if (webhook.length() > 200) {
            throw new ApiException(409, "url_webhook_muito_longa",
                    "A URL pública do webhook ficou longa demais para o IntegraNotas. Fale com o suporte.");
        }
        return webhook;
    }

    private static ApiException semToken() {
        return new ApiException(409, "emitente_existente_sem_token",
                "Este CNPJ já está cadastrado na IntegraNotas, mas o token do emitente não pôde ser obtido. "
                        + "Confira o token da softhouse ou fale com o suporte.");
    }

    /** Mapeia a Empresa para os dados do emitente (cadastro e sincronizacao). Exige {@link #camposFaltantes} vazio. */
    static EmitenteNovo montarEmitente(EmpresaEntity e, String webhook) {
        String doc = digitos(e.getDocumento());
        MunicipioEntity m = e.getMunicipio();
        Integer cibs = e.getRegimeIbsCbs() != null && e.getRegimeIbsCbs() >= 0 && e.getRegimeIbsCbs() <= 4
                ? e.getRegimeIbsCbs() : null;
        return new EmitenteNovo(corta(e.getNomeFantasia(), 60), corta(e.getRazaoSocial(), 60), e.getRegimeTributario(),
                digitos(e.getTelefone()), corta(e.getLogradouro(), 60), corta(e.getNumeroEndereco(), 60),
                corta(e.getBairro(), 60), digitos(m.getCodigoIbge()), corta(m.getNome(), 60),
                m.getUf().strip().toUpperCase(), digitos(e.getCep()), doc.length() == 14 ? doc : null,
                doc.length() == 11 ? doc : null,
                vazio(e.getInscricaoMunicipal()) ? null : corta(e.getInscricaoMunicipal(), 20),
                vazio(e.getEmail()) ? null : corta(e.getEmail(), 60),
                vazio(e.getComplemento()) ? null : corta(e.getComplemento(), 60), cibs, webhook);
    }

    // ------------------------------------------------------------------------------------------- internos

    private String empresaDaSessao() {
        String id = escopo.empresaIdOuNulo();
        if (id == null) {
            throw new ApiException(403, "empresa_nao_selecionada",
                    "Esta operação vale para a empresa da sessão. Entre na empresa para continuar.");
        }
        return id;
    }

    private EmpresaEntity carregar(String empresaId, boolean travar) {
        var q = em.createQuery("select e from EmpresaEntity e where e.id = :id", EmpresaEntity.class)
                .setParameter("id", empresaId);
        if (travar) {
            q.setLockMode(LockModeType.PESSIMISTIC_WRITE); // serializa PUTs concorrentes da mesma empresa
        }
        return q.getResultStream().findFirst()
                .orElseThrow(() -> new ApiException(404, "nao_encontrado", "Empresa não encontrada."));
    }

    private static NfseDto dto(EmpresaEntity e, boolean integracao) {
        return dto(e, integracao, List.of());
    }

    private static NfseDto dto(EmpresaEntity e, boolean integracao, List<String> avisos) {
        int ultimo = e.getUltimoNumeroNfse() == null ? 0 : e.getUltimoNumeroNfse();
        return new NfseDto(Boolean.TRUE.equals(e.getNfseHabilitada()), e.getSerieNfse(), ultimo, ultimo + 1L,
                e.getAmbiente(), e.getStatusCadastro(), e.getDataCadastroGateway(), e.getReferenciaTokenEmpresa() != null,
                CertificadoSituacao.situacao(e), e.getValidadeCertificado(), integracao, avisos);
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String digitos(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    private static String corta(String s, int max) {
        String t = s.strip();
        return t.length() > max ? t.substring(0, max) : t;
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

    private static ApiException numeracaoInvalida(String motivo) {
        return new ApiException(409, "numeracao_invalida", motivo);
    }

    private static ApiException naoConfigurada() {
        return new ApiException(409, "integranotas_nao_configurada",
                "A integração com a IntegraNotas não está configurada. Fale com o suporte.");
    }
}
