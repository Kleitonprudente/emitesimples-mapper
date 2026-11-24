package br.com.emitesimples.usuarios.mapper;

import br.com.emitesimples.usuarios.domain.VinculoUsuario;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import br.com.emitesimples.usuarios.builder.VinculoUsuarioEntityBuilder;
import br.com.emitesimples.usuarios.repository.VinculoUsuarioRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class VinculoUsuarioMapper {

    @Inject VinculoUsuarioRepository repository;
    @Inject 
    UsuarioMapper usuarioMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public VinculoUsuario entityToApi(VinculoUsuarioEntity entity) {
        if ( entity == null ) {
            return null;
        }

        VinculoUsuario vinculoUsuario = new VinculoUsuario();

        vinculoUsuario.setVersion( entity.getVersion() );
        vinculoUsuario.setPapel( entity.getPapel() );
        vinculoUsuario.setPodeCancelarNfse( entity.getPodeCancelarNfse() );
        vinculoUsuario.setAtivo( entity.getAtivo() );
        vinculoUsuario.setId( entity.getId() );
        vinculoUsuario.setDataCriacao( entity.getDataCriacao() );
        vinculoUsuario.setDataAtualizacao( entity.getDataAtualizacao() );
        vinculoUsuario.setUsuario(usuarioMapper.entityToApi(entity.getUsuario() ));
        vinculoUsuario.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(vinculoUsuario);

        return vinculoUsuario;
    }

    public VinculoUsuarioEntity apiToEntity(VinculoUsuario api) {
        if ( api == null ) {
            return null;
        }
        VinculoUsuarioEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity VinculoUsuario found for Id: " + api.getId()));
            }
        } else {
            VinculoUsuarioEntityBuilder vinculoUsuarioBuilder = VinculoUsuarioEntity.builderOf();

            vinculoUsuarioBuilder.version = api.getVersion();
            vinculoUsuarioBuilder.papel = api.getPapel();
            vinculoUsuarioBuilder.podeCancelarNfse = api.getPodeCancelarNfse();
            vinculoUsuarioBuilder.ativo = api.getAtivo();
            vinculoUsuarioBuilder.id = api.getId();
            vinculoUsuarioBuilder.dataCriacao = api.getDataCriacao();
            vinculoUsuarioBuilder.dataAtualizacao = api.getDataAtualizacao();
            vinculoUsuarioBuilder.usuario = usuarioMapper.apiToEntity(api.getUsuario());
            vinculoUsuarioBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = vinculoUsuarioBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<VinculoUsuario> entityListToApiList(List<VinculoUsuarioEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<VinculoUsuario> list = new ArrayList<VinculoUsuario>( entity.size() );
        for ( VinculoUsuarioEntity vinculoUsuarioEntity : entity ) {
            list.add( entityToApi( vinculoUsuarioEntity ) );
        }

        return list;
    }

    public List<VinculoUsuarioEntity> apiListToEntityList(List<VinculoUsuario> api) {
        if ( api == null ) {
            return null;
        }

        List<VinculoUsuarioEntity> list = new ArrayList<VinculoUsuarioEntity>( api.size() );
        for ( VinculoUsuario vinculoUsuario : api ) {
            list.add( apiToEntity( vinculoUsuario ) );
        }

        return list;
    }

    public VinculoUsuario updateEntityToApi(VinculoUsuarioEntity entity, VinculoUsuario api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setPapel( entity.getPapel() );
        api.setPodeCancelarNfse( entity.getPodeCancelarNfse() );
        api.setAtivo( entity.getAtivo() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getUsuario() != null ) {
            if ( api.getUsuario() == null ) {
                api.setUsuario( new Usuario() );
            }
            usuarioMapper.updateEntityToApi(entity.getUsuario(), api.getUsuario());
        } else {
            api.setUsuario( null );
        }

        if ( entity.getEmpresa() != null ) {
            if ( api.getEmpresa() == null ) {
                api.setEmpresa( new Empresa() );
            }
            empresaMapper.updateEntityToApi(entity.getEmpresa(), api.getEmpresa());
        } else {
            api.setEmpresa( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public VinculoUsuarioEntity updateApiToEntity(VinculoUsuario api, VinculoUsuarioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity VinculoUsuario found for Id: " + api.getId()));
            }
        } else {
            VinculoUsuarioEntityBuilder vinculoUsuarioBuilder = VinculoUsuarioEntity.builderOf();

            vinculoUsuarioBuilder.version = api.getVersion();
            entity.setPapel( api.getPapel() );
            entity.setPodeCancelarNfse( api.getPodeCancelarNfse() );
            entity.setAtivo( api.getAtivo() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getUsuario() != null ) {
                if ( entity.getUsuario() == null ) {
                    entity.setUsuario( new UsuarioEntity() );
                }
                entity.setUsuario(usuarioMapper.updateApiToEntity( api.getUsuario(), entity.getUsuario()));
            } else {
                entity.setUsuario( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public VinculoUsuarioEntity patchApiToEntity(VinculoUsuario api, VinculoUsuarioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getPapel() != null) entity.setPapel( api.getPapel() ); 
        if (api.getPodeCancelarNfse() != null) entity.setPodeCancelarNfse( api.getPodeCancelarNfse() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getUsuario() != null ) {
            if ( entity.getUsuario() == null ) {
                entity.setUsuario( new UsuarioEntity() );
            }
            entity.setUsuario(usuarioMapper.updateApiToEntity( api.getUsuario(), entity.getUsuario()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public VinculoUsuarioEntity setEntityDefaultValues(VinculoUsuarioEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getPapel() == null) {
            entity.setPapel("CONSULTA");
        }
        if (entity.getPodeCancelarNfse() == null) {
            entity.setPodeCancelarNfse(false);
        }
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }

        return entity;
    }

    public VinculoUsuario setApiDefaultValues(VinculoUsuario api) {
        if ( api == null ) {
            return null;
        }

        if (api.getPapel() == null) {
            api.setPapel("CONSULTA");
        }
        if (api.getPodeCancelarNfse() == null) {
            api.setPodeCancelarNfse(false);
        }
        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }

        return api;
    }

}
