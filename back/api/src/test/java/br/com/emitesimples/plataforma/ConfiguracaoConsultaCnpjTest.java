package br.com.emitesimples.plataforma;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.security.ApiException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;

/** /api/admin/configuracoes/consulta-cnpj e o cofre de segredos. */
@QuarkusTest
class ConfiguracaoConsultaCnpjTest extends BaseApiTest {

    private static final String URL = "/api/admin/configuracoes/consulta-cnpj";
    private static final String BASE = "https://comercial.cnpj.ws/cnpj";

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

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
        put(Map.of("urlBase", BASE, "token", "")).then().statusCode(204);
    }

    @SuppressWarnings("unchecked")
    private Object[] linhaSegredo() {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<Object[]> l = em.createNativeQuery("select s.id, s.valor_cifrado, s.iv from segredo s "
                    + "join configuracao_consulta_cnpj c on c.segredo_token_id = s.id").getResultList();
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
        assertEquals(401, status("PUT", URL, null, Map.of("urlBase", BASE)));
        for (String papel : new String[] {"consulta", "operador", "administrador"}) {
            String t = token(dados.usuarioComPapel(empresa, papel).email());
            assertEquals(403, status("GET", URL, t, null), papel);
            assertEquals(403, status("PUT", URL, t, Map.of("urlBase", BASE, "token", "abc")), papel);
        }
        // metodos que nao existem seguem negados, ate para a plataforma
        assertEquals(403, status("DELETE", URL, admin(), null));
        assertEquals(403, status("POST", URL, admin(), Map.of()));
        assertEquals(403, status("PATCH", URL, admin(), Map.of()));
        assertEquals(403, status("DELETE", "/api/admin/configuracoes/outra", admin(), null));
        // a tabela nao e exposta por recurso gerado
        assertTrue(Arrays.asList(404, 403).contains(status("GET", "/api/Segredo", admin(), null)));
        assertTrue(Arrays.asList(404, 403).contains(status("GET", "/api/ConfiguracaoConsultaCnpj", admin(), null)));
    }

    // ---------------------------------------------------------------------------------------------- leitura e gravacao

    @Test
    void leGravaSubstituiMantemERemove() {
        reiniciar();
        get().then().statusCode(200).body("urlBase", equalTo(BASE)).body("tokenConfigurado", equalTo(false));

        String token = "tok-SECRETO-" + System.nanoTime();
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        try {
            put(Map.of("urlBase", "https://api.exemplo.com.br/cnpj/", "token", token)).then().statusCode(204);
            Response r = get();
            r.then().statusCode(200).body("urlBase", equalTo("https://api.exemplo.com.br/cnpj")) // barra final normalizada
                    .body("tokenConfigurado", equalTo(true)).body("dataAtualizacao", notNullValue())
                    .body("atualizadoPor.nome", notNullValue()).body("atualizadoPor.id", notNullValue());
            assertFalse(r.asString().contains(token), "a leitura nunca devolve o token");
            assertFalse(r.asString().toLowerCase().contains("segredo"));

            // banco: cifrado, nao em texto puro; decifrar pelo servico devolve o token
            Object[] linha = linhaSegredo();
            byte[] cifrado = (byte[]) linha[1];
            assertNotEquals(token, new String(cifrado, java.nio.charset.StandardCharsets.UTF_8));
            assertFalse(new String(cifrado, java.nio.charset.StandardCharsets.ISO_8859_1).contains(token));
            assertEquals(12, ((byte[]) linha[2]).length);
            assertEquals(token, segredos.ler((String) linha[0]));

            // substitui: mesmo id de segredo, valor e IV novos
            String idAntes = (String) linha[0];
            byte[] ivAntes = (byte[]) linha[2];
            String token2 = "tok-OUTRO-" + System.nanoTime();
            put(Map.of("urlBase", BASE, "token", token2)).then().statusCode(204);
            Object[] depois = linhaSegredo();
            assertEquals(idAntes, depois[0]);
            assertFalse(Arrays.equals(ivAntes, (byte[]) depois[2]), "IV novo a cada gravacao");
            assertEquals(token2, segredos.ler(idAntes));

            // token ausente (ou null) mantem
            put(Map.of("urlBase", "https://outra.exemplo.com.br/x")).then().statusCode(204);
            get().then().body("urlBase", equalTo("https://outra.exemplo.com.br/x")).body("tokenConfigurado", equalTo(true));
            assertEquals(token2, segredos.ler(idAntes));
            java.util.HashMap<String, Object> comNulo = new java.util.HashMap<>();
            comNulo.put("urlBase", BASE);
            comNulo.put("token", null);
            put(comNulo).then().statusCode(204);
            get().then().body("tokenConfigurado", equalTo(true));

            // "" remove o token e apaga o segredo
            long antes = totalSegredos();
            put(Map.of("urlBase", BASE, "token", "")).then().statusCode(204);
            get().then().body("tokenConfigurado", equalTo(false)).body("urlBase", equalTo(BASE));
            assertEquals(antes - 1, totalSegredos());
            assertEquals(null, linhaSegredo());
        } finally {
            descapturar(h);
        }
        for (String l : logs) {
            assertFalse(l.contains(token) || l.contains("tok-OUTRO-"), "o token nao pode aparecer em log: " + l);
        }
    }

    @Test
    void gravaComTokenSoNaPrimeiraVezSemSegredoPrevio() {
        reiniciar();
        long antes = totalSegredos();
        put(Map.of("urlBase", BASE, "token", "abc123")).then().statusCode(204);
        assertEquals(antes + 1, totalSegredos());
        reiniciar();
    }

    // ---------------------------------------------------------------------------------------------- validacao

    @Test
    void urlInvalidaDevolve400() {
        reiniciar();
        String[] ruins = {
            "http://api.exemplo.com.br/cnpj", // http fora do host local
            "https://user:senha@api.exemplo.com.br/cnpj", // userinfo
            "https://user@api.exemplo.com.br/cnpj",
            "https://api.exemplo.com.br/cnpj?x=1", // query
            "https://api.exemplo.com.br/cnpj#a",
            "https://api.exemplo.com.br/cnpj//", // barra final dupla
            "https://10.0.0.5/cnpj", "https://192.168.1.10/cnpj", "https://172.16.0.1/cnpj", "https://169.254.169.254/latest",
            "https://[fd00::1]/cnpj", "https://2130706433/cnpj",
            "https://0x7f000001/cnpj", "https://intranet/cnpj", "https://servidor.local/cnpj", "https://api.internal/cnpj",
            "ftp://api.exemplo.com.br/cnpj", "api.exemplo.com.br/cnpj", "https:///cnpj", "nao e url", "",
            "https://api.exemplo.com.br/" + "a".repeat(200)
        };
        for (String u : ruins) {
            Response r = put(Map.of("urlBase", u));
            assertEquals(400, r.statusCode(), u);
            assertEquals("url_invalida", r.jsonPath().getString("codigo"), u);
        }
        // nada mudou
        get().then().body("urlBase", equalTo(BASE));
        // sem urlBase / corpo invalido
        assertEquals(400, status("PUT", URL, admin(), Map.of("token", "x")));
        Response lixo = chamar("PUT", URL, admin(), "{token: segredo-que-nao-pode-vazar");
        assertEquals(400, lixo.statusCode());
        assertFalse(lixo.asString().contains("segredo-que-nao-pode-vazar"));
    }

    @Test
    void tokenInvalidoDevolve400() {
        reiniciar();
        for (String t : new String[] {"com espaco", "com\nquebra", "tab\taqui", "a".repeat(501)}) {
            Response r = put(Map.of("urlBase", BASE, "token", t));
            assertEquals(400, r.statusCode());
            assertEquals("token_invalido", r.jsonPath().getString("codigo"));
        }
        put(Map.of("urlBase", BASE, "token", "a".repeat(500))).then().statusCode(204);
        reiniciar();
    }

    @Test
    void emModoLocalPermiteLocalhostMasNaoRedeInterna() {
        // test: emitesimples.cnpj.permitir-url-local=true
        put(Map.of("urlBase", "http://localhost:8099/cnpj")).then().statusCode(204);
        put(Map.of("urlBase", "http://127.0.0.1:8099/cnpj")).then().statusCode(204);
        put(Map.of("urlBase", "http://10.0.0.1/cnpj")).then().statusCode(400).body("codigo", equalTo("url_invalida"));
        put(Map.of("urlBase", "http://169.254.169.254/x")).then().statusCode(400);
        reiniciar();
    }

    @Test
    void emProducaoLocalhostEBloqueado() {
        ConfiguracaoConsultaCnpjService s = new ConfiguracaoConsultaCnpjService();
        s.permitirUrlLocal = false;
        for (String u : new String[] {"http://localhost/x", "https://localhost/x", "https://127.0.0.1/x", "https://LOCALHOST./x",
                "https://[::ffff:127.0.0.1]/x", "https://0.0.0.0/x", "https://100.64.0.1/x"}) {
            assertEquals("url_invalida", assertThrows(ApiException.class, () -> s.validarUrl(u), u).getCodigo(), u);
        }
        assertEquals(BASE, s.validarUrl(BASE + "/"));
        assertEquals("https://8.8.8.8/x", s.validarUrl("https://8.8.8.8/x"));
    }

    // ---------------------------------------------------------------------------------------------- cofre

    @Test
    void cadaGravacaoUsaIvDistintoEOValorNaoFicaEmClaro() {
        String valor = "mesmo-valor-" + System.nanoTime();
        String a = segredos.guardar(valor);
        String b = segredos.guardar(valor);
        try {
            List<?> linhas = QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                    "select id, valor_cifrado, iv from segredo where id in (?1, ?2) order by id")
                    .setParameter(1, a).setParameter(2, b).getResultList());
            assertEquals(2, linhas.size());
            Object[] l1 = (Object[]) linhas.get(0);
            Object[] l2 = (Object[]) linhas.get(1);
            assertFalse(Arrays.equals((byte[]) l1[2], (byte[]) l2[2]), "IVs distintos");
            assertFalse(Arrays.equals((byte[]) l1[1], (byte[]) l2[1]), "cifrados distintos para o mesmo valor");
            assertEquals(valor, segredos.ler(a));
            assertEquals(valor, segredos.ler(b));
            segredos.substituir(a, "novo");
            assertEquals("novo", segredos.ler(a));
            assertEquals(valor, segredos.ler(b));
        } finally {
            segredos.remover(a);
            segredos.remover(b);
        }
        assertThrows(IllegalStateException.class, () -> segredos.ler(a));
    }

    @Test
    void chaveAusenteOuInvalidaFalhaComErroClaro() {
        for (Optional<String> chave : List.of(Optional.<String>empty(), Optional.of(""), Optional.of("curta"),
                Optional.of("!!!nao-e-base64!!!"),
                Optional.of(java.util.Base64.getEncoder().encodeToString(new byte[16])))) {
            SegredoService s = new SegredoService();
            s.chaveBase64 = chave;
            ApiException e = assertThrows(ApiException.class, () -> s.guardar("segredo"));
            assertEquals("segredos_chave_invalida", e.getCodigo());
            assertTrue(e.getMessage().contains("EMITESIMPLES_SEGREDOS_CHAVE"), e.getMessage());
            assertTrue(e.getMessage().contains("openssl rand -base64 32"), e.getMessage());
            assertThrows(ApiException.class, () -> s.ler("x"));
            assertThrows(ApiException.class, () -> s.substituir("x", "y"));
            assertFalse(e.getMessage().contains("segredo\""));
        }
    }

    @Test
    void cifraComOutraChaveNaoDecifraENaoVazaValor() {
        String valor = "valor-que-nao-deve-vazar";
        String id = segredos.guardar(valor);
        try {
            SegredoService outro = new SegredoService();
            outro.em = em;
            outro.chaveBase64 = Optional.of(java.util.Base64.getEncoder().encodeToString(new byte[32]));
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> outro.ler(id));
            assertFalse(e.getMessage().contains(valor));
            assertEquals(null, e.getCause());
            assertFalse(segredos.toString().contains(valor));
        } finally {
            segredos.remover(id);
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
