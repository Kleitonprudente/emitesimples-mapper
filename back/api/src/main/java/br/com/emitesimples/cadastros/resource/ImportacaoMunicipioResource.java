package br.com.emitesimples.cadastros.resource;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.PersistenceException;
import org.eclipse.microprofile.jwt.JsonWebToken;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.panache.common.Sort;

import br.com.emitesimples.exceptions.InvalidInputException;
import br.com.emitesimples.exceptions.NotFoundException;
import java.time.LocalDateTime;

import br.com.emitesimples.cadastros.domain.ImportacaoMunicipio;
import br.com.emitesimples.cadastros.entity.ImportacaoMunicipioEntity;
import br.com.emitesimples.cadastros.repository.ImportacaoMunicipioRepository;
import br.com.emitesimples.cadastros.mapper.ImportacaoMunicipioMapper;
import br.com.emitesimples.results.CountResult;

@Path("/ImportacaoMunicipio")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportacaoMunicipioResource {

    @Inject
    ImportacaoMunicipioRepository repository;

    @Inject
    ImportacaoMunicipioMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de ImportacaoMunicipio.
     */
    @GET
    public List<ImportacaoMunicipio> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nomeArquivo") String nomeArquivo,
        @QueryParam("codificacaoDetectada") String codificacaoDetectada,
        @QueryParam("separadorDetectado") String separadorDetectado,
        @QueryParam("estado") String estado,
        @QueryParam("dataHoraInicio") String dataHoraInicio,
        @QueryParam("dataHoraFim") String dataHoraFim,
        @QueryParam("linhasLidas") String linhasLidas,
        @QueryParam("linhasInseridas") String linhasInseridas,
        @QueryParam("linhasAtualizadas") String linhasAtualizadas,
        @QueryParam("linhasIguais") String linhasIguais,
        @QueryParam("linhasRejeitadas") String linhasRejeitadas,
        @QueryParam("resumoRejeicoes") String resumoRejeicoes,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String usuarioNome,
        @QueryParam("email") String usuarioEmail,
        @QueryParam("usuario.id") String usuarioId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<ImportacaoMunicipioEntity> entityList = repository.findWithFilters(0, 100, sort, nomeArquivo, codificacaoDetectada, separadorDetectado, estado, dataHoraInicio, dataHoraFim, linhasLidas, linhasInseridas, linhasAtualizadas, linhasIguais, linhasRejeitadas, resumoRejeicoes, id, dataCriacao, dataAtualizacao, usuarioNome, usuarioEmail, usuarioId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de ImportacaoMunicipio.
     */
    @POST
    @Transactional
    public ImportacaoMunicipio create(ImportacaoMunicipio body) {
      try {
        ImportacaoMunicipioEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de ImportacaoMunicipio.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nomeArquivo") String nomeArquivo,
        @QueryParam("codificacaoDetectada") String codificacaoDetectada,
        @QueryParam("separadorDetectado") String separadorDetectado,
        @QueryParam("estado") String estado,
        @QueryParam("dataHoraInicio") String dataHoraInicio,
        @QueryParam("dataHoraFim") String dataHoraFim,
        @QueryParam("linhasLidas") String linhasLidas,
        @QueryParam("linhasInseridas") String linhasInseridas,
        @QueryParam("linhasAtualizadas") String linhasAtualizadas,
        @QueryParam("linhasIguais") String linhasIguais,
        @QueryParam("linhasRejeitadas") String linhasRejeitadas,
        @QueryParam("resumoRejeicoes") String resumoRejeicoes,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String usuarioNome,
        @QueryParam("email") String usuarioEmail,
        @QueryParam("usuario.id") String usuarioId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( nomeArquivo, codificacaoDetectada, separadorDetectado, estado, dataHoraInicio, dataHoraFim, linhasLidas, linhasInseridas, linhasAtualizadas, linhasIguais, linhasRejeitadas, resumoRejeicoes, id, dataCriacao, dataAtualizacao, usuarioNome, usuarioEmail, usuarioId));
        return countResult;
    }

    /**
     * Lista os registros de ImportacaoMunicipio por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<ImportacaoMunicipio> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nomeArquivo") String nomeArquivo,
        @QueryParam("codificacaoDetectada") String codificacaoDetectada,
        @QueryParam("separadorDetectado") String separadorDetectado,
        @QueryParam("estado") String estado,
        @QueryParam("dataHoraInicio") String dataHoraInicio,
        @QueryParam("dataHoraFim") String dataHoraFim,
        @QueryParam("linhasLidas") String linhasLidas,
        @QueryParam("linhasInseridas") String linhasInseridas,
        @QueryParam("linhasAtualizadas") String linhasAtualizadas,
        @QueryParam("linhasIguais") String linhasIguais,
        @QueryParam("linhasRejeitadas") String linhasRejeitadas,
        @QueryParam("resumoRejeicoes") String resumoRejeicoes,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String usuarioNome,
        @QueryParam("email") String usuarioEmail,
        @QueryParam("usuario.id") String usuarioId
    ) {

        if (from > to) {
            throw new InvalidInputException("The parameter from with value " + from + " must be lower than " + to + ".");
        }
        int limit = to - from + 1;
        if (limit > 100) {
            throw new InvalidInputException("Maximum of 100 entities are allowed in listRange operation.");
        }
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<ImportacaoMunicipioEntity> entityList = repository.findWithFilters(from, limit, sort, nomeArquivo, codificacaoDetectada, separadorDetectado, estado, dataHoraInicio, dataHoraFim, linhasLidas, linhasInseridas, linhasAtualizadas, linhasIguais, linhasRejeitadas, resumoRejeicoes, id, dataCriacao, dataAtualizacao, usuarioNome, usuarioEmail, usuarioId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de ImportacaoMunicipio usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public ImportacaoMunicipio getById(@PathParam("value") String value) {
      ImportacaoMunicipioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No importacaoMunicipio found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de ImportacaoMunicipio.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, ImportacaoMunicipio body) {
        ImportacaoMunicipioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No importacaoMunicipio found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de ImportacaoMunicipio pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, ImportacaoMunicipio body) {
        ImportacaoMunicipioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No importacaoMunicipio found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de ImportacaoMunicipio pelo ID.
     */
    @DELETE
    @Path("/id/{value}")
    @Transactional
    public void deleteById(@PathParam("value") String value) {
        try {
            repository.deleteById(value);
        } catch (PersistenceException pe) {
            throw new InvalidInputException("Unable to delete profile with ID: " + value, pe.getCause());
        }
    }

}
