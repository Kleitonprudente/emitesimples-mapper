package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.ContaFinanceira;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.financeiro.builder.ContaFinanceiraEntityBuilder;
import br.com.emitesimples.financeiro.repository.ContaFinanceiraRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import java.math.BigDecimal;

@ApplicationScoped
public class ContaFinanceiraMapper {

    @Inject ContaFinanceiraRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;

    public ContaFinanceira entityToApi(ContaFinanceiraEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ContaFinanceira contaFinanceira = new ContaFinanceira();

        contaFinanceira.setVersion( entity.getVersion() );
        contaFinanceira.setNome( entity.getNome() );
        contaFinanceira.setTipo( entity.getTipo() );
        contaFinanceira.setAtivo( entity.getAtivo() );
        contaFinanceira.setCodigoBanco( entity.getCodigoBanco() );
        contaFinanceira.setAgencia( entity.getAgencia() );
        contaFinanceira.setDadosDoBanco( entity.getDadosDoBanco() );
        contaFinanceira.setSaldoInicial( entity.getSaldoInicial() );
        contaFinanceira.setDataSaldoInicial( entity.getDataSaldoInicial() );
        contaFinanceira.setPadraoDinheiro( entity.getPadraoDinheiro() );
        contaFinanceira.setPadraoPix( entity.getPadraoPix() );
        contaFinanceira.setPadraoBoleto( entity.getPadraoBoleto() );
        contaFinanceira.setPadraoCartao( entity.getPadraoCartao() );
        contaFinanceira.setPadraoTransferencia( entity.getPadraoTransferencia() );
        contaFinanceira.setId( entity.getId() );
        contaFinanceira.setDataCriacao( entity.getDataCriacao() );
        contaFinanceira.setDataAtualizacao( entity.getDataAtualizacao() );
        contaFinanceira.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(contaFinanceira);

        return contaFinanceira;
    }

    public ContaFinanceiraEntity apiToEntity(ContaFinanceira api) {
        if ( api == null ) {
            return null;
        }
        ContaFinanceiraEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ContaFinanceira found for Id: " + api.getId()));
            }
        } else {
            ContaFinanceiraEntityBuilder contaFinanceiraBuilder = ContaFinanceiraEntity.builderOf();

            contaFinanceiraBuilder.version = api.getVersion();
            contaFinanceiraBuilder.nome = api.getNome();
            contaFinanceiraBuilder.tipo = api.getTipo();
            contaFinanceiraBuilder.ativo = api.getAtivo();
            contaFinanceiraBuilder.codigoBanco = api.getCodigoBanco();
            contaFinanceiraBuilder.agencia = api.getAgencia();
            contaFinanceiraBuilder.dadosDoBanco = api.getDadosDoBanco();
            contaFinanceiraBuilder.saldoInicial = api.getSaldoInicial();
            contaFinanceiraBuilder.dataSaldoInicial = api.getDataSaldoInicial();
            contaFinanceiraBuilder.padraoDinheiro = api.getPadraoDinheiro();
            contaFinanceiraBuilder.padraoPix = api.getPadraoPix();
            contaFinanceiraBuilder.padraoBoleto = api.getPadraoBoleto();
            contaFinanceiraBuilder.padraoCartao = api.getPadraoCartao();
            contaFinanceiraBuilder.padraoTransferencia = api.getPadraoTransferencia();
            contaFinanceiraBuilder.id = api.getId();
            contaFinanceiraBuilder.dataCriacao = api.getDataCriacao();
            contaFinanceiraBuilder.dataAtualizacao = api.getDataAtualizacao();
            contaFinanceiraBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = contaFinanceiraBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<ContaFinanceira> entityListToApiList(List<ContaFinanceiraEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ContaFinanceira> list = new ArrayList<ContaFinanceira>( entity.size() );
        for ( ContaFinanceiraEntity contaFinanceiraEntity : entity ) {
            list.add( entityToApi( contaFinanceiraEntity ) );
        }

        return list;
    }

    public List<ContaFinanceiraEntity> apiListToEntityList(List<ContaFinanceira> api) {
        if ( api == null ) {
            return null;
        }

        List<ContaFinanceiraEntity> list = new ArrayList<ContaFinanceiraEntity>( api.size() );
        for ( ContaFinanceira contaFinanceira : api ) {
            list.add( apiToEntity( contaFinanceira ) );
        }

        return list;
    }

    public ContaFinanceira updateEntityToApi(ContaFinanceiraEntity entity, ContaFinanceira api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setTipo( entity.getTipo() );
        api.setAtivo( entity.getAtivo() );
        api.setCodigoBanco( entity.getCodigoBanco() );
        api.setAgencia( entity.getAgencia() );
        api.setDadosDoBanco( entity.getDadosDoBanco() );
        api.setSaldoInicial( entity.getSaldoInicial() );
        api.setDataSaldoInicial( entity.getDataSaldoInicial() );
        api.setPadraoDinheiro( entity.getPadraoDinheiro() );
        api.setPadraoPix( entity.getPadraoPix() );
        api.setPadraoBoleto( entity.getPadraoBoleto() );
        api.setPadraoCartao( entity.getPadraoCartao() );
        api.setPadraoTransferencia( entity.getPadraoTransferencia() );
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

    public ContaFinanceiraEntity updateApiToEntity(ContaFinanceira api, ContaFinanceiraEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ContaFinanceira found for Id: " + api.getId()));
            }
        } else {
            ContaFinanceiraEntityBuilder contaFinanceiraBuilder = ContaFinanceiraEntity.builderOf();

            contaFinanceiraBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setTipo( api.getTipo() );
            entity.setAtivo( api.getAtivo() );
            entity.setCodigoBanco( api.getCodigoBanco() );
            entity.setAgencia( api.getAgencia() );
            entity.setDadosDoBanco( api.getDadosDoBanco() );
            entity.setSaldoInicial( api.getSaldoInicial() );
            entity.setDataSaldoInicial( api.getDataSaldoInicial() );
            entity.setPadraoDinheiro( api.getPadraoDinheiro() );
            entity.setPadraoPix( api.getPadraoPix() );
            entity.setPadraoBoleto( api.getPadraoBoleto() );
            entity.setPadraoCartao( api.getPadraoCartao() );
            entity.setPadraoTransferencia( api.getPadraoTransferencia() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ContaFinanceiraEntity patchApiToEntity(ContaFinanceira api, ContaFinanceiraEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getCodigoBanco() != null) entity.setCodigoBanco( api.getCodigoBanco() ); 
        if (api.getAgencia() != null) entity.setAgencia( api.getAgencia() ); 
        if (api.getDadosDoBanco() != null) entity.setDadosDoBanco( api.getDadosDoBanco() ); 
        if (api.getSaldoInicial() != null) entity.setSaldoInicial( api.getSaldoInicial() ); 
        if (api.getDataSaldoInicial() != null) entity.setDataSaldoInicial( api.getDataSaldoInicial() ); 
        if (api.getPadraoDinheiro() != null) entity.setPadraoDinheiro( api.getPadraoDinheiro() ); 
        if (api.getPadraoPix() != null) entity.setPadraoPix( api.getPadraoPix() ); 
        if (api.getPadraoBoleto() != null) entity.setPadraoBoleto( api.getPadraoBoleto() ); 
        if (api.getPadraoCartao() != null) entity.setPadraoCartao( api.getPadraoCartao() ); 
        if (api.getPadraoTransferencia() != null) entity.setPadraoTransferencia( api.getPadraoTransferencia() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public ContaFinanceiraEntity setEntityDefaultValues(ContaFinanceiraEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        if (entity.getSaldoInicial() == null) {
            entity.setSaldoInicial(new BigDecimal("0"));
        }
        if (entity.getPadraoDinheiro() == null) {
            entity.setPadraoDinheiro(false);
        }
        if (entity.getPadraoPix() == null) {
            entity.setPadraoPix(false);
        }
        if (entity.getPadraoBoleto() == null) {
            entity.setPadraoBoleto(false);
        }
        if (entity.getPadraoCartao() == null) {
            entity.setPadraoCartao(false);
        }
        if (entity.getPadraoTransferencia() == null) {
            entity.setPadraoTransferencia(false);
        }

        return entity;
    }

    public ContaFinanceira setApiDefaultValues(ContaFinanceira api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }
        if (api.getSaldoInicial() == null) {
            api.setSaldoInicial(new BigDecimal("0"));
        }
        if (api.getPadraoDinheiro() == null) {
            api.setPadraoDinheiro(false);
        }
        if (api.getPadraoPix() == null) {
            api.setPadraoPix(false);
        }
        if (api.getPadraoBoleto() == null) {
            api.setPadraoBoleto(false);
        }
        if (api.getPadraoCartao() == null) {
            api.setPadraoCartao(false);
        }
        if (api.getPadraoTransferencia() == null) {
            api.setPadraoTransferencia(false);
        }

        return api;
    }

}
