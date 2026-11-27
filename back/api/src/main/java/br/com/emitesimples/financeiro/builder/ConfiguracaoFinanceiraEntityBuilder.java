package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.ConfiguracaoFinanceira;
import br.com.emitesimples.financeiro.entity.ConfiguracaoFinanceiraEntity;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * ConfiguracaoFinanceira entity builder
*/
public class ConfiguracaoFinanceiraEntityBuilder {
    public LocalDateTime version;
    public BigDecimal multaPercentual;
    public BigDecimal jurosMensalPercentual;
    public Integer carenciaDias;
    public Boolean lembretesAtivos;
    public Integer lembreteDiasAntes;
    public String lembreteDiasDepois;
    public Integer vencimentoPadraoDias;
    public String observacaoPadraoReceber;
    public String formaPrevistaPadraoReceber;
    public LocalDateTime padroesCriadosEm;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public CategoriaFinanceiraEntity categoriaPadraoReceita;
    public CategoriaFinanceiraEntity categoriaPadraoDespesa;
    public EmpresaEntity empresa;

    public ConfiguracaoFinanceiraEntityBuilder() {
    }
    public ConfiguracaoFinanceiraEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }
    public ConfiguracaoFinanceiraEntityBuilder withMultaPercentual(BigDecimal multaPercentual) {
        this.multaPercentual = multaPercentual;
        return this;
    }
    public ConfiguracaoFinanceiraEntityBuilder withJurosMensalPercentual(BigDecimal jurosMensalPercentual) {
        this.jurosMensalPercentual = jurosMensalPercentual;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withCarenciaDias(Integer carenciaDias) {
        this.carenciaDias = carenciaDias;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withLembretesAtivos(Boolean lembretesAtivos) {
        this.lembretesAtivos = lembretesAtivos;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withLembreteDiasAntes(Integer lembreteDiasAntes) {
        this.lembreteDiasAntes = lembreteDiasAntes;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withLembreteDiasDepois(String lembreteDiasDepois) {
        this.lembreteDiasDepois = lembreteDiasDepois;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withVencimentoPadraoDias(Integer vencimentoPadraoDias) {
        this.vencimentoPadraoDias = vencimentoPadraoDias;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withObservacaoPadraoReceber(String observacaoPadraoReceber) {
        this.observacaoPadraoReceber = observacaoPadraoReceber;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withFormaPrevistaPadraoReceber(String formaPrevistaPadraoReceber) {
        this.formaPrevistaPadraoReceber = formaPrevistaPadraoReceber;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withPadroesCriadosEm(LocalDateTime padroesCriadosEm) {
        this.padroesCriadosEm = padroesCriadosEm;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withCategoriaPadraoReceita(CategoriaFinanceiraEntity categoriaPadraoReceita) {
        this.categoriaPadraoReceita = categoriaPadraoReceita;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withCategoriaPadraoDespesa(CategoriaFinanceiraEntity categoriaPadraoDespesa) {
        this.categoriaPadraoDespesa = categoriaPadraoDespesa;
        return this;
    }

    public ConfiguracaoFinanceiraEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public ConfiguracaoFinanceiraEntity build() {
        return new ConfiguracaoFinanceiraEntity(this);
    }

    public ConfiguracaoFinanceiraEntityBuilder withModel(ConfiguracaoFinanceira configuracaoFinanceira) {
        return this.withVersion(configuracaoFinanceira.getVersion())
                .withMultaPercentual(configuracaoFinanceira.getMultaPercentual())
                .withJurosMensalPercentual(configuracaoFinanceira.getJurosMensalPercentual())
                .withCarenciaDias(configuracaoFinanceira.getCarenciaDias())
                .withLembretesAtivos(configuracaoFinanceira.getLembretesAtivos())
                .withLembreteDiasAntes(configuracaoFinanceira.getLembreteDiasAntes())
                .withLembreteDiasDepois(configuracaoFinanceira.getLembreteDiasDepois())
                .withVencimentoPadraoDias(configuracaoFinanceira.getVencimentoPadraoDias())
                .withObservacaoPadraoReceber(configuracaoFinanceira.getObservacaoPadraoReceber())
                .withFormaPrevistaPadraoReceber(configuracaoFinanceira.getFormaPrevistaPadraoReceber())
                .withPadroesCriadosEm(configuracaoFinanceira.getPadroesCriadosEm())
                .withId(configuracaoFinanceira.getId())
                .withDataCriacao(configuracaoFinanceira.getDataCriacao())
                .withDataAtualizacao(configuracaoFinanceira.getDataAtualizacao())
                .withCategoriaPadraoReceita(CategoriaFinanceiraEntity.builderOf().withModel(configuracaoFinanceira.getCategoriaPadraoReceita()).build())
                .withCategoriaPadraoDespesa(CategoriaFinanceiraEntity.builderOf().withModel(configuracaoFinanceira.getCategoriaPadraoDespesa()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(configuracaoFinanceira.getEmpresa()).build());
    }

    public List<ConfiguracaoFinanceiraEntity> withModelList(List<ConfiguracaoFinanceira> configuracaoFinanceiraList) {
        if (configuracaoFinanceiraList == null) {
            return null;
        }

        List<ConfiguracaoFinanceiraEntity> list = new ArrayList<ConfiguracaoFinanceiraEntity>(configuracaoFinanceiraList.size());
        for (ConfiguracaoFinanceira configuracaoFinanceira: configuracaoFinanceiraList) {
           list.add(withModel(configuracaoFinanceira).build());
        }
        return list;
    }

}
