package br.com.emitesimples.plataforma;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.security.ApiException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/** /api/admin/configuracoes/integranotas, IntegraNotasConfigService e a migracao V6. Nenhuma chamada ao IntegraNotas. */
@QuarkusTest
class ConfiguracaoIntegraNotasTest extends BaseApiTest {

    private static final String URL = "/api/admin/configuracoes/integranotas";
    private static final String HOM = "https://hom-api.integranotas.com.br";
    private static final String PROD = "https://api.integranotas.com.br";

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @Inject
    IntegraNotasConfigService config;

    private String admin;

    private String admin() {
        if (admin == null) {
            admin = token(dados.usuario(true, true).email());
        }
        return admin;
    }

    private Response put(Map<String, Object> corpo) {
        return chamar("PUT", URL, admin(), corpo);
    }

    private Response get() {
        return chamar("GET", URL, admin(), null);
    }

    private void reiniciar() {
        Map<String, Object> c = new HashMap<>();
        c.put("ambienteAtivo", "HOMOLOGACAO");
        c.put("urlHomologacao", HOM);
        c.put("urlProducao", PROD);
        c.put("urlPublicaWebhook", "");
        c.put("tokenHomologacao", "");
        c.put("tokenProducao", "");
        put(c).then().statusCode(204);
    }

    @SuppressWarnings("unchecked")
    private Object[] linhaSegredo(String coluna) {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<Object[]> l = em.createNativeQuery("select s.id, s.valor_cifrado, s.iv from segredo s "
                    + "join configuracao_integranotas c on c." + coluna + " = s.id").getResultList();
            return l.isEmpty() ? null : l.get(0);
        });
    }

    private long totalSegredos() {
        return ((Number) QuarkusTransaction.requiringNew().call(
                () -> em.createNativeQuery("select count(*) from segredo").getSingleResult())).longValue();
    }

    // ---------------------------------------------------------------------------------------------- acesso

    @Test
    void soAdministradorDaPlataforma() {
        String empresa = dados.empresaAtiva();
        assertEquals(401, status("GET", URL, null, null));
        assertEquals(401, status("PUT", URL, null, Map.of("ambienteAtivo", "HOMOLOGACAO")));
        for (String papel : new String[] {"CONSULTA", "OPERADOR", "ADMINISTRADOR"}) {
            String t = token(dados.usuarioComPapel(empresa, papel).email());
            assertEquals(403, status("GET", URL, t, null), papel);
            assertEquals(403, status("PUT", URL, t, Map.of("tokenHomologacao", "abc")), papel);
        }
        assertEquals(403, status("DELETE", URL, admin(), null));
        assertEquals(403, status("POST", URL, admin(), Map.of()));
        assertEquals(403, status("PATCH", URL, admin(), Map.of()));
        assertTrue(Arrays.asList(404, 403).contains(status("GET", "/api/ConfiguracaoIntegranotas", admin(), null)));
        assertTrue(Arrays.asList(404, 403).contains(status("GET", "/api/ConfiguracaoIntegraNotas", admin(), null)));
    }

    // ---------------------------------------------------------------------------------------------- leitura e gravacao

    @Test
    void leituraInicialTemPadroesSemTokens() {
        reiniciar();
        get().then().statusCode(200).body("ambienteAtivo", equalTo("HOMOLOGACAO")).body("urlHomologacao", equalTo(HOM))
                .body("urlProducao", equalTo(PROD)).body("urlPublicaWebhook", nullValue())
                .body("tokenHomologacaoConfigurado", equalTo(false)).body("tokenProducaoConfigurado", equalTo(false))
                .body("producaoPermitida", equalTo(false)).body("dataAtualizacao", notNullValue());
        assertEquals("HOMOLOGACAO", config.ambienteAtivo());
        assertEquals(HOM, config.urlBase("HOMOLOGACAO"));
        assertEquals(PROD, config.urlBase("PRODUCAO"));
        assertEquals(Optional.empty(), config.tokenSofthouse("HOMOLOGACAO"));
        assertEquals(Optional.empty(), config.tokenSofthouse("PRODUCAO"));
        assertEquals(Optional.empty(), config.urlPublicaWebhook());
        assertThrows(IllegalArgumentException.class, () -> config.urlBase("outro"));
    }

    @Test
    void gravaMantemSubstituiERemove() {
        reiniciar();
        String tokHom = "tok-HOM-SECRETO-" + System.nanoTime();
        String tokProd = "tok-PROD-SECRETO-" + System.nanoTime();
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        try {
            Map<String, Object> c = new HashMap<>();
            c.put("urlHomologacao", "https://stub.hom-api.integranotas.com.br/");
            c.put("urlProducao", "http://localhost:8099/prod/");
            c.put("urlPublicaWebhook", "https://api.exemplo.com.br/");
            c.put("tokenHomologacao", tokHom);
            c.put("tokenProducao", tokProd);
            put(c).then().statusCode(204);

            Response r = get();
            r.then().statusCode(200).body("urlHomologacao", equalTo("https://stub.hom-api.integranotas.com.br"))
                    .body("urlProducao", equalTo("http://localhost:8099/prod")) // barra final normalizada
                    .body("urlPublicaWebhook", equalTo("https://api.exemplo.com.br"))
                    .body("ambienteAtivo", equalTo("HOMOLOGACAO"))
                    .body("tokenHomologacaoConfigurado", equalTo(true)).body("tokenProducaoConfigurado", equalTo(true))
                    .body("atualizadoPor.nome", notNullValue()).body("atualizadoPor.id", notNullValue());
            assertFalse(r.asString().contains(tokHom) || r.asString().contains(tokProd), "a leitura nunca devolve token");

            // banco: so cifrado; o servico decifra
            Object[] lh = linhaSegredo("segredo_softhouse_hom_id");
            Object[] lp = linhaSegredo("segredo_softhouse_prod_id");
            assertFalse(new String((byte[]) lh[1], java.nio.charset.StandardCharsets.ISO_8859_1).contains(tokHom));
            assertEquals(12, ((byte[]) lh[2]).length);
            assertEquals(tokHom, segredos.ler((String) lh[0]));
            assertEquals(Optional.of(tokHom), config.tokenSofthouse("HOMOLOGACAO"));
            assertEquals(Optional.of(tokProd), config.tokenSofthouse("PRODUCAO"));
            assertEquals(Optional.of("https://api.exemplo.com.br"), config.urlPublicaWebhook());
            assertEquals("https://stub.hom-api.integranotas.com.br", config.urlBase("HOMOLOGACAO"));

            // campo ausente (e null) mantem tudo
            put(Map.of("urlPublicaWebhook", "https://outro.exemplo.com.br/app")).then().statusCode(204);
            HashMap<String, Object> nulos = new HashMap<>();
            nulos.put("tokenHomologacao", null);
            nulos.put("urlProducao", null);
            put(nulos).then().statusCode(204);
            get().then().body("tokenHomologacaoConfigurado", equalTo(true)).body("tokenProducaoConfigurado", equalTo(true))
                    .body("urlHomologacao", equalTo("https://stub.hom-api.integranotas.com.br"))
                    .body("urlProducao", equalTo("http://localhost:8099/prod"))
                    .body("urlPublicaWebhook", equalTo("https://outro.exemplo.com.br/app"));
            assertEquals(Optional.of(tokHom), config.tokenSofthouse("HOMOLOGACAO"));

            // substitui: mesmo id, IV novo
            String tokHom2 = "tok-HOM-OUTRO-" + System.nanoTime();
            put(Map.of("tokenHomologacao", tokHom2)).then().statusCode(204);
            Object[] depois = linhaSegredo("segredo_softhouse_hom_id");
            assertEquals(lh[0], depois[0]);
            assertFalse(Arrays.equals((byte[]) lh[2], (byte[]) depois[2]));
            assertEquals(Optional.of(tokHom2), config.tokenSofthouse("HOMOLOGACAO"));
            assertEquals(Optional.of(tokProd), config.tokenSofthouse("PRODUCAO")); // o outro ambiente intacto

            // "" remove so o do ambiente pedido e apaga o segredo
            long antes = totalSegredos();
            put(Map.of("tokenProducao", "")).then().statusCode(204);
            get().then().body("tokenProducaoConfigurado", equalTo(false)).body("tokenHomologacaoConfigurado", equalTo(true));
            assertEquals(antes - 1, totalSegredos());
            assertEquals(null, linhaSegredo("segredo_softhouse_prod_id"));
            assertEquals(lp[0] == null, false);
            assertEquals(Optional.empty(), config.tokenSofthouse("PRODUCAO"));

            put(Map.of("urlPublicaWebhook", "")).then().statusCode(204);
            get().then().body("urlPublicaWebhook", nullValue());
            assertEquals(Optional.empty(), config.urlPublicaWebhook());
        } finally {
            descapturar(h);
            reiniciar();
        }
        for (String l : logs) {
            assertFalse(l.contains("tok-HOM-") || l.contains("tok-PROD-"), "o token nao pode aparecer em log: " + l);
        }
    }

    // ---------------------------------------------------------------------------------------------- validacao

    @Test
    void producaoBloqueadaPorPadrao() {
        reiniciar();
        put(Map.of("tokenHomologacao", "abc")).then().statusCode(204);
        long antes = totalSegredos();
        // pedido misto: a producao bloqueada derruba o pedido inteiro, sem gravar nada
        Response r = put(Map.of("ambienteAtivo", "PRODUCAO", "tokenHomologacao", "novo", "urlPublicaWebhook", "https://a.exemplo.com.br"));
        assertEquals(409, r.statusCode());
        assertEquals("producao_bloqueada", r.jsonPath().getString("codigo"));
        assertEquals("Produção está bloqueada neste servidor.", r.jsonPath().getString("mensagem"));
        assertEquals(antes, totalSegredos());
        get().then().body("ambienteAtivo", equalTo("HOMOLOGACAO")).body("urlPublicaWebhook", nullValue());
        assertEquals(Optional.of("abc"), config.tokenSofthouse("HOMOLOGACAO"));
        // homologacao explicita continua valendo
        put(Map.of("ambienteAtivo", "HOMOLOGACAO")).then().statusCode(204);
        reiniciar();
    }

    @Test
    void ambienteInvalido() {
        reiniciar();
        for (String a : new String[] {"", "producao", "homologacao", "sandbox", "homologação"}) {
            Response r = put(Map.of("ambienteAtivo", a));
            assertEquals(400, r.statusCode(), a);
            assertEquals("ambiente_invalido", r.jsonPath().getString("codigo"), a);
        }
        assertEquals("ambiente_invalido", put(Map.of("ambienteAtivo", 5)).jsonPath().getString("codigo"));
    }

    @Test
    void producaoLiberadaPorPropriedadeEAceita() {
        IntegraNotasConfigService s = new IntegraNotasConfigService();
        s.permitirProducao = true;
        assertEquals("PRODUCAO", s.validarAmbiente("PRODUCAO"));
        s.permitirProducao = false;
        assertEquals(409, assertThrows(ApiException.class, () -> s.validarAmbiente("PRODUCAO")).getStatus());
    }

    @Test
    void urlDaApiInvalidaDevolve400() {
        reiniciar();
        String[] ruins = {
            "http://hom-api.integranotas.com.br", // http fora do host local
            "https://api.outrodominio.com.br", // outro dominio
            "https://integranotas.com.br.evil.com", // sufixo falso
            "https://evilintegranotas.com.br", // sem ponto antes do dominio
            "https://user:senha@hom-api.integranotas.com.br", "https://user@hom-api.integranotas.com.br",
            "https://hom-api.integranotas.com.br/x?a=1", "https://hom-api.integranotas.com.br/x#a",
            "https://hom-api.integranotas.com.br//", "https://10.0.0.5", "https://169.254.169.254/latest",
            "ftp://hom-api.integranotas.com.br", "hom-api.integranotas.com.br", "nao e url", "",
            "https://hom-api.integranotas.com.br/" + "a".repeat(200)
        };
        for (String u : ruins) {
            for (String campo : new String[] {"urlHomologacao", "urlProducao"}) {
                Response r = put(Map.of(campo, u));
                assertEquals(400, r.statusCode(), campo + " " + u);
                assertEquals("url_invalida", r.jsonPath().getString("codigo"), campo + " " + u);
            }
        }
        assertEquals("url_invalida", put(Map.of("urlHomologacao", 7)).jsonPath().getString("codigo"));
        get().then().body("urlHomologacao", equalTo(HOM)).body("urlProducao", equalTo(PROD));
    }

    @Test
    void urlDaApiEmProducaoDasRegrasNaoAceitaLocal() {
        IntegraNotasConfigService s = new IntegraNotasConfigService();
        s.permitirUrlLocal = false;
        for (String u : new String[] {"http://localhost/x", "https://localhost/x", "https://127.0.0.1/x", "https://[::1]/x"}) {
            assertEquals("url_invalida", assertThrows(ApiException.class, () -> s.validarUrlApi(u), u).getCodigo(), u);
        }
        assertEquals(HOM, s.validarUrlApi(HOM + "/"));
        assertEquals("https://API.integranotas.com.br", s.validarUrlApi("https://API.integranotas.com.br")); // caixa aceita
        s.permitirUrlLocal = true;
        assertEquals("http://localhost:8099/x", s.validarUrlApi("http://localhost:8099/x/"));
        assertThrows(ApiException.class, () -> s.validarUrlApi("http://10.0.0.1/x"));
    }

    @Test
    void urlPublicaDoWebhookInvalidaDevolve400() {
        reiniciar();
        for (String u : new String[] {"http://api.exemplo.com.br", "https://localhost", "https://127.0.0.1", "https://10.1.2.3",
                "https://192.168.0.9", "https://[fd00::1]", "https://intranet", "https://servidor.local", "https://u:p@api.exemplo.com.br",
                "https://api.exemplo.com.br?x=1", "https://api.exemplo.com.br/#a", "nao e url",
                "https://api.exemplo.com.br/" + "a".repeat(200)}) {
            Response r = put(Map.of("urlPublicaWebhook", u));
            assertEquals(400, r.statusCode(), u);
            assertEquals("url_invalida", r.jsonPath().getString("codigo"), u);
        }
        get().then().body("urlPublicaWebhook", nullValue());
        // regras em "modo producao" (unidade): localhost nunca vale para o webhook
        IntegraNotasConfigService s = new IntegraNotasConfigService();
        s.permitirUrlLocal = true;
        for (String u : new String[] {"https://localhost", "https://127.0.0.1:8080", "https://172.16.0.1", "https://100.64.0.1"}) {
            assertEquals("url_invalida", assertThrows(ApiException.class, () -> s.validarUrlPublica(u), u).getCodigo(), u);
        }
        assertEquals("https://api.exemplo.com.br", s.validarUrlPublica("https://api.exemplo.com.br/"));
    }

    @Test
    void tokenInvalidoENuncaVaza() {
        reiniciar();
        for (String t : new String[] {"com espaco", "com\nquebra", "tab\taqui", "a".repeat(2001)}) {
            for (String campo : new String[] {"tokenHomologacao", "tokenProducao"}) {
                Response r = put(Map.of(campo, t));
                assertEquals(400, r.statusCode());
                assertEquals("token_invalido", r.jsonPath().getString("codigo"));
                assertFalse(r.asString().contains("com espaco"));
            }
        }
        assertEquals("token_invalido", put(Map.of("tokenHomologacao", 12)).jsonPath().getString("codigo"));
        put(Map.of("tokenHomologacao", "a".repeat(2000))).then().statusCode(204);
        reiniciar();
        // corpo invalido nao ecoa o conteudo
        Response lixo = chamar("PUT", URL, admin(), "{tokenHomologacao: segredo-que-nao-pode-vazar");
        assertEquals(400, lixo.statusCode());
        assertFalse(lixo.asString().contains("segredo-que-nao-pode-vazar"));
        assertEquals(400, chamar("PUT", URL, admin(), "[1]").statusCode());
    }

    // ---------------------------------------------------------------------------------------------- migracao V6

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

    private static String coluna(Connection c, String tabela, String col) throws SQLException {
        return escalar(c, "select data_type || '/' || is_nullable || '/' || coalesce(column_default, '-') || '/' "
                + "|| coalesce(character_maximum_length::text, coalesce(numeric_precision::text || ',' || numeric_scale::text, '')) "
                + "from information_schema.columns where table_schema = current_schema() and table_name = ? and column_name = ?",
                tabela, col);
    }

    @Test
    void migracaoV6() throws SQLException {
        try (Connection c = dono()) {
            assertEquals("boolean/NO/false/", coluna(c, "empresa", "nfse_habilitada"));
            assertEquals("character varying/YES/-/5", coluna(c, "empresa", "serie_nfse"));
            assertEquals("integer/NO/0/32,0", coluna(c, "empresa", "ultimo_numero_nfse"));
            assertEquals("numeric/YES/-/5,2", coluna(c, "servico", "percentual_tributos_simples"));
            assertEquals("character varying/NO/'HOMOLOGACAO'::character varying/15", coluna(c, "configuracao_integranotas", "ambiente_ativo"));
            assertEquals("character varying/YES/-/200", coluna(c, "configuracao_integranotas", "url_publica_webhook"));
            assertEquals("character varying/YES/-/36", coluna(c, "configuracao_integranotas", "segredo_softhouse_hom_id"));
            assertEquals("character varying/YES/-/36", coluna(c, "configuracao_integranotas", "segredo_softhouse_prod_id"));
            assertEquals("character varying/YES/-/36", coluna(c, "configuracao_integranotas", "atualizado_por"));
            assertEquals("1", escalar(c, "select count(*) from configuracao_integranotas"));
            assertEquals("unica", escalar(c, "select id from configuracao_integranotas"));
            // defaults da linha inicial (nao dependem do estado dos outros testes)
            assertEquals("https://hom-api.integranotas.com.br/https://api.integranotas.com.br", escalar(c,
                    "select column_default from information_schema.columns where table_name = 'configuracao_integranotas' "
                            + "and column_name = 'url_homologacao'").replace("'", "").replace("::character varying", "")
                    + "/" + escalar(c, "select column_default from information_schema.columns where table_name = 'configuracao_integranotas' "
                            + "and column_name = 'url_producao'").replace("'", "").replace("::character varying", ""));
            // checks: id unico e ambiente restrito
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate(
                    "insert into configuracao_integranotas (id, data_atualizacao) values ('outra', now())"));
            assertThrows(SQLException.class, () -> c.createStatement().executeUpdate(
                    "update configuracao_integranotas set ambiente_ativo = 'sandbox'"));
        }
        // empresa criada sem as colunas novas recebe os defaults
        String empresa = dados.empresaAtiva();
        try (Connection c = dono()) {
            assertEquals("false/null/0", escalar(c,
                    "select nfse_habilitada || '/' || coalesce(serie_nfse, 'null') || '/' || ultimo_numero_nfse from empresa where id = ?",
                    empresa));
        }
    }

    // ---------------------------------------------------------------------------------------------- logs

    private static Handler capturarLogs(List<String> destino) {
        Handler h = new Handler() {
            @Override
            public void publish(LogRecord r) {
                StringBuilder sb = new StringBuilder(String.valueOf(r.getMessage()));
                if (r.getParameters() != null) {
                    sb.append(Arrays.toString(r.getParameters()));
                }
                for (Throwable t = r.getThrown(); t != null; t = t.getCause()) {
                    sb.append(' ').append(t);
                }
                synchronized (destino) {
                    destino.add(sb.toString());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        LogManager.getLogManager().getLogger("").addHandler(h);
        return h;
    }

    private static void descapturar(Handler h) {
        LogManager.getLogManager().getLogger("").removeHandler(h);
    }
}
