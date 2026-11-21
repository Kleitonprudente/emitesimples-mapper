package br.com.emitesimples.configuracao.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
* Item do quadro de sócios e administradores (QSA) de uma Empresa, com nome, CPF, 
* qualificação e data de entrada na sociedade. Uma empresa tem vários.
 * Existe só dentro da empresa e no mesmo contexto, por isso é detalhe da Empresa 
* ([m]/[o]). O tenant é a própria empresa. Não tem ambiente: herda o da Empresa.
 * Vive no contexto configuracao (domain.configuracao).

*/
public class QsaEmpresa {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 300, message = "Attribute nome must have 300 characters maximum.")
    private String nome;

    @Size(max = 14, message = "Attribute cpf must have 14 characters maximum.")
    private String cpf;

    @Size(max = 100, message = "Attribute qualificacao must have 100 characters maximum.")
    private String qualificacao;

    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate dataEntradaSociedade;

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

    public QsaEmpresa() {
        this.version = null;
        this.nome = null;
        this.cpf = null;
        this.qualificacao = null;
        this.dataEntradaSociedade = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
    }
    public QsaEmpresa(
        LocalDateTime version,
        String nome,
        String cpf,
        String qualificacao,
        LocalDate dataEntradaSociedade,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa
    ) {
        this.version = version;
        this.nome = nome;
        this.cpf = cpf;
        this.qualificacao = qualificacao;
        this.dataEntradaSociedade = dataEntradaSociedade;
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

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getCpf() {
        return this.cpf;
    }

    public void setQualificacao(String qualificacao) {
        this.qualificacao = qualificacao;
    }

    public String getQualificacao() {
        return this.qualificacao;
    }

    public void setDataEntradaSociedade(LocalDate dataEntradaSociedade) {
        this.dataEntradaSociedade = dataEntradaSociedade;
    }

    public LocalDate getDataEntradaSociedade() {
        return this.dataEntradaSociedade;
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
            this.cpf != null || 
            this.qualificacao != null || 
            this.dataEntradaSociedade != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null;
    }

}
