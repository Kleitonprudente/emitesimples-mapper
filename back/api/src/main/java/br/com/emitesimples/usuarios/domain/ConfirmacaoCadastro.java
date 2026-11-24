package br.com.emitesimples.usuarios.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Confirmação do signup por link de e-mail. O link enviado ao usuário leva o 
* token; o sistema guarda SÓ o hash do token, nunca o token. Ao confirmar, a 
* empresa passa de PENDENTE_CONFIRMACAO para ATIVA e a dataConfirmacao dela é 
* preenchida.
 * O endpoint de confirmação é PÚBLICO (o usuário ainda não tem sessão): esta 
* entidade NÃO deve ter recurso REST exposto, é regra da aplicação escrita à mão.

*/
public class ConfirmacaoCadastro {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute email can't be empty.")
    @Size(max = 160, message = "Attribute email must have 160 characters maximum.")
    private String email;

    @NotNull(message = "The attribute dataExpiracao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataExpiracao;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataConfirmacao;

    @NotEmpty(message = "The attribute estado can't be empty.")
    @Size(max = 10, message = "Attribute estado must have 10 characters maximum.")
    private String estado;

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

    public ConfirmacaoCadastro() {
        this.version = null;
        this.email = null;
        this.dataExpiracao = null;
        this.dataConfirmacao = null;
        this.estado = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.usuario = null;
        this.empresa = null;
    }
    public ConfirmacaoCadastro(
        LocalDateTime version,
        String email,
        LocalDateTime dataExpiracao,
        LocalDateTime dataConfirmacao,
        String estado,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Usuario usuario,
        Empresa empresa
    ) {
        this.version = version;
        this.email = email;
        this.dataExpiracao = dataExpiracao;
        this.dataConfirmacao = dataConfirmacao;
        this.estado = estado;
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

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public LocalDateTime getDataExpiracao() {
        return this.dataExpiracao;
    }

    public void setDataConfirmacao(LocalDateTime dataConfirmacao) {
        this.dataConfirmacao = dataConfirmacao;
    }

    public LocalDateTime getDataConfirmacao() {
        return this.dataConfirmacao;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return this.estado;
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
            this.email != null || 
            this.dataExpiracao != null || 
            this.dataConfirmacao != null || 
            this.estado != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.usuario != null || 
            this.empresa != null;
    }

}
