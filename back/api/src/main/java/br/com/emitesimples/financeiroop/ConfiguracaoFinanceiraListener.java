package br.com.emitesimples.financeiroop;

import br.com.emitesimples.financeiro.entity.ConfiguracaoFinanceiraEntity;
import br.com.emitesimples.security.ApiException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras de UMA linha da configuracao financeira (docs/financeiro/README.md, F1-H4), registrada em META-INF/orm.xml. Os percentuais sao
 * conferidos AQUI porque acima de 999,99 o proprio numeric(5,2) estoura antes do CHECK da V14 e o erro sairia sem o campo. As demais
 * faixas ficam nos CHECK da V14 (traduzidos pela RestricoesFinanceiro).
 */
public class ConfiguracaoFinanceiraListener {

    static final BigDecimal MAXIMO = new BigDecimal("999.99");

    @PrePersist
    @PreUpdate
    void validar(ConfiguracaoFinanceiraEntity c) {
        List<Map<String, String>> f = problemas(c.getMultaPercentual(), c.getJurosMensalPercentual());
        if (!f.isEmpty()) {
            throw new ApiException(400, "requisicao_invalida", f.get(0).get("mensagem"), Map.of("campos", f));
        }
    }

    static List<Map<String, String>> problemas(BigDecimal multa, BigDecimal juros) {
        List<Map<String, String>> f = new ArrayList<>();
        percentual(f, "multaPercentual", "A multa deve ser de 0 a 999,99%, com até 2 casas decimais.", multa);
        percentual(f, "jurosMensalPercentual", "Os juros ao mês devem ser de 0 a 999,99%, com até 2 casas decimais.", juros);
        return f;
    }

    private static void percentual(List<Map<String, String>> f, String campo, String mensagem, BigDecimal v) {
        if (v != null && (v.signum() < 0 || v.compareTo(MAXIMO) > 0 || v.stripTrailingZeros().scale() > 2)) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("campo", campo);
            m.put("mensagem", mensagem);
            f.add(m);
        }
    }
}
