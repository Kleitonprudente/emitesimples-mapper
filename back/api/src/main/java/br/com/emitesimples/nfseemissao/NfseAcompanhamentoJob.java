package br.com.emitesimples.nfseemissao;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/** Job que acompanha as NFS-e em andamento. Desligado com {@code emitesimples.nfse.acompanhamento.habilitado=false} (testes). */
@ApplicationScoped
public class NfseAcompanhamentoJob {

    private static final Logger LOG = Logger.getLogger(NfseAcompanhamentoJob.class);

    @ConfigProperty(name = "emitesimples.nfse.acompanhamento.habilitado", defaultValue = "true")
    boolean habilitado;

    @Inject
    NfseAcompanhamentoService service;

    @Scheduled(every = "{emitesimples.nfse.acompanhamento.intervalo}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void ciclo() {
        if (!habilitado) {
            return;
        }
        try {
            service.executarCiclo();
        } catch (RuntimeException e) {
            LOG.warnf("NFS-e: ciclo de acompanhamento falhou (%s)", e.getClass().getSimpleName());
        }
    }
}
