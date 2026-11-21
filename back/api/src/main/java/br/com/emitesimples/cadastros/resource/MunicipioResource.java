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

import br.com.emitesimples.cadastros.domain.Municipio;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.cadastros.repository.MunicipioRepository;
import br.com.emitesimples.cadastros.mapper.MunicipioMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Municipio")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MunicipioResource {

    @Inject
    MunicipioRepository repository;

    @Inject
    MunicipioMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Municipio.
     */
    @GET
    public List<Municipio> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("codigoIbge") String codigoIbge,
        @QueryParam("nome") String nome,
        @QueryParam("uf") String uf,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<MunicipioEntity> entityList = repository.findWithFilters(0, 100, sort, codigoIbge, nome, uf, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Municipio.
     */
    @POST
    @Transactional
    public Municipio create(Municipio body) {
      try {
        MunicipioEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Municipio.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("codigoIbge") String codigoIbge,
        @QueryParam("nome") String nome,
        @QueryParam("uf") String uf,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( codigoIbge, nome, uf, id, dataCriacao, dataAtualizacao));
        return countResult;
    }

    /**
     * Lista os registros de Municipio por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Municipio> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("codigoIbge") String codigoIbge,
        @QueryParam("nome") String nome,
        @QueryParam("uf") String uf,
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
        List<MunicipioEntity> entityList = repository.findWithFilters(from, limit, sort, codigoIbge, nome, uf, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Municipio usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Municipio getById(@PathParam("value") String value) {
      MunicipioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No municipio found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Municipio.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Municipio body) {
        MunicipioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No municipio found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Municipio pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Municipio body) {
        MunicipioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No municipio found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Municipio pelo ID.
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
