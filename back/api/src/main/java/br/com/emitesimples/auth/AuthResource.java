package br.com.emitesimples.auth;

import br.com.emitesimples.auth.Dtos.Emitida;
import br.com.emitesimples.auth.Dtos.LoginRequest;
import br.com.emitesimples.auth.Dtos.Perfil;
import br.com.emitesimples.auth.Dtos.SenhaRequest;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.security.Authenticated;
import br.com.emitesimples.security.ErroApi;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Autenticação. O acesso por caminho (login, refresh e logout públicos; o resto autenticado) é decidido em
 * application.properties; {@code @Authenticated} nos métodos protegidos é um cinto extra.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    /** Nome do cookie do refresh token. Path restrito a /api/auth: o navegador so o envia a estes endpoints. */
    public static final String COOKIE_REFRESH = "refresh_token";
    static final String COOKIE_PATH = "/api/auth";
    /** Defesa em profundidade contra CSRF: refresh e logout exigem este cabecalho (um formulario cross-site nao o envia). */
    public static final String CABECALHO_CSRF = "X-Requested-With";
    public static final String VALOR_CSRF = "emitesimples";

    @Inject
    AuthService service;

    @Inject
    JsonWebToken jwt;

    @ConfigProperty(name = "emitesimples.auth.refresh-token-dias", defaultValue = "7")
    long refreshDias;

    /** Secure ligado por padrao; desligavel (so dev/test em http puro) por emitesimples.auth.cookie-secure=false. */
    @ConfigProperty(name = "emitesimples.auth.cookie-secure", defaultValue = "true")
    boolean cookieSecure;

    @POST
    @Path("/login")
    public Response login(@Valid LoginRequest body) {
        return comSessao(service.login(corpo(body)));
    }

    @POST
    @Path("/refresh")
    @Consumes(MediaType.WILDCARD)
    public Response refresh(@HeaderParam(CABECALHO_CSRF) String csrf, @CookieParam(COOKIE_REFRESH) String refreshToken) {
        exigirCabecalho(csrf);
        if (refreshToken == null || refreshToken.isBlank() || refreshToken.length() > 200) {
            return erroSessao(new ApiException(401, "sessao_invalida", "Sessão inválida ou expirada. Entre novamente."));
        }
        try {
            return comSessao(service.refresh(refreshToken));
        } catch (ApiException e) {
            if (e.getStatus() == 401) {
                return erroSessao(e); // sessao_invalida: o cookie nao vale mais, apaga
            }
            throw e;
        }
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.WILDCARD)
    public Response logout(@HeaderParam(CABECALHO_CSRF) String csrf, @CookieParam(COOKIE_REFRESH) String refreshToken) {
        exigirCabecalho(csrf);
        if (refreshToken != null && !refreshToken.isBlank() && refreshToken.length() <= 200) {
            service.logout(refreshToken);
        }
        return Response.noContent().cookie(cookieApagado()).build();
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
        return Response.noContent().cookie(cookieApagado()).build();
    }

    private Response comSessao(Emitida e) {
        Response.ResponseBuilder b = Response.ok(e.sessao());
        if (e.refreshToken() != null) {
            b.cookie(cookieRefresh(e.refreshToken()));
        }
        return b.build();
    }

    /** 401 sessao_invalida do refresh: mesmo corpo de erro de sempre, mais o cookie apagado. */
    private Response erroSessao(ApiException e) {
        return Response.status(e.getStatus()).type(MediaType.APPLICATION_JSON)
                .entity(new ErroApi(e.getCodigo(), e.getMessage(), e.getDetalhes()))
                .cookie(cookieApagado()).build();
    }

    private static void exigirCabecalho(String valor) {
        if (!VALOR_CSRF.equals(valor)) {
            throw new ApiException(403, "cabecalho_obrigatorio",
                    "Cabeçalho " + CABECALHO_CSRF + ": " + VALOR_CSRF + " obrigatório neste endpoint.");
        }
    }

    private NewCookie cookieRefresh(String token) {
        return base(token).maxAge((int) Duration.ofDays(refreshDias).toSeconds()).build();
    }

    private NewCookie cookieApagado() {
        return base("").maxAge(0).expiry(Date.from(Instant.EPOCH)).build();
    }

    private NewCookie.Builder base(String valor) {
        return new NewCookie.Builder(COOKIE_REFRESH).value(valor).path(COOKIE_PATH).httpOnly(true)
                .secure(cookieSecure).sameSite(NewCookie.SameSite.STRICT);
    }

    private static <T> T corpo(T body) {
        if (body == null) {
            throw new ApiException(400, "requisicao_invalida", "O corpo da requisição é obrigatório.");
        }
        return body;
    }
}
