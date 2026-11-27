package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.Movimento;
import br.com.emitesimples.financeiro.entity.MovimentoEntity;
import br.com.emitesimples.financeiro.builder.MovimentoEntityBuilder;
import br.com.emitesimples.financeiro.repository.MovimentoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.financeiro.domain.ContaFinanceira;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.financeiro.domain.Titulo;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.financeiro.domain.CategoriaFinanceira;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import java.math.BigDecimal;

@ApplicationScoped
public class MovimentoMapper {

    @Inject MovimentoRepository repository;
    @Inject 
    ContaFinanceiraMapper contaFinanceiraMapper;
    @Inject 
    TituloMapper tituloMapper;
    @Inject 
    CategoriaFinanceiraMapper categoriaFinanceiraMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public Movimento entityToApi(MovimentoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Movimento movimento = new Movimento();

        movimento.setVersion( entity.getVersion() );
        movimento.setTipo( entity.getTipo() );
        movimento.setSentido( entity.getSentido() );
        movimento.setData( entity.getData() );
        movimento.setDescricao( entity.getDescricao() );
        movimento.setForma( entity.getForma() );
        movimento.setValor( entity.getValor() );
        movimento.setQuitado( entity.getQuitado() );
        movimento.setJuros( entity.getJuros() );
        movimento.setMulta( entity.getMulta() );
        movimento.setDesconto( entity.getDesconto() );
        movimento.setIdLote( entity.getIdLote() );
        movimento.setIdTransferencia( entity.getIdTransferencia() );
        movimento.setIdEstornoDe( entity.getIdEstornoDe() );
        movimento.setIdCriadoPor( entity.getIdCriadoPor() );
        movimento.setId( entity.getId() );
        movimento.setDataCriacao( entity.getDataCriacao() );
        movimento.setConta(contaFinanceiraMapper.entityToApi(entity.getConta() ));
        movimento.setTitulo(tituloMapper.entityToApi(entity.getTitulo() ));
        movimento.setCategoria(categoriaFinanceiraMapper.entityToApi(entity.getCategoria() ));
        movimento.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(movimento);

        return movimento;
    }

    public MovimentoEntity apiToEntity(Movimento api) {
        if ( api == null ) {
            return null;
        }
        MovimentoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Movimento found for Id: " + api.getId()));
            }
        } else {
            MovimentoEntityBuilder movimentoBuilder = MovimentoEntity.builderOf();

            movimentoBuilder.version = api.getVersion();
            movimentoBuilder.tipo = api.getTipo();
            movimentoBuilder.sentido = api.getSentido();
            movimentoBuilder.data = api.getData();
            movimentoBuilder.descricao = api.getDescricao();
            movimentoBuilder.forma = api.getForma();
            movimentoBuilder.valor = api.getValor();
            movimentoBuilder.quitado = api.getQuitado();
            movimentoBuilder.juros = api.getJuros();
            movimentoBuilder.multa = api.getMulta();
            movimentoBuilder.desconto = api.getDesconto();
            movimentoBuilder.idLote = api.getIdLote();
            movimentoBuilder.idTransferencia = api.getIdTransferencia();
            movimentoBuilder.idEstornoDe = api.getIdEstornoDe();
            movimentoBuilder.idCriadoPor = api.getIdCriadoPor();
            movimentoBuilder.id = api.getId();
            movimentoBuilder.dataCriacao = api.getDataCriacao();
            movimentoBuilder.conta = contaFinanceiraMapper.apiToEntity(api.getConta());
            movimentoBuilder.titulo = tituloMapper.apiToEntity(api.getTitulo());
            movimentoBuilder.categoria = categoriaFinanceiraMapper.apiToEntity(api.getCategoria());
            movimentoBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = movimentoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Movimento> entityListToApiList(List<MovimentoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Movimento> list = new ArrayList<Movimento>( entity.size() );
        for ( MovimentoEntity movimentoEntity : entity ) {
            list.add( entityToApi( movimentoEntity ) );
        }

        return list;
    }

    public List<MovimentoEntity> apiListToEntityList(List<Movimento> api) {
        if ( api == null ) {
            return null;
        }

        List<MovimentoEntity> list = new ArrayList<MovimentoEntity>( api.size() );
        for ( Movimento movimento : api ) {
            list.add( apiToEntity( movimento ) );
        }

        return list;
    }

    public Movimento updateEntityToApi(MovimentoEntity entity, Movimento api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setTipo( entity.getTipo() );
        api.setSentido( entity.getSentido() );
        api.setData( entity.getData() );
        api.setDescricao( entity.getDescricao() );
        api.setForma( entity.getForma() );
        api.setValor( entity.getValor() );
        api.setQuitado( entity.getQuitado() );
        api.setJuros( entity.getJuros() );
        api.setMulta( entity.getMulta() );
        api.setDesconto( entity.getDesconto() );
        api.setIdLote( entity.getIdLote() );
        api.setIdTransferencia( entity.getIdTransferencia() );
        api.setIdEstornoDe( entity.getIdEstornoDe() );
        api.setIdCriadoPor( entity.getIdCriadoPor() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );

        if ( entity.getConta() != null ) {
            if ( api.getConta() == null ) {
                api.setConta( new ContaFinanceira() );
            }
            contaFinanceiraMapper.updateEntityToApi(entity.getConta(), api.getConta());
        } else {
            api.setConta( null );
        }

        if ( entity.getTitulo() != null ) {
            if ( api.getTitulo() == null ) {
                api.setTitulo( new Titulo() );
            }
            tituloMapper.updateEntityToApi(entity.getTitulo(), api.getTitulo());
        } else {
            api.setTitulo( null );
        }

        if ( entity.getCategoria() != null ) {
            if ( api.getCategoria() == null ) {
                api.setCategoria( new CategoriaFinanceira() );
            }
            categoriaFinanceiraMapper.updateEntityToApi(entity.getCategoria(), api.getCategoria());
        } else {
            api.setCategoria( null );
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

    public MovimentoEntity updateApiToEntity(Movimento api, MovimentoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Movimento found for Id: " + api.getId()));
            }
        } else {
            MovimentoEntityBuilder movimentoBuilder = MovimentoEntity.builderOf();

            movimentoBuilder.version = api.getVersion();
            entity.setTipo( api.getTipo() );
            entity.setSentido( api.getSentido() );
            entity.setData( api.getData() );
            entity.setDescricao( api.getDescricao() );
            entity.setForma( api.getForma() );
            entity.setValor( api.getValor() );
            entity.setQuitado( api.getQuitado() );
            entity.setJuros( api.getJuros() );
            entity.setMulta( api.getMulta() );
            entity.setDesconto( api.getDesconto() );
            entity.setIdLote( api.getIdLote() );
            entity.setIdTransferencia( api.getIdTransferencia() );
            entity.setIdEstornoDe( api.getIdEstornoDe() );
            entity.setIdCriadoPor( api.getIdCriadoPor() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );

            if ( api.getConta() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setConta(contaFinanceiraMapper.updateApiToEntity( api.getConta(), entity.getConta() != null ? entity.getConta() : new ContaFinanceiraEntity()));
            } else {
                entity.setConta( null );
            }

            if ( api.getTitulo() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setTitulo(tituloMapper.updateApiToEntity( api.getTitulo(), entity.getTitulo() != null ? entity.getTitulo() : new TituloEntity()));
            } else {
                entity.setTitulo( null );
            }

            if ( api.getCategoria() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setCategoria(categoriaFinanceiraMapper.updateApiToEntity( api.getCategoria(), entity.getCategoria() != null ? entity.getCategoria() : new CategoriaFinanceiraEntity()));
            } else {
                entity.setCategoria( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public MovimentoEntity patchApiToEntity(Movimento api, MovimentoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getSentido() != null) entity.setSentido( api.getSentido() ); 
        if (api.getData() != null) entity.setData( api.getData() ); 
        if (api.getDescricao() != null) entity.setDescricao( api.getDescricao() ); 
        if (api.getForma() != null) entity.setForma( api.getForma() ); 
        if (api.getValor() != null) entity.setValor( api.getValor() ); 
        if (api.getQuitado() != null) entity.setQuitado( api.getQuitado() ); 
        if (api.getJuros() != null) entity.setJuros( api.getJuros() ); 
        if (api.getMulta() != null) entity.setMulta( api.getMulta() ); 
        if (api.getDesconto() != null) entity.setDesconto( api.getDesconto() ); 
        if (api.getIdLote() != null) entity.setIdLote( api.getIdLote() ); 
        if (api.getIdTransferencia() != null) entity.setIdTransferencia( api.getIdTransferencia() ); 
        if (api.getIdEstornoDe() != null) entity.setIdEstornoDe( api.getIdEstornoDe() ); 
        if (api.getIdCriadoPor() != null) entity.setIdCriadoPor( api.getIdCriadoPor() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if ( api.getConta() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setConta(contaFinanceiraMapper.updateApiToEntity( api.getConta(), entity.getConta() != null ? entity.getConta() : new ContaFinanceiraEntity()));
        }
        if ( api.getTitulo() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setTitulo(tituloMapper.updateApiToEntity( api.getTitulo(), entity.getTitulo() != null ? entity.getTitulo() : new TituloEntity()));
        }
        if ( api.getCategoria() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setCategoria(categoriaFinanceiraMapper.updateApiToEntity( api.getCategoria(), entity.getCategoria() != null ? entity.getCategoria() : new CategoriaFinanceiraEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public MovimentoEntity setEntityDefaultValues(MovimentoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getQuitado() == null) {
            entity.setQuitado(new BigDecimal("0"));
        }
        if (entity.getJuros() == null) {
            entity.setJuros(new BigDecimal("0"));
        }
        if (entity.getMulta() == null) {
            entity.setMulta(new BigDecimal("0"));
        }
        if (entity.getDesconto() == null) {
            entity.setDesconto(new BigDecimal("0"));
        }

        return entity;
    }

    public Movimento setApiDefaultValues(Movimento api) {
        if ( api == null ) {
            return null;
        }

        if (api.getQuitado() == null) {
            api.setQuitado(new BigDecimal("0"));
        }
        if (api.getJuros() == null) {
            api.setJuros(new BigDecimal("0"));
        }
        if (api.getMulta() == null) {
            api.setMulta(new BigDecimal("0"));
        }
        if (api.getDesconto() == null) {
            api.setDesconto(new BigDecimal("0"));
        }

        return api;
    }

}
