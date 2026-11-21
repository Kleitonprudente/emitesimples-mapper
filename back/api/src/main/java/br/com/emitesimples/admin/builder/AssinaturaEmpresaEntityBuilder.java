package br.com.emitesimples.admin.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.admin.domain.AssinaturaEmpresa;
import br.com.emitesimples.admin.entity.AssinaturaEmpresaEntity;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDate;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.admin.entity.PlanoEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

/*
 * AssinaturaEmpresa entity builder
*/
public class AssinaturaEmpresaEntityBuilder {
    public LocalDateTime version;
    public String estado;
    public LocalDate dataInicio;
    public LocalDate dataFim;
    public String referenciaCrm;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public PlanoEntity plano;
    public UsuarioEntity vinculadaPor;

    public AssinaturaEmpresaEntityBuilder() {
    }
    public AssinaturaEmpresaEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withEstado(String estado) {
        this.estado = estado;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withReferenciaCrm(String referenciaCrm) {
        this.referenciaCrm = referenciaCrm;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withPlano(PlanoEntity plano) {
        this.plano = plano;
        return this;
    }

    public AssinaturaEmpresaEntityBuilder withVinculadaPor(UsuarioEntity vinculadaPor) {
        this.vinculadaPor = vinculadaPor;
        return this;
    }

    public AssinaturaEmpresaEntity build() {
        return new AssinaturaEmpresaEntity(this);
    }

    public AssinaturaEmpresaEntityBuilder withModel(AssinaturaEmpresa assinaturaEmpresa) {
        return this.withVersion(assinaturaEmpresa.getVersion())
                .withEstado(assinaturaEmpresa.getEstado())
                .withDataInicio(assinaturaEmpresa.getDataInicio())
                .withDataFim(assinaturaEmpresa.getDataFim())
                .withReferenciaCrm(assinaturaEmpresa.getReferenciaCrm())
                .withId(assinaturaEmpresa.getId())
                .withDataCriacao(assinaturaEmpresa.getDataCriacao())
                .withDataAtualizacao(assinaturaEmpresa.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(assinaturaEmpresa.getEmpresa()).build())
                .withPlano(PlanoEntity.builderOf().withModel(assinaturaEmpresa.getPlano()).build())
                .withVinculadaPor(UsuarioEntity.builderOf().withModel(assinaturaEmpresa.getVinculadaPor()).build());
    }

    public List<AssinaturaEmpresaEntity> withModelList(List<AssinaturaEmpresa> assinaturaEmpresaList) {
        if (assinaturaEmpresaList == null) {
            return null;
        }

        List<AssinaturaEmpresaEntity> list = new ArrayList<AssinaturaEmpresaEntity>(assinaturaEmpresaList.size());
        for (AssinaturaEmpresa assinaturaEmpresa: assinaturaEmpresaList) {
           list.add(withModel(assinaturaEmpresa).build());
        }
        return list;
    }

}
