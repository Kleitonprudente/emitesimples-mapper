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

import br.com.emitesimples.cadastros.domain.Servico;
import br.com.emitesimples.cadastros.entity.ServicoEntity;
import br.com.emitesimples.cadastros.repository.ServicoRepository;
import br.com.emitesimples.cadastros.mapper.ServicoMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Servico")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ServicoResource {

    @Inject
    ServicoRepository repository;

    @Inject
    ServicoMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Servico.
     */
    @GET
    public List<Servico> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("codigoInterno") String codigoInterno,
        @QueryParam("ativo") String ativo,
        @QueryParam("codigoServico") String codigoServico,
        @QueryParam("codigoTributacaoMunicipio") String codigoTributacaoMunicipio,
        @QueryParam("codigoNbs") String codigoNbs,
        @QueryParam("descricaoCurta") String descricaoCurta,
        @QueryParam("discriminacaoPadrao") String discriminacaoPadrao,
        @QueryParam("valorPadrao") String valorPadrao,
        @QueryParam("aliquotaIss") String aliquotaIss,
        @QueryParam("issRetido") String issRetido,
        @QueryParam("tipoOperacaoIss") String tipoOperacaoIss,
        @QueryParam("cstPisCofins") String cstPisCofins,
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
        List<ServicoEntity> entityList = repository.findWithFilters(0, 100, sort, nome, codigoInterno, ativo, codigoServico, codigoTributacaoMunicipio, codigoNbs, descricaoCurta, discriminacaoPadrao, valorPadrao, aliquotaIss, issRetido, tipoOperacaoIss, cstPisCofins, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Servico.
     */
    @POST
    @Transactional
    public Servico create(Servico body) {
      try {
        ServicoEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Servico.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nome") String nome,
        @QueryParam("codigoInterno") String codigoInterno,
        @QueryParam("ativo") String ativo,
        @QueryParam("codigoServico") String codigoServico,
        @QueryParam("codigoTributacaoMunicipio") String codigoTributacaoMunicipio,
        @QueryParam("codigoNbs") String codigoNbs,
        @QueryParam("descricaoCurta") String descricaoCurta,
        @QueryParam("discriminacaoPadrao") String discriminacaoPadrao,
        @QueryParam("valorPadrao") String valorPadrao,
        @QueryParam("aliquotaIss") String aliquotaIss,
        @QueryParam("issRetido") String issRetido,
        @QueryParam("tipoOperacaoIss") String tipoOperacaoIss,
        @QueryParam("cstPisCofins") String cstPisCofins,
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
        countResult.setCount(repository.countWithFilters( nome, codigoInterno, ativo, codigoServico, codigoTributacaoMunicipio, codigoNbs, descricaoCurta, discriminacaoPadrao, valorPadrao, aliquotaIss, issRetido, tipoOperacaoIss, cstPisCofins, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId));
        return countResult;
    }

    /**
     * Lista os registros de Servico por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Servico> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nome") String nome,
        @QueryParam("codigoInterno") String codigoInterno,
        @QueryParam("ativo") String ativo,
        @QueryParam("codigoServico") String codigoServico,
        @QueryParam("codigoTributacaoMunicipio") String codigoTributacaoMunicipio,
        @QueryParam("codigoNbs") String codigoNbs,
        @QueryParam("descricaoCurta") String descricaoCurta,
        @QueryParam("discriminacaoPadrao") String discriminacaoPadrao,
        @QueryParam("valorPadrao") String valorPadrao,
        @QueryParam("aliquotaIss") String aliquotaIss,
        @QueryParam("issRetido") String issRetido,
        @QueryParam("tipoOperacaoIss") String tipoOperacaoIss,
        @QueryParam("cstPisCofins") String cstPisCofins,
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
        List<ServicoEntity> entityList = repository.findWithFilters(from, limit, sort, nome, codigoInterno, ativo, codigoServico, codigoTributacaoMunicipio, codigoNbs, descricaoCurta, discriminacaoPadrao, valorPadrao, aliquotaIss, issRetido, tipoOperacaoIss, cstPisCofins, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Servico usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Servico getById(@PathParam("value") String value) {
      ServicoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No servico found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Servico.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Servico body) {
        ServicoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No servico found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Servico pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Servico body) {
        ServicoEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No servico found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Servico pelo ID.
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
