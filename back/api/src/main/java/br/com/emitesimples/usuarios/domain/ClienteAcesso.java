package br.com.emitesimples.usuarios.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Credencial de máquina para os agentes do próprio usuário (fase 1), vinculada a 
* uma única empresa. Troca clientId e segredo por um JWT de acesso com a claim da 
* empresa e os scopes permitidos. Vive no contexto usuarios (domain.usuarios), que 
* depende de configuracao e cadastros.
 * Não é cliente OAuth de terceiros: registro dinâmico e consentimento ficam para a 
* fase 2.
 * O clientId é único no sistema. NUNCA guarda o segredo, só o hash, e NUNCA guarda 
* token.

*/
public class ClienteAcesso {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 120, message = "Attribute nome must have 120 characters maximum.")
    private String nome;

    @NotEmpty(message = "The attribute clientId can't be empty.")
    @Size(max = 64, message = "Attribute clientId must have 64 characters maximum.")
    private String clientId;

    @NotEmpty(message = "The attribute scopes can't be empty.")
    @Size(max = 100, message = "Attribute scopes must have 100 characters maximum.")
    private String scopes;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataUltimoUso;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataExpiracao;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public ClienteAcesso() {
        this.version = null;
        this.nome = null;
        this.clientId = null;
        this.scopes = null;
        this.ativo = null;
        this.dataUltimoUso = null;
        this.dataExpiracao = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
    }
    public ClienteAcesso(
        LocalDateTime version,
        String nome,
        String clientId,
        String scopes,
        Boolean ativo,
        LocalDateTime dataUltimoUso,
        LocalDateTime dataExpiracao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa
    ) {
        this.version = version;
        this.nome = nome;
        this.clientId = clientId;
        this.scopes = scopes;
        this.ativo = ativo;
        this.dataUltimoUso = dataUltimoUso;
        this.dataExpiracao = dataExpiracao;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.empresa = empresa;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getScopes() {
        return this.scopes;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setDataUltimoUso(LocalDateTime dataUltimoUso) {
        this.dataUltimoUso = dataUltimoUso;
    }

    public LocalDateTime getDataUltimoUso() {
        return this.dataUltimoUso;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public LocalDateTime getDataExpiracao() {
        return this.dataExpiracao;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataCriacao() {
        return this.dataCriacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return this.dataAtualizacao;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public Empresa getEmpresa() {
        return this.empresa;
    }

    @JsonIgnore
    public boolean isWeakReference() {
      return !this.isAnyNotKeyFieldsDefined()
              && (
                      this.isIdDefined()
              );
    }
    @JsonIgnore
    public boolean isIdDefined() {
        return this.id != null;
    }

    @JsonIgnore
    public boolean isAnyNotKeyFieldsDefined() {
        return 
            this.nome != null || 
            this.clientId != null || 
            this.scopes != null || 
            this.ativo != null || 
            this.dataUltimoUso != null || 
            this.dataExpiracao != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null;
    }

}
