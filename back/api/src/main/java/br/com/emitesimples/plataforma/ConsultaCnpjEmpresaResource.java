package br.com.emitesimples.plataforma;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Consulta de CNPJ para o usuario da EMPRESA (administrador ou operador) preencher cadastros; tambem a plataforma.
 * Mesmo servico, corpo, erros e limite do {@code /admin/consulta-cnpj}: o contador de 30/min por usuario e COMPARTILHADO
 * entre as duas rotas (mesmo {@link LimiteConsultaCnpj}). Cada chamada gasta uma consulta do plano CNPJ.ws da plataforma.
 */
@Path("/consulta-cnpj")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"ADMINISTRADOR", "OPERADOR", "PLATAFORMA_ADMIN"})
public class ConsultaCnpjEmpresaResource {

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
