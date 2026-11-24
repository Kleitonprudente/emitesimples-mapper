package br.com.emitesimples.nfse.resource;

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

import br.com.emitesimples.nfse.domain.Cancelamento;
import br.com.emitesimples.nfse.entity.CancelamentoEntity;
import br.com.emitesimples.nfse.repository.CancelamentoRepository;
import br.com.emitesimples.nfse.mapper.CancelamentoMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Cancelamento")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CancelamentoResource {

    @Inject
    CancelamentoRepository repository;

    @Inject
    CancelamentoMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Cancelamento.
     */
    @GET
    public List<Cancelamento> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("codigoMotivo") String codigoMotivo,
        @QueryParam("motivo") String motivo,
        @QueryParam("dataHoraAprovacao") String dataHoraAprovacao,
        @QueryParam("dataHoraDescarte") String dataHoraDescarte,
        @QueryParam("dataHoraRecusa") String dataHoraRecusa,
        @QueryParam("estado") String estado,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("dataHoraEvento") String dataHoraEvento,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String preparadoPorUsuarioNome,
        @QueryParam("email") String preparadoPorUsuarioEmail,
        @QueryParam("preparadoPorUsuario.id") String preparadoPorUsuarioId,
        @QueryParam("nome") String preparadoPorClienteNome,
        @QueryParam("clientId") String preparadoPorClienteClientId,
        @QueryParam("preparadoPorCliente.id") String preparadoPorClienteId,
        @QueryParam("nome") String aprovadoPorNome,
        @QueryParam("email") String aprovadoPorEmail,
        @QueryParam("aprovadoPor.id") String aprovadoPorId,
        @QueryParam("nome") String descartadoPorNome,
        @QueryParam("email") String descartadoPorEmail,
        @QueryParam("descartadoPor.id") String descartadoPorId,
        @QueryParam("numero") String notaNumero,
        @QueryParam("estado") String notaEstado,
        @QueryParam("chave") String notaChave,
        @QueryParam("ambiente") String notaAmbiente,
        @QueryParam("nota.id") String notaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<CancelamentoEntity> entityList = repository.findWithFilters(0, 100, sort, codigoMotivo, motivo, dataHoraAprovacao, dataHoraDescarte, dataHoraRecusa, estado, codigoRetorno, mensagemRetorno, dataHoraEvento, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, preparadoPorUsuarioNome, preparadoPorUsuarioEmail, preparadoPorUsuarioId, preparadoPorClienteNome, preparadoPorClienteClientId, preparadoPorClienteId, aprovadoPorNome, aprovadoPorEmail, aprovadoPorId, descartadoPorNome, descartadoPorEmail, descartadoPorId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Cancelamento.
     */
    @POST
    @Transactional
    public Cancelamento create(Cancelamento body) {
      try {
        CancelamentoEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Cancelamento.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("codigoMotivo") String codigoMotivo,
        @QueryParam("motivo") String motivo,
        @QueryParam("dataHoraAprovacao") String dataHoraAprovacao,
        @QueryParam("dataHoraDescarte") String dataHoraDescarte,
        @QueryParam("dataHoraRecusa") String dataHoraRecusa,
        @QueryParam("estado") String estado,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("dataHoraEvento") String dataHoraEvento,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String preparadoPorUsuarioNome,
        @QueryParam("email") String preparadoPorUsuarioEmail,
        @QueryParam("preparadoPorUsuario.id") String preparadoPorUsuarioId,
        @QueryParam("nome") String preparadoPorClienteNome,
        @QueryParam("clientId") String preparadoPorClienteClientId,
        @QueryParam("preparadoPorCliente.id") String preparadoPorClienteId,
        @QueryParam("nome") String aprovadoPorNome,
        @QueryParam("email") String aprovadoPorEmail,
        @QueryParam("aprovadoPor.id") String aprovadoPorId,
        @QueryParam("nome") String descartadoPorNome,
        @QueryParam("email") String descartadoPorEmail,
        @QueryParam("descartadoPor.id") String descartadoPorId,
        @QueryParam("numero") String notaNumero,
        @QueryParam("estado") String notaEstado,
        @QueryParam("chave") String notaChave,
        @QueryParam("ambiente") String notaAmbiente,
        @QueryParam("nota.id") String notaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( codigoMotivo, motivo, dataHoraAprovacao, dataHoraDescarte, dataHoraRecusa, estado, codigoRetorno, mensagemRetorno, dataHoraEvento, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, preparadoPorUsuarioNome, preparadoPorUsuarioEmail, preparadoPorUsuarioId, preparadoPorClienteNome, preparadoPorClienteClientId, preparadoPorClienteId, aprovadoPorNome, aprovadoPorEmail, aprovadoPorId, descartadoPorNome, descartadoPorEmail, descartadoPorId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId));
        return countResult;
    }

    /**
     * Lista os registros de Cancelamento por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Cancelamento> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("codigoMotivo") String codigoMotivo,
        @QueryParam("motivo") String motivo,
        @QueryParam("dataHoraAprovacao") String dataHoraAprovacao,
        @QueryParam("dataHoraDescarte") String dataHoraDescarte,
        @QueryParam("dataHoraRecusa") String dataHoraRecusa,
        @QueryParam("estado") String estado,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("dataHoraEvento") String dataHoraEvento,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("nome") String preparadoPorUsuarioNome,
        @QueryParam("email") String preparadoPorUsuarioEmail,
        @QueryParam("preparadoPorUsuario.id") String preparadoPorUsuarioId,
        @QueryParam("nome") String preparadoPorClienteNome,
        @QueryParam("clientId") String preparadoPorClienteClientId,
        @QueryParam("preparadoPorCliente.id") String preparadoPorClienteId,
        @QueryParam("nome") String aprovadoPorNome,
        @QueryParam("email") String aprovadoPorEmail,
        @QueryParam("aprovadoPor.id") String aprovadoPorId,
        @QueryParam("nome") String descartadoPorNome,
        @QueryParam("email") String descartadoPorEmail,
        @QueryParam("descartadoPor.id") String descartadoPorId,
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
        List<CancelamentoEntity> entityList = repository.findWithFilters(from, limit, sort, codigoMotivo, motivo, dataHoraAprovacao, dataHoraDescarte, dataHoraRecusa, estado, codigoRetorno, mensagemRetorno, dataHoraEvento, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, preparadoPorUsuarioNome, preparadoPorUsuarioEmail, preparadoPorUsuarioId, preparadoPorClienteNome, preparadoPorClienteClientId, preparadoPorClienteId, aprovadoPorNome, aprovadoPorEmail, aprovadoPorId, descartadoPorNome, descartadoPorEmail, descartadoPorId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Cancelamento usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Cancelamento getById(@PathParam("value") String value) {
      CancelamentoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No cancelamento found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Cancelamento.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Cancelamento body) {
        CancelamentoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No cancelamento found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Cancelamento pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Cancelamento body) {
        CancelamentoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No cancelamento found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Cancelamento pelo ID.
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
