package br.com.emitesimples.apoio;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ViaCEP SIMULADO (servidor HTTP local em porta livre): os testes NUNCA chamam a internet. O comportamento e escolhido pelo
 * CEP pedido (o estado fica aqui dentro, e os testes leem a contagem por GET /_contagem/{cep}):
 * <ul>
 *   <li>99999999: {"erro": true} (CEP inexistente); 88888888: {"erro": "true"} (a forma antiga do ViaCEP)</li>
 *   <li>50000000: HTTP 500; 60000000: demora 5 s (estoura o timeout); 70000000: corpo que nao e JSON</li>
 *   <li>20000000: achado, com IBGE 9999999 (sem linha em Municipio)</li>
 *   <li>qualquer outro: achado, Praca da Se, IBGE 3550308</li>
 * </ul>
 */
public class ViaCepStubResource implements QuarkusTestResourceLifecycleManager {

    private HttpServer servidor;
    private final Map<String, AtomicInteger> contagem = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> start() {
        try {
            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Não subiu o ViaCEP simulado", e);
        }
        servidor.createContext("/", this::tratar);
        servidor.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        servidor.start();
        return Map.of("emitesimples.cep.url-base", "http://127.0.0.1:" + servidor.getAddress().getPort() + "/ws");
    }

    private void tratar(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.startsWith("/_contagem/")) {
                String cep = path.substring("/_contagem/".length());
                responder(ex, 200, String.valueOf(contagem.getOrDefault(cep, new AtomicInteger()).get()));
                return;
            }
            String[] partes = path.split("/"); // "", "ws", cep, "json"
            if (partes.length < 4 || !"ws".equals(partes[1]) || !"json".equals(partes[3])) {
                responder(ex, 404, "{}");
                return;
            }
            String cep = partes[2];
            contagem.computeIfAbsent(cep, k -> new AtomicInteger()).incrementAndGet();
            switch (cep) {
                case "99999999" -> responder(ex, 200, "{\n  \"erro\": true\n}");
                case "88888888" -> responder(ex, 200, "{\n  \"erro\": \"true\"\n}");
                case "50000000" -> responder(ex, 500, "{\"erro\":\"interno\"}");
                case "60000000" -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    responder(ex, 200, "{}");
                }
                case "70000000" -> responder(ex, 200, "<html>nao e json</html>");
                case "20000000" -> responder(ex, 200, achado(cep, "9999999"));
                default -> responder(ex, 200, achado(cep, "3550308"));
            }
        } catch (IOException e) {
            // cliente desistiu (timeout): normal no teste de timeout
        } finally {
            ex.close();
        }
    }

    private static String achado(String cep, String ibge) {
        return "{\"cep\":\"" + cep.substring(0, 5) + "-" + cep.substring(5) + "\",\"logradouro\":\"Praça da Sé\","
                + "\"complemento\":\"lado ímpar\",\"unidade\":\"\",\"bairro\":\"Sé\",\"localidade\":\"São Paulo\","
                + "\"uf\":\"SP\",\"estado\":\"São Paulo\",\"regiao\":\"Sudeste\",\"ibge\":\"" + ibge + "\","
                + "\"gia\":\"1004\",\"ddd\":\"11\",\"siafi\":\"7107\"}";
    }

    private static void responder(HttpExchange ex, int status, String corpo) throws IOException {
        byte[] b = corpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, b.length);
        ex.getResponseBody().write(b);
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }
}
