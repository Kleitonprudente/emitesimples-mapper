package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.nfseemissao.NfseEmissaoDtos.*;
import br.com.emitesimples.security.Papeis;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * Emissao de NFS-e (escrito a mao). Cancelar: administrador/plataforma sempre, operador so com delegacao (403 cancelamento_nao_delegado). Leitura: administrador, operador, consulta; escrita: administrador e operador. Sempre a
 * empresa da sessao (plataforma sem empresa: 403 empresa_nao_selecionada).
 */
@Path("/nfse/notas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NfseEmissaoResource {

    private static final String[] LEITURA = {Papeis.ADMINISTRADOR, Papeis.OPERADOR, Papeis.CONSULTA, Papeis.PLATAFORMA_ADMIN};

    @Inject
    NfseEmissaoService service;

    @GET
    @Path("/count")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "PLATAFORMA_ADMIN"})
    public Contagem contar(@QueryParam("estado") String estado, @QueryParam("busca") String busca) {
        return service.contar(estado, busca);
    }

    @GET
    @Path("/{from}/{to}")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "PLATAFORMA_ADMIN"})
    public List<NotaItem> listar(@PathParam("from") int from, @PathParam("to") int to,
            @QueryParam("estado") String estado, @QueryParam("busca") String busca) {
        return service.listar(from, to, estado, busca);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "PLATAFORMA_ADMIN"})
    public NotaDetalhe detalhe(@PathParam("id") String id) {
        return service.detalhe(id);
    }

    @GET
    @Path("/{id}/pdf")
    @Produces({"application/pdf", "application/json"})
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "PLATAFORMA_ADMIN"})
    public Response pdf(@PathParam("id") String id) {
        return Response.ok(service.pdf(id)).type("application/pdf").header("Content-Disposition", "inline").build();
    }

    @POST
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe criar(CriarEntrada corpo) {
        return service.criar(corpo);
    }

    @POST
    @Path("/{id}/enviar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe enviar(@PathParam("id") String id, EnviarEntrada corpo) {
        return service.enviar(id, corpo);
    }

    @POST
    @Path("/preparar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe preparar(CriarEntrada corpo) {
        return service.preparar(corpo);
    }

    @POST
    @Path("/{id}/aprovar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe aprovar(@PathParam("id") String id, EnviarEntrada corpo) {
        return service.aprovar(id, corpo);
    }

    @POST
    @Path("/{id}/descartar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe descartar(@PathParam("id") String id) {
        return service.descartar(id);
    }

    @POST
    @Path("/{id}/cancelamentos/preparar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe prepararCancelamento(@PathParam("id") String id, CancelarEntrada corpo) {
        return service.prepararCancelamento(id, corpo);
    }

    @POST
    @Path("/{id}/cancelar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe cancelar(@PathParam("id") String id, CancelarEntrada corpo) {
        return service.cancelar(id, corpo);
    }

    @GET
    @Path("/{id}/cancelamento/pdf")
    @Produces({"application/pdf", "application/json"})
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "PLATAFORMA_ADMIN"})
    public Response pdfCancelamento(@PathParam("id") String id) {
        return Response.ok(service.pdfCancelamento(id)).type("application/pdf").header("Content-Disposition", "inline").build();
    }

    @POST
    @Path("/{id}/atualizar")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
    public NotaDetalhe atualizar(@PathParam("id") String id) {
        return service.atualizar(id);
    }
}
