package br.com.emitesimples.admin.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/*
* Catálogo de planos da plataforma, criado e mantido pelo suporte. É dado GLOBAL: 
* não pertence a uma empresa (sem tenant) e não tem ambiente. A cobrança é do CRM 
* (PerfexCRM da Marke): o valor é o que o CRM cobra, e este sistema não processa 
* pagamento. Um plano é desativado, nunca apagado.
 * EM ABERTO: o que o plano limita (notas por mês, usuários) e se há período de 
* teste ainda não foi respondido pelo usuário, por isso nenhum limite foi 
* modelado. Os limites entram depois por migração. A periodicidade é PROVISÓRIA.

*/
public class Plano {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotEmpty(message = "The attribute nome can't be empty.")
    @Size(max = 100, message = "Attribute nome must have 100 characters maximum.")
    private String nome;

    @Size(max = 500, message = "Attribute descricao must have 500 characters maximum.")
    private String descricao;

    @NotNull(message = "The attribute valor can't be null.")
    private BigDecimal valor;

    @NotEmpty(message = "The attribute periodicidade can't be empty.")
    @Size(max = 10, message = "Attribute periodicidade must have 10 characters maximum.")
    private String periodicidade;

    @NotNull(message = "The attribute ativo can't be null.")
    private Boolean ativo;

    @Size(max = 50, message = "Attribute referenciaCrm must have 50 characters maximum.")
    private String referenciaCrm;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    public Plano() {
        this.version = null;
        this.nome = null;
        this.descricao = null;
        this.valor = null;
        this.periodicidade = null;
        this.ativo = null;
        this.referenciaCrm = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
    }
    public Plano(
        LocalDateTime version,
        String nome,
        String descricao,
        BigDecimal valor,
        String periodicidade,
        Boolean ativo,
        String referenciaCrm,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao
    ) {
        this.version = version;
        this.nome = nome;
        this.descricao = descricao;
        this.valor = valor;
        this.periodicidade = periodicidade;
        this.ativo = ativo;
        this.referenciaCrm = referenciaCrm;
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

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return this.descricao;
    }
    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
    public BigDecimal getValor() {
        return this.valor;
    }

    public void setPeriodicidade(String periodicidade) {
        this.periodicidade = periodicidade;
    }

    public String getPeriodicidade() {
        return this.periodicidade;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Boolean getAtivo() {
        return this.ativo;
    }

    public void setReferenciaCrm(String referenciaCrm) {
        this.referenciaCrm = referenciaCrm;
    }

    public String getReferenciaCrm() {
        return this.referenciaCrm;
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
            this.descricao != null || 
            this.valor != null || 
            this.periodicidade != null || 
            this.ativo != null || 
            this.referenciaCrm != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null;
    }

}
