package br.com.emitesimples.tenant;

import io.agroal.api.AgroalPoolInterceptor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jboss.logging.Logger;

/**
 * Aplica o escopo de tenant (RLS) em TODA conexao entregue pelo pool, antes de qualquer SQL da aplicacao,
 * e o limpa quando a conexao volta ao pool. Cobre Hibernate, Panache, EntityManager e JDBC direto.
 */
@ApplicationScoped
public class RlsPoolInterceptor implements AgroalPoolInterceptor {

    private static final Logger LOG = Logger.getLogger(RlsPoolInterceptor.class);
    private static final String SQL =
            "select set_config('app.empresa_id', ?, ?), set_config('app.irrestrito', ?, ?)";

    @Inject
    ContextoTenantBanco contexto;

    @Override
    public void onConnectionAcquire(Connection c) {
        aplicar(c, contexto.resolver());
    }

    @Override
    public void onConnectionReturn(Connection c) {
        try {
            aplicar(c, ContextoTenantBanco.Escopo.NEGADO);
        } catch (RuntimeException e) {
            // a proxima aquisicao sobrescreve de qualquer forma; nao impede a devolucao ao pool
            LOG.debug("Nao foi possivel limpar o escopo RLS ao devolver a conexao", e);
        }
    }

    private static void aplicar(Connection c, ContextoTenantBanco.Escopo e) {
        try {
            // Dentro de transacao (autocommit desligado): is_local=true, morre no commit/rollback.
            // Fora de transacao (leitura sem @Transactional, autocommit): is_local=true seria descartado no mesmo
            // instante, entao vale na sessao e e sobrescrito a cada aquisicao e zerado na devolucao.
            boolean local = !c.getAutoCommit();
            try (PreparedStatement ps = c.prepareStatement(SQL)) {
                ps.setString(1, e.empresaId());
                ps.setBoolean(2, local);
                ps.setString(3, e.irrestrito() ? "true" : "false");
                ps.setBoolean(4, local);
                ps.execute();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Falha ao aplicar o escopo de tenant (RLS) na conexao", ex);
        }
    }
}
