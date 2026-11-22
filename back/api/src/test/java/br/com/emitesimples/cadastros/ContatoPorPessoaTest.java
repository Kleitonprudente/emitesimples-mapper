package br.com.emitesimples.cadastros;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * CRUD GERADO de Contato: so LEITURA e permitida (a escrita e feita por PUT /api/Pessoa/{id}/contatos, ver
 * ContatosDaPessoaTest). Aqui se prova que a escrita direta esta fechada para todos (fecha as lacunas do gerador:
 * PUT que sobrescrevia a Pessoa, sem validacao) e como a leitura direta se comporta, inclusive o filtro pessoa.id ignorado.
 */
@QuarkusTest
class ContatoPorPessoaTest extends BaseApiTest {

    @Test
    void escritaDiretaEhNegadaParaTodosOsPapeisEAnonimo() {
        String emp = dados.empresaAtiva();
        String p = dados.pessoa(emp, "Dona");
        String c = dados.contato(emp, p, "Existente");
        Map<String, Object> corpo = Map.of("tipoContato", "socio", "nome", "X", "pessoa", Map.of("id", p));
        for (String papel : List.of("administrador", "operador", "consulta")) {
            String tk = token(dados.usuarioComPapel(emp, papel).email());
            assertEquals(403, status("POST", "/api/Contato", tk, corpo), papel);
            assertEquals(403, status("PUT", "/api/Contato/id/" + c, tk, corpo), papel);
            assertEquals(403, status("PATCH", "/api/Contato/id/" + c, tk, corpo), papel);
            assertEquals(403, status("DELETE", "/api/Contato/id/" + c, tk, null), papel);
        }
        String plat = token(dados.usuario(true, true).email());
        assertEquals(403, status("POST", "/api/Contato", plat, corpo));
        assertEquals(403, status("DELETE", "/api/Contato/id/" + c, plat, null));
        assertEquals(401, status("POST", "/api/Contato", null, corpo));
        assertEquals(401, status("DELETE", "/api/Contato/id/" + c, null, null));
        String tk = token(dados.usuarioComPapel(emp, "administrador").email());
        comToken(tk).get("/api/Contato/id/" + c).then().statusCode(200).body("nome", equalTo("Existente"));
    }

    @Test
    void leituraDiretaOrdenadaPorNomeESoDaEmpresaDaSessao() {
        String empresa = dados.empresaAtiva();
        String outra = dados.empresaAtiva();
        String p = dados.pessoa(empresa, "Ordem");
        String po = dados.pessoa(outra, "Outra");
        String tk = token(dados.usuarioComPapel(empresa, "consulta").email());
        for (String n : List.of("Carla", "Ana", "Bruno")) {
            dados.contato(empresa, p, n);
        }
        String cb = dados.contato(outra, po, "Zeca da outra");
        assertEquals(List.of("Ana", "Bruno", "Carla"),
                comToken(tk).get("/api/Contato/0/99?sortBy=nome").then().statusCode(200).extract().jsonPath().getList("nome"));
        assertEquals(List.of("Carla", "Bruno", "Ana"),
                comToken(tk).get("/api/Contato/0/99?sortBy=nome&sortDirection=desc").then().extract().jsonPath().getList("nome"));
        comToken(tk).get("/api/Contato/count").then().statusCode(200).body("count", equalTo(3));
        comToken(tk).get("/api/Contato/0/99").then().body("empresa.id", everyItem(equalTo(empresa)));
        comToken(tk).get("/api/Contato/0/99?pessoa.id=" + po).then().statusCode(200).body("id", not(hasItem(cb)));
        comToken(tk).get("/api/Contato/0/99?empresa.id=" + outra).then().statusCode(200).body("$", empty());
        comToken(tk).get("/api/Contato/id/" + cb).then().statusCode(404).body("codigo", equalTo("nao_encontrado"));
    }

    @Test
    void gapFiltroPessoaIdEhIgnoradoNoGetDireto_ofrontNaoOUsa() {
        // GAP mantido: o repositorio gerado nao aplica pessoa.id (lista e count trazem todas as pessoas da empresa).
        // O front usa GET /api/Pessoa/{id}/contatos. Inverter este teste quando o gerador aplicar o filtro.
        String empresa = dados.empresaAtiva();
        String p1 = dados.pessoa(empresa, "P1");
        String p2 = dados.pessoa(empresa, "P2");
        String tk = token(dados.usuarioComPapel(empresa, "administrador").email());
        String c1 = dados.contato(empresa, p1, "Do P1");
        String c2 = dados.contato(empresa, p2, "Do P2");
        comToken(tk).get("/api/Contato/0/99?pessoa.id=" + p1).then().statusCode(200)
                .body("id", hasItem(c1)).body("id", hasItem(c2));
        comToken(tk).get("/api/Contato/count?pessoa.id=" + p1).then().statusCode(200).body("count", equalTo(2));
    }
}
