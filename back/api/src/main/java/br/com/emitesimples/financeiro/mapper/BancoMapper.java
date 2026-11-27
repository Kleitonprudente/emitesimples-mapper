package br.com.emitesimples.financeiro.mapper;

import br.com.emitesimples.financeiro.domain.Banco;
import br.com.emitesimples.financeiro.entity.BancoEntity;
import br.com.emitesimples.financeiro.builder.BancoEntityBuilder;
import br.com.emitesimples.financeiro.repository.BancoRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;

@ApplicationScoped
public class BancoMapper {

    @Inject BancoRepository repository;

    public Banco entityToApi(BancoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Banco banco = new Banco();

        banco.setVersion( entity.getVersion() );
        banco.setCodigo( entity.getCodigo() );
        banco.setIspb( entity.getIspb() );
        banco.setNome( entity.getNome() );
        banco.setNomeExtenso( entity.getNomeExtenso() );
        banco.setParticipaCompe( entity.getParticipaCompe() );
        banco.setIntegradoGateway( entity.getIntegradoGateway() );
        banco.setUrlPassoAPasso( entity.getUrlPassoAPasso() );
        banco.setId( entity.getId() );
        banco.setDataCriacao( entity.getDataCriacao() );
        banco.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(banco);

        return banco;
    }

    public BancoEntity apiToEntity(Banco api) {
        if ( api == null ) {
            return null;
        }
        BancoEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Banco found for Id: " + api.getId()));
            }
        } else {
            BancoEntityBuilder bancoBuilder = BancoEntity.builderOf();

            bancoBuilder.version = api.getVersion();
            bancoBuilder.codigo = api.getCodigo();
            bancoBuilder.ispb = api.getIspb();
            bancoBuilder.nome = api.getNome();
            bancoBuilder.nomeExtenso = api.getNomeExtenso();
            bancoBuilder.participaCompe = api.getParticipaCompe();
            bancoBuilder.integradoGateway = api.getIntegradoGateway();
            bancoBuilder.urlPassoAPasso = api.getUrlPassoAPasso();
            bancoBuilder.id = api.getId();
            bancoBuilder.dataCriacao = api.getDataCriacao();
            bancoBuilder.dataAtualizacao = api.getDataAtualizacao();
            entity = bancoBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<Banco> entityListToApiList(List<BancoEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<Banco> list = new ArrayList<Banco>( entity.size() );
        for ( BancoEntity bancoEntity : entity ) {
            list.add( entityToApi( bancoEntity ) );
        }

        return list;
    }

    public List<BancoEntity> apiListToEntityList(List<Banco> api) {
        if ( api == null ) {
            return null;
        }

        List<BancoEntity> list = new ArrayList<BancoEntity>( api.size() );
        for ( Banco banco : api ) {
            list.add( apiToEntity( banco ) );
        }

        return list;
    }

    public Banco updateEntityToApi(BancoEntity entity, Banco api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setCodigo( entity.getCodigo() );
        api.setIspb( entity.getIspb() );
        api.setNome( entity.getNome() );
        api.setNomeExtenso( entity.getNomeExtenso() );
        api.setParticipaCompe( entity.getParticipaCompe() );
        api.setIntegradoGateway( entity.getIntegradoGateway() );
        api.setUrlPassoAPasso( entity.getUrlPassoAPasso() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );

        this.setApiDefaultValues(api);

        return api;
    }

    public BancoEntity updateApiToEntity(Banco api, BancoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity Banco found for Id: " + api.getId()));
            }
        } else {
            BancoEntityBuilder bancoBuilder = BancoEntity.builderOf();

            bancoBuilder.version = api.getVersion();
            entity.setCodigo( api.getCodigo() );
            entity.setIspb( api.getIspb() );
            entity.setNome( api.getNome() );
            entity.setNomeExtenso( api.getNomeExtenso() );
            entity.setParticipaCompe( api.getParticipaCompe() );
            entity.setIntegradoGateway( api.getIntegradoGateway() );
            entity.setUrlPassoAPasso( api.getUrlPassoAPasso() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public BancoEntity patchApiToEntity(Banco api, BancoEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getCodigo() != null) entity.setCodigo( api.getCodigo() ); 
        if (api.getIspb() != null) entity.setIspb( api.getIspb() ); 
        if (api.getNome() != null) entity.setNome( api.getNome() ); 
        if (api.getNomeExtenso() != null) entity.setNomeExtenso( api.getNomeExtenso() ); 
        if (api.getParticipaCompe() != null) entity.setParticipaCompe( api.getParticipaCompe() ); 
        if (api.getIntegradoGateway() != null) entity.setIntegradoGateway( api.getIntegradoGateway() ); 
        if (api.getUrlPassoAPasso() != null) entity.setUrlPassoAPasso( api.getUrlPassoAPasso() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 

        return entity;
    }

    public BancoEntity setEntityDefaultValues(BancoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getParticipaCompe() == null) {
            entity.setParticipaCompe(false);
        }
        if (entity.getIntegradoGateway() == null) {
            entity.setIntegradoGateway(false);
        }

        return entity;
    }

    public Banco setApiDefaultValues(Banco api) {
        if ( api == null ) {
            return null;
        }

        if (api.getParticipaCompe() == null) {
            api.setParticipaCompe(false);
        }
        if (api.getIntegradoGateway() == null) {
            api.setIntegradoGateway(false);
        }

        return api;
    }

}
