package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

/*
* Modelo de parcelamento reutilizável: forma, número de parcelas, dias até a 
* primeira parcela e intervalo entre as parcelas.
 * Usada principalmente para gerar títulos a partir da nota fiscal (Fase 5). Não 
* existe condição padrão global: quem usa guarda a sua.
 * Regras documentadas e não expressas no mapa (docs/financeiro/README.md, F1-H3), 
* implementadas na migração:
 * nome único por empresa; parcelas de 1 a 48; intervalo e dias até a primeira 
* parcela de 0 a 365;
 * receber na emissão só com 1 parcela e 0 dias, e exige conta padrão ativa da 
* forma (trigger).
 * Nunca se apaga: só se desativa. Escrita só do administrador da empresa.

*/
public class CondicaoPagamento {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 100, message = "Attribute nome must have 100 characters maximum.")
    private String nome;

    @NotEmpty(message = "The attribute forma can't be empty.")
    @Size(max = 20, message = "Attribute forma must have 20 characters maximum.")
    private String forma;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @NotNull(message = "The attribute parcelas can't be null.")
    private Integer parcelas;

    @NotNull(message = "The attribute diasPrimeiroVencimento can't be null.")
    private Integer diasPrimeiroVencimento;

    @NotNull(message = "The attribute intervaloDias can't be null.")
    private Integer intervaloDias;

    @NotNull(message = "The attribute receberNaEmissao can't be null.")
    private Boolean receberNaEmissao;

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

    public CondicaoPagamento() {
        this.version = null;
        this.nome = null;
        this.forma = null;
        this.ativo = null;
        this.parcelas = null;
        this.diasPrimeiroVencimento = null;
        this.intervaloDias = null;
        this.receberNaEmissao = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.empresa = null;
    }
    public CondicaoPagamento(
        LocalDateTime version,
        String nome,
        String forma,
        Boolean ativo,
        Integer parcelas,
        Integer diasPrimeiroVencimento,
        Integer intervaloDias,
        Boolean receberNaEmissao,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        Empresa empresa
    ) {
        this.version = version;
        this.nome = nome;
        this.forma = forma;
        this.ativo = ativo;
        this.parcelas = parcelas;
        this.diasPrimeiroVencimento = diasPrimeiroVencimento;
        this.intervaloDias = intervaloDias;
        this.receberNaEmissao = receberNaEmissao;
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

    public void setForma(String forma) {
        this.forma = forma;
    }

    public String getForma() {
        return this.forma;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setParcelas(Integer parcelas) {
        this.parcelas = parcelas;
    }

    public Integer getParcelas() {
        return this.parcelas;
    }

    public void setDiasPrimeiroVencimento(Integer diasPrimeiroVencimento) {
        this.diasPrimeiroVencimento = diasPrimeiroVencimento;
    }

    public Integer getDiasPrimeiroVencimento() {
        return this.diasPrimeiroVencimento;
    }

    public void setIntervaloDias(Integer intervaloDias) {
        this.intervaloDias = intervaloDias;
    }

    public Integer getIntervaloDias() {
        return this.intervaloDias;
    }

    public void setReceberNaEmissao(Boolean receberNaEmissao) {
        this.receberNaEmissao = receberNaEmissao;
    }

    public Boolean getReceberNaEmissao() {
        return this.receberNaEmissao;
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
            this.forma != null || 
            this.ativo != null || 
            this.parcelas != null || 
            this.diasPrimeiroVencimento != null || 
            this.intervaloDias != null || 
            this.receberNaEmissao != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.empresa != null;
    }

}
