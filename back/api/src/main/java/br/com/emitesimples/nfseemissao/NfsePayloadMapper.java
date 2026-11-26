package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UNICO ponto que monta o corpo do {@code POST /v2/nfse} do IntegraNotas (docs/integranotas-nfse-v2.md). Tudo do payload esta
 * aqui; nenhum outro codigo conhece nomes de campos do gateway.
 *
 * <p>Regras: o prestador NAO vai no corpo (vem do token); campos opcionais ausentes sao OMITIDOS (nada de null); valores
 * monetarios sao {@code number} com 2 casas; percentuais em pontos percentuais ({@code 5.00} = 5%); datas com offset
 * {@code -03:00} (emissao) e {@code yyyy-MM-dd} (competencia). {@code identificacao} = id da nota sem hifens (32 chars, cabe
 * nos 40 do gateway) e e a chave de correlacao com o webhook e com a consulta.
 *
 * <p>NAO enviados por decisao: {@code ibs_cbs}; {@code tributos_municipais.tipo_operacao} (nao existe no schema de
 * 2026-11-24); {@code valor_previdencia_social}, CNAE, IE e tipo de ente (nao chegam a nota nacional).
 *
 * <p>Tributacao (regras em {@link TributacaoNota}; docs/historias-emissao-nota.md, "Tributacao da nota"): cada grupo so vai com
 * dado. {@code tributos_municipais} segue a regra E0625 e leva sempre {@code aliquota_iss} e {@code valor_base_calculo_iss};
 * {@code tributos_nacionais} leva sempre {@code cst} e {@code valor_base_calculo}, com {@code tipo_retencao} como TEXTO.
 *
 * <p>{@code servico.tributos_totais.percentual_tributos_simples_nacional}: enviado para optantes do Simples Nacional
 * (CRT 1 e 2) com o percentual do cadastro do servico. CONFIRMADO em homologacao: para ME/EPP o governo recusa o indicador
 * de valor total de tributos (E0712) e o percentual do SN e o que deve ir. Desligavel com
 * {@code emitesimples.integranotas.enviar-tributos-totais=false}.
 *
 * <p>A CONFIRMAR EM HOMOLOGACAO: (1) se {@code tributos_totais} fica dentro de {@code servico} (como na documentacao) e nao
 * na raiz; (2) se {@code numero} aceita o valor sem zeros a esquerda (envia-se o numero
 * como string simples); (3) se {@code tipo_destinatario} pode ir para tomador PF; (4) quais blocos o gateway exige por si.
 */
final class NfsePayloadMapper {

    /** Campo do percentual do Simples Nacional em {@code tributos_totais} (informado pelo usuario, confirmado em homologacao). */
    static final String CAMPO_PERCENTUAL_SIMPLES = "percentual_tributos_simples_nacional";

    /** Optante do Simples Nacional ME/EPP: CRT 1 (Simples) e 2 (Simples com excesso de sublimite). */
    static boolean optanteSimples(Integer crt) {
        return crt != null && (crt == 1 || crt == 2);
    }

    static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter EMISSAO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private NfsePayloadMapper() {
    }

    /** Corpo de {@code POST /v1/nfse/cancela}: chave (50 digitos), codigo_cancelamento (1, 2 ou 9) e motivo_cancelamento. */
    static ObjectNode cancelamento(String chave, int codigoMotivo, String motivo) {
        ObjectNode o = JSON.createObjectNode();
        o.put("chave", chave);
        o.put("codigo_cancelamento", codigoMotivo);
        o.put("motivo_cancelamento", motivo);
        return o;
    }

    /** Payload montado + um resumo curto e seguro para o log do gateway (sem dados do tomador). */
    record Montado(ObjectNode corpo, String identificacao, String resumo) {
    }

    /** Identificacao enviada ao gateway para a nota: o id sem hifens. */
    static String identificacao(String notaId) {
        return notaId.replace("-", "");
    }

    /** Campos do tomador que faltam para emitir (lista vazia = completo). Cada item: campo + mensagem. */
    static List<Map<String, String>> tomadorIncompleto(PessoaEntity p) {
        List<Map<String, String>> f = new ArrayList<>();
        int doc = digitos(p.getDocumento()).length();
        if (doc != 11 && doc != 14) {
            f.add(erro("documento", "Informe o CPF ou o CNPJ do tomador."));
        }
        if (vazio(p.getLogradouro())) {
            f.add(erro("logradouro", "Informe o logradouro do tomador."));
        }
        if (vazio(p.getNumeroEndereco())) {
            f.add(erro("numeroEndereco", "Informe o número do endereço do tomador."));
        }
        if (vazio(p.getBairro())) {
            f.add(erro("bairro", "Informe o bairro do tomador."));
        }
        MunicipioEntity m = p.getCidade();
        if (m == null || digitos(m.getCodigoIbge()).length() != 7 || vazio(m.getUf()) || m.getUf().strip().length() != 2) {
            f.add(erro("cidade", "Informe o município do tomador (com código IBGE)."));
        }
        return f;
    }

    /**
     * Regime de apuracao dos tributos do SN (regApTribSN) padrao pelo CRT da empresa: 1 Simples Nacional => 1 (federais e
     * municipal pelo SN); 2 Simples com excesso de sublimite => 2 (federais pelo SN e ISSQN pela NFS-e); MEI (4) e regime
     * normal (3) nao enviam. A CONFIRMAR com o contador: quem recolhe o ISS por fora do SN precisa de 2 ou 3.
     */
    static Integer regimeApuracaoPadrao(Integer crt) {
        if (crt == null) {
            return null;
        }
        return switch (crt) {
            case 1 -> 1;
            case 2 -> 2;
            default -> null;
        };
    }

    static Montado montar(NotaEntity n, PessoaEntity p, boolean enviarTributosTotais) {
        if (n.getNumero() == null || n.getSerie() == null || n.getDataEmissao() == null) {
            // so o rascunho do assistente nao tem numeracao; ela e alocada ao virar processando (ck_nota_numeracao)
            throw new IllegalStateException("Nota " + n.getId() + " sem numero, serie ou data de emissao: nao pode ser enviada ao gateway.");
        }
        ObjectNode raiz = JSON.createObjectNode();
        raiz.put("data_emissao", EMISSAO.format(n.getDataEmissao().truncatedTo(ChronoUnit.SECONDS).atZone(SAO_PAULO)
                .toOffsetDateTime()));
        raiz.put("numero", n.getNumero());
        raiz.put("serie", n.getSerie());
        String identificacao = identificacao(n.getId());
        raiz.put("identificacao", identificacao);
        // dCompet: o gateway exige data E hora com fuso (padrao ^AAAA-MM-DD(T| )hh:mm:ss-0[0-5]:00$, confirmado em homologacao);
        // a competencia e uma data, entao vai o inicio daquele dia em Sao Paulo (nunca depois da emissao).
        raiz.put("data_competencia", EMISSAO.format(n.getDataCompetencia().atStartOfDay(SAO_PAULO).toOffsetDateTime()));
        // regApTribSN: o governo (E0166, 5028) exige para o optante do Simples Nacional ME/EPP; vem da nota (snapshot).
        if (n.getRegimeApuracao() != null) {
            raiz.put("regime_apuracao", n.getRegimeApuracao());
        }
        if (!vazio(n.getInformacoesComplementares())) {
            raiz.put("informacoes_complementares", n.getInformacoesComplementares());
        }

        raiz.set("tomador", tomador(p));

        ObjectNode s = raiz.putObject("servico");
        s.put("codigo", n.getCodigoServico());
        s.put("discriminacao", n.getDiscriminacao());
        if (!vazio(n.getDescricao())) {
            s.put("descricao", corta(n.getDescricao(), 200));
        }
        if (!vazio(n.getCodigoTributacaoMunicipio())) {
            s.put("codigo_tributacao_municipio", n.getCodigoTributacaoMunicipio());
        }
        if (!vazio(n.getCodigoNbs())) {
            s.put("codigo_nbs", n.getCodigoNbs());
        }
        s.put("valor_servicos", dinheiro(n.getValorServicos()));
        BigDecimal desconto = n.getValorDescontoIncondicionado();
        if (desconto != null && desconto.signum() > 0) {
            s.put("valor_desconto_incondicionado", dinheiro(desconto));
        }

        // E0625: no regime 1 so com ISS retido; aliquota_iss e valor_base_calculo_iss sao obrigatorios no grupo
        if (TributacaoNota.issVaiNoEnvio(n, n.getRegimeApuracao())) {
            ObjectNode tm = s.putObject("tributos_municipais");
            boolean retido = Boolean.TRUE.equals(n.getIssRetido());
            tm.put("iss_retido", retido);
            percentual(tm, "aliquota_iss", n.getAliquotaIss());
            tm.put("valor_base_calculo_iss", dinheiro(TributacaoNota.baseIssParaEnvio(n)));
            if (retido) {
                valor(tm, "valor_iss_retido", n.getValorIssRetido());
            }
        }

        if (TributacaoNota.federaisTemDados(n)) {
            ObjectNode tn = s.putObject("tributos_nacionais");
            tn.put("cst", TributacaoNota.cst(n));
            if (n.getTipoRetencao() != null) {
                tn.put("tipo_retencao", String.valueOf(n.getTipoRetencao())); // texto no schema
            }
            tn.put("valor_base_calculo", dinheiro(TributacaoNota.baseFederalParaEnvio(n)));
            percentual(tn, "aliquota_pis", n.getAliquotaPis());
            valor(tn, "valor_pis", n.getValorPis());
            percentual(tn, "aliquota_cofins", n.getAliquotaCofins());
            valor(tn, "valor_cofins", n.getValorCofins());
            valor(tn, "valor_inss", n.getValorInss());
            valor(tn, "valor_ir", n.getValorIr());
            valor(tn, "valor_csll", n.getValorCsll());
        }

        // Unico ponto de montagem de tributos_totais: so para optante do Simples Nacional, com o percentual do servico.
        ServicoEntity sv = n.getServico();
        Integer crt = n.getEmpresa() == null ? null : n.getEmpresa().getRegimeTributario();
        if (enviarTributosTotais && optanteSimples(crt) && sv != null && sv.getPercentualTributosSimples() != null) {
            s.putObject("tributos_totais").put(CAMPO_PERCENTUAL_SIMPLES,
                    sv.getPercentualTributosSimples().setScale(2, RoundingMode.HALF_UP));
        }

        String resumo = "numero=" + n.getNumero() + " serie=" + n.getSerie() + " valor=" + dinheiro(n.getValorServicos())
                + " codigoServico=" + n.getCodigoServico();
        return new Montado(raiz, identificacao, resumo);
    }

    private static ObjectNode tomador(PessoaEntity p) {
        ObjectNode t = JSON.createObjectNode();
        String doc = digitos(p.getDocumento());
        t.put(doc.length() == 14 ? "cnpj" : "cpf", doc);
        t.put("razao_social", corta(p.getNome(), 300));
        if (!vazio(p.getInscricaoMunicipal())) {
            t.put("im", corta(p.getInscricaoMunicipal().strip(), 15));
        }
        String fone = digitos(p.getTelefone());
        if (fone.length() >= 6 && fone.length() <= 20) {
            t.put("telefone", fone);
        }
        if (!vazio(p.getEmail())) {
            t.put("email", corta(p.getEmail().strip(), 60));
        }
        if (p.getTipoDestinatario() != null && (p.getTipoDestinatario() == 0 || p.getTipoDestinatario() == 1)) {
            t.put("tipo_destinatario", p.getTipoDestinatario());
        }
        ObjectNode e = t.putObject("endereco");
        e.put("logradouro", corta(p.getLogradouro(), 255));
        e.put("numero", corta(p.getNumeroEndereco(), 60));
        if (!vazio(p.getComplemento())) {
            e.put("complemento", corta(p.getComplemento(), 256));
        }
        e.put("bairro", corta(p.getBairro(), 60));
        String cep = digitos(p.getCep());
        if (cep.length() == 8) {
            e.put("cep", cep);
        }
        MunicipioEntity m = p.getCidade();
        e.put("codigo_municipio", digitos(m.getCodigoIbge()));
        e.put("uf", m.getUf().strip().toUpperCase());
        return t;
    }

    // ------------------------------------------------------------------------------------- helpers

    static BigDecimal dinheiro(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static void valor(ObjectNode n, String campo, BigDecimal v) {
        if (v != null) {
            n.put(campo, dinheiro(v));
        }
    }

    private static void percentual(ObjectNode n, String campo, BigDecimal v) {
        if (v != null) {
            n.put(campo, v.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String digitos(String s) {
        return s == null ? "" : s.replaceAll("[^0-9]", "");
    }

    private static String corta(String s, int max) {
        String t = s.strip();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }
}
