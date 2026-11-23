package br.com.emitesimples.gateway;

import br.com.emitesimples.plataforma.ConfiguracaoConsultaCnpjService;
import br.com.emitesimples.security.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Cliente HTTP do gateway IntegraNotas (auth: header {@code Authorization: <token>} CRU, sem Bearer). Mesmas defesas de
 * {@code ConsultaCnpjCliente}: sem redirecionamento, timeout de conexao e de resposta (inclusive na leitura do corpo),
 * corpo limitado a 1 MB, so https (exceto {@code emitesimples.integranotas.permitir-url-local}, dev/test) e todo IP resolvido
 * do host conferido ANTES de conectar (janela residual de DNS rebinding: ver {@code ConsultaCnpjCliente}).
 *
 * <p>Os tokens so existem como parametro/variavel local: nunca sao campo, nunca vao para log nem para excecao, e o corpo da
 * resposta (que pode trazer o token do emitente) nunca e registrado. Decide-se SEMPRE pelo {@code codigo} do JSON (nunca pelo
 * texto da mensagem). Codigos estaveis de erro: {@code integranotas_token_recusado}, {@code integranotas_recusou},
 * {@code integranotas_indisponivel}, {@code integranotas_resposta_invalida}.
 *
 * <p>Proximas fases (certificado, envio, consulta, cancelamento) reaproveitam {@link #chamar} e {@link #interpretar}: so
 * montam o corpo e leem os campos do sucesso. NAO implementadas aqui.
 */
@ApplicationScoped
public class IntegraNotasCliente {

    private static final Logger LOG = Logger.getLogger(IntegraNotasCliente.class);
    static final int MAX_BYTES = 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ERROS = 10;
    private static final int MAX_TEXTO = 200;

    @ConfigProperty(name = "emitesimples.integranotas.permitir-url-local", defaultValue = "false")
    boolean permitirUrlLocal;

    @ConfigProperty(name = "emitesimples.integranotas.timeout-conexao", defaultValue = "5s")
    Duration timeoutConexao;

    @ConfigProperty(name = "emitesimples.integranotas.timeout-resposta", defaultValue = "10s")
    Duration timeoutResposta;

    private HttpClient http;

    @PostConstruct
    void iniciar() {
        http = HttpClient.newBuilder()
                .connectTimeout(timeoutConexao)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // ------------------------------------------------------------------------------------- dados

    /** Dados do cadastro do emitente. {@code cnpj} OU {@code cpf}; opcionais podem ser null. */
    public record EmitenteNovo(String nome, String razao, int crt, String telefone, String rua, String numero,
                               String bairro, String cmun, String municipio, String uf, String cep, String cnpj,
                               String cpf, String im, String email, String complemento, Integer regimeCibs,
                               String webhook) {
    }

    /** Token do emitente devolvido pelo gateway: segredo, por isso o toString o esconde. */
    public record EmitenteCriado(String token) {
        @Override
        public String toString() {
            return "EmitenteCriado[token=***]";
        }
    }

    /** Resposta ja lida e parseada (objeto JSON); {@code codigo} pode ser null se o gateway nao o enviou. */
    public record Resposta(int status, JsonNode raiz) {
    }

    // ------------------------------------------------------------------------------------- operacoes

    /**
     * {@code POST {urlBase}/v1/soft/emitente} com o token da SOFTHOUSE. Sucesso: {@code {sucesso:true, codigo:200, token}}.
     *
     * @param urlBase URL base ja validada (sem barra final)
     * @param tokenSofthouse token da softhouse em claro (usado so aqui, nunca guardado)
     */
    public EmitenteCriado criarEmitente(String urlBase, String tokenSofthouse, EmitenteNovo d) {
        ObjectNode corpo = JSON.createObjectNode();
        corpo.put("nome", d.nome());
        corpo.put("razao", d.razao());
        corpo.put("crt", d.crt());
        corpo.put("telefone", d.telefone());
        corpo.put("rua", d.rua());
        corpo.put("numero", d.numero());
        corpo.put("bairro", d.bairro());
        corpo.put("cmun", d.cmun());
        corpo.put("municipio", d.municipio());
        corpo.put("uf", d.uf());
        corpo.put("cep", d.cep());
        if (d.cnpj() != null) {
            corpo.put("cnpj", d.cnpj());
        }
        if (d.cpf() != null) {
            corpo.put("cpf", d.cpf());
        }
        if (d.im() != null) {
            corpo.put("im", d.im());
        }
        if (d.email() != null) {
            corpo.put("email", d.email());
        }
        if (d.complemento() != null) {
            corpo.put("complemento", d.complemento());
        }
        if (d.regimeCibs() != null) {
            corpo.put("regime_cibs", d.regimeCibs().intValue());
        }
        if (d.webhook() != null) {
            corpo.put("webhook", d.webhook());
        }
        corpo.putObject("documentos").put("nfse", true);

        Resposta r = chamar("POST", urlBase, "/v1/soft/emitente", tokenSofthouse, corpo);
        JsonNode raiz = interpretar(r, "cadastro do emitente");
        String token = raiz.path("token").isTextual() ? raiz.path("token").asText().trim() : "";
        if (token.isEmpty()) {
            throw respostaInvalida();
        }
        return new EmitenteCriado(token);
    }

    // ------------------------------------------------------------------------------------- emitente existente

    /** Rota da listagem de emitentes da softhouse. A CONFIRMAR EM HOMOLOGACAO (o metodo/rota exatos nao estao confirmados). */
    static final String ROTA_LISTA_EMITENTES = "/v1/soft/emitente";
    /** Rota de atualizacao do emitente (token do EMITENTE). Confirmada pelo usuario: PUT /v1/emitente. */
    static final String ROTA_ATUALIZA_EMITENTE = "/v1/emitente";
    /** Rota para registrar o webhook de um emitente existente (token do EMITENTE). A CONFIRMAR EM HOMOLOGACAO. */
    static final String ROTA_WEBHOOK_EMITENTE = "/v1/emitente/webhook";
    /** Codigo generico da tabela do gateway: "Voce esta tentando criar algo que ja existe." */
    static final int GW_JA_EXISTE = 5008;

    /**
     * {@code PUT {urlBase}/v1/emitente} com o token do EMITENTE: sincroniza os dados (mesmo mapeamento do cadastro, SEM
     * cnpj/cpf, SEM {@code documentos} e SEM webhook). Nao cria e nao devolve token. Validacao recusada (5001...): 502
     * {@code integranotas_recusou} com {@code detalhes.erros}; token do emitente recusado (6000-6003): 502
     * {@code integranotas_token_recusado}.
     */
    public void atualizarEmitente(String urlBase, String tokenEmitente, EmitenteNovo d) {
        ObjectNode corpo = JSON.createObjectNode();
        corpo.put("nome", d.nome());
        corpo.put("razao", d.razao());
        corpo.put("crt", d.crt());
        corpo.put("telefone", d.telefone());
        corpo.put("rua", d.rua());
        corpo.put("numero", d.numero());
        corpo.put("bairro", d.bairro());
        corpo.put("cmun", d.cmun());
        corpo.put("municipio", d.municipio());
        corpo.put("uf", d.uf());
        corpo.put("cep", d.cep());
        if (d.im() != null) {
            corpo.put("im", d.im());
        }
        if (d.email() != null) {
            corpo.put("email", d.email());
        }
        if (d.complemento() != null) {
            corpo.put("complemento", d.complemento());
        }
        if (d.regimeCibs() != null) {
            corpo.put("regime_cibs", d.regimeCibs().intValue());
        }
        interpretar(chamar("PUT", urlBase, ROTA_ATUALIZA_EMITENTE, tokenEmitente, corpo), "atualização do emitente", true);
    }

    /**
     * Procura o emitente pelo CNPJ/CPF (so digitos, comparacao normalizada) na lista da softhouse e devolve SO o token dele.
     * A lista traz tokens de OUTROS emitentes: nada dela e guardado, registrado ou devolvido. Rota A CONFIRMAR EM HOMOLOGACAO.
     * Lanca as excecoes estaveis do gateway (token recusado, indisponivel, resposta invalida).
     */
    public Optional<String> tokenDoEmitente(String urlBase, String tokenSofthouse, String documentoDigitos) {
        Resposta r = chamar("GET", urlBase, ROTA_LISTA_EMITENTES, tokenSofthouse, null, MAX_BYTES_CONSULTA);
        JsonNode raiz = interpretar(r, "listagem de emitentes");
        JsonNode lista = raiz.get("emitentes");
        if (lista == null || !lista.isArray()) {
            throw respostaInvalida();
        }
        for (JsonNode e : lista) {
            String doc = texto(e, "cnpj");
            if (doc == null) {
                doc = texto(e, "cpf");
            }
            if (doc != null && documentoDigitos.equals(doc.replaceAll("[^0-9]", ""))) {
                String token = texto(e, "token");
                return token == null || token.isBlank() ? Optional.empty() : Optional.of(token.strip());
            }
        }
        return Optional.empty();
    }

    /** {@code POST {urlBase}/v1/emitente/webhook} {@code {webhook}} com o token do EMITENTE. Rota A CONFIRMAR EM HOMOLOGACAO. */
    public void atualizarWebhook(String urlBase, String tokenEmitente, String urlWebhook) {
        ObjectNode corpo = JSON.createObjectNode();
        corpo.put("webhook", urlWebhook);
        interpretar(chamar("POST", urlBase, ROTA_WEBHOOK_EMITENTE, tokenEmitente, corpo), "registro do webhook", true);
    }

    /**
     * O gateway recusou o cadastro porque o emitente JA existe? Lugar UNICO da deteccao: codigo 5008 OU texto com
     * "ja existe" / "ja cadastrado" (com ou sem acento, qualquer caixa) na mensagem ou nos erros. O codigo especifico de
     * "CNPJ ja existe" nao esta documentado.
     */
    public static boolean emitenteJaExiste(ApiException falha) {
        if (falha == null || !"integranotas_recusou".equals(falha.getCodigo())) {
            return false;
        }
        StringBuilder texto = new StringBuilder(String.valueOf(falha.getMessage()));
        if (falha.getDetalhes() instanceof Map<?, ?> det) {
            if (Integer.valueOf(GW_JA_EXISTE).equals(det.get("codigoGateway"))) {
                return true;
            }
            if (det.get("erros") instanceof List<?> erros) {
                erros.forEach(x -> texto.append(' ').append(x));
            }
        }
        String t = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return t.contains("ja existe") || t.contains("ja cadastrado");
    }

    /** Codigos do gateway para certificado recusado (decide-se pelo codigo, nunca pelo texto). */
    static final int GW_CERTIFICADO_INVALIDO = 5003;
    static final int GW_CERTIFICADO_VENCIDO = 5011;
    static final int GW_CERTIFICADO_NAO_PERTENCE = 5029;

    /**
     * {@code POST {urlBase}/v1/certificado} com o token do EMITENTE: {@code {certificado: <Base64 do .pfx>, senha}}. Sucesso: o
     * metodo volta sem excecao. Recusas do certificado ({@code sucesso:false}): 5003 -> 400 {@code certificado_invalido_gateway};
     * 5011 -> 400 {@code certificado_vencido_gateway}; 5029 -> 400 {@code certificado_nao_pertence_gateway}. Token recusado,
     * indisponivel e resposta invalida seguem os codigos estaveis do cadastro do emitente. O pfx e a senha so existem como
     * parametro: nunca vao para log, excecao ou toString.
     */
    public void enviarCertificado(String urlBase, String tokenEmitente, byte[] pfx, String senha) {
        ObjectNode corpo = JSON.createObjectNode();
        corpo.put("certificado", java.util.Base64.getEncoder().encodeToString(pfx));
        corpo.put("senha", senha);
        Resposta r = chamar("POST", urlBase, "/v1/certificado", tokenEmitente, corpo);
        JsonNode raiz = r.raiz();
        int status = r.status();
        if (raiz != null && raiz.isObject() && (status == 200 || status >= 400 && status < 500 && status != 401 && status != 403)
                && raiz.path("sucesso").isBoolean() && !raiz.path("sucesso").asBoolean()
                && raiz.path("codigo").isIntegralNumber()) {
            int codigo = raiz.path("codigo").asInt();
            if (codigo == GW_CERTIFICADO_INVALIDO) {
                throw new ApiException(400, "certificado_invalido_gateway",
                        "A IntegraNotas não conseguiu abrir o certificado (arquivo corrompido ou senha incorreta).");
            }
            if (codigo == GW_CERTIFICADO_VENCIDO) {
                throw new ApiException(400, "certificado_vencido_gateway",
                        "A IntegraNotas informou que o certificado está vencido.");
            }
            if (codigo == GW_CERTIFICADO_NAO_PERTENCE) {
                throw new ApiException(400, "certificado_nao_pertence_gateway",
                        "A IntegraNotas informou que o certificado não pertence ao CNPJ/CPF do emitente.");
            }
        }
        interpretar(r, "envio do certificado", true);
    }

    // ------------------------------------------------------------------------------------- NFS-e (envio v2 / consulta v1)

    /** Teto do corpo da CONSULTA (traz xml e pdf em Base64, ate 2 MB cada depois de decodificados). */
    static final int MAX_BYTES_CONSULTA = 6 * 1024 * 1024;

    /**
     * Retorno ja interpretado do envio ou da consulta de NFS-e: {@code codigo} sempre presente (decide-se SO por ele),
     * {@code sucesso} pode ser null, {@code chave} so se tiver 1 a 50 digitos. {@code raiz} e o JSON inteiro (traz xml/pdf:
     * nunca registrar). Falhas de infraestrutura NAO viram retorno: lancam {@link ApiException} com os codigos estaveis
     * {@code integranotas_token_recusado}, {@code integranotas_indisponivel}, {@code integranotas_resposta_invalida}.
     */
    public record RetornoNfse(int httpStatus, int codigo, Boolean sucesso, String mensagem, String chave, JsonNode raiz) {
        @Override
        public String toString() {
            return "RetornoNfse[codigo=" + codigo + "]";
        }
    }

    /** {@code POST {urlBase}/v2/nfse} com o token do EMITENTE (header cru). O prestador nao vai no corpo. */
    public RetornoNfse enviarNfse(String urlBase, String tokenEmitente, JsonNode payload) {
        return interpretarNfse(chamar("POST", urlBase, "/v2/nfse", tokenEmitente, payload));
    }

    /** {@code GET {urlBase}/v1/nfse/{chave}} com o token do EMITENTE. */
    public RetornoNfse consultarNfse(String urlBase, String tokenEmitente, String chave) {
        if (chave == null || !chave.matches("[0-9]{1,50}")) {
            throw respostaInvalida();
        }
        return interpretarNfse(chamar("GET", urlBase, "/v1/nfse/" + chave, tokenEmitente, null, MAX_BYTES_CONSULTA));
    }

    /**
     * {@code POST {urlBase}/v1/nfse/cancela} com o token do EMITENTE. O corpo ({@code chave, codigo_cancelamento,
     * motivo_cancelamento}) e montado em {@code NfsePayloadMapper}. A resposta de sucesso traz xml/pdf do evento (Base64).
     */
    public RetornoNfse cancelarNfse(String urlBase, String tokenEmitente, JsonNode payload) {
        return interpretarNfse(chamar("POST", urlBase, "/v1/nfse/cancela", tokenEmitente, payload, MAX_BYTES_CONSULTA));
    }

    static RetornoNfse interpretarNfse(Resposta r) {
        int status = r.status();
        JsonNode raiz = r.raiz();
        Integer codigo = raiz != null && raiz.path("codigo").isIntegralNumber() && raiz.path("codigo").canConvertToInt()
                ? raiz.path("codigo").asInt() : null;
        if (codigo != null && codigo >= 6000 && codigo <= 6003 || status == 401 || status == 403) {
            throw tokenRecusado(true);
        }
        if (raiz == null || !raiz.isObject() || codigo == null || !(status == 200 || status >= 400 && status < 500)) {
            throw respostaInvalida();
        }
        Boolean sucesso = raiz.path("sucesso").isBoolean() ? raiz.path("sucesso").asBoolean() : null;
        String msg = raiz.path("mensagem").isTextual()
                ? raiz.path("mensagem").asText().replaceAll("[\\p{Cntrl}]", " ").strip() : null;
        if (msg != null && msg.isEmpty()) {
            msg = null;
        }
        if (msg != null && msg.length() > 500) {
            msg = msg.substring(0, 500);
        }
        String chave = raiz.path("chave").isTextual() ? raiz.path("chave").asText().strip() : null;
        if (chave != null && !chave.matches("[0-9]{1,50}")) {
            chave = null;
        }
        return new RetornoNfse(status, codigo, sucesso, msg, chave, raiz);
    }

    // ------------------------------------------------------------------------------------- nucleo

    /**
     * Interpreta uma resposta do gateway: devolve a raiz quando e SUCESSO ({@code sucesso:true} e {@code codigo} 2xx ou
     * 5023/100/101 etc. que a operacao aceitar: aqui, so 200) e lanca a excecao de dominio nos demais casos.
     * Decide pelo {@code codigo}, nunca pelo texto.
     */
    static JsonNode interpretar(Resposta r, String operacao) {
        return interpretar(r, operacao, false);
    }

    /**
     * {@code doEmitente=true}: a operacao usa o token do EMITENTE (mensagem de token recusado propria) e aceita sucesso sem
     * {@code codigo} (a doc do certificado nao mostra o corpo de sucesso); se o {@code codigo} vier, tem de ser 200.
     */
    static JsonNode interpretar(Resposta r, String operacao, boolean doEmitente) {
        int status = r.status();
        JsonNode raiz = r.raiz();
        Integer codigo = raiz != null && raiz.path("codigo").canConvertToInt() && raiz.path("codigo").isNumber()
                ? raiz.path("codigo").asInt() : null;
        if (codigo != null && codigo >= 6000 && codigo <= 6003) {
            throw tokenRecusado(doEmitente);
        }
        if (status == 401 || status == 403) {
            throw tokenRecusado(doEmitente);
        }
        if (raiz == null || !raiz.isObject() || !(status == 200 || status >= 400 && status < 500)) {
            throw respostaInvalida(); // 3xx (nao seguido), 5xx, corpo que nao e objeto
        }
        JsonNode sucesso = raiz.get("sucesso");
        if (sucesso == null || !sucesso.isBoolean()) {
            throw respostaInvalida();
        }
        if (!sucesso.asBoolean()) {
            throw recusou(raiz, codigo, operacao);
        }
        if (status != 200 || (codigo == null ? !doEmitente : codigo != 200)) {
            throw respostaInvalida();
        }
        return raiz;
    }

    /** Faz a chamada com TODAS as defesas e devolve status + JSON parseado. Nunca registra corpo, token ou cabecalhos. */
    public Resposta chamar(String metodo, String urlBase, String caminho, String token, JsonNode corpo) {
        return chamar(metodo, urlBase, caminho, token, corpo, MAX_BYTES);
    }

    /** Igual a {@link #chamar(String, String, String, String, JsonNode)} com teto proprio do corpo da resposta. */
    Resposta chamar(String metodo, String urlBase, String caminho, String token, JsonNode corpo, int maxBytes) {
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

        long inicio = System.nanoTime();
        int status = -1;
        try {
            HttpRequest.Builder b;
            try {
                b = HttpRequest.newBuilder(URI.create(urlBase + caminho));
            } catch (IllegalArgumentException e) {
                throw urlNaoPermitida();
            }
            b.timeout(timeoutResposta).header("Accept", "application/json").header("Authorization", token);
            if (corpo == null) {
                b.method(metodo, HttpRequest.BodyPublishers.noBody());
            } else {
                b.header("Content-Type", "application/json");
                b.method(metodo, HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(corpo)));
            }
            HttpResponse<InputStream> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofInputStream());
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
                if (status >= 300 && status < 400) {
                    throw respostaInvalida();
                }
                if (resp.headers().firstValueAsLong("Content-Length").orElse(0) > maxBytes) {
                    throw respostaInvalida();
                }
                byte[] bytes = in.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes) {
                    throw respostaInvalida();
                }
                JsonNode raiz = null;
                if (bytes.length > 0) {
                    try {
                        raiz = JSON.readTree(bytes);
                    } catch (IOException e) {
                        raiz = null; // sem a causa: ela cita trecho do corpo
                    }
                }
                int ms = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);
                LOG.infof("IntegraNotas: %s host=%s %s status=%d (%d ms)", metodo, host, caminho, status, ms);
                return new Resposta(status, raiz);
            }
        } catch (ApiException e) {
            LOG.infof("IntegraNotas: %s host=%s %s status=%d -> %s", metodo, host, caminho, status, e.getCodigo());
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warnf("IntegraNotas: %s host=%s %s interrompida", metodo, host, caminho);
            throw indisponivel();
        } catch (JsonProcessingException e) {
            LOG.warnf("IntegraNotas: %s host=%s %s corpo nao serializavel", metodo, host, caminho);
            throw indisponivel();
        } catch (IOException | RuntimeException e) {
            LOG.warnf("IntegraNotas: %s host=%s %s falhou (%s)", metodo, host, caminho, e.getClass().getSimpleName());
            throw indisponivel();
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
            LOG.warnf("IntegraNotas: host=%s nao resolvido", host);
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

    // ------------------------------------------------------------------------------------- erros de dominio

    private static ApiException recusou(JsonNode raiz, Integer codigo, String operacao) {
        String msg = limpar(raiz.path("mensagem").isTextual() ? raiz.path("mensagem").asText() : null);
        List<String> resumo = new ArrayList<>();
        JsonNode erros = raiz.get("erros");
        if (erros != null && erros.isArray()) {
            for (JsonNode e : erros) {
                if (resumo.size() >= MAX_ERROS) {
                    break;
                }
                String campo = limpar(texto(e, "campo"));
                String desc = limpar(texto(e, "descricao"));
                if (desc == null) {
                    desc = limpar(texto(e, "erro"));
                }
                if (campo != null && desc != null) {
                    resumo.add(campo + ": " + desc);
                } else if (desc != null || campo != null) {
                    resumo.add(desc != null ? desc : campo);
                }
            }
        }
        StringBuilder m = new StringBuilder("O IntegraNotas recusou o ").append(operacao).append('.');
        if (msg != null) {
            m.append(' ').append(msg);
        }
        Map<String, Object> det = new LinkedHashMap<>();
        if (codigo != null) {
            det.put("codigoGateway", codigo);
        }
        if (!resumo.isEmpty()) {
            det.put("erros", resumo);
        }
        return new ApiException(502, "integranotas_recusou", m.toString(), det.isEmpty() ? null : det);
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n == null ? null : n.get(campo);
        return v != null && v.isValueNode() && !v.isNull() ? v.asText() : null;
    }

    /** Remove controles, apara e limita: texto do gateway e dado nao confiavel. */
    static String limpar(String s) {
        if (s == null) {
            return null;
        }
        String t = s.replaceAll("[\\p{Cntrl}]", " ").strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > MAX_TEXTO ? t.substring(0, MAX_TEXTO) + "..." : t;
    }

    private static ApiException tokenRecusado(boolean doEmitente) {
        return new ApiException(502, "integranotas_token_recusado", doEmitente
                ? "O IntegraNotas recusou o token do emitente. Fale com o suporte."
                : "O IntegraNotas recusou o token da softhouse. Fale com o suporte.");
    }

    private static ApiException respostaInvalida() {
        return new ApiException(502, "integranotas_resposta_invalida", "O IntegraNotas devolveu uma resposta inesperada.");
    }

    private static ApiException indisponivel() {
        return new ApiException(504, "integranotas_indisponivel",
                "Não foi possível falar com o IntegraNotas agora (tempo esgotado ou falha de rede). Tente novamente.");
    }

    private static ApiException urlNaoPermitida() {
        return new ApiException(409, "url_base_nao_permitida",
                "A URL base do IntegraNotas não é permitida. Fale com o suporte.");
    }

    @Override
    public String toString() {
        return "IntegraNotasCliente";
    }

}
