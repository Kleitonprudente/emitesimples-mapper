package br.com.emitesimples.gateway;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.integranotas.entity.EventoWebhookEntity;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import org.jboss.logging.Logger;

/**
 * Registro do evento BRUTO MINIMO recebido no webhook do IntegraNotas. So o envelope (origem, cnpj_cpf, tipo, chave, sucesso,
 * codigo, mensagem, identificacao); NUNCA xml/pdf nem o corpo inteiro. O processamento (atualizar a nota) e da fase das
 * notas: o evento nasce {@code estadoProcessamento=pendente} e {@code resultadoAssinatura=nao_verificada}. Chamada
 * ANONIMA: o banco e aberto como a empresa do caminho (RLS), nunca como sistema.
 */
@ApplicationScoped
public class WebhookIntegraNotasService {

    private static final Logger LOG = Logger.getLogger(WebhookIntegraNotasService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    EntityManager em;

    @Inject
    ContextoTenantBanco contexto;


    /**
     * Persiste o evento. Devolve false se a empresa nao existe. Corpo que nao e objeto JSON, ou origem {@code TESTE}
     * (chamada de validacao da URL feita pelo gateway), nao gera evento.
     */
    boolean registrar(String empresaId, byte[] corpo) {
        JsonNode raiz = null;
        try {
            raiz = JSON.readTree(corpo);
        } catch (IOException e) {
            LOG.infof("Webhook IntegraNotas: empresa=%s corpo nao e JSON (%d bytes)", empresaId, corpo.length);
        }
        String origem = raiz != null && raiz.isObject() ? texto(raiz, "origem", 30) : null;
        final JsonNode corpoJson = raiz;
        boolean gravar = raiz != null && raiz.isObject() && !"TESTE".equalsIgnoreCase(origem);
        return contexto.comoEmpresa(empresaId, () -> QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity emp = em.find(EmpresaEntity.class, empresaId);
            if (emp == null) {
                return false;
            }
            if (!gravar) {
                return true;
            }
            var agora = AuthService.agora();
            JsonNode r = corpoJson;
            EventoWebhookEntity e = EventoWebhookEntity.builderOf()
                    .withOrigem(origem)
                    .withCnpjCpf(digitos(texto(r, "cnpj_cpf", 40), 14))
                    .withTipo(texto(r, "tipo", 30))
                    .withChave(texto(r, "chave", 50))
                    .withSucesso(r.path("sucesso").isBoolean() ? r.path("sucesso").asBoolean() : null)
                    .withCodigo(r.path("codigo").isInt() ? r.path("codigo").asInt() : null)
                    .withMensagem(texto(r, "mensagem", 500))
                    .withIdentificacao(texto(r, "identificacao", 40))
                    .withDataRecebimento(agora)
                    .withResultadoAssinatura("nao_verificada")
                    .withEstadoProcessamento("pendente")
                    .withAmbiente(emp.getAmbiente())
                    .withDataCriacao(agora).withDataAtualizacao(agora)
                    .withEmpresa(emp)
                    .build();
            em.persist(e);
            return true;
        }));
    }

    private static String texto(JsonNode n, String campo, int max) {
        JsonNode v = n.get(campo);
        if (v == null || v.isNull() || !v.isValueNode()) {
            return null;
        }
        String s = v.asText("").replaceAll("\\p{Cntrl}", " ").strip();
        if (s.isEmpty()) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String digitos(String s, int max) {
        if (s == null) {
            return null;
        }
        String d = s.replaceAll("[^0-9]", "");
        return d.isEmpty() || d.length() > max ? null : d;
    }
}
