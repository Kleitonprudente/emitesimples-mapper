package br.com.emitesimples.plataforma;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.DadosDeTeste.Usr;
import br.com.emitesimples.cadastros.entity.ImportacaoMunicipioEntity;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

/** POST /api/admin/municipios/importacao e GET /api/admin/municipios/importacoes (docs/carga-municipios.md). */
@QuarkusTest
class ImportacaoMunicipioTest extends BaseApiTest {

    private static final String CAB_IBGE = "CÓDIGO DO MUNICÍPIO - TOM;CÓDIGO DO MUNICÍPIO - IBGE;MUNICÍPIO - TOM;MUNICÍPIO - IBGE;UF";
    private static final Charset WIN1252 = Charset.forName("windows-1252");
    private static final String URL = "/api/admin/municipios/importacao";

    @Inject
    EntityManager em;

    private String admin() {
        return token(dados.usuario(true, true).email());
    }

    private static String codigo() {
        return "9" + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private static String nome(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private static Response enviar(String token, String nomeArquivo, byte[] bytes) {
        var s = given().accept("application/json");
        if (token != null) {
            s = s.header("Authorization", "Bearer " + token);
        }
        return s.multiPart("arquivo", nomeArquivo, bytes, "text/csv").post(URL);
    }

    private static byte[] win(String... linhas) {
        return (String.join("\r\n", linhas) + "\r\n").getBytes(WIN1252);
    }

    private MunicipioEntity municipio(String codigoIbge) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select m from MunicipioEntity m where m.codigoIbge = :c", MunicipioEntity.class)
                .setParameter("c", codigoIbge).getResultStream().findFirst().orElse(null));
    }

    private long totalMunicipios() {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery("select count(m) from MunicipioEntity m", Long.class).getSingleResult());
    }

    private ImportacaoMunicipioEntity historico(String id) {
        return QuarkusTransaction.requiringNew().call(() -> {
            ImportacaoMunicipioEntity h = em.find(ImportacaoMunicipioEntity.class, id);
            h.getUsuario().getId(); // carrega
            return h;
        });
    }

    // ---------------------------------------------------------------------------------------------- acesso

    @Test
    void soAdministradorDaPlataforma() {
        byte[] arq = win(CAB_IBGE, "1;" + codigo() + ";X;" + nome("Cidade") + ";SP");
        assertEquals(401, enviar(null, "m.csv", arq).statusCode());
        String empresa = dados.empresaAtiva();
        for (String papel : new String[] {"consulta", "operador", "administrador"}) {
            String t = token(dados.usuarioComPapel(empresa, papel).email());
            enviar(t, "m.csv", arq).then().statusCode(403);
            assertEquals(403, status("GET", "/api/admin/municipios/importacoes", t, null));
        }
        assertEquals(401, status("GET", "/api/admin/municipios/importacoes", null, null));
        // DELETE e outros metodos nunca
        assertEquals(403, status("DELETE", URL, admin(), null));
    }

    // ------------------------------------------------------------------------------------------ caminho feliz

    @Test
    void windows1252PontoEVirgulaComTomIgnoradaEExterior() {
        String c1 = codigo();
        String c2 = codigo();
        String n1 = nome("Brasília");
        String n2 = nome("São João do Piauí");
        byte[] arq = win(CAB_IBGE,
                "1;" + c1 + ";BRASILIA;" + n1 + ";DF",
                "2;" + c2 + ";SAO JOAO;" + n2 + ";PI",
                "9707;0;EXTERIOR;;EX");
        Response r = enviar(admin(), "municipios.csv", arq);
        r.then().statusCode(200)
                .body("estado", equalTo("concluida"))
                .body("codificacao", equalTo("Windows-1252"))
                .body("separador", equalTo(";"))
                .body("nomeArquivo", equalTo("municipios.csv"))
                .body("linhasLidas", equalTo(3))
                .body("totalRejeitados", equalTo(0))
                .body("rejeitados", hasSize(0));
        int criados = r.jsonPath().getInt("criados");
        int iguais = r.jsonPath().getInt("inalterados");
        assertEquals(3, criados + iguais + r.jsonPath().getInt("atualizados"));
        assertTrue(criados >= 2);

        assertEquals(n1, municipio(c1).getNome()); // acento correto: veio de Windows-1252
        assertEquals("DF", municipio(c1).getUf());
        assertEquals(n2, municipio(c2).getNome());
        MunicipioEntity ex = municipio("0");
        assertNotNull(ex);
        assertEquals("Exterior", ex.getNome());
        assertEquals("EX", ex.getUf());

        // historico gravado
        ImportacaoMunicipioEntity h = historico(r.jsonPath().getString("id"));
        assertEquals("concluida", h.getEstado());
        assertEquals("Windows-1252", h.getCodificacaoDetectada());
        assertEquals(3, h.getLinhasLidas());
        assertEquals(criados, h.getLinhasInseridas());
        assertNull(h.getResumoRejeicoes());
        assertNotNull(h.getDataHoraFim());

        // segundo envio do mesmo arquivo: nada muda
        Response r2 = enviar(admin(), "municipios.csv", arq);
        r2.then().statusCode(200).body("criados", equalTo(0)).body("atualizados", equalTo(0))
                .body("inalterados", equalTo(3)).body("linhasLidas", equalTo(3));
    }

    @Test
    void utf8ComBomEVirgulaETabulacao() {
        String c1 = codigo();
        String n1 = nome("Maceió");
        String csv = "\uFEFF\"CÓDIGO DO MUNICÍPIO - TOM\",\"CÓDIGO DO MUNICÍPIO - IBGE\",\"MUNICÍPIO - TOM\",\"MUNICÍPIO - IBGE\",\"UF\"\n"
                + "1,\"" + c1 + "\",X,\"" + n1 + "\",al\n";
        enviar(admin(), "u.csv", csv.getBytes(StandardCharsets.UTF_8)).then().statusCode(200)
                .body("codificacao", equalTo("UTF-8")).body("separador", equalTo(","))
                .body("criados", equalTo(1));
        assertEquals(n1, municipio(c1).getNome());
        assertEquals("AL", municipio(c1).getUf()); // UF em caixa baixa e normalizada

        String c2 = codigo();
        String n2 = nome("Ouro Preto");
        String tab = "CÓDIGO DO MUNICÍPIO - TOM\tCÓDIGO DO MUNICÍPIO - IBGE\tMUNICÍPIO - TOM\tMUNICÍPIO - IBGE\tUF\n"
                + "1\t" + c2 + "\tX\t" + n2 + "\tMG\n";
        enviar(admin(), "t.tsv", tab.getBytes(StandardCharsets.UTF_8)).then().statusCode(200)
                .body("separador", equalTo("tab")).body("criados", equalTo(1));
    }

    @Test
    void colunasPeloNomeEmQualquerOrdemEIgnoraColunasExtras() {
        String c = codigo();
        String n = nome("Niterói");
        byte[] arq = win("uf;extra;municipio - ibge;codigo do municipio - ibge",
                "RJ;lixo;" + n + ";" + c);
        enviar(admin(), "o.csv", arq).then().statusCode(200).body("criados", equalTo(1));
        assertEquals(n, municipio(c).getNome());
    }

    // ------------------------------------------------------------------------------------------- rejeicoes

    @Test
    void rejeitaSemAdivinharEAplicaAsValidas() {
        String ok = codigo();
        String nOk = nome("Valida");
        String dup = codigo();
        String nConf = nome("Conflito");
        String c1 = codigo();
        byte[] arq = win(CAB_IBGE,
                "1;" + ok + ";X;" + nOk + ";SP",                       // linha 2: ok
                "1;123456;X;" + nome("Curto") + ";SP",                // linha 3: codigo com 6 digitos
                "1;" + codigo() + ";X;" + nome("UfRuim") + ";XX",     // linha 4: UF invalida
                "1;" + codigo() + ";X;;SP",                           // linha 5: nome vazio
                "",                                                    // linha 6: em branco, ignorada
                "1;" + dup + ";X;" + nome("Primeira") + ";RJ",        // linha 7: ok
                "1;" + dup + ";X;" + nome("Segunda") + ";RJ",         // linha 8: codigo repetido
                "9707;5;EXTERIOR;;EX",                                 // linha 9: EX com codigo 5
                "1;" + c1 + ";X;" + nConf + ";SP",                    // linha 10: ok
                "1;" + codigo() + ";X;" + nConf + ";SP",              // linha 11: mesmo nome+UF de outro codigo
                "1;ABCDEFG;X;" + nome("Letras") + ";SP",              // linha 12: codigo nao numerico
                "1;" + codigo() + ";X;" + "A".repeat(101) + ";SP");   // linha 13: nome longo demais
        Response r = enviar(admin(), "r.csv", arq);
        r.then().statusCode(200)
                .body("estado", equalTo("concluida_com_rejeicoes"))
                .body("linhasLidas", equalTo(11))
                .body("criados", equalTo(3))
                .body("totalRejeitados", equalTo(8));
        List<Map<String, Object>> rej = r.jsonPath().getList("rejeitados");
        assertEquals(List.of(3, 4, 5, 8, 9, 11, 12, 13), rej.stream().map(m -> (Integer) m.get("linha")).toList());
        assertTrue(((String) rej.get(0).get("motivo")).contains("7 dígitos"));
        assertTrue(((String) rej.get(1).get("motivo")).contains("UF inválida"));
        assertTrue(((String) rej.get(2).get("motivo")).contains("vazio"));
        assertTrue(((String) rej.get(3).get("motivo")).contains("repetido") && ((String) rej.get(3).get("motivo")).contains("linha 7"));
        assertTrue(((String) rej.get(4).get("motivo")).contains("EX"));
        assertTrue(((String) rej.get(5).get("motivo")).contains("mesmo nome e UF"));

        assertNotNull(municipio(ok));
        assertNotNull(municipio(dup));
        assertEquals(0, QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(m) from MunicipioEntity m where m.nome like 'Segunda %'", Long.class).getSingleResult()), "linha repetida nao entra");
        // o relatorio fica no historico
        ImportacaoMunicipioEntity h = historico(r.jsonPath().getString("id"));
        assertEquals("concluida_com_rejeicoes", h.getEstado());
        assertEquals(8, h.getLinhasRejeitadas());
        assertTrue(h.getResumoRejeicoes().contains("linha 3:") && h.getResumoRejeicoes().contains("linha 13:"));
    }

    @Test
    void atualizaPorCodigoSemApagarNemQuebrarReferencias() {
        String c = codigo();
        String nAntigo = nome("Nome Antigo");
        String nNovo = nome("Nome Novo");
        enviar(admin(), "a.csv", win(CAB_IBGE, "1;" + c + ";X;" + nAntigo + ";GO")).then().statusCode(200);
        String idAntes = municipio(c).getId();
        // uma empresa passa a referenciar o municipio
        String empresa = dados.empresaAtiva();
        QuarkusTransaction.requiringNew().run(() -> em.createQuery(
                "update EmpresaEntity e set e.municipio.id = :m where e.id = :e").setParameter("m", idAntes)
                .setParameter("e", empresa).executeUpdate());
        long antes = totalMunicipios();

        // arquivo novo NAO traz o municipio da empresa de teste anterior nem o de DadosDeTeste; o nome do codigo c muda
        Response r = enviar(admin(), "b.csv", win(CAB_IBGE, "1;" + c + ";X;" + nNovo + ";GO"));
        r.then().statusCode(200).body("atualizados", equalTo(1)).body("criados", equalTo(0)).body("inalterados", equalTo(0));
        MunicipioEntity depois = municipio(c);
        assertEquals(idAntes, depois.getId(), "mesmo registro (id estavel): atualiza, nao recria");
        assertEquals(nNovo, depois.getNome());
        assertEquals(antes, totalMunicipios(), "nunca apaga municipio fora do arquivo");
        String refId = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select e.municipio.id from EmpresaEntity e where e.id = :e", String.class).setParameter("e", empresa).getSingleResult());
        assertEquals(idAntes, refId);
    }

    @Test
    void trocaDeNomesEntreCodigosNaoEstouraOIndiceUnico() {
        // A passa a ter o nome que era de B e B ganha outro: a ordem do arquivo importa e nada pode virar 500
        String a = codigo();
        String b = codigo();
        String nA = nome("Alfa");
        String nB = nome("Beta");
        String nC = nome("Gama");
        enviar(admin(), "x.csv", win(CAB_IBGE, "1;" + a + ";X;" + nA + ";SP", "1;" + b + ";X;" + nB + ";SP")).then().statusCode(200);
        // B libera nB (vira nC) e depois A toma nB
        enviar(admin(), "y.csv", win(CAB_IBGE, "1;" + b + ";X;" + nC + ";SP", "1;" + a + ";X;" + nB + ";SP")).then().statusCode(200)
                .body("atualizados", equalTo(2)).body("totalRejeitados", equalTo(0));
        assertEquals(nB, municipio(a).getNome());
        assertEquals(nC, municipio(b).getNome());
    }

    // ------------------------------------------------------------------------------------- erros do arquivo

    @Test
    void cabecalhoSemAsColunasObrigatoriasDa400RegistraFalhaENaoGravaNada() {
        long antes = totalMunicipios();
        String t = admin();
        Response r = enviar(t, "ruim.csv", win("CODIGO;NOME;UF", "1;X;SP"));
        r.then().statusCode(400).body("codigo", equalTo("arquivo_invalido"));
        assertEquals(antes, totalMunicipios());
        Response lista = chamar("GET", "/api/admin/municipios/importacoes?limite=1", t, null);
        lista.then().statusCode(200).body("[0].estado", equalTo("falhou")).body("[0].nomeArquivo", equalTo("ruim.csv"))
                .body("[0].linhasLidas", equalTo(0));
    }

    @Test
    void arquivoVazioOuSemCampoOuTamanhoExcedido() {
        String t = admin();
        enviar(t, "vazio.csv", new byte[0]).then().statusCode(400).body("codigo", equalTo("arquivo_invalido"));
        // sem o campo "arquivo"
        given().header("Authorization", "Bearer " + t).accept("application/json")
                .multiPart("outro", "x.csv", "a;b".getBytes(StandardCharsets.UTF_8), "text/csv").post(URL)
                .then().statusCode(400).body("codigo", equalTo("arquivo_obrigatorio"));
        // JSON no lugar de multipart
        chamar("POST", URL, t, Map.of("a", 1)).then().statusCode(415).body("codigo", equalTo("tipo_nao_suportado"));
        // acima de 2 MB
        byte[] grande = new byte[2 * 1024 * 1024 + 1];
        java.util.Arrays.fill(grande, (byte) 'a');
        enviar(t, "grande.csv", grande).then().statusCode(413).body("codigo", equalTo("arquivo_grande_demais"));
    }

    @Test
    void nomeDoArquivoComCaminhoSoGuardaOUltimoSegmento() {
        Response r = enviar(admin(), "C:\\Users\\x\\Downloads\\municipios.csv",
                win(CAB_IBGE, "1;" + codigo() + ";X;" + nome("Cidade") + ";SP"));
        r.then().statusCode(200).body("nomeArquivo", equalTo("municipios.csv"));
    }

    // -------------------------------------------------------------------------------------- tudo ou nada

    @Test
    void falhaNoMeioDesfazTudo() {
        // o usuario do token deixa de existir: o historico (FK) falha no fim e a transacao inteira volta
        Usr u = dados.usuario(true, true);
        String t = token(u.email());
        QuarkusTransaction.requiringNew().run(() -> em.createQuery("delete from UsuarioEntity x where x.id = :i")
                .setParameter("i", u.id()).executeUpdate());
        String c = codigo();
        long antes = totalMunicipios();
        enviar(t, "f.csv", win(CAB_IBGE, "1;" + c + ";X;" + nome("Fantasma") + ";SP")).then().statusCode(500)
                .body("codigo", equalTo("erro_interno")).body("mensagem", not(containsString("SQL")));
        assertNull(municipio(c), "nada pode ter sido gravado");
        assertEquals(antes, totalMunicipios());
    }

    // ----------------------------------------------------------------------------------------- listagem

    @Test
    void listaUltimasImportacoesDaMaisRecenteParaAMaisAntiga() {
        String t = admin();
        enviar(t, "l1.csv", win(CAB_IBGE, "1;" + codigo() + ";X;" + nome("L") + ";SP")).then().statusCode(200);
        enviar(t, "l2.csv", win(CAB_IBGE, "1;" + codigo() + ";X;" + nome("L") + ";SP")).then().statusCode(200);
        Response r = chamar("GET", "/api/admin/municipios/importacoes?limite=2", t, null);
        r.then().statusCode(200).body("$", hasSize(2))
                .body("[0].nomeArquivo", equalTo("l2.csv")).body("[1].nomeArquivo", equalTo("l1.csv"))
                .body("[0].usuario.email", not(equalTo(null)))
                .body("[0].criados", equalTo(1));
        String corpo = r.asString();
        assertTrue(!corpo.contains("senha") && !corpo.contains("Hash"), "a listagem nao expoe senha nem hash");
        chamar("GET", "/api/admin/municipios/importacoes", t, null).then().statusCode(200);
        chamar("GET", "/api/admin/municipios/importacoes?limite=0", t, null).then().statusCode(400);
        chamar("GET", "/api/admin/municipios/importacoes?limite=101", t, null).then().statusCode(400);
    }

    // ------------------------------------------- correcao manual: escrita em /api/Municipio so do admin da plataforma

    @Test
    void correcaoManualDeMunicipioSoParaAdminDaPlataforma() {
        String id = dados.municipio();
        String empresa = dados.empresaAtiva();
        Map<String, Object> corpo = Map.of("nome", nome("Corrigido"));
        for (String papel : new String[] {"consulta", "operador", "administrador"}) {
            String t = token(dados.usuarioComPapel(empresa, papel).email());
            assertEquals(403, status("PATCH", "/api/Municipio/id/" + id, t, corpo), papel);
            assertEquals(403, status("PUT", "/api/Municipio/id/" + id, t, corpo), papel);
            assertEquals(403, status("POST", "/api/Municipio", t, corpo), papel);
            assertEquals(403, status("DELETE", "/api/Municipio/id/" + id, t, null), papel);
        }
        String adm = admin();
        assertEquals(403, status("DELETE", "/api/Municipio/id/" + id, adm, null)); // nem o admin apaga
        int s = status("PATCH", "/api/Municipio/id/" + id, adm, corpo);
        assertTrue(s == 200 || s == 204, "PATCH do admin da plataforma: " + s);
        assertEquals(corpo.get("nome"), QuarkusTransaction.requiringNew().call(() -> em.find(MunicipioEntity.class, id).getNome()));
    }
}
