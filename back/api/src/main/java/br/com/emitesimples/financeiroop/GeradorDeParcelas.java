package br.com.emitesimples.financeiroop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gera as parcelas de um lancamento (docs/financeiro/README.md, F2-H1; referencia §5.1). Sem banco e sem estado.
 *
 * <ul>
 * <li>PARCELADO: divide o total; o resíduo de centavos vai na ULTIMA parcela; vencimentos a cada {@code intervaloDias} da 1ª data.</li>
 * <li>RECORRENTE: {@code repetirMeses} titulos de MESMO valor, no mesmo dia dos meses seguintes, sempre contado da 1ª data
 * (31/01 -> 28/02 -> 31/03: {@link LocalDate#plusMonths} nao "estoura" o mes e nao acumula o recuo de fevereiro).</li>
 * </ul>
 */
public final class GeradorDeParcelas {

    public record Parcela(int numero, BigDecimal valor, LocalDate vencimento) {
    }

    private GeradorDeParcelas() {
    }

    public static List<Parcela> parcelado(BigDecimal total, LocalDate vencimento, int parcelas, int intervaloDias) {
        BigDecimal[] valores = Dinheiro.ratear(total, parcelas);
        List<Parcela> r = new ArrayList<>(parcelas);
        for (int i = 0; i < parcelas; i++) {
            r.add(new Parcela(i + 1, valores[i], vencimento.plusDays((long) i * intervaloDias)));
        }
        return r;
    }

    public static List<Parcela> recorrente(BigDecimal valor, LocalDate vencimento, int repetirMeses) {
        List<Parcela> r = new ArrayList<>(repetirMeses);
        for (int i = 0; i < repetirMeses; i++) {
            r.add(new Parcela(i + 1, Dinheiro.normalizar(valor), vencimento.plusMonths(i)));
        }
        return r;
    }
}
