package br.com.emitesimples.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.DadosDeTeste.Usr;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Com emitesimples.auth.cookie-secure=true (o padrao de producao) o cookie sai com o atributo Secure. */
@QuarkusTest
@TestProfile(CookieSecureTest.ComSecure.class)
class CookieSecureTest extends BaseApiTest {

    public static class ComSecure implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("emitesimples.auth.cookie-secure", "true");
        }
    }

    @Test
    void cookieSaiComSecureQuandoLigado() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "OPERADOR");
        Response l = login(u.email(), SENHA);
        l.then().statusCode(200);
        String sc = l.getHeader("Set-Cookie").toLowerCase();
        assertTrue(sc.contains("secure"), sc);
        assertTrue(sc.contains("httponly") && sc.contains("samesite=strict") && sc.contains("path=/api/auth"), sc);
        Response r = refresh(refreshCookie(l));
        r.then().statusCode(200);
        assertTrue(r.getHeader("Set-Cookie").toLowerCase().contains("secure"));
        Response out = logout(refreshCookie(r));
        out.then().statusCode(204);
        assertTrue(out.getHeader("Set-Cookie").toLowerCase().contains("secure"), "o cookie apagado tambem leva Secure");
    }
}
