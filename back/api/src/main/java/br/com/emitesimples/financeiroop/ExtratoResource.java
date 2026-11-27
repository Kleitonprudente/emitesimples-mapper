package br.com.emitesimples.financeiroop;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** Leitura do livro (docs/financeiro/README.md, F3-H7): extrato, saldos das contas e fluxo de caixa por mes. Qualquer papel da empresa. */
@Path("/financeiro")
@Produces(MediaType.APPLICATION_JSON)
public class ExtratoResource {

    @Inject
    ExtratoService extrato;

    @GET
    @Path("/extrato")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public ExtratoService.Extrato extrato(@QueryParam("idConta") String idConta, @QueryParam("de") String de, @QueryParam("ate") String ate,
            @QueryParam("pagina") Integer pagina) {
        return extrato.extrato(idConta, TitulosResource.data("de", de), TitulosResource.data("ate", ate), pagina);
    }

    @GET
    @Path("/saldos")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public ExtratoService.Saldos saldos() {
        return extrato.saldos();
    }

    @GET
    @Path("/fluxo-de-caixa")
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA"})
    public List<ExtratoService.MesDoFluxo> fluxoDeCaixa(@QueryParam("de") String de, @QueryParam("ate") String ate) {
        return extrato.fluxoDeCaixa(TitulosResource.data("de", de), TitulosResource.data("ate", ate));
    }
}
