package br.com.emitesimples.financeiroop;

import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras de UMA linha da categoria financeira (docs/financeiro/README.md, F1-H2), registrada em META-INF/orm.xml: o nome sem espacos
 * nas pontas e as dicas sem os sinais {@code <} e {@code >} (removidos ao gravar) e com ate 500 caracteres: conferido AQUI porque o
 * varchar(500) estouraria antes e o erro sairia sem o campo (a API gerada nao aplica o @Size do dominio). A hierarquia (pai do mesmo tipo, ativo, de topo, fora
 * de uso) esta na trigger da migracao V14.
 */
public class CategoriaFinanceiraListener {

    @PrePersist
    @PreUpdate
    void ajustar(CategoriaFinanceiraEntity c) {
        if (c.getNome() != null) {
            c.setNome(c.getNome().trim());
        }
        c.setDicas(limparDicas(c.getDicas()));
        if (c.getDicas() != null && c.getDicas().length() > MAX_DICAS) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("campo", "dicas");
            m.put("mensagem", "As dicas têm no máximo " + MAX_DICAS + " caracteres.");
            throw new ApiException(400, "requisicao_invalida", m.get("mensagem"), Map.of("campos", List.of(m)));
        }
    }

    static final int MAX_DICAS = 500;

    static String limparDicas(String dicas) {
        if (dicas == null) {
            return null;
        }
        String d = dicas.replace("<", "").replace(">", "").trim();
        return d.isEmpty() ? null : d;
    }
}
