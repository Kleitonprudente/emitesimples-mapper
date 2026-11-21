package br.com.emitesimples.cadastros.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Contato de uma Pessoa, com tipo, nome, WhatsApp e e-mail. Uma pessoa tem vários. 
* Existe só dentro da pessoa e no mesmo contexto, por isso é detalhe da Pessoa 
* ([m]/[o]).
 * Os sócios do QSA da consulta de CNPJ entram como contatos do tipo sócio. O 
* e-mail e o telefone principais ficam na própria Pessoa.
 * Vive no contexto cadastros (domain.cadastros) e referencia a Empresa 
* (configuracao) como tenant.

*/
public class Contato {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute tipoContato can't be empty.")
    @Size(max = 10, message = "Attribute tipoContato must have 10 characters maximum.")
    private String tipoContato;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 300, message = "Attribute nome must have 300 characters maximum.")
    private String nome;

    @Size(max = 20, message = "Attribute whatsapp must have 20 characters maximum.")
    private String whatsapp;

    @Size(max = 60, message = "Attribute email must have 60 characters maximum.")
    private String email;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    @NotNull(message = "The attribute pessoa can't be null.")
    private Pessoa pessoa;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public Contato() {
        this.version = null;
        this.tipoContato = null;
        this.nome = null;
        this.whatsapp = null;
        this.email = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.pessoa = null;
        this.empresa = null;
    }
    public Contato(
        LocalDateTime version,
        String tipoContato,
        String nome,
        String whatsapp,
        String email,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Pessoa pessoa,
        Empresa empresa
    ) {
        this.version = version;
        this.tipoContato = tipoContato;
        this.nome = nome;
        this.whatsapp = whatsapp;
        this.email = email;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.pessoa = pessoa;
        this.empresa = empresa;
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }

    public void setTipoContato(String tipoContato) {
        this.tipoContato = tipoContato;
    }

    public String getTipoContato() {
        return this.tipoContato;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return this.nome;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getWhatsapp() {
        return this.whatsapp;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
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

    public void setPessoa(Pessoa pessoa) {
        this.pessoa = pessoa;
    }

    public Pessoa getPessoa() {
        return this.pessoa;
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
            this.tipoContato != null || 
            this.nome != null || 
            this.whatsapp != null || 
            this.email != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.pessoa != null || 
            this.empresa != null;
    }

}
