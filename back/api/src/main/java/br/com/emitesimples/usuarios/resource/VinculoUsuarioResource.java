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

import br.com.emitesimples.usuarios.domain.VinculoUsuario;
import br.com.emitesimples.usuarios.entity.VinculoUsuarioEntity;
import br.com.emitesimples.usuarios.repository.VinculoUsuarioRepository;
import br.com.emitesimples.usuarios.mapper.VinculoUsuarioMapper;
import br.com.emitesimples.results.CountResult;

@Path("/VinculoUsuario")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VinculoUsuarioResource {

    @Inject
    VinculoUsuarioRepository repository;

    @Inject
    VinculoUsuarioMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de VinculoUsuario.
     */
    @GET
    public List<VinculoUsuario> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("papel") String papel,
        @QueryParam("ativo") String ativo,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String usuarioNome,
        @QueryParam("email") String usuarioEmail,
        @QueryParam("usuario.id") String usuarioId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<VinculoUsuarioEntity> entityList = repository.findWithFilters(0, 100, sort, papel, ativo, id, dataCriacao, dataAtualizacao, usuarioNome, usuarioEmail, usuarioId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de VinculoUsuario.
     */
    @POST
    @Transactional
    public VinculoUsuario create(VinculoUsuario body) {
      try {
        VinculoUsuarioEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de VinculoUsuario.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("papel") String papel,
        @QueryParam("ativo") String ativo,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String usuarioNome,
        @QueryParam("email") String usuarioEmail,
        @QueryParam("usuario.id") String usuarioId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( papel, ativo, id, dataCriacao, dataAtualizacao, usuarioNome, usuarioEmail, usuarioId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId));
        return countResult;
    }

    /**
     * Lista os registros de VinculoUsuario por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<VinculoUsuario> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("papel") String papel,
        @QueryParam("ativo") String ativo,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String usuarioNome,
        @QueryParam("email") String usuarioEmail,
        @QueryParam("usuario.id") String usuarioId,
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
        List<VinculoUsuarioEntity> entityList = repository.findWithFilters(from, limit, sort, papel, ativo, id, dataCriacao, dataAtualizacao, usuarioNome, usuarioEmail, usuarioId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de VinculoUsuario usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public VinculoUsuario getById(@PathParam("value") String value) {
      VinculoUsuarioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No vinculoUsuario found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de VinculoUsuario.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, VinculoUsuario body) {
        VinculoUsuarioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No vinculoUsuario found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de VinculoUsuario pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, VinculoUsuario body) {
        VinculoUsuarioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No vinculoUsuario found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de VinculoUsuario pelo ID.
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
