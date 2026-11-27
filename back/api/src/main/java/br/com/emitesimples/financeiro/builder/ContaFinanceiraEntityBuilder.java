package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.ContaFinanceira;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * ContaFinanceira entity builder
*/
public class ContaFinanceiraEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String tipo;
    public Boolean ativo;
    public String codigoBanco;
    public String agencia;
    public String dadosDoBanco;
    public BigDecimal saldoInicial;
    public LocalDate dataSaldoInicial;
    public Boolean padraoDinheiro;
    public Boolean padraoPix;
    public Boolean padraoBoleto;
    public Boolean padraoCartao;
    public Boolean padraoTransferencia;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;

    public ContaFinanceiraEntityBuilder() {
    }
    public ContaFinanceiraEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public ContaFinanceiraEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public ContaFinanceiraEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public ContaFinanceiraEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public ContaFinanceiraEntityBuilder withCodigoBanco(String codigoBanco) {
        this.codigoBanco = codigoBanco;
        return this;
    }

    public ContaFinanceiraEntityBuilder withAgencia(String agencia) {
        this.agencia = agencia;
        return this;
    }

    public ContaFinanceiraEntityBuilder withDadosDoBanco(String dadosDoBanco) {
        this.dadosDoBanco = dadosDoBanco;
        return this;
    }
    public ContaFinanceiraEntityBuilder withSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
        return this;
    }

    public ContaFinanceiraEntityBuilder withDataSaldoInicial(LocalDate dataSaldoInicial) {
        this.dataSaldoInicial = dataSaldoInicial;
        return this;
    }

    public ContaFinanceiraEntityBuilder withPadraoDinheiro(Boolean padraoDinheiro) {
        this.padraoDinheiro = padraoDinheiro;
        return this;
    }

    public ContaFinanceiraEntityBuilder withPadraoPix(Boolean padraoPix) {
        this.padraoPix = padraoPix;
        return this;
    }

    public ContaFinanceiraEntityBuilder withPadraoBoleto(Boolean padraoBoleto) {
        this.padraoBoleto = padraoBoleto;
        return this;
    }

    public ContaFinanceiraEntityBuilder withPadraoCartao(Boolean padraoCartao) {
        this.padraoCartao = padraoCartao;
        return this;
    }

    public ContaFinanceiraEntityBuilder withPadraoTransferencia(Boolean padraoTransferencia) {
        this.padraoTransferencia = padraoTransferencia;
        return this;
    }

    public ContaFinanceiraEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ContaFinanceiraEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ContaFinanceiraEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ContaFinanceiraEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public ContaFinanceiraEntity build() {
        return new ContaFinanceiraEntity(this);
    }

    public ContaFinanceiraEntityBuilder withModel(ContaFinanceira contaFinanceira) {
        return this.withVersion(contaFinanceira.getVersion())
                .withNome(contaFinanceira.getNome())
                .withTipo(contaFinanceira.getTipo())
                .withAtivo(contaFinanceira.getAtivo())
                .withCodigoBanco(contaFinanceira.getCodigoBanco())
                .withAgencia(contaFinanceira.getAgencia())
                .withDadosDoBanco(contaFinanceira.getDadosDoBanco())
                .withSaldoInicial(contaFinanceira.getSaldoInicial())
                .withDataSaldoInicial(contaFinanceira.getDataSaldoInicial())
                .withPadraoDinheiro(contaFinanceira.getPadraoDinheiro())
                .withPadraoPix(contaFinanceira.getPadraoPix())
                .withPadraoBoleto(contaFinanceira.getPadraoBoleto())
                .withPadraoCartao(contaFinanceira.getPadraoCartao())
                .withPadraoTransferencia(contaFinanceira.getPadraoTransferencia())
                .withId(contaFinanceira.getId())
                .withDataCriacao(contaFinanceira.getDataCriacao())
                .withDataAtualizacao(contaFinanceira.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(contaFinanceira.getEmpresa()).build());
    }

    public List<ContaFinanceiraEntity> withModelList(List<ContaFinanceira> contaFinanceiraList) {
        if (contaFinanceiraList == null) {
            return null;
        }

        List<ContaFinanceiraEntity> list = new ArrayList<ContaFinanceiraEntity>(contaFinanceiraList.size());
        for (ContaFinanceira contaFinanceira: contaFinanceiraList) {
           list.add(withModel(contaFinanceira).build());
        }
        return list;
    }

}
