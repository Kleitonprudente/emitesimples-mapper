package br.com.emitesimples.auth;

import br.com.emitesimples.auth.Dtos.Emitida;
import br.com.emitesimples.auth.Dtos.EmpresaOpcao;
import br.com.emitesimples.auth.Dtos.EmpresaResumo;
import br.com.emitesimples.auth.Dtos.LoginRequest;
import br.com.emitesimples.auth.Dtos.Perfil;
import br.com.emitesimples.auth.Dtos.Sessao;
import br.com.emitesimples.auth.Dtos.UsuarioResumo;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.security.JwtEmissor;
import br.com.emitesimples.usuarios.entity.TokenRenovacaoEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Regras de login, renovação, logout, perfil e troca de senha. Escrito à mão (o código gerado não cobre isso).
 *
 * Nunca registra senha, token nem hash em log. As consultas usam o EntityManager direto, e NÃO os repositórios
 * gerados, porque estes filtram pelo JWT da requisição (que no login ainda não existe).
 */
@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    public static final String SITUACAO_ATIVA = "ativa";
    private static final String MSG_CREDENCIAIS = "E-mail ou senha inválidos.";
    private static final String MSG_SESSAO = "Sessão inválida ou expirada. Entre novamente.";

    /** Hash de uma senha aleatória: gasta o mesmo tempo de bcrypt quando o usuário não existe. */
    private static final String HASH_DE_MENTIRA = SenhaPolitica.hash("mentira-" + UUID.randomUUID());

    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Inject
    EntityManager em;

    @Inject
    JwtEmissor jwt;

    @Inject
    ForcaBruta forcaBruta;

    /** Login, refresh, logout e senha leem Vinculo, Token e Empresa ANTES de haver empresa na sessao: rodam como sistema (RLS). */
    @Inject
    br.com.emitesimples.tenant.ContextoTenantBanco contexto;

    @ConfigProperty(name = "emitesimples.auth.refresh-token-dias", defaultValue = "7")
    long refreshDias;

    /** Resultado de uma transação que precisa COMMITAR mesmo quando a resposta é um erro (ex.: revogação por reuso). */
    private record Tx<T>(T valor, ApiException erro) {
        static <T> Tx<T> ok(T v) {
            return new Tx<>(v, null);
        }

        static <T> Tx<T> falha(ApiException e) {
            return new Tx<>(null, e);
        }

        T obter() {
            if (erro != null) {
                throw erro;
            }
            return valor;
        }
    }

    public static LocalDateTime agora() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------------------------------------ login

    public Emitida login(LoginRequest req) {
        String email = normalizarEmail(req.email());
        forcaBruta.verificar(email);
        try {
            Emitida s = contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> loginNaTransacao(email, req)));
            forcaBruta.registrarSucesso(email);
            return s;
        } catch (ApiException e) {
            if (e.getStatus() == 401) {
                forcaBruta.registrarFalha(email);
            }
            throw e;
        }
    }

    private Emitida loginNaTransacao(String email, LoginRequest req) {
        UsuarioEntity u = buscarUsuarioPorEmail(email);
        String hash = (u == null || u.getSenhaHash() == null) ? HASH_DE_MENTIRA : u.getSenhaHash();
        boolean senhaConfere = SenhaPolitica.bytes(req.senha()) <= SenhaPolitica.MAX_BYTES
                && SenhaPolitica.confere(req.senha(), hash);
        if (u == null || u.getSenhaHash() == null || !senhaConfere || !Boolean.TRUE.equals(u.getAtivo())) {
            throw credenciaisInvalidas();
        }
        boolean admin = Boolean.TRUE.equals(u.getAdministradorPlataforma());
        List<VinculoUsuarioEntity> vinculos = vinculosAtivos(u.getId());

        VinculoUsuarioEntity escolhido = null;
        String pedida = req.empresaId() == null || req.empresaId().isBlank() ? null : req.empresaId().trim();
        if (pedida != null) {
            escolhido = vinculos.stream().filter(v -> v.getEmpresa().getId().equals(pedida)).findFirst()
                    .orElseThrow(AuthService::credenciaisInvalidas);
        } else if (vinculos.size() == 1) {
            escolhido = vinculos.get(0);
        } else if (vinculos.size() > 1 && !admin) {
            List<EmpresaOpcao> opcoes = vinculos.stream()
                    .map(v -> new EmpresaOpcao(v.getEmpresa().getId(), v.getEmpresa().getRazaoSocial(), v.getPapel()))
                    .toList();
            throw new ApiException(409, "empresa_obrigatoria",
                    "Você tem acesso a mais de uma empresa. Informe empresaId.", Map.of("empresas", opcoes));
        } else if (vinculos.isEmpty() && !admin) {
            throw credenciaisInvalidas();
        }
        // administrador da plataforma sem vínculo único e sem empresaId: entra sem empresa

        u.setDataUltimoAcesso(agora());
        u.setDataAtualizacao(agora());
        return montarSessao(u, escolhido == null ? null : escolhido.getEmpresa(),
                escolhido == null ? null : escolhido.getPapel(), admin);
    }

    // ---------------------------------------------------------------------------------------------- refresh

    public Emitida refresh(String refreshToken) {
        String hash = hashDoToken(refreshToken);
        return contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> refreshNaTransacao(hash))).obter();
    }

    private Tx<Emitida> refreshNaTransacao(String hash) {
        TokenRenovacaoEntity t = buscarTokenComTrava(hash);
        if (t == null) {
            return Tx.falha(sessaoInvalida());
        }
        UsuarioEntity u = t.getUsuario();
        EmpresaEntity e = t.getEmpresa();
        LocalDateTime agora = agora();
        if (Boolean.TRUE.equals(t.getUsado()) || Boolean.TRUE.equals(t.getRevogado())) {
            // REUSO: o token já foi trocado (ou revogado) e reapareceu, logo pode ter vazado. Derruba a sessão inteira.
            int n = revogarAtivos(u.getId(), e.getId(), agora);
            LOG.warnf("Reuso de refresh token detectado (usuario=%s, empresa=%s); %d token(s) ativo(s) revogado(s).",
                    u.getId(), e.getId(), n);
            return Tx.falha(sessaoInvalida());
        }
        if (t.getDataExpiracao() == null || !t.getDataExpiracao().isAfter(agora)) {
            return Tx.falha(sessaoInvalida());
        }
        VinculoUsuarioEntity v = vinculoAtivo(u.getId(), e.getId());
        if (!Boolean.TRUE.equals(u.getAtivo()) || !SITUACAO_ATIVA.equals(e.getSituacao()) || v == null) {
            // usuário/empresa/vínculo deixou de valer desde o login: encerra este token
            t.setRevogado(true);
            t.setDataRevogacao(agora);
            return Tx.falha(sessaoInvalida());
        }
        t.setUsado(true);
        t.setDataUso(agora);
        t.setDataAtualizacao(agora);
        boolean admin = Boolean.TRUE.equals(u.getAdministradorPlataforma());
        return Tx.ok(montarSessao(u, e, v.getPapel(), admin));
    }

    // ----------------------------------------------------------------------------------------------- logout

    /** Idempotente: token desconhecido ou já revogado não é erro. */
    public void logout(String refreshToken) {
        String hash = hashDoToken(refreshToken);
        contexto.rodarComoSistema(() -> QuarkusTransaction.requiringNew().run(() -> {
            TokenRenovacaoEntity t = buscarTokenComTrava(hash);
            if (t != null && !Boolean.TRUE.equals(t.getRevogado())) {
                t.setRevogado(true);
                t.setDataRevogacao(agora());
                t.setDataAtualizacao(agora());
            }
        }));
    }

    // -------------------------------------------------------------------------------------------------- me

    public Perfil me(String usuarioId, String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            UsuarioEntity u = em.find(UsuarioEntity.class, usuarioId);
            if (u == null || !Boolean.TRUE.equals(u.getAtivo())) {
                throw new ApiException(401, "nao_autenticado", MSG_SESSAO);
            }
            EmpresaEntity e = null;
            String papel = null;
            if (empresaId != null) {
                VinculoUsuarioEntity v = vinculoAtivo(usuarioId, empresaId);
                if (v == null) {
                    throw new ApiException(401, "nao_autenticado", MSG_SESSAO);
                }
                e = v.getEmpresa();
                papel = v.getPapel();
            }
            return new Perfil(resumo(u), resumo(e), papel, Boolean.TRUE.equals(u.getAdministradorPlataforma()));
        });
    }

    // ------------------------------------------------------------------------------------------------ senha

    public void trocarSenha(String usuarioId, String senhaAtual, String novaSenha) {
        SenhaPolitica.validarNova(novaSenha);
        String chaveForca = QuarkusTransaction.requiringNew().call(() -> {
            UsuarioEntity u = em.find(UsuarioEntity.class, usuarioId);
            return u == null ? null : normalizarEmail(u.getEmail());
        });
        if (chaveForca == null) {
            throw new ApiException(401, "nao_autenticado", MSG_SESSAO);
        }
        forcaBruta.verificar(chaveForca);
        try {
            contexto.rodarComoSistema(() -> QuarkusTransaction.requiringNew().run(() -> trocarSenhaNaTransacao(usuarioId, senhaAtual, novaSenha)));
            forcaBruta.registrarSucesso(chaveForca);
        } catch (ApiException e) {
            if ("senha_atual_invalida".equals(e.getCodigo())) {
                forcaBruta.registrarFalha(chaveForca);
            }
            throw e;
        }
    }

    private void trocarSenhaNaTransacao(String usuarioId, String senhaAtual, String novaSenha) {
        UsuarioEntity u = em.find(UsuarioEntity.class, usuarioId);
        if (u == null || !Boolean.TRUE.equals(u.getAtivo())) {
            throw new ApiException(401, "nao_autenticado", MSG_SESSAO);
        }
        if (u.getSenhaHash() != null) {
            boolean confere = senhaAtual != null && SenhaPolitica.bytes(senhaAtual) <= SenhaPolitica.MAX_BYTES
                    && SenhaPolitica.confere(senhaAtual, u.getSenhaHash());
            if (!confere) {
                throw new ApiException(403, "senha_atual_invalida", "A senha atual está incorreta.");
            }
            if (SenhaPolitica.confere(novaSenha, u.getSenhaHash())) {
                throw new ApiException(400, "senha_invalida", "A nova senha deve ser diferente da atual.",
                        Map.of("campos", List.of(Map.of("campo", "novaSenha",
                                "mensagem", "A nova senha deve ser diferente da atual."))));
            }
        }
        LocalDateTime agora = agora();
        u.setSenhaHash(SenhaPolitica.hash(novaSenha));
        u.setDataAtualizacao(agora);
        em.createQuery("update TokenRenovacaoEntity t set t.revogado = true, t.dataRevogacao = :agora "
                        + "where t.usuario.id = :uid and (t.revogado is null or t.revogado = false)")
                .setParameter("agora", agora)
                .setParameter("uid", usuarioId)
                .executeUpdate();
    }

    // ------------------------------------------------------------------------------------------- internos

    private Emitida montarSessao(UsuarioEntity u, EmpresaEntity e, String papel, boolean admin) {
        String access = jwt.emitir(u, e, papel, admin);
        String refresh = e == null ? null : criarRefreshToken(u, e);
        return new Emitida(new Sessao(access, jwt.expiresInSegundos(), resumo(u), resumo(e), papel, admin), refresh);
    }

    private String criarRefreshToken(UsuarioEntity u, EmpresaEntity e) {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime agora = agora();
        TokenRenovacaoEntity t = TokenRenovacaoEntity.builderOf()
                .withTokenHash(hashDoToken(token))
                .withDataExpiracao(agora.plusDays(refreshDias))
                .withUsado(false)
                .withRevogado(false)
                .withDataCriacao(agora)
                .withDataAtualizacao(agora)
                .withUsuario(u)
                .withEmpresa(e)
                .build();
        em.persist(t);
        return token;
    }

    /** SHA-256 hexadecimal: é o que se grava (o token em si nunca vai ao banco). */
    public static String hashDoToken(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private UsuarioEntity buscarUsuarioPorEmail(String email) {
        return em.createQuery("select u from UsuarioEntity u where lower(u.email) = :e", UsuarioEntity.class)
                .setParameter("e", email)
                .getResultStream().findFirst().orElse(null);
    }

    private List<VinculoUsuarioEntity> vinculosAtivos(String usuarioId) {
        return em.createQuery("select v from VinculoUsuarioEntity v join fetch v.empresa e "
                        + "where v.usuario.id = :uid and v.ativo = true and e.situacao = :ativa "
                        + "order by e.razaoSocial, e.id", VinculoUsuarioEntity.class)
                .setParameter("uid", usuarioId)
                .setParameter("ativa", SITUACAO_ATIVA)
                .getResultList();
    }

    private VinculoUsuarioEntity vinculoAtivo(String usuarioId, String empresaId) {
        return em.createQuery("select v from VinculoUsuarioEntity v join fetch v.empresa e "
                        + "where v.usuario.id = :uid and e.id = :eid and v.ativo = true and e.situacao = :ativa",
                        VinculoUsuarioEntity.class)
                .setParameter("uid", usuarioId)
                .setParameter("eid", empresaId)
                .setParameter("ativa", SITUACAO_ATIVA)
                .getResultStream().findFirst().orElse(null);
    }

    private TokenRenovacaoEntity buscarTokenComTrava(String hash) {
        return em.createQuery("select t from TokenRenovacaoEntity t join fetch t.usuario join fetch t.empresa "
                        + "where t.tokenHash = :h", TokenRenovacaoEntity.class)
                .setParameter("h", hash)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().orElse(null);
    }

    private int revogarAtivos(String usuarioId, String empresaId, LocalDateTime agora) {
        return em.createQuery("update TokenRenovacaoEntity t set t.revogado = true, t.dataRevogacao = :agora "
                        + "where t.usuario.id = :uid and t.empresa.id = :eid "
                        + "and (t.revogado is null or t.revogado = false) and (t.usado is null or t.usado = false)")
                .setParameter("agora", agora)
                .setParameter("uid", usuarioId)
                .setParameter("eid", empresaId)
                .executeUpdate();
    }

    private static UsuarioResumo resumo(UsuarioEntity u) {
        return new UsuarioResumo(u.getId(), u.getNome(), u.getEmail());
    }

    private static EmpresaResumo resumo(EmpresaEntity e) {
        return e == null ? null : new EmpresaResumo(e.getId(), e.getRazaoSocial(), e.getNomeFantasia());
    }

    private static ApiException credenciaisInvalidas() {
        return new ApiException(401, "credenciais_invalidas", MSG_CREDENCIAIS);
    }

    private static ApiException sessaoInvalida() {
        return new ApiException(401, "sessao_invalida", MSG_SESSAO);
    }
}
