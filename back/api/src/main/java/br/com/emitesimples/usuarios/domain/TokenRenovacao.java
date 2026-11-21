package br.com.emitesimples.usuarios.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Registro do token de renovação do login, que permite um JWT de acesso curto. 
* Guarda só o hash do token e o estado, para rotação (cada uso gera um novo e 
* marca o anterior) e detecção de reuso (token já usado ou revogado reapresentado 
* derruba a sessão).
 * NUNCA guarda o token, só o hash. O JWT de acesso não é guardado em lugar nenhum.

*/
public class TokenRenovacao {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotNull(message = "The attribute dataExpiracao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataExpiracao;

    @NotNull(message = "The attribute usado can't be null.")
    private Boolean usado;

    @NotNull(message = "The attribute revogado can't be null.")
    private Boolean revogado;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataUso;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataRevogacao;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute usuario can't be null.")
    private Usuario usuario;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public TokenRenovacao() {
        this.version = null;
        this.dataExpiracao = null;
        this.usado = null;
        this.revogado = null;
        this.dataUso = null;
        this.dataRevogacao = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.usuario = null;
        this.empresa = null;
    }
    public TokenRenovacao(
        LocalDateTime version,
        LocalDateTime dataExpiracao,
        Boolean usado,
        Boolean revogado,
        LocalDateTime dataUso,
        LocalDateTime dataRevogacao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Usuario usuario,
        Empresa empresa
    ) {
        this.version = version;
        this.dataExpiracao = dataExpiracao;
        this.usado = usado;
        this.revogado = revogado;
        this.dataUso = dataUso;
        this.dataRevogacao = dataRevogacao;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.usuario = usuario;
        this.empresa = empresa;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public LocalDateTime getDataExpiracao() {
        return this.dataExpiracao;
    }

    public void setUsado(Boolean usado) {
        this.usado = usado;
    }

    public Boolean getUsado() {
        return this.usado;
    }

    public void setRevogado(Boolean revogado) {
        this.revogado = revogado;
    }

    public Boolean getRevogado() {
        return this.revogado;
    }

    public void setDataUso(LocalDateTime dataUso) {
        this.dataUso = dataUso;
    }

    public LocalDateTime getDataUso() {
        return this.dataUso;
    }

    public void setDataRevogacao(LocalDateTime dataRevogacao) {
        this.dataRevogacao = dataRevogacao;
    }

    public LocalDateTime getDataRevogacao() {
        return this.dataRevogacao;
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

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return this.usuario;
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
            this.dataExpiracao != null || 
            this.usado != null || 
            this.revogado != null || 
            this.dataUso != null || 
            this.dataRevogacao != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.usuario != null || 
            this.empresa != null;
    }

}
