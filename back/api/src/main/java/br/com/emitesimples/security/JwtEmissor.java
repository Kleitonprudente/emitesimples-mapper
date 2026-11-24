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
        return b.sign();
    }
}
