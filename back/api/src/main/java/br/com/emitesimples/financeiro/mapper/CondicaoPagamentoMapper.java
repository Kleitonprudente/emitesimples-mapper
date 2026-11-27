package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.CondicaoPagamento;
import br.com.emitesimples.financeiro.entity.CondicaoPagamentoEntity;
import br.com.emitesimples.financeiro.builder.CondicaoPagamentoEntityBuilder;
import br.com.emitesimples.financeiro.repository.CondicaoPagamentoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class CondicaoPagamentoMapper {

    @Inject CondicaoPagamentoRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;

    public CondicaoPagamento entityToApi(CondicaoPagamentoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        CondicaoPagamento condicaoPagamento = new CondicaoPagamento();

        condicaoPagamento.setVersion( entity.getVersion() );
        condicaoPagamento.setNome( entity.getNome() );
        condicaoPagamento.setForma( entity.getForma() );
        condicaoPagamento.setAtivo( entity.getAtivo() );
        condicaoPagamento.setParcelas( entity.getParcelas() );
        condicaoPagamento.setDiasPrimeiroVencimento( entity.getDiasPrimeiroVencimento() );
        condicaoPagamento.setIntervaloDias( entity.getIntervaloDias() );
        condicaoPagamento.setReceberNaEmissao( entity.getReceberNaEmissao() );
        condicaoPagamento.setId( entity.getId() );
        condicaoPagamento.setDataCriacao( entity.getDataCriacao() );
        condicaoPagamento.setDataAtualizacao( entity.getDataAtualizacao() );
        condicaoPagamento.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(condicaoPagamento);

        return condicaoPagamento;
    }

    public CondicaoPagamentoEntity apiToEntity(CondicaoPagamento api) {
        if ( api == null ) {
            return null;
        }
        CondicaoPagamentoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CondicaoPagamento found for Id: " + api.getId()));
            }
        } else {
            CondicaoPagamentoEntityBuilder condicaoPagamentoBuilder = CondicaoPagamentoEntity.builderOf();

            condicaoPagamentoBuilder.version = api.getVersion();
            condicaoPagamentoBuilder.nome = api.getNome();
            condicaoPagamentoBuilder.forma = api.getForma();
            condicaoPagamentoBuilder.ativo = api.getAtivo();
            condicaoPagamentoBuilder.parcelas = api.getParcelas();
            condicaoPagamentoBuilder.diasPrimeiroVencimento = api.getDiasPrimeiroVencimento();
            condicaoPagamentoBuilder.intervaloDias = api.getIntervaloDias();
            condicaoPagamentoBuilder.receberNaEmissao = api.getReceberNaEmissao();
            condicaoPagamentoBuilder.id = api.getId();
            condicaoPagamentoBuilder.dataCriacao = api.getDataCriacao();
            condicaoPagamentoBuilder.dataAtualizacao = api.getDataAtualizacao();
            condicaoPagamentoBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = condicaoPagamentoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<CondicaoPagamento> entityListToApiList(List<CondicaoPagamentoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<CondicaoPagamento> list = new ArrayList<CondicaoPagamento>( entity.size() );
        for ( CondicaoPagamentoEntity condicaoPagamentoEntity : entity ) {
            list.add( entityToApi( condicaoPagamentoEntity ) );
        }

        return list;
    }

    public List<CondicaoPagamentoEntity> apiListToEntityList(List<CondicaoPagamento> api) {
        if ( api == null ) {
            return null;
        }

        List<CondicaoPagamentoEntity> list = new ArrayList<CondicaoPagamentoEntity>( api.size() );
        for ( CondicaoPagamento condicaoPagamento : api ) {
            list.add( apiToEntity( condicaoPagamento ) );
        }

        return list;
    }

    public CondicaoPagamento updateEntityToApi(CondicaoPagamentoEntity entity, CondicaoPagamento api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setForma( entity.getForma() );
        api.setAtivo( entity.getAtivo() );
        api.setParcelas( entity.getParcelas() );
        api.setDiasPrimeiroVencimento( entity.getDiasPrimeiroVencimento() );
        api.setIntervaloDias( entity.getIntervaloDias() );
        api.setReceberNaEmissao( entity.getReceberNaEmissao() );
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

    public CondicaoPagamentoEntity updateApiToEntity(CondicaoPagamento api, CondicaoPagamentoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity CondicaoPagamento found for Id: " + api.getId()));
            }
        } else {
            CondicaoPagamentoEntityBuilder condicaoPagamentoBuilder = CondicaoPagamentoEntity.builderOf();

            condicaoPagamentoBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setForma( api.getForma() );
            entity.setAtivo( api.getAtivo() );
            entity.setParcelas( api.getParcelas() );
            entity.setDiasPrimeiroVencimento( api.getDiasPrimeiroVencimento() );
            entity.setIntervaloDias( api.getIntervaloDias() );
            entity.setReceberNaEmissao( api.getReceberNaEmissao() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public CondicaoPagamentoEntity patchApiToEntity(CondicaoPagamento api, CondicaoPagamentoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getForma() != null) entity.setForma( api.getForma() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getParcelas() != null) entity.setParcelas( api.getParcelas() ); 
        if (api.getDiasPrimeiroVencimento() != null) entity.setDiasPrimeiroVencimento( api.getDiasPrimeiroVencimento() ); 
        if (api.getIntervaloDias() != null) entity.setIntervaloDias( api.getIntervaloDias() ); 
        if (api.getReceberNaEmissao() != null) entity.setReceberNaEmissao( api.getReceberNaEmissao() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public CondicaoPagamentoEntity setEntityDefaultValues(CondicaoPagamentoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        if (entity.getParcelas() == null) {
            entity.setParcelas(1);
        }
        if (entity.getDiasPrimeiroVencimento() == null) {
            entity.setDiasPrimeiroVencimento(0);
        }
        if (entity.getIntervaloDias() == null) {
            entity.setIntervaloDias(30);
        }
        if (entity.getReceberNaEmissao() == null) {
            entity.setReceberNaEmissao(false);
        }

        return entity;
    }

    public CondicaoPagamento setApiDefaultValues(CondicaoPagamento api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }
        if (api.getParcelas() == null) {
            api.setParcelas(1);
        }
        if (api.getDiasPrimeiroVencimento() == null) {
            api.setDiasPrimeiroVencimento(0);
        }
        if (api.getIntervaloDias() == null) {
            api.setIntervaloDias(30);
        }
        if (api.getReceberNaEmissao() == null) {
            api.setReceberNaEmissao(false);
        }

        return api;
    }

}
