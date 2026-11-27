package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CondicaoPagamento;
import br.com.emitesimples.financeiro.entity.CondicaoPagamentoEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * CondicaoPagamento entity builder
*/
public class CondicaoPagamentoEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String forma;
    public Boolean ativo;
    public Integer parcelas;
    public Integer diasPrimeiroVencimento;
    public Integer intervaloDias;
    public Boolean receberNaEmissao;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;

    public CondicaoPagamentoEntityBuilder() {
    }
    public CondicaoPagamentoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withForma(String forma) {
        this.forma = forma;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withParcelas(Integer parcelas) {
        this.parcelas = parcelas;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withDiasPrimeiroVencimento(Integer diasPrimeiroVencimento) {
        this.diasPrimeiroVencimento = diasPrimeiroVencimento;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withIntervaloDias(Integer intervaloDias) {
        this.intervaloDias = intervaloDias;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withReceberNaEmissao(Boolean receberNaEmissao) {
        this.receberNaEmissao = receberNaEmissao;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public CondicaoPagamentoEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public CondicaoPagamentoEntity build() {
        return new CondicaoPagamentoEntity(this);
    }

    public CondicaoPagamentoEntityBuilder withModel(CondicaoPagamento condicaoPagamento) {
        return this.withVersion(condicaoPagamento.getVersion())
                .withNome(condicaoPagamento.getNome())
                .withForma(condicaoPagamento.getForma())
                .withAtivo(condicaoPagamento.getAtivo())
                .withParcelas(condicaoPagamento.getParcelas())
                .withDiasPrimeiroVencimento(condicaoPagamento.getDiasPrimeiroVencimento())
                .withIntervaloDias(condicaoPagamento.getIntervaloDias())
                .withReceberNaEmissao(condicaoPagamento.getReceberNaEmissao())
                .withId(condicaoPagamento.getId())
                .withDataCriacao(condicaoPagamento.getDataCriacao())
                .withDataAtualizacao(condicaoPagamento.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(condicaoPagamento.getEmpresa()).build());
    }

    public List<CondicaoPagamentoEntity> withModelList(List<CondicaoPagamento> condicaoPagamentoList) {
        if (condicaoPagamentoList == null) {
            return null;
        }

        List<CondicaoPagamentoEntity> list = new ArrayList<CondicaoPagamentoEntity>(condicaoPagamentoList.size());
        for (CondicaoPagamento condicaoPagamento: condicaoPagamentoList) {
           list.add(withModel(condicaoPagamento).build());
        }
        return list;
    }

}
