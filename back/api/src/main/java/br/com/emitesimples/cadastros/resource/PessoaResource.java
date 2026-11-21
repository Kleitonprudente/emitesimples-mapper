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

import br.com.emitesimples.cadastros.domain.Pessoa;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.cadastros.repository.PessoaRepository;
import br.com.emitesimples.cadastros.mapper.PessoaMapper;
import br.com.emitesimples.results.CountResult;

@Path("/Pessoa")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PessoaResource {

    @Inject
    PessoaRepository repository;

    @Inject
    PessoaMapper mapper;

    @Inject
    JsonWebToken jwt;

    /**
     * Lista todos os registros de Pessoa.
     */
    @GET
    public List<Pessoa> listAll(
        @QueryParam("sortBy") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("tipoPessoa") String tipoPessoa,
        @QueryParam("documento") String documento,
        @QueryParam("nome") String nome,
        @QueryParam("nomeFantasia") String nomeFantasia,
        @QueryParam("inscricaoMunicipal") String inscricaoMunicipal,
        @QueryParam("rgInscricaoEstadual") String rgInscricaoEstadual,
        @QueryParam("tipoDestinatario") String tipoDestinatario,
        @QueryParam("consumidorFinal") String consumidorFinal,
        @QueryParam("telefone") String telefone,
        @QueryParam("email") String email,
        @QueryParam("logradouro") String logradouro,
        @QueryParam("numeroEndereco") String numeroEndereco,
        @QueryParam("complemento") String complemento,
        @QueryParam("bairro") String bairro,
        @QueryParam("cep") String cep,
        @QueryParam("codigoPais") String codigoPais,
        @QueryParam("codigoPostal") String codigoPostal,
        @QueryParam("nomeMunicipioExterior") String nomeMunicipioExterior,
        @QueryParam("nomeProvincia") String nomeProvincia,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("codigoIbge") String cidadeCodigoIbge,
        @QueryParam("nome") String cidadeNome,
        @QueryParam("uf") String cidadeUf,
        @QueryParam("cidade.id") String cidadeId
    ) {
        Sort sort = Sort.by(sortBy != null ? sortBy : "id")
                        .direction(sortDirection.equalsIgnoreCase("desc") ?
                                   Sort.Direction.Descending : Sort.Direction.Ascending);
        List<PessoaEntity> entityList = repository.findWithFilters(0, 100, sort, tipoPessoa, documento, nome, nomeFantasia, inscricaoMunicipal, rgInscricaoEstadual, tipoDestinatario, consumidorFinal, telefone, email, logradouro, numeroEndereco, complemento, bairro, cep, codigoPais, codigoPostal, nomeMunicipioExterior, nomeProvincia, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, cidadeCodigoIbge, cidadeNome, cidadeUf, cidadeId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Cria um registro de Pessoa.
     */
    @POST
    @Transactional
    public Pessoa create(Pessoa body) {
      try {
        PessoaEntity entity = mapper.apiToEntity(body);
        repository.persist(entity);
        repository.gravarAgora();
        return mapper.entityToApi(entity);

      } catch (PersistenceException pe) {
        throw new InvalidInputException(pe.getMessage(), pe.getCause());
      } 
    }

    /**
     * Retorna contagem dos registros de Pessoa.
     */
    @GET
    @Path("/count")
    public CountResult count(
        @QueryParam("tipoPessoa") String tipoPessoa,
        @QueryParam("documento") String documento,
        @QueryParam("nome") String nome,
        @QueryParam("nomeFantasia") String nomeFantasia,
        @QueryParam("inscricaoMunicipal") String inscricaoMunicipal,
        @QueryParam("rgInscricaoEstadual") String rgInscricaoEstadual,
        @QueryParam("tipoDestinatario") String tipoDestinatario,
        @QueryParam("consumidorFinal") String consumidorFinal,
        @QueryParam("telefone") String telefone,
        @QueryParam("email") String email,
        @QueryParam("logradouro") String logradouro,
        @QueryParam("numeroEndereco") String numeroEndereco,
        @QueryParam("complemento") String complemento,
        @QueryParam("bairro") String bairro,
        @QueryParam("cep") String cep,
        @QueryParam("codigoPais") String codigoPais,
        @QueryParam("codigoPostal") String codigoPostal,
        @QueryParam("nomeMunicipioExterior") String nomeMunicipioExterior,
        @QueryParam("nomeProvincia") String nomeProvincia,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("codigoIbge") String cidadeCodigoIbge,
        @QueryParam("nome") String cidadeNome,
        @QueryParam("uf") String cidadeUf,
        @QueryParam("cidade.id") String cidadeId
    ) {

        CountResult countResult = new CountResult();
        countResult.setCount(repository.countWithFilters( tipoPessoa, documento, nome, nomeFantasia, inscricaoMunicipal, rgInscricaoEstadual, tipoDestinatario, consumidorFinal, telefone, email, logradouro, numeroEndereco, complemento, bairro, cep, codigoPais, codigoPostal, nomeMunicipioExterior, nomeProvincia, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, cidadeCodigoIbge, cidadeNome, cidadeUf, cidadeId));
        return countResult;
    }

    /**
     * Lista os registros de Pessoa por faixa de posição.
     */
    @GET
    @Path("/{from}/{to}")
    public List<Pessoa> listRange(
        @PathParam("from") int from,
        @PathParam("to") int to,
        @QueryParam("sortBy") @DefaultValue("id") String sortBy,
        @QueryParam("sortDirection") @DefaultValue("asc") String sortDirection,
        @QueryParam("tipoPessoa") String tipoPessoa,
        @QueryParam("documento") String documento,
        @QueryParam("nome") String nome,
        @QueryParam("nomeFantasia") String nomeFantasia,
        @QueryParam("inscricaoMunicipal") String inscricaoMunicipal,
        @QueryParam("rgInscricaoEstadual") String rgInscricaoEstadual,
        @QueryParam("tipoDestinatario") String tipoDestinatario,
        @QueryParam("consumidorFinal") String consumidorFinal,
        @QueryParam("telefone") String telefone,
        @QueryParam("email") String email,
        @QueryParam("logradouro") String logradouro,
        @QueryParam("numeroEndereco") String numeroEndereco,
        @QueryParam("complemento") String complemento,
        @QueryParam("bairro") String bairro,
        @QueryParam("cep") String cep,
        @QueryParam("codigoPais") String codigoPais,
        @QueryParam("codigoPostal") String codigoPostal,
        @QueryParam("nomeMunicipioExterior") String nomeMunicipioExterior,
        @QueryParam("nomeProvincia") String nomeProvincia,
        @QueryParam("id") String id,
        @QueryParam("dataCriacao") String dataCriacao,
        @QueryParam("dataAtualizacao") String dataAtualizacao,
        @QueryParam("nomeFantasia") String empresaNomeFantasia,
        @QueryParam("razaoSocial") String empresaRazaoSocial,
        @QueryParam("documento") String empresaDocumento,
        @QueryParam("rgInscricaoEstadual") String empresaRgInscricaoEstadual,
        @QueryParam("empresa.id") String empresaId,
        @QueryParam("codigoIbge") String cidadeCodigoIbge,
        @QueryParam("nome") String cidadeNome,
        @QueryParam("uf") String cidadeUf,
        @QueryParam("cidade.id") String cidadeId
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
        List<PessoaEntity> entityList = repository.findWithFilters(from, limit, sort, tipoPessoa, documento, nome, nomeFantasia, inscricaoMunicipal, rgInscricaoEstadual, tipoDestinatario, consumidorFinal, telefone, email, logradouro, numeroEndereco, complemento, bairro, cep, codigoPais, codigoPostal, nomeMunicipioExterior, nomeProvincia, id, dataCriacao, dataAtualizacao, empresaNomeFantasia, empresaRazaoSocial, empresaDocumento, empresaRgInscricaoEstadual, empresaId, cidadeCodigoIbge, cidadeNome, cidadeUf, cidadeId);

        return mapper.entityListToApiList(entityList);
    }

    /**
     * Retorna o respectivo registro de Pessoa usando o ID da API.
     */
    @GET
    @Path("/id/{value}")
    public Pessoa getById(@PathParam("value") String value) {
      PessoaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No pessoa found for Id: " + value));

      return mapper.entityToApi(entity);
    }

    /**
     * Altera o respectivo registro de Pessoa.
     */
    @PUT
    @Path("/id/{value}")
    @Transactional
    public void updateById(@PathParam("value") String value, Pessoa body) {
        PessoaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No pessoa found for Id: " + value));
        body.setId(value);
        mapper.updateApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Atualiza parcialmente um registro de Pessoa pelo ID.
     */
    @PATCH
    @Path("/id/{value}")
    @Transactional
    public void patchById(@PathParam("value") String value, Pessoa body) {
        PessoaEntity entity = repository.findByIdOptional(value)
            .orElseThrow(() -> new NotFoundException("No pessoa found with ID: " + value));
        body.setId(value);
        mapper.patchApiToEntity(body, entity);
        repository.gravarAgora();
    }

    /**
     * Deleta um registro de Pessoa pelo ID.
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
