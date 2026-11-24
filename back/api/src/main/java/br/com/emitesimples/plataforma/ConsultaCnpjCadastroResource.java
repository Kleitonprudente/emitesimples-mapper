package br.com.emitesimples.plataforma;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** Consulta de CNPJ para preencher o cadastro de empresa; so o administrador da plataforma. Cada chamada gasta uma consulta do plano. */
@Path("/admin/consulta-cnpj")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("PLATAFORMA_ADMIN")
public class ConsultaCnpjCadastroResource {

    @Inject
    ConsultaCnpjCadastroService service;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/{cnpj}")
    public ConsultaCnpjCadastro consultar(@PathParam("cnpj") String cnpj) {
        return service.consultar(jwt.getSubject(), cnpj);
    }
}
