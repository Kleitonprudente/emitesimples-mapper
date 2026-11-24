package br.com.emitesimples.cadastros;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.test.junit.QuarkusTest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/** Toda Pessoa tem ao menos um papel (cliente, fornecedor ou ambos): CHECK ck_pessoa_papel (V7) com resposta 400 legivel. */
@QuarkusTest
class PessoaPapelTest extends BaseApiTest {

    private static Map<String, Object> corpo(String doc, boolean cliente, boolean fornecedor) {
        Map<String, Object> m = new HashMap<>();
        m.put("tipoPessoa", "FISICA");
        m.put("documento", doc);
        m.put("nome", "Pessoa " + doc);
        m.put("consumidorFinal", true);
        m.put("cliente", cliente);
        m.put("fornecedor", fornecedor);
        return m;
    }

    @Test
    void pessoaSemPapelEhRecusadaComMensagemLegivel() {
        String emp = dados.empresaAtiva();
        String tk = token(dados.usuarioComPapel(emp, "OPERADOR").email());
        comToken(tk).body(corpo("39053344705", false, false)).post("/api/Pessoa").then().statusCode(400)
                .body("codigo", equalTo("pessoa_sem_papel"))
                .body("mensagem", equalTo("A pessoa precisa ser cliente, fornecedor ou as duas coisas."));
    }

    @Test
    void umPapelOuAmbosSaoAceitos() throws SQLException {
        String emp = dados.empresaAtiva();
        String tk = token(dados.usuarioComPapel(emp, "OPERADOR").email());
        for (Object[] c : new Object[][] {{"52998224725", true, false}, {"11144477735", false, true}, {"45317828791", true, true}}) {
            var r = chamar("POST", "/api/Pessoa", tk, corpo((String) c[0], (boolean) c[1], (boolean) c[2]));
            assertEquals(200, r.statusCode(), (String) c[0]);
        }
        // a API nega DELETE de Pessoa por desenho; limpa como dono para nao deixar linhas no banco compartilhado entre
        // os testes (outros leem as primeiras 100 pessoas)
        try (Connection c = DriverManager.getConnection(ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class),
                "dono_teste", "dono-teste-efemero");
                PreparedStatement ps = c.prepareStatement("delete from pessoa where empresa_id = ?")) {
            ps.setString(1, emp);
            assertEquals(3, ps.executeUpdate());
        }
    }
}
