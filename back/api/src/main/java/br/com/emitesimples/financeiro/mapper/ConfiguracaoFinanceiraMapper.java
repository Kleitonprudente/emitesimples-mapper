package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.ConfiguracaoFinanceira;
import br.com.emitesimples.financeiro.entity.ConfiguracaoFinanceiraEntity;
import br.com.emitesimples.financeiro.builder.ConfiguracaoFinanceiraEntityBuilder;
import br.com.emitesimples.financeiro.repository.ConfiguracaoFinanceiraRepository;

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
public class ConfiguracaoFinanceiraMapper {

    @Inject ConfiguracaoFinanceiraRepository repository;
    @Inject 
    CategoriaFinanceiraMapper categoriaFinanceiraMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public ConfiguracaoFinanceira entityToApi(ConfiguracaoFinanceiraEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ConfiguracaoFinanceira configuracaoFinanceira = new ConfiguracaoFinanceira();

        configuracaoFinanceira.setVersion( entity.getVersion() );
        configuracaoFinanceira.setMultaPercentual( entity.getMultaPercentual() );
        configuracaoFinanceira.setJurosMensalPercentual( entity.getJurosMensalPercentual() );
        configuracaoFinanceira.setCarenciaDias( entity.getCarenciaDias() );
        configuracaoFinanceira.setLembretesAtivos( entity.getLembretesAtivos() );
        configuracaoFinanceira.setLembreteDiasAntes( entity.getLembreteDiasAntes() );
        configuracaoFinanceira.setLembreteDiasDepois( entity.getLembreteDiasDepois() );
        configuracaoFinanceira.setVencimentoPadraoDias( entity.getVencimentoPadraoDias() );
        configuracaoFinanceira.setObservacaoPadraoReceber( entity.getObservacaoPadraoReceber() );
        configuracaoFinanceira.setFormaPrevistaPadraoReceber( entity.getFormaPrevistaPadraoReceber() );
        configuracaoFinanceira.setPadroesCriadosEm( entity.getPadroesCriadosEm() );
        configuracaoFinanceira.setId( entity.getId() );
        configuracaoFinanceira.setDataCriacao( entity.getDataCriacao() );
        configuracaoFinanceira.setDataAtualizacao( entity.getDataAtualizacao() );
        configuracaoFinanceira.setCategoriaPadraoReceita(categoriaFinanceiraMapper.entityToApi(entity.getCategoriaPadraoReceita() ));
        configuracaoFinanceira.setCategoriaPadraoDespesa(categoriaFinanceiraMapper.entityToApi(entity.getCategoriaPadraoDespesa() ));
        configuracaoFinanceira.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(configuracaoFinanceira);

        return configuracaoFinanceira;
    }

    public ConfiguracaoFinanceiraEntity apiToEntity(ConfiguracaoFinanceira api) {
        if ( api == null ) {
            return null;
        }
        ConfiguracaoFinanceiraEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ConfiguracaoFinanceira found for Id: " + api.getId()));
            }
        } else {
            ConfiguracaoFinanceiraEntityBuilder configuracaoFinanceiraBuilder = ConfiguracaoFinanceiraEntity.builderOf();

            configuracaoFinanceiraBuilder.version = api.getVersion();
            configuracaoFinanceiraBuilder.multaPercentual = api.getMultaPercentual();
            configuracaoFinanceiraBuilder.jurosMensalPercentual = api.getJurosMensalPercentual();
            configuracaoFinanceiraBuilder.carenciaDias = api.getCarenciaDias();
            configuracaoFinanceiraBuilder.lembretesAtivos = api.getLembretesAtivos();
            configuracaoFinanceiraBuilder.lembreteDiasAntes = api.getLembreteDiasAntes();
            configuracaoFinanceiraBuilder.lembreteDiasDepois = api.getLembreteDiasDepois();
            configuracaoFinanceiraBuilder.vencimentoPadraoDias = api.getVencimentoPadraoDias();
            configuracaoFinanceiraBuilder.observacaoPadraoReceber = api.getObservacaoPadraoReceber();
            configuracaoFinanceiraBuilder.formaPrevistaPadraoReceber = api.getFormaPrevistaPadraoReceber();
            configuracaoFinanceiraBuilder.padroesCriadosEm = api.getPadroesCriadosEm();
            configuracaoFinanceiraBuilder.id = api.getId();
            configuracaoFinanceiraBuilder.dataCriacao = api.getDataCriacao();
            configuracaoFinanceiraBuilder.dataAtualizacao = api.getDataAtualizacao();
            configuracaoFinanceiraBuilder.categoriaPadraoReceita = categoriaFinanceiraMapper.apiToEntity(api.getCategoriaPadraoReceita());
            configuracaoFinanceiraBuilder.categoriaPadraoDespesa = categoriaFinanceiraMapper.apiToEntity(api.getCategoriaPadraoDespesa());
            configuracaoFinanceiraBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = configuracaoFinanceiraBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<ConfiguracaoFinanceira> entityListToApiList(List<ConfiguracaoFinanceiraEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ConfiguracaoFinanceira> list = new ArrayList<ConfiguracaoFinanceira>( entity.size() );
        for ( ConfiguracaoFinanceiraEntity configuracaoFinanceiraEntity : entity ) {
            list.add( entityToApi( configuracaoFinanceiraEntity ) );
        }

        return list;
    }

    public List<ConfiguracaoFinanceiraEntity> apiListToEntityList(List<ConfiguracaoFinanceira> api) {
        if ( api == null ) {
            return null;
        }

        List<ConfiguracaoFinanceiraEntity> list = new ArrayList<ConfiguracaoFinanceiraEntity>( api.size() );
        for ( ConfiguracaoFinanceira configuracaoFinanceira : api ) {
            list.add( apiToEntity( configuracaoFinanceira ) );
        }

        return list;
    }

    public ConfiguracaoFinanceira updateEntityToApi(ConfiguracaoFinanceiraEntity entity, ConfiguracaoFinanceira api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setMultaPercentual( entity.getMultaPercentual() );
        api.setJurosMensalPercentual( entity.getJurosMensalPercentual() );
        api.setCarenciaDias( entity.getCarenciaDias() );
        api.setLembretesAtivos( entity.getLembretesAtivos() );
        api.setLembreteDiasAntes( entity.getLembreteDiasAntes() );
        api.setLembreteDiasDepois( entity.getLembreteDiasDepois() );
        api.setVencimentoPadraoDias( entity.getVencimentoPadraoDias() );
        api.setObservacaoPadraoReceber( entity.getObservacaoPadraoReceber() );
        api.setFormaPrevistaPadraoReceber( entity.getFormaPrevistaPadraoReceber() );
        api.setPadroesCriadosEm( entity.getPadroesCriadosEm() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getCategoriaPadraoReceita() != null ) {
            if ( api.getCategoriaPadraoReceita() == null ) {
                api.setCategoriaPadraoReceita( new CategoriaFinanceira() );
            }
            categoriaFinanceiraMapper.updateEntityToApi(entity.getCategoriaPadraoReceita(), api.getCategoriaPadraoReceita());
        } else {
            api.setCategoriaPadraoReceita( null );
        }

        if ( entity.getCategoriaPadraoDespesa() != null ) {
            if ( api.getCategoriaPadraoDespesa() == null ) {
                api.setCategoriaPadraoDespesa( new CategoriaFinanceira() );
            }
            categoriaFinanceiraMapper.updateEntityToApi(entity.getCategoriaPadraoDespesa(), api.getCategoriaPadraoDespesa());
        } else {
            api.setCategoriaPadraoDespesa( null );
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

    public ConfiguracaoFinanceiraEntity updateApiToEntity(ConfiguracaoFinanceira api, ConfiguracaoFinanceiraEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ConfiguracaoFinanceira found for Id: " + api.getId()));
            }
        } else {
            ConfiguracaoFinanceiraEntityBuilder configuracaoFinanceiraBuilder = ConfiguracaoFinanceiraEntity.builderOf();

            configuracaoFinanceiraBuilder.version = api.getVersion();
            entity.setMultaPercentual( api.getMultaPercentual() );
            entity.setJurosMensalPercentual( api.getJurosMensalPercentual() );
            entity.setCarenciaDias( api.getCarenciaDias() );
            entity.setLembretesAtivos( api.getLembretesAtivos() );
            entity.setLembreteDiasAntes( api.getLembreteDiasAntes() );
            entity.setLembreteDiasDepois( api.getLembreteDiasDepois() );
            entity.setVencimentoPadraoDias( api.getVencimentoPadraoDias() );
            entity.setObservacaoPadraoReceber( api.getObservacaoPadraoReceber() );
            entity.setFormaPrevistaPadraoReceber( api.getFormaPrevistaPadraoReceber() );
            entity.setPadroesCriadosEm( api.getPadroesCriadosEm() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getCategoriaPadraoReceita() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setCategoriaPadraoReceita(categoriaFinanceiraMapper.updateApiToEntity( api.getCategoriaPadraoReceita(), entity.getCategoriaPadraoReceita() != null ? entity.getCategoriaPadraoReceita() : new CategoriaFinanceiraEntity()));
            } else {
                entity.setCategoriaPadraoReceita( null );
            }

            if ( api.getCategoriaPadraoDespesa() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setCategoriaPadraoDespesa(categoriaFinanceiraMapper.updateApiToEntity( api.getCategoriaPadraoDespesa(), entity.getCategoriaPadraoDespesa() != null ? entity.getCategoriaPadraoDespesa() : new CategoriaFinanceiraEntity()));
            } else {
                entity.setCategoriaPadraoDespesa( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ConfiguracaoFinanceiraEntity patchApiToEntity(ConfiguracaoFinanceira api, ConfiguracaoFinanceiraEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getMultaPercentual() != null) entity.setMultaPercentual( api.getMultaPercentual() ); 
        if (api.getJurosMensalPercentual() != null) entity.setJurosMensalPercentual( api.getJurosMensalPercentual() ); 
        if (api.getCarenciaDias() != null) entity.setCarenciaDias( api.getCarenciaDias() ); 
        if (api.getLembretesAtivos() != null) entity.setLembretesAtivos( api.getLembretesAtivos() ); 
        if (api.getLembreteDiasAntes() != null) entity.setLembreteDiasAntes( api.getLembreteDiasAntes() ); 
        if (api.getLembreteDiasDepois() != null) entity.setLembreteDiasDepois( api.getLembreteDiasDepois() ); 
        if (api.getVencimentoPadraoDias() != null) entity.setVencimentoPadraoDias( api.getVencimentoPadraoDias() ); 
        if (api.getObservacaoPadraoReceber() != null) entity.setObservacaoPadraoReceber( api.getObservacaoPadraoReceber() ); 
        if (api.getFormaPrevistaPadraoReceber() != null) entity.setFormaPrevistaPadraoReceber( api.getFormaPrevistaPadraoReceber() ); 
        if (api.getPadroesCriadosEm() != null) entity.setPadroesCriadosEm( api.getPadroesCriadosEm() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getCategoriaPadraoReceita() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setCategoriaPadraoReceita(categoriaFinanceiraMapper.updateApiToEntity( api.getCategoriaPadraoReceita(), entity.getCategoriaPadraoReceita() != null ? entity.getCategoriaPadraoReceita() : new CategoriaFinanceiraEntity()));
        }
        if ( api.getCategoriaPadraoDespesa() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setCategoriaPadraoDespesa(categoriaFinanceiraMapper.updateApiToEntity( api.getCategoriaPadraoDespesa(), entity.getCategoriaPadraoDespesa() != null ? entity.getCategoriaPadraoDespesa() : new CategoriaFinanceiraEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public ConfiguracaoFinanceiraEntity setEntityDefaultValues(ConfiguracaoFinanceiraEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getMultaPercentual() == null) {
            entity.setMultaPercentual(new BigDecimal("2.00"));
        }
        if (entity.getJurosMensalPercentual() == null) {
            entity.setJurosMensalPercentual(new BigDecimal("1.00"));
        }
        if (entity.getCarenciaDias() == null) {
            entity.setCarenciaDias(2);
        }
        if (entity.getLembretesAtivos() == null) {
            entity.setLembretesAtivos(false);
        }
        if (entity.getLembreteDiasAntes() == null) {
            entity.setLembreteDiasAntes(3);
        }
        if (entity.getLembreteDiasDepois() == null) {
            entity.setLembreteDiasDepois("1,7,15");
        }
        if (entity.getVencimentoPadraoDias() == null) {
            entity.setVencimentoPadraoDias(0);
        }

        return entity;
    }

    public ConfiguracaoFinanceira setApiDefaultValues(ConfiguracaoFinanceira api) {
        if ( api == null ) {
            return null;
        }

        if (api.getMultaPercentual() == null) {
            api.setMultaPercentual(new BigDecimal("2.00"));
        }
        if (api.getJurosMensalPercentual() == null) {
            api.setJurosMensalPercentual(new BigDecimal("1.00"));
        }
        if (api.getCarenciaDias() == null) {
            api.setCarenciaDias(2);
        }
        if (api.getLembretesAtivos() == null) {
            api.setLembretesAtivos(false);
        }
        if (api.getLembreteDiasAntes() == null) {
            api.setLembreteDiasAntes(3);
        }
        if (api.getLembreteDiasDepois() == null) {
            api.setLembreteDiasDepois("1,7,15");
        }
        if (api.getVencimentoPadraoDias() == null) {
            api.setVencimentoPadraoDias(0);
        }

        return api;
    }

}
