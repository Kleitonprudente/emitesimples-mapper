package br.com.emitesimples.cadastros.mapper;

import br.com.emitesimples.cadastros.domain.ImportacaoMunicipio;
import br.com.emitesimples.cadastros.entity.ImportacaoMunicipioEntity;
import br.com.emitesimples.cadastros.builder.ImportacaoMunicipioEntityBuilder;
import br.com.emitesimples.cadastros.repository.ImportacaoMunicipioRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import br.com.emitesimples.exceptions.NotFoundException;

@ApplicationScoped
public class ImportacaoMunicipioMapper {

    @Inject ImportacaoMunicipioRepository repository;

    public ImportacaoMunicipio entityToApi(ImportacaoMunicipioEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ImportacaoMunicipio importacaoMunicipio = new ImportacaoMunicipio();

        importacaoMunicipio.setVersion( entity.getVersion() );
        importacaoMunicipio.setNomeArquivo( entity.getNomeArquivo() );
        importacaoMunicipio.setCodificacaoDetectada( entity.getCodificacaoDetectada() );
        importacaoMunicipio.setSeparadorDetectado( entity.getSeparadorDetectado() );
        importacaoMunicipio.setEstado( entity.getEstado() );
        importacaoMunicipio.setDataHoraInicio( entity.getDataHoraInicio() );
        importacaoMunicipio.setDataHoraFim( entity.getDataHoraFim() );
        importacaoMunicipio.setLinhasLidas( entity.getLinhasLidas() );
        importacaoMunicipio.setLinhasInseridas( entity.getLinhasInseridas() );
        importacaoMunicipio.setLinhasAtualizadas( entity.getLinhasAtualizadas() );
        importacaoMunicipio.setLinhasIguais( entity.getLinhasIguais() );
        importacaoMunicipio.setLinhasRejeitadas( entity.getLinhasRejeitadas() );
        importacaoMunicipio.setResumoRejeicoes( entity.getResumoRejeicoes() );
        importacaoMunicipio.setId( entity.getId() );
        importacaoMunicipio.setDataCriacao( entity.getDataCriacao() );
        importacaoMunicipio.setDataAtualizacao( entity.getDataAtualizacao() );
        importacaoMunicipio.setIdUsuario( entity.getIdUsuario() );

        this.setApiDefaultValues(importacaoMunicipio);

        return importacaoMunicipio;
    }

    public ImportacaoMunicipioEntity apiToEntity(ImportacaoMunicipio api) {
        if ( api == null ) {
            return null;
        }
        ImportacaoMunicipioEntity entity = null;

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ImportacaoMunicipio found for Id: " + api.getId()));
            }
        } else {
            ImportacaoMunicipioEntityBuilder importacaoMunicipioBuilder = ImportacaoMunicipioEntity.builderOf();

            importacaoMunicipioBuilder.version = api.getVersion();
            importacaoMunicipioBuilder.nomeArquivo = api.getNomeArquivo();
            importacaoMunicipioBuilder.codificacaoDetectada = api.getCodificacaoDetectada();
            importacaoMunicipioBuilder.separadorDetectado = api.getSeparadorDetectado();
            importacaoMunicipioBuilder.estado = api.getEstado();
            importacaoMunicipioBuilder.dataHoraInicio = api.getDataHoraInicio();
            importacaoMunicipioBuilder.dataHoraFim = api.getDataHoraFim();
            importacaoMunicipioBuilder.linhasLidas = api.getLinhasLidas();
            importacaoMunicipioBuilder.linhasInseridas = api.getLinhasInseridas();
            importacaoMunicipioBuilder.linhasAtualizadas = api.getLinhasAtualizadas();
            importacaoMunicipioBuilder.linhasIguais = api.getLinhasIguais();
            importacaoMunicipioBuilder.linhasRejeitadas = api.getLinhasRejeitadas();
            importacaoMunicipioBuilder.resumoRejeicoes = api.getResumoRejeicoes();
            importacaoMunicipioBuilder.id = api.getId();
            importacaoMunicipioBuilder.dataCriacao = api.getDataCriacao();
            importacaoMunicipioBuilder.dataAtualizacao = api.getDataAtualizacao();
            importacaoMunicipioBuilder.idUsuario = api.getIdUsuario();
            entity = importacaoMunicipioBuilder.build();
            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public List<ImportacaoMunicipio> entityListToApiList(List<ImportacaoMunicipioEntity> entity) {
        if ( entity == null ) {
            return null;
        }

        List<ImportacaoMunicipio> list = new ArrayList<ImportacaoMunicipio>( entity.size() );
        for ( ImportacaoMunicipioEntity importacaoMunicipioEntity : entity ) {
            list.add( entityToApi( importacaoMunicipioEntity ) );
        }

        return list;
    }

    public List<ImportacaoMunicipioEntity> apiListToEntityList(List<ImportacaoMunicipio> api) {
        if ( api == null ) {
            return null;
        }

        List<ImportacaoMunicipioEntity> list = new ArrayList<ImportacaoMunicipioEntity>( api.size() );
        for ( ImportacaoMunicipio importacaoMunicipio : api ) {
            list.add( apiToEntity( importacaoMunicipio ) );
        }

        return list;
    }

    public ImportacaoMunicipio updateEntityToApi(ImportacaoMunicipioEntity entity, ImportacaoMunicipio api) {
        if ( entity == null ) {
            return null;
        }

        api.setVersion( entity.getVersion() );
        api.setNomeArquivo( entity.getNomeArquivo() );
        api.setCodificacaoDetectada( entity.getCodificacaoDetectada() );
        api.setSeparadorDetectado( entity.getSeparadorDetectado() );
        api.setEstado( entity.getEstado() );
        api.setDataHoraInicio( entity.getDataHoraInicio() );
        api.setDataHoraFim( entity.getDataHoraFim() );
        api.setLinhasLidas( entity.getLinhasLidas() );
        api.setLinhasInseridas( entity.getLinhasInseridas() );
        api.setLinhasAtualizadas( entity.getLinhasAtualizadas() );
        api.setLinhasIguais( entity.getLinhasIguais() );
        api.setLinhasRejeitadas( entity.getLinhasRejeitadas() );
        api.setResumoRejeicoes( entity.getResumoRejeicoes() );
        api.setId( entity.getId() );
        api.setDataCriacao( entity.getDataCriacao() );
        api.setDataAtualizacao( entity.getDataAtualizacao() );
        api.setIdUsuario( entity.getIdUsuario() );

        this.setApiDefaultValues(api);

        return api;
    }

    public ImportacaoMunicipioEntity updateApiToEntity(ImportacaoMunicipio api, ImportacaoMunicipioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.isWeakReference()) {
            if (api.isIdDefined()) {
                entity = repository.findByIdOptional(api.getId()).orElseThrow(() -> new NotFoundException("No entity ImportacaoMunicipio found for Id: " + api.getId()));
            }
        } else {
            ImportacaoMunicipioEntityBuilder importacaoMunicipioBuilder = ImportacaoMunicipioEntity.builderOf();

            importacaoMunicipioBuilder.version = api.getVersion();
            entity.setNomeArquivo( api.getNomeArquivo() );
            entity.setCodificacaoDetectada( api.getCodificacaoDetectada() );
            entity.setSeparadorDetectado( api.getSeparadorDetectado() );
            entity.setEstado( api.getEstado() );
            entity.setDataHoraInicio( api.getDataHoraInicio() );
            entity.setDataHoraFim( api.getDataHoraFim() );
            entity.setLinhasLidas( api.getLinhasLidas() );
            entity.setLinhasInseridas( api.getLinhasInseridas() );
            entity.setLinhasAtualizadas( api.getLinhasAtualizadas() );
            entity.setLinhasIguais( api.getLinhasIguais() );
            entity.setLinhasRejeitadas( api.getLinhasRejeitadas() );
            entity.setResumoRejeicoes( api.getResumoRejeicoes() );
            entity.setId( api.getId() );
            entity.setDataCriacao( api.getDataCriacao() );
            entity.setDataAtualizacao( api.getDataAtualizacao() );
            entity.setIdUsuario( api.getIdUsuario() );

            this.setEntityDefaultValues(entity);
        }

        return entity;
    }

    public ImportacaoMunicipioEntity patchApiToEntity(ImportacaoMunicipio api, ImportacaoMunicipioEntity entity) {
        if ( api == null ) {
            return null;
        }

        if (api.getNomeArquivo() != null) entity.setNomeArquivo( api.getNomeArquivo() ); 
        if (api.getCodificacaoDetectada() != null) entity.setCodificacaoDetectada( api.getCodificacaoDetectada() ); 
        if (api.getSeparadorDetectado() != null) entity.setSeparadorDetectado( api.getSeparadorDetectado() ); 
        if (api.getEstado() != null) entity.setEstado( api.getEstado() ); 
        if (api.getDataHoraInicio() != null) entity.setDataHoraInicio( api.getDataHoraInicio() ); 
        if (api.getDataHoraFim() != null) entity.setDataHoraFim( api.getDataHoraFim() ); 
        if (api.getLinhasLidas() != null) entity.setLinhasLidas( api.getLinhasLidas() ); 
        if (api.getLinhasInseridas() != null) entity.setLinhasInseridas( api.getLinhasInseridas() ); 
        if (api.getLinhasAtualizadas() != null) entity.setLinhasAtualizadas( api.getLinhasAtualizadas() ); 
        if (api.getLinhasIguais() != null) entity.setLinhasIguais( api.getLinhasIguais() ); 
        if (api.getLinhasRejeitadas() != null) entity.setLinhasRejeitadas( api.getLinhasRejeitadas() ); 
        if (api.getResumoRejeicoes() != null) entity.setResumoRejeicoes( api.getResumoRejeicoes() ); 
        if (api.getId() != null) entity.setId( api.getId() ); 
        if (api.getDataCriacao() != null) entity.setDataCriacao( api.getDataCriacao() ); 
        if (api.getDataAtualizacao() != null) entity.setDataAtualizacao( api.getDataAtualizacao() ); 
        if (api.getIdUsuario() != null) entity.setIdUsuario( api.getIdUsuario() ); 

        return entity;
    }

    public ImportacaoMunicipioEntity setEntityDefaultValues(ImportacaoMunicipioEntity entity) {
        if ( entity == null ) {
            return null;
        }

        if (entity.getEstado() == null) {
            entity.setEstado("em_andamento");
        }
        if (entity.getLinhasLidas() == null) {
            entity.setLinhasLidas(0);
        }
        if (entity.getLinhasInseridas() == null) {
            entity.setLinhasInseridas(0);
        }
        if (entity.getLinhasAtualizadas() == null) {
            entity.setLinhasAtualizadas(0);
        }
        if (entity.getLinhasIguais() == null) {
            entity.setLinhasIguais(0);
        }
        if (entity.getLinhasRejeitadas() == null) {
            entity.setLinhasRejeitadas(0);
        }

        return entity;
    }

    public ImportacaoMunicipio setApiDefaultValues(ImportacaoMunicipio api) {
        if ( api == null ) {
            return null;
        }

        if (api.getEstado() == null) {
            api.setEstado("em_andamento");
        }
        if (api.getLinhasLidas() == null) {
            api.setLinhasLidas(0);
        }
        if (api.getLinhasInseridas() == null) {
            api.setLinhasInseridas(0);
        }
        if (api.getLinhasAtualizadas() == null) {
            api.setLinhasAtualizadas(0);
        }
        if (api.getLinhasIguais() == null) {
            api.setLinhasIguais(0);
        }
        if (api.getLinhasRejeitadas() == null) {
            api.setLinhasRejeitadas(0);
        }

        return api;
    }

}
