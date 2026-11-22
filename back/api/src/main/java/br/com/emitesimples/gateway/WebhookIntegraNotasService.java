package br.com.emitesimples.gateway;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.integranotas.entity.EventoWebhookEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.nfseemissao.NfseAcompanhamentoService;
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

    @Inject
    NfseAcompanhamentoService acompanhamento;


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
        final String[] notaCasada = new String[1];
        boolean ok = contexto.comoEmpresa(empresaId, () -> QuarkusTransaction.requiringNew().call(() -> {
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
                    .withEstadoProcessamento("ignorado")
                    .withAmbiente(emp.getAmbiente())
                    .withDataCriacao(agora).withDataAtualizacao(agora)
                    .withEmpresa(emp)
                    .build();
            // O evento e SO gatilho: nunca confia no conteudo (assinatura nao verificada). Se casar com uma nota da empresa
            // (por chave ou identificacao), o gateway e consultado (autoridade); senao o evento e ignorado.
            if (e.getChave() != null && e.getCodigo() != null) {
                // reentrega do mesmo evento (o gateway tenta ate 3 vezes; indice unico de deduplicacao): nao regrava, mas
                // ainda serve de gatilho
                long ja = em.createQuery("select count(x) from EventoWebhookEntity x where x.empresa.id = :e and x.ambiente = :a "
                        + "and x.chave = :c and x.codigo = :k", Long.class).setParameter("e", empresaId)
                        .setParameter("a", emp.getAmbiente()).setParameter("c", e.getChave()).setParameter("k", e.getCodigo())
                        .getSingleResult();
                if (ja > 0) {
                    notaCasada[0] = notaDaEmpresa(empresaId, e.getChave(), e.getIdentificacao());
                    return true;
                }
            }
            String notaId = notaDaEmpresa(empresaId, e.getChave(), e.getIdentificacao());
            if (notaId != null) {
                e.setNota(em.getReference(NotaEntity.class, notaId));
                e.setEstadoProcessamento("processado");
                e.setDataProcessamento(agora);
                notaCasada[0] = notaId;
            }
            em.persist(e);
            return true;
        }));
        if (ok && notaCasada[0] != null) {
            acompanhamento.agendarConsulta(empresaId, notaCasada[0]);
        }
        return ok;
    }

    /** Id da nota DESTA empresa que casa com a chave ou com a identificacao (id da nota, com ou sem hifens); ou null. */
    private String notaDaEmpresa(String empresaId, String chave, String identificacao) {
        if (chave != null) {
            var l = em.createQuery("select n.id from NotaEntity n where n.empresa.id = :e and n.chave = :c", String.class)
                    .setParameter("e", empresaId).setParameter("c", chave).getResultList();
            if (!l.isEmpty()) {
                return l.get(0);
            }
        }
        if (identificacao != null) {
            String id = identificacao;
            if (id.matches("[0-9a-fA-F]{32}")) {
                id = id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20)
                        + "-" + id.substring(20);
            }
            if (id.length() <= 36) {
                var l = em.createQuery("select n.id from NotaEntity n where n.empresa.id = :e and n.id = :i", String.class)
                        .setParameter("e", empresaId).setParameter("i", id.toLowerCase()).getResultList();
                if (!l.isEmpty()) {
                    return l.get(0);
                }
            }
        }
        return null;
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
