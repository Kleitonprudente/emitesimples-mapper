package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.security.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Mutex em memoria por empresa (64 faixas por hash do id) para as operacoes que fazem chamada externa fora de transacao
 * (habilitar a NFS-e e enviar o certificado). Espera ate 30 s; senao 409 {@code operacao_em_andamento}. Vale por JVM.
 */
@ApplicationScoped
public class TravaEmpresa {

    private static final ReentrantLock[] TRAVAS = new ReentrantLock[64];

    static {
        for (int i = 0; i < TRAVAS.length; i++) {
            TRAVAS[i] = new ReentrantLock();
        }
    }

    public <T> T executar(String empresaId, Supplier<T> acao) {
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
            return acao.get();
        } finally {
            trava.unlock();
        }
    }
}
