package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.Titulo;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.financeiro.builder.TituloEntityBuilder;
import br.com.emitesimples.financeiro.repository.TituloRepository;

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
import java.math.BigDecimal;

@ApplicationScoped
public class TituloMapper {

    @Inject TituloRepository repository;
    @Inject 
    CategoriaFinanceiraMapper categoriaFinanceiraMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public Titulo entityToApi(TituloEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Titulo titulo = new Titulo();

        titulo.setVersion( entity.getVersion() );
        titulo.setTipo( entity.getTipo() );
        titulo.setDescricao( entity.getDescricao() );
        titulo.setObservacao( entity.getObservacao() );
        titulo.setIdPessoa( entity.getIdPessoa() );
        titulo.setValor( entity.getValor() );
        titulo.setQuitado( entity.getQuitado() );
        titulo.setMultaPercentual( entity.getMultaPercentual() );
        titulo.setJurosMensalPercentual( entity.getJurosMensalPercentual() );
        titulo.setVencimento( entity.getVencimento() );
        titulo.setFormaPrevista( entity.getFormaPrevista() );
        titulo.setParcela( entity.getParcela() );
        titulo.setTotalParcelas( entity.getTotalParcelas() );
        titulo.setIdLancamento( entity.getIdLancamento() );
        titulo.setOrigemTipo( entity.getOrigemTipo() );
        titulo.setIdOrigem( entity.getIdOrigem() );
        titulo.setOrigemNumero( entity.getOrigemNumero() );
        titulo.setSituacao( entity.getSituacao() );
        titulo.setParaRevisao( entity.getParaRevisao() );
        titulo.setCanceladoEm( entity.getCanceladoEm() );
        titulo.setAprovadoEm( entity.getAprovadoEm() );
        titulo.setIdAprovadoPor( entity.getIdAprovadoPor() );
        titulo.setIdCriadoPor( entity.getIdCriadoPor() );
        titulo.setIdCriadoPorCliente( entity.getIdCriadoPorCliente() );
        titulo.setId( entity.getId() );
        titulo.setDataCriacao( entity.getDataCriacao() );
        titulo.setDataAtualizacao( entity.getDataAtualizacao() );
        titulo.setCategoria(categoriaFinanceiraMapper.entityToApi(entity.getCategoria() ));
        titulo.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(titulo);

        return titulo;
    }

    public TituloEntity apiToEntity(Titulo api) {
        if ( api == null ) {
            return null;
        }
        TituloEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Titulo found for Id: " + api.getId()));
            }
        } else {
            TituloEntityBuilder tituloBuilder = TituloEntity.builderOf();

            tituloBuilder.version = api.getVersion();
            tituloBuilder.tipo = api.getTipo();
            tituloBuilder.descricao = api.getDescricao();
            tituloBuilder.observacao = api.getObservacao();
            tituloBuilder.idPessoa = api.getIdPessoa();
            tituloBuilder.valor = api.getValor();
            tituloBuilder.quitado = api.getQuitado();
            tituloBuilder.multaPercentual = api.getMultaPercentual();
            tituloBuilder.jurosMensalPercentual = api.getJurosMensalPercentual();
            tituloBuilder.vencimento = api.getVencimento();
            tituloBuilder.formaPrevista = api.getFormaPrevista();
            tituloBuilder.parcela = api.getParcela();
            tituloBuilder.totalParcelas = api.getTotalParcelas();
            tituloBuilder.idLancamento = api.getIdLancamento();
            tituloBuilder.origemTipo = api.getOrigemTipo();
            tituloBuilder.idOrigem = api.getIdOrigem();
            tituloBuilder.origemNumero = api.getOrigemNumero();
            tituloBuilder.situacao = api.getSituacao();
            tituloBuilder.paraRevisao = api.getParaRevisao();
            tituloBuilder.canceladoEm = api.getCanceladoEm();
            tituloBuilder.aprovadoEm = api.getAprovadoEm();
            tituloBuilder.idAprovadoPor = api.getIdAprovadoPor();
            tituloBuilder.idCriadoPor = api.getIdCriadoPor();
            tituloBuilder.idCriadoPorCliente = api.getIdCriadoPorCliente();
            tituloBuilder.id = api.getId();
            tituloBuilder.dataCriacao = api.getDataCriacao();
            tituloBuilder.dataAtualizacao = api.getDataAtualizacao();
            tituloBuilder.categoria = categoriaFinanceiraMapper.apiToEntity(api.getCategoria());
            tituloBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = tituloBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Titulo> entityListToApiList(List<TituloEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Titulo> list = new ArrayList<Titulo>( entity.size() );
        for ( TituloEntity tituloEntity : entity ) {
            list.add( entityToApi( tituloEntity ) );
        }

        return list;
    }

    public List<TituloEntity> apiListToEntityList(List<Titulo> api) {
        if ( api == null ) {
            return null;
        }

        List<TituloEntity> list = new ArrayList<TituloEntity>( api.size() );
        for ( Titulo titulo : api ) {
            list.add( apiToEntity( titulo ) );
        }

        return list;
    }

    public Titulo updateEntityToApi(TituloEntity entity, Titulo api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setTipo( entity.getTipo() );
        api.setDescricao( entity.getDescricao() );
        api.setObservacao( entity.getObservacao() );
        api.setIdPessoa( entity.getIdPessoa() );
        api.setValor( entity.getValor() );
        api.setQuitado( entity.getQuitado() );
        api.setMultaPercentual( entity.getMultaPercentual() );
        api.setJurosMensalPercentual( entity.getJurosMensalPercentual() );
        api.setVencimento( entity.getVencimento() );
        api.setFormaPrevista( entity.getFormaPrevista() );
        api.setParcela( entity.getParcela() );
        api.setTotalParcelas( entity.getTotalParcelas() );
        api.setIdLancamento( entity.getIdLancamento() );
        api.setOrigemTipo( entity.getOrigemTipo() );
        api.setIdOrigem( entity.getIdOrigem() );
        api.setOrigemNumero( entity.getOrigemNumero() );
        api.setSituacao( entity.getSituacao() );
        api.setParaRevisao( entity.getParaRevisao() );
        api.setCanceladoEm( entity.getCanceladoEm() );
        api.setAprovadoEm( entity.getAprovadoEm() );
        api.setIdAprovadoPor( entity.getIdAprovadoPor() );
        api.setIdCriadoPor( entity.getIdCriadoPor() );
        api.setIdCriadoPorCliente( entity.getIdCriadoPorCliente() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

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

    public TituloEntity updateApiToEntity(Titulo api, TituloEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Titulo found for Id: " + api.getId()));
            }
        } else {
            TituloEntityBuilder tituloBuilder = TituloEntity.builderOf();

            tituloBuilder.version = api.getVersion();
            entity.setTipo( api.getTipo() );
            entity.setDescricao( api.getDescricao() );
            entity.setObservacao( api.getObservacao() );
            entity.setIdPessoa( api.getIdPessoa() );
            entity.setValor( api.getValor() );
            entity.setQuitado( api.getQuitado() );
            entity.setMultaPercentual( api.getMultaPercentual() );
            entity.setJurosMensalPercentual( api.getJurosMensalPercentual() );
            entity.setVencimento( api.getVencimento() );
            entity.setFormaPrevista( api.getFormaPrevista() );
            entity.setParcela( api.getParcela() );
            entity.setTotalParcelas( api.getTotalParcelas() );
            entity.setIdLancamento( api.getIdLancamento() );
            entity.setOrigemTipo( api.getOrigemTipo() );
            entity.setIdOrigem( api.getIdOrigem() );
            entity.setOrigemNumero( api.getOrigemNumero() );
            entity.setSituacao( api.getSituacao() );
            entity.setParaRevisao( api.getParaRevisao() );
            entity.setCanceladoEm( api.getCanceladoEm() );
            entity.setAprovadoEm( api.getAprovadoEm() );
            entity.setIdAprovadoPor( api.getIdAprovadoPor() );
            entity.setIdCriadoPor( api.getIdCriadoPor() );
            entity.setIdCriadoPorCliente( api.getIdCriadoPorCliente() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

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

    public TituloEntity patchApiToEntity(Titulo api, TituloEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getDescricao() != null) entity.setDescricao( api.getDescricao() ); 
        if (api.getObservacao() != null) entity.setObservacao( api.getObservacao() ); 
        if (api.getIdPessoa() != null) entity.setIdPessoa( api.getIdPessoa() ); 
        if (api.getValor() != null) entity.setValor( api.getValor() ); 
        if (api.getQuitado() != null) entity.setQuitado( api.getQuitado() ); 
        if (api.getMultaPercentual() != null) entity.setMultaPercentual( api.getMultaPercentual() ); 
        if (api.getJurosMensalPercentual() != null) entity.setJurosMensalPercentual( api.getJurosMensalPercentual() ); 
        if (api.getVencimento() != null) entity.setVencimento( api.getVencimento() ); 
        if (api.getFormaPrevista() != null) entity.setFormaPrevista( api.getFormaPrevista() ); 
        if (api.getParcela() != null) entity.setParcela( api.getParcela() ); 
        if (api.getTotalParcelas() != null) entity.setTotalParcelas( api.getTotalParcelas() ); 
        if (api.getIdLancamento() != null) entity.setIdLancamento( api.getIdLancamento() ); 
        if (api.getOrigemTipo() != null) entity.setOrigemTipo( api.getOrigemTipo() ); 
        if (api.getIdOrigem() != null) entity.setIdOrigem( api.getIdOrigem() ); 
        if (api.getOrigemNumero() != null) entity.setOrigemNumero( api.getOrigemNumero() ); 
        if (api.getSituacao() != null) entity.setSituacao( api.getSituacao() ); 
        if (api.getParaRevisao() != null) entity.setParaRevisao( api.getParaRevisao() ); 
        if (api.getCanceladoEm() != null) entity.setCanceladoEm( api.getCanceladoEm() ); 
        if (api.getAprovadoEm() != null) entity.setAprovadoEm( api.getAprovadoEm() ); 
        if (api.getIdAprovadoPor() != null) entity.setIdAprovadoPor( api.getIdAprovadoPor() ); 
        if (api.getIdCriadoPor() != null) entity.setIdCriadoPor( api.getIdCriadoPor() ); 
        if (api.getIdCriadoPorCliente() != null) entity.setIdCriadoPorCliente( api.getIdCriadoPorCliente() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getCategoria() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setCategoria(categoriaFinanceiraMapper.updateApiToEntity( api.getCategoria(), entity.getCategoria() != null ? entity.getCategoria() : new CategoriaFinanceiraEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public TituloEntity setEntityDefaultValues(TituloEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getQuitado() == null) {
            entity.setQuitado(new BigDecimal("0"));
        }
        if (entity.getParcela() == null) {
            entity.setParcela(1);
        }
        if (entity.getTotalParcelas() == null) {
            entity.setTotalParcelas(1);
        }
        if (entity.getOrigemTipo() == null) {
            entity.setOrigemTipo("MANUAL");
        }
        if (entity.getSituacao() == null) {
            entity.setSituacao("ABERTO");
        }
        if (entity.getParaRevisao() == null) {
            entity.setParaRevisao(false);
        }

        return entity;
    }

    public Titulo setApiDefaultValues(Titulo api) {
        if ( api == null ) {
            return null;
        }

        if (api.getQuitado() == null) {
            api.setQuitado(new BigDecimal("0"));
        }
        if (api.getParcela() == null) {
            api.setParcela(1);
        }
        if (api.getTotalParcelas() == null) {
            api.setTotalParcelas(1);
        }
        if (api.getOrigemTipo() == null) {
            api.setOrigemTipo("MANUAL");
        }
        if (api.getSituacao() == null) {
            api.setSituacao("ABERTO");
        }
        if (api.getParaRevisao() == null) {
            api.setParaRevisao(false);
        }

        return api;
    }

}
