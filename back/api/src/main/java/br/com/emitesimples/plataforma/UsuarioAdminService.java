package br.com.emitesimples.plataforma;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.auth.ForcaBruta;
import br.com.emitesimples.auth.SenhaPolitica;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.plataforma.PlataformaDtos.EmpresaRef;
import br.com.emitesimples.plataforma.PlataformaDtos.UsuarioAdmin;
import br.com.emitesimples.plataforma.PlataformaDtos.UsuarioEdicao;
import br.com.emitesimples.plataforma.PlataformaDtos.UsuarioNovo;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoAdmin;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoAlteracao;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoNovo;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * Gerenciamento de usuarios e vinculos pelo administrador da plataforma. Escrito a mao.
 *
 * Roda com o escopo do PROPRIO token: o administrador da plataforma tem {@code plataforma_admin=true}, que a RLS
 * (RlsPoolInterceptor) traduz em irrestrito. Por isso nao usa {@code comoSistema}: se um dia alguem sem a claim
 * chegasse aqui, o banco ainda barraria VINCULOUSUARIO e TOKENRENOVACAO. Nunca devolve nem registra senha ou hash.
 */
@ApplicationScoped
public class UsuarioAdminService {

    private static final Logger LOG = Logger.getLogger(UsuarioAdminService.class);

    public static final Set<String> PAPEIS = Set.of("administrador", "operador", "consulta");
    static final int MAX_PAGINA = 100;

    @Inject
    EntityManager em;

    @Inject
    ForcaBruta forcaBruta;

    // ------------------------------------------------------------------------------------------- consulta

    public List<UsuarioAdmin> listar(String busca, Boolean ativo, int from, int to) {
        if (from < 0 || from > to) {
            throw new ApiException(400, "requisicao_invalida", "from deve ser >= 0 e menor ou igual a to.");
        }
        if (to - from + 1 > MAX_PAGINA) {
            throw new ApiException(400, "requisicao_invalida", "No máximo " + MAX_PAGINA + " usuários por página.");
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            TypedQuery<UsuarioEntity> q = em.createQuery("select u from UsuarioEntity u" + filtro(busca, ativo)
                    + " order by lower(u.nome), u.id", UsuarioEntity.class);
            parametros(q, busca, ativo);
            return q.setFirstResult(from).setMaxResults(to - from + 1).getResultList().stream()
                    .map(u -> paraAdmin(u, null)).toList();
        });
    }

    public long contar(String busca, Boolean ativo) {
        return QuarkusTransaction.requiringNew().call(() -> {
            TypedQuery<Long> q = em.createQuery("select count(u) from UsuarioEntity u" + filtro(busca, ativo), Long.class);
            parametros(q, busca, ativo);
            return q.getSingleResult();
        });
    }

    public UsuarioAdmin obter(String id) {
        return QuarkusTransaction.requiringNew().call(() -> paraAdmin(usuario(id), null));
    }

    public List<VinculoAdmin> vinculos(String usuarioId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            usuario(usuarioId);
            return vinculosDe(usuarioId);
        });
    }

    // ---------------------------------------------------------------------------------------------- escrita

    public UsuarioAdmin criar(UsuarioNovo n) {
        String email = AuthService.normalizarEmail(n.email());
        validarSenha(n.senha());
        String nome = n.nome().trim();
        if (nome.isEmpty()) {
            throw campoInvalido("nome", "O nome é obrigatório.");
        }
        List<VinculoNovo> vinculos = n.vinculos() == null ? List.of() : n.vinculos();
        Set<String> empresas = new HashSet<>();
        for (VinculoNovo v : vinculos) {
            validarPapel(v.papel());
            if (!empresas.add(v.empresaId().trim())) {
                throw new ApiException(400, "vinculos_duplicados", "A mesma empresa aparece mais de uma vez em vinculos.");
            }
        }
        String hash = SenhaPolitica.hash(n.senha());
        return QuarkusTransaction.requiringNew().call(() -> {
            exigirEmailLivre(email);
            LocalDateTime agora = AuthService.agora();
            UsuarioEntity u = UsuarioEntity.builderOf()
                    .withNome(nome)
                    .withEmail(email)
                    .withWhatsapp(vazioParaNulo(n.whatsapp()))
                    .withSenhaHash(hash)
                    .withAtivo(n.ativo() == null || n.ativo())
                    .withAdministradorPlataforma(false)
                    .withDataCriacao(agora).withDataAtualizacao(agora)
                    .build();
            gravar(() -> em.persist(u));
            for (VinculoNovo v : vinculos) {
                persistirVinculo(u, v.empresaId().trim(), v.papel(), v.ativo() == null || v.ativo(), agora);
            }
            gravar(() -> em.flush());
            LOG.infof("Usuário criado pelo administrador da plataforma (id=%s).", u.getId());
            return paraAdmin(u, vinculosDe(u.getId()));
        });
    }

    /** @param solicitanteId id do administrador que chama (nao pode desativar a si mesmo) */
    public UsuarioAdmin editar(String id, UsuarioEdicao e, String solicitanteId) {
        if (e.nome() != null && e.nome().isBlank()) {
            throw campoInvalido("nome", "O nome não pode ficar vazio.");
        }
        if (Boolean.FALSE.equals(e.ativo()) && id.equals(solicitanteId)) {
            throw new ApiException(400, "auto_desativacao", "Você não pode desativar o seu próprio usuário.");
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            UsuarioEntity u = usuario(id);
            if (e.nome() != null) {
                u.setNome(e.nome().trim());
            }
            if (e.whatsapp() != null) {
                u.setWhatsapp(vazioParaNulo(e.whatsapp()));
            }
            if (e.ativo() != null) {
                u.setAtivo(e.ativo());
            }
            u.setDataAtualizacao(AuthService.agora());
            em.flush();
            return paraAdmin(u, null);
        });
    }

    /** Senha temporaria definida pelo admin: troca o hash e revoga TODOS os refresh tokens do usuario (como a troca de senha). */
    public void redefinirSenha(String id, String senha) {
        validarSenha(senha);
        String hash = SenhaPolitica.hash(senha);
        String email = QuarkusTransaction.requiringNew().call(() -> {
            UsuarioEntity u = usuario(id);
            LocalDateTime agora = AuthService.agora();
            u.setSenhaHash(hash);
            u.setDataAtualizacao(agora);
            em.createQuery("update TokenRenovacaoEntity t set t.revogado = true, t.dataRevogacao = :agora "
                            + "where t.usuario.id = :uid and (t.revogado is null or t.revogado = false)")
                    .setParameter("agora", agora)
                    .setParameter("uid", id)
                    .executeUpdate();
            return AuthService.normalizarEmail(u.getEmail());
        });
        forcaBruta.registrarSucesso(email); // o admin que redefine a senha tambem destrava um bloqueio por tentativas
        LOG.infof("Senha do usuário redefinida pelo administrador da plataforma (id=%s).", id);
    }

    public VinculoAdmin criarVinculo(String usuarioId, VinculoNovo v) {
        validarPapel(v.papel());
        return QuarkusTransaction.requiringNew().call(() -> {
            UsuarioEntity u = usuario(usuarioId);
            String empresaId = v.empresaId().trim();
            Long existe = em.createQuery("select count(x) from VinculoUsuarioEntity x where x.usuario.id = :u and x.empresa.id = :e",
                    Long.class).setParameter("u", usuarioId).setParameter("e", empresaId).getSingleResult();
            if (existe > 0) {
                throw new ApiException(409, "vinculo_ja_existe",
                        "O usuário já tem vínculo com esta empresa. Altere o vínculo existente (papel ou ativo).");
            }
            VinculoUsuarioEntity nv = persistirVinculo(u, empresaId, v.papel(), v.ativo() == null || v.ativo(), AuthService.agora());
            gravar(() -> em.flush());
            return paraAdmin(nv);
        });
    }

    public VinculoAdmin alterarVinculo(String usuarioId, String vinculoId, VinculoAlteracao a) {
        if (a.papel() != null) {
            validarPapel(a.papel());
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            usuario(usuarioId);
            VinculoUsuarioEntity v = em.createQuery("select v from VinculoUsuarioEntity v join fetch v.empresa "
                            + "where v.id = :id and v.usuario.id = :u", VinculoUsuarioEntity.class)
                    .setParameter("id", vinculoId).setParameter("u", usuarioId)
                    .getResultStream().findFirst()
                    .orElseThrow(() -> new ApiException(404, "vinculo_nao_encontrado", "Vínculo não encontrado."));
            LocalDateTime agora = AuthService.agora();
            boolean desativando = Boolean.FALSE.equals(a.ativo()) && !Boolean.FALSE.equals(v.getAtivo());
            if (a.papel() != null) {
                v.setPapel(a.papel());
            }
            if (a.ativo() != null) {
                v.setAtivo(a.ativo());
            }
            v.setDataAtualizacao(agora);
            if (desativando) {
                // a sessao daquele usuario naquela empresa deixa de poder renovar (o refresh ja recusa vinculo inativo; isto fecha tambem o token)
                em.createQuery("update TokenRenovacaoEntity t set t.revogado = true, t.dataRevogacao = :agora "
                                + "where t.usuario.id = :u and t.empresa.id = :e and (t.revogado is null or t.revogado = false)")
                        .setParameter("agora", agora).setParameter("u", usuarioId)
                        .setParameter("e", v.getEmpresa().getId()).executeUpdate();
            }
            em.flush();
            return paraAdmin(v);
        });
    }

    // --------------------------------------------------------------------------------------------- internos

    private UsuarioEntity usuario(String id) {
        UsuarioEntity u = em.find(UsuarioEntity.class, id);
        if (u == null) {
            throw new ApiException(404, "usuario_nao_encontrado", "Usuário não encontrado.");
        }
        return u;
    }

    private VinculoUsuarioEntity persistirVinculo(UsuarioEntity u, String empresaId, String papel, boolean ativo, LocalDateTime agora) {
        EmpresaEntity e = em.find(EmpresaEntity.class, empresaId);
        if (e == null) {
            throw new ApiException(404, "empresa_nao_encontrada", "Empresa não encontrada.");
        }
        VinculoUsuarioEntity v = VinculoUsuarioEntity.builderOf()
                .withUsuario(u).withEmpresa(e).withPapel(papel).withAtivo(ativo)
                .withDataCriacao(agora).withDataAtualizacao(agora).build();
        gravar(() -> em.persist(v));
        return v;
    }

    private List<VinculoAdmin> vinculosDe(String usuarioId) {
        return em.createQuery("select v from VinculoUsuarioEntity v join fetch v.empresa e where v.usuario.id = :u "
                        + "order by lower(e.razaoSocial), v.id", VinculoUsuarioEntity.class)
                .setParameter("u", usuarioId).getResultList().stream().map(UsuarioAdminService::paraAdmin).toList();
    }

    private void exigirEmailLivre(String email) {
        long n = em.createQuery("select count(u) from UsuarioEntity u where lower(u.email) = :e", Long.class)
                .setParameter("e", email).getSingleResult();
        if (n > 0) {
            throw emailJaCadastrado();
        }
    }

    /** Roda a gravacao e traduz violacao de unicidade (corrida entre dois cadastros) em 409. */
    private static void gravar(Runnable r) {
        try {
            r.run();
        } catch (PersistenceException e) {
            if (violacaoUnica(e)) {
                throw emailJaCadastrado();
            }
            throw e;
        }
    }

    private static boolean violacaoUnica(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof java.sql.SQLException s && "23505".equals(s.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static ApiException emailJaCadastrado() {
        return new ApiException(409, "email_ja_cadastrado", "Já existe um usuário com este e-mail.");
    }

    private static void validarPapel(String papel) {
        if (papel == null || !PAPEIS.contains(papel)) {
            throw new ApiException(400, "papel_invalido", "Papel inválido. Use administrador, operador ou consulta.",
                    Map.of("campos", List.of(Map.of("campo", "papel", "mensagem", "Use administrador, operador ou consulta."))));
        }
    }

    /** Mesma politica do login, mas o campo do erro e "senha" (nome do campo desta API). */
    private static void validarSenha(String senha) {
        try {
            SenhaPolitica.validarNova(senha);
        } catch (ApiException e) {
            throw new ApiException(400, "senha_invalida", e.getMessage(),
                    Map.of("campos", List.of(Map.of("campo", "senha", "mensagem", e.getMessage()))));
        }
    }

    private static ApiException campoInvalido(String campo, String msg) {
        return new ApiException(400, "requisicao_invalida", "Há campos inválidos na requisição.",
                Map.of("campos", List.of(Map.of("campo", campo, "mensagem", msg))));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String filtro(String busca, Boolean ativo) {
        StringBuilder sb = new StringBuilder();
        String e = " where ";
        if (busca != null && !busca.isBlank()) {
            sb.append(e).append("(lower(function('unaccent', u.nome)) like :b escape '\\' or lower(u.email) like :b escape '\\')");
            e = " and ";
        }
        if (ativo != null) {
            sb.append(e).append("u.ativo = :ativo");
        }
        return sb.toString();
    }

    private static void parametros(TypedQuery<?> q, String busca, Boolean ativo) {
        if (busca != null && !busca.isBlank()) {
            String semAcento = Normalizer.normalize(busca.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            q.setParameter("b", "%" + semAcento + "%");
        }
        if (ativo != null) {
            q.setParameter("ativo", ativo);
        }
    }

    private static UsuarioAdmin paraAdmin(UsuarioEntity u, List<VinculoAdmin> vinculos) {
        return new UsuarioAdmin(u.getId(), u.getNome(), u.getEmail(), u.getWhatsapp(), u.getAtivo(),
                u.getAdministradorPlataforma(), u.getSenhaHash() != null, u.getDataUltimoAcesso(), u.getDataCriacao(),
                u.getDataAtualizacao(), vinculos);
    }

    private static VinculoAdmin paraAdmin(VinculoUsuarioEntity v) {
        EmpresaEntity e = v.getEmpresa();
        return new VinculoAdmin(v.getId(), v.getUsuario().getId(),
                new EmpresaRef(e.getId(), e.getRazaoSocial(), e.getNomeFantasia(), e.getSituacao()),
                v.getPapel(), v.getAtivo(), v.getDataCriacao(), v.getDataAtualizacao());
    }
}
