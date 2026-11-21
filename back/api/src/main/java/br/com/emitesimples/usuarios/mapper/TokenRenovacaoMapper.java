package br.com.emitesimples.usuarios.mapper;

import br.com.emitesimples.usuarios.domain.TokenRenovacao;
import br.com.emitesimples.usuarios.entity.TokenRenovacaoEntity;
import br.com.emitesimples.usuarios.builder.TokenRenovacaoEntityBuilder;
import br.com.emitesimples.usuarios.repository.TokenRenovacaoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class TokenRenovacaoMapper {

    @Inject TokenRenovacaoRepository repository;
    @Inject 
    UsuarioMapper usuarioMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public TokenRenovacao entityToApi(TokenRenovacaoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        TokenRenovacao tokenRenovacao = new TokenRenovacao();

        tokenRenovacao.setVersion( entity.getVersion() );
        tokenRenovacao.setDataExpiracao( entity.getDataExpiracao() );
        tokenRenovacao.setUsado( entity.getUsado() );
        tokenRenovacao.setRevogado( entity.getRevogado() );
        tokenRenovacao.setDataUso( entity.getDataUso() );
        tokenRenovacao.setDataRevogacao( entity.getDataRevogacao() );
        tokenRenovacao.setId( entity.getId() );
        tokenRenovacao.setDataCriacao( entity.getDataCriacao() );
        tokenRenovacao.setDataAtualizacao( entity.getDataAtualizacao() );
        tokenRenovacao.setUsuario(usuarioMapper.entityToApi(entity.getUsuario() ));
        tokenRenovacao.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(tokenRenovacao);

        return tokenRenovacao;
    }

    public TokenRenovacaoEntity apiToEntity(TokenRenovacao api) {
        if ( api == null ) {
            return null;
        }
        TokenRenovacaoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity TokenRenovacao found for Id: " + api.getId()));
            }
        } else {
            TokenRenovacaoEntityBuilder tokenRenovacaoBuilder = TokenRenovacaoEntity.builderOf();

            tokenRenovacaoBuilder.version = api.getVersion();
            tokenRenovacaoBuilder.dataExpiracao = api.getDataExpiracao();
            tokenRenovacaoBuilder.usado = api.getUsado();
            tokenRenovacaoBuilder.revogado = api.getRevogado();
            tokenRenovacaoBuilder.dataUso = api.getDataUso();
            tokenRenovacaoBuilder.dataRevogacao = api.getDataRevogacao();
            tokenRenovacaoBuilder.id = api.getId();
            tokenRenovacaoBuilder.dataCriacao = api.getDataCriacao();
            tokenRenovacaoBuilder.dataAtualizacao = api.getDataAtualizacao();
            tokenRenovacaoBuilder.usuario = usuarioMapper.apiToEntity(api.getUsuario());
            tokenRenovacaoBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = tokenRenovacaoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<TokenRenovacao> entityListToApiList(List<TokenRenovacaoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<TokenRenovacao> list = new ArrayList<TokenRenovacao>( entity.size() );
        for ( TokenRenovacaoEntity tokenRenovacaoEntity : entity ) {
            list.add( entityToApi( tokenRenovacaoEntity ) );
        }

        return list;
    }

    public List<TokenRenovacaoEntity> apiListToEntityList(List<TokenRenovacao> api) {
        if ( api == null ) {
            return null;
        }

        List<TokenRenovacaoEntity> list = new ArrayList<TokenRenovacaoEntity>( api.size() );
        for ( TokenRenovacao tokenRenovacao : api ) {
            list.add( apiToEntity( tokenRenovacao ) );
        }

        return list;
    }

    public TokenRenovacao updateEntityToApi(TokenRenovacaoEntity entity, TokenRenovacao api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setDataExpiracao( entity.getDataExpiracao() );
        api.setUsado( entity.getUsado() );
        api.setRevogado( entity.getRevogado() );
        api.setDataUso( entity.getDataUso() );
        api.setDataRevogacao( entity.getDataRevogacao() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getUsuario() != null ) {
            if ( api.getUsuario() == null ) {
                api.setUsuario( new Usuario() );
            }
            usuarioMapper.updateEntityToApi(entity.getUsuario(), api.getUsuario());
        } else {
            api.setUsuario( null );
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

    public TokenRenovacaoEntity updateApiToEntity(TokenRenovacao api, TokenRenovacaoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity TokenRenovacao found for Id: " + api.getId()));
            }
        } else {
            TokenRenovacaoEntityBuilder tokenRenovacaoBuilder = TokenRenovacaoEntity.builderOf();

            tokenRenovacaoBuilder.version = api.getVersion();
            entity.setDataExpiracao( api.getDataExpiracao() );
            entity.setUsado( api.getUsado() );
            entity.setRevogado( api.getRevogado() );
            entity.setDataUso( api.getDataUso() );
            entity.setDataRevogacao( api.getDataRevogacao() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getUsuario() != null ) {
                if ( entity.getUsuario() == null ) {
                    entity.setUsuario( new UsuarioEntity() );
                }
                entity.setUsuario(usuarioMapper.updateApiToEntity( api.getUsuario(), entity.getUsuario()));
            } else {
                entity.setUsuario( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public TokenRenovacaoEntity patchApiToEntity(TokenRenovacao api, TokenRenovacaoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getDataExpiracao() != null) entity.setDataExpiracao( api.getDataExpiracao() ); 
        if (api.getUsado() != null) entity.setUsado( api.getUsado() ); 
        if (api.getRevogado() != null) entity.setRevogado( api.getRevogado() ); 
        if (api.getDataUso() != null) entity.setDataUso( api.getDataUso() ); 
        if (api.getDataRevogacao() != null) entity.setDataRevogacao( api.getDataRevogacao() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getUsuario() != null ) {
            if ( entity.getUsuario() == null ) {
                entity.setUsuario( new UsuarioEntity() );
            }
            entity.setUsuario(usuarioMapper.updateApiToEntity( api.getUsuario(), entity.getUsuario()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public TokenRenovacaoEntity setEntityDefaultValues(TokenRenovacaoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getUsado() == null) {
            entity.setUsado(false);
        }
        if (entity.getRevogado() == null) {
            entity.setRevogado(false);
        }

        return entity;
    }

    public TokenRenovacao setApiDefaultValues(TokenRenovacao api) {
        if ( api == null ) {
            return null;
        }

        if (api.getUsado() == null) {
            api.setUsado(false);
        }
        if (api.getRevogado() == null) {
            api.setRevogado(false);
        }

        return api;
    }

}
