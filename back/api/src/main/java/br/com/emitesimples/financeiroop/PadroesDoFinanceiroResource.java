package br.com.emitesimples.financeiroop;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

/**
 * POST /api/financeiro/padroes: garante os padroes iniciais do financeiro da empresa da sessao (F1-H6). Idempotente: a segunda chamada
 * nao cria nada e devolve {@code criados: []}. Administrador ou operador da empresa (regra de caminho no application.properties e
 * {@code @RolesAllowed} como cinto extra).
 */
@Path("/financeiro/padroes")
@Produces(MediaType.APPLICATION_JSON)
public class PadroesDoFinanceiroResource {

    @Inject
    PadroesDoFinanceiro padroes;

    @POST
    @RolesAllowed({"ADMINISTRADOR", "OPERADOR"})
    public Map<String, Object> garantir() {
        return PadroesDoFinanceiro.comoMapa(padroes.garantir());
    }
}
