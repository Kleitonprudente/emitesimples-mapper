package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.Cobranca;
import br.com.emitesimples.financeiro.entity.CobrancaEntity;
import br.com.emitesimples.financeiro.builder.CobrancaEntityBuilder;
import br.com.emitesimples.financeiro.repository.CobrancaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.financeiro.domain.Titulo;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.financeiro.domain.ContaFinanceira;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class CobrancaMapper {

    @Inject CobrancaRepository repository;
    @Inject 
    TituloMapper tituloMapper;
    @Inject 
    ContaFinanceiraMapper contaFinanceiraMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public Cobranca entityToApi(CobrancaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Cobranca cobranca = new Cobranca();

        cobranca.setVersion( entity.getVersion() );
        cobranca.setProvedor( entity.getProvedor() );
        cobranca.setTipo( entity.getTipo() );
        cobranca.setEstado( entity.getEstado() );
        cobranca.setValor( entity.getValor() );
        cobranca.setVencimento( entity.getVencimento() );
        cobranca.setMultaPercentual( entity.getMultaPercentual() );
        cobranca.setJurosMensalPercentual( entity.getJurosMensalPercentual() );
        cobranca.setIdExterno( entity.getIdExterno() );
        cobranca.setLinhaDigitavel( entity.getLinhaDigitavel() );
        cobranca.setCodigoDeBarras( entity.getCodigoDeBarras() );
        cobranca.setUrlDoBoleto( entity.getUrlDoBoleto() );
        cobranca.setPixCopiaECola( entity.getPixCopiaECola() );
        cobranca.setUrlDoPix( entity.getUrlDoPix() );
        cobranca.setUrlDoLink( entity.getUrlDoLink() );
        cobranca.setPagoEm( entity.getPagoEm() );
        cobranca.setValorPago( entity.getValorPago() );
        cobranca.setMeioPago( entity.getMeioPago() );
        cobranca.setTarifa( entity.getTarifa() );
        cobranca.setIdBaixaMovimento( entity.getIdBaixaMovimento() );
        cobranca.setIdTarifaMovimento( entity.getIdTarifaMovimento() );
        cobranca.setMotivo( entity.getMotivo() );
        cobranca.setRespostaDoProvedor( entity.getRespostaDoProvedor() );
        cobranca.setUltimaConsultaEm( entity.getUltimaConsultaEm() );
        cobranca.setIdCriadoPor( entity.getIdCriadoPor() );
        cobranca.setId( entity.getId() );
        cobranca.setDataCriacao( entity.getDataCriacao() );
        cobranca.setDataAtualizacao( entity.getDataAtualizacao() );
        cobranca.setTitulo(tituloMapper.entityToApi(entity.getTitulo() ));
        cobranca.setConta(contaFinanceiraMapper.entityToApi(entity.getConta() ));
        cobranca.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(cobranca);

        return cobranca;
    }

    public CobrancaEntity apiToEntity(Cobranca api) {
        if ( api == null ) {
            return null;
        }
        CobrancaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Cobranca found for Id: " + api.getId()));
            }
        } else {
            CobrancaEntityBuilder cobrancaBuilder = CobrancaEntity.builderOf();

            cobrancaBuilder.version = api.getVersion();
            cobrancaBuilder.provedor = api.getProvedor();
            cobrancaBuilder.tipo = api.getTipo();
            cobrancaBuilder.estado = api.getEstado();
            cobrancaBuilder.valor = api.getValor();
            cobrancaBuilder.vencimento = api.getVencimento();
            cobrancaBuilder.multaPercentual = api.getMultaPercentual();
            cobrancaBuilder.jurosMensalPercentual = api.getJurosMensalPercentual();
            cobrancaBuilder.idExterno = api.getIdExterno();
            cobrancaBuilder.linhaDigitavel = api.getLinhaDigitavel();
            cobrancaBuilder.codigoDeBarras = api.getCodigoDeBarras();
            cobrancaBuilder.urlDoBoleto = api.getUrlDoBoleto();
            cobrancaBuilder.pixCopiaECola = api.getPixCopiaECola();
            cobrancaBuilder.urlDoPix = api.getUrlDoPix();
            cobrancaBuilder.urlDoLink = api.getUrlDoLink();
            cobrancaBuilder.pagoEm = api.getPagoEm();
            cobrancaBuilder.valorPago = api.getValorPago();
            cobrancaBuilder.meioPago = api.getMeioPago();
            cobrancaBuilder.tarifa = api.getTarifa();
            cobrancaBuilder.idBaixaMovimento = api.getIdBaixaMovimento();
            cobrancaBuilder.idTarifaMovimento = api.getIdTarifaMovimento();
            cobrancaBuilder.motivo = api.getMotivo();
            cobrancaBuilder.respostaDoProvedor = api.getRespostaDoProvedor();
            cobrancaBuilder.ultimaConsultaEm = api.getUltimaConsultaEm();
            cobrancaBuilder.idCriadoPor = api.getIdCriadoPor();
            cobrancaBuilder.id = api.getId();
            cobrancaBuilder.dataCriacao = api.getDataCriacao();
            cobrancaBuilder.dataAtualizacao = api.getDataAtualizacao();
            cobrancaBuilder.titulo = tituloMapper.apiToEntity(api.getTitulo());
            cobrancaBuilder.conta = contaFinanceiraMapper.apiToEntity(api.getConta());
            cobrancaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = cobrancaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Cobranca> entityListToApiList(List<CobrancaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Cobranca> list = new ArrayList<Cobranca>( entity.size() );
        for ( CobrancaEntity cobrancaEntity : entity ) {
            list.add( entityToApi( cobrancaEntity ) );
        }

        return list;
    }

    public List<CobrancaEntity> apiListToEntityList(List<Cobranca> api) {
        if ( api == null ) {
            return null;
        }

        List<CobrancaEntity> list = new ArrayList<CobrancaEntity>( api.size() );
        for ( Cobranca cobranca : api ) {
            list.add( apiToEntity( cobranca ) );
        }

        return list;
    }

    public Cobranca updateEntityToApi(CobrancaEntity entity, Cobranca api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setProvedor( entity.getProvedor() );
        api.setTipo( entity.getTipo() );
        api.setEstado( entity.getEstado() );
        api.setValor( entity.getValor() );
        api.setVencimento( entity.getVencimento() );
        api.setMultaPercentual( entity.getMultaPercentual() );
        api.setJurosMensalPercentual( entity.getJurosMensalPercentual() );
        api.setIdExterno( entity.getIdExterno() );
        api.setLinhaDigitavel( entity.getLinhaDigitavel() );
        api.setCodigoDeBarras( entity.getCodigoDeBarras() );
        api.setUrlDoBoleto( entity.getUrlDoBoleto() );
        api.setPixCopiaECola( entity.getPixCopiaECola() );
        api.setUrlDoPix( entity.getUrlDoPix() );
        api.setUrlDoLink( entity.getUrlDoLink() );
        api.setPagoEm( entity.getPagoEm() );
        api.setValorPago( entity.getValorPago() );
        api.setMeioPago( entity.getMeioPago() );
        api.setTarifa( entity.getTarifa() );
        api.setIdBaixaMovimento( entity.getIdBaixaMovimento() );
        api.setIdTarifaMovimento( entity.getIdTarifaMovimento() );
        api.setMotivo( entity.getMotivo() );
        api.setRespostaDoProvedor( entity.getRespostaDoProvedor() );
        api.setUltimaConsultaEm( entity.getUltimaConsultaEm() );
        api.setIdCriadoPor( entity.getIdCriadoPor() );
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

    public CobrancaEntity updateApiToEntity(Cobranca api, CobrancaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Cobranca found for Id: " + api.getId()));
            }
        } else {
            CobrancaEntityBuilder cobrancaBuilder = CobrancaEntity.builderOf();

            cobrancaBuilder.version = api.getVersion();
            entity.setProvedor( api.getProvedor() );
            entity.setTipo( api.getTipo() );
            entity.setEstado( api.getEstado() );
            entity.setValor( api.getValor() );
            entity.setVencimento( api.getVencimento() );
            entity.setMultaPercentual( api.getMultaPercentual() );
            entity.setJurosMensalPercentual( api.getJurosMensalPercentual() );
            entity.setIdExterno( api.getIdExterno() );
            entity.setLinhaDigitavel( api.getLinhaDigitavel() );
            entity.setCodigoDeBarras( api.getCodigoDeBarras() );
            entity.setUrlDoBoleto( api.getUrlDoBoleto() );
            entity.setPixCopiaECola( api.getPixCopiaECola() );
            entity.setUrlDoPix( api.getUrlDoPix() );
            entity.setUrlDoLink( api.getUrlDoLink() );
            entity.setPagoEm( api.getPagoEm() );
            entity.setValorPago( api.getValorPago() );
            entity.setMeioPago( api.getMeioPago() );
            entity.setTarifa( api.getTarifa() );
            entity.setIdBaixaMovimento( api.getIdBaixaMovimento() );
            entity.setIdTarifaMovimento( api.getIdTarifaMovimento() );
            entity.setMotivo( api.getMotivo() );
            entity.setRespostaDoProvedor( api.getRespostaDoProvedor() );
            entity.setUltimaConsultaEm( api.getUltimaConsultaEm() );
            entity.setIdCriadoPor( api.getIdCriadoPor() );
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

    public CobrancaEntity patchApiToEntity(Cobranca api, CobrancaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getProvedor() != null) entity.setProvedor( api.getProvedor() ); 
        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getEstado() != null) entity.setEstado( api.getEstado() ); 
        if (api.getValor() != null) entity.setValor( api.getValor() ); 
        if (api.getVencimento() != null) entity.setVencimento( api.getVencimento() ); 
        if (api.getMultaPercentual() != null) entity.setMultaPercentual( api.getMultaPercentual() ); 
        if (api.getJurosMensalPercentual() != null) entity.setJurosMensalPercentual( api.getJurosMensalPercentual() ); 
        if (api.getIdExterno() != null) entity.setIdExterno( api.getIdExterno() ); 
        if (api.getLinhaDigitavel() != null) entity.setLinhaDigitavel( api.getLinhaDigitavel() ); 
        if (api.getCodigoDeBarras() != null) entity.setCodigoDeBarras( api.getCodigoDeBarras() ); 
        if (api.getUrlDoBoleto() != null) entity.setUrlDoBoleto( api.getUrlDoBoleto() ); 
        if (api.getPixCopiaECola() != null) entity.setPixCopiaECola( api.getPixCopiaECola() ); 
        if (api.getUrlDoPix() != null) entity.setUrlDoPix( api.getUrlDoPix() ); 
        if (api.getUrlDoLink() != null) entity.setUrlDoLink( api.getUrlDoLink() ); 
        if (api.getPagoEm() != null) entity.setPagoEm( api.getPagoEm() ); 
        if (api.getValorPago() != null) entity.setValorPago( api.getValorPago() ); 
        if (api.getMeioPago() != null) entity.setMeioPago( api.getMeioPago() ); 
        if (api.getTarifa() != null) entity.setTarifa( api.getTarifa() ); 
        if (api.getIdBaixaMovimento() != null) entity.setIdBaixaMovimento( api.getIdBaixaMovimento() ); 
        if (api.getIdTarifaMovimento() != null) entity.setIdTarifaMovimento( api.getIdTarifaMovimento() ); 
        if (api.getMotivo() != null) entity.setMotivo( api.getMotivo() ); 
        if (api.getRespostaDoProvedor() != null) entity.setRespostaDoProvedor( api.getRespostaDoProvedor() ); 
        if (api.getUltimaConsultaEm() != null) entity.setUltimaConsultaEm( api.getUltimaConsultaEm() ); 
        if (api.getIdCriadoPor() != null) entity.setIdCriadoPor( api.getIdCriadoPor() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getTitulo() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setTitulo(tituloMapper.updateApiToEntity( api.getTitulo(), entity.getTitulo() != null ? entity.getTitulo() : new TituloEntity()));
        }
        if ( api.getConta() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setConta(contaFinanceiraMapper.updateApiToEntity( api.getConta(), entity.getConta() != null ? entity.getConta() : new ContaFinanceiraEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public CobrancaEntity setEntityDefaultValues(CobrancaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getEstado() == null) {
            entity.setEstado("CRIANDO");
        }

        return entity;
    }

    public Cobranca setApiDefaultValues(Cobranca api) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstado() == null) {
            api.setEstado("CRIANDO");
        }

        return api;
    }

}
