package br.com.emitesimples.configuracao.resource;

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

import br.com.emitesimples.configuracao.domain.Empresa;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.configuracao.repository.EmpresaRepository;
import br.com.emitesimples.configuracao.mapper.EmpresaMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Empresa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmpresaResource {

    @Inject
    EmpresaRepository repository;

    @Inject
    EmpresaMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Empresa.
     */
    @GET
    public List<Empresa> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nomeFantasia") String nomeFantasia,
        @QueryParam("razaoSocial") String razaoSocial,
        @QueryParam("documento") String documento,
        @QueryParam("rgInscricaoEstadual") String rgInscricaoEstadual,
        @QueryParam("inscricaoMunicipal") String inscricaoMunicipal,
        @QueryParam("email") String email,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("telefone") String telefone,
        @QueryParam("logradouro") String logradouro,
        @QueryParam("numeroEndereco") String numeroEndereco,
        @QueryParam("complemento") String complemento,
        @QueryParam("bairro") String bairro,
        @QueryParam("cep") String cep,
        @QueryParam("regimeTributario") String regimeTributario,
        @QueryParam("regimeIbsCbs") String regimeIbsCbs,
        @QueryParam("urlWebhook") String urlWebhook,
        @QueryParam("situacao") String situacao,
        @QueryParam("dataConfirmacao") String dataConfirmacao,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("statusCadastro") String statusCadastro,
        @QueryParam("dataCadastroGateway") String dataCadastroGateway,
        @QueryParam("validadeCertificado") String validadeCertificado,
        @QueryParam("situacaoCertificado") String situacaoCertificado,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("codigoIbge") String municipioCodigoIbge,
        @QueryParam("nome") String municipioNome,
        @QueryParam("uf") String municipioUf,
        @QueryParam("municipio.id") String municipioId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<EmpresaEntity> entityList = repository.findWithFilters(0, 100, sort, nomeFantasia, razaoSocial, documento, rgInscricaoEstadual, inscricaoMunicipal, email, whatsapp, telefone, logradouro, numeroEndereco, complemento, bairro, cep, regimeTributario, regimeIbsCbs, urlWebhook, situacao, dataConfirmacao, referenciaCrm, statusCadastro, dataCadastroGateway, validadeCertificado, situacaoCertificado, id, ambiente, dataCriacao, dataAtualizacao, municipioCodigoIbge, municipioNome, municipioUf, municipioId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Empresa.
     */
    @POST
    @Transactional
    public Empresa create(Empresa body) {
      try {
        EmpresaEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Empresa.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("nomeFantasia") String nomeFantasia,
        @QueryParam("razaoSocial") String razaoSocial,
        @QueryParam("documento") String documento,
        @QueryParam("rgInscricaoEstadual") String rgInscricaoEstadual,
        @QueryParam("inscricaoMunicipal") String inscricaoMunicipal,
        @QueryParam("email") String email,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("telefone") String telefone,
        @QueryParam("logradouro") String logradouro,
        @QueryParam("numeroEndereco") String numeroEndereco,
        @QueryParam("complemento") String complemento,
        @QueryParam("bairro") String bairro,
        @QueryParam("cep") String cep,
        @QueryParam("regimeTributario") String regimeTributario,
        @QueryParam("regimeIbsCbs") String regimeIbsCbs,
        @QueryParam("urlWebhook") String urlWebhook,
        @QueryParam("situacao") String situacao,
        @QueryParam("dataConfirmacao") String dataConfirmacao,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("statusCadastro") String statusCadastro,
        @QueryParam("dataCadastroGateway") String dataCadastroGateway,
        @QueryParam("validadeCertificado") String validadeCertificado,
        @QueryParam("situacaoCertificado") String situacaoCertificado,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("codigoIbge") String municipioCodigoIbge,
        @QueryParam("nome") String municipioNome,
        @QueryParam("uf") String municipioUf,
        @QueryParam("municipio.id") String municipioId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( nomeFantasia, razaoSocial, documento, rgInscricaoEstadual, inscricaoMunicipal, email, whatsapp, telefone, logradouro, numeroEndereco, complemento, bairro, cep, regimeTributario, regimeIbsCbs, urlWebhook, situacao, dataConfirmacao, referenciaCrm, statusCadastro, dataCadastroGateway, validadeCertificado, situacaoCertificado, id, ambiente, dataCriacao, dataAtualizacao, municipioCodigoIbge, municipioNome, municipioUf, municipioId));
        return countResult;
    }

    /**
     * Lista os registros de Empresa por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Empresa> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("nomeFantasia") String nomeFantasia,
        @QueryParam("razaoSocial") String razaoSocial,
        @QueryParam("documento") String documento,
        @QueryParam("rgInscricaoEstadual") String rgInscricaoEstadual,
        @QueryParam("inscricaoMunicipal") String inscricaoMunicipal,
        @QueryParam("email") String email,
        @QueryParam("whatsapp") String whatsapp,
        @QueryParam("telefone") String telefone,
        @QueryParam("logradouro") String logradouro,
        @QueryParam("numeroEndereco") String numeroEndereco,
        @QueryParam("complemento") String complemento,
        @QueryParam("bairro") String bairro,
        @QueryParam("cep") String cep,
        @QueryParam("regimeTributario") String regimeTributario,
        @QueryParam("regimeIbsCbs") String regimeIbsCbs,
        @QueryParam("urlWebhook") String urlWebhook,
        @QueryParam("situacao") String situacao,
        @QueryParam("dataConfirmacao") String dataConfirmacao,
        @QueryParam("referenciaCrm") String referenciaCrm,
        @QueryParam("statusCadastro") String statusCadastro,
        @QueryParam("dataCadastroGateway") String dataCadastroGateway,
        @QueryParam("validadeCertificado") String validadeCertificado,
        @QueryParam("situacaoCertificado") String situacaoCertificado,
        @QueryParam("id") String id,
        @QueryParam("ambiente") String ambiente,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("codigoIbge") String municipioCodigoIbge,
        @QueryParam("nome") String municipioNome,
        @QueryParam("uf") String municipioUf,
        @QueryParam("municipio.id") String municipioId
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
        List<EmpresaEntity> entityList = repository.findWithFilters(from, limit, sort, nomeFantasia, razaoSocial, documento, rgInscricaoEstadual, inscricaoMunicipal, email, whatsapp, telefone, logradouro, numeroEndereco, complemento, bairro, cep, regimeTributario, regimeIbsCbs, urlWebhook, situacao, dataConfirmacao, referenciaCrm, statusCadastro, dataCadastroGateway, validadeCertificado, situacaoCertificado, id, ambiente, dataCriacao, dataAtualizacao, municipioCodigoIbge, municipioNome, municipioUf, municipioId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Empresa usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Empresa getById(@PathParam("value") String value) {
      EmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No empresa found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Empresa.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Empresa body) {
        EmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No empresa found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Empresa pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Empresa body) {
        EmpresaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No empresa found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Empresa pelo ID.
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
