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
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.emitesimples.nfse.domain.Nota;
import br.com.emitesimples.nfse.entity.NotaEntity;
import br.com.emitesimples.nfse.repository.NotaRepository;
import br.com.emitesimples.nfse.mapper.NotaMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Nota")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NotaResource {

    @Inject
    NotaRepository repository;

    @Inject
    NotaMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Nota.
     */
    @GET
    public List<Nota> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("numero") String numero,
        @QueryParam("serie") String serie,
        @QueryParam("dataEmissao") String dataEmissao,
        @QueryParam("dataCompetencia") String dataCompetencia,
        @QueryParam("informacoesComplementares") String informacoesComplementares,
        @QueryParam("regimeApuracao") String regimeApuracao,
        @QueryParam("regimeTributacao") String regimeTributacao,
        @QueryParam("idCondicaoPagamento") String idCondicaoPagamento,
        @QueryParam("codigoServico") String codigoServico,
        @QueryParam("codigoTributacaoMunicipio") String codigoTributacaoMunicipio,
        @QueryParam("codigoNbs") String codigoNbs,
        @QueryParam("discriminacao") String discriminacao,
        @QueryParam("descricao") String descricao,
        @QueryParam("valorServicos") String valorServicos,
        @QueryParam("valorRecebido") String valorRecebido,
        @QueryParam("valorDescontoCondicionado") String valorDescontoCondicionado,
        @QueryParam("valorDescontoIncondicionado") String valorDescontoIncondicionado,
        @QueryParam("tipoOperacaoIss") String tipoOperacaoIss,
        @QueryParam("aliquotaIss") String aliquotaIss,
        @QueryParam("valorBaseCalculoIss") String valorBaseCalculoIss,
        @QueryParam("valorIssRetido") String valorIssRetido,
        @QueryParam("issRetido") String issRetido,
        @QueryParam("responsavelRetencao") String responsavelRetencao,
        @QueryParam("cstPisCofins") String cstPisCofins,
        @QueryParam("tipoRetencao") String tipoRetencao,
        @QueryParam("valorBaseCalculo") String valorBaseCalculo,
        @QueryParam("aliquotaPis") String aliquotaPis,
        @QueryParam("valorPis") String valorPis,
        @QueryParam("aliquotaCofins") String aliquotaCofins,
        @QueryParam("valorCofins") String valorCofins,
        @QueryParam("valorInss") String valorInss,
        @QueryParam("valorIr") String valorIr,
        @QueryParam("valorCsll") String valorCsll,
        @QueryParam("dataHoraAprovacao") String dataHoraAprovacao,
        @QueryParam("dataHoraDescarte") String dataHoraDescarte,
        @QueryParam("estado") String estado,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("errosRetorno") String errosRetorno,
        @QueryParam("chave") String chave,
        @QueryParam("numeroNfse") String numeroNfse,
        @QueryParam("rpsNumero") String rpsNumero,
        @QueryParam("rpsSerie") String rpsSerie,
        @QueryParam("codigoVerificacao") String codigoVerificacao,
        @QueryParam("dataHoraEvento") String dataHoraEvento,
        @QueryParam("linkPdf") String linkPdf,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("documento") String pessoaDocumento,
        @QueryParam("nome") String pessoaNome,
        @QueryParam("nomeFantasia") String pessoaNomeFantasia,
        @QueryParam("pessoa.id") String pessoaId,
        @QueryParam("nome") String servicoNome,
        @QueryParam("codigoServico") String servicoCodigoServico,
        @QueryParam("servico.id") String servicoId,
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
        @QueryParam("descartadoPor.id") String descartadoPorId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<NotaEntity> entityList = repository.findWithFilters(0, 100, sort, numero, serie, dataEmissao, dataCompetencia, informacoesComplementares, regimeApuracao, regimeTributacao, idCondicaoPagamento, codigoServico, codigoTributacaoMunicipio, codigoNbs, discriminacao, descricao, valorServicos, valorRecebido, valorDescontoCondicionado, valorDescontoIncondicionado, tipoOperacaoIss, aliquotaIss, valorBaseCalculoIss, valorIssRetido, issRetido, responsavelRetencao, cstPisCofins, tipoRetencao, valorBaseCalculo, aliquotaPis, valorPis, aliquotaCofins, valorCofins, valorInss, valorIr, valorCsll, dataHoraAprovacao, dataHoraDescarte, estado, codigoRetorno, mensagemRetorno, errosRetorno, chave, numeroNfse, rpsNumero, rpsSerie, codigoVerificacao, dataHoraEvento, linkPdf, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, pessoaDocumento, pessoaNome, pessoaNomeFantasia, pessoaId, servicoNome, servicoCodigoServico, servicoId, preparadoPorUsuarioNome, preparadoPorUsuarioEmail, preparadoPorUsuarioId, preparadoPorClienteNome, preparadoPorClienteClientId, preparadoPorClienteId, aprovadoPorNome, aprovadoPorEmail, aprovadoPorId, descartadoPorNome, descartadoPorEmail, descartadoPorId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Nota.
     */
    @POST
    @Transactional
    public Nota create(Nota body) {
      try {
        NotaEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Nota.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("numero") String numero,
        @QueryParam("serie") String serie,
        @QueryParam("dataEmissao") String dataEmissao,
        @QueryParam("dataCompetencia") String dataCompetencia,
        @QueryParam("informacoesComplementares") String informacoesComplementares,
        @QueryParam("regimeApuracao") String regimeApuracao,
        @QueryParam("regimeTributacao") String regimeTributacao,
        @QueryParam("idCondicaoPagamento") String idCondicaoPagamento,
        @QueryParam("codigoServico") String codigoServico,
        @QueryParam("codigoTributacaoMunicipio") String codigoTributacaoMunicipio,
        @QueryParam("codigoNbs") String codigoNbs,
        @QueryParam("discriminacao") String discriminacao,
        @QueryParam("descricao") String descricao,
        @QueryParam("valorServicos") String valorServicos,
        @QueryParam("valorRecebido") String valorRecebido,
        @QueryParam("valorDescontoCondicionado") String valorDescontoCondicionado,
        @QueryParam("valorDescontoIncondicionado") String valorDescontoIncondicionado,
        @QueryParam("tipoOperacaoIss") String tipoOperacaoIss,
        @QueryParam("aliquotaIss") String aliquotaIss,
        @QueryParam("valorBaseCalculoIss") String valorBaseCalculoIss,
        @QueryParam("valorIssRetido") String valorIssRetido,
        @QueryParam("issRetido") String issRetido,
        @QueryParam("responsavelRetencao") String responsavelRetencao,
        @QueryParam("cstPisCofins") String cstPisCofins,
        @QueryParam("tipoRetencao") String tipoRetencao,
        @QueryParam("valorBaseCalculo") String valorBaseCalculo,
        @QueryParam("aliquotaPis") String aliquotaPis,
        @QueryParam("valorPis") String valorPis,
        @QueryParam("aliquotaCofins") String aliquotaCofins,
        @QueryParam("valorCofins") String valorCofins,
        @QueryParam("valorInss") String valorInss,
        @QueryParam("valorIr") String valorIr,
        @QueryParam("valorCsll") String valorCsll,
        @QueryParam("dataHoraAprovacao") String dataHoraAprovacao,
        @QueryParam("dataHoraDescarte") String dataHoraDescarte,
        @QueryParam("estado") String estado,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("errosRetorno") String errosRetorno,
        @QueryParam("chave") String chave,
        @QueryParam("numeroNfse") String numeroNfse,
        @QueryParam("rpsNumero") String rpsNumero,
        @QueryParam("rpsSerie") String rpsSerie,
        @QueryParam("codigoVerificacao") String codigoVerificacao,
        @QueryParam("dataHoraEvento") String dataHoraEvento,
        @QueryParam("linkPdf") String linkPdf,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("documento") String pessoaDocumento,
        @QueryParam("nome") String pessoaNome,
        @QueryParam("nomeFantasia") String pessoaNomeFantasia,
        @QueryParam("pessoa.id") String pessoaId,
        @QueryParam("nome") String servicoNome,
        @QueryParam("codigoServico") String servicoCodigoServico,
        @QueryParam("servico.id") String servicoId,
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
        @QueryParam("descartadoPor.id") String descartadoPorId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( numero, serie, dataEmissao, dataCompetencia, informacoesComplementares, regimeApuracao, regimeTributacao, idCondicaoPagamento, codigoServico, codigoTributacaoMunicipio, codigoNbs, discriminacao, descricao, valorServicos, valorRecebido, valorDescontoCondicionado, valorDescontoIncondicionado, tipoOperacaoIss, aliquotaIss, valorBaseCalculoIss, valorIssRetido, issRetido, responsavelRetencao, cstPisCofins, tipoRetencao, valorBaseCalculo, aliquotaPis, valorPis, aliquotaCofins, valorCofins, valorInss, valorIr, valorCsll, dataHoraAprovacao, dataHoraDescarte, estado, codigoRetorno, mensagemRetorno, errosRetorno, chave, numeroNfse, rpsNumero, rpsSerie, codigoVerificacao, dataHoraEvento, linkPdf, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, pessoaDocumento, pessoaNome, pessoaNomeFantasia, pessoaId, servicoNome, servicoCodigoServico, servicoId, preparadoPorUsuarioNome, preparadoPorUsuarioEmail, preparadoPorUsuarioId, preparadoPorClienteNome, preparadoPorClienteClientId, preparadoPorClienteId, aprovadoPorNome, aprovadoPorEmail, aprovadoPorId, descartadoPorNome, descartadoPorEmail, descartadoPorId));
        return countResult;
    }

    /**
     * Lista os registros de Nota por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Nota> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("numero") String numero,
        @QueryParam("serie") String serie,
        @QueryParam("dataEmissao") String dataEmissao,
        @QueryParam("dataCompetencia") String dataCompetencia,
        @QueryParam("informacoesComplementares") String informacoesComplementares,
        @QueryParam("regimeApuracao") String regimeApuracao,
        @QueryParam("regimeTributacao") String regimeTributacao,
        @QueryParam("idCondicaoPagamento") String idCondicaoPagamento,
        @QueryParam("codigoServico") String codigoServico,
        @QueryParam("codigoTributacaoMunicipio") String codigoTributacaoMunicipio,
        @QueryParam("codigoNbs") String codigoNbs,
        @QueryParam("discriminacao") String discriminacao,
        @QueryParam("descricao") String descricao,
        @QueryParam("valorServicos") String valorServicos,
        @QueryParam("valorRecebido") String valorRecebido,
        @QueryParam("valorDescontoCondicionado") String valorDescontoCondicionado,
        @QueryParam("valorDescontoIncondicionado") String valorDescontoIncondicionado,
        @QueryParam("tipoOperacaoIss") String tipoOperacaoIss,
        @QueryParam("aliquotaIss") String aliquotaIss,
        @QueryParam("valorBaseCalculoIss") String valorBaseCalculoIss,
        @QueryParam("valorIssRetido") String valorIssRetido,
        @QueryParam("issRetido") String issRetido,
        @QueryParam("responsavelRetencao") String responsavelRetencao,
        @QueryParam("cstPisCofins") String cstPisCofins,
        @QueryParam("tipoRetencao") String tipoRetencao,
        @QueryParam("valorBaseCalculo") String valorBaseCalculo,
        @QueryParam("aliquotaPis") String aliquotaPis,
        @QueryParam("valorPis") String valorPis,
        @QueryParam("aliquotaCofins") String aliquotaCofins,
        @QueryParam("valorCofins") String valorCofins,
        @QueryParam("valorInss") String valorInss,
        @QueryParam("valorIr") String valorIr,
        @QueryParam("valorCsll") String valorCsll,
        @QueryParam("dataHoraAprovacao") String dataHoraAprovacao,
        @QueryParam("dataHoraDescarte") String dataHoraDescarte,
        @QueryParam("estado") String estado,
        @QueryParam("codigoRetorno") String codigoRetorno,
        @QueryParam("mensagemRetorno") String mensagemRetorno,
        @QueryParam("errosRetorno") String errosRetorno,
        @QueryParam("chave") String chave,
        @QueryParam("numeroNfse") String numeroNfse,
        @QueryParam("rpsNumero") String rpsNumero,
        @QueryParam("rpsSerie") String rpsSerie,
        @QueryParam("codigoVerificacao") String codigoVerificacao,
        @QueryParam("dataHoraEvento") String dataHoraEvento,
        @QueryParam("linkPdf") String linkPdf,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("documento") String pessoaDocumento,
        @QueryParam("nome") String pessoaNome,
        @QueryParam("nomeFantasia") String pessoaNomeFantasia,
        @QueryParam("pessoa.id") String pessoaId,
        @QueryParam("nome") String servicoNome,
        @QueryParam("codigoServico") String servicoCodigoServico,
        @QueryParam("servico.id") String servicoId,
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
        @QueryParam("descartadoPor.id") String descartadoPorId
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
        List<NotaEntity> entityList = repository.findWithFilters(from, limit, sort, numero, serie, dataEmissao, dataCompetencia, informacoesComplementares, regimeApuracao, regimeTributacao, idCondicaoPagamento, codigoServico, codigoTributacaoMunicipio, codigoNbs, discriminacao, descricao, valorServicos, valorRecebido, valorDescontoCondicionado, valorDescontoIncondicionado, tipoOperacaoIss, aliquotaIss, valorBaseCalculoIss, valorIssRetido, issRetido, responsavelRetencao, cstPisCofins, tipoRetencao, valorBaseCalculo, aliquotaPis, valorPis, aliquotaCofins, valorCofins, valorInss, valorIr, valorCsll, dataHoraAprovacao, dataHoraDescarte, estado, codigoRetorno, mensagemRetorno, errosRetorno, chave, numeroNfse, rpsNumero, rpsSerie, codigoVerificacao, dataHoraEvento, linkPdf, id, ambiente, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, pessoaDocumento, pessoaNome, pessoaNomeFantasia, pessoaId, servicoNome, servicoCodigoServico, servicoId, preparadoPorUsuarioNome, preparadoPorUsuarioEmail, preparadoPorUsuarioId, preparadoPorClienteNome, preparadoPorClienteClientId, preparadoPorClienteId, aprovadoPorNome, aprovadoPorEmail, aprovadoPorId, descartadoPorNome, descartadoPorEmail, descartadoPorId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Nota usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Nota getById(@PathParam("value") String value) {
      NotaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No nota found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Nota.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Nota body) {
        NotaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No nota found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Nota pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Nota body) {
        NotaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No nota found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Nota pelo ID.
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
