package br.com.emitesimples.security;

import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.tenant.EscopoTenant;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Emite o JWT de acesso (RS256, issuer emitesimples). A chave privada vem de smallrye.jwt.sign.key[.location]. */
@ApplicationScoped
public class JwtEmissor {

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "emitesimples.auth.access-token-minutos", defaultValue = "15")
    long minutos;

    public long expiresInSegundos() {
        return Duration.ofMinutes(minutos).toSeconds();
    }

    /**
     * @param empresa empresa da sessão (null só para o administrador da plataforma sem empresa)
     * @param papel   papel no vínculo com a empresa (null junto com a empresa)
     */
    public String emitir(UsuarioEntity usuario, EmpresaEntity empresa, String papel, boolean plataformaAdmin) {
        return emitir(usuario, empresa, papel, plataformaAdmin, null);
    }

    /** Claims do token de agente (docs/financeiro/README.md, 9.4, B3): o cliente de acesso e os escopos dados a ele. */
    public static final String CLAIM_CLIENTE_ACESSO = "cliente_acesso";
    public static final String CLAIM_ESCOPOS = "escopos";

    /**
     * Token de acesso de um AGENTE: sub = usuario dono (por quem o agente age), grupo AGENTE (nunca o papel do usuario), a empresa do
     * cliente de acesso, o id do cliente e os escopos (consultar, preparar). Sem refresh: o agente troca o segredo de novo.
     */
    public String emitirAgente(UsuarioEntity usuario, EmpresaEntity empresa, String idClienteAcesso, java.util.List<String> escopos) {
        return Jwt.issuer(issuer)
                .subject(usuario.getId())
                .upn(usuario.getEmail())
                .groups(Set.of(Papeis.AGENTE))
                .claim(EscopoTenant.CLAIM_EMPRESA, empresa.getId())
                .claim(CLAIM_CLIENTE_ACESSO, idClienteAcesso)
                .claim(CLAIM_ESCOPOS, String.join(" ", escopos))
                .expiresIn(Duration.ofMinutes(minutos))
                .sign();
    }

    /** Claim com o instante (epoch, segundos) ate o qual a senha foi confirmada (docs/financeiro/README.md, 9.2, A7). */
    public static final String CLAIM_SENHA_RECENTE = "senha_recente_ate";

    /** @param senhaRecenteAte epoch em segundos ate quando vale a senha recente, ou null (token comum) */
    public String emitir(UsuarioEntity usuario, EmpresaEntity empresa, String papel, boolean plataformaAdmin, Long senhaRecenteAte) {
        Set<String> grupos = new LinkedHashSet<>();
        if (papel != null) {
            grupos.add(papel);
        }
        if (plataformaAdmin) {
            grupos.add(Papeis.PLATAFORMA_ADMIN);
        }
        var b = Jwt.issuer(issuer)
                .subject(usuario.getId())
                .upn(usuario.getEmail())
                .groups(grupos)
                .expiresIn(Duration.ofMinutes(minutos));
        if (empresa != null) {
            b.claim(EscopoTenant.CLAIM_EMPRESA, empresa.getId());
        }
        if (plataformaAdmin) {
            b.claim(EscopoTenant.CLAIM_ADMIN_PLATAFORMA, true);
        }
        if (senhaRecenteAte != null) {
            b.claim(CLAIM_SENHA_RECENTE, senhaRecenteAte);
        }
        return b.sign();
    }
}
