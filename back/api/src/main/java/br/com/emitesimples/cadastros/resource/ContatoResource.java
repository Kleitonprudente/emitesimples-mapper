package br.com.emitesimples.cadastros.resource;

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

import br.com.emitesimples.cadastros.domain.Contato;
import br.com.emitesimples.cadastros.entity.ContatoEntity;
import br.com.emitesimples.cadastros.repository.ContatoRepository;
import br.com.emitesimples.cadastros.mapper.ContatoMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Contato")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ContatoResource {

    @Inject
    ContatoRepository repository;

    @Inject
    ContatoMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Contato.
     */
    @GET
    public List<Contato> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("tipoContato") String tipoContato,
        @QueryParam("nome") String nome,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("email") String email,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("documento") String pessoaDocumento,
        @QueryParam("nome") String pessoaNome,
        @QueryParam("nomeFantasia") String pessoaNomeFantasia,
        @QueryParam("pessoa.id") String pessoaId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<ContatoEntity> entityList = repository.findWithFilters(0, 100, sort, tipoContato, nome, whatsapp, email, id, dataCriacao, dataAtualizacao, pessoaDocumento, pessoaNome, pessoaNomeFantasia, pessoaId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Contato.
     */
    @POST
    @Transactional
    public Contato create(Contato body) {
      try {
        ContatoEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Contato.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("tipoContato") String tipoContato,
        @QueryParam("nome") String nome,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("email") String email,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("documento") String pessoaDocumento,
        @QueryParam("nome") String pessoaNome,
        @QueryParam("nomeFantasia") String pessoaNomeFantasia,
        @QueryParam("pessoa.id") String pessoaId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( tipoContato, nome, whatsapp, email, id, dataCriacao, dataAtualizacao, pessoaDocumento, pessoaNome, pessoaNomeFantasia, pessoaId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId));
        return countResult;
    }

    /**
     * Lista os registros de Contato por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Contato> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("tipoContato") String tipoContato,
        @QueryParam("nome") String nome,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("email") String email,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("documento") String pessoaDocumento,
        @QueryParam("nome") String pessoaNome,
        @QueryParam("nomeFantasia") String pessoaNomeFantasia,
        @QueryParam("pessoa.id") String pessoaId,
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
        List<ContatoEntity> entityList = repository.findWithFilters(from, limit, sort, tipoContato, nome, whatsapp, email, id, dataCriacao, dataAtualizacao, pessoaDocumento, pessoaNome, pessoaNomeFantasia, pessoaId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Contato usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Contato getById(@PathParam("value") String value) {
      ContatoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No contato found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Contato.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Contato body) {
        ContatoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No contato found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Contato pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Contato body) {
        ContatoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No contato found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Contato pelo ID.
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
