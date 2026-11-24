package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.gateway.IntegraNotasCliente;
import br.com.emitesimples.gateway.IntegraNotasCliente.RetornoNfse;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.plataforma.IntegraNotasConfigService;
import br.com.emitesimples.plataforma.SegredoService;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Acompanhamento das NFS-e em andamento: consulta o gateway ({@code GET /v1/nfse/{chave}}, a AUTORIDADE) e aplica o retorno
 * pela tabela de {@link NfseRetorno}. Tres gatilhos: o job agendado, o {@code POST /atualizar} e o webhook (que e so gatilho).
 * O job roda SEM sessao HTTP: a unica leitura entre empresas (ids das notas pendentes) usa {@code comoSistema}; cada consulta
 * roda {@code comoEmpresa(empresaId)} (RLS da propria empresa). Backoff por nota em memoria: a cada 10 s nos primeiros 2 min
 * e a cada 60 s depois, ate 24 h; passado isso a mensagem vira "Sem retorno do gateway; use Atualizar".
 */
@ApplicationScoped
public class NfseAcompanhamentoService {

    private static final Logger LOG = Logger.getLogger(NfseAcompanhamentoService.class);
    static final String MSG_SEM_RETORNO = "Sem retorno do gateway; use Atualizar";
    static final Duration LIMITE_ACOMPANHAMENTO = Duration.ofHours(24);

    @ConfigProperty(name = "emitesimples.nfse.acompanhamento.max-consultas-por-ciclo", defaultValue = "20")
    int maxPorCiclo;

    @ConfigProperty(name = "emitesimples.nfse.permitir-emissao-producao", defaultValue = "false")
    boolean permitirProducao;

    @Inject
    EntityManager em;

    @Inject
    IntegraNotasConfigService config;

    @Inject
    SegredoService segredos;

    @Inject
    IntegraNotasCliente cliente;

    @Inject
    ContextoTenantBanco contexto;

    @Inject
    NfseChamadaLog chamadas;

    private final Map<String, Instant> ultimaConsulta = new ConcurrentHashMap<>();
    private volatile ThreadPoolExecutor executor;

    private record Ctx(String chave, String ambiente, String tokenRef, String identificacao) {
    }

    // ------------------------------------------------------------------------------------- consulta de UMA nota

    /**
     * Consulta o gateway e aplica o retorno. Devolve false se a nota nao e acompanhavel (estado, sem chave, outro ambiente).
     * Falha de infraestrutura (token recusado, indisponivel, resposta invalida) lanca {@link ApiException}.
     * Quem chama define o contexto de empresa (HTTP: o JWT; job/webhook: {@code comoEmpresa}).
     */
    public boolean consultar(String empresaId, String notaId) {
        Ctx c = QuarkusTransaction.requiringNew().call(() -> contexto(empresaId, notaId));
        if (c == null) {
            return false;
        }
        String token;
        String urlBase;
        try {
            token = segredos.ler(c.tokenRef());
            urlBase = config.urlBase(c.ambiente());
        } catch (RuntimeException e) {
            LOG.warnf("NFS-e: cofre/config indisponivel na consulta (empresa=%s, %s)", empresaId, e.getClass().getSimpleName());
            throw new ApiException(504, "integranotas_indisponivel", "Não foi possível falar com o IntegraNotas agora.");
        }
        ultimaConsulta.put(notaId, Instant.now());
        long ini = System.nanoTime();
        RetornoNfse r;
        try {
            r = cliente.consultarNfse(urlBase, token, c.chave());
        } catch (ApiException e) {
            chamadas.registrar(empresaId, notaId, "CONSULTAR", "GET", "/v1/nfse/{chave}", "V1", null, null, e.getCodigo(),
                    ms(ini), c.chave(), c.identificacao(), null, c.ambiente());
            throw e;
        }
        chamadas.registrar(empresaId, notaId, "CONSULTAR", "GET", "/v1/nfse/{chave}", "V1", r.httpStatus(), r.codigo(),
                r.mensagem(), ms(ini), c.chave(), c.identificacao(), null, c.ambiente());
        String idResp = NfseRetorno.texto(r.raiz(), "identificacao");
        if (idResp != null && !idResp.equals(c.identificacao()) && !idResp.equals(notaId)) {
            LOG.warnf("NFS-e: consulta devolveu identificacao diferente da nota (empresa=%s); retorno ignorado", empresaId);
            return true;
        }
        QuarkusTransaction.requiringNew().run(() -> {
            NotaEntity n = travar(empresaId, notaId);
            if (n != null) {
                NfseRetorno.aplicarConsulta(n, r);
                NfseRetorno.concluirCancelamentoPendente(em, n, r);
            }
        });
        return true;
    }

    private Ctx contexto(String empresaId, String notaId) {
        NotaEntity n = carregar(empresaId, notaId, false);
        if (n == null || n.getChave() == null || !NfseRetorno.emAndamento(n.getEstado()) && !cancelamentoPendente(n)) {
            return null;
        }
        EmpresaEntity e = em.find(EmpresaEntity.class, empresaId);
        String ativo = config.ambienteAtivo();
        if (e == null || e.getReferenciaTokenEmpresa() == null || n.getAmbiente() == null
                || !n.getAmbiente().equals(e.getAmbiente()) || !n.getAmbiente().equals(ativo)
                || IntegraNotasConfigService.PRODUCAO.equals(ativo) && !permitirProducao) {
            return null;
        }
        return new Ctx(n.getChave(), n.getAmbiente(), e.getReferenciaTokenEmpresa(), NfsePayloadMapper.identificacao(notaId));
    }

    /** Nota autorizada com pedido de cancelamento ainda sem resposta conclusiva: acompanhada ate ver o 101. */
    private boolean cancelamentoPendente(NotaEntity n) {
        return EstadosNfse.AUTORIZADA.equals(n.getEstado()) && em.createQuery("select count(c) from CancelamentoEntity c where "
                + "c.nota.id = :n and c.empresa.id = :e and c.estado = 'PROCESSANDO'", Long.class)
                .setParameter("n", n.getId()).setParameter("e", n.getEmpresa().getId()).getSingleResult() > 0;
    }

    NotaEntity carregar(String empresaId, String notaId, boolean travar) {
        var q = em.createQuery("select n from NotaEntity n where n.id = :id and n.empresa.id = :e", NotaEntity.class)
                .setParameter("id", notaId).setParameter("e", empresaId);
        if (travar) {
            q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        return q.getResultStream().findFirst().orElse(null);
    }

    private NotaEntity travar(String empresaId, String notaId) {
        return carregar(empresaId, notaId, true);
    }

    private static long ms(long iniNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - iniNanos);
    }

    // ------------------------------------------------------------------------------------- job

    /** Um ciclo do job: consulta ate N notas devidas (backoff) e marca as que passaram de 24 h. */
    public int executarCiclo() {
        String ativo;
        try {
            ativo = config.ambienteAtivo();
        } catch (RuntimeException e) {
            return 0;
        }
        if (IntegraNotasConfigService.PRODUCAO.equals(ativo) && !permitirProducao) {
            return 0;
        }
        LocalDateTime corte = AuthService.agora().minus(LIMITE_ACOMPANHAMENTO);
        String amb = ativo;
        List<Object> linhas = new java.util.ArrayList<>(contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select n.id, n.empresa.id, n.dataCriacao from NotaEntity n where n.estado in ('PROCESSANDO','EMITIDA_OFFLINE') "
                        + "and n.chave is not null and n.ambiente = :a and n.dataCriacao > :c order by n.dataAtualizacao asc, n.id")
                .setParameter("a", amb).setParameter("c", corte).setMaxResults(500).getResultList())));
        // notas autorizadas com cancelamento pendente (a idade vem do pedido, sem corte de 24 h: a consulta o expira)
        linhas.addAll(contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select n.id, n.empresa.id, c.dataCriacao from CancelamentoEntity c join c.nota n where c.estado = 'PROCESSANDO' "
                        + "and n.estado = 'AUTORIZADA' and n.chave is not null and n.ambiente = :a order by c.dataAtualizacao asc, c.id")
                .setParameter("a", amb).setMaxResults(100).getResultList())));
        Instant agora = Instant.now();
        int feitas = 0;
        for (Object o : linhas) {
            if (feitas >= maxPorCiclo) {
                break;
            }
            Object[] l = (Object[]) o;
            String notaId = (String) l[0];
            String empresaId = (String) l[1];
            LocalDateTime criada = (LocalDateTime) l[2];
            Duration idade = Duration.between(criada, AuthService.agora());
            Duration intervalo = idade.compareTo(Duration.ofMinutes(2)) < 0 ? Duration.ofSeconds(10) : Duration.ofSeconds(60);
            Instant ult = ultimaConsulta.get(notaId);
            if (ult != null && Duration.between(ult, agora).compareTo(intervalo) < 0) {
                continue;
            }
            feitas++;
            consultarComoEmpresa(empresaId, notaId);
        }
        marcarSemRetorno(corte, amb);
        if (ultimaConsulta.size() > 10_000) {
            Instant limite = agora.minus(Duration.ofHours(1));
            ultimaConsulta.values().removeIf(i -> i.isBefore(limite));
        }
        return feitas;
    }

    /** Consulta no contexto da empresa, engolindo (e registrando sem detalhes) falhas: usado pelo job e pelo webhook. */
    void consultarComoEmpresa(String empresaId, String notaId) {
        try {
            contexto.comoEmpresa(empresaId, () -> consultar(empresaId, notaId));
        } catch (ApiException e) {
            LOG.infof("NFS-e: consulta nao concluida (empresa=%s, codigo=%s)", empresaId, e.getCodigo());
        } catch (RuntimeException e) {
            LOG.warnf("NFS-e: consulta falhou (empresa=%s, %s)", empresaId, e.getClass().getSimpleName());
        }
    }

    private void marcarSemRetorno(LocalDateTime corte, String ambiente) {
        List<?> antigas = contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select n.id, n.empresa.id from NotaEntity n where n.estado in ('PROCESSANDO','EMITIDA_OFFLINE') "
                        + "and n.chave is not null and n.ambiente = :a and n.dataCriacao <= :c "
                        + "and (n.mensagemRetorno is null or n.mensagemRetorno <> :m)")
                .setParameter("a", ambiente).setParameter("c", corte).setParameter("m", MSG_SEM_RETORNO)
                .setMaxResults(50).getResultList()));
        for (Object o : antigas) {
            Object[] l = (Object[]) o;
            String notaId = (String) l[0];
            String empresaId = (String) l[1];
            try {
                contexto.comoEmpresa(empresaId, () -> QuarkusTransaction.requiringNew().call(() -> {
                    NotaEntity n = travar(empresaId, notaId);
                    if (n != null && NfseRetorno.emAndamento(n.getEstado())) {
                        n.setMensagemRetorno(MSG_SEM_RETORNO);
                        n.setDataAtualizacao(AuthService.agora());
                    }
                    return null;
                }));
                ultimaConsulta.remove(notaId);
            } catch (RuntimeException e) {
                LOG.warnf("NFS-e: nao foi possivel marcar nota sem retorno (%s)", e.getClass().getSimpleName());
            }
        }
    }

    // ------------------------------------------------------------------------------------- webhook (gatilho)

    /** Agenda uma consulta imediata (fora da requisicao do webhook, que precisa responder em 3 s). */
    public void agendarConsulta(String empresaId, String notaId) {
        ThreadPoolExecutor ex = executor;
        if (ex == null) {
            synchronized (this) {
                if (executor == null) {
                    ThreadPoolExecutor novo = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(200), r -> {
                        Thread t = new Thread(r, "nfse-gatilho");
                        t.setDaemon(true);
                        return t;
                    }, new ThreadPoolExecutor.DiscardPolicy());
                    executor = novo;
                }
                ex = executor;
            }
        }
        ex.execute(() -> consultarComoEmpresa(empresaId, notaId));
    }

    @PreDestroy
    void encerrar() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
