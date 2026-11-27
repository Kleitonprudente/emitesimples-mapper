package br.com.emitesimples.financeiroop;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Casos de uso do Titulo (docs/financeiro/README.md, secao 3, F2-H1 a F2-H6; referencia §5). O Titulo NAO tem CRUD gerado: so este
 * servico grava nele (e, na Fase 3, o livro de movimentos). A empresa e SEMPRE a da sessao; a RLS do banco vale por cima. Toda
 * mudanca de um titulo existente trava a linha (FOR UPDATE).
 *
 * <p>Erros: 400 {@code requisicao_invalida} com o campo (entrada invalida); 409 {@code conflito} quando a situacao do titulo nao
 * permite a operacao; 404 quando o titulo nao e da empresa.
 */
@ApplicationScoped
public class TitulosService {

    public static final Set<String> TIPOS = Set.of("RECEBER", "PAGAR");
    public static final Set<String> FORMAS = Set.of("DINHEIRO", "PIX", "BOLETO", "CARTAO", "TRANSFERENCIA");
    /** B3 (README 9.4): rascunhos de agente aguardando aprovacao, por usuario dono. */
    static final int MAX_RASCUNHOS_DE_AGENTE = 20;

    public static final Set<String> FILTROS = Set.of("EM_ABERTO", "PARCIAIS", "VENCIDOS", "PAGOS", "CANCELADOS", "PARA_REVISAO");
    static final Map<String, String> ROTULO_SITUACAO = Map.of("ABERTO", "Aberto", "PARCIAL", "Parcial", "PAGO", "Pago", "CANCELADO", "Cancelado");
    static final BigDecimal PERCENTUAL_MAXIMO = new BigDecimal("999.99");

    public record CriarTitulo(String tipo, String idPessoa, String descricao, String observacao, BigDecimal valor, LocalDate vencimento,
            String idCategoria, String formaPrevista, Integer parcelas, Integer intervaloDias, Integer repetirMeses,
            BigDecimal multaPercentual, BigDecimal jurosMensalPercentual) {
    }

    /** PUT: substitui os campos editaveis (opcional ausente = apagado). {@code valor} ausente = mantem o atual. */
    public record AtualizarTitulo(String descricao, String observacao, BigDecimal valor, LocalDate vencimento, String idCategoria,
            String formaPrevista, BigDecimal multaPercentual, BigDecimal jurosMensalPercentual) {
    }

    public record CategoriaResumo(String id, String nome) {
    }

    public record TituloDto(String id, String tipo, String idPessoa, String nomePessoa, String descricao, String observacao, BigDecimal valor,
            BigDecimal quitado, BigDecimal saldo, LocalDate vencimento, boolean vencido, String formaPrevista, String situacao,
            int parcela, int totalParcelas, String idLancamento, String origemTipo, String idOrigem, String origemNumero,
            BigDecimal multaPercentual, BigDecimal jurosMensalPercentual, boolean paraRevisao, boolean aprovado,
            CategoriaResumo categoria) {
    }

    public record Totais(BigDecimal valor, BigDecimal quitado, BigDecimal saldo) {
    }

    public record Pagina(List<TituloDto> itens, long total, int pagina, int tamanho, Totais totais) {
    }

    public record ResumoDoTipo(BigDecimal emAberto, BigDecimal vencido, long quantidadeVencidos) {
    }

    public record Resumo(ResumoDoTipo receber, ResumoDoTipo pagar) {
    }

    public record Sugestoes(String tipo, LocalDate vencimento, String observacao, String formaPrevista, CategoriaResumo categoria) {
    }

    public record Filtro(String tipo, String filtro, LocalDate de, LocalDate ate, String idPessoa, String idCategoria, String idLancamento,
            String busca, Integer pagina, Integer tamanho) {
    }

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    @Inject
    JsonWebToken jwt;

    @Inject
    CobrancasService cobrancas;

    static LocalDate hoje() {
        return LocalDate.now(ContaFinanceiraListener.FUSO);
    }

    // ------------------------------------------------------------------------------------------------------------ F2-H1

    public List<TituloDto> criar(CriarTitulo r) {
        String empresa = escopo.exigir("empresa_id");
        String usuario = jwt.getSubject();
        List<Map<String, String>> erros = new ArrayList<>();
        if (r == null) {
            throw invalido(List.of(erro("corpo", "Informe os dados do título.")));
        }
        if (r.tipo() == null || !TIPOS.contains(r.tipo())) {
            erros.add(erro("tipo", "Escolha o tipo do título: a receber ou a pagar."));
        }
        validarTextos(erros, r.descricao(), r.observacao());
        validarValor(erros, r.valor());
        if (r.vencimento() == null) {
            erros.add(erro("vencimento", "Informe o vencimento."));
        }
        validarForma(erros, r.formaPrevista());
        validarPercentuais(erros, r.multaPercentual(), r.jurosMensalPercentual());
        int parcelas = r.parcelas() == null ? 1 : r.parcelas();
        int intervalo = r.intervaloDias() == null ? 30 : r.intervaloDias();
        if (parcelas < 1 || parcelas > 48) {
            erros.add(erro("parcelas", "O número de parcelas deve ser de 1 a 48."));
        }
        if (intervalo < 0 || intervalo > 365) {
            erros.add(erro("intervaloDias", "O intervalo entre parcelas deve ser de 0 a 365 dias."));
        }
        if (r.repetirMeses() != null && (r.repetirMeses() < 1 || r.repetirMeses() > 24)) {
            erros.add(erro("repetirMeses", "Repita de 1 a 24 meses."));
        }
        if (r.repetirMeses() != null && parcelas > 1) {
            erros.add(erro("repetirMeses", "Escolha parcelado ou recorrente, não os dois."));
        }
        if (r.idPessoa() != null && r.idPessoa().length() > 36) {
            erros.add(erro("idPessoa", "Pessoa não encontrada."));
        }
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        List<GeradorDeParcelas.Parcela> geradas;
        try {
            geradas = r.repetirMeses() != null
                    ? GeradorDeParcelas.recorrente(r.valor(), r.vencimento(), r.repetirMeses())
                    : GeradorDeParcelas.parcelado(r.valor(), r.vencimento(), parcelas, intervalo);
        } catch (IllegalArgumentException e) {
            throw invalido(List.of(erro("valor", e.getMessage())));
        }
        List<TituloDto> lista = QuarkusTransaction.requiringNew().call(() -> {
            List<Map<String, String>> f = new ArrayList<>();
            conferirPessoa(f, empresa, r.idPessoa(), r.tipo());
            CategoriaFinanceiraEntity categoria = categoria(f, empresa, r.idCategoria(), r.tipo());
            if (!f.isEmpty()) {
                throw invalido(f);
            }
            Object cliente = jwt.getClaim(br.com.emitesimples.security.JwtEmissor.CLAIM_CLIENTE_ACESSO);
            String agente = cliente == null ? null : String.valueOf(cliente).replace("\"", "");
            if (agente != null) {
                // B3 (README 9.4): no maximo 20 rascunhos de agente aguardando aprovacao por usuario dono
                long pendentes = ((Number) em.createQuery("select count(t) from TituloEntity t where t.empresa.id = :e and t.idCriadoPor = :u "
                        + "and t.idCriadoPorCliente is not null and t.aprovadoEm is null and t.situacao <> 'CANCELADO'")
                        .setParameter("e", empresa).setParameter("u", usuario).getSingleResult()).longValue();
                if (pendentes + geradas.size() > MAX_RASCUNHOS_DE_AGENTE) {
                    throw new ApiException(409, "rascunhos_demais", "Há " + pendentes + " rascunho(s) de agente aguardando aprovação (o limite é "
                            + MAX_RASCUNHOS_DE_AGENTE + "). Aprove ou cancele antes de lançar mais.");
                }
            }
            EmpresaEntity emp = em.getReference(EmpresaEntity.class, empresa);
            String lancamento = UUID.randomUUID().toString();
            var agora = AuthService.agora();
            List<TituloDto> criados = new ArrayList<>();
            for (GeradorDeParcelas.Parcela p : geradas) {
                TituloEntity t = new TituloEntity();
                t.setEmpresa(emp);
                t.setTipo(r.tipo());
                t.setIdPessoa(vazioParaNulo(r.idPessoa()));
                t.setDescricao(r.descricao().trim());
                t.setObservacao(vazioParaNulo(r.observacao()));
                t.setValor(p.valor());
                t.setQuitado(Dinheiro.ZERO);
                t.setVencimento(p.vencimento());
                t.setCategoria(categoria);
                t.setFormaPrevista(vazioParaNulo(r.formaPrevista()));
                t.setSituacao("ABERTO");
                t.setParcela(p.numero());
                t.setTotalParcelas(geradas.size());
                t.setIdLancamento(lancamento);
                t.setOrigemTipo("MANUAL");
                t.setMultaPercentual(r.multaPercentual());
                t.setJurosMensalPercentual(r.jurosMensalPercentual());
                t.setParaRevisao(false);
                // B3 (README 9.4): lancado por pessoa nasce aprovado; por agente, nasce sem aprovacao (nao aceita baixa ate aprovar)
                t.setAprovadoEm(agente == null ? agora : null);
                t.setIdAprovadoPor(agente == null ? usuario : null);
                t.setIdCriadoPor(usuario);
                t.setIdCriadoPorCliente(agente);
                t.setDataCriacao(agora);
                t.setDataAtualizacao(agora);
                em.persist(t);
                criados.add(dto(t));
            }
            em.flush();
            return criados;
        });
        // Fase 7 (README 9.5): cobranca automatica depois do commit, uma tentativa so; recorrente nao dispara
        if (r.repetirMeses() == null) {
            cobrancas.automatica(empresa, lista, usuario);
        }
        return lista;
    }

    // ------------------------------------------------------------------------------------------------------------ F2-H2

    public Sugestoes sugestoes(String tipo) {
        String empresa = escopo.exigir("empresa_id");
        if (tipo == null || !TIPOS.contains(tipo)) {
            throw invalido(List.of(erro("tipo", "Escolha o tipo do título: a receber ou a pagar.")));
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            List<Object[]> linhas = em.createQuery("select c.vencimentoPadraoDias, c.observacaoPadraoReceber, c.formaPrevistaPadraoReceber, "
                    + "r.id, r.nome, d.id, d.nome from ConfiguracaoFinanceiraEntity c left join c.categoriaPadraoReceita r "
                    + "left join c.categoriaPadraoDespesa d where c.empresa.id = :e", Object[].class)
                    .setParameter("e", empresa).getResultList();
            boolean receber = "RECEBER".equals(tipo);
            if (linhas.isEmpty()) {
                return new Sugestoes(tipo, receber ? hoje() : null, null, null, null);
            }
            Object[] c = linhas.get(0);
            CategoriaResumo cat = receber ? (c[3] == null ? null : new CategoriaResumo((String) c[3], (String) c[4]))
                    : (c[5] == null ? null : new CategoriaResumo((String) c[5], (String) c[6]));
            if (!receber) {
                return new Sugestoes(tipo, null, null, null, cat);
            }
            int dias = c[0] == null ? 0 : ((Number) c[0]).intValue();
            return new Sugestoes(tipo, hoje().plusDays(dias), (String) c[1], (String) c[2], cat);
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F2-H3

    public TituloDto atualizar(String id, AtualizarTitulo r) {
        String empresa = escopo.exigir("empresa_id");
        if (r == null) {
            throw invalido(List.of(erro("corpo", "Informe os dados do título.")));
        }
        List<Map<String, String>> erros = new ArrayList<>();
        validarTextos(erros, r.descricao(), r.observacao());
        if (r.valor() != null) {
            validarValor(erros, r.valor());
        }
        if (r.vencimento() == null) {
            erros.add(erro("vencimento", "Informe o vencimento."));
        }
        validarForma(erros, r.formaPrevista());
        validarPercentuais(erros, r.multaPercentual(), r.jurosMensalPercentual());
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            TituloEntity t = travar(empresa, id);
            if (!Set.of("ABERTO", "PARCIAL").contains(t.getSituacao())) {
                throw conflito("Título " + ROTULO_SITUACAO.get(t.getSituacao()) + ": não pode ser editado.");
            }
            BigDecimal novoValor = r.valor() == null ? t.getValor() : Dinheiro.normalizar(r.valor());
            if (novoValor.compareTo(t.getValor()) != 0
                    && !("MANUAL".equals(t.getOrigemTipo()) && t.getQuitado().signum() == 0 && "ABERTO".equals(t.getSituacao()))) {
                throw invalido(List.of(erro("valor", "O valor só muda em título lançado à mão e sem nenhuma baixa.")));
            }
            List<Map<String, String>> f = new ArrayList<>();
            CategoriaFinanceiraEntity categoria = categoria(f, empresa, r.idCategoria(), t.getTipo());
            if (!f.isEmpty()) {
                throw invalido(f);
            }
            t.setDescricao(r.descricao().trim());
            t.setObservacao(vazioParaNulo(r.observacao()));
            t.setValor(novoValor);
            t.setVencimento(r.vencimento());
            t.setCategoria(categoria);
            t.setFormaPrevista(vazioParaNulo(r.formaPrevista()));
            t.setMultaPercentual(r.multaPercentual());
            t.setJurosMensalPercentual(r.jurosMensalPercentual());
            t.setDataAtualizacao(AuthService.agora());
            em.flush();
            return dto(t);
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F2-H4 e F2-H5

    public TituloDto cancelar(String id) {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> {
            TituloEntity t = travar(empresa, id);
            if ("CANCELADO".equals(t.getSituacao())) {
                throw conflito("Este título já está cancelado.");
            }
            if (t.getQuitado().signum() > 0) {
                throw conflito("Estorne a baixa antes de cancelar.");
            }
            var agora = AuthService.agora();
            t.setSituacao("CANCELADO");
            t.setCanceladoEm(agora);
            t.setDataAtualizacao(agora);
            cobrancas.cancelarVivas(t.getId()); // Fase 7 (README 9.5): o titulo cancelado cancela as cobrancas vivas
            em.flush();
            return dto(t);
        });
    }

    public TituloDto aprovar(String id) {
        String empresa = escopo.exigir("empresa_id");
        String usuario = jwt.getSubject();
        return QuarkusTransaction.requiringNew().call(() -> {
            TituloEntity t = travar(empresa, id);
            if ("CANCELADO".equals(t.getSituacao())) {
                throw conflito("Título cancelado não pode ser aprovado.");
            }
            if (t.getAprovadoEm() != null) {
                throw conflito("Este título já está aprovado.");
            }
            var agora = AuthService.agora();
            t.setAprovadoEm(agora);
            t.setIdAprovadoPor(usuario);
            t.setDataAtualizacao(agora);
            em.flush();
            return dto(t);
        });
    }

    // ------------------------------------------------------------------------------------------------------------ F2-H6

    public TituloDto buscar(String id) {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> {
            TituloEntity t = daEmpresa(empresa, id, false);
            return dto(t, nomes(List.of(t)));
        });
    }

    public Pagina listar(Filtro f) {
        String empresa = escopo.exigir("empresa_id");
        List<Map<String, String>> erros = new ArrayList<>();
        if (f.tipo() == null || !TIPOS.contains(f.tipo())) {
            erros.add(erro("tipo", "Escolha o tipo do título: a receber ou a pagar."));
        }
        if (f.filtro() != null && !FILTROS.contains(f.filtro())) {
            erros.add(erro("filtro", "Filtro inválido. Use EM_ABERTO, PARCIAIS, VENCIDOS, PAGOS, CANCELADOS ou PARA_REVISAO."));
        }
        if (f.de() != null && f.ate() != null && f.ate().isBefore(f.de())) {
            erros.add(erro("ate", "A data final não pode ser antes da inicial."));
        }
        int pagina = f.pagina() == null ? 1 : f.pagina();
        int tamanho = f.tamanho() == null ? 50 : f.tamanho();
        if (pagina < 1) {
            erros.add(erro("pagina", "A página começa em 1."));
        }
        if (tamanho < 1 || tamanho > 100) {
            erros.add(erro("tamanho", "O tamanho da página deve ser de 1 a 100."));
        }
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        Map<String, Object> p = new LinkedHashMap<>();
        String onde = condicao(empresa, f, p);
        return QuarkusTransaction.requiringNew().call(() -> {
            TypedQuery<TituloEntity> q = em.createQuery("select t from TituloEntity t left join fetch t.categoria where " + onde
                    + " order by t.vencimento, t.parcela, t.id", TituloEntity.class);
            p.forEach(q::setParameter);
            List<TituloEntity> pagina1 = q.setFirstResult((pagina - 1) * tamanho).setMaxResults(tamanho).getResultList();
            Map<String, String> nomesDaPagina = nomes(pagina1);
            List<TituloDto> itens = pagina1.stream().map(x -> dto(x, nomesDaPagina)).toList();
            var tq = em.createQuery("select count(t), coalesce(sum(t.valor), 0), coalesce(sum(t.quitado), 0) from TituloEntity t where "
                    + onde, Object[].class);
            p.forEach(tq::setParameter);
            Object[] tot = tq.getSingleResult();
            BigDecimal valor = Dinheiro.normalizar((BigDecimal) tot[1]);
            BigDecimal quitado = Dinheiro.normalizar((BigDecimal) tot[2]);
            return new Pagina(itens, ((Number) tot[0]).longValue(), pagina, tamanho, new Totais(valor, quitado, valor.subtract(quitado)));
        });
    }

    public Resumo resumo() {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> new Resumo(resumoDo(empresa, "RECEBER"), resumoDo(empresa, "PAGAR")));
    }

    private ResumoDoTipo resumoDo(String empresa, String tipo) {
        BigDecimal emAberto = (BigDecimal) em.createQuery("select coalesce(sum(t.valor - t.quitado), 0) from TituloEntity t "
                + "where t.empresa.id = :e and t.tipo = :t and t.situacao in ('ABERTO', 'PARCIAL')")
                .setParameter("e", empresa).setParameter("t", tipo).getSingleResult();
        Object[] v = (Object[]) em.createQuery("select coalesce(sum(t.valor - t.quitado), 0), count(t) from TituloEntity t "
                + "where t.empresa.id = :e and t.tipo = :t and t.situacao in ('ABERTO', 'PARCIAL') and t.vencimento < :h")
                .setParameter("e", empresa).setParameter("t", tipo).setParameter("h", hoje()).getSingleResult();
        return new ResumoDoTipo(Dinheiro.normalizar(emAberto), Dinheiro.normalizar((BigDecimal) v[0]), ((Number) v[1]).longValue());
    }

    /** Monta o WHERE da lista e dos totais (a mesma definicao de filtro nos dois, §7). */
    static String condicao(String empresa, Filtro f, Map<String, Object> p) {
        StringBuilder w = new StringBuilder("t.empresa.id = :e and t.tipo = :tipo");
        p.put("e", empresa);
        p.put("tipo", f.tipo());
        if (f.filtro() != null) {
            switch (f.filtro()) {
                case "EM_ABERTO" -> w.append(" and t.situacao in ('ABERTO', 'PARCIAL')");
                case "PARCIAIS" -> w.append(" and t.situacao = 'PARCIAL'");
                case "VENCIDOS" -> {
                    w.append(" and t.situacao in ('ABERTO', 'PARCIAL') and t.vencimento < :hoje");
                    p.put("hoje", hoje());
                }
                case "PAGOS" -> w.append(" and t.situacao = 'PAGO'");
                case "CANCELADOS" -> w.append(" and t.situacao = 'CANCELADO'");
                case "PARA_REVISAO" -> w.append(" and t.paraRevisao = true");
                default -> throw new IllegalStateException(f.filtro());
            }
        }
        if (f.de() != null) {
            w.append(" and t.vencimento >= :de");
            p.put("de", f.de());
        }
        if (f.ate() != null) {
            w.append(" and t.vencimento <= :ate");
            p.put("ate", f.ate());
        }
        if (f.idPessoa() != null && !f.idPessoa().isBlank()) {
            w.append(" and t.idPessoa = :pessoa");
            p.put("pessoa", f.idPessoa());
        }
        if (f.idCategoria() != null && !f.idCategoria().isBlank()) {
            // a categoria pai inclui as filhas (titulo so usa categoria folha; decisao registrada em docs/financeiro, F2-H6)
            w.append(" and (t.categoria.id = :cat or t.categoria.categoriaPai.id = :cat)");
            p.put("cat", f.idCategoria());
        }
        if (f.busca() != null && !f.busca().isBlank()) {
            // busca na descricao, sem acento e sem caixa (extensao unaccent da V2, como a busca do CRUD gerado)
            w.append(" and cast(function('unaccent', lower(t.descricao)) as String) like cast(function('unaccent', lower(:busca)) as String)");
            p.put("busca", "%" + f.busca().trim().replace("%", "").replace("_", "") + "%");
        }
        if (f.idLancamento() != null && !f.idLancamento().isBlank()) {
            w.append(" and t.idLancamento = :lanc");
            p.put("lanc", f.idLancamento());
        }
        return w.toString();
    }

    // ------------------------------------------------------------------------------------------------------------ apoio

    private TituloEntity travar(String empresa, String id) {
        return daEmpresa(empresa, id, true);
    }

    private TituloEntity daEmpresa(String empresa, String id, boolean travar) {
        TituloEntity t = id == null || id.length() > 36 ? null
                : travar ? em.find(TituloEntity.class, id, LockModeType.PESSIMISTIC_WRITE) : em.find(TituloEntity.class, id);
        if (t == null || t.getEmpresa() == null || !empresa.equals(t.getEmpresa().getId())) {
            throw new ApiException(404, "nao_encontrado", "Título não encontrado.");
        }
        return t;
    }

    private void conferirPessoa(List<Map<String, String>> f, String empresa, String idPessoa, String tipo) {
        if (idPessoa == null || idPessoa.isBlank()) {
            return;
        }
        PessoaEntity p = em.find(PessoaEntity.class, idPessoa);
        if (p == null || p.getEmpresa() == null || !empresa.equals(p.getEmpresa().getId())) {
            f.add(erro("idPessoa", "Pessoa não encontrada."));
        } else if ("RECEBER".equals(tipo) && !Boolean.TRUE.equals(p.getCliente())) {
            f.add(erro("idPessoa", "Esta pessoa não está cadastrada como cliente."));
        } else if ("PAGAR".equals(tipo) && !Boolean.TRUE.equals(p.getFornecedor())) {
            f.add(erro("idPessoa", "Esta pessoa não está cadastrada como fornecedor."));
        }
    }

    /** Categoria do titulo: da empresa, ativa, do tipo certo e sem filhas. Ausente = sem categoria. */
    private CategoriaFinanceiraEntity categoria(List<Map<String, String>> f, String empresa, String id, String tipoTitulo) {
        if (id == null || id.isBlank()) {
            return null;
        }
        CategoriaFinanceiraEntity c = id.length() > 36 ? null : em.find(CategoriaFinanceiraEntity.class, id);
        if (c == null || c.getEmpresa() == null || !empresa.equals(c.getEmpresa().getId())) {
            f.add(erro("idCategoria", "Categoria não encontrada."));
            return null;
        }
        String tipoCategoria = "RECEBER".equals(tipoTitulo) ? "RECEITA" : "DESPESA";
        if (!tipoCategoria.equals(c.getTipo())) {
            f.add(erro("idCategoria", "RECEBER".equals(tipoTitulo) ? "Título a receber usa categoria de receita."
                    : "Título a pagar usa categoria de despesa."));
        } else if (!Boolean.TRUE.equals(c.getAtivo())) {
            f.add(erro("idCategoria", "Esta categoria está inativa."));
        } else if (em.createQuery("select count(x) from CategoriaFinanceiraEntity x where x.categoriaPai.id = :id", Long.class)
                .setParameter("id", c.getId()).getSingleResult() > 0) {
            f.add(erro("idCategoria", "Escolha uma categoria sem filhas: a categoria pai só agrupa."));
        }
        return c;
    }

    private static void validarTextos(List<Map<String, String>> erros, String descricao, String observacao) {
        if (descricao == null || descricao.isBlank()) {
            erros.add(erro("descricao", "Informe a descrição."));
        } else if (descricao.trim().length() > 255) {
            erros.add(erro("descricao", "A descrição tem no máximo 255 caracteres."));
        }
        if (observacao != null && observacao.length() > 255) {
            erros.add(erro("observacao", "A observação tem no máximo 255 caracteres."));
        }
    }

    private static void validarValor(List<Map<String, String>> erros, BigDecimal valor) {
        if (valor == null) {
            erros.add(erro("valor", "Informe o valor."));
        } else if (valor.stripTrailingZeros().scale() > 2) {
            erros.add(erro("valor", "O valor tem no máximo 2 casas decimais."));
        } else if (valor.compareTo(new BigDecimal("0.01")) < 0 || valor.compareTo(Dinheiro.TETO) > 0) {
            erros.add(erro("valor", "O valor deve ser de R$ 0,01 a R$ 999.999.999,99."));
        }
    }

    private static void validarForma(List<Map<String, String>> erros, String forma) {
        if (forma != null && !forma.isBlank() && !FORMAS.contains(forma)) {
            erros.add(erro("formaPrevista", "Escolha uma forma de pagamento válida."));
        }
    }

    private static void validarPercentuais(List<Map<String, String>> erros, BigDecimal multa, BigDecimal juros) {
        if (fora(multa)) {
            erros.add(erro("multaPercentual", "A multa deve ser de 0 a 999,99%, com até 2 casas decimais."));
        }
        if (fora(juros)) {
            erros.add(erro("jurosMensalPercentual", "Os juros ao mês devem ser de 0 a 999,99%, com até 2 casas decimais."));
        }
    }

    private static boolean fora(BigDecimal v) {
        return v != null && (v.signum() < 0 || v.compareTo(PERCENTUAL_MAXIMO) > 0 || v.stripTrailingZeros().scale() > 2);
    }

    static TituloDto dto(TituloEntity t) {
        return dto(t, null);
    }

    /** Nomes das pessoas dos titulos, lidos do cadastro (o titulo guarda so o idPessoa, A8). Pessoa que nao existe mais fica sem nome. */
    private Map<String, String> nomes(List<TituloEntity> titulos) {
        Set<String> ids = new java.util.HashSet<>();
        titulos.forEach(x -> {
            if (x.getIdPessoa() != null) {
                ids.add(x.getIdPessoa());
            }
        });
        Map<String, String> r = new java.util.HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] o : em.createQuery("select p.id, p.nome from PessoaEntity p where p.id in :ids", Object[].class)
                    .setParameter("ids", ids).getResultList()) {
                r.put((String) o[0], (String) o[1]);
            }
        }
        return r;
    }

    static TituloDto dto(TituloEntity t, Map<String, String> nomes) {
        BigDecimal saldo = Dinheiro.normalizar(t.getValor()).subtract(Dinheiro.normalizar(t.getQuitado()));
        boolean vencido = Set.of("ABERTO", "PARCIAL").contains(t.getSituacao()) && t.getVencimento().isBefore(hoje());
        CategoriaFinanceiraEntity c = t.getCategoria();
        return new TituloDto(t.getId(), t.getTipo(), t.getIdPessoa(), nomes == null ? null : nomes.get(t.getIdPessoa()), t.getDescricao(), t.getObservacao(), Dinheiro.normalizar(t.getValor()),
                Dinheiro.normalizar(t.getQuitado()), saldo, t.getVencimento(), vencido, t.getFormaPrevista(), t.getSituacao(),
                t.getParcela(), t.getTotalParcelas(), t.getIdLancamento(), t.getOrigemTipo(), t.getIdOrigem(), t.getOrigemNumero(),
                t.getMultaPercentual(), t.getJurosMensalPercentual(), Boolean.TRUE.equals(t.getParaRevisao()), t.getAprovadoEm() != null,
                c == null ? null : new CategoriaResumo(c.getId(), c.getNome()));
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

    private static ApiException invalido(List<Map<String, String>> campos) {
        return new ApiException(400, "requisicao_invalida", campos.get(0).get("mensagem"), Map.of("campos", campos));
    }

    private static ApiException conflito(String mensagem) {
        return new ApiException(409, "conflito", mensagem);
    }
}
