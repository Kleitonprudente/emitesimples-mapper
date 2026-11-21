package br.com.emitesimples.configuracao.mapper;

import br.com.emitesimples.configuracao.domain.AtividadeEmpresa;
import br.com.emitesimples.configuracao.entity.AtividadeEmpresaEntity;
import br.com.emitesimples.configuracao.builder.AtividadeEmpresaEntityBuilder;
import br.com.emitesimples.configuracao.repository.AtividadeEmpresaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class AtividadeEmpresaMapper {

    @Inject AtividadeEmpresaRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;

    public AtividadeEmpresa entityToApi(AtividadeEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AtividadeEmpresa atividadeEmpresa = new AtividadeEmpresa();

        atividadeEmpresa.setVersion( entity.getVersion() );
        atividadeEmpresa.setTipo( entity.getTipo() );
        atividadeEmpresa.setCodigoCnae( entity.getCodigoCnae() );
        atividadeEmpresa.setDescricao( entity.getDescricao() );
        atividadeEmpresa.setId( entity.getId() );
        atividadeEmpresa.setDataCriacao( entity.getDataCriacao() );
        atividadeEmpresa.setDataAtualizacao( entity.getDataAtualizacao() );
        if ( entity.getEmpresa() != null ) {
            Empresa empresaRef = new Empresa();
            empresaRef.setId( entity.getEmpresa().getId() );
            atividadeEmpresa.setEmpresa( empresaRef );
        }

        this.setApiDefaultValues(atividadeEmpresa);

        return atividadeEmpresa;
    }

    public AtividadeEmpresaEntity apiToEntity(AtividadeEmpresa api) {
        if ( api == null ) {
            return null;
        }
        AtividadeEmpresaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity AtividadeEmpresa found for Id: " + api.getId()));
            }
        } else {
            AtividadeEmpresaEntityBuilder atividadeEmpresaBuilder = AtividadeEmpresaEntity.builderOf();

            atividadeEmpresaBuilder.version = api.getVersion();
            atividadeEmpresaBuilder.tipo = api.getTipo();
            atividadeEmpresaBuilder.codigoCnae = api.getCodigoCnae();
            atividadeEmpresaBuilder.descricao = api.getDescricao();
            atividadeEmpresaBuilder.id = api.getId();
            atividadeEmpresaBuilder.dataCriacao = api.getDataCriacao();
            atividadeEmpresaBuilder.dataAtualizacao = api.getDataAtualizacao();
            atividadeEmpresaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = atividadeEmpresaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<AtividadeEmpresa> entityListToApiList(List<AtividadeEmpresaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<AtividadeEmpresa> list = new ArrayList<AtividadeEmpresa>( entity.size() );
        for ( AtividadeEmpresaEntity atividadeEmpresaEntity : entity ) {
            list.add( entityToApi( atividadeEmpresaEntity ) );
        }

        return list;
    }

    public List<AtividadeEmpresaEntity> apiListToEntityList(List<AtividadeEmpresa> api) {
        if ( api == null ) {
            return null;
        }

        List<AtividadeEmpresaEntity> list = new ArrayList<AtividadeEmpresaEntity>( api.size() );
        for ( AtividadeEmpresa atividadeEmpresa : api ) {
            list.add( apiToEntity( atividadeEmpresa ) );
        }

        return list;
    }

    public AtividadeEmpresa updateEntityToApi(AtividadeEmpresaEntity entity, AtividadeEmpresa api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setTipo( entity.getTipo() );
        api.setCodigoCnae( entity.getCodigoCnae() );
        api.setDescricao( entity.getDescricao() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );
        if ( entity.getEmpresa() != null ) {
            Empresa empresaRef = new Empresa();
            empresaRef.setId( entity.getEmpresa().getId() );
            api.setEmpresa( empresaRef );
        } else {
            api.setEmpresa( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public AtividadeEmpresaEntity updateApiToEntity(AtividadeEmpresa api, AtividadeEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity AtividadeEmpresa found for Id: " + api.getId()));
            }
        } else {
            AtividadeEmpresaEntityBuilder atividadeEmpresaBuilder = AtividadeEmpresaEntity.builderOf();

            atividadeEmpresaBuilder.version = api.getVersion();
            entity.setTipo( api.getTipo() );
            entity.setCodigoCnae( api.getCodigoCnae() );
            entity.setDescricao( api.getDescricao() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public AtividadeEmpresaEntity patchApiToEntity(AtividadeEmpresa api, AtividadeEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getCodigoCnae() != null) entity.setCodigoCnae( api.getCodigoCnae() ); 
        if (api.getDescricao() != null) entity.setDescricao( api.getDescricao() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public AtividadeEmpresaEntity setEntityDefaultValues(AtividadeEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public AtividadeEmpresa setApiDefaultValues(AtividadeEmpresa api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
