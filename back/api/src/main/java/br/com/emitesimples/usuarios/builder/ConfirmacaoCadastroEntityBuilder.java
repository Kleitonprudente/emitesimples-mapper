package br.com.emitesimples.usuarios.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.ConfirmacaoCadastro;
import br.com.emitesimples.usuarios.entity.ConfirmacaoCadastroEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

/*
 * ConfirmacaoCadastro entity builder
*/
public class ConfirmacaoCadastroEntityBuilder {
    public LocalDateTime version;
    public String tokenHash;
    public String email;
    public LocalDateTime dataExpiracao;
    public LocalDateTime dataConfirmacao;
    public String estado;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public UsuarioEntity usuario;
    public EmpresaEntity empresa;

    public ConfirmacaoCadastroEntityBuilder() {
    }
    public ConfirmacaoCadastroEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withDataConfirmacao(LocalDateTime dataConfirmacao) {
        this.dataConfirmacao = dataConfirmacao;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withEstado(String estado) {
        this.estado = estado;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
        return this;
    }

    public ConfirmacaoCadastroEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public ConfirmacaoCadastroEntity build() {
        return new ConfirmacaoCadastroEntity(this);
    }

    public ConfirmacaoCadastroEntityBuilder withModel(ConfirmacaoCadastro confirmacaoCadastro) {
        return this.withVersion(confirmacaoCadastro.getVersion())
                .withEmail(confirmacaoCadastro.getEmail())
                .withDataExpiracao(confirmacaoCadastro.getDataExpiracao())
                .withDataConfirmacao(confirmacaoCadastro.getDataConfirmacao())
                .withEstado(confirmacaoCadastro.getEstado())
                .withId(confirmacaoCadastro.getId())
                .withDataCriacao(confirmacaoCadastro.getDataCriacao())
                .withDataAtualizacao(confirmacaoCadastro.getDataAtualizacao())
                .withUsuario(UsuarioEntity.builderOf().withModel(confirmacaoCadastro.getUsuario()).build())
                .withEmpresa(EmpresaEntity.builderOf().withModel(confirmacaoCadastro.getEmpresa()).build());
    }

    public List<ConfirmacaoCadastroEntity> withModelList(List<ConfirmacaoCadastro> confirmacaoCadastroList) {
        if (confirmacaoCadastroList == null) {
            return null;
        }

        List<ConfirmacaoCadastroEntity> list = new ArrayList<ConfirmacaoCadastroEntity>(confirmacaoCadastroList.size());
        for (ConfirmacaoCadastro confirmacaoCadastro: confirmacaoCadastroList) {
           list.add(withModel(confirmacaoCadastro).build());
        }
        return list;
    }

}
