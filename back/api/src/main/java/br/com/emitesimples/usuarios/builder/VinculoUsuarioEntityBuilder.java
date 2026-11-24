package br.com.emitesimples.usuarios.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.VinculoUsuario;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * VinculoUsuario entity builder
*/
public class VinculoUsuarioEntityBuilder {
    public LocalDateTime version;
    public String papel;
    public Boolean podeCancelarNfse;
    public Boolean ativo;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public UsuarioEntity usuario;
    public EmpresaEntity empresa;

    public VinculoUsuarioEntityBuilder() {
    }
    public VinculoUsuarioEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public VinculoUsuarioEntityBuilder withPapel(String papel) {
        this.papel = papel;
        return this;
    }

    public VinculoUsuarioEntityBuilder withPodeCancelarNfse(Boolean podeCancelarNfse) {
        this.podeCancelarNfse = podeCancelarNfse;
        return this;
    }

    public VinculoUsuarioEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public VinculoUsuarioEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public VinculoUsuarioEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public VinculoUsuarioEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public VinculoUsuarioEntityBuilder withUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
        return this;
    }

    public VinculoUsuarioEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public VinculoUsuarioEntity build() {
        return new VinculoUsuarioEntity(this);
    }

    public VinculoUsuarioEntityBuilder withModel(VinculoUsuario vinculoUsuario) {
        return this.withVersion(vinculoUsuario.getVersion())
                .withPapel(vinculoUsuario.getPapel())
                .withPodeCancelarNfse(vinculoUsuario.getPodeCancelarNfse())
                .withAtivo(vinculoUsuario.getAtivo())
                .withId(vinculoUsuario.getId())
                .withDataCriacao(vinculoUsuario.getDataCriacao())
                .withDataAtualizacao(vinculoUsuario.getDataAtualizacao())
                .withUsuario(UsuarioEntity.builderOf().withModel(vinculoUsuario.getUsuario()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(vinculoUsuario.getEmpresa()).build());
    }

    public List<VinculoUsuarioEntity> withModelList(List<VinculoUsuario> vinculoUsuarioList) {
        if (vinculoUsuarioList == null) {
            return null;
        }

        List<VinculoUsuarioEntity> list = new ArrayList<VinculoUsuarioEntity>(vinculoUsuarioList.size());
        for (VinculoUsuario vinculoUsuario: vinculoUsuarioList) {
           list.add(withModel(vinculoUsuario).build());
        }
        return list;
    }

}
