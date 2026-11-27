package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.nfseemissao.NfseEmissaoDtos.Tributos;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Regras da tributacao da NFS-e (ISS e retencoes federais): docs/historias-emissao-nota.md, secao "Tributacao da nota".
 * Toda regra mora aqui; a preparacao da nota ({@link NfseEmissaoService}) e a montagem do JSON ({@link NfsePayloadMapper}) so
 * chamam estes metodos.
 *
 * <p>Principios: tudo opcional (cada grupo so vai com dado); dinheiro em {@code BigDecimal} com 2 casas, arredondamento
 * HALF_UP; o sistema nunca escolhe alíquota nem CST; so calcula a base padrao e o ISS retido (base x aliquota), as duas contas
 * que o governo tambem faz.
 */
final class TributacaoNota {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal VALOR_MAX = NfseEmissaoService.VALOR_MAX;

    /** CST do PIS/COFINS aceitos (lista fechada do mapa, Servico.cstPisCofins / Nota.cstPisCofins, values com codigo). */
    static final Set<String> CSTS = Set.of("01", "02", "03", "04", "05", "06", "07", "08", "09", "49", "50", "51", "52", "53",
            "54", "55", "56", "60", "61", "62", "63", "64", "65", "66", "67", "70", "71", "72", "73", "74", "75", "98", "99");

    /** Valor que as notas anteriores a esta regra gravavam no lugar de "sem CST". Tratado como ausente. */
    static final String CST_LEGADO_SEM_TRIBUTOS = "00";

    static final String MSG_ALIQUOTA = "Informe a alíquota do ISS (por exemplo 5,00). Confirme com seu contador.";
    static final String MSG_CST = "Escolha o CST do PIS/COFINS. Confirme o código com seu contador.";
    static final String MSG_RETIDOS = "O desconto e os impostos retidos, somados, não podem passar do valor da nota.";

    private TributacaoNota() {
    }

    // ------------------------------------------------------------------------------------------- ISS (tributos_municipais)

    /**
     * Ha dado de ISS: ISS retido ligado ou aliquota. Base e ISS retido sao calculados a partir deles (decisao do dono,
     * 2026-11-25). {@code issRetido = false} sozinho nao conta.
     */
    static boolean issTemDados(NotaEntity n) {
        return Boolean.TRUE.equals(n.getIssRetido()) || n.getAliquotaIss() != null;
    }

    /**
     * Regra E0625: no regime de apuracao 1 (ISS no DAS do Simples) o governo so aceita o grupo do ISS quando o tomador retem o
     * ISS; nos regimes 2 e 3 vai sempre que houver dado. Sem regime, vale 1.
     */
    static boolean issVaiNoEnvio(NotaEntity n, Integer regime) {
        boolean regimeUm = regime == null || regime == 1;
        return issTemDados(n) && (!regimeUm || Boolean.TRUE.equals(n.getIssRetido()));
    }

    /** Base do ISS em branco = valor dos servicos - desconto incondicionado (minimo 0). Deducoes ficaram fora do modelo. */
    static BigDecimal baseLegalIss(NotaEntity n) {
        BigDecimal valor = zero(n.getValorServicos());
        BigDecimal r = valor.subtract(zero(n.getValorDescontoIncondicionado()));
        return dinheiro(r.signum() < 0 ? BigDecimal.ZERO : r);
    }

    /** ISS = base x aliquota / 100, 2 casas, meio para cima. */
    static BigDecimal issCalculado(BigDecimal base, BigDecimal aliquota) {
        return base.multiply(aliquota).divide(CEM, 2, RoundingMode.HALF_UP);
    }

    /** Base do ISS que vai no envio: a da nota ou, em branco, a legal (cobre rascunho antigo sem base gravada). */
    static BigDecimal baseIssParaEnvio(NotaEntity n) {
        return n.getValorBaseCalculoIss() != null ? n.getValorBaseCalculoIss() : baseLegalIss(n);
    }

    // ------------------------------------------------------------------------------------------- federais (tributos_nacionais)

    /** CST valido para envio: vazio e o "00" das notas antigas contam como ausente. */
    static String cst(NotaEntity n) {
        String c = n.getCstPisCofins();
        return c == null || c.isBlank() || CST_LEGADO_SEM_TRIBUTOS.equals(c.strip()) ? null : c.strip();
    }

    static boolean federaisTemDados(NotaEntity n) {
        return cst(n) != null || n.getTipoRetencao() != null || n.getValorBaseCalculo() != null || n.getAliquotaPis() != null
                || n.getAliquotaCofins() != null || n.getValorPis() != null || n.getValorCofins() != null
                || n.getValorInss() != null || n.getValorIr() != null || n.getValorCsll() != null;
    }

    /** Base federal que vai no envio: a da nota ou, em branco, o valor dos servicos cheio. */
    static BigDecimal baseFederalParaEnvio(NotaEntity n) {
        return n.getValorBaseCalculo() != null ? n.getValorBaseCalculo() : dinheiro(zero(n.getValorServicos()));
    }

    /**
     * Valor LIQUIDO da nota, o que o cliente paga: servicos - desconto incondicionado - ISS retido (so quando retido) - federais retidos.
     * A mesma conta da validacao abaixo. Usado no titulo a receber gerado pela nota (docs/financeiro/README.md, E2).
     */
    static BigDecimal valorLiquido(NotaEntity n) {
        BigDecimal issRetido = Boolean.TRUE.equals(n.getIssRetido()) ? zero(n.getValorIssRetido()) : BigDecimal.ZERO;
        return dinheiro(zero(n.getValorServicos()).subtract(zero(n.getValorDescontoIncondicionado())).subtract(issRetido)
                .subtract(somaRetencoesFederais(n)));
    }

    static BigDecimal somaRetencoesFederais(NotaEntity n) {
        return zero(n.getValorPis()).add(zero(n.getValorCofins())).add(zero(n.getValorInss())).add(zero(n.getValorIr()))
                .add(zero(n.getValorCsll()));
    }

    // ------------------------------------------------------------------------------------------- preparacao

    /**
     * Padroes do servico copiados para a nota (quando a tela nao mandou {@code tributos}). Valores em reais das retencoes,
     * desconto, base e ISS retido nunca vem do servico (base e ISS sao calculados na nota).
     */
    static void copiarPadroesDoServico(NotaEntity n, ServicoEntity s, BigDecimal valor) {
        n.setIssRetido(Boolean.TRUE.equals(s.getIssRetido()));
        n.setAliquotaIss(s.getAliquotaIss());
        n.setValorBaseCalculoIss(null);
        n.setValorIssRetido(null);
        n.setCstPisCofins(cstNormalizado(s.getCstPisCofins()));
        n.setTipoRetencao(s.getTipoRetencao());
        n.setAliquotaPis(s.getAliquotaPis());
        n.setAliquotaCofins(s.getAliquotaCofins());
        n.setValorBaseCalculo(null);
        n.setValorPis(null);
        n.setValorCofins(null);
        n.setValorInss(null);
        n.setValorIr(null);
        n.setValorCsll(null);
        n.setValorDescontoIncondicionado(null);
    }

    /** O que a tela mandou substitui tudo: campo em branco limpa, inclusive o padrao do servico. */
    static void aplicarEntrada(NotaEntity n, Tributos t) {
        n.setIssRetido(Boolean.TRUE.equals(t.issRetido()));
        n.setAliquotaIss(t.aliquotaIss());
        // base e ISS retido nunca vem da entrada: sao calculados (o governo ignora o informado e recalcula)
        n.setValorBaseCalculoIss(null);
        n.setValorIssRetido(null);
        n.setCstPisCofins(cstNormalizado(t.cstPisCofins()));
        n.setTipoRetencao(t.tipoRetencao());
        n.setValorBaseCalculo(t.valorBaseCalculo());
        n.setAliquotaPis(t.aliquotaPis());
        n.setAliquotaCofins(t.aliquotaCofins());
        n.setValorPis(t.valorPis());
        n.setValorCofins(t.valorCofins());
        n.setValorInss(t.valorInss());
        n.setValorIr(t.valorIr());
        n.setValorCsll(t.valorCsll());
        n.setValorDescontoIncondicionado(
                t.valorDescontoIncondicionado() == null || t.valorDescontoIncondicionado().signum() == 0 ? null
                        : t.valorDescontoIncondicionado());
    }

    /**
     * Calcula SEMPRE, do mesmo jeito que o governo (vBC = vServ - vDescIncond; vISSQN = vBC x pAliq): base do ISS (so com dado
     * de ISS) e ISS retido (so com ISS retido ligado e aliquota valida). Sem dado, ficam vazios. A base federal em branco nao e
     * gravada: vale o valor da nota no envio.
     */
    static void completar(NotaEntity n) {
        n.setValorBaseCalculoIss(issTemDados(n) ? baseLegalIss(n) : null);
        boolean calcula = Boolean.TRUE.equals(n.getIssRetido()) && n.getAliquotaIss() != null && validoPercentual(n.getAliquotaIss());
        n.setValorIssRetido(calcula ? issCalculado(n.getValorBaseCalculoIss(), n.getAliquotaIss()) : null);
    }

    /** Problemas da nota (campo + mensagem). Vazio = pode enviar. */
    static List<Map<String, String>> problemas(NotaEntity n, Integer regime) {
        List<Map<String, String>> f = new ArrayList<>();
        BigDecimal valor = zero(n.getValorServicos());

        // formatos
        percentual(f, "aliquotaIss", "A alíquota do ISS", n.getAliquotaIss());
        percentual(f, "aliquotaPis", "A alíquota do PIS", n.getAliquotaPis());
        percentual(f, "aliquotaCofins", "A alíquota da COFINS", n.getAliquotaCofins());
        dinheiro(f, "valorBaseCalculoIss", "A base de cálculo do ISS", n.getValorBaseCalculoIss());
        dinheiro(f, "valorIssRetido", "O valor do ISS retido", n.getValorIssRetido());
        dinheiro(f, "valorBaseCalculo", "A base de cálculo federal", n.getValorBaseCalculo());
        dinheiro(f, "valorPis", "O PIS retido", n.getValorPis());
        dinheiro(f, "valorCofins", "A COFINS retida", n.getValorCofins());
        dinheiro(f, "valorInss", "O INSS retido", n.getValorInss());
        dinheiro(f, "valorIr", "O IR retido", n.getValorIr());
        dinheiro(f, "valorCsll", "A CSLL retida", n.getValorCsll());
        dinheiro(f, "valorDescontoIncondicionado", "O desconto", n.getValorDescontoIncondicionado());
        if (cst(n) != null && !CSTS.contains(cst(n))) {
            f.add(erro("cstPisCofins", "Escolha um CST do PIS/COFINS da lista. Confirme o código com seu contador."));
        }
        if (n.getTipoRetencao() != null && (n.getTipoRetencao() < 0 || n.getTipoRetencao() > 9)) {
            f.add(erro("tipoRetencao", "Escolha um tipo de retenção da lista."));
        }
        String cod = n.getCodigoTributacaoMunicipio();
        if (cod != null && !cod.isBlank() && (!cod.strip().matches("[0-9]{3}") || "000".equals(cod.strip()))) {
            f.add(erro("codigoTributacaoMunicipio", "O código de tributação municipal do serviço deve ter 3 dígitos e não pode ser 000. "
                    + "Se a sua prefeitura não tem código próprio, deixe em branco no cadastro do serviço."));
        }
        if (!f.isEmpty()) {
            return f; // sem formato valido nao ha conta que faca sentido
        }

        // ISS
        boolean retido = Boolean.TRUE.equals(n.getIssRetido());
        if (n.getValorIssRetido() != null && !retido) {
            f.add(erro("valorIssRetido", "Informe o valor do ISS retido só quando o cliente retém o ISS."));
        }
        if (issVaiNoEnvio(n, regime) && n.getAliquotaIss() == null) {
            f.add(erro("aliquotaIss", MSG_ALIQUOTA));
        }
        if (n.getValorBaseCalculoIss() != null && n.getValorBaseCalculoIss().compareTo(valor) > 0) {
            f.add(erro("valorBaseCalculoIss", "A base de cálculo do ISS não pode passar do valor da nota."));
        }
        if (retido && n.getValorIssRetido() != null && n.getValorIssRetido().compareTo(
                n.getValorBaseCalculoIss() != null ? n.getValorBaseCalculoIss() : valor) > 0) {
            f.add(erro("valorIssRetido", "O ISS retido não pode passar da base de cálculo do ISS."));
        }

        // federais
        if (federaisTemDados(n) && cst(n) == null) {
            f.add(erro("cstPisCofins", MSG_CST));
        }
        if (n.getValorBaseCalculo() != null && n.getValorBaseCalculo().compareTo(valor) > 0) {
            f.add(erro("valorBaseCalculo", "A base de cálculo federal não pode passar do valor da nota."));
        }
        BigDecimal federais = somaRetencoesFederais(n);
        if (federais.compareTo(valor) > 0) {
            f.add(erro("valorInss", "Os impostos federais retidos, somados, não podem passar do valor da nota."));
        }

        // o que sai do bolso do cliente nao pode passar do valor (senao o liquido fica negativo e o governo recusa)
        BigDecimal desconto = zero(n.getValorDescontoIncondicionado());
        if (desconto.compareTo(valor) > 0) {
            f.add(erro("valorDescontoIncondicionado", "O desconto não pode passar do valor da nota."));
        } else {
            BigDecimal issRetido = retido ? zero(n.getValorIssRetido()) : BigDecimal.ZERO;
            if (desconto.add(issRetido).add(federais).compareTo(valor) > 0) {
                f.add(erro("valorServicos", MSG_RETIDOS));
            }
        }
        return f;
    }

    // ------------------------------------------------------------------------------------------- helpers

    static BigDecimal dinheiro(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Vazio e o "00" (legado: "sem CST") viram null. */
    private static String cstNormalizado(String s) {
        return s == null || s.isBlank() || CST_LEGADO_SEM_TRIBUTOS.equals(s.strip()) ? null : s.strip();
    }

    private static boolean validoPercentual(BigDecimal v) {
        return v.signum() >= 0 && v.compareTo(CEM) <= 0 && v.stripTrailingZeros().scale() <= 2;
    }

    private static void percentual(List<Map<String, String>> f, String campo, String nome, BigDecimal v) {
        if (v != null && !validoPercentual(v)) {
            f.add(erro(campo, nome + " deve ser de 0 a 100, com até 2 casas decimais (5,00 significa 5%)."));
        }
    }

    private static void dinheiro(List<Map<String, String>> f, String campo, String nome, BigDecimal v) {
        if (v != null && (v.signum() < 0 || v.compareTo(VALOR_MAX) > 0 || v.stripTrailingZeros().scale() > 2)) {
            f.add(erro(campo, nome + " deve ser um valor de 0 a 9999999999999,99, com até 2 casas decimais."));
        }
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }
}
