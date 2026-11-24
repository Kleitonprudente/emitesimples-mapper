package br.com.emitesimples.plataforma;

import static br.com.emitesimples.apoio.CnpjWsStubResource.cnpj;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.CnpjWsStubResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GET /api/consulta-cnpj/{cnpj}: consulta para usuarios da empresa (administrador/operador) e plataforma. CNPJ.ws simulado. */
@QuarkusTest
class ConsultaCnpjEmpresaTest extends BaseApiTest {

    private static final String CFG = "/api/admin/configuracoes/consulta-cnpj";
    private static final String BASE = "/api/consulta-cnpj/";
    private static final String BASE_ADMIN = "/api/admin/consulta-cnpj/";
    private static final String BASE_PADRAO = "https://comercial.cnpj.ws/cnpj";

    @ConfigProperty(name = CnpjWsStubResource.PROP_URL)
    String urlStub;

    @Inject
    LimiteConsultaCnpj limite;

    private String plataforma;

    private String plataforma() {
        if (plataforma == null) {
            plataforma = token(dados.usuario(true, true).email());
        }
        return plataforma;
    }

    private String papel(String papel) {
        return token(dados.usuarioComPapel(dados.empresaAtiva(), papel).email());
    }

    private void configurar(String token) {
        chamar("PUT", CFG, plataforma(), Map.of("urlBase", urlStub, "token", token)).then().statusCode(204);
    }

    @BeforeEach
    void preparar() {
        limite.limparTudo();
        configurar("tok-emp-" + System.nanoTime());
    }

    @AfterEach
    void restaurar() {
        chamar("PUT", CFG, plataforma(), Map.of("urlBase", BASE_PADRAO, "token", "")).then().statusCode(204);
        limite.limparTudo();
    }

    private String stub(String caminho) {
        return given().get(urlStub.substring(0, urlStub.length() - "/cnpj".length()) + caminho).asString().trim();
    }

    @Test
    void operadorEAdministradorConsultamComOMesmoMapeamentoDaRotaAdmin() {
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        String esperado = chamar("GET", BASE_ADMIN + c, plataforma(), null).then().statusCode(200).extract().asString();
        for (String p : new String[] {"OPERADOR", "ADMINISTRADOR"}) {
            Response r = chamar("GET", BASE + c, papel(p), null);
            r.then().statusCode(200)
                    .body("cnpj", equalTo(c))
                    .body("razaoSocial", equalTo("CADASTRO TESTE LTDA"))
                    .body("uf", equalTo("RJ"))
                    .body("regimeTributario", equalTo(1))
                    .body("inscricaoEstadual", equalTo("333333333"))
                    .body("socios.size()", equalTo(2))
                    .body("socios[0].nome", equalTo("MARIA SOCIA"))
                    .body("socios[0].qualificacao", equalTo("Sócio-Administrador"));
            assertEquals(esperado, r.asString(), p);
        }
        chamar("GET", BASE + c, plataforma(), null).then().statusCode(200).body("cnpj", equalTo(c));
    }

    @Test
    void errosSaoOsMesmosDaRotaAdmin() {
        String t = papel("OPERADOR");
        chamar("GET", BASE + "123", t, null).then().statusCode(400).body("codigo", equalTo("cnpj_invalido"));
        chamar("GET", BASE + cnpj(CnpjWsStubResource.RAIZ_404), t, null).then().statusCode(404)
                .body("codigo", equalTo("cnpj_nao_encontrado"));
        chamar("GET", BASE + cnpj(CnpjWsStubResource.RAIZ_401), t, null).then().statusCode(502)
                .body("codigo", equalTo("token_recusado"));
        chamar("GET", BASE + cnpj(CnpjWsStubResource.RAIZ_429), t, null).then().statusCode(429)
                .body("codigo", equalTo("limite_consulta_excedido"));
        configurar("");
        chamar("GET", BASE + cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES), t, null).then().statusCode(409)
                .body("codigo", equalTo("token_nao_configurado"));
    }

    @Test
    void papelConsultaNegadoAnonimoNaoAutenticadoEEscritaNegada() {
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        int antes = Integer.parseInt(stub("/_contagem/" + c));
        assertEquals(401, status("GET", BASE + c, null, null));
        assertEquals(403, status("GET", BASE + c, papel("CONSULTA"), null));
        assertEquals(antes, Integer.parseInt(stub("/_contagem/" + c)));
        for (String m : new String[] {"POST", "PUT", "PATCH", "DELETE"}) {
            for (String t : new String[] {papel("ADMINISTRADOR"), plataforma()}) {
                assertEquals(403, status(m, BASE + c, t, Map.of()), m);
            }
            assertEquals(401, status(m, BASE + c, null, Map.of()), m);
        }
        assertEquals(200, status("GET", BASE + c, papel("OPERADOR"), null));
    }

    @Test
    void naoAbreNadaAlemDaRota() {
        String t = papel("ADMINISTRADOR");
        assertEquals(403, status("GET", BASE_ADMIN + cnpj(CnpjWsStubResource.RAIZ_MINIMO), t, null));
        assertEquals(403, status("GET", CFG, t, null));
        assertEquals(403, status("POST", CFG + "/testar", t, Map.of()));
    }

    @Test
    void limiteDeTrintaPorMinutoPorUsuarioCompartilhadoComARotaAdmin() {
        String c = cnpj(CnpjWsStubResource.RAIZ_MINIMO);
        String t = papel("OPERADOR");
        for (int i = 0; i < 30; i++) {
            assertEquals(200, status("GET", BASE + c, t, null));
        }
        Response r = chamar("GET", BASE + c, t, null);
        r.then().statusCode(429).body("codigo", equalTo("muitas_tentativas"));
        assertTrue(Integer.parseInt(r.getHeader("Retry-After")) >= 1);
        // outro usuario nao e afetado
        assertEquals(200, status("GET", BASE + c, papel("OPERADOR"), null));
        // contador compartilhado: a plataforma gasta 15 em cada rota e a 31a cai em qualquer uma
        String p = plataforma();
        for (int i = 0; i < 15; i++) {
            assertEquals(200, status("GET", BASE + c, p, null));
            assertEquals(200, status("GET", BASE_ADMIN + c, p, null));
        }
        assertEquals(429, status("GET", BASE_ADMIN + c, p, null));
        assertEquals(429, status("GET", BASE + c, p, null));
    }

    @Test
    void tokenNuncaApareceNaRespostaNemNosLogs() {
        String token = "tok-SECRETO-EMP-" + System.nanoTime();
        configurar(token);
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        String t = papel("OPERADOR");
        Response ok;
        Response erro;
        try {
            ok = chamar("GET", BASE + cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES), t, null);
            erro = chamar("GET", BASE + cnpj(CnpjWsStubResource.RAIZ_401), t, null);
        } finally {
            LogManager.getLogManager().getLogger("").removeHandler(h);
        }
        assertEquals(200, ok.statusCode());
        assertTrue(Arrays.asList(stub("/_tokens").split("\n")).contains(token));
        for (Response r : new Response[] {ok, erro}) {
            assertFalse(r.asString().contains(token), r.asString());
            assertFalse(r.getHeaders().toString().contains(token));
        }
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        assertFalse(logs.isEmpty());
        for (String l : logs) {
            assertFalse(l.contains(token) || l.contains("token="), "token em log: " + l);
            assertFalse(l.contains(c), "CNPJ completo em log: " + l);
        }
    }

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
}
