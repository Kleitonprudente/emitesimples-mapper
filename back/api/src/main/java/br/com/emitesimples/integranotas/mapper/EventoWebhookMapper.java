package br.com.emitesimples.integranotas.mapper;

import br.com.emitesimples.integranotas.domain.EventoWebhook;
import br.com.emitesimples.integranotas.entity.EventoWebhookEntity;
import br.com.emitesimples.integranotas.builder.EventoWebhookEntityBuilder;
import br.com.emitesimples.integranotas.repository.EventoWebhookRepository;

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
public class EventoWebhookMapper {

    @Inject EventoWebhookRepository repository;
    @Inject 
    EmpresaMapper empresaMapper;
    @Inject 
    NotaMapper notaMapper;

    public EventoWebhook entityToApi(EventoWebhookEntity entity) {
        if ( entity == null ) {
            return null;
        }

        EventoWebhook eventoWebhook = new EventoWebhook();

        eventoWebhook.setVersion( entity.getVersion() );
        eventoWebhook.setOrigem( entity.getOrigem() );
        eventoWebhook.setCnpjCpf( entity.getCnpjCpf() );
        eventoWebhook.setTipo( entity.getTipo() );
        eventoWebhook.setChave( entity.getChave() );
        eventoWebhook.setSucesso( entity.getSucesso() );
        eventoWebhook.setCodigo( entity.getCodigo() );
        eventoWebhook.setMensagem( entity.getMensagem() );
        eventoWebhook.setIdentificacao( entity.getIdentificacao() );
        eventoWebhook.setDataRecebimento( entity.getDataRecebimento() );
        eventoWebhook.setResultadoAssinatura( entity.getResultadoAssinatura() );
        eventoWebhook.setEstadoProcessamento( entity.getEstadoProcessamento() );
        eventoWebhook.setDataProcessamento( entity.getDataProcessamento() );
        eventoWebhook.setId( entity.getId() );
        eventoWebhook.setAmbiente( entity.getAmbiente() );
        eventoWebhook.setDataCriacao( entity.getDataCriacao() );
        eventoWebhook.setDataAtualizacao( entity.getDataAtualizacao() );
        eventoWebhook.setEmpresa(empresaMapper.entityToApi(entity.getEmpresa() ));
        eventoWebhook.setNota(notaMapper.entityToApi(entity.getNota() ));

        this.setApiDefaultValues(eventoWebhook);

        return eventoWebhook;
    }

    public EventoWebhookEntity apiToEntity(EventoWebhook api) {
        if ( api == null ) {
            return null;
        }
        EventoWebhookEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity EventoWebhook found for Id: " + api.getId()));
            }
        } else {
            EventoWebhookEntityBuilder eventoWebhookBuilder = EventoWebhookEntity.builderOf();

            eventoWebhookBuilder.version = api.getVersion();
            eventoWebhookBuilder.origem = api.getOrigem();
            eventoWebhookBuilder.cnpjCpf = api.getCnpjCpf();
            eventoWebhookBuilder.tipo = api.getTipo();
            eventoWebhookBuilder.chave = api.getChave();
            eventoWebhookBuilder.sucesso = api.getSucesso();
            eventoWebhookBuilder.codigo = api.getCodigo();
            eventoWebhookBuilder.mensagem = api.getMensagem();
            eventoWebhookBuilder.identificacao = api.getIdentificacao();
            eventoWebhookBuilder.dataRecebimento = api.getDataRecebimento();
            eventoWebhookBuilder.resultadoAssinatura = api.getResultadoAssinatura();
            eventoWebhookBuilder.estadoProcessamento = api.getEstadoProcessamento();
            eventoWebhookBuilder.dataProcessamento = api.getDataProcessamento();
            eventoWebhookBuilder.id = api.getId();
            eventoWebhookBuilder.ambiente = api.getAmbiente();
            eventoWebhookBuilder.dataCriacao = api.getDataCriacao();
            eventoWebhookBuilder.dataAtualizacao = api.getDataAtualizacao();
            eventoWebhookBuilder.empresa = repository.empresaEfetiva(api.getEmpresa() == null ? null : api.getEmpresa().getId());
            eventoWebhookBuilder.nota = notaMapper.apiToEntity(api.getNota());
            entity = eventoWebhookBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<EventoWebhook> entityListToApiList(List<EventoWebhookEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<EventoWebhook> list = new ArrayList<EventoWebhook>( entity.size() );
        for ( EventoWebhookEntity eventoWebhookEntity : entity ) {
            list.add( entityToApi( eventoWebhookEntity ) );
        }

        return list;
    }

    public List<EventoWebhookEntity> apiListToEntityList(List<EventoWebhook> api) {
        if ( api == null ) {
            return null;
        }

        List<EventoWebhookEntity> list = new ArrayList<EventoWebhookEntity>( api.size() );
        for ( EventoWebhook eventoWebhook : api ) {
            list.add( apiToEntity( eventoWebhook ) );
        }

        return list;
    }

    public EventoWebhook updateEntityToApi(EventoWebhookEntity entity, EventoWebhook api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setOrigem( entity.getOrigem() );
        api.setCnpjCpf( entity.getCnpjCpf() );
        api.setTipo( entity.getTipo() );
        api.setChave( entity.getChave() );
        api.setSucesso( entity.getSucesso() );
        api.setCodigo( entity.getCodigo() );
        api.setMensagem( entity.getMensagem() );
        api.setIdentificacao( entity.getIdentificacao() );
        api.setDataRecebimento( entity.getDataRecebimento() );
        api.setResultadoAssinatura( entity.getResultadoAssinatura() );
        api.setEstadoProcessamento( entity.getEstadoProcessamento() );
        api.setDataProcessamento( entity.getDataProcessamento() );
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

    public EventoWebhookEntity updateApiToEntity(EventoWebhook api, EventoWebhookEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity EventoWebhook found for Id: " + api.getId()));
            }
        } else {
            EventoWebhookEntityBuilder eventoWebhookBuilder = EventoWebhookEntity.builderOf();

            eventoWebhookBuilder.version = api.getVersion();
            entity.setOrigem( api.getOrigem() );
            entity.setCnpjCpf( api.getCnpjCpf() );
            entity.setTipo( api.getTipo() );
            entity.setChave( api.getChave() );
            entity.setSucesso( api.getSucesso() );
            entity.setCodigo( api.getCodigo() );
            entity.setMensagem( api.getMensagem() );
            entity.setIdentificacao( api.getIdentificacao() );
            entity.setDataRecebimento( api.getDataRecebimento() );
            entity.setResultadoAssinatura( api.getResultadoAssinatura() );
            entity.setEstadoProcessamento( api.getEstadoProcessamento() );
            entity.setDataProcessamento( api.getDataProcessamento() );
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

    public EventoWebhookEntity patchApiToEntity(EventoWebhook api, EventoWebhookEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getOrigem() != null) entity.setOrigem( api.getOrigem() ); 
        if (api.getCnpjCpf() != null) entity.setCnpjCpf( api.getCnpjCpf() ); 
        if (api.getTipo() != null) entity.setTipo( api.getTipo() ); 
        if (api.getChave() != null) entity.setChave( api.getChave() ); 
        if (api.getSucesso() != null) entity.setSucesso( api.getSucesso() ); 
        if (api.getCodigo() != null) entity.setCodigo( api.getCodigo() ); 
        if (api.getMensagem() != null) entity.setMensagem( api.getMensagem() ); 
        if (api.getIdentificacao() != null) entity.setIdentificacao( api.getIdentificacao() ); 
        if (api.getDataRecebimento() != null) entity.setDataRecebimento( api.getDataRecebimento() ); 
        if (api.getResultadoAssinatura() != null) entity.setResultadoAssinatura( api.getResultadoAssinatura() ); 
        if (api.getEstadoProcessamento() != null) entity.setEstadoProcessamento( api.getEstadoProcessamento() ); 
        if (api.getDataProcessamento() != null) entity.setDataProcessamento( api.getDataProcessamento() ); 
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

    public EventoWebhookEntity setEntityDefaultValues(EventoWebhookEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getEstadoProcessamento() == null) {
            entity.setEstadoProcessamento("RECEBIDO");
        }
        if (entity.getAmbiente() == null) {
            entity.setAmbiente("HOMOLOGACAO");
        }

        return entity;
    }

    public EventoWebhook setApiDefaultValues(EventoWebhook api) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstadoProcessamento() == null) {
            api.setEstadoProcessamento("RECEBIDO");
        }
        if (api.getAmbiente() == null) {
            api.setAmbiente("HOMOLOGACAO");
        }

        return api;
    }

}
