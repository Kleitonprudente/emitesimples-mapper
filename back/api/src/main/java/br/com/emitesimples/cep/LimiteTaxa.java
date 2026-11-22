package br.com.emitesimples.cep;

import br.com.emitesimples.security.LimiteExcedidoException;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Limite de taxa por usuario (janela fixa de 1 minuto, em memoria; mesma limitacao do ForcaBruta: com varias instancias
 * o limite efetivo e N x o configurado). Estourou: 429 muitas_tentativas com Retry-After.
 */
@ApplicationScoped
public class LimiteTaxa {

    static final Duration JANELA = Duration.ofMinutes(1);
    private static final int LIMITE_DO_MAPA = 10_000;

    private static final class Janela {
        Instant inicio;
        int usos;
    }

    @ConfigProperty(name = "emitesimples.cep.limite-por-minuto", defaultValue = "30")
    int limite;

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private volatile Clock relogio = Clock.systemUTC();

    void usarRelogio(Clock novo) {
        this.relogio = novo;
    }

    void limparTudo() {
        janelas.clear();
        this.relogio = Clock.systemUTC();
    }

    /** Conta uma chamada da chave; lanca 429 se passou do limite da janela. */
    public void consumir(String chave) {
        Instant agora = relogio.instant();
        if (janelas.size() > LIMITE_DO_MAPA) {
            janelas.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().inicio == null || !agora.isBefore(e.getValue().inicio.plus(JANELA));
                }
            });
        }
        Janela j = janelas.computeIfAbsent(chave, k -> new Janela());
        synchronized (j) {
            if (j.inicio == null || !agora.isBefore(j.inicio.plus(JANELA))) {
                j.inicio = agora;
                j.usos = 0;
            }
            if (j.usos >= limite) {
                long restante = Math.max(1, Duration.between(agora, j.inicio.plus(JANELA)).toSeconds());
                throw new LimiteExcedidoException(restante);
            }
            j.usos++;
        }
    }
}
