package br.com.emitesimples.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/** Migracao V8: ImportacaoMunicipio guarda so o id do usuario (id_usuario, NOT NULL), sem FK para USUARIO. SQL como dono. */
@QuarkusTest
class MigracaoV8ImportacaoTest extends BaseApiTest {

    private static String escalar(String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection(ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class),
                "dono_teste", "dono-teste-efemero"); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    @Test
    void colunaIdUsuarioObrigatoriaSemFkNemColunaAntiga() throws SQLException {
        assertEquals("character varying/NO", escalar("select data_type || '/' || is_nullable from information_schema.columns "
                + "where table_schema = current_schema() and table_name = 'importacaomunicipio' and column_name = 'id_usuario'"));
        assertEquals(null, escalar("select 1 from information_schema.columns where table_schema = current_schema() "
                + "and table_name = 'importacaomunicipio' and column_name = 'usuario_id'"));
        assertEquals("0", escalar("select count(*) from information_schema.table_constraints where table_schema = current_schema() "
                + "and table_name = 'importacaomunicipio' and constraint_type = 'FOREIGN KEY'"));
        assertEquals("ix_importacaomunicipio_id_usuario", escalar("select indexname from pg_indexes where schemaname = current_schema() "
                + "and tablename = 'importacaomunicipio' and indexname = 'ix_importacaomunicipio_id_usuario'"));
    }
}
