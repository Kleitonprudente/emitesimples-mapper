package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.nfseemissao.NfseEmissaoDtos.NotaDetalhe;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Aprovar e descartar o rascunho de cancelamento preparado pelo assistente (escrito a mao). Papeis: administrador, operador,
 * plataforma_admin; a permissao de cancelar (delegacao do operador) e checada no servico (403 cancelamento_nao_delegado).
 * Sempre a empresa da sessao; cancelamento de outra empresa: 404. Responde com o detalhe da nota.
 */
@Path("/nfse/cancelamentos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CancelamentoAcaoResource {

    @Inject
    NfseEmissaoService service;

    @POST
    @Path("/{id}/aprovar")
    @RolesAllowed({"administrador", "operador", "plataforma_admin"})
    public NotaDetalhe aprovar(@PathParam("id") String id) {
        return service.aprovarCancelamento(id);
    }

    @POST
    @Path("/{id}/descartar")
    @RolesAllowed({"administrador", "operador", "plataforma_admin"})
    public NotaDetalhe descartar(@PathParam("id") String id) {
        return service.descartarCancelamento(id);
    }
}
