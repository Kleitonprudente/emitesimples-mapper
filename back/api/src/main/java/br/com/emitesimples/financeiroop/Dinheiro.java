package br.com.emitesimples.financeiroop;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Dinheiro do modulo financeiro (escrito a mao; docs/financeiro/README.md, F1-H7 e adaptacao A1). Valor em {@link BigDecimal} com
 * ESCALA 2 (o mapa usa ValorMonetario(15,2)); nunca double/float. Arredondamento NBR 5891 = {@link RoundingMode#HALF_EVEN}, UMA vez,
 * no fim do calculo. Teto de R$ 999.999.999,99 em todo campo de valor.
 */
public final class Dinheiro {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;
    public static final BigDecimal TETO = new BigDecimal("999999999.99");
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA);
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    /** "1.234,56", "1234,56", "-10", "R$ 5,5" (ponto e separador de milhar; virgula, de centavos). */
    private static final Pattern TEXTO = Pattern.compile("^-?(\\d{1,3}(\\.\\d{3})*|\\d+)(,\\d{1,2})?$");

    private Dinheiro() {
    }

    /** Converte o texto da tela ("1.234,56") em valor com escala 2. Recusa texto fora do formato ou acima do teto. */
    public static BigDecimal deTexto(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("Informe o valor.");
        }
        String t = texto.replace("R$", "").replace(" ", "").replace(" ", "").trim();
        if (!TEXTO.matcher(t).matches()) {
            throw new IllegalArgumentException("Valor inválido. Use o formato 1.234,56.");
        }
        return dentroDoTeto(new BigDecimal(t.replace(".", "").replace(',', '.')).setScale(ESCALA, ARREDONDAMENTO));
    }

    /** Formata em reais: "R$ 1.234,56" (negativo: "-R$ 1.234,56"). */
    public static String formatar(BigDecimal valor) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        BigDecimal v = normalizar(valor);
        return (v.signum() < 0 ? "-R$ " : "R$ ") + f.format(v.abs());
    }

    /** Escala 2 (HALF_EVEN); nulo vira zero. */
    public static BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? ZERO : valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    /** Recusa valor cujo modulo passe do teto. */
    public static BigDecimal dentroDoTeto(BigDecimal valor) {
        if (valor.abs().compareTo(TETO) > 0) {
            throw new IllegalArgumentException("O valor passa do limite de R$ 999.999.999,99.");
        }
        return valor;
    }

    /**
     * Divide o total em partes iguais em centavos; o RESIDUO vai na ULTIMA parcela (R$ 100,00 em 3 = 33,33 / 33,33 / 33,34).
     * Cada parte precisa de ao menos 1 centavo.
     */
    public static BigDecimal[] ratear(BigDecimal total, int partes) {
        if (partes < 1) {
            throw new IllegalArgumentException("O número de parcelas deve ser ao menos 1.");
        }
        long centavos = normalizar(total).movePointRight(ESCALA).longValueExact();
        long base = centavos / partes;
        long resto = centavos % partes;
        if (base < 1) {
            throw new IllegalArgumentException("O valor é pequeno demais para " + partes + " parcelas: cada uma precisa de ao menos R$ 0,01.");
        }
        BigDecimal[] v = new BigDecimal[partes];
        Arrays.fill(v, BigDecimal.valueOf(base, ESCALA));
        v[partes - 1] = BigDecimal.valueOf(base + resto, ESCALA);
        return v;
    }

    /** valor x percentual / 100, arredondado uma vez (HALF_EVEN, escala 2). */
    public static BigDecimal porcentagem(BigDecimal valor, BigDecimal percentual) {
        return porcentagemProporcional(valor, percentual, 1, 1);
    }

    /**
     * valor x percentual / 100 x numerador / denominador, arredondado UMA vez no fim (HALF_EVEN, escala 2). Ex.: juros simples de
     * 1% ao mes por 10 dias sobre R$ 1.000,00 = porcentagemProporcional(1000, 1, 10, 30) = R$ 3,33.
     */
    public static BigDecimal porcentagemProporcional(BigDecimal valor, BigDecimal percentual, long numerador, long denominador) {
        if (denominador == 0) {
            throw new IllegalArgumentException("Denominador zero.");
        }
        return valor.multiply(percentual).multiply(BigDecimal.valueOf(numerador))
                .divide(CEM.multiply(BigDecimal.valueOf(denominador)), ESCALA, ARREDONDAMENTO);
    }
}
