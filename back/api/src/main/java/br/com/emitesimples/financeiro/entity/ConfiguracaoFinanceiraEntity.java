package br.com.emitesimples.financeiro.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.ConfiguracaoFinanceira;
import br.com.emitesimples.financeiro.builder.ConfiguracaoFinanceiraEntityBuilder;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
* Parâmetros do financeiro da empresa: multa, juros e carência dos atrasos, 
* lembretes de cobrança e os padrões que pré-preenchem o título novo.
 * Uma linha por empresa, criada pelos padrões iniciais (POST 
* /api/financeiro/padroes); nunca se cria pelo CRUD (POST negado) e nunca se 
* apaga.
 * Os padrões só pré-preenchem o formulário: a pessoa pode apagar.
 * Regras documentadas e não expressas no mapa (docs/financeiro/README.md, F1-H4), 
* implementadas na migração:
 * uma linha por empresa; multa e juros de 0 a 999,99; carência de 0 a 365 dias; 
* lembrete antes de 0 a 30 dias;
 * lembretes depois com até 5 números de 1 ou 2 dígitos separados por vírgula; 
* categoria padrão ativa, do tipo certo e sem filhas (trigger).
 * Escrita só do administrador da empresa.

*/
@Entity
@Table(name = "CONFIGURACAOFINANCEIRA")
public class ConfiguracaoFinanceiraEntity {
    @Version
    @Column(name="VERSION")
    private LocalDateTime version;

    @Column(name="MULTA_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal multaPercentual;

    @Column(name="JUROS_MENSAL_PERCENTUAL", precision = 5, scale = 2)
    private BigDecimal jurosMensalPercentual;

    @Column(name="CARENCIA_DIAS", precision = 3)
    private Integer carenciaDias;

    @Column(name="LEMBRETES_ATIVOS")
    private Boolean lembretesAtivos;

    @Column(name="LEMBRETE_DIAS_ANTES", precision = 2)
    private Integer lembreteDiasAntes;

    @Column(name="LEMBRETE_DIAS_DEPOIS", length=40)
    private String lembreteDiasDepois;

    @Column(name="VENCIMENTO_PADRAO_DIAS", precision = 3)
    private Integer vencimentoPadraoDias;

    @Column(name="OBSERVACAO_PADRAO_RECEBER", length=255)
    private String observacaoPadraoReceber;

    @Column(name="FORMA_PREVISTA_PADRAO_RECEBER", length=20)
    private String formaPrevistaPadraoReceber;

    @Column(name="PADROES_CRIADOS_EM")
    private LocalDateTime padroesCriadosEm;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", updatable = false, nullable = false, length = 36)
    private String id;

    @Column(name="DATA_CRIACAO")
    private LocalDateTime dataCriacao;

    @Column(name="DATA_ATUALIZACAO")
    private LocalDateTime dataAtualizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CATEGORIA_PADRAO_RECEITA_ID", nullable = true)
    private CategoriaFinanceiraEntity categoriaPadraoReceita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CATEGORIA_PADRAO_DESPESA_ID", nullable = true)
    private CategoriaFinanceiraEntity categoriaPadraoDespesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="EMPRESA_ID", nullable = false)
    private EmpresaEntity empresa;

    public ConfiguracaoFinanceiraEntity() {
    }

    public ConfiguracaoFinanceiraEntity(
        BigDecimal multaPercentual,
        BigDecimal jurosMensalPercentual,
        Integer carenciaDias,
        Boolean lembretesAtivos,
        Integer lembreteDiasAntes,
        String lembreteDiasDepois,
        Integer vencimentoPadraoDias,
        String observacaoPadraoReceber,
        String formaPrevistaPadraoReceber,
        LocalDateTime padroesCriadosEm,
        String id,
        LocalDateTime dataCriacao,
        LocalDateTime dataAtualizacao,
        CategoriaFinanceiraEntity categoriaPadraoReceita,
        CategoriaFinanceiraEntity categoriaPadraoDespesa,
        EmpresaEntity empresa
    ) {
        this.multaPercentual = multaPercentual;
        this.jurosMensalPercentual = jurosMensalPercentual;
        this.carenciaDias = carenciaDias;
        this.lembretesAtivos = lembretesAtivos;
        this.lembreteDiasAntes = lembreteDiasAntes;
        this.lembreteDiasDepois = lembreteDiasDepois;
        this.vencimentoPadraoDias = vencimentoPadraoDias;
        this.observacaoPadraoReceber = observacaoPadraoReceber;
        this.formaPrevistaPadraoReceber = formaPrevistaPadraoReceber;
        this.padroesCriadosEm = padroesCriadosEm;
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
        this.categoriaPadraoReceita = categoriaPadraoReceita;
        this.categoriaPadraoDespesa = categoriaPadraoDespesa;
        this.empresa = empresa;
    }

    public ConfiguracaoFinanceiraEntity(ConfiguracaoFinanceiraEntityBuilder builder) {
        this.version = builder.version;
        this.multaPercentual = builder.multaPercentual;
        this.jurosMensalPercentual = builder.jurosMensalPercentual;
        this.carenciaDias = builder.carenciaDias;
        this.lembretesAtivos = builder.lembretesAtivos;
        this.lembreteDiasAntes = builder.lembreteDiasAntes;
        this.lembreteDiasDepois = builder.lembreteDiasDepois;
        this.vencimentoPadraoDias = builder.vencimentoPadraoDias;
        this.observacaoPadraoReceber = builder.observacaoPadraoReceber;
        this.formaPrevistaPadraoReceber = builder.formaPrevistaPadraoReceber;
        this.padroesCriadosEm = builder.padroesCriadosEm;
        this.id = builder.id;
        this.dataCriacao = builder.dataCriacao;
        this.dataAtualizacao = builder.dataAtualizacao;
        this.categoriaPadraoReceita = builder.categoriaPadraoReceita;
        this.categoriaPadraoDespesa = builder.categoriaPadraoDespesa;
        this.empresa = builder.empresa;
    }

    public static ConfiguracaoFinanceiraEntityBuilder builderOf() {
        return new ConfiguracaoFinanceiraEntityBuilder();
    }
    public void setVersion(LocalDateTime version) {
        this.version = version;
    }

    public LocalDateTime getVersion() {
        return this.version;
    }
    public void setMultaPercentual(BigDecimal multaPercentual) {
        this.multaPercentual = multaPercentual;
    }
    public BigDecimal getMultaPercentual() {
        return this.multaPercentual;
    }
    public void setJurosMensalPercentual(BigDecimal jurosMensalPercentual) {
        this.jurosMensalPercentual = jurosMensalPercentual;
    }
    public BigDecimal getJurosMensalPercentual() {
        return this.jurosMensalPercentual;
    }

    public void setCarenciaDias(Integer carenciaDias) {
        this.carenciaDias = carenciaDias;
    }

    public Integer getCarenciaDias() {
        return this.carenciaDias;
    }

    public void setLembretesAtivos(Boolean lembretesAtivos) {
        this.lembretesAtivos = lembretesAtivos;
    }

    public Boolean getLembretesAtivos() {
        return this.lembretesAtivos;
    }

    public void setLembreteDiasAntes(Integer lembreteDiasAntes) {
        this.lembreteDiasAntes = lembreteDiasAntes;
    }

    public Integer getLembreteDiasAntes() {
        return this.lembreteDiasAntes;
    }

    public void setLembreteDiasDepois(String lembreteDiasDepois) {
        this.lembreteDiasDepois = lembreteDiasDepois;
    }

    public String getLembreteDiasDepois() {
        return this.lembreteDiasDepois;
    }

    public void setVencimentoPadraoDias(Integer vencimentoPadraoDias) {
        this.vencimentoPadraoDias = vencimentoPadraoDias;
    }

    public Integer getVencimentoPadraoDias() {
        return this.vencimentoPadraoDias;
    }

    public void setObservacaoPadraoReceber(String observacaoPadraoReceber) {
        this.observacaoPadraoReceber = observacaoPadraoReceber;
    }

    public String getObservacaoPadraoReceber() {
        return this.observacaoPadraoReceber;
    }

    public void setFormaPrevistaPadraoReceber(String formaPrevistaPadraoReceber) {
        this.formaPrevistaPadraoReceber = formaPrevistaPadraoReceber;
    }

    public String getFormaPrevistaPadraoReceber() {
        return this.formaPrevistaPadraoReceber;
    }

    public void setPadroesCriadosEm(LocalDateTime padroesCriadosEm) {
        this.padroesCriadosEm = padroesCriadosEm;
    }

    public LocalDateTime getPadroesCriadosEm() {
        return this.padroesCriadosEm;
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

    public void setCategoriaPadraoReceita(CategoriaFinanceiraEntity categoriaPadraoReceita) {
        this.categoriaPadraoReceita = categoriaPadraoReceita;
    }

    public CategoriaFinanceiraEntity getCategoriaPadraoReceita() {
        return this.categoriaPadraoReceita;
    }

    public void setCategoriaPadraoDespesa(CategoriaFinanceiraEntity categoriaPadraoDespesa) {
        this.categoriaPadraoDespesa = categoriaPadraoDespesa;
    }

    public CategoriaFinanceiraEntity getCategoriaPadraoDespesa() {
        return this.categoriaPadraoDespesa;
    }

    public void setEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
    }

    public EmpresaEntity getEmpresa() {
        return this.empresa;
    }

}
