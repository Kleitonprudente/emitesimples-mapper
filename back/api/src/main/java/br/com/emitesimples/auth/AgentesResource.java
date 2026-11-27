package br.com.emitesimples.auth;

import br.com.emitesimples.tenant.EscopoTenant;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Agentes (cliente de acesso, fase 1; docs/financeiro/README.md, 9.4, B3). Administracao: so o ADMINISTRADOR da empresa da sessao.
 * Troca do segredo por token: publica (o agente nao tem sessao), com as travas de forca bruta do login.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class AgentesResource {

    @Inject
    AgentesService agentes;

    @Inject
    EscopoTenant escopo;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("agentes")
    @RolesAllowed("ADMINISTRADOR")
    public List<AgentesService.Agente> listar() {
        return agentes.listar(escopo.exigir("empresa_id"));
    }

    @POST
    @Path("agentes")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public AgentesService.AgenteCriado criar(AgentesService.CriarAgente corpo) {
        return agentes.criar(escopo.exigir("empresa_id"), jwt.getSubject(), corpo);
    }

    @POST
    @Path("agentes/{id}/revogar")
    @RolesAllowed("ADMINISTRADOR")
    public AgentesService.Agente revogar(@PathParam("id") String id) {
        return agentes.revogar(escopo.exigir("empresa_id"), id);
    }

    @POST
    @Path("auth/agente/token")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    public AgentesService.TokenDeAgente token(AgentesService.PedidoDeToken corpo) {
        return agentes.token(corpo);
    }
}
