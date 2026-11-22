package br.com.emitesimples.plataforma;

import br.com.emitesimples.plataforma.ConsultaCnpjResumo.Atividade;
import br.com.emitesimples.plataforma.ConsultaCnpjResumo.Endereco;
import br.com.emitesimples.security.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Cliente do CNPJ.ws: {@code GET {urlBase}/{cnpj14}?token={token}}. Defesas: sem redirecionamento, timeout de conexao e de
 * resposta (inclusive na leitura do corpo), corpo limitado a 1 MB, so https (exceto {@code permitir-url-local}) e todo IP
 * resolvido do host e conferido ANTES da chamada.
 *
 * <p>Janela residual (DNS rebinding): a verificacao do IP e a conexao do HttpClient resolvem o nome em momentos separados;
 * um DNS malicioso poderia responder IP publico na verificacao e interno na conexao. Mitigacoes: a URL base so e editavel
 * pelo administrador da plataforma, nao ha redirecionamento, o corpo nunca e devolvido (so um resumo) e o limite de taxa e
 * baixo. Fechar de vez exigiria conectar direto no IP validado (com SNI/Host do nome) ou um proxy de saida com allowlist.
 *
 * <p>O token so existe como parametro/variavel local: a URL com token e montada no momento da chamada e NUNCA vai para log
 * nem para mensagem de excecao (so se registra host, CNPJ mascarado e status; falhas de rede, so a classe da excecao).
 */
@ApplicationScoped
public class ConsultaCnpjCliente {

    private static final Logger LOG = Logger.getLogger(ConsultaCnpjCliente.class);
    static final int MAX_BYTES = 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    @ConfigProperty(name = "emitesimples.cnpj.permitir-url-local", defaultValue = "false")
    boolean permitirUrlLocal;

    @ConfigProperty(name = "emitesimples.cnpj.timeout-conexao", defaultValue = "5s")
    Duration timeoutConexao;

    @ConfigProperty(name = "emitesimples.cnpj.timeout-resposta", defaultValue = "10s")
    Duration timeoutResposta;

    private HttpClient http;

    @PostConstruct
    void iniciar() {
        http = HttpClient.newBuilder()
                .connectTimeout(timeoutConexao)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * @param urlBase URL base ja validada (sem barra final)
     * @param cnpj 14 digitos validados
     * @param token token em claro (usado so aqui, nunca guardado)
     */
    public ConsultaCnpjResumo consultar(String urlBase, String cnpj, String token) {
        Bruto b = consultarBruto(urlBase, cnpj, token);
        return mapear(b.raiz(), cnpj, b.duracaoMs(), b.urlConsultada());
    }

    /** Resposta ja validada e parseada (objeto JSON), sem mapeamento. {@code urlConsultada} nunca tem o token. */
    public record Bruto(JsonNode raiz, int duracaoMs, String urlConsultada) {
    }

    /** Faz a chamada com TODAS as defesas e devolve o JSON bruto parseado; o mapeamento fica com quem chama. */
    public Bruto consultarBruto(String urlBase, String cnpj, String token) {
        URI base;
        try {
            base = URI.create(urlBase);
        } catch (IllegalArgumentException e) {
            throw urlNaoPermitida();
        }
        String esquema = base.getScheme() == null ? "" : base.getScheme().toLowerCase(Locale.ROOT);
        String host = base.getHost();
        if (host == null || !(esquema.equals("https") || permitirUrlLocal && esquema.equals("http"))) {
            throw urlNaoPermitida();
        }
        verificarHost(host);

        String urlConsultada = urlBase + "/" + cnpj; // SEM token
        String mascara = mascarar(cnpj);
        long inicio = System.nanoTime();
        int status = -1;
        try {
            URI uri;
            try {
                uri = URI.create(urlConsultada + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw urlNaoPermitida(); // sem a causa: ela poderia citar a URL
            }
            HttpRequest req = HttpRequest.newBuilder(uri).timeout(timeoutResposta)
                    .header("Accept", "application/json").GET().build();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            status = resp.statusCode();
            try (InputStream in = resp.body()) {
                // HttpRequest.timeout cobre so ate os cabecalhos: o relogio do corpo e um fechamento agendado do stream.
                CompletableFuture.runAsync(() -> {
                    try {
                        in.close();
                    } catch (IOException ignorada) {
                        // ja fechado
                    }
                }, CompletableFuture.delayedExecutor(timeoutResposta.toMillis(), TimeUnit.MILLISECONDS));
                tratarStatus(resp, status);
                if (resp.headers().firstValueAsLong("Content-Length").orElse(0) > MAX_BYTES) {
                    throw respostaInvalida();
                }
                byte[] corpo = in.readNBytes(MAX_BYTES + 1);
                if (corpo.length > MAX_BYTES) {
                    throw respostaInvalida();
                }
                JsonNode raiz;
                try {
                    raiz = JSON.readTree(corpo);
                } catch (IOException e) {
                    throw respostaInvalida(); // sem a causa: ela cita trecho do corpo
                }
                if (raiz == null || !raiz.isObject()) {
                    throw respostaInvalida();
                }
                int ms = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
                LOG.infof("Consulta CNPJ: host=%s cnpj=%s status=%d (%d ms)", host, mascara, status, ms);
                return new Bruto(raiz, ms, urlConsultada);
            }
        } catch (ApiException e) {
            LOG.infof("Consulta CNPJ: host=%s cnpj=%s status=%d -> %s", host, mascara, status, e.getCodigo());
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warnf("Consulta CNPJ: host=%s cnpj=%s interrompida", host, mascara);
            throw indisponivel();
        } catch (IOException | RuntimeException e) {
            LOG.warnf("Consulta CNPJ: host=%s cnpj=%s falhou (%s)", host, mascara, e.getClass().getSimpleName());
            throw indisponivel();
        }
    }

    private static void tratarStatus(HttpResponse<?> resp, int status) {
        if (status == 200) {
            return;
        }
        switch (status) {
            case 404 -> throw new ApiException(404, "cnpj_nao_encontrado", "CNPJ não encontrado no CNPJ.ws.");
            case 401, 403 -> throw new ApiException(502, "token_recusado",
                    "O CNPJ.ws recusou o token configurado. Confira o token.");
            case 429 -> {
                Long espera = resp.headers().firstValue("Retry-After").map(ConsultaCnpjCliente::segundos).orElse(null);
                throw new ApiException(429, "limite_consulta_excedido",
                        "O limite de consultas do CNPJ.ws foi excedido. Tente novamente mais tarde.",
                        espera == null ? null : java.util.Map.of("esperaSegundos", espera));
            }
            default -> throw respostaInvalida(); // inclui 3xx (redirecionamento nao e seguido) e 5xx
        }
    }

    private static Long segundos(String v) {
        try {
            long n = Long.parseLong(v.trim());
            return n >= 0 && n < 86_400 ? n : null;
        } catch (NumberFormatException e) {
            return null; // formato de data HTTP: nao repassamos
        }
    }

    // ------------------------------------------------------------------------------------- host / SSRF

    /** Resolve o host e recusa se QUALQUER IP for interno. Sem efeito com permitir-url-local. */
    void verificarHost(String host) {
        if (permitirUrlLocal) {
            return;
        }
        InetAddress[] ips;
        try {
            ips = InetAddress.getAllByName(host);
        } catch (IOException | RuntimeException e) {
            LOG.warnf("Consulta CNPJ: host=%s nao resolvido", host);
            throw indisponivel();
        }
        verificarEnderecos(ips);
    }

    static void verificarEnderecos(InetAddress[] ips) {
        if (ips == null || ips.length == 0) {
            throw urlNaoPermitida();
        }
        for (InetAddress ip : ips) {
            if (ConfiguracaoConsultaCnpjService.ipProibido(ip)) {
                throw urlNaoPermitida();
            }
        }
    }

    // ------------------------------------------------------------------------------------- mapeamento

    static ConsultaCnpjResumo mapear(JsonNode r, String cnpj, int duracaoMs, String urlConsultada) {
        JsonNode est = r.path("estabelecimento");
        JsonNode ativ = est.path("atividade_principal");
        String codigo = texto(ativ, "subclasse");
        if (codigo == null) {
            codigo = texto(ativ, "id");
        }
        Atividade atividade = ativ.isObject() && (codigo != null || texto(ativ, "descricao") != null)
                ? new Atividade(codigo, texto(ativ, "descricao")) : null;

        String tipo = texto(est, "tipo_logradouro");
        String logr = texto(est, "logradouro");
        String logradouro = logr == null ? tipo : tipo == null ? logr : tipo + " " + logr;
        Endereco endereco = new Endereco(logradouro, texto(est, "numero"), texto(est, "complemento"),
                texto(est, "bairro"), texto(est, "cep"), texto(est.path("cidade"), "nome"),
                texto(est.path("estado"), "sigla"));
        if (endereco.logradouro() == null && endereco.numero() == null && endereco.complemento() == null
                && endereco.bairro() == null && endereco.cep() == null && endereco.municipio() == null
                && endereco.uf() == null) {
            endereco = null;
        }

        String ddd = texto(est, "ddd1");
        String fone = texto(est, "telefone1");
        String telefone = fone == null ? null : ddd == null ? fone : ddd + fone;

        JsonNode simples = r.path("simples");
        Boolean optanteSimples = simples.isObject() && texto(simples, "simples") != null
                ? "sim".equalsIgnoreCase(texto(simples, "simples")) : null;
        Boolean mei = simples.isObject() && texto(simples, "mei") != null
                ? "sim".equalsIgnoreCase(texto(simples, "mei")) : null;

        JsonNode socios = r.path("socios");
        return new ConsultaCnpjResumo(cnpj, texto(r, "razao_social"), texto(est, "nome_fantasia"),
                texto(est, "situacao_cadastral"), texto(est, "data_inicio_atividade"),
                texto(r.path("porte"), "descricao"), texto(r.path("natureza_juridica"), "descricao"), atividade,
                texto(r, "capital_social"), endereco, texto(est, "email"), telefone, optanteSimples, mei,
                socios.isArray() ? socios.size() : 0, duracaoMs, urlConsultada);
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n == null ? null : n.get(campo);
        if (v == null || v.isNull() || !v.isValueNode()) {
            return null;
        }
        String s = v.asText("").trim();
        return s.isEmpty() ? null : s;
    }

    // ------------------------------------------------------------------------------------- utilidades

    /** 27865757000102 -> 27.865.757/****-** */
    static String mascarar(String cnpj) {
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/****-**";
    }

    private static ApiException respostaInvalida() {
        return new ApiException(502, "resposta_invalida", "O CNPJ.ws devolveu uma resposta inesperada.");
    }

    private static ApiException indisponivel() {
        return new ApiException(504, "servico_cnpj_indisponivel",
                "Não foi possível consultar o CNPJ.ws agora (tempo esgotado ou falha de rede).");
    }

    private static ApiException urlNaoPermitida() {
        return new ApiException(409, "url_base_nao_permitida",
                "A URL base configurada não é permitida. Corrija a configuração da consulta de CNPJ.");
    }
}
