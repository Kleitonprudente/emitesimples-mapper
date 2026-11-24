package br.com.emitesimples.nfseconfig;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

/**
 * Habilitar a NFS-e quando o CNPJ JA existe no IntegraNotas (criado fora do sistema) e sincronizacao do emitente. Tudo contra o
 * stub local (IntegraNotasStubResource); nenhuma chamada externa.
 */
@QuarkusTest
class NfseEmitenteExistenteTest extends BaseApiTest {

    private static final String URL = "/api/configuracao/nfse";
    private static final String CFG = "/api/admin/configuracoes/integranotas";
    private static final String HOM = "https://hom-api.integranotas.com.br";
    private static final String TOKEN_SOFTHOUSE = "soft-token-existente-456";
    private static final String BASE_WEBHOOK = "https://api.exemplo.com.br";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String[] TOKENS_ALHEIOS = {"token-de-outro-emitente-1", "token-de-outro-emitente-2"};

    @ConfigProperty(name = IntegraNotasStubResource.PROP_URL)
    String urlStub;

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

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
        c.put("urlPublicaWebhook", urlPublica == null ? "" : urlPublica);
        c.put("tokenHomologacao", tokenSofthouse == null ? "" : tokenSofthouse);
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
    }

    @BeforeEach
    void preparar() {
        configurar(null, TOKEN_SOFTHOUSE);
        stubPost("/_modo_lista", "ok");
        stubPost("/_webhook_falha", "nao");
    }

    @AfterEach
    void restaurar() {
        Map<String, Object> c = new HashMap<>();
        c.put("urlHomologacao", HOM);
        c.put("urlPublicaWebhook", "");
        c.put("tokenHomologacao", "");
        chamar("PUT", CFG, plataforma(), c).then().statusCode(204);
        stubPost("/_modo_lista", "ok");
        stubPost("/_webhook_falha", "nao");
    }

    // ------------------------------------------------------------------------------------------ apoio

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
            e.setRegimeTributario(1);
        });
        return id;
    }

    private String empresa() {
        return empresa("Fantasia Existente");
    }

    private void renomear(String empresaId, String fantasia) {
        QuarkusTransaction.requiringNew().run(() -> em.find(EmpresaEntity.class, empresaId).setNomeFantasia(fantasia));
    }

    private String admin(String empresaId) {
        return token(dados.usuarioComPapel(empresaId, "ADMINISTRADOR").email());
    }

    private static Map<String, Object> habilitar() {
        Map<String, Object> m = new HashMap<>();
        m.put("habilitada", true);
        m.put("serie", "1");
        m.put("ultimoNumero", 0);
        return m;
    }

    private Response put(String tk, Object corpo) {
        return chamar("PUT", URL, tk, corpo);
    }

    private EmpresaEntity ler(String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, empresaId);
            em.detach(e);
            return e;
        });
    }

    private void stubPost(String caminho, String corpo) {
        given().body(corpo).post(urlStub + caminho).then().statusCode(200);
    }

    private int contagem(String caminho) {
        return Integer.parseInt(given().get(urlStub + caminho).asString().trim());
    }

    private void semear(String doc, String token, boolean oculto) {
        given().post(urlStub + "/_semear?doc=" + doc + "&token=" + token + (oculto ? "&oculto=1" : "")).then().statusCode(200);
    }

    private JsonNode ultimo(String caminho) throws Exception {
        return JSON.readTree(given().get(urlStub + caminho).asString());
    }

    private String textoDaLinha(String empresaId) {
        return (String) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select cast(e as text) from empresa e where e.id = ?1").setParameter(1, empresaId).getSingleResult());
    }

    private long segredosComId(String id) {
        return ((Number) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select count(*) from segredo where id = ?1").setParameter(1, id).getSingleResult())).longValue();
    }

    private void semSegredosVazados(String empresaId, String... tokens) {
        String linha = textoDaLinha(empresaId);
        String ref = ler(empresaId).getReferenciaTokenEmpresa();
        byte[] cifrado = (byte[]) QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "select valor_cifrado from segredo where id = ?1").setParameter(1, ref).getSingleResult());
        String c = new String(cifrado, StandardCharsets.ISO_8859_1);
        for (String t : tokens) {
            assertFalse(linha.contains(t), "token na linha da empresa");
            assertFalse(c.contains(t), "token em claro no cofre");
        }
    }

    // ------------------------------------------------------------------------------------------ CNPJ ja existe

    private void recuperaPeloMarcador(String marcador) throws Exception {
        String emp = empresa("Fant " + marcador);
        String doc = ler(emp).getDocumento();
        String tokenExistente = "tok-existente-" + System.nanoTime();
        semear(doc, tokenExistente, false);
        String tk = admin(emp);
        int posts = contagem("/_contagem");
        int puts = contagem("/_contagem_put");
        int listas = contagem("/_contagem_lista");
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        Response r;
        try {
            r = put(tk, habilitar());
        } finally {
            descapturar(h);
        }
        r.then().statusCode(200).body("habilitada", equalTo(true)).body("statusCadastro", equalTo("CADASTRADO"))
                .body("emitenteTokenConfigurado", equalTo(true)).body("dataCadastroGateway", notNullValue());
        assertEquals(posts + 1, contagem("/_contagem"), "tentou criar uma vez");
        assertEquals(listas + 1, contagem("/_contagem_lista"));
        assertEquals(puts + 1, contagem("/_contagem_put"), "sincronizou uma vez");

        EmpresaEntity e = ler(emp);
        assertTrue(e.getNfseHabilitada());
        assertEquals(tokenExistente, segredos.ler(e.getReferenciaTokenEmpresa()));
        assertEquals(TOKEN_SOFTHOUSE, ultimo("/_ultima_lista").path("authorization").asText());
        JsonNode sync = ultimo("/_ultimo_put");
        assertEquals(tokenExistente, sync.path("authorization").asText());
        JsonNode corpo = sync.path("corpo");
        assertFalse(corpo.has("cnpj") || corpo.has("cpf") || corpo.has("documentos") || corpo.has("webhook"));
        assertEquals("Razao Social Teste Ltda", corpo.path("razao").asText());
        assertEquals("11988887777", corpo.path("telefone").asText());

        String todos = r.asString() + String.join("\n", logs) + get(tk).asString();
        for (String t : new String[] {tokenExistente, TOKENS_ALHEIOS[0], TOKENS_ALHEIOS[1], TOKEN_SOFTHOUSE}) {
            assertFalse(todos.contains(t), "segredo vazou em resposta/log: " + t);
        }
        semSegredosVazados(emp, tokenExistente, TOKENS_ALHEIOS[0], TOKENS_ALHEIOS[1]);
    }

    private Response get(String tk) {
        return chamar("GET", URL, tk, null);
    }

    @Test
    void cnpjJaExisteComMensagemDoGatewayRecuperaPelaLista() throws Exception {
        recuperaPeloMarcador("#x"); // mensagem "Esse CNPJ [..] já existe cadastrado em nosso sistema" (codigo nao documentado)
    }

    @Test
    void cnpjJaExistePeloCodigo5008() throws Exception {
        recuperaPeloMarcador(IntegraNotasStubResource.M_JA5008);
    }

    @Test
    void cnpjJaExisteDetectadoPeloTextoSemAcento() throws Exception {
        recuperaPeloMarcador(IntegraNotasStubResource.M_JA_SEM_ACENTO);
    }

    @Test
    void emitenteForaDaListaDa409SemTokenEMarcaErro() {
        String emp = empresa();
        semear(ler(emp).getDocumento(), "tok-oculto-" + System.nanoTime(), true);
        int puts = contagem("/_contagem_put");
        put(admin(emp), habilitar()).then().statusCode(409).body("codigo", equalTo("emitente_existente_sem_token"))
                .body("mensagem", equalTo("Este CNPJ já está cadastrado na IntegraNotas, mas o token do emitente não pôde ser obtido. "
                        + "Confira o token da softhouse ou fale com o suporte."));
        EmpresaEntity e = ler(emp);
        assertEquals("ERRO", e.getStatusCadastro());
        assertFalse(e.getNfseHabilitada());
        assertEquals(null, e.getReferenciaTokenEmpresa());
        assertEquals(puts, contagem("/_contagem_put"));
    }

    @Test
    void emitenteListadoSemTokenDa409() {
        String emp = empresa();
        semear(ler(emp).getDocumento(), "tok-" + System.nanoTime(), false);
        stubPost("/_modo_lista", "semtoken");
        put(admin(emp), habilitar()).then().statusCode(409).body("codigo", equalTo("emitente_existente_sem_token"));
        assertEquals("ERRO", ler(emp).getStatusCadastro());
    }

    @Test
    void listagemQueFalhaDa409() {
        for (String modo : new String[] {"500", "erro", "json", "lento"}) {
            String emp = empresa();
            semear(ler(emp).getDocumento(), "tok-" + System.nanoTime(), false);
            stubPost("/_modo_lista", modo);
            put(admin(emp), habilitar()).then().statusCode(409).body("codigo", equalTo("emitente_existente_sem_token"));
            EmpresaEntity e = ler(emp);
            assertEquals("ERRO", e.getStatusCadastro(), modo);
            assertFalse(e.getNfseHabilitada());
        }
    }

    @Test
    void semTokenDaSofthouseDa409NaoConfigurada() {
        configurar(null, null);
        String emp = empresa();
        semear(ler(emp).getDocumento(), "tok-" + System.nanoTime(), false);
        int listas = contagem("/_contagem_lista");
        put(admin(emp), habilitar()).then().statusCode(409).body("codigo", equalTo("integranotas_nao_configurada"));
        assertEquals(listas, contagem("/_contagem_lista"));
        assertFalse(ler(emp).getNfseHabilitada());
    }

    // ------------------------------------------------------------------------------------------ token do cofre recusado

    @Test
    void tokenDoCofreRecusadoRecuperaPelaLista() throws Exception {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar()).then().statusCode(200);
        EmpresaEntity antes = ler(emp);
        String refAntiga = antes.getReferenciaTokenEmpresa();
        String tokenVelho = segredos.ler(refAntiga);
        String doc = antes.getDocumento();
        // o gateway trocou o token: o antigo deixa de valer, o novo aparece na lista
        String tokenNovo = "tok-novo-" + System.nanoTime();
        given().post(urlStub + "/_revogar?token=" + tokenVelho).then().statusCode(200);
        semear(doc, tokenNovo, false);
        put(tk, Map.of("habilitada", false)).then().statusCode(200);

        int posts = contagem("/_contagem");
        int listas = contagem("/_contagem_lista");
        put(tk, habilitar()).then().statusCode(200).body("habilitada", equalTo(true))
                .body("statusCadastro", equalTo("CADASTRADO"));
        assertEquals(posts, contagem("/_contagem"), "nao tenta criar de novo");
        assertEquals(listas + 1, contagem("/_contagem_lista"));
        EmpresaEntity e = ler(emp);
        assertNotEquals(refAntiga, e.getReferenciaTokenEmpresa());
        assertEquals(tokenNovo, segredos.ler(e.getReferenciaTokenEmpresa()));
        assertEquals(0, segredosComId(refAntiga), "segredo substituido e removido do cofre");
        assertEquals(tokenNovo, ultimo("/_ultimo_put").path("authorization").asText());
    }

    @Test
    void tokenDoCofreRecusadoSemSofthouseNaoRecupera() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar()).then().statusCode(200);
        String tokenVelho = segredos.ler(ler(emp).getReferenciaTokenEmpresa());
        given().post(urlStub + "/_revogar?token=" + tokenVelho).then().statusCode(200);
        put(tk, Map.of("habilitada", false)).then().statusCode(200);
        configurar(null, null);
        int listas = contagem("/_contagem_lista");
        put(tk, habilitar()).then().statusCode(502).body("codigo", equalTo("integranotas_token_recusado"));
        assertEquals(listas, contagem("/_contagem_lista"));
        assertFalse(ler(emp).getNfseHabilitada());
    }

    // ------------------------------------------------------------------------------------------ sincronizacao

    @Test
    void validacaoRecusadaNaSincronizacaoBloqueiaComOsErros() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar()).then().statusCode(200);
        put(tk, Map.of("habilitada", false)).then().statusCode(200);
        renomear(emp, "Fant " + IntegraNotasStubResource.M_PUT5001);
        put(tk, habilitar()).then().statusCode(502).body("codigo", equalTo("integranotas_recusou"))
                .body("detalhes.erros", contains("cep: CEP inválido")).body("detalhes.codigoGateway", equalTo(5001));
        assertFalse(ler(emp).getNfseHabilitada());
        assertEquals("CADASTRADO", ler(emp).getStatusCadastro(), "o token continua valido");
        renomear(emp, "Fant corrigida");
        put(tk, habilitar()).then().statusCode(200).body("habilitada", equalTo(true));
    }

    @Test
    void validacaoRecusadaDepoisDeRecuperarBloqueiaMasGuardaOToken() throws Exception {
        String emp = empresa("Fant " + IntegraNotasStubResource.M_PUT5001);
        String tokenExistente = "tok-existente-" + System.nanoTime();
        semear(ler(emp).getDocumento(), tokenExistente, false);
        put(admin(emp), habilitar()).then().statusCode(502).body("codigo", equalTo("integranotas_recusou"))
                .body("detalhes.erros", hasItem("cep: CEP inválido"));
        EmpresaEntity e = ler(emp);
        assertFalse(e.getNfseHabilitada());
        assertEquals("CADASTRADO", e.getStatusCadastro());
        assertEquals(tokenExistente, segredos.ler(e.getReferenciaTokenEmpresa()));
    }

    @Test
    void webhookQueFalhaNaoBloqueiaEViraAviso() {
        configurar(BASE_WEBHOOK, TOKEN_SOFTHOUSE);
        String emp = empresa();
        String tk = admin(emp);
        int webhooks = contagem("/_contagem_webhook");
        put(tk, habilitar()).then().statusCode(200);
        assertEquals(webhooks, contagem("/_contagem_webhook"), "o cadastro novo ja leva o webhook no POST");
        put(tk, Map.of("habilitada", false)).then().statusCode(200);

        stubPost("/_webhook_falha", "sim");
        put(tk, habilitar()).then().statusCode(200).body("habilitada", equalTo(true))
                .body("avisos", contains("webhook_nao_atualizado"));
        assertEquals(webhooks + 1, contagem("/_contagem_webhook"));
        // GET nao traz o aviso (so a resposta da operacao)
        get(tk).then().statusCode(200).body("avisos", org.hamcrest.Matchers.nullValue());
    }

    @Test
    void webhookEnviadoAposSincronizar() throws Exception {
        configurar(BASE_WEBHOOK, TOKEN_SOFTHOUSE);
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar()).then().statusCode(200);
        put(tk, Map.of("habilitada", false)).then().statusCode(200);
        int webhooks = contagem("/_contagem_webhook");
        Response r = put(tk, habilitar());
        r.then().statusCode(200);
        assertFalse(r.asString().contains("avisos"));
        assertEquals(webhooks + 1, contagem("/_contagem_webhook"));
        JsonNode w = ultimo("/_ultimo_webhook");
        String token = segredos.ler(ler(emp).getReferenciaTokenEmpresa());
        assertEquals(token, w.path("authorization").asText());
        assertTrue(w.path("corpo").path("webhook").asText().startsWith(BASE_WEBHOOK + "/api/webhooks/integranotas/" + emp + "/"));
    }

    @Test
    void reabilitarComTokenValidoSincronizaUmaVezSemCriarNemListar() {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar()).then().statusCode(200);
        int posts = contagem("/_contagem");
        int puts = contagem("/_contagem_put");
        int listas = contagem("/_contagem_lista");
        // gravar serie/numero com a NFS-e ja habilitada nao chama o gateway
        Map<String, Object> m = habilitar();
        m.put("ultimoNumero", 5);
        put(tk, m).then().statusCode(200);
        assertEquals(puts, contagem("/_contagem_put"));
        // desabilitar e reabilitar: uma sincronizacao, sem criar nem listar
        put(tk, Map.of("habilitada", false)).then().statusCode(200);
        put(tk, m).then().statusCode(200).body("habilitada", equalTo(true));
        assertEquals(puts + 1, contagem("/_contagem_put"));
        assertEquals(posts, contagem("/_contagem"));
        assertEquals(listas, contagem("/_contagem_lista"));
    }

    @Test
    void concorrenciaRecuperaUmaSoVez() throws Exception {
        String emp = empresa("Fant " + IntegraNotasStubResource.M_DEVAGAR);
        String doc = ler(emp).getDocumento();
        semear(doc, "tok-existente-" + System.nanoTime(), false);
        String tk = admin(emp);
        int posts = contagem("/_contagem");
        int listas = contagem("/_contagem_lista");
        int puts = contagem("/_contagem_put");
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Callable<Integer>> tarefas = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                tarefas.add(() -> put(tk, habilitar()).statusCode());
            }
            for (Future<Integer> f : pool.invokeAll(tarefas)) {
                assertEquals(200, f.get());
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(posts + 1, contagem("/_contagem"));
        assertEquals(listas + 1, contagem("/_contagem_lista"));
        assertEquals(puts + 1, contagem("/_contagem_put"));
    }

    // ------------------------------------------------------------------------------------------ dados da empresa

    private Map<String, Object> dadosDaEmpresa(String tk, String fantasia) {
        Map<String, Object> m = new HashMap<>(chamar("GET", "/api/configuracao/empresa", tk, null).jsonPath().getMap(""));
        m.remove("certificado");
        m.remove("documentoEditavel");
        m.remove("avisos");
        m.put("municipio", Map.of("id", ((Map<?, ?>) m.get("municipio")).get("id")));
        m.put("nomeFantasia", fantasia);
        return m;
    }

    @Test
    void salvarDadosDaEmpresaSincronizaOEmitenteEAvisaSeFalhar() throws Exception {
        String emp = empresa();
        String tk = admin(emp);
        put(tk, habilitar()).then().statusCode(200);
        String token = segredos.ler(ler(emp).getReferenciaTokenEmpresa());
        int puts = contagem("/_contagem_put");

        Response ok = chamar("PUT", "/api/configuracao/empresa", tk, dadosDaEmpresa(tk, "Fantasia Nova"));
        ok.then().statusCode(200).body("nomeFantasia", equalTo("Fantasia Nova"));
        assertFalse(ok.asString().contains("avisos"));
        assertEquals(puts + 1, contagem("/_contagem_put"));
        assertEquals(token, ultimo("/_ultimo_put").path("authorization").asText());
        assertEquals("Fantasia Nova", ultimo("/_ultimo_put").path("corpo").path("nome").asText());

        given().post(urlStub + "/_revogar?token=" + token).then().statusCode(200);
        chamar("PUT", "/api/configuracao/empresa", tk, dadosDaEmpresa(tk, "Fantasia Falha")).then().statusCode(200)
                .body("nomeFantasia", equalTo("Fantasia Falha")).body("avisos", contains("emitente_nao_atualizado"));
        assertEquals("Fantasia Falha", ler(emp).getNomeFantasia(), "o salvamento nunca falha por causa do gateway");
    }

    @Test
    void salvarDadosSemEmitenteCadastradoNaoChamaOGateway() {
        String emp = empresa();
        String tk = admin(emp);
        int puts = contagem("/_contagem_put");
        Response r = chamar("PUT", "/api/configuracao/empresa", tk, dadosDaEmpresa(tk, "Outra Fantasia"));
        r.then().statusCode(200);
        assertFalse(r.asString().contains("avisos"));
        assertEquals(puts, contagem("/_contagem_put"));
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
