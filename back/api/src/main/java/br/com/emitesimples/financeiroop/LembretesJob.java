package br.com.emitesimples.financeiroop;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Lembretes de cobranca (docs/financeiro/README.md, 9.3, Fase 6): todo dia as 9h (fuso de Sao Paulo), para as empresas com os lembretes
 * ligados na configuracao. Desligado com {@code emitesimples.cobranca.lembretes.habilitado=false} (testes chamam o servico direto).
 */
@ApplicationScoped
public class LembretesJob {

    private static final Logger LOG = Logger.getLogger(LembretesJob.class);

    @ConfigProperty(name = "emitesimples.cobranca.lembretes.habilitado", defaultValue = "true")
    boolean habilitado;

    @Inject
    CobrancaPorEmail cobranca;

    @Scheduled(cron = "0 0 9 * * ?", timeZone = "America/Sao_Paulo", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void diario() {
        if (!habilitado) {
            return;
        }
        try {
            int n = cobranca.rodarLembretes(TitulosService.hoje());
            LOG.infof("Lembretes de cobranca: %d e-mail(s) enviado(s).", n);
        } catch (RuntimeException e) {
            LOG.warnf("Lembretes de cobranca falharam (%s).", e.getClass().getSimpleName());
        }
    }
}
