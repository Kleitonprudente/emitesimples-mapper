package br.com.emitesimples.configuracao.mapper;

import br.com.emitesimples.configuracao.domain.QsaEmpresa;
import br.com.emitesimples.configuracao.entity.QsaEmpresaEntity;
import br.com.emitesimples.configuracao.builder.QsaEmpresaEntityBuilder;
import br.com.emitesimples.configuracao.repository.QsaEmpresaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class QsaEmpresaMapper {

    @Inject QsaEmpresaRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;

    public QsaEmpresa entityToApi(QsaEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        QsaEmpresa qsaEmpresa = new QsaEmpresa();

        qsaEmpresa.setVersion( entity.getVersion() );
        qsaEmpresa.setNome( entity.getNome() );
        qsaEmpresa.setCpf( entity.getCpf() );
        qsaEmpresa.setQualificacao( entity.getQualificacao() );
        qsaEmpresa.setDataEntradaSociedade( entity.getDataEntradaSociedade() );
        qsaEmpresa.setId( entity.getId() );
        qsaEmpresa.setDataCriacao( entity.getDataCriacao() );
        qsaEmpresa.setDataAtualizacao( entity.getDataAtualizacao() );
        if ( entity.getEmpresa() != null ) {
            Empresa empresaRef = new Empresa();
            empresaRef.setId( entity.getEmpresa().getId() );
            qsaEmpresa.setEmpresa( empresaRef );
        }

        this.setApiDefaultValues(qsaEmpresa);

        return qsaEmpresa;
    }

    public QsaEmpresaEntity apiToEntity(QsaEmpresa api) {
        if ( api == null ) {
            return null;
        }
        QsaEmpresaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity QsaEmpresa found for Id: " + api.getId()));
            }
        } else {
            QsaEmpresaEntityBuilder qsaEmpresaBuilder = QsaEmpresaEntity.builderOf();

            qsaEmpresaBuilder.version = api.getVersion();
            qsaEmpresaBuilder.nome = api.getNome();
            qsaEmpresaBuilder.cpf = api.getCpf();
            qsaEmpresaBuilder.qualificacao = api.getQualificacao();
            qsaEmpresaBuilder.dataEntradaSociedade = api.getDataEntradaSociedade();
            qsaEmpresaBuilder.id = api.getId();
            qsaEmpresaBuilder.dataCriacao = api.getDataCriacao();
            qsaEmpresaBuilder.dataAtualizacao = api.getDataAtualizacao();
            qsaEmpresaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = qsaEmpresaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<QsaEmpresa> entityListToApiList(List<QsaEmpresaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<QsaEmpresa> list = new ArrayList<QsaEmpresa>( entity.size() );
        for ( QsaEmpresaEntity qsaEmpresaEntity : entity ) {
            list.add( entityToApi( qsaEmpresaEntity ) );
        }

        return list;
    }

    public List<QsaEmpresaEntity> apiListToEntityList(List<QsaEmpresa> api) {
        if ( api == null ) {
            return null;
        }

        List<QsaEmpresaEntity> list = new ArrayList<QsaEmpresaEntity>( api.size() );
        for ( QsaEmpresa qsaEmpresa : api ) {
            list.add( apiToEntity( qsaEmpresa ) );
        }

        return list;
    }

    public QsaEmpresa updateEntityToApi(QsaEmpresaEntity entity, QsaEmpresa api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setCpf( entity.getCpf() );
        api.setQualificacao( entity.getQualificacao() );
        api.setDataEntradaSociedade( entity.getDataEntradaSociedade() );
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

    public QsaEmpresaEntity updateApiToEntity(QsaEmpresa api, QsaEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity QsaEmpresa found for Id: " + api.getId()));
            }
        } else {
            QsaEmpresaEntityBuilder qsaEmpresaBuilder = QsaEmpresaEntity.builderOf();

            qsaEmpresaBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setCpf( api.getCpf() );
            entity.setQualificacao( api.getQualificacao() );
            entity.setDataEntradaSociedade( api.getDataEntradaSociedade() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public QsaEmpresaEntity patchApiToEntity(QsaEmpresa api, QsaEmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getCpf() != null) entity.setCpf( api.getCpf() ); 
        if (api.getQualificacao() != null) entity.setQualificacao( api.getQualificacao() ); 
        if (api.getDataEntradaSociedade() != null) entity.setDataEntradaSociedade( api.getDataEntradaSociedade() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public QsaEmpresaEntity setEntityDefaultValues(QsaEmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public QsaEmpresa setApiDefaultValues(QsaEmpresa api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
