package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CondicaoPagamento;
import br.com.emitesimples.financeiro.builder.CondicaoPagamentoEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

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
@Entity
@Table(name = "CONDICAOPAGAMENTO")
public class CondicaoPagamentoEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="NOME", length=100)
    private String nome;

    @Column(name="FORMA", length=20)
    private String forma;

    @Column(name="ATIVO")
    private Boolean ativo;

    @Column(name="PARCELAS", precision = 2)
    private Integer parcelas;

    @Column(name="DIAS_PRIMEIRO_VENCIMENTO", precision = 3)
    private Integer diasPrimeiroVencimento;

    @Column(name="INTERVALO_DIAS", precision = 3)
    private Integer intervaloDias;

    @Column(name="RECEBER_NA_EMISSAO")
    private Boolean receberNaEmissao;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public CondicaoPagamentoEntity() {
    }

    public CondicaoPagamentoEntity(
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
        EmpresaEntity empresa
    ) {
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

    public CondicaoPagamentoEntity(CondicaoPagamentoEntityBuilder builder) {
        this.version = builder.version;
        this.nome = builder.nome;
        this.forma = builder.forma;
        this.ativo = builder.ativo;
        this.parcelas = builder.parcelas;
        this.diasPrimeiroVencimento = builder.diasPrimeiroVencimento;
        this.intervaloDias = builder.intervaloDias;
        this.receberNaEmissao = builder.receberNaEmissao;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.empresa = builder.empresa;
    }

    public static CondicaoPagamentoEntityBuilder builderOf() {
        return new CondicaoPagamentoEntityBuilder();
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

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
