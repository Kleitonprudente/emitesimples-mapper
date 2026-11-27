package br.com.emitesimples.financeiroop;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.CondicaoPagamentoEntity;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.nfseemissao.DocumentoAutorizado;
import br.com.emitesimples.nfseemissao.DocumentoCancelado;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Titulos gerados pelos documentos fiscais (docs/financeiro/README.md, secao 6; referencia §8). Observa os eventos que o modulo da nota
 * publica e reage DEPOIS do commit da nota ({@code AFTER_SUCCESS}), numa transacao propria e como a empresa da nota (RLS). O modulo da
 * nota nao depende deste. Tudo e idempotente e a ordem dos eventos nao importa: geracao e cancelamento do mesmo documento se serializam
 * por {@code pg_advisory_xact_lock}, e o cancelamento deixa uma marca (DOCUMENTOCANCELADO) que a geracao consulta. Falha aqui nunca
 * desfaz a nota: e registrada no log.
 */
@ApplicationScoped
public class TitulosDosDocumentos {

    private static final Logger LOG = Logger.getLogger(TitulosDosDocumentos.class);

    @Inject
    EntityManager em;

    @Inject
    ContextoTenantBanco contexto;

    @Inject
    LivroDeMovimentos livro;

    void aoAutorizar(@Observes(during = TransactionPhase.AFTER_SUCCESS) DocumentoAutorizado e) {
        try {
            emTransacaoPropria(e.idEmpresa(), () -> gerar(e));
        } catch (RuntimeException ex) {
            LOG.errorf(ex, "Titulo do documento %s %s nao gerado; a nota segue autorizada.", e.tipo(), e.idDocumento());
        }
    }

    void aoCancelar(@Observes(during = TransactionPhase.AFTER_SUCCESS) DocumentoCancelado e) {
        try {
            emTransacaoPropria(e.idEmpresa(), () -> cancelar(e));
        } catch (RuntimeException ex) {
            LOG.errorf(ex, "Titulos do documento %s %s nao tratados no cancelamento; a nota segue cancelada.", e.tipo(), e.idDocumento());
        }
    }

    /**
     * No AFTER_SUCCESS a transacao da nota ja fez commit, mas ainda esta associada a thread: o ContextoTenantBanco recusa abrir o bloco da
     * empresa com transacao associada. Suspende a transacao encerrada, abre o bloco da empresa e roda numa transacao nova.
     */
    private void emTransacaoPropria(String empresa, Runnable trabalho) {
        QuarkusTransaction.suspendingExisting().run(
                () -> contexto.rodarComoEmpresa(empresa, () -> QuarkusTransaction.requiringNew().run(trabalho)));
    }

    /** F5-H1, com a condicao de pagamento da nota (docs/financeiro/README.md, 9.1, E1). */
    void gerar(DocumentoAutorizado e) {
        travar(e.tipo(), e.idDocumento());
        if (cancelado(e.idEmpresa(), e.tipo(), e.idDocumento())) {
            return; // o cancelamento chegou antes: documento cancelado nunca ganha titulo ativo
        }
        if (!titulos(e.idEmpresa(), e.tipo(), e.idDocumento()).isEmpty()) {
            return; // evento repetido
        }
        BigDecimal valor = Dinheiro.normalizar(e.valorLiquido());
        if (valor.signum() <= 0) {
            LOG.infof("Documento %s %s com valor liquido zero: nenhum titulo.", e.tipo(), e.idDocumento());
            return;
        }
        CondicaoPagamentoEntity condicao = condicao(e.idEmpresa(), e.idCondicaoPagamento());
        List<GeradorDeParcelas.Parcela> parcelas = condicao == null
                ? List.of(new GeradorDeParcelas.Parcela(1, valor, e.data()))
                : GeradorDeParcelas.parcelado(valor, e.data().plusDays(condicao.getDiasPrimeiroVencimento()), condicao.getParcelas(),
                        condicao.getIntervaloDias());
        Object[] cfg = configuracao(e.idEmpresa());
        CategoriaFinanceiraEntity categoria = categoriaReceita(e.idEmpresa(), cfg == null ? null : (String) cfg[0]);
        String lancamento = UUID.randomUUID().toString();
        var agora = AuthService.agora();
        TituloEntity primeiro = null;
        for (GeradorDeParcelas.Parcela p : parcelas) {
            TituloEntity t = new TituloEntity();
            t.setEmpresa(em.getReference(EmpresaEntity.class, e.idEmpresa()));
            t.setTipo("RECEBER");
            t.setIdPessoa(e.idPessoa());
            t.setDescricao(e.numero() == null || e.numero().isBlank() ? "NFS-e" : "NFS-e nº " + e.numero());
            t.setObservacao(cfg == null ? null : (String) cfg[1]);
            t.setValor(p.valor());
            t.setQuitado(Dinheiro.ZERO);
            t.setVencimento(p.vencimento());
            t.setCategoria(categoria);
            t.setFormaPrevista(condicao == null ? null : condicao.getForma());
            t.setSituacao("ABERTO");
            t.setParcela(p.numero());
            t.setTotalParcelas(parcelas.size());
            t.setIdLancamento(lancamento);
            t.setOrigemTipo(e.tipo());
            t.setIdOrigem(e.idDocumento());
            t.setOrigemNumero(e.numero());
            t.setParaRevisao(false);
            t.setAprovadoEm(agora);
            t.setIdAprovadoPor(e.idUsuario());
            t.setIdCriadoPor(e.idUsuario());
            t.setDataCriacao(agora);
            t.setDataAtualizacao(agora);
            em.persist(t);
            primeiro = primeiro == null ? t : primeiro;
        }
        em.flush();
        if (condicao != null && Boolean.TRUE.equals(condicao.getReceberNaEmissao()) && parcelas.size() == 1) {
            receberNaEmissao(e, primeiro, condicao.getForma());
        }
    }

    /**
     * Baixa do titulo a vista na conta padrao ativa da forma, so se quem emitiu baixa (ADMINISTRADOR ou OPERADOR). Qualquer recusa deixa o
     * titulo em aberto (log), sem desfazer o que foi gerado.
     */
    private void receberNaEmissao(DocumentoAutorizado e, TituloEntity t, String forma) {
        if (e.idUsuario() == null || !podeBaixar(e.idEmpresa(), e.idUsuario())) {
            LOG.infof("Documento %s %s: receber na emissao sem permissao de baixa de quem emitiu; titulo em aberto.", e.tipo(), e.idDocumento());
            return;
        }
        String campo = PADRAO_DA_FORMA.get(forma);
        List<ContaFinanceiraEntity> contas = campo == null ? List.of() : em.createQuery("select c from ContaFinanceiraEntity c "
                + "where c.empresa.id = :e and c.ativo = true and c." + campo + " = true", ContaFinanceiraEntity.class)
                .setParameter("e", e.idEmpresa()).getResultList();
        if (contas.isEmpty()) {
            LOG.infof("Documento %s %s: sem conta padrao ativa de %s; titulo em aberto.", e.tipo(), e.idDocumento(), forma);
            return;
        }
        try {
            livro.baixarNaEmissao(t, contas.get(0), e.data(), forma, e.idUsuario());
            em.flush();
        } catch (ApiException ex) {
            LOG.infof("Documento %s %s: receber na emissao recusado (%s); titulo em aberto.", e.tipo(), e.idDocumento(), ex.getMessage());
        }
    }

    /** O campo de conta padrao de cada forma (lista fechada: nunca texto vindo de fora na consulta). */
    private static final java.util.Map<String, String> PADRAO_DA_FORMA = java.util.Map.of("DINHEIRO", "padraoDinheiro", "PIX", "padraoPix",
            "BOLETO", "padraoBoleto", "CARTAO", "padraoCartao", "TRANSFERENCIA", "padraoTransferencia");

    private boolean podeBaixar(String empresa, String usuario) {
        return ((Number) em.createNativeQuery("select count(*) from vinculousuario where empresa_id = :e and usuario_id = :u and ativo "
                + "and papel in ('ADMINISTRADOR', 'OPERADOR')").setParameter("e", empresa).setParameter("u", usuario).getSingleResult())
                .longValue() > 0;
    }

    /** A condicao da nota, se existe, e da empresa e esta ativa; senao null (1 titulo a vista). */
    private CondicaoPagamentoEntity condicao(String empresa, String id) {
        if (id == null || id.isBlank() || id.length() > 36) {
            return null;
        }
        CondicaoPagamentoEntity c = em.find(CondicaoPagamentoEntity.class, id);
        if (c == null || c.getEmpresa() == null || !empresa.equals(c.getEmpresa().getId()) || !Boolean.TRUE.equals(c.getAtivo())) {
            LOG.infof("Condicao de pagamento %s inexistente, de outra empresa ou inativa: titulo a vista.", id);
            return null;
        }
        return c;
    }

    /** F5-H2. */
    void cancelar(DocumentoCancelado e) {
        travar(e.tipo(), e.idDocumento());
        em.createNativeQuery("insert into documentocancelado (empresa_id, origem_tipo, id_origem, cancelado_em) values (:e, :t, :d, localtimestamp) "
                + "on conflict do nothing").setParameter("e", e.idEmpresa()).setParameter("t", e.tipo()).setParameter("d", e.idDocumento())
                .executeUpdate();
        var agora = AuthService.agora();
        for (TituloEntity t : titulos(e.idEmpresa(), e.tipo(), e.idDocumento())) {
            if ("CANCELADO".equals(t.getSituacao())) {
                continue;
            }
            if (Dinheiro.normalizar(t.getQuitado()).signum() == 0) {
                t.setSituacao("CANCELADO");
                t.setCanceladoEm(agora);
            } else {
                t.setParaRevisao(true); // ja baixado: alguem decide entre estornar ou registrar devolucao
            }
            t.setDataAtualizacao(agora);
        }
        em.flush();
    }

    private void travar(String tipo, String idDocumento) {
        em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:k, 0))").setParameter("k", "documento:" + tipo + ":" + idDocumento)
                .getSingleResult();
    }

    private boolean cancelado(String empresa, String tipo, String idDocumento) {
        return ((Number) em.createNativeQuery("select count(*) from documentocancelado where empresa_id = :e and origem_tipo = :t and id_origem = :d")
                .setParameter("e", empresa).setParameter("t", tipo).setParameter("d", idDocumento).getSingleResult()).longValue() > 0;
    }

    private List<TituloEntity> titulos(String empresa, String tipo, String idDocumento) {
        return em.createQuery("select t from TituloEntity t where t.empresa.id = :e and t.origemTipo = :t and t.idOrigem = :d order by t.parcela",
                TituloEntity.class).setParameter("e", empresa).setParameter("t", tipo).setParameter("d", idDocumento)
                .setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
    }

    /** [id da categoria padrao de receita, observacao padrao a receber] da configuracao, ou null se a empresa ainda nao tem. */
    private Object[] configuracao(String empresa) {
        List<Object[]> r = em.createQuery("select r.id, c.observacaoPadraoReceber from ConfiguracaoFinanceiraEntity c "
                + "left join c.categoriaPadraoReceita r where c.empresa.id = :e", Object[].class).setParameter("e", empresa).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** A padrao de receita da configuracao; senao "Prestacao de servicos", se existir como folha ativa; senao nenhuma. */
    private CategoriaFinanceiraEntity categoriaReceita(String empresa, String idPadrao) {
        if (idPadrao != null) {
            return em.find(CategoriaFinanceiraEntity.class, idPadrao);
        }
        List<CategoriaFinanceiraEntity> r = em.createQuery("select c from CategoriaFinanceiraEntity c where c.empresa.id = :e and c.tipo = 'RECEITA' "
                + "and c.ativo = true and c.nome = 'Prestação de serviços' and not exists (select f from CategoriaFinanceiraEntity f "
                + "where f.categoriaPai = c)", CategoriaFinanceiraEntity.class).setParameter("e", empresa).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }
}
