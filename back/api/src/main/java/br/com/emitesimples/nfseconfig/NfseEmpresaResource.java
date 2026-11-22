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
 * NFS-e da empresa da sessao (escrito a mao): GET qualquer papel da empresa; PUT so administrador da empresa. O
 * administrador da plataforma sem empresa na sessao nao tem o que ler: 403 {@code empresa_nao_selecionada}.
 */
@Path("/configuracao/nfse")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NfseEmpresaResource {

    @Inject
    NfseEmpresaService service;

    @GET
    @RolesAllowed({"administrador", "operador", "consulta", "plataforma_admin"})
    public NfseEmpresaService.NfseDto obter() {
        return service.obter();
    }

    @PUT
    @RolesAllowed("administrador")
    public NfseEmpresaService.NfseDto atualizar(NfseEmpresaService.NfseEntrada corpo) {
        return service.atualizar(corpo);
    }
}
