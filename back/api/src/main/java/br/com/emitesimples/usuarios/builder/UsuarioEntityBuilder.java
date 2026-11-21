package br.com.emitesimples.usuarios.builder;

import java.time.LocalDateTime;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import java.util.List;
import java.util.ArrayList;

/*
 * Usuario entity builder
*/
public class UsuarioEntityBuilder {
    public LocalDateTime version;
    public String nome;
    public String email;
    public String whatsapp;
    public String senhaHash;
    public Boolean ativo;
    public Boolean administradorPlataforma;
    public LocalDateTime dataUltimoAcesso;
    public String id;
    public LocalDateTime dataCriacao;
    public LocalDateTime dataAtualizacao;

    public UsuarioEntityBuilder() {
    }
    public UsuarioEntityBuilder withVersion(LocalDateTime version) {
        this.version = version;
        return this;
    }

    public UsuarioEntityBuilder withNome(String nome) {
        this.nome = nome;
        return this;
    }

    public UsuarioEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UsuarioEntityBuilder withWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
        return this;
    }

    public UsuarioEntityBuilder withSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
        return this;
    }

    public UsuarioEntityBuilder withAtivo(Boolean ativo) {
        this.ativo = ativo;
        return this;
    }

    public UsuarioEntityBuilder withAdministradorPlataforma(Boolean administradorPlataforma) {
        this.administradorPlataforma = administradorPlataforma;
        return this;
    }

    public UsuarioEntityBuilder withDataUltimoAcesso(LocalDateTime dataUltimoAcesso) {
        this.dataUltimoAcesso = dataUltimoAcesso;
        return this;
    }

    public UsuarioEntityBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public UsuarioEntityBuilder withDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
        return this;
    }

    public UsuarioEntityBuilder withDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
        return this;
    }

    public UsuarioEntity build() {
        return new UsuarioEntity(this);
    }

    public UsuarioEntityBuilder withModel(Usuario usuario) {
        return this.withVersion(usuario.getVersion())
                .withNome(usuario.getNome())
                .withEmail(usuario.getEmail())
                .withWhatsapp(usuario.getWhatsapp())
                .withAtivo(usuario.getAtivo())
                .withAdministradorPlataforma(usuario.getAdministradorPlataforma())
                .withDataUltimoAcesso(usuario.getDataUltimoAcesso())
                .withId(usuario.getId())
                .withDataCriacao(usuario.getDataCriacao())
                .withDataAtualizacao(usuario.getDataAtualizacao());
    }

    public List<UsuarioEntity> withModelList(List<Usuario> usuarioList) {
        if (usuarioList == null) {
            return null;
        }

        List<UsuarioEntity> list = new ArrayList<UsuarioEntity>(usuarioList.size());
        for (Usuario usuario: usuarioList) {
           list.add(withModel(usuario).build());
        }
        return list;
    }

}
