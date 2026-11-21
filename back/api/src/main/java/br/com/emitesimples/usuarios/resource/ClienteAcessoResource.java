package br.com.emitesimples.usuarios.resource;

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

import br.com.emitesimples.usuarios.domain.ClienteAcesso;
import br.com.emitesimples.usuarios.entity.ClienteAcessoEntity;
import br.com.emitesimples.usuarios.repository.ClienteAcessoRepository;
import br.com.emitesimples.usuarios.mapper.ClienteAcessoMapper;
import br.com.emitesimples.results.CountResult;

@Path("/ClienteAcesso")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClienteAcessoResource {

    @Inject
    ClienteAcessoRepository repository;

    @Inject
    ClienteAcessoMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de ClienteAcesso.
     */
    @GET
    public List<ClienteAcesso> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("clientId") String clientId,
        @QueryParam("scopes") String scopes,
        @QueryParam("ativo") String ativo,
        @QueryParam("dataUltimoUso") String dataUltimoUso,
        @QueryParam("dataExpiracao") String dataExpiracao,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<ClienteAcessoEntity> entityList = repository.findWithFilters(0, 100, sort, nome, clientId, scopes, ativo, dataUltimoUso, dataExpiracao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de ClienteAcesso.
     */
    @POST
    @Transactional
    public ClienteAcesso create(ClienteAcesso body) {
      try {
        ClienteAcessoEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de ClienteAcesso.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nome") String nome,
        @QueryParam("clientId") String clientId,
        @QueryParam("scopes") String scopes,
        @QueryParam("ativo") String ativo,
        @QueryParam("dataUltimoUso") String dataUltimoUso,
        @QueryParam("dataExpiracao") String dataExpiracao,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( nome, clientId, scopes, ativo, dataUltimoUso, dataExpiracao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId));
        return countResult;
    }

    /**
     * Lista os registros de ClienteAcesso por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<ClienteAcesso> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("clientId") String clientId,
        @QueryParam("scopes") String scopes,
        @QueryParam("ativo") String ativo,
        @QueryParam("dataUltimoUso") String dataUltimoUso,
        @QueryParam("dataExpiracao") String dataExpiracao,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
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
        List<ClienteAcessoEntity> entityList = repository.findWithFilters(from, limit, sort, nome, clientId, scopes, ativo, dataUltimoUso, dataExpiracao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de ClienteAcesso usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public ClienteAcesso getById(@PathParam("value") String value) {
      ClienteAcessoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No clienteAcesso found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de ClienteAcesso.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, ClienteAcesso body) {
        ClienteAcessoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No clienteAcesso found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de ClienteAcesso pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, ClienteAcesso body) {
        ClienteAcessoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No clienteAcesso found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de ClienteAcesso pelo ID.
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
