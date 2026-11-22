package br.com.emitesimples.security;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ISOLAMENTO ENTRE EMPRESAS. Duas empresas com os mesmos tipos de dado; o usuário da A nunca pode ver nem mexer no da B.
 * O filtro é do código GERADO (repositórios + EscopoTenant); aqui só se prova o comportamento de ponta a ponta.
 */
@QuarkusTest
class IsolamentoEmpresaTest extends BaseApiTest {

    @Inject
    EntityManager em;

    @Inject
    JwtEmissor emissor;

    static String empA, empB, pesA, pesB, srvA, srvB, conA, conB, notA, notB;
    static String tokenA, tokenAConsulta, tokenB, tokenPlat;

    @BeforeEach
    void preparar() {
        if (empA != null) {
            return;
        }
        empA = dados.empresaAtiva();
        empB = dados.empresaAtiva();
        pesA = dados.pessoa(empA, "Pessoa da A");
        pesB = dados.pessoa(empB, "Pessoa da B");
        srvA = dados.servico(empA, "Servico da A");
        srvB = dados.servico(empB, "Servico da B");
        conA = dados.contato(empA, pesA, "Contato da A");
        conB = dados.contato(empB, pesB, "Contato da B");
        notA = dados.nota(empA, pesA, srvA);
        notB = dados.nota(empB, pesB, srvB);
        tokenA = token(dados.usuarioComPapel(empA, "operador").email());
        tokenAConsulta = token(dados.usuarioComPapel(empA, "consulta").email());
        tokenB = token(dados.usuarioComPapel(empB, "administrador").email());
        tokenPlat = token(dados.usuario(true, true).email());
    }

    private record Rec(String entidade, String idA, String idB) {
    }

    private List<Rec> recursos() {
        return List.of(new Rec("Pessoa", pesA, pesB), new Rec("Servico", srvA, srvB),
                new Rec("Contato", conA, conB), new Rec("Nota", notA, notB));
    }

    @Test
    void listagemDaEmpresaANuncaTrazDadosDaB() {
        for (Rec r : recursos()) {
            for (String tk : List.of(tokenA, tokenAConsulta)) {
                comToken(tk).get("/api/" + r.entidade()).then().statusCode(200)
                        .body("id", hasItem(r.idA()))
                        .body("id", not(hasItem(r.idB())))
                        .body("empresa.id", everyItem(equalTo(empA)));
                // paginação gerada
                comToken(tk).get("/api/" + r.entidade() + "/0/50").then().statusCode(200)
                        .body("id", not(hasItem(r.idB())));
                // contagem só enxerga a própria empresa
                comToken(tk).get("/api/" + r.entidade() + "/count").then().statusCode(200).body("count", equalTo((int) contarDe(r.entidade(), empA)));
            }
        }
    }

    @Test
    void filtroPorEmpresaIdDeOutraEmpresaNaoFuraOEscopo() {
        for (Rec r : recursos()) {
            comToken(tokenA).queryParam("empresa.id", empB).get("/api/" + r.entidade()).then().statusCode(200)
                    .body("$", empty());
            comToken(tokenA).queryParam("id", r.idB()).get("/api/" + r.entidade()).then().statusCode(200)
                    .body("$", empty());
        }
    }

    @Test
    void buscaPorIdDeOutraEmpresaDa404() {
        for (Rec r : recursos()) {
            comToken(tokenA).get("/api/" + r.entidade() + "/id/" + r.idB()).then().statusCode(404)
                    .body("codigo", equalTo("nao_encontrado"));
            comToken(tokenA).get("/api/" + r.entidade() + "/id/" + r.idA()).then().statusCode(200)
                    .body("id", equalTo(r.idA()));
            // simetria: a B também não vê a A
            comToken(tokenB).get("/api/" + r.entidade() + "/id/" + r.idA()).then().statusCode(404);
            comToken(tokenB).get("/api/" + r.entidade() + "/id/" + r.idB()).then().statusCode(200);
        }
    }

    @Test
    void empresaSoVeASiMesma() {
        comToken(tokenA).get("/api/Empresa").then().statusCode(200).body("id", hasSize(1)).body("id[0]", equalTo(empA));
        comToken(tokenA).get("/api/Empresa/id/" + empB).then().statusCode(404);
        comToken(tokenA).get("/api/Empresa/id/" + empA).then().statusCode(200);
    }

    @Test
    void vinculosListadosSaoSoDaPropriaEmpresa() {
        comToken(tokenA).get("/api/VinculoUsuario").then().statusCode(200)
                .body("empresa.id", everyItem(equalTo(empA)));
    }

    @Test
    void escritaPorIdEmRegistroDeOutraEmpresaDa404ENaoAltera() {
        Map<String, Object> corpo = Map.of("tipoPessoa", "fisica", "nome", "INVADIDA", "empresa", Map.of("id", empB));
        comToken(tokenA).body(corpo).put("/api/Pessoa/id/" + pesB).then().statusCode(404);
        comToken(tokenA).body(Map.of("nome", "INVADIDA")).patch("/api/Pessoa/id/" + pesB).then().statusCode(404);
        comToken(tokenA).delete("/api/Contato/id/" + conB).then().statusCode(403);
        // nada mudou na B
        comToken(tokenB).get("/api/Pessoa/id/" + pesB).then().statusCode(200).body("nome", equalTo("Pessoa da B"));
        comToken(tokenB).get("/api/Contato/id/" + conB).then().statusCode(200).body("nome", equalTo("Contato da B"));
    }

    @Test
    void administradorDaPlataformaEnxergaAsDuasEmpresas() {
        for (Rec r : recursos()) {
            comToken(tokenPlat).queryParam("empresa.id", empA).get("/api/" + r.entidade()).then().statusCode(200)
                    .body("id", hasItem(r.idA()));
            comToken(tokenPlat).queryParam("empresa.id", empB).get("/api/" + r.entidade()).then().statusCode(200)
                    .body("id", hasItem(r.idB()));
            comToken(tokenPlat).get("/api/" + r.entidade() + "/id/" + r.idA()).then().statusCode(200);
            comToken(tokenPlat).get("/api/" + r.entidade() + "/id/" + r.idB()).then().statusCode(200);
        }
        comToken(tokenPlat).get("/api/Empresa/id/" + empA).then().statusCode(200);
        comToken(tokenPlat).get("/api/Empresa/id/" + empB).then().statusCode(200);
    }

    @Test
    void usuarioDeEmpresaNaoLeUsuariosDeNinguem() {
        comToken(tokenA).get("/api/Usuario").then().statusCode(403);
        comToken(tokenA).get("/api/Usuario/count").then().statusCode(403);
        comToken(tokenPlat).get("/api/Usuario").then().statusCode(200);
    }

    @Test
    void tokenValidoSemEmpresaENaoAdminFalhaFechado() {
        // JWT assinado pelo servidor, mas sem empresa_id e sem plataforma_admin: o escopo deve negar (403), nunca liberar tudo
        var u = dados.usuario(true, false);
        String jwt = QuarkusTransaction.requiringNew().call(() -> emissor.emitir(
                em.find(br.com.emitesimples.usuarios.entity.UsuarioEntity.class, u.id()), null, "operador", false));
        Response r = comToken(jwt).get("/api/Pessoa");
        assertEquals(403, r.statusCode(), r.asString());
        assertEquals(403, comToken(jwt).get("/api/Pessoa/id/" + pesA).statusCode());
        assertEquals(403, comToken(jwt).get("/api/Nota").statusCode());
    }

    private String versaoDaEmpresa(String id) {
        return QuarkusTransaction.requiringNew().call(() -> java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(em.find(EmpresaEntity.class, id).getVersion().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)));
    }

    private Map<String, Object> corpoPessoa(String nome, String empresaId) {
        return Map.of("tipoPessoa", "fisica", "nome", nome,
                "empresa", Map.of("id", empresaId, "version", versaoDaEmpresa(empresaId)));
    }

    @Test
    void postNaPropriaEmpresaFuncionaParaOperador() {
        // o POST gerado exige a empresa aninhada COM version (a referência só com id falha: ver o relatório)
        Response r = chamar("POST", "/api/Pessoa", tokenA, corpoPessoa("Criada pelo operador", empA));
        assertEquals(200, r.statusCode(), r.asString());
        assertEquals(empA, r.jsonPath().getString("empresa.id"));
    }

    @Test
    void postComEmpresaSoComIdFuncionaComOGeradorCorrigido() {
        Response r = chamar("POST", "/api/Pessoa", tokenA,
                Map.of("tipoPessoa", "fisica", "nome", "x", "empresa", Map.of("id", empA)));
        assertEquals(200, r.statusCode(), r.asString());
        assertEquals(empA, r.jsonPath().getString("empresa.id"));
    }

    @Test
    void postGeradoComEmpresaDeOutroTenantEBarradoPelaRls() {
        // O código GERADO ainda não impõe o tenant no persist (achado da auditoria): o repositório grava a empresa que
        // vier no body. A RLS do banco (V3) é a segunda camada e barra: nada é criado na B. Antes da RLS este teste
        // afirmava o contrário (depois - antes == 1); a expectativa foi invertida de propósito. Prova completa: RlsTest.
        long antes = contar(empB);
        Response r = chamar("POST", "/api/Pessoa", tokenA, corpoPessoa("Injetada pela A", empB));
        long depois = contar(empB);
        System.out.println("[RLS] POST /api/Pessoa de usuário da A com empresa=B: status=" + r.statusCode()
                + ", pessoas da B antes=" + antes + ", depois=" + depois);
        assertEquals(0, depois - antes, "a RLS tem de impedir a criação na empresa B. Resposta: " + r.asString());
        assertEquals(403, r.statusCode(), r.asString());
    }

    @Test
    void putNaoConseguePassarRegistroParaOutraEmpresa() {
        String p = dados.pessoa(empA, "Vai mudar de empresa");
        Response r = chamar("PUT", "/api/Pessoa/id/" + p, tokenA, Map.of("tipoPessoa", "fisica", "nome", "Movida",
                "empresa", Map.of("id", empB, "version", versaoDaEmpresa(empB))));
        String dona = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select p.empresa.id from PessoaEntity p where p.id = :id", String.class).setParameter("id", p)
                .getSingleResult());
        System.out.println("[ACHADO] PUT movendo Pessoa da A para a B: status=" + r.statusCode() + ", empresa dona agora="
                + (dona.equals(empB) ? "B" : dona.equals(empA) ? "A" : "?"));
        assertEquals(empA, dona, "o registro não pode mudar de empresa por PUT");
        assertTrue(r.statusCode() >= 400, "o PUT que troca a empresa tem de falhar; veio " + r.statusCode());
    }

    @Test
    void postGeradoDeServicoComEmpresaDeOutroTenantEBarradoPelaRls() {
        // mesma barreira do POST de Pessoa (ver acima), agora em Servico
        long antes = contarDe("Servico", empB);
        Response r = chamar("POST", "/api/Servico", tokenA, Map.of("nome", "Servico injetado pela A", "ativo", true,
                "issRetido", false, "tipoOperacaoIss", 1,
                "empresa", Map.of("id", empB, "version", versaoDaEmpresa(empB))));
        long depois = contarDe("Servico", empB);
        System.out.println("[RLS] POST /api/Servico de usuário da A com empresa=B: status=" + r.statusCode()
                + ", servicos da B antes=" + antes + ", depois=" + depois);
        assertEquals(0, depois - antes, "a RLS tem de impedir a criação na empresa B. Resposta: " + r.asString());
        assertEquals(403, r.statusCode(), r.asString());
    }

    private long contar(String empresa) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(p) from PessoaEntity p where p.empresa.id = :e", Long.class).setParameter("e", empresa)
                .getSingleResult());
    }

    private long contarDe(String entidade, String empresa) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(x) from " + entidade + "Entity x where x.empresa.id = :e", Long.class)
                .setParameter("e", empresa).getSingleResult());
    }
}
