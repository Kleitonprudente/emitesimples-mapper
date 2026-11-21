package br.com.emitesimples.cadastros.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.cadastros.domain.ImportacaoMunicipio;
import br.com.emitesimples.cadastros.entity.ImportacaoMunicipioEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

/*
 * ImportacaoMunicipio entity builder
*/
public class ImportacaoMunicipioEntityBuilder {
    public LocalDateTime version;
    public String nomeArquivo;
    public String codificacaoDetectada;
    public String separadorDetectado;
    public String estado;
    public LocalDateTime dataHoraInicio;
    public LocalDateTime dataHoraFim;
    public Integer linhasLidas;
    public Integer linhasInseridas;
    public Integer linhasAtualizadas;
    public Integer linhasIguais;
    public Integer linhasRejeitadas;
    public String resumoRejeicoes;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public UsuarioEntity usuario;

    public ImportacaoMunicipioEntityBuilder() {
    }
    public ImportacaoMunicipioEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withCodificacaoDetectada(String codificacaoDetectada) {
        this.codificacaoDetectada = codificacaoDetectada;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withSeparadorDetectado(String separadorDetectado) {
        this.separadorDetectado = separadorDetectado;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withEstado(String estado) {
        this.estado = estado;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withDataHoraInicio(LocalDateTime dataHoraInicio) {
        this.dataHoraInicio = dataHoraInicio;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withDataHoraFim(LocalDateTime dataHoraFim) {
        this.dataHoraFim = dataHoraFim;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withLinhasLidas(Integer linhasLidas) {
        this.linhasLidas = linhasLidas;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withLinhasInseridas(Integer linhasInseridas) {
        this.linhasInseridas = linhasInseridas;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withLinhasAtualizadas(Integer linhasAtualizadas) {
        this.linhasAtualizadas = linhasAtualizadas;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withLinhasIguais(Integer linhasIguais) {
        this.linhasIguais = linhasIguais;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withLinhasRejeitadas(Integer linhasRejeitadas) {
        this.linhasRejeitadas = linhasRejeitadas;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withResumoRejeicoes(String resumoRejeicoes) {
        this.resumoRejeicoes = resumoRejeicoes;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ImportacaoMunicipioEntityBuilder withUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
        return this;
    }

    public ImportacaoMunicipioEntity build() {
        return new ImportacaoMunicipioEntity(this);
    }

    public ImportacaoMunicipioEntityBuilder withModel(ImportacaoMunicipio importacaoMunicipio) {
        return this.withVersion(importacaoMunicipio.getVersion())
                .withNomeArquivo(importacaoMunicipio.getNomeArquivo())
                .withCodificacaoDetectada(importacaoMunicipio.getCodificacaoDetectada())
                .withSeparadorDetectado(importacaoMunicipio.getSeparadorDetectado())
                .withEstado(importacaoMunicipio.getEstado())
                .withDataHoraInicio(importacaoMunicipio.getDataHoraInicio())
                .withDataHoraFim(importacaoMunicipio.getDataHoraFim())
                .withLinhasLidas(importacaoMunicipio.getLinhasLidas())
                .withLinhasInseridas(importacaoMunicipio.getLinhasInseridas())
                .withLinhasAtualizadas(importacaoMunicipio.getLinhasAtualizadas())
                .withLinhasIguais(importacaoMunicipio.getLinhasIguais())
                .withLinhasRejeitadas(importacaoMunicipio.getLinhasRejeitadas())
                .withResumoRejeicoes(importacaoMunicipio.getResumoRejeicoes())
                .withId(importacaoMunicipio.getId())
                .withDataCriacao(importacaoMunicipio.getDataCriacao())
                .withDataAtualizacao(importacaoMunicipio.getDataAtualizacao())
                .withUsuario(UsuarioEntity.builderOf().withModel(importacaoMunicipio.getUsuario()).build());
    }

    public List<ImportacaoMunicipioEntity> withModelList(List<ImportacaoMunicipio> importacaoMunicipioList) {
        if (importacaoMunicipioList == null) {
            return null;
        }

        List<ImportacaoMunicipioEntity> list = new ArrayList<ImportacaoMunicipioEntity>(importacaoMunicipioList.size());
        for (ImportacaoMunicipio importacaoMunicipio: importacaoMunicipioList) {
           list.add(withModel(importacaoMunicipio).build());
        }
        return list;
    }

}
