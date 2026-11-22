package br.com.emitesimples.plataforma;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/** Corpos de requisicao e resposta de /api/admin (escritos a mao). Nenhum deles tem campo de senha ou de hash na RESPOSTA. */
public final class PlataformaDtos {

    private PlataformaDtos() {
    }

    // -------------------------------------------------------------------------- importacao de municipios

    public record Rejeicao(int linha, String motivo) {
    }

    public record UsuarioRef(String id, String nome, String email) {
    }

    /**
     * Resumo de uma importacao. No POST traz {@code rejeitados} (lista, ate 1000; o excedente em
     * {@code rejeitadosOmitidos}); na listagem traz {@code resumoRejeicoes} (texto) e {@code usuario}.
     * Campos nulos nao saem no JSON.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ImportacaoResumo(
            String id,
            String nomeArquivo,
            String estado,
            String codificacao,
            String separador,
            LocalDateTime dataHoraInicio,
            LocalDateTime dataHoraFim,
            Integer linhasLidas,
            Integer criados,
            Integer atualizados,
            Integer inalterados,
            Integer totalRejeitados,
            List<Rejeicao> rejeitados,
            Integer rejeitadosOmitidos,
            String resumoRejeicoes,
            UsuarioRef usuario) {
    }

    // -------------------------------------------------------------------------------- usuarios (admin)

    private static final String EMAIL = "^\\s*[^@\\s]+@[^@\\s]+\\.[^@\\s]+\\s*$";

    public record VinculoNovo(
            @NotBlank(message = "A empresa é obrigatória.") @Size(max = 36, message = "empresaId inválido.") String empresaId,
            @NotBlank(message = "O papel é obrigatório.") @Size(max = 15, message = "Papel inválido.") String papel,
            Boolean ativo) {
    }

    public record UsuarioNovo(
            @NotBlank(message = "O nome é obrigatório.") @Size(max = 120, message = "Nome longo demais (máximo 120).") String nome,
            @NotBlank(message = "O e-mail é obrigatório.") @Size(max = 160, message = "E-mail longo demais (máximo 160).")
            @Pattern(regexp = EMAIL, message = "E-mail inválido.") String email,
            @Size(max = 20, message = "WhatsApp longo demais (máximo 20).") String whatsapp,
            @Size(max = 1000, message = "Senha longa demais.") String senha,
            Boolean ativo,
            @Valid List<@NotNull(message = "Vínculo inválido.") @Valid VinculoNovo> vinculos) {
    }

    /** PATCH: campo ausente (null) = nao muda. whatsapp "" apaga o numero. */
    public record UsuarioEdicao(
            @Size(max = 120, message = "Nome longo demais (máximo 120).") String nome,
            @Size(max = 20, message = "WhatsApp longo demais (máximo 20).") String whatsapp,
            Boolean ativo) {
    }

    public record SenhaTemporaria(
            @Size(max = 1000, message = "Senha longa demais.") String senha) {
    }

    public record VinculoAlteracao(
            @Size(max = 15, message = "Papel inválido.") String papel,
            Boolean ativo) {
    }

    public record EmpresaRef(String id, String razaoSocial, String nomeFantasia, String situacao) {
    }

    public record VinculoAdmin(String id, String usuarioId, EmpresaRef empresa, String papel, Boolean ativo,
                               LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
    }

    /** Usuario como o admin enxerga: NUNCA com senha nem hash ({@code temSenha} diz so se ha senha definida). */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record UsuarioAdmin(
            String id,
            String nome,
            String email,
            String whatsapp,
            Boolean ativo,
            Boolean administradorPlataforma,
            boolean temSenha,
            LocalDateTime dataUltimoAcesso,
            LocalDateTime dataCriacao,
            LocalDateTime dataAtualizacao,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<VinculoAdmin> vinculos) {
    }
}
