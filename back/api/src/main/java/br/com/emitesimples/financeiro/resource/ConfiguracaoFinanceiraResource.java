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

import br.com.emitesimples.financeiro.domain.ConfiguracaoFinanceira;
import br.com.emitesimples.financeiro.entity.ConfiguracaoFinanceiraEntity;
import br.com.emitesimples.financeiro.repository.ConfiguracaoFinanceiraRepository;
import br.com.emitesimples.financeiro.mapper.ConfiguracaoFinanceiraMapper;
import br.com.emitesimples.results.CountResult;

@Path("/ConfiguracaoFinanceira")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfiguracaoFinanceiraResource {

    @Inject
    ConfiguracaoFinanceiraRepository repository;

    @Inject
    ConfiguracaoFinanceiraMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de ConfiguracaoFinanceira.
     */
    @GET
    public List<ConfiguracaoFinanceira> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("multaPercentual") String multaPercentual,
        @QueryParam("jurosMensalPercentual") String jurosMensalPercentual,
        @QueryParam("carenciaDias") String carenciaDias,
        @QueryParam("lembretesAtivos") String lembretesAtivos,
        @QueryParam("lembreteDiasAntes") String lembreteDiasAntes,
        @QueryParam("lembreteDiasDepois") String lembreteDiasDepois,
        @QueryParam("vencimentoPadraoDias") String vencimentoPadraoDias,
        @QueryParam("observacaoPadraoReceber") String observacaoPadraoReceber,
        @QueryParam("formaPrevistaPadraoReceber") String formaPrevistaPadraoReceber,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String categoriaPadraoReceitaNome,
        @QueryParam("categoriaPadraoReceita.id") String categoriaPadraoReceitaId,
        @QueryParam("nome") String categoriaPadraoDespesaNome,
        @QueryParam("categoriaPadraoDespesa.id") String categoriaPadraoDespesaId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<ConfiguracaoFinanceiraEntity> entityList = repository.findWithFilters(0, 100, sort, multaPercentual, jurosMensalPercentual, carenciaDias, lembretesAtivos, lembreteDiasAntes, lembreteDiasDepois, vencimentoPadraoDias, observacaoPadraoReceber, formaPrevistaPadraoReceber, id, dataCriacao, dataAtualizacao, categoriaPadraoReceitaNome, categoriaPadraoReceitaId, categoriaPadraoDespesaNome, categoriaPadraoDespesaId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de ConfiguracaoFinanceira.
     */
    @POST
    @Transactional
    public ConfiguracaoFinanceira create(ConfiguracaoFinanceira body) {
      try {
        ConfiguracaoFinanceiraEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de ConfiguracaoFinanceira.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("multaPercentual") String multaPercentual,
        @QueryParam("jurosMensalPercentual") String jurosMensalPercentual,
        @QueryParam("carenciaDias") String carenciaDias,
        @QueryParam("lembretesAtivos") String lembretesAtivos,
        @QueryParam("lembreteDiasAntes") String lembreteDiasAntes,
        @QueryParam("lembreteDiasDepois") String lembreteDiasDepois,
        @QueryParam("vencimentoPadraoDias") String vencimentoPadraoDias,
        @QueryParam("observacaoPadraoReceber") String observacaoPadraoReceber,
        @QueryParam("formaPrevistaPadraoReceber") String formaPrevistaPadraoReceber,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String categoriaPadraoReceitaNome,
        @QueryParam("categoriaPadraoReceita.id") String categoriaPadraoReceitaId,
        @QueryParam("nome") String categoriaPadraoDespesaNome,
        @QueryParam("categoriaPadraoDespesa.id") String categoriaPadraoDespesaId,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( multaPercentual, jurosMensalPercentual, carenciaDias, lembretesAtivos, lembreteDiasAntes, lembreteDiasDepois, vencimentoPadraoDias, observacaoPadraoReceber, formaPrevistaPadraoReceber, id, dataCriacao, dataAtualizacao, categoriaPadraoReceitaNome, categoriaPadraoReceitaId, categoriaPadraoDespesaNome, categoriaPadraoDespesaId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId));
        return countResult;
    }

    /**
     * Lista os registros de ConfiguracaoFinanceira por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<ConfiguracaoFinanceira> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("multaPercentual") String multaPercentual,
        @QueryParam("jurosMensalPercentual") String jurosMensalPercentual,
        @QueryParam("carenciaDias") String carenciaDias,
        @QueryParam("lembretesAtivos") String lembretesAtivos,
        @QueryParam("lembreteDiasAntes") String lembreteDiasAntes,
        @QueryParam("lembreteDiasDepois") String lembreteDiasDepois,
        @QueryParam("vencimentoPadraoDias") String vencimentoPadraoDias,
        @QueryParam("observacaoPadraoReceber") String observacaoPadraoReceber,
        @QueryParam("formaPrevistaPadraoReceber") String formaPrevistaPadraoReceber,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nome") String categoriaPadraoReceitaNome,
        @QueryParam("categoriaPadraoReceita.id") String categoriaPadraoReceitaId,
        @QueryParam("nome") String categoriaPadraoDespesaNome,
        @QueryParam("categoriaPadraoDespesa.id") String categoriaPadraoDespesaId,
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
        List<ConfiguracaoFinanceiraEntity> entityList = repository.findWithFilters(from, limit, sort, multaPercentual, jurosMensalPercentual, carenciaDias, lembretesAtivos, lembreteDiasAntes, lembreteDiasDepois, vencimentoPadraoDias, observacaoPadraoReceber, formaPrevistaPadraoReceber, id, dataCriacao, dataAtualizacao, categoriaPadraoReceitaNome, categoriaPadraoReceitaId, categoriaPadraoDespesaNome, categoriaPadraoDespesaId, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de ConfiguracaoFinanceira usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public ConfiguracaoFinanceira getById(@PathParam("value") String value) {
      ConfiguracaoFinanceiraEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No configuracaoFinanceira found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de ConfiguracaoFinanceira.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, ConfiguracaoFinanceira body) {
        ConfiguracaoFinanceiraEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No configuracaoFinanceira found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de ConfiguracaoFinanceira pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, ConfiguracaoFinanceira body) {
        ConfiguracaoFinanceiraEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No configuracaoFinanceira found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de ConfiguracaoFinanceira pelo ID.
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
