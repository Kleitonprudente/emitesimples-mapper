package br.com.emitesimples.plataforma;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.DadosDeTeste.Usr;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import io.quarkus.test.junit.QuarkusTest;

/** /api/admin/usuarios: usuarios e vinculos pelo administrador da plataforma. */
@QuarkusTest
class UsuarioAdminTest extends BaseApiTest {

    private static final String BASE = "/api/admin/usuarios";
    private static final String TEMP = "Temp-Senha-123";

    private Usr adminUsr;
    private String admin;

    private String admin() {
        if (admin == null) {
            adminUsr = dados.usuario(true, true);
            admin = token(adminUsr.email());
        }
        return admin;
    }

    private static String emailNovo() {
        return "novo" + UUID.randomUUID().toString().replace("-", "") + "@Teste.Local";
    }

    private static Map<String, Object> novo(String email, Object... extras) {
        Map<String, Object> m = new HashMap<>();
        m.put("nome", "Fulano de Tal");
        m.put("email", email);
        m.put("senha", TEMP);
        for (int i = 0; i < extras.length; i += 2) {
            m.put((String) extras[i], extras[i + 1]);
        }
        return m;
    }

    private static Map<String, Object> vinc(String empresaId, String papel) {
        return Map.of("empresaId", empresaId, "papel", papel);
    }

    private Response criar(Map<String, Object> corpo) {
        return chamar("POST", BASE, admin(), corpo);
    }

    private static void semSegredo(Response r) {
        String s = r.asString();
        for (String proibido : new String[] {"senhaHash", "SENHA_HASH", "tokenHash", "\"senha\"", "$2a$", "$2b$", "$2y$", "TEMP-Senha"}) {
            assertFalse(s.contains(proibido), "a resposta nao pode conter " + proibido + ": " + s);
        }
        assertFalse(s.contains(TEMP), "a resposta nao pode conter a senha");
    }

    // ----------------------------------------------------------------------------------------------- acesso

    @Test
    void soAdministradorDaPlataforma() {
        Usr alvo = dados.usuario(true, false);
        String empresa = dados.empresaAtiva();
        String[][] chamadas = {
                {"GET", BASE}, {"GET", BASE + "/count"}, {"GET", BASE + "/0/9"}, {"GET", BASE + "/" + alvo.id()},
                {"POST", BASE}, {"PATCH", BASE + "/" + alvo.id()}, {"POST", BASE + "/" + alvo.id() + "/senha"},
                {"GET", BASE + "/" + alvo.id() + "/vinculos"}, {"POST", BASE + "/" + alvo.id() + "/vinculos"},
                {"PATCH", BASE + "/" + alvo.id() + "/vinculos/x"}, {"PUT", BASE + "/" + alvo.id()}};
        List<String> falhas = new ArrayList<>();
        for (String[] c : chamadas) {
            if (status(c[0], c[1], null, Map.of()) != 401) {
                falhas.add("sem token " + c[0] + " " + c[1]);
            }
            for (String papel : new String[] {"consulta", "operador", "administrador"}) {
                String t = token(dados.usuarioComPapel(empresa, papel).email());
                if (status(c[0], c[1], t, Map.of()) != 403) {
                    falhas.add(papel + " " + c[0] + " " + c[1]);
                }
            }
        }
        assertTrue(falhas.isEmpty(), "deveriam ser 401/403: " + falhas);
        // DELETE nunca, nem para o administrador da plataforma
        assertEquals(403, status("DELETE", BASE + "/" + alvo.id(), admin(), null));
        assertEquals(403, status("DELETE", BASE + "/" + alvo.id() + "/vinculos/x", admin(), null));
    }

    // ------------------------------------------------------------------------------------------------ criar

    @Test
    void criaUsuarioComVinculosESeguraNaoVazaSegredo() {
        String e1 = dados.empresaAtiva();
        String e2 = dados.empresaAtiva();
        String email = emailNovo();
        Response r = criar(novo(email, "whatsapp", "+5511999998888", "vinculos", List.of(vinc(e1, "operador"), vinc(e2, "consulta"))));
        r.then().statusCode(201)
                .header("Location", org.hamcrest.Matchers.containsString(BASE + "/"))
                .body("id", notNullValue())
                .body("nome", equalTo("Fulano de Tal"))
                .body("email", equalTo(email.toLowerCase()))
                .body("whatsapp", equalTo("+5511999998888"))
                .body("ativo", equalTo(true))
                .body("administradorPlataforma", equalTo(false))
                .body("temSenha", equalTo(true))
                .body("vinculos", hasSize(2))
                .body("vinculos.papel", org.hamcrest.Matchers.containsInAnyOrder("operador", "consulta"))
                .body("vinculos[0].empresa.id", notNullValue())
                .body("vinculos[0].ativo", equalTo(true));
        semSegredo(r);
        String id = r.jsonPath().getString("id");

        // o usuario entra com a senha temporaria (escolhendo a empresa, pois tem duas)
        login(email.toLowerCase(), TEMP).then().statusCode(409).body("codigo", equalTo("empresa_obrigatoria"));
        login(email, TEMP, e1).then().statusCode(200).body("papel", equalTo("operador"));

        chamar("GET", BASE + "/" + id, admin(), null).then().statusCode(200).body("email", equalTo(email.toLowerCase()));
        Response vs = chamar("GET", BASE + "/" + id + "/vinculos", admin(), null);
        vs.then().statusCode(200).body("$", hasSize(2));
        semSegredo(vs);
    }

    @Test
    void criaSemVinculosEInativo() {
        String email = emailNovo();
        criar(novo(email, "ativo", false)).then().statusCode(201).body("ativo", equalTo(false)).body("whatsapp", nullValue())
                .body("vinculos", hasSize(0));
        login(email, TEMP).then().statusCode(401); // inativo nao entra
    }

    @Test
    void emailUnicoSemDiferencaDeCaixa() {
        String email = emailNovo();
        criar(novo(email)).then().statusCode(201);
        criar(novo(email.toUpperCase())).then().statusCode(409).body("codigo", equalTo("email_ja_cadastrado"));
        criar(novo(email.toLowerCase())).then().statusCode(409).body("codigo", equalTo("email_ja_cadastrado"));
        criar(novo("  " + email + "  ")).then().statusCode(409).body("codigo", equalTo("email_ja_cadastrado"));
        // contra usuario que ja existia (criado fora da API)
        Usr existente = dados.usuario(true, false);
        criar(novo(existente.email().toUpperCase())).then().statusCode(409).body("codigo", equalTo("email_ja_cadastrado"));
    }

    @Test
    void validacoesDaCriacao() {
        String e = dados.empresaAtiva();
        // senha fora da politica
        criar(novo(emailNovo(), "senha", "curta")).then().statusCode(400).body("codigo", equalTo("senha_invalida"))
                .body("detalhes.campos[0].campo", equalTo("senha"));
        criar(novo(emailNovo(), "senha", "x".repeat(73))).then().statusCode(400).body("codigo", equalTo("senha_invalida"));
        Map<String, Object> semSenha = novo(emailNovo());
        semSenha.remove("senha");
        criar(semSenha).then().statusCode(400).body("codigo", equalTo("senha_invalida"));
        // campos
        criar(novo(emailNovo(), "nome", " ")).then().statusCode(400).body("codigo", equalTo("requisicao_invalida"));
        criar(novo("sem-arroba")).then().statusCode(400).body("codigo", equalTo("requisicao_invalida"))
                .body("detalhes.campos[0].campo", equalTo("email"));
        criar(novo(emailNovo(), "nome", "n".repeat(121))).then().statusCode(400).body("codigo", equalTo("requisicao_invalida"));
        // papel invalido
        criar(novo(emailNovo(), "vinculos", List.of(vinc(e, "dono")))).then().statusCode(400).body("codigo", equalTo("papel_invalido"));
        criar(novo(emailNovo(), "vinculos", List.of(vinc(e, "ADMINISTRADOR")))).then().statusCode(400).body("codigo", equalTo("papel_invalido"));
        // empresa de vinculo duplicada
        criar(novo(emailNovo(), "vinculos", List.of(vinc(e, "operador"), vinc(e, "consulta")))).then().statusCode(400)
                .body("codigo", equalTo("vinculos_duplicados"));
        // administradorPlataforma no corpo e ignorado: ninguem vira admin por esta rota
        criar(novo(emailNovo(), "administradorPlataforma", true)).then().statusCode(201).body("administradorPlataforma", equalTo(false));
        // sem corpo
        chamar("POST", BASE, admin(), null).then().statusCode(400);
    }

    @Test
    void empresaInexistenteDa404ENaoCriaNada() {
        String email = emailNovo();
        criar(novo(email, "vinculos", List.of(vinc(dados.empresaAtiva(), "operador"), vinc(UUID.randomUUID().toString(), "consulta"))))
                .then().statusCode(404).body("codigo", equalTo("empresa_nao_encontrada"));
        assertEquals(0, dados.contarUsuariosPorEmail(email), "tudo ou nada: o usuario nao pode ter sido criado");
    }

    // ----------------------------------------------------------------------------------------------- listar

    @Test
    void listaComBuscaPorNomeOuEmailSemAcentoEPaginacao() {
        String marca = UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            criar(novo("zz" + marca + i + "@teste.local", "nome", "Zoé Ávila " + marca + " " + i)).then().statusCode(201);
        }
        criar(novo("outro" + marca + "@teste.local", "nome", "Fulano Qualquer")).then().statusCode(201);

        // por nome, sem acento e sem diferenca de caixa
        Response r = chamar("GET", BASE + "?busca=zoe avila " + marca.toUpperCase(), admin(), null);
        r.then().statusCode(200).body("$", hasSize(3)).body("[0].nome", equalTo("Zoé Ávila " + marca + " 0"));
        semSegredo(r);
        // por e-mail
        chamar("GET", BASE + "?busca=outro" + marca, admin(), null).then().statusCode(200).body("$", hasSize(1));
        // curingas do like sao literais
        chamar("GET", BASE + "?busca=%25" + marca, admin(), null).then().statusCode(200).body("$", hasSize(0));
        chamar("GET", BASE + "?busca=_" + marca, admin(), null).then().statusCode(200).body("$", hasSize(0));
        // contagem e faixa
        chamar("GET", BASE + "/count?busca=" + marca, admin(), null).then().statusCode(200).body("count", equalTo(4));
        chamar("GET", BASE + "/0/1?busca=" + marca, admin(), null).then().statusCode(200).body("$", hasSize(2));
        chamar("GET", BASE + "/2/9?busca=" + marca, admin(), null).then().statusCode(200).body("$", hasSize(2));
        chamar("GET", BASE + "/4/9?busca=" + marca, admin(), null).then().statusCode(200).body("$", hasSize(0));
        // filtro por ativo
        chamar("GET", BASE + "/count?busca=" + marca + "&ativo=false", admin(), null).then().body("count", equalTo(0));
        chamar("GET", BASE + "/count?busca=" + marca + "&ativo=true", admin(), null).then().body("count", equalTo(4));
        // faixa invalida
        chamar("GET", BASE + "/5/2", admin(), null).then().statusCode(400).body("codigo", equalTo("requisicao_invalida"));
        chamar("GET", BASE + "/0/100", admin(), null).then().statusCode(400);
        chamar("GET", BASE + "/-1/5", admin(), null).then().statusCode(400);
        chamar("GET", BASE + "/0/99", admin(), null).then().statusCode(200);
    }

    @Test
    void obterUsuarioInexistenteDa404() {
        chamar("GET", BASE + "/" + UUID.randomUUID(), admin(), null).then().statusCode(404).body("codigo", equalTo("usuario_nao_encontrado"));
        chamar("GET", BASE + "/" + UUID.randomUUID() + "/vinculos", admin(), null).then().statusCode(404);
    }

    // ----------------------------------------------------------------------------------------------- editar

    @Test
    void editaNomeWhatsappEAtivo() {
        String email = emailNovo();
        String id = criar(novo(email, "whatsapp", "11 99999-0000")).jsonPath().getString("id");
        Response r = chamar("PATCH", BASE + "/" + id, admin(), Map.of("nome", "  Novo Nome  ", "whatsapp", "11 98888-1111"));
        r.then().statusCode(200).body("nome", equalTo("Novo Nome")).body("whatsapp", equalTo("11 98888-1111")).body("ativo", equalTo(true))
                .body("email", equalTo(email.toLowerCase()));
        semSegredo(r);
        // campo ausente nao muda; whatsapp "" apaga
        chamar("PATCH", BASE + "/" + id, admin(), Map.of("whatsapp", "")).then().statusCode(200)
                .body("whatsapp", nullValue()).body("nome", equalTo("Novo Nome"));
        // email e administradorPlataforma nao sao editaveis por aqui (ignorados)
        chamar("PATCH", BASE + "/" + id, admin(), Map.of("email", "outro@x.com", "administradorPlataforma", true)).then().statusCode(200)
                .body("email", equalTo(email.toLowerCase())).body("administradorPlataforma", equalTo(false));
        // desativar e reativar
        chamar("PATCH", BASE + "/" + id, admin(), Map.of("ativo", false)).then().statusCode(200).body("ativo", equalTo(false));
        login(email, TEMP).then().statusCode(401);
        chamar("PATCH", BASE + "/" + id, admin(), Map.of("ativo", true)).then().statusCode(200).body("ativo", equalTo(true));
        // validacoes
        chamar("PATCH", BASE + "/" + id, admin(), Map.of("nome", " ")).then().statusCode(400);
        chamar("PATCH", BASE + "/" + UUID.randomUUID(), admin(), Map.of("nome", "x")).then().statusCode(404)
                .body("codigo", equalTo("usuario_nao_encontrado"));
    }

    @Test
    void adminNaoDesativaASiMesmo() {
        String t = admin();
        chamar("PATCH", BASE + "/" + adminUsr.id(), t, Map.of("ativo", false)).then().statusCode(400)
                .body("codigo", equalTo("auto_desativacao"));
        assertTrue(dados.usuarioPorId(adminUsr.id()).getAtivo());
        // editar o proprio nome pode
        chamar("PATCH", BASE + "/" + adminUsr.id(), t, Map.of("nome", "Eu Mesmo")).then().statusCode(200);
    }

    // ---------------------------------------------------------------------------------------- redefinir senha

    @Test
    void redefinirSenhaRevogaRefreshTokensETrocaASenha() {
        String empresa = dados.empresaAtiva();
        Usr u = dados.usuarioComPapel(empresa, "operador");
        Response l = login(u.email(), SENHA);
        l.then().statusCode(200);
        String refreshAntigo = refreshCookie(l);
        assertNotNull(refreshAntigo);

        chamar("POST", BASE + "/" + u.id() + "/senha", admin(), Map.of("senha", "Outra-Temp-456")).then().statusCode(204);

        refresh(refreshAntigo).then().statusCode(401).body("codigo", equalTo("sessao_invalida"));
        assertTrue(dados.tokensDe(u.id(), empresa).stream().allMatch(t -> Boolean.TRUE.equals(t.getRevogado())));
        login(u.email(), SENHA).then().statusCode(401);
        login(u.email(), "Outra-Temp-456").then().statusCode(200);
    }

    @Test
    void redefinirSenhaValidaPoliticaEUsuarioEAbreOUsuarioSemSenha() {
        Usr u = dados.usuario(true, false, null); // sem senha definida
        chamar("POST", BASE + "/" + u.id() + "/senha", admin(), Map.of("senha", "curta")).then().statusCode(400)
                .body("codigo", equalTo("senha_invalida")).body("detalhes.campos[0].campo", equalTo("senha"));
        chamar("POST", BASE + "/" + u.id() + "/senha", admin(), Map.of("senha", "x".repeat(73))).then().statusCode(400);
        chamar("POST", BASE + "/" + u.id() + "/senha", admin(), Map.of()).then().statusCode(400).body("codigo", equalTo("senha_invalida"));
        chamar("POST", BASE + "/" + UUID.randomUUID() + "/senha", admin(), Map.of("senha", TEMP)).then().statusCode(404);
        chamar("POST", BASE + "/" + u.id() + "/senha", admin(), Map.of("senha", TEMP)).then().statusCode(204);
        assertNotNull(dados.usuarioPorId(u.id()).getSenhaHash());
    }

    @Test
    void redefinirSenhaDestravaOBloqueioPorTentativas() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "consulta");
        for (int i = 0; i < 5; i++) {
            login(u.email(), "errada-errada").then().statusCode(401);
        }
        login(u.email(), SENHA).then().statusCode(429);
        chamar("POST", BASE + "/" + u.id() + "/senha", admin(), Map.of("senha", TEMP)).then().statusCode(204);
        login(u.email(), TEMP).then().statusCode(200);
    }

    // -------------------------------------------------------------------------------------------- vinculos

    @Test
    void criaAlteraEDesativaVinculoSemNuncaApagar() {
        String e1 = dados.empresaAtiva();
        String e2 = dados.empresaAtiva();
        String email = emailNovo();
        String id = criar(novo(email)).jsonPath().getString("id");
        login(email, TEMP).then().statusCode(401); // sem vinculo e nao admin: nao entra

        Response c = chamar("POST", BASE + "/" + id + "/vinculos", admin(), vinc(e1, "consulta"));
        c.then().statusCode(201).body("papel", equalTo("consulta")).body("ativo", equalTo(true))
                .body("empresa.id", equalTo(e1)).body("usuarioId", equalTo(id)).body("id", notNullValue());
        semSegredo(c);
        String vid = c.jsonPath().getString("id");
        // vinculo duplicado: 409
        chamar("POST", BASE + "/" + id + "/vinculos", admin(), vinc(e1, "operador")).then().statusCode(409)
                .body("codigo", equalTo("vinculo_ja_existe"));
        // validacoes
        chamar("POST", BASE + "/" + id + "/vinculos", admin(), vinc(e2, "chefe")).then().statusCode(400).body("codigo", equalTo("papel_invalido"));
        chamar("POST", BASE + "/" + id + "/vinculos", admin(), vinc(UUID.randomUUID().toString(), "operador")).then().statusCode(404)
                .body("codigo", equalTo("empresa_nao_encontrada"));
        chamar("POST", BASE + "/" + UUID.randomUUID() + "/vinculos", admin(), vinc(e2, "operador")).then().statusCode(404)
                .body("codigo", equalTo("usuario_nao_encontrado"));
        chamar("POST", BASE + "/" + id + "/vinculos", admin(), Map.of("papel", "operador")).then().statusCode(400);

        // agora entra, com o papel do vinculo; a mudanca de papel vale no proximo login
        String tokenAntes = login(email, TEMP, e1).jsonPath().getString("accessToken");
        assertEquals(403, status("POST", "/api/Pessoa", tokenAntes, Map.of())); // consulta nao escreve
        chamar("PATCH", BASE + "/" + id + "/vinculos/" + vid, admin(), Map.of("papel", "operador")).then().statusCode(200)
                .body("papel", equalTo("operador")).body("ativo", equalTo(true));
        login(email, TEMP, e1).then().statusCode(200).body("papel", equalTo("operador"));

        // segundo vinculo, depois desativa o primeiro: continua listado, ativo=false, e o login naquela empresa falha
        String vid2 = chamar("POST", BASE + "/" + id + "/vinculos", admin(), Map.of("empresaId", e2, "papel", "administrador", "ativo", true))
                .jsonPath().getString("id");
        Response l = login(email, TEMP, e1);
        String refresh = refreshCookie(l);
        assertNotNull(refresh);
        chamar("PATCH", BASE + "/" + id + "/vinculos/" + vid, admin(), Map.of("ativo", false)).then().statusCode(200).body("ativo", equalTo(false));
        refresh(refresh).then().statusCode(401); // token daquela empresa revogado
        login(email, TEMP, e1).then().statusCode(401);
        login(email, TEMP).then().statusCode(200).body("empresa.id", equalTo(e2)); // so resta o vinculo da e2
        Response lista = chamar("GET", BASE + "/" + id + "/vinculos", admin(), null);
        lista.then().statusCode(200).body("$", hasSize(2));
        List<Boolean> ativos = lista.jsonPath().getList("ativo");
        assertTrue(ativos.contains(false) && ativos.contains(true));
        // reativar
        chamar("PATCH", BASE + "/" + id + "/vinculos/" + vid, admin(), Map.of("ativo", true)).then().statusCode(200).body("ativo", equalTo(true));
        login(email, TEMP, e1).then().statusCode(200);
        // desativar o segundo nao mexe nos tokens do primeiro
        chamar("PATCH", BASE + "/" + id + "/vinculos/" + vid2, admin(), Map.of("ativo", false)).then().statusCode(200);
        login(email, TEMP).then().statusCode(200).body("empresa.id", equalTo(e1));
    }

    @Test
    void alterarVinculoValidaPapelEPertencimento() {
        String e = dados.empresaAtiva();
        String idA = criar(novo(emailNovo(), "vinculos", List.of(vinc(e, "operador")))).jsonPath().getString("id");
        String idB = criar(novo(emailNovo(), "vinculos", List.of(vinc(e, "operador")))).jsonPath().getString("id");
        String vidA = chamar("GET", BASE + "/" + idA + "/vinculos", admin(), null).jsonPath().getString("[0].id");
        chamar("PATCH", BASE + "/" + idA + "/vinculos/" + vidA, admin(), Map.of("papel", "rei")).then().statusCode(400)
                .body("codigo", equalTo("papel_invalido"));
        // vinculo de A pelo caminho de B: 404 (nao vaza nem altera)
        chamar("PATCH", BASE + "/" + idB + "/vinculos/" + vidA, admin(), Map.of("ativo", false)).then().statusCode(404)
                .body("codigo", equalTo("vinculo_nao_encontrado"));
        chamar("PATCH", BASE + "/" + idA + "/vinculos/" + UUID.randomUUID(), admin(), Map.of("ativo", false)).then().statusCode(404);
        chamar("PATCH", BASE + "/" + UUID.randomUUID() + "/vinculos/" + vidA, admin(), Map.of("ativo", false)).then().statusCode(404)
                .body("codigo", equalTo("usuario_nao_encontrado"));
        // nada mudou
        chamar("GET", BASE + "/" + idA + "/vinculos", admin(), null).then().body("[0].ativo", equalTo(true)).body("[0].papel", equalTo("operador"));
    }

    // -------------------------------------- o recurso GERADO /api/Usuario: nunca devolve senha_hash (achado)

    @Test
    void recursoGeradoUsuarioNaoVazaHash() {
        Usr u = dados.usuarioComPapel(dados.empresaAtiva(), "consulta");
        Response lista = chamar("GET", "/api/Usuario", admin(), null);
        lista.then().statusCode(200);
        semSegredo(lista);
        Response um = chamar("GET", "/api/Usuario/id/" + u.id(), admin(), null);
        um.then().statusCode(200);
        semSegredo(um);
        assertFalse(um.asString().toLowerCase().contains("hash"));
        assertEquals(403, status("GET", "/api/Usuario/id/" + u.id(), token(dados.usuarioComPapel(dados.empresaAtiva(), "administrador").email()), null));
        // PUT gerado com o corpo devolvido pelo GET nao apaga a senha de ninguem
        chamar("PUT", "/api/Usuario/id/" + u.id(), admin(), um.getBody().as(Map.class)).then().statusCode(204);
        login(u.email(), SENHA).then().statusCode(200);
    }
}
