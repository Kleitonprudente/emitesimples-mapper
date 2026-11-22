package br.com.emitesimples.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/** Migracao V5: Pessoa ganha cliente/fornecedor (boolean not null default false) e indices por empresa. SQL como dono (ignora RLS). */
@QuarkusTest
class MigracaoV5PessoaTest extends BaseApiTest {

    private static Connection dono() throws SQLException {
        return DriverManager.getConnection(ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class),
                "dono_teste", "dono-teste-efemero");
    }

    private static String escalar(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) {
                ps.setObject(i + 1, p[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    @Test
    void colunasBooleanasObrigatoriasComDefaultFalseEIndices() throws SQLException {
        try (Connection c = dono()) {
            for (String col : new String[] {"cliente", "fornecedor"}) {
                String q = "select data_type || '/' || is_nullable || '/' || column_default from information_schema.columns "
                        + "where table_schema = current_schema() and table_name = 'pessoa' and column_name = ?";
                assertEquals("boolean/NO/false", escalar(c, q, col), col);
            }
            for (String ix : new String[] {"ix_pessoa_empresa_cliente", "ix_pessoa_empresa_fornecedor"}) {
                assertEquals(ix, escalar(c, "select indexname from pg_indexes where schemaname = current_schema() "
                        + "and tablename = 'pessoa' and indexname = ?", ix));
            }
        }
    }

    @Test
    void linhaInseridaSemOsValoresRecebeFalse() throws SQLException {
        String empresa = dados.empresaAtiva();
        String id = UUID.randomUUID().toString();
        try (Connection c = dono()) {
            try (PreparedStatement ps = c.prepareStatement("insert into pessoa (id, empresa_id, nome) values (?, ?, 'V5')")) {
                ps.setString(1, id);
                ps.setString(2, empresa);
                ps.executeUpdate();
            }
            assertEquals("false/false", escalar(c, "select cliente || '/' || fornecedor from pessoa where id = ?", id));
        }
        // pessoa criada pelo codigo de teste (entity regenerada, com cliente/fornecedor): o ajudante a cria como cliente
        String viaEntity = dados.pessoa(empresa, "V5 entity");
        try (Connection c = dono()) {
            assertEquals("true/false", escalar(c, "select cliente || '/' || fornecedor from pessoa where id = ?", viaEntity));
        }
    }
}
