package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.gateway.IntegraNotasCliente;
import br.com.emitesimples.gateway.IntegraNotasCliente.EmitenteCriado;
import br.com.emitesimples.gateway.IntegraNotasCliente.EmitenteNovo;
import br.com.emitesimples.gateway.WebhookToken;
import br.com.emitesimples.plataforma.IntegraNotasConfigService;
import br.com.emitesimples.plataforma.SegredoService;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
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
    private static final ReentrantLock[] TRAVAS = new ReentrantLock[64];

    static {
        for (int i = 0; i < TRAVAS.length; i++) {
            TRAVAS[i] = new ReentrantLock();
        }
    }

    public record NfseDto(boolean habilitada, String serie, int ultimoNumero, long proximoNumero, String ambiente,
                          String statusCadastro, LocalDateTime dataCadastroGateway, boolean emitenteTokenConfigurado,
                          String certificadoSituacao, LocalDate validadeCertificado, boolean integracaoDisponivel) {
    }

    public record NfseEntrada(Boolean habilitada, String serie, Long ultimoNumero) {
    }

    /** O que a validacao decidiu: criar o emitente (dados) ou nao (null). */
    private record Plano(EmitenteNovo novo, String ambiente) {
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
        ReentrantLock trava = TRAVAS[Math.floorMod(empresaId.hashCode(), TRAVAS.length)];
        boolean obtida;
        try {
            obtida = trava.tryLock(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            obtida = false;
        }
        if (!obtida) {
            throw new ApiException(409, "operacao_em_andamento",
                    "Já há uma alteração da NFS-e desta empresa em andamento. Tente novamente em instantes.");
        }
        try {
            return in.habilitada() ? habilitar(empresaId, in) : desabilitar(empresaId);
        } finally {
            trava.unlock();
        }
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

        String novoSegredo = null;
        if (plano.novo() != null) {
            String ambiente = plano.ambiente();
            String tokenSofthouse = config.tokenSofthouse(ambiente).orElseThrow(NfseEmpresaService::naoConfigurada);
            EmitenteCriado criado;
            try {
                criado = cliente.criarEmitente(config.urlBase(ambiente), tokenSofthouse, plano.novo());
            } catch (ApiException falha) {
                marcarErro(empresaId, falha.getCodigo());
                throw falha;
            }
            novoSegredo = segredos.guardar(criado.token());
        }
        String segredoParaGravar = novoSegredo;
        try {
            return gravar(empresaId, serie, ultimo, plano.ambiente(), segredoParaGravar, integracao);
        } catch (RuntimeException e) {
            if (novoSegredo != null) {
                // o emitente JA existe no gateway: o segredo fica no cofre (recuperavel pelo suporte); nada do valor no log
                LOG.errorf("NFS-e: emitente criado mas a gravacao falhou (empresa=%s, segredo=%s, %s)", empresaId,
                        novoSegredo, e.getClass().getSimpleName());
            }
            throw e;
        }
    }

    /** Transacao 1 (com lock): valida tudo e decide se o emitente precisa ser criado. */
    private Plano planejar(String empresaId, String serie, long ultimo, IntegraNotasConfigService.Atual cfg,
            boolean integracao) {
        EmpresaEntity e = carregar(empresaId, true);
        String ambiente = cfg.ambienteAtivo();
        boolean criar = !jaCadastrado(e, ambiente);
        if (criar && !integracao) {
            throw naoConfigurada();
        }
        List<Map<String, String>> faltas = camposFaltantes(e);
        if (!faltas.isEmpty()) {
            throw new ApiException(422, "cadastro_incompleto", "Complete os dados da empresa para habilitar a NFS-e.",
                    Map.of("campos", faltas));
        }
        validarNumeracao(e, serie, ultimo);
        if (!criar) {
            return new Plano(null, ambiente);
        }
        return new Plano(montarEmitente(e, cfg), ambiente);
    }

    /** Transacao 2 (falha do gateway): so marca erro; nunca derruba um emitente que ja estava cadastrado no ambiente. */
    private void marcarErro(String empresaId, String codigo) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                EmpresaEntity e = carregar(empresaId, true);
                e.setStatusCadastro("erro");
                e.setDataAtualizacao(AuthService.agora());
            });
        } catch (RuntimeException ex) {
            LOG.warnf("NFS-e: nao foi possivel marcar statusCadastro=erro (empresa=%s, %s)", empresaId,
                    ex.getClass().getSimpleName());
        }
        LOG.infof("NFS-e: cadastro do emitente falhou (empresa=%s, codigo=%s)", empresaId, codigo);
    }

    /** Transacao 3 (com lock): grava habilitacao, numeracao e, se criou, a referencia do token e o status. */
    private NfseDto gravar(String empresaId, String serie, long ultimo, String ambiente, String novoSegredo,
            boolean integracao) {
        String[] antigo = new String[1];
        NfseDto dto = QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = carregar(empresaId, true);
            validarNumeracao(e, serie, ultimo);
            var agora = AuthService.agora();
            if (novoSegredo != null) {
                antigo[0] = e.getReferenciaTokenEmpresa();
                e.setReferenciaTokenEmpresa(novoSegredo);
                e.setStatusCadastro("cadastrado");
                e.setDataCadastroGateway(agora);
                e.setAmbiente(ambiente);
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
        return "cadastrado".equals(e.getStatusCadastro()) && e.getReferenciaTokenEmpresa() != null
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

    private static List<Map<String, String>> camposFaltantes(EmpresaEntity e) {
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

    private EmitenteNovo montarEmitente(EmpresaEntity e, IntegraNotasConfigService.Atual cfg) {
        String doc = digitos(e.getDocumento());
        MunicipioEntity m = e.getMunicipio();
        String webhook = null;
        if (cfg.urlPublicaWebhook() != null) {
            webhook = webhookToken.url(cfg.urlPublicaWebhook(), e.getId());
            if (webhook.length() > 200) {
                throw new ApiException(409, "url_webhook_muito_longa",
                        "A URL pública do webhook ficou longa demais para o IntegraNotas. Fale com o suporte.");
            }
        }
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
        int ultimo = e.getUltimoNumeroNfse() == null ? 0 : e.getUltimoNumeroNfse();
        return new NfseDto(Boolean.TRUE.equals(e.getNfseHabilitada()), e.getSerieNfse(), ultimo, ultimo + 1L,
                e.getAmbiente(), e.getStatusCadastro(), e.getDataCadastroGateway(), e.getReferenciaTokenEmpresa() != null,
                e.getSituacaoCertificado(), e.getValidadeCertificado(), integracao);
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
