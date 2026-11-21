package br.com.emitesimples.security;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Mapeadores globais de exceção: todo erro sai como JSON {codigo, mensagem, detalhes?}.
 * O 500 nunca vaza pilha nem SQL (o detalhe vai só para o log).
 */
public class ErrosHandler {

    private static final Logger LOG = Logger.getLogger(ErrosHandler.class);

    private static Response resposta(int status, String codigo, String mensagem, Object detalhes) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroApi(codigo, mensagem, detalhes))
                .build();
    }

    @ServerExceptionMapper
    public Response apiException(ApiException e) {
        Response.ResponseBuilder b = Response.status(e.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroApi(e.getCodigo(), e.getMessage(), e.getDetalhes()));
        if (e instanceof LimiteExcedidoException l) {
            b.header("Retry-After", l.getRetryAfterSegundos());
        }
        return b.build();
    }

    @ServerExceptionMapper
    public Response validacao(ConstraintViolationException e) {
        List<Map<String, String>> campos = new ArrayList<>();
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            Map<String, String> c = new LinkedHashMap<>();
            c.put("campo", nomeDoCampo(v.getPropertyPath()));
            c.put("mensagem", v.getMessage());
            campos.add(c);
        }
        campos.sort((a, b) -> a.get("campo").compareTo(b.get("campo")));
        return resposta(400, "requisicao_invalida", "Há campos inválidos na requisição.", Map.of("campos", campos));
    }

    private static String nomeDoCampo(Path caminho) {
        String ultimo = "";
        for (Path.Node n : caminho) {
            ultimo = n.getName();
        }
        return ultimo;
    }

    @ServerExceptionMapper
    public Response naoAutenticado(AuthenticationFailedException e) {
        return resposta(401, "nao_autenticado", "Autenticação necessária ou token inválido.", null);
    }

    @ServerExceptionMapper
    public Response naoAutorizado(UnauthorizedException e) {
        return resposta(401, "nao_autenticado", "Autenticação necessária ou token inválido.", null);
    }

    @ServerExceptionMapper
    public Response proibido(io.quarkus.security.ForbiddenException e) {
        return resposta(403, "acesso_negado", "Você não tem permissão para esta operação.", null);
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException e) {
        int status = e.getResponse().getStatus();
        if (violacaoRls(e)) {
            return negadoPeloBanco(e);
        }
        if (status >= 500) {
            LOG.error("Erro 5xx em operação da API", e);
            return resposta(500, "erro_interno", "Erro interno. Tente novamente mais tarde.", null);
        }
        String msg = e.getMessage();
        if (e.getCause() != null) {
            // as exceções geradas embrulham PersistenceException: a mensagem pode trazer SQL, então não sai
            LOG.warn("Requisição recusada pela persistência: " + e.getCause().getClass().getSimpleName());
            msg = null;
        }
        if (msg == null || msg.isBlank() || msg.startsWith("HTTP ")) {
            msg = mensagemPadrao(status);
        }
        return resposta(status, codigoDoStatus(status), msg, null);
    }

    @ServerExceptionMapper
    public Response inesperado(Throwable e) {
        if (violacaoRls(e)) {
            return negadoPeloBanco(e);
        }
        LOG.error("Erro inesperado", e);
        return resposta(500, "erro_interno", "Erro interno. Tente novamente mais tarde.", null);
    }

    /** SQLSTATE 42501 (insufficient_privilege): a RLS do banco barrou a gravação (linha de outra empresa). */
    static boolean violacaoRls(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof java.sql.SQLException sql && "42501".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static Response negadoPeloBanco(Throwable e) {
        LOG.warn("Gravação barrada pela RLS do banco (registro de outra empresa).");
        return resposta(403, "isolamento_empresa", "Você não tem permissão para gravar dados de outra empresa.", null);
    }

    static String codigoDoStatus(int status) {
        return switch (status) {
            case 400 -> "requisicao_invalida";
            case 401 -> "nao_autenticado";
            case 403 -> "acesso_negado";
            case 404 -> "nao_encontrado";
            case 405 -> "metodo_nao_permitido";
            case 409 -> "conflito";
            case 415 -> "tipo_nao_suportado";
            case 429 -> "muitas_tentativas";
            default -> status >= 500 ? "erro_interno" : "requisicao_invalida";
        };
    }

    static String mensagemPadrao(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida.";
            case 401 -> "Autenticação necessária ou token inválido.";
            case 403 -> "Você não tem permissão para esta operação.";
            case 404 -> "Recurso não encontrado.";
            case 405 -> "Método não permitido.";
            case 409 -> "Conflito com o estado atual do recurso.";
            case 415 -> "Tipo de conteúdo não suportado.";
            case 429 -> "Muitas tentativas. Tente novamente mais tarde.";
            default -> "Requisição não atendida.";
        };
    }
}
