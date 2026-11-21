package br.com.emitesimples.usuarios.mapper;

import br.com.emitesimples.usuarios.domain.ClienteAcesso;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import br.com.emitesimples.usuarios.builder.ClienteAcessoEntityBuilder;
import br.com.emitesimples.usuarios.repository.ClienteAcessoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class ClienteAcessoMapper {

    @Inject ClienteAcessoRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;

    public ClienteAcesso entityToApi(ClienteAcessoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ClienteAcesso clienteAcesso = new ClienteAcesso();

        clienteAcesso.setVersion( entity.getVersion() );
        clienteAcesso.setNome( entity.getNome() );
        clienteAcesso.setClientId( entity.getClientId() );
        clienteAcesso.setScopes( entity.getScopes() );
        clienteAcesso.setAtivo( entity.getAtivo() );
        clienteAcesso.setDataUltimoUso( entity.getDataUltimoUso() );
        clienteAcesso.setDataExpiracao( entity.getDataExpiracao() );
        clienteAcesso.setId( entity.getId() );
        clienteAcesso.setDataCriacao( entity.getDataCriacao() );
        clienteAcesso.setDataAtualizacao( entity.getDataAtualizacao() );
        clienteAcesso.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(clienteAcesso);

        return clienteAcesso;
    }

    public ClienteAcessoEntity apiToEntity(ClienteAcesso api) {
        if ( api == null ) {
            return null;
        }
        ClienteAcessoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ClienteAcesso found for Id: " + api.getId()));
            }
        } else {
            ClienteAcessoEntityBuilder clienteAcessoBuilder = ClienteAcessoEntity.builderOf();

            clienteAcessoBuilder.version = api.getVersion();
            clienteAcessoBuilder.nome = api.getNome();
            clienteAcessoBuilder.clientId = api.getClientId();
            clienteAcessoBuilder.scopes = api.getScopes();
            clienteAcessoBuilder.ativo = api.getAtivo();
            clienteAcessoBuilder.dataUltimoUso = api.getDataUltimoUso();
            clienteAcessoBuilder.dataExpiracao = api.getDataExpiracao();
            clienteAcessoBuilder.id = api.getId();
            clienteAcessoBuilder.dataCriacao = api.getDataCriacao();
            clienteAcessoBuilder.dataAtualizacao = api.getDataAtualizacao();
            clienteAcessoBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = clienteAcessoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<ClienteAcesso> entityListToApiList(List<ClienteAcessoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ClienteAcesso> list = new ArrayList<ClienteAcesso>( entity.size() );
        for ( ClienteAcessoEntity clienteAcessoEntity : entity ) {
            list.add( entityToApi( clienteAcessoEntity ) );
        }

        return list;
    }

    public List<ClienteAcessoEntity> apiListToEntityList(List<ClienteAcesso> api) {
        if ( api == null ) {
            return null;
        }

        List<ClienteAcessoEntity> list = new ArrayList<ClienteAcessoEntity>( api.size() );
        for ( ClienteAcesso clienteAcesso : api ) {
            list.add( apiToEntity( clienteAcesso ) );
        }

        return list;
    }

    public ClienteAcesso updateEntityToApi(ClienteAcessoEntity entity, ClienteAcesso api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setClientId( entity.getClientId() );
        api.setScopes( entity.getScopes() );
        api.setAtivo( entity.getAtivo() );
        api.setDataUltimoUso( entity.getDataUltimoUso() );
        api.setDataExpiracao( entity.getDataExpiracao() );
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

        this.setApiDefaultValues(api);

        return api;
    }

    public ClienteAcessoEntity updateApiToEntity(ClienteAcesso api, ClienteAcessoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ClienteAcesso found for Id: " + api.getId()));
            }
        } else {
            ClienteAcessoEntityBuilder clienteAcessoBuilder = ClienteAcessoEntity.builderOf();

            clienteAcessoBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setClientId( api.getClientId() );
            entity.setScopes( api.getScopes() );
            entity.setAtivo( api.getAtivo() );
            entity.setDataUltimoUso( api.getDataUltimoUso() );
            entity.setDataExpiracao( api.getDataExpiracao() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ClienteAcessoEntity patchApiToEntity(ClienteAcesso api, ClienteAcessoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getClientId() != null) entity.setClientId( api.getClientId() ); 
        if (api.getScopes() != null) entity.setScopes( api.getScopes() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getDataUltimoUso() != null) entity.setDataUltimoUso( api.getDataUltimoUso() ); 
        if (api.getDataExpiracao() != null) entity.setDataExpiracao( api.getDataExpiracao() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public ClienteAcessoEntity setEntityDefaultValues(ClienteAcessoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }

        return entity;
    }

    public ClienteAcesso setApiDefaultValues(ClienteAcesso api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }

        return api;
    }

}
