package br.com.emitesimples.cadastros.mapper;

import br.com.emitesimples.cadastros.domain.Servico;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.cadastros.builder.ServicoEntityBuilder;
import br.com.emitesimples.cadastros.repository.ServicoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class ServicoMapper {

    @Inject ServicoRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;

    public Servico entityToApi(ServicoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Servico servico = new Servico();

        servico.setVersion( entity.getVersion() );
        servico.setNome( entity.getNome() );
        servico.setCodigoInterno( entity.getCodigoInterno() );
        servico.setAtivo( entity.getAtivo() );
        servico.setCodigoServico( entity.getCodigoServico() );
        servico.setCodigoTributacaoMunicipio( entity.getCodigoTributacaoMunicipio() );
        servico.setCodigoNbs( entity.getCodigoNbs() );
        servico.setDescricaoCurta( entity.getDescricaoCurta() );
        servico.setDiscriminacaoPadrao( entity.getDiscriminacaoPadrao() );
        servico.setValorPadrao( entity.getValorPadrao() );
        servico.setAliquotaIss( entity.getAliquotaIss() );
        servico.setIssRetido( entity.getIssRetido() );
        servico.setTipoOperacaoIss( entity.getTipoOperacaoIss() );
        servico.setCstPisCofins( entity.getCstPisCofins() );
        servico.setPercentualTributosSimples( entity.getPercentualTributosSimples() );
        servico.setId( entity.getId() );
        servico.setDataCriacao( entity.getDataCriacao() );
        servico.setDataAtualizacao( entity.getDataAtualizacao() );
        servico.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(servico);

        return servico;
    }

    public ServicoEntity apiToEntity(Servico api) {
        if ( api == null ) {
            return null;
        }
        ServicoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Servico found for Id: " + api.getId()));
            }
        } else {
            ServicoEntityBuilder servicoBuilder = ServicoEntity.builderOf();

            servicoBuilder.version = api.getVersion();
            servicoBuilder.nome = api.getNome();
            servicoBuilder.codigoInterno = api.getCodigoInterno();
            servicoBuilder.ativo = api.getAtivo();
            servicoBuilder.codigoServico = api.getCodigoServico();
            servicoBuilder.codigoTributacaoMunicipio = api.getCodigoTributacaoMunicipio();
            servicoBuilder.codigoNbs = api.getCodigoNbs();
            servicoBuilder.descricaoCurta = api.getDescricaoCurta();
            servicoBuilder.discriminacaoPadrao = api.getDiscriminacaoPadrao();
            servicoBuilder.valorPadrao = api.getValorPadrao();
            servicoBuilder.aliquotaIss = api.getAliquotaIss();
            servicoBuilder.issRetido = api.getIssRetido();
            servicoBuilder.tipoOperacaoIss = api.getTipoOperacaoIss();
            servicoBuilder.cstPisCofins = api.getCstPisCofins();
            servicoBuilder.percentualTributosSimples = api.getPercentualTributosSimples();
            servicoBuilder.id = api.getId();
            servicoBuilder.dataCriacao = api.getDataCriacao();
            servicoBuilder.dataAtualizacao = api.getDataAtualizacao();
            servicoBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = servicoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Servico> entityListToApiList(List<ServicoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Servico> list = new ArrayList<Servico>( entity.size() );
        for ( ServicoEntity servicoEntity : entity ) {
            list.add( entityToApi( servicoEntity ) );
        }

        return list;
    }

    public List<ServicoEntity> apiListToEntityList(List<Servico> api) {
        if ( api == null ) {
            return null;
        }

        List<ServicoEntity> list = new ArrayList<ServicoEntity>( api.size() );
        for ( Servico servico : api ) {
            list.add( apiToEntity( servico ) );
        }

        return list;
    }

    public Servico updateEntityToApi(ServicoEntity entity, Servico api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNome( entity.getNome() );
        api.setCodigoInterno( entity.getCodigoInterno() );
        api.setAtivo( entity.getAtivo() );
        api.setCodigoServico( entity.getCodigoServico() );
        api.setCodigoTributacaoMunicipio( entity.getCodigoTributacaoMunicipio() );
        api.setCodigoNbs( entity.getCodigoNbs() );
        api.setDescricaoCurta( entity.getDescricaoCurta() );
        api.setDiscriminacaoPadrao( entity.getDiscriminacaoPadrao() );
        api.setValorPadrao( entity.getValorPadrao() );
        api.setAliquotaIss( entity.getAliquotaIss() );
        api.setIssRetido( entity.getIssRetido() );
        api.setTipoOperacaoIss( entity.getTipoOperacaoIss() );
        api.setCstPisCofins( entity.getCstPisCofins() );
        api.setPercentualTributosSimples( entity.getPercentualTributosSimples() );
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

    public ServicoEntity updateApiToEntity(Servico api, ServicoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Servico found for Id: " + api.getId()));
            }
        } else {
            ServicoEntityBuilder servicoBuilder = ServicoEntity.builderOf();

            servicoBuilder.version = api.getVersion();
            entity.setNome( api.getNome() );
            entity.setCodigoInterno( api.getCodigoInterno() );
            entity.setAtivo( api.getAtivo() );
            entity.setCodigoServico( api.getCodigoServico() );
            entity.setCodigoTributacaoMunicipio( api.getCodigoTributacaoMunicipio() );
            entity.setCodigoNbs( api.getCodigoNbs() );
            entity.setDescricaoCurta( api.getDescricaoCurta() );
            entity.setDiscriminacaoPadrao( api.getDiscriminacaoPadrao() );
            entity.setValorPadrao( api.getValorPadrao() );
            entity.setAliquotaIss( api.getAliquotaIss() );
            entity.setIssRetido( api.getIssRetido() );
            entity.setTipoOperacaoIss( api.getTipoOperacaoIss() );
            entity.setCstPisCofins( api.getCstPisCofins() );
            entity.setPercentualTributosSimples( api.getPercentualTributosSimples() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ServicoEntity patchApiToEntity(Servico api, ServicoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getCodigoInterno() != null) entity.setCodigoInterno( api.getCodigoInterno() ); 
        if (api.getAtivo() != null) entity.setAtivo( api.getAtivo() ); 
        if (api.getCodigoServico() != null) entity.setCodigoServico( api.getCodigoServico() ); 
        if (api.getCodigoTributacaoMunicipio() != null) entity.setCodigoTributacaoMunicipio( api.getCodigoTributacaoMunicipio() ); 
        if (api.getCodigoNbs() != null) entity.setCodigoNbs( api.getCodigoNbs() ); 
        if (api.getDescricaoCurta() != null) entity.setDescricaoCurta( api.getDescricaoCurta() ); 
        if (api.getDiscriminacaoPadrao() != null) entity.setDiscriminacaoPadrao( api.getDiscriminacaoPadrao() ); 
        if (api.getValorPadrao() != null) entity.setValorPadrao( api.getValorPadrao() ); 
        if (api.getAliquotaIss() != null) entity.setAliquotaIss( api.getAliquotaIss() ); 
        if (api.getIssRetido() != null) entity.setIssRetido( api.getIssRetido() ); 
        if (api.getTipoOperacaoIss() != null) entity.setTipoOperacaoIss( api.getTipoOperacaoIss() ); 
        if (api.getCstPisCofins() != null) entity.setCstPisCofins( api.getCstPisCofins() ); 
        if (api.getPercentualTributosSimples() != null) entity.setPercentualTributosSimples( api.getPercentualTributosSimples() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public ServicoEntity setEntityDefaultValues(ServicoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAtivo() == null) {
            entity.setAtivo(true);
        }
        if (entity.getIssRetido() == null) {
            entity.setIssRetido(false);
        }
        if (entity.getTipoOperacaoIss() == null) {
            entity.setTipoOperacaoIss(1);
        }

        return entity;
    }

    public Servico setApiDefaultValues(Servico api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAtivo() == null) {
            api.setAtivo(true);
        }
        if (api.getIssRetido() == null) {
            api.setIssRetido(false);
        }
        if (api.getTipoOperacaoIss() == null) {
            api.setTipoOperacaoIss(1);
        }

        return api;
    }

}
