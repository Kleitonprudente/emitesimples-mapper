package br.com.emitesimples.usuarios.mapper;

import br.com.emitesimples.usuarios.domain.ConfirmacaoCadastro;
import br.com.emitesimples.usuarios.entity.ConfirmacaoCadastroEntity;
import br.com.emitesimples.usuarios.builder.ConfirmacaoCadastroEntityBuilder;
import br.com.emitesimples.usuarios.repository.ConfirmacaoCadastroRepository;

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
public class ConfirmacaoCadastroMapper {

    @Inject ConfirmacaoCadastroRepository repository;
    @Inject 
    UsuarioMapper usuarioMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public ConfirmacaoCadastro entityToApi(ConfirmacaoCadastroEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ConfirmacaoCadastro confirmacaoCadastro = new ConfirmacaoCadastro();

        confirmacaoCadastro.setVersion( entity.getVersion() );
        confirmacaoCadastro.setEmail( entity.getEmail() );
        confirmacaoCadastro.setDataExpiracao( entity.getDataExpiracao() );
        confirmacaoCadastro.setDataConfirmacao( entity.getDataConfirmacao() );
        confirmacaoCadastro.setEstado( entity.getEstado() );
        confirmacaoCadastro.setId( entity.getId() );
        confirmacaoCadastro.setDataCriacao( entity.getDataCriacao() );
        confirmacaoCadastro.setDataAtualizacao( entity.getDataAtualizacao() );
        confirmacaoCadastro.setUsuario(usuarioMapper.entityToApi(entity.getUsuario() ));
        confirmacaoCadastro.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(confirmacaoCadastro);

        return confirmacaoCadastro;
    }

    public ConfirmacaoCadastroEntity apiToEntity(ConfirmacaoCadastro api) {
        if ( api == null ) {
            return null;
        }
        ConfirmacaoCadastroEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ConfirmacaoCadastro found for Id: " + api.getId()));
            }
        } else {
            ConfirmacaoCadastroEntityBuilder confirmacaoCadastroBuilder = ConfirmacaoCadastroEntity.builderOf();

            confirmacaoCadastroBuilder.version = api.getVersion();
            confirmacaoCadastroBuilder.email = api.getEmail();
            confirmacaoCadastroBuilder.dataExpiracao = api.getDataExpiracao();
            confirmacaoCadastroBuilder.dataConfirmacao = api.getDataConfirmacao();
            confirmacaoCadastroBuilder.estado = api.getEstado();
            confirmacaoCadastroBuilder.id = api.getId();
            confirmacaoCadastroBuilder.dataCriacao = api.getDataCriacao();
            confirmacaoCadastroBuilder.dataAtualizacao = api.getDataAtualizacao();
            confirmacaoCadastroBuilder.usuario = usuarioMapper.apiToEntity(api.getUsuario());
            confirmacaoCadastroBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = confirmacaoCadastroBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<ConfirmacaoCadastro> entityListToApiList(List<ConfirmacaoCadastroEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ConfirmacaoCadastro> list = new ArrayList<ConfirmacaoCadastro>( entity.size() );
        for ( ConfirmacaoCadastroEntity confirmacaoCadastroEntity : entity ) {
            list.add( entityToApi( confirmacaoCadastroEntity ) );
        }

        return list;
    }

    public List<ConfirmacaoCadastroEntity> apiListToEntityList(List<ConfirmacaoCadastro> api) {
        if ( api == null ) {
            return null;
        }

        List<ConfirmacaoCadastroEntity> list = new ArrayList<ConfirmacaoCadastroEntity>( api.size() );
        for ( ConfirmacaoCadastro confirmacaoCadastro : api ) {
            list.add( apiToEntity( confirmacaoCadastro ) );
        }

        return list;
    }

    public ConfirmacaoCadastro updateEntityToApi(ConfirmacaoCadastroEntity entity, ConfirmacaoCadastro api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setEmail( entity.getEmail() );
        api.setDataExpiracao( entity.getDataExpiracao() );
        api.setDataConfirmacao( entity.getDataConfirmacao() );
        api.setEstado( entity.getEstado() );
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

    public ConfirmacaoCadastroEntity updateApiToEntity(ConfirmacaoCadastro api, ConfirmacaoCadastroEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ConfirmacaoCadastro found for Id: " + api.getId()));
            }
        } else {
            ConfirmacaoCadastroEntityBuilder confirmacaoCadastroBuilder = ConfirmacaoCadastroEntity.builderOf();

            confirmacaoCadastroBuilder.version = api.getVersion();
            entity.setEmail( api.getEmail() );
            entity.setDataExpiracao( api.getDataExpiracao() );
            entity.setDataConfirmacao( api.getDataConfirmacao() );
            entity.setEstado( api.getEstado() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getUsuario() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setUsuario(usuarioMapper.updateApiToEntity( api.getUsuario(), entity.getUsuario() != null ? entity.getUsuario() : new UsuarioEntity()));
            } else {
                entity.setUsuario( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ConfirmacaoCadastroEntity patchApiToEntity(ConfirmacaoCadastro api, ConfirmacaoCadastroEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getEmail() != null) entity.setEmail( api.getEmail() ); 
        if (api.getDataExpiracao() != null) entity.setDataExpiracao( api.getDataExpiracao() ); 
        if (api.getDataConfirmacao() != null) entity.setDataConfirmacao( api.getDataConfirmacao() ); 
        if (api.getEstado() != null) entity.setEstado( api.getEstado() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getUsuario() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setUsuario(usuarioMapper.updateApiToEntity( api.getUsuario(), entity.getUsuario() != null ? entity.getUsuario() : new UsuarioEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public ConfirmacaoCadastroEntity setEntityDefaultValues(ConfirmacaoCadastroEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getEstado() == null) {
            entity.setEstado("PENDENTE");
        }

        return entity;
    }

    public ConfirmacaoCadastro setApiDefaultValues(ConfirmacaoCadastro api) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstado() == null) {
            api.setEstado("PENDENTE");
        }

        return api;
    }

}
