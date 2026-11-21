package br.com.emitesimples.usuarios.mapper;

import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.builder.UsuarioEntityBuilder;
import br.com.emitesimples.usuarios.repository.UsuarioRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;

@ApplicationScoped
public class UsuarioMapper {

    @Inject UsuarioRepository repository;

    public Usuario entityToApi(UsuarioEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Usuario usuario = new Usuario();

        usuario.setVersion( entity.getVersion() );
        usuario.setNome( entity.getNome() );
        usuario.setEmail( entity.getEmail() );
        usuario.setWhatsapp( entity.getWhatsapp() );
        usuario.setAtivo( entity.getAtivo() );
        usuario.setAdministradorPlataforma( entity.getAdministradorPlataforma() );
        usuario.setDataUltimoAcesso( entity.getDataUltimoAcesso() );
        usuario.setId( entity.getId() );
        usuario.setDataCriacao( entity.getDataCriacao() );
        usuario.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(usuario);

        return usuario;
    }

    public UsuarioEntity apiToEntity(Usuario api) {
        if ( api == null ) {
            return null;
        }
        UsuarioEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Usuario found for Id: " + api.getId()));
            }
        } else {
            UsuarioEntityBuilder usuarioBuilder = UsuarioEntity.builderOf();

            usuarioBuilder.version = api.getVersion();
            usuarioBuilder.nome = api.getNome();
            usuarioBuilder.email = api.getEmail();
            usuarioBuilder.whatsapp = api.getWhatsapp();
            usuarioBuilder.ativo = api.getAtivo();
            usuarioBuilder.administradorPlataforma = api.getAdministradorPlataforma();
            usuarioBuilder.dataUltimoAcesso = api.getDataUltimoAcesso();
            usuarioBuilder.id = api.getId();
            usuarioBuilder.dataCriacao = api.getDataCriacao();
            usuarioBuilder.dataAtualizacao = api.getDataAtualizacao();
            entity = usuarioBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Usuario> entityListToApiList(List<UsuarioEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Usuario> list = new ArrayList<Usuario>( entity.size() );
        for ( UsuarioEntity usuarioEntity : entity ) {
            list.add( entityToApi( usuarioEntity ) );
        }

        return list;
    }

    public List<UsuarioEntity> apiListToEntityList(List<Usuario> api) {
        if ( api == null ) {
            return null;
        }

        List<UsuarioEntity> list = new ArrayList<UsuarioEntity>( api.size() );
        for ( Usuario usuario : api ) {
            list.add( apiToEntity( usuario ) );
        }

        return list;
    }

    public Usuario updateEntityToApi(UsuarioEntity entity, Usuario api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setEmail( entity.getEmail() );
        api.setWhatsapp( entity.getWhatsapp() );
        api.setAtivo( entity.getAtivo() );
        api.setAdministradorPlataforma( entity.getAdministradorPlataforma() );
        api.setDataUltimoAcesso( entity.getDataUltimoAcesso() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(api);

        return api;
    }

    public UsuarioEntity updateApiToEntity(Usuario api, UsuarioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Usuario found for Id: " + api.getId()));
            }
        } else {
            UsuarioEntityBuilder usuarioBuilder = UsuarioEntity.builderOf();

            usuarioBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setEmail( api.getEmail() );
            entity.setWhatsapp( api.getWhatsapp() );
            entity.setAtivo( api.getAtivo() );
            entity.setAdministradorPlataforma( api.getAdministradorPlataforma() );
            entity.setDataUltimoAcesso( api.getDataUltimoAcesso() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public UsuarioEntity patchApiToEntity(Usuario api, UsuarioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getEmail() != null) entity.setEmail( api.getEmail() ); 
        if (api.getWhatsapp() != null) entity.setWhatsapp( api.getWhatsapp() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getAdministradorPlataforma() != null) entity.setAdministradorPlataforma( api.getAdministradorPlataforma() ); 
        if (api.getDataUltimoAcesso() != null) entity.setDataUltimoAcesso( api.getDataUltimoAcesso() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 

        return entity;
    }

    public UsuarioEntity setEntityDefaultValues(UsuarioEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        if (entity.getAdministradorPlataforma() == null) {
            entity.setAdministradorPlataforma(false);
        }

        return entity;
    }

    public Usuario setApiDefaultValues(Usuario api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }
        if (api.getAdministradorPlataforma() == null) {
            api.setAdministradorPlataforma(false);
        }

        return api;
    }

}
