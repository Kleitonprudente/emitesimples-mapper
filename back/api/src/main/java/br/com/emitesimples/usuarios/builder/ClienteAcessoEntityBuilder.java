package br.com.emitesimples.usuarios.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.ClienteAcesso;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import java.util.List;
import java.util.ArrayList;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

/*
 * ClienteAcesso entity builder
*/
public class ClienteAcessoEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String clientId;
    public String segredoHash;
    public String scopes;
    public Boolean ativo;
    public LocalDateTime dataUltimoUso;
    public LocalDateTime dataExpiracao;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;
    public EmpresaEntity empresa;
    public UsuarioEntity usuario;

    public ClienteAcessoEntityBuilder() {
    }
    public ClienteAcessoEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public ClienteAcessoEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public ClienteAcessoEntityBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public ClienteAcessoEntityBuilder withSegredoHash(String segredoHash) {
        this.segredoHash = segredoHash;
        return this;
    }

    public ClienteAcessoEntityBuilder withScopes(String scopes) {
        this.scopes = scopes;
        return this;
    }

    public ClienteAcessoEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public ClienteAcessoEntityBuilder withDataUltimoUso(LocalDateTime dataUltimoUso) {
        this.dataUltimoUso = dataUltimoUso;
        return this;
    }

    public ClienteAcessoEntityBuilder withDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
        return this;
    }

    public ClienteAcessoEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public ClienteAcessoEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public ClienteAcessoEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public ClienteAcessoEntityBuilder withEmpresa(EmpresaEntity empresa) {
        this.empresa = empresa;
        return this;
    }

    public ClienteAcessoEntityBuilder withUsuario(UsuarioEntity usuario) {
        this.usuario = usuario;
        return this;
    }

    public ClienteAcessoEntity build() {
        return new ClienteAcessoEntity(this);
    }

    public ClienteAcessoEntityBuilder withModel(ClienteAcesso clienteAcesso) {
        return this.withVersion(clienteAcesso.getVersion())
                .withNome(clienteAcesso.getNome())
                .withClientId(clienteAcesso.getClientId())
                .withScopes(clienteAcesso.getScopes())
                .withAtivo(clienteAcesso.getAtivo())
                .withDataUltimoUso(clienteAcesso.getDataUltimoUso())
                .withDataExpiracao(clienteAcesso.getDataExpiracao())
                .withId(clienteAcesso.getId())
                .withDataCriacao(clienteAcesso.getDataCriacao())
                .withDataAtualizacao(clienteAcesso.getDataAtualizacao())
                .withEmpresa(EmpresaEntity.builderOf().withModel(clienteAcesso.getEmpresa()).build())
                .withUsuario(UsuarioEntity.builderOf().withModel(clienteAcesso.getUsuario()).build());
    }

    public List<ClienteAcessoEntity> withModelList(List<ClienteAcesso> clienteAcessoList) {
        if (clienteAcessoList == null) {
            return null;
        }

        List<ClienteAcessoEntity> list = new ArrayList<ClienteAcessoEntity>(clienteAcessoList.size());
        for (ClienteAcesso clienteAcesso: clienteAcessoList) {
           list.add(withModel(clienteAcesso).build());
        }
        return list;
    }

}
