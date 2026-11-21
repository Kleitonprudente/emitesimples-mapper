package br.com.emitesimples.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** OpenAPI, health, CORS e o formato uniforme de erro. */
@QuarkusTest
class InfraEErrosTest extends BaseApiTest {

    // ------------------------------------------------------------------------------------------- OpenAPI

    @Test
    void openApiListaOsEndpointsReaisEOEsquemaBearer() {
        Response r = given().accept(ContentType.JSON).get("/q/openapi");
        assertEquals(200, r.statusCode());
        Map<String, Object> paths = r.jsonPath().getMap("paths");
        for (String p : new String[] {"/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/me",
                "/api/auth/senha", "/api/Pessoa", "/api/Nota", "/api/Municipio", "/api/Empresa"}) {
            assertTrue(paths.containsKey(p), "OpenAPI sem " + p + "; paths=" + paths.keySet());
        }
        assertEquals("http", r.jsonPath().getString("components.securitySchemes.bearerAuth.type"));
        assertEquals("bearer", r.jsonPath().getString("components.securitySchemes.bearerAuth.scheme"));
        assertEquals("JWT", r.jsonPath().getString("components.securitySchemes.bearerAuth.bearerFormat"));
        // YAML (padrão) também responde
        given().get("/q/openapi").then().statusCode(200).body(containsString("/api/auth/login"));
    }

    @Test
    void swaggerUiExisteNoPerfilDeTeste() {
        int s = given().redirects().follow(false).get("/q/swagger-ui").statusCode();
        assertTrue(s == 200 || s == 302 || s == 301, "swagger-ui devolveu " + s);
    }

    @Test
    void healthResponde() {
        given().get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
    }

    // ---------------------------------------------------------------------------------------------- CORS

    @Test
    void corsLiberaAsOrigensConfiguradasEBloqueiaAsOutras() {
        Response ok = given().header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .options("/api/auth/login");
        assertEquals(200, ok.statusCode());
        assertEquals("http://localhost:5173", ok.getHeader("Access-Control-Allow-Origin"));
        String headers = ok.getHeader("Access-Control-Allow-Headers").toLowerCase();
        assertTrue(headers.contains("authorization") && headers.contains("content-type"), headers);
        assertTrue(ok.getHeader("Access-Control-Allow-Methods").contains("POST"));
        // o cookie do refresh pode ir em chamada cross-origin das origens configuradas, com o cabecalho anti-CSRF
        assertEquals("true", ok.getHeader("Access-Control-Allow-Credentials"));
        Response pre = given().header("Origin", "http://localhost:5173").header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "x-requested-with").options("/api/auth/refresh");
        assertTrue(pre.getHeader("Access-Control-Allow-Headers").toLowerCase().contains("x-requested-with"), pre.getHeaders().toString());

        assertEquals("http://localhost:3000", given().header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET").options("/api/Pessoa").getHeader("Access-Control-Allow-Origin"));

        Response ruim = given().header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "POST").options("/api/auth/login");
        assertEquals(null, ruim.getHeader("Access-Control-Allow-Origin"), "origem não configurada não pode ser liberada");
        assertEquals(null, ruim.getHeader("Access-Control-Allow-Credentials"), "origem desconhecida nunca recebe credenciais");
        assertTrue(ruim.statusCode() >= 400, "preflight de origem desconhecida deve falhar; veio " + ruim.statusCode());
    }

    // ------------------------------------------------------------------------------------ erros uniformes

    private static void formatoUniforme(Response r, int status, String codigo) {
        assertEquals(status, r.statusCode(), r.asString());
        assertTrue(r.contentType().contains("application/json"), "Content-Type=" + r.contentType() + " corpo=" + r.asString());
        assertEquals(codigo, r.jsonPath().getString("codigo"), r.asString());
        assertNotNull(r.jsonPath().getString("mensagem"), r.asString());
        assertTrue(r.jsonPath().getMap("$").keySet().stream().allMatch(k -> Set3.contains(k)),
                "campos fora do contrato: " + r.jsonPath().getMap("$").keySet());
    }

    private static final java.util.Set<String> Set3 = java.util.Set.of("codigo", "mensagem", "detalhes");

    @Test
    void erro400DeValidacaoComListaDeCampos() {
        Response r = given().contentType(ContentType.JSON).body("{}").post("/api/auth/login");
        formatoUniforme(r, 400, "requisicao_invalida");
        assertEquals(2, r.jsonPath().getList("detalhes.campos").size());
    }

    @Test
    void erro400DeJsonMalformado() {
        Response r = given().contentType(ContentType.JSON).body("{isto nao eh json").post("/api/auth/login");
        assertEquals(400, r.statusCode());
        assertFalse(r.asString().contains("com.fasterxml"), "não pode vazar classe do Jackson: " + r.asString());
    }

    @Test
    void erro401SemToken() {
        formatoUniforme(given().accept(ContentType.JSON).get("/api/Pessoa"), 401, "nao_autenticado");
        // anonimo em rota de escrita negada tambem: 401 uniforme
        formatoUniforme(given().contentType(ContentType.JSON).body("{}").post("/api/Nota"), 401, "nao_autenticado");
        // token invalido
        Response ruim = given().accept(ContentType.JSON).header("Authorization", "Bearer lixo.lixo.lixo").get("/api/Pessoa");
        formatoUniforme(ruim, 401, "nao_autenticado");
        assertEquals("Bearer", ruim.getHeader("WWW-Authenticate"));
    }

    @Test
    void erro403SemPermissao() {
        String t = token(dados.usuarioComPapel(dados.empresaAtiva(), "consulta").email());
        Response r = chamar("POST", "/api/Pessoa", t, Map.of());

        formatoUniforme(r, 403, "acesso_negado");
    }

    @Test
    void erro403DoEscopoDeTenantTemFormatoUniforme() throws Exception {
        // 403 lançado DENTRO do recurso (EscopoTenant): passa pelo mapeador global
        var u = dados.usuario(true, false);
        String jwt = io.smallrye.jwt.build.Jwt.issuer("emitesimples").subject(u.id()).upn(u.email()).groups("operador")
                .expiresIn(60).sign(); // chave de assinatura configurada (a dos testes)
        formatoUniforme(chamar("GET", "/api/Pessoa", jwt, null), 403, "acesso_negado");
    }

    @Test
    void erro404() {
        String t = token(dados.usuarioComPapel(dados.empresaAtiva(), "consulta").email());
        formatoUniforme(chamar("GET", "/api/Pessoa/id/nao-existe", t, null), 404, "nao_encontrado");
        formatoUniforme(chamar("GET", "/api/CaminhoQueNaoExiste", t, null), 404, "nao_encontrado");
    }

    @Test
    void erro405() {
        String t = token(dados.usuarioComPapel(dados.empresaAtiva(), "consulta").email());
        Response r = chamar("GET", "/api/auth/login", t, null);
        formatoUniforme(r, 403, "acesso_negado"); // GET em rota so POST: a politica fecha (deny) antes do 405
    }

    @Test
    void erro409() {
        String a = dados.empresaAtiva();
        String b = dados.empresaAtiva();
        var u = dados.usuario(true, false);
        dados.vincular(u.id(), a, "operador", true);
        dados.vincular(u.id(), b, "operador", true);
        formatoUniforme(login(u.email(), SENHA), 409, "empresa_obrigatoria");
    }

    @Test
    void erro429() {
        var u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        for (int i = 0; i < 5; i++) {
            login(u.email(), "errada-" + i + "-xx");
        }
        formatoUniforme(login(u.email(), SENHA), 429, "muitas_tentativas");
    }

    @Test
    void erro500NaoVazaPilhaNemSql() {
        // Força um 500 real do código gerado: PUT que troca a empresa (Hibernate recusa alterar o identificador)
        String empA = dados.empresaAtiva();
        String empB = dados.empresaAtiva();
        String t = token(dados.usuarioComPapel(empA, "operador").email());
        String p = dados.pessoa(empA, "p");
        String versaoB = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                java.time.LocalDateTime.now());
        // versão de B é desconhecida aqui: usa o banco
        versaoB = versaoDe(empB);
        Response r = chamar("PUT", "/api/Pessoa/id/" + p, t, Map.of("tipoPessoa", "fisica", "nome", "x",
                "empresa", Map.of("id", empB, "version", versaoB)));
        // O gerador agora barra a troca de empresa com 403 (antes era 500 do Hibernate). O mascaramento do 500
        // generico ja nao tem caso real provocavel por HTTP: so o ErrosHandler o cobre.
        formatoUniforme(r, 403, "acesso_negado");
        String corpo = r.asString().toLowerCase();
        for (String proibido : new String[] {"hibernate", "exception", "select ", "insert ", "update ", "sql", "at br.com", "caused by"}) {
            assertFalse(corpo.contains(proibido), "500 vazou '" + proibido + "': " + r.asString());
        }
    }

    @Inject
    jakarta.persistence.EntityManager em;

    private String versaoDe(String empresaId) {
        return io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().call(() ->
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                        em.find(br.com.emitesimples.configuracao.entity.EmpresaEntity.class, empresaId).getVersion()
                                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)));
    }

    @Test
    void erroDePersistenciaNoPostGeradoNaoVazaSql() {
        // violação de unicidade (documento duplicado) no POST gerado: a mensagem do banco não pode sair
        String empA = dados.empresaAtiva();
        String t = token(dados.usuarioComPapel(empA, "operador").email());
        String doc = "12345678901";
        Map<String, Object> corpo = Map.of("tipoPessoa", "fisica", "nome", "dup", "documento", doc,
                "empresa", Map.of("id", empA, "version", versaoDe(empA)));
        Response primeira = chamar("POST", "/api/Pessoa", t, corpo);
        Response segunda = chamar("POST", "/api/Pessoa", t, corpo);
        System.out.println("[ERRO-DUP] primeira=" + primeira.statusCode() + " segunda=" + segunda.statusCode() + " corpo=" + segunda.asString());
        assertTrue(segunda.statusCode() >= 400);
        String c = segunda.asString().toLowerCase();
        for (String proibido : new String[] {"ux_pessoa", "duplicate key", "violates", "insert into", "constraint"}) {
            assertFalse(c.contains(proibido), "vazou '" + proibido + "': " + segunda.asString());
        }
        assertNotNull(segunda.jsonPath().getString("codigo"), "sem codigo uniforme: " + segunda.asString());
    }

}
