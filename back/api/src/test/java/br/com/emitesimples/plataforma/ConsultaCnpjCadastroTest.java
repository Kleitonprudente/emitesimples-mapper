package br.com.emitesimples.plataforma;

import static br.com.emitesimples.apoio.CnpjWsStubResource.cnpj;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.CnpjWsStubResource;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

/** GET /api/admin/consulta-cnpj/{cnpj}: CNPJ.ws SIMULADO (CnpjWsStubResource); sem internet. */
@QuarkusTest
class ConsultaCnpjCadastroTest extends BaseApiTest {

    private static final String CFG = "/api/admin/configuracoes/consulta-cnpj";
    private static final String BASE = "/api/admin/consulta-cnpj/";
    private static final String BASE_PADRAO = "https://comercial.cnpj.ws/cnpj";

    @ConfigProperty(name = CnpjWsStubResource.PROP_URL)
    String urlStub;

    @Inject
    LimiteConsultaCnpj limite;

    @Inject
    EntityManager em;

    private String admin;
    private String municipioId;

    private String admin() {
        if (admin == null) {
            admin = token(dados.usuario(true, true).email());
        }
        return admin;
    }

    private void configurar(String token) {
        chamar("PUT", CFG, admin(), Map.of("urlBase", urlStub, "token", token == null ? "" : token)).then().statusCode(204);
    }

    @BeforeEach
    void preparar() {
        limite.limparTudo();
        configurar("tok-cad-" + System.nanoTime());
        if (municipioId == null) {
            municipioId = QuarkusTransaction.requiringNew().call(() -> {
                List<MunicipioEntity> l = em.createQuery("select m from MunicipioEntity m where m.codigoIbge = :c",
                        MunicipioEntity.class).setParameter("c", CnpjWsStubResource.IBGE_CADASTRO).getResultList();
                if (!l.isEmpty()) {
                    return l.get(0).getId();
                }
                MunicipioEntity m = MunicipioEntity.builderOf().withCodigoIbge(CnpjWsStubResource.IBGE_CADASTRO)
                        .withNome("Cidade do Banco").withUf("RJ").build();
                em.persist(m);
                return m.getId();
            });
        }
    }

    @AfterEach
    void restaurar() {
        chamar("PUT", CFG, admin(), Map.of("urlBase", BASE_PADRAO, "token", "")).then().statusCode(204);
        limite.limparTudo();
    }

    private Response consultar(String cnpj) {
        return chamar("GET", BASE + cnpj, admin(), null);
    }

    private String stub(String caminho) {
        return given().get(urlStub.substring(0, urlStub.length() - "/cnpj".length()) + caminho).asString().trim();
    }

    @Test
    void mapeiaTodosOsCamposEResolveOMunicipio() {
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        consultar(c).then().statusCode(200)
                .body("cnpj", equalTo(c))
                .body("razaoSocial", equalTo("CADASTRO TESTE LTDA"))
                .body("nomeFantasia", equalTo("CADASTRO FANTASIA"))
                .body("email", equalTo("cad@exemplo.com.br"))
                .body("telefone", equalTo("1133334444"))
                .body("cep", equalTo("01310100"))
                .body("logradouro", equalTo("AVENIDA PAULISTA"))
                .body("numeroEndereco", equalTo("1000"))
                .body("complemento", equalTo("SALA 5"))
                .body("bairro", equalTo("BELA VISTA"))
                .body("municipio.id", equalTo(municipioId))
                .body("municipio.nome", equalTo("Cidade do Banco"))
                .body("municipio.uf", equalTo("RJ"))
                .body("municipio.codigoIbge", equalTo(CnpjWsStubResource.IBGE_CADASTRO))
                .body("uf", equalTo("RJ"))
                .body("codigoIbge", equalTo(CnpjWsStubResource.IBGE_CADASTRO))
                .body("regimeTributario", equalTo(1))
                .body("inscricaoEstadual", equalTo("333333333"))
                .body("situacaoCadastral", equalTo("Ativa"))
                .body("dataInicioAtividade", equalTo("2010-03-15"))
                .body("socios.size()", equalTo(2))
                .body("socios[0].nome", equalTo("MARIA SOCIA"))
                .body("socios[0].tipo", equalTo("Pessoa Física"))
                .body("socios[0].qualificacao", equalTo("Sócio-Administrador"))
                .body("socios[1].nome", equalTo("JOAO SEM QUALIFICACAO"))
                .body("socios[1].tipo", nullValue())
                .body("socios[1].qualificacao", nullValue());
        consultar(cnpj(CnpjWsStubResource.RAIZ_MINIMO)).then().statusCode(200).body("socios", equalTo(List.of()));
    }

    @Test
    void municipioNaoAchadoViraNullMasMantemUfEIbge() {
        consultar(cnpj(CnpjWsStubResource.RAIZ_CAD_NORMAL)).then().statusCode(200)
                .body("municipio", nullValue()).body("codigoIbge", equalTo("9199998")).body("uf", equalTo("RJ"));
    }

    @Test
    void regimeSugeridoMeiSimplesOuNull() {
        consultar(cnpj(CnpjWsStubResource.RAIZ_CAD_MEI)).then().statusCode(200).body("regimeTributario", equalTo(4));
        consultar(cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES)).then().body("regimeTributario", equalTo(1));
        Response normal = consultar(cnpj(CnpjWsStubResource.RAIZ_CAD_NORMAL));
        normal.then().statusCode(200).body("regimeTributario", nullValue()).body("inscricaoEstadual", nullValue());
        normal.then().body("$", hasKey("regimeTributario"));
        // resposta minima: tudo null, sem falhar
        consultar(cnpj(CnpjWsStubResource.RAIZ_MINIMO)).then().statusCode(200)
                .body("razaoSocial", equalTo("Empresa Minima Ltda")).body("regimeTributario", nullValue())
                .body("municipio", nullValue()).body("telefone", nullValue()).body("cep", nullValue())
                .body("logradouro", nullValue()).body("$", hasKey("uf"));
    }

    @Test
    void cnpjComPontuacaoEAceito() {
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        String pontuado = c.substring(0, 2) + "." + c.substring(2, 5) + "." + c.substring(5, 8) + "-" + c.substring(8, 12)
                + "-" + c.substring(12);
        consultar(pontuado).then().statusCode(200).body("cnpj", equalTo(c));
    }

    @Test
    void cnpjInvalidoDa400() {
        for (String ruim : new String[] {"27865757000103", "11111111111111", "123", "abc", "2786575700010a"}) {
            consultar(ruim).then().statusCode(400).body("codigo", equalTo("cnpj_invalido"));
        }
    }

    @Test
    void semTokenSalvoDa409() {
        configurar(null);
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        int antes = Integer.parseInt(stub("/_contagem/" + c));
        consultar(c).then().statusCode(409).body("codigo", equalTo("token_nao_configurado"));
        assertEquals(String.valueOf(antes), stub("/_contagem/" + c));
    }

    @Test
    void errosDoProvedor() {
        consultar(cnpj(CnpjWsStubResource.RAIZ_404)).then().statusCode(404).body("codigo", equalTo("cnpj_nao_encontrado"));
        consultar(cnpj(CnpjWsStubResource.RAIZ_401)).then().statusCode(502).body("codigo", equalTo("token_recusado"));
        consultar(cnpj(CnpjWsStubResource.RAIZ_429)).then().statusCode(429).body("codigo", equalTo("limite_consulta_excedido"))
                .body("detalhes.esperaSegundos", equalTo(30));
        consultar(cnpj(CnpjWsStubResource.RAIZ_500)).then().statusCode(502).body("codigo", equalTo("resposta_invalida"));
        consultar(cnpj(CnpjWsStubResource.RAIZ_JSON_INVALIDO)).then().statusCode(502).body("codigo", equalTo("resposta_invalida"));
        consultar(cnpj(CnpjWsStubResource.RAIZ_GIGANTE_CHUNKED)).then().statusCode(502).body("codigo", equalTo("resposta_invalida"));
        consultar(cnpj(CnpjWsStubResource.RAIZ_REDIRECT)).then().statusCode(502).body("codigo", equalTo("resposta_invalida"));
        consultar(cnpj(CnpjWsStubResource.RAIZ_LENTO)).then().statusCode(504).body("codigo", equalTo("servico_cnpj_indisponivel"));
        chamar("PUT", CFG, admin(), Map.of("urlBase", "http://127.0.0.1:1/cnpj")).then().statusCode(204);
        consultar(cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES)).then().statusCode(504)
                .body("codigo", equalTo("servico_cnpj_indisponivel"));
    }

    @Test
    void tokenNuncaApareceNaRespostaNemNosLogs() {
        String token = "tok-SECRETO-" + System.nanoTime();
        configurar(token);
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response ok;
        Response erro;
        try {
            ok = consultar(cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES));
            erro = consultar(cnpj(CnpjWsStubResource.RAIZ_401));
        } finally {
            LogManager.getLogManager().getLogger("").removeHandler(h);
        }
        assertEquals(200, ok.statusCode());
        assertTrue(Arrays.asList(stub("/_tokens").split("\n")).contains(token));
        for (Response r : new Response[] {ok, erro}) {
            assertFalse(r.asString().contains(token), r.asString());
            assertFalse(r.getHeaders().toString().contains(token));
        }
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        assertFalse(logs.isEmpty());
        for (String l : logs) {
            assertFalse(l.contains(token) || l.contains("token="), "token em log: " + l);
            assertFalse(l.contains(c), "CNPJ completo em log: " + l);
        }
    }

    @Test
    void soAdministradorDaPlataformaEApenasGet() {
        String c = cnpj(CnpjWsStubResource.RAIZ_CAD_SIMPLES);
        assertEquals(401, status("GET", BASE + c, null, null));
        for (String papel : new String[] {"CONSULTA", "OPERADOR", "ADMINISTRADOR"}) {
            String t = token(dados.usuarioComPapel(dados.empresaAtiva(), papel).email());
            assertEquals(403, status("GET", BASE + c, t, null), papel);
        }
        assertEquals(200, status("GET", BASE + c, admin(), null));
        for (String m : new String[] {"POST", "PUT", "PATCH", "DELETE"}) {
            assertEquals(403, status(m, BASE + c, admin(), Map.of()), m);
        }
    }

    @Test
    void limiteDeTrintaPorMinutoPorUsuario() {
        String c = cnpj(CnpjWsStubResource.RAIZ_MINIMO);
        int antes = Integer.parseInt(stub("/_contagem/" + c));
        for (int i = 0; i < 30; i++) {
            consultar(c).then().statusCode(200);
        }
        Response r = consultar(c);
        r.then().statusCode(429).body("codigo", equalTo("muitas_tentativas"));
        assertTrue(Integer.parseInt(r.getHeader("Retry-After")) >= 1);
        assertEquals(String.valueOf(antes + 30), stub("/_contagem/" + c));
        String outro = token(dados.usuario(true, true).email());
        assertEquals(200, status("GET", BASE + c, outro, null));
    }

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
}
