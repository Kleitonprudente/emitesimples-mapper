package br.com.emitesimples.financeiroop;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.MovimentoEntity;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * O LIVRO DE MOVIMENTOS (docs/financeiro/README.md, secao 4; referencia §6). E o UNICO que grava em MOVIMENTO, e so INSERT: o banco recusa
 * UPDATE e DELETE (C3), e erro se corrige com estorno. Tambem e o unico que muda {@code quitado} e a situacao do titulo pela baixa.
 * Toda operacao numa transacao; titulo travado com FOR UPDATE; estorno serializado por advisory lock (a linha original nao pode ser
 * travada com FOR UPDATE, pois a aplicacao nao tem UPDATE no livro). A empresa e SEMPRE a da sessao; a RLS vale por cima.
 */
@ApplicationScoped
public class LivroDeMovimentos {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> SENTIDOS = Set.of("ENTRADA", "SAIDA");

    public record Baixa(String idConta, LocalDate data, String forma, BigDecimal quitado, BigDecimal juros, BigDecimal multa,
            BigDecimal desconto, String observacao) {
    }

    public record BaixaEmLote(List<String> idsTitulos, String idConta, LocalDate data, String forma) {
    }

    public record Avulso(String idConta, String sentido, BigDecimal valor, LocalDate data, String forma, String idCategoria, String descricao) {
    }

    public record Transferencia(String idContaOrigem, String idContaDestino, BigDecimal valor, LocalDate data, String descricao) {
    }

    public record MovimentoDto(String id, String tipo, String sentido, LocalDate data, String descricao, String forma, BigDecimal valor,
            BigDecimal quitado, BigDecimal juros, BigDecimal multa, BigDecimal desconto, String idConta, String idTitulo, String idCategoria,
            String idLote, String idTransferencia, String idEstornoDe) {
    }

    public record ResultadoDaBaixa(MovimentoDto movimento, TitulosService.TituloDto titulo) {
    }

    /** Linha do livro de um titulo (baixa ou estorno), com {@code estornado} quando ja tem estorno. */
    public record LinhaDoTitulo(MovimentoDto movimento, boolean estornado) {
    }

    public record SugestaoDeBaixa(long diasAtraso, BigDecimal quitado, BigDecimal multa, BigDecimal juros, BigDecimal total) {
    }

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    @Inject
    JsonWebToken jwt;

    @Inject
    CobrancasService cobrancas;

    // ------------------------------------------------------------------------------------------------------------ F3-H2

    public SugestaoDeBaixa sugestao(String idTitulo, LocalDate data, BigDecimal quitado) {
        String empresa = escopo.exigir("empresa_id");
        LocalDate dia = data == null ? TitulosService.hoje() : data;
        return QuarkusTransaction.requiringNew().call(() -> {
            TituloEntity t = titulo(empresa, idTitulo, false);
            BigDecimal saldo = saldo(t);
            BigDecimal q = quitado == null ? saldo : Dinheiro.normalizar(quitado);
            CalculadoraDeAcrescimos.Sugestao s = sugerir(empresa, t, q, dia);
            return new SugestaoDeBaixa(s.diasAtraso(), q, s.multa(), s.juros(), s.total(q));
        });
    }

    /** As linhas do livro do titulo, em ordem de gravacao (docs/financeiro/README.md, acrescimos pedidos pelas telas). */
    public List<LinhaDoTitulo> baixasDoTitulo(String idTitulo) {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> {
            TituloEntity t = titulo(empresa, idTitulo, false);
            List<MovimentoEntity> linhas = em.createQuery("select m from MovimentoEntity m where m.empresa.id = :e and m.titulo.id = :t "
                    + "order by m.dataCriacao, m.id", MovimentoEntity.class).setParameter("e", empresa).setParameter("t", t.getId()).getResultList();
            Set<String> estornados = new java.util.HashSet<>();
            linhas.forEach(m -> {
                if (m.getIdEstornoDe() != null) {
                    estornados.add(m.getIdEstornoDe());
                }
            });
            return linhas.stream().map(m -> new LinhaDoTitulo(dto(m), estornados.contains(m.getId()))).toList();
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F3-H1

    public ResultadoDaBaixa baixar(String idTitulo, Baixa b) {
        String empresa = escopo.exigir("empresa_id");
        if (b == null) {
            throw invalido("corpo", "Informe os dados da baixa.");
        }
        List<Map<String, String>> erros = new ArrayList<>();
        exigirForma(erros, b.forma());
        if (b.data() == null) {
            erros.add(erro("data", "Informe a data."));
        }
        if (b.quitado() == null || b.quitado().compareTo(new BigDecimal("0.01")) < 0) {
            erros.add(erro("quitado", "Informe o valor quitado (a partir de R$ 0,01)."));
        }
        for (var p : List.of(Map.entry("juros", nz(b.juros())), Map.entry("multa", nz(b.multa())), Map.entry("desconto", nz(b.desconto())),
                Map.entry("quitado", nz(b.quitado())))) {
            if (p.getValue().signum() < 0 || p.getValue().stripTrailingZeros().scale() > 2 || p.getValue().compareTo(Dinheiro.TETO) > 0) {
                erros.add(erro(p.getKey(), "Valor inválido: use de R$ 0,00 a R$ 999.999.999,99, com até 2 casas decimais."));
            }
        }
        if (b.observacao() != null && b.observacao().length() > 255) {
            erros.add(erro("observacao", "A observação tem no máximo 255 caracteres."));
        }
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            TituloEntity t = titulo(empresa, idTitulo, true);
            ContaFinanceiraEntity conta = conta(empresa, b.idConta(), "idConta", b.data());
            MovimentoEntity m = gravarBaixa(t, conta, b.data(), b.forma(), Dinheiro.normalizar(b.quitado()), Dinheiro.normalizar(b.juros()),
                    Dinheiro.normalizar(b.multa()), Dinheiro.normalizar(b.desconto()), b.observacao(), null, jwt.getSubject());
            em.flush();
            return new ResultadoDaBaixa(dto(m), TitulosService.dto(t));
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F3-H3

    public List<ResultadoDaBaixa> baixarEmLote(BaixaEmLote l) {
        String empresa = escopo.exigir("empresa_id");
        if (l == null) {
            throw invalido("corpo", "Informe os dados da baixa.");
        }
        List<Map<String, String>> erros = new ArrayList<>();
        Set<String> ids = l.idsTitulos() == null ? Set.of() : new LinkedHashSet<>(l.idsTitulos());
        if (ids.isEmpty() || ids.size() > 100) {
            erros.add(erro("idsTitulos", "Escolha de 1 a 100 títulos."));
        }
        exigirForma(erros, l.forma());
        if (l.data() == null) {
            erros.add(erro("data", "Informe a data."));
        }
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            ContaFinanceiraEntity conta = conta(empresa, l.idConta(), "idConta", l.data());
            String lote = UUID.randomUUID().toString();
            List<ResultadoDaBaixa> r = new ArrayList<>();
            // trava em ordem de id: dois lotes com os mesmos titulos nao se travam em cruz
            for (String id : ids.stream().sorted().toList()) {
                TituloEntity t = titulo(empresa, id, true);
                try {
                    BigDecimal saldo = saldo(t);
                    CalculadoraDeAcrescimos.Sugestao s = sugerir(empresa, t, saldo, l.data());
                    MovimentoEntity m = gravarBaixa(t, conta, l.data(), l.forma(), saldo, s.juros(), s.multa(), Dinheiro.ZERO, null, lote,
                            jwt.getSubject());
                    r.add(new ResultadoDaBaixa(dto(m), TitulosService.dto(t)));
                } catch (ApiException e) {
                    throw new ApiException(e.getStatus(), e.getCodigo(), "Título \"" + t.getDescricao() + "\" (" + t.getParcela() + "/"
                            + t.getTotalParcelas() + "): " + e.getMessage(), e.getDetalhes());
                }
            }
            em.flush();
            return r;
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F3-H4

    public List<MovimentoDto> estornar(String idMovimento) {
        String empresa = escopo.exigir("empresa_id");
        String usuario = jwt.getSubject();
        return QuarkusTransaction.requiringNew().call(() -> {
            MovimentoEntity original = movimento(empresa, idMovimento);
            if ("ESTORNO".equals(original.getTipo())) {
                throw conflito("Um estorno não se estorna: lance a operação de novo.");
            }
            // transferencia estorna as duas pernas: a trava e a conferencia sao pela transferencia inteira
            String chave = original.getIdTransferencia() != null ? original.getIdTransferencia() : original.getId();
            em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:k, 0))").setParameter("k", "estorno:" + chave).getSingleResult();
            List<MovimentoEntity> pernas = original.getIdTransferencia() == null ? List.of(original)
                    : em.createQuery("select m from MovimentoEntity m where m.empresa.id = :e and m.idTransferencia = :t and m.tipo <> 'ESTORNO' "
                            + "order by m.sentido desc", MovimentoEntity.class)
                            .setParameter("e", empresa).setParameter("t", original.getIdTransferencia()).getResultList();
            for (MovimentoEntity p : pernas) {
                long ja = em.createQuery("select count(m) from MovimentoEntity m where m.empresa.id = :e and m.idEstornoDe = :id", Long.class)
                        .setParameter("e", empresa).setParameter("id", p.getId()).getSingleResult();
                if (ja > 0) {
                    throw conflito("Este movimento já foi estornado.");
                }
            }
            var agora = AuthService.agora();
            List<MovimentoDto> r = new ArrayList<>();
            for (MovimentoEntity p : pernas) {
                ContaFinanceiraEntity conta = p.getConta();
                LocalDate hoje = TitulosService.hoje();
                LocalDate data = hoje.isBefore(conta.getDataSaldoInicial()) ? conta.getDataSaldoInicial() : hoje;
                MovimentoEntity e = novo(p.getEmpresa(), conta, "ESTORNO", "ENTRADA".equals(p.getSentido()) ? "SAIDA" : "ENTRADA", data,
                        cortar("Estorno: " + (p.getDescricao() == null ? "" : p.getDescricao())), p.getForma(), p.getValor(), usuario, agora);
                e.setTitulo(p.getTitulo());
                e.setCategoria(p.getCategoria());
                e.setQuitado(p.getQuitado());
                e.setJuros(p.getJuros());
                e.setMulta(p.getMulta());
                e.setDesconto(p.getDesconto());
                e.setIdLote(p.getIdLote());
                e.setIdTransferencia(p.getIdTransferencia());
                e.setIdEstornoDe(p.getId());
                em.persist(e);
                if (p.getTitulo() != null) {
                    TituloEntity t = em.find(TituloEntity.class, p.getTitulo().getId(), LockModeType.PESSIMISTIC_WRITE);
                    t.setQuitado(Dinheiro.normalizar(t.getQuitado()).subtract(Dinheiro.normalizar(p.getQuitado())));
                    if (!"CANCELADO".equals(t.getSituacao())) {
                        t.setSituacao(t.getQuitado().signum() == 0 ? "ABERTO" : "PARCIAL");
                    }
                    t.setDataAtualizacao(agora);
                    cobrancas.cancelarVivas(t.getId()); // Fase 7 (README 9.5): a baixa estornada cancela as cobrancas vivas do titulo
                }
                r.add(dto(e));
            }
            em.flush();
            return r;
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F3-H5

    public MovimentoDto lancarAvulso(Avulso a) {
        String empresa = escopo.exigir("empresa_id");
        String usuario = jwt.getSubject();
        if (a == null) {
            throw invalido("corpo", "Informe os dados do lançamento.");
        }
        List<Map<String, String>> erros = new ArrayList<>();
        if (a.sentido() == null || !SENTIDOS.contains(a.sentido())) {
            erros.add(erro("sentido", "Escolha entrada ou saída."));
        }
        validarValor(erros, a.valor());
        if (a.data() == null) {
            erros.add(erro("data", "Informe a data."));
        }
        if (a.forma() != null && !a.forma().isBlank() && !TitulosService.FORMAS.contains(a.forma())) {
            erros.add(erro("forma", "Escolha uma forma de pagamento válida."));
        }
        if (a.descricao() != null && a.descricao().length() > 255) {
            erros.add(erro("descricao", "A descrição tem no máximo 255 caracteres."));
        }
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            ContaFinanceiraEntity conta = conta(empresa, a.idConta(), "idConta", a.data());
            CategoriaFinanceiraEntity cat = categoriaFolha(empresa, a.idCategoria(), "ENTRADA".equals(a.sentido()) ? "RECEITA" : "DESPESA");
            MovimentoEntity m = novo(conta.getEmpresa(), conta, "ENTRADA".equals(a.sentido()) ? "ENTRADA_AVULSA" : "SAIDA_AVULSA", a.sentido(),
                    a.data(), vazioParaNulo(a.descricao()), vazioParaNulo(a.forma()), Dinheiro.normalizar(a.valor()), usuario, AuthService.agora());
            m.setCategoria(cat);
            em.persist(m);
            em.flush();
            return dto(m);
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F3-H6

    public List<MovimentoDto> transferir(Transferencia t) {
        String empresa = escopo.exigir("empresa_id");
        String usuario = jwt.getSubject();
        if (t == null) {
            throw invalido("corpo", "Informe os dados da transferência.");
        }
        List<Map<String, String>> erros = new ArrayList<>();
        validarValor(erros, t.valor());
        if (t.data() == null) {
            erros.add(erro("data", "Informe a data."));
        }
        if (t.idContaOrigem() != null && t.idContaOrigem().equals(t.idContaDestino())) {
            erros.add(erro("idContaDestino", "A conta de destino precisa ser diferente da de origem."));
        }
        if (t.descricao() != null && t.descricao().length() > 255) {
            erros.add(erro("descricao", "A descrição tem no máximo 255 caracteres."));
        }
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            ContaFinanceiraEntity origem = conta(empresa, t.idContaOrigem(), "idContaOrigem", t.data());
            ContaFinanceiraEntity destino = conta(empresa, t.idContaDestino(), "idContaDestino", t.data());
            String id = UUID.randomUUID().toString();
            var agora = AuthService.agora();
            BigDecimal valor = Dinheiro.normalizar(t.valor());
            String desc = vazioParaNulo(t.descricao());
            MovimentoEntity saida = novo(origem.getEmpresa(), origem, "TRANSFERENCIA_SAIDA", "SAIDA", t.data(),
                    desc == null ? cortar("Transferência para " + destino.getNome()) : desc, null, valor, usuario, agora);
            MovimentoEntity entrada = novo(destino.getEmpresa(), destino, "TRANSFERENCIA_ENTRADA", "ENTRADA", t.data(),
                    desc == null ? cortar("Transferência de " + origem.getNome()) : desc, null, valor, usuario, agora);
            saida.setIdTransferencia(id);
            entrada.setIdTransferencia(id);
            em.persist(saida);
            em.persist(entrada);
            em.flush();
            return List.of(dto(saida), dto(entrada));
        });
    }

    // ------------------------------------------------------------------------------------------------------------ apoio

    /**
     * "Receber na emissao" (docs/financeiro/README.md, 9.1, E1): baixa do saldo inteiro, sem acrescimos, na transacao de quem chama (o
     * titulo acabou de nascer). Sem sessao de usuario: {@code usuario} e quem emitiu a nota. Recusa como a baixa da tela (conta inativa,
     * data antes da abertura...).
     */
    public MovimentoEntity baixarNaEmissao(TituloEntity t, ContaFinanceiraEntity conta, LocalDate data, String forma, String usuario) {
        conta(t.getEmpresa().getId(), conta.getId(), "idConta", data);
        return gravarBaixa(t, conta, data, forma, saldo(t), Dinheiro.ZERO, Dinheiro.ZERO, Dinheiro.ZERO, null, null, usuario);
    }

    /**
     * Baixa pelo gateway (docs/financeiro/README.md, 9.5, Fase 7): o pagamento confirmado pelo provedor quita o saldo e o que passou dele
     * vira juros. Sem sessao (sincronizador): sem usuario. Data = a do pagamento, ou a abertura da conta se o pagamento for anterior.
     */
    public MovimentoEntity baixarPeloGateway(TituloEntity t, ContaFinanceiraEntity conta, LocalDate pagoEm, String forma, BigDecimal quitado,
            BigDecimal juros) {
        LocalDate data = pagoEm.isBefore(conta.getDataSaldoInicial()) ? conta.getDataSaldoInicial() : pagoEm;
        data = data.isAfter(TitulosService.hoje()) ? TitulosService.hoje() : data;
        conta(t.getEmpresa().getId(), conta.getId(), "idConta", data);
        return gravarBaixa(t, conta, data, forma, quitado, juros, Dinheiro.ZERO, Dinheiro.ZERO, "Pagamento pelo gateway", null, null);
    }

    /** Tarifa do provedor como saida avulsa na categoria "Tarifas" (se existir como folha ativa de despesa). Sem usuario. */
    public MovimentoEntity tarifaDoGateway(ContaFinanceiraEntity conta, LocalDate data, BigDecimal valor, String descricao) {
        LocalDate d = data.isBefore(conta.getDataSaldoInicial()) ? conta.getDataSaldoInicial() : data;
        d = d.isAfter(TitulosService.hoje()) ? TitulosService.hoje() : d;
        List<CategoriaFinanceiraEntity> tarifas = em.createQuery("select c from CategoriaFinanceiraEntity c where c.empresa.id = :e "
                + "and c.tipo = 'DESPESA' and c.ativo = true and c.nome = 'Tarifas' and not exists (select f from CategoriaFinanceiraEntity f "
                + "where f.categoriaPai = c)", CategoriaFinanceiraEntity.class).setParameter("e", conta.getEmpresa().getId()).getResultList();
        MovimentoEntity m = novo(conta.getEmpresa(), conta, "SAIDA_AVULSA", "SAIDA", d, cortar(descricao), null, Dinheiro.normalizar(valor), null,
                AuthService.agora());
        m.setCategoria(tarifas.isEmpty() ? null : tarifas.get(0));
        em.persist(m);
        return m;
    }

    /** Grava a linha de baixa e atualiza o titulo (ja travado). Regras da §6.1, passos 2 a 8. */
    private MovimentoEntity gravarBaixa(TituloEntity t, ContaFinanceiraEntity conta, LocalDate data, String forma, BigDecimal quitado,
            BigDecimal juros, BigDecimal multa, BigDecimal desconto, String observacao, String lote, String usuario) {
        if (t.getAprovadoEm() == null) {
            throw conflito("Este título está aguardando aprovação: lance-o antes de dar baixa.");
        }
        if (!Set.of("ABERTO", "PARCIAL").contains(t.getSituacao())) {
            throw conflito("Este título está " + TitulosService.ROTULO_SITUACAO.get(t.getSituacao()) + ": não aceita baixa.");
        }
        if (quitado.compareTo(saldo(t)) > 0) {
            throw invalido("quitado", "O valor quitado não pode passar do saldo do título. Juros e multa vão nos campos próprios.");
        }
        BigDecimal valor = quitado.add(juros).add(multa).subtract(desconto);
        if (valor.signum() <= 0) {
            throw invalido("desconto", "O desconto não pode zerar a baixa. Para abrir mão do título, cancele-o.");
        }
        if (valor.compareTo(Dinheiro.TETO) > 0) {
            throw invalido("quitado", "A baixa passa do limite de R$ 999.999.999,99.");
        }
        boolean receber = "RECEBER".equals(t.getTipo());
        var agora = AuthService.agora();
        String desc = vazioParaNulo(observacao) != null ? observacao.trim() : t.getDescricao();
        MovimentoEntity m = novo(t.getEmpresa(), conta, receber ? "RECEBIMENTO" : "PAGAMENTO", receber ? "ENTRADA" : "SAIDA", data, cortar(desc),
                forma, valor, usuario, agora);
        m.setTitulo(t);
        m.setCategoria(t.getCategoria());
        m.setQuitado(quitado);
        m.setJuros(juros);
        m.setMulta(multa);
        m.setDesconto(desconto);
        m.setIdLote(lote);
        em.persist(m);
        t.setQuitado(Dinheiro.normalizar(t.getQuitado()).add(quitado));
        t.setSituacao(t.getQuitado().compareTo(Dinheiro.normalizar(t.getValor())) >= 0 ? "PAGO" : "PARCIAL");
        t.setDataAtualizacao(agora);
        return m;
    }

    private CalculadoraDeAcrescimos.Sugestao sugerir(String empresa, TituloEntity t, BigDecimal quitado, LocalDate data) {
        List<Object[]> c = em.createQuery("select c.carenciaDias, c.multaPercentual, c.jurosMensalPercentual from ConfiguracaoFinanceiraEntity c "
                + "where c.empresa.id = :e", Object[].class).setParameter("e", empresa).getResultList();
        int carencia = c.isEmpty() || c.get(0)[0] == null ? 2 : ((Number) c.get(0)[0]).intValue();
        BigDecimal multa = t.getMultaPercentual() != null ? t.getMultaPercentual() : c.isEmpty() ? new BigDecimal("2.00") : (BigDecimal) c.get(0)[1];
        BigDecimal juros = t.getJurosMensalPercentual() != null ? t.getJurosMensalPercentual()
                : c.isEmpty() ? new BigDecimal("1.00") : (BigDecimal) c.get(0)[2];
        return CalculadoraDeAcrescimos.sugerir("RECEBER".equals(t.getTipo()), quitado, t.getVencimento(), data, carencia, multa, juros);
    }

    private MovimentoEntity novo(EmpresaEntity empresa, ContaFinanceiraEntity conta, String tipo, String sentido, LocalDate data, String descricao,
            String forma, BigDecimal valor, String usuario, java.time.LocalDateTime agora) {
        MovimentoEntity m = new MovimentoEntity();
        m.setEmpresa(empresa);
        m.setConta(conta);
        m.setTipo(tipo);
        m.setSentido(sentido);
        m.setData(data);
        m.setDescricao(descricao);
        m.setForma(forma);
        m.setValor(valor);
        m.setQuitado(Dinheiro.ZERO);
        m.setJuros(Dinheiro.ZERO);
        m.setMulta(Dinheiro.ZERO);
        m.setDesconto(Dinheiro.ZERO);
        m.setIdCriadoPor(usuario);
        m.setDataCriacao(agora);
        return m;
    }

    /** Conta da empresa, ativa, e a data dentro da regra: nao futura e nao antes da abertura. */
    private ContaFinanceiraEntity conta(String empresa, String id, String campo, LocalDate data) {
        ContaFinanceiraEntity c = id == null || id.isBlank() || id.length() > 36 ? null : em.find(ContaFinanceiraEntity.class, id);
        if (c == null || c.getEmpresa() == null || !empresa.equals(c.getEmpresa().getId())) {
            throw invalido(campo, "Conta não encontrada.");
        }
        if (!Boolean.TRUE.equals(c.getAtivo())) {
            throw invalido(campo, "A conta " + c.getNome() + " está inativa.");
        }
        if (data.isAfter(TitulosService.hoje())) {
            throw invalido("data", "A data não pode ser futura.");
        }
        if (data.isBefore(c.getDataSaldoInicial())) {
            throw invalido("data", "A data não pode ser antes da abertura da conta " + c.getNome() + " (" + c.getDataSaldoInicial().format(BR) + ").");
        }
        return c;
    }

    private CategoriaFinanceiraEntity categoriaFolha(String empresa, String id, String tipo) {
        if (id == null || id.isBlank()) {
            return null;
        }
        CategoriaFinanceiraEntity c = id.length() > 36 ? null : em.find(CategoriaFinanceiraEntity.class, id);
        if (c == null || c.getEmpresa() == null || !empresa.equals(c.getEmpresa().getId())) {
            throw invalido("idCategoria", "Categoria não encontrada.");
        }
        if (!tipo.equals(c.getTipo())) {
            throw invalido("idCategoria", "RECEITA".equals(tipo) ? "Entrada usa categoria de receita." : "Saída usa categoria de despesa.");
        }
        if (!Boolean.TRUE.equals(c.getAtivo())) {
            throw invalido("idCategoria", "Esta categoria está inativa.");
        }
        if (em.createQuery("select count(x) from CategoriaFinanceiraEntity x where x.categoriaPai.id = :id", Long.class)
                .setParameter("id", c.getId()).getSingleResult() > 0) {
            throw invalido("idCategoria", "Escolha uma categoria sem filhas: a categoria pai só agrupa.");
        }
        return c;
    }

    private TituloEntity titulo(String empresa, String id, boolean travar) {
        TituloEntity t = id == null || id.length() > 36 ? null
                : travar ? em.find(TituloEntity.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(TituloEntity.class, id);
        if (t == null || t.getEmpresa() == null || !empresa.equals(t.getEmpresa().getId())) {
            throw new ApiException(404, "nao_encontrado", "Título não encontrado.");
        }
        return t;
    }

    private MovimentoEntity movimento(String empresa, String id) {
        MovimentoEntity m = id == null || id.length() > 36 ? null : em.find(MovimentoEntity.class, id);
        if (m == null || m.getEmpresa() == null || !empresa.equals(m.getEmpresa().getId())) {
            throw new ApiException(404, "nao_encontrado", "Movimento não encontrado.");
        }
        return m;
    }

    private static BigDecimal saldo(TituloEntity t) {
        return Dinheiro.normalizar(t.getValor()).subtract(Dinheiro.normalizar(t.getQuitado()));
    }

    private static void exigirForma(List<Map<String, String>> erros, String forma) {
        if (forma == null || !TitulosService.FORMAS.contains(forma)) {
            erros.add(erro("forma", "Escolha a forma: dinheiro, Pix, boleto, cartão ou transferência."));
        }
    }

    private static void validarValor(List<Map<String, String>> erros, BigDecimal valor) {
        if (valor == null || valor.compareTo(new BigDecimal("0.01")) < 0 || valor.compareTo(Dinheiro.TETO) > 0
                || valor.stripTrailingZeros().scale() > 2) {
            erros.add(erro("valor", "O valor deve ser de R$ 0,01 a R$ 999.999.999,99, com até 2 casas decimais."));
        }
    }

    static MovimentoDto dto(MovimentoEntity m) {
        return new MovimentoDto(m.getId(), m.getTipo(), m.getSentido(), m.getData(), m.getDescricao(), m.getForma(),
                Dinheiro.normalizar(m.getValor()), Dinheiro.normalizar(m.getQuitado()), Dinheiro.normalizar(m.getJuros()),
                Dinheiro.normalizar(m.getMulta()), Dinheiro.normalizar(m.getDesconto()), m.getConta() == null ? null : m.getConta().getId(),
                m.getTitulo() == null ? null : m.getTitulo().getId(), m.getCategoria() == null ? null : m.getCategoria().getId(),
                m.getIdLote(), m.getIdTransferencia(), m.getIdEstornoDe());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String cortar(String s) {
        return s == null ? null : s.length() <= 255 ? s : s.substring(0, 255);
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }

    private static ApiException invalido(String campo, String mensagem) {
        return invalido(List.of(erro(campo, mensagem)));
    }

    private static ApiException invalido(List<Map<String, String>> campos) {
        return new ApiException(400, "requisicao_invalida", campos.get(0).get("mensagem"), Map.of("campos", campos));
    }

    private static ApiException conflito(String mensagem) {
        return new ApiException(409, "conflito", mensagem);
    }
}
