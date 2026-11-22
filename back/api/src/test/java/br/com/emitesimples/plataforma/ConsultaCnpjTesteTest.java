package br.com.emitesimples.plataforma;

import static br.com.emitesimples.apoio.CnpjWsStubResource.cnpj;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.CnpjWsStubResource;
import br.com.emitesimples.security.ApiException;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** POST /api/admin/configuracoes/consulta-cnpj/testar: o CNPJ.ws e SIMULADO (CnpjWsStubResource); sem internet. */
@QuarkusTest
class ConsultaCnpjTesteTest extends BaseApiTest {

    private static final String CFG = "/api/admin/configuracoes/consulta-cnpj";
    private static final String URL = CFG + "/testar";
    private static final String BASE_PADRAO = "https://comercial.cnpj.ws/cnpj";

    @ConfigProperty(name = CnpjWsStubResource.PROP_URL)
    String urlStub;

    @Inject
    LimiteTesteCnpj limite;

    private String admin;

    private String admin() {
        if (admin == null) {
            admin = token(dados.usuario(true, true).email());
        }
        return admin;
    }

    private void configurar(String token) {
        Map<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("urlBase", urlStub);
        corpo.put("token", token == null ? "" : token);
        chamar("PUT", CFG, admin(), corpo).then().statusCode(204);
    }

    @BeforeEach
    void preparar() {
        limite.limparTudo();
        configurar("tok-teste-" + System.nanoTime());
    }

    @AfterEach
    void restaurar() {
        chamar("PUT", CFG, admin(), Map.of("urlBase", BASE_PADRAO, "token", "")).then().statusCode(204);
        limite.limparTudo();
    }

    private Response testar(String cnpj) {
        return chamar("POST", URL, admin(), Map.of("cnpj", cnpj));
    }

    private String stub(String caminho) {
        return given().get(urlStub.substring(0, urlStub.length() - "/cnpj".length()) + caminho).asString().trim();
    }

    // ---------------------------------------------------------------------------------------------- sucesso

    @Test
    void mapeiaTodosOsCamposDoResumo() {
        String cnpj = "27865757000102";
        assertEquals(cnpj, cnpj(CnpjWsStubResource.RAIZ_COMPLETO), "o CNPJ de exemplo e valido");
        testar(cnpj).then().statusCode(200)
                .body("cnpj", equalTo(cnpj))
                .body("razaoSocial", equalTo("GLOBO COMUNICACAO E PARTICIPACOES S/A"))
                .body("nomeFantasia", equalTo("TV GLOBO"))
                .body("situacaoCadastral", equalTo("Ativa"))
                .body("dataInicioAtividade", equalTo("1998-01-05"))
                .body("porte", equalTo("Demais"))
                .body("naturezaJuridica", equalTo("Sociedade Anônima Aberta"))
                .body("atividadePrincipal.codigo", equalTo("6021-7/00"))
                .body("atividadePrincipal.descricao", equalTo("Atividades de televisão aberta"))
                .body("capitalSocial", equalTo("1273650139.00"))
                .body("endereco.logradouro", equalTo("RUA LOPES QUINTAS"))
                .body("endereco.numero", equalTo("303"))
                .body("endereco.complemento", equalTo("PARTE"))
                .body("endereco.bairro", equalTo("JARDIM BOTANICO"))
                .body("endereco.cep", equalTo("22460901"))
                .body("endereco.municipio", equalTo("Rio de Janeiro"))
                .body("endereco.uf", equalTo("RJ"))
                .body("email", equalTo("contato@exemplo.com.br"))
                .body("telefone", equalTo("2125402000"))
                .body("simples", equalTo(false))
                .body("mei", equalTo(true))
                .body("quantidadeSocios", equalTo(3))
                .body("duracaoMs", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
                .body("urlConsultada", equalTo(urlStub + "/" + cnpj));
    }

    @Test
    void cnpjComPontuacaoEAceito() {
        testar("27.865.757/0001-02").then().statusCode(200).body("cnpj", equalTo("27865757000102"));
    }

    @Test
    void campoAusenteViraNullSemFalhar() {
        Response r = testar(cnpj(CnpjWsStubResource.RAIZ_MINIMO));
        r.then().statusCode(200).body("razaoSocial", equalTo("Empresa Minima Ltda"))
                .body("nomeFantasia", nullValue()).body("situacaoCadastral", nullValue())
                .body("dataInicioAtividade", nullValue()).body("porte", nullValue())
                .body("naturezaJuridica", nullValue()).body("atividadePrincipal", nullValue())
                .body("capitalSocial", nullValue()).body("endereco", nullValue()).body("email", nullValue())
                .body("telefone", nullValue()).body("simples", nullValue()).body("mei", nullValue())
                .body("quantidadeSocios", equalTo(0));
        // as chaves existem (null explicito)
        r.then().body("$", hasKey("nomeFantasia")).body("$", hasKey("endereco"));
    }

    // ---------------------------------------------------------------------------------------------- token

    @Test
    void tokenVaiComoQueryAoStubENuncaVoltaNemVaiParaLog() {
        String token = "tok-SECRETO-" + System.nanoTime() + "&x=1 %";
        configurar(token.replace(" ", "_"));
        String enviado = token.replace(" ", "_");
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response ok;
        Response erro;
        try {
            ok = testar("27865757000102");
            erro = testar(cnpj(CnpjWsStubResource.RAIZ_401));
            Response invalido = testar("123");
            assertEquals(400, invalido.statusCode());
        } finally {
            descapturar(h);
        }
        assertEquals(200, ok.statusCode());
        assertEquals(502, erro.statusCode());
        assertTrue(Arrays.asList(stub("/_tokens").split("\n")).contains(enviado), "o stub recebeu o token na query");
        for (Response r : new Response[] {ok, erro}) {
            assertFalse(r.asString().contains(enviado), "token na resposta: " + r.asString());
            assertFalse(r.asString().contains("token="), r.asString());
            assertFalse(r.getHeaders().toString().contains(enviado));
        }
        assertFalse(ok.jsonPath().getString("urlConsultada").contains("token"));
        assertFalse(logs.isEmpty(), "ha log da consulta");
        for (String l : logs) {
            assertFalse(l.contains(enviado) || l.contains("tok-SECRETO") || l.contains("token="), "token em log: " + l);
            assertFalse(l.contains("27865757000102"), "CNPJ completo em log: " + l);
        }
        assertTrue(logs.stream().anyMatch(l -> l.contains("27.865.757/****-**")), "CNPJ mascarado no log");
    }

    @Test
    void semTokenSalvoDa409() {
        configurar(null);
        int antes = Integer.parseInt(stub("/_contagem/27865757000102"));
        testar("27865757000102").then().statusCode(409).body("codigo", equalTo("token_nao_configurado"));
        assertEquals(String.valueOf(antes), stub("/_contagem/27865757000102"), "nao chamou o servico externo");
    }

    // ---------------------------------------------------------------------------------------------- entrada

    @Test
    void cnpjInvalidoDa400() {
        for (String ruim : new String[] {"27865757000103", "11111111111111", "123", "abc", "2786575700010a", "",
                "27865757000102999"}) {
            testar(ruim).then().statusCode(400).body("codigo", equalTo("cnpj_invalido"));
        }
        chamar("POST", URL, admin(), Map.of()).then().statusCode(400).body("codigo", equalTo("cnpj_invalido"));
    }

    // ---------------------------------------------------------------------------------------------- erros do provedor

    @Test
    void errosDoProvedor() {
        testar(cnpj(CnpjWsStubResource.RAIZ_404)).then().statusCode(404).body("codigo", equalTo("cnpj_nao_encontrado"));
        testar(cnpj(CnpjWsStubResource.RAIZ_401)).then().statusCode(502).body("codigo", equalTo("token_recusado"));
        testar(cnpj(CnpjWsStubResource.RAIZ_403)).then().statusCode(502).body("codigo", equalTo("token_recusado"));
        testar(cnpj(CnpjWsStubResource.RAIZ_429)).then().statusCode(429).body("codigo", equalTo("limite_consulta_excedido"))
                .body("detalhes.esperaSegundos", equalTo(30));
        testar(cnpj(CnpjWsStubResource.RAIZ_429_SEM_ESPERA)).then().statusCode(429)
                .body("codigo", equalTo("limite_consulta_excedido")).body("detalhes", nullValue());
        testar(cnpj(CnpjWsStubResource.RAIZ_500)).then().statusCode(502).body("codigo", equalTo("resposta_invalida"));
    }

    @Test
    void respostaLentaEstouraOTimeoutEDa504() {
        long ini = System.nanoTime();
        testar(cnpj(CnpjWsStubResource.RAIZ_LENTO)).then().statusCode(504)
                .body("codigo", equalTo("servico_cnpj_indisponivel"));
        assertTrue((System.nanoTime() - ini) / 1_000_000 < 3500, "respondeu no timeout, nao esperou o stub");
    }

    @Test
    void servidorForaDoArDa504() {
        // porta fechada em loopback
        chamar("PUT", CFG, admin(), Map.of("urlBase", "http://127.0.0.1:1/cnpj")).then().statusCode(204);
        Response r = testar("27865757000102");
        r.then().statusCode(504).body("codigo", equalTo("servico_cnpj_indisponivel"));
        assertFalse(r.asString().contains("127.0.0.1"), "sem host/URL na mensagem");
    }

    @Test
    void respostaInvalidaOuGiganteOuRedirecionamentoDa502() {
        for (String raiz : new String[] {CnpjWsStubResource.RAIZ_JSON_INVALIDO, CnpjWsStubResource.RAIZ_ARRAY,
                CnpjWsStubResource.RAIZ_GIGANTE_CHUNKED, CnpjWsStubResource.RAIZ_GIGANTE_LENGTH,
                CnpjWsStubResource.RAIZ_REDIRECT}) {
            testar(cnpj(raiz)).then().statusCode(502).body("codigo", equalTo("resposta_invalida"));
        }
        assertEquals("0", stub("/_contagem/_redirecionado"), "o redirecionamento nao foi seguido");
    }

    // ---------------------------------------------------------------------------------------------- acesso e limite

    @Test
    void soAdministradorDaPlataforma() {
        Map<String, String> corpo = Map.of("cnpj", "27865757000102");
        assertEquals(401, status("POST", URL, null, corpo));
        for (String papel : new String[] {"consulta", "operador", "administrador"}) {
            String t = token(dados.usuarioComPapel(dados.empresaAtiva(), papel).email());
            assertEquals(403, status("POST", URL, t, corpo), papel);
        }
        assertEquals(200, status("POST", URL, admin(), corpo));
    }

    @Test
    void postSoLiberadoNoCaminhoDoTeste() {
        // o resto de /api/admin/configuracoes segue sem POST, ate para a plataforma
        assertEquals(403, status("POST", CFG, admin(), Map.of()));
        assertEquals(403, status("POST", "/api/admin/configuracoes/outra", admin(), Map.of()));
        assertEquals(403, status("POST", "/api/admin/configuracoes/consulta-cnpj/outra", admin(), Map.of()));
        // e o caminho do teste so aceita POST
        for (String m : new String[] {"GET", "PUT", "PATCH", "DELETE"}) {
            assertEquals(403, status(m, URL, admin(), null), m);
        }
    }

    @Test
    void limiteProprioDeDezPorMinutoPorUsuario() {
        String c = cnpj(CnpjWsStubResource.RAIZ_MINIMO);
        int antes = Integer.parseInt(stub("/_contagem/" + c));
        for (int i = 0; i < 10; i++) {
            testar(c).then().statusCode(200);
        }
        Response r = testar(c);
        r.then().statusCode(429).body("codigo", equalTo("muitas_tentativas"));
        assertTrue(Integer.parseInt(r.getHeader("Retry-After")) >= 1);
        assertEquals(String.valueOf(antes + 10), stub("/_contagem/" + c), "a 11a nao chegou ao CNPJ.ws");
        // outro usuario nao e afetado
        String outro = token(dados.usuario(true, true).email());
        assertEquals(200, status("POST", URL, outro, Map.of("cnpj", c)));
    }

    // ---------------------------------------------------------------------------------------------- SSRF (unidade)

    @Test
    void hostQueResolveParaIpInternoERecusado() throws Exception {
        for (String ip : new String[] {"10.0.0.5", "172.16.0.1", "192.168.1.1", "127.0.0.1", "169.254.169.254", "100.64.0.1",
                "0.0.0.0", "224.0.0.1", "::1", "fe80::1", "fc00::1", "fd12:3456::1", "::ffff:10.0.0.1"}) {
            ApiException e = assertThrows(ApiException.class,
                    () -> ConsultaCnpjCliente.verificarEnderecos(new InetAddress[] {InetAddress.getByName(ip)}), ip);
            assertEquals("url_base_nao_permitida", e.getCodigo(), ip);
        }
        // QUALQUER IP interno entre os resolvidos basta
        assertThrows(ApiException.class, () -> ConsultaCnpjCliente.verificarEnderecos(
                new InetAddress[] {InetAddress.getByName("8.8.8.8"), InetAddress.getByName("10.0.0.5")}));
        // publico passa
        ConsultaCnpjCliente.verificarEnderecos(new InetAddress[] {InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("2606:4700:4700::1111")});
        assertThrows(ApiException.class, () -> ConsultaCnpjCliente.verificarEnderecos(new InetAddress[0]));
    }

    @Test
    void localhostResolvidoERecusadoQuandoUrlLocalNaoEPermitida() {
        ConsultaCnpjCliente c = new ConsultaCnpjCliente();
        c.permitirUrlLocal = false;
        assertEquals("url_base_nao_permitida", assertThrows(ApiException.class, () -> c.verificarHost("localhost")).getCodigo());
        c.permitirUrlLocal = true;
        c.verificarHost("localhost"); // permitido so com a propriedade
    }

    @Test
    void validadorDeCnpj() {
        assertEquals("27865757000102", ConsultaCnpjTesteService.normalizar(" 27.865.757/0001-02 "));
        assertEquals("27865757000102", ConsultaCnpjTesteService.normalizar("27865757000102"));
        assertThrows(ApiException.class, () -> ConsultaCnpjTesteService.normalizar("27865757000112"));
        assertThrows(ApiException.class, () -> ConsultaCnpjTesteService.normalizar(null));
        assertEquals("27.865.757/****-**", ConsultaCnpjCliente.mascarar("27865757000102"));
    }

    // ---------------------------------------------------------------------------------------------- logs

    private static Handler capturarLogs(List<String> destino) {
        Handler h = new Handler() {
            @Override
            public void publish(LogRecord r) {
                StringBuilder sb = new StringBuilder(String.valueOf(r.getMessage()));
                if (r.getParameters() != null) {
                    sb.append(Arrays.toString(r.getParameters()));
                }
                for (Throwable t = r.getThrown(); t != null; t = t.getCause()) {
                    sb.append(' ').append(t);
                }
                synchronized (destino) {
                    destino.add(sb.toString());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        LogManager.getLogManager().getLogger("").addHandler(h);
        return h;
    }

    private static void descapturar(Handler h) {
        LogManager.getLogManager().getLogger("").removeHandler(h);
    }
}
