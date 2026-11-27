package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.CobrancaEnviada;
import br.com.emitesimples.financeiro.entity.CobrancaEnviadaEntity;
import br.com.emitesimples.financeiro.builder.CobrancaEnviadaEntityBuilder;
import br.com.emitesimples.financeiro.repository.CobrancaEnviadaRepository;

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
public class CobrancaEnviadaMapper {

    @Inject CobrancaEnviadaRepository repository;
    @Inject 
    TituloMapper tituloMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public CobrancaEnviada entityToApi(CobrancaEnviadaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CobrancaEnviada cobrancaEnviada = new CobrancaEnviada();

        cobrancaEnviada.setVersion( entity.getVersion() );
        cobrancaEnviada.setIdPessoa( entity.getIdPessoa() );
        cobrancaEnviada.setOrigem( entity.getOrigem() );
        cobrancaEnviada.setDia( entity.getDia() );
        cobrancaEnviada.setEmail( entity.getEmail() );
        cobrancaEnviada.setEnviadoEm( entity.getEnviadoEm() );
        cobrancaEnviada.setIdEnviadoPor( entity.getIdEnviadoPor() );
        cobrancaEnviada.setId( entity.getId() );
        cobrancaEnviada.setDataCriacao( entity.getDataCriacao() );
        cobrancaEnviada.setDataAtualizacao( entity.getDataAtualizacao() );
        cobrancaEnviada.setTitulo(tituloMapper.entityToApi(entity.getTitulo() ));
        cobrancaEnviada.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(cobrancaEnviada);

        return cobrancaEnviada;
    }

    public CobrancaEnviadaEntity apiToEntity(CobrancaEnviada api) {
        if ( api == null ) {
            return null;
        }
        CobrancaEnviadaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CobrancaEnviada found for Id: " + api.getId()));
            }
        } else {
            CobrancaEnviadaEntityBuilder cobrancaEnviadaBuilder = CobrancaEnviadaEntity.builderOf();

            cobrancaEnviadaBuilder.version = api.getVersion();
            cobrancaEnviadaBuilder.idPessoa = api.getIdPessoa();
            cobrancaEnviadaBuilder.origem = api.getOrigem();
            cobrancaEnviadaBuilder.dia = api.getDia();
            cobrancaEnviadaBuilder.email = api.getEmail();
            cobrancaEnviadaBuilder.enviadoEm = api.getEnviadoEm();
            cobrancaEnviadaBuilder.idEnviadoPor = api.getIdEnviadoPor();
            cobrancaEnviadaBuilder.id = api.getId();
            cobrancaEnviadaBuilder.dataCriacao = api.getDataCriacao();
            cobrancaEnviadaBuilder.dataAtualizacao = api.getDataAtualizacao();
            cobrancaEnviadaBuilder.titulo = tituloMapper.apiToEntity(api.getTitulo());
            cobrancaEnviadaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = cobrancaEnviadaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<CobrancaEnviada> entityListToApiList(List<CobrancaEnviadaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<CobrancaEnviada> list = new ArrayList<CobrancaEnviada>( entity.size() );
        for ( CobrancaEnviadaEntity cobrancaEnviadaEntity : entity ) {
            list.add( entityToApi( cobrancaEnviadaEntity ) );
        }

        return list;
    }

    public List<CobrancaEnviadaEntity> apiListToEntityList(List<CobrancaEnviada> api) {
        if ( api == null ) {
            return null;
        }

        List<CobrancaEnviadaEntity> list = new ArrayList<CobrancaEnviadaEntity>( api.size() );
        for ( CobrancaEnviada cobrancaEnviada : api ) {
            list.add( apiToEntity( cobrancaEnviada ) );
        }

        return list;
    }

    public CobrancaEnviada updateEntityToApi(CobrancaEnviadaEntity entity, CobrancaEnviada api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setIdPessoa( entity.getIdPessoa() );
        api.setOrigem( entity.getOrigem() );
        api.setDia( entity.getDia() );
        api.setEmail( entity.getEmail() );
        api.setEnviadoEm( entity.getEnviadoEm() );
        api.setIdEnviadoPor( entity.getIdEnviadoPor() );
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

    public CobrancaEnviadaEntity updateApiToEntity(CobrancaEnviada api, CobrancaEnviadaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CobrancaEnviada found for Id: " + api.getId()));
            }
        } else {
            CobrancaEnviadaEntityBuilder cobrancaEnviadaBuilder = CobrancaEnviadaEntity.builderOf();

            cobrancaEnviadaBuilder.version = api.getVersion();
            entity.setIdPessoa( api.getIdPessoa() );
            entity.setOrigem( api.getOrigem() );
            entity.setDia( api.getDia() );
            entity.setEmail( api.getEmail() );
            entity.setEnviadoEm( api.getEnviadoEm() );
            entity.setIdEnviadoPor( api.getIdEnviadoPor() );
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

    public CobrancaEnviadaEntity patchApiToEntity(CobrancaEnviada api, CobrancaEnviadaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getIdPessoa() != null) entity.setIdPessoa( api.getIdPessoa() ); 
        if (api.getOrigem() != null) entity.setOrigem( api.getOrigem() ); 
        if (api.getDia() != null) entity.setDia( api.getDia() ); 
        if (api.getEmail() != null) entity.setEmail( api.getEmail() ); 
        if (api.getEnviadoEm() != null) entity.setEnviadoEm( api.getEnviadoEm() ); 
        if (api.getIdEnviadoPor() != null) entity.setIdEnviadoPor( api.getIdEnviadoPor() ); 
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

    public CobrancaEnviadaEntity setEntityDefaultValues(CobrancaEnviadaEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public CobrancaEnviada setApiDefaultValues(CobrancaEnviada api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
