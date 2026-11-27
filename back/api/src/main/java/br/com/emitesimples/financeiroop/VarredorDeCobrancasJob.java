package br.com.emitesimples.financeiroop;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Varredor das cobrancas vivas (docs/financeiro/README.md, 9.5, Fase 7): a cada 10 minutos, Pix e link; boleto, so os que nao foram
 * consultados nas ultimas 12 horas (2 vezes por dia). Desligado com {@code emitesimples.cobranca.varredor.habilitado=false} (testes).
 */
@ApplicationScoped
public class VarredorDeCobrancasJob {

    private static final Logger LOG = Logger.getLogger(VarredorDeCobrancasJob.class);

    @ConfigProperty(name = "emitesimples.cobranca.varredor.habilitado", defaultValue = "true")
    boolean habilitado;

    @Inject
    CobrancasService cobrancas;

    @Scheduled(every = "10m", delayed = "1m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void varrer() {
        if (!habilitado) {
            return;
        }
        try {
            int n = cobrancas.varrer();
            if (n > 0) {
                LOG.debugf("Varredor de cobrancas: %d consultada(s).", n);
            }
        } catch (RuntimeException e) {
            LOG.warnf("Varredor de cobrancas falhou (%s).", e.getClass().getSimpleName());
        }
    }
}
