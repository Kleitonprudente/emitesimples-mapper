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

import br.com.emitesimples.integranotas.domain.EventoWebhook;
import br.com.emitesimples.integranotas.entity.EventoWebhookEntity;
import br.com.emitesimples.integranotas.repository.EventoWebhookRepository;
import br.com.emitesimples.integranotas.mapper.EventoWebhookMapper;
import br.com.emitesimples.results.CountResult;

@Path("/EventoWebhook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EventoWebhookResource {

    @Inject
    EventoWebhookRepository repository;

    @Inject
    EventoWebhookMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de EventoWebhook.
     */
    @GET
    public List<EventoWebhook> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("origem") String origem,
        @QueryParam("cnpjCpf") String cnpjCpf,
        @QueryParam("tipo") String tipo,
        @QueryParam("chave") String chave,
        @QueryParam("sucesso") String sucesso,
        @QueryParam("codigo") String codigo,
        @QueryParam("mensagem") String mensagem,
        @QueryParam("identificacao") String identificacao,
        @QueryParam("dataRecebimento") String dataRecebimento,
        @QueryParam("resultadoAssinatura") String resultadoAssinatura,
        @QueryParam("estadoProcessamento") String estadoProcessamento,
        @QueryParam("dataProcessamento") String dataProcessamento,
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
        List<EventoWebhookEntity> entityList = repository.findWithFilters(0, 100, sort, origem, cnpjCpf, tipo, chave, sucesso, codigo, mensagem, identificacao, dataRecebimento, resultadoAssinatura, estadoProcessamento, dataProcessamento, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de EventoWebhook.
     */
    @POST
    @Transactional
    public EventoWebhook create(EventoWebhook body) {
      try {
        EventoWebhookEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de EventoWebhook.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("origem") String origem,
        @QueryParam("cnpjCpf") String cnpjCpf,
        @QueryParam("tipo") String tipo,
        @QueryParam("chave") String chave,
        @QueryParam("sucesso") String sucesso,
        @QueryParam("codigo") String codigo,
        @QueryParam("mensagem") String mensagem,
        @QueryParam("identificacao") String identificacao,
        @QueryParam("dataRecebimento") String dataRecebimento,
        @QueryParam("resultadoAssinatura") String resultadoAssinatura,
        @QueryParam("estadoProcessamento") String estadoProcessamento,
        @QueryParam("dataProcessamento") String dataProcessamento,
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
        countResult.setCount(repository.countWithFilters( origem, cnpjCpf, tipo, chave, sucesso, codigo, mensagem, identificacao, dataRecebimento, resultadoAssinatura, estadoProcessamento, dataProcessamento, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId));
        return countResult;
    }

    /**
     * Lista os registros de EventoWebhook por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<EventoWebhook> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("origem") String origem,
        @QueryParam("cnpjCpf") String cnpjCpf,
        @QueryParam("tipo") String tipo,
        @QueryParam("chave") String chave,
        @QueryParam("sucesso") String sucesso,
        @QueryParam("codigo") String codigo,
        @QueryParam("mensagem") String mensagem,
        @QueryParam("identificacao") String identificacao,
        @QueryParam("dataRecebimento") String dataRecebimento,
        @QueryParam("resultadoAssinatura") String resultadoAssinatura,
        @QueryParam("estadoProcessamento") String estadoProcessamento,
        @QueryParam("dataProcessamento") String dataProcessamento,
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
        List<EventoWebhookEntity> entityList = repository.findWithFilters(from, limit, sort, origem, cnpjCpf, tipo, chave, sucesso, codigo, mensagem, identificacao, dataRecebimento, resultadoAssinatura, estadoProcessamento, dataProcessamento, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, notaNumero, notaEstado, notaChave, notaAmbiente, notaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de EventoWebhook usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public EventoWebhook getById(@PathParam("value") String value) {
      EventoWebhookEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No eventoWebhook found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de EventoWebhook.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, EventoWebhook body) {
        EventoWebhookEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No eventoWebhook found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de EventoWebhook pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, EventoWebhook body) {
        EventoWebhookEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No eventoWebhook found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de EventoWebhook pelo ID.
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
