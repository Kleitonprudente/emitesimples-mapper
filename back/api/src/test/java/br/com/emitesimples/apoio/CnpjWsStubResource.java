package br.com.emitesimples.apoio;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CNPJ.ws SIMULADO (servidor HTTP local em porta livre): os testes NUNCA chamam a internet. Serve GET /cnpj/{cnpj14}?token=...
 * O comportamento e escolhido pela RAIZ (8 primeiros digitos) do CNPJ; use {@link #cnpj(String)} para montar um CNPJ valido.
 * Sem o parametro {@code token} responde 401. Cada requisicao registra o token recebido (GET /_tokens) e a contagem
 * (GET /_contagem/{cnpj}).
 */
public class CnpjWsStubResource implements QuarkusTestResourceLifecycleManager {

    public static final String PROP_URL = "emitesimples.teste.cnpjws-url";
    /** Resposta completa de exemplo. */
    public static final String RAIZ_COMPLETO = "27865757";
    public static final String RAIZ_MINIMO = "10000001";
    public static final String RAIZ_404 = "10000002";
    public static final String RAIZ_401 = "10000003";
    public static final String RAIZ_403 = "10000004";
    public static final String RAIZ_429 = "10000005";
    public static final String RAIZ_429_SEM_ESPERA = "10000006";
    public static final String RAIZ_500 = "10000007";
    public static final String RAIZ_LENTO = "10000008";
    public static final String RAIZ_JSON_INVALIDO = "10000009";
    public static final String RAIZ_GIGANTE_CHUNKED = "10000010";
    public static final String RAIZ_GIGANTE_LENGTH = "10000011";
    public static final String RAIZ_REDIRECT = "10000012";
    public static final String RAIZ_ARRAY = "10000013";
    /** Cadastro completo: simples=Sim, mei=Nao, IBGE existente no teste ({@link #IBGE_CADASTRO}), IEs de RJ e SP. */
    public static final String RAIZ_CAD_SIMPLES = "10000020";
    /** mei=Sim (e simples=Sim), cidade sem linha em Municipio. */
    public static final String RAIZ_CAD_MEI = "10000021";
    /** simples=Nao, mei=Nao, sem IEs, IBGE nao cadastrado. */
    public static final String RAIZ_CAD_NORMAL = "10000022";
    public static final String IBGE_CADASTRO = "9100001";

    private HttpServer servidor;
    private final List<String> tokens = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> contagem = new ConcurrentHashMap<>();

    /** CNPJ valido de 14 digitos: raiz de 8 + filial 0001 + digitos verificadores. */
    public static String cnpj(String raiz) {
        String base = raiz + "0001";
        String d = base + dv(base);
        return d + dv(d);
    }

    private static int dv(String s) {
        int soma = 0;
        int peso = 2;
        for (int i = s.length() - 1; i >= 0; i--) {
            soma += (s.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int r = soma % 11;
        return r < 2 ? 0 : 11 - r;
    }

    @Override
    public Map<String, String> start() {
        try {
            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Não subiu o CNPJ.ws simulado", e);
        }
        servidor.createContext("/", this::tratar);
        servidor.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        servidor.start();
        return Map.of(PROP_URL, "http://127.0.0.1:" + servidor.getAddress().getPort() + "/cnpj");
    }

    private void tratar(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.equals("/_tokens")) {
                responder(ex, 200, String.join("\n", tokens));
                return;
            }
            if (path.startsWith("/_contagem/")) {
                responder(ex, 200, String.valueOf(contagem.getOrDefault(path.substring("/_contagem/".length()), 0)));
                return;
            }
            String[] partes = path.split("/"); // "", "cnpj", cnpj14
            if (partes.length != 3 || !"cnpj".equals(partes[1])) {
                responder(ex, 404, "{}");
                return;
            }
            String cnpj = partes[2];
            contagem.merge(cnpj, 1, Integer::sum);
            String token = parametro(ex.getRequestURI().getRawQuery(), "token");
            if (token == null) {
                responder(ex, 401, "{\"titulo\":\"sem token\"}");
                return;
            }
            tokens.add(token);
            switch (cnpj.substring(0, 8)) {
                case RAIZ_COMPLETO -> responder(ex, 200, COMPLETO);
                case RAIZ_CAD_SIMPLES -> responder(ex, 200, cadastro("Sim", "Não", IBGE_CADASTRO, true));
                case RAIZ_CAD_MEI -> responder(ex, 200, cadastro("Sim", "Sim", "9199998", false));
                case RAIZ_CAD_NORMAL -> responder(ex, 200, cadastro("Não", "Não", "9199998", false));
                case RAIZ_MINIMO -> responder(ex, 200, "{\"razao_social\":\"Empresa Minima Ltda\"}");
                case RAIZ_404 -> responder(ex, 404, "{\"titulo\":\"Nao encontrado\"}");
                case RAIZ_401 -> responder(ex, 401, "{\"titulo\":\"token invalido\"}");
                case RAIZ_403 -> responder(ex, 403, "{\"titulo\":\"proibido\"}");
                case RAIZ_429 -> {
                    ex.getResponseHeaders().add("Retry-After", "30");
                    responder(ex, 429, "{\"titulo\":\"limite\"}");
                }
                case RAIZ_429_SEM_ESPERA -> responder(ex, 429, "{\"titulo\":\"limite\"}");
                case RAIZ_500 -> responder(ex, 500, "{\"titulo\":\"erro\"}");
                case RAIZ_LENTO -> {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    responder(ex, 200, COMPLETO);
                }
                case RAIZ_JSON_INVALIDO -> responder(ex, 200, "<html>nao e json</html>");
                case RAIZ_ARRAY -> responder(ex, 200, "[1,2,3]");
                case RAIZ_GIGANTE_CHUNKED -> gigante(ex, false);
                case RAIZ_GIGANTE_LENGTH -> gigante(ex, true);
                case RAIZ_REDIRECT -> {
                    ex.getResponseHeaders().add("Location", "/cnpj/_redirecionado");
                    responder(ex, 302, "");
                }
                default -> responder(ex, 404, "{}");
            }
        } catch (IOException e) {
            // cliente desistiu (timeout ou corpo cortado): normal nos testes
        } finally {
            ex.close();
        }
    }

    private static String parametro(String query, String nome) {
        if (query == null) {
            return null;
        }
        for (String p : query.split("&")) {
            int i = p.indexOf('=');
            if (i > 0 && p.substring(0, i).equals(nome)) {
                return java.net.URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static void gigante(HttpExchange ex, boolean comLength) throws IOException {
        byte[] bloco = new byte[64 * 1024];
        java.util.Arrays.fill(bloco, (byte) 'a');
        long total = 1_300_000;
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, comLength ? total : 0);
        long enviado = 0;
        while (enviado < total) {
            int n = (int) Math.min(bloco.length, total - enviado);
            ex.getResponseBody().write(bloco, 0, n);
            enviado += n;
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

    private static String cadastro(String simples, String mei, String ibge, boolean comIes) {
        String ies = comIes ? """
                [
                  {"inscricao_estadual": "111111111", "ativo": false, "estado": {"sigla": "RJ"}},
                  {"inscricao_estadual": "222222222", "ativo": true, "estado": {"sigla": "SP"}},
                  {"inscricao_estadual": "333333333", "ativo": true, "estado": {"sigla": "RJ"}}
                ]""" : "[]";
        return """
                {
                  "razao_social": "CADASTRO TESTE LTDA",
                  "simples": {"simples": "%s", "mei": "%s"},
                  "estabelecimento": {
                    "nome_fantasia": "CADASTRO FANTASIA",
                    "situacao_cadastral": "Ativa",
                    "data_inicio_atividade": "2010-03-15",
                    "tipo_logradouro": "AVENIDA",
                    "logradouro": "PAULISTA",
                    "numero": "1000",
                    "complemento": "SALA 5",
                    "bairro": "BELA VISTA",
                    "cep": "01310100",
                    "ddd1": "11",
                    "telefone1": "3333-4444",
                    "email": "cad@exemplo.com.br",
                    "estado": {"id": 19, "nome": "Rio de Janeiro", "sigla": "RJ"},
                    "cidade": {"id": 1, "nome": "Cidade do Provedor", "ibge_id": %s},
                    "inscricoes_estaduais": %s
                  }
                }
                """.formatted(simples, mei, ibge, ies);
    }

    private static final String COMPLETO = """
            {
              "cnpj_raiz": "27865757",
              "razao_social": "GLOBO COMUNICACAO E PARTICIPACOES S/A",
              "capital_social": "1273650139.00",
              "porte": {"id": "5", "descricao": "Demais"},
              "natureza_juridica": {"id": "2046", "descricao": "Sociedade Anônima Aberta"},
              "campo_desconhecido": {"x": [1, 2, 3]},
              "simples": {"simples": "Não", "mei": "Sim"},
              "socios": [{"nome": "A"}, {"nome": "B"}, {"nome": "C"}],
              "estabelecimento": {
                "cnpj": "27865757000102",
                "nome_fantasia": "TV GLOBO",
                "situacao_cadastral": "Ativa",
                "data_inicio_atividade": "1998-01-05",
                "tipo_logradouro": "RUA",
                "logradouro": "LOPES QUINTAS",
                "numero": "303",
                "complemento": "PARTE",
                "bairro": "JARDIM BOTANICO",
                "cep": "22460901",
                "ddd1": "21",
                "telefone1": "25402000",
                "email": "contato@exemplo.com.br",
                "atividade_principal": {"id": "6021700", "subclasse": "6021-7/00", "descricao": "Atividades de televisão aberta"},
                "estado": {"id": 19, "nome": "Rio de Janeiro", "sigla": "RJ"},
                "cidade": {"id": 3243, "nome": "Rio de Janeiro"}
              }
            }
            """;
}
