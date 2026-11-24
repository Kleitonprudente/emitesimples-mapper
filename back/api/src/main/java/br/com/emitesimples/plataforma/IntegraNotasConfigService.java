package br.com.emitesimples.plataforma;

import br.com.emitesimples.security.ApiException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuracao do IntegraNotas (singleton 'unica'): ambiente ativo, URLs das APIs, URL publica deste back (webhooks) e os
 * tokens da softhouse (um por ambiente, no cofre {@link SegredoService}). O token NUNCA sai por API: so
 * {@code tokenXConfigurado}; quem precisa dele (as proximas fases) chama {@link #tokenSofthouse(String)}.
 */
@ApplicationScoped
public class IntegraNotasConfigService {

    public static final String HOMOLOGACAO = "HOMOLOGACAO";
    public static final String PRODUCAO = "PRODUCAO";
    static final int MAX_URL = 200;
    static final int MAX_TOKEN = 2000;
    private static final String DOMINIO = "integranotas.com.br";
    private static final Pattern ESPACO = Pattern.compile("\\s");

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @ConfigProperty(name = "emitesimples.integranotas.permitir-producao", defaultValue = "false")
    boolean permitirProducao;

    @ConfigProperty(name = "emitesimples.integranotas.permitir-url-local", defaultValue = "false")
    boolean permitirUrlLocal;

    public record Atual(String ambienteAtivo, String urlHomologacao, String urlProducao, String urlPublicaWebhook,
                        boolean tokenHomologacaoConfigurado, boolean tokenProducaoConfigurado,
                        LocalDateTime dataAtualizacao, String atualizadoPorId, String atualizadoPorNome,
                        boolean producaoPermitida) {
    }

    /** Alteracao parcial: null = mantem; nos tokens e na URL do webhook, "" = remove. */
    public record Alteracao(String ambienteAtivo, String urlHomologacao, String urlProducao, String urlPublicaWebhook,
                            String tokenHomologacao, String tokenProducao) {
    }

    private record Linha(String ambiente, String urlHom, String urlProd, String webhook, String segHom, String segProd,
                         LocalDateTime data, String porId, String porNome) {
    }

    // ------------------------------------------------------------------------------------------- leitura

    public Atual obter() {
        Linha l = linha();
        return new Atual(l.ambiente, l.urlHom, l.urlProd, l.webhook, l.segHom != null, l.segProd != null, l.data, l.porId,
                l.porNome, permitirProducao);
    }

    /** Ambiente ativo: "HOMOLOGACAO" (padrao) ou "PRODUCAO". */
    public String ambienteAtivo() {
        return linha().ambiente;
    }

    /** URL base da API do IntegraNotas do ambiente informado, sem barra final. */
    public String urlBase(String ambiente) {
        Linha l = linha();
        return exigirAmbiente(ambiente).equals(PRODUCAO) ? l.urlProd : l.urlHom;
    }

    /** Token da softhouse do ambiente, em claro (so uso interno; nunca devolver por API nem registrar em log). */
    public Optional<String> tokenSofthouse(String ambiente) {
        Linha l = linha();
        String id = exigirAmbiente(ambiente).equals(PRODUCAO) ? l.segProd : l.segHom;
        return id == null ? Optional.empty() : Optional.of(segredos.ler(id));
    }

    /** Base publica deste back (ex.: https://api.exemplo.com.br), usada para montar a URL de webhook. */
    public Optional<String> urlPublicaWebhook() {
        return Optional.ofNullable(linha().webhook);
    }

    private static String exigirAmbiente(String ambiente) {
        if (!HOMOLOGACAO.equals(ambiente) && !PRODUCAO.equals(ambiente)) {
            throw new IllegalArgumentException("Ambiente desconhecido.");
        }
        return ambiente;
    }

    private Linha linha() {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<?> l = em.createNativeQuery(
                    "select c.ambiente_ativo, c.url_homologacao, c.url_producao, c.url_publica_webhook, "
                            + "c.segredo_softhouse_hom_id, c.segredo_softhouse_prod_id, c.data_atualizacao, u.id, u.nome "
                            + "from configuracao_integranotas c left join usuario u on u.id = c.atualizado_por "
                            + "where c.id = 'unica'").getResultList();
            if (l.isEmpty()) {
                throw new IllegalStateException("Configuração do IntegraNotas ausente (migração V6).");
            }
            Object[] r = (Object[]) l.get(0);
            LocalDateTime data = r[6] == null ? null
                    : r[6] instanceof Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) r[6];
            return new Linha((String) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4], (String) r[5], data,
                    (String) r[7], (String) r[8]);
        });
    }

    // ------------------------------------------------------------------------------------------- gravacao

    public void atualizar(Alteracao a, String usuarioId) {
        // 1) valida TUDO antes de tocar no cofre ou no banco (falha = nada muda)
        String ambiente = a.ambienteAtivo() == null ? null : validarAmbiente(a.ambienteAtivo());
        String urlHom = a.urlHomologacao() == null ? null : validarUrlApi(a.urlHomologacao());
        String urlProd = a.urlProducao() == null ? null : validarUrlApi(a.urlProducao());
        String webhook = a.urlPublicaWebhook() == null ? null
                : a.urlPublicaWebhook().isEmpty() ? "" : validarUrlPublica(a.urlPublicaWebhook());
        if (a.tokenHomologacao() != null && !a.tokenHomologacao().isEmpty()) {
            validarToken(a.tokenHomologacao());
        }
        if (a.tokenProducao() != null && !a.tokenProducao().isEmpty()) {
            validarToken(a.tokenProducao());
        }
        Linha atual = linha();
        // 2) o cofre abre transacoes proprias: segredos antes de gravar a configuracao
        String idHom = atual.segHom;
        String idProd = atual.segProd;
        String apagarHom = null;
        String apagarProd = null;
        String tk = a.tokenHomologacao();
        if (tk != null) {
            if (tk.isEmpty()) {
                idHom = null;
                apagarHom = atual.segHom;
            } else if (atual.segHom == null) {
                idHom = segredos.guardar(tk);
            } else {
                segredos.substituir(atual.segHom, tk);
            }
        }
        tk = a.tokenProducao();
        if (tk != null) {
            if (tk.isEmpty()) {
                idProd = null;
                apagarProd = atual.segProd;
            } else if (atual.segProd == null) {
                idProd = segredos.guardar(tk);
            } else {
                segredos.substituir(atual.segProd, tk);
            }
        }
        String novoAmbiente = ambiente != null ? ambiente : atual.ambiente;
        String novaHom = urlHom != null ? urlHom : atual.urlHom;
        String novaProd = urlProd != null ? urlProd : atual.urlProd;
        String novoWebhook = webhook == null ? atual.webhook : webhook.isEmpty() ? null : webhook;
        String refHom = idHom;
        String refProd = idProd;
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                "update configuracao_integranotas set ambiente_ativo = ?1, url_homologacao = ?2, url_producao = ?3, "
                        + "url_publica_webhook = ?4, segredo_softhouse_hom_id = ?5, segredo_softhouse_prod_id = ?6, "
                        + "data_atualizacao = ?7, atualizado_por = ?8 where id = 'unica'")
                .setParameter(1, novoAmbiente).setParameter(2, novaHom).setParameter(3, novaProd)
                .setParameter(4, novoWebhook).setParameter(5, refHom).setParameter(6, refProd)
                .setParameter(7, Timestamp.valueOf(LocalDateTime.now())).setParameter(8, usuarioId).executeUpdate());
        if (apagarHom != null) {
            segredos.remover(apagarHom);
        }
        if (apagarProd != null) {
            segredos.remover(apagarProd);
        }
    }

    // ------------------------------------------------------------------------------------------- validacoes

    String validarAmbiente(String ambiente) {
        if (!HOMOLOGACAO.equals(ambiente) && !PRODUCAO.equals(ambiente)) {
            throw new ApiException(400, "ambiente_invalido", "Ambiente inválido. Use HOMOLOGACAO ou PRODUCAO.");
        }
        if (PRODUCAO.equals(ambiente) && !permitirProducao) {
            throw new ApiException(409, "producao_bloqueada", "Produção está bloqueada neste servidor.");
        }
        return ambiente;
    }

    static void validarToken(String token) {
        if (token.length() > MAX_TOKEN) {
            throw new ApiException(400, "token_invalido", "O token deve ter no máximo " + MAX_TOKEN + " caracteres.");
        }
        if (ESPACO.matcher(token).find()) {
            throw new ApiException(400, "token_invalido", "O token não pode conter espaços nem quebras de linha.");
        }
    }

    /** URL da API do IntegraNotas: https, host *.integranotas.com.br (local so se permitido), sem userinfo/query. */
    String validarUrlApi(String bruta) {
        URI uri = analisar(bruta);
        String host = uri.getHost();
        String esquema = uri.getScheme().toLowerCase(Locale.ROOT);
        if (ConfiguracaoConsultaCnpjService.hostLocal(host)) {
            if (!permitirUrlLocal) {
                throw urlInvalida("O host da URL não é permitido.");
            }
            if (!esquema.equals("https") && !esquema.equals("http")) {
                throw urlInvalida("A URL deve usar https.");
            }
        } else {
            if (!esquema.equals("https")) {
                throw urlInvalida("A URL deve usar https.");
            }
            String h = host.toLowerCase(Locale.ROOT);
            while (h.endsWith(".")) {
                h = h.substring(0, h.length() - 1);
            }
            if (ConfiguracaoConsultaCnpjService.hostProibido(host) || !(h.equals(DOMINIO) || h.endsWith("." + DOMINIO))) {
                throw urlInvalida("O host deve terminar em " + DOMINIO + ".");
            }
        }
        return semBarraFinal(bruta.strip());
    }

    /** Base publica deste back: https, host publico (nunca localhost/privado), sem userinfo/query. */
    String validarUrlPublica(String bruta) {
        URI uri = analisar(bruta);
        if (!uri.getScheme().equalsIgnoreCase("https")) {
            throw urlInvalida("A URL deve usar https.");
        }
        if (ConfiguracaoConsultaCnpjService.hostLocal(uri.getHost())
                || ConfiguracaoConsultaCnpjService.hostProibido(uri.getHost())) {
            throw urlInvalida("O host da URL deve ser público.");
        }
        return semBarraFinal(bruta.strip());
    }

    private static URI analisar(String bruta) {
        if (bruta == null || bruta.isBlank()) {
            throw urlInvalida("Informe a URL.");
        }
        String s = bruta.strip();
        if (s.length() > MAX_URL) {
            throw urlInvalida("A URL deve ter no máximo " + MAX_URL + " caracteres.");
        }
        if (s.endsWith("//")) {
            throw urlInvalida("A URL não pode terminar com barras duplas.");
        }
        if (s.indexOf('?') >= 0 || s.indexOf('#') >= 0) {
            throw urlInvalida("A URL não pode ter query string nem fragmento.");
        }
        URI uri;
        try {
            uri = new URI(s);
        } catch (URISyntaxException e) {
            throw urlInvalida("URL malformada.");
        }
        if (uri.getScheme() == null || uri.getHost() == null || uri.getHost().isBlank()) {
            throw urlInvalida("A URL precisa de esquema e host válidos.");
        }
        if (uri.getUserInfo() != null || uri.getRawAuthority().contains("@")) {
            throw urlInvalida("A URL não pode conter usuário ou senha.");
        }
        return uri;
    }

    private static String semBarraFinal(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static ApiException urlInvalida(String motivo) {
        return new ApiException(400, "url_invalida", "URL inválida. " + motivo);
    }
}
