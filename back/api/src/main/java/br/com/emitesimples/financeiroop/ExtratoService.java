package br.com.emitesimples.financeiroop;

import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Leitura do livro (docs/financeiro/README.md, F3-H7; referencia §7): extrato com saldo corrente calculado NO BANCO (SUM() OVER), totais por
 * forma, saldo das contas e fluxo de caixa por mes (C1). So le; quem grava e o LivroDeMovimentos. Saldo nunca e guardado: saldo inicial mais
 * a soma do livro. A empresa e sempre a da sessao (filtro explicito e RLS).
 */
@ApplicationScoped
public class ExtratoService {

    public static final int POR_PAGINA = 15;
    static final Map<String, String> ROTULO_FORMA = Map.of("DINHEIRO", "Dinheiro", "PIX", "Pix", "BOLETO", "Boleto", "CARTAO", "Cartão",
            "TRANSFERENCIA", "Transferência", "TRANSFERENCIAS", "Transferências entre contas", "SEM_FORMA", "Sem forma informada");

    public record Linha(String id, LocalDate data, String tipo, String sentido, String descricao, String forma, BigDecimal valor,
            BigDecimal saldoCorrente, String idTitulo, String idEstornoDe, boolean estornado) {
    }

    public record TotalDaForma(String forma, String rotulo, BigDecimal entradas, BigDecimal saidas) {
    }

    public record Extrato(String idConta, String nomeConta, LocalDate de, LocalDate ate, BigDecimal saldoAnterior, BigDecimal saldoFinal,
            List<Linha> linhas, long total, int pagina, List<TotalDaForma> totaisPorForma) {
    }

    public record SaldoDaConta(String idConta, String nome, String tipo, BigDecimal saldo) {
    }

    public record Saldos(List<SaldoDaConta> contas, BigDecimal total) {
    }

    public record MesDoFluxo(String mes, BigDecimal entradasPrevistas, BigDecimal saidasPrevistas, BigDecimal entradasRealizadas,
            BigDecimal saidasRealizadas, BigDecimal saldoPrevisto, BigDecimal saldoRealizado) {
    }

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    public Extrato extrato(String idConta, LocalDate de, LocalDate ate, Integer pagina) {
        String empresa = escopo.exigir("empresa_id");
        int p = pagina == null ? 1 : pagina;
        if (p < 1) {
            throw invalido("pagina", "A página começa em 1.");
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            ContaFinanceiraEntity c = idConta == null || idConta.length() > 36 ? null : em.find(ContaFinanceiraEntity.class, idConta);
            if (c == null || c.getEmpresa() == null || !empresa.equals(c.getEmpresa().getId())) {
                throw invalido("idConta", "Conta não encontrada.");
            }
            LocalDate inicio = de == null || de.isBefore(c.getDataSaldoInicial()) ? c.getDataSaldoInicial() : de;
            LocalDate fim = ate == null ? TitulosService.hoje() : ate;
            if (fim.isBefore(inicio)) {
                throw invalido("ate", "A data final não pode ser antes da inicial.");
            }
            BigDecimal anterior = Dinheiro.normalizar(c.getSaldoInicial()).add(soma(empresa, c.getId(), inicio.minusDays(1)));
            // saldo corrente pela janela NO BANCO, sobre o periodo inteiro; a paginacao e feita por fora, para o saldo nao depender da pagina
            @SuppressWarnings("unchecked")
            List<Object[]> rs = em.createNativeQuery("""
                    select * from (
                      select m.id, m.data, m.tipo, m.sentido, m.descricao, m.forma, m.valor, m.titulo_id, m.id_estorno_de,
                             cast(:anterior as numeric) + sum(case when m.sentido = 'ENTRADA' then m.valor else -m.valor end)
                                 over (order by m.data, m.data_criacao, m.id) as saldo_corrente,
                             exists (select 1 from movimento e where e.empresa_id = m.empresa_id and e.id_estorno_de = m.id) as estornado,
                             row_number() over (order by m.data, m.data_criacao, m.id) as ordem
                      from movimento m
                      where m.empresa_id = :e and m.conta_id = :c and m.data between :de and :ate
                    ) x order by x.ordem offset :off limit :lim""")
                    .setParameter("anterior", anterior).setParameter("e", empresa).setParameter("c", c.getId())
                    .setParameter("de", inicio).setParameter("ate", fim)
                    .setParameter("off", (p - 1) * POR_PAGINA).setParameter("lim", POR_PAGINA).getResultList();
            List<Linha> linhas = new ArrayList<>();
            for (Object[] r : rs) {
                linhas.add(new Linha((String) r[0], data(r[1]), (String) r[2], (String) r[3], (String) r[4], (String) r[5],
                        Dinheiro.normalizar((BigDecimal) r[6]), Dinheiro.normalizar((BigDecimal) r[9]), (String) r[7], (String) r[8],
                        Boolean.TRUE.equals(r[10])));
            }
            long total = ((Number) em.createNativeQuery("select count(*) from movimento where empresa_id = :e and conta_id = :c "
                    + "and data between :de and :ate").setParameter("e", empresa).setParameter("c", c.getId())
                    .setParameter("de", inicio).setParameter("ate", fim).getSingleResult()).longValue();
            BigDecimal saldoFinal = Dinheiro.normalizar(c.getSaldoInicial()).add(soma(empresa, c.getId(), fim));
            return new Extrato(c.getId(), c.getNome(), inicio, fim, anterior, saldoFinal, linhas, total, p,
                    totaisPorForma(empresa, c.getId(), inicio, fim));
        });
    }

    private List<TotalDaForma> totaisPorForma(String empresa, String conta, LocalDate de, LocalDate ate) {
        @SuppressWarnings("unchecked")
        List<Object[]> rs = em.createNativeQuery("""
                select case when id_transferencia is not null then 'TRANSFERENCIAS' when forma is null then 'SEM_FORMA' else forma end as grupo,
                       coalesce(sum(case when sentido = 'ENTRADA' then valor end), 0), coalesce(sum(case when sentido = 'SAIDA' then valor end), 0)
                from movimento where empresa_id = :e and conta_id = :c and data between :de and :ate
                group by 1 order by 1""")
                .setParameter("e", empresa).setParameter("c", conta).setParameter("de", de).setParameter("ate", ate).getResultList();
        return rs.stream().map(r -> new TotalDaForma((String) r[0], ROTULO_FORMA.get((String) r[0]), Dinheiro.normalizar((BigDecimal) r[1]),
                Dinheiro.normalizar((BigDecimal) r[2]))).toList();
    }

    public Saldos saldos() {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> {
            @SuppressWarnings("unchecked")
            List<Object[]> rs = em.createNativeQuery("""
                    select c.id, c.nome, c.tipo, c.saldo_inicial + coalesce((select sum(case when m.sentido = 'ENTRADA' then m.valor else -m.valor end)
                           from movimento m where m.empresa_id = c.empresa_id and m.conta_id = c.id and m.data <= :hoje), 0)
                    from contafinanceira c where c.empresa_id = :e and c.ativo order by c.nome""")
                    .setParameter("e", empresa).setParameter("hoje", TitulosService.hoje()).getResultList();
            List<SaldoDaConta> contas = rs.stream().map(r -> new SaldoDaConta((String) r[0], (String) r[1], (String) r[2],
                    Dinheiro.normalizar((BigDecimal) r[3]))).toList();
            return new Saldos(contas, contas.stream().map(SaldoDaConta::saldo).reduce(Dinheiro.ZERO, BigDecimal::add));
        });
    }

    public List<MesDoFluxo> fluxoDeCaixa(LocalDate de, LocalDate ate) {
        String empresa = escopo.exigir("empresa_id");
        if (de == null || ate == null) {
            throw invalido(de == null ? "de" : "ate", "Informe o período (de e até).");
        }
        if (ate.isBefore(de)) {
            throw invalido("ate", "A data final não pode ser antes da inicial.");
        }
        if (ChronoUnit.MONTHS.between(YearMonth.from(de), YearMonth.from(ate)) >= 24) {
            throw invalido("ate", "O período tem no máximo 24 meses.");
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            Map<String, BigDecimal[]> meses = new TreeMap<>();
            for (YearMonth m = YearMonth.from(de); !m.isAfter(YearMonth.from(ate)); m = m.plusMonths(1)) {
                meses.put(m.toString(), new BigDecimal[] {Dinheiro.ZERO, Dinheiro.ZERO, Dinheiro.ZERO, Dinheiro.ZERO});
            }
            @SuppressWarnings("unchecked")
            List<Object[]> previsto = em.createNativeQuery("""
                    select to_char(vencimento, 'YYYY-MM'), coalesce(sum(case when tipo = 'RECEBER' then valor end), 0),
                           coalesce(sum(case when tipo = 'PAGAR' then valor end), 0)
                    from titulo where empresa_id = :e and situacao <> 'CANCELADO' and vencimento between :de and :ate group by 1""")
                    .setParameter("e", empresa).setParameter("de", de).setParameter("ate", ate).getResultList();
            for (Object[] r : previsto) {
                BigDecimal[] v = meses.get((String) r[0]);
                v[0] = v[0].add((BigDecimal) r[1]);
                v[1] = v[1].add((BigDecimal) r[2]);
            }
            @SuppressWarnings("unchecked")
            List<Object[]> realizado = em.createNativeQuery("""
                    select to_char(data, 'YYYY-MM'), coalesce(sum(case when sentido = 'ENTRADA' then valor end), 0),
                           coalesce(sum(case when sentido = 'SAIDA' then valor end), 0)
                    from movimento where empresa_id = :e and id_transferencia is null and data between :de and :ate group by 1""")
                    .setParameter("e", empresa).setParameter("de", de).setParameter("ate", ate).getResultList();
            for (Object[] r : realizado) {
                BigDecimal[] v = meses.get((String) r[0]);
                v[2] = v[2].add((BigDecimal) r[1]);
                v[3] = v[3].add((BigDecimal) r[2]);
            }
            List<MesDoFluxo> r = new ArrayList<>();
            meses.forEach((mes, v) -> r.add(new MesDoFluxo(mes, Dinheiro.normalizar(v[0]), Dinheiro.normalizar(v[1]), Dinheiro.normalizar(v[2]),
                    Dinheiro.normalizar(v[3]), Dinheiro.normalizar(v[0].subtract(v[1])), Dinheiro.normalizar(v[2].subtract(v[3])))));
            return r;
        });
    }

    private BigDecimal soma(String empresa, String conta, LocalDate ate) {
        Object v = em.createNativeQuery("select coalesce(sum(case when sentido = 'ENTRADA' then valor else -valor end), 0) from movimento "
                + "where empresa_id = :e and conta_id = :c and data <= :ate").setParameter("e", empresa).setParameter("c", conta)
                .setParameter("ate", ate).getSingleResult();
        return Dinheiro.normalizar((BigDecimal) v);
    }

    private static LocalDate data(Object o) {
        return o instanceof LocalDate d ? d : o instanceof Date d ? d.toLocalDate() : LocalDate.parse(String.valueOf(o));
    }

    private static ApiException invalido(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return new ApiException(400, "requisicao_invalida", mensagem, Map.of("campos", List.of(m)));
    }
}
