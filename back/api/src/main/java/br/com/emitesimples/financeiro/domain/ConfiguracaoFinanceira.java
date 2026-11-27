package br.com.emitesimples.financeiro.domain;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import br.com.emitesimples.configuracao.domain.Empresa;

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
public class ConfiguracaoFinanceira {

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime version;

    @NotNull(message = "The attribute multaPercentual can't be null.")
    private BigDecimal multaPercentual;

    @NotNull(message = "The attribute jurosMensalPercentual can't be null.")
    private BigDecimal jurosMensalPercentual;

    @NotNull(message = "The attribute carenciaDias can't be null.")
    private Integer carenciaDias;

    @NotNull(message = "The attribute lembretesAtivos can't be null.")
    private Boolean lembretesAtivos;

    @NotNull(message = "The attribute lembreteDiasAntes can't be null.")
    private Integer lembreteDiasAntes;

    @Size(max = 40, message = "Attribute lembreteDiasDepois must have 40 characters maximum.")
    private String lembreteDiasDepois;

    @NotNull(message = "The attribute vencimentoPadraoDias can't be null.")
    private Integer vencimentoPadraoDias;

    @Size(max = 255, message = "Attribute observacaoPadraoReceber must have 255 characters maximum.")
    private String observacaoPadraoReceber;

    @Size(max = 20, message = "Attribute formaPrevistaPadraoReceber must have 20 characters maximum.")
    private String formaPrevistaPadraoReceber;

    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime padroesCriadosEm;

    @NotEmpty(message = "The attribute id can't be empty.")
    @Size(max = 36, message = "Attribute id must have 36 characters maximum.")
    private String id;

    @NotNull(message = "The attribute dataCriacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    @NotNull(message = "The attribute dataAtualizacao can't be null.")
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    private CategoriaFinanceira categoriaPadraoReceita;

    private CategoriaFinanceira categoriaPadraoDespesa;

    @NotNull(message = "The attribute empresa can't be null.")
    private Empresa empresa;

    public ConfiguracaoFinanceira() {
        this.version = null;
        this.multaPercentual = null;
        this.jurosMensalPercentual = null;
        this.carenciaDias = null;
        this.lembretesAtivos = null;
        this.lembreteDiasAntes = null;
        this.lembreteDiasDepois = null;
        this.vencimentoPadraoDias = null;
        this.observacaoPadraoReceber = null;
        this.formaPrevistaPadraoReceber = null;
        this.padroesCriadosEm = null;
        this.id = null;
        this.dataCriacao = null;
        this.dataAtualizacao = null;
        this.categoriaPadraoReceita = null;
        this.categoriaPadraoDespesa = null;
        this.empresa = null;
    }
    public ConfiguracaoFinanceira(
        LocalDateTime version,
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
        CategoriaFinanceira categoriaPadraoReceita,
        CategoriaFinanceira categoriaPadraoDespesa,
        Empresa empresa
    ) {
        this.version = version;
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

    public void setCategoriaPadraoReceita(CategoriaFinanceira categoriaPadraoReceita) {
        this.categoriaPadraoReceita = categoriaPadraoReceita;
    }

    public CategoriaFinanceira getCategoriaPadraoReceita() {
        return this.categoriaPadraoReceita;
    }

    public void setCategoriaPadraoDespesa(CategoriaFinanceira categoriaPadraoDespesa) {
        this.categoriaPadraoDespesa = categoriaPadraoDespesa;
    }

    public CategoriaFinanceira getCategoriaPadraoDespesa() {
        return this.categoriaPadraoDespesa;
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
            this.multaPercentual != null || 
            this.jurosMensalPercentual != null || 
            this.carenciaDias != null || 
            this.lembretesAtivos != null || 
            this.lembreteDiasAntes != null || 
            this.lembreteDiasDepois != null || 
            this.vencimentoPadraoDias != null || 
            this.observacaoPadraoReceber != null || 
            this.formaPrevistaPadraoReceber != null || 
            this.padroesCriadosEm != null || 
            this.dataCriacao != null || 
            this.dataAtualizacao != null || 
            this.categoriaPadraoReceita != null || 
            this.categoriaPadraoDespesa != null || 
            this.empresa != null;
    }

}
