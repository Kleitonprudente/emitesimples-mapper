package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Movimento;
import br.com.emitesimples.financeiro.entity.MovimentoEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * Movimento entity builder
*/
public class MovimentoEntityBuilder {
    public LocalDateTime version;
    public String tipo;
    public String sentido;
    public LocalDate data;
    public String descricao;
    public String forma;
    public BigDecimal valor;
    public BigDecimal quitado;
    public BigDecimal juros;
    public BigDecimal multa;
    public BigDecimal desconto;
    public String idLote;
    public String idTransferencia;
    public String idEstornoDe;
    public String idCriadoPor;
    public String id;
    public LocalDateTime dataCriacao;
    public ContaFinanceiraEntity conta;
    public TituloEntity titulo;
    public CategoriaFinanceiraEntity categoria;
    public EmpresaEntity empresa;

    public MovimentoEntityBuilder() {
    }
    public MovimentoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public MovimentoEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public MovimentoEntityBuilder withSentido(String sentido) {
        this.sentido = sentido;
        return this;
    }

    public MovimentoEntityBuilder withData(LocalDate data) {
        this.data = data;
        return this;
    }

    public MovimentoEntityBuilder withDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    public MovimentoEntityBuilder withForma(String forma) {
        this.forma = forma;
        return this;
    }
    public MovimentoEntityBuilder withValor(BigDecimal valor) {
        this.valor = valor;
        return this;
    }
    public MovimentoEntityBuilder withQuitado(BigDecimal quitado) {
        this.quitado = quitado;
        return this;
    }
    public MovimentoEntityBuilder withJuros(BigDecimal juros) {
        this.juros = juros;
        return this;
    }
    public MovimentoEntityBuilder withMulta(BigDecimal multa) {
        this.multa = multa;
        return this;
    }
    public MovimentoEntityBuilder withDesconto(BigDecimal desconto) {
        this.desconto = desconto;
        return this;
    }

    public MovimentoEntityBuilder withIdLote(String idLote) {
        this.idLote = idLote;
        return this;
    }

    public MovimentoEntityBuilder withIdTransferencia(String idTransferencia) {
        this.idTransferencia = idTransferencia;
        return this;
    }

    public MovimentoEntityBuilder withIdEstornoDe(String idEstornoDe) {
        this.idEstornoDe = idEstornoDe;
        return this;
    }

    public MovimentoEntityBuilder withIdCriadoPor(String idCriadoPor) {
        this.idCriadoPor = idCriadoPor;
        return this;
    }

    public MovimentoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public MovimentoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public MovimentoEntityBuilder withConta(ContaFinanceiraEntity conta) {
        this.conta = conta;
        return this;
    }

    public MovimentoEntityBuilder withTitulo(TituloEntity titulo) {
        this.titulo = titulo;
        return this;
    }

    public MovimentoEntityBuilder withCategoria(CategoriaFinanceiraEntity categoria) {
        this.categoria = categoria;
        return this;
    }

    public MovimentoEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public MovimentoEntity build() {
        return new MovimentoEntity(this);
    }

    public MovimentoEntityBuilder withModel(Movimento movimento) {
        return this.withVersion(movimento.getVersion())
                .withTipo(movimento.getTipo())
                .withSentido(movimento.getSentido())
                .withData(movimento.getData())
                .withDescricao(movimento.getDescricao())
                .withForma(movimento.getForma())
                .withValor(movimento.getValor())
                .withQuitado(movimento.getQuitado())
                .withJuros(movimento.getJuros())
                .withMulta(movimento.getMulta())
                .withDesconto(movimento.getDesconto())
                .withIdLote(movimento.getIdLote())
                .withIdTransferencia(movimento.getIdTransferencia())
                .withIdEstornoDe(movimento.getIdEstornoDe())
                .withIdCriadoPor(movimento.getIdCriadoPor())
                .withId(movimento.getId())
                .withDataCriacao(movimento.getDataCriacao())
                .withConta(ContaFinanceiraEntity.builderOf().withModel(movimento.getConta()).build())
                .withTitulo(TituloEntity.builderOf().withModel(movimento.getTitulo()).build())
                .withCategoria(CategoriaFinanceiraEntity.builderOf().withModel(movimento.getCategoria()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(movimento.getEmpresa()).build());
    }

    public List<MovimentoEntity> withModelList(List<Movimento> movimentoList) {
        if (movimentoList == null) {
            return null;
        }

        List<MovimentoEntity> list = new ArrayList<MovimentoEntity>(movimentoList.size());
        for (Movimento movimento: movimentoList) {
           list.add(withModel(movimento).build());
        }
        return list;
    }

}
