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
            if (nfse(ex, path)) {
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


    // ------------------------------------------------------------------------------------------ NFS-e (envio v2 / consulta v1)

    /**
     * Controle (todos POST, corpo = texto): /_nfse/modo_envio (5023 padrao, 5001, 5028, 5027, 6001, json, lento, 100,
     * 100sem, 500, 5034, semchave, 8429), /_nfse/modo_consulta (100 padrao, 100gzip, 100zip, 5023, 2, 101, 5028, 5027, 6001, json,
     * 5006, 500, idoutro) e /_nfse/reset. Leitura: GET /_nfse/estado (JSON com contagens, ultimo envio e ultima consulta).
     */
    private volatile String modoEnvio = "5023";
    private volatile String modoConsulta = "100";
    private final java.util.List<JsonNode> envios = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private final java.util.List<String> consultas = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    private final java.util.Map<String, String> identPorChave = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, String> numeroPorChave = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, String> chavePorIdent = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile String ultimoAuthEnvio;
    private volatile String ultimoAuthConsulta;
    public static final String PDF_MINIMO = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n";
    public static final String XML_MINIMO = "<NFSe><infNFSe Id=\"x\"/></NFSe>";

    private boolean nfse(HttpExchange ex, String path) throws IOException {
        String m = ex.getRequestMethod();
        if (path.startsWith("/_nfse/") && "POST".equals(m)) {
            String corpo = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).strip();
            switch (path) {
                case "/_nfse/modo_envio" -> modoEnvio = corpo;
                case "/_nfse/modo_consulta" -> modoConsulta = corpo;
                case "/_nfse/reset" -> {
                    modoEnvio = "5023";
                    modoConsulta = "100";
                    envios.clear();
                    consultas.clear();
                    ultimoAuthEnvio = null;
                    ultimoAuthConsulta = null;
                }
                default -> {
                    responder(ex, 404, "{}");
                    return true;
                }
            }
            responder(ex, 200, "{}");
            return true;
        }
        if (path.equals("/_nfse/estado")) {
            com.fasterxml.jackson.databind.node.ObjectNode o = JSON.createObjectNode();
            o.put("envios", envios.size());
            o.put("consultas", consultas.size());
            o.put("authEnvio", ultimoAuthEnvio);
            o.put("authConsulta", ultimoAuthConsulta);
            synchronized (envios) {
                if (!envios.isEmpty()) {
                    o.set("ultimoEnvio", envios.get(envios.size() - 1));
                }
                com.fasterxml.jackson.databind.node.ArrayNode todos = o.putArray("todosEnvios");
                envios.forEach(todos::add);
            }
            synchronized (consultas) {
                if (!consultas.isEmpty()) {
                    o.put("ultimaConsulta", consultas.get(consultas.size() - 1));
                }
            }
            responder(ex, 200, JSON.writeValueAsString(o));
            return true;
        }
        if (path.equals("/v2/nfse") && "POST".equals(m)) {
            envioNfse(ex);
            return true;
        }
        if (path.startsWith("/v1/nfse/") && "GET".equals(m)) {
            consultaNfse(ex, path.substring("/v1/nfse/".length()));
            return true;
        }
        return false;
    }

    private void envioNfse(HttpExchange ex) throws IOException {
        byte[] bytes = ex.getRequestBody().readAllBytes();
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        ultimoAuthEnvio = auth;
        String modo = modoEnvio;
        JsonNode corpo;
        try {
            corpo = JSON.readTree(bytes);
        } catch (IOException e) {
            responder(ex, 200, "{\"sucesso\":false,\"codigo\":5001,\"mensagem\":\"JSON inválido.\"}");
            return;
        }
        com.fasterxml.jackson.databind.node.ObjectNode reg = JSON.createObjectNode();
        reg.put("authorization", auth);
        reg.set("corpo", corpo);
        envios.add(reg);
        if (auth == null || auth.isBlank() || modo.equals("6001")) {
            responder(ex, 200, "{\"sucesso\":false,\"codigo\":6001,\"mensagem\":\"Token inválido.\"}");
            return;
        }
        String ident = corpo.path("identificacao").asText("");
        switch (modo) {
            case "5001" -> {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":5001,\"mensagem\":\"Erro de validação.\",\"erros\":["
                        + "{\"campo\":\"tomador.endereco.bairro\",\"erro\":\"obrigatorio\",\"descricao\":\"Bairro é obrigatório\",\"detalhes\":\"x\"},"
                        + "{\"campo\":\"servico.discriminacao\",\"erro\":\"tamanho\",\"descricao\":\"Discriminação inválida\"}]}");
                return;
            }
            case "5028" -> {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":5028,\"mensagem\":\"A prefeitura rejeitou o valor.\"}");
                return;
            }
            case "json" -> {
                responder(ex, 200, "<html>nao e json</html>");
                return;
            }
            case "500" -> {
                responder(ex, 500, "{\"erro\":\"interno\"}");
                return;
            }
            case "lento" -> dormir(4000);
            case "8429" -> {
                responder(ex, 200, "{\"sucesso\":false,\"codigo\":8429,\"mensagem\":\"Excesso de requisicoes.\"}");
                return;
            }
            default -> {
            }
        }
        // idempotencia do gateway simulado: mesma identificacao = mesma chave
        String chave = chavePorIdent.computeIfAbsent(ident, k -> {
            String c = String.format("%050d", Math.abs(UUID.randomUUID().getMostSignificantBits()));
            identPorChave.put(c, k);
            numeroPorChave.put(c, corpo.path("numero").asText(""));
            return c;
        });
        switch (modo) {
            case "5027" -> responder(ex, 200, "{\"sucesso\":false,\"codigo\":5027,\"mensagem\":\"Conflito.\",\"chave\":\"" + chave + "\"}");
            case "5034" -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":5034,\"mensagem\":\"Autorizador sem resposta.\",\"chave\":\"" + chave + "\"}");
            case "semchave" -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":5023,\"mensagem\":\"Lote em processamento.\"}");
            case "100" -> responder(ex, 200, autorizado(chave, true));
            case "100sem" -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":100,\"mensagem\":\"ok\",\"chave\":\"" + chave + "\"}");
            default -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":5023,\"mensagem\":\"Lote em processamento, aguarde e tente novamente mais tarde.\",\"chave\":\"" + chave + "\"}");
        }
    }

    private void consultaNfse(HttpExchange ex, String chave) throws IOException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        ultimoAuthConsulta = auth;
        consultas.add(chave);
        String modo = modoConsulta;
        if (auth == null || auth.isBlank() || modo.equals("6001")) {
            responder(ex, 200, "{\"sucesso\":false,\"codigo\":6001,\"mensagem\":\"Token inválido.\"}");
            return;
        }
        switch (modo) {
            case "json" -> responder(ex, 200, "<html>nao e json</html>");
            case "500" -> responder(ex, 500, "{}");
            case "5006" -> responder(ex, 200, "{\"sucesso\":false,\"codigo\":5006,\"mensagem\":\"Não encontrada.\"}");
            case "5023" -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":5023,\"mensagem\":\"Em processamento.\",\"chave\":\"" + chave + "\"}");
            case "5028" -> responder(ex, 200, "{\"sucesso\":false,\"codigo\":5028,\"mensagem\":\"Valor rejeitado.\",\"chave\":\"" + chave + "\"}");
            case "5027" -> responder(ex, 200, "{\"sucesso\":false,\"codigo\":5027,\"mensagem\":\"Conflito.\",\"chave\":\"" + chave + "\"}");
            case "2" -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":2,\"mensagem\":\"Offline.\",\"chave\":\"" + chave
                    + "\",\"data_hora_evento\":null,\"protocolo\":null,\"pdf\":\"" + b64(PDF_MINIMO) + "\"}");
            case "101" -> responder(ex, 200, "{\"sucesso\":true,\"codigo\":101,\"mensagem\":\"Homologado o cancelamento da NFS-e\","
                    + "\"data_hora_evento\":\"2026-11-21 10:00:00\",\"xml\":\"" + b64(XML_MINIMO) + "\",\"pdf\":\"" + b64(PDF_MINIMO) + "\"}");
            default -> responder(ex, 200, autorizado(chave, false));
        }
    }

    private String autorizado(String chave, boolean envio) {
        String ident = identPorChave.getOrDefault(chave, "");
        String numero = numeroPorChave.getOrDefault(chave, "1");
        String modo = modoConsulta;
        if (modo.equals("idoutro")) {
            ident = "outra-identificacao";
        }
        String xml;
        String pdf;
        try {
            if (!envio && modo.equals("100gzip")) {
                xml = b64(gzip(XML_MINIMO));
                pdf = b64(gzip(PDF_MINIMO));
            } else if (!envio && modo.equals("100zip")) {
                xml = b64(zip(XML_MINIMO));
                pdf = b64(zip(PDF_MINIMO));
            } else {
                xml = b64(XML_MINIMO);
                pdf = b64(PDF_MINIMO);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return "{\"sucesso\":true,\"codigo\":100,\"mensagem\":\"Autorizado o uso da NFS-e\",\"status\":\"Autorizado\",\"numero\":\"9"
                + numero + "\",\"rps_numero\":\"" + numero + "\",\"rps_serie\":\"1\",\"identificacao\":\"" + ident
                + "\",\"chave\":\"" + chave + "\",\"codigo_verificacao\":\"82 55 B2\",\"data_hora_evento\":\"2026-11-21 11:25:22\","
                + "\"xml\":\"" + xml + "\",\"pdf\":\"" + pdf + "\",\"link_pdf\":\"https://exemplo.local/nfse/" + chave + ".pdf\"}";
    }

    private static String b64(String s) {
        return java.util.Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64(byte[] b) {
        return java.util.Base64.getEncoder().encodeToString(b);
    }

    private static byte[] gzip(String s) throws IOException {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream g = new java.util.zip.GZIPOutputStream(bo)) {
            g.write(s.getBytes(StandardCharsets.UTF_8));
        }
        return bo.toByteArray();
    }

    private static byte[] zip(String s) throws IOException {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(bo)) {
            z.putNextEntry(new java.util.zip.ZipEntry("arquivo"));
            z.write(s.getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return bo.toByteArray();
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
