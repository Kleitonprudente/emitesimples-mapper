package br.com.emitesimples.financeiroop;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * Operacoes criticas do livro sem titulo (docs/financeiro/README.md, F3-H4 a F3-H6): estornar, lancar avulso e transferir. So ADMINISTRADOR
 * (decisao C4); a senha recente da referencia esta pendente (C5). A baixa de titulo fica em /api/financeiro/titulos/{id}/baixar.
 */
@Path("/financeiro/movimentos")
@Produces(MediaType.APPLICATION_JSON)
public class MovimentosResource {

    @Inject
    LivroDeMovimentos livro;

    @POST
    @Path("/{id}/estornar")
    @RolesAllowed("ADMINISTRADOR")
    public List<LivroDeMovimentos.MovimentoDto> estornar(@PathParam("id") String id) {
        return livro.estornar(id);
    }

    @POST
    @Path("/avulso")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public LivroDeMovimentos.MovimentoDto avulso(LivroDeMovimentos.Avulso corpo) {
        return livro.lancarAvulso(corpo);
    }

    @POST
    @Path("/transferencia")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMINISTRADOR")
    public List<LivroDeMovimentos.MovimentoDto> transferencia(LivroDeMovimentos.Transferencia corpo) {
        return livro.transferir(corpo);
    }
}
