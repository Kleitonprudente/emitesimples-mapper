package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.gateway.IntegraNotasCliente;
import br.com.emitesimples.plataforma.IntegraNotasConfigService;
import br.com.emitesimples.plataforma.SegredoService;
import br.com.emitesimples.security.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Envio do certificado GUARDADO ao gateway (usado pelo POST do certificado e pela habilitacao da NFS-e). Le o token do emitente
 * e o pfx/senha do cofre so na hora do envio, chama o gateway FORA de transacao e grava a situacao conforme o retorno:
 * sucesso -> {@code valido}; 5003 -> {@code invalido}; 5011 -> {@code vencido}; 5029 -> {@code nao_pertence}. Qualquer outra
 * falha (token recusado, indisponivel, resposta invalida) NAO muda a situacao: o certificado segue guardado ({@code nao_enviado})
 * para nova tentativa. Quem chama deve segurar a {@link TravaEmpresa}. Nada de segredo em log.
 *
 * <p>Formato do segredo do certificado no cofre: JSON {@code {"pfx":"<base64>","senha":"..."}} (o cofre guarda texto).
 */
@ApplicationScoped
public class CertificadoEnvioService {

    private static final Logger LOG = Logger.getLogger(CertificadoEnvioService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Resultado do envio: {@code falha == null} = aceito pelo gateway; senao, o erro (a situacao ja foi gravada se aplicavel). */
    public record Desfecho(ApiException falha) {
        public boolean enviado() {
            return falha == null;
        }
    }

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @Inject
    IntegraNotasConfigService config;

    @Inject
    IntegraNotasCliente cliente;

    // ------------------------------------------------------------------------------------------- formato do segredo

    static String empacotar(byte[] pfx, String senha) {
        ObjectNode n = JSON.createObjectNode();
        n.put("pfx", Base64.getEncoder().encodeToString(pfx));
        n.put("senha", senha);
        try {
            return JSON.writeValueAsString(n);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nao foi possivel empacotar o certificado.");
        }
    }

    /** pfx e senha lidos do cofre (uso interno; nunca devolver por API). */
    record Pacote(byte[] pfx, String senha) {
        @Override
        public String toString() {
            return "Pacote[***]";
        }
    }

    static Pacote desempacotar(String json) {
        try {
            JsonNode n = JSON.readTree(json);
            return new Pacote(Base64.getDecoder().decode(n.path("pfx").asText()), n.path("senha").asText());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("Segredo do certificado ilegivel."); // sem a causa: ela cita trecho do conteudo
        }
    }

    // ------------------------------------------------------------------------------------------- envio

    /** Envia o certificado guardado da empresa. Nao lanca erro de gateway: devolve no {@link Desfecho}. */
    public Desfecho enviarGuardado(String empresaId) {
        String[] refs = QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = carregar(empresaId, false);
            return new String[] {e.getReferenciaCertificado(), e.getReferenciaTokenEmpresa(), e.getAmbiente()};
        });
        if (refs[0] == null) {
            return new Desfecho(new ApiException(409, "certificado_nao_enviado_ainda", "A empresa não tem certificado guardado."));
        }
        if (refs[1] == null) {
            return new Desfecho(new ApiException(409, "emitente_nao_cadastrado",
                    "O emitente ainda não foi cadastrado no IntegraNotas. Habilite a NFS-e da empresa primeiro."));
        }
        try {
            Pacote p = desempacotar(segredos.ler(refs[0]));
            String tokenEmitente = segredos.ler(refs[1]);
            cliente.enviarCertificado(config.urlBase(refs[2]), tokenEmitente, p.pfx(), p.senha());
        } catch (ApiException falha) {
            String situacao = switch (falha.getCodigo()) {
                case "certificado_invalido_gateway" -> CertificadoSituacao.INVALIDO;
                case "certificado_vencido_gateway" -> CertificadoSituacao.VENCIDO;
                case "certificado_nao_pertence_gateway" -> CertificadoSituacao.NAO_PERTENCE;
                default -> null;
            };
            if (situacao != null) {
                gravarSituacao(empresaId, situacao);
                return new Desfecho(falha);
            }
            LOG.infof("Certificado: envio nao concluido (empresa=%s, codigo=%s); segue guardado", empresaId, falha.getCodigo());
            Map<String, Object> det = new LinkedHashMap<>();
            if (falha.getDetalhes() instanceof Map<?, ?> m) {
                m.forEach((k, v) -> det.put(String.valueOf(k), v));
            }
            det.put("certificadoGuardado", true);
            return new Desfecho(new ApiException(falha.getStatus(), falha.getCodigo(), falha.getMessage(), det));
        } catch (IllegalStateException | IllegalArgumentException e) {
            // cofre (chave-mestra diferente, segredo ausente ou ilegivel): sem detalhes do conteudo
            LOG.warnf("Certificado: nao foi possivel ler o cofre (empresa=%s, %s)", empresaId, e.getClass().getSimpleName());
            return new Desfecho(new ApiException(500, "cofre_indisponivel",
                    "Não foi possível ler o certificado guardado. Envie o certificado de novo."));
        }
        gravarSituacao(empresaId, CertificadoSituacao.VALIDO);
        return new Desfecho(null);
    }

    void gravarSituacao(String empresaId, String situacao) {
        QuarkusTransaction.requiringNew().run(() -> {
            EmpresaEntity e = carregar(empresaId, true);
            e.setSituacaoCertificado(situacao);
            e.setDataAtualizacao(AuthService.agora());
        });
    }

    EmpresaEntity carregar(String empresaId, boolean travar) {
        var q = em.createQuery("select e from EmpresaEntity e where e.id = :id", EmpresaEntity.class)
                .setParameter("id", empresaId);
        if (travar) {
            q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        }
        return q.getResultStream().findFirst()
                .orElseThrow(() -> new ApiException(404, "nao_encontrado", "Empresa não encontrada."));
    }
}
