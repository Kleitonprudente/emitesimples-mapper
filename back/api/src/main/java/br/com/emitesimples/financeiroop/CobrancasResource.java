package br.com.emitesimples.financeiroop;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Cobranca por gateway (docs/financeiro/README.md, 9.5, Fase 7). Integracao (credencial por conta), acoes na cobranca e webhook.
 * Criar cobranca e listar as de um titulo ficam no TitulosResource ({@code /financeiro/titulos/{id}/cobrancas}).
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CobrancasResource {

    @Inject
    CobrancasService cobrancas;

    @GET
    @Path("financeiro/cobranca/credenciais")
    @RolesAllowed("ADMINISTRADOR")
    public List<CobrancasService.CredencialDto> credenciais() {
        return cobrancas.credenciais();
    }

    /** Configurar a integracao de uma conta (critico, so ADMINISTRADOR). O token nunca volta; a URL do webhook so na primeira vez. */
    @PUT
    @Path("financeiro/cobranca/credenciais/{idConta}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public CobrancasService.CredencialConfigurada configurar(@PathParam("idConta") String idConta, CobrancasService.ConfigurarCredencial corpo) {
        return cobrancas.configurar(idConta, corpo);
    }

    /** Botao "Atualizar situacao": consulta o provedor e aplica (baixa, se pago). */
    @POST
    @Path("financeiro/cobrancas/{id}/atualizar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR"})
    public CobrancasService.CobrancaDto atualizar(@PathParam("id") String id) {
        return cobrancas.atualizar(id);
    }

    @POST
    @Path("financeiro/cobrancas/{id}/descartar")
    @RolesAllowed("ADMINISTRADOR")
    public CobrancasService.CobrancaDto descartar(@PathParam("id") String id) {
        return cobrancas.descartar(id);
    }

    /** Corpo do webhook: so o id da cobranca no provedor. Nunca e fonte de verdade: dispara a consulta. */
    public record AvisoDoProvedor(String idExterno) {
    }

    /** Webhook do provedor, sem sessao: o segredo no caminho acha a empresa (so o hash fica guardado). 204 sempre que o segredo vale. */
    @POST
    @Path("webhooks/cobranca/{segredo}")
    @Consumes(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response webhook(@PathParam("segredo") String segredo, AvisoDoProvedor corpo) {
        boolean conhecido = cobrancas.webhook(segredo, corpo == null ? null : corpo.idExterno());
        return conhecido ? Response.noContent().build() : Response.status(404).build();
    }
}
