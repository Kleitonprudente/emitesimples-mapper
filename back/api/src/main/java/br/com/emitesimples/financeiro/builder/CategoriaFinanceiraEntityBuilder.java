package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CategoriaFinanceira;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * CategoriaFinanceira entity builder
*/
public class CategoriaFinanceiraEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String tipo;
    public String dicas;
    public Boolean ativo;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public CategoriaFinanceiraEntity categoriaPai;
    public EmpresaEntity empresa;

    public CategoriaFinanceiraEntityBuilder() {
    }
    public CategoriaFinanceiraEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withDicas(String dicas) {
        this.dicas = dicas;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withCategoriaPai(CategoriaFinanceiraEntity categoriaPai) {
        this.categoriaPai = categoriaPai;
        return this;
    }

    public CategoriaFinanceiraEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public CategoriaFinanceiraEntity build() {
        return new CategoriaFinanceiraEntity(this);
    }

    public CategoriaFinanceiraEntityBuilder withModel(CategoriaFinanceira categoriaFinanceira) {
        return this.withVersion(categoriaFinanceira.getVersion())
                .withNome(categoriaFinanceira.getNome())
                .withTipo(categoriaFinanceira.getTipo())
                .withDicas(categoriaFinanceira.getDicas())
                .withAtivo(categoriaFinanceira.getAtivo())
                .withId(categoriaFinanceira.getId())
                .withDataCriacao(categoriaFinanceira.getDataCriacao())
                .withDataAtualizacao(categoriaFinanceira.getDataAtualizacao())
                .withCategoriaPai(CategoriaFinanceiraEntity.builderOf().withModel(categoriaFinanceira.getCategoriaPai()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(categoriaFinanceira.getEmpresa()).build());
    }

    public List<CategoriaFinanceiraEntity> withModelList(List<CategoriaFinanceira> categoriaFinanceiraList) {
        if (categoriaFinanceiraList == null) {
            return null;
        }

        List<CategoriaFinanceiraEntity> list = new ArrayList<CategoriaFinanceiraEntity>(categoriaFinanceiraList.size());
        for (CategoriaFinanceira categoriaFinanceira: categoriaFinanceiraList) {
           list.add(withModel(categoriaFinanceira).build());
        }
        return list;
    }

}
