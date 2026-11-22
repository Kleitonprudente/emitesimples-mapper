package br.com.emitesimples.nfseconfig;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** GET e PUT /api/configuracao/empresa: dados da empresa da sessao, editados pelo administrador da propria empresa. */
@QuarkusTest
class EmpresaDadosTest extends BaseApiTest {

    private static final String URL = "/api/configuracao/empresa";

    @Inject
    EntityManager em;

    private String admin(String empresaId) {
        return token(dados.usuarioComPapel(empresaId, "administrador").email());
    }

    private EmpresaEntity ler(String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, empresaId);
            em.detach(e);
            return e;
        });
    }

    private Map<String, Object> corpo(String municipioId) {
        Map<String, Object> m = new HashMap<>();
        m.put("razaoSocial", "Nova Razao Social Ltda");
        m.put("nomeFantasia", "Nova Fantasia");
        m.put("documento", novoDoc(14));
        m.put("inscricaoMunicipal", "98765");
        m.put("rgInscricaoEstadual", "123456789");
        m.put("email", "contato@nova.com.br");
        m.put("whatsapp", "(11) 98888-7777");
        m.put("telefone", "(11) 3333-4444");
        m.put("cep", "01310-100");
        m.put("logradouro", "Avenida Paulista");
        m.put("numeroEndereco", "1000");
        m.put("complemento", "Conj 12");
        m.put("bairro", "Bela Vista");
        m.put("municipio", Map.of("id", municipioId));
        m.put("regimeTributario", 4);
        m.put("regimeIbsCbs", 2);
        return m;
    }

    private static String novoDoc(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(java.util.concurrent.ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    private Response put(String tk, Object c) {
        return chamar("PUT", URL, tk, c);
    }

    private Response get(String tk) {
        return chamar("GET", URL, tk, null);
    }

    @Test
    void getDevolveOsDadosDaEmpresaEOBlocoDoCertificado() {
        String emp = dados.empresaAtiva();
        EmpresaEntity e = ler(emp);
        MunicipioEntity m = QuarkusTransaction.requiringNew().call(() -> em.find(MunicipioEntity.class, e.getMunicipio().getId()));
        get(admin(emp)).then().statusCode(200)
                .body("razaoSocial", equalTo(e.getRazaoSocial())).body("nomeFantasia", equalTo("Fantasia"))
                .body("documento", equalTo(e.getDocumento())).body("inscricaoMunicipal", nullValue())
                .body("rgInscricaoEstadual", nullValue()).body("email", nullValue()).body("whatsapp", nullValue())
                .body("telefone", nullValue()).body("complemento", nullValue())
                .body("municipio.id", equalTo(m.getId())).body("municipio.nome", equalTo(m.getNome()))
                .body("municipio.uf", equalTo("SP")).body("municipio.codigoIbge", equalTo(m.getCodigoIbge()))
                .body("regimeTributario", equalTo(1)).body("regimeIbsCbs", nullValue())
                .body("documentoEditavel", equalTo(true))
                .body("certificado.situacao", equalTo("nao_enviado")).body("certificado.validade", nullValue())
                .body("certificado.pendenteDeEnvio", equalTo(false)).body("certificado.venceEmDias", nullValue());
    }

    @Test
    void putGravaNormalizaEDevolveOGet() {
        String emp = dados.empresaAtiva();
        String novoMunicipio = dados.municipio();
        String tk = admin(emp);
        Map<String, Object> c = corpo(novoMunicipio);
        String doc = novoDoc(14);
        c.put("documento", doc.substring(0, 2) + "." + doc.substring(2, 5) + "." + doc.substring(5, 8) + "/" + doc.substring(8, 12) + "-" + doc.substring(12));
        put(tk, c).then().statusCode(200)
                .body("razaoSocial", equalTo("Nova Razao Social Ltda")).body("nomeFantasia", equalTo("Nova Fantasia"))
                .body("documento", equalTo(doc)).body("inscricaoMunicipal", equalTo("98765"))
                .body("rgInscricaoEstadual", equalTo("123456789")).body("email", equalTo("contato@nova.com.br"))
                .body("whatsapp", equalTo("(11) 98888-7777")).body("telefone", equalTo("1133334444"))
                .body("cep", equalTo("01310100")).body("logradouro", equalTo("Avenida Paulista"))
                .body("numeroEndereco", equalTo("1000")).body("complemento", equalTo("Conj 12"))
                .body("bairro", equalTo("Bela Vista")).body("municipio.id", equalTo(novoMunicipio))
                .body("regimeTributario", equalTo(4)).body("regimeIbsCbs", equalTo(2))
                .body("documentoEditavel", equalTo(true));
        Response g = get(tk);
        g.then().statusCode(200).body("razaoSocial", equalTo("Nova Razao Social Ltda"))
                .body("municipio.id", equalTo(novoMunicipio));
        EmpresaEntity e = ler(emp);
        assertEquals(doc, e.getDocumento());
        assertEquals("1133334444", e.getTelefone());
        assertEquals("01310100", e.getCep());
        assertEquals(4, e.getRegimeTributario());
        assertEquals(novoMunicipio, e.getMunicipio().getId());
        assertNotEquals(null, e.getDataAtualizacao());
    }

    @Test
    void putOpcionaisVaziosViramNulo() {
        String emp = dados.empresaAtiva();
        String tk = admin(emp);
        put(tk, corpo(dados.municipio())).then().statusCode(200);
        Map<String, Object> c = corpo(ler(emp).getMunicipio().getId());
        c.put("inscricaoMunicipal", "  ");
        c.put("rgInscricaoEstadual", null);
        c.put("email", "");
        c.put("whatsapp", null);
        c.put("telefone", "");
        c.put("complemento", " ");
        c.put("regimeIbsCbs", null);
        put(tk, c).then().statusCode(200).body("inscricaoMunicipal", nullValue()).body("rgInscricaoEstadual", nullValue())
                .body("email", nullValue()).body("whatsapp", nullValue()).body("telefone", nullValue())
                .body("complemento", nullValue()).body("regimeIbsCbs", nullValue());
    }

    @Test
    void putNaoMexeEmAmbienteSituacaoReferenciasGatewayCertificadoNemNfse() {
        String emp = dados.empresaAtiva();
        LocalDate validade = LocalDate.now().plusDays(100);
        QuarkusTransaction.requiringNew().run(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, emp);
            e.setReferenciaCertificado("ref-cert");
            e.setReferenciaTokenEmpresa("ref-token");
            e.setValidadeCertificado(validade);
            e.setSituacaoCertificado("valido");
            e.setNfseHabilitada(true);
            e.setSerieNfse("7");
            e.setUltimoNumeroNfse(41);
            e.setSituacao("ativa");
            e.setUrlWebhook("https://exemplo.com/x");
            e.setReferenciaCrm("crm-1");
        });
        EmpresaEntity antes = ler(emp);
        Map<String, Object> c = corpo(dados.municipio());
        c.put("ambiente", "producao");
        c.put("situacao", "suspensa");
        c.put("statusCadastro", "cadastrado");
        c.put("referenciaCertificado", "outra");
        c.put("situacaoCertificado", "invalido");
        c.put("nfseHabilitada", false);
        c.put("serieNfse", "99");
        c.put("ultimoNumeroNfse", 1);
        put(admin(emp), c).then().statusCode(200).body("certificado.situacao", equalTo("valido"))
                .body("certificado.pendenteDeEnvio", equalTo(false));
        EmpresaEntity depois = ler(emp);
        assertEquals(antes.getAmbiente(), depois.getAmbiente());
        assertEquals(antes.getSituacao(), depois.getSituacao());
        assertEquals(antes.getStatusCadastro(), depois.getStatusCadastro());
        assertEquals("ref-cert", depois.getReferenciaCertificado());
        assertEquals("ref-token", depois.getReferenciaTokenEmpresa());
        assertEquals(validade, depois.getValidadeCertificado());
        assertEquals("valido", depois.getSituacaoCertificado());
        assertTrue(depois.getNfseHabilitada());
        assertEquals("7", depois.getSerieNfse());
        assertEquals(41, depois.getUltimoNumeroNfse());
        assertEquals("https://exemplo.com/x", depois.getUrlWebhook());
        assertEquals("crm-1", depois.getReferenciaCrm());
    }

    // ------------------------------------------------------------------------------------------ validacoes

    @Test
    void validaTudoDeUmaVezComDetalhesPorCampo() {
        String emp = dados.empresaAtiva();
        String longo61 = "x".repeat(61);
        Map<String, Object> c = new HashMap<>();
        c.put("razaoSocial", "");
        c.put("nomeFantasia", longo61);
        c.put("documento", "123");
        c.put("inscricaoMunicipal", "y".repeat(21));
        c.put("rgInscricaoEstadual", "z".repeat(21));
        c.put("email", "isto-nao-e-email");
        c.put("whatsapp", "9".repeat(21));
        c.put("telefone", "123");
        c.put("cep", "1234");
        c.put("logradouro", longo61);
        c.put("numeroEndereco", longo61);
        c.put("complemento", longo61);
        c.put("bairro", "");
        c.put("municipio", Map.of("id", "nao-existe"));
        c.put("regimeTributario", 5);
        c.put("regimeIbsCbs", 9);
        Response r = put(admin(emp), c);
        r.then().statusCode(400).body("codigo", equalTo("requisicao_invalida"))
                .body("detalhes.campos.campo", hasItem("razaoSocial")).body("detalhes.campos.campo", hasItem("nomeFantasia"))
                .body("detalhes.campos.campo", hasItem("documento")).body("detalhes.campos.campo", hasItem("inscricaoMunicipal"))
                .body("detalhes.campos.campo", hasItem("rgInscricaoEstadual")).body("detalhes.campos.campo", hasItem("email"))
                .body("detalhes.campos.campo", hasItem("whatsapp")).body("detalhes.campos.campo", hasItem("telefone"))
                .body("detalhes.campos.campo", hasItem("cep")).body("detalhes.campos.campo", hasItem("logradouro"))
                .body("detalhes.campos.campo", hasItem("numeroEndereco")).body("detalhes.campos.campo", hasItem("complemento"))
                .body("detalhes.campos.campo", hasItem("bairro")).body("detalhes.campos.campo", hasItem("municipio"))
                .body("detalhes.campos.campo", hasItem("regimeTributario"))
                .body("detalhes.campos.campo", hasItem("regimeIbsCbs")).body("detalhes.campos[0].mensagem", notNullValue());
        assertEquals(16, r.jsonPath().getList("detalhes.campos").size());
        assertEquals("Fantasia", ler(emp).getNomeFantasia(), "nada foi gravado");
    }

    @Test
    void obrigatoriosAusentesEMunicipioSemId() {
        String emp = dados.empresaAtiva();
        Response r = put(admin(emp), Map.of("municipio", Map.of()));
        r.then().statusCode(400).body("detalhes.campos.campo", hasItem("razaoSocial"))
                .body("detalhes.campos.campo", hasItem("municipio")).body("detalhes.campos.campo", hasItem("regimeTributario"))
                .body("detalhes.campos.campo", hasItem("cep")).body("detalhes.campos.campo", hasItem("documento"))
                .body("detalhes.campos.campo", not(hasItem("telefone"))).body("detalhes.campos.campo", not(hasItem("email")));
        put(admin(emp), Map.of()).then().statusCode(400).body("detalhes.campos.campo", hasItem("municipio"));
    }

    @Test
    void limitesNoLimiteSaoAceitos() {
        String emp = dados.empresaAtiva();
        Map<String, Object> c = corpo(dados.municipio());
        c.put("razaoSocial", "r".repeat(60));
        c.put("nomeFantasia", "f".repeat(60));
        c.put("inscricaoMunicipal", "i".repeat(20));
        c.put("email", "a".repeat(48) + "@teste.com");
        c.put("telefone", "123456");
        c.put("complemento", "c".repeat(60));
        put(admin(emp), c).then().statusCode(200).body("razaoSocial", equalTo("r".repeat(60)));
        c.put("telefone", "1".repeat(14));
        put(admin(emp), c).then().statusCode(200);
        c.put("telefone", "1".repeat(15));
        put(admin(emp), c).then().statusCode(400).body("detalhes.campos.campo", hasItem("telefone"));
        c.put("telefone", "123456");
        String cpf = novoDoc(11);
        c.put("documento", cpf); // CPF
        put(admin(emp), c).then().statusCode(200).body("documento", equalTo(cpf));
    }

    // ------------------------------------------------------------------------------------------ documento

    @Test
    void documentoBloqueadoDepoisDeCadastrarOEmitente() {
        String emp = dados.empresaAtiva();
        String tk = admin(emp);
        String doc = ler(emp).getDocumento();
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, emp).setStatusCadastro("cadastrado"));
        get(tk).then().statusCode(200).body("documentoEditavel", equalTo(false));

        Map<String, Object> c = corpo(dados.municipio());
        c.put("documento", "99999999000191");
        put(tk, c).then().statusCode(409).body("codigo", equalTo("documento_bloqueado"));
        assertEquals(doc, ler(emp).getDocumento());
        assertNotEquals("Nova Fantasia", ler(emp).getNomeFantasia(), "nada foi gravado");

        c.put("documento", doc); // o mesmo documento passa (mesmo formatado)
        put(tk, c).then().statusCode(200).body("documento", equalTo(doc)).body("documentoEditavel", equalTo(false));
        c.remove("documento"); // ausente tambem
        put(tk, c).then().statusCode(200).body("documento", equalTo(doc));
        assertEquals("Nova Fantasia", ler(emp).getNomeFantasia());

        // com erro de outro campo, o 400 vem primeiro
        c.put("documento", "99999999000191");
        c.put("cep", "1");
        put(tk, c).then().statusCode(400);
    }

    @Test
    void statusDiferenteDeCadastradoPermiteTrocarODocumento() {
        String emp = dados.empresaAtiva();
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, emp).setStatusCadastro("erro"));
        Map<String, Object> cc = corpo(dados.municipio());
        put(admin(emp), cc).then().statusCode(200).body("documentoEditavel", equalTo(true))
                .body("documento", equalTo(cc.get("documento")));
    }

    @Test
    void documentoDuplicadoNaPlataformaDevolve409SemVazarAConstraint() {
        String a = dados.empresaAtiva();
        String b = dados.empresaAtiva();
        String docB = ler(b).getDocumento();
        Map<String, Object> c = corpo(dados.municipio());
        c.put("documento", docB);
        Response r = put(admin(a), c);
        r.then().statusCode(409).body("codigo", equalTo("documento_duplicado"))
                .body("mensagem", equalTo("Já existe uma empresa cadastrada com este documento."));
        String texto = r.asString().toLowerCase();
        assertFalse(texto.contains("constraint") || texto.contains("ux_empresa") || texto.contains("duplicate")
                || texto.contains("sql"), texto);
        assertNotEquals(docB, ler(a).getDocumento());
        assertEquals("Fantasia", ler(a).getNomeFantasia());
        // a empresa continua editavel depois do erro
        c.put("documento", novoDoc(14));
        put(admin(a), c).then().statusCode(200);
    }

    @Test
    void municipioInexistenteOuAusente() {
        String emp = dados.empresaAtiva();
        Map<String, Object> c = corpo("00000000-0000-0000-0000-000000000000");
        put(admin(emp), c).then().statusCode(400).body("detalhes.campos.campo", hasItem("municipio"))
                .body("detalhes.campos.find { it.campo == 'municipio' }.mensagem", containsString("não encontrado"));
        c.put("municipio", null);
        put(admin(emp), c).then().statusCode(400).body("detalhes.campos.campo", hasItem("municipio"));
    }

    // ------------------------------------------------------------------------------------------ acesso

    @Test
    void papeis() {
        String emp = dados.empresaAtiva();
        String adm = admin(emp);
        String op = token(dados.usuarioComPapel(emp, "operador").email());
        String con = token(dados.usuarioComPapel(emp, "consulta").email());
        for (String tk : new String[] {adm, op, con}) {
            get(tk).then().statusCode(200).body("documento", notNullValue());
        }
        Map<String, Object> c = corpo(dados.municipio());
        assertEquals(403, put(op, c).statusCode());
        assertEquals(403, put(con, c).statusCode());
        assertEquals(401, put(null, c).statusCode());
        assertEquals(401, get(null).statusCode());
        assertEquals("Fantasia", ler(emp).getNomeFantasia());
        for (String m : new String[] {"POST", "PATCH", "DELETE"}) {
            assertEquals(403, status(m, URL, adm, Map.of()), m + " negado");
        }
        put(adm, c).then().statusCode(200);
        // o CRUD gerado de Empresa segue so da plataforma
        assertEquals(403, chamar("PUT", "/api/Empresa/" + emp, adm, Map.of("nomeFantasia", "Hack")).statusCode());
        assertEquals("Nova Fantasia", ler(emp).getNomeFantasia());
    }

    @Test
    void plataformaSemEmpresaNaSessao() {
        String plataforma = token(dados.usuario(true, true).email());
        get(plataforma).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        assertEquals(403, put(plataforma, corpo(dados.municipio())).statusCode());
    }

    @Test
    void empresasSaoIsoladas() {
        String a = dados.empresaAtiva();
        String b = dados.empresaAtiva();
        String tkA = admin(a);
        String tkB = admin(b);
        Map<String, Object> ca = corpo(dados.municipio());
        put(tkA, ca).then().statusCode(200);
        get(tkB).then().statusCode(200).body("nomeFantasia", equalTo("Fantasia")).body("documento", equalTo(ler(b).getDocumento()));
        // o corpo nao troca de empresa
        Map<String, Object> c = corpo(dados.municipio());
        c.put("documento", novoDoc(14));
        c.put("empresaId", a);
        c.put("id", a);
        c.put("empresa", Map.of("id", a));
        c.put("nomeFantasia", "Fantasia de B");
        put(tkB, c).then().statusCode(200).body("nomeFantasia", equalTo("Fantasia de B"));
        assertEquals("Nova Fantasia", ler(a).getNomeFantasia());
        assertEquals(ca.get("documento"), ler(a).getDocumento());
        assertEquals("Fantasia de B", ler(b).getNomeFantasia());
        assertNull(ler(b).getReferenciaCertificado());
    }
}
