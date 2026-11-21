package br.com.emitesimples.auth;

import br.com.emitesimples.security.LimiteExcedidoException;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Proteção contra força bruta: 5 falhas em 15 minutos, por e-mail, bloqueiam o e-mail por 15 minutos (429).
 *
 * LIMITAÇÃO: o contador vive na MEMÓRIA desta instância. Com várias instâncias (ou reinício) cada uma tem o seu
 * contador, e o limite efetivo vira N x 5. Em produção com mais de uma instância é preciso armazenamento
 * compartilhado (Redis, tabela no banco). Efeito colateral conhecido: quem sabe o e-mail de alguém pode bloqueá-lo
 * (negação de serviço dirigida); mitigação futura: limitar também por IP.
 */
@ApplicationScoped
public class ForcaBruta {

    static final int MAX_FALHAS = 5;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final Duration BLOQUEIO = Duration.ofMinutes(15);
    private static final int LIMITE_DO_MAPA = 10_000;

    private static final class Estado {
        int falhas;
        Instant inicioJanela;
        Instant bloqueadoAte;
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();
    private volatile Clock relogio = Clock.systemUTC();

    /** Só para teste: troca o relógio. */
    void usarRelogio(Clock novo) {
        this.relogio = novo;
    }

    void limparTudo() {
        estados.clear();
        this.relogio = Clock.systemUTC();
    }

    /** Lança 429 se o e-mail está bloqueado. */
    public void verificar(String chave) {
        Estado e = estados.get(chave);
        if (e == null) {
            return;
        }
        Instant agora = relogio.instant();
        synchronized (e) {
            if (e.bloqueadoAte != null) {
                if (agora.isBefore(e.bloqueadoAte)) {
                    throw new LimiteExcedidoException(Math.max(1, Duration.between(agora, e.bloqueadoAte).toSeconds()));
                }
                estados.remove(chave, e);
            }
        }
    }

    public void registrarFalha(String chave) {
        Instant agora = relogio.instant();
        if (estados.size() > LIMITE_DO_MAPA) {
            estados.entrySet().removeIf(en -> expirado(en.getValue(), agora));
        }
        Estado e = estados.computeIfAbsent(chave, k -> new Estado());
        synchronized (e) {
            if (e.inicioJanela == null || agora.isAfter(e.inicioJanela.plus(JANELA))) {
                e.inicioJanela = agora;
                e.falhas = 0;
            }
            e.falhas++;
            if (e.falhas >= MAX_FALHAS) {
                e.bloqueadoAte = agora.plus(BLOQUEIO);
            }
        }
    }

    public void registrarSucesso(String chave) {
        estados.remove(chave);
    }

    private static boolean expirado(Estado e, Instant agora) {
        synchronized (e) {
            if (e.bloqueadoAte != null) {
                return !agora.isBefore(e.bloqueadoAte);
            }
            return e.inicioJanela == null || agora.isAfter(e.inicioJanela.plus(JANELA));
        }
    }
}
