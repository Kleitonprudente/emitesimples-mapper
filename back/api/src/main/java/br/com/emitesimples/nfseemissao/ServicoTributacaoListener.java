package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.security.ApiException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coerencia da tributacao no CADASTRO DO SERVICO (roteiro de tributacao, H5 e H6; docs/historias-emissao-nota.md, "Validacoes no
 * cadastro do servico"). Roda antes de inserir ou atualizar a {@link ServicoEntity}, com o ESTADO FINAL do registro: vale para POST,
 * PUT e PATCH do recurso gerado sem tocar no codigo gerado (registrado em {@code META-INF/orm.xml}).
 *
 * <p>Regras: ISS retido ligado exige a aliquota do ISS; tipo de retencao ou aliquota de PIS/COFINS exigem o CST; CST so da lista
 * fechada; tipo de retencao 0 a 9; aliquotas de 0 a 100 com ate 2 casas; codigo de tributacao municipal com 3 digitos e nunca 000.
 * O CST legado "00" conta como sem CST. Recusa com 400 {@code requisicao_invalida} e o erro no campo (mesmo formato da emissao).
 */
public class ServicoTributacaoListener {

    private static final BigDecimal CEM = new BigDecimal("100");

    @PrePersist
    @PreUpdate
    void validar(ServicoEntity s) {
        List<Map<String, String>> f = problemas(s);
        if (!f.isEmpty()) {
            throw new ApiException(400, "requisicao_invalida", "Há campos inválidos na requisição.", Map.of("campos", f));
        }
    }

    static List<Map<String, String>> problemas(ServicoEntity s) {
        List<Map<String, String>> f = new ArrayList<>();
        percentual(f, "aliquotaIss", "A alíquota do ISS", s.getAliquotaIss());
        percentual(f, "aliquotaPis", "A alíquota do PIS", s.getAliquotaPis());
        percentual(f, "aliquotaCofins", "A alíquota da COFINS", s.getAliquotaCofins());
        if (Boolean.TRUE.equals(s.getIssRetido()) && s.getAliquotaIss() == null) {
            f.add(erro("aliquotaIss", TributacaoNota.MSG_ALIQUOTA));
        }
        String cst = s.getCstPisCofins() == null || s.getCstPisCofins().isBlank()
                || TributacaoNota.CST_LEGADO_SEM_TRIBUTOS.equals(s.getCstPisCofins().strip()) ? null : s.getCstPisCofins().strip();
        if (cst != null && !TributacaoNota.CSTS.contains(cst)) {
            f.add(erro("cstPisCofins", "Escolha um CST do PIS/COFINS da lista. Confirme o código com seu contador."));
        } else if (cst == null && (s.getTipoRetencao() != null || s.getAliquotaPis() != null || s.getAliquotaCofins() != null)) {
            f.add(erro("cstPisCofins", TributacaoNota.MSG_CST));
        }
        if (s.getTipoRetencao() != null && (s.getTipoRetencao() < 0 || s.getTipoRetencao() > 9)) {
            f.add(erro("tipoRetencao", "Escolha um tipo de retenção da lista."));
        }
        String cod = s.getCodigoTributacaoMunicipio();
        if (cod != null && !cod.isBlank() && (!cod.strip().matches("[0-9]{3}") || "000".equals(cod.strip()))) {
            f.add(erro("codigoTributacaoMunicipio", "Use 3 dígitos, só se a sua prefeitura tiver código próprio; se não tiver, deixe em "
                    + "branco (o governo recusa o 000)."));
        }
        return f;
    }

    private static void percentual(List<Map<String, String>> f, String campo, String nome, BigDecimal v) {
        if (v != null && (v.signum() < 0 || v.compareTo(CEM) > 0 || v.stripTrailingZeros().scale() > 2)) {
            f.add(erro(campo, nome + " deve ser de 0 a 100, com até 2 casas decimais (5,00 significa 5%)."));
        }
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }
}
