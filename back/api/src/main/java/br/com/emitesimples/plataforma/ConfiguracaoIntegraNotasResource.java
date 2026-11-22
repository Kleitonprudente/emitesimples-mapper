package br.com.emitesimples.plataforma;

import br.com.emitesimples.plataforma.IntegraNotasConfigService.Alteracao;
import br.com.emitesimples.plataforma.IntegraNotasConfigService.Atual;
import br.com.emitesimples.security.ApiException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Configuracao do IntegraNotas, so para o administrador da plataforma. Os tokens da softhouse sao SEGREDO: nunca voltam na
 * leitura (so {@code tokenXConfigurado}) nem vao para log. O corpo do PUT e lido como texto e interpretado aqui para que uma
 * falha de parse nao carregue trecho do corpo (que pode conter token) para log ou resposta.
 */
@Path("/admin/configuracoes/integranotas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("plataforma_admin")
public class ConfiguracaoIntegraNotasResource {

    private static final ObjectMapper JSON = new ObjectMapper();

    public record UsuarioResumo(String id, String nome) {
    }

    public record ConfiguracaoResposta(String ambienteAtivo, String urlHomologacao, String urlProducao,
                                       @JsonInclude(JsonInclude.Include.ALWAYS) String urlPublicaWebhook,
                                       boolean tokenHomologacaoConfigurado, boolean tokenProducaoConfigurado,
                                       LocalDateTime dataAtualizacao,
                                       @JsonInclude(JsonInclude.Include.ALWAYS) UsuarioResumo atualizadoPor,
                                       boolean producaoPermitida) {
    }

    @Inject
    IntegraNotasConfigService service;

    @Inject
    JsonWebToken jwt;

    @GET
    public ConfiguracaoResposta obter() {
        Atual a = service.obter();
        UsuarioResumo por = a.atualizadoPorId() == null ? null : new UsuarioResumo(a.atualizadoPorId(), a.atualizadoPorNome());
        return new ConfiguracaoResposta(a.ambienteAtivo(), a.urlHomologacao(), a.urlProducao(), a.urlPublicaWebhook(),
                a.tokenHomologacaoConfigurado(), a.tokenProducaoConfigurado(), a.dataAtualizacao(), por, a.producaoPermitida());
    }

    @PUT
    public Response atualizar(String corpo) {
        JsonNode raiz;
        try {
            raiz = corpo == null || corpo.isBlank() ? null : JSON.readTree(corpo);
        } catch (Exception e) {
            raiz = null; // sem registrar a causa: ela pode citar trecho do corpo
        }
        if (raiz == null || !raiz.isObject()) {
            throw new ApiException(400, "requisicao_invalida", "O corpo da requisição deve ser um objeto JSON.");
        }
        Alteracao a = new Alteracao(
                texto(raiz, "ambienteAtivo", "ambiente_invalido", "Ambiente inválido. O ambiente deve ser um texto."),
                texto(raiz, "urlHomologacao", "url_invalida", "URL inválida. A URL deve ser um texto."),
                texto(raiz, "urlProducao", "url_invalida", "URL inválida. A URL deve ser um texto."),
                texto(raiz, "urlPublicaWebhook", "url_invalida", "URL inválida. A URL deve ser um texto."),
                texto(raiz, "tokenHomologacao", "token_invalido", "O token deve ser um texto."),
                texto(raiz, "tokenProducao", "token_invalido", "O token deve ser um texto."));
        service.atualizar(a, jwt.getSubject());
        return Response.noContent().build();
    }

    /** Campo ausente ou null = null (mantem); texto = o texto; outro tipo = 400. */
    private static String texto(JsonNode raiz, String campo, String codigo, String mensagem) {
        JsonNode n = raiz.get(campo);
        if (n == null || n.isNull()) {
            return null;
        }
        if (!n.isTextual()) {
            throw new ApiException(400, codigo, mensagem);
        }
        return n.asText();
    }
}
