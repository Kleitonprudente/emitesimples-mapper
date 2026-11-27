package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.CategoriaFinanceira;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.financeiro.builder.CategoriaFinanceiraEntityBuilder;
import br.com.emitesimples.financeiro.repository.CategoriaFinanceiraRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.financeiro.domain.CategoriaFinanceira;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class CategoriaFinanceiraMapper {

    @Inject CategoriaFinanceiraRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;

    public CategoriaFinanceira entityToApi(CategoriaFinanceiraEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CategoriaFinanceira categoriaFinanceira = new CategoriaFinanceira();

        categoriaFinanceira.setVersion( entity.getVersion() );
        categoriaFinanceira.setNome( entity.getNome() );
        categoriaFinanceira.setTipo( entity.getTipo() );
        categoriaFinanceira.setDicas( entity.getDicas() );
        categoriaFinanceira.setAtivo( entity.getAtivo() );
        categoriaFinanceira.setId( entity.getId() );
        categoriaFinanceira.setDataCriacao( entity.getDataCriacao() );
        categoriaFinanceira.setDataAtualizacao( entity.getDataAtualizacao() );
        categoriaFinanceira.setCategoriaPai(this.entityToApi(entity.getCategoriaPai() ));
        categoriaFinanceira.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(categoriaFinanceira);

        return categoriaFinanceira;
    }

    public CategoriaFinanceiraEntity apiToEntity(CategoriaFinanceira api) {
        if ( api == null ) {
            return null;
        }
        CategoriaFinanceiraEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CategoriaFinanceira found for Id: " + api.getId()));
            }
        } else {
            CategoriaFinanceiraEntityBuilder categoriaFinanceiraBuilder = CategoriaFinanceiraEntity.builderOf();

            categoriaFinanceiraBuilder.version = api.getVersion();
            categoriaFinanceiraBuilder.nome = api.getNome();
            categoriaFinanceiraBuilder.tipo = api.getTipo();
            categoriaFinanceiraBuilder.dicas = api.getDicas();
            categoriaFinanceiraBuilder.ativo = api.getAtivo();
            categoriaFinanceiraBuilder.id = api.getId();
            categoriaFinanceiraBuilder.dataCriacao = api.getDataCriacao();
            categoriaFinanceiraBuilder.dataAtualizacao = api.getDataAtualizacao();
            categoriaFinanceiraBuilder.categoriaPai = this.apiToEntity(api.getCategoriaPai());
            categoriaFinanceiraBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = categoriaFinanceiraBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<CategoriaFinanceira> entityListToApiList(List<CategoriaFinanceiraEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<CategoriaFinanceira> list = new ArrayList<CategoriaFinanceira>( entity.size() );
        for ( CategoriaFinanceiraEntity categoriaFinanceiraEntity : entity ) {
            list.add( entityToApi( categoriaFinanceiraEntity ) );
        }

        return list;
    }

    public List<CategoriaFinanceiraEntity> apiListToEntityList(List<CategoriaFinanceira> api) {
        if ( api == null ) {
            return null;
        }

        List<CategoriaFinanceiraEntity> list = new ArrayList<CategoriaFinanceiraEntity>( api.size() );
        for ( CategoriaFinanceira categoriaFinanceira : api ) {
            list.add( apiToEntity( categoriaFinanceira ) );
        }

        return list;
    }

    public CategoriaFinanceira updateEntityToApi(CategoriaFinanceiraEntity entity, CategoriaFinanceira api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setTipo( entity.getTipo() );
        api.setDicas( entity.getDicas() );
        api.setAtivo( entity.getAtivo() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getCategoriaPai() != null ) {
            if ( api.getCategoriaPai() == null ) {
                api.setCategoriaPai( new CategoriaFinanceira() );
            }
            this.updateEntityToApi(entity.getCategoriaPai(), api.getCategoriaPai());
        } else {
            api.setCategoriaPai( null );
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

    public CategoriaFinanceiraEntity updateApiToEntity(CategoriaFinanceira api, CategoriaFinanceiraEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CategoriaFinanceira found for Id: " + api.getId()));
            }
        } else {
            CategoriaFinanceiraEntityBuilder categoriaFinanceiraBuilder = CategoriaFinanceiraEntity.builderOf();

            categoriaFinanceiraBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setTipo( api.getTipo() );
            entity.setDicas( api.getDicas() );
            entity.setAtivo( api.getAtivo() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getCategoriaPai() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setCategoriaPai(this.updateApiToEntity( api.getCategoriaPai(), entity.getCategoriaPai() != null ? entity.getCategoriaPai() : new CategoriaFinanceiraEntity()));
            } else {
                entity.setCategoriaPai( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public CategoriaFinanceiraEntity patchApiToEntity(CategoriaFinanceira api, CategoriaFinanceiraEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getDicas() != null) entity.setDicas( api.getDicas() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getCategoriaPai() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setCategoriaPai(this.updateApiToEntity( api.getCategoriaPai(), entity.getCategoriaPai() != null ? entity.getCategoriaPai() : new CategoriaFinanceiraEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public CategoriaFinanceiraEntity setEntityDefaultValues(CategoriaFinanceiraEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }

        return entity;
    }

    public CategoriaFinanceira setApiDefaultValues(CategoriaFinanceira api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }

        return api;
    }

}
