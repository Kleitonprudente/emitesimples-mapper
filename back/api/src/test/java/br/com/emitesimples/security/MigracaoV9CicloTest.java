package br.com.emitesimples.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/**
 * Migracao V9: ciclo da Nota e do Cancelamento. CHECK ck_nota_numeracao, colunas novas, recusado_por_id removida,
 * pode_cancelar_nfse NOT NULL default false e os UPDATEs de estado (executados aqui a partir do proprio arquivo da migracao).
 * SQL como dono.
 */
@QuarkusTest
class MigracaoV9CicloTest extends BaseApiTest {

    private static Connection dono() throws SQLException {
        return DriverManager.getConnection(ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class),
                "dono_teste", "dono-teste-efemero");
    }

    private static String escalar(String sql) throws SQLException {
        try (Connection c = dono(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static void executar(String sql) throws SQLException {
        try (Connection c = dono(); Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
    }

    private static String lit(String s) {
        return s == null ? "null" : "'" + s.replace("'", "''") + "'";
    }

    private String notaComEstado(String estado, String numero, String serie, boolean comData) throws SQLException {
        String emp = dados.empresaAtiva();
        String pessoa = dados.pessoa(emp, "Tomador V9");
        String servico = dados.servico(emp, "Servico V9");
        String id = UUID.randomUUID().toString();
        executar("insert into nota (id, estado, numero, serie, ambiente, descricao, empresa_id, pessoa_id, servico_id, "
                + "data_emissao) values ('" + id + "', " + lit(estado) + ", " + lit(numero) + ", " + lit(serie)
                + ", 'homologacao', 'n', '" + emp + "', '" + pessoa + "', '" + servico + "', "
                + (comData ? "now()" : "null") + ")");
        return id;
    }

    @Test
    void checkDeNumeracaoSoIsentaORascunhoEADescartada() throws SQLException {
        assertTrue(escalar("select pg_get_constraintdef(oid) from pg_constraint where conname = 'ck_nota_numeracao'")
                .contains("rascunho"));
        notaComEstado("rascunho", null, null, false); // o rascunho do assistente nao tem numeracao
        notaComEstado("descartada", null, null, false); // nem a nota descartada (nunca foi enviada)
        notaComEstado("processando", "10", "1", true);
        for (String estado : new String[] {"processando", "falha_envio", "rejeitada", "autorizada"}) {
            assertThrows(SQLException.class, () -> notaComEstado(estado, null, "1", true), estado + " sem numero");
            assertThrows(SQLException.class, () -> notaComEstado(estado, "11", null, true), estado + " sem serie");
            assertThrows(SQLException.class, () -> notaComEstado(estado, "12", "1", false), estado + " sem data de emissao");
        }
    }

    @Test
    void colunasNovasERecusadoPorRemovida() throws SQLException {
        assertEquals("timestamp without time zone/YES", escalar("select data_type || '/' || is_nullable from information_schema.columns "
                + "where table_schema = current_schema() and table_name = 'cancelamento' and column_name = 'data_hora_descarte'"));
        assertEquals("character varying/YES", escalar("select data_type || '/' || is_nullable from information_schema.columns "
                + "where table_schema = current_schema() and table_name = 'cancelamento' and column_name = 'descartado_por_id'"));
        assertEquals(null, escalar("select 1 from information_schema.columns where table_schema = current_schema() "
                + "and table_name = 'cancelamento' and column_name = 'recusado_por_id'"));
        assertEquals("1", escalar("select count(*) from pg_constraint where conname = 'fk_cancelamento_descartado_por'"));
        assertEquals("ix_cancelamento_descartado_por", escalar("select indexname from pg_indexes where schemaname = current_schema() "
                + "and tablename = 'cancelamento' and indexname = 'ix_cancelamento_descartado_por'"));
    }

    @Test
    void podeCancelarNfseObrigatorioComPadraoFalse() throws SQLException {
        assertEquals("boolean/NO/false", escalar("select data_type || '/' || is_nullable || '/' || column_default from "
                + "information_schema.columns where table_schema = current_schema() and table_name = 'vinculousuario' "
                + "and column_name = 'pode_cancelar_nfse'"));
        // insercao sem a coluna recebe false; null explicito e barrado
        String emp = dados.empresaAtiva();
        var u = dados.usuario(true, false);
        String vid = UUID.randomUUID().toString();
        executar("insert into vinculousuario (id, papel, ativo, usuario_id, empresa_id) values ('" + vid
                + "', 'operador', true, '" + u.id() + "', '" + emp + "')");
        assertEquals("f", escalar("select pode_cancelar_nfse from vinculousuario where id = '" + vid + "'").substring(0, 1));
        assertThrows(SQLException.class, () -> executar("update vinculousuario set pode_cancelar_nfse = null where id = '" + vid + "'"));
    }

    @Test
    void updatesDeEstadoDaMigracao() throws Exception {
        String sql;
        try (var in = getClass().getClassLoader().getResourceAsStream("db/migration/V9__ciclo_nota_e_cancelamento.sql")) {
            assertNotNull(in);
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        List<String> updates = new ArrayList<>();
        for (String linha : sql.split("\n")) {
            if (linha.startsWith("UPDATE ")) {
                updates.add(linha.strip().replaceAll(";\\s*(--.*)?$", ""));
            }
        }
        assertEquals(6, updates.size()); // 1 de NOTA + 5 de CANCELAMENTO
        String emp = dados.empresaAtiva();
        // numero ja alocado em 'rascunho' (comportamento antigo) vira falha_envio; rascunho sem numero (assistente) fica
        String nota = notaComEstado("rascunho", "77", "1", true);
        String notaSemNumero = notaComEstado("rascunho", null, null, false);
        String msgExpirado = "O IntegraNotas não confirmou o cancelamento. Verifique a nota e solicite o cancelamento novamente.";
        String[][] casos = {{"aguardando_aprovacao", null, "rascunho"}, {"solicitado", null, "processando"},
                {"cancelado", null, "concluido"}, {"rejeitado", null, "recusado"}, {"recusado", msgExpirado, "expirado"},
                {"recusado", "O gateway recusou.", "recusado"}};
        List<String> ids = new ArrayList<>();
        for (String[] c : casos) {
            String id = UUID.randomUUID().toString();
            ids.add(id);
            executar("insert into cancelamento (id, estado, codigo_motivo, motivo, ambiente, empresa_id, nota_id, mensagem_retorno) "
                    + "values ('" + id + "', " + lit(c[0]) + ", 1, 'motivo de teste qualquer', 'homologacao', '" + emp + "', '"
                    + nota + "', " + lit(c[1]) + ")");
        }
        for (String u : updates) {
            executar(u);
        }
        for (int i = 0; i < casos.length; i++) {
            assertEquals(casos[i][2], escalar("select estado from cancelamento where id = '" + ids.get(i) + "'"), casos[i][0]);
        }
        assertEquals("falha_envio", escalar("select estado from nota where id = '" + nota + "'"));
        assertEquals("rascunho", escalar("select estado from nota where id = '" + notaSemNumero + "'"));
    }
}
