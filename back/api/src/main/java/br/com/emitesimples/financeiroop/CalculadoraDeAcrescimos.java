package br.com.emitesimples.financeiroop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Sugestao de multa e juros na baixa de titulo A RECEBER (docs/financeiro/README.md, F3-H2; referencia §6.2). A pessoa pode editar a
 * sugestao livremente. Titulo A PAGAR nao usa: os valores vem do boleto do fornecedor e sao digitados.
 *
 * <pre>
 * diasAtraso = max(0, dataBaixa - vencimento)            dias corridos
 * diasAtraso <= carencia  ->  multa = 0, juros = 0
 * senao: multa = quitado x multa% / 100                   unica, sobre o que se quita AGORA
 *        juros = quitado x juros% / 100 x dias / 30       simples, mes de 30 dias, desde o dia seguinte ao vencimento, sem a multa
 * </pre>
 * Arredonda UMA vez cada parcela do acrescimo (HALF_EVEN, escala 2).
 */
public final class CalculadoraDeAcrescimos {

    public record Sugestao(long diasAtraso, BigDecimal multa, BigDecimal juros) {

        public BigDecimal total(BigDecimal quitado) {
            return Dinheiro.normalizar(quitado).add(multa).add(juros);
        }
    }

    private CalculadoraDeAcrescimos() {
    }

    /**
     * @param receber   true para titulo a receber (a pagar devolve zero)
     * @param multaPct  percentual do titulo, ou o da configuracao quando o titulo nao tem o seu
     * @param jurosPct  idem, juros simples ao mes
     */
    public static Sugestao sugerir(boolean receber, BigDecimal quitado, LocalDate vencimento, LocalDate dataBaixa, int carenciaDias,
            BigDecimal multaPct, BigDecimal jurosPct) {
        long dias = Math.max(0, ChronoUnit.DAYS.between(vencimento, dataBaixa));
        if (!receber || dias <= carenciaDias || quitado == null || quitado.signum() <= 0) {
            return new Sugestao(dias, Dinheiro.ZERO, Dinheiro.ZERO);
        }
        BigDecimal multa = multaPct == null ? Dinheiro.ZERO : Dinheiro.porcentagem(quitado, multaPct);
        BigDecimal juros = jurosPct == null ? Dinheiro.ZERO : Dinheiro.porcentagemProporcional(quitado, jurosPct, dias, 30);
        return new Sugestao(dias, multa, juros);
    }
}
