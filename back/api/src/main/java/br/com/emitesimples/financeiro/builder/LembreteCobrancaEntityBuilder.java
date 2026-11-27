package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.LembreteCobranca;
import br.com.emitesimples.financeiro.entity.LembreteCobrancaEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * LembreteCobranca entity builder
*/
public class LembreteCobrancaEntityBuilder {
    public LocalDateTime version;
    public String tipo;
    public Integer dias;
    public LocalDateTime enviadoEm;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public TituloEntity titulo;
    public EmpresaEntity empresa;

    public LembreteCobrancaEntityBuilder() {
    }
    public LembreteCobrancaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public LembreteCobrancaEntityBuilder withTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public LembreteCobrancaEntityBuilder withDias(Integer dias) {
        this.dias = dias;
        return this;
    }

    public LembreteCobrancaEntityBuilder withEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
        return this;
    }

    public LembreteCobrancaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public LembreteCobrancaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public LembreteCobrancaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public LembreteCobrancaEntityBuilder withTitulo(TituloEntity titulo) {
        this.titulo = titulo;
        return this;
    }

    public LembreteCobrancaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public LembreteCobrancaEntity build() {
        return new LembreteCobrancaEntity(this);
    }

    public LembreteCobrancaEntityBuilder withModel(LembreteCobranca lembreteCobranca) {
        return this.withVersion(lembreteCobranca.getVersion())
                .withTipo(lembreteCobranca.getTipo())
                .withDias(lembreteCobranca.getDias())
                .withEnviadoEm(lembreteCobranca.getEnviadoEm())
                .withId(lembreteCobranca.getId())
                .withDataCriacao(lembreteCobranca.getDataCriacao())
                .withDataAtualizacao(lembreteCobranca.getDataAtualizacao())
                .withTitulo(TituloEntity.builderOf().withModel(lembreteCobranca.getTitulo()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(lembreteCobranca.getEmpresa()).build());
    }

    public List<LembreteCobrancaEntity> withModelList(List<LembreteCobranca> lembreteCobrancaList) {
        if (lembreteCobrancaList == null) {
            return null;
        }

        List<LembreteCobrancaEntity> list = new ArrayList<LembreteCobrancaEntity>(lembreteCobrancaList.size());
        for (LembreteCobranca lembreteCobranca: lembreteCobrancaList) {
           list.add(withModel(lembreteCobranca).build());
        }
        return list;
    }

}
