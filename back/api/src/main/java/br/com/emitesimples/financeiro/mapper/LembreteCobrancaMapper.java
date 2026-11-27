package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.LembreteCobranca;
import br.com.emitesimples.financeiro.entity.LembreteCobrancaEntity;
import br.com.emitesimples.financeiro.builder.LembreteCobrancaEntityBuilder;
import br.com.emitesimples.financeiro.repository.LembreteCobrancaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.financeiro.domain.Titulo;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class LembreteCobrancaMapper {

    @Inject LembreteCobrancaRepository repository;
    @Inject 
    TituloMapper tituloMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public LembreteCobranca entityToApi(LembreteCobrancaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LembreteCobranca lembreteCobranca = new LembreteCobranca();

        lembreteCobranca.setVersion( entity.getVersion() );
        lembreteCobranca.setTipo( entity.getTipo() );
        lembreteCobranca.setDias( entity.getDias() );
        lembreteCobranca.setEnviadoEm( entity.getEnviadoEm() );
        lembreteCobranca.setId( entity.getId() );
        lembreteCobranca.setDataCriacao( entity.getDataCriacao() );
        lembreteCobranca.setDataAtualizacao( entity.getDataAtualizacao() );
        lembreteCobranca.setTitulo(tituloMapper.entityToApi(entity.getTitulo() ));
        lembreteCobranca.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(lembreteCobranca);

        return lembreteCobranca;
    }

    public LembreteCobrancaEntity apiToEntity(LembreteCobranca api) {
        if ( api == null ) {
            return null;
        }
        LembreteCobrancaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity LembreteCobranca found for Id: " + api.getId()));
            }
        } else {
            LembreteCobrancaEntityBuilder lembreteCobrancaBuilder = LembreteCobrancaEntity.builderOf();

            lembreteCobrancaBuilder.version = api.getVersion();
            lembreteCobrancaBuilder.tipo = api.getTipo();
            lembreteCobrancaBuilder.dias = api.getDias();
            lembreteCobrancaBuilder.enviadoEm = api.getEnviadoEm();
            lembreteCobrancaBuilder.id = api.getId();
            lembreteCobrancaBuilder.dataCriacao = api.getDataCriacao();
            lembreteCobrancaBuilder.dataAtualizacao = api.getDataAtualizacao();
            lembreteCobrancaBuilder.titulo = tituloMapper.apiToEntity(api.getTitulo());
            lembreteCobrancaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = lembreteCobrancaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<LembreteCobranca> entityListToApiList(List<LembreteCobrancaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<LembreteCobranca> list = new ArrayList<LembreteCobranca>( entity.size() );
        for ( LembreteCobrancaEntity lembreteCobrancaEntity : entity ) {
            list.add( entityToApi( lembreteCobrancaEntity ) );
        }

        return list;
    }

    public List<LembreteCobrancaEntity> apiListToEntityList(List<LembreteCobranca> api) {
        if ( api == null ) {
            return null;
        }

        List<LembreteCobrancaEntity> list = new ArrayList<LembreteCobrancaEntity>( api.size() );
        for ( LembreteCobranca lembreteCobranca : api ) {
            list.add( apiToEntity( lembreteCobranca ) );
        }

        return list;
    }

    public LembreteCobranca updateEntityToApi(LembreteCobrancaEntity entity, LembreteCobranca api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setTipo( entity.getTipo() );
        api.setDias( entity.getDias() );
        api.setEnviadoEm( entity.getEnviadoEm() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getTitulo() != null ) {
            if ( api.getTitulo() == null ) {
                api.setTitulo( new Titulo() );
            }
            tituloMapper.updateEntityToApi(entity.getTitulo(), api.getTitulo());
        } else {
            api.setTitulo( null );
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

    public LembreteCobrancaEntity updateApiToEntity(LembreteCobranca api, LembreteCobrancaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity LembreteCobranca found for Id: " + api.getId()));
            }
        } else {
            LembreteCobrancaEntityBuilder lembreteCobrancaBuilder = LembreteCobrancaEntity.builderOf();

            lembreteCobrancaBuilder.version = api.getVersion();
            entity.setTipo( api.getTipo() );
            entity.setDias( api.getDias() );
            entity.setEnviadoEm( api.getEnviadoEm() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getTitulo() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setTitulo(tituloMapper.updateApiToEntity( api.getTitulo(), entity.getTitulo() != null ? entity.getTitulo() : new TituloEntity()));
            } else {
                entity.setTitulo( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public LembreteCobrancaEntity patchApiToEntity(LembreteCobranca api, LembreteCobrancaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getDias() != null) entity.setDias( api.getDias() ); 
        if (api.getEnviadoEm() != null) entity.setEnviadoEm( api.getEnviadoEm() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getTitulo() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setTitulo(tituloMapper.updateApiToEntity( api.getTitulo(), entity.getTitulo() != null ? entity.getTitulo() : new TituloEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public LembreteCobrancaEntity setEntityDefaultValues(LembreteCobrancaEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public LembreteCobranca setApiDefaultValues(LembreteCobranca api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
