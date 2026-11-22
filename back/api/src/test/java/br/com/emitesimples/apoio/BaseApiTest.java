package br.com.emitesimples.apoio;

import static io.restassured.RestAssured.given;

import io.quarkus.test.common.QuarkusTestResource;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import java.util.Base64;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Apoio comum: chaves JWT em memória, dados de teste e chamadas HTTP reais contra a aplicação. */
@QuarkusTestResource(ChavesJwtTestResource.class)
@QuarkusTestResource(ViaCepStubResource.class)
public abstract class BaseApiTest {

    @Inject
    protected DadosDeTeste dados;

    protected static final String SENHA = DadosDeTeste.SENHA;

    protected static Response login(String email, String senha, String empresaId) {
        Map<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("email", email);
        corpo.put("senha", senha);
        if (empresaId != null) {
            corpo.put("empresaId", empresaId);
        }
        return given().contentType(ContentType.JSON).body(corpo).post("/api/auth/login");
    }

    protected static Response login(String email, String senha) {
        return login(email, senha, null);
    }

    /** Faz o login real e devolve o accessToken. */
    protected static String token(String email) {
        return token(email, null);
    }

    protected static String token(String email, String empresaId) {
        Response r = login(email, SENHA, empresaId);
        r.then().statusCode(200);
        return r.jsonPath().getString("accessToken");
    }

    protected static RequestSpecification comToken(String token) {
        RequestSpecification s = given().contentType(ContentType.JSON).accept(ContentType.JSON);
        return token == null ? s : s.header("Authorization", "Bearer " + token);
    }

    protected static Response chamar(String metodo, String caminho, String token, Object corpo) {
        RequestSpecification s = comToken(token);
        if (corpo != null) {
            s = s.body(corpo);
        }
        return s.request(metodo, caminho);
    }

    protected static int status(String metodo, String caminho, String token, Object corpo) {
        return chamar(metodo, caminho, token, corpo).statusCode();
    }

    /** Refresh como o front: sem corpo, cookie + cabecalho anti-CSRF. */
    protected static Response refresh(String refreshToken) {
        return given().header("X-Requested-With", "emitesimples").cookie("refresh_token", refreshToken).post("/api/auth/refresh");
    }

    protected static Response logout(String refreshToken) {
        return given().header("X-Requested-With", "emitesimples").cookie("refresh_token", refreshToken).post("/api/auth/logout");
    }

    /** Valor do cookie refresh_token da resposta (null se a resposta nao o definiu). */
    protected static String refreshCookie(Response r) {
        return r.getCookie("refresh_token");
    }

    /** Claims do payload do JWT (sem verificar assinatura: a verificação é feita pelo servidor nas chamadas). */
    @SuppressWarnings("unchecked")
    protected static Map<String, Object> claims(String jwt) {
        try {
            String payload = jwt.split("\\.")[1];
            return new ObjectMapper().readValue(Base64.getUrlDecoder().decode(payload), Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> cabecalho(String jwt) {
        try {
            return new ObjectMapper().readValue(Base64.getUrlDecoder().decode(jwt.split("\\.")[0]), Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
