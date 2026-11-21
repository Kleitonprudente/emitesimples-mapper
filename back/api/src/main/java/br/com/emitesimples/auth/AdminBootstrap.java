package br.com.emitesimples.auth;

import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Cria o administrador inicial da plataforma ao subir, se NÃO existir nenhum e as propriedades
 * emitesimples.bootstrap.admin.email/senha (EMITESIMPLES_ADMIN_EMAIL / EMITESIMPLES_ADMIN_SENHA) estiverem definidas.
 * Idempotente. Nunca registra a senha. Sem vínculo com empresa.
 */
@ApplicationScoped
public class AdminBootstrap {

    private static final Logger LOG = Logger.getLogger(AdminBootstrap.class);

    public enum Resultado { CRIADO, JA_EXISTE_ADMIN, SEM_CONFIGURACAO, CONFIGURACAO_INVALIDA, EMAIL_EM_USO }

    @Inject
    EntityManager em;

    @ConfigProperty(name = "emitesimples.bootstrap.admin.email")
    Optional<String> email;

    @ConfigProperty(name = "emitesimples.bootstrap.admin.senha")
    Optional<String> senha;

    void aoSubir(@Observes StartupEvent ev) {
        executar();
    }

    /** Usa as propriedades configuradas. */
    public Resultado executar() {
        return executar(email.orElse(null), senha.orElse(null));
    }

    public Resultado executar(String emailAdmin, String senhaAdmin) {
        boolean temEmail = emailAdmin != null && !emailAdmin.isBlank();
        boolean temSenha = senhaAdmin != null && !senhaAdmin.isBlank();
        return QuarkusTransaction.requiringNew().call(() -> {
            long admins = em.createQuery(
                    "select count(u) from UsuarioEntity u where u.administradorPlataforma = true", Long.class)
                    .getSingleResult();
            if (admins > 0) {
                return Resultado.JA_EXISTE_ADMIN;
            }
            if (!temEmail || !temSenha) {
                LOG.warn("Nenhum administrador da plataforma existe e EMITESIMPLES_ADMIN_EMAIL / EMITESIMPLES_ADMIN_SENHA "
                        + "não estão definidas: ninguém poderá administrar a plataforma. Defina as duas variáveis e reinicie.");
                return Resultado.SEM_CONFIGURACAO;
            }
            String e = AuthService.normalizarEmail(emailAdmin);
            try {
                SenhaPolitica.validarNova(senhaAdmin);
            } catch (RuntimeException ex) {
                LOG.warn("O administrador inicial não foi criado: a senha de EMITESIMPLES_ADMIN_SENHA não atende à política "
                        + "(" + SenhaPolitica.MIN_BYTES + " a " + SenhaPolitica.MAX_BYTES + " bytes).");
                return Resultado.CONFIGURACAO_INVALIDA;
            }
            long existente = em.createQuery("select count(u) from UsuarioEntity u where lower(u.email) = :e", Long.class)
                    .setParameter("e", e).getSingleResult();
            if (existente > 0) {
                LOG.warn("O administrador inicial não foi criado: já existe um usuário com o e-mail configurado "
                        + "(que não é administrador da plataforma).");
                return Resultado.EMAIL_EM_USO;
            }
            LocalDateTime agora = AuthService.agora();
            UsuarioEntity u = UsuarioEntity.builderOf()
                    .withNome("Administrador da plataforma")
                    .withEmail(e)
                    .withSenhaHash(SenhaPolitica.hash(senhaAdmin))
                    .withAtivo(true)
                    .withAdministradorPlataforma(true)
                    .withDataCriacao(agora)
                    .withDataAtualizacao(agora)
                    .build();
            em.persist(u);
            LOG.info("Administrador inicial da plataforma criado (" + e + ").");
            return Resultado.CRIADO;
        });
    }
}
