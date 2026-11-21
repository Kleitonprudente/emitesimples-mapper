package br.com.emitesimples.admin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.Plano;
import br.com.emitesimples.admin.builder.PlanoEntityBuilder;
import java.math.BigDecimal;

/*
* Catálogo de planos da plataforma, criado e mantido pelo suporte. É dado GLOBAL: 
* não pertence a uma empresa (sem tenant) e não tem ambiente. A cobrança é do CRM 
* (PerfexCRM da Marke): o valor é o que o CRM cobra, e este sistema não processa 
* pagamento. Um plano é desativado, nunca apagado.
 * EM ABERTO: o que o plano limita (notas por mês, usuários) e se há período de 
* teste ainda não foi respondido pelo usuário, por isso nenhum limite foi 
* modelado. Os limites entram depois por migração. A periodicidade é PROVISÓRIA.

*/
@Entity
@Table(name = "PLANO")
public class PlanoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=100)
    private String nome;

    @Column(name="DESCRICAO", length=500)
    private String descricao;

    @Column(name="VALOR", precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name="PERIODICIDADE", length=10)
    private String periodicidade;

    @Column(name="ATIVO")
    private Boolean ativo;

    @Column(name="REFERENCIA_CRM", length=50)
    private String referenciaCrm;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    public PlanoEntity() {
    }

    public PlanoEntity(
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

    public PlanoEntity(PlanoEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.descricao = builder.descricao;
        this.valor = builder.valor;
        this.periodicidade = builder.periodicidade;
        this.ativo = builder.ativo;
        this.referenciaCrm = builder.referenciaCrm;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
    }

    public static PlanoEntityBuilder builderOf() {
        return new PlanoEntityBuilder();
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

}
