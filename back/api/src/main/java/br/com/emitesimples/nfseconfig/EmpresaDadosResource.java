package br.com.emitesimples.nfseconfig;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Dados da empresa da sessao (escrito a mao): GET qualquer papel da empresa; PUT so administrador da empresa. O administrador da
 * plataforma sem empresa na sessao: 403 {@code empresa_nao_selecionada}.
 */
@Path("/configuracao/empresa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmpresaDadosResource {

    @Inject
    EmpresaDadosService service;

    @GET
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR", "CONSULTA", "PLATAFORMA_ADMIN"})
    public EmpresaDadosService.EmpresaDto obter() {
        return service.obter();
    }

    @PUT
    @RolesAllowed("ADMINISTRADOR")
    public EmpresaDadosService.EmpresaDto atualizar(EmpresaDadosService.EmpresaEntrada corpo) {
        return service.atualizar(corpo);
    }
}
