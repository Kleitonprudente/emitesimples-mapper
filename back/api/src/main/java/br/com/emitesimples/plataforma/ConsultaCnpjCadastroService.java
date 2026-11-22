package br.com.emitesimples.plataforma;

import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.plataforma.ConsultaCnpjCadastro.MunicipioRef;
import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Consulta de CNPJ para PREENCHER o cadastro da empresa. Reusa o {@link ConsultaCnpjCliente} (todas as defesas) e a
 * configuracao salva; so mapeia o JSON bruto para os campos da Empresa e resolve o municipio pelo codigo IBGE.
 */
@ApplicationScoped
public class ConsultaCnpjCadastroService {

    /** Codigos do enum regimeTributario de Configuracao/Empresa.mi. */
    static final int REGIME_SIMPLES_NACIONAL = 1;
    static final int REGIME_MEI = 4;

    @Inject
    ConsultaCnpjTesteService base;

    @Inject
    ConsultaCnpjCliente cliente;

    @Inject
    LimiteConsultaCnpj limite;

    @Inject
    EntityManager em;

    public ConsultaCnpjCadastro consultar(String usuarioId, String cnpjBruto) {
        limite.consumir(usuarioId);
        ConsultaCnpjTesteService.Preparo p = base.preparar(cnpjBruto);
        JsonNode raiz = cliente.consultarBruto(p.url(), p.cnpj(), p.token()).raiz();
        return mapear(raiz, p.cnpj());
    }

    ConsultaCnpjCadastro mapear(JsonNode r, String cnpj) {
        JsonNode est = r.path("estabelecimento");
        String uf = texto(est.path("estado"), "sigla");
        String ibge = texto(est.path("cidade"), "ibge_id");
        String tipo = texto(est, "tipo_logradouro");
        String logr = texto(est, "logradouro");
        String logradouro = logr == null ? tipo : tipo == null ? logr : tipo + " " + logr;
        String cep = soDigitos(texto(est, "cep"));
        String telefone = soDigitos(concat(texto(est, "ddd1"), texto(est, "telefone1")));
        return new ConsultaCnpjCadastro(cnpj, texto(r, "razao_social"), texto(est, "nome_fantasia"),
                texto(est, "email"), telefone, cep, logradouro, texto(est, "numero"), texto(est, "complemento"),
                texto(est, "bairro"), municipio(ibge), uf, ibge, regime(r.path("simples")),
                inscricao(est.path("inscricoes_estaduais"), uf), texto(est, "situacao_cadastral"),
                texto(est, "data_inicio_atividade"));
    }

    /** Sugestao pelo Simples do CNPJ.ws: MEI, depois Simples Nacional; senao null (regime normal nao se adivinha). */
    static Integer regime(JsonNode simples) {
        if (!simples.isObject()) {
            return null;
        }
        if ("sim".equalsIgnoreCase(texto(simples, "mei"))) {
            return REGIME_MEI;
        }
        if ("sim".equalsIgnoreCase(texto(simples, "simples"))) {
            return REGIME_SIMPLES_NACIONAL;
        }
        return null;
    }

    /** IE ativa cujo estado e o da UF do estabelecimento. */
    static String inscricao(JsonNode lista, String uf) {
        if (!lista.isArray() || uf == null) {
            return null;
        }
        for (JsonNode ie : lista) {
            JsonNode ativo = ie.get("ativo");
            boolean ativa = ativo != null && (ativo.asBoolean(false) || "true".equalsIgnoreCase(ativo.asText("")));
            if (ativa && uf.equalsIgnoreCase(texto(ie.path("estado"), "sigla"))) {
                String n = texto(ie, "inscricao_estadual");
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    private MunicipioRef municipio(String ibge) {
        if (ibge == null) {
            return null;
        }
        return QuarkusTransaction.requiringNew().call(() -> em
                .createQuery("select m from MunicipioEntity m where m.codigoIbge = :c", MunicipioEntity.class)
                .setParameter("c", ibge).setMaxResults(1).getResultStream().findFirst()
                .map(m -> new MunicipioRef(m.getId(), m.getNome(), m.getUf(), m.getCodigoIbge())).orElse(null));
    }

    private static String concat(String a, String b) {
        return b == null ? null : a == null ? b : a + b;
    }

    private static String soDigitos(String s) {
        if (s == null) {
            return null;
        }
        String d = s.replaceAll("[^0-9]", "");
        return d.isEmpty() ? null : d;
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n == null ? null : n.get(campo);
        if (v == null || v.isNull() || !v.isValueNode()) {
            return null;
        }
        String s = v.asText("").trim();
        return s.isEmpty() ? null : s;
    }
}
