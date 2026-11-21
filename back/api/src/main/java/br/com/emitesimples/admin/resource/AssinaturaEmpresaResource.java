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

import br.com.emitesimples.admin.domain.AssinaturaEmpresa;
import br.com.emitesimples.admin.entity.AssinaturaEmpresaEntity;
import br.com.emitesimples.admin.repository.AssinaturaEmpresaRepository;
import br.com.emitesimples.admin.mapper.AssinaturaEmpresaMapper;
import br.com.emitesimples.results.CountResult;

@Path("/AssinaturaEmpresa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AssinaturaEmpresaResource {

    @Inject
    AssinaturaEmpresaRepository repository;

    @Inject
    AssinaturaEmpresaMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de AssinaturaEmpresa.
     */
    @GET
    public List<AssinaturaEmpresa> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("estado") String estado,
        @QueryParam("dataInicio") String dataInicio,
        @QueryParam("dataFim") String dataFim,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String planoNome,
        @QueryParam("plano.id") String planoId,
        @QueryParam("nome") String vinculadaPorNome,
        @QueryParam("email") String vinculadaPorEmail,
        @QueryParam("vinculadaPor.id") String vinculadaPorId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<AssinaturaEmpresaEntity> entityList = repository.findWithFilters(0, 100, sort, estado, dataInicio, dataFim, referenciaCrm, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, planoNome, planoId, vinculadaPorNome, vinculadaPorEmail, vinculadaPorId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de AssinaturaEmpresa.
     */
    @POST
    @Transactional
    public AssinaturaEmpresa create(AssinaturaEmpresa body) {
      try {
        AssinaturaEmpresaEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de AssinaturaEmpresa.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("estado") String estado,
        @QueryParam("dataInicio") String dataInicio,
        @QueryParam("dataFim") String dataFim,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String planoNome,
        @QueryParam("plano.id") String planoId,
        @QueryParam("nome") String vinculadaPorNome,
        @QueryParam("email") String vinculadaPorEmail,
        @QueryParam("vinculadaPor.id") String vinculadaPorId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( estado, dataInicio, dataFim, referenciaCrm, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, planoNome, planoId, vinculadaPorNome, vinculadaPorEmail, vinculadaPorId));
        return countResult;
    }

    /**
     * Lista os registros de AssinaturaEmpresa por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<AssinaturaEmpresa> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("estado") String estado,
        @QueryParam("dataInicio") String dataInicio,
        @QueryParam("dataFim") String dataFim,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String planoNome,
        @QueryParam("plano.id") String planoId,
        @QueryParam("nome") String vinculadaPorNome,
        @QueryParam("email") String vinculadaPorEmail,
        @QueryParam("vinculadaPor.id") String vinculadaPorId
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
        List<AssinaturaEmpresaEntity> entityList = repository.findWithFilters(from, limit, sort, estado, dataInicio, dataFim, referenciaCrm, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, planoNome, planoId, vinculadaPorNome, vinculadaPorEmail, vinculadaPorId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de AssinaturaEmpresa usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public AssinaturaEmpresa getById(@PathParam("value") String value) {
      AssinaturaEmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No assinaturaEmpresa found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de AssinaturaEmpresa.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, AssinaturaEmpresa body) {
        AssinaturaEmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No assinaturaEmpresa found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de AssinaturaEmpresa pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, AssinaturaEmpresa body) {
        AssinaturaEmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No assinaturaEmpresa found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de AssinaturaEmpresa pelo ID.
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
