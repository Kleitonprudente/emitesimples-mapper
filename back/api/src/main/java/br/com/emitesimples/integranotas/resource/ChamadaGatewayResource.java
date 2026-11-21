package br.com.emitesimples.integranotas.resource;

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

import br.com.emitesimples.integranotas.domain.ChamadaGateway;
import br.com.emitesimples.integranotas.entity.ChamadaGatewayEntity;
import br.com.emitesimples.integranotas.repository.ChamadaGatewayRepository;
import br.com.emitesimples.integranotas.mapper.ChamadaGatewayMapper;
import br.com.emitesimples.results.CountResult;

@Path("/ChamadaGateway")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChamadaGatewayResource {

    @Inject
    ChamadaGatewayRepository repository;

    @Inject
    ChamadaGatewayMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de ChamadaGateway.
     */
    @GET
    public List<ChamadaGateway> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("operacao") String operacao,
        @QueryParam("metodoHttp") String metodoHttp,
        @QueryParam("rota") String rota,
        @QueryParam("versaoApi") String versaoApi,
        @QueryParam("codigoHttp") String codigoHttp,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("duracaoMs") String duracaoMs,
        @QueryParam("dataChamada") String dataChamada,
        @QueryParam("chave") String chave,
        @QueryParam("identificacao") String identificacao,
        @QueryParam("resumoEnvio") String resumoEnvio,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("numero") String notaNumero,
        @QueryParam("estado") String notaEstado,
        @QueryParam("chave") String notaChave,
        @QueryParam("ambiente") String notaAmbiente,
        @QueryParam("nota.id") String notaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<ChamadaGatewayEntity> entityList = repository.findWithFilters(0, 100, sort, operacao, metodoHttp, rota, versaoApi, codigoHttp, codigoRetorno, mensagemRetorno, duracaoMs, dataChamada, chave, identificacao, resumoEnvio, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de ChamadaGateway.
     */
    @POST
    @Transactional
    public ChamadaGateway create(ChamadaGateway body) {
      try {
        ChamadaGatewayEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de ChamadaGateway.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("operacao") String operacao,
        @QueryParam("metodoHttp") String metodoHttp,
        @QueryParam("rota") String rota,
        @QueryParam("versaoApi") String versaoApi,
        @QueryParam("codigoHttp") String codigoHttp,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("duracaoMs") String duracaoMs,
        @QueryParam("dataChamada") String dataChamada,
        @QueryParam("chave") String chave,
        @QueryParam("identificacao") String identificacao,
        @QueryParam("resumoEnvio") String resumoEnvio,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("numero") String notaNumero,
        @QueryParam("estado") String notaEstado,
        @QueryParam("chave") String notaChave,
        @QueryParam("ambiente") String notaAmbiente,
        @QueryParam("nota.id") String notaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( operacao, metodoHttp, rota, versaoApi, codigoHttp, codigoRetorno, mensagemRetorno, duracaoMs, dataChamada, chave, identificacao, resumoEnvio, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId));
        return countResult;
    }

    /**
     * Lista os registros de ChamadaGateway por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<ChamadaGateway> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("operacao") String operacao,
        @QueryParam("metodoHttp") String metodoHttp,
        @QueryParam("rota") String rota,
        @QueryParam("versaoApi") String versaoApi,
        @QueryParam("codigoHttp") String codigoHttp,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("duracaoMs") String duracaoMs,
        @QueryParam("dataChamada") String dataChamada,
        @QueryParam("chave") String chave,
        @QueryParam("identificacao") String identificacao,
        @QueryParam("resumoEnvio") String resumoEnvio,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("numero") String notaNumero,
        @QueryParam("estado") String notaEstado,
        @QueryParam("chave") String notaChave,
        @QueryParam("ambiente") String notaAmbiente,
        @QueryParam("nota.id") String notaId
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
        List<ChamadaGatewayEntity> entityList = repository.findWithFilters(from, limit, sort, operacao, metodoHttp, rota, versaoApi, codigoHttp, codigoRetorno, mensagemRetorno, duracaoMs, dataChamada, chave, identificacao, resumoEnvio, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de ChamadaGateway usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public ChamadaGateway getById(@PathParam("value") String value) {
      ChamadaGatewayEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No chamadaGateway found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de ChamadaGateway.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, ChamadaGateway body) {
        ChamadaGatewayEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No chamadaGateway found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de ChamadaGateway pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, ChamadaGateway body) {
        ChamadaGatewayEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No chamadaGateway found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de ChamadaGateway pelo ID.
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
