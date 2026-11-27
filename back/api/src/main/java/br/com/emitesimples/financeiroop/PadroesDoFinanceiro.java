package br.com.emitesimples.financeiroop;

import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Padroes iniciais do financeiro (docs/financeiro/README.md, F1-H6; referencia §4.6). IDEMPOTENTE: pode ser chamado sempre que a
 * empresa abre o modulo. Garante a linha de configuracao (travada com FOR UPDATE durante o resto) e cria cada grupo SO se a empresa
 * ainda nao tem nenhum registro dele. A empresa e SEMPRE a da sessao; a RLS do banco vale por cima.
 *
 * <p>Grava por SQL nativo (nao depende das entidades geradas). As triggers e CHECK da V14 continuam valendo.
 */
@ApplicationScoped
public class PadroesDoFinanceiro {

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    /** O que foi criado agora (vazio se a empresa ja tinha tudo) e o id da configuracao. */
    public record Resultado(String configuracaoId, List<String> criados) {
    }

    public Resultado garantir() {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> garantir(empresa));
    }

    private Resultado garantir(String empresa) {
        List<String> criados = new ArrayList<>();

        int novaConfig = em.createNativeQuery("insert into configuracaofinanceira (id, empresa_id, data_criacao, data_atualizacao, version) "
                + "values (:id, :e, localtimestamp, localtimestamp, localtimestamp) on conflict (empresa_id) do nothing")
                .setParameter("id", UUID.randomUUID().toString()).setParameter("e", empresa).executeUpdate();
        if (novaConfig > 0) {
            criados.add("configuracao");
        }
        Object[] config = (Object[]) em.createNativeQuery(
                "select id, padroes_criados_em from configuracaofinanceira where empresa_id = :e for update")
                .setParameter("e", empresa).getSingleResult();
        String configId = (String) config[0];

        if (contar("contafinanceira", empresa, null) == 0) {
            em.createNativeQuery("insert into contafinanceira (id, empresa_id, nome, tipo, ativo, saldo_inicial, data_saldo_inicial, "
                    + "padrao_dinheiro, padrao_pix, padrao_boleto, padrao_cartao, padrao_transferencia, data_criacao, data_atualizacao, version) "
                    + "values (:id, :e, 'Caixa', 'CAIXA', true, 0, :hoje, true, false, false, false, false, localtimestamp, localtimestamp, localtimestamp)")
                    .setParameter("id", UUID.randomUUID().toString()).setParameter("e", empresa)
                    .setParameter("hoje", LocalDate.now(ContaFinanceiraListener.FUSO)).executeUpdate();
            criados.add("conta");
        }
        if (contar("categoriafinanceira", empresa, "RECEITA") == 0) {
            for (String nome : List.of("Prestação de serviços", "Outras receitas")) {
                categoria(empresa, nome, "RECEITA");
            }
            criados.add("categorias de receita");
        }
        if (contar("categoriafinanceira", empresa, "DESPESA") == 0) {
            for (String nome : List.of("Aluguel", "Tarifas", "Impostos", "Folha de pagamento", "Fornecedores", "Outras despesas")) {
                categoria(empresa, nome, "DESPESA");
            }
            criados.add("categorias de despesa");
        }
        if (contar("condicaopagamento", empresa, null) == 0) {
            // "receber na emissao" exige conta padrao ativa de dinheiro (trigger da V14): sem ela, a condicao nasce sem a baixa automatica
            boolean temContaDinheiro = ((Number) em.createNativeQuery(
                    "select count(*) from contafinanceira where empresa_id = :e and ativo and padrao_dinheiro")
                    .setParameter("e", empresa).getSingleResult()).longValue() > 0;
            condicao(empresa, "Dinheiro à vista", "DINHEIRO", 1, 0, 0, temContaDinheiro);
            condicao(empresa, "Pix à vista", "PIX", 1, 0, 0, false);
            condicao(empresa, "Boleto 1x (30 dias)", "BOLETO", 1, 30, 30, false);
            condicao(empresa, "Boleto 2x (30/60)", "BOLETO", 2, 30, 30, false);
            condicao(empresa, "Boleto 3x (30/60/90)", "BOLETO", 3, 30, 30, false);
            criados.add("condicoes de pagamento");
        }
        if (config[1] == null) {
            em.createNativeQuery("update configuracaofinanceira set padroes_criados_em = localtimestamp, data_atualizacao = localtimestamp "
                    + "where id = :id").setParameter("id", configId).executeUpdate();
        }
        return new Resultado(configId, criados);
    }

    private long contar(String tabela, String empresa, String tipo) {
        // tabela e tipo vem so deste arquivo (constantes), nunca da requisicao
        String sql = "select count(*) from " + tabela + " where empresa_id = :e" + (tipo == null ? "" : " and tipo = :t");
        var q = em.createNativeQuery(sql).setParameter("e", empresa);
        if (tipo != null) {
            q.setParameter("t", tipo);
        }
        return ((Number) q.getSingleResult()).longValue();
    }

    private void categoria(String empresa, String nome, String tipo) {
        em.createNativeQuery("insert into categoriafinanceira (id, empresa_id, nome, tipo, ativo, data_criacao, data_atualizacao, version) "
                + "values (:id, :e, :n, :t, true, localtimestamp, localtimestamp, localtimestamp)")
                .setParameter("id", UUID.randomUUID().toString()).setParameter("e", empresa)
                .setParameter("n", nome).setParameter("t", tipo).executeUpdate();
    }

    private void condicao(String empresa, String nome, String forma, int parcelas, int primeiro, int intervalo, boolean receberNaEmissao) {
        em.createNativeQuery("insert into condicaopagamento (id, empresa_id, nome, forma, ativo, parcelas, dias_primeiro_vencimento, "
                + "intervalo_dias, receber_na_emissao, data_criacao, data_atualizacao, version) "
                + "values (:id, :e, :n, :f, true, :p, :d, :i, :r, localtimestamp, localtimestamp, localtimestamp)")
                .setParameter("id", UUID.randomUUID().toString()).setParameter("e", empresa).setParameter("n", nome)
                .setParameter("f", forma).setParameter("p", parcelas).setParameter("d", primeiro).setParameter("i", intervalo)
                .setParameter("r", receberNaEmissao).executeUpdate();
    }

    /** Para respostas: mapa simples. */
    static Map<String, Object> comoMapa(Resultado r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configuracaoId", r.configuracaoId());
        m.put("criados", r.criados());
        return m;
    }
}
