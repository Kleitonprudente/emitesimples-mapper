package br.com.emitesimples.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.emitesimples.apoio.BaseApiTest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

/** Perfil padrão dos testes: SEM as propriedades do administrador inicial. Nada pode ser criado. */
@QuarkusTest
class AdminBootstrapSemConfiguracaoTest extends BaseApiTest {

    @Inject
    AdminBootstrap bootstrap;

    @Inject
    EntityManager em;

    @Test
    void semPropriedadesENenhumAdminNaoCriaNada() {
        dados.apagarAdministradoresDaPlataforma();
        long usuariosAntes = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(u) from UsuarioEntity u", Long.class).getSingleResult());
        assertEquals(AdminBootstrap.Resultado.SEM_CONFIGURACAO, bootstrap.executar());
        long usuariosDepois = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(u) from UsuarioEntity u", Long.class).getSingleResult());
        assertEquals(usuariosAntes, usuariosDepois);
    }
}
