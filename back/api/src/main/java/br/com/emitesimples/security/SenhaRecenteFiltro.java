package br.com.emitesimples.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.regex.Pattern;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Senha recente (docs/financeiro/README.md, 9.2, A7; referencia §10): o estorno de movimento e toda alteracao de conta financeira (PUT ou
 * PATCH no CRUD gerado, inclusive inativar) so passam com um token de acesso emitido por POST /api/auth/reautenticar ha no maximo 5
 * minutos (claim {@code senha_recente_ate}). Sem ela: 403 {@code senha_recente_exigida}, e a tela pede a senha e repete a operacao.
 * O papel continua sendo conferido pelo @RolesAllowed e pelas regras do application.properties; este filtro so acrescenta a senha.
 */
public class SenhaRecenteFiltro {

    static final Pattern ESTORNO = Pattern.compile("^/api/financeiro/movimentos/[^/]+/estornar/?$");
    static final Pattern CONTA = Pattern.compile("^/api/ContaFinanceira(/.*)?$");

    @Inject
    JsonWebToken jwt;

    @ServerRequestFilter
    public Response exigirSenhaRecente(ContainerRequestContext ctx) {
        String caminho = ctx.getUriInfo().getRequestUri().getPath();
        String metodo = ctx.getMethod();
        if (!exige(metodo, caminho)) {
            return null;
        }
        if (jwt == null || jwt.getSubject() == null) {
            return null; // anonimo: a politica de acesso responde 401
        }
        if (valida(jwt.getClaim(JwtEmissor.CLAIM_SENHA_RECENTE), Instant.now().getEpochSecond())) {
            return null;
        }
        return Response.status(403).type(MediaType.APPLICATION_JSON)
                .entity(new ErroApi("senha_recente_exigida", "Confirme sua senha para continuar.", null)).build();
    }

    static boolean exige(String metodo, String caminho) {
        if ("POST".equals(metodo) && ESTORNO.matcher(caminho).matches()) {
            return true;
        }
        return ("PUT".equals(metodo) || "PATCH".equals(metodo)) && CONTA.matcher(caminho).matches();
    }

    static boolean valida(Object claim, long agora) {
        long ate;
        if (claim instanceof Number n) {
            ate = n.longValue();
        } else if (claim instanceof jakarta.json.JsonNumber jn) {
            ate = jn.longValue();
        } else if (claim != null) {
            try {
                ate = Long.parseLong(claim.toString());
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
        return ate >= agora;
    }
}
