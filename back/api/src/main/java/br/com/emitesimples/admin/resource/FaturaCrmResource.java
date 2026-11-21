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
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.emitesimples.admin.domain.FaturaCrm;
import br.com.emitesimples.admin.entity.FaturaCrmEntity;
import br.com.emitesimples.admin.repository.FaturaCrmRepository;
import br.com.emitesimples.admin.mapper.FaturaCrmMapper;
import br.com.emitesimples.results.CountResult;

@Path("/FaturaCrm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FaturaCrmResource {

    @Inject
    FaturaCrmRepository repository;

    @Inject
    FaturaCrmMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de FaturaCrm.
     */
    @GET
    public List<FaturaCrm> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("crmFaturaId") String crmFaturaId,
        @QueryParam("numero") String numero,
        @QueryParam("valor") String valor,
        @QueryParam("dataEmissao") String dataEmissao,
        @QueryParam("dataVencimento") String dataVencimento,
        @QueryParam("dataPagamento") String dataPagamento,
        @QueryParam("estado") String estado,
        @QueryParam("linkFatura") String linkFatura,
        @QueryParam("dataSincronizacao") String dataSincronizacao,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("estado") String assinaturaEstado,
        @QueryParam("assinatura.id") String assinaturaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<FaturaCrmEntity> entityList = repository.findWithFilters(0, 100, sort, crmFaturaId, numero, valor, dataEmissao, dataVencimento, dataPagamento, estado, linkFatura, dataSincronizacao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, assinaturaEstado, assinaturaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de FaturaCrm.
     */
    @POST
    @Transactional
    public FaturaCrm create(FaturaCrm body) {
      try {
        FaturaCrmEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de FaturaCrm.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("crmFaturaId") String crmFaturaId,
        @QueryParam("numero") String numero,
        @QueryParam("valor") String valor,
        @QueryParam("dataEmissao") String dataEmissao,
        @QueryParam("dataVencimento") String dataVencimento,
        @QueryParam("dataPagamento") String dataPagamento,
        @QueryParam("estado") String estado,
        @QueryParam("linkFatura") String linkFatura,
        @QueryParam("dataSincronizacao") String dataSincronizacao,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("estado") String assinaturaEstado,
        @QueryParam("assinatura.id") String assinaturaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( crmFaturaId, numero, valor, dataEmissao, dataVencimento, dataPagamento, estado, linkFatura, dataSincronizacao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, assinaturaEstado, assinaturaId));
        return countResult;
    }

    /**
     * Lista os registros de FaturaCrm por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<FaturaCrm> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("crmFaturaId") String crmFaturaId,
        @QueryParam("numero") String numero,
        @QueryParam("valor") String valor,
        @QueryParam("dataEmissao") String dataEmissao,
        @QueryParam("dataVencimento") String dataVencimento,
        @QueryParam("dataPagamento") String dataPagamento,
        @QueryParam("estado") String estado,
        @QueryParam("linkFatura") String linkFatura,
        @QueryParam("dataSincronizacao") String dataSincronizacao,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("estado") String assinaturaEstado,
        @QueryParam("assinatura.id") String assinaturaId
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
        List<FaturaCrmEntity> entityList = repository.findWithFilters(from, limit, sort, crmFaturaId, numero, valor, dataEmissao, dataVencimento, dataPagamento, estado, linkFatura, dataSincronizacao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, assinaturaEstado, assinaturaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de FaturaCrm usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public FaturaCrm getById(@PathParam("value") String value) {
      FaturaCrmEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No faturaCrm found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de FaturaCrm.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, FaturaCrm body) {
        FaturaCrmEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No faturaCrm found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de FaturaCrm pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, FaturaCrm body) {
        FaturaCrmEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No faturaCrm found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de FaturaCrm pelo ID.
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
