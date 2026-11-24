package br.com.emitesimples.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
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
 * Migracao V10: enums de TEXTO em MAIUSCULAS (secao 9.7). Depois de migrado nao ha como inserir linha "pre-V10" que o CHECK novo
 * aceite, entao os UPDATEs sao lidos do proprio arquivo da migracao e executados aqui sobre linhas com os valores ANTIGOS
 * (inseridas como dono, nas tabelas sem CHECK); o CHECK e o indice parcial recriados sao provados por insercao. SQL como dono.
 */
@QuarkusTest
class MigracaoV10EnumsTest extends BaseApiTest {

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

    private static String id() {
        return UUID.randomUUID().toString();
    }

    // uma empresa/pessoa/servico so para a classe: a listagem da plataforma e paginada e outros testes (RlsTest) leem a primeira pagina
    private static String empNota;
    private static String pessoaNota;
    private static String servicoNota;

    private String notaComEstado(String estado, String numero, String serie, boolean comData) throws SQLException {
        if (empNota == null) {
            empNota = dados.empresaAtiva();
            pessoaNota = dados.pessoa(empNota, "Tomador V10");
            servicoNota = dados.servico(empNota, "Servico V10");
        }
        String emp = empNota;
        String pessoa = pessoaNota;
        String servico = servicoNota;
        String id = id();
        executar("insert into nota (id, estado, numero, serie, ambiente, descricao, empresa_id, pessoa_id, servico_id, "
                + "data_emissao) values ('" + id + "', " + lit(estado) + ", " + lit(numero) + ", " + lit(serie)
                + ", 'HOMOLOGACAO', 'n', '" + emp + "', '" + pessoa + "', '" + servico + "', "
                + (comData ? "now()" : "null") + ")");
        return id;
    }

    @Test
    void checkDeNumeracaoRecriadoComValoresMaiusculos() throws SQLException {
        String def = escalar("select pg_get_constraintdef(oid) from pg_constraint where conname = 'ck_nota_numeracao'");
        assertNotNull(def);
        assertTrue(def.contains("'RASCUNHO'") && def.contains("'DESCARTADA'"), def);
        assertFalse(def.contains("'rascunho'") || def.contains("'descartada'"), def);
        notaComEstado("RASCUNHO", null, null, false); // rascunho do assistente: sem numeracao
        notaComEstado("DESCARTADA", null, null, false); // nota descartada: nunca foi enviada
        notaComEstado("PROCESSANDO", "10", "1", true);
        for (String estado : new String[] {"PROCESSANDO", "FALHA_ENVIO", "REJEITADA", "AUTORIZADA", "EMITIDA_OFFLINE"}) {
            assertThrows(SQLException.class, () -> notaComEstado(estado, null, "1", true), estado + " sem numero");
            assertThrows(SQLException.class, () -> notaComEstado(estado, "11", null, true), estado + " sem serie");
            assertThrows(SQLException.class, () -> notaComEstado(estado, "12", "1", false), estado + " sem data de emissao");
        }
        // o CHECK novo so isenta os valores NOVOS: 'rascunho' em minusculas ja nao e isento
        assertThrows(SQLException.class, () -> notaComEstado("rascunho", null, null, false));
        assertThrows(SQLException.class, () -> notaComEstado("descartada", null, null, false));
    }

    @Test
    void indiceDeAssinaturaVigenteUsaValoresMaiusculos() throws SQLException {
        String def = escalar("select indexdef from pg_indexes where schemaname = current_schema() "
                + "and indexname = 'ux_assinaturaempresa_vigente'");
        assertNotNull(def);
        assertTrue(def.contains("'ATIVA'") && def.contains("'SUSPENSA'"), def);
        String emp = dados.empresaAtiva();
        String plano = id();
        executar("insert into plano (id, nome) values ('" + plano + "', 'Plano V10')");
        executar("insert into assinaturaempresa (id, estado, empresa_id, plano_id) values ('" + id() + "', 'ATIVA', '" + emp
                + "', '" + plano + "')");
        // vigente (ATIVA ou SUSPENSA) e unica por empresa; CANCELADA nao conta
        assertThrows(SQLException.class, () -> executar("insert into assinaturaempresa (id, estado, empresa_id, plano_id) values ('"
                + id() + "', 'SUSPENSA', '" + emp + "', '" + plano + "')"));
        executar("insert into assinaturaempresa (id, estado, empresa_id, plano_id) values ('" + id() + "', 'CANCELADA', '" + emp
                + "', '" + plano + "')");
    }

    @Test
    void configuracaoIntegraNotasAceitaSoAmbienteMaiusculo() throws SQLException {
        assertEquals("'HOMOLOGACAO'::character varying", escalar("select column_default from information_schema.columns "
                + "where table_schema = current_schema() and table_name = 'configuracao_integranotas' "
                + "and column_name = 'ambiente_ativo'"));
        assertThrows(SQLException.class, () -> executar("update configuracao_integranotas set ambiente_ativo = 'homologacao'"));
        assertThrows(SQLException.class, () -> executar("update configuracao_integranotas set ambiente_ativo = 'producao'"));
        assertThrows(SQLException.class, () -> executar("update configuracao_integranotas set ambiente_ativo = 'OUTRO'"));
        String atual = escalar("select ambiente_ativo from configuracao_integranotas where id = 'unica'");
        assertTrue(List.of("HOMOLOGACAO", "PRODUCAO").contains(atual), atual);
        executar("update configuracao_integranotas set ambiente_ativo = ambiente_ativo where id = 'unica'");
    }

    @Test
    void updatesDaMigracaoConvertemValoresAntigos() throws Exception {
        String sql;
        try (var in = getClass().getClassLoader().getResourceAsStream("db/migration/V10__enums_em_maiusculas.sql")) {
            assertNotNull(in);
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        // comandos sem comentarios de linha, separados por ';' (nao ha ';' dentro de literais nesta migracao)
        StringBuilder limpo = new StringBuilder();
        for (String linha : sql.split("\n")) {
            if (!linha.stripLeading().startsWith("--")) {
                limpo.append(linha).append('\n');
            }
        }
        List<String> updates = new ArrayList<>();
        for (String cmd : limpo.toString().split(";")) {
            if (cmd.strip().startsWith("UPDATE ")) {
                updates.add(cmd.strip());
            }
        }
        assertEquals(16, updates.size()); // 15 tabelas de dominio + configuracao_integranotas

        // linhas com os valores ANTIGOS (camelCase do mapa antigo ou snake_case/minusculo gravado pelo codigo)
        String emp = dados.empresa("pendenteConfirmacao");
        executar("update empresa set status_cadastro = 'naoCadastrado', situacao_certificado = 'nao_pertence', "
                + "ambiente = 'homologacao' where id = '" + emp + "'");
        String hist = id();
        executar("insert into historicosituacaoempresa (id, empresa_id, situacao_anterior, situacao_nova, origem) values ('" + hist
                + "', '" + emp + "', 'pendenteConfirmacao', 'ativa', 'confirmacaoEmail')");
        String plano = id();
        executar("insert into plano (id, nome, periodicidade) values ('" + plano + "', 'Plano antigo', 'mensal')");
        String assin = id();
        executar("insert into assinaturaempresa (id, estado, empresa_id, plano_id) values ('" + assin + "', 'suspensa', '" + emp
                + "', '" + plano + "')");
        String fat = id();
        executar("insert into faturacrm (id, estado, empresa_id) values ('" + fat + "', 'vencida', '" + emp + "')");
        var usuario = dados.usuario(true, false);
        String imp = id();
        executar("insert into importacaomunicipio (id, estado, id_usuario) values ('" + imp + "', 'concluida_com_rejeicoes', '"
                + usuario.id() + "')");
        String pessoa = dados.pessoa(emp, "Pessoa V10");
        executar("update pessoa set tipo_pessoa = 'juridica' where id = '" + pessoa + "'");
        String ativ = id();
        executar("insert into atividadeempresa (id, tipo, empresa_id) values ('" + ativ + "', 'secundaria', '" + emp + "')");
        String contato = id();
        executar("insert into contato (id, tipo_contato, empresa_id, pessoa_id) values ('" + contato + "', 'financeiro', '" + emp
                + "', '" + pessoa + "')");
        String conf = id();
        executar("insert into confirmacaocadastro (id, estado, empresa_id, usuario_id) values ('" + conf + "', 'confirmado', '"
                + emp + "', '" + usuario.id() + "')");
        String vinc = id();
        executar("insert into vinculousuario (id, papel, ativo, usuario_id, empresa_id) values ('" + vinc + "', 'operador', true, '"
                + usuario.id() + "', '" + emp + "')");
        String servico = dados.servico(emp, "Servico V10 antigo");
        String nota = id();
        executar("insert into nota (id, estado, numero, serie, ambiente, descricao, empresa_id, pessoa_id, servico_id, data_emissao) "
                + "values ('" + nota + "', 'emitida_offline', '5', '1', 'producao', 'n', '" + emp + "', '" + pessoa + "', '"
                + servico + "', now())");
        String canc = id();
        executar("insert into cancelamento (id, estado, codigo_motivo, motivo, ambiente, empresa_id, nota_id) values ('" + canc
                + "', 'concluido', 1, 'motivo de teste qualquer', 'homologacao', '" + emp + "', '" + nota + "')");
        String[] chamadas = {id(), id(), id(), id()};
        String[][] ch = {{"enviar_nfse", "v2", "post"}, {"consultar_nfse", "v1", "get"}, {"cancelar_nfse", "v1", "post"},
                {"cadastrarEmitente", "v1", "post"}};
        for (int i = 0; i < ch.length; i++) {
            executar("insert into chamadagateway (id, empresa_id, operacao, versao_api, metodo_http, ambiente) values ('"
                    + chamadas[i] + "', '" + emp + "', " + lit(ch[i][0]) + ", " + lit(ch[i][1]) + ", " + lit(ch[i][2])
                    + ", 'homologacao')");
        }
        String ev = id();
        executar("insert into eventowebhook (id, empresa_id, ambiente, estado_processamento, resultado_assinatura) values ('" + ev
                + "', '" + emp + "', 'producao', 'comErro', 'invalida')");

        for (String u : updates) {
            executar(u);
        }

        assertEquals("PENDENTE_CONFIRMACAO/NAO_CADASTRADO/NAO_PERTENCE/HOMOLOGACAO", escalar("select situacao || '/' || status_cadastro || '/' "
                + "|| situacao_certificado || '/' || ambiente from empresa where id = '" + emp + "'"));
        assertEquals("PENDENTE_CONFIRMACAO/ATIVA/CONFIRMACAO_EMAIL", escalar("select situacao_anterior || '/' || situacao_nova || '/' "
                + "|| origem from historicosituacaoempresa where id = '" + hist + "'"));
        assertEquals("MENSAL", escalar("select periodicidade from plano where id = '" + plano + "'"));
        assertEquals("SUSPENSA", escalar("select estado from assinaturaempresa where id = '" + assin + "'"));
        assertEquals("VENCIDA", escalar("select estado from faturacrm where id = '" + fat + "'"));
        assertEquals("CONCLUIDA_COM_REJEICOES", escalar("select estado from importacaomunicipio where id = '" + imp + "'"));
        assertEquals("JURIDICA", escalar("select tipo_pessoa from pessoa where id = '" + pessoa + "'"));
        assertEquals("SECUNDARIA", escalar("select tipo from atividadeempresa where id = '" + ativ + "'"));
        assertEquals("FINANCEIRO", escalar("select tipo_contato from contato where id = '" + contato + "'"));
        assertEquals("CONFIRMADO", escalar("select estado from confirmacaocadastro where id = '" + conf + "'"));
        assertEquals("OPERADOR", escalar("select papel from vinculousuario where id = '" + vinc + "'"));
        assertEquals("EMITIDA_OFFLINE/PRODUCAO", escalar("select estado || '/' || ambiente from nota where id = '" + nota + "'"));
        assertEquals("CONCLUIDO/HOMOLOGACAO", escalar("select estado || '/' || ambiente from cancelamento where id = '" + canc + "'"));
        String[] esperado = {"EMITIR/V2/POST", "CONSULTAR/V1/GET", "CANCELAR/V1/POST", "CADASTRAR_EMITENTE/V1/POST"};
        for (int i = 0; i < ch.length; i++) {
            assertEquals(esperado[i] + "/HOMOLOGACAO", escalar("select operacao || '/' || versao_api || '/' || metodo_http || '/' "
                    + "|| ambiente from chamadagateway where id = '" + chamadas[i] + "'"));
        }
        assertEquals("PRODUCAO/COM_ERRO/INVALIDA", escalar("select ambiente || '/' || estado_processamento || '/' "
                + "|| resultado_assinatura from eventowebhook where id = '" + ev + "'"));

        // idempotente: rodar de novo nao muda nada (valores ja maiusculos)
        for (String u : updates) {
            executar(u);
        }
        assertEquals("CADASTRAR_EMITENTE/V1/POST/HOMOLOGACAO", escalar("select operacao || '/' || versao_api || '/' || metodo_http "
                + "|| '/' || ambiente from chamadagateway where id = '" + chamadas[3] + "'"));
    }
}
