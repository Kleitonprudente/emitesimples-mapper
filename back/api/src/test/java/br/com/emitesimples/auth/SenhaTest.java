package br.com.emitesimples.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.DadosDeTeste.Usr;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.security.JwtEmissor;
import br.com.emitesimples.usuarios.entity.TokenRenovacaoEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SenhaTest extends BaseApiTest {

    @Inject
    JwtEmissor emissor;

    @Inject
    EntityManager em;

    private static Map<String, Object> corpo(String atual, String nova) {
        Map<String, Object> m = new HashMap<>();
        if (atual != null) {
            m.put("senhaAtual", atual);
        }
        if (nova != null) {
            m.put("novaSenha", nova);
        }
        return m;
    }

    @Test
    void semTokenDa401() {
        chamar("POST", "/api/auth/senha", null, corpo(SENHA, "Outra-Senha-1")).then().statusCode(401);
        chamar("GET", "/api/auth/me", null, null).then().statusCode(401);
    }

    @Test
    void senhaAtualErradaOuAusenteDa403() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String t = token(u.email());
        chamar("POST", "/api/auth/senha", t, corpo("errada-errada", "Nova-Senha-123")).then().statusCode(403)
                .body("codigo", equalTo("senha_atual_invalida"));
        chamar("POST", "/api/auth/senha", t, corpo(null, "Nova-Senha-123")).then().statusCode(403)
                .body("codigo", equalTo("senha_atual_invalida"));
        login(u.email(), SENHA).then().statusCode(200); // a senha não mudou
    }

    @Test
    void novaSenhaFracaOuLongaDemais() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String t = token(u.email());
        chamar("POST", "/api/auth/senha", t, corpo(SENHA, "curta12")).then().statusCode(400)
                .body("codigo", equalTo("senha_invalida")).body("detalhes.campos[0].campo", equalTo("novaSenha"));
        chamar("POST", "/api/auth/senha", t, corpo(SENHA, "   ")).then().statusCode(400);
        chamar("POST", "/api/auth/senha", t, corpo(SENHA, null)).then().statusCode(400);
        // 73 bytes ASCII
        chamar("POST", "/api/auth/senha", t, corpo(SENHA, "a".repeat(73))).then().statusCode(400)
                .body("codigo", equalTo("senha_invalida"));
        // 37 x "é" = 37 caracteres mas 74 BYTES em UTF-8: também recusa
        chamar("POST", "/api/auth/senha", t, corpo(SENHA, "é".repeat(37))).then().statusCode(400);
        // a senha antiga continua valendo
        login(u.email(), SENHA).then().statusCode(200);
    }

    @Test
    void limitesExatosDe8E72BytesSaoAceitos() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String s72 = "b".repeat(72);
        chamar("POST", "/api/auth/senha", token(u.email()), corpo(SENHA, s72)).then().statusCode(204);
        login(u.email(), s72).then().statusCode(200);
        // 73º byte diferente NÃO pode passar (o bcrypt truncaria; nós recusamos no login)
        login(u.email(), s72 + "z").then().statusCode(401);
        String s8 = "12345678";
        chamar("POST", "/api/auth/senha", loginToken(u.email(), s72), corpo(s72, s8))
                .then().statusCode(204);
        login(u.email(), s8).then().statusCode(200);
    }

    private String loginToken(String email, String senha) {
        return login(email, senha).jsonPath().getString("accessToken");
    }

    @Test
    void trocaDeSenhaFuncionaERevogaOsRefreshTokens() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "operador");
        var l1 = login(u.email(), SENHA);
        String access = l1.jsonPath().getString("accessToken");
        String refreshAntigo = refreshCookie(l1);
        String refreshOutraSessao = refreshCookie(login(u.email(), SENHA));

        var trocada = chamar("POST", "/api/auth/senha", access, corpo(SENHA, "Nova-Senha-456"));
        trocada.then().statusCode(204);
        assertTrue(trocada.getHeader("Set-Cookie").contains("Max-Age=0"), "a troca apaga o cookie do refresh");

        login(u.email(), SENHA).then().statusCode(401);
        login(u.email(), "Nova-Senha-456").then().statusCode(200);

        refresh(refreshAntigo).then().statusCode(401);
        refresh(refreshOutraSessao).then().statusCode(401);
        for (TokenRenovacaoEntity t : dados.tokensDe(u.id(), empresa)) {
            if (t.getTokenHash().equals(AuthService.hashDoToken(refreshAntigo))
                    || t.getTokenHash().equals(AuthService.hashDoToken(refreshOutraSessao))) {
                assertTrue(t.getRevogado());
                assertNotNull(t.getDataRevogacao());
            }
        }
        // o hash mudou e não guarda a senha em claro
        String hash = dados.usuarioPorId(u.id()).getSenhaHash();
        assertTrue(hash.startsWith("$2"), "bcrypt");
        assertNotEquals("Nova-Senha-456", hash);
    }

    @Test
    void novaSenhaIgualAAtualEhRecusada() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        chamar("POST", "/api/auth/senha", token(u.email()), corpo(SENHA, SENHA)).then().statusCode(400)
                .body("codigo", equalTo("senha_invalida"));
    }

    @Test
    void primeiraSenhaComSenhaHashNuloNaoExigeSenhaAtual() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuario(true, false, null);
        dados.vincular(u.id(), empresa, "operador", true);
        login(u.email(), SENHA).then().statusCode(401); // sem senha não entra

        // um token válido para esse usuário (ex.: sessão criada por outro meio, como o convite/confirmação futuros)
        String jwt = QuarkusTransaction.requiringNew().call(() -> emissor.emitir(
                em.find(br.com.emitesimples.usuarios.entity.UsuarioEntity.class, u.id()),
                em.find(EmpresaEntity.class, empresa), "operador", false));
        chamar("POST", "/api/auth/senha", jwt, corpo(null, "Primeira-Senha-1")).then().statusCode(204);

        assertNotNull(dados.usuarioPorId(u.id()).getSenhaHash());
        login(u.email(), "Primeira-Senha-1").then().statusCode(200);
        // agora que tem senha, passa a exigir a atual
        chamar("POST", "/api/auth/senha", jwt, corpo(null, "Segunda-Senha-12")).then().statusCode(403);
    }

    @Test
    void meDevolvePerfilEmpresaEPapel() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "consulta");
        chamar("GET", "/api/auth/me", token(u.email()), null).then().statusCode(200)
                .body("usuario.id", equalTo(u.id()))
                .body("usuario.email", equalTo(u.email()))
                .body("empresa.id", equalTo(empresa))
                .body("papel", equalTo("consulta"))
                .body("plataformaAdmin", equalTo(false))
                .body("$", not(org.hamcrest.Matchers.hasKey("senhaHash")))
                .body("usuario", not(org.hamcrest.Matchers.hasKey("senhaHash")));
    }
}
