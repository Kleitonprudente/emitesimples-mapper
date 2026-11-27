package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.CredencialCobranca;
import br.com.emitesimples.financeiro.entity.CredencialCobrancaEntity;
import br.com.emitesimples.financeiro.builder.CredencialCobrancaEntityBuilder;
import br.com.emitesimples.financeiro.repository.CredencialCobrancaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.financeiro.domain.ContaFinanceira;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class CredencialCobrancaMapper {

    @Inject CredencialCobrancaRepository repository;
    @Inject 
    ContaFinanceiraMapper contaFinanceiraMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public CredencialCobranca entityToApi(CredencialCobrancaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CredencialCobranca credencialCobranca = new CredencialCobranca();

        credencialCobranca.setVersion( entity.getVersion() );
        credencialCobranca.setProvedor( entity.getProvedor() );
        credencialCobranca.setEndpoint( entity.getEndpoint() );
        credencialCobranca.setSellerId( entity.getSellerId() );
        credencialCobranca.setReferenciaToken( entity.getReferenciaToken() );
        credencialCobranca.setReferenciaSegredoWebhook( entity.getReferenciaSegredoWebhook() );
        credencialCobranca.setAtivo( entity.getAtivo() );
        credencialCobranca.setId( entity.getId() );
        credencialCobranca.setDataCriacao( entity.getDataCriacao() );
        credencialCobranca.setDataAtualizacao( entity.getDataAtualizacao() );
        credencialCobranca.setConta(contaFinanceiraMapper.entityToApi(entity.getConta() ));
        credencialCobranca.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(credencialCobranca);

        return credencialCobranca;
    }

    public CredencialCobrancaEntity apiToEntity(CredencialCobranca api) {
        if ( api == null ) {
            return null;
        }
        CredencialCobrancaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CredencialCobranca found for Id: " + api.getId()));
            }
        } else {
            CredencialCobrancaEntityBuilder credencialCobrancaBuilder = CredencialCobrancaEntity.builderOf();

            credencialCobrancaBuilder.version = api.getVersion();
            credencialCobrancaBuilder.provedor = api.getProvedor();
            credencialCobrancaBuilder.endpoint = api.getEndpoint();
            credencialCobrancaBuilder.sellerId = api.getSellerId();
            credencialCobrancaBuilder.referenciaToken = api.getReferenciaToken();
            credencialCobrancaBuilder.referenciaSegredoWebhook = api.getReferenciaSegredoWebhook();
            credencialCobrancaBuilder.ativo = api.getAtivo();
            credencialCobrancaBuilder.id = api.getId();
            credencialCobrancaBuilder.dataCriacao = api.getDataCriacao();
            credencialCobrancaBuilder.dataAtualizacao = api.getDataAtualizacao();
            credencialCobrancaBuilder.conta = contaFinanceiraMapper.apiToEntity(api.getConta());
            credencialCobrancaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = credencialCobrancaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<CredencialCobranca> entityListToApiList(List<CredencialCobrancaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<CredencialCobranca> list = new ArrayList<CredencialCobranca>( entity.size() );
        for ( CredencialCobrancaEntity credencialCobrancaEntity : entity ) {
            list.add( entityToApi( credencialCobrancaEntity ) );
        }

        return list;
    }

    public List<CredencialCobrancaEntity> apiListToEntityList(List<CredencialCobranca> api) {
        if ( api == null ) {
            return null;
        }

        List<CredencialCobrancaEntity> list = new ArrayList<CredencialCobrancaEntity>( api.size() );
        for ( CredencialCobranca credencialCobranca : api ) {
            list.add( apiToEntity( credencialCobranca ) );
        }

        return list;
    }

    public CredencialCobranca updateEntityToApi(CredencialCobrancaEntity entity, CredencialCobranca api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setProvedor( entity.getProvedor() );
        api.setEndpoint( entity.getEndpoint() );
        api.setSellerId( entity.getSellerId() );
        api.setReferenciaToken( entity.getReferenciaToken() );
        api.setReferenciaSegredoWebhook( entity.getReferenciaSegredoWebhook() );
        api.setAtivo( entity.getAtivo() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getConta() != null ) {
            if ( api.getConta() == null ) {
                api.setConta( new ContaFinanceira() );
            }
            contaFinanceiraMapper.updateEntityToApi(entity.getConta(), api.getConta());
        } else {
            api.setConta( null );
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

    public CredencialCobrancaEntity updateApiToEntity(CredencialCobranca api, CredencialCobrancaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CredencialCobranca found for Id: " + api.getId()));
            }
        } else {
            CredencialCobrancaEntityBuilder credencialCobrancaBuilder = CredencialCobrancaEntity.builderOf();

            credencialCobrancaBuilder.version = api.getVersion();
            entity.setProvedor( api.getProvedor() );
            entity.setEndpoint( api.getEndpoint() );
            entity.setSellerId( api.getSellerId() );
            entity.setReferenciaToken( api.getReferenciaToken() );
            entity.setReferenciaSegredoWebhook( api.getReferenciaSegredoWebhook() );
            entity.setAtivo( api.getAtivo() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getConta() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setConta(contaFinanceiraMapper.updateApiToEntity( api.getConta(), entity.getConta() != null ? entity.getConta() : new ContaFinanceiraEntity()));
            } else {
                entity.setConta( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public CredencialCobrancaEntity patchApiToEntity(CredencialCobranca api, CredencialCobrancaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getProvedor() != null) entity.setProvedor( api.getProvedor() ); 
        if (api.getEndpoint() != null) entity.setEndpoint( api.getEndpoint() ); 
        if (api.getSellerId() != null) entity.setSellerId( api.getSellerId() ); 
        if (api.getReferenciaToken() != null) entity.setReferenciaToken( api.getReferenciaToken() ); 
        if (api.getReferenciaSegredoWebhook() != null) entity.setReferenciaSegredoWebhook( api.getReferenciaSegredoWebhook() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getConta() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setConta(contaFinanceiraMapper.updateApiToEntity( api.getConta(), entity.getConta() != null ? entity.getConta() : new ContaFinanceiraEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public CredencialCobrancaEntity setEntityDefaultValues(CredencialCobrancaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }

        return entity;
    }

    public CredencialCobranca setApiDefaultValues(CredencialCobranca api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }

        return api;
    }

}
