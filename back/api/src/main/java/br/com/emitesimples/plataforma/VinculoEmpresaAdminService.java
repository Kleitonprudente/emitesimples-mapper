package br.com.emitesimples.plataforma;

import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoAlteracao;
import br.com.emitesimples.plataforma.PlataformaDtos.VinculoNovo;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Vinculos de usuario VISTOS PELA EMPRESA. Leitura propria (ordenada pelo nome do usuario); criar e alterar DELEGAM ao
 * {@link UsuarioAdminService} (mesmas regras, mensagens e revogacao de tokens ao desativar). Nunca apaga: so desativa.
 */
@ApplicationScoped
public class VinculoEmpresaAdminService {

    public record UsuarioVinculo(String id, String nome, String email, Boolean ativo) {
    }

    public record VinculoEmpresa(String id, UsuarioVinculo usuario, String papel, Boolean ativo, LocalDateTime dataCriacao) {
    }

    @Inject
    EntityManager em;

    @Inject
    UsuarioAdminService usuarios;

    public List<VinculoEmpresa> listar(String empresaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            exigirEmpresa(empresaId);
            return em.createQuery("select v from VinculoUsuarioEntity v join fetch v.usuario u where v.empresa.id = :e "
                            + "order by lower(u.nome), u.id", VinculoUsuarioEntity.class)
                    .setParameter("e", empresaId).getResultList().stream().map(VinculoEmpresaAdminService::dto).toList();
        });
    }

    public VinculoEmpresa criar(String empresaId, String usuarioId, String papel, Boolean ativo) {
        QuarkusTransaction.requiringNew().run(() -> exigirEmpresa(empresaId));
        String id = usuarios.criarVinculo(usuarioId.trim(), new VinculoNovo(empresaId, papel, ativo)).id();
        return obter(empresaId, id);
    }

    public VinculoEmpresa alterar(String empresaId, String vinculoId, VinculoAlteracao a) {
        String usuarioId = QuarkusTransaction.requiringNew().call(() -> {
            exigirEmpresa(empresaId);
            return em.createQuery("select v.usuario.id from VinculoUsuarioEntity v where v.id = :id and v.empresa.id = :e",
                            String.class)
                    .setParameter("id", vinculoId).setParameter("e", empresaId).getResultStream().findFirst()
                    .orElseThrow(() -> new ApiException(404, "vinculo_nao_encontrado", "Vínculo não encontrado."));
        });
        usuarios.alterarVinculo(usuarioId, vinculoId, a);
        return obter(empresaId, vinculoId);
    }

    private VinculoEmpresa obter(String empresaId, String vinculoId) {
        return QuarkusTransaction.requiringNew().call(() -> dto(em.createQuery("select v from VinculoUsuarioEntity v "
                        + "join fetch v.usuario where v.id = :id and v.empresa.id = :e", VinculoUsuarioEntity.class)
                .setParameter("id", vinculoId).setParameter("e", empresaId).getSingleResult()));
    }

    private void exigirEmpresa(String empresaId) {
        if (em.find(EmpresaEntity.class, empresaId) == null) {
            throw new ApiException(404, "empresa_nao_encontrada", "Empresa não encontrada.");
        }
    }

    private static VinculoEmpresa dto(VinculoUsuarioEntity v) {
        UsuarioEntity u = v.getUsuario();
        return new VinculoEmpresa(v.getId(), new UsuarioVinculo(u.getId(), u.getNome(), u.getEmail(), u.getAtivo()),
                v.getPapel(), v.getAtivo(), v.getDataCriacao());
    }
}
