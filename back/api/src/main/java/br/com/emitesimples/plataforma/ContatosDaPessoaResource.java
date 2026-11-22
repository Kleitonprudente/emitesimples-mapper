package br.com.emitesimples.plataforma;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Contatos de uma Pessoa (cliente/fornecedor), escritos A MAO: o CRUD gerado de Contato e so leitura
 * (ver application.properties). GET lista; PUT substitui a lista inteira numa unica transacao.
 */
@Path("/Pessoa/{id}/contatos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContatosDaPessoaResource {

    @Inject
    ContatosDaPessoaService service;

    @GET
    @RolesAllowed({"administrador", "operador", "consulta", "plataforma_admin"})
    public List<ContatosDaPessoaService.ContatoDto> listar(@PathParam("id") String pessoaId) {
        return service.listar(pessoaId);
    }

    @PUT
    @RolesAllowed({"administrador", "operador", "plataforma_admin"})
    public List<ContatosDaPessoaService.ContatoDto> substituir(@PathParam("id") String pessoaId,
            List<ContatosDaPessoaService.ContatoEntrada> corpo) {
        return service.substituir(pessoaId, corpo);
    }
}
