package br.com.emitesimples.admin.mapper;

import br.com.emitesimples.admin.domain.FaturaCrm;
import br.com.emitesimples.admin.entity.FaturaCrmEntity;
import br.com.emitesimples.admin.builder.FaturaCrmEntityBuilder;
import br.com.emitesimples.admin.repository.FaturaCrmRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.admin.domain.AssinaturaEmpresa;
import br.com.emitesimples.admin.entity.AssinaturaEmpresaEntity;

@ApplicationScoped
public class FaturaCrmMapper {

    @Inject FaturaCrmRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    AssinaturaEmpresaMapper assinaturaEmpresaMapper;

    public FaturaCrm entityToApi(FaturaCrmEntity entity) {
        if ( entity == null ) {
            return null;
        }

        FaturaCrm faturaCrm = new FaturaCrm();

        faturaCrm.setVersion( entity.getVersion() );
        faturaCrm.setCrmFaturaId( entity.getCrmFaturaId() );
        faturaCrm.setNumero( entity.getNumero() );
        faturaCrm.setValor( entity.getValor() );
        faturaCrm.setDataEmissao( entity.getDataEmissao() );
        faturaCrm.setDataVencimento( entity.getDataVencimento() );
        faturaCrm.setDataPagamento( entity.getDataPagamento() );
        faturaCrm.setEstado( entity.getEstado() );
        faturaCrm.setLinkFatura( entity.getLinkFatura() );
        faturaCrm.setDataSincronizacao( entity.getDataSincronizacao() );
        faturaCrm.setId( entity.getId() );
        faturaCrm.setDataCriacao( entity.getDataCriacao() );
        faturaCrm.setDataAtualizacao( entity.getDataAtualizacao() );
        faturaCrm.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        faturaCrm.setAssinatura(assinaturaEmpresaMapper.entityToApi(entity.getAssinatura() ));

        this.setApiDefaultValues(faturaCrm);

        return faturaCrm;
    }

    public FaturaCrmEntity apiToEntity(FaturaCrm api) {
        if ( api == null ) {
            return null;
        }
        FaturaCrmEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity FaturaCrm found for Id: " + api.getId()));
            }
        } else {
            FaturaCrmEntityBuilder faturaCrmBuilder = FaturaCrmEntity.builderOf();

            faturaCrmBuilder.version = api.getVersion();
            faturaCrmBuilder.crmFaturaId = api.getCrmFaturaId();
            faturaCrmBuilder.numero = api.getNumero();
            faturaCrmBuilder.valor = api.getValor();
            faturaCrmBuilder.dataEmissao = api.getDataEmissao();
            faturaCrmBuilder.dataVencimento = api.getDataVencimento();
            faturaCrmBuilder.dataPagamento = api.getDataPagamento();
            faturaCrmBuilder.estado = api.getEstado();
            faturaCrmBuilder.linkFatura = api.getLinkFatura();
            faturaCrmBuilder.dataSincronizacao = api.getDataSincronizacao();
            faturaCrmBuilder.id = api.getId();
            faturaCrmBuilder.dataCriacao = api.getDataCriacao();
            faturaCrmBuilder.dataAtualizacao = api.getDataAtualizacao();
            faturaCrmBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            faturaCrmBuilder.assinatura = assinaturaEmpresaMapper.apiToEntity(api.getAssinatura());
            entity = faturaCrmBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<FaturaCrm> entityListToApiList(List<FaturaCrmEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<FaturaCrm> list = new ArrayList<FaturaCrm>( entity.size() );
        for ( FaturaCrmEntity faturaCrmEntity : entity ) {
            list.add( entityToApi( faturaCrmEntity ) );
        }

        return list;
    }

    public List<FaturaCrmEntity> apiListToEntityList(List<FaturaCrm> api) {
        if ( api == null ) {
            return null;
        }

        List<FaturaCrmEntity> list = new ArrayList<FaturaCrmEntity>( api.size() );
        for ( FaturaCrm faturaCrm : api ) {
            list.add( apiToEntity( faturaCrm ) );
        }

        return list;
    }

    public FaturaCrm updateEntityToApi(FaturaCrmEntity entity, FaturaCrm api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setCrmFaturaId( entity.getCrmFaturaId() );
        api.setNumero( entity.getNumero() );
        api.setValor( entity.getValor() );
        api.setDataEmissao( entity.getDataEmissao() );
        api.setDataVencimento( entity.getDataVencimento() );
        api.setDataPagamento( entity.getDataPagamento() );
        api.setEstado( entity.getEstado() );
        api.setLinkFatura( entity.getLinkFatura() );
        api.setDataSincronizacao( entity.getDataSincronizacao() );
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

        if ( entity.getAssinatura() != null ) {
            if ( api.getAssinatura() == null ) {
                api.setAssinatura( new AssinaturaEmpresa() );
            }
            assinaturaEmpresaMapper.updateEntityToApi(entity.getAssinatura(), api.getAssinatura());
        } else {
            api.setAssinatura( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public FaturaCrmEntity updateApiToEntity(FaturaCrm api, FaturaCrmEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity FaturaCrm found for Id: " + api.getId()));
            }
        } else {
            FaturaCrmEntityBuilder faturaCrmBuilder = FaturaCrmEntity.builderOf();

            faturaCrmBuilder.version = api.getVersion();
            entity.setCrmFaturaId( api.getCrmFaturaId() );
            entity.setNumero( api.getNumero() );
            entity.setValor( api.getValor() );
            entity.setDataEmissao( api.getDataEmissao() );
            entity.setDataVencimento( api.getDataVencimento() );
            entity.setDataPagamento( api.getDataPagamento() );
            entity.setEstado( api.getEstado() );
            entity.setLinkFatura( api.getLinkFatura() );
            entity.setDataSincronizacao( api.getDataSincronizacao() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            if ( api.getAssinatura() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setAssinatura(assinaturaEmpresaMapper.updateApiToEntity( api.getAssinatura(), entity.getAssinatura() != null ? entity.getAssinatura() : new AssinaturaEmpresaEntity()));
            } else {
                entity.setAssinatura( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public FaturaCrmEntity patchApiToEntity(FaturaCrm api, FaturaCrmEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getCrmFaturaId() != null) entity.setCrmFaturaId( api.getCrmFaturaId() ); 
        if (api.getNumero() != null) entity.setNumero( api.getNumero() ); 
        if (api.getValor() != null) entity.setValor( api.getValor() ); 
        if (api.getDataEmissao() != null) entity.setDataEmissao( api.getDataEmissao() ); 
        if (api.getDataVencimento() != null) entity.setDataVencimento( api.getDataVencimento() ); 
        if (api.getDataPagamento() != null) entity.setDataPagamento( api.getDataPagamento() ); 
        if (api.getEstado() != null) entity.setEstado( api.getEstado() ); 
        if (api.getLinkFatura() != null) entity.setLinkFatura( api.getLinkFatura() ); 
        if (api.getDataSincronizacao() != null) entity.setDataSincronizacao( api.getDataSincronizacao() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());
        if ( api.getAssinatura() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setAssinatura(assinaturaEmpresaMapper.updateApiToEntity( api.getAssinatura(), entity.getAssinatura() != null ? entity.getAssinatura() : new AssinaturaEmpresaEntity()));
        }

        return entity;
    }

    public FaturaCrmEntity setEntityDefaultValues(FaturaCrmEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getEstado() == null) {
            entity.setEstado("ABERTA");
        }

        return entity;
    }

    public FaturaCrm setApiDefaultValues(FaturaCrm api) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstado() == null) {
            api.setEstado("ABERTA");
        }

        return api;
    }

}
