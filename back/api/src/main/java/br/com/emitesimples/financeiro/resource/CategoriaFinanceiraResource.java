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

import br.com.emitesimples.financeiro.domain.CategoriaFinanceira;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.financeiro.repository.CategoriaFinanceiraRepository;
import br.com.emitesimples.financeiro.mapper.CategoriaFinanceiraMapper;
import br.com.emitesimples.results.CountResult;

@Path("/CategoriaFinanceira")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoriaFinanceiraResource {

    @Inject
    CategoriaFinanceiraRepository repository;

    @Inject
    CategoriaFinanceiraMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de CategoriaFinanceira.
     */
    @GET
    public List<CategoriaFinanceira> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("tipo") String tipo,
        @QueryParam("dicas") String dicas,
        @QueryParam("ativo") String ativo,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String categoriaPaiNome,
        @QueryParam("categoriaPai.id") String categoriaPaiId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<CategoriaFinanceiraEntity> entityList = repository.findWithFilters(0, 100, sort, nome, tipo, dicas, ativo, id, dataCriacao, dataAtualizacao, categoriaPaiNome, categoriaPaiId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de CategoriaFinanceira.
     */
    @POST
    @Transactional
    public CategoriaFinanceira create(CategoriaFinanceira body) {
      try {
        CategoriaFinanceiraEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de CategoriaFinanceira.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nome") String nome,
        @QueryParam("tipo") String tipo,
        @QueryParam("dicas") String dicas,
        @QueryParam("ativo") String ativo,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String categoriaPaiNome,
        @QueryParam("categoriaPai.id") String categoriaPaiId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( nome, tipo, dicas, ativo, id, dataCriacao, dataAtualizacao, categoriaPaiNome, categoriaPaiId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId));
        return countResult;
    }

    /**
     * Lista os registros de CategoriaFinanceira por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<CategoriaFinanceira> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("tipo") String tipo,
        @QueryParam("dicas") String dicas,
        @QueryParam("ativo") String ativo,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String categoriaPaiNome,
        @QueryParam("categoriaPai.id") String categoriaPaiId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
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
        List<CategoriaFinanceiraEntity> entityList = repository.findWithFilters(from, limit, sort, nome, tipo, dicas, ativo, id, dataCriacao, dataAtualizacao, categoriaPaiNome, categoriaPaiId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de CategoriaFinanceira usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public CategoriaFinanceira getById(@PathParam("value") String value) {
      CategoriaFinanceiraEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No categoriaFinanceira found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de CategoriaFinanceira.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, CategoriaFinanceira body) {
        CategoriaFinanceiraEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No categoriaFinanceira found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de CategoriaFinanceira pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, CategoriaFinanceira body) {
        CategoriaFinanceiraEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No categoriaFinanceira found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de CategoriaFinanceira pelo ID.
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
