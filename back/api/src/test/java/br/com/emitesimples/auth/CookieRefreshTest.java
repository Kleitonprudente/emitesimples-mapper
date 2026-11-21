package br.com.emitesimples.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.DadosDeTeste.Usr;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Contrato do refresh token em cookie httpOnly: atributos, rotacao, reuso, logout, CSRF e corrida. */
@QuarkusTest
class CookieRefreshTest extends BaseApiTest {

    private static final long SETE_DIAS = 7L * 24 * 3600;

    private Usr operador() {
        return dados.usuarioComPapel(dados.empresaAtiva(), "operador");
    }

    /** O unico Set-Cookie da resposta (deve ser o refresh_token). */
    private static String setCookie(Response r) {
        List<String> todos = r.getHeaders().getValues("Set-Cookie");
        assertEquals(1, todos.size(), "exatamente um Set-Cookie esperado: " + todos);
        assertTrue(todos.get(0).startsWith("refresh_token="), todos.get(0));
        return todos.get(0);
    }

    private static void exigirAtributos(String sc, boolean secure) {
        String l = sc.toLowerCase();
        assertTrue(l.contains("httponly"), sc);
        assertTrue(l.contains("samesite=strict"), sc);
        assertTrue(l.contains("path=/api/auth"), sc);
        assertEquals(secure, l.contains("secure"), sc);
    }

    @Test
    void loginDefineCookieComTodosOsAtributosESemRefreshNoCorpo() {
        Usr u = operador();
        Response r = login(u.email(), SENHA);
        r.then().statusCode(200).body("refreshToken", nullValue());
        assertFalse(r.asString().contains(refreshCookie(r)), "o valor do refresh nao pode aparecer no corpo");
        String sc = setCookie(r);
        exigirAtributos(sc, false); // perfil de teste: Secure desligado (http)
        assertTrue(sc.contains("Max-Age=" + SETE_DIAS), sc);
        assertEquals(43, refreshCookie(r).length());
    }

    @Test
    void refreshRotacionaOCookieESemCorpoNoRequest() {
        Usr u = operador();
        String r1 = refreshCookie(login(u.email(), SENHA));
        Response r = refresh(r1);
        r.then().statusCode(200).body("refreshToken", nullValue()).body("expiresIn", equalTo(900));
        String r2 = refreshCookie(r);
        assertNotNull(r2);
        assertNotEquals(r1, r2, "o cookie deve ser rotacionado");
        String sc = setCookie(r);
        exigirAtributos(sc, false);
        assertTrue(sc.contains("Max-Age=" + SETE_DIAS), sc);
        comToken(r.jsonPath().getString("accessToken")).get("/api/auth/me").then().statusCode(200);
        refresh(r2).then().statusCode(200);
    }

    @Test
    void reusoDoCookieAntigoRevogaEApagaOCookie() {
        Usr u = operador();
        String r1 = refreshCookie(login(u.email(), SENHA));
        String r2 = refreshCookie(refresh(r1));
        Response reuso = refresh(r1);
        reuso.then().statusCode(401).body("codigo", equalTo("sessao_invalida"));
        assertTrue(setCookie(reuso).contains("Max-Age=0"), setCookie(reuso));
        refresh(r2).then().statusCode(401); // o legitimo tambem caiu
    }

    @Test
    void semCookieOuCookieInvalidoDa401EApagaOCookie() {
        Response sem = given().header("X-Requested-With", "emitesimples").post("/api/auth/refresh");
        sem.then().statusCode(401).body("codigo", equalTo("sessao_invalida"));
        String sc = setCookie(sem);
        assertTrue(sc.contains("Max-Age=0"), sc);
        exigirAtributos(sc, false); // para apagar, Path/SameSite/HttpOnly precisam casar com o cookie original

        Response lixo = refresh("lixo-" + System.nanoTime());
        lixo.then().statusCode(401).body("codigo", equalTo("sessao_invalida"));
        assertTrue(setCookie(lixo).contains("Max-Age=0"));
        refresh("a".repeat(500)).then().statusCode(401);
    }

    @Test
    void expiradoDa401EApagaOCookie() {
        Usr u = operador();
        String r1 = refreshCookie(login(u.email(), SENHA));
        dados.expirarToken(AuthService.hashDoToken(r1));
        Response r = refresh(r1);
        r.then().statusCode(401).body("codigo", equalTo("sessao_invalida"));
        assertTrue(setCookie(r).contains("Max-Age=0"));
    }

    @Test
    void logoutRevogaApagaOCookieEEIdempotente() {
        Usr u = operador();
        String r1 = refreshCookie(login(u.email(), SENHA));
        Response l = logout(r1);
        l.then().statusCode(204);
        String sc = setCookie(l);
        assertTrue(sc.contains("Max-Age=0"), sc);
        exigirAtributos(sc, false);
        assertTrue(dados.tokenPorHash(AuthService.hashDoToken(r1)).getRevogado());
        logout(r1).then().statusCode(204);
        // sem cookie tambem 204 (e apaga)
        Response sem = given().header("X-Requested-With", "emitesimples").post("/api/auth/logout");
        sem.then().statusCode(204);
        assertTrue(setCookie(sem).contains("Max-Age=0"));
        refresh(r1).then().statusCode(401);
    }

    @Test
    void refreshELogoutSemOCabecalhoAntiCsrfSaoRecusadosSemEfeito() {
        Usr u = operador();
        String r1 = refreshCookie(login(u.email(), SENHA));
        for (String c : List.of("/api/auth/refresh", "/api/auth/logout")) {
            given().cookie("refresh_token", r1).post(c).then().statusCode(403).body("codigo", equalTo("cabecalho_obrigatorio"));
            given().header("X-Requested-With", "outro").cookie("refresh_token", r1).post(c).then().statusCode(403);
        }
        // nada foi consumido nem revogado: o token segue valido
        assertFalse(dados.tokenPorHash(AuthService.hashDoToken(r1)).getUsado());
        assertFalse(dados.tokenPorHash(AuthService.hashDoToken(r1)).getRevogado());
        refresh(r1).then().statusCode(200);
    }

    @Test
    void soAceitaPost() {
        for (String c : List.of("/api/auth/refresh", "/api/auth/logout")) {
            assertTrue(given().header("X-Requested-With", "emitesimples").get(c).statusCode() >= 400, c);
        }
    }

    @Test
    void refreshTokenNoCorpoNaoEMaisAceito() {
        Usr u = operador();
        String r1 = refreshCookie(login(u.email(), SENHA));
        given().header("X-Requested-With", "emitesimples").contentType("application/json")
                .body("{\"refreshToken\":\"" + r1 + "\"}").post("/api/auth/refresh").then().statusCode(401);
        assertFalse(dados.tokenPorHash(AuthService.hashDoToken(r1)).getUsado());
    }

    @Test
    void doisRefreshesSimultaneosComOMesmoCookieUmPassaEOutroLevaUm401() throws Exception {
        Usr u = operador();
        String r1 = refreshCookie(login(u.email(), SENHA));
        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Response>> tarefas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tarefas.add(() -> refresh(r1));
            }
            List<Integer> codigos = new ArrayList<>();
            for (Future<Response> f : ex.invokeAll(tarefas)) {
                Response r = f.get();
                codigos.add(r.statusCode());
                if (r.statusCode() == 200) {
                    assertNotNull(refreshCookie(r));
                } else {
                    assertTrue(r.getHeader("Set-Cookie").contains("Max-Age=0"));
                }
            }
            codigos.sort(Integer::compare);
            assertEquals(List.of(200, 401), codigos);
        } finally {
            ex.shutdownNow();
        }
    }

    @Test
    void adminDaPlataformaSemEmpresaNaoRecebeCookie() {
        Usr u = dados.usuario(true, true);
        Response r = login(u.email(), SENHA);
        r.then().statusCode(200);
        assertEquals(0, r.getHeaders().getValues("Set-Cookie").size());
        assertNull(refreshCookie(r));
    }

    @Test
    void outrosEndpointsNaoReemitemCookie() {
        Usr u = operador();
        Response l = login(u.email(), SENHA);
        Response me = comToken(l.jsonPath().getString("accessToken")).get("/api/auth/me");
        me.then().statusCode(200);
        assertEquals(0, me.getHeaders().getValues("Set-Cookie").size());
        assertFalse(me.asString().contains(refreshCookie(l)));
    }
}
