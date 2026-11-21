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

import br.com.emitesimples.admin.domain.HistoricoSituacaoEmpresa;
import br.com.emitesimples.admin.entity.HistoricoSituacaoEmpresaEntity;
import br.com.emitesimples.admin.repository.HistoricoSituacaoEmpresaRepository;
import br.com.emitesimples.admin.mapper.HistoricoSituacaoEmpresaMapper;
import br.com.emitesimples.results.CountResult;

@Path("/HistoricoSituacaoEmpresa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HistoricoSituacaoEmpresaResource {

    @Inject
    HistoricoSituacaoEmpresaRepository repository;

    @Inject
    HistoricoSituacaoEmpresaMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de HistoricoSituacaoEmpresa.
     */
    @GET
    public List<HistoricoSituacaoEmpresa> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("situacaoAnterior") String situacaoAnterior,
        @QueryParam("situacaoNova") String situacaoNova,
        @QueryParam("motivo") String motivo,
        @QueryParam("origem") String origem,
        @QueryParam("dataHora") String dataHora,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String responsavelNome,
        @QueryParam("email") String responsavelEmail,
        @QueryParam("responsavel.id") String responsavelId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<HistoricoSituacaoEmpresaEntity> entityList = repository.findWithFilters(0, 100, sort, situacaoAnterior, situacaoNova, motivo, origem, dataHora, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, responsavelNome, responsavelEmail, responsavelId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de HistoricoSituacaoEmpresa.
     */
    @POST
    @Transactional
    public HistoricoSituacaoEmpresa create(HistoricoSituacaoEmpresa body) {
      try {
        HistoricoSituacaoEmpresaEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de HistoricoSituacaoEmpresa.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("situacaoAnterior") String situacaoAnterior,
        @QueryParam("situacaoNova") String situacaoNova,
        @QueryParam("motivo") String motivo,
        @QueryParam("origem") String origem,
        @QueryParam("dataHora") String dataHora,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String responsavelNome,
        @QueryParam("email") String responsavelEmail,
        @QueryParam("responsavel.id") String responsavelId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( situacaoAnterior, situacaoNova, motivo, origem, dataHora, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, responsavelNome, responsavelEmail, responsavelId));
        return countResult;
    }

    /**
     * Lista os registros de HistoricoSituacaoEmpresa por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<HistoricoSituacaoEmpresa> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("situacaoAnterior") String situacaoAnterior,
        @QueryParam("situacaoNova") String situacaoNova,
        @QueryParam("motivo") String motivo,
        @QueryParam("origem") String origem,
        @QueryParam("dataHora") String dataHora,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String responsavelNome,
        @QueryParam("email") String responsavelEmail,
        @QueryParam("responsavel.id") String responsavelId
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
        List<HistoricoSituacaoEmpresaEntity> entityList = repository.findWithFilters(from, limit, sort, situacaoAnterior, situacaoNova, motivo, origem, dataHora, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, responsavelNome, responsavelEmail, responsavelId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de HistoricoSituacaoEmpresa usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public HistoricoSituacaoEmpresa getById(@PathParam("value") String value) {
      HistoricoSituacaoEmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No historicoSituacaoEmpresa found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de HistoricoSituacaoEmpresa.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, HistoricoSituacaoEmpresa body) {
        HistoricoSituacaoEmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No historicoSituacaoEmpresa found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de HistoricoSituacaoEmpresa pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, HistoricoSituacaoEmpresa body) {
        HistoricoSituacaoEmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No historicoSituacaoEmpresa found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de HistoricoSituacaoEmpresa pelo ID.
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
