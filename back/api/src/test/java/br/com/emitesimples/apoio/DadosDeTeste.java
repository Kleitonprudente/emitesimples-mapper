package br.com.emitesimples.apoio;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.auth.SenhaPolitica;
import br.com.emitesimples.cadastros.entity.ContatoEntity;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.usuarios.entity.TokenRenovacaoEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Monta os dados dos testes direto pelo EntityManager (sem passar pela API, que é o que se está provando). */
@ApplicationScoped
public class DadosDeTeste {

    public static final String SENHA = "Senha-Teste-123";

    public record Usr(String id, String email) {
    }

    @Inject
    EntityManager em;

    private static String digitos(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }

    public String municipio() {
        return QuarkusTransaction.requiringNew().call(() -> {
            MunicipioEntity m = MunicipioEntity.builderOf()
                    .withCodigoIbge(digitos(7)).withNome("Cidade " + UUID.randomUUID()).withUf("SP").build();
            em.persist(m);
            return m.getId();
        });
    }

    public String empresa(String situacao) {
        String municipioId = municipio();
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity e = EmpresaEntity.builderOf()
                    .withRazaoSocial("Empresa " + UUID.randomUUID().toString().substring(0, 8))
                    .withNomeFantasia("Fantasia")
                    .withDocumento(digitos(14))
                    .withAmbiente("homologacao")
                    .withRegimeTributario(1)
                    .withSituacao(situacao)
                    .withMunicipio(em.getReference(MunicipioEntity.class, municipioId))
                    .build();
            em.persist(e);
            return e.getId();
        });
    }

    public String empresaAtiva() {
        return empresa("ativa");
    }

    /** Usuário com senha {@link #SENHA}; e-mail único. */
    public Usr usuario(boolean ativo, boolean administradorPlataforma) {
        return usuario(ativo, administradorPlataforma, SENHA);
    }

    /** senha null = usuário sem senha definida (senhaHash nulo). */
    public Usr usuario(boolean ativo, boolean administradorPlataforma, String senha) {
        String email = "u" + UUID.randomUUID().toString().replace("-", "") + "@teste.local";
        return QuarkusTransaction.requiringNew().call(() -> {
            LocalDateTime agora = AuthService.agora();
            UsuarioEntity u = UsuarioEntity.builderOf()
                    .withNome("Usuario Teste").withEmail(email)
                    .withSenhaHash(senha == null ? null : SenhaPolitica.hash(senha))
                    .withAtivo(ativo).withAdministradorPlataforma(administradorPlataforma)
                    .withDataCriacao(agora).withDataAtualizacao(agora).build();
            em.persist(u);
            return new Usr(u.getId(), email);
        });
    }

    public void vincular(String usuarioId, String empresaId, String papel, boolean ativo) {
        QuarkusTransaction.requiringNew().run(() -> {
            LocalDateTime agora = AuthService.agora();
            VinculoUsuarioEntity v = VinculoUsuarioEntity.builderOf()
                    .withPapel(papel).withAtivo(ativo)
                    .withUsuario(em.getReference(UsuarioEntity.class, usuarioId))
                    .withEmpresa(em.getReference(EmpresaEntity.class, empresaId))
                    .withDataCriacao(agora).withDataAtualizacao(agora).build();
            em.persist(v);
        });
    }

    /** Usuário ativo com um vínculo ativo. */
    public Usr usuarioComPapel(String empresaId, String papel) {
        Usr u = usuario(true, false);
        vincular(u.id(), empresaId, papel, true);
        return u;
    }

    public String pessoa(String empresaId, String nome) {
        return QuarkusTransaction.requiringNew().call(() -> {
            PessoaEntity p = PessoaEntity.builderOf()
                    .withTipoPessoa("fisica").withNome(nome).withDocumento(digitos(11))
                    .withEmpresa(em.getReference(EmpresaEntity.class, empresaId)).build();
            em.persist(p);
            return p.getId();
        });
    }

    public String servico(String empresaId, String nome) {
        return QuarkusTransaction.requiringNew().call(() -> {
            ServicoEntity s = ServicoEntity.builderOf()
                    .withNome(nome).withAtivo(true).withIssRetido(false).withTipoOperacaoIss(1)
                    .withValorPadrao(new BigDecimal("10.00"))
                    .withEmpresa(em.getReference(EmpresaEntity.class, empresaId)).build();
            em.persist(s);
            return s.getId();
        });
    }

    public String contato(String empresaId, String pessoaId, String nome) {
        return QuarkusTransaction.requiringNew().call(() -> {
            ContatoEntity c = ContatoEntity.builderOf()
                    .withTipoContato("email").withNome(nome).withEmail("c@teste.local")
                    .withPessoa(em.getReference(PessoaEntity.class, pessoaId))
                    .withEmpresa(em.getReference(EmpresaEntity.class, empresaId)).build();
            em.persist(c);
            return c.getId();
        });
    }

    public String nota(String empresaId, String pessoaId, String servicoId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            NotaEntity n = NotaEntity.builderOf()
                    .withNumero(digitos(9)).withSerie("1").withAmbiente("homologacao").withEstado("rascunho")
                    .withDescricao("Nota de teste")
                    .withEmpresa(em.getReference(EmpresaEntity.class, empresaId))
                    .withPessoa(em.getReference(PessoaEntity.class, pessoaId))
                    .withServico(em.getReference(ServicoEntity.class, servicoId)).build();
            em.persist(n);
            return n.getId();
        });
    }

    // ------------------------------------------------------------------------------------------ leitura

    public List<TokenRenovacaoEntity> tokensDe(String usuarioId, String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select t from TokenRenovacaoEntity t where t.usuario.id = :u and t.empresa.id = :e order by t.dataCriacao, t.id",
                TokenRenovacaoEntity.class).setParameter("u", usuarioId).setParameter("e", empresaId).getResultList());
    }

    public TokenRenovacaoEntity tokenPorHash(String hash) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select t from TokenRenovacaoEntity t where t.tokenHash = :h", TokenRenovacaoEntity.class)
                .setParameter("h", hash).getSingleResult());
    }

    public UsuarioEntity usuarioPorId(String id) {
        return QuarkusTransaction.requiringNew().call(() -> em.find(UsuarioEntity.class, id));
    }

    public void expirarToken(String hash) {
        QuarkusTransaction.requiringNew().run(() -> em.createQuery(
                "update TokenRenovacaoEntity t set t.dataExpiracao = :d where t.tokenHash = :h")
                .setParameter("d", AuthService.agora().minusMinutes(1)).setParameter("h", hash).executeUpdate());
    }

    public void anularSenha(String usuarioId) {
        QuarkusTransaction.requiringNew().run(() -> em.createQuery(
                "update UsuarioEntity u set u.senhaHash = null where u.id = :id").setParameter("id", usuarioId).executeUpdate());
    }

    public long contarUsuariosPorEmail(String email) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select count(u) from UsuarioEntity u where lower(u.email) = :e", Long.class)
                .setParameter("e", email.toLowerCase()).getSingleResult());
    }

    /** Apaga todos os administradores da plataforma (e o que os referencia) para testar o bootstrap do zero. */
    public void apagarAdministradoresDaPlataforma() {
        QuarkusTransaction.requiringNew().run(() -> {
            em.createQuery("delete from TokenRenovacaoEntity t where t.usuario.administradorPlataforma = true").executeUpdate();
            em.createQuery("delete from VinculoUsuarioEntity v where v.usuario.administradorPlataforma = true").executeUpdate();
            em.createQuery("delete from UsuarioEntity u where u.administradorPlataforma = true").executeUpdate();
        });
    }
}
