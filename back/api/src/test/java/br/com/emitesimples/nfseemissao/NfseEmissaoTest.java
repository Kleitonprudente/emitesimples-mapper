package br.com.emitesimples.nfseemissao;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.apoio.IntegraNotasStubResource;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.gateway.WebhookToken;
import br.com.emitesimples.integranotas.entity.EventoWebhookEntity;
import br.com.emitesimples.nfse.entity.CancelamentoEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import br.com.emitesimples.plataforma.SegredoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
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

/** /api/nfse/notas: emissao, acompanhamento, listagem e acesso. O IntegraNotas e SEMPRE o stub local: nenhuma chamada externa. */
@QuarkusTest
class NfseEmissaoTest extends BaseApiTest {

    private static final String URL = "/api/nfse/notas";
    private static final String CFG = "/api/admin/configuracoes/integranotas";
    private static final String HOM = "https://hom-api.integranotas.com.br";
    private static final ObjectMapper JSON = new ObjectMapper();

    @ConfigProperty(name = IntegraNotasStubResource.PROP_URL)
    String urlStub;

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @Inject
    NfseAcompanhamentoService acompanhamento;

    @Inject
    WebhookToken webhookToken;

    @Inject
    ContextoTenantBanco contexto;

    private String plataforma;

    record Cen(String emp, String adm, String pessoa, String servico, String tokenEmitente) {
    }

    @BeforeEach
    void preparar() {
        plataforma = token(dados.usuario(true, true).email());
        Map<String, Object> c = new HashMap<>();
        c.put("ambienteAtivo", "homologacao");
        c.put("urlHomologacao", urlStub);
        c.put("urlProducao", "https://api.integranotas.com.br");
        c.put("urlPublicaWebhook", "");
        c.put("tokenHomologacao", "soft-token-nfse");
        c.put("tokenProducao", "");
        chamar("PUT", CFG, plataforma, c).then().statusCode(204);
        stub("/_nfse/reset", "");
    }

    @AfterEach
    void restaurar() {
        Map<String, Object> c = new HashMap<>();
        c.put("urlHomologacao", HOM);
        c.put("urlPublicaWebhook", "");
        c.put("tokenHomologacao", "");
        chamar("PUT", CFG, plataforma, c).then().statusCode(204);
    }

    // ------------------------------------------------------------------------------------------ apoio

    private void stub(String caminho, String corpo) {
        given().body(corpo).post(urlStub + caminho).then().statusCode(200);
    }

    private JsonNode estadoStub() throws Exception {
        return JSON.readTree(given().get(urlStub + "/_nfse/estado").asString());
    }

    private static String dig(int n) {
        StringBuilder sb = new StringBuilder();
        String base = String.valueOf(System.nanoTime()) + UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        for (int i = 0; sb.length() < n; i++) {
            sb.append(base.charAt(i % base.length()));
        }
        return sb.toString();
    }

    private Cen cenario() {
        String emp = dados.empresaAtiva();
        String tokenEmit = "emit-" + UUID.randomUUID();
        String ref = segredos.guardar(tokenEmit);
        QuarkusTransaction.requiringNew().run(() -> {
            EmpresaEntity e = em.find(EmpresaEntity.class, emp);
            e.setNfseHabilitada(true);
            e.setSerieNfse("1");
            e.setUltimoNumeroNfse(0);
            e.setAmbiente("homologacao");
            e.setStatusCadastro("cadastrado");
            e.setReferenciaTokenEmpresa(ref);
            e.setSituacaoCertificado("valido");
            e.setValidadeCertificado(LocalDate.now().plusYears(1));
        });
        String adm = token(dados.usuarioComPapel(emp, "administrador").email());
        return new Cen(emp, adm, pessoa(emp, "Cliente " + UUID.randomUUID().toString().substring(0, 6), false), servico(emp),
                tokenEmit);
    }

    private String pessoa(String emp, String nome, boolean juridica) {
        String id = dados.pessoa(emp, nome);
        String cidade = dados.municipio();
        QuarkusTransaction.requiringNew().run(() -> {
            PessoaEntity p = em.find(PessoaEntity.class, id);
            p.setTipoPessoa(juridica ? "juridica" : "fisica");
            p.setDocumento(dig(juridica ? 14 : 11));
            p.setLogradouro("Rua do Tomador");
            p.setNumeroEndereco("45");
            p.setComplemento("Apto 2");
            p.setBairro("Centro");
            p.setCep("01310100");
            p.setTelefone("(11) 98888-7777");
            p.setEmail("tomador@teste.local");
            p.setCidade(em.getReference(MunicipioEntity.class, cidade));
        });
        return id;
    }

    private String servico(String emp) {
        String id = dados.servico(emp, "Servico " + UUID.randomUUID().toString().substring(0, 6));
        QuarkusTransaction.requiringNew().run(() -> {
            ServicoEntity s = em.find(ServicoEntity.class, id);
            s.setCodigoServico("010101");
            s.setCodigoTributacaoMunicipio("123");
            s.setDescricaoCurta("Desenvolvimento");
            s.setDiscriminacaoPadrao("Servico de desenvolvimento prestado");
            s.setAliquotaIss(new BigDecimal("2.50"));
            s.setPercentualTributosSimples(new BigDecimal("13.75")); // Simples (CRT 1): o governo exige (E0712)
        });
        return id;
    }

    private static Map<String, Object> corpo(Cen c, String valor) {
        Map<String, Object> m = new HashMap<>();
        m.put("pessoaId", c.pessoa());
        m.put("servicoId", c.servico());
        m.put("valorServicos", new BigDecimal(valor));
        return m;
    }

    private Response criar(Cen c) {
        return chamar("POST", URL, c.adm(), corpo(c, "150.50"));
    }

    private NotaEntity nota(String id) {
        return QuarkusTransaction.requiringNew().call(() -> {
            NotaEntity n = em.find(NotaEntity.class, id);
            em.detach(n);
            return n;
        });
    }

    private int ultimo(String emp) {
        return QuarkusTransaction.requiringNew().call(() -> em.find(EmpresaEntity.class, emp).getUltimoNumeroNfse());
    }

    private void empresa(String emp, java.util.function.Consumer<EmpresaEntity> f) {
        QuarkusTransaction.requiringNew().run(() -> f.accept(em.find(EmpresaEntity.class, emp)));
    }

    private long contarNotas(String emp) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(n) from NotaEntity n where n.empresa.id = :e", Long.class).setParameter("e", emp).getSingleResult());
    }

    private Response atualizar(Cen c, String id) {
        return chamar("POST", URL + "/" + id + "/atualizar", c.adm(), null);
    }

    /** Cria (stub aceita com 5023) e devolve o id. */
    private String criarProcessando(Cen c) {
        Response r = criar(c);
        r.then().statusCode(200).body("estado", equalTo("processando"));
        return r.jsonPath().getString("id");
    }

    // ------------------------------------------------------------------------------------------ criar + enviar

    @Test
    void criarEEnviarPJMontaOPayloadExato() throws Exception {
        Cen c = cenario();
        String pj = pessoa(c.emp(), "Empresa Tomadora SA", true);
        Map<String, Object> b = corpo(c, "150.50");
        b.put("pessoaId", pj);
        b.put("informacoesComplementares", "Pedido 77");
        Response r = chamar("POST", URL, c.adm(), b);
        r.then().statusCode(200).body("estado", equalTo("processando")).body("numero", equalTo("1")).body("serie", equalTo("1"))
                .body("chave", notNullValue()).body("codigoRetorno", equalTo(5023)).body("editavel", equalTo(false))
                .body("podeReenviar", equalTo(false)).body("podeAtualizar", equalTo(true))
                .body("pessoa.nome", equalTo("Empresa Tomadora SA")).body("informacoesComplementares", equalTo("Pedido 77"))
                .body("discriminacao", equalTo("Servico de desenvolvimento prestado")).body("codigoServico", equalTo("010101"));
        String id = r.jsonPath().getString("id");
        assertEquals(1, ultimo(c.emp()));
        JsonNode env = estadoStub().path("ultimoEnvio");
        assertEquals(c.tokenEmitente(), env.path("authorization").asText());
        JsonNode p = env.path("corpo");
        assertEquals("1", p.path("numero").asText());
        assertEquals("1", p.path("serie").asText());
        assertEquals(id.replace("-", ""), p.path("identificacao").asText());
        assertTrue(p.path("data_emissao").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-03:00"), p.path("data_emissao").asText());
        // o gateway real exige data e hora com fuso na competencia (ex.: 2020-05-16T12:33:20-03:00)
        assertEquals(LocalDate.now(NfsePayloadMapper.SAO_PAULO) + "T00:00:00-03:00", p.path("data_competencia").asText());
        assertTrue(p.path("data_competencia").asText().matches("^(20[0-9]{2}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]))(\s|T)(2[0-3]|[01][0-9]):?([0-5][0-9]):?([0-5][0-9])-(0[0-5]):00$"), p.path("data_competencia").asText());
        // optante do Simples Nacional (CRT 1): o governo exige o regime de apuracao (E0166, confirmado em homologacao)
        assertEquals(1, p.path("regime_apuracao").asInt(), p.toString());
        assertEquals("Pedido 77", p.path("informacoes_complementares").asText());
        JsonNode t = p.path("tomador");
        assertEquals(14, t.path("cnpj").asText().length());
        assertTrue(t.path("cpf").isMissingNode());
        assertEquals("Empresa Tomadora SA", t.path("razao_social").asText());
        assertEquals("11988887777", t.path("telefone").asText());
        assertEquals("tomador@teste.local", t.path("email").asText());
        JsonNode e = t.path("endereco");
        assertEquals("Rua do Tomador", e.path("logradouro").asText());
        assertEquals("45", e.path("numero").asText());
        assertEquals("Apto 2", e.path("complemento").asText());
        assertEquals("Centro", e.path("bairro").asText());
        assertEquals("01310100", e.path("cep").asText());
        assertEquals(7, e.path("codigo_municipio").asText().length());
        assertEquals("SP", e.path("uf").asText());
        JsonNode s = p.path("servico");
        assertEquals("010101", s.path("codigo").asText());
        assertEquals("Servico de desenvolvimento prestado", s.path("discriminacao").asText());
        assertEquals("Desenvolvimento", s.path("descricao").asText());
        assertEquals("123", s.path("codigo_tributacao_municipio").asText());
        assertEquals(0, new BigDecimal("150.50").compareTo(s.path("valor_servicos").decimalValue()));
        assertEquals(1, s.path("tributos_municipais").path("tipo_operacao").asInt());
        assertEquals(0, new BigDecimal("2.50").compareTo(s.path("tributos_municipais").path("aliquota_iss").decimalValue()));
        assertFalse(s.path("tributos_municipais").path("iss_retido").asBoolean());
        // Simples Nacional: tributos_totais com o percentual do servico (E0712 confirmado em homologacao)
        assertEquals(0, new BigDecimal("13.75").compareTo(s.path("tributos_totais").path("percentual_tributos_simples_nacional").decimalValue()), s.toString());
        // sem tributos_nacionais (CST 00), sem ibs_cbs, sem prestador
        for (String ausente : new String[] {"tributos_nacionais", "ibs_cbs"}) {
            assertTrue(s.path(ausente).isMissingNode(), ausente);
            assertTrue(p.path(ausente).isMissingNode(), ausente);
        }
        assertTrue(p.path("prestador").isMissingNode() && p.path("emitente").isMissingNode());
        // snapshot na nota
        NotaEntity n = nota(id);
        assertEquals("00", n.getCstPisCofins());
        assertEquals(Integer.valueOf(1), n.getTipoOperacaoIss());
        assertEquals("homologacao", n.getAmbiente());
    }

    @Test
    void criarParaPessoaFisicaUsaCpf() throws Exception {
        Cen c = cenario();
        criar(c).then().statusCode(200);
        JsonNode t = estadoStub().path("ultimoEnvio").path("corpo").path("tomador");
        assertEquals(11, t.path("cpf").asText().length());
        assertTrue(t.path("cnpj").isMissingNode());
    }

    @Test
    void simplesNacionalExigePercentualDeTributosNoCadastroDoServico() {
        Cen c = cenario();
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setPercentualTributosSimples(null));
        chamar("POST", URL, c.adm(), corpo(c, "10.00")).then().statusCode(409).body("codigo", equalTo("servico_sem_tributos_aproximados"));
        // nada foi consumido: sem nota e sem numero queimado
        assertEquals(0, QuarkusTransaction.requiringNew().call(() -> ((Number) em.createNativeQuery(
                "select count(*) from nota where empresa_id = :e").setParameter("e", c.emp()).getSingleResult()).intValue()));
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setPercentualTributosSimples(new BigDecimal("5")));
        criar(c).then().statusCode(200);
    }

    @Test
    void meiNaoExigeNemEnviaTributosTotais() {
        Cen c = cenario();
        empresa(c.emp(), e -> e.setRegimeTributario(4)); // MEI
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setPercentualTributosSimples(null));
        criar(c).then().statusCode(200);
    }

    @Test
    void snapshotDoServicoNaoMudaQuandoOCadastroMuda() {
        Cen c = cenario();
        String id = criarProcessando(c);
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setCodigoServico("999999"));
        assertEquals("010101", nota(id).getCodigoServico());
    }

    @Test
    void numeracaoUsaOUltimoMaisUmESerieDaEmpresa() throws Exception {
        Cen c = cenario();
        empresa(c.emp(), e -> {
            e.setUltimoNumeroNfse(41);
            e.setSerieNfse("7");
        });
        chamar("POST", URL, c.adm(), corpo(c, "10.00")).then().statusCode(200).body("numero", equalTo("42")).body("serie", equalTo("7"));
        assertEquals(42, ultimo(c.emp()));
        assertEquals("42", estadoStub().path("ultimoEnvio").path("corpo").path("numero").asText());
        assertEquals("7", estadoStub().path("ultimoEnvio").path("corpo").path("serie").asText());
    }

    @Test
    void dezCriacoesSimultaneasGeramNumerosDistintosEConsecutivos() throws Exception {
        Cen c = cenario();
        ExecutorService ex = Executors.newFixedThreadPool(10);
        try {
            List<Future<Response>> fs = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                fs.add(ex.submit((Callable<Response>) () -> criar(c)));
            }
            TreeSet<Integer> numeros = new TreeSet<>();
            for (Future<Response> f : fs) {
                Response r = f.get();
                assertEquals(200, r.statusCode(), r.asString());
                numeros.add(Integer.parseInt(r.jsonPath().getString("numero")));
            }
            assertEquals(10, numeros.size());
            assertEquals(1, numeros.first());
            assertEquals(10, numeros.last());
            assertEquals(10, ultimo(c.emp()));
        } finally {
            ex.shutdownNow();
        }
    }

    // ------------------------------------------------------------------------------------------ acompanhamento

    @Test
    void cincoMilVinteETresDepoisAtualizarAutorizaEServeOPdf() throws Exception {
        Cen c = cenario();
        String id = criarProcessando(c);
        assertNull(nota(id).getXml());
        stub("/_nfse/modo_consulta", "100");
        Response r = atualizar(c, id);
        r.then().statusCode(200).body("estado", equalTo("autorizada")).body("codigoVerificacao", equalTo("82 55 B2"))
                .body("numeroNfse", notNullValue()).body("temPdf", equalTo(true)).body("linkPdf", containsString("https://"))
                .body("dataHoraEvento", equalTo("2026-11-21T11:25:22")).body("podeAtualizar", equalTo(false))
                .body("editavel", equalTo(false));
        assertEquals(c.tokenEmitente(), estadoStub().path("authConsulta").asText());
        NotaEntity n = nota(id);
        assertTrue(new String(n.getXml(), "UTF-8").startsWith("<NFSe"));
        byte[] pdf = given().header("Authorization", "Bearer " + c.adm()).get(URL + "/" + id + "/pdf").then().statusCode(200)
                .contentType("application/pdf").header("Content-Disposition", containsString("inline")).extract().asByteArray();
        assertTrue(new String(pdf, 0, 4, "UTF-8").equals("%PDF"));
    }

    @Test
    void jobAutorizaNotaProcessandoEMarcaSemRetornoDepoisDe24h() throws Exception {
        Cen c = cenario();
        String id = criarProcessando(c);
        stub("/_nfse/modo_consulta", "100");
        for (int i = 0; i < 25 && !"autorizada".equals(nota(id).getEstado()); i++) {
            acompanhamento.executarCiclo();
        }
        assertEquals("autorizada", nota(id).getEstado());
        // outra nota: velha demais -> mensagem "Sem retorno..."
        stub("/_nfse/modo_consulta", "5023");
        String velha = criarProcessando(c);
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery("update nota set data_criacao = ?1 where id = ?2")
                .setParameter(1, LocalDateTime.now().minusHours(25)).setParameter(2, velha).executeUpdate());
        acompanhamento.executarCiclo();
        assertEquals(NfseAcompanhamentoService.MSG_SEM_RETORNO, nota(velha).getMensagemRetorno());
        assertEquals("processando", nota(velha).getEstado());
    }

    @Test
    void consultaAindaProcessandoMantemOEstado() {
        Cen c = cenario();
        String id = criarProcessando(c);
        stub("/_nfse/modo_consulta", "5023");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("processando"));
    }

    @Test
    void autorizacaoComXmlEPdfEmGzipEZip() throws Exception {
        for (String modo : new String[] {"100gzip", "100zip"}) {
            Cen c = cenario();
            String id = criarProcessando(c);
            stub("/_nfse/modo_consulta", modo);
            atualizar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"));
            NotaEntity n = nota(id);
            assertTrue(new String(n.getXml(), "UTF-8").startsWith("<NFSe"), modo);
            assertTrue(new String(n.getPdf(), "UTF-8").startsWith("%PDF"), modo);
        }
    }

    @Test
    void envioQueJaTrazCemAutorizadoViraAutorizada() {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "100");
        criar(c).then().statusCode(200).body("estado", equalTo("autorizada")).body("temPdf", equalTo(true));
    }

    @Test
    void envioComCemSemStatusFazUmaConsultaImediata() throws Exception {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "100sem");
        stub("/_nfse/modo_consulta", "100");
        criar(c).then().statusCode(200).body("estado", equalTo("autorizada"));
        assertEquals(1, estadoStub().path("consultas").asInt());
    }

    @Test
    void offlineDepoisAutorizada() {
        Cen c = cenario();
        String id = criarProcessando(c);
        stub("/_nfse/modo_consulta", "2");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("emitida_offline")).body("temPdf", equalTo(true))
                .body("podeAtualizar", equalTo(true));
        assertNull(nota(id).getXml());
        stub("/_nfse/modo_consulta", "100");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"));
        // reenviar uma offline e proibido
        chamar("POST", URL + "/" + id + "/enviar", c.adm(), null).then().statusCode(409);
    }

    @Test
    void cancelamentoVindoDaConsulta() {
        Cen c = cenario();
        String id = criarProcessando(c);
        stub("/_nfse/modo_consulta", "101");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("cancelada"));
    }

    @Test
    void consultaNaoRegrideNotaFinalNemAceitaIdentificacaoDeOutra() throws Exception {
        Cen c = cenario();
        String id = criarProcessando(c);
        stub("/_nfse/modo_consulta", "idoutro");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("processando"));
        stub("/_nfse/modo_consulta", "100");
        atualizar(c, id).then().body("estado", equalTo("autorizada"));
        int antes = estadoStub().path("consultas").asInt();
        stub("/_nfse/modo_consulta", "5028");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"));
        assertEquals(antes, estadoStub().path("consultas").asInt());
        assertEquals(false, acompanhamento.consultar(c.emp(), id));
        assertEquals("autorizada", nota(id).getEstado());
    }

    @Test
    void falhasDaConsultaNaoMudamAEstadoENaoVazamToken() {
        Cen c = cenario();
        String id = criarProcessando(c);
        stub("/_nfse/modo_consulta", "6001");
        Response r = atualizar(c, id);
        r.then().statusCode(502).body("codigo", equalTo("integranotas_token_recusado"));
        assertFalse(r.asString().contains(c.tokenEmitente()));
        stub("/_nfse/modo_consulta", "json");
        atualizar(c, id).then().statusCode(502).body("codigo", equalTo("integranotas_resposta_invalida"));
        stub("/_nfse/modo_consulta", "5006");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("processando")).body("codigoRetorno", equalTo(5006));
        stub("/_nfse/modo_consulta", "5027");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("em_conflito")).body("podeAtualizar", equalTo(true));
    }

    @Test
    void atualizarNotaSemAcompanhamentoDevolveODetalheSemChamarOGateway() throws Exception {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "json");
        String id = criar(c).jsonPath().getString("id");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("falha_envio"));
        assertEquals(0, estadoStub().path("consultas").asInt());
    }

    @Test
    void pdfIndisponivelDa404() {
        Cen c = cenario();
        String id = criarProcessando(c);
        chamar("GET", URL + "/" + id + "/pdf", c.adm(), null).then().statusCode(404).body("codigo", equalTo("pdf_indisponivel"));
    }

    // ------------------------------------------------------------------------------------------ rejeicao, conflito, indisponivel

    @Test
    void rejeicaoGuardaErrosLegiveisEReenvioUsaOMesmoNumero() throws Exception {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "5001");
        Response r = criar(c);
        r.then().statusCode(200).body("estado", equalTo("rejeitada")).body("codigoRetorno", equalTo(5001)).body("editavel", equalTo(true))
                .body("podeReenviar", equalTo(true)).body("chave", nullValue()).body("errosRetorno", hasSize(2))
                .body("errosRetorno[0].campo", equalTo("tomador.endereco.bairro"))
                .body("errosRetorno[0].erro", equalTo("obrigatorio")).body("errosRetorno[0].descricao", equalTo("Bairro é obrigatório"));
        String id = r.jsonPath().getString("id");
        assertEquals("1", r.jsonPath().getString("numero"));
        assertEquals(1, ultimo(c.emp()));
        // corrige e reenvia: mesmo numero, ultimoNumeroNfse nao muda
        stub("/_nfse/modo_envio", "5023");
        Map<String, Object> b = new HashMap<>();
        b.put("discriminacao", "Discriminacao corrigida");
        b.put("valorServicos", new BigDecimal("200.00"));
        chamar("POST", URL + "/" + id + "/enviar", c.adm(), b).then().statusCode(200).body("estado", equalTo("processando"))
                .body("numero", equalTo("1")).body("errosRetorno", nullValue()).body("discriminacao", equalTo("Discriminacao corrigida"));
        assertEquals(1, ultimo(c.emp()));
        JsonNode p = estadoStub().path("ultimoEnvio").path("corpo");
        assertEquals("1", p.path("numero").asText());
        assertEquals("Discriminacao corrigida", p.path("servico").path("discriminacao").asText());
        assertEquals(0, new BigDecimal("200.00").compareTo(p.path("servico").path("valor_servicos").decimalValue()));
        stub("/_nfse/modo_consulta", "100");
        atualizar(c, id).then().body("estado", equalTo("autorizada"));
        // autorizada nao reenvia
        chamar("POST", URL + "/" + id + "/enviar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_editavel"));
    }

    @Test
    void cincoMilVinteEOitoRejeitaSemLista() {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "5028");
        criar(c).then().statusCode(200).body("estado", equalTo("rejeitada")).body("codigoRetorno", equalTo(5028))
                .body("errosRetorno", nullValue()).body("mensagemRetorno", containsString("rejeitou"));
    }

    @Test
    void conflitoBarraReenvio() throws Exception {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "5027");
        Response r = criar(c);
        r.then().statusCode(200).body("estado", equalTo("em_conflito")).body("editavel", equalTo(false))
                .body("podeReenviar", equalTo(false));
        String id = r.jsonPath().getString("id");
        int envios = estadoStub().path("envios").asInt();
        chamar("POST", URL + "/" + id + "/enviar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_em_conflito"));
        assertEquals(envios, estadoStub().path("envios").asInt());
    }

    @Test
    void gatewayIndisponivelDeixaFalhaEnvioEReenvioFunciona() throws Exception {
        for (String modo : new String[] {"json", "500", "lento", "6001"}) {
            Cen c = cenario();
            stub("/_nfse/modo_envio", modo);
            Response r = criar(c);
            r.then().statusCode(200).body("estado", equalTo("falha_envio")).body("podeReenviar", equalTo(true))
                    .body("numero", equalTo("1")).body("chave", nullValue()).body("mensagemRetorno", notNullValue());
            if (!modo.equals("6001")) {
                assertEquals("A IntegraNotas está indisponível. Tente enviar novamente.", r.jsonPath().getString("mensagemRetorno"), modo);
            }
            assertEquals(1, ultimo(c.emp()));
            stub("/_nfse/modo_envio", "5023");
            String id = r.jsonPath().getString("id");
            chamar("POST", URL + "/" + id + "/enviar", c.adm(), null).then().statusCode(200).body("estado", equalTo("processando"))
                    .body("numero", equalTo("1"));
            assertEquals(1, ultimo(c.emp()));
        }
    }

    @Test
    void cincoMilTrintaEQuatroSemChaveVaiParaFalhaEnvioEComChaveFicaProcessando() {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "semchave");
        criar(c).then().statusCode(200).body("estado", equalTo("falha_envio"));
        stub("/_nfse/modo_envio", "5034");
        criar(c).then().statusCode(200).body("estado", equalTo("processando")).body("codigoRetorno", equalTo(5034));
        stub("/_nfse/modo_envio", "8429");
        criar(c).then().statusCode(200).body("estado", equalTo("falha_envio")).body("codigoRetorno", equalTo(8429));
    }

    @Test
    void duploCliqueNoEnviarCriaUmEnvio() throws Exception {
        Cen c = cenario();
        stub("/_nfse/modo_envio", "5001");
        String id = criar(c).jsonPath().getString("id");
        stub("/_nfse/modo_envio", "5023");
        int antes = estadoStub().path("envios").asInt();
        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            Future<Response> a = ex.submit(() -> chamar("POST", URL + "/" + id + "/enviar", c.adm(), null));
            Future<Response> b = ex.submit(() -> chamar("POST", URL + "/" + id + "/enviar", c.adm(), null));
            List<Integer> st = new ArrayList<>(List.of(a.get().statusCode(), b.get().statusCode()));
            st.sort(Integer::compare);
            assertEquals(List.of(200, 409), st);
            assertEquals(antes + 1, estadoStub().path("envios").asInt());
        } finally {
            ex.shutdownNow();
        }
        chamar("POST", URL + "/" + id + "/enviar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_em_processamento"));
    }

    // ------------------------------------------------------------------------------------------ pre-condicoes e validacao

    private void esperar409(Cen c, String codigo, long notasAntes) {
        criar(c).then().statusCode(409).body("codigo", equalTo(codigo));
        assertEquals(notasAntes, contarNotas(c.emp()));
        assertEquals(0, ultimo(c.emp()));
    }

    @Test
    void preCondicoesDaEmpresa() {
        Cen c = cenario();
        empresa(c.emp(), e -> e.setNfseHabilitada(false));
        esperar409(c, "nfse_nao_habilitada", 0);
        empresa(c.emp(), e -> e.setNfseHabilitada(true));
        empresa(c.emp(), e -> e.setReferenciaTokenEmpresa(null));
        esperar409(c, "emitente_nao_cadastrado", 0);
        String ref = segredos.guardar("x-" + UUID.randomUUID());
        empresa(c.emp(), e -> {
            e.setReferenciaTokenEmpresa(ref);
            e.setStatusCadastro("erro");
        });
        esperar409(c, "emitente_nao_cadastrado", 0);
        empresa(c.emp(), e -> {
            e.setStatusCadastro("cadastrado");
            e.setAmbiente("producao");
        });
        esperar409(c, "emitente_de_outro_ambiente", 0);
        empresa(c.emp(), e -> {
            e.setAmbiente("homologacao");
            e.setSituacaoCertificado("invalido");
        });
        esperar409(c, "certificado_nao_valido", 0);
        empresa(c.emp(), e -> {
            e.setSituacaoCertificado("valido");
            e.setValidadeCertificado(LocalDate.now().minusDays(1)); // vencido pela data
        });
        esperar409(c, "certificado_nao_valido", 0);
        empresa(c.emp(), e -> e.setValidadeCertificado(LocalDate.now().plusYears(1)));
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setAtivo(false));
        esperar409(c, "servico_inativo", 0);
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setAtivo(true));
        criar(c).then().statusCode(200); // tudo certo de novo
    }

    @Test
    void emissaoEmProducaoEstaBloqueada() {
        Cen c = cenario();
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                "update configuracao_integranotas set ambiente_ativo = 'producao'").executeUpdate());
        try {
            criar(c).then().statusCode(409).body("codigo", equalTo("emissao_producao_bloqueada"));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                    "update configuracao_integranotas set ambiente_ativo = 'homologacao'").executeUpdate());
        }
        assertEquals(0, contarNotas(c.emp()));
    }

    @Test
    void pessoaOuServicoDeOutraEmpresaOuInexistenteDa404() {
        Cen c = cenario();
        Cen outra = cenario();
        Map<String, Object> b = corpo(c, "10.00");
        b.put("pessoaId", outra.pessoa());
        chamar("POST", URL, c.adm(), b).then().statusCode(404).body("codigo", equalTo("nao_encontrado"));
        b = corpo(c, "10.00");
        b.put("servicoId", outra.servico());
        chamar("POST", URL, c.adm(), b).then().statusCode(404);
        b = corpo(c, "10.00");
        b.put("pessoaId", UUID.randomUUID().toString());
        chamar("POST", URL, c.adm(), b).then().statusCode(404);
        assertEquals(0, contarNotas(c.emp()));
        assertEquals(0, ultimo(c.emp()));
    }

    @Test
    void tomadorIncompletoDa422ComOsCampos() {
        Cen c = cenario();
        String sem = dados.pessoa(c.emp(), "Sem endereco");
        QuarkusTransaction.requiringNew().run(() -> em.find(PessoaEntity.class, sem).setDocumento(null));
        Map<String, Object> b = corpo(c, "10.00");
        b.put("pessoaId", sem);
        Response r = chamar("POST", URL, c.adm(), b);
        r.then().statusCode(422).body("codigo", equalTo("tomador_incompleto"));
        List<String> campos = r.jsonPath().getList("detalhes.campos.campo");
        assertTrue(campos.containsAll(List.of("documento", "logradouro", "numeroEndereco", "bairro", "cidade")), campos.toString());
        assertEquals(0, ultimo(c.emp()));
    }

    @Test
    void valoresInvalidosDao400() {
        Cen c = cenario();
        for (String v : new String[] {"0", "0.00", "-1", "10000000000000", "1.005"}) {
            chamar("POST", URL, c.adm(), corpo(c, v)).then().statusCode(400).body("codigo", equalTo("requisicao_invalida"))
                    .body("detalhes.campos.campo", org.hamcrest.Matchers.hasItem("valorServicos"));
        }
        chamar("POST", URL, c.adm(), corpo(c, "9999999999999.99")).then().statusCode(200);
        Map<String, Object> b = corpo(c, "10.00");
        b.put("informacoesComplementares", "x".repeat(2001));
        chamar("POST", URL, c.adm(), b).then().statusCode(400).body("detalhes.campos.campo", org.hamcrest.Matchers.hasItem("informacoesComplementares"));
        b = corpo(c, "10.00");
        b.put("discriminacao", "x".repeat(2001));
        chamar("POST", URL, c.adm(), b).then().statusCode(400).body("detalhes.campos.campo", org.hamcrest.Matchers.hasItem("discriminacao"));
        b = corpo(c, "10.00");
        b.remove("pessoaId");
        chamar("POST", URL, c.adm(), b).then().statusCode(400).body("detalhes.campos.campo", org.hamcrest.Matchers.hasItem("pessoaId"));
        // sem discriminacao informada e sem padrao no servico
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setDiscriminacaoPadrao(null));
        long antes = contarNotas(c.emp());
        chamar("POST", URL, c.adm(), corpo(c, "10.00")).then().statusCode(400).body("codigo", equalTo("discriminacao_obrigatoria"));
        assertEquals(antes, contarNotas(c.emp()));
        b = corpo(c, "10.00");
        b.put("discriminacao", "Informada");
        chamar("POST", URL, c.adm(), b).then().statusCode(200).body("discriminacao", equalTo("Informada"));
    }

    @Test
    void limiteDeTrintaEnviosPorMinutoPorUsuario() {
        Cen c = cenario();
        for (int i = 0; i < 30; i++) {
            chamar("POST", URL, c.adm(), corpo(c, "0")).then().statusCode(400);
        }
        chamar("POST", URL, c.adm(), corpo(c, "0")).then().statusCode(429);
    }

    // ------------------------------------------------------------------------------------------ leitura

    @Test
    void listagemFiltroBuscaOrdemEContagemSemXmlNemPdf() throws Exception {
        Cen c = cenario();
        String alfa = pessoa(c.emp(), "Alfa Comercio", true);
        String beta = pessoa(c.emp(), "Beta Servicos", false);
        Map<String, Object> b = corpo(c, "10.00");
        b.put("pessoaId", alfa);
        String n1 = chamar("POST", URL, c.adm(), b).jsonPath().getString("id");
        b.put("pessoaId", beta);
        stub("/_nfse/modo_envio", "5001");
        String n2 = chamar("POST", URL, c.adm(), b).jsonPath().getString("id");
        stub("/_nfse/modo_envio", "5023");
        String n3 = chamar("POST", URL, c.adm(), b).jsonPath().getString("id");
        stub("/_nfse/modo_consulta", "100");
        atualizar(c, n1).then().body("estado", equalTo("autorizada"));
        // ordenacao: mais recente primeiro
        Response todas = chamar("GET", URL + "/0/99", c.adm(), null);
        todas.then().statusCode(200).body("$", hasSize(3)).body("[0].id", equalTo(n3)).body("[1].id", equalTo(n2)).body("[2].id", equalTo(n1));
        assertFalse(todas.asString().contains("\"xml\""));
        assertFalse(todas.asString().contains("\"pdf\""));
        todas.then().body("[2].temPdf", equalTo(true)).body("[0].temPdf", equalTo(false))
                .body("[2].pessoa.nome", equalTo("Alfa Comercio")).body("[2].servico.id", equalTo(c.servico()))
                .body("[2].numeroNfse", notNullValue()).body("[1].mensagemRetorno", notNullValue());
        // paginacao
        chamar("GET", URL + "/0/1", c.adm(), null).then().body("$", hasSize(2)).body("[0].id", equalTo(n3));
        chamar("GET", URL + "/2/5", c.adm(), null).then().body("$", hasSize(1)).body("[0].id", equalTo(n1));
        chamar("GET", URL + "/5/2", c.adm(), null).then().statusCode(400);
        // filtro de estado
        chamar("GET", URL + "/0/99?estado=rejeitada", c.adm(), null).then().body("$", hasSize(1)).body("[0].id", equalTo(n2));
        chamar("GET", URL + "/count?estado=autorizada", c.adm(), null).then().statusCode(200).body("count", equalTo(1));
        chamar("GET", URL + "/count", c.adm(), null).then().body("count", equalTo(3));
        // busca: nome (sem diferenciar caixa), numero, chave
        chamar("GET", URL + "/0/99?busca=alfa", c.adm(), null).then().body("$", hasSize(1)).body("[0].id", equalTo(n1));
        chamar("GET", URL + "/count?busca=BETA", c.adm(), null).then().body("count", equalTo(2));
        chamar("GET", URL + "/0/99?busca=1&estado=autorizada", c.adm(), null).then().body("$", hasSize(1));
        String chave = chamar("GET", URL + "/" + n3, c.adm(), null).jsonPath().getString("chave");
        chamar("GET", URL + "/0/99?busca=" + chave.substring(30, 50), c.adm(), null).then().body("$", hasSize(1)).body("[0].id", equalTo(n3));
        chamar("GET", URL + "/count?busca=%25", c.adm(), null).then().body("count", equalTo(0)); // % e literal
        // detalhe
        chamar("GET", URL + "/" + n2, c.adm(), null).then().statusCode(200).body("editavel", equalTo(true))
                .body("podeReenviar", equalTo(true)).body("podeAtualizar", equalTo(false)).body("errosRetorno", hasSize(2));
        assertFalse(chamar("GET", URL + "/" + n1, c.adm(), null).asString().contains("\"xml\""));
    }

    @Test
    void errosRetornoComTextoLivreETolerante() {
        assertEquals("texto solto", NfseEmissaoService.erros("texto solto").get(0).descricao());
        assertEquals(1, NfseEmissaoService.erros("[{\"campo\":\"a\",\"erro\":\"b\",\"descricao\":\"c\"}]").size());
        assertEquals(null, NfseEmissaoService.erros(null));
    }

    @Test
    void isolamentoEntreEmpresas() {
        Cen a = cenario();
        Cen b = cenario();
        String id = criarProcessando(a);
        chamar("GET", URL + "/" + id, b.adm(), null).then().statusCode(404);
        chamar("POST", URL + "/" + id + "/atualizar", b.adm(), null).then().statusCode(404);
        chamar("POST", URL + "/" + id + "/enviar", b.adm(), null).then().statusCode(404);
        chamar("GET", URL + "/" + id + "/pdf", b.adm(), null).then().statusCode(404);
        chamar("GET", URL + "/0/99", b.adm(), null).then().body("$", hasSize(0));
        chamar("GET", URL + "/count", b.adm(), null).then().body("count", equalTo(0));
        // plataforma COM empresa na sessao tambem so enxerga a empresa da sessao
        var up = dados.usuario(true, true);
        dados.vincular(up.id(), b.emp(), "administrador", true);
        String plat = token(up.email(), b.emp());
        chamar("GET", URL + "/" + id, plat, null).then().statusCode(404);
        chamar("GET", URL + "/0/99", plat, null).then().body("$", hasSize(0));
        assertEquals("processando", nota(id).getEstado());
    }

    @Test
    void papeisEAcesso() {
        Cen c = cenario();
        String id = criarProcessando(c);
        String consulta = token(dados.usuarioComPapel(c.emp(), "consulta").email());
        String operador = token(dados.usuarioComPapel(c.emp(), "operador").email());
        chamar("GET", URL + "/0/9", consulta, null).then().statusCode(200);
        chamar("GET", URL + "/count", consulta, null).then().statusCode(200);
        chamar("GET", URL + "/" + id, consulta, null).then().statusCode(200);
        chamar("POST", URL, consulta, corpo(c, "10.00")).then().statusCode(403);
        chamar("POST", URL + "/" + id + "/enviar", consulta, null).then().statusCode(403);
        chamar("POST", URL + "/" + id + "/atualizar", consulta, null).then().statusCode(403);
        chamar("POST", URL, operador, corpo(c, "10.00")).then().statusCode(200);
        // anonimo
        chamar("GET", URL + "/0/9", null, null).then().statusCode(401);
        chamar("POST", URL, null, corpo(c, "10.00")).then().statusCode(401);
        // PUT/PATCH/DELETE negados
        for (String m : new String[] {"PUT", "PATCH", "DELETE"}) {
            chamar(m, URL + "/" + id, c.adm(), null).then().statusCode(403);
        }
        // plataforma sem empresa
        String plat = token(dados.usuario(true, true).email());
        chamar("GET", URL + "/0/9", plat, null).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        chamar("POST", URL, plat, corpo(c, "10.00")).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        // a API gerada de Nota continua somente leitura
        chamar("POST", "/api/Nota", c.adm(), Map.of()).then().statusCode(403);
    }

    // ------------------------------------------------------------------------------------------ webhook como gatilho

    private Response webhook(String emp, String corpo) {
        return given().contentType(ContentType.JSON).body(corpo)
                .post("/api/webhooks/integranotas/" + emp + "/" + webhookToken.gerar(emp));
    }

    private static String eventoForjado(String chave, String ident) {
        return "{\"origem\":\"NFSE\",\"tipo\":\"autorizacao\",\"chave\":\"" + chave + "\",\"sucesso\":true,\"codigo\":100,"
                + "\"mensagem\":\"Autorizado\",\"identificacao\":\"" + ident + "\",\"status\":\"autorizada\"}";
    }

    private void esperar(String id, String estado) throws Exception {
        for (int i = 0; i < 100 && !estado.equals(nota(id).getEstado()); i++) {
            Thread.sleep(100);
        }
        assertEquals(estado, nota(id).getEstado());
    }

    @Test
    void webhookSoEGatilhoEventoForjadoNaoAutorizaSemOGateway() throws Exception {
        Cen c = cenario();
        String id = criarProcessando(c);
        String chave = nota(id).getChave();
        stub("/_nfse/modo_consulta", "5023");
        webhook(c.emp(), eventoForjado(chave, id.replace("-", ""))).then().statusCode(200);
        for (int i = 0; i < 50 && estadoStub().path("consultas").asInt() < 1; i++) {
            Thread.sleep(100);
        }
        assertTrue(estadoStub().path("consultas").asInt() >= 1, "o gatilho deve consultar o gateway");
        Thread.sleep(300);
        assertEquals("processando", nota(id).getEstado()); // o evento disse "autorizada", o gateway nao
        EventoWebhookEntity ev = eventoDe(c.emp()).get(0);
        assertEquals("processado", ev.getEstadoProcessamento());
        // agora o gateway confirma: o proximo gatilho autoriza
        stub("/_nfse/modo_consulta", "100");
        webhook(c.emp(), eventoForjado(chave, "")).then().statusCode(200);
        esperar(id, "autorizada");
    }

    @Test
    void webhookPorIdentificacaoEEventoDeOutraEmpresaOuSemNotaFicaIgnorado() throws Exception {
        Cen a = cenario();
        Cen b = cenario();
        String id = criarProcessando(a);
        String chave = nota(id).getChave();
        int antes = estadoStub().path("consultas").asInt();
        webhook(b.emp(), eventoForjado(chave, id.replace("-", ""))).then().statusCode(200);
        webhook(b.emp(), eventoForjado("7".repeat(50), "naoexiste")).then().statusCode(200);
        Thread.sleep(500);
        assertEquals(antes, estadoStub().path("consultas").asInt());
        List<EventoWebhookEntity> evs = eventoDe(b.emp());
        assertEquals(2, evs.size());
        evs.forEach(e -> assertEquals("ignorado", e.getEstadoProcessamento()));
        assertEquals("processando", nota(id).getEstado());
        // so a identificacao (sem chave) tambem casa com a nota da propria empresa
        stub("/_nfse/modo_consulta", "100");
        webhook(a.emp(), eventoForjado("", id.replace("-", ""))).then().statusCode(200);
        esperar(id, "autorizada");
    }

    private List<EventoWebhookEntity> eventoDe(String emp) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select e from EventoWebhookEntity e where e.empresa.id = :e order by e.dataCriacao, e.id", EventoWebhookEntity.class)
                .setParameter("e", emp).getResultList());
    }

    // ------------------------------------------------------------------------------------------ seguranca

    @Test
    void tokenPayloadEArquivosNaoVaoParaOsLogsNemParaAChamadaGateway() throws Exception {
        Cen c = cenario();
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        String id;
        try {
            id = criarProcessando(c);
            stub("/_nfse/modo_consulta", "100");
            atualizar(c, id).then().statusCode(200);
            stub("/_nfse/modo_envio", "6001");
            criar(c).then().statusCode(200);
        } finally {
            LogManager.getLogManager().getLogger("").removeHandler(h);
        }
        String todos = String.join("\n", logs);
        String pdfB64 = java.util.Base64.getEncoder().encodeToString(IntegraNotasStubResource.PDF_MINIMO.getBytes("UTF-8"));
        assertFalse(todos.contains(c.tokenEmitente()));
        assertFalse(todos.contains(pdfB64));
        assertFalse(todos.contains("Servico de desenvolvimento prestado"));
        assertFalse(todos.contains("tomador@teste.local"));
        assertFalse(todos.contains("Rua do Tomador"));
        // ChamadaGateway: metadados, sem payload nem token
        List<Object[]> linhas = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select c.operacao, c.metodoHttp, c.rota, c.versaoApi, c.codigoHttp, c.codigoRetorno, c.resumoEnvio, c.chave, "
                        + "c.identificacao, c.mensagemRetorno from ChamadaGatewayEntity c where c.nota.id = :n order by c.dataChamada",
                Object[].class).setParameter("n", id).getResultList());
        assertEquals(2, linhas.size());
        assertEquals("enviar_nfse", linhas.get(0)[0]);
        assertEquals("POST", linhas.get(0)[1]);
        assertEquals("/v2/nfse", linhas.get(0)[2]);
        assertEquals("v2", linhas.get(0)[3]);
        assertEquals(200, linhas.get(0)[4]);
        assertEquals(5023, linhas.get(0)[5]);
        assertEquals(id.replace("-", ""), linhas.get(0)[8]);
        assertEquals("consultar_nfse", linhas.get(1)[0]);
        assertEquals(100, linhas.get(1)[5]);
        for (Object[] l : linhas) {
            String tudo = Arrays.toString(l);
            assertFalse(tudo.contains(c.tokenEmitente()) || tudo.contains(pdfB64) || tudo.contains("razao_social"));
        }
    }

    // ------------------------------------------------------------------------------------------ cancelamento

    private static final String MOTIVO = "Valor informado errado na emissao";

    /** Cria a nota ja autorizada (o stub devolve 100 no proprio envio: nenhuma consulta, nada de backoff do job). */
    private String autorizada(Cen c) {
        stub("/_nfse/modo_envio", "100");
        try {
            Response r = criar(c);
            r.then().statusCode(200).body("estado", equalTo("autorizada"));
            return r.jsonPath().getString("id");
        } finally {
            stub("/_nfse/modo_envio", "5023");
        }
    }

    private Response cancelar(String token, String id, Object codigo, Object motivo) {
        Map<String, Object> m = new HashMap<>();
        m.put("codigoMotivo", codigo);
        m.put("motivo", motivo);
        return chamar("POST", URL + "/" + id + "/cancelar", token, m);
    }

    private Response cancelar(Cen c, String id) {
        return cancelar(c.adm(), id, 1, MOTIVO);
    }

    private int chamadasCancela() throws Exception {
        return estadoStub().path("cancelamentos").asInt();
    }

    private List<CancelamentoEntity> cancelamentos(String notaId) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select c from CancelamentoEntity c where c.nota.id = :n order by c.dataCriacao", CancelamentoEntity.class)
                .setParameter("n", notaId).getResultList());
    }

    @Test
    void cancelarNotaAutorizadaEnviaAoGatewayEViraCancelada() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        chamar("GET", URL + "/" + id, c.adm(), null).then().statusCode(200).body("podeCancelar", equalTo(true))
                .body("cancelamento", nullValue());
        cancelar(c, id).then().statusCode(200).body("estado", equalTo("cancelada")).body("podeCancelar", equalTo(false))
                .body("podeAtualizar", equalTo(false)).body("cancelamento.estado", equalTo("concluido"))
                .body("cancelamento.codigoMotivo", equalTo(1)).body("cancelamento.motivo", equalTo(MOTIVO))
                .body("cancelamento.codigoRetorno", equalTo(101))
                .body("cancelamento.mensagemRetorno", containsString("cancelamento"))
                .body("cancelamento.dataHoraEvento", equalTo("2026-11-23T09:30:00"))
                .body("cancelamento.solicitadoEm", notNullValue());
        JsonNode st = estadoStub();
        assertEquals(1, st.path("cancelamentos").asInt());
        JsonNode env = st.path("ultimoCancelamento");
        assertEquals(c.tokenEmitente(), env.path("authorization").asText());
        assertEquals(nota(id).getChave(), env.path("corpo").path("chave").asText());
        assertEquals(1, env.path("corpo").path("codigo_cancelamento").asInt());
        assertEquals(MOTIVO, env.path("corpo").path("motivo_cancelamento").asText());
        assertEquals(3, env.path("corpo").size());
        NotaEntity n = nota(id);
        assertEquals("cancelada", n.getEstado());
        assertEquals(101, n.getCodigoRetorno());
        List<CancelamentoEntity> cs = cancelamentos(id);
        assertEquals(1, cs.size());
        CancelamentoEntity ce = cs.get(0);
        assertEquals("concluido", ce.getEstado());
        assertEquals(c.emp(), QuarkusTransaction.requiringNew().call(() -> em.find(CancelamentoEntity.class, ce.getId())
                .getEmpresa().getId()));
        assertEquals("homologacao", ce.getAmbiente());
        assertTrue(new String(ce.getXml(), "UTF-8").startsWith("<NFSe"));
        assertTrue(new String(ce.getPdf(), "UTF-8").startsWith("%PDF"));
        // o PDF do evento sai em /cancelamento/pdf
        byte[] pdf = given().header("Authorization", "Bearer " + c.adm()).get(URL + "/" + id + "/cancelamento/pdf").then()
                .statusCode(200).contentType("application/pdf").extract().asByteArray();
        assertEquals("%PDF", new String(pdf, 0, 4, "UTF-8"));
        // idempotente: nota ja cancelada devolve o detalhe e NAO chama o gateway de novo
        cancelar(c, id).then().statusCode(200).body("estado", equalTo("cancelada"))
                .body("cancelamento.estado", equalTo("concluido"));
        assertEquals(1, chamadasCancela());
        assertEquals(1, cancelamentos(id).size());
        // ChamadaGateway: so metadados
        List<Object[]> ls = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select c.rota, c.versaoApi, c.codigoRetorno, c.resumoEnvio, c.mensagemRetorno from ChamadaGatewayEntity c "
                        + "where c.nota.id = :n and c.operacao = 'cancelar_nfse'", Object[].class).setParameter("n", id)
                .getResultList());
        assertEquals(1, ls.size());
        assertEquals("/v1/nfse/cancela", ls.get(0)[0]);
        assertEquals("v1", ls.get(0)[1]);
        assertEquals(101, ls.get(0)[2]);
        assertFalse(Arrays.toString(ls.get(0)).contains(c.tokenEmitente()));
        assertFalse(Arrays.toString(ls.get(0)).contains(MOTIVO));
    }

    @Test
    void operadorTambemCancelaECodigosDeMotivo2e9() throws Exception {
        Cen c = cenario();
        String operador = token(dados.usuarioComPapel(c.emp(), "operador", true).email());
        String a = autorizada(c);
        cancelar(operador, a, 2, "Servico nao foi concluido a tempo").then().statusCode(200)
                .body("estado", equalTo("cancelada")).body("cancelamento.codigoMotivo", equalTo(2));
        assertEquals(2, estadoStub().path("ultimoCancelamento").path("corpo").path("codigo_cancelamento").asInt());
        String b = autorizada(c);
        cancelar(c.adm(), b, 9, "  Outro motivo qualquer aqui  ").then().statusCode(200).body("estado", equalTo("cancelada"))
                .body("cancelamento.motivo", equalTo("Outro motivo qualquer aqui"));
        assertEquals(9, estadoStub().path("ultimoCancelamento").path("corpo").path("codigo_cancelamento").asInt());
    }

    @Test
    void cincoMilEQuinzeJaCanceladaNoGatewayESucessoIdempotente() {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "5015");
        cancelar(c, id).then().statusCode(200).body("estado", equalTo("cancelada"))
                .body("cancelamento.estado", equalTo("concluido")).body("cancelamento.codigoRetorno", equalTo(5015));
        assertEquals("cancelada", nota(id).getEstado());
    }

    @Test
    void cincoMilETrezeDa409ECancelamentoRecusadoENotaContinuaAutorizada() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "5013");
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("cancelamento_recusado"))
                .body("mensagem", containsString("autorizada")).body("detalhes.codigoRetorno", equalTo(5013));
        assertEquals("autorizada", nota(id).getEstado());
        List<CancelamentoEntity> cs = cancelamentos(id);
        assertEquals(1, cs.size());
        assertEquals("recusado", cs.get(0).getEstado());
        assertEquals(5013, cs.get(0).getCodigoRetorno());
        assertNotNull(cs.get(0).getMensagemRetorno());
        chamar("GET", URL + "/" + id, c.adm(), null).then().body("estado", equalTo("autorizada"))
                .body("podeCancelar", equalTo(true)).body("cancelamento.estado", equalTo("recusado"))
                .body("cancelamento.codigoRetorno", equalTo(5013));
        // depois de recusado, da para tentar de novo
        stub("/_nfse/modo_cancela", "101");
        cancelar(c, id).then().statusCode(200).body("estado", equalTo("cancelada"));
        assertEquals(2, cancelamentos(id).size());
    }

    @Test
    void validacaoDoGatewayCincoMilEUmERecusaELegivel() {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "5001");
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("cancelamento_recusado"))
                .body("mensagem", equalTo("Erro de validação."));
        assertEquals("autorizada", nota(id).getEstado());
    }

    @Test
    void tokenRecusadoNoCancelamentoDa409SemVazarOToken() {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "6001");
        String corpo = cancelar(c, id).then().statusCode(409).body("codigo", equalTo("cancelamento_recusado")).extract().asString();
        assertFalse(corpo.contains(c.tokenEmitente()));
        assertEquals("recusado", cancelamentos(id).get(0).getEstado());
        assertEquals("autorizada", nota(id).getEstado());
    }

    @Test
    void foraDoMesVigenteRecusaSemChamarOGateway() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        QuarkusTransaction.requiringNew().run(() -> em.find(NotaEntity.class, id)
                .setDataEmissao(LocalDateTime.now().minusMonths(1).withDayOfMonth(15)));
        chamar("GET", URL + "/" + id, c.adm(), null).then().body("podeCancelar", equalTo(false));
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("cancelamento_fora_do_mes")).body("mensagem",
                equalTo("O cancelamento só é possível dentro do mês vigente da emissão da nota."));
        assertEquals(0, chamadasCancela());
        assertEquals(0, cancelamentos(id).size());
        assertEquals("autorizada", nota(id).getEstado());
    }

    @Test
    void mesVigenteUsaMesEAnoEmSaoPaulo() {
        var agora = LocalDateTime.now(NfsePayloadMapper.SAO_PAULO);
        assertTrue(NfseEmissaoService.mesVigente(agora));
        assertTrue(NfseEmissaoService.mesVigente(agora.withDayOfMonth(1).withHour(0).withMinute(0)));
        assertFalse(NfseEmissaoService.mesVigente(agora.minusMonths(1)));
        assertFalse(NfseEmissaoService.mesVigente(agora.minusYears(1))); // mesmo mes, outro ano
        assertFalse(NfseEmissaoService.mesVigente(agora.plusMonths(1)));
        assertFalse(NfseEmissaoService.mesVigente(null));
    }

    @Test
    void soSeCancelaNotaAutorizada() throws Exception {
        Cen c = cenario();
        String id = criarProcessando(c);
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("nota_nao_autorizada"));
        QuarkusTransaction.requiringNew().run(() -> em.find(NotaEntity.class, id).setEstado("rejeitada"));
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("nota_nao_autorizada"));
        assertEquals(0, chamadasCancela());
        assertEquals(0, cancelamentos(id).size());
    }

    @Test
    void motivoECodigoInvalidosDao400ComOsCampos() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String curto = "a".repeat(14);
        String longo = "a".repeat(256);
        cancelar(c.adm(), id, 1, curto).then().statusCode(400).body("codigo", equalTo("requisicao_invalida"))
                .body("detalhes.campos.campo", hasItem("motivo"));
        cancelar(c.adm(), id, 1, " ".repeat(10) + curto + " ".repeat(10)).then().statusCode(400) // conta depois do trim
                .body("detalhes.campos.campo", hasItem("motivo"));
        cancelar(c.adm(), id, 1, longo).then().statusCode(400).body("detalhes.campos.campo", hasItem("motivo"));
        cancelar(c.adm(), id, 1, null).then().statusCode(400).body("detalhes.campos.campo", hasItem("motivo"));
        cancelar(c.adm(), id, 3, MOTIVO).then().statusCode(400).body("detalhes.campos.campo", hasItem("codigoMotivo"));
        cancelar(c.adm(), id, 0, MOTIVO).then().statusCode(400).body("detalhes.campos.campo", hasItem("codigoMotivo"));
        cancelar(c.adm(), id, null, MOTIVO).then().statusCode(400).body("detalhes.campos.campo", hasItem("codigoMotivo"));
        cancelar(c.adm(), id, 7, curto).then().statusCode(400)
                .body("detalhes.campos.campo", hasItems("codigoMotivo", "motivo"));
        chamar("POST", URL + "/" + id + "/cancelar", c.adm(), null).then().statusCode(400);
        // limites exatos passam
        cancelar(c.adm(), id, 1, "a".repeat(15)).then().statusCode(200);
        String outra = autorizada(c);
        cancelar(c.adm(), outra, 9, "b".repeat(255)).then().statusCode(200);
        assertEquals(2, chamadasCancela());
    }

    @Test
    void papeisDoCancelamento() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String consulta = token(dados.usuarioComPapel(c.emp(), "consulta").email());
        cancelar(consulta, id, 1, MOTIVO).then().statusCode(403);
        cancelar(null, id, 1, MOTIVO).then().statusCode(401);
        String plat = token(dados.usuario(true, true).email());
        cancelar(plat, id, 1, MOTIVO).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        // leitura do PDF do evento: consulta pode; sem PDF -> 404 pdf_indisponivel
        chamar("GET", URL + "/" + id + "/cancelamento/pdf", consulta, null).then().statusCode(404)
                .body("codigo", equalTo("pdf_indisponivel"));
        chamar("GET", URL + "/" + id + "/cancelamento/pdf", null, null).then().statusCode(401);
        assertEquals(0, chamadasCancela());
        assertEquals("autorizada", nota(id).getEstado());
    }

    @Test
    void operadorSemDelegacaoNaoCancelaEComDelegacaoCancela() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        var opSem = dados.usuarioComPapel(c.emp(), "operador", false);
        String tokenSem = token(opSem.email());
        // detalhe coerente com a permissao: false para operador sem delegacao
        chamar("GET", URL + "/" + id, tokenSem, null).then().statusCode(200).body("podeCancelar", equalTo(false));
        cancelar(tokenSem, id, 1, MOTIVO).then().statusCode(403).body("codigo", equalTo("cancelamento_nao_delegado"))
                .body("mensagem", equalTo("Seu usuário não tem permissão para cancelar notas. Peça ao administrador."));
        assertEquals(0, chamadasCancela());
        assertEquals(0, cancelamentos(id).size());
        assertEquals("autorizada", nota(id).getEstado());
        // /me e o administrador
        chamar("GET", "/api/auth/me", tokenSem, null).then().statusCode(200).body("podeCancelarNfse", equalTo(false));
        chamar("GET", "/api/auth/me", c.adm(), null).then().statusCode(200).body("podeCancelarNfse", equalTo(true));
        chamar("GET", URL + "/" + id, c.adm(), null).then().body("podeCancelar", equalTo(true));
        // delegacao concedida no banco vale na hora, sem novo login
        QuarkusTransaction.requiringNew().run(() -> em.createQuery("update VinculoUsuarioEntity v set v.podeCancelarNfse = true "
                + "where v.usuario.id = :u").setParameter("u", opSem.id()).executeUpdate());
        chamar("GET", URL + "/" + id, tokenSem, null).then().body("podeCancelar", equalTo(true));
        cancelar(tokenSem, id, 1, MOTIVO).then().statusCode(200).body("estado", equalTo("cancelada"));
        // delegacao so vale para o operador: vinculo de consulta com a flag continua barrado
        String c2 = token(dados.usuarioComPapel(c.emp(), "consulta", true).email());
        cancelar(c2, autorizada(c), 1, MOTIVO).then().statusCode(403);
    }

    @Test
    void operadorDelegadoSoNaEmpresaDoVinculoAtivo() throws Exception {
        Cen c = cenario();
        var op = dados.usuarioComPapel(c.emp(), "operador", true);
        String tk = token(op.email());
        String id = autorizada(c);
        QuarkusTransaction.requiringNew().run(() -> em.createQuery("update VinculoUsuarioEntity v set v.ativo = false "
                + "where v.usuario.id = :u").setParameter("u", op.id()).executeUpdate());
        cancelar(tk, id, 1, MOTIVO).then().statusCode(403).body("codigo", equalTo("cancelamento_nao_delegado"));
    }

    @Test
    void notaDeOutraEmpresaOuInexistenteDa404EIsolaOCancelamento() throws Exception {
        Cen a = cenario();
        Cen b = cenario();
        String id = autorizada(a);
        cancelar(b, id).then().statusCode(404);
        cancelar(b, UUID.randomUUID().toString()).then().statusCode(404);
        chamar("GET", URL + "/" + id + "/cancelamento/pdf", b.adm(), null).then().statusCode(404)
                .body("codigo", equalTo("nao_encontrado"));
        var up = dados.usuario(true, true);
        dados.vincular(up.id(), b.emp(), "administrador", true);
        cancelar(token(up.email(), b.emp()), id, 1, MOTIVO).then().statusCode(404);
        assertEquals(0, chamadasCancela());
        assertEquals(0, cancelamentos(id).size());
        assertEquals("autorizada", nota(id).getEstado());
        // a empresa dona cancela e o detalhe da outra empresa continua 404
        cancelar(a, id).then().statusCode(200);
        chamar("GET", URL + "/" + id, b.adm(), null).then().statusCode(404);
        // RLS: sessao da empresa B nao enxerga o Cancelamento da empresa A
        long visiveis = contexto.comoEmpresa(b.emp(), () -> QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(c) from CancelamentoEntity c", Long.class).getSingleResult()));
        assertEquals(0, visiveis);
    }

    @Test
    void cancelamentoEmProducaoBloqueado() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                "update configuracao_integranotas set ambiente_ativo = 'producao'").executeUpdate());
        try {
            cancelar(c, id).then().statusCode(409).body("codigo", equalTo("emissao_producao_bloqueada"));
        } finally {
            QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                    "update configuracao_integranotas set ambiente_ativo = 'homologacao'").executeUpdate());
        }
        assertEquals(0, chamadasCancela());
        assertEquals(0, cancelamentos(id).size());
        assertEquals("autorizada", nota(id).getEstado());
    }

    @Test
    void respostaInconclusivaDeixaProcessandoEAConsultaComCentoEUmConclui() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "5023");
        cancelar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"))
                .body("cancelamento.estado", equalTo("processando")).body("podeCancelar", equalTo(false))
                .body("podeAtualizar", equalTo(true));
        assertEquals("processando", cancelamentos(id).get(0).getEstado());
        // um segundo pedido enquanto ha um em processamento
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("cancelamento_em_andamento"));
        assertEquals(1, chamadasCancela());
        // o gateway ainda diz autorizada: continua pendente
        stub("/_nfse/modo_consulta", "100");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"))
                .body("cancelamento.estado", equalTo("processando"));
        // 101: nota cancelada E registro concluido
        stub("/_nfse/modo_consulta", "101");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("cancelada"))
                .body("cancelamento.estado", equalTo("concluido")).body("cancelamento.codigoRetorno", equalTo(101))
                .body("cancelamento.dataHoraEvento", equalTo("2026-11-21T10:00:00")).body("podeAtualizar", equalTo(false));
        CancelamentoEntity ce = cancelamentos(id).get(0);
        assertTrue(new String(ce.getXml(), "UTF-8").startsWith("<NFSe"));
        assertTrue(new String(ce.getPdf(), "UTF-8").startsWith("%PDF"));
    }

    @Test
    void falhaDeRedeOuRespostaInvalidaTambemDeixaProcessando() {
        for (String modo : new String[] {"500", "json"}) {
            Cen c = cenario();
            String id = autorizada(c);
            stub("/_nfse/modo_cancela", modo);
            cancelar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"))
                    .body("cancelamento.estado", equalTo("processando"));
            assertEquals("autorizada", nota(id).getEstado());
        }
    }

    @Test
    void timeoutDoGatewayDeixaProcessando() {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "lento");
        cancelar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"))
                .body("cancelamento.estado", equalTo("processando"));
    }

    @Test
    void jobConcluiCancelamentoPendente() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "5023");
        cancelar(c, id).then().statusCode(200).body("cancelamento.estado", equalTo("processando"));
        stub("/_nfse/modo_consulta", "101");
        for (int i = 0; i < 5 && !"cancelada".equals(nota(id).getEstado()); i++) {
            acompanhamento.executarCiclo();
        }
        assertEquals("cancelada", nota(id).getEstado());
        assertEquals("concluido", cancelamentos(id).get(0).getEstado());
    }

    @Test
    void webhookTambemConcluiOCancelamentoPendente() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "5023");
        cancelar(c, id).then().statusCode(200);
        stub("/_nfse/modo_consulta", "101");
        webhook(c.emp(), eventoForjado(nota(id).getChave(), nota(id).getId().replace("-", ""))).then().statusCode(200);
        esperar(id, "cancelada");
        for (int i = 0; i < 50 && !"concluido".equals(cancelamentos(id).get(0).getEstado()); i++) {
            Thread.sleep(100);
        }
        assertEquals("concluido", cancelamentos(id).get(0).getEstado());
    }

    @Test
    void pedidoPendenteHaMaisDe24hSemConfirmacaoViraExpiradoELiberaNovoPedido() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        stub("/_nfse/modo_cancela", "5023");
        cancelar(c, id).then().statusCode(200);
        String cid = cancelamentos(id).get(0).getId();
        QuarkusTransaction.requiringNew().run(() -> em.find(CancelamentoEntity.class, cid)
                .setDataCriacao(LocalDateTime.now().minusHours(25)));
        stub("/_nfse/modo_consulta", "100");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"))
                .body("cancelamento.estado", equalTo("expirado")).body("podeCancelar", equalTo(true));
        CancelamentoEntity exp = cancelamentos(id).get(0);
        assertEquals("expirado", exp.getEstado());
        assertEquals(null, exp.getDataHoraRecusa());
        assertEquals(br.com.emitesimples.nfseemissao.NfseRetorno.MSG_CANCELAMENTO_SEM_CONFIRMACAO, exp.getMensagemRetorno());
        stub("/_nfse/modo_cancela", "101");
        cancelar(c, id).then().statusCode(200).body("estado", equalTo("cancelada"));
    }

    @Test
    void emitenteDeOutroAmbienteOuSemCadastroNaoCancela() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        empresa(c.emp(), e -> e.setAmbiente("producao"));
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("emitente_de_outro_ambiente"));
        empresa(c.emp(), e -> {
            e.setAmbiente("homologacao");
            e.setStatusCadastro("pendente");
        });
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("emitente_nao_cadastrado"));
        assertEquals(0, chamadasCancela());
        assertEquals(0, cancelamentos(id).size());
    }

    @Test
    void cancelamentoNaoVazaTokenNemMotivoNosLogs() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        List<String> logs = new ArrayList<>();
        Handler h = capturarLogs(logs);
        try {
            stub("/_nfse/modo_cancela", "500");
            cancelar(c, id).then().statusCode(200);
        } finally {
            LogManager.getLogManager().getLogger("").removeHandler(h);
        }
        String todos = String.join("\n", logs);
        assertFalse(todos.contains(c.tokenEmitente()));
        assertFalse(todos.contains(MOTIVO));
    }

    // ------------------------------------------------------------------------------------------ assistente: rascunho de nota

    private Response preparar(String token, Cen c) {
        return chamar("POST", URL + "/preparar", token, corpo(c, "150.50"));
    }

    private String rascunho(Cen c) {
        Response r = preparar(c.adm(), c);
        r.then().statusCode(200).body("estado", equalTo("rascunho"));
        return r.jsonPath().getString("id");
    }

    private int enviosStub() throws Exception {
        return estadoStub().path("envios").asInt();
    }

    @Test
    void prepararGravaRascunhoSemNumeroSemGatewayESemConsumirNumeracao() throws Exception {
        Cen c = cenario();
        // preparar nao exige certificado/emitente (so a aprovacao)
        empresa(c.emp(), e -> {
            e.setSituacaoCertificado("pendente");
            e.setStatusCadastro("pendente");
        });
        Response r = preparar(c.adm(), c);
        r.then().statusCode(200).body("estado", equalTo("rascunho")).body("numero", nullValue()).body("serie", nullValue())
                .body("dataEmissao", nullValue()).body("chave", nullValue()).body("valorServicos", equalTo(150.5f))
                .body("discriminacao", equalTo("Servico de desenvolvimento prestado")).body("descricao", equalTo("Desenvolvimento"))
                .body("codigoServico", equalTo("010101")).body("podeAprovar", equalTo(true))
                .body("podeDescartar", equalTo(true)).body("podeAtualizar", equalTo(false)).body("podeCancelar", equalTo(false))
                .body("editavel", equalTo(false)).body("podeReenviar", equalTo(false));
        String id = r.jsonPath().getString("id");
        NotaEntity n = nota(id);
        assertNull(n.getNumero());
        assertNull(n.getSerie());
        assertNull(n.getDataEmissao());
        assertNull(n.getDataHoraAprovacao());
        assertNull(n.getAprovadoPor());
        assertNull(n.getPreparadoPorCliente());
        assertNotNull(n.getPreparadoPorUsuario());
        assertEquals(0, ultimo(c.emp()));
        assertEquals(0, enviosStub());
        // preparar nao consome o limite de envio (30/min): 35 preparos seguidos passam
        for (int i = 0; i < 35; i++) {
            preparar(c.adm(), c).then().statusCode(200);
        }
        assertEquals(0, ultimo(c.emp()));
        assertEquals(0, enviosStub());
        // aparece na lista e no detalhe
        chamar("GET", URL + "/" + id, c.adm(), null).then().statusCode(200).body("estado", equalTo("rascunho"))
                .body("podeAprovar", equalTo(true));
        chamar("GET", URL + "/0/100?estado=rascunho", c.adm(), null).then().statusCode(200).body("id", hasItem(id));
    }

    @Test
    void prepararValidaOQueOPreparoPodeValidar() throws Exception {
        Cen c = cenario();
        Cen outra = cenario();
        Map<String, Object> m = corpo(c, "10.00");
        m.put("pessoaId", outra.pessoa());
        chamar("POST", URL + "/preparar", c.adm(), m).then().statusCode(404);
        m = corpo(c, "10.00");
        m.put("servicoId", outra.servico());
        chamar("POST", URL + "/preparar", c.adm(), m).then().statusCode(404);
        m = corpo(c, "0");
        chamar("POST", URL + "/preparar", c.adm(), m).then().statusCode(400).body("detalhes.campos.campo", hasItem("valorServicos"));
        m = corpo(c, "10.001");
        chamar("POST", URL + "/preparar", c.adm(), m).then().statusCode(400);
        chamar("POST", URL + "/preparar", c.adm(), null).then().statusCode(400);
        // servico inativo
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setAtivo(false));
        preparar(c.adm(), c).then().statusCode(409).body("codigo", equalTo("servico_inativo"));
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setAtivo(true));
        // discriminacao obrigatoria
        QuarkusTransaction.requiringNew().run(() -> em.find(ServicoEntity.class, c.servico()).setDiscriminacaoPadrao(null));
        preparar(c.adm(), c).then().statusCode(400).body("codigo", equalTo("discriminacao_obrigatoria"));
        m = corpo(c, "10.00");
        m.put("discriminacao", "Discriminacao informada");
        chamar("POST", URL + "/preparar", c.adm(), m).then().statusCode(200).body("discriminacao", equalTo("Discriminacao informada"));
        // tomador incompleto
        QuarkusTransaction.requiringNew().run(() -> em.find(PessoaEntity.class, c.pessoa()).setLogradouro(null));
        chamar("POST", URL + "/preparar", c.adm(), corpo(c, "10.00")).then().statusCode(422).body("codigo", equalTo("tomador_incompleto"));
        // nfse desabilitada
        Cen d = cenario();
        empresa(d.emp(), e -> e.setNfseHabilitada(false));
        preparar(d.adm(), d).then().statusCode(409).body("codigo", equalTo("nfse_nao_habilitada"));
        assertEquals(0, ultimo(c.emp()));
        assertEquals(0, enviosStub());
    }

    @Test
    void aprovarAlocaNumeroEnviaAoGatewayEASeguirOFluxo() throws Exception {
        Cen c = cenario();
        String id = rascunho(c);
        assertEquals(0, enviosStub());
        chamar("POST", URL + "/" + id + "/aprovar", c.adm(), null).then().statusCode(200).body("estado", equalTo("processando"))
                .body("numero", equalTo("1")).body("serie", equalTo("1")).body("dataEmissao", notNullValue())
                .body("podeAprovar", equalTo(false)).body("podeDescartar", equalTo(false)).body("podeAtualizar", equalTo(true));
        assertEquals(1, enviosStub());
        assertEquals(1, ultimo(c.emp()));
        NotaEntity n = nota(id);
        assertNotNull(n.getAprovadoPor());
        assertNotNull(n.getDataHoraAprovacao());
        assertNotNull(n.getPreparadoPorUsuario());
        assertEquals("homologacao", n.getAmbiente());
        assertNotNull(n.getDataCompetencia());
        // segue o fluxo normal: a consulta autoriza
        stub("/_nfse/modo_consulta", "100");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("autorizada"));
        // aprovar de novo: nao e mais rascunho
        chamar("POST", URL + "/" + id + "/aprovar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_e_rascunho"));
        assertEquals(1, enviosStub());
        assertEquals(1, ultimo(c.emp()));
    }

    @Test
    void aprovarPodeAjustarOsCamposEExigeOEmitente() throws Exception {
        Cen c = cenario();
        String id = rascunho(c);
        empresa(c.emp(), e -> e.setSituacaoCertificado("pendente"));
        chamar("POST", URL + "/" + id + "/aprovar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("certificado_nao_valido"));
        assertEquals("rascunho", nota(id).getEstado());
        assertEquals(0, ultimo(c.emp()));
        assertEquals(0, enviosStub());
        empresa(c.emp(), e -> {
            e.setSituacaoCertificado("valido");
            e.setStatusCadastro("pendente");
        });
        chamar("POST", URL + "/" + id + "/aprovar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("emitente_nao_cadastrado"));
        empresa(c.emp(), e -> e.setStatusCadastro("cadastrado"));
        Map<String, Object> ajuste = new HashMap<>();
        ajuste.put("valorServicos", new BigDecimal("99.90"));
        ajuste.put("informacoesComplementares", "ajustado na aprovacao");
        chamar("POST", URL + "/" + id + "/aprovar", c.adm(), ajuste).then().statusCode(200).body("estado", equalTo("processando"))
                .body("valorServicos", equalTo(99.9f)).body("informacoesComplementares", equalTo("ajustado na aprovacao"));
        assertEquals(1, ultimo(c.emp()));
    }

    @Test
    void aprovarOuDescartarNotaQueNaoERascunhoDaErro() throws Exception {
        Cen c = cenario();
        String id = criarProcessando(c);
        int envios = enviosStub();
        chamar("POST", URL + "/" + id + "/aprovar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_e_rascunho"));
        chamar("POST", URL + "/" + id + "/descartar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_e_rascunho"));
        assertEquals(envios, enviosStub());
        assertEquals("processando", nota(id).getEstado());
        // rascunho nao e reenviado por /enviar
        String r = rascunho(c);
        chamar("POST", URL + "/" + r + "/enviar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_editavel"));
        assertEquals(envios, enviosStub());
    }

    /** BLOQUEADO: o CHECK ck_nota_numeracao (V9) exige numero/serie/dataEmissao em todo estado que nao seja 'rascunho'; precisa de V10. */
    @Test
    void notaDescartadaNaoAprovaNemDescartaNemEnvia() throws Exception {
        Cen c = cenario();
        String r = rascunho(c);
        chamar("POST", URL + "/" + r + "/descartar", c.adm(), null).then().statusCode(200);
        chamar("POST", URL + "/" + r + "/aprovar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_e_rascunho"));
        chamar("POST", URL + "/" + r + "/descartar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_e_rascunho"));
        chamar("POST", URL + "/" + r + "/enviar", c.adm(), null).then().statusCode(409).body("codigo", equalTo("nota_nao_editavel"));
        assertEquals(0, enviosStub());
    }

    @Test
    void descartarGravaQuemQuandoENaoTocaNumeracaoNemGateway() throws Exception {
        Cen c = cenario();
        String id = rascunho(c);
        chamar("POST", URL + "/" + id + "/descartar", c.adm(), null).then().statusCode(200).body("estado", equalTo("descartada"))
                .body("podeAprovar", equalTo(false)).body("podeDescartar", equalTo(false)).body("numero", nullValue());
        NotaEntity n = nota(id);
        assertEquals("descartada", n.getEstado());
        assertNotNull(n.getDescartadoPor());
        assertNotNull(n.getDataHoraDescarte());
        assertNull(n.getAprovadoPor());
        assertEquals(0, ultimo(c.emp()));
        assertEquals(0, enviosStub());
    }

    @Test
    void papeisDoRascunhoDeNota() throws Exception {
        Cen c = cenario();
        String id = rascunho(c);
        String consulta = token(dados.usuarioComPapel(c.emp(), "consulta").email());
        String operador = token(dados.usuarioComPapel(c.emp(), "operador").email());
        preparar(consulta, c).then().statusCode(403);
        preparar(null, c).then().statusCode(401);
        chamar("POST", URL + "/" + id + "/aprovar", consulta, null).then().statusCode(403);
        chamar("POST", URL + "/" + id + "/descartar", consulta, null).then().statusCode(403);
        chamar("POST", URL + "/" + id + "/aprovar", null, null).then().statusCode(401);
        // consulta le o rascunho, mas sem podeAprovar/podeDescartar
        chamar("GET", URL + "/" + id, consulta, null).then().statusCode(200).body("estado", equalTo("rascunho"))
                .body("podeAprovar", equalTo(false)).body("podeDescartar", equalTo(false));
        // plataforma sem empresa na sessao
        String plat = token(dados.usuario(true, true).email());
        preparar(plat, c).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        chamar("POST", URL + "/" + id + "/aprovar", plat, null).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        assertEquals("rascunho", nota(id).getEstado());
        assertEquals(0, enviosStub());
        // operador prepara e aprova
        preparar(operador, c).then().statusCode(200).body("podeAprovar", equalTo(true));
        chamar("POST", URL + "/" + id + "/aprovar", operador, null).then().statusCode(200).body("estado", equalTo("processando"));
    }

    @Test
    void rascunhoDeOutraEmpresaOuInexistenteDa404() throws Exception {
        Cen a = cenario();
        Cen b = cenario();
        String id = rascunho(a);
        chamar("POST", URL + "/" + id + "/aprovar", b.adm(), null).then().statusCode(404);
        chamar("POST", URL + "/" + id + "/descartar", b.adm(), null).then().statusCode(404);
        chamar("POST", URL + "/" + UUID.randomUUID() + "/aprovar", a.adm(), null).then().statusCode(404);
        chamar("POST", URL + "/" + UUID.randomUUID() + "/descartar", a.adm(), null).then().statusCode(404);
        chamar("GET", URL + "/" + id, b.adm(), null).then().statusCode(404);
        // tomador de outra empresa
        Map<String, Object> m = corpo(a, "10.00");
        m.put("pessoaId", b.pessoa());
        chamar("POST", URL + "/preparar", a.adm(), m).then().statusCode(404);
        assertEquals("rascunho", nota(id).getEstado());
        assertEquals(0, enviosStub());
        assertEquals(0, ultimo(a.emp()));
    }

    @Test
    void consultaNaoEscreveNoRascunho() throws Exception {
        Cen c = cenario();
        String id = rascunho(c);
        NotaEntity antes = nota(id);
        chamar("GET", URL + "/" + id, c.adm(), null).then().statusCode(200);
        chamar("GET", URL + "/0/10", c.adm(), null).then().statusCode(200);
        chamar("GET", URL + "/count?estado=rascunho", c.adm(), null).then().statusCode(200);
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("rascunho"));
        NotaEntity depois = nota(id);
        assertEquals(antes.getEstado(), depois.getEstado());
        assertEquals(antes.getDataAtualizacao(), depois.getDataAtualizacao());
        assertEquals(0, enviosStub());
    }

    @Test
    void duasAprovacoesSimultaneasAlocamUmNumeroEEnviamUmaVez() throws Exception {
        Cen c = cenario();
        String id = rascunho(c);
        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            Future<Response> a = ex.submit(() -> chamar("POST", URL + "/" + id + "/aprovar", c.adm(), null));
            Future<Response> b = ex.submit(() -> chamar("POST", URL + "/" + id + "/aprovar", c.adm(), null));
            List<Integer> st = new ArrayList<>(List.of(a.get().statusCode(), b.get().statusCode()));
            st.sort(Integer::compare);
            assertEquals(List.of(200, 409), st);
        } finally {
            ex.shutdownNow();
        }
        assertEquals(1, enviosStub());
        assertEquals(1, ultimo(c.emp()));
    }

    // ------------------------------------------------------------------------------------------ assistente: rascunho de cancelamento

    private Response prepararCancel(String token, String notaId, Object codigo, Object motivo) {
        Map<String, Object> m = new HashMap<>();
        m.put("codigoMotivo", codigo);
        m.put("motivo", motivo);
        return chamar("POST", URL + "/" + notaId + "/cancelamentos/preparar", token, m);
    }

    private String rascunhoCancel(Cen c, String notaId) {
        Response r = prepararCancel(c.adm(), notaId, 1, MOTIVO);
        r.then().statusCode(200).body("estado", equalTo("autorizada")).body("cancelamento.estado", equalTo("rascunho"));
        return r.jsonPath().getString("cancelamento.id");
    }

    private Response aprovarCancel(String token, String cid) {
        return chamar("POST", "/api/nfse/cancelamentos/" + cid + "/aprovar", token, null);
    }

    private Response descartarCancel(String token, String cid) {
        return chamar("POST", "/api/nfse/cancelamentos/" + cid + "/descartar", token, null);
    }

    @Test
    void prepararCancelamentoValidaESemChamarOGateway() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        prepararCancel(c.adm(), id, 2, "  " + MOTIVO + "  ").then().statusCode(200).body("estado", equalTo("autorizada"))
                .body("cancelamento.id", notNullValue()).body("cancelamento.estado", equalTo("rascunho"))
                .body("cancelamento.codigoMotivo", equalTo(2)).body("cancelamento.motivo", equalTo(MOTIVO))
                .body("cancelamento.podeAprovar", equalTo(true)).body("cancelamento.podeDescartar", equalTo(true))
                .body("podeCancelar", equalTo(false)).body("podeAtualizar", equalTo(false));
        assertEquals(0, chamadasCancela());
        List<CancelamentoEntity> cs = cancelamentos(id);
        assertEquals(1, cs.size());
        CancelamentoEntity ce = cs.get(0);
        assertEquals("rascunho", ce.getEstado());
        assertNull(ce.getAprovadoPor());
        assertNull(ce.getDataHoraAprovacao());
        assertNull(ce.getPreparadoPorCliente());
        assertNotNull(ce.getPreparadoPorUsuario());
        assertEquals("homologacao", ce.getAmbiente());
        assertEquals("autorizada", nota(id).getEstado());
        // o GET tambem mostra o rascunho
        chamar("GET", URL + "/" + id, c.adm(), null).then().body("cancelamento.estado", equalTo("rascunho"))
                .body("cancelamento.id", equalTo(ce.getId()));
    }

    @Test
    void prepararCancelamentoRecusaForaDoMesNotaNaoAutorizadaEMotivoInvalido() throws Exception {
        Cen c = cenario();
        String proc = criarProcessando(c);
        prepararCancel(c.adm(), proc, 1, MOTIVO).then().statusCode(409).body("codigo", equalTo("nota_nao_autorizada"));
        String id = autorizada(c);
        String curto = "a".repeat(14);
        prepararCancel(c.adm(), id, 1, curto).then().statusCode(400).body("detalhes.campos.campo", hasItem("motivo"));
        prepararCancel(c.adm(), id, 1, "a".repeat(256)).then().statusCode(400).body("detalhes.campos.campo", hasItem("motivo"));
        prepararCancel(c.adm(), id, 3, MOTIVO).then().statusCode(400).body("detalhes.campos.campo", hasItem("codigoMotivo"));
        prepararCancel(c.adm(), id, null, null).then().statusCode(400).body("detalhes.campos.campo", hasItems("codigoMotivo", "motivo"));
        chamar("POST", URL + "/" + id + "/cancelamentos/preparar", c.adm(), null).then().statusCode(400);
        assertEquals(0, cancelamentos(id).size());
        prepararCancel(c.adm(), id, 9, "a".repeat(15)).then().statusCode(200);
        // fora do mes vigente
        String velha = autorizada(c);
        QuarkusTransaction.requiringNew().run(() -> em.find(NotaEntity.class, velha)
                .setDataEmissao(LocalDateTime.now().minusMonths(1).withDayOfMonth(15)));
        prepararCancel(c.adm(), velha, 1, MOTIVO).then().statusCode(409).body("codigo", equalTo("cancelamento_fora_do_mes"));
        assertEquals(0, cancelamentos(velha).size());
        // ambiente
        String outro = autorizada(c);
        empresa(c.emp(), e -> e.setAmbiente("producao"));
        prepararCancel(c.adm(), outro, 1, MOTIVO).then().statusCode(409).body("codigo", equalTo("emitente_de_outro_ambiente"));
        empresa(c.emp(), e -> e.setAmbiente("homologacao"));
        assertEquals(0, chamadasCancela());
        // nota de outra empresa / inexistente
        Cen b = cenario();
        prepararCancel(b.adm(), id, 1, MOTIVO).then().statusCode(404);
        prepararCancel(c.adm(), UUID.randomUUID().toString(), 1, MOTIVO).then().statusCode(404);
    }

    @Test
    void duplicidadeDeCancelamentoRascunhoOuProcessandoBloqueia() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        prepararCancel(c.adm(), id, 1, MOTIVO).then().statusCode(409).body("codigo", equalTo("cancelamento_em_andamento"));
        cancelar(c, id).then().statusCode(409).body("codigo", equalTo("cancelamento_em_andamento"));
        assertEquals(0, chamadasCancela());
        assertEquals(1, cancelamentos(id).size());
        // descartado libera novo preparo e o cancelamento direto
        descartarCancel(c.adm(), cid).then().statusCode(200);
        chamar("GET", URL + "/" + id, c.adm(), null).then().body("podeCancelar", equalTo(true));
        String outro = rascunhoCancel(c, id);
        assertNotEquals(cid, outro);
        // processando bloqueia o preparo
        stub("/_nfse/modo_cancela", "5023");
        aprovarCancel(c.adm(), outro).then().statusCode(200).body("cancelamento.estado", equalTo("processando"));
        prepararCancel(c.adm(), id, 1, MOTIVO).then().statusCode(409).body("codigo", equalTo("cancelamento_em_andamento"));
    }

    @Test
    void aprovarCancelamentoEnviaAoGatewayEConcluiComoOCancelamentoDireto() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        aprovarCancel(c.adm(), cid).then().statusCode(200).body("estado", equalTo("cancelada")).body("podeCancelar", equalTo(false))
                .body("cancelamento.id", equalTo(cid)).body("cancelamento.estado", equalTo("concluido"))
                .body("cancelamento.codigoRetorno", equalTo(101)).body("cancelamento.podeAprovar", equalTo(false))
                .body("cancelamento.podeDescartar", equalTo(false));
        assertEquals(1, chamadasCancela());
        JsonNode env = estadoStub().path("ultimoCancelamento");
        assertEquals(c.tokenEmitente(), env.path("authorization").asText());
        assertEquals(nota(id).getChave(), env.path("corpo").path("chave").asText());
        assertEquals(1, env.path("corpo").path("codigo_cancelamento").asInt());
        assertEquals(MOTIVO, env.path("corpo").path("motivo_cancelamento").asText());
        CancelamentoEntity ce = cancelamentos(id).get(0);
        assertEquals("concluido", ce.getEstado());
        assertNotNull(ce.getAprovadoPor());
        assertNotNull(ce.getDataHoraAprovacao());
        assertNotNull(ce.getPreparadoPorUsuario());
        // aprovar/descartar de novo: nao e mais rascunho
        aprovarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("cancelamento_nao_e_rascunho"));
        descartarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("cancelamento_nao_e_rascunho"));
        assertEquals(1, chamadasCancela());
    }

    @Test
    void aprovarCancelamentoRecusadoInconclusivoEJaCancelado() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        stub("/_nfse/modo_cancela", "5013");
        aprovarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("cancelamento_recusado"))
                .body("detalhes.codigoRetorno", equalTo(5013));
        assertEquals("recusado", cancelamentos(id).get(0).getEstado());
        assertEquals("autorizada", nota(id).getEstado());
        // inconclusivo: fica processando e a consulta conclui
        String cid2 = rascunhoCancel(c, id);
        stub("/_nfse/modo_cancela", "5023");
        aprovarCancel(c.adm(), cid2).then().statusCode(200).body("estado", equalTo("autorizada"))
                .body("cancelamento.estado", equalTo("processando"));
        stub("/_nfse/modo_consulta", "101");
        atualizar(c, id).then().statusCode(200).body("estado", equalTo("cancelada")).body("cancelamento.estado", equalTo("concluido"));
        // 5015: ja cancelada no gateway = concluido
        String id2 = autorizada(c);
        String cid3 = rascunhoCancel(c, id2);
        stub("/_nfse/modo_cancela", "5015");
        aprovarCancel(c.adm(), cid3).then().statusCode(200).body("estado", equalTo("cancelada"))
                .body("cancelamento.codigoRetorno", equalTo(5015));
    }

    @Test
    void aprovarCancelamentoRevalidaOMesVigente() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        QuarkusTransaction.requiringNew().run(() -> em.find(NotaEntity.class, id)
                .setDataEmissao(LocalDateTime.now().minusMonths(1).withDayOfMonth(15)));
        aprovarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("cancelamento_fora_do_mes"));
        assertEquals(0, chamadasCancela());
        assertEquals("rascunho", cancelamentos(id).get(0).getEstado());
        // o rascunho ainda pode ser descartado
        descartarCancel(c.adm(), cid).then().statusCode(200).body("cancelamento.estado", equalTo("descartado"));
    }

    @Test
    void aprovarCancelamentoRevalidaAmbienteEEmitente() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        empresa(c.emp(), e -> e.setAmbiente("producao"));
        aprovarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("emitente_de_outro_ambiente"));
        empresa(c.emp(), e -> {
            e.setAmbiente("homologacao");
            e.setStatusCadastro("pendente");
        });
        aprovarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("emitente_nao_cadastrado"));
        assertEquals(0, chamadasCancela());
        assertEquals("rascunho", cancelamentos(id).get(0).getEstado());
    }

    @Test
    void operadorSemDelegacaoNaoAprovaNemDescartaCancelamentoMasPrepara() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        var opSem = dados.usuarioComPapel(c.emp(), "operador", false);
        String tokenSem = token(opSem.email());
        // o preparo nao exige a delegacao
        Response r = prepararCancel(tokenSem, id, 1, MOTIVO);
        r.then().statusCode(200).body("cancelamento.estado", equalTo("rascunho")).body("cancelamento.podeAprovar", equalTo(false))
                .body("cancelamento.podeDescartar", equalTo(false));
        String cid = r.jsonPath().getString("cancelamento.id");
        aprovarCancel(tokenSem, cid).then().statusCode(403).body("codigo", equalTo("cancelamento_nao_delegado"));
        descartarCancel(tokenSem, cid).then().statusCode(403).body("codigo", equalTo("cancelamento_nao_delegado"));
        assertEquals(0, chamadasCancela());
        assertEquals("rascunho", cancelamentos(id).get(0).getEstado());
        // com a delegacao (vale na hora)
        QuarkusTransaction.requiringNew().run(() -> em.createQuery("update VinculoUsuarioEntity v set v.podeCancelarNfse = true "
                + "where v.usuario.id = :u").setParameter("u", opSem.id()).executeUpdate());
        chamar("GET", URL + "/" + id, tokenSem, null).then().body("cancelamento.podeAprovar", equalTo(true))
                .body("cancelamento.podeDescartar", equalTo(true));
        aprovarCancel(tokenSem, cid).then().statusCode(200).body("estado", equalTo("cancelada"));
        assertEquals(1, chamadasCancela());
        assertNotNull(cancelamentos(id).get(0).getAprovadoPor());
    }

    @Test
    void descartarCancelamentoGravaQuemEQuando() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        descartarCancel(c.adm(), cid).then().statusCode(200).body("estado", equalTo("autorizada"))
                .body("cancelamento.estado", equalTo("descartado")).body("cancelamento.podeAprovar", equalTo(false))
                .body("podeCancelar", equalTo(true));
        CancelamentoEntity ce = cancelamentos(id).get(0);
        assertEquals("descartado", ce.getEstado());
        assertNotNull(ce.getDescartadoPor());
        assertNotNull(ce.getDataHoraDescarte());
        assertNull(ce.getAprovadoPor());
        aprovarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("cancelamento_nao_e_rascunho"));
        descartarCancel(c.adm(), cid).then().statusCode(409).body("codigo", equalTo("cancelamento_nao_e_rascunho"));
        assertEquals(0, chamadasCancela());
    }

    @Test
    void papeisEIsolamentoDosRascunhosDeCancelamento() throws Exception {
        Cen c = cenario();
        Cen b = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        String consulta = token(dados.usuarioComPapel(c.emp(), "consulta").email());
        prepararCancel(consulta, id, 1, MOTIVO).then().statusCode(403);
        prepararCancel(null, id, 1, MOTIVO).then().statusCode(401);
        aprovarCancel(consulta, cid).then().statusCode(403);
        descartarCancel(consulta, cid).then().statusCode(403);
        aprovarCancel(null, cid).then().statusCode(401);
        chamar("GET", URL + "/" + id, consulta, null).then().statusCode(200).body("cancelamento.podeAprovar", equalTo(false));
        String plat = token(dados.usuario(true, true).email());
        aprovarCancel(plat, cid).then().statusCode(403).body("codigo", equalTo("empresa_nao_selecionada"));
        // outra empresa e inexistente: 404
        aprovarCancel(b.adm(), cid).then().statusCode(404);
        descartarCancel(b.adm(), cid).then().statusCode(404);
        aprovarCancel(c.adm(), UUID.randomUUID().toString()).then().statusCode(404);
        descartarCancel(c.adm(), UUID.randomUUID().toString()).then().statusCode(404);
        // so POST em /api/nfse/cancelamentos
        chamar("GET", "/api/nfse/cancelamentos/" + cid + "/aprovar", c.adm(), null).then().statusCode(403);
        assertEquals(0, chamadasCancela());
        assertEquals("rascunho", cancelamentos(id).get(0).getEstado());
    }

    @Test
    void consultaNaoEscreveNoRascunhoDeCancelamentoEDuasAprovacoesNaoDuplicam() throws Exception {
        Cen c = cenario();
        String id = autorizada(c);
        String cid = rascunhoCancel(c, id);
        CancelamentoEntity antes = cancelamentos(id).get(0);
        chamar("GET", URL + "/" + id, c.adm(), null).then().statusCode(200);
        atualizar(c, id).then().statusCode(200).body("cancelamento.estado", equalTo("rascunho"));
        assertEquals(antes.getDataAtualizacao(), cancelamentos(id).get(0).getDataAtualizacao());
        assertEquals(0, chamadasCancela());
        ExecutorService ex = Executors.newFixedThreadPool(2);
        try {
            Future<Response> a = ex.submit(() -> aprovarCancel(c.adm(), cid));
            Future<Response> b = ex.submit(() -> aprovarCancel(c.adm(), cid));
            List<Integer> st = new ArrayList<>(List.of(a.get().statusCode(), b.get().statusCode()));
            st.sort(Integer::compare);
            assertEquals(List.of(200, 409), st);
        } finally {
            ex.shutdownNow();
        }
        assertEquals(1, chamadasCancela());
        assertEquals(1, cancelamentos(id).size());
    }

    private static void assertNull(Object o) {
        org.junit.jupiter.api.Assertions.assertNull(o);
    }

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
}
