package br.com.emitesimples.financeiroop;

import br.com.emitesimples.security.ApiException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduz as restricoes do banco do modulo financeiro (migracao V14: CHECK, indice unico, FK e as triggers que levantam SQLSTATE 23514
 * com o nome da regra) para 400 {@code requisicao_invalida} (indice unico: 409 {@code conflito}) com o erro no campo, no mesmo formato do ServicoTributacaoListener.
 * Chamado pelo ErrosHandler. A mensagem diz O QUE FAZER (docs/financeiro/README.md). Nunca devolve o texto do banco (traz SQL).
 */
public final class RestricoesFinanceiro {

    private record Regra(String campo, String mensagem) {
    }

    private static final Map<String, Regra> REGRAS = new LinkedHashMap<>();

    static {
        // Conta financeira (F1-H1)
        REGRAS.put("ux_contafinanceira_nome", new Regra("nome", "Já existe uma conta com este nome."));
        REGRAS.put("ck_contafinanceira_tipo", new Regra("tipo", "Escolha o tipo da conta: caixa, conta em banco ou carteira digital."));
        REGRAS.put("ck_contafinanceira_agencia", new Regra("agencia", "A agência só aceita letras, números e hífen."));
        REGRAS.put("ck_contafinanceira_saldo_teto", new Regra("saldoInicial", "O saldo inicial passa do limite de R$ 999.999.999,99."));
        REGRAS.put("fk_contafinanceira_banco", new Regra("codigoBanco", "Banco não encontrado. Confira o código do banco."));
        for (String forma : List.of("dinheiro", "pix", "boleto", "cartao", "transferencia")) {
            String campo = "padrao" + Character.toUpperCase(forma.charAt(0)) + forma.substring(1);
            REGRAS.put("ux_contafinanceira_padrao_" + forma,
                    new Regra(campo, "Outra conta acabou de ser marcada como padrão desta forma. Tente de novo."));
        }
        // Categoria (F1-H2)
        REGRAS.put("ux_categoriafinanceira_nome", new Regra("nome", "Já existe uma categoria com este nome neste tipo."));
        REGRAS.put("ck_categoriafinanceira_tipo", new Regra("tipo", "Escolha o tipo da categoria: receita ou despesa."));
        REGRAS.put("ck_categoriafinanceira_pai_proprio", new Regra("categoriaPai", "Uma categoria não pode ser pai de si mesma."));
        REGRAS.put("ck_categoriafinanceira_dicas", new Regra("dicas", "As dicas não podem ter os sinais < e >."));
        REGRAS.put("ck_categoria_pai_empresa", new Regra("categoriaPai", "Categoria pai não encontrada."));
        REGRAS.put("ck_categoria_pai_tipo", new Regra("categoriaPai", "A categoria pai precisa ser do mesmo tipo (receita ou despesa)."));
        REGRAS.put("ck_categoria_pai_nivel", new Regra("categoriaPai", "A categoria pai precisa ser de topo: escolha uma categoria sem pai."));
        REGRAS.put("ck_categoria_com_filhas_vira_filha", new Regra("categoriaPai", "Uma categoria com filhas não pode virar filha de outra."));
        REGRAS.put("ck_categoria_pai_inativo", new Regra("categoriaPai", "Ative a categoria pai antes."));
        REGRAS.put("ck_categoria_pai_em_uso", new Regra("categoriaPai",
                "A categoria escolhida como pai já está em uso e não pode agrupar outras. Escolha outra categoria pai."));
        REGRAS.put("ck_categoria_tipo_com_filhas", new Regra("tipo", "Uma categoria com filhas não pode mudar de tipo."));
        REGRAS.put("ck_categoria_inativar_pai", new Regra("ativo", "Inative as filhas antes."));
        // Condicao de pagamento (F1-H3)
        REGRAS.put("ux_condicaopagamento_nome", new Regra("nome", "Já existe uma condição de pagamento com este nome."));
        REGRAS.put("ck_condicaopagamento_forma", new Regra("forma", "Escolha a forma de pagamento."));
        REGRAS.put("ck_condicaopagamento_parcelas", new Regra("parcelas", "O número de parcelas deve ser de 1 a 48."));
        REGRAS.put("ck_condicaopagamento_intervalo", new Regra("intervaloDias", "O intervalo entre parcelas deve ser de 0 a 365 dias."));
        REGRAS.put("ck_condicaopagamento_primeiro", new Regra("diasPrimeiroVencimento", "Os dias até a 1ª parcela devem ser de 0 a 365."));
        REGRAS.put("ck_condicaopagamento_receber_na_emissao", new Regra("receberNaEmissao",
                "Receber na emissão só vale para 1 parcela à vista (0 dias)."));
        REGRAS.put("ck_condicao_sem_conta_padrao", new Regra("receberNaEmissao",
                "Marque uma conta ativa como padrão desta forma antes de receber na emissão."));
        // Configuracao (F1-H4)
        REGRAS.put("ux_configuracaofinanceira_empresa", new Regra("empresa", "A empresa já tem configuração financeira."));
        REGRAS.put("ck_configuracaofinanceira_multa", new Regra("multaPercentual", "A multa deve ser de 0 a 999,99%."));
        REGRAS.put("ck_configuracaofinanceira_juros", new Regra("jurosMensalPercentual", "Os juros ao mês devem ser de 0 a 999,99%."));
        REGRAS.put("ck_configuracaofinanceira_carencia", new Regra("carenciaDias", "A carência deve ser de 0 a 365 dias."));
        REGRAS.put("ck_configuracaofinanceira_lembrete_antes", new Regra("lembreteDiasAntes", "O lembrete antes do vencimento deve ser de 0 a 30 dias."));
        REGRAS.put("ck_configuracaofinanceira_lembrete_depois", new Regra("lembreteDiasDepois",
                "Informe até 5 números de 1 ou 2 dígitos separados por vírgula (ex.: 1,7,15)."));
        REGRAS.put("ck_configuracaofinanceira_vencimento", new Regra("vencimentoPadraoDias", "O vencimento padrão não pode ser negativo."));
        REGRAS.put("ck_configuracaofinanceira_forma", new Regra("formaPrevistaPadraoReceber", "Escolha uma forma de pagamento válida."));
        REGRAS.put("ck_configuracao_categoria_receita", new Regra("categoriaPadraoReceita",
                "Escolha uma categoria de receita ativa e sem filhas."));
        REGRAS.put("ck_configuracao_categoria_despesa", new Regra("categoriaPadraoDespesa",
                "Escolha uma categoria de despesa ativa e sem filhas."));
        // Livro de movimentos (Fase 3)
        REGRAS.put("ck_conta_com_movimentos", new Regra("saldoInicial",
                "A conta já tem movimentos: o saldo inicial e a data de abertura não mudam."));
        REGRAS.put("ux_movimento_estorno", new Regra("id", "Este movimento já foi estornado."));
        REGRAS.put("ck_movimento_imutavel", new Regra("id", "O livro de movimentos não se altera: corrija com um estorno."));
        // Banco (F1-H5)
        REGRAS.put("ux_banco_codigo", new Regra("codigo", "Já existe um banco com este código."));
        REGRAS.put("ck_banco_codigo", new Regra("codigo", "O código do banco tem até 4 dígitos, sem zeros à esquerda (ex.: 1, 341)."));
        REGRAS.put("ck_banco_url", new Regra("urlPassoAPasso", "O link do passo a passo precisa começar com https://."));
    }

    /** Nomes do mais longo ao mais curto: um nome que contem outro casa primeiro. */
    private static final List<String> NOMES = REGRAS.keySet().stream()
            .sorted(Comparator.comparingInt(String::length).reversed()).toList();

    private RestricoesFinanceiro() {
    }

    /** A ApiException da regra violada, ou null se o erro nao e uma restricao conhecida do financeiro. */
    public static ApiException traduzir(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null
                    && List.of("23505", "23514", "23503").contains(sql.getSQLState()) && sql.getMessage() != null) {
                String msg = sql.getMessage();
                for (String nome : NOMES) {
                    if (msg.contains(nome)) {
                        Regra r = REGRAS.get(nome);
                        Map<String, String> campo = new LinkedHashMap<>();
                        campo.put("campo", r.campo());
                        campo.put("mensagem", r.mensagem());
                        // duplicado (indice unico) segue o padrao do projeto: 409 conflito; as demais regras, 400
                        boolean duplicado = "23505".equals(sql.getSQLState());
                        return new ApiException(duplicado ? 409 : 400, duplicado ? "conflito" : "requisicao_invalida", r.mensagem(),
                                Map.of("campos", List.of(campo)));
                    }
                }
            }
        }
        return null;
    }
}
