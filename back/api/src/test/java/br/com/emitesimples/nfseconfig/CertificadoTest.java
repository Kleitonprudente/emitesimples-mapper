package br.com.emitesimples.nfseconfig;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.IntegraNotasStubResource;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.plataforma.SegredoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * POST /api/configuracao/empresa/certificado (certificado A1) e o envio ao gateway. O IntegraNotas e SIMULADO
 * ({@link IntegraNotasStubResource}): nenhuma chamada externa. Os PFX de teste estao em src/test/resources/certificados.
 */
@QuarkusTest
class CertificadoTest extends BaseApiTest {

    private static final String URL = "/api/configuracao/empresa/certificado";
    private static final String NFSE = "/api/configuracao/nfse";
    private static final String EMPRESA = "/api/configuracao/empresa";
    private static final String CFG = "/api/admin/configuracoes/integranotas";
    private static final String HOM = "https://hom-api.integranotas.com.br";
    private static final String TOKEN_SOFTHOUSE = "soft-token-de-teste-cert";
    private static final String CNPJ_DO_CERTIFICADO = "27865757000102";
    private static final ObjectMapper JSON = new ObjectMapper();

    @ConfigProperty(name = IntegraNotasStubResource.PROP_URL)
    String urlStub;

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @Inject
    LimiteCertificado limite;

    private final List<String> comCnpjDoCertificado = new ArrayList<>();
    private String plataforma;

    private String plataforma() {
        if (plataforma == null) {
            plataforma = token(dados.usuario(true, true).email());
        }
        return plataforma;
    }

    @BeforeEach
    void preparar() {
        limite.limparTudo();
        Map<String, Object> c = new HashMap<>();
        c.put("ambienteAtivo", "homologacao");
        c.put("urlHomologacao", urlStub);
        c.put("urlPublicaWebhook", "");
        c.put("tokenHomologacao", TOKEN_SOFTHOUSE);
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
        modoGateway("ok");
    }

    @AfterEach
    void restaurar() {
        modoGateway("ok");
        liberarCnpj();
        Map<String, Object> c = new HashMap<>();
        c.put("urlHomologacao", HOM);
        c.put("tokenHomologacao", "");
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
    }

    // ------------------------------------------------------------------------------------------ apoio

    private void liberarCnpj() {
        for (String id : comCnpjDoCertificado) {
            QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, id).setDocumento(docAleatorio()));
        }
        comCnpjDoCertificado.clear();
    }

    private static String docAleatorio() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    /** Empresa pronta para habilitar a NFS-e, com o CNPJ do certificado valido. */
    private String empresa() {
        liberarCnpj();
        String id = dados.empresaAtiva();
        QuarkusTransaction.requiringNew().run(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, id);
            e.setDocumento(CNPJ_DO_CERTIFICADO);
            e.setNomeFantasia("Fantasia Teste");
            e.setRazaoSocial("Razao Social Teste Ltda");
            e.setTelefone("11988887777");
            e.setLogradouro("Rua das Flores");
            e.setNumeroEndereco("123");
            e.setBairro("Centro");
            e.setCep("01310100");
            e.setRegimeTributario(1);
        });
        comCnpjDoCertificado.add(id);
        return id;
    }

    private String admin(String empresaId) {
        return token(dados.usuarioComPapel(empresaId, "administrador").email());
    }

    private static byte[] arquivo(String nome) {
        try {
            Path p = Paths.get("src/test/resources/certificados", nome);
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RequestSpecification multipart(String tk, String nomeNoUpload, byte[] bytes, String senha) {
        RequestSpecification s = given().accept("application/json");
        if (tk != null) {
            s = s.header("Authorization", "Bearer " + tk);
        }
        if (bytes != null) {
            s = s.multiPart("arquivo", nomeNoUpload, bytes, "application/x-pkcs12");
        }
        if (senha != null) {
            s = s.multiPart("senha", senha);
        }
        return s;
    }

    private Response enviar(String tk, String arquivoDeTeste, String senha) {
        return multipart(tk, arquivoDeTeste, arquivo(arquivoDeTeste), senha).post(URL);
    }

    private Response enviarValido(String tk) {
        return enviar(tk, "valido.pfx", "senha123");
    }

    private Response habilitar(String tk) {
        return chamar("PUT", NFSE, tk, Map.of("habilitada", true, "serie", "1", "ultimoNumero", 0));
    }

    private void modoGateway(String modo) {
        given().body(modo).post(urlStub + "/_modo_certificado").then().statusCode(200);
    }

    private int enviosAoStub() {
        return Integer.parseInt(given().get(urlStub + "/_contagem_certificado").asString().trim());
    }

    private JsonNode ultimoEnvioAoStub() throws Exception {
        return JSON.readTree(given().get(urlStub + "/_ultimo_certificado").asString());
    }

    private JsonNode ultimoEmitenteDoStub() throws Exception {
        return JSON.readTree(given().get(urlStub + "/_ultima").asString());
    }

    private EmpresaEntity ler(String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, empresaId);
            em.detach(e);
            return e;
        });
    }

    private String textoDaLinha(String empresaId) {
        return (String) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select cast(e as text) from empresa e where e.id = ?1").setParameter(1, empresaId).getSingleResult());
    }

    private byte[] cifradoDo(String segredoId) {
        return (byte[]) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select valor_cifrado from segredo where id = ?1").setParameter(1, segredoId).getSingleResult());
    }

    private long segredosComId(String id) {
        return ((Number) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select count(*) from segredo where id = ?1").setParameter(1, id).getSingleResult())).longValue();
    }

    // ------------------------------------------------------------------------------------------ sucesso

    @Test
    void validoComNfseHabilitadaEnviaAoGatewayEGuardaCifrado() throws Exception {
        String emp = empresa();
        String tk = admin(emp);
        habilitar(tk).then().statusCode(200);
        String tokenEmitente = ultimoEmitenteDoStub().path("tokenDevolvido").asText();
        int antes = enviosAoStub();
        byte[] pfx = arquivo("valido.pfx");
        String b64 = Base64.getEncoder().encodeToString(pfx);

        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response r;
        try {
            r = enviarValido(tk);
        } finally {
            descapturar(h);
        }
        r.then().statusCode(200).body("situacao", equalTo("valido")).body("validade", notNullValue())
                .body("pendenteDeEnvio", equalTo(false)).body("venceEmDias", greaterThan(36000))
                .body("avisos", empty());

        assertEquals(antes + 1, enviosAoStub());
        JsonNode env = ultimoEnvioAoStub();
        assertEquals(tokenEmitente, env.path("authorization").asText(), "Authorization = token do emitente, cru");
        assertEquals(b64, env.path("certificado").asText(), "corpo: Base64 do arquivo");
        assertEquals("senha123", env.path("senha").asText());
        assertTrue(env.path("contentType").asText().startsWith("application/json"));

        EmpresaEntity e = ler(emp);
        assertEquals("valido", e.getSituacaoCertificado());
        assertNotNull(e.getValidadeCertificado());
        assertTrue(e.getValidadeCertificado().getYear() >= 2126);
        String ref = e.getReferenciaCertificado();
        assertNotNull(ref);

        // cofre: cifrado; decifrar devolve os dados; nada em claro no banco, na resposta, no GET ou no log
        JsonNode guardado = JSON.readTree(segredos.ler(ref));
        assertEquals(b64, guardado.path("pfx").asText());
        assertEquals("senha123", guardado.path("senha").asText());
        String cifrado = new String(cifradoDo(ref), StandardCharsets.ISO_8859_1);
        assertFalse(cifrado.contains("senha123"));
        assertFalse(cifrado.contains(b64.substring(0, 40)));
        String linha = textoDaLinha(emp);
        assertFalse(linha.contains("senha123"));
        assertFalse(linha.contains(b64.substring(0, 40)));
        assertFalse(r.asString().contains("senha123") || r.asString().contains(b64.substring(0, 40)));
        String get = chamar("GET", EMPRESA, tk, null).asString() + chamar("GET", NFSE, tk, null).asString();
        assertFalse(get.contains("senha123") || get.contains(b64.substring(0, 40)) || get.contains(ref));
        String todosLogs = String.join("\n", logs);
        assertFalse(todosLogs.contains("senha123"), "senha no log");
        assertFalse(todosLogs.contains(b64.substring(0, 40)), "arquivo no log");
        assertFalse(todosLogs.contains(tokenEmitente), "token do emitente no log");
        assertTrue(logs.stream().anyMatch(l -> l.contains("/v1/certificado")), "a chamada deixa rastro (sem segredos)");

        // consistencia entre as telas
        chamar("GET", EMPRESA, tk, null).then().statusCode(200).body("certificado.situacao", equalTo("valido"))
                .body("certificado.pendenteDeEnvio", equalTo(false)).body("certificado.validade", notNullValue());
        chamar("GET", NFSE, tk, null).then().statusCode(200).body("certificadoSituacao", equalTo("valido"))
                .body("validadeCertificado", notNullValue());
    }

    @Test
    void certificadoComExtensaoP12EMaiusculasEAceito() {
        String emp = empresa();
        multipart(admin(emp), "MEU CERTIFICADO.P12", arquivo("valido.pfx"), "senha123").post(URL).then().statusCode(200)
                .body("situacao", equalTo("nao_enviado")).body("pendenteDeEnvio", equalTo(true));
    }

    @Test
    void comNfseDesabilitadaFicaPendenteEDepoisEnviaAoHabilitar() throws Exception {
        String emp = empresa();
        String tk = admin(emp);
        int antes = enviosAoStub();
        enviarValido(tk).then().statusCode(200).body("situacao", equalTo("nao_enviado"))
                .body("pendenteDeEnvio", equalTo(true)).body("validade", notNullValue()).body("avisos", empty());
        assertEquals(antes, enviosAoStub(), "gateway nao chamado");
        EmpresaEntity e = ler(emp);
        assertNotNull(e.getReferenciaCertificado());
        assertEquals("nao_enviado", e.getSituacaoCertificado());
        chamar("GET", EMPRESA, tk, null).then().body("certificado.situacao", equalTo("nao_enviado"))
                .body("certificado.pendenteDeEnvio", equalTo(true));
        chamar("GET", NFSE, tk, null).then().body("certificadoSituacao", equalTo("nao_enviado"));

        // habilitar cadastra o emitente e envia o certificado guardado
        Response r = habilitar(tk);
        r.then().statusCode(200).body("habilitada", equalTo(true)).body("certificadoSituacao", equalTo("valido"))
                .body("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasKey("avisos")));
        assertEquals(antes + 1, enviosAoStub());
        JsonNode env = ultimoEnvioAoStub();
        assertEquals(ultimoEmitenteDoStub().path("tokenDevolvido").asText(), env.path("authorization").asText());
        assertEquals(Base64.getEncoder().encodeToString(arquivo("valido.pfx")), env.path("certificado").asText());
        assertEquals("valido", ler(emp).getSituacaoCertificado());
        chamar("GET", EMPRESA, tk, null).then().body("certificado.situacao", equalTo("valido"))
                .body("certificado.pendenteDeEnvio", equalTo(false));

        // um novo PUT de habilitar nao reenvia (ja valido)
        habilitar(tk).then().statusCode(200);
        assertEquals(antes + 1, enviosAoStub());
    }

    @Test
    void emitenteExistenteMasNfseDesabilitadaTambemFicaPendente() {
        String emp = empresa();
        String tk = admin(emp);
        habilitar(tk).then().statusCode(200);
        chamar("PUT", NFSE, tk, Map.of("habilitada", false)).then().statusCode(200);
        int antes = enviosAoStub();
        enviarValido(tk).then().statusCode(200).body("pendenteDeEnvio", equalTo(true));
        assertEquals(antes, enviosAoStub());
        habilitar(tk).then().statusCode(200).body("certificadoSituacao", equalTo("valido"));
        assertEquals(antes + 1, enviosAoStub());
    }

    @Test
    void semCnpjNoCertificadoAceitaComAviso() {
        String emp = empresa();
        enviar(admin(emp), "sem-cnpj.pfx", "senha123").then().statusCode(200)
                .body("avisos", hasItem("nao_foi_possivel_conferir_cnpj")).body("situacao", equalTo("nao_enviado"));
        assertNotNull(ler(emp).getReferenciaCertificado());
    }

    @Test
    void empresaComCpfAceitaComAviso() {
        String emp = empresa();
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, emp).setDocumento("12345678909"));
        enviarValido(admin(emp)).then().statusCode(200).body("avisos", hasItem("nao_foi_possivel_conferir_cnpj"));
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, emp).setDocumento(docAleatorio()));
    }

    @Test
    void reenvioSubstituiOSegredoAnterior() throws Exception {
        String emp = empresa();
        String tk = admin(emp);
        enviarValido(tk).then().statusCode(200);
        String primeiro = ler(emp).getReferenciaCertificado();
        enviar(tk, "sem-cnpj.pfx", "senha123").then().statusCode(200);
        String segundo = ler(emp).getReferenciaCertificado();
        assertNotEquals(primeiro, segundo);
        assertEquals(0, segredosComId(primeiro), "segredo anterior apagado");
        assertEquals(1, segredosComId(segundo));
        assertThrows(IllegalStateException.class, () -> segredos.ler(primeiro));
        assertEquals(Base64.getEncoder().encodeToString(arquivo("sem-cnpj.pfx")), JSON.readTree(segredos.ler(segundo)).path("pfx").asText());
    }

    // ------------------------------------------------------------------------------------------ validacao local

    @Test
    void senhaComSimboloOuForaDaRegraERecusadaAntesDeAbrir() {
        String emp = empresa();
        String tk = admin(emp);
        String msg = "A senha do certificado deve ter só letras e números, sem acentos, até 60 caracteres. "
                + "A IntegraNotas não aceita outros caracteres.";
        enviar(tk, "senha-simbolo.pfx", "senha@123").then().statusCode(400)
                .body("codigo", equalTo("certificado_senha_invalida")).body("mensagem", equalTo(msg));
        for (String s : new String[] {"", "  ", "senhá123", "senha 123", "a".repeat(61), "senha-123"}) {
            enviar(tk, "valido.pfx", s).then().statusCode(400).body("codigo", equalTo("certificado_senha_invalida"));
        }
        multipart(tk, "valido.pfx", arquivo("valido.pfx"), null).post(URL).then().statusCode(400)
                .body("codigo", equalTo("certificado_senha_invalida"));
        assertNull(ler(emp).getReferenciaCertificado());
    }

    @Test
    void senhaIncorretaDevolve400SemVazarASenha() {
        String emp = empresa();
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response r;
        try {
            r = enviar(admin(emp), "valido.pfx", "errada999");
        } finally {
            descapturar(h);
        }
        r.then().statusCode(400).body("codigo", equalTo("certificado_senha_incorreta"));
        assertFalse(r.asString().contains("errada999"));
        assertFalse(String.join("\n", logs).contains("errada999"));
        assertNull(ler(emp).getReferenciaCertificado());
    }

    @Test
    void arquivoQueNaoEPkcs12() {
        String emp = empresa();
        enviar(admin(emp), "nao-pkcs12.pfx", "senha123").then().statusCode(400).body("codigo", equalTo("certificado_invalido"));
        multipart(admin(emp), "vazio.pfx", new byte[0], "senha123").post(URL).then().statusCode(400)
                .body("codigo", equalTo("certificado_invalido"));
        byte[] cortado = Arrays.copyOf(arquivo("valido.pfx"), 500);
        multipart(admin(emp), "cortado.pfx", cortado, "senha123").post(URL).then().statusCode(400)
                .body("codigo", equalTo("certificado_invalido"));
        assertNull(ler(emp).getReferenciaCertificado());
    }

    @Test
    void certificadoVencido() {
        String emp = empresa();
        enviar(admin(emp), "vencido.pfx", "senha123").then().statusCode(400).body("codigo", equalTo("certificado_vencido"));
        assertNull(ler(emp).getReferenciaCertificado());
    }

    @Test
    void certificadoQueAindaNaoComecou() {
        String emp = empresa();
        enviar(admin(emp), "futuro.pfx", "senha123").then().statusCode(400).body("codigo", equalTo("certificado_invalido"));
    }

    @Test
    void certificadoDeOutroCnpj() {
        String emp = empresa();
        enviar(admin(emp), "outro-cnpj.pfx", "senha123").then().statusCode(400)
                .body("codigo", equalTo("certificado_nao_pertence")).body("mensagem", equalTo("O certificado pertence a outro CNPJ."));
        EmpresaEntity e = ler(emp);
        assertNull(e.getReferenciaCertificado());
        assertNull(e.getValidadeCertificado());
    }

    @Test
    void falhaDeValidacaoNaoApagaOCertificadoJaGuardado() {
        String emp = empresa();
        String tk = admin(emp);
        enviarValido(tk).then().statusCode(200);
        String ref = ler(emp).getReferenciaCertificado();
        enviar(tk, "outro-cnpj.pfx", "senha123").then().statusCode(400);
        enviar(tk, "vencido.pfx", "senha123").then().statusCode(400);
        assertEquals(ref, ler(emp).getReferenciaCertificado());
        assertEquals(1, segredosComId(ref));
    }

    @Test
    void arquivoGrandeDevolve413() {
        String emp = empresa();
        byte[] grande = new byte[300 * 1024];
        new Random(1).nextBytes(grande);
        multipart(admin(emp), "grande.pfx", grande, "senha123").post(URL).then().statusCode(413)
                .body("codigo", equalTo("arquivo_grande"));
        assertNull(ler(emp).getReferenciaCertificado());
    }

    @Test
    void tipoDeArquivoErradoEArquivoAusente() {
        String emp = empresa();
        String tk = admin(emp);
        for (String nome : new String[] {"certificado.txt", "certificado.pem", "certificado", "certificado.pfx.exe", ""}) {
            multipart(tk, nome.isEmpty() ? "x" : nome, arquivo("valido.pfx"), "senha123").post(URL).then().statusCode(400)
                    .body("codigo", equalTo("arquivo_tipo_invalido"));
        }
        multipart(tk, null, null, "senha123").post(URL).then().statusCode(400).body("codigo", equalTo("arquivo_obrigatorio"));
        assertNull(ler(emp).getReferenciaCertificado());
    }

    @Test
    void leitorExtraiValidadeECnpjDoTitularEOtherNameDaSan() throws Exception {
        CertificadoLeitor.Lido l = CertificadoLeitor.ler(arquivo("valido.pfx"), "senha123", java.time.Instant.now());
        assertEquals(Set.of(CNPJ_DO_CERTIFICADO), l.cnpjs());
        assertTrue(CertificadoLeitor.ler(arquivo("sem-cnpj.pfx"), "senha123", java.time.Instant.now()).cnpjs().isEmpty());
        // otherName ICP-Brasil: OID 2.16.76.1.3.3 seguido do CNPJ (DER montado a mao)
        byte[] oid = {0x06, 0x05, 0x60, 0x4C, 0x01, 0x03, 0x03};
        byte[] valor = (" \u0010\u0013\u000e" + "11222333000181").getBytes(StandardCharsets.ISO_8859_1);
        byte[] der = new byte[oid.length + valor.length + 2];
        der[0] = 0x30;
        der[1] = (byte) (oid.length + valor.length);
        System.arraycopy(oid, 0, der, 2, oid.length);
        System.arraycopy(valor, 0, der, 2 + oid.length, valor.length);
        Set<String> achados = new LinkedHashSet<>();
        CertificadoLeitor.cnpjDoOtherName(der, achados);
        assertEquals(Set.of("11222333000181"), achados);
        Set<String> nenhum = new LinkedHashSet<>();
        CertificadoLeitor.cnpjDoOtherName("sem oid 11222333000181".getBytes(StandardCharsets.ISO_8859_1), nenhum);
        assertTrue(nenhum.isEmpty());
    }

    // ------------------------------------------------------------------------------------------ gateway

    /** Envia o valido com a NFS-e habilitada e o gateway no modo dado; confere o erro e a situacao gravada. */
    private String comFalhaDoGateway(String modo, int status, String codigo, String situacao) {
        String emp = empresa();
        String tk = admin(emp);
        habilitar(tk).then().statusCode(200);
        modoGateway(modo);
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response r;
        try {
            r = enviarValido(tk);
        } finally {
            descapturar(h);
        }
        r.then().statusCode(status).body("codigo", equalTo(codigo));
        assertFalse(r.asString().contains("senha123"));
        assertFalse(String.join("\n", logs).contains("senha123"));
        EmpresaEntity e = ler(emp);
        assertNotNull(e.getReferenciaCertificado(), "o certificado segue guardado");
        assertEquals(situacao, e.getSituacaoCertificado());
        chamar("GET", NFSE, tk, null).then().body("certificadoSituacao", equalTo(situacao));
        chamar("GET", EMPRESA, tk, null).then().body("certificado.situacao", equalTo(situacao))
                .body("certificado.pendenteDeEnvio", equalTo(true));
        return emp;
    }

    @Test
    void gateway5003MarcaInvalido() {
        comFalhaDoGateway("5003", 400, "certificado_invalido_gateway", "invalido");
    }

    @Test
    void gateway5003ComoHttp400TambemMarcaInvalido() {
        comFalhaDoGateway("5003http400", 400, "certificado_invalido_gateway", "invalido");
    }

    @Test
    void mensagemDoGateway5003() {
        String emp = empresa();
        String tk = admin(emp);
        habilitar(tk).then().statusCode(200);
        modoGateway("5003");
        enviarValido(tk).then().statusCode(400).body("mensagem",
                equalTo("A IntegraNotas não conseguiu abrir o certificado (arquivo corrompido ou senha incorreta)."));
    }

    @Test
    void gateway5011MarcaVencido() {
        comFalhaDoGateway("5011", 400, "certificado_vencido_gateway", "vencido");
    }

    @Test
    void gateway5029MarcaNaoPertence() {
        comFalhaDoGateway("5029", 400, "certificado_nao_pertence_gateway", "nao_pertence");
    }

    @Test
    void tokenDoEmitenteRecusadoNaoMudaASituacao() {
        String emp = comFalhaDoGateway("6001", 502, "integranotas_token_recusado", "nao_enviado");
        assertNotNull(emp);
    }

    @Test
    void gatewayIndisponivelNaoMudaASituacaoEDevolve504() {
        comFalhaDoGateway("lento", 504, "integranotas_indisponivel", "nao_enviado");
    }

    @Test
    void gatewayComRespostaInvalida() {
        comFalhaDoGateway("json", 502, "integranotas_resposta_invalida", "nao_enviado");
        comFalhaDoGateway("500", 502, "integranotas_resposta_invalida", "nao_enviado");
    }

    @Test
    void falhaDeTransporteGuardaOCertificadoEOutroEnvioRecupera() {
        String emp = comFalhaDoGateway("6001", 502, "integranotas_token_recusado", "nao_enviado");
        String tk = admin(emp);
        // o erro avisa que o certificado ficou guardado
        modoGateway("6001");
        enviarValido(tk).then().statusCode(502).body("detalhes.certificadoGuardado", equalTo(true));
        modoGateway("ok");
        enviarValido(tk).then().statusCode(200).body("situacao", equalTo("valido")).body("pendenteDeEnvio", equalTo(false));
        assertEquals("valido", ler(emp).getSituacaoCertificado());
    }

    @Test
    void gatewayRecusouEUmNovoEnvioCorrige() {
        String emp = comFalhaDoGateway("5003", 400, "certificado_invalido_gateway", "invalido");
        modoGateway("ok");
        enviarValido(admin(emp)).then().statusCode(200).body("situacao", equalTo("valido"));
    }

    @Test
    void sucessoSemCodigoNoCorpoEAceito() {
        String emp = empresa();
        String tk = admin(emp);
        habilitar(tk).then().statusCode(200);
        modoGateway("semcodigo");
        enviarValido(tk).then().statusCode(200).body("situacao", equalTo("valido"));
    }

    @Test
    void gatewayForaDoArGuardaEMantemPendente() {
        String emp = empresa();
        String tk = admin(emp);
        habilitar(tk).then().statusCode(200);
        Map<String, Object> c = new HashMap<>();
        c.put("urlHomologacao", "http://127.0.0.1:1");
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
        enviarValido(tk).then().statusCode(504).body("codigo", equalTo("integranotas_indisponivel"));
        assertEquals("nao_enviado", ler(emp).getSituacaoCertificado());
        assertNotNull(ler(emp).getReferenciaCertificado());
    }

    // ------------------------------------------------------------------------------------------ habilitacao com falha no envio

    @Test
    void falhaNoEnvioAoHabilitarNaoDesfazAHabilitacao() {
        String emp = empresa();
        String tk = admin(emp);
        enviarValido(tk).then().statusCode(200);
        modoGateway("6001");
        Response r = habilitar(tk);
        r.then().statusCode(200).body("habilitada", equalTo(true)).body("certificadoSituacao", equalTo("nao_enviado"))
                .body("avisos", hasItem("certificado_nao_enviado")).body("statusCadastro", equalTo("cadastrado"));
        assertTrue(ler(emp).getNfseHabilitada());
        chamar("GET", NFSE, tk, null).then().body("habilitada", equalTo(true)).body("certificadoSituacao", equalTo("nao_enviado"));

        modoGateway("5029");
        habilitar(tk).then().statusCode(200).body("avisos", hasItem("certificado_recusado"))
                .body("certificadoSituacao", equalTo("nao_pertence"));

        modoGateway("ok");
        habilitar(tk).then().statusCode(200).body("certificadoSituacao", equalTo("valido"))
                .body("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasKey("avisos")));
    }

    @Test
    void habilitarSemCertificadoNaoTocaNoGatewayDeCertificado() {
        String emp = empresa();
        int antes = enviosAoStub();
        habilitar(admin(emp)).then().statusCode(200).body("certificadoSituacao", equalTo("nao_enviado"))
                .body("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasKey("avisos")));
        assertEquals(antes, enviosAoStub());
    }

    // ------------------------------------------------------------------------------------------ limite, papeis, isolamento

    @Test
    void limiteDeDezPorMinutoPorUsuario() {
        String emp = empresa();
        String tk = admin(emp);
        for (int i = 0; i < 10; i++) {
            enviar(tk, "nao-pkcs12.pfx", "senha123").then().statusCode(400);
        }
        Response r = enviar(tk, "nao-pkcs12.pfx", "senha123");
        r.then().statusCode(429).body("codigo", equalTo("muitas_tentativas"));
        assertNotNull(r.getHeader("Retry-After"));
        // outro usuario (de outra empresa) nao e afetado
        String outra = dados.empresaAtiva();
        enviar(admin(outra), "nao-pkcs12.pfx", "senha123").then().statusCode(400);
    }

    @Test
    void papeisEMetodos() {
        String emp = empresa();
        String adm = admin(emp);
        String op = token(dados.usuarioComPapel(emp, "operador").email());
        String con = token(dados.usuarioComPapel(emp, "consulta").email());
        int antes = enviosAoStub();
        assertEquals(403, enviarValido(op).statusCode());
        assertEquals(403, enviarValido(con).statusCode());
        assertEquals(401, enviarValido(null).statusCode());
        assertEquals(403, enviarValido(plataforma()).statusCode());
        assertNull(ler(emp).getReferenciaCertificado());
        assertEquals(antes, enviosAoStub());
        for (String m : new String[] {"GET", "PUT", "PATCH", "DELETE"}) {
            assertEquals(403, status(m, URL, adm, Map.of()), m + " negado");
        }
        enviarValido(adm).then().statusCode(200);
    }

    @Test
    void empresasSaoIsoladas() {
        String a = empresa();
        String b = dados.empresaAtiva();
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, b).setDocumento(docAleatorio()));
        enviarValido(admin(a)).then().statusCode(200);
        assertNotNull(ler(a).getReferenciaCertificado());
        assertNull(ler(b).getReferenciaCertificado());
        chamar("GET", EMPRESA, admin(b), null).then().body("certificado.situacao", equalTo("nao_enviado"))
                .body("certificado.validade", nullValue()).body("certificado.pendenteDeEnvio", equalTo(false));
        // empresa B nao consegue usar o certificado de A (CNPJ diferente) e o segredo de A fica intacto
        enviarValido(admin(b)).then().statusCode(400).body("codigo", equalTo("certificado_nao_pertence"));
        assertNull(ler(b).getReferenciaCertificado());
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
