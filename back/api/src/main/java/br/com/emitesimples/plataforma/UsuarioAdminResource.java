package br.com.emitesimples.plataforma;

import br.com.emitesimples.plataforma.PlataformaDtos.SenhaTemporaria;
import br.com.emitesimples.plataforma.PlataformaDtos.UsuarioAdmin;
import br.com.emitesimples.plataforma.PlataformaDtos.UsuarioEdicao;
import br.com.emitesimples.plataforma.PlataformaDtos.UsuarioNovo;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoAdmin;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoAlteracao;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoNovo;
import br.com.emitesimples.results.CountResult;
import br.com.emitesimples.security.ApiException;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Usuarios e vinculos, so para o administrador da plataforma. Nunca devolve senha nem hash. Nao ha DELETE: usuario e
 * vinculo nunca se apagam, so se desativam (ativo=false).
 */
@Path("/admin/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("plataforma_admin")
public class UsuarioAdminResource {

    private static final int PAGINA_PADRAO = 100;

    @Inject
    UsuarioAdminService service;

    @Inject
    JsonWebToken jwt;

    @GET
    public List<UsuarioAdmin> listar(@QueryParam("busca") String busca, @QueryParam("ativo") Boolean ativo) {
        return service.listar(busca, ativo, 0, PAGINA_PADRAO - 1);
    }

    @GET
    @Path("/count")
    public CountResult contar(@QueryParam("busca") String busca, @QueryParam("ativo") Boolean ativo) {
        CountResult r = new CountResult();
        r.setCount(service.contar(busca, ativo));
        return r;
    }

    @GET
    @Path("/{from}/{to}")
    public List<UsuarioAdmin> faixa(@PathParam("from") int from, @PathParam("to") int to,
                                    @QueryParam("busca") String busca, @QueryParam("ativo") Boolean ativo) {
        return service.listar(busca, ativo, from, to);
    }

    @GET
    @Path("/{id}")
    public UsuarioAdmin obter(@PathParam("id") String id) {
        return service.obter(id);
    }

    @POST
    public Response criar(@Valid UsuarioNovo body) {
        UsuarioAdmin criado = service.criar(corpo(body));
        return Response.created(URI.create("/api/admin/usuarios/" + criado.id())).entity(criado).build();
    }

    @PATCH
    @Path("/{id}")
    public UsuarioAdmin editar(@PathParam("id") String id, @Valid UsuarioEdicao body) {
        return service.editar(id, corpo(body), jwt.getSubject());
    }

    @POST
    @Path("/{id}/senha")
    public Response redefinirSenha(@PathParam("id") String id, @Valid SenhaTemporaria body) {
        service.redefinirSenha(id, corpo(body).senha());
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/vinculos")
    public List<VinculoAdmin> vinculos(@PathParam("id") String id) {
        return service.vinculos(id);
    }

    @POST
    @Path("/{id}/vinculos")
    public Response criarVinculo(@PathParam("id") String id, @Valid VinculoNovo body) {
        VinculoAdmin v = service.criarVinculo(id, corpo(body));
        return Response.created(URI.create("/api/admin/usuarios/" + id + "/vinculos/" + v.id())).entity(v).build();
    }

    @PATCH
    @Path("/{id}/vinculos/{vinculoId}")
    public VinculoAdmin alterarVinculo(@PathParam("id") String id, @PathParam("vinculoId") String vinculoId,
                                       @Valid VinculoAlteracao body) {
        return service.alterarVinculo(id, vinculoId, corpo(body));
    }

    private static <T> T corpo(T body) {
        if (body == null) {
            throw new ApiException(400, "requisicao_invalida", "O corpo da requisição é obrigatório.");
        }
        return body;
    }
}
