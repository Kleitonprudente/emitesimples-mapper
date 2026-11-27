package br.com.emitesimples.cadastros.mapper;

import br.com.emitesimples.cadastros.domain.Contato;
import br.com.emitesimples.cadastros.entity.ContatoEntity;
import br.com.emitesimples.cadastros.builder.ContatoEntityBuilder;
import br.com.emitesimples.cadastros.repository.ContatoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.cadastros.domain.Pessoa;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;

@ApplicationScoped
public class ContatoMapper {

    @Inject ContatoRepository repository;
    @Inject 
    PessoaMapper pessoaMapper;
    @Inject 
    EmpresaMapper empresaMapper;

    public Contato entityToApi(ContatoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Contato contato = new Contato();

        contato.setVersion( entity.getVersion() );
        contato.setTipoContato( entity.getTipoContato() );
        contato.setNome( entity.getNome() );
        contato.setWhatsapp( entity.getWhatsapp() );
        contato.setEmail( entity.getEmail() );
        contato.setId( entity.getId() );
        contato.setDataCriacao( entity.getDataCriacao() );
        contato.setDataAtualizacao( entity.getDataAtualizacao() );
        if ( entity.getPessoa() != null ) {
            Pessoa pessoaRef = new Pessoa();
            pessoaRef.setId( entity.getPessoa().getId() );
            contato.setPessoa( pessoaRef );
        }
        contato.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));

        this.setApiDefaultValues(contato);

        return contato;
    }

    public ContatoEntity apiToEntity(Contato api) {
        if ( api == null ) {
            return null;
        }
        ContatoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Contato found for Id: " + api.getId()));
            }
        } else {
            ContatoEntityBuilder contatoBuilder = ContatoEntity.builderOf();

            contatoBuilder.version = api.getVersion();
            contatoBuilder.tipoContato = api.getTipoContato();
            contatoBuilder.nome = api.getNome();
            contatoBuilder.whatsapp = api.getWhatsapp();
            contatoBuilder.email = api.getEmail();
            contatoBuilder.id = api.getId();
            contatoBuilder.dataCriacao = api.getDataCriacao();
            contatoBuilder.dataAtualizacao = api.getDataAtualizacao();
            contatoBuilder.pessoa = pessoaMapper.apiToEntity(api.getPessoa());
            contatoBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            entity = contatoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Contato> entityListToApiList(List<ContatoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Contato> list = new ArrayList<Contato>( entity.size() );
        for ( ContatoEntity contatoEntity : entity ) {
            list.add( entityToApi( contatoEntity ) );
        }

        return list;
    }

    public List<ContatoEntity> apiListToEntityList(List<Contato> api) {
        if ( api == null ) {
            return null;
        }

        List<ContatoEntity> list = new ArrayList<ContatoEntity>( api.size() );
        for ( Contato contato : api ) {
            list.add( apiToEntity( contato ) );
        }

        return list;
    }

    public Contato updateEntityToApi(ContatoEntity entity, Contato api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setTipoContato( entity.getTipoContato() );
        api.setNome( entity.getNome() );
        api.setWhatsapp( entity.getWhatsapp() );
        api.setEmail( entity.getEmail() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );
        if ( entity.getPessoa() != null ) {
            Pessoa pessoaRef = new Pessoa();
            pessoaRef.setId( entity.getPessoa().getId() );
            api.setPessoa( pessoaRef );
        } else {
            api.setPessoa( null );
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

    public ContatoEntity updateApiToEntity(Contato api, ContatoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Contato found for Id: " + api.getId()));
            }
        } else {
            ContatoEntityBuilder contatoBuilder = ContatoEntity.builderOf();

            contatoBuilder.version = api.getVersion();
            entity.setTipoContato( api.getTipoContato() );
            entity.setNome( api.getNome() );
            entity.setWhatsapp( api.getWhatsapp() );
            entity.setEmail( api.getEmail() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            if ( api.getPessoa() != null ) {
                // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
                // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
                entity.setPessoa(pessoaMapper.updateApiToEntity( api.getPessoa(), entity.getPessoa() != null ? entity.getPessoa() : new PessoaEntity()));
            } else {
                entity.setPessoa( null );
            }
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ContatoEntity patchApiToEntity(Contato api, ContatoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getTipoContato() != null) entity.setTipoContato( api.getTipoContato() ); 
        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getWhatsapp() != null) entity.setWhatsapp( api.getWhatsapp() ); 
        if (api.getEmail() != null) entity.setEmail( api.getEmail() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if ( api.getPessoa() != null ) {
            // resolve a referencia ANTES de liga-la a entidade gerenciada: uma entidade nova pendurada nela
            // faria o flush automatico da busca por id falhar (TransientPropertyValueException)
            entity.setPessoa(pessoaMapper.updateApiToEntity( api.getPessoa(), entity.getPessoa() != null ? entity.getPessoa() : new PessoaEntity()));
        }
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

        return entity;
    }

    public ContatoEntity setEntityDefaultValues(ContatoEntity entity) {
        if ( entity == null ) {
            return null;
        }


        return entity;
    }

    public Contato setApiDefaultValues(Contato api) {
        if ( api == null ) {
            return null;
        }


        return api;
    }

}
