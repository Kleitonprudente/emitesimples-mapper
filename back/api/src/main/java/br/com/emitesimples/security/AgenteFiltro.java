package br.com.emitesimples.security;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Limites do token de AGENTE (docs/financeiro/README.md, 9.4, B3). Com a claim {@code cliente_acesso}: GET/HEAD so com o escopo
 * {@code consultar}; POST so nos caminhos de rascunho (lancar titulo, preparar nota) e com o escopo {@code preparar}; qualquer outra
 * coisa e 403 {@code agente_sem_permissao}, mesmo que uma regra de caminho deixasse passar. Operacoes criticas (aprovar, baixar,
 * estornar, cancelar, enviar cobranca) nunca sao de agente.
 */
public class AgenteFiltro {

    static final Set<String> RASCUNHOS = Set.of("/api/financeiro/titulos", "/api/nfse/notas/preparar");
    static final Pattern LEITURA_PROIBIDA = Pattern.compile("^/api/(agentes|auth/.*|Usuario.*|VinculoUsuario.*|ClienteAcesso.*)$");

    @Inject
    JsonWebToken jwt;

    @ServerRequestFilter
    public Response limitarAgente(ContainerRequestContext ctx) {
        if (jwt == null || jwt.getClaim(JwtEmissor.CLAIM_CLIENTE_ACESSO) == null) {
            return null;
        }
        Object e = jwt.getClaim(JwtEmissor.CLAIM_ESCOPOS);
        Set<String> escopos = e == null ? Set.of() : Set.copyOf(Arrays.asList(String.valueOf(e).replace("\"", "").split("\\s+")));
        String caminho = ctx.getUriInfo().getRequestUri().getPath().replaceAll("/+$", "");
        return permitido(ctx.getMethod(), caminho, escopos) ? null : negar();
    }

    static boolean permitido(String metodo, String caminho, Set<String> escopos) {
        if (("GET".equals(metodo) || "HEAD".equals(metodo))) {
            return escopos.contains("consultar") && !LEITURA_PROIBIDA.matcher(caminho).matches();
        }
        return "POST".equals(metodo) && RASCUNHOS.contains(caminho) && escopos.contains("preparar");
    }

    private static Response negar() {
        return Response.status(403).type(MediaType.APPLICATION_JSON)
                .entity(new ErroApi("agente_sem_permissao", "Um agente só consulta e prepara rascunhos; esta operação é de uma pessoa.", null))
                .build();
    }
}
