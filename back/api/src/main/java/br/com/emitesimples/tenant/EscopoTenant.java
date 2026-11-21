package br.com.emitesimples.tenant;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Escopo de tenant das consultas geradas: cada empresa só enxerga os próprios dados.
 *
 * O repositório gerado (quarkus-repository.mi) chama {@code irrestrito()} e {@code exigir(coluna)}.
 *
 * PROVISÓRIO: os nomes das claims abaixo ainda não foram decididos pelo usuário. A implementação é FECHADA
 * por padrão: sem a claim da empresa, ou sem a claim de administrador da plataforma, nenhum dado é liberado.
 * Revisar junto com a emissão dos JWT (fase 1: front-end e agentes próprios).
 */
@RequestScoped
public class EscopoTenant {

    /** Claim com o id da empresa (tenant) do token. Nome provisório. */
    public static final String CLAIM_EMPRESA = "empresa_id";

    /** Claim que marca o administrador da plataforma (modo irrestrito, ex.: cadastrar empresas). Nome provisório. */
    public static final String CLAIM_ADMIN_PLATAFORMA = "plataforma_admin";

    @Inject
    JsonWebToken jwt;

    /** Verdadeiro só para o administrador da plataforma; qualquer outro caso é escopado. */
    public boolean irrestrito() {
        if (jwt == null) {
            return false;
        }
        Object valor = jwt.getClaim(CLAIM_ADMIN_PLATAFORMA);
        return valor != null && "true".equalsIgnoreCase(String.valueOf(valor));
    }

    /** Empresa da claim, ou null se ausente/vazia (nao lanca). Usado pela RLS (ContextoTenantBanco). */
    public String empresaIdOuNulo() {
        Object valor = jwt == null ? null : jwt.getClaim(CLAIM_EMPRESA);
        return valor == null || String.valueOf(valor).isBlank() ? null : String.valueOf(valor);
    }

    /**
     * Devolve o id da empresa do token, para filtrar a coluna indicada ({@code empresa_id} nas entidades da
     * empresa, {@code id} na própria Empresa). Falha fechada: sem a claim, nega o acesso.
     */
    public String exigir(String coluna) {
        Object valor = jwt == null ? null : jwt.getClaim(CLAIM_EMPRESA);
        if (valor == null || String.valueOf(valor).isBlank()) {
            throw new ForbiddenException("Token sem a empresa (claim " + CLAIM_EMPRESA + "), acesso negado.");
        }
        return String.valueOf(valor);
    }
}
