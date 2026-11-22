package br.com.emitesimples.cep;

import br.com.emitesimples.cep.CepDtos.Endereco;
import br.com.emitesimples.security.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Cliente do ViaCEP (urlBase + "/" + cep + "/json/"). Timeout curto, sem redirecionamento, resposta limitada em tamanho.
 * Qualquer falha vira 502 servico_cep_indisponivel com mensagem generica: nunca vaza URL, host nem pilha
 * (o detalhe tecnico, sem o CEP, vai so para o log).
 */
@ApplicationScoped
public class ViaCepCliente {

    private static final Logger LOG = Logger.getLogger(ViaCepCliente.class);
    private static final int MAX_BYTES = 16 * 1024;
    static final String MSG_INDISPONIVEL = "Não foi possível consultar o CEP agora. Tente novamente em instantes.";

    /** Resultado da consulta: o endereço, ou {@code null} quando o ViaCEP diz que o CEP não existe. */
    public record Resultado(Endereco endereco) {
    }

    @ConfigProperty(name = "emitesimples.cep.url-base", defaultValue = "https://viacep.com.br/ws")
    String urlBase;

    @ConfigProperty(name = "emitesimples.cep.timeout-ms", defaultValue = "3000")
    long timeoutMs;

    private final ObjectMapper json = new ObjectMapper();
    private HttpClient http;

    @PostConstruct
    void iniciar() {
        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.min(timeoutMs, 2000)))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** @param cep 8 digitos, ja validado */
    public Resultado buscar(String cep) {
        try {
            String base = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/" + cep + "/json/"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            byte[] corpo;
            try (InputStream in = resp.body()) {
                corpo = in.readNBytes(MAX_BYTES + 1);
            }
            if (resp.statusCode() != 200 || corpo.length > MAX_BYTES) {
                throw indisponivel("status " + resp.statusCode());
            }
            JsonNode n = json.readTree(new String(corpo, StandardCharsets.UTF_8));
            if (n == null || !n.isObject()) {
                throw indisponivel("resposta inesperada");
            }
            if (n.has("erro") && "true".equalsIgnoreCase(n.get("erro").asText())) {
                return new Resultado(null);
            }
            return new Resultado(new Endereco(cep, texto(n, "logradouro"), texto(n, "complemento"),
                    texto(n, "bairro"), texto(n, "uf"), texto(n, "ibge")));
        } catch (ApiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw indisponivel("interrompida");
        } catch (IOException | RuntimeException e) {
            throw indisponivel(e.getClass().getSimpleName());
        }
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n.get(campo);
        return v == null || v.isNull() ? "" : v.asText("").trim();
    }

    private static ApiException indisponivel(String motivo) {
        LOG.warnf("Consulta de CEP falhou (%s).", motivo); // sem URL, sem CEP, sem pilha
        return new ApiException(502, "servico_cep_indisponivel", MSG_INDISPONIVEL);
    }
}
