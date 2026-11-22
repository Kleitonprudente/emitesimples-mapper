package br.com.emitesimples.gateway;

import br.com.emitesimples.plataforma.SegredoService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Token do caminho do webhook: HMAC-SHA256 do {@code empresaId} em base64url (sem padding), com uma sub-chave DERIVADA da
 * chave-mestra do cofre (HMAC da chave-mestra sobre um rotulo fixo), para nao reutilizar a chave do AES. Estavel por
 * empresa (a URL e registrada no gateway uma vez). Quem conhece a URL consegue chamar o webhook, entao ela e tratada como
 * segredo (nunca em log). Rotacionar a chave-mestra invalida todas as URLs ja registradas.
 */
@ApplicationScoped
public class WebhookToken {

    private static final String ROTULO = "emitesimples-webhook-integranotas-v1";

    @Inject
    SegredoService segredos;

    public String gerar(String empresaId) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(subchave(), empresaId));
    }

    /** Comparacao em tempo constante; null/vazio nunca confere. */
    public boolean confere(String empresaId, String token) {
        if (empresaId == null || token == null || token.isEmpty() || token.length() > 100) {
            return false;
        }
        byte[] esperado = gerar(empresaId).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(esperado, token.getBytes(StandardCharsets.UTF_8));
    }

    /** {base}/api/webhooks/integranotas/{empresaId}/{token}; {@code base} sem barra final. */
    public String url(String base, String empresaId) {
        return base + "/api/webhooks/integranotas/" + empresaId + "/" + gerar(empresaId);
    }

    private byte[] subchave() {
        return hmac(segredos.chave().getEncoded(), ROTULO);
    }

    private static byte[] hmac(byte[] chave, String dado) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            return mac.doFinal(dado.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC indisponivel.");
        }
    }

    @Override
    public String toString() {
        return "WebhookToken";
    }
}
