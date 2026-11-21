package br.com.emitesimples.configuracao.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.configuracao.domain.AtividadeEmpresa;
import br.com.emitesimples.configuracao.entity.AtividadeEmpresaEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * AtividadeEmpresa entity builder
*/
public class AtividadeEmpresaEntityBuilder {
    public LocalDateTime version;
    public String tipo;
    public String codigoCnae;
    public String descricao;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;

    public AtividadeEmpresaEntityBuilder() {
    }
    public AtividadeEmpresaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public AtividadeEmpresaEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public AtividadeEmpresaEntityBuilder withCodigoCnae(String codigoCnae) {
        this.codigoCnae = codigoCnae;
        return this;
    }

    public AtividadeEmpresaEntityBuilder withDescricao(String descricao) {
        this.descricao = descricao;
        return this;
    }

    public AtividadeEmpresaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public AtividadeEmpresaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public AtividadeEmpresaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public AtividadeEmpresaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public AtividadeEmpresaEntity build() {
        return new AtividadeEmpresaEntity(this);
    }

    public AtividadeEmpresaEntityBuilder withModel(AtividadeEmpresa atividadeEmpresa) {
        return this.withVersion(atividadeEmpresa.getVersion())
                .withTipo(atividadeEmpresa.getTipo())
                .withCodigoCnae(atividadeEmpresa.getCodigoCnae())
                .withDescricao(atividadeEmpresa.getDescricao())
                .withId(atividadeEmpresa.getId())
                .withDataCriacao(atividadeEmpresa.getDataCriacao())
                .withDataAtualizacao(atividadeEmpresa.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(atividadeEmpresa.getEmpresa()).build());
    }

    public List<AtividadeEmpresaEntity> withModelList(List<AtividadeEmpresa> atividadeEmpresaList) {
        if (atividadeEmpresaList == null) {
            return null;
        }

        List<AtividadeEmpresaEntity> list = new ArrayList<AtividadeEmpresaEntity>(atividadeEmpresaList.size());
        for (AtividadeEmpresa atividadeEmpresa: atividadeEmpresaList) {
           list.add(withModel(atividadeEmpresa).build());
        }
        return list;
    }

}
