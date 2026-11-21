package br.com.emitesimples.admin.mapper;

import br.com.emitesimples.admin.domain.AssinaturaEmpresa;
import br.com.emitesimples.admin.entity.AssinaturaEmpresaEntity;
import br.com.emitesimples.admin.builder.AssinaturaEmpresaEntityBuilder;
import br.com.emitesimples.admin.repository.AssinaturaEmpresaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.admin.domain.Plano;
import br.com.emitesimples.admin.entity.PlanoEntity;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.mapper.UsuarioMapper;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

@ApplicationScoped
public class AssinaturaEmpresaMapper {

    @Inject AssinaturaEmpresaRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    PlanoMapper planoMapper;
    @Inject 
    UsuarioMapper usuarioMapper;

    public AssinaturaEmpresa entityToApi(AssinaturaEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AssinaturaEmpresa assinaturaEmpresa = new AssinaturaEmpresa();

        assinaturaEmpresa.setVersion( entity.getVersion() );
        assinaturaEmpresa.setEstado( entity.getEstado() );
        assinaturaEmpresa.setDataInicio( entity.getDataInicio() );
        assinaturaEmpresa.setDataFim( entity.getDataFim() );
        assinaturaEmpresa.setReferenciaCrm( entity.getReferenciaCrm() );
        assinaturaEmpresa.setId( entity.getId() );
        assinaturaEmpresa.setDataCriacao( entity.getDataCriacao() );
        assinaturaEmpresa.setDataAtualizacao( entity.getDataAtualizacao() );
        assinaturaEmpresa.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        assinaturaEmpresa.setPlano(planoMapper.entityToApi(entity.getPlano() ));
        assinaturaEmpresa.setVinculadaPor(usuarioMapper.entityToApi(entity.getVinculadaPor() ));

        this.setApiDefaultValues(assinaturaEmpresa);

        return assinaturaEmpresa;
    }

    public AssinaturaEmpresaEntity apiToEntity(AssinaturaEmpresa api) {
        if ( api == null ) {
            return null;
        }
        AssinaturaEmpresaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity AssinaturaEmpresa found for Id: " + api.getId()));
            }
        } else {
            AssinaturaEmpresaEntityBuilder assinaturaEmpresaBuilder = AssinaturaEmpresaEntity.builderOf();

            assinaturaEmpresaBuilder.version = api.getVersion();
            assinaturaEmpresaBuilder.estado = api.getEstado();
            assinaturaEmpresaBuilder.dataInicio = api.getDataInicio();
            assinaturaEmpresaBuilder.dataFim = api.getDataFim();
            assinaturaEmpresaBuilder.referenciaCrm = api.getReferenciaCrm();
            assinaturaEmpresaBuilder.id = api.getId();
            assinaturaEmpresaBuilder.dataCriacao = api.getDataCriacao();
            assinaturaEmpresaBuilder.dataAtualizacao = api.getDataAtualizacao();
            assinaturaEmpresaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            assinaturaEmpresaBuilder.plano = planoMapper.apiToEntity(api.getPlano());
            assinaturaEmpresaBuilder.vinculadaPor = usuarioMapper.apiToEntity(api.getVinculadaPor());
            entity = assinaturaEmpresaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<AssinaturaEmpresa> entityListToApiList(List<AssinaturaEmpresaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<AssinaturaEmpresa> list = new ArrayList<AssinaturaEmpresa>( entity.size() );
        for ( AssinaturaEmpresaEntity assinaturaEmpresaEntity : entity ) {
            list.add( entityToApi( assinaturaEmpresaEntity ) );
        }

        return list;
    }

    public List<AssinaturaEmpresaEntity> apiListToEntityList(List<AssinaturaEmpresa> api) {
        if ( api == null ) {
            return null;
        }

        List<AssinaturaEmpresaEntity> list = new ArrayList<AssinaturaEmpresaEntity>( api.size() );
        for ( AssinaturaEmpresa assinaturaEmpresa : api ) {
            list.add( apiToEntity( assinaturaEmpresa ) );
        }

        return list;
    }

    public AssinaturaEmpresa updateEntityToApi(AssinaturaEmpresaEntity entity, AssinaturaEmpresa api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setEstado( entity.getEstado() );
        api.setDataInicio( entity.getDataInicio() );
        api.setDataFim( entity.getDataFim() );
        api.setReferenciaCrm( entity.getReferenciaCrm() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getEmpresa() != null ) {
            if ( api.getEmpresa() == null ) {
                api.setEmpresa( new Empresa() );
            }
            empresaMapper.updateEntityToApi(entity.getEmpresa(), api.getEmpresa());
        } else {
            api.setEmpresa( null );
        }

        if ( entity.getPlano() != null ) {
            if ( api.getPlano() == null ) {
                api.setPlano( new Plano() );
            }
            planoMapper.updateEntityToApi(entity.getPlano(), api.getPlano());
        } else {
            api.setPlano( null );
        }

        if ( entity.getVinculadaPor() != null ) {
            if ( api.getVinculadaPor() == null ) {
                api.setVinculadaPor( new Usuario() );
            }
            usuarioMapper.updateEntityToApi(entity.getVinculadaPor(), api.getVinculadaPor());
        } else {
            api.setVinculadaPor( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public AssinaturaEmpresaEntity updateApiToEntity(AssinaturaEmpresa api, AssinaturaEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity AssinaturaEmpresa found for Id: " + api.getId()));
            }
        } else {
            AssinaturaEmpresaEntityBuilder assinaturaEmpresaBuilder = AssinaturaEmpresaEntity.builderOf();

            assinaturaEmpresaBuilder.version = api.getVersion();
            entity.setEstado( api.getEstado() );
            entity.setDataInicio( api.getDataInicio() );
            entity.setDataFim( api.getDataFim() );
            entity.setReferenciaCrm( api.getReferenciaCrm() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            if ( api.getPlano() != null ) {
                if ( entity.getPlano() == null ) {
                    entity.setPlano( new PlanoEntity() );
                }
                entity.setPlano(planoMapper.updateApiToEntity( api.getPlano(), entity.getPlano()));
            } else {
                entity.setPlano( null );
            }

            if ( api.getVinculadaPor() != null ) {
                if ( entity.getVinculadaPor() == null ) {
                    entity.setVinculadaPor( new UsuarioEntity() );
                }
                entity.setVinculadaPor(usuarioMapper.updateApiToEntity( api.getVinculadaPor(), entity.getVinculadaPor()));
            } else {
                entity.setVinculadaPor( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public AssinaturaEmpresaEntity patchApiToEntity(AssinaturaEmpresa api, AssinaturaEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstado() != null) entity.setEstado( api.getEstado() ); 
        if (api.getDataInicio() != null) entity.setDataInicio( api.getDataInicio() ); 
        if (api.getDataFim() != null) entity.setDataFim( api.getDataFim() ); 
        if (api.getReferenciaCrm() != null) entity.setReferenciaCrm( api.getReferenciaCrm() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());
        if ( api.getPlano() != null ) {
            if ( entity.getPlano() == null ) {
                entity.setPlano( new PlanoEntity() );
            }
            entity.setPlano(planoMapper.updateApiToEntity( api.getPlano(), entity.getPlano()));
        }
        if ( api.getVinculadaPor() != null ) {
            if ( entity.getVinculadaPor() == null ) {
                entity.setVinculadaPor( new UsuarioEntity() );
            }
            entity.setVinculadaPor(usuarioMapper.updateApiToEntity( api.getVinculadaPor(), entity.getVinculadaPor()));
        }

        return entity;
    }

    public AssinaturaEmpresaEntity setEntityDefaultValues(AssinaturaEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getEstado() == null) {
            entity.setEstado("ativa");
        }

        return entity;
    }

    public AssinaturaEmpresa setApiDefaultValues(AssinaturaEmpresa api) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstado() == null) {
            api.setEstado("ativa");
        }

        return api;
    }

}
