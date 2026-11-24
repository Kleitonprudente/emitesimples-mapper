package br.com.emitesimples.plataforma;

import br.com.emitesimples.plataforma.PlataformaDtos.VinculoAlteracao;
import br.com.emitesimples.plataforma.VinculoEmpresaAdminService.VinculoEmpresa;
import br.com.emitesimples.security.ApiException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/** Vinculos de usuario de uma empresa, so para o administrador da plataforma. Sem DELETE: so se desativa (ativo=false). */
@Path("/admin/empresas/{empresaId}/vinculos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("PLATAFORMA_ADMIN")
public class VinculoEmpresaAdminResource {

    public record NovoVinculoEmpresa(
            @NotBlank(message = "O usuário é obrigatório.") @Size(max = 36, message = "usuarioId inválido.") String usuarioId,
            @NotBlank(message = "O papel é obrigatório.") @Size(max = 15, message = "Papel inválido.") String papel,
            Boolean ativo,
            Boolean podeCancelarNfse) {
    }

    @Inject
    VinculoEmpresaAdminService service;

    @GET
    public List<VinculoEmpresa> listar(@PathParam("empresaId") String empresaId) {
        return service.listar(empresaId);
    }

    @POST
    public Response criar(@PathParam("empresaId") String empresaId, @Valid NovoVinculoEmpresa body) {
        NovoVinculoEmpresa b = corpo(body);
        VinculoEmpresa v = service.criar(empresaId, b.usuarioId(), b.papel(), b.ativo(), b.podeCancelarNfse());
        return Response.created(URI.create("/admin/empresas/" + empresaId + "/vinculos/" + v.id())).entity(v).build();
    }

    @PATCH
    @Path("/{vinculoId}")
    public VinculoEmpresa alterar(@PathParam("empresaId") String empresaId, @PathParam("vinculoId") String vinculoId,
                                  @Valid VinculoAlteracao body) {
        return service.alterar(empresaId, vinculoId, corpo(body));
    }

    private static <T> T corpo(T body) {
        if (body == null) {
            throw new ApiException(400, "requisicao_invalida", "O corpo da requisição é obrigatório.");
        }
        return body;
    }
}
