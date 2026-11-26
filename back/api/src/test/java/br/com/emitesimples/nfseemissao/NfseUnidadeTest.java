package br.com.emitesimples.nfseemissao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.gateway.IntegraNotasCliente.RetornoNfse;
import br.com.emitesimples.nfse.entity.NotaEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

/** Unidade (sem Quarkus): tabela de retorno do gateway, decodificacao de arquivos e mapeador de payload. */
class NfseUnidadeTest {

    private static final ObjectMapper J = new ObjectMapper();

    private static RetornoNfse ret(String json) throws Exception {
        var raiz = J.readTree(json);
        return new RetornoNfse(200, raiz.get("codigo").asInt(), raiz.has("sucesso") ? raiz.get("sucesso").asBoolean() : null,
                raiz.path("mensagem").asText(null), raiz.path("chave").asText(null), raiz);
    }

    @Test
    void regimeDeApuracaoPadraoPorCrtDaEmpresa() {
        assertEquals(1, NfsePayloadMapper.regimeApuracaoPadrao(1)); // Simples Nacional: federais e municipal pelo SN
        assertEquals(2, NfsePayloadMapper.regimeApuracaoPadrao(2)); // excesso de sublimite: ISSQN pela NFS-e
        assertNull(NfsePayloadMapper.regimeApuracaoPadrao(3)); // regime normal nao envia
        assertNull(NfsePayloadMapper.regimeApuracaoPadrao(4)); // MEI nao envia
        assertNull(NfsePayloadMapper.regimeApuracaoPadrao(null));
    }

    private static NotaEntity nota(String estado) {
        NotaEntity n = new NotaEntity();
        n.setEstado(estado);
        n.setId("11111111-2222-3333-4444-555555555555");
        n.setNumero("5");
        n.setSerie("1");
        n.setDataEmissao(LocalDateTime.of(2026, 11, 21, 10, 0, 0));
        n.setDataCompetencia(LocalDate.of(2026, 11, 21));
        n.setCodigoServico("010101");
        n.setDiscriminacao("d");
        n.setValorServicos(new BigDecimal("10"));
        return n;
    }

    @Test
    void autorizadaNaoRegrideEcanceladaSoVindaDe101() throws Exception {
        NotaEntity n = nota("AUTORIZADA");
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":5028,\"sucesso\":false}"));
        assertEquals("AUTORIZADA", n.getEstado());
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":5023,\"sucesso\":true}"));
        assertEquals("AUTORIZADA", n.getEstado());
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":101,\"sucesso\":true}"));
        assertEquals("CANCELADA", n.getEstado());
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":100,\"sucesso\":true}"));
        assertEquals("CANCELADA", n.getEstado());
    }

    @Test
    void rejeitadaFalhaEnvioERascunhoNaoSaoAlteradosPelaConsulta() throws Exception {
        for (String e : new String[] {"REJEITADA", "FALHA_ENVIO", "RASCUNHO", "DESCARTADA"}) {
            NotaEntity n = nota(e);
            NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":100,\"sucesso\":true,\"mensagem\":\"ok\"}"));
            assertEquals(e, n.getEstado());
        }
    }

    @Test
    void falhaDeInfraestruturaEIndisponivelVaoParaFalhaEnvioComOMesmoNumero() throws Exception {
        NotaEntity n = nota("PROCESSANDO");
        NfseRetorno.aplicarFalhaDeEnvio(n, "integranotas_indisponivel");
        assertEquals("FALHA_ENVIO", n.getEstado());
        assertEquals("5", n.getNumero());
        n = nota("PROCESSANDO");
        NfseRetorno.aplicarFalhaDeEnvio(n, "integranotas_token_recusado");
        assertEquals("FALHA_ENVIO", n.getEstado());
        assertEquals(NfseRetorno.MSG_TOKEN, n.getMensagemRetorno());
        n = nota("PROCESSANDO");
        assertEquals(NfseRetorno.Efeito.FALHA_ENVIO, NfseRetorno.aplicarEnvio(n, ret("{\"codigo\":0,\"sucesso\":false}")));
        assertEquals("FALHA_ENVIO", n.getEstado());
        n = nota("PROCESSANDO");
        NfseRetorno.aplicarEnvio(n, ret("{\"codigo\":5023,\"sucesso\":true}")); // aceite sem chave
        assertEquals("FALHA_ENVIO", n.getEstado());
    }

    @Test
    void payloadRecusaNotaSemNumeracao() {
        for (int i = 0; i < 3; i++) {
            NotaEntity n = nota("PROCESSANDO");
            if (i == 0) {
                n.setNumero(null);
            } else if (i == 1) {
                n.setSerie(null);
            } else {
                n.setDataEmissao(null);
            }
            var ex = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                    () -> NfsePayloadMapper.montar(n, new PessoaEntity(), true));
            assertTrue(ex.getMessage().contains("sem numero, serie ou data de emissao"));
        }
    }

    @Test
    void tabelaDeCodigosDaConsulta() throws Exception {
        NotaEntity n = nota("PROCESSANDO");
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":5027,\"sucesso\":false}"));
        assertEquals("EM_CONFLITO", n.getEstado());
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":7001,\"sucesso\":false,\"mensagem\":\"falha\"}"));
        assertEquals("EM_CONFLITO", n.getEstado());
        n = nota("PROCESSANDO");
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":5024,\"sucesso\":false,\"mensagem\":\"negado\"}"));
        assertEquals("REJEITADA", n.getEstado());
        n = nota("PROCESSANDO");
        NfseRetorno.aplicarConsulta(n, ret("{\"codigo\":8429,\"sucesso\":false,\"mensagem\":\"limite\"}"));
        assertEquals("PROCESSANDO", n.getEstado());
        assertEquals(8429, n.getCodigoRetorno());
    }

    @Test
    void errosLegiveisLimitadosA4000() throws Exception {
        StringBuilder sb = new StringBuilder("{\"codigo\":5001,\"sucesso\":false,\"erros\":[");
        for (int i = 0; i < 30; i++) {
            sb.append(i > 0 ? "," : "").append("{\"campo\":\"c").append(i).append("\",\"erro\":\"e\",\"descricao\":\"")
                    .append("d".repeat(390)).append("\"}");
        }
        sb.append("]}");
        String s = NfseRetorno.errosComoJson(J.readTree(sb.toString()));
        assertNotNull(s);
        assertTrue(s.length() <= 4000);
        assertTrue(J.readTree(s).isArray());
        assertNull(NfseRetorno.errosComoJson(J.readTree("{\"codigo\":5028}")));
    }

    @Test
    void decodificaBase64GzipTextoPurAELimitaTamanho() throws Exception {
        byte[] t = "<xml/>".getBytes("UTF-8");
        assertEquals("<xml/>", new String(ArquivoGateway.decodificar(Base64.getEncoder().encodeToString(t)), "UTF-8"));
        assertEquals("<xml/>", new String(ArquivoGateway.decodificar("<xml/>"), "UTF-8"));
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(bo)) {
            g.write(t);
        }
        assertEquals("<xml/>", new String(ArquivoGateway.decodificar(Base64.getEncoder().encodeToString(bo.toByteArray())), "UTF-8"));
        ByteArrayOutputStream bomba = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(bomba)) {
            g.write(new byte[3 * 1024 * 1024]);
        }
        assertNull(ArquivoGateway.decodificar(Base64.getEncoder().encodeToString(bomba.toByteArray())));
        assertNull(ArquivoGateway.decodificar(null));
        assertNull(ArquivoGateway.decodificar("   "));
    }

    private static PessoaEntity pessoa() {
        PessoaEntity p = new PessoaEntity();
        p.setNome("Fulano");
        p.setDocumento("12345678901");
        p.setLogradouro("Rua A");
        p.setNumeroEndereco("1");
        p.setBairro("Centro");
        MunicipioEntity m = new MunicipioEntity();
        m.setCodigoIbge("3550308");
        m.setUf("SP");
        p.setCidade(m);
        return p;
    }

    @Test
    void tributosNacionaisSoComDadoECstLegado00ContaComoAusente() {
        NotaEntity n = nota("PROCESSANDO");
        n.setServico(new ServicoEntity());
        n.setCstPisCofins("00"); // notas antigas gravavam 00 no lugar de "sem CST"
        assertTrue(NfsePayloadMapper.montar(n, pessoa(), false).corpo().path("servico").path("tributos_nacionais").isMissingNode());
        n.setCstPisCofins("01");
        var tn = NfsePayloadMapper.montar(n, pessoa(), false).corpo().path("servico").path("tributos_nacionais");
        assertEquals("01", tn.path("cst").asText());
        assertEquals(0, new BigDecimal("10.00").compareTo(tn.path("valor_base_calculo").decimalValue())); // base em branco = valor
    }

    @Test
    void tributosTotaisSoComAFlagLigadaEParaOptanteDoSimples() {
        NotaEntity n = nota("PROCESSANDO");
        ServicoEntity s = new ServicoEntity();
        s.setPercentualTributosSimples(new BigDecimal("6"));
        n.setServico(s);
        var emp = new br.com.emitesimples.configuracao.entity.EmpresaEntity();
        emp.setRegimeTributario(1);
        n.setEmpresa(emp);
        // MEI e regime normal nao enviam o bloco, mesmo com percentual e flag ligada
        for (int crt : new int[] {3, 4}) {
            emp.setRegimeTributario(crt);
            assertTrue(NfsePayloadMapper.montar(n, pessoa(), true).corpo().path("servico").path("tributos_totais").isMissingNode(), "crt " + crt);
        }
        emp.setRegimeTributario(2);
        assertEquals(0, new BigDecimal("6.00").compareTo(NfsePayloadMapper.montar(n, pessoa(), true).corpo().path("servico").path("tributos_totais").path("percentual_tributos_simples_nacional").decimalValue()));
        emp.setRegimeTributario(1);
        assertTrue(NfsePayloadMapper.montar(n, pessoa(), false).corpo().path("servico").path("tributos_totais").isMissingNode());
        var tt = NfsePayloadMapper.montar(n, pessoa(), true).corpo().path("servico").path("tributos_totais");
        assertEquals(0, new BigDecimal("6.00").compareTo(tt.path(NfsePayloadMapper.CAMPO_PERCENTUAL_SIMPLES).decimalValue()));
    }
}
