package br.com.emitesimples.auth;

import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.security.JwtEmissor;
import br.com.emitesimples.security.Papeis;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Agentes do proprio usuario, fase 1 (docs/financeiro/README.md, 9.4, B3; mapa usuarios/ClienteAcesso). O administrador da empresa cria
 * um cliente de acesso (clientId + segredo, mostrado UMA vez; guarda so o hash bcrypt), ve o ultimo uso e revoga. O agente troca
 * clientId e segredo por um token de acesso curto ({@link JwtEmissor#emitirAgente}) com o grupo AGENTE e os escopos dados:
 * {@code consultar} (ler) e {@code preparar} (rascunho de titulo e de nota). Nunca aprova, baixa, estorna nem cancela.
 */
@ApplicationScoped
public class AgentesService {

    public static final Set<String> ESCOPOS = Set.of("consultar", "preparar");
    static final int VALIDADE_DIAS = 365;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public record Agente(String id, String nome, String clientId, List<String> escopos, boolean ativo, LocalDateTime dataUltimoUso,
            LocalDateTime dataExpiracao, String nomeDoUsuario, LocalDateTime dataCriacao) {
    }

    /** Resposta da criacao: o segredo aparece so aqui. */
    public record AgenteCriado(Agente agente, String segredo) {
    }

    public record CriarAgente(String nome, List<String> escopos) {
    }

    public record PedidoDeToken(String clientId, String segredo) {
    }

    public record TokenDeAgente(String accessToken, long expiresIn, String tokenType) {
    }

    @Inject
    EntityManager em;

    @Inject
    JwtEmissor jwt;

    @Inject
    ForcaBruta forcaBruta;

    @Inject
    ContextoTenantBanco contexto;

    // ------------------------------------------------------------------------------------------------ administracao (sessao de pessoa)

    public List<Agente> listar(String empresa) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select c from ClienteAcessoEntity c where c.empresa.id = :e order by c.dataCriacao desc", ClienteAcessoEntity.class)
                .setParameter("e", empresa).getResultList().stream().map(AgentesService::dto).toList());
    }

    public AgenteCriado criar(String empresa, String usuario, CriarAgente pedido) {
        if (pedido == null) {
            throw invalido("corpo", "Informe os dados do agente.");
        }
        String nome = pedido.nome() == null ? "" : pedido.nome().strip();
        if (nome.isEmpty() || nome.length() > 120) {
            throw invalido("nome", "Informe o nome do agente (até 120 caracteres).");
        }
        Set<String> escopos = new LinkedHashSet<>(pedido.escopos() == null ? List.of() : pedido.escopos());
        if (escopos.isEmpty() || !ESCOPOS.containsAll(escopos)) {
            throw invalido("escopos", "Escolha o que o agente pode fazer: consultar e/ou preparar rascunhos.");
        }
        String clientId = "agt_" + aleatorio(12);
        String segredo = aleatorio(32);
        var agora = AuthService.agora();
        ClienteAcessoEntity c = QuarkusTransaction.requiringNew().call(() -> {
            ClienteAcessoEntity n = new ClienteAcessoEntity();
            n.setNome(nome);
            n.setClientId(clientId);
            n.setSegredoHash(SenhaPolitica.hash(segredo));
            n.setScopes(String.join(" ", escopos));
            n.setAtivo(true);
            n.setDataExpiracao(agora.plusDays(VALIDADE_DIAS));
            n.setEmpresa(em.getReference(EmpresaEntity.class, empresa));
            n.setUsuario(em.getReference(UsuarioEntity.class, usuario));
            n.setDataCriacao(agora);
            n.setDataAtualizacao(agora);
            em.persist(n);
            em.flush();
            em.refresh(n);
            return n;
        });
        return new AgenteCriado(dto(c), segredo);
    }

    public Agente revogar(String empresa, String id) {
        return QuarkusTransaction.requiringNew().call(() -> {
            ClienteAcessoEntity c = id == null || id.length() > 36 ? null : em.find(ClienteAcessoEntity.class, id);
            if (c == null || !empresa.equals(c.getEmpresa().getId())) {
                throw new ApiException(404, "nao_encontrado", "Agente não encontrado.");
            }
            c.setAtivo(false);
            c.setDataAtualizacao(AuthService.agora());
            return dto(c);
        });
    }

    // ------------------------------------------------------------------------------------------------ troca do segredo (sem sessao)

    /** Troca clientId e segredo por um token de acesso de agente. Qualquer falha responde a mesma coisa (401), com as travas do login. */
    public TokenDeAgente token(PedidoDeToken pedido) {
        String clientId = pedido == null || pedido.clientId() == null ? "" : pedido.clientId().strip();
        String segredo = pedido == null ? null : pedido.segredo();
        if (clientId.isEmpty() || clientId.length() > 64 || segredo == null || SenhaPolitica.bytes(segredo) > SenhaPolitica.MAX_BYTES) {
            throw negado();
        }
        String chave = "agente:" + clientId;
        forcaBruta.verificar(chave);
        String token = contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> emitir(clientId, segredo)));
        if (token == null) {
            forcaBruta.registrarFalha(chave);
            throw negado();
        }
        forcaBruta.registrarSucesso(chave);
        return new TokenDeAgente(token, jwt.expiresInSegundos(), "Bearer");
    }

    private String emitir(String clientId, String segredo) {
        List<ClienteAcessoEntity> l = em.createQuery("select c from ClienteAcessoEntity c where c.clientId = :c", ClienteAcessoEntity.class)
                .setParameter("c", clientId).getResultList();
        if (l.isEmpty()) {
            return null;
        }
        ClienteAcessoEntity c = l.get(0);
        var agora = AuthService.agora();
        if (!Boolean.TRUE.equals(c.getAtivo()) || c.getDataExpiracao() != null && !c.getDataExpiracao().isAfter(agora)
                || c.getSegredoHash() == null || !SenhaPolitica.confere(segredo, c.getSegredoHash())) {
            return null;
        }
        UsuarioEntity u = c.getUsuario();
        EmpresaEntity e = c.getEmpresa();
        if (u == null || !Boolean.TRUE.equals(u.getAtivo()) || e == null || !"ATIVA".equals(e.getSituacao())) {
            return null;
        }
        List<VinculoUsuarioEntity> v = em.createQuery("select v from VinculoUsuarioEntity v where v.usuario.id = :u and v.empresa.id = :e "
                + "and v.ativo = true", VinculoUsuarioEntity.class).setParameter("u", u.getId()).setParameter("e", e.getId()).getResultList();
        if (v.isEmpty()) {
            return null; // o dono perdeu o acesso a empresa: o agente para junto
        }
        List<String> escopos = new ArrayList<>(escopos(c.getScopes()));
        if (Papeis.CONSULTA.equals(v.get(0).getPapel())) {
            escopos.remove("preparar"); // o agente nunca faz mais do que o dono pode
        }
        c.setDataUltimoUso(agora);
        return jwt.emitirAgente(u, e, c.getId(), escopos);
    }

    // ------------------------------------------------------------------------------------------------ apoio

    static List<String> escopos(String texto) {
        List<String> r = new ArrayList<>();
        if (texto != null) {
            for (String s : texto.split("[\\s,]+")) {
                if (ESCOPOS.contains(s)) {
                    r.add(s);
                }
            }
        }
        return r;
    }

    private static Agente dto(ClienteAcessoEntity c) {
        return new Agente(c.getId(), c.getNome(), c.getClientId(), escopos(c.getScopes()), Boolean.TRUE.equals(c.getAtivo()), c.getDataUltimoUso(),
                c.getDataExpiracao(), c.getUsuario() == null ? null : c.getUsuario().getNome(), c.getDataCriacao());
    }

    private static String aleatorio(int bytes) {
        byte[] b = new byte[bytes];
        ALEATORIO.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static ApiException negado() {
        return new ApiException(401, "credencial_invalida", "Credencial do agente inválida, revogada ou vencida.");
    }

    private static ApiException invalido(String campo, String mensagem) {
        return new ApiException(400, "requisicao_invalida", mensagem, Map.of("campos", List.of(Map.of("campo", campo, "mensagem", mensagem))));
    }
}
