package br.com.emitesimples.apoio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IntegraNotas SIMULADO (servidor HTTP local em porta livre): os testes NUNCA chamam a internet. Serve
 * {@code POST /v1/soft/emitente}: exige {@code Authorization} (cru) e os campos obrigatorios, e devolve um token novo.
 * O modo de falha e escolhido por um MARCADOR no {@code nome} (fantasia) enviado: veja as constantes {@code M_*}.
 * Sem {@code Authorization}, ou com o token {@link #TOKEN_RECUSADO}, responde codigo 6001. Auxiliares:
 * {@code GET /_ultima} (JSON {authorization, corpo, tokenDevolvido} da ultima requisicao valida) e {@code GET /_contagem}
 * (quantas chamadas a /v1/soft/emitente chegaram).
 */
public class IntegraNotasStubResource implements QuarkusTestResourceLifecycleManager {

    public static final String PROP_URL = "emitesimples.teste.integranotas-url";
    public static final String TOKEN_RECUSADO = "token-softhouse-invalido";
    /** Marcadores no nome fantasia. */
    public static final String M_RECUSA = "#recusa";
    public static final String M_6001 = "#6001";
    public static final String M_500 = "#500";
    public static final String M_LENTO = "#lento";
    public static final String M_DEVAGAR = "#devagar";
    public static final String M_JSON = "#json";
    public static final String M_REDIRECT = "#redirect";
    public static final String M_SEM_TOKEN = "#semtoken";
    public static final String M_GIGANTE = "#gigante";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> OBRIGATORIOS = List.of("nome", "razao", "crt", "telefone", "rua", "numero", "bairro",
            "cmun", "municipio", "uf", "cep");

    private HttpServer servidor;
    private final AtomicInteger contagem = new AtomicInteger();
    private volatile String ultima = "{}";
    private final AtomicInteger contagemCertificado = new AtomicInteger();
    private volatile String ultimoCertificado = "{}";
    private volatile String modoCertificado = MODO_OK;
    private final java.util.Set<String> tokensEmitidos = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public Map<String, String> start() {
        try {
            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Não subiu o IntegraNotas simulado", e);
        }
        servidor.createContext("/", this::tratar);
        servidor.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        servidor.start();
        return Map.of(PROP_URL, "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    private void tratar(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.equals("/_ultima")) {
                responder(ex, 200, ultima);
                return;
            }
            if (path.equals("/_contagem")) {
                responder(ex, 200, String.valueOf(contagem.get()));
                return;
            }
            if (path.equals("/_modo_certificado") && "POST".equals(ex.getRequestMethod())) {
                modoCertificado = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).strip();
                responder(ex, 200, "{}");
                return;
            }
            if (path.equals("/_ultimo_certificado")) {
                responder(ex, 200, ultimoCertificado);
                return;
            }
            if (path.equals("/_contagem_certificado")) {
                responder(ex, 200, String.valueOf(contagemCertificado.get()));
                return;
            }
            if (path.equals("/v1/certificado") && "POST".equals(ex.getRequestMethod())) {
                certificado(ex);
                return;
            }
            if (!path.equals("/v1/soft/emitente") || !"POST".equals(ex.getRequestMethod())) {
                responder(ex, 404, "{}");
                return;
            }
            contagem.incrementAndGet();
            byte[] bytes = ex.getRequestBody().readAllBytes();
            String auth = ex.getRequestHeaders().getFirst("Authorization");
            if (auth == null || auth.isBlank() || auth.equals(TOKEN_RECUSADO)) {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":6001,\"mensagem\":\"Token inválido.\"}");
                return;
            }
            JsonNode corpo;
            try {
                corpo = JSON.readTree(bytes);
            } catch (IOException e) {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":5001,\"mensagem\":\"JSON inválido.\"}");
                return;
            }
            for (String c : OBRIGATORIOS) {
                if (corpo.path(c).isMissingNode() || corpo.path(c).isNull()) {
                    responder(ex, 200, "{\"sucesso\":false,\"codigo\":4000,\"mensagem\":\"Campo obrigatório.\","
                            + "\"erros\":[{\"campo\":\"" + c + "\",\"erro\":\"obrigatorio\",\"descricao\":\"Campo obrigatório\"}]}");
                    return;
                }
            }
            if (corpo.path("cnpj").isMissingNode() && corpo.path("cpf").isMissingNode()) {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":4000,\"mensagem\":\"Informe cnpj ou cpf.\"}");
                return;
            }
            String nome = corpo.path("nome").asText("");
            if (nome.contains(M_RECUSA)) {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":4002,\"mensagem\":\"Dados recusados.\","
                        + "\"erros\":[{\"campo\":\"cmun\",\"erro\":\"invalido\",\"descricao\":\"Município inválido\",\"detalhes\":\"x\"},"
                        + "{\"campo\":\"cep\",\"erro\":\"invalido\",\"descricao\":\"CEP inválido\"}]}");
                return;
            }
            if (nome.contains(M_6001)) {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":6001,\"mensagem\":\"Token inválido.\"}");
                return;
            }
            if (nome.contains(M_500)) {
                responder(ex, 500, "{\"erro\":\"interno\"}");
                return;
            }
            if (nome.contains(M_LENTO)) {
                dormir(4000);
            }
            if (nome.contains(M_DEVAGAR)) {
                dormir(400);
            }
            if (nome.contains(M_JSON)) {
                responder(ex, 200, "<html>nao e json</html>");
                return;
            }
            if (nome.contains(M_REDIRECT)) {
                ex.getResponseHeaders().add("Location", "/v1/soft/emitente-redirecionado");
                responder(ex, 302, "");
                return;
            }
            if (nome.contains(M_GIGANTE)) {
                byte[] bloco = new byte[64 * 1024];
                java.util.Arrays.fill(bloco, (byte) 'a');
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, 0);
                for (int i = 0; i < 20; i++) {
                    ex.getResponseBody().write(bloco);
                }
                return;
            }
            if (nome.contains(M_SEM_TOKEN)) {
                responder(ex, 200, "{\"sucesso\":true,\"codigo\":200,\"mensagem\":\"Sucesso.\"}");
                return;
            }
            String token = "emitente-" + UUID.randomUUID();
            tokensEmitidos.add(token);
            ultima = JSON.writeValueAsString(Map.of("authorization", auth, "corpo", corpo, "tokenDevolvido", token));
            responder(ex, 200, "{\"sucesso\":true,\"codigo\":200,\"mensagem\":\"Sucesso.\",\"token\":\"" + token + "\"}");
        } catch (IOException e) {
            // cliente desistiu (timeout ou corpo cortado): normal nos testes
        } finally {
            ex.close();
        }
    }

    /** Modos do {@code POST /v1/certificado} (escolhidos por {@code POST /_modo_certificado}): ok, 5003, 5011, 5029, 6001, 500, lento, json, semcodigo, 5003http400. */
    public static final String MODO_OK = "ok";

    private void certificado(HttpExchange ex) throws IOException {
        contagemCertificado.incrementAndGet();
        byte[] bytes = ex.getRequestBody().readAllBytes();
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        String modo = modoCertificado;
        if (auth == null || auth.isBlank() || !tokensEmitidos.contains(auth) || modo.equals("6001")) {
            responder(ex, 200, "{\"sucesso\":false,\"codigo\":6001,\"mensagem\":\"Token inválido.\"}");
            return;
        }
        JsonNode corpo = JSON.readTree(bytes);
        ultimoCertificado = JSON.writeValueAsString(Map.of("authorization", auth, "certificado", corpo.path("certificado").asText(""),
                "senha", corpo.path("senha").asText(""), "contentType", String.valueOf(ex.getRequestHeaders().getFirst("Content-Type"))));
        switch (modo) {
            case "5003" -> responder(ex, 200, "{\"sucesso\":false,\"codigo\":5003,\"mensagem\":\"Certificado inválido.\"}");
            case "5003http400" -> responder(ex, 400, "{\"sucesso\":false,\"codigo\":5003,\"mensagem\":\"Certificado inválido.\"}");
            case "5011" -> responder(ex, 200, "{\"sucesso\":false,\"codigo\":5011,\"mensagem\":\"Certificado vencido.\"}");
            case "5029" -> responder(ex, 200, "{\"sucesso\":false,\"codigo\":5029,\"mensagem\":\"Certificado de outro CNPJ.\"}");
            case "500" -> responder(ex, 500, "{\"erro\":\"interno\"}");
            case "lento" -> {
                dormir(4000);
                responder(ex, 200, "{\"sucesso\":true,\"codigo\":200,\"mensagem\":\"Sucesso.\"}");
            }
            case "json" -> responder(ex, 200, "<html>nao e json</html>");
            case "semcodigo" -> responder(ex, 200, "{\"sucesso\":true,\"mensagem\":\"Sucesso.\"}");
            default -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":200,\"mensagem\":\"Sucesso.\"}");
        }
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void responder(HttpExchange ex, int status, String corpo) throws IOException {
        byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            ex.getResponseBody().write(b);
        }
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }
}
