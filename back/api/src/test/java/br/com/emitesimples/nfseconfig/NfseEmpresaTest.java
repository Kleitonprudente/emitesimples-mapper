package br.com.emitesimples.nfseconfig;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.IntegraNotasStubResource;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.gateway.WebhookToken;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.plataforma.SegredoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** GET e PUT /api/configuracao/nfse. O IntegraNotas e SIMULADO (IntegraNotasStubResource): nenhuma chamada externa. */
@QuarkusTest
class NfseEmpresaTest extends BaseApiTest {

    private static final String URL = "/api/configuracao/nfse";
    private static final String CFG = "/api/admin/configuracoes/integranotas";
    private static final String HOM = "https://hom-api.integranotas.com.br";
    private static final String PROD = "https://api.integranotas.com.br";
    private static final String TOKEN_SOFTHOUSE = "soft-token-de-teste-123";
    private static final String BASE_WEBHOOK = "https://api.exemplo.com.br";
    private static final ObjectMapper JSON = new ObjectMapper();

    @ConfigProperty(name = IntegraNotasStubResource.PROP_URL)
    String urlStub;

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @Inject
    WebhookToken webhookToken;

    private String plataforma;

    private String plataforma() {
        if (plataforma == null) {
            plataforma = token(dados.usuario(true, true).email());
        }
        return plataforma;
    }

    private void configurar(String urlPublica, String tokenSofthouse) {
        Map<String, Object> c = new HashMap<>();
        c.put("ambienteAtivo", "HOMOLOGACAO");
        c.put("urlHomologacao", urlStub);
        c.put("urlProducao", PROD);
        c.put("urlPublicaWebhook", urlPublica == null ? "" : urlPublica);
        c.put("tokenHomologacao", tokenSofthouse == null ? "" : tokenSofthouse);
        c.put("tokenProducao", "");
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
    }

    @BeforeEach
    void preparar() {
        configurar(null, TOKEN_SOFTHOUSE);
    }

    @AfterEach
    void restaurar() {
        Map<String, Object> c = new HashMap<>();
        c.put("urlHomologacao", HOM);
        c.put("urlPublicaWebhook", "");
        c.put("tokenHomologacao", "");
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
    }

    // ------------------------------------------------------------------------------------------ apoio

    /** Empresa com todos os dados do emitente. {@code fantasia} pode levar um marcador de falha do stub. */
    private String empresa(String fantasia) {
        String id = dados.empresaAtiva();
        QuarkusTransaction.requiringNew().run(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, id);
            e.setNomeFantasia(fantasia);
            e.setRazaoSocial("Razao Social Teste Ltda");
            e.setTelefone("(11)988887777");
            e.setLogradouro("Rua das Flores");
            e.setNumeroEndereco("123");
            e.setBairro("Centro");
            e.setCep("01310100");
            e.setComplemento("Sala 4");
            e.setInscricaoMunicipal("12345");
            e.setEmail("empresa@teste.local");
            e.setRegimeTributario(1);
        });
        return id;
    }

    private String empresa() {
        return empresa("Fantasia Teste");
    }

    private void renomear(String empresaId, String fantasia) {
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, empresaId).setNomeFantasia(fantasia));
    }

    private String admin(String empresaId) {
        return token(dados.usuarioComPapel(empresaId, "ADMINISTRADOR").email());
    }

    private static Map<String, Object> habilitar(String serie, long ultimo) {
        Map<String, Object> m = new HashMap<>();
        m.put("habilitada", true);
        m.put("serie", serie);
        m.put("ultimoNumero", ultimo);
        return m;
    }

    private static Map<String, Object> desabilitar() {
        return Map.of("habilitada", false);
    }

    private Response put(String tk, Object corpo) {
        return chamar("PUT", URL, tk, corpo);
    }

    private Response get(String tk) {
        return chamar("GET", URL, tk, null);
    }

    private int chamadasAoStub() {
        return Integer.parseInt(given().get(urlStub + "/_contagem").asString().trim());
    }

    private JsonNode ultimaDoStub() throws Exception {
        return JSON.readTree(given().get(urlStub + "/_ultima").asString());
    }

    private EmpresaEntity ler(String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, empresaId);
            em.detach(e);
            return e;
        });
    }

    private void criarNota(String empresaId, String numero, String serie) {
        String pessoa = dados.pessoa(empresaId, "Tomador " + System.nanoTime());
        String servico = dados.servico(empresaId, "Servico " + System.nanoTime());
        QuarkusTransaction.requiringNew().run(() -> em.persist(NotaEntity.builderOf()
                .withNumero(numero).withSerie(serie).withAmbiente("HOMOLOGACAO").withEstado("AUTORIZADA")
                .withDataEmissao(java.time.LocalDateTime.now())
                .withDescricao("Nota")
                .withEmpresa(em.getReference(EmpresaEntity.class, empresaId))
                .withPessoa(em.getReference(br.com.emitesimples.cadastros.entity.PessoaEntity.class, pessoa))
                .withServico(em.getReference(ServicoEntity.class, servico)).build()));
    }

    private String ibgeDa(String empresaId) {
        return QuarkusTransaction.requiringNew().call(
                () -> em.find(EmpresaEntity.class, empresaId).getMunicipio().getCodigoIbge());
    }

    private String textoDaLinha(String empresaId) {
        return (String) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select cast(e as text) from empresa e where e.id = ?1").setParameter(1, empresaId).getSingleResult());
    }

    private byte[] cifradoDo(String segredoId) {
        return (byte[]) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select valor_cifrado from segredo where id = ?1").setParameter(1, segredoId).getSingleResult());
    }

    // ------------------------------------------------------------------------------------------ habilitar

    @Test
    void habilitaCadastraOEmitenteComOPayloadExatoEGuardaOTokenNoCofre() throws Exception {
        configurar(BASE_WEBHOOK, TOKEN_SOFTHOUSE);
        String emp = empresa();
        String tk = admin(emp);
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response r;
        try {
            r = put(tk, habilitar("12", 99));
        } finally {
            descapturar(h);
        }
        r.then().statusCode(200)
                .body("habilitada", equalTo(true)).body("serie", equalTo("12")).body("ultimoNumero", equalTo(99))
                .body("proximoNumero", equalTo(100)).body("ambiente", equalTo("HOMOLOGACAO"))
                .body("statusCadastro", equalTo("CADASTRADO")).body("dataCadastroGateway", notNullValue())
                .body("emitenteTokenConfigurado", equalTo(true)).body("integracaoDisponivel", equalTo(true))
                .body("validadeCertificado", nullValue());

        JsonNode ultima = ultimaDoStub();
        String tokenEmitente = ultima.path("tokenDevolvido").asText();
        assertTrue(tokenEmitente.startsWith("emitente-"));
        assertEquals(TOKEN_SOFTHOUSE, ultima.path("authorization").asText(), "Authorization e o token cru, sem Bearer");
        JsonNode c = ultima.path("corpo");
        EmpresaEntity e = ler(emp);
        assertEquals("Fantasia Teste", c.path("nome").asText());
        assertEquals("Razao Social Teste Ltda", c.path("razao").asText());
        assertEquals(1, c.path("crt").asInt());
        assertTrue(c.path("crt").isInt());
        assertEquals("11988887777", c.path("telefone").asText());
        assertEquals("Rua das Flores", c.path("rua").asText());
        assertEquals("123", c.path("numero").asText());
        assertEquals("Centro", c.path("bairro").asText());
        assertEquals(ibgeDa(emp), c.path("cmun").asText());
        assertEquals(7, c.path("cmun").asText().length());
        assertEquals("SP", c.path("uf").asText());
        assertEquals("01310100", c.path("cep").asText());
        assertEquals(e.getDocumento(), c.path("cnpj").asText());
        assertFalse(c.has("cpf"));
        assertEquals("12345", c.path("im").asText());
        assertEquals("empresa@teste.local", c.path("email").asText());
        assertEquals("Sala 4", c.path("complemento").asText());
        assertTrue(c.path("documentos").path("nfse").asBoolean());
        assertEquals(BASE_WEBHOOK + "/api/webhooks/integranotas/" + emp + "/" + webhookToken.gerar(emp),
                c.path("webhook").asText());
        assertFalse(c.has("regime_cibs"), "regime IBS/CBS ausente na empresa: nao enviado");

        // empresa gravada
        assertEquals("CADASTRADO", e.getStatusCadastro());
        assertTrue(e.getNfseHabilitada());
        assertEquals("12", e.getSerieNfse());
        assertEquals(99, e.getUltimoNumeroNfse());
        assertNotNull(e.getDataCadastroGateway());
        assertEquals("HOMOLOGACAO", e.getAmbiente());

        // token: no cofre (cifrado), nunca em claro, nunca na resposta nem em log
        String ref = e.getReferenciaTokenEmpresa();
        assertNotNull(ref);
        assertEquals(tokenEmitente, segredos.ler(ref));
        assertFalse(new String(cifradoDo(ref), StandardCharsets.ISO_8859_1).contains(tokenEmitente));
        assertFalse(textoDaLinha(emp).contains(tokenEmitente), "o banco da empresa nao guarda o token em claro");
        assertFalse(r.asString().contains(tokenEmitente));
        assertFalse(get(tk).asString().contains(tokenEmitente));
        String todosLogs = String.join("\n", logs);
        assertFalse(todosLogs.contains(tokenEmitente), "token do emitente no log");
        assertFalse(todosLogs.contains(TOKEN_SOFTHOUSE), "token da softhouse no log");
        assertTrue(logs.stream().anyMatch(l -> l.contains("IntegraNotas")), "a chamada deixa rastro (sem segredos)");
    }

    @Test
    void semUrlPublicaOWebhookNaoVaiNoPayload() throws Exception {
        String emp = empresa();
        put(admin(emp), habilitar("1", 0)).then().statusCode(200);
        assertFalse(ultimaDoStub().path("corpo").has("webhook"));
    }

    @Test
    void regimeIbsCbsEViradoParaRegimeCibs() throws Exception {
        String emp = empresa();
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, emp).setRegimeIbsCbs(2));
        put(admin(emp), habilitar("1", 0)).then().statusCode(200);
        assertEquals(2, ultimaDoStub().path("corpo").path("regime_cibs").asInt());
    }

    @Test
    void documentoDeOnzeDigitosViraCpf() throws Exception {
        String emp = empresa();
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, emp).setDocumento("12345678901"));
        put(admin(emp), habilitar("1", 0)).then().statusCode(200);
        JsonNode c = ultimaDoStub().path("corpo");
        assertEquals("12345678901", c.path("cpf").asText());
        assertFalse(c.has("cnpj"));
    }

    @Test
    void segundoPutNaoRecriaOEmitente() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar("1", 5)).then().statusCode(200);
        int antes = chamadasAoStub();
        String ref = ler(emp).getReferenciaTokenEmpresa();
        put(tk, habilitar("1", 8)).then().statusCode(200).body("ultimoNumero", equalTo(8))
                .body("statusCadastro", equalTo("CADASTRADO"));
        put(tk, habilitar("1", 8)).then().statusCode(200);
        assertEquals(antes, chamadasAoStub(), "idempotente: o gateway nao e chamado de novo");
        assertEquals(ref, ler(emp).getReferenciaTokenEmpresa());
    }

    @Test
    void desabilitarEReabilitarMantemOEmitenteESerie() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar("3", 20)).then().statusCode(200);
        int antes = chamadasAoStub();
        String ref = ler(emp).getReferenciaTokenEmpresa();

        put(tk, desabilitar()).then().statusCode(200).body("habilitada", equalTo(false)).body("serie", equalTo("3"))
                .body("ultimoNumero", equalTo(20)).body("statusCadastro", equalTo("CADASTRADO"))
                .body("emitenteTokenConfigurado", equalTo(true));
        assertEquals(ref, ler(emp).getReferenciaTokenEmpresa());

        put(tk, habilitar("3", 20)).then().statusCode(200).body("habilitada", equalTo(true));
        assertEquals(antes, chamadasAoStub());
        assertEquals(ref, ler(emp).getReferenciaTokenEmpresa());
    }

    @Test
    void desabilitarEmpresaSemEmitenteSoGravaAFlag() {
        String emp = empresa();
        int antes = chamadasAoStub();
        put(admin(emp), desabilitar()).then().statusCode(200).body("habilitada", equalTo(false))
                .body("emitenteTokenConfigurado", equalTo(false));
        assertEquals(antes, chamadasAoStub());
    }

    // ------------------------------------------------------------------------------------------ validacoes

    @Test
    void cadastroIncompletoDaEmpresaDevolve422ComOsCampos() {
        String emp = dados.empresaAtiva(); // sem telefone, endereco e cep
        int antes = chamadasAoStub();
        put(admin(emp), habilitar("1", 0)).then().statusCode(422).body("codigo", equalTo("cadastro_incompleto"))
                .body("mensagem", equalTo("Complete os dados da empresa para habilitar a NFS-e."))
                .body("detalhes.campos.campo", hasItem("telefone")).body("detalhes.campos.campo", hasItem("logradouro"))
                .body("detalhes.campos.campo", hasItem("numeroEndereco")).body("detalhes.campos.campo", hasItem("bairro"))
                .body("detalhes.campos.campo", hasItem("cep"))
                .body("detalhes.campos.campo", not(hasItem("razaoSocial")))
                .body("detalhes.campos[0].mensagem", notNullValue());
        assertEquals(antes, chamadasAoStub());
        assertFalse(ler(emp).getNfseHabilitada());
    }

    @Test
    void integracaoNaoConfiguradaDevolve409() {
        configurar(null, null);
        String emp = empresa();
        String tk = admin(emp);
        get(tk).then().statusCode(200).body("integracaoDisponivel", equalTo(false));
        put(tk, habilitar("1", 0)).then().statusCode(409).body("codigo", equalTo("integranotas_nao_configurada"))
                .body("mensagem", equalTo("A integração com a IntegraNotas não está configurada. Fale com o suporte."));
        assertFalse(ler(emp).getNfseHabilitada());
    }

    @Test
    void validaSerieENumeroDoCorpo() {
        String emp = empresa();
        String tk = admin(emp);
        int antes = chamadasAoStub();
        for (String serie : new String[] {null, "", "  ", "abc", "123456", "1a", "-1", "1 2"}) {
            put(tk, habilitar(serie, 0)).then().statusCode(400).body("codigo", equalTo("requisicao_invalida"))
                    .body("detalhes.campos[0].campo", equalTo("serie"));
        }
        for (Long n : new Long[] {null, -1L, 1_000_000_000L}) {
            Map<String, Object> m = habilitar("1", 0);
            m.put("ultimoNumero", n);
            put(tk, m).then().statusCode(400).body("detalhes.campos[0].campo", equalTo("ultimoNumero"));
        }
        put(tk, Map.of("serie", "1")).then().statusCode(400); // sem 'habilitada'
        assertEquals(antes, chamadasAoStub());
        put(tk, habilitar("99999", 999_999_999L)).then().statusCode(200).body("proximoNumero", equalTo(1_000_000_000));
    }

    @Test
    void numeracaoNuncaAndaParaTrasNemAbaixoDasNotas() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar("1", 10)).then().statusCode(200);
        put(tk, habilitar("1", 9)).then().statusCode(409).body("codigo", equalTo("numeracao_invalida"));
        assertEquals(10, ler(emp).getUltimoNumeroNfse());

        criarNota(emp, "50", "1");
        put(tk, habilitar("1", 49)).then().statusCode(409).body("codigo", equalTo("numeracao_invalida"));
        put(tk, habilitar("1", 50)).then().statusCode(200).body("ultimoNumero", equalTo(50));
        put(tk, habilitar("1", 40)).then().statusCode(409).body("codigo", equalTo("numeracao_invalida"));
        // com nota, nao muda de serie
        put(tk, habilitar("2", 0)).then().statusCode(409).body("codigo", equalTo("numeracao_invalida"));
        assertEquals("1", ler(emp).getSerieNfse());
    }

    @Test
    void notaMaiorQueOContadorBarraONumeroMenor() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar("1", 5)).then().statusCode(200);
        criarNota(emp, "700", "1"); // contador defasado em relacao as notas
        put(tk, habilitar("1", 699)).then().statusCode(409).body("codigo", equalTo("numeracao_invalida"));
        put(tk, habilitar("1", 700)).then().statusCode(200);
    }

    @Test
    void mudarDeSerieSoSemNotas() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar("1", 30)).then().statusCode(200);
        put(tk, habilitar("2", 0)).then().statusCode(200).body("serie", equalTo("2")).body("ultimoNumero", equalTo(0));
        put(tk, habilitar("2", 7)).then().statusCode(200);
        put(tk, habilitar("2", 3)).then().statusCode(409).body("codigo", equalTo("numeracao_invalida"));
    }

    // ------------------------------------------------------------------------------------------ falhas do gateway

    private void falha(String marcador, int status, String codigo) {
        String emp = empresa("Fant " + marcador);
        String tk = admin(emp);
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response r;
        try {
            r = put(tk, habilitar("1", 0));
        } finally {
            descapturar(h);
        }
        r.then().statusCode(status).body("codigo", equalTo(codigo));
        assertFalse(r.asString().contains(TOKEN_SOFTHOUSE));
        assertFalse(String.join("\n", logs).contains(TOKEN_SOFTHOUSE));
        EmpresaEntity e = ler(emp);
        assertEquals("ERRO", e.getStatusCadastro());
        assertFalse(e.getNfseHabilitada());
        assertEquals(null, e.getReferenciaTokenEmpresa());
        get(tk).then().statusCode(200).body("statusCadastro", equalTo("ERRO")).body("habilitada", equalTo(false))
                .body("emitenteTokenConfigurado", equalTo(false));

        // corrigido o problema, o mesmo PUT recupera
        renomear(emp, "Fantasia Corrigida");
        put(tk, habilitar("1", 0)).then().statusCode(200).body("statusCadastro", equalTo("CADASTRADO"))
                .body("habilitada", equalTo(true));
        assertNotNull(ler(emp).getReferenciaTokenEmpresa());
    }

    @Test
    void gatewayRecusaComErrosResumidosEStatusErro() {
        String emp = empresa("Fant " + IntegraNotasStubResource.M_RECUSA);
        String tk = admin(emp);
        put(tk, habilitar("1", 0)).then().statusCode(502).body("codigo", equalTo("integranotas_recusou"))
                .body("mensagem", containsString("Dados recusados."))
                .body("detalhes.erros", hasItem("cmun: Município inválido"))
                .body("detalhes.erros", hasItem("cep: CEP inválido")).body("detalhes.codigoGateway", equalTo(4002));
        assertEquals("ERRO", ler(emp).getStatusCadastro());
    }

    @Test
    void recusaDoGatewayRecuperaDepois() {
        falha(IntegraNotasStubResource.M_RECUSA, 502, "integranotas_recusou");
    }

    @Test
    void tokenDaSofthouseRecusadoPeloGateway6001() {
        falha(IntegraNotasStubResource.M_6001, 502, "integranotas_token_recusado");
    }

    @Test
    void tokenDaSofthouseRecusadoPorTokenInvalidoNoHeader() {
        configurar(null, IntegraNotasStubResource.TOKEN_RECUSADO);
        String emp = empresa();
        put(admin(emp), habilitar("1", 0)).then().statusCode(502).body("codigo", equalTo("integranotas_token_recusado"));
        assertEquals("ERRO", ler(emp).getStatusCadastro());
    }

    @Test
    void gatewayComErro500() {
        falha(IntegraNotasStubResource.M_500, 502, "integranotas_resposta_invalida");
    }

    @Test
    void gatewayLentoViraIndisponivel() {
        falha(IntegraNotasStubResource.M_LENTO, 504, "integranotas_indisponivel");
    }

    @Test
    void gatewayComJsonInvalido() {
        falha(IntegraNotasStubResource.M_JSON, 502, "integranotas_resposta_invalida");
    }

    @Test
    void gatewayComRedirectNaoESeguido() {
        falha(IntegraNotasStubResource.M_REDIRECT, 502, "integranotas_resposta_invalida");
    }

    @Test
    void gatewayComSucessoSemToken() {
        falha(IntegraNotasStubResource.M_SEM_TOKEN, 502, "integranotas_resposta_invalida");
    }

    @Test
    void gatewayComCorpoAcimaDeUmMega() {
        falha(IntegraNotasStubResource.M_GIGANTE, 502, "integranotas_resposta_invalida");
    }

    @Test
    void gatewayForaDoArViraIndisponivel() {
        // porta fechada: a conexao e recusada
        Map<String, Object> c = new HashMap<>();
        c.put("urlHomologacao", "http://127.0.0.1:1");
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
        String emp = empresa();
        put(admin(emp), habilitar("1", 0)).then().statusCode(504).body("codigo", equalTo("integranotas_indisponivel"));
        assertEquals("ERRO", ler(emp).getStatusCadastro());
    }

    // ------------------------------------------------------------------------------------------ papeis e isolamento

    @Test
    void papeis() {
        String emp = empresa();
        String adm = admin(emp);
        String op = token(dados.usuarioComPapel(emp, "OPERADOR").email());
        String con = token(dados.usuarioComPapel(emp, "CONSULTA").email());
        for (String tk : new String[] {adm, op, con}) {
            get(tk).then().statusCode(200).body("habilitada", equalTo(false)).body("serie", nullValue())
                    .body("ultimoNumero", equalTo(0)).body("proximoNumero", equalTo(1))
                    .body("emitenteTokenConfigurado", equalTo(false)).body("integracaoDisponivel", equalTo(true));
        }
        int antes = chamadasAoStub();
        assertEquals(403, put(op, habilitar("1", 0)).statusCode());
        assertEquals(403, put(con, habilitar("1", 0)).statusCode());
        assertEquals(401, put(null, habilitar("1", 0)).statusCode());
        assertEquals(401, get(null).statusCode());
        assertEquals(antes, chamadasAoStub());
        assertFalse(ler(emp).getNfseHabilitada());
        for (String m : new String[] {"POST", "PATCH", "DELETE"}) {
            assertEquals(403, status(m, URL, adm, Map.of()), m + " negado");
        }
        put(adm, habilitar("1", 0)).then().statusCode(200);
    }

    @Test
    void plataformaSemEmpresaNaSessaoNaoTemNada() {
        get(plataforma()).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        assertEquals(403, put(plataforma(), habilitar("1", 0)).statusCode());
    }

    @Test
    void empresasSaoIsoladas() {
        String a = empresa();
        String b = empresa();
        String tkA = admin(a);
        String tkB = admin(b);
        put(tkA, habilitar("7", 70)).then().statusCode(200);
        get(tkB).then().statusCode(200).body("habilitada", equalTo(false)).body("serie", nullValue())
                .body("ultimoNumero", equalTo(0));
        put(tkB, habilitar("8", 1)).then().statusCode(200).body("serie", equalTo("8"));
        get(tkA).then().body("serie", equalTo("7")).body("ultimoNumero", equalTo(70));
        assertNotEquals(ler(a).getReferenciaTokenEmpresa(), ler(b).getReferenciaTokenEmpresa());
        // nota de A nao interfere na numeracao de B
        criarNota(a, "900", "8");
        put(tkB, habilitar("8", 2)).then().statusCode(200);
        // o corpo nao troca de empresa
        Map<String, Object> corpo = habilitar("8", 3);
        corpo.put("empresaId", a);
        corpo.put("empresa", Map.of("id", a));
        put(tkB, corpo).then().statusCode(200);
        assertEquals("7", ler(a).getSerieNfse());
        assertEquals(70, ler(a).getUltimoNumeroNfse());
    }

    // ------------------------------------------------------------------------------------------ concorrencia

    @Test
    void doisPutsSimultaneosCriamUmSoEmitente() throws Exception {
        String emp = empresa("Fant " + IntegraNotasStubResource.M_DEVAGAR);
        String tk = admin(emp);
        int antes = chamadasAoStub();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> tarefas = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tarefas.add(() -> put(tk, habilitar("1", 5)).statusCode());
            }
            for (Future<Integer> f : pool.invokeAll(tarefas)) {
                assertEquals(200, f.get());
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(antes + 1, chamadasAoStub(), "um unico emitente criado");
        EmpresaEntity e = ler(emp);
        assertEquals("CADASTRADO", e.getStatusCadastro());
        assertTrue(e.getNfseHabilitada());
    }

    // ------------------------------------------------------------------------------------------ logs

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
