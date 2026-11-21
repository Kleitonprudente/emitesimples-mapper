package br.com.emitesimples.cadastros.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.Municipio;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import java.util.List;
import java.util.ArrayList;

/*
 * Municipio entity builder
*/
public class MunicipioEntityBuilder {
    public LocalDateTime version;
    public String codigoIbge;
    public String nome;
    public String uf;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;

    public MunicipioEntityBuilder() {
    }
    public MunicipioEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public MunicipioEntityBuilder withCodigoIbge(String codigoIbge) {
        this.codigoIbge = codigoIbge;
        return this;
    }

    public MunicipioEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public MunicipioEntityBuilder withUf(String uf) {
        this.uf = uf;
        return this;
    }

    public MunicipioEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public MunicipioEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public MunicipioEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public MunicipioEntity build() {
        return new MunicipioEntity(this);
    }

    public MunicipioEntityBuilder withModel(Municipio municipio) {
        return this.withVersion(municipio.getVersion())
                .withCodigoIbge(municipio.getCodigoIbge())
                .withNome(municipio.getNome())
                .withUf(municipio.getUf())
                .withId(municipio.getId())
                .withDataCriacao(municipio.getDataCriacao())
                .withDataAtualizacao(municipio.getDataAtualizacao());
    }

    public List<MunicipioEntity> withModelList(List<Municipio> municipioList) {
        if (municipioList == null) {
            return null;
        }

        List<MunicipioEntity> list = new ArrayList<MunicipioEntity>(municipioList.size());
        for (Municipio municipio: municipioList) {
           list.add(withModel(municipio).build());
        }
        return list;
    }

}
