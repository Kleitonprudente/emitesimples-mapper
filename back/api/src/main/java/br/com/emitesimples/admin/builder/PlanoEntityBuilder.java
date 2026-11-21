package br.com.emitesimples.admin.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.Plano;
import br.com.emitesimples.admin.entity.PlanoEntity;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

/*
 * Plano entity builder
*/
public class PlanoEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String descricao;
    public BigDecimal valor;
    public String periodicidade;
    public Boolean ativo;
    public String referenciaCrm;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;

    public PlanoEntityBuilder() {
    }
    public PlanoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public PlanoEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public PlanoEntityBuilder withDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }
    public PlanoEntityBuilder withValor(BigDecimal valor) {
        this.valor = valor;
        return this;
    }

    public PlanoEntityBuilder withPeriodicidade(String periodicidade) {
        this.periodicidade = periodicidade;
        return this;
    }

    public PlanoEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public PlanoEntityBuilder withReferenciaCrm(String referenciaCrm) {
        this.referenciaCrm = referenciaCrm;
        return this;
    }

    public PlanoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public PlanoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public PlanoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public PlanoEntity build() {
        return new PlanoEntity(this);
    }

    public PlanoEntityBuilder withModel(Plano plano) {
        return this.withVersion(plano.getVersion())
                .withNome(plano.getNome())
                .withDescricao(plano.getDescricao())
                .withValor(plano.getValor())
                .withPeriodicidade(plano.getPeriodicidade())
                .withAtivo(plano.getAtivo())
                .withReferenciaCrm(plano.getReferenciaCrm())
                .withId(plano.getId())
                .withDataCriacao(plano.getDataCriacao())
                .withDataAtualizacao(plano.getDataAtualizacao());
    }

    public List<PlanoEntity> withModelList(List<Plano> planoList) {
        if (planoList == null) {
            return null;
        }

        List<PlanoEntity> list = new ArrayList<PlanoEntity>(planoList.size());
        for (Plano plano: planoList) {
           list.add(withModel(plano).build());
        }
        return list;
    }

}
