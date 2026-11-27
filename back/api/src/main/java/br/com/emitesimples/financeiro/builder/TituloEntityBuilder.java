package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.Titulo;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import java.math.BigDecimal;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * Titulo entity builder
*/
public class TituloEntityBuilder {
    public LocalDateTime version;
    public String tipo;
    public String descricao;
    public String observacao;
    public String idPessoa;
    public BigDecimal valor;
    public BigDecimal quitado;
    public BigDecimal multaPercentual;
    public BigDecimal jurosMensalPercentual;
    public LocalDate vencimento;
    public String formaPrevista;
    public Integer parcela;
    public Integer totalParcelas;
    public String idLancamento;
    public String origemTipo;
    public String idOrigem;
    public String origemNumero;
    public String situacao;
    public Boolean paraRevisao;
    public LocalDateTime canceladoEm;
    public LocalDateTime aprovadoEm;
    public String idAprovadoPor;
    public String idCriadoPor;
    public String idCriadoPorCliente;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public CategoriaFinanceiraEntity categoria;
    public EmpresaEntity empresa;

    public TituloEntityBuilder() {
    }
    public TituloEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public TituloEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public TituloEntityBuilder withDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    public TituloEntityBuilder withObservacao(String observacao) {
        this.observacao = observacao;
        return this;
    }

    public TituloEntityBuilder withIdPessoa(String idPessoa) {
        this.idPessoa = idPessoa;
        return this;
    }
    public TituloEntityBuilder withValor(BigDecimal valor) {
        this.valor = valor;
        return this;
    }
    public TituloEntityBuilder withQuitado(BigDecimal quitado) {
        this.quitado = quitado;
        return this;
    }
    public TituloEntityBuilder withMultaPercentual(BigDecimal multaPercentual) {
        this.multaPercentual = multaPercentual;
        return this;
    }
    public TituloEntityBuilder withJurosMensalPercentual(BigDecimal jurosMensalPercentual) {
        this.jurosMensalPercentual = jurosMensalPercentual;
        return this;
    }

    public TituloEntityBuilder withVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
        return this;
    }

    public TituloEntityBuilder withFormaPrevista(String formaPrevista) {
        this.formaPrevista = formaPrevista;
        return this;
    }

    public TituloEntityBuilder withParcela(Integer parcela) {
        this.parcela = parcela;
        return this;
    }

    public TituloEntityBuilder withTotalParcelas(Integer totalParcelas) {
        this.totalParcelas = totalParcelas;
        return this;
    }

    public TituloEntityBuilder withIdLancamento(String idLancamento) {
        this.idLancamento = idLancamento;
        return this;
    }

    public TituloEntityBuilder withOrigemTipo(String origemTipo) {
        this.origemTipo = origemTipo;
        return this;
    }

    public TituloEntityBuilder withIdOrigem(String idOrigem) {
        this.idOrigem = idOrigem;
        return this;
    }

    public TituloEntityBuilder withOrigemNumero(String origemNumero) {
        this.origemNumero = origemNumero;
        return this;
    }

    public TituloEntityBuilder withSituacao(String situacao) {
        this.situacao = situacao;
        return this;
    }

    public TituloEntityBuilder withParaRevisao(Boolean paraRevisao) {
        this.paraRevisao = paraRevisao;
        return this;
    }

    public TituloEntityBuilder withCanceladoEm(LocalDateTime canceladoEm) {
        this.canceladoEm = canceladoEm;
        return this;
    }

    public TituloEntityBuilder withAprovadoEm(LocalDateTime aprovadoEm) {
        this.aprovadoEm = aprovadoEm;
        return this;
    }

    public TituloEntityBuilder withIdAprovadoPor(String idAprovadoPor) {
        this.idAprovadoPor = idAprovadoPor;
        return this;
    }

    public TituloEntityBuilder withIdCriadoPor(String idCriadoPor) {
        this.idCriadoPor = idCriadoPor;
        return this;
    }

    public TituloEntityBuilder withIdCriadoPorCliente(String idCriadoPorCliente) {
        this.idCriadoPorCliente = idCriadoPorCliente;
        return this;
    }

    public TituloEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public TituloEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public TituloEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public TituloEntityBuilder withCategoria(CategoriaFinanceiraEntity categoria) {
        this.categoria = categoria;
        return this;
    }

    public TituloEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public TituloEntity build() {
        return new TituloEntity(this);
    }

    public TituloEntityBuilder withModel(Titulo titulo) {
        return this.withVersion(titulo.getVersion())
                .withTipo(titulo.getTipo())
                .withDescricao(titulo.getDescricao())
                .withObservacao(titulo.getObservacao())
                .withIdPessoa(titulo.getIdPessoa())
                .withValor(titulo.getValor())
                .withQuitado(titulo.getQuitado())
                .withMultaPercentual(titulo.getMultaPercentual())
                .withJurosMensalPercentual(titulo.getJurosMensalPercentual())
                .withVencimento(titulo.getVencimento())
                .withFormaPrevista(titulo.getFormaPrevista())
                .withParcela(titulo.getParcela())
                .withTotalParcelas(titulo.getTotalParcelas())
                .withIdLancamento(titulo.getIdLancamento())
                .withOrigemTipo(titulo.getOrigemTipo())
                .withIdOrigem(titulo.getIdOrigem())
                .withOrigemNumero(titulo.getOrigemNumero())
                .withSituacao(titulo.getSituacao())
                .withParaRevisao(titulo.getParaRevisao())
                .withCanceladoEm(titulo.getCanceladoEm())
                .withAprovadoEm(titulo.getAprovadoEm())
                .withIdAprovadoPor(titulo.getIdAprovadoPor())
                .withIdCriadoPor(titulo.getIdCriadoPor())
                .withIdCriadoPorCliente(titulo.getIdCriadoPorCliente())
                .withId(titulo.getId())
                .withDataCriacao(titulo.getDataCriacao())
                .withDataAtualizacao(titulo.getDataAtualizacao())
                .withCategoria(CategoriaFinanceiraEntity.builderOf().withModel(titulo.getCategoria()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(titulo.getEmpresa()).build());
    }

    public List<TituloEntity> withModelList(List<Titulo> tituloList) {
        if (tituloList == null) {
            return null;
        }

        List<TituloEntity> list = new ArrayList<TituloEntity>(tituloList.size());
        for (Titulo titulo: tituloList) {
           list.add(withModel(titulo).build());
        }
        return list;
    }

}
