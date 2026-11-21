package br.com.emitesimples.cadastros.mapper;

import br.com.emitesimples.cadastros.domain.Municipio;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.builder.MunicipioEntityBuilder;
import br.com.emitesimples.cadastros.repository.MunicipioRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;

@ApplicationScoped
public class MunicipioMapper {

    @Inject MunicipioRepository repository;

    public Municipio entityToApi(MunicipioEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Municipio municipio = new Municipio();

        municipio.setVersion( entity.getVersion() );
        municipio.setCodigoIbge( entity.getCodigoIbge() );
        municipio.setNome( entity.getNome() );
        municipio.setUf( entity.getUf() );
        municipio.setId( entity.getId() );
        municipio.setDataCriacao( entity.getDataCriacao() );
        municipio.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(municipio);

        return municipio;
    }

    public MunicipioEntity apiToEntity(Municipio api) {
        if ( api == null ) {
            return null;
        }
        MunicipioEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Municipio found for Id: " + api.getId()));
            }
        } else {
            MunicipioEntityBuilder municipioBuilder = MunicipioEntity.builderOf();

            municipioBuilder.version = api.getVersion();
            municipioBuilder.codigoIbge = api.getCodigoIbge();
            municipioBuilder.nome = api.getNome();
            municipioBuilder.uf = api.getUf();
            municipioBuilder.id = api.getId();
            municipioBuilder.dataCriacao = api.getDataCriacao();
            municipioBuilder.dataAtualizacao = api.getDataAtualizacao();
            entity = municipioBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Municipio> entityListToApiList(List<MunicipioEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Municipio> list = new ArrayList<Municipio>( entity.size() );
        for ( MunicipioEntity municipioEntity : entity ) {
            list.add( entityToApi( municipioEntity ) );
        }

        return list;
    }

    public List<MunicipioEntity> apiListToEntityList(List<Municipio> api) {
        if ( api == null ) {
            return null;
        }

        List<MunicipioEntity> list = new ArrayList<MunicipioEntity>( api.size() );
        for ( Municipio municipio : api ) {
            list.add( apiToEntity( municipio ) );
        }

        return list;
    }

    public Municipio updateEntityToApi(MunicipioEntity entity, Municipio api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setCodigoIbge( entity.getCodigoIbge() );
        api.setNome( entity.getNome() );
        api.setUf( entity.getUf() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(api);

        return api;
    }

    public MunicipioEntity updateApiToEntity(Municipio api, MunicipioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Municipio found for Id: " + api.getId()));
            }
        } else {
            MunicipioEntityBuilder municipioBuilder = MunicipioEntity.builderOf();

            municipioBuilder.version = api.getVersion();
            entity.setCodigoIbge( api.getCodigoIbge() );
            entity.setNome( api.getNome() );
            entity.setUf( api.getUf() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public MunicipioEntity patchApiToEntity(Municipio api, MunicipioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getCodigoIbge() != null) entity.setCodigoIbge( api.getCodigoIbge() ); 
        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getUf() != null) entity.setUf( api.getUf() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 

        return entity;
    }

    public MunicipioEntity setEntityDefaultValues(MunicipioEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public Municipio setApiDefaultValues(Municipio api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
