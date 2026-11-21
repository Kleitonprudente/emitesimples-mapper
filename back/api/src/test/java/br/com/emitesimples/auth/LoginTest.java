package br.com.emitesimples.auth;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.DadosDeTeste.Usr;
import br.com.emitesimples.usuarios.entity.TokenRenovacaoEntity;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class LoginTest extends BaseApiTest {

    @Test
    void sucessoComClaimsCorretas() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "administrador");

        Response r = login(u.email(), SENHA);
        r.then().statusCode(200)
                .body("expiresIn", equalTo(900))
                .body("usuario.id", equalTo(u.id()))
                .body("usuario.email", equalTo(u.email()))
                .body("empresa.id", equalTo(empresa))
                .body("papel", equalTo("administrador"))
                .body("plataformaAdmin", equalTo(false))
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());

        String jwt = r.jsonPath().getString("accessToken");
        Map<String, Object> c = claims(jwt);
        assertEquals("emitesimples", c.get("iss"));
        assertEquals(u.id(), c.get("sub"));
        assertEquals(u.email(), c.get("upn"));
        assertEquals(List.of("administrador"), c.get("groups"));
        assertEquals(empresa, c.get("empresa_id"));
        assertFalse(c.containsKey("plataforma_admin"), "usuário comum não pode ter a claim plataforma_admin");
        assertEquals(900, ((Number) c.get("exp")).longValue() - ((Number) c.get("iat")).longValue());
        assertEquals("RS256", cabecalho(jwt).get("alg"));

        assertNotNull(dados.usuarioPorId(u.id()).getDataUltimoAcesso(), "dataUltimoAcesso deve ser atualizada");

        // o refresh token só existe como SHA-256 no banco, nunca em claro; validade de 7 dias
        String refresh = r.jsonPath().getString("refreshToken");
        assertEquals(43, refresh.length(), "32 bytes em Base64URL sem padding");
        List<TokenRenovacaoEntity> tokens = dados.tokensDe(u.id(), empresa);
        assertEquals(1, tokens.size());
        TokenRenovacaoEntity t = tokens.get(0);
        assertNotEquals(refresh, t.getTokenHash());
        assertEquals(AuthService.hashDoToken(refresh), t.getTokenHash());
        assertEquals(64, t.getTokenHash().length());
        long dias = Duration.between(AuthService.agora(), t.getDataExpiracao()).toHours();
        assertTrue(dias >= 167 && dias <= 168, "expira em 7 dias, mas foi em " + dias + "h");
        assertFalse(t.getUsado());
        assertFalse(t.getRevogado());
    }

    @Test
    void tokenEmitidoEAceitoPeloServidor() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        comToken(token(u.email())).get("/api/auth/me").then().statusCode(200)
                .body("usuario.email", equalTo(u.email())).body("papel", equalTo("operador"));
    }

    @Test
    void emailNaoDiferenciaMaiusculas() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "consulta");
        login(u.email().toUpperCase(), SENHA).then().statusCode(200);
    }

    @Test
    void senhaErradaEEmailInexistenteDaoO401GenericoIdentico() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        Response errada = login(u.email(), "senha-errada-1");
        Response inexistente = login("nao-existe-" + System.nanoTime() + "@teste.local", "senha-errada-1");
        errada.then().statusCode(401);
        inexistente.then().statusCode(401);
        assertEquals(errada.getBody().asString(), inexistente.getBody().asString(),
                "o corpo do 401 não pode revelar se o e-mail existe");
        errada.then().body("codigo", equalTo("credenciais_invalidas")).body("mensagem", equalTo("E-mail ou senha inválidos."));
    }

    @Test
    void usuarioInativoNaoLoga() {
        Usr u = dados.usuario(false, false);
        dados.vincular(u.id(), dados.empresaAtiva(), "operador", true);
        Response r = login(u.email(), SENHA);
        r.then().statusCode(401).body("codigo", equalTo("credenciais_invalidas"));
        assertEquals(login("nao-existe-" + System.nanoTime() + "@teste.local", SENHA).getBody().asString(), r.getBody().asString());
    }

    @Test
    void empresaPendenteDeConfirmacaoNaoLoga() {
        Usr u = dados.usuarioComPapel(dados.empresa("pendente_confirmacao"), "administrador");
        login(u.email(), SENHA).then().statusCode(401).body("codigo", equalTo("credenciais_invalidas"));
    }

    @Test
    void empresaDesativadaNaoLoga() {
        Usr u = dados.usuarioComPapel(dados.empresa("desativada"), "administrador");
        login(u.email(), SENHA).then().statusCode(401).body("codigo", equalTo("credenciais_invalidas"));
    }

    @Test
    void vinculoInativoNaoLoga() {
        Usr u = dados.usuario(true, false);
        dados.vincular(u.id(), dados.empresaAtiva(), "operador", false);
        login(u.email(), SENHA).then().statusCode(401).body("codigo", equalTo("credenciais_invalidas"));
    }

    @Test
    void usuarioSemSenhaDefinidaNaoLoga() {
        Usr u = dados.usuario(true, false, null);
        dados.vincular(u.id(), dados.empresaAtiva(), "operador", true);
        login(u.email(), SENHA).then().statusCode(401).body("codigo", equalTo("credenciais_invalidas"));
    }

    @Test
    void senhaComMaisDe72BytesNaoLoga() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        login(u.email(), SENHA + "x".repeat(80)).then().statusCode(401);
    }

    @Test
    void variosVinculosExigemEmpresaId() {
        String a = dados.empresaAtiva();
        String b = dados.empresaAtiva();
        String inativa = dados.empresa("desativada");
        Usr u = dados.usuario(true, false);
        dados.vincular(u.id(), a, "administrador", true);
        dados.vincular(u.id(), b, "consulta", true);
        dados.vincular(u.id(), inativa, "operador", true); // não pode aparecer na lista

        login(u.email(), SENHA).then().statusCode(409)
                .body("codigo", equalTo("empresa_obrigatoria"))
                .body("detalhes.empresas", hasSize(2))
                .body("detalhes.empresas.id", containsInAnyOrder(a, b))
                .body("detalhes.empresas.papel", containsInAnyOrder("administrador", "consulta"))
                .body("detalhes.empresas[0]", hasKey("razaoSocial"));

        login(u.email(), SENHA, b).then().statusCode(200)
                .body("empresa.id", equalTo(b)).body("papel", equalTo("consulta"));
        Map<String, Object> c = claims(login(u.email(), SENHA, a).jsonPath().getString("accessToken"));
        assertEquals(a, c.get("empresa_id"));
        assertEquals(List.of("administrador"), c.get("groups"));

        // empresa em que NÃO tem vínculo ativo: 401 genérico (não revela nada)
        login(u.email(), SENHA, inativa).then().statusCode(401).body("codigo", equalTo("credenciais_invalidas"));
        login(u.email(), SENHA, dados.empresaAtiva()).then().statusCode(401).body("codigo", equalTo("credenciais_invalidas"));
    }

    @Test
    void administradorDaPlataformaLogaSemEmpresa() {
        Usr u = dados.usuario(true, true);
        Response r = login(u.email(), SENHA);
        r.then().statusCode(200)
                .body("plataformaAdmin", equalTo(true))
                .body("empresa", nullValue())
                .body("papel", nullValue())
                .body("refreshToken", nullValue())
                .body("accessToken", notNullValue());
        Map<String, Object> c = claims(r.jsonPath().getString("accessToken"));
        assertEquals(List.of("plataforma_admin"), c.get("groups"));
        assertEquals(true, c.get("plataforma_admin"));
        assertFalse(c.containsKey("empresa_id"), "sem empresa na sessão não há claim empresa_id");

        comToken(r.jsonPath().getString("accessToken")).get("/api/auth/me").then().statusCode(200)
                .body("plataformaAdmin", equalTo(true)).body("empresa", nullValue());
    }

    @Test
    void administradorDaPlataformaComVinculoUnicoEntraNaEmpresa() {
        String e = dados.empresaAtiva();
        Usr u = dados.usuario(true, true);
        dados.vincular(u.id(), e, "operador", true);
        Response r = login(u.email(), SENHA);
        r.then().statusCode(200).body("empresa.id", equalTo(e)).body("refreshToken", notNullValue());
        Map<String, Object> c = claims(r.jsonPath().getString("accessToken"));
        assertEquals(List.of("operador", "plataforma_admin"), c.get("groups"));
        assertEquals(true, c.get("plataforma_admin"));
    }

    @Test
    void corpoInvalidoDevolve400ComListaDeCampos() {
        io.restassured.RestAssured.given().contentType(ContentType.JSON).body(Map.of("email", " ")).post("/api/auth/login")
                .then().statusCode(400)
                .body("codigo", equalTo("requisicao_invalida"))
                .body("detalhes.campos.campo", containsInAnyOrder("email", "senha"))
                .body("detalhes.campos.mensagem", not(empty()));
        io.restassured.RestAssured.given().contentType(ContentType.JSON).post("/api/auth/login")
                .then().statusCode(400).body("codigo", equalTo("requisicao_invalida"));
    }

    @Test
    void bloqueioPorForcaBrutaAposCincoFalhas() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        for (int i = 0; i < 5; i++) {
            login(u.email(), "errada-" + i + "-xx").then().statusCode(401);
        }
        // mesmo com a senha CERTA, está bloqueado
        Response r = login(u.email(), SENHA);
        r.then().statusCode(429).body("codigo", equalTo("muitas_tentativas"));
        long retry = Long.parseLong(r.getHeader("Retry-After"));
        assertTrue(retry > 0 && retry <= 900, "Retry-After=" + retry);
        // outro e-mail não é afetado
        Usr outro = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        login(outro.email(), SENHA).then().statusCode(200);
        // e-mail inexistente também é contado (não revela existência): 5 falhas -> 429
        String fantasma = "fantasma-" + System.nanoTime() + "@teste.local";
        for (int i = 0; i < 5; i++) {
            login(fantasma, "qualquer-coisa").then().statusCode(401);
        }
        login(fantasma, "qualquer-coisa").then().statusCode(429);
    }

    @Test
    void quatroFalhasNaoBloqueiamESucessoZeraContador() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        for (int i = 0; i < 4; i++) {
            login(u.email(), "errada-" + i + "-xx").then().statusCode(401);
        }
        login(u.email(), SENHA).then().statusCode(200);
        for (int i = 0; i < 4; i++) {
            login(u.email(), "errada-" + i + "-xx").then().statusCode(401);
        }
        login(u.email(), SENHA).then().statusCode(200);
    }
}
