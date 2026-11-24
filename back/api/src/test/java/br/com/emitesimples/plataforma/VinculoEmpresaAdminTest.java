package br.com.emitesimples.plataforma;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** /api/admin/empresas/{empresaId}/vinculos: vinculos vistos pela empresa. */
@QuarkusTest
class VinculoEmpresaAdminTest extends BaseApiTest {

    private static final String TEMP = "Temp-Senha-123";

    private String admin;

    private String admin() {
        if (admin == null) {
            admin = token(dados.usuario(true, true).email());
        }
        return admin;
    }

    private static String base(String empresaId) {
        return "/api/admin/empresas/" + empresaId + "/vinculos";
    }

    private static String emailNovo() {
        return "v" + UUID.randomUUID().toString().replace("-", "") + "@teste.local";
    }

    private String usuario(String nome) {
        Response r = chamar("POST", "/api/admin/usuarios", admin(),
                Map.of("nome", nome, "email", emailNovo(), "senha", TEMP));
        r.then().statusCode(201);
        return r.jsonPath().getString("id");
    }

    @Test
    void listaOrdenadaPorNomeEComFormatoDoContrato() {
        String e = dados.empresaAtiva();
        String c = usuario("carlos");
        String a = usuario("Ana");
        String b = usuario("Bruno");
        dados.vincular(c, e, "consulta", true);
        dados.vincular(a, e, "administrador", true);
        dados.vincular(b, e, "operador", false);
        dados.vincular(usuario("Outro"), dados.empresaAtiva(), "operador", true); // de outra empresa: nao aparece
        Response r = chamar("GET", base(e), admin(), null);
        r.then().statusCode(200).body("$", hasSize(3))
                .body("[0].usuario.nome", equalTo("Ana")).body("[0].papel", equalTo("administrador"))
                .body("[0].usuario.id", equalTo(a)).body("[0].usuario.ativo", equalTo(true))
                .body("[0].usuario.email", notNullValue()).body("[0].id", notNullValue())
                .body("[0].ativo", equalTo(true)).body("[0].dataCriacao", notNullValue())
                .body("[1].usuario.nome", equalTo("Bruno")).body("[1].ativo", equalTo(false))
                .body("[2].usuario.nome", equalTo("carlos"));
        assertFalse(r.asString().toLowerCase().contains("hash"));
        chamar("GET", base(UUID.randomUUID().toString()), admin(), null).then().statusCode(404)
                .body("codigo", equalTo("empresa_nao_encontrada"));
    }

    @Test
    void criaVinculoComPadraoAtivoEBarraDuplicado() {
        String e = dados.empresaAtiva();
        String u = usuario("Fulano");
        Response c = chamar("POST", base(e), admin(), Map.of("usuarioId", u, "papel", "operador"));
        c.then().statusCode(201).body("papel", equalTo("operador")).body("ativo", equalTo(true))
                .body("usuario.id", equalTo(u)).body("usuario.nome", equalTo("Fulano")).body("id", notNullValue());
        assertEquals(base(e) + "/" + c.jsonPath().getString("id"),
                c.getHeader("Location").replaceAll("^https?://[^/]+", ""));
        chamar("POST", base(e), admin(), Map.of("usuarioId", u, "papel", "consulta")).then().statusCode(409)
                .body("codigo", equalTo("vinculo_ja_existe"));
        chamar("POST", base(e), admin(), Map.of("usuarioId", usuario("Inativo"), "papel", "consulta", "ativo", false))
                .then().statusCode(201).body("ativo", equalTo(false));
        chamar("GET", base(e), admin(), null).then().body("$", hasSize(2));
    }

    @Test
    void validacoesDoPost() {
        String e = dados.empresaAtiva();
        String u = usuario("Valida");
        chamar("POST", base(e), admin(), Map.of("usuarioId", u, "papel", "chefe")).then().statusCode(400)
                .body("codigo", equalTo("papel_invalido"));
        chamar("POST", base(e), admin(), Map.of("usuarioId", UUID.randomUUID().toString(), "papel", "operador"))
                .then().statusCode(404).body("codigo", equalTo("usuario_nao_encontrado"));
        chamar("POST", base(UUID.randomUUID().toString()), admin(), Map.of("usuarioId", u, "papel", "operador"))
                .then().statusCode(404).body("codigo", equalTo("empresa_nao_encontrada"));
        chamar("POST", base(e), admin(), Map.of("papel", "operador")).then().statusCode(400);
        chamar("POST", base(e), admin(), Map.of("usuarioId", u)).then().statusCode(400);
        chamar("GET", base(e), admin(), null).then().body("$", hasSize(0));
    }

    @Test
    void patchAlteraPapelEDesativaRevogandoSessao() {
        String e = dados.empresaAtiva();
        String email = emailNovo();
        String u = chamar("POST", "/api/admin/usuarios", admin(), Map.of("nome", "Paula", "email", email, "senha", TEMP))
                .jsonPath().getString("id");
        String vid = chamar("POST", base(e), admin(), Map.of("usuarioId", u, "papel", "consulta")).jsonPath().getString("id");
        String refresh = refreshCookie(login(email, TEMP, e));
        chamar("PATCH", base(e) + "/" + vid, admin(), Map.of("papel", "administrador")).then().statusCode(200)
                .body("papel", equalTo("administrador")).body("ativo", equalTo(true)).body("usuario.id", equalTo(u));
        chamar("PATCH", base(e) + "/" + vid, admin(), Map.of("papel", "rei")).then().statusCode(400)
                .body("codigo", equalTo("papel_invalido"));
        chamar("PATCH", base(e) + "/" + vid, admin(), Map.of("ativo", false)).then().statusCode(200)
                .body("ativo", equalTo(false)).body("papel", equalTo("administrador"));
        refresh(refresh).then().statusCode(401); // sessao daquela empresa revogada
        login(email, TEMP, e).then().statusCode(401);
        chamar("PATCH", base(e) + "/" + vid, admin(), Map.of("ativo", true)).then().statusCode(200).body("ativo", equalTo(true));
        login(email, TEMP, e).then().statusCode(200).body("papel", equalTo("administrador"));
        chamar("GET", base(e), admin(), null).then().body("$", hasSize(1));
    }

    @Test
    void delegacaoDeCancelarNfseSoPelaPlataformaEComPadraoFalse() {
        String e = dados.empresaAtiva();
        String email = emailNovo();
        String u = chamar("POST", "/api/admin/usuarios", admin(), Map.of("nome", "Delegada", "email", email, "senha", TEMP))
                .jsonPath().getString("id");
        Response c = chamar("POST", base(e), admin(), Map.of("usuarioId", u, "papel", "operador"));
        c.then().statusCode(201).body("podeCancelarNfse", equalTo(false));
        String vid = c.jsonPath().getString("id");
        login(email, TEMP, e).then().statusCode(200).body("podeCancelarNfse", equalTo(false));
        chamar("PATCH", base(e) + "/" + vid, admin(), Map.of("podeCancelarNfse", true)).then().statusCode(200)
                .body("podeCancelarNfse", equalTo(true)).body("papel", equalTo("operador"));
        login(email, TEMP, e).then().statusCode(200).body("podeCancelarNfse", equalTo(true));
        chamar("GET", base(e), admin(), null).then().body("[0].podeCancelarNfse", equalTo(true));
        // criar ja delegado
        Response d = chamar("POST", base(e), admin(), Map.of("usuarioId", usuario("Delegado"), "papel", "operador",
                "podeCancelarNfse", true));
        d.then().statusCode(201).body("podeCancelarNfse", equalTo(true));
        // nem o administrador da empresa nem o operador (mesmo delegado) alteram vinculos: so a plataforma
        String adm = token(dados.usuarioComPapel(e, "administrador").email());
        chamar("PATCH", base(e) + "/" + vid, adm, Map.of("podeCancelarNfse", false)).then().statusCode(403);
        String op = token(dados.usuarioComPapel(e, "operador", true).email());
        chamar("PATCH", base(e) + "/" + vid, op, Map.of("podeCancelarNfse", true)).then().statusCode(403);
        chamar("PUT", "/api/VinculoUsuario/" + vid, op, Map.of("podeCancelarNfse", true)).then().statusCode(403);
        chamar("PUT", "/api/VinculoUsuario/" + vid, adm, Map.of("podeCancelarNfse", true)).then().statusCode(403);
    }

    @Test
    void vinculoDeOutraEmpresaOuInexistenteDa404() {
        String e1 = dados.empresaAtiva();
        String e2 = dados.empresaAtiva();
        String vid = chamar("POST", base(e1), admin(), Map.of("usuarioId", usuario("Dono"), "papel", "operador"))
                .jsonPath().getString("id");
        chamar("PATCH", base(e2) + "/" + vid, admin(), Map.of("ativo", false)).then().statusCode(404)
                .body("codigo", equalTo("vinculo_nao_encontrado"));
        chamar("PATCH", base(e1) + "/" + UUID.randomUUID(), admin(), Map.of("ativo", false)).then().statusCode(404);
        chamar("PATCH", base(UUID.randomUUID().toString()) + "/" + vid, admin(), Map.of("ativo", false)).then().statusCode(404)
                .body("codigo", equalTo("empresa_nao_encontrada"));
        chamar("GET", base(e1), admin(), null).then().body("[0].ativo", equalTo(true));
    }

    @Test
    void acesso() {
        String e = dados.empresaAtiva();
        String vid = chamar("POST", base(e), admin(), Map.of("usuarioId", usuario("Acesso"), "papel", "operador"))
                .jsonPath().getString("id");
        Object[][] chamadas = {
                {"GET", base(e), null}, {"POST", base(e), Map.of("usuarioId", "x", "papel", "operador")},
                {"PATCH", base(e) + "/" + vid, Map.of("ativo", false)}};
        for (Object[] c : chamadas) {
            assertEquals(401, status((String) c[0], (String) c[1], null, c[2]), c[0] + " anonimo");
            for (String papel : List.of("consulta", "operador", "administrador")) {
                String t = token(dados.usuarioComPapel(e, papel).email());
                assertEquals(403, status((String) c[0], (String) c[1], t, c[2]), c[0] + " " + papel);
            }
        }
        chamar("GET", base(e), admin(), null).then().body("[0].ativo", equalTo(true)); // nada mudou
    }

    @Test
    void deleteEPutNegadosAteParaAPlataforma() {
        String e = dados.empresaAtiva();
        String vid = chamar("POST", base(e), admin(), Map.of("usuarioId", usuario("Sem delete"), "papel", "operador"))
                .jsonPath().getString("id");
        for (String m : new String[] {"DELETE", "PUT"}) {
            assertEquals(403, status(m, base(e) + "/" + vid, admin(), Map.of()), m);
            assertEquals(403, status(m, base(e), admin(), Map.of()), m);
        }
        chamar("GET", base(e), admin(), null).then().body("$", hasSize(1));
        // o recurso gerado /api/Empresa nao e afetado (caminho distinto): GET segue autenticado
        assertEquals(200, status("GET", "/api/Empresa", admin(), null));
    }
}
