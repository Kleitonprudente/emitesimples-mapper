package br.com.emitesimples.auth;

import br.com.emitesimples.auth.Dtos.LoginRequest;
import br.com.emitesimples.auth.Dtos.Perfil;
import br.com.emitesimples.auth.Dtos.RefreshRequest;
import br.com.emitesimples.auth.Dtos.SenhaRequest;
import br.com.emitesimples.auth.Dtos.Sessao;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Autenticação. O acesso por caminho (login, refresh e logout públicos; o resto autenticado) é decidido em
 * application.properties; {@code @Authenticated} nos métodos protegidos é um cinto extra.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService service;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    public Sessao login(@Valid LoginRequest body) {
        return service.login(corpo(body));
    }

    @POST
    @Path("/refresh")
    public Sessao refresh(@Valid RefreshRequest body) {
        return service.refresh(corpo(body).refreshToken());
    }

    @POST
    @Path("/logout")
    public Response logout(@Valid RefreshRequest body) {
        service.logout(corpo(body).refreshToken());
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    @Authenticated
    public Perfil me() {
        Object empresa = jwt.getClaim(EscopoTenant.CLAIM_EMPRESA);
        return service.me(jwt.getSubject(), empresa == null ? null : String.valueOf(empresa));
    }

    @POST
    @Path("/senha")
    @Authenticated
    public Response senha(SenhaRequest body) {
        service.trocarSenha(jwt.getSubject(), corpo(body).senhaAtual(), corpo(body).novaSenha());
        return Response.noContent().build();
    }

    private static <T> T corpo(T body) {
        if (body == null) {
            throw new ApiException(400, "requisicao_invalida", "O corpo da requisição é obrigatório.");
        }
        return body;
    }
}
