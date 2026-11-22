package br.com.emitesimples.plataforma;

import br.com.emitesimples.plataforma.ConfiguracaoConsultaCnpjService.Atual;
import br.com.emitesimples.security.ApiException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Configuracao da consulta de CNPJ (CNPJ.ws), so para o administrador da plataforma. O token e SEGREDO: nunca volta na
 * leitura (so {@code tokenConfigurado}) e nunca e registrado em log. O corpo do PUT e lido como texto e interpretado aqui
 * para que uma falha de parse nao carregue trecho do corpo (que pode conter o token) para log ou resposta.
 */
@Path("/admin/configuracoes/consulta-cnpj")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("plataforma_admin")
public class ConfiguracaoConsultaCnpjResource {

    private static final ObjectMapper JSON = new ObjectMapper();

    public record UsuarioResumo(String id, String nome) {
    }

    public record ConfiguracaoResposta(String urlBase, boolean tokenConfigurado, LocalDateTime dataAtualizacao,
                                       @JsonInclude(JsonInclude.Include.ALWAYS) UsuarioResumo atualizadoPor) {
    }

    @Inject
    ConfiguracaoConsultaCnpjService service;

    @Inject
    JsonWebToken jwt;

    @GET
    public ConfiguracaoResposta obter() {
        Atual a = service.obter();
        UsuarioResumo por = a.atualizadoPorId() == null ? null : new UsuarioResumo(a.atualizadoPorId(), a.atualizadoPorNome());
        return new ConfiguracaoResposta(a.urlBase(), a.tokenConfigurado(), a.dataAtualizacao(), por);
    }

    @Inject
    ConsultaCnpjTesteService teste;

    /** Consulta de TESTE ao CNPJ.ws com a URL e o token salvos. Cada chamada consome uma consulta do plano do CNPJ.ws. */
    @POST
    @Path("/testar")
    public ConsultaCnpjResumo testar(TesteConsultaCnpjRequest corpo) {
        return teste.testar(jwt.getSubject(), corpo == null ? null : corpo.cnpj());
    }

    @PUT
    public Response atualizar(String corpo) {
        JsonNode raiz;
        try {
            raiz = corpo == null || corpo.isBlank() ? null : JSON.readTree(corpo);
        } catch (Exception e) {
            raiz = null; // sem registrar a causa: ela pode citar trecho do corpo
        }
        if (raiz == null || !raiz.isObject()) {
            throw new ApiException(400, "requisicao_invalida", "O corpo da requisição deve ser um objeto JSON.");
        }
        JsonNode url = raiz.get("urlBase");
        if (url == null || !url.isTextual()) {
            throw new ApiException(400, "url_invalida", "URL inválida. Informe a URL.");
        }
        JsonNode t = raiz.get("token");
        String token = null;
        if (t != null && !t.isNull()) {
            if (!t.isTextual()) {
                throw new ApiException(400, "token_invalido", "O token deve ser um texto.");
            }
            token = t.asText();
        }
        service.atualizar(url.asText(), token, jwt.getSubject());
        return Response.noContent().build();
    }
}
