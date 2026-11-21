package br.com.emitesimples.admin.mapper;

import br.com.emitesimples.admin.domain.Plano;
import br.com.emitesimples.admin.entity.PlanoEntity;
import br.com.emitesimples.admin.builder.PlanoEntityBuilder;
import br.com.emitesimples.admin.repository.PlanoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;

@ApplicationScoped
public class PlanoMapper {

    @Inject PlanoRepository repository;

    public Plano entityToApi(PlanoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Plano plano = new Plano();

        plano.setVersion( entity.getVersion() );
        plano.setNome( entity.getNome() );
        plano.setDescricao( entity.getDescricao() );
        plano.setValor( entity.getValor() );
        plano.setPeriodicidade( entity.getPeriodicidade() );
        plano.setAtivo( entity.getAtivo() );
        plano.setReferenciaCrm( entity.getReferenciaCrm() );
        plano.setId( entity.getId() );
        plano.setDataCriacao( entity.getDataCriacao() );
        plano.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(plano);

        return plano;
    }

    public PlanoEntity apiToEntity(Plano api) {
        if ( api == null ) {
            return null;
        }
        PlanoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Plano found for Id: " + api.getId()));
            }
        } else {
            PlanoEntityBuilder planoBuilder = PlanoEntity.builderOf();

            planoBuilder.version = api.getVersion();
            planoBuilder.nome = api.getNome();
            planoBuilder.descricao = api.getDescricao();
            planoBuilder.valor = api.getValor();
            planoBuilder.periodicidade = api.getPeriodicidade();
            planoBuilder.ativo = api.getAtivo();
            planoBuilder.referenciaCrm = api.getReferenciaCrm();
            planoBuilder.id = api.getId();
            planoBuilder.dataCriacao = api.getDataCriacao();
            planoBuilder.dataAtualizacao = api.getDataAtualizacao();
            entity = planoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Plano> entityListToApiList(List<PlanoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Plano> list = new ArrayList<Plano>( entity.size() );
        for ( PlanoEntity planoEntity : entity ) {
            list.add( entityToApi( planoEntity ) );
        }

        return list;
    }

    public List<PlanoEntity> apiListToEntityList(List<Plano> api) {
        if ( api == null ) {
            return null;
        }

        List<PlanoEntity> list = new ArrayList<PlanoEntity>( api.size() );
        for ( Plano plano : api ) {
            list.add( apiToEntity( plano ) );
        }

        return list;
    }

    public Plano updateEntityToApi(PlanoEntity entity, Plano api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setDescricao( entity.getDescricao() );
        api.setValor( entity.getValor() );
        api.setPeriodicidade( entity.getPeriodicidade() );
        api.setAtivo( entity.getAtivo() );
        api.setReferenciaCrm( entity.getReferenciaCrm() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(api);

        return api;
    }

    public PlanoEntity updateApiToEntity(Plano api, PlanoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Plano found for Id: " + api.getId()));
            }
        } else {
            PlanoEntityBuilder planoBuilder = PlanoEntity.builderOf();

            planoBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setDescricao( api.getDescricao() );
            entity.setValor( api.getValor() );
            entity.setPeriodicidade( api.getPeriodicidade() );
            entity.setAtivo( api.getAtivo() );
            entity.setReferenciaCrm( api.getReferenciaCrm() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public PlanoEntity patchApiToEntity(Plano api, PlanoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getDescricao() != null) entity.setDescricao( api.getDescricao() ); 
        if (api.getValor() != null) entity.setValor( api.getValor() ); 
        if (api.getPeriodicidade() != null) entity.setPeriodicidade( api.getPeriodicidade() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getReferenciaCrm() != null) entity.setReferenciaCrm( api.getReferenciaCrm() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 

        return entity;
    }

    public PlanoEntity setEntityDefaultValues(PlanoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getPeriodicidade() == null) {
            entity.setPeriodicidade("mensal");
        }
        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }

        return entity;
    }

    public Plano setApiDefaultValues(Plano api) {
        if ( api == null ) {
            return null;
        }

        if (api.getPeriodicidade() == null) {
            api.setPeriodicidade("mensal");
        }
        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }

        return api;
    }

}
