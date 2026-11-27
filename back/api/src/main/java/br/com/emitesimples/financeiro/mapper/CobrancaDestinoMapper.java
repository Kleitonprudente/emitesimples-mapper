package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.CobrancaDestino;
import br.com.emitesimples.financeiro.entity.CobrancaDestinoEntity;
import br.com.emitesimples.financeiro.builder.CobrancaDestinoEntityBuilder;
import br.com.emitesimples.financeiro.repository.CobrancaDestinoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.financeiro.domain.CredencialCobranca;
import br.com.emitesimples.financeiro.entity.CredencialCobrancaEntity;

@ApplicationScoped
public class CobrancaDestinoMapper {

    @Inject CobrancaDestinoRepository repository;
    @Inject 
    CredencialCobrancaMapper credencialCobrancaMapper;

    public CobrancaDestino entityToApi(CobrancaDestinoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CobrancaDestino cobrancaDestino = new CobrancaDestino();

        cobrancaDestino.setVersion( entity.getVersion() );
        cobrancaDestino.setIdEmpresa( entity.getIdEmpresa() );
        cobrancaDestino.setSegredoHash( entity.getSegredoHash() );
        cobrancaDestino.setId( entity.getId() );
        cobrancaDestino.setDataCriacao( entity.getDataCriacao() );
        cobrancaDestino.setDataAtualizacao( entity.getDataAtualizacao() );
        cobrancaDestino.setCredencial(credencialCobrancaMapper.entityToApi(entity.getCredencial() ));

        this.setApiDefaultValues(cobrancaDestino);

        return cobrancaDestino;
    }

    public CobrancaDestinoEntity apiToEntity(CobrancaDestino api) {
        if ( api == null ) {
            return null;
        }
        CobrancaDestinoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CobrancaDestino found for Id: " + api.getId()));
            }
        } else {
            CobrancaDestinoEntityBuilder cobrancaDestinoBuilder = CobrancaDestinoEntity.builderOf();

            cobrancaDestinoBuilder.version = api.getVersion();
            cobrancaDestinoBuilder.idEmpresa = api.getIdEmpresa();
            cobrancaDestinoBuilder.segredoHash = api.getSegredoHash();
            cobrancaDestinoBuilder.id = api.getId();
            cobrancaDestinoBuilder.dataCriacao = api.getDataCriacao();
            cobrancaDestinoBuilder.dataAtualizacao = api.getDataAtualizacao();
            cobrancaDestinoBuilder.credencial = credencialCobrancaMapper.apiToEntity(api.getCredencial());
            entity = cobrancaDestinoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<CobrancaDestino> entityListToApiList(List<CobrancaDestinoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<CobrancaDestino> list = new ArrayList<CobrancaDestino>( entity.size() );
        for ( CobrancaDestinoEntity cobrancaDestinoEntity : entity ) {
            list.add( entityToApi( cobrancaDestinoEntity ) );
        }

        return list;
    }

    public List<CobrancaDestinoEntity> apiListToEntityList(List<CobrancaDestino> api) {
        if ( api == null ) {
            return null;
        }

        List<CobrancaDestinoEntity> list = new ArrayList<CobrancaDestinoEntity>( api.size() );
        for ( CobrancaDestino cobrancaDestino : api ) {
            list.add( apiToEntity( cobrancaDestino ) );
        }

        return list;
    }

    public CobrancaDestino updateEntityToApi(CobrancaDestinoEntity entity, CobrancaDestino api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setIdEmpresa( entity.getIdEmpresa() );
        api.setSegredoHash( entity.getSegredoHash() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getCredencial() != null ) {
            if ( api.getCredencial() == null ) {
                api.setCredencial( new CredencialCobranca() );
            }
            credencialCobrancaMapper.updateEntityToApi(entity.getCredencial(), api.getCredencial());
        } else {
            api.setCredencial( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public CobrancaDestinoEntity updateApiToEntity(CobrancaDestino api, CobrancaDestinoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CobrancaDestino found for Id: " + api.getId()));
            }
        } else {
            CobrancaDestinoEntityBuilder cobrancaDestinoBuilder = CobrancaDestinoEntity.builderOf();

            cobrancaDestinoBuilder.version = api.getVersion();
            entity.setIdEmpresa( api.getIdEmpresa() );
            entity.setSegredoHash( api.getSegredoHash() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getCredencial() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setCredencial(credencialCobrancaMapper.updateApiToEntity( api.getCredencial(), entity.getCredencial() != null ? entity.getCredencial() : new CredencialCobrancaEntity()));
            } else {
                entity.setCredencial( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public CobrancaDestinoEntity patchApiToEntity(CobrancaDestino api, CobrancaDestinoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getIdEmpresa() != null) entity.setIdEmpresa( api.getIdEmpresa() ); 
        if (api.getSegredoHash() != null) entity.setSegredoHash( api.getSegredoHash() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getCredencial() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setCredencial(credencialCobrancaMapper.updateApiToEntity( api.getCredencial(), entity.getCredencial() != null ? entity.getCredencial() : new CredencialCobrancaEntity()));
        }

        return entity;
    }

    public CobrancaDestinoEntity setEntityDefaultValues(CobrancaDestinoEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public CobrancaDestino setApiDefaultValues(CobrancaDestino api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
