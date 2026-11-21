package br.com.emitesimples.admin.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.HistoricoSituacaoEmpresa;
import br.com.emitesimples.admin.entity.HistoricoSituacaoEmpresaEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

/*
 * HistoricoSituacaoEmpresa entity builder
*/
public class HistoricoSituacaoEmpresaEntityBuilder {
    public LocalDateTime version;
    public String situacaoAnterior;
    public String situacaoNova;
    public String motivo;
    public String origem;
    public LocalDateTime dataHora;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public UsuarioEntity responsavel;

    public HistoricoSituacaoEmpresaEntityBuilder() {
    }
    public HistoricoSituacaoEmpresaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withSituacaoAnterior(String situacaoAnterior) {
        this.situacaoAnterior = situacaoAnterior;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withSituacaoNova(String situacaoNova) {
        this.situacaoNova = situacaoNova;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withMotivo(String motivo) {
        this.motivo = motivo;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withOrigem(String origem) {
        this.origem = origem;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public HistoricoSituacaoEmpresaEntityBuilder withResponsavel(UsuarioEntity responsavel) {
        this.responsavel = responsavel;
        return this;
    }

    public HistoricoSituacaoEmpresaEntity build() {
        return new HistoricoSituacaoEmpresaEntity(this);
    }

    public HistoricoSituacaoEmpresaEntityBuilder withModel(HistoricoSituacaoEmpresa historicoSituacaoEmpresa) {
        return this.withVersion(historicoSituacaoEmpresa.getVersion())
                .withSituacaoAnterior(historicoSituacaoEmpresa.getSituacaoAnterior())
                .withSituacaoNova(historicoSituacaoEmpresa.getSituacaoNova())
                .withMotivo(historicoSituacaoEmpresa.getMotivo())
                .withOrigem(historicoSituacaoEmpresa.getOrigem())
                .withDataHora(historicoSituacaoEmpresa.getDataHora())
                .withId(historicoSituacaoEmpresa.getId())
                .withDataCriacao(historicoSituacaoEmpresa.getDataCriacao())
                .withDataAtualizacao(historicoSituacaoEmpresa.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(historicoSituacaoEmpresa.getEmpresa()).build())
                .withResponsavel(UsuarioEntity.builderOf().withModel(historicoSituacaoEmpresa.getResponsavel()).build());
    }

    public List<HistoricoSituacaoEmpresaEntity> withModelList(List<HistoricoSituacaoEmpresa> historicoSituacaoEmpresaList) {
        if (historicoSituacaoEmpresaList == null) {
            return null;
        }

        List<HistoricoSituacaoEmpresaEntity> list = new ArrayList<HistoricoSituacaoEmpresaEntity>(historicoSituacaoEmpresaList.size());
        for (HistoricoSituacaoEmpresa historicoSituacaoEmpresa: historicoSituacaoEmpresaList) {
           list.add(withModel(historicoSituacaoEmpresa).build());
        }
        return list;
    }

}
