package br.com.emitesimples.security;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prova a MATRIZ de acesso por caminho (application.properties) contra a aplicação real, com JWT emitido pelo login.
 * "Permitido" significa: a política deixou passar (status diferente de 401/403); o que o recurso gerado responde depois
 * (400, 404, 200...) não é assunto desta política.
 */
@QuarkusTest

class PoliticaAcessoTest extends BaseApiTest {

    static String consulta;
    static String operador;
    static String administrador;
    static String plataforma;

    @BeforeEach
    void preparar() {
        if (consulta != null) {
            return;
        }
        String empresa = dados.empresaAtiva();
        consulta = token(dados.usuarioComPapel(empresa, "consulta").email());
        operador = token(dados.usuarioComPapel(empresa, "operador").email());
        administrador = token(dados.usuarioComPapel(empresa, "administrador").email());
        plataforma = token(dados.usuario(true, true).email());
    }

    private static final List<String> FLUXO = List.of("Nota", "Cancelamento", "ChamadaGateway", "EventoWebhook", "FaturaCrm",
            "HistoricoSituacaoEmpresa", "ImportacaoMunicipio");
    private static final List<String> SO_PLATAFORMA_ESCRITA = List.of("Municipio", "Plano", "AssinaturaEmpresa", "Empresa",
            "AtividadeEmpresa", "QsaEmpresa");
    private static final List<String> CADASTROS = List.of("Pessoa", "Servico");

    private static final Map<String, Object> CORPO = Map.of();

    private static boolean passou(int status) {
        return status != 401 && status != 403;
    }

    private void esperar(List<String> falhas, String metodo, String caminho, String papel, String token, boolean permitido) {
        io.restassured.response.Response resp = chamar(metodo, caminho, token, CORPO);
        int s = resp.statusCode();
        // 403 com codigo isolamento_empresa = a RLS do banco barrou a gravacao DEPOIS de passar pela matriz de papeis
        boolean barradoPelaRls = s == 403 && "isolamento_empresa".equals(resp.jsonPath().getString("codigo"));
        boolean ok;
        String esperado;
        if (permitido) {
            ok = passou(s) || barradoPelaRls;
            esperado = "liberado (nem 401 nem 403)";
        } else if (token == null) {
            ok = s == 401;
            esperado = "401";
        } else {
            ok = s == 403 && !barradoPelaRls;
            esperado = "403";
        }
        if (!ok) {
            falhas.add(metodo + " " + caminho + " como " + papel + ": esperado " + esperado + ", veio " + s);
        }
    }

    private void porPapel(List<String> falhas, String metodo, String caminho, boolean anon, boolean cons, boolean oper,
            boolean adm, boolean plat) {
        esperar(falhas, metodo, caminho, "anonimo", null, anon);
        esperar(falhas, metodo, caminho, "consulta", consulta, cons);
        esperar(falhas, metodo, caminho, "operador", operador, oper);
        esperar(falhas, metodo, caminho, "administrador", administrador, adm);
        esperar(falhas, metodo, caminho, "plataforma", plataforma, plat);
    }

    private static void semFalhas(List<String> falhas) {
        assertTrue(falhas.isEmpty(), "Violações da matriz de acesso:\n" + String.join("\n", falhas));
    }

    /**
     * Rotas do assistente de IA (preparar/aprovar/descartar) em /api/nfse/notas e /api/nfse/cancelamentos: consulta e anonimo
     * barrados; administrador/operador passam pela politica (o servico responde 400/404 com corpo vazio); plataforma passa pela
     * politica e o servico responde 403 empresa_nao_selecionada (sem empresa na sessao); PUT/PATCH/DELETE sempre negados.
     */
    @Test
    void rotasDoAssistenteDeNotasECancelamentos() {
        List<String> falhas = new ArrayList<>();
        String id = "00000000-0000-0000-0000-000000000000";
        List<String> posts = List.of("/api/nfse/notas/preparar", "/api/nfse/notas/" + id + "/aprovar",
                "/api/nfse/notas/" + id + "/descartar", "/api/nfse/notas/" + id + "/cancelamentos/preparar",
                "/api/nfse/cancelamentos/" + id + "/aprovar", "/api/nfse/cancelamentos/" + id + "/descartar");
        for (String c : posts) {
            esperar(falhas, "POST", c, "anonimo", null, false);
            esperar(falhas, "POST", c, "consulta", consulta, false);
            for (String[] p : new String[][] {{"operador", operador}, {"administrador", administrador}}) {
                Response r = chamar("POST", c, p[1], CORPO);
                if (r.statusCode() == 401 || r.statusCode() == 403) {
                    falhas.add("POST " + c + " como " + p[0] + ": esperado liberado, veio " + r.statusCode());
                }
            }
            Response r = chamar("POST", c, plataforma, CORPO);
            if (r.statusCode() != 403 || !"empresa_nao_selecionada".equals(r.jsonPath().getString("codigo"))) {
                falhas.add("POST " + c + " como plataforma: esperado 403 empresa_nao_selecionada, veio " + r.statusCode());
            }
            for (String m : List.of("PUT", "PATCH", "DELETE")) {
                esperar(falhas, m, c, "administrador", administrador, false);
            }
        }
        // cancelamentos: so POST
        for (String m : List.of("GET", "PUT", "PATCH", "DELETE")) {
            esperar(falhas, m, "/api/nfse/cancelamentos/" + id + "/aprovar", "administrador", administrador, false);
            esperar(falhas, m, "/api/nfse/cancelamentos/" + id + "/aprovar", "anonimo", null, false);
        }
        semFalhas(falhas);
    }

    @Test
    void publico() {
        // login/refresh/logout são públicos (corpo vazio chega ao recurso: 400, não 401)
        int login = given().contentType(ContentType.JSON).body("{}").post("/api/auth/login").statusCode();
        assertEquals(400, login, "login deveria chegar ao recurso sem token");
        // refresh e logout sem o cabecalho anti-CSRF chegam ao recurso e levam 403 cabecalho_obrigatorio (nao 401)
        for (String c : List.of("/api/auth/refresh", "/api/auth/logout")) {
            given().post(c).then().statusCode(403).body("codigo", equalTo("cabecalho_obrigatorio"));
        }
        assertEquals(401, given().header("X-Requested-With", "emitesimples").post("/api/auth/refresh").statusCode());
        assertEquals(204, given().header("X-Requested-With", "emitesimples").post("/api/auth/logout").statusCode());
        assertEquals(200, given().get("/q/health").statusCode());
        assertEquals(200, given().get("/q/health/live").statusCode());
        assertEquals(200, given().get("/q/health/ready").statusCode());
        assertEquals(200, given().get("/q/openapi").statusCode());
        // o que NÃO é público
        assertEquals(401, given().get("/api/auth/me").statusCode());
        assertEquals(401, given().contentType(ContentType.JSON).body("{}").post("/api/auth/senha").statusCode());
        assertEquals(401, given().get("/api/auth/login").statusCode(), "só POST é público em /api/auth/login");
    }

    @Test
    void semTokenOuTokenInvalidoDa401EmEndpointProtegido() {
        assertEquals(401, status("GET", "/api/Pessoa", null, null));
        assertEquals(401, status("GET", "/api/Municipio", null, null));
        assertEquals(401, status("GET", "/api/Pessoa", "isto.nao.eh.um.jwt", null));
        assertEquals(401, status("GET", "/api/Pessoa", plataforma.substring(0, plataforma.length() - 4) + "AAAA", null),
                "assinatura adulterada");
        // token válido de outro emissor (outra chave) não passa
        String estranho = io.smallrye.jwt.build.Jwt.issuer("emitesimples").subject("x").groups("plataforma_admin")
                .claim("plataforma_admin", true).sign(chaveEstranha());
        assertEquals(401, status("GET", "/api/Pessoa", estranho, null), "JWT assinado com outra chave");
        // issuer errado
        assertEquals(401, status("GET", "/api/Pessoa", io.smallrye.jwt.build.Jwt.issuer("outro").subject("x")
                .groups("plataforma_admin").sign(chaveEstranha()), null));
        // alg none
        String semAssinatura = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes())
                + "." + plataforma.split("\\.")[1] + ".";
        assertEquals(401, status("GET", "/api/Pessoa", semAssinatura, null), "alg=none");
    }

    private static java.security.PrivateKey chaveEstranha() {
        try {
            var g = java.security.KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            return g.generateKeyPair().getPrivate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void leituraLiberadaParaQualquerAutenticado() {
        List<String> falhas = new ArrayList<>();
        List<String> todas = new ArrayList<>();
        todas.addAll(FLUXO);
        todas.addAll(SO_PLATAFORMA_ESCRITA);
        todas.addAll(CADASTROS);
        todas.addAll(List.of("VinculoUsuario", "ClienteAcesso", "Contato"));
        for (String e : todas) {
            porPapel(falhas, "GET", "/api/" + e, false, true, true, true, true);
            porPapel(falhas, "GET", "/api/" + e + "/count", false, true, true, true, true);
            porPapel(falhas, "GET", "/api/" + e + "/id/inexistente", false, true, true, true, true);
        }
        semFalhas(falhas);
    }

    @Test
    void escritaDiretaProibidaNasEntidadesDeFluxoParaTodos() {
        List<String> falhas = new ArrayList<>();
        for (String e : FLUXO) {
            for (String m : List.of("POST", "PUT", "PATCH", "DELETE")) {
                String c = m.equals("POST") ? "/api/" + e : "/api/" + e + "/id/qualquer";
                porPapel(falhas, m, c, false, false, false, false, false);
            }
            porPapel(falhas, "POST", "/api/" + e + "/", false, false, false, false, false);
            porPapel(falhas, "POST", "/api/" + e + "/qualquer/coisa", false, false, false, false, false);
        }
        semFalhas(falhas);
    }

    @Test
    void usuarioEGlobalSoAdministradorDaPlataforma() {
        List<String> falhas = new ArrayList<>();
        porPapel(falhas, "GET", "/api/Usuario", false, false, false, false, true);
        porPapel(falhas, "GET", "/api/Usuario/count", false, false, false, false, true);
        porPapel(falhas, "GET", "/api/Usuario/id/qualquer", false, false, false, false, true);
        porPapel(falhas, "POST", "/api/Usuario", false, false, false, false, true);
        porPapel(falhas, "PUT", "/api/Usuario/id/qualquer", false, false, false, false, true);
        porPapel(falhas, "PATCH", "/api/Usuario/id/qualquer", false, false, false, false, true);
        porPapel(falhas, "DELETE", "/api/Usuario/id/qualquer", false, false, false, false, false);
        semFalhas(falhas);
    }

    @Test
    void escritaDeCatalogosEEmpresaSoAdministradorDaPlataforma() {
        List<String> falhas = new ArrayList<>();
        for (String e : SO_PLATAFORMA_ESCRITA) {
            porPapel(falhas, "POST", "/api/" + e, false, false, false, false, true);
            porPapel(falhas, "PUT", "/api/" + e + "/id/qualquer", false, false, false, false, true);
            porPapel(falhas, "PATCH", "/api/" + e + "/id/qualquer", false, false, false, false, true);
            porPapel(falhas, "DELETE", "/api/" + e + "/id/qualquer", false, false, false, false, false);
        }
        semFalhas(falhas);
    }

    @Test
    void escritaDiretaDeContatoNegadaATodos() {
        List<String> falhas = new ArrayList<>();
        porPapel(falhas, "POST", "/api/Contato", false, false, false, false, false);
        porPapel(falhas, "PUT", "/api/Contato/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "PATCH", "/api/Contato/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/Contato/id/qualquer", false, false, false, false, false);
        semFalhas(falhas);
    }

    @Test
    void escritaDeCadastrosAdministradorOuOperadorEConsultaSoLe() {
        List<String> falhas = new ArrayList<>();
        for (String e : CADASTROS) {
            porPapel(falhas, "POST", "/api/" + e, false, false, true, true, true);
            porPapel(falhas, "PUT", "/api/" + e + "/id/qualquer", false, false, true, true, true);
            porPapel(falhas, "PATCH", "/api/" + e + "/id/qualquer", false, false, true, true, true);
        }
        semFalhas(falhas);
    }

    @Test
    void deleteNegadoEmTudo() {
        List<String> falhas = new ArrayList<>();
        porPapel(falhas, "DELETE", "/api/Pessoa/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/Servico/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/Empresa/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/Municipio/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/Plano/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/VinculoUsuario/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/ClienteAcesso/id/qualquer", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/Pessoa", false, false, false, false, false);
        porPapel(falhas, "DELETE", "/api/Qualquer/Coisa/Nova", false, false, false, false, false);
        // Contato tambem: so se gerencia pela Pessoa (PUT /api/Pessoa/{id}/contatos)
        porPapel(falhas, "DELETE", "/api/Contato/id/qualquer", false, false, false, false, false);
        semFalhas(falhas);
    }

    @Test
    void vinculoEClienteAcessoSaoSoLeitura() {
        List<String> falhas = new ArrayList<>();
        for (String e : List.of("VinculoUsuario", "ClienteAcesso")) {
            porPapel(falhas, "POST", "/api/" + e, false, false, false, false, false);
            porPapel(falhas, "PUT", "/api/" + e + "/id/qualquer", false, false, false, false, false);
            porPapel(falhas, "PATCH", "/api/" + e + "/id/qualquer", false, false, false, false, false);
            porPapel(falhas, "DELETE", "/api/" + e + "/id/qualquer", false, false, false, false, false);
        }
        semFalhas(falhas);
    }

    @Test
    void caminhosDesconhecidosEForaDeApiSaoFechados() {
        // desconhecido sob /api: anônimo 401, autenticado 404 (a política deixa e o recurso não existe)
        assertEquals(401, status("GET", "/api/NaoExiste", null, null));
        assertEquals(404, status("GET", "/api/NaoExiste", consulta, null));
        // endpoint novo/desconhecido nasce FECHADO para escrita (nem o administrador da plataforma escreve)
        assertEquals(403, status("POST", "/api/NaoExiste", plataforma, null));
        assertEquals(403, status("DELETE", "/api/NaoExiste/id/1", plataforma, null));
        // fora de /api e de /q publicados: negado por padrão (nada fica aberto por esquecimento)
        assertEquals(401, status("GET", "/qualquer-coisa", null, null));
        assertEquals(403, status("GET", "/qualquer-coisa", plataforma, null));
        assertEquals(401, status("GET", "/", null, null));
    }

    @Test
    void tentativasDeContornarOCaminho() {
        List<String> falhas = new ArrayList<>();
        // barra dupla, barra final, segmento com ponto, caso diferente, parâmetros de caminho
        for (String c : List.of("//api/Nota", "/api//Nota", "/api/Nota/", "/api/./Nota", "/api/Pessoa/../Nota",
                "/api/Nota;x=1", "/api/%4Eota", "/api/Nota%2F", "/api/Nota/%2e%2e/Nota", "/api/nota", "/API/Nota")) {
            int s = given().urlEncodingEnabled(false).contentType(ContentType.JSON).accept(ContentType.JSON)
                    .header("Authorization", "Bearer " + plataforma).body("{}").post(c).statusCode();
            // o que importa: nunca chegar a criar (2xx); 403/404/400/405 são todos fechados
            if (s >= 200 && s < 300) {
                falhas.add("POST " + c + " como plataforma devolveu " + s + " (contornou a negação)");
            }
        }
        for (String c : List.of("//api/Usuario", "/api/Usuario/", "/api/Usuario;x=1", "/api/%55suario", "/api//Usuario")) {
            int s = given().urlEncodingEnabled(false).accept(ContentType.JSON)
                    .header("Authorization", "Bearer " + consulta).get(c).statusCode();
            if (s >= 200 && s < 300) {
                falhas.add("GET " + c + " como consulta devolveu " + s + " (vazou Usuario)");
            }
        }
        semFalhas(falhas);
    }

    @Test
    void metodosNaoListadosNaoAbremBrecha() {
        // HEAD e OPTIONS em caminho de escrita proibida não podem escrever; e TRACE não passa
        List<String> falhas = new ArrayList<>();
        for (String m : List.of("HEAD", "OPTIONS", "TRACE")) {
            int s = status(m, "/api/Nota", consulta, null);
            if (s >= 200 && s < 300 && !m.equals("HEAD") && !m.equals("OPTIONS")) {
                falhas.add(m + " /api/Nota devolveu " + s);
            }
        }
        semFalhas(falhas);
    }

    @Test
    void operacoesReaisDeEscritaQueAPoliticaLibera() {
        // administrador da plataforma cria um Municipio de verdade; usuário comum não
        String ibge = String.valueOf(1000000 + (int) (Math.random() * 8999999));
        Map<String, Object> m = Map.of("codigoIbge", ibge, "nome", "Cidade " + ibge, "uf", "RJ");
        assertEquals(403, status("POST", "/api/Municipio", operador, m));
        assertEquals(403, status("POST", "/api/Municipio", administrador, m));
        Response criado = chamar("POST", "/api/Municipio", plataforma, m);
        assertEquals(200, criado.statusCode(), criado.asString());
        assertEquals(ibge, criado.jsonPath().getString("codigoIbge"));
        assertEquals(200, status("GET", "/api/Municipio/id/" + criado.jsonPath().getString("id"), consulta, null));

        // DELETE direto de Contato: negado a todos (o registro continua)
        String empresa = dados.empresaAtiva();
        String tk = token(dados.usuarioComPapel(empresa, "operador").email());
        String tkConsulta = token(dados.usuarioComPapel(empresa, "consulta").email());
        String contato = dados.contato(empresa, dados.pessoa(empresa, "Dona do contato"), "Contato a apagar");
        assertEquals(403, status("DELETE", "/api/Contato/id/" + contato, tkConsulta, null));
        assertEquals(403, status("DELETE", "/api/Contato/id/" + contato, tk, null));
        assertEquals(200, status("GET", "/api/Contato/id/" + contato, tk, null));

        // DELETE da Empresa negado até para o administrador da plataforma, e a empresa continua lá
        assertEquals(403, status("DELETE", "/api/Empresa/id/" + empresa, plataforma, null));
        assertEquals(200, status("GET", "/api/Empresa/id/" + empresa, plataforma, null));
    }
}
