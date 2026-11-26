package br.com.emitesimples.plataforma;

import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfseconfig.CertificadoSituacao;
import br.com.emitesimples.nfseconfig.StatusCadastro;
import br.com.emitesimples.security.ApiException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Token do EMITENTE informado a mao pelo administrador da plataforma, para uma empresa que JA e emitente no IntegraNotas (ex.:
 * migrada de outro sistema) quando o token da softhouse nao esta disponivel para recupera-lo pela lista.
 *
 * <p>{@code POST /api/admin/empresas/{empresaId}/integranotas/token-emitente} {@code {token}} -> 204. Guarda o token no cofre
 * (cifrado; substitui o anterior do mesmo ambiente) como o token do emitente da empresa no AMBIENTE ATIVO e deixa o cadastro
 * {@code NAO_CADASTRADO}: a proxima habilitacao da NFS-e ({@code PUT /api/configuracao/nfse}) sincroniza com esse token
 * ({@code PUT /v1/emitente}) em vez de criar o emitente, e envia o certificado guardado. O valor nunca volta em resposta nem
 * vai a log. So {@code PLATAFORMA_ADMIN} (caminho /api/admin/*; PUT nesse caminho e negado pela configuracao, por isso POST).
 */
@Path("/admin/empresas/{empresaId}/integranotas/token-emitente")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("PLATAFORMA_ADMIN")
public class TokenEmitenteAdminResource {

    private static final Logger LOG = Logger.getLogger(TokenEmitenteAdminResource.class);
    /** JWT: tres partes base64url separadas por ponto. */
    private static final String FORMATO_JWT = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$";
    static final int TAMANHO_MAXIMO = 4000;

    public record TokenEmitente(String token) {
    }

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @Inject
    IntegraNotasConfigService config;

    @POST
    public Response informar(@PathParam("empresaId") String empresaId, TokenEmitente body) {
        String token = body == null || body.token() == null ? "" : body.token().strip();
        if (token.isEmpty() || token.length() > TAMANHO_MAXIMO || !token.matches(FORMATO_JWT)) {
            throw new ApiException(400, "requisicao_invalida", "Há campos inválidos na requisição.", Map.of("campos",
                    List.of(erro("token", "Informe o token do emitente do IntegraNotas (o JWT completo)."))));
        }
        String ambiente = config.ambienteAtivo();
        QuarkusTransaction.requiringNew().run(() -> {
            EmpresaEntity e = empresaId == null || empresaId.length() > 36 ? null : em.find(EmpresaEntity.class, empresaId);
            if (e == null) {
                throw new ApiException(404, "empresa_nao_encontrada", "Empresa não encontrada.");
            }
            String ref = e.getReferenciaTokenEmpresa();
            if (ref != null && ambiente.equals(e.getAmbiente())) {
                segredos.substituir(ref, token);
            } else {
                e.setReferenciaTokenEmpresa(segredos.guardar(token));
            }
            e.setAmbiente(ambiente);
            e.setStatusCadastro(StatusCadastro.NAO_CADASTRADO); // a habilitacao sincroniza com o token e marca CADASTRADO
            if (CertificadoSituacao.guardado(e)) {
                e.setSituacaoCertificado(CertificadoSituacao.NAO_ENVIADO); // o certificado e (re)enviado na habilitacao
            }
            e.setDataAtualizacao(br.com.emitesimples.auth.AuthService.agora());
        });
        LOG.infof("IntegraNotas: token do emitente informado pela plataforma (empresa=%s, ambiente=%s)", empresaId, ambiente);
        return Response.noContent().build();
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }
}
