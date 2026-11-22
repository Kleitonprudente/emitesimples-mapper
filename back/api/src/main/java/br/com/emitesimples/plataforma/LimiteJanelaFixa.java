package br.com.emitesimples.plataforma;

import br.com.emitesimples.security.LimiteExcedidoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

/** Limite por chave em janela fixa de 1 minuto, em memoria (com varias instancias o limite efetivo e N x). */
public final class LimiteJanelaFixa {

    static final Duration JANELA = Duration.ofMinutes(1);
    private static final int LIMITE_DO_MAPA = 10_000;

    private static final class Janela {
        Instant inicio;
        int usos;
    }

    private final IntSupplier limite;
    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private volatile Clock relogio = Clock.systemUTC();

    public LimiteJanelaFixa(IntSupplier limite) {
        this.limite = limite;
    }

    public void limparTudo() {
        janelas.clear();
        this.relogio = Clock.systemUTC();
    }

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
            if (j.usos >= limite.getAsInt()) {
                long restante = Math.max(1, Duration.between(agora, j.inicio.plus(JANELA)).toSeconds());
                throw new LimiteExcedidoException(restante);
            }
            j.usos++;
        }
    }
}
