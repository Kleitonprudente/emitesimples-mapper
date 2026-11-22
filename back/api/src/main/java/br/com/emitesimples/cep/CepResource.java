package br.com.emitesimples.cep;

import br.com.emitesimples.cep.CepDtos.CepResposta;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

/** GET /api/cep/{cep}: qualquer usuario autenticado. O back chama o ViaCEP; o front nunca o chama. */
@Path("/cep")
@Produces(MediaType.APPLICATION_JSON)
public class CepResource {

    @Inject
    CepService service;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/{cep}")
    @Authenticated
    public CepResposta consultar(@PathParam("cep") String cep) {
        return service.consultar(jwt.getSubject(), cep);
    }
}
