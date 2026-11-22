package br.com.emitesimples.cadastros.mapper;

import br.com.emitesimples.cadastros.domain.Pessoa;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.builder.PessoaEntityBuilder;
import br.com.emitesimples.cadastros.repository.PessoaRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.cadastros.domain.Municipio;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.domain.Contato;
import br.com.emitesimples.cadastros.entity.ContatoEntity;

@ApplicationScoped
public class PessoaMapper {

    @Inject PessoaRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    MunicipioMapper municipioMapper;
    @Inject 
    ContatoMapper contatoMapper;

    public Pessoa entityToApi(PessoaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Pessoa pessoa = new Pessoa();

        pessoa.setVersion( entity.getVersion() );
        pessoa.setTipoPessoa( entity.getTipoPessoa() );
        pessoa.setDocumento( entity.getDocumento() );
        pessoa.setNome( entity.getNome() );
        pessoa.setNomeFantasia( entity.getNomeFantasia() );
        pessoa.setInscricaoMunicipal( entity.getInscricaoMunicipal() );
        pessoa.setRgInscricaoEstadual( entity.getRgInscricaoEstadual() );
        pessoa.setTipoDestinatario( entity.getTipoDestinatario() );
        pessoa.setConsumidorFinal( entity.getConsumidorFinal() );
        pessoa.setCliente( entity.getCliente() );
        pessoa.setFornecedor( entity.getFornecedor() );
        pessoa.setTelefone( entity.getTelefone() );
        pessoa.setEmail( entity.getEmail() );
        pessoa.setLogradouro( entity.getLogradouro() );
        pessoa.setNumeroEndereco( entity.getNumeroEndereco() );
        pessoa.setComplemento( entity.getComplemento() );
        pessoa.setBairro( entity.getBairro() );
        pessoa.setCep( entity.getCep() );
        pessoa.setCodigoPais( entity.getCodigoPais() );
        pessoa.setCodigoPostal( entity.getCodigoPostal() );
        pessoa.setNomeMunicipioExterior( entity.getNomeMunicipioExterior() );
        pessoa.setNomeProvincia( entity.getNomeProvincia() );
        pessoa.setId( entity.getId() );
        pessoa.setDataCriacao( entity.getDataCriacao() );
        pessoa.setDataAtualizacao( entity.getDataAtualizacao() );
        pessoa.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        pessoa.setCidade(municipioMapper.entityToApi(entity.getCidade() ));

        this.setApiDefaultValues(pessoa);

        return pessoa;
    }

    public PessoaEntity apiToEntity(Pessoa api) {
        if ( api == null ) {
            return null;
        }
        PessoaEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Pessoa found for Id: " + api.getId()));
            }
        } else {
            PessoaEntityBuilder pessoaBuilder = PessoaEntity.builderOf();

            pessoaBuilder.version = api.getVersion();
            pessoaBuilder.tipoPessoa = api.getTipoPessoa();
            pessoaBuilder.documento = api.getDocumento();
            pessoaBuilder.nome = api.getNome();
            pessoaBuilder.nomeFantasia = api.getNomeFantasia();
            pessoaBuilder.inscricaoMunicipal = api.getInscricaoMunicipal();
            pessoaBuilder.rgInscricaoEstadual = api.getRgInscricaoEstadual();
            pessoaBuilder.tipoDestinatario = api.getTipoDestinatario();
            pessoaBuilder.consumidorFinal = api.getConsumidorFinal();
            pessoaBuilder.cliente = api.getCliente();
            pessoaBuilder.fornecedor = api.getFornecedor();
            pessoaBuilder.telefone = api.getTelefone();
            pessoaBuilder.email = api.getEmail();
            pessoaBuilder.logradouro = api.getLogradouro();
            pessoaBuilder.numeroEndereco = api.getNumeroEndereco();
            pessoaBuilder.complemento = api.getComplemento();
            pessoaBuilder.bairro = api.getBairro();
            pessoaBuilder.cep = api.getCep();
            pessoaBuilder.codigoPais = api.getCodigoPais();
            pessoaBuilder.codigoPostal = api.getCodigoPostal();
            pessoaBuilder.nomeMunicipioExterior = api.getNomeMunicipioExterior();
            pessoaBuilder.nomeProvincia = api.getNomeProvincia();
            pessoaBuilder.id = api.getId();
            pessoaBuilder.dataCriacao = api.getDataCriacao();
            pessoaBuilder.dataAtualizacao = api.getDataAtualizacao();
            pessoaBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            pessoaBuilder.cidade = municipioMapper.apiToEntity(api.getCidade());
            entity = pessoaBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Pessoa> entityListToApiList(List<PessoaEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Pessoa> list = new ArrayList<Pessoa>( entity.size() );
        for ( PessoaEntity pessoaEntity : entity ) {
            list.add( entityToApi( pessoaEntity ) );
        }

        return list;
    }

    public List<PessoaEntity> apiListToEntityList(List<Pessoa> api) {
        if ( api == null ) {
            return null;
        }

        List<PessoaEntity> list = new ArrayList<PessoaEntity>( api.size() );
        for ( Pessoa pessoa : api ) {
            list.add( apiToEntity( pessoa ) );
        }

        return list;
    }

    public Pessoa updateEntityToApi(PessoaEntity entity, Pessoa api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setTipoPessoa( entity.getTipoPessoa() );
        api.setDocumento( entity.getDocumento() );
        api.setNome( entity.getNome() );
        api.setNomeFantasia( entity.getNomeFantasia() );
        api.setInscricaoMunicipal( entity.getInscricaoMunicipal() );
        api.setRgInscricaoEstadual( entity.getRgInscricaoEstadual() );
        api.setTipoDestinatario( entity.getTipoDestinatario() );
        api.setConsumidorFinal( entity.getConsumidorFinal() );
        api.setCliente( entity.getCliente() );
        api.setFornecedor( entity.getFornecedor() );
        api.setTelefone( entity.getTelefone() );
        api.setEmail( entity.getEmail() );
        api.setLogradouro( entity.getLogradouro() );
        api.setNumeroEndereco( entity.getNumeroEndereco() );
        api.setComplemento( entity.getComplemento() );
        api.setBairro( entity.getBairro() );
        api.setCep( entity.getCep() );
        api.setCodigoPais( entity.getCodigoPais() );
        api.setCodigoPostal( entity.getCodigoPostal() );
        api.setNomeMunicipioExterior( entity.getNomeMunicipioExterior() );
        api.setNomeProvincia( entity.getNomeProvincia() );
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

        if ( entity.getCidade() != null ) {
            if ( api.getCidade() == null ) {
                api.setCidade( new Municipio() );
            }
            municipioMapper.updateEntityToApi(entity.getCidade(), api.getCidade());
        } else {
            api.setCidade( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public PessoaEntity updateApiToEntity(Pessoa api, PessoaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Pessoa found for Id: " + api.getId()));
            }
        } else {
            PessoaEntityBuilder pessoaBuilder = PessoaEntity.builderOf();

            pessoaBuilder.version = api.getVersion();
            entity.setTipoPessoa( api.getTipoPessoa() );
            entity.setDocumento( api.getDocumento() );
            entity.setNome( api.getNome() );
            entity.setNomeFantasia( api.getNomeFantasia() );
            entity.setInscricaoMunicipal( api.getInscricaoMunicipal() );
            entity.setRgInscricaoEstadual( api.getRgInscricaoEstadual() );
            entity.setTipoDestinatario( api.getTipoDestinatario() );
            entity.setConsumidorFinal( api.getConsumidorFinal() );
            entity.setCliente( api.getCliente() );
            entity.setFornecedor( api.getFornecedor() );
            entity.setTelefone( api.getTelefone() );
            entity.setEmail( api.getEmail() );
            entity.setLogradouro( api.getLogradouro() );
            entity.setNumeroEndereco( api.getNumeroEndereco() );
            entity.setComplemento( api.getComplemento() );
            entity.setBairro( api.getBairro() );
            entity.setCep( api.getCep() );
            entity.setCodigoPais( api.getCodigoPais() );
            entity.setCodigoPostal( api.getCodigoPostal() );
            entity.setNomeMunicipioExterior( api.getNomeMunicipioExterior() );
            entity.setNomeProvincia( api.getNomeProvincia() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            if ( api.getCidade() != null ) {
                if ( entity.getCidade() == null ) {
                    entity.setCidade( new MunicipioEntity() );
                }
                entity.setCidade(municipioMapper.updateApiToEntity( api.getCidade(), entity.getCidade()));
            } else {
                entity.setCidade( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public PessoaEntity patchApiToEntity(Pessoa api, PessoaEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getTipoPessoa() != null) entity.setTipoPessoa( api.getTipoPessoa() ); 
        if (api.getDocumento() != null) entity.setDocumento( api.getDocumento() ); 
        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getNomeFantasia() != null) entity.setNomeFantasia( api.getNomeFantasia() ); 
        if (api.getInscricaoMunicipal() != null) entity.setInscricaoMunicipal( api.getInscricaoMunicipal() ); 
        if (api.getRgInscricaoEstadual() != null) entity.setRgInscricaoEstadual( api.getRgInscricaoEstadual() ); 
        if (api.getTipoDestinatario() != null) entity.setTipoDestinatario( api.getTipoDestinatario() ); 
        if (api.getConsumidorFinal() != null) entity.setConsumidorFinal( api.getConsumidorFinal() ); 
        if (api.getCliente() != null) entity.setCliente( api.getCliente() ); 
        if (api.getFornecedor() != null) entity.setFornecedor( api.getFornecedor() ); 
        if (api.getTelefone() != null) entity.setTelefone( api.getTelefone() ); 
        if (api.getEmail() != null) entity.setEmail( api.getEmail() ); 
        if (api.getLogradouro() != null) entity.setLogradouro( api.getLogradouro() ); 
        if (api.getNumeroEndereco() != null) entity.setNumeroEndereco( api.getNumeroEndereco() ); 
        if (api.getComplemento() != null) entity.setComplemento( api.getComplemento() ); 
        if (api.getBairro() != null) entity.setBairro( api.getBairro() ); 
        if (api.getCep() != null) entity.setCep( api.getCep() ); 
        if (api.getCodigoPais() != null) entity.setCodigoPais( api.getCodigoPais() ); 
        if (api.getCodigoPostal() != null) entity.setCodigoPostal( api.getCodigoPostal() ); 
        if (api.getNomeMunicipioExterior() != null) entity.setNomeMunicipioExterior( api.getNomeMunicipioExterior() ); 
        if (api.getNomeProvincia() != null) entity.setNomeProvincia( api.getNomeProvincia() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());
        if ( api.getCidade() != null ) {
            if ( entity.getCidade() == null ) {
                entity.setCidade( new MunicipioEntity() );
            }
            entity.setCidade(municipioMapper.updateApiToEntity( api.getCidade(), entity.getCidade()));
        }

        return entity;
    }

    public PessoaEntity setEntityDefaultValues(PessoaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getConsumidorFinal() == null) {
            entity.setConsumidorFinal(true);
        }
        if (entity.getCliente() == null) {
            entity.setCliente(false);
        }
        if (entity.getFornecedor() == null) {
            entity.setFornecedor(false);
        }

        return entity;
    }

    public Pessoa setApiDefaultValues(Pessoa api) {
        if ( api == null ) {
            return null;
        }

        if (api.getConsumidorFinal() == null) {
            api.setConsumidorFinal(true);
        }
        if (api.getCliente() == null) {
            api.setCliente(false);
        }
        if (api.getFornecedor() == null) {
            api.setFornecedor(false);
        }

        return api;
    }

}
