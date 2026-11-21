package br.com.emitesimples.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.DadosDeTeste.Usr;
import br.com.emitesimples.usuarios.entity.TokenRenovacaoEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RefreshTest extends BaseApiTest {

    @Inject
    EntityManager em;

    private String refreshDoLogin(Usr u) {
        return login(u.email(), SENHA).jsonPath().getString("refreshToken");
    }

    @Test
    void rotacaoGeraNovoParEMarcaOAntigoComoUsado() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "operador");
        String r1 = refreshDoLogin(u);

        Response resp = refresh(r1);
        resp.then().statusCode(200)
                .body("accessToken", notNullValue()).body("expiresIn", equalTo(900))
                .body("empresa.id", equalTo(empresa)).body("papel", equalTo("operador"));
        String r2 = resp.jsonPath().getString("refreshToken");
        assertNotEquals(r1, r2);

        // o novo access token funciona
        comToken(resp.jsonPath().getString("accessToken")).get("/api/auth/me").then().statusCode(200);

        TokenRenovacaoEntity antigo = dados.tokenPorHash(AuthService.hashDoToken(r1));
        assertTrue(antigo.getUsado());
        assertNotNull(antigo.getDataUso());
        assertFalse(antigo.getRevogado());
        TokenRenovacaoEntity novo = dados.tokenPorHash(AuthService.hashDoToken(r2));
        assertFalse(novo.getUsado());
        assertFalse(novo.getRevogado());

        // e o novo também roda (cadeia)
        refresh(r2).then().statusCode(200);
    }

    @Test
    void reusoDoTokenAntigoDaUm401ERevogaTodosOsAtivos() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "operador");
        String r1 = refreshDoLogin(u);
        String r2 = refresh(r1).jsonPath().getString("refreshToken");
        String outraSessao = refreshDoLogin(u); // segunda sessão do MESMO usuário e empresa, também ativa

        // o atacante reapresenta r1 (já usado)
        refresh(r1).then().statusCode(401).body("codigo", equalTo("sessao_invalida"));

        // r2 (o legítimo, que estava ativo) e a outra sessão foram revogados
        refresh(r2).then().statusCode(401);
        refresh(outraSessao).then().statusCode(401);
        TokenRenovacaoEntity t2 = dados.tokenPorHash(AuthService.hashDoToken(r2));
        assertTrue(t2.getRevogado());
        assertNotNull(t2.getDataRevogacao());
        TokenRenovacaoEntity t3 = dados.tokenPorHash(AuthService.hashDoToken(outraSessao));
        assertTrue(t3.getRevogado());
        assertNotNull(t3.getDataRevogacao());
    }

    @Test
    void reusoNaoRevogaTokensDeOutraEmpresaDoMesmoUsuario() {
        String a = dados.empresaAtiva();
        String b = dados.empresaAtiva();
        Usr u = dados.usuario(true, false);
        dados.vincular(u.id(), a, "operador", true);
        dados.vincular(u.id(), b, "operador", true);
        String ra = login(u.email(), SENHA, a).jsonPath().getString("refreshToken");
        String rb = login(u.email(), SENHA, b).jsonPath().getString("refreshToken");
        refresh(ra).then().statusCode(200);
        refresh(ra).then().statusCode(401); // reuso na empresa A
        refresh(rb).then().statusCode(200); // a sessão da empresa B segue válida (revoga "usuário e empresa")
    }

    @Test
    void tokenRevogadoQueReaparecesTambemEReuso() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String r1 = refreshDoLogin(u);
        String r2 = refreshDoLogin(u);
        logout(r1).then().statusCode(204);
        refresh(r1).then().statusCode(401); // revogado reaparecendo = reuso
        refresh(r2).then().statusCode(401); // derrubou a outra sessão ativa
    }

    @Test
    void tokenExpiradoDa401() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String r1 = refreshDoLogin(u);
        dados.expirarToken(AuthService.hashDoToken(r1));
        refresh(r1).then().statusCode(401).body("codigo", equalTo("sessao_invalida"));
    }

    @Test
    void tokenInexistenteOuLixoDa401() {
        refresh("token-que-nao-existe-" + System.nanoTime()).then().statusCode(401).body("codigo", equalTo("sessao_invalida"));
        refresh("a".repeat(43)).then().statusCode(401);
        io.restassured.RestAssured.given().contentType("application/json").body("{}").post("/api/auth/refresh")
                .then().statusCode(400);
    }

    @Test
    void logoutRevogaEEIdempotente() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String r1 = refreshDoLogin(u);
        logout(r1).then().statusCode(204);
        TokenRenovacaoEntity t = dados.tokenPorHash(AuthService.hashDoToken(r1));
        assertTrue(t.getRevogado());
        assertNotNull(t.getDataRevogacao());
        logout(r1).then().statusCode(204);
        logout("desconhecido-" + System.nanoTime()).then().statusCode(204);
        refresh(r1).then().statusCode(401);
    }

    @Test
    void logoutNaoDerrubaOutraSessao() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String r1 = refreshDoLogin(u);
        String r2 = refreshDoLogin(u);
        logout(r1).then().statusCode(204);
        refresh(r2).then().statusCode(200);
    }

    @Test
    void refreshDeUsuarioInativadoOuEmpresaDesativadaDa401() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "operador");
        String r1 = refreshDoLogin(u);
        QuarkusTransaction.requiringNew().run(() -> em.createQuery("update UsuarioEntity x set x.ativo = false where x.id = :id")
                .setParameter("id", u.id()).executeUpdate());
        refresh(r1).then().statusCode(401);

        String empresa2 = dados.empresaAtiva();
        Usr u2 = dados.usuarioComPapel(empresa2, "operador");
        String r2 = refreshDoLogin(u2);
        QuarkusTransaction.requiringNew().run(() -> em.createQuery("update EmpresaEntity x set x.situacao = 'desativada' where x.id = :id")
                .setParameter("id", empresa2).executeUpdate());
        refresh(r2).then().statusCode(401);
    }

    @Test
    void refreshReavaliaOPapelDoVinculo() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "administrador");
        String r1 = refreshDoLogin(u);
        QuarkusTransaction.requiringNew().run(() -> em.createQuery(
                "update VinculoUsuarioEntity v set v.papel = 'consulta' where v.usuario.id = :id")
                .setParameter("id", u.id()).executeUpdate());
        Response r = refresh(r1);
        r.then().statusCode(200).body("papel", equalTo("consulta"));
        assertEquals(List.of("consulta"), claims(r.jsonPath().getString("accessToken")).get("groups"));
    }

    @Test
    void doisRefreshesSimultaneosComOMesmoTokenSoUmPassa() throws Exception {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "operador");
        String r1 = refreshDoLogin(u);
        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> tarefas = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                tarefas.add(() -> refresh(r1).statusCode());
            }
            List<Future<Integer>> fs = ex.invokeAll(tarefas);
            List<Integer> codigos = new ArrayList<>();
            for (Future<Integer> f : fs) {
                codigos.add(f.get());
            }
            codigos.sort(Integer::compare);
            assertEquals(List.of(200, 401), codigos, "exatamente um deve vencer a corrida do mesmo token");
        } finally {
            ex.shutdownNow();
        }
    }
}
