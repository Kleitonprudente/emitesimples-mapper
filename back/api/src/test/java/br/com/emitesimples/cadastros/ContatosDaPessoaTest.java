package br.com.emitesimples.cadastros;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GET e PUT /api/Pessoa/{id}/contatos (a unica forma de o front gerenciar contatos). */
@QuarkusTest
class ContatosDaPessoaTest extends BaseApiTest {

    static String empA, empB, pesA, pesA2, pesB;
    static String tokenOp, tokenAdm, tokenConsulta, tokenB, tokenPlat;

    @BeforeEach
    void preparar() {
        if (empA != null) {
            return;
        }
        empA = dados.empresaAtiva();
        empB = dados.empresaAtiva();
        tokenOp = token(dados.usuarioComPapel(empA, "OPERADOR").email());
        tokenAdm = token(dados.usuarioComPapel(empA, "ADMINISTRADOR").email());
        tokenConsulta = token(dados.usuarioComPapel(empA, "CONSULTA").email());
        tokenB = token(dados.usuarioComPapel(empB, "ADMINISTRADOR").email());
        tokenPlat = token(dados.usuario(true, true).email());
    }

    private static Map<String, Object> item(String id, String tipo, String nome, String whatsapp, String email) {
        Map<String, Object> m = new HashMap<>();
        if (id != null) {
            m.put("id", id);
        }
        m.put("tipoContato", tipo);
        m.put("nome", nome);
        m.put("whatsapp", whatsapp);
        m.put("email", email);
        return m;
    }

    private static Response put(String tk, String pessoaId, Object corpo) {
        return chamar("PUT", "/api/Pessoa/" + pessoaId + "/contatos", tk, corpo);
    }

    private static Response get(String tk, String pessoaId) {
        return chamar("GET", "/api/Pessoa/" + pessoaId + "/contatos", tk, null);
    }

    private String novaPessoa() {
        return dados.pessoa(empA, "Pessoa " + System.nanoTime());
    }

    @Test
    void getListaSoDaPessoaESoDaEmpresa_ordenadoPorNome_qualquerPapel() {
        String p1 = novaPessoa();
        String p2 = novaPessoa();
        dados.contato(empA, p1, "Carlos");
        dados.contato(empA, p1, "ana");
        dados.contato(empA, p2, "De outra pessoa");
        for (String tk : List.of(tokenOp, tokenAdm, tokenConsulta, tokenPlat)) {
            get(tk, p1).then().statusCode(200).body("nome", equalTo(List.of("ana", "Carlos")))
                    .body("[0].id", notNullValue()).body("[0].tipoContato", notNullValue())
                    .body("[0].whatsapp", nullValue()).body("[0].empresa", nullValue());
        }
        get(tokenOp, pesVazia()).then().statusCode(200).body("$", hasSize(0));
    }

    private String pesVazia() {
        return novaPessoa();
    }

    @Test
    void putCriaAtualizaEApagaNumaChamada() {
        String p = novaPessoa();
        Response r1 = put(tokenOp, p, List.of(
                item(null, "SOCIO", "  Zeca  ", "(11) 99999-8888", "zeca@teste.com"),
                item(null, "FISCAL", "Ana", null, null),
                item(null, "OUTRO", "Bia", "", "")));
        r1.then().statusCode(200).body("$", hasSize(3)).body("nome", equalTo(List.of("Ana", "Bia", "Zeca")))
                .body("find { it.nome == 'Zeca' }.whatsapp", equalTo("11999998888"))
                .body("find { it.nome == 'Zeca' }.email", equalTo("zeca@teste.com"))
                .body("find { it.nome == 'Bia' }.whatsapp", nullValue())
                .body("find { it.nome == 'Bia' }.email", nullValue());
        String idAna = r1.jsonPath().getString("find { it.nome == 'Ana' }.id");
        String idZeca = r1.jsonPath().getString("find { it.nome == 'Zeca' }.id");
        String idBia = r1.jsonPath().getString("find { it.nome == 'Bia' }.id");

        // altera Ana, mantem Zeca igual, apaga Bia, cria Davi
        Response r2 = put(tokenAdm, p, List.of(
                item(idAna, "FINANCEIRO", "Ana Maria", "11 4000-1234", "ana@teste.com"),
                item(idZeca, "SOCIO", "Zeca", "11999998888", "zeca@teste.com"),
                item(null, "COMERCIAL", "Davi", null, null)));
        r2.then().statusCode(200).body("nome", equalTo(List.of("Ana Maria", "Davi", "Zeca")))
                .body("find { it.nome == 'Ana Maria' }.id", equalTo(idAna))
                .body("find { it.nome == 'Ana Maria' }.tipoContato", equalTo("FINANCEIRO"))
                .body("find { it.nome == 'Ana Maria' }.whatsapp", equalTo("1140001234"))
                .body("find { it.nome == 'Zeca' }.id", equalTo(idZeca))
                .body("id", org.hamcrest.Matchers.not(hasItem(idBia)));
        get(tokenConsulta, p).then().statusCode(200).body("nome", equalTo(List.of("Ana Maria", "Davi", "Zeca")));
        // a empresa gravada e a da sessao
        chamar("GET", "/api/Contato/id/" + idAna, tokenOp, null).then().statusCode(200)
                .body("empresa.id", equalTo(empA)).body("pessoa.id", equalTo(p));
        chamar("GET", "/api/Contato/id/" + idBia, tokenOp, null).then().statusCode(404);
    }

    @Test
    void idempotente_mesmoPutDuasVezesNaoDuplica() {
        String p = novaPessoa();
        Response r1 = put(tokenOp, p, List.of(item(null, "SOCIO", "Unico", null, null), item(null, "FISCAL", "Outro", null, null)));
        r1.then().statusCode(200);
        // reenviar o MESMO corpo ainda sem ids: o PUT e "lista desejada": recria (apaga os antigos e insere), sem duplicar
        Response r2 = put(tokenOp, p, List.of(item(null, "SOCIO", "Unico", null, null), item(null, "FISCAL", "Outro", null, null)));
        r2.then().statusCode(200).body("$", hasSize(2));
        // reenviar a resposta (com ids): nada muda, mesmos ids
        List<Map<String, Object>> salvo = r2.jsonPath().getList("$");
        Response r3 = put(tokenOp, p, salvo);
        r3.then().statusCode(200).body("$", hasSize(2));
        assertEquals(r2.jsonPath().getList("id"), r3.jsonPath().getList("id"));
        get(tokenOp, p).then().body("$", hasSize(2));
    }

    @Test
    void listaVaziaApagaTodos() {
        String p = novaPessoa();
        put(tokenOp, p, List.of(item(null, "SOCIO", "A", null, null), item(null, "SOCIO", "B", null, null))).then().statusCode(200);
        put(tokenOp, p, List.of()).then().statusCode(200).body("$", hasSize(0));
        get(tokenOp, p).then().body("$", hasSize(0));
    }

    @Test
    void atomicidade_umItemInvalidoNaoMudaNada() {
        String p = novaPessoa();
        Response ok = put(tokenOp, p, List.of(item(null, "SOCIO", "Antigo 1", null, null), item(null, "FISCAL", "Antigo 2", null, null)));
        List<String> ids = ok.jsonPath().getList("id");
        // apagaria o Antigo 2, alteraria o Antigo 1 e criaria Novo, mas o ultimo item e invalido
        Response r = put(tokenOp, p, List.of(
                item(ids.get(0), "COMERCIAL", "Mudado", null, null),
                item(null, "SOCIO", "Novo", null, null),
                item(null, "INVALIDO", "Ruim", null, null)));
        r.then().statusCode(400).body("codigo", equalTo("requisicao_invalida"));
        get(tokenOp, p).then().body("nome", equalTo(List.of("Antigo 1", "Antigo 2"))).body("tipoContato", equalTo(List.of("SOCIO", "FISCAL")));
    }

    @Test
    void idDeOutraPessoaOuInexistenteOuDeOutraEmpresaDa400SemEfeito() {
        String p1 = novaPessoa();
        String p2 = novaPessoa();
        String doP2 = put(tokenOp, p2, List.of(item(null, "SOCIO", "Do P2", null, null))).jsonPath().getString("[0].id");
        String cB = dados.contato(empB, dados.pessoa(empB, "PB"), "Da B");
        put(tokenOp, p1, List.of(item(null, "SOCIO", "Meu", null, null))).then().statusCode(200);
        for (String idAlheio : List.of(doP2, "nao-existe", cB)) {
            put(tokenOp, p1, List.of(item(null, "SOCIO", "Novo", null, null), item(idAlheio, "FISCAL", "Sequestro", null, null)))
                    .then().statusCode(400).body("codigo", equalTo("requisicao_invalida"))
                    .body("detalhes.campos[0].campo", equalTo("contatos[1].id"));
        }
        get(tokenOp, p1).then().body("nome", equalTo(List.of("Meu")));
        get(tokenOp, p2).then().body("nome", equalTo(List.of("Do P2"))).body("[0].tipoContato", equalTo("SOCIO"));
        get(tokenB, pesBDe(cB)).then().statusCode(200);
    }

    private String pesBDe(String contatoB) {
        return chamar("GET", "/api/Contato/id/" + contatoB, tokenB, null).jsonPath().getString("pessoa.id");
    }

    @Test
    void validacoesComDetalhesCamposApontandoOItem() {
        String p = novaPessoa();
        List<Object> corpo = new ArrayList<>();
        corpo.add(item(null, "SOCIO", "Valido", null, null));
        corpo.add(item(null, "xxx", "Tipo ruim", null, null));
        corpo.add(item(null, "SOCIO", "  ", null, null));
        corpo.add(item(null, "FISCAL", "Nome longo " + "x".repeat(300), null, null));
        corpo.add(item(null, "FISCAL", "Fone", "abc123", null));
        corpo.add(item(null, "FISCAL", "Fone longo", "1".repeat(21), null));
        corpo.add(item(null, "FISCAL", "Email", null, "isto-nao-e-email"));
        corpo.add(item(null, "FISCAL", "Email longo", null, "a".repeat(60) + "@x.com"));
        corpo.add(item(null, null, "Sem tipo", null, null));
        Response r = put(tokenOp, p, corpo);
        r.then().statusCode(400).body("codigo", equalTo("requisicao_invalida"));
        List<String> campos = r.jsonPath().getList("detalhes.campos.campo");
        for (String esperado : List.of("contatos[1].tipoContato", "contatos[2].nome", "contatos[3].nome",
                "contatos[4].whatsapp", "contatos[5].whatsapp", "contatos[6].email", "contatos[7].email",
                "contatos[8].tipoContato")) {
            assertTrue(campos.contains(esperado), esperado + " em " + campos);
        }
        assertTrue(!campos.contains("contatos[0].nome"), campos.toString());
        r.then().body("detalhes.campos[0].mensagem", notNullValue());
        get(tokenOp, p).then().body("$", hasSize(0));
    }

    @Test
    void limiteDe50ItensEIdRepetidoECorpoAusente() {
        String p = novaPessoa();
        List<Object> cinquenta = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            cinquenta.add(item(null, "OUTRO", "C" + String.format("%02d", i), null, null));
        }
        put(tokenOp, p, cinquenta).then().statusCode(200).body("$", hasSize(50));
        cinquenta.add(item(null, "OUTRO", "C50", null, null));
        put(tokenOp, p, cinquenta).then().statusCode(400).body("detalhes.campos[0].campo", equalTo("contatos"));
        get(tokenOp, p).then().body("$", hasSize(50));

        String id = get(tokenOp, p).jsonPath().getString("[0].id");
        put(tokenOp, p, List.of(item(id, "OUTRO", "A", null, null), item(id, "OUTRO", "B", null, null))).then().statusCode(400)
                .body("detalhes.campos[0].campo", equalTo("contatos[1].id"));
        put(tokenOp, p, null).then().statusCode(400);
    }

    @Test
    void consultaLeMasNaoEscreve_anonimo401() {
        String p = novaPessoa();
        put(tokenConsulta, p, List.of(item(null, "SOCIO", "X", null, null))).then().statusCode(403).body("codigo", equalTo("acesso_negado"));
        get(tokenConsulta, p).then().statusCode(200);
        put(null, p, List.of()).then().statusCode(401);
        get(null, p).then().statusCode(401);
        get(tokenOp, p).then().body("$", hasSize(0));
    }

    @Test
    void empresaDoCorpoEhIgnorada_vemDaSessao() {
        String p = novaPessoa();
        Map<String, Object> i = item(null, "SOCIO", "Com empresa", null, null);
        i.put("empresa", Map.of("id", empB));
        i.put("pessoa", Map.of("id", pesBQualquer()));
        Response r = put(tokenOp, p, List.of(i));
        r.then().statusCode(200).body("$", hasSize(1));
        chamar("GET", "/api/Contato/id/" + r.jsonPath().getString("[0].id"), tokenOp, null).then().statusCode(200)
                .body("empresa.id", equalTo(empA)).body("pessoa.id", equalTo(p));
    }

    private String pesBQualquer() {
        return dados.pessoa(empB, "PB");
    }

    @Test
    void isolamentoEntreEmpresas_pessoaDeOutraEmpresaOuInexistenteEh404() {
        String pB = dados.pessoa(empB, "Pessoa da B");
        dados.contato(empB, pB, "Contato B");
        for (String id : List.of(pB, "nao-existe")) {
            get(tokenOp, id).then().statusCode(404).body("codigo", equalTo("nao_encontrado"));
            put(tokenOp, id, List.of(item(null, "SOCIO", "Invasao", null, null))).then().statusCode(404)
                    .body("codigo", equalTo("nao_encontrado"));
        }
        get(tokenB, pB).then().statusCode(200).body("nome", equalTo(List.of("Contato B")));
        put(tokenB, novaPessoa(), List.of()).then().statusCode(404); // e o inverso: B nao mexe em pessoa da A
    }

    @Test
    void administradorDaPlataformaSemEmpresaNaSessao_enxergaQualquerPessoaEOContatoHerdaAEmpresaDela() {
        String p = novaPessoa();
        Response r = put(tokenPlat, p, List.of(item(null, "SOCIO", "Pela plataforma", null, null)));
        r.then().statusCode(200).body("$", hasSize(1));
        chamar("GET", "/api/Contato/id/" + r.jsonPath().getString("[0].id"), tokenPlat, null).then().statusCode(200)
                .body("empresa.id", equalTo(empA));
        get(tokenPlat, p).then().statusCode(200).body("nome", equalTo(List.of("Pela plataforma")));
        get(tokenOp, p).then().body("nome", equalTo(List.of("Pela plataforma")));
        put(tokenPlat, "nao-existe", List.of()).then().statusCode(404);
    }
}
