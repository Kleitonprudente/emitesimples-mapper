package br.com.emitesimples.financeiro.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.financeiro.domain.CobrancaDestino;
import br.com.emitesimples.financeiro.entity.CobrancaDestinoEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.financeiro.entity.CredencialCobrancaEntity;

/*
 * CobrancaDestino entity builder
*/
public class CobrancaDestinoEntityBuilder {
    public LocalDateTime version;
    public String idEmpresa;
    public String segredoHash;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public CredencialCobrancaEntity credencial;

    public CobrancaDestinoEntityBuilder() {
    }
    public CobrancaDestinoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public CobrancaDestinoEntityBuilder withIdEmpresa(String idEmpresa) {
        this.idEmpresa = idEmpresa;
        return this;
    }

    public CobrancaDestinoEntityBuilder withSegredoHash(String segredoHash) {
        this.segredoHash = segredoHash;
        return this;
    }

    public CobrancaDestinoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public CobrancaDestinoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public CobrancaDestinoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public CobrancaDestinoEntityBuilder withCredencial(CredencialCobrancaEntity credencial) {
        this.credencial = credencial;
        return this;
    }

    public CobrancaDestinoEntity build() {
        return new CobrancaDestinoEntity(this);
    }

    public CobrancaDestinoEntityBuilder withModel(CobrancaDestino cobrancaDestino) {
        return this.withVersion(cobrancaDestino.getVersion())
                .withIdEmpresa(cobrancaDestino.getIdEmpresa())
                .withSegredoHash(cobrancaDestino.getSegredoHash())
                .withId(cobrancaDestino.getId())
                .withDataCriacao(cobrancaDestino.getDataCriacao())
                .withDataAtualizacao(cobrancaDestino.getDataAtualizacao())
                .withCredencial(CredencialCobrancaEntity.builderOf().withModel(cobrancaDestino.getCredencial()).build());
    }

    public List<CobrancaDestinoEntity> withModelList(List<CobrancaDestino> cobrancaDestinoList) {
        if (cobrancaDestinoList == null) {
            return null;
        }

        List<CobrancaDestinoEntity> list = new ArrayList<CobrancaDestinoEntity>(cobrancaDestinoList.size());
        for (CobrancaDestino cobrancaDestino: cobrancaDestinoList) {
           list.add(withModel(cobrancaDestino).build());
        }
        return list;
    }

}
