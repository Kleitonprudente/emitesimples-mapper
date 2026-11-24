package br.com.emitesimples.integranotas.mapper;

import br.com.emitesimples.integranotas.domain.ChamadaGateway;
import br.com.emitesimples.integranotas.entity.ChamadaGatewayEntity;
import br.com.emitesimples.integranotas.builder.ChamadaGatewayEntityBuilder;
import br.com.emitesimples.integranotas.repository.ChamadaGatewayRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;
import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.nfse.domain.Nota;
import br.com.emitesimples.nfse.mapper.NotaMapper;
import br.com.emitesimples.nfse.entity.NotaEntity;

@ApplicationScoped
public class ChamadaGatewayMapper {

    @Inject ChamadaGatewayRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    NotaMapper notaMapper;

    public ChamadaGateway entityToApi(ChamadaGatewayEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ChamadaGateway chamadaGateway = new ChamadaGateway();

        chamadaGateway.setVersion( entity.getVersion() );
        chamadaGateway.setOperacao( entity.getOperacao() );
        chamadaGateway.setMetodoHttp( entity.getMetodoHttp() );
        chamadaGateway.setRota( entity.getRota() );
        chamadaGateway.setVersaoApi( entity.getVersaoApi() );
        chamadaGateway.setCodigoHttp( entity.getCodigoHttp() );
        chamadaGateway.setCodigoRetorno( entity.getCodigoRetorno() );
        chamadaGateway.setMensagemRetorno( entity.getMensagemRetorno() );
        chamadaGateway.setDuracaoMs( entity.getDuracaoMs() );
        chamadaGateway.setDataChamada( entity.getDataChamada() );
        chamadaGateway.setChave( entity.getChave() );
        chamadaGateway.setIdentificacao( entity.getIdentificacao() );
        chamadaGateway.setResumoEnvio( entity.getResumoEnvio() );
        chamadaGateway.setId( entity.getId() );
        chamadaGateway.setAmbiente( entity.getAmbiente() );
        chamadaGateway.setDataCriacao( entity.getDataCriacao() );
        chamadaGateway.setDataAtualizacao( entity.getDataAtualizacao() );
        chamadaGateway.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        chamadaGateway.setNota(notaMapper.entityToApi(entity.getNota() ));

        this.setApiDefaultValues(chamadaGateway);

        return chamadaGateway;
    }

    public ChamadaGatewayEntity apiToEntity(ChamadaGateway api) {
        if ( api == null ) {
            return null;
        }
        ChamadaGatewayEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ChamadaGateway found for Id: " + api.getId()));
            }
        } else {
            ChamadaGatewayEntityBuilder chamadaGatewayBuilder = ChamadaGatewayEntity.builderOf();

            chamadaGatewayBuilder.version = api.getVersion();
            chamadaGatewayBuilder.operacao = api.getOperacao();
            chamadaGatewayBuilder.metodoHttp = api.getMetodoHttp();
            chamadaGatewayBuilder.rota = api.getRota();
            chamadaGatewayBuilder.versaoApi = api.getVersaoApi();
            chamadaGatewayBuilder.codigoHttp = api.getCodigoHttp();
            chamadaGatewayBuilder.codigoRetorno = api.getCodigoRetorno();
            chamadaGatewayBuilder.mensagemRetorno = api.getMensagemRetorno();
            chamadaGatewayBuilder.duracaoMs = api.getDuracaoMs();
            chamadaGatewayBuilder.dataChamada = api.getDataChamada();
            chamadaGatewayBuilder.chave = api.getChave();
            chamadaGatewayBuilder.identificacao = api.getIdentificacao();
            chamadaGatewayBuilder.resumoEnvio = api.getResumoEnvio();
            chamadaGatewayBuilder.id = api.getId();
            chamadaGatewayBuilder.ambiente = api.getAmbiente();
            chamadaGatewayBuilder.dataCriacao = api.getDataCriacao();
            chamadaGatewayBuilder.dataAtualizacao = api.getDataAtualizacao();
            chamadaGatewayBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            chamadaGatewayBuilder.nota = notaMapper.apiToEntity(api.getNota());
            entity = chamadaGatewayBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<ChamadaGateway> entityListToApiList(List<ChamadaGatewayEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ChamadaGateway> list = new ArrayList<ChamadaGateway>( entity.size() );
        for ( ChamadaGatewayEntity chamadaGatewayEntity : entity ) {
            list.add( entityToApi( chamadaGatewayEntity ) );
        }

        return list;
    }

    public List<ChamadaGatewayEntity> apiListToEntityList(List<ChamadaGateway> api) {
        if ( api == null ) {
            return null;
        }

        List<ChamadaGatewayEntity> list = new ArrayList<ChamadaGatewayEntity>( api.size() );
        for ( ChamadaGateway chamadaGateway : api ) {
            list.add( apiToEntity( chamadaGateway ) );
        }

        return list;
    }

    public ChamadaGateway updateEntityToApi(ChamadaGatewayEntity entity, ChamadaGateway api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setOperacao( entity.getOperacao() );
        api.setMetodoHttp( entity.getMetodoHttp() );
        api.setRota( entity.getRota() );
        api.setVersaoApi( entity.getVersaoApi() );
        api.setCodigoHttp( entity.getCodigoHttp() );
        api.setCodigoRetorno( entity.getCodigoRetorno() );
        api.setMensagemRetorno( entity.getMensagemRetorno() );
        api.setDuracaoMs( entity.getDuracaoMs() );
        api.setDataChamada( entity.getDataChamada() );
        api.setChave( entity.getChave() );
        api.setIdentificacao( entity.getIdentificacao() );
        api.setResumoEnvio( entity.getResumoEnvio() );
        api.setId( entity.getId() );
        api.setAmbiente( entity.getAmbiente() );
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

        if ( entity.getNota() != null ) {
            if ( api.getNota() == null ) {
                api.setNota( new Nota() );
            }
            notaMapper.updateEntityToApi(entity.getNota(), api.getNota());
        } else {
            api.setNota( null );
        }

        this.setApiDefaultValues(api);

        return api;
    }

    public ChamadaGatewayEntity updateApiToEntity(ChamadaGateway api, ChamadaGatewayEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ChamadaGateway found for Id: " + api.getId()));
            }
        } else {
            ChamadaGatewayEntityBuilder chamadaGatewayBuilder = ChamadaGatewayEntity.builderOf();

            chamadaGatewayBuilder.version = api.getVersion();
            entity.setOperacao( api.getOperacao() );
            entity.setMetodoHttp( api.getMetodoHttp() );
            entity.setRota( api.getRota() );
            entity.setVersaoApi( api.getVersaoApi() );
            entity.setCodigoHttp( api.getCodigoHttp() );
            entity.setCodigoRetorno( api.getCodigoRetorno() );
            entity.setMensagemRetorno( api.getMensagemRetorno() );
            entity.setDuracaoMs( api.getDuracaoMs() );
            entity.setDataChamada( api.getDataChamada() );
            entity.setChave( api.getChave() );
            entity.setIdentificacao( api.getIdentificacao() );
            entity.setResumoEnvio( api.getResumoEnvio() );
            entity.setId( api.getId() );
            entity.setAmbiente( api.getAmbiente() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());

            if ( api.getNota() != null ) {
                if ( entity.getNota() == null ) {
                    entity.setNota( new NotaEntity() );
                }
                entity.setNota(notaMapper.updateApiToEntity( api.getNota(), entity.getNota()));
            } else {
                entity.setNota( null );
            }

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ChamadaGatewayEntity patchApiToEntity(ChamadaGateway api, ChamadaGatewayEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getOperacao() != null) entity.setOperacao( api.getOperacao() ); 
        if (api.getMetodoHttp() != null) entity.setMetodoHttp( api.getMetodoHttp() ); 
        if (api.getRota() != null) entity.setRota( api.getRota() ); 
        if (api.getVersaoApi() != null) entity.setVersaoApi( api.getVersaoApi() ); 
        if (api.getCodigoHttp() != null) entity.setCodigoHttp( api.getCodigoHttp() ); 
        if (api.getCodigoRetorno() != null) entity.setCodigoRetorno( api.getCodigoRetorno() ); 
        if (api.getMensagemRetorno() != null) entity.setMensagemRetorno( api.getMensagemRetorno() ); 
        if (api.getDuracaoMs() != null) entity.setDuracaoMs( api.getDuracaoMs() ); 
        if (api.getDataChamada() != null) entity.setDataChamada( api.getDataChamada() ); 
        if (api.getChave() != null) entity.setChave( api.getChave() ); 
        if (api.getIdentificacao() != null) entity.setIdentificacao( api.getIdentificacao() ); 
        if (api.getResumoEnvio() != null) entity.setResumoEnvio( api.getResumoEnvio() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getAmbiente() != null) entity.setAmbiente( api.getAmbiente() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        repository.conferirEmpresaImutavel(entity, api.getEmpresa() == null ? null : api.getEmpresa().getId());
        if ( api.getNota() != null ) {
            if ( entity.getNota() == null ) {
                entity.setNota( new NotaEntity() );
            }
            entity.setNota(notaMapper.updateApiToEntity( api.getNota(), entity.getNota()));
        }

        return entity;
    }

    public ChamadaGatewayEntity setEntityDefaultValues(ChamadaGatewayEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getAmbiente() == null) {
            entity.setAmbiente("HOMOLOGACAO");
        }

        return entity;
    }

    public ChamadaGateway setApiDefaultValues(ChamadaGateway api) {
        if ( api == null ) {
            return null;
        }

        if (api.getAmbiente() == null) {
            api.setAmbiente("HOMOLOGACAO");
        }

        return api;
    }

}
