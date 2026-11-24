package br.com.emitesimples.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpos de requisição e resposta de /api/auth. */
public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "O e-mail é obrigatório.") @Size(max = 160, message = "E-mail longo demais.") String email,
            @NotBlank(message = "A senha é obrigatória.") @Size(max = 1000, message = "Senha longa demais.") String senha,
            @Size(max = 36, message = "empresaId inválido.") String empresaId) {
    }


    public record SenhaRequest(
            @Size(max = 1000, message = "Senha longa demais.") String senhaAtual,
            @Size(max = 1000, message = "Senha longa demais.") String novaSenha) {
    }

    public record UsuarioResumo(String id, String nome, String email) {
    }

    public record EmpresaResumo(String id, String razaoSocial, String nomeFantasia) {
    }

    public record EmpresaOpcao(String id, String razaoSocial, String papel) {
    }

    /** Resposta do login e do refresh. O refresh token NAO vai no corpo: vai em cookie httpOnly (AuthResource). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Sessao(
            String accessToken,
            long expiresIn,
            UsuarioResumo usuario,
            EmpresaResumo empresa,
            String papel,
            boolean plataformaAdmin,
            boolean podeCancelarNfse) {
    }

    /** Resultado interno do login/refresh: a sessao (corpo) e o refresh token (cookie; null para admin sem empresa). */
    public record Emitida(Sessao sessao, String refreshToken) {
    }

    /** Resposta de GET /api/auth/me. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Perfil(UsuarioResumo usuario, EmpresaResumo empresa, String papel, boolean plataformaAdmin,
                  boolean podeCancelarNfse) {
    }
}
