package br.com.emitesimples.usuarios.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Liga um Usuario a uma Empresa com um papel. É aqui que está o isolamento: o 
* usuário só enxerga as empresas em que tem vínculo ativo, e a empresa do token 
* vem daqui.
 * Unicidade: um usuário tem no máximo um vínculo por empresa (par usuario, empresa 
* único).
 * Papel para scopes: administrador = consultar, preparar, emitir, cancelar; 
* operador = consultar, preparar, emitir; consulta = consultar. Emitir e cancelar 
* exigem confirmação humana, regra de fluxo fora deste mapa.

*/
public class VinculoUsuario {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute papel can't be empty.")
    @Size(max = 15, message = "Attribute papel must have 15 characters maximum.")
    private String papel;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

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

    public VinculoUsuario() {
        this.version = null;
        this.papel = null;
        this.ativo = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.usuario = null;
        this.empresa = null;
    }
    public VinculoUsuario(
        LocalDateTime version,
        String papel,
        Boolean ativo,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Usuario usuario,
        Empresa empresa
    ) {
        this.version = version;
        this.papel = papel;
        this.ativo = ativo;
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

    public void setPapel(String papel) {
        this.papel = papel;
    }

    public String getPapel() {
        return this.papel;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
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
            this.papel != null || 
            this.ativo != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.usuario != null || 
            this.empresa != null;
    }

}
