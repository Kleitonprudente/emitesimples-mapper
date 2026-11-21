package br.com.emitesimples.admin.mapper;

import br.com.emitesimples.admin.domain.HistoricoSituacaoEmpresa;
import br.com.emitesimples.admin.entity.HistoricoSituacaoEmpresaEntity;
import br.com.emitesimples.admin.builder.HistoricoSituacaoEmpresaEntityBuilder;
import br.com.emitesimples.admin.repository.HistoricoSituacaoEmpresaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.mapper.UsuarioMapper;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;

@ApplicationScoped
public class HistoricoSituacaoEmpresaMapper {

    @Inject HistoricoSituacaoEmpresaRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    UsuarioMapper usuarioMapper;

    public HistoricoSituacaoEmpresa entityToApi(HistoricoSituacaoEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        HistoricoSituacaoEmpresa historicoSituacaoEmpresa = new HistoricoSituacaoEmpresa();

        historicoSituacaoEmpresa.setVersion( entity.getVersion() );
        historicoSituacaoEmpresa.setSituacaoAnterior( entity.getSituacaoAnterior() );
        historicoSituacaoEmpresa.setSituacaoNova( entity.getSituacaoNova() );
        historicoSituacaoEmpresa.setMotivo( entity.getMotivo() );
        historicoSituacaoEmpresa.setOrigem( entity.getOrigem() );
        historicoSituacaoEmpresa.setDataHora( entity.getDataHora() );
        historicoSituacaoEmpresa.setId( entity.getId() );
        historicoSituacaoEmpresa.setDataCriacao( entity.getDataCriacao() );
        historicoSituacaoEmpresa.setDataAtualizacao( entity.getDataAtualizacao() );
        historicoSituacaoEmpresa.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        historicoSituacaoEmpresa.setResponsavel(usuarioMapper.entityToApi(entity.getResponsavel() ));

        this.setApiDefaultValues(historicoSituacaoEmpresa);

        return historicoSituacaoEmpresa;
    }

    public HistoricoSituacaoEmpresaEntity apiToEntity(HistoricoSituacaoEmpresa api) {
        if ( api == null ) {
            return null;
        }
        HistoricoSituacaoEmpresaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity HistoricoSituacaoEmpresa found for Id: " + api.getId()));
            }
        } else {
            HistoricoSituacaoEmpresaEntityBuilder historicoSituacaoEmpresaBuilder = HistoricoSituacaoEmpresaEntity.builderOf();

            historicoSituacaoEmpresaBuilder.version = api.getVersion();
            historicoSituacaoEmpresaBuilder.situacaoAnterior = api.getSituacaoAnterior();
            historicoSituacaoEmpresaBuilder.situacaoNova = api.getSituacaoNova();
            historicoSituacaoEmpresaBuilder.motivo = api.getMotivo();
            historicoSituacaoEmpresaBuilder.origem = api.getOrigem();
            historicoSituacaoEmpresaBuilder.dataHora = api.getDataHora();
            historicoSituacaoEmpresaBuilder.id = api.getId();
            historicoSituacaoEmpresaBuilder.dataCriacao = api.getDataCriacao();
            historicoSituacaoEmpresaBuilder.dataAtualizacao = api.getDataAtualizacao();
            historicoSituacaoEmpresaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            historicoSituacaoEmpresaBuilder.responsavel = usuarioMapper.apiToEntity(api.getResponsavel());
            entity = historicoSituacaoEmpresaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<HistoricoSituacaoEmpresa> entityListToApiList(List<HistoricoSituacaoEmpresaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<HistoricoSituacaoEmpresa> list = new ArrayList<HistoricoSituacaoEmpresa>( entity.size() );
        for ( HistoricoSituacaoEmpresaEntity historicoSituacaoEmpresaEntity : entity ) {
            list.add( entityToApi( historicoSituacaoEmpresaEntity ) );
        }

        return list;
    }

    public List<HistoricoSituacaoEmpresaEntity> apiListToEntityList(List<HistoricoSituacaoEmpresa> api) {
        if ( api == null ) {
            return null;
        }

        List<HistoricoSituacaoEmpresaEntity> list = new ArrayList<HistoricoSituacaoEmpresaEntity>( api.size() );
        for ( HistoricoSituacaoEmpresa historicoSituacaoEmpresa : api ) {
            list.add( apiToEntity( historicoSituacaoEmpresa ) );
        }

        return list;
    }

    public HistoricoSituacaoEmpresa updateEntityToApi(HistoricoSituacaoEmpresaEntity entity, HistoricoSituacaoEmpresa api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setSituacaoAnterior( entity.getSituacaoAnterior() );
        api.setSituacaoNova( entity.getSituacaoNova() );
        api.setMotivo( entity.getMotivo() );
        api.setOrigem( entity.getOrigem() );
        api.setDataHora( entity.getDataHora() );
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

        if ( entity.getResponsavel() != null ) {
            if ( api.getResponsavel() == null ) {
                api.setResponsavel( new Usuario() );
            }
            usuarioMapper.updateEntityToApi(entity.getResponsavel(), api.getResponsavel());
        } else {
            api.setResponsavel( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public HistoricoSituacaoEmpresaEntity updateApiToEntity(HistoricoSituacaoEmpresa api, HistoricoSituacaoEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity HistoricoSituacaoEmpresa found for Id: " + api.getId()));
            }
        } else {
            HistoricoSituacaoEmpresaEntityBuilder historicoSituacaoEmpresaBuilder = HistoricoSituacaoEmpresaEntity.builderOf();

            historicoSituacaoEmpresaBuilder.version = api.getVersion();
            entity.setSituacaoAnterior( api.getSituacaoAnterior() );
            entity.setSituacaoNova( api.getSituacaoNova() );
            entity.setMotivo( api.getMotivo() );
            entity.setOrigem( api.getOrigem() );
            entity.setDataHora( api.getDataHora() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            if ( api.getResponsavel() != null ) {
                if ( entity.getResponsavel() == null ) {
                    entity.setResponsavel( new UsuarioEntity() );
                }
                entity.setResponsavel(usuarioMapper.updateApiToEntity( api.getResponsavel(), entity.getResponsavel()));
            } else {
                entity.setResponsavel( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public HistoricoSituacaoEmpresaEntity patchApiToEntity(HistoricoSituacaoEmpresa api, HistoricoSituacaoEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getSituacaoAnterior() != null) entity.setSituacaoAnterior( api.getSituacaoAnterior() ); 
        if (api.getSituacaoNova() != null) entity.setSituacaoNova( api.getSituacaoNova() ); 
        if (api.getMotivo() != null) entity.setMotivo( api.getMotivo() ); 
        if (api.getOrigem() != null) entity.setOrigem( api.getOrigem() ); 
        if (api.getDataHora() != null) entity.setDataHora( api.getDataHora() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());
        if ( api.getResponsavel() != null ) {
            if ( entity.getResponsavel() == null ) {
                entity.setResponsavel( new UsuarioEntity() );
            }
            entity.setResponsavel(usuarioMapper.updateApiToEntity( api.getResponsavel(), entity.getResponsavel()));
        }

        return entity;
    }

    public HistoricoSituacaoEmpresaEntity setEntityDefaultValues(HistoricoSituacaoEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public HistoricoSituacaoEmpresa setApiDefaultValues(HistoricoSituacaoEmpresa api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
