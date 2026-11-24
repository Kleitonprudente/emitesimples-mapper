package br.com.emitesimples.cep;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** GET /api/cep/{cep}: o ViaCEP e SIMULADO (ViaCepStubResource); nenhum teste chama a internet. */
@QuarkusTest
class CepTest extends BaseApiTest {

    @Inject
    EntityManager em;

    @Inject
    CepService cepService;

    @Inject
    LimiteTaxa limiteTaxa;

    @ConfigProperty(name = "emitesimples.cep.url-base")
    String urlBase;

    private String token;

    private String token() {
        if (token == null) {
            token = token(dados.usuarioComPapel(dados.empresaAtiva(), "CONSULTA").email());
        }
        return token;
    }

    @AfterEach
    void restaurarRelogios() {
        cepService.usarRelogio(Clock.systemUTC());
        limiteTaxa.usarRelogio(Clock.systemUTC());
    }


    private void garantirMunicipioSp() {
        QuarkusTransaction.requiringNew().run(() -> {
            long n = em.createQuery("select count(m) from MunicipioEntity m where m.codigoIbge = '3550308'", Long.class).getSingleResult();
            if (n == 0) {
                em.persist(MunicipioEntity.builderOf().withCodigoIbge("3550308").withNome("São Paulo").withUf("SP").build());
            }
        });
    }

    private int chamadasAoStub(String cep) {
        String base = urlBase.substring(0, urlBase.length() - "/ws".length());
        return Integer.parseInt(given().get(base + "/_contagem/" + cep).asString().trim());
    }

    @Test
    void configuracaoDeTesteNaoApontaParaAInternet() {
        assertTrue(urlBase.startsWith("http://127.0.0.1:"), urlBase);
    }

    @Test
    void semTokenDa401() {
        chamar("GET", "/api/cep/01001000", null, null).then().statusCode(401);
    }

    @Test
    void qualquerPapelAutenticadoConsulta() {
        for (String papel : new String[] {"CONSULTA", "OPERADOR", "ADMINISTRADOR"}) {
            String t = token(dados.usuarioComPapel(dados.empresaAtiva(), papel).email());
            chamar("GET", "/api/cep/01001000", t, null).then().statusCode(200);
        }
        // administrador da plataforma sem empresa tambem
        chamar("GET", "/api/cep/01001000", token(dados.usuario(true, true).email()), null).then().statusCode(200);
    }

    @Test
    void escritaNegada() {
        for (String m : new String[] {"POST", "PUT", "PATCH", "DELETE"}) {
            assertEquals(403, status(m, "/api/cep/01001000", token(), null), m);
        }
    }

    @Test
    void respostaComMunicipioDaTabela() {
        garantirMunicipioSp();
        Response r = chamar("GET", "/api/cep/01001-000", token(), null);
        r.then().statusCode(200)
                .body("cep", equalTo("01001000"))
                .body("logradouro", equalTo("Praça da Sé"))
                .body("complemento", equalTo("lado ímpar"))
                .body("bairro", equalTo("Sé"))
                .body("uf", equalTo("SP"))
                .body("codigoIbge", equalTo("3550308"))
                .body("municipio.nome", equalTo("São Paulo"))
                .body("municipio.uf", equalTo("SP"))
                .body("municipio.codigoIbge", equalTo("3550308"))
                .body("municipio.id", not(nullValue()));
        // so os campos do contrato
        r.then().body("$", hasKey("municipio")).body("size()", equalTo(7));
    }

    @Test
    void municipioNuloQuandoATabelaNaoTemOCodigo() {
        chamar("GET", "/api/cep/20000000", token(), null).then().statusCode(200)
                .body("codigoIbge", equalTo("9999999"))
                .body("municipio", nullValue())
                .body("$", hasKey("municipio"));
    }

    @Test
    void formatoInvalidoDa400SemChamarOViaCep() {
        for (String ruim : new String[] {"123", "abcdefgh", "01001-00", "0100-1000", "010010000", "01001000x", "0100100-", "01001--000"}) {
            chamar("GET", "/api/cep/" + ruim, token(), null).then().statusCode(400).body("codigo", equalTo("cep_invalido"));
        }
        chamar("GET", "/api/cep/%20%20", token(), null).then().statusCode(400).body("codigo", equalTo("cep_invalido"));
    }

    @Test
    void cepInexistenteDa404() {
        chamar("GET", "/api/cep/99999999", token(), null).then().statusCode(404).body("codigo", equalTo("cep_nao_encontrado"));
        chamar("GET", "/api/cep/88888888", token(), null).then().statusCode(404).body("codigo", equalTo("cep_nao_encontrado")); // "erro": "true"
    }

    @Test
    void falhaDoServicoDa502GenericoENaoFicaEmCache() {
        for (String cep : new String[] {"50000000", "70000000"}) {
            int antes = chamadasAoStub(cep);
            Response r = chamar("GET", "/api/cep/" + cep, token(), null);
            r.then().statusCode(502).body("codigo", equalTo("servico_cep_indisponivel"));
            String corpo = r.asString();
            assertTrue(!corpo.contains("127.0.0.1") && !corpo.contains("http") && !corpo.contains("Exception") && !corpo.contains("at br."),
                    "a mensagem nao pode vazar URL nem pilha: " + corpo);
            chamar("GET", "/api/cep/" + cep, token(), null).then().statusCode(502);
            assertEquals(antes + 2, chamadasAoStub(cep), "falha nao entra no cache");
        }
    }

    @Test
    void timeoutDa502() {
        long t0 = System.nanoTime();
        chamar("GET", "/api/cep/60000000", token(), null).then().statusCode(502).body("codigo", equalTo("servico_cep_indisponivel"));
        assertTrue(Duration.ofNanos(System.nanoTime() - t0).toMillis() < 4500, "o timeout curto tem de cortar antes dos 5 s do stub");
    }

    @Test
    void cacheEvitaNovaChamadaEExpiraEm24h() {
        String cep = "01310100";
        assertEquals(0, chamadasAoStub(cep));
        chamar("GET", "/api/cep/" + cep, token(), null).then().statusCode(200);
        chamar("GET", "/api/cep/01310-100", token(), null).then().statusCode(200); // mesma chave, com hifen
        assertEquals(1, chamadasAoStub(cep), "a segunda consulta vem do cache");

        // 23h depois ainda vale; 25h depois consulta de novo
        Instant agora = Instant.now();
        cepService.usarRelogio(Clock.fixed(agora.plus(Duration.ofHours(23)), ZoneOffset.UTC));
        chamar("GET", "/api/cep/" + cep, token(), null).then().statusCode(200);
        assertEquals(1, chamadasAoStub(cep));
        cepService.usarRelogio(Clock.fixed(agora.plus(Duration.ofHours(25)), ZoneOffset.UTC));
        chamar("GET", "/api/cep/" + cep, token(), null).then().statusCode(200);
        assertEquals(2, chamadasAoStub(cep));
    }

    @Test
    void cepNaoEncontradoTambemFicaEmCache() {
        // reutiliza 99999999 do teste de 404: o contador so cresce uma vez por janela de cache
        String cep = "99999999";
        chamar("GET", "/api/cep/" + cep, token(), null).then().statusCode(404);
        int depoisDaPrimeira = chamadasAoStub(cep);
        chamar("GET", "/api/cep/" + cep, token(), null).then().statusCode(404);
        assertEquals(depoisDaPrimeira, chamadasAoStub(cep));
    }

    @Test
    void limitePorUsuarioDa429ComRetryAfter() {
        String a = token(dados.usuarioComPapel(dados.empresaAtiva(), "CONSULTA").email());
        String b = token(dados.usuarioComPapel(dados.empresaAtiva(), "CONSULTA").email());
        for (int i = 0; i < 30; i++) {
            chamar("GET", "/api/cep/01001000", a, null).then().statusCode(200);
        }
        Response r = chamar("GET", "/api/cep/01001000", a, null);
        r.then().statusCode(429).body("codigo", equalTo("muitas_tentativas")).header("Retry-After", containsString(""));
        long retry = Long.parseLong(r.getHeader("Retry-After"));
        assertTrue(retry >= 1 && retry <= 60, "Retry-After=" + retry);
        // outro usuario nao e afetado
        chamar("GET", "/api/cep/01001000", b, null).then().statusCode(200);
        // passada a janela de 1 minuto, o usuario volta
        limiteTaxa.usarRelogio(Clock.fixed(Instant.now().plus(Duration.ofSeconds(61)), ZoneOffset.UTC));
        chamar("GET", "/api/cep/01001000", a, null).then().statusCode(200);
    }
}
