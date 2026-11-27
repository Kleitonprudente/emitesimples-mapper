package br.com.emitesimples.financeiro.resource;

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

import br.com.emitesimples.financeiro.domain.Banco;
import br.com.emitesimples.financeiro.entity.BancoEntity;
import br.com.emitesimples.financeiro.repository.BancoRepository;
import br.com.emitesimples.financeiro.mapper.BancoMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Banco")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BancoResource {

    @Inject
    BancoRepository repository;

    @Inject
    BancoMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Banco.
     */
    @GET
    public List<Banco> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("codigo") String codigo,
        @QueryParam("ispb") String ispb,
        @QueryParam("nome") String nome,
        @QueryParam("nomeExtenso") String nomeExtenso,
        @QueryParam("participaCompe") String participaCompe,
        @QueryParam("integradoGateway") String integradoGateway,
        @QueryParam("urlPassoAPasso") String urlPassoAPasso,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<BancoEntity> entityList = repository.findWithFilters(0, 100, sort, codigo, ispb, nome, nomeExtenso, participaCompe, integradoGateway, urlPassoAPasso, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Banco.
     */
    @POST
    @Transactional
    public Banco create(Banco body) {
      try {
        BancoEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Banco.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("codigo") String codigo,
        @QueryParam("ispb") String ispb,
        @QueryParam("nome") String nome,
        @QueryParam("nomeExtenso") String nomeExtenso,
        @QueryParam("participaCompe") String participaCompe,
        @QueryParam("integradoGateway") String integradoGateway,
        @QueryParam("urlPassoAPasso") String urlPassoAPasso,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( codigo, ispb, nome, nomeExtenso, participaCompe, integradoGateway, urlPassoAPasso, id, dataCriacao, dataAtualizacao));
        return countResult;
    }

    /**
     * Lista os registros de Banco por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Banco> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("codigo") String codigo,
        @QueryParam("ispb") String ispb,
        @QueryParam("nome") String nome,
        @QueryParam("nomeExtenso") String nomeExtenso,
        @QueryParam("participaCompe") String participaCompe,
        @QueryParam("integradoGateway") String integradoGateway,
        @QueryParam("urlPassoAPasso") String urlPassoAPasso,
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
        List<BancoEntity> entityList = repository.findWithFilters(from, limit, sort, codigo, ispb, nome, nomeExtenso, participaCompe, integradoGateway, urlPassoAPasso, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Banco usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Banco getById(@PathParam("value") String value) {
      BancoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No banco found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Banco.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Banco body) {
        BancoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No banco found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Banco pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Banco body) {
        BancoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No banco found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Banco pelo ID.
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
