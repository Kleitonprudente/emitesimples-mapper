package br.com.emitesimples.security;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Só dá corpo JSON ao 401 de quem chegou SEM credencial (o desafio sai do Vert.x, antes dos mapeadores JAX-RS, e
 * viria com corpo vazio). NÃO autentica ninguém: {@code authenticate} devolve nulo e a autenticação segue para o
 * mecanismo JWT padrão. O cabeçalho WWW-Authenticate: Bearer é mantido.
 */
@ApplicationScoped
public class DesafioJsonMechanism implements HttpAuthenticationMechanism {

    private static final byte[] CORPO = ("{\"codigo\":\"nao_autenticado\","
            + "\"mensagem\":\"Autenticação necessária ou token inválido.\"}").getBytes(StandardCharsets.UTF_8);

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer"));
    }

    @Override
    public Uni<Boolean> sendChallenge(RoutingContext context) {
        var resp = context.response();
        resp.setStatusCode(401);
        resp.putHeader("WWW-Authenticate", "Bearer");
        resp.putHeader("Content-Type", "application/json;charset=UTF-8");
        resp.end(io.vertx.core.buffer.Buffer.buffer(CORPO));
        return Uni.createFrom().item(true);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().nullItem();
    }

    @Override
    public int getPriority() {
        return 5000;
    }
}
