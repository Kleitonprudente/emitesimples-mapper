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

import br.com.emitesimples.usuarios.domain.Usuario;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import br.com.emitesimples.usuarios.repository.UsuarioRepository;
import br.com.emitesimples.usuarios.mapper.UsuarioMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Usuario")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    @Inject
    UsuarioRepository repository;

    @Inject
    UsuarioMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Usuario.
     */
    @GET
    public List<Usuario> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("email") String email,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("ativo") String ativo,
        @QueryParam("administradorPlataforma") String administradorPlataforma,
        @QueryParam("dataUltimoAcesso") String dataUltimoAcesso,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<UsuarioEntity> entityList = repository.findWithFilters(0, 100, sort, nome, email, whatsapp, ativo, administradorPlataforma, dataUltimoAcesso, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Usuario.
     */
    @POST
    @Transactional
    public Usuario create(Usuario body) {
      try {
        UsuarioEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Usuario.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nome") String nome,
        @QueryParam("email") String email,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("ativo") String ativo,
        @QueryParam("administradorPlataforma") String administradorPlataforma,
        @QueryParam("dataUltimoAcesso") String dataUltimoAcesso,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( nome, email, whatsapp, ativo, administradorPlataforma, dataUltimoAcesso, id, dataCriacao, dataAtualizacao));
        return countResult;
    }

    /**
     * Lista os registros de Usuario por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Usuario> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("email") String email,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("ativo") String ativo,
        @QueryParam("administradorPlataforma") String administradorPlataforma,
        @QueryParam("dataUltimoAcesso") String dataUltimoAcesso,
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
        List<UsuarioEntity> entityList = repository.findWithFilters(from, limit, sort, nome, email, whatsapp, ativo, administradorPlataforma, dataUltimoAcesso, id, dataCriacao, dataAtualizacao);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Usuario usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Usuario getById(@PathParam("value") String value) {
      UsuarioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No usuario found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Usuario.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Usuario body) {
        UsuarioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No usuario found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Usuario pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Usuario body) {
        UsuarioEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No usuario found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Usuario pelo ID.
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
