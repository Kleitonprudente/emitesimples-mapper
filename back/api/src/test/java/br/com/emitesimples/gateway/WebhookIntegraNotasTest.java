package br.com.emitesimples.gateway;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.integranotas.entity.EventoWebhookEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** POST /api/webhooks/integranotas/{empresaId}/{token}: publico, token HMAC no caminho, evento minimo persistido. */
@QuarkusTest
class WebhookIntegraNotasTest extends BaseApiTest {

    private static final String BASE = "/api/webhooks/integranotas/";

    @Inject
    WebhookToken tokens;

    @Inject
    LimiteWebhook limite;

    @Inject
    EntityManager em;

    @BeforeEach
    void limpar() {
        limite.limparTudo();
    }

    private static Response post(String caminho, String corpo) {
        return given().contentType(ContentType.JSON).body(corpo).post(caminho);
    }

    private String url(String empresa) {
        return BASE + empresa + "/" + tokens.gerar(empresa);
    }

    private List<EventoWebhookEntity> eventos(String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select e from EventoWebhookEntity e where e.empresa.id = :e order by e.dataCriacao, e.id",
                EventoWebhookEntity.class).setParameter("e", empresaId).getResultList());
    }

    private static final String CORPO = """
            {"origem":"NFSE","cnpj_cpf":"12.345.678/0001-95","tipo":"autorizacao","chave":"%s","sucesso":true,"codigo":100,
             "mensagem":"Autorizado o uso da NFS-e","identificacao":"abc-123","signature":"xyz","xml":"PHhtbD48L3htbD4=",
             "pdf":"JVBERi0="}
            """;

    @Test
    void tokenValidoResponde200EPersisteOEventoMinimoSemXmlNemPdf() {
        String emp = dados.empresaAtiva();
        String chave = "3".repeat(50);
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        try {
            post(url(emp), CORPO.formatted(chave)).then().statusCode(200).body("ok", equalTo(true));
        } finally {
            descapturar(h);
        }
        List<EventoWebhookEntity> l = eventos(emp);
        assertEquals(1, l.size());
        EventoWebhookEntity e = l.get(0);
        assertEquals("NFSE", e.getOrigem());
        assertEquals("12345678000195", e.getCnpjCpf());
        assertEquals("autorizacao", e.getTipo());
        assertEquals(chave, e.getChave());
        assertEquals(Boolean.TRUE, e.getSucesso());
        assertEquals(100, e.getCodigo());
        assertEquals("Autorizado o uso da NFS-e", e.getMensagem());
        assertEquals("abc-123", e.getIdentificacao());
        assertEquals("nao_verificada", e.getResultadoAssinatura());
        assertEquals("ignorado", e.getEstadoProcessamento()); // sem nota correspondente: so gatilho
        assertEquals("homologacao", e.getAmbiente());
        assertNotNull(e.getDataRecebimento());
        assertNull(e.getNota());
        // nada de xml/pdf/assinatura em nenhum campo de texto da linha
        String linha = QuarkusTransaction.requiringNew().call(() -> (String) em.createNativeQuery(
                "select cast(e as text) from eventowebhook e where e.id = ?1").setParameter(1, e.getId()).getSingleResult());
        assertFalse(linha.contains("PHhtbD48") || linha.contains("JVBERi0") || linha.contains("xyz"), linha);
        // o token do caminho nao vai para o log
        String todos = String.join("\n", logs);
        assertFalse(todos.contains(tokens.gerar(emp)));
    }

    private static void assertNotNull(Object o) {
        org.junit.jupiter.api.Assertions.assertNotNull(o);
    }

    @Test
    void tokenInvalidoOuDeOutraEmpresaDa404SemDizerPorque() {
        String a = dados.empresaAtiva();
        String b = dados.empresaAtiva();
        for (String caminho : new String[] {BASE + a + "/invalido", BASE + a + "/" + tokens.gerar(b),
                BASE + a + "/" + tokens.gerar(a).substring(1), BASE + a + "/" + tokens.gerar(a) + "x",
                BASE + a + "/" + tokens.gerar(a).toUpperCase().replace('_', 'A')}) {
            Response r = post(caminho, "{}");
            r.then().statusCode(404).body("codigo", equalTo("nao_encontrado"));
            assertEquals("Não encontrado.", r.jsonPath().getString("mensagem"));
        }
        assertTrue(eventos(a).isEmpty());
        assertTrue(eventos(b).isEmpty());
    }

    @Test
    void empresaInexistenteComTokenQueConfere404() {
        String fantasma = UUID.randomUUID().toString();
        post(url(fantasma), CORPO.formatted("1".repeat(50))).then().statusCode(404);
    }

    @Test
    void corpoAcimaDeQuatroMegasDa413() {
        String emp = dados.empresaAtiva();
        String grande = "{\"mensagem\":\"" + "a".repeat(5 * 1024 * 1024) + "\"}";
        post(url(emp), grande).then().statusCode(413);
        assertTrue(eventos(emp).isEmpty());
        // abaixo do teto passa (e a mensagem e truncada ao tamanho da coluna)
        String medio = "{\"origem\":\"NFSE\",\"mensagem\":\"" + "b".repeat(3 * 1024 * 1024) + "\"}";
        post(url(emp), medio).then().statusCode(200);
        List<EventoWebhookEntity> l = eventos(emp);
        assertEquals(1, l.size());
        assertEquals(500, l.get(0).getMensagem().length());
    }

    @Test
    void chamadaDeTesteDoGatewayResponde200SemGravarEvento() {
        String emp = dados.empresaAtiva();
        post(url(emp), "{\"origem\":\"TESTE\",\"cnpj_cpf\":\"123\",\"tipo\":\"teste\"}").then().statusCode(200)
                .body("ok", equalTo(true));
        assertTrue(eventos(emp).isEmpty());
    }

    @Test
    void corpoQueNaoEJsonOuVazioAindaResponde200SemGravar() {
        String emp = dados.empresaAtiva();
        post(url(emp), "isto nao e json <xml>").then().statusCode(200).body("ok", equalTo(true));
        post(url(emp), "").then().statusCode(200);
        post(url(emp), "[1,2,3]").then().statusCode(200);
        assertTrue(eventos(emp).isEmpty());
    }

    @Test
    void camposLongosSaoTruncadosEPersistemSoOEnvelope() {
        String emp = dados.empresaAtiva();
        post(url(emp), "{\"origem\":\"" + "o".repeat(100) + "\",\"tipo\":\"" + "t".repeat(100) + "\",\"chave\":\""
                + "c".repeat(100) + "\",\"identificacao\":\"" + "i".repeat(100) + "\",\"cnpj_cpf\":\"" + "9".repeat(30)
                + "\",\"codigo\":\"nao-e-numero\",\"sucesso\":\"talvez\"}").then().statusCode(200);
        EventoWebhookEntity e = eventos(emp).get(0);
        assertEquals(30, e.getOrigem().length());
        assertEquals(30, e.getTipo().length());
        assertEquals(50, e.getChave().length());
        assertEquals(40, e.getIdentificacao().length());
        assertNull(e.getCnpjCpf());
        assertNull(e.getCodigo());
        assertNull(e.getSucesso());
    }

    @Test
    void rotaFechadaParaOutrosMetodosECaminhos() {
        String emp = dados.empresaAtiva();
        String adm = token(dados.usuarioComPapel(emp, "administrador").email());
        String caminho = url(emp);
        for (String m : new String[] {"GET", "PUT", "PATCH", "DELETE"}) {
            assertEquals(401, status(m, caminho, null, null), m + " anonimo");
            assertEquals(403, status(m, caminho, adm, null), m + " autenticado");
        }
        assertEquals(401, status("GET", "/api/webhooks", null, null));
        assertEquals(401, status("POST", "/api/webhooks", null, "{}"));
        assertEquals(401, status("POST", "/api/webhooks/outro", null, "{}"));
        assertEquals(401, status("POST", "/api/webhooks/outro/" + emp + "/x", null, "{}"));
        assertEquals(401, status("GET", BASE, null, null));
        assertEquals(404, status("POST", BASE + emp, null, "{}"), "sem o token no caminho nao ha rota");
        assertTrue(eventos(emp).isEmpty());
    }

    @Test
    void limitePorIpComRetryAfter() {
        String emp = dados.empresaAtiva();
        // limite de teste por IP: 20/min (application.properties); tokens invalidos tambem contam
        int ok = 0;
        Response ultima = null;
        for (int i = 0; i < 25; i++) {
            ultima = post(BASE + emp + "/invalido" + i, "{}");
            if (ultima.statusCode() == 404) {
                ok++;
            } else {
                break;
            }
        }
        assertEquals(20, ok);
        assertEquals(429, ultima.statusCode());
        assertNotNull(ultima.header("Retry-After"));
        // com o IP bloqueado, nem o token certo passa
        post(url(emp), "{}").then().statusCode(429);
    }

    @Test
    void limitePorEmpresa() {
        String a = dados.empresaAtiva();
        String b = dados.empresaAtiva();
        // limite de teste por empresa: 5/min
        for (int i = 0; i < 5; i++) {
            post(url(a), "{}").then().statusCode(200);
        }
        post(url(a), "{}").then().statusCode(429);
        post(url(b), "{}").then().statusCode(200); // outra empresa nao e afetada
    }

    @Test
    void tokenEEstavelPorEmpresaEDiferenteEntreEmpresas() {
        String a = UUID.randomUUID().toString();
        String b = UUID.randomUUID().toString();
        assertEquals(tokens.gerar(a), tokens.gerar(a));
        assertNotEquals(tokens.gerar(a), tokens.gerar(b));
        assertEquals(43, tokens.gerar(a).length());
        assertTrue(tokens.gerar(a).matches("[A-Za-z0-9_-]{43}"));
        assertTrue(tokens.confere(a, tokens.gerar(a)));
        assertFalse(tokens.confere(a, tokens.gerar(b)));
        assertFalse(tokens.confere(a, null));
        assertFalse(tokens.confere(a, ""));
        assertFalse(tokens.confere(null, tokens.gerar(a)));
    }

    // ------------------------------------------------------------------------------------------ logs

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
