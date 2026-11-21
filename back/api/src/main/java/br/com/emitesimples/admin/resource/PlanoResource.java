package br.com.emitesimples.admin.resource;

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

import br.com.emitesimples.admin.domain.Plano;
import br.com.emitesimples.admin.entity.PlanoEntity;
import br.com.emitesimples.admin.repository.PlanoRepository;
import br.com.emitesimples.admin.mapper.PlanoMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Plano")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlanoResource {

    @Inject
    PlanoRepository repository;

    @Inject
    PlanoMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Plano.
     */
    @GET
    public List<Plano> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("descricao") String descricao,
        @QueryParam("valor") String valor,
        @QueryParam("periodicidade") String periodicidade,
        @QueryParam("ativo") String ativo,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<PlanoEntity> entityList = repository.findWithFilters(0, 100, sort, nome, descricao, valor, periodicidade, ativo, referenciaCrm, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Plano.
     */
    @POST
    @Transactional
    public Plano create(Plano body) {
      try {
        PlanoEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Plano.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nome") String nome,
        @QueryParam("descricao") String descricao,
        @QueryParam("valor") String valor,
        @QueryParam("periodicidade") String periodicidade,
        @QueryParam("ativo") String ativo,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( nome, descricao, valor, periodicidade, ativo, referenciaCrm, id, dataCriacao, dataAtualizacao));
        return countResult;
    }

    /**
     * Lista os registros de Plano por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Plano> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("descricao") String descricao,
        @QueryParam("valor") String valor,
        @QueryParam("periodicidade") String periodicidade,
        @QueryParam("ativo") String ativo,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
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
        List<PlanoEntity> entityList = repository.findWithFilters(from, limit, sort, nome, descricao, valor, periodicidade, ativo, referenciaCrm, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Plano usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Plano getById(@PathParam("value") String value) {
      PlanoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No plano found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Plano.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Plano body) {
        PlanoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No plano found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Plano pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Plano body) {
        PlanoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No plano found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Plano pelo ID.
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
