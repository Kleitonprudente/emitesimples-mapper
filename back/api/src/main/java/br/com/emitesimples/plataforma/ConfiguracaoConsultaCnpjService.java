package br.com.emitesimples.plataforma;

import br.com.emitesimples.security.ApiException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Configuracao da consulta de CNPJ (CNPJ.ws): URL base e token (este guardado no cofre, {@link SegredoService}). A tabela
 * e um singleton (id 'unica'). O token NUNCA sai daqui por leitura publica: so {@code tokenConfigurado}.
 */
@ApplicationScoped
public class ConfiguracaoConsultaCnpjService {

    static final int MAX_URL = 200;
    static final int MAX_TOKEN = 500;
    private static final Pattern ESPACO = Pattern.compile("\\s");
    private static final Pattern SO_NUMEROS_E_PONTOS = Pattern.compile("[0-9.]+");
    private static final Pattern IPV4 = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");

    @Inject
    EntityManager em;

    @Inject
    SegredoService segredos;

    @ConfigProperty(name = "emitesimples.cnpj.permitir-url-local", defaultValue = "false")
    boolean permitirUrlLocal;

    public record Atual(String urlBase, boolean tokenConfigurado, LocalDateTime dataAtualizacao, String atualizadoPorId,
                        String atualizadoPorNome) {
    }

    public Atual obter() {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<?> l = em.createNativeQuery(
                    "select c.url_base, c.segredo_token_id, c.data_atualizacao, u.id, u.nome "
                            + "from configuracao_consulta_cnpj c left join usuario u on u.id = c.atualizado_por "
                            + "where c.id = 'unica'").getResultList();
            if (l.isEmpty()) {
                throw new IllegalStateException("Configuração da consulta de CNPJ ausente (migração V4).");
            }
            Object[] r = (Object[]) l.get(0);
            LocalDateTime data = r[2] == null ? null
                    : r[2] instanceof Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) r[2];
            return new Atual((String) r[0], r[1] != null, data, (String) r[3], (String) r[4]);
        });
    }

    /**
     * @param token null = mantem; "" = remove; outro = grava/substitui (cifrado)
     * @param usuarioId quem altera (auditoria minima)
     */
    public void atualizar(String urlBase, String token, String usuarioId) {
        String url = validarUrl(urlBase);
        if (token != null && !token.isEmpty()) {
            validarToken(token);
        }
        // O cofre abre transacoes proprias; por isso o segredo e tratado antes de gravar a configuracao.
        String idAtual = idSegredoAtual();
        String idNovo = idAtual;
        String apagar = null;
        if (token != null) {
            if (token.isEmpty()) {
                idNovo = null;
                apagar = idAtual;
            } else if (idAtual == null) {
                idNovo = segredos.guardar(token);
            } else {
                segredos.substituir(idAtual, token);
            }
        }
        String ref = idNovo;
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                "update configuracao_consulta_cnpj set url_base = ?1, segredo_token_id = ?2, data_atualizacao = ?3, "
                        + "atualizado_por = ?4 where id = 'unica'")
                .setParameter(1, url).setParameter(2, ref).setParameter(3, Timestamp.valueOf(LocalDateTime.now()))
                .setParameter(4, usuarioId).executeUpdate());
        if (apagar != null) {
            segredos.remover(apagar);
        }
    }

    /** Uso interno futuro (o service de consulta): o token em claro, ou null se nao houver. */
    public String tokenAtual() {
        String id = idSegredoAtual();
        return id == null ? null : segredos.ler(id);
    }

    private String idSegredoAtual() {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<?> l = em.createNativeQuery("select segredo_token_id from configuracao_consulta_cnpj where id = 'unica'")
                    .getResultList();
            return l.isEmpty() ? null : (String) l.get(0);
        });
    }

    // ------------------------------------------------------------------------------------------- validacoes

    static void validarToken(String token) {
        if (token.length() > MAX_TOKEN) {
            throw new ApiException(400, "token_invalido", "O token deve ter no máximo " + MAX_TOKEN + " caracteres.");
        }
        if (ESPACO.matcher(token).find()) {
            throw new ApiException(400, "token_invalido", "O token não pode conter espaços nem quebras de linha.");
        }
    }

    String validarUrl(String bruta) {
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
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw urlInvalida("A URL precisa de um host válido.");
        }
        if (uri.getUserInfo() != null || uri.getRawAuthority().contains("@")) {
            throw urlInvalida("A URL não pode conter usuário ou senha.");
        }
        boolean local = hostLocal(host);
        if (local) {
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
            if (hostProibido(host)) {
                throw urlInvalida("O host da URL não é permitido.");
            }
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /** localhost e loopback (os unicos liberados, e so quando emitesimples.cnpj.permitir-url-local=true). */
    static boolean hostLocal(String host) {
        String h = limparHost(host);
        if (h.equals("localhost") || h.endsWith(".localhost")) {
            return true;
        }
        InetAddress ip = ipLiteral(h);
        return ip != null && ip.isLoopbackAddress();
    }

    static boolean hostProibido(String host) {
        String h = limparHost(host);
        if (h.equals("localhost") || h.endsWith(".localhost") || h.endsWith(".local") || h.endsWith(".internal")
                || h.equals("local") || h.equals("internal") || !h.contains(".") && !h.contains(":")) {
            return true; // nome sem ponto (intranet) tambem fica de fora
        }
        if (h.startsWith("0x") || SO_NUMEROS_E_PONTOS.matcher(h).matches() && !IPV4.matcher(h).matches()) {
            return true; // formas obscuras de IP (decimal inteiro, hexadecimal, octetos faltando)
        }
        InetAddress ip = ipLiteral(h);
        if (ip == null) {
            return false;
        }
        return ipProibido(ip);
    }

    /**
     * Compartilhado com a consulta de CNPJ (que o aplica aos IPs RESOLVIDOS pelo DNS): loopback, any-local, link-local,
     * privado (site-local), multicast, 0/8, CGNAT 100.64/10, reservados (&gt;= 224) e ULA fc00::/7. IPv4 mapeado em IPv6
     * ja chega aqui como IPv4 (o Java converte).
     */
    static boolean ipProibido(InetAddress ip) {
        if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isSiteLocalAddress()
                || ip.isMulticastAddress()) {
            return true;
        }
        byte[] b = ip.getAddress();
        if (b.length == 4) {
            int a = b[0] & 0xff;
            int c = b[1] & 0xff;
            return a == 0 || a == 100 && c >= 64 && c <= 127 || a >= 224; // 0/8, CGNAT 100.64/10, reservados
        }
        return (b[0] & 0xfe) == 0xfc; // fc00::/7 (ULA)
    }

    private static String limparHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);
        }
        while (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        return h;
    }

    /** So interpreta IP LITERAL (nunca consulta DNS). */
    private static InetAddress ipLiteral(String h) {
        try {
            java.util.regex.Matcher m = IPV4.matcher(h);
            if (m.matches()) {
                byte[] b = new byte[4];
                for (int i = 0; i < 4; i++) {
                    int v = Integer.parseInt(m.group(i + 1));
                    if (v > 255) {
                        return null;
                    }
                    b[i] = (byte) v;
                }
                return InetAddress.getByAddress(b);
            }
            if (h.contains(":")) {
                return InetAddress.getByName(h); // IPv6 literal: sem DNS
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private static ApiException urlInvalida(String motivo) {
        return new ApiException(400, "url_invalida", "URL inválida. " + motivo);
    }
}
