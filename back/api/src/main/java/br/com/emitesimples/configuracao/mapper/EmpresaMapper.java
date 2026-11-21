package br.com.emitesimples.configuracao.mapper;

import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.configuracao.builder.EmpresaEntityBuilder;
import br.com.emitesimples.configuracao.repository.EmpresaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.AtividadeEmpresa;
import br.com.emitesimples.configuracao.entity.AtividadeEmpresaEntity;
import br.com.emitesimples.configuracao.domain.QsaEmpresa;
import br.com.emitesimples.configuracao.entity.QsaEmpresaEntity;
import br.com.emitesimples.cadastros.domain.Municipio;
import br.com.emitesimples.cadastros.mapper.MunicipioMapper;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;

@ApplicationScoped
public class EmpresaMapper {

    @Inject EmpresaRepository repository;
    @Inject 
    AtividadeEmpresaMapper atividadeEmpresaMapper;
    @Inject 
    QsaEmpresaMapper qsaEmpresaMapper;
    @Inject 
    MunicipioMapper municipioMapper;

    public Empresa entityToApi(EmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Empresa empresa = new Empresa();

        empresa.setVersion( entity.getVersion() );
        empresa.setNomeFantasia( entity.getNomeFantasia() );
        empresa.setRazaoSocial( entity.getRazaoSocial() );
        empresa.setDocumento( entity.getDocumento() );
        empresa.setRgInscricaoEstadual( entity.getRgInscricaoEstadual() );
        empresa.setInscricaoMunicipal( entity.getInscricaoMunicipal() );
        empresa.setEmail( entity.getEmail() );
        empresa.setWhatsapp( entity.getWhatsapp() );
        empresa.setTelefone( entity.getTelefone() );
        empresa.setLogradouro( entity.getLogradouro() );
        empresa.setNumeroEndereco( entity.getNumeroEndereco() );
        empresa.setComplemento( entity.getComplemento() );
        empresa.setBairro( entity.getBairro() );
        empresa.setCep( entity.getCep() );
        empresa.setRegimeTributario( entity.getRegimeTributario() );
        empresa.setRegimeIbsCbs( entity.getRegimeIbsCbs() );
        empresa.setLogo( entity.getLogo() );
        empresa.setUrlWebhook( entity.getUrlWebhook() );
        empresa.setSituacao( entity.getSituacao() );
        empresa.setDataConfirmacao( entity.getDataConfirmacao() );
        empresa.setReferenciaCrm( entity.getReferenciaCrm() );
        empresa.setStatusCadastro( entity.getStatusCadastro() );
        empresa.setDataCadastroGateway( entity.getDataCadastroGateway() );
        empresa.setValidadeCertificado( entity.getValidadeCertificado() );
        empresa.setSituacaoCertificado( entity.getSituacaoCertificado() );
        empresa.setId( entity.getId() );
        empresa.setAmbiente( entity.getAmbiente() );
        empresa.setDataCriacao( entity.getDataCriacao() );
        empresa.setDataAtualizacao( entity.getDataAtualizacao() );
        empresa.setMunicipio(municipioMapper.entityToApi(entity.getMunicipio() ));

        this.setApiDefaultValues(empresa);

        return empresa;
    }

    public EmpresaEntity apiToEntity(Empresa api) {
        if ( api == null ) {
            return null;
        }
        EmpresaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Empresa found for Id: " + api.getId()));
            }
        } else {
            EmpresaEntityBuilder empresaBuilder = EmpresaEntity.builderOf();

            empresaBuilder.version = api.getVersion();
            empresaBuilder.nomeFantasia = api.getNomeFantasia();
            empresaBuilder.razaoSocial = api.getRazaoSocial();
            empresaBuilder.documento = api.getDocumento();
            empresaBuilder.rgInscricaoEstadual = api.getRgInscricaoEstadual();
            empresaBuilder.inscricaoMunicipal = api.getInscricaoMunicipal();
            empresaBuilder.email = api.getEmail();
            empresaBuilder.whatsapp = api.getWhatsapp();
            empresaBuilder.telefone = api.getTelefone();
            empresaBuilder.logradouro = api.getLogradouro();
            empresaBuilder.numeroEndereco = api.getNumeroEndereco();
            empresaBuilder.complemento = api.getComplemento();
            empresaBuilder.bairro = api.getBairro();
            empresaBuilder.cep = api.getCep();
            empresaBuilder.regimeTributario = api.getRegimeTributario();
            empresaBuilder.regimeIbsCbs = api.getRegimeIbsCbs();
            empresaBuilder.logo = api.getLogo();
            empresaBuilder.urlWebhook = api.getUrlWebhook();
            empresaBuilder.situacao = api.getSituacao();
            empresaBuilder.dataConfirmacao = api.getDataConfirmacao();
            empresaBuilder.referenciaCrm = api.getReferenciaCrm();
            empresaBuilder.statusCadastro = api.getStatusCadastro();
            empresaBuilder.dataCadastroGateway = api.getDataCadastroGateway();
            empresaBuilder.validadeCertificado = api.getValidadeCertificado();
            empresaBuilder.situacaoCertificado = api.getSituacaoCertificado();
            empresaBuilder.id = api.getId();
            empresaBuilder.ambiente = api.getAmbiente();
            empresaBuilder.dataCriacao = api.getDataCriacao();
            empresaBuilder.dataAtualizacao = api.getDataAtualizacao();
            empresaBuilder.municipio = municipioMapper.apiToEntity(api.getMunicipio());
            entity = empresaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Empresa> entityListToApiList(List<EmpresaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Empresa> list = new ArrayList<Empresa>( entity.size() );
        for ( EmpresaEntity empresaEntity : entity ) {
            list.add( entityToApi( empresaEntity ) );
        }

        return list;
    }

    public List<EmpresaEntity> apiListToEntityList(List<Empresa> api) {
        if ( api == null ) {
            return null;
        }

        List<EmpresaEntity> list = new ArrayList<EmpresaEntity>( api.size() );
        for ( Empresa empresa : api ) {
            list.add( apiToEntity( empresa ) );
        }

        return list;
    }

    public Empresa updateEntityToApi(EmpresaEntity entity, Empresa api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNomeFantasia( entity.getNomeFantasia() );
        api.setRazaoSocial( entity.getRazaoSocial() );
        api.setDocumento( entity.getDocumento() );
        api.setRgInscricaoEstadual( entity.getRgInscricaoEstadual() );
        api.setInscricaoMunicipal( entity.getInscricaoMunicipal() );
        api.setEmail( entity.getEmail() );
        api.setWhatsapp( entity.getWhatsapp() );
        api.setTelefone( entity.getTelefone() );
        api.setLogradouro( entity.getLogradouro() );
        api.setNumeroEndereco( entity.getNumeroEndereco() );
        api.setComplemento( entity.getComplemento() );
        api.setBairro( entity.getBairro() );
        api.setCep( entity.getCep() );
        api.setRegimeTributario( entity.getRegimeTributario() );
        api.setRegimeIbsCbs( entity.getRegimeIbsCbs() );
        api.setLogo( entity.getLogo() );
        api.setUrlWebhook( entity.getUrlWebhook() );
        api.setSituacao( entity.getSituacao() );
        api.setDataConfirmacao( entity.getDataConfirmacao() );
        api.setReferenciaCrm( entity.getReferenciaCrm() );
        api.setStatusCadastro( entity.getStatusCadastro() );
        api.setDataCadastroGateway( entity.getDataCadastroGateway() );
        api.setValidadeCertificado( entity.getValidadeCertificado() );
        api.setSituacaoCertificado( entity.getSituacaoCertificado() );
        api.setId( entity.getId() );
        api.setAmbiente( entity.getAmbiente() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        if ( entity.getMunicipio() != null ) {
            if ( api.getMunicipio() == null ) {
                api.setMunicipio( new Municipio() );
            }
            municipioMapper.updateEntityToApi(entity.getMunicipio(), api.getMunicipio());
        } else {
            api.setMunicipio( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public EmpresaEntity updateApiToEntity(Empresa api, EmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Empresa found for Id: " + api.getId()));
            }
        } else {
            EmpresaEntityBuilder empresaBuilder = EmpresaEntity.builderOf();

            empresaBuilder.version = api.getVersion();
            entity.setNomeFantasia( api.getNomeFantasia() );
            entity.setRazaoSocial( api.getRazaoSocial() );
            entity.setDocumento( api.getDocumento() );
            entity.setRgInscricaoEstadual( api.getRgInscricaoEstadual() );
            entity.setInscricaoMunicipal( api.getInscricaoMunicipal() );
            entity.setEmail( api.getEmail() );
            entity.setWhatsapp( api.getWhatsapp() );
            entity.setTelefone( api.getTelefone() );
            entity.setLogradouro( api.getLogradouro() );
            entity.setNumeroEndereco( api.getNumeroEndereco() );
            entity.setComplemento( api.getComplemento() );
            entity.setBairro( api.getBairro() );
            entity.setCep( api.getCep() );
            entity.setRegimeTributario( api.getRegimeTributario() );
            entity.setRegimeIbsCbs( api.getRegimeIbsCbs() );
            entity.setLogo( api.getLogo() );
            entity.setUrlWebhook( api.getUrlWebhook() );
            entity.setSituacao( api.getSituacao() );
            entity.setDataConfirmacao( api.getDataConfirmacao() );
            entity.setReferenciaCrm( api.getReferenciaCrm() );
            entity.setStatusCadastro( api.getStatusCadastro() );
            entity.setDataCadastroGateway( api.getDataCadastroGateway() );
            entity.setValidadeCertificado( api.getValidadeCertificado() );
            entity.setSituacaoCertificado( api.getSituacaoCertificado() );
            entity.setId( api.getId() );
            entity.setAmbiente( api.getAmbiente() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getMunicipio() != null ) {
                if ( entity.getMunicipio() == null ) {
                    entity.setMunicipio( new MunicipioEntity() );
                }
                entity.setMunicipio(municipioMapper.updateApiToEntity( api.getMunicipio(), entity.getMunicipio()));
            } else {
                entity.setMunicipio( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public EmpresaEntity patchApiToEntity(Empresa api, EmpresaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNomeFantasia() != null) entity.setNomeFantasia( api.getNomeFantasia() ); 
        if (api.getRazaoSocial() != null) entity.setRazaoSocial( api.getRazaoSocial() ); 
        if (api.getDocumento() != null) entity.setDocumento( api.getDocumento() ); 
        if (api.getRgInscricaoEstadual() != null) entity.setRgInscricaoEstadual( api.getRgInscricaoEstadual() ); 
        if (api.getInscricaoMunicipal() != null) entity.setInscricaoMunicipal( api.getInscricaoMunicipal() ); 
        if (api.getEmail() != null) entity.setEmail( api.getEmail() ); 
        if (api.getWhatsapp() != null) entity.setWhatsapp( api.getWhatsapp() ); 
        if (api.getTelefone() != null) entity.setTelefone( api.getTelefone() ); 
        if (api.getLogradouro() != null) entity.setLogradouro( api.getLogradouro() ); 
        if (api.getNumeroEndereco() != null) entity.setNumeroEndereco( api.getNumeroEndereco() ); 
        if (api.getComplemento() != null) entity.setComplemento( api.getComplemento() ); 
        if (api.getBairro() != null) entity.setBairro( api.getBairro() ); 
        if (api.getCep() != null) entity.setCep( api.getCep() ); 
        if (api.getRegimeTributario() != null) entity.setRegimeTributario( api.getRegimeTributario() ); 
        if (api.getRegimeIbsCbs() != null) entity.setRegimeIbsCbs( api.getRegimeIbsCbs() ); 
        if (api.getLogo() != null) entity.setLogo( api.getLogo() ); 
        if (api.getUrlWebhook() != null) entity.setUrlWebhook( api.getUrlWebhook() ); 
        if (api.getSituacao() != null) entity.setSituacao( api.getSituacao() ); 
        if (api.getDataConfirmacao() != null) entity.setDataConfirmacao( api.getDataConfirmacao() ); 
        if (api.getReferenciaCrm() != null) entity.setReferenciaCrm( api.getReferenciaCrm() ); 
        if (api.getStatusCadastro() != null) entity.setStatusCadastro( api.getStatusCadastro() ); 
        if (api.getDataCadastroGateway() != null) entity.setDataCadastroGateway( api.getDataCadastroGateway() ); 
        if (api.getValidadeCertificado() != null) entity.setValidadeCertificado( api.getValidadeCertificado() ); 
        if (api.getSituacaoCertificado() != null) entity.setSituacaoCertificado( api.getSituacaoCertificado() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getAmbiente() != null) entity.setAmbiente( api.getAmbiente() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getMunicipio() != null ) {
            if ( entity.getMunicipio() == null ) {
                entity.setMunicipio( new MunicipioEntity() );
            }
            entity.setMunicipio(municipioMapper.updateApiToEntity( api.getMunicipio(), entity.getMunicipio()));
        }

        return entity;
    }

    public EmpresaEntity setEntityDefaultValues(EmpresaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getSituacao() == null) {
            entity.setSituacao("pendente_confirmacao");
        }
        if (entity.getStatusCadastro() == null) {
            entity.setStatusCadastro("nao_cadastrado");
        }
        if (entity.getSituacaoCertificado() == null) {
            entity.setSituacaoCertificado("nao_enviado");
        }
        if (entity.getAmbiente() == null) {
            entity.setAmbiente("homologacao");
        }

        return entity;
    }

    public Empresa setApiDefaultValues(Empresa api) {
        if ( api == null ) {
            return null;
        }

        if (api.getSituacao() == null) {
            api.setSituacao("pendente_confirmacao");
        }
        if (api.getStatusCadastro() == null) {
            api.setStatusCadastro("nao_cadastrado");
        }
        if (api.getSituacaoCertificado() == null) {
            api.setSituacaoCertificado("nao_enviado");
        }
        if (api.getAmbiente() == null) {
            api.setAmbiente("homologacao");
        }

        return api;
    }

}
