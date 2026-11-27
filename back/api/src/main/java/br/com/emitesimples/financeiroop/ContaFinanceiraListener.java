package br.com.emitesimples.financeiroop;

import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.security.ApiException;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras de UMA linha da conta financeira (docs/financeiro/README.md, F1-H1), com o ESTADO FINAL do registro: vale para POST, PUT e
 * PATCH do recurso gerado sem tocar no codigo gerado (registrado em META-INF/orm.xml). As regras entre linhas (nome unico, banco
 * existente, conta padrao por forma) estao na migracao V14.
 *
 * <p>A data do saldo inicial nao pode ser futura no fuso do modulo (America/Sao_Paulo, adaptacao A12).
 */
public class ContaFinanceiraListener {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @PrePersist
    @PreUpdate
    void validar(ContaFinanceiraEntity c) {
        if (c.getNome() != null) {
            c.setNome(c.getNome().trim());
        }
        List<Map<String, String>> f = problemas(c.getDataSaldoInicial(), LocalDate.now(FUSO));
        if (!f.isEmpty()) {
            throw new ApiException(400, "requisicao_invalida", "Há campos inválidos na requisição.", Map.of("campos", f));
        }
    }

    static List<Map<String, String>> problemas(LocalDate dataSaldoInicial, LocalDate hoje) {
        List<Map<String, String>> f = new ArrayList<>();
        if (dataSaldoInicial != null && dataSaldoInicial.isAfter(hoje)) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("campo", "dataSaldoInicial");
            m.put("mensagem", "A data do saldo inicial não pode ser futura.");
            f.add(m);
        }
        return f;
    }
}
