package br.com.emitesimples.usuarios.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
* Identidade de uma pessoa que entra no sistema. É global: uma pessoa pode ter 
* vínculo com várias empresas, por isso não tem tenantKey, e o isolamento por 
* empresa está no VinculoUsuario. Vive no contexto usuarios (domain.usuarios), que 
* depende de configuracao e cadastros.
 * O e-mail é único no sistema inteiro (o mapa não expressa unicidade, ela é regra 
* de negócio e índice único no banco).
 * NUNCA guarda a senha, só o hash. NUNCA guarda token.

*/
public class Usuario {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 120, message = "Attribute nome must have 120 characters maximum.")
    private String nome;

    @NotEmpty(message = "The attribute email can't be empty.")
    @Size(max = 160, message = "Attribute email must have 160 characters maximum.")
    private String email;

    @Size(max = 20, message = "Attribute whatsapp must have 20 characters maximum.")
    private String whatsapp;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @NotNull(message = "The attribute administradorPlataforma can't be null.")
    private Boolean administradorPlataforma;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataUltimoAcesso;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    public Usuario() {
        this.version = null;
        this.nome = null;
        this.email = null;
        this.whatsapp = null;
        this.ativo = null;
        this.administradorPlataforma = null;
        this.dataUltimoAcesso = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
    }
    public Usuario(
        LocalDateTime version,
        String nome,
        String email,
        String whatsapp,
        Boolean ativo,
        Boolean administradorPlataforma,
        LocalDateTime dataUltimoAcesso,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
    ) {
        this.version = version;
        this.nome = nome;
        this.email = email;
        this.whatsapp = whatsapp;
        this.ativo = ativo;
        this.administradorPlataforma = administradorPlataforma;
        this.dataUltimoAcesso = dataUltimoAcesso;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getWhatsapp() {
        return this.whatsapp;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setAdministradorPlataforma(Boolean administradorPlataforma) {
        this.administradorPlataforma = administradorPlataforma;
    }

    public Boolean getAdministradorPlataforma() {
        return this.administradorPlataforma;
    }

    public void setDataUltimoAcesso(LocalDateTime dataUltimoAcesso) {
        this.dataUltimoAcesso = dataUltimoAcesso;
    }

    public LocalDateTime getDataUltimoAcesso() {
        return this.dataUltimoAcesso;
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
            this.email != null || 
            this.whatsapp != null || 
            this.ativo != null || 
            this.administradorPlataforma != null || 
            this.dataUltimoAcesso != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null;
    }

}
