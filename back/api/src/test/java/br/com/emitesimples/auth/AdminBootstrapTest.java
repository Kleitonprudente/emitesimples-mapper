package br.com.emitesimples.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/** Bootstrap do administrador inicial, com as propriedades definidas (perfil de teste próprio). */
@QuarkusTest
@TestProfile(AdminBootstrapTest.ComAdmin.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminBootstrapTest extends BaseApiTest {

    static final String EMAIL = "bootstrap.admin@teste.local";
    static final String SENHA_ADMIN = "Bootstrap-Senha-1";

    public static class ComAdmin implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("emitesimples.bootstrap.admin.email", EMAIL,
                    "emitesimples.bootstrap.admin.senha", SENHA_ADMIN);
        }
    }

    @Inject
    AdminBootstrap bootstrap;

    @Inject
    EntityManager em;

    private long admins() {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(u) from UsuarioEntity u where u.administradorPlataforma = true", Long.class).getSingleResult());
    }

    @Test
    @Order(1) // precisa rodar ANTES dos testes que apagam administradores
    void aoSubirComAsPropriedadesDefinidasExisteAdministrador() {
        // o StartupEvent já rodou ao iniciar a aplicação deste perfil: tem de haver administrador
        assertEquals(1, dados.contarUsuariosPorEmail(EMAIL), "o StartupEvent deveria ter criado o administrador configurado");
        System.out.println("[BOOTSTRAP] admin criado na subida com o e-mail configurado: " + (dados.contarUsuariosPorEmail(EMAIL) == 1));
    }

    @Test
    void criaUmaVezEEIdempotenteEComSenhaBcryptESemVinculo() {
        dados.apagarAdministradoresDaPlataforma();
        assertEquals(0, admins());

        assertEquals(AdminBootstrap.Resultado.CRIADO, bootstrap.executar());
        assertEquals(AdminBootstrap.Resultado.JA_EXISTE_ADMIN, bootstrap.executar());
        assertEquals(AdminBootstrap.Resultado.JA_EXISTE_ADMIN, bootstrap.executar());
        assertEquals(1, admins());
        assertEquals(1, dados.contarUsuariosPorEmail(EMAIL));

        UsuarioEntity u = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select u from UsuarioEntity u where lower(u.email) = :e", UsuarioEntity.class)
                .setParameter("e", EMAIL).getSingleResult());
        assertTrue(u.getAdministradorPlataforma());
        assertTrue(u.getAtivo());
        assertTrue(u.getSenhaHash().startsWith("$2"), "bcrypt");
        assertFalse(u.getSenhaHash().contains(SENHA_ADMIN));
        assertNotNull(u.getDataCriacao());
        long vinculos = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(v) from VinculoUsuarioEntity v where v.usuario.id = :id", Long.class)
                .setParameter("id", u.getId()).getSingleResult());
        assertEquals(0, vinculos, "o administrador inicial não tem vínculo com empresa");

        // e ele consegue entrar, sem empresa
        login(EMAIL, SENHA_ADMIN).then().statusCode(200)
                .body("plataformaAdmin", org.hamcrest.Matchers.equalTo(true))
                .body("empresa", org.hamcrest.Matchers.nullValue());
    }

    @Test
    void naoCriaSeJaExisteAdministradorMesmoComPropriedades() {
        dados.apagarAdministradoresDaPlataforma();
        dados.usuario(true, true); // outro administrador já existe
        assertEquals(AdminBootstrap.Resultado.JA_EXISTE_ADMIN, bootstrap.executar());
        assertEquals(0, dados.contarUsuariosPorEmail(EMAIL));
    }

    @Test
    void naoCriaSemEmailOuSemSenhaOuComSenhaFraca() {
        dados.apagarAdministradoresDaPlataforma();
        assertEquals(AdminBootstrap.Resultado.SEM_CONFIGURACAO, bootstrap.executar(null, null));
        assertEquals(AdminBootstrap.Resultado.SEM_CONFIGURACAO, bootstrap.executar("so.email@teste.local", null));
        assertEquals(AdminBootstrap.Resultado.SEM_CONFIGURACAO, bootstrap.executar("  ", "Qualquer-Senha-1"));
        assertEquals(AdminBootstrap.Resultado.SEM_CONFIGURACAO, bootstrap.executar("so.email@teste.local", "   "));
        assertEquals(AdminBootstrap.Resultado.CONFIGURACAO_INVALIDA, bootstrap.executar("fraca@teste.local", "curta"));
        assertEquals(AdminBootstrap.Resultado.CONFIGURACAO_INVALIDA, bootstrap.executar("longa@teste.local", "x".repeat(73)));
        assertEquals(0, admins());
        assertEquals(0, dados.contarUsuariosPorEmail("so.email@teste.local"));
        assertEquals(0, dados.contarUsuariosPorEmail("fraca@teste.local"));
    }

    @Test
    void emailJaUsadoPorUsuarioComumNaoViraAdministrador() {
        dados.apagarAdministradoresDaPlataforma();
        var comum = dados.usuario(true, false);
        assertEquals(AdminBootstrap.Resultado.EMAIL_EM_USO, bootstrap.executar(comum.email(), "Bootstrap-Senha-1"));
        assertFalse(dados.usuarioPorId(comum.id()).getAdministradorPlataforma(), "não pode promover usuário existente");
        assertEquals(0, admins());
    }
}
