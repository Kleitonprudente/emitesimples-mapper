package br.com.emitesimples.financeiro.resource;

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

import br.com.emitesimples.financeiro.domain.CondicaoPagamento;
import br.com.emitesimples.financeiro.entity.CondicaoPagamentoEntity;
import br.com.emitesimples.financeiro.repository.CondicaoPagamentoRepository;
import br.com.emitesimples.financeiro.mapper.CondicaoPagamentoMapper;
import br.com.emitesimples.results.CountResult;

@Path("/CondicaoPagamento")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CondicaoPagamentoResource {

    @Inject
    CondicaoPagamentoRepository repository;

    @Inject
    CondicaoPagamentoMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de CondicaoPagamento.
     */
    @GET
    public List<CondicaoPagamento> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("forma") String forma,
        @QueryParam("ativo") String ativo,
        @QueryParam("parcelas") String parcelas,
        @QueryParam("diasPrimeiroVencimento") String diasPrimeiroVencimento,
        @QueryParam("intervaloDias") String intervaloDias,
        @QueryParam("receberNaEmissao") String receberNaEmissao,
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
        List<CondicaoPagamentoEntity> entityList = repository.findWithFilters(0, 100, sort, nome, forma, ativo, parcelas, diasPrimeiroVencimento, intervaloDias, receberNaEmissao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de CondicaoPagamento.
     */
    @POST
    @Transactional
    public CondicaoPagamento create(CondicaoPagamento body) {
      try {
        CondicaoPagamentoEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de CondicaoPagamento.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nome") String nome,
        @QueryParam("forma") String forma,
        @QueryParam("ativo") String ativo,
        @QueryParam("parcelas") String parcelas,
        @QueryParam("diasPrimeiroVencimento") String diasPrimeiroVencimento,
        @QueryParam("intervaloDias") String intervaloDias,
        @QueryParam("receberNaEmissao") String receberNaEmissao,
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
        countResult.setCount(repository.countWithFilters( nome, forma, ativo, parcelas, diasPrimeiroVencimento, intervaloDias, receberNaEmissao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId));
        return countResult;
    }

    /**
     * Lista os registros de CondicaoPagamento por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<CondicaoPagamento> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("forma") String forma,
        @QueryParam("ativo") String ativo,
        @QueryParam("parcelas") String parcelas,
        @QueryParam("diasPrimeiroVencimento") String diasPrimeiroVencimento,
        @QueryParam("intervaloDias") String intervaloDias,
        @QueryParam("receberNaEmissao") String receberNaEmissao,
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
        List<CondicaoPagamentoEntity> entityList = repository.findWithFilters(from, limit, sort, nome, forma, ativo, parcelas, diasPrimeiroVencimento, intervaloDias, receberNaEmissao, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de CondicaoPagamento usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public CondicaoPagamento getById(@PathParam("value") String value) {
      CondicaoPagamentoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No condicaoPagamento found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de CondicaoPagamento.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, CondicaoPagamento body) {
        CondicaoPagamentoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No condicaoPagamento found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de CondicaoPagamento pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, CondicaoPagamento body) {
        CondicaoPagamentoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No condicaoPagamento found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de CondicaoPagamento pelo ID.
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
