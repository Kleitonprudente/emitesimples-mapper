package br.com.emitesimples.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.emitesimples.security.LimiteExcedidoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Janela e expiração do bloqueio, com relógio controlado (sem esperar 15 minutos de verdade). */
class ForcaBrutaTest {

    @Test
    void bloqueiaNaQuintaFalhaELiberaApos15Minutos() {
        ForcaBruta f = new ForcaBruta();
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        f.usarRelogio(Clock.fixed(t0, ZoneOffset.UTC));
        for (int i = 0; i < 4; i++) {
            f.registrarFalha("a@x");
            assertDoesNotThrow(() -> f.verificar("a@x"));
        }
        f.registrarFalha("a@x");
        assertThrows(LimiteExcedidoException.class, () -> f.verificar("a@x"));

        f.usarRelogio(Clock.fixed(t0.plus(Duration.ofMinutes(14)), ZoneOffset.UTC));
        assertThrows(LimiteExcedidoException.class, () -> f.verificar("a@x"));
        f.usarRelogio(Clock.fixed(t0.plus(Duration.ofMinutes(15)).plusSeconds(1), ZoneOffset.UTC));
        assertDoesNotThrow(() -> f.verificar("a@x"));
    }

    @Test
    void falhasForaDaJanelaDe15MinutosNaoSeAcumulam() {
        ForcaBruta f = new ForcaBruta();
        Instant t0 = Instant.parse("2026-01-01T10:00:00Z");
        f.usarRelogio(Clock.fixed(t0, ZoneOffset.UTC));
        for (int i = 0; i < 4; i++) {
            f.registrarFalha("b@x");
        }
        f.usarRelogio(Clock.fixed(t0.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        f.registrarFalha("b@x"); // abre janela nova: é a 1ª
        assertDoesNotThrow(() -> f.verificar("b@x"));
    }

    @Test
    void sucessoZeraOContador() {
        ForcaBruta f = new ForcaBruta();
        for (int i = 0; i < 4; i++) {
            f.registrarFalha("c@x");
        }
        f.registrarSucesso("c@x");
        f.registrarFalha("c@x");
        assertDoesNotThrow(() -> f.verificar("c@x"));
    }
}
