package br.com.emitesimples.gateway;

import br.com.emitesimples.security.ApiException;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.annotation.security.PermitAll;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Webhook PUBLICO do IntegraNotas: {@code POST /api/webhooks/integranotas/{empresaId}/{token}}. Anonimo (regra propria em
 * application.properties, so POST nesse caminho; o resto de /api/webhooks/* e negado). O {@code token} e o HMAC do
 * empresaId ({@link WebhookToken}); invalido, empresa inexistente ou de formato estranho: 404 sem dizer o motivo.
 * Token confere: 200 {@code {ok:true}} (a IntegraNotas valida a URL com uma chamada de origem TESTE). Corpo ate 4 MB (413).
 * Limite de taxa por IP (antes de tudo) e por empresa (apos o token). Nada do corpo nem do token vai para log.
 */
@Path("/webhooks/integranotas/{empresaId}/{token}")
@Produces(MediaType.APPLICATION_JSON)
public class WebhookIntegraNotasResource {

    private static final Logger LOG = Logger.getLogger(WebhookIntegraNotasResource.class);
    static final int MAX_BYTES = 4 * 1024 * 1024;

    @Inject
    WebhookToken tokens;

    @Inject
    LimiteWebhook limite;

    @Inject
    WebhookIntegraNotasService service;

    @POST
    @PermitAll
    public Map<String, Object> receber(@PathParam("empresaId") String empresaId, @PathParam("token") String token,
            @Context HttpServerRequest req, InputStream corpo) throws IOException {
        limite.consumirIp(req.remoteAddress() == null ? null : req.remoteAddress().host());
        if (empresaId == null || empresaId.length() > 36 || !tokens.confere(empresaId, token)) {
            throw naoEncontrado();
        }
        limite.consumirEmpresa(empresaId);
        byte[] bytes = corpo == null ? new byte[0] : corpo.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) {
            throw new ApiException(413, "corpo_grande_demais", "O corpo da notificação excede 4 MB.");
        }
        if (!service.registrar(empresaId, bytes)) {
            throw naoEncontrado();
        }
        LOG.debugf("Webhook IntegraNotas: empresa=%s recebido (%d bytes)", empresaId, bytes.length);
        return Map.of("ok", true);
    }

    private static ApiException naoEncontrado() {
        return new ApiException(404, "nao_encontrado", "Não encontrado.");
    }
}
