package br.com.emitesimples.tenant;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.function.Supplier;

/**
 * Decide, para cada conexao que o pool entrega, sob qual escopo de tenant o banco (RLS, migracao V3) deve responder.
 * E o ponto de extensao unico: webhook, IntegraNotas e jobs devem usar {@link #comoEmpresa} ou {@link #comoSistema}.
 *
 * Ordem de decisao ({@link #resolver()}):
 * 1. bloco explicito ({@code comoEmpresa} / {@code comoSistema}) na thread corrente;
 * 2. requisicao HTTP em curso: claims do JWT ({@code empresa_id}, {@code plataforma_admin}); sem token => nada
 *    liberado (falha fechada);
 * 3. SEM requisicao HTTP (startup, bootstrap, testes que montam dados, jobs): sistema (irrestrito).
 *
 * O bloco explicito precisa ser aberto ANTES da transacao (a configuracao e aplicada quando a conexao e adquirida);
 * abri-lo dentro de uma transacao ja ativa nao teria efeito e por isso lanca {@link IllegalStateException}.
 */
@ApplicationScoped
public class ContextoTenantBanco {

    /** Escopo aplicado ao banco: empresa (pode ser vazia) e se e irrestrito. */
    public record Escopo(String empresaId, boolean irrestrito) {
        public static final Escopo SISTEMA = new Escopo("", true);
        public static final Escopo NEGADO = new Escopo("", false);
    }

    private static final ThreadLocal<Escopo> EXPLICITO = new ThreadLocal<>();

    /** Executa o bloco com o banco restrito a UMA empresa. */
    public <T> T comoEmpresa(String empresaId, Supplier<T> bloco) {
        if (empresaId == null || empresaId.isBlank()) {
            throw new IllegalArgumentException("empresaId obrigatorio");
        }
        return executar(new Escopo(empresaId, false), bloco);
    }

    public void rodarComoEmpresa(String empresaId, Runnable bloco) {
        comoEmpresa(empresaId, () -> {
            bloco.run();
            return null;
        });
    }

    /** Executa o bloco como sistema: enxerga e grava todas as empresas. Use so em codigo confiavel (auth, jobs). */
    public <T> T comoSistema(Supplier<T> bloco) {
        return executar(Escopo.SISTEMA, bloco);
    }

    public void rodarComoSistema(Runnable bloco) {
        comoSistema(() -> {
            bloco.run();
            return null;
        });
    }

    private <T> T executar(Escopo escopo, Supplier<T> bloco) {
        if (QuarkusTransaction.isActive()) {
            throw new IllegalStateException(
                    "ContextoTenantBanco: abra o bloco antes da transacao (a transacao ativa ja tem conexao e escopo).");
        }
        Escopo anterior = EXPLICITO.get();
        EXPLICITO.set(escopo);
        try {
            return bloco.get();
        } finally {
            if (anterior == null) {
                EXPLICITO.remove();
            } else {
                EXPLICITO.set(anterior);
            }
        }
    }

    /** Escopo valido agora, nesta thread. Chamado pelo interceptor do pool a cada aquisicao de conexao. */
    public Escopo resolver() {
        Escopo explicito = EXPLICITO.get();
        if (explicito != null) {
            return explicito;
        }
        if (!requisicaoHttpEmCurso()) {
            return Escopo.SISTEMA;
        }
        EscopoTenant jwt = Arc.container().instance(EscopoTenant.class).get();
        String empresa = jwt.empresaIdOuNulo();
        return new Escopo(empresa == null ? "" : empresa, jwt.irrestrito());
    }

    private static boolean requisicaoHttpEmCurso() {
        ManagedContext ctx = Arc.container().requestContext();
        if (!ctx.isActive()) {
            return false;
        }
        var atual = Arc.container().instance(CurrentVertxRequest.class).get();
        return atual != null && atual.getCurrent() != null;
    }
}
