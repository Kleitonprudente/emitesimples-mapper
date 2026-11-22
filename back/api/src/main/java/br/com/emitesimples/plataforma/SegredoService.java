package br.com.emitesimples.plataforma;

import br.com.emitesimples.security.ApiException;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Cofre PROVISORIO de segredos: cifra com AES-256-GCM (IV aleatorio de 12 bytes por gravacao) e guarda na tabela
 * {@code segredo}. A chave-mestra vem de {@code emitesimples.segredos.chave} (32 bytes em base64). O valor em claro nunca
 * e registrado em log, em excecao nem em toString. O id do segredo entra como dado associado (AAD): um valor cifrado nao
 * pode ser trocado de linha. Cada operacao abre a propria transacao.
 */
@ApplicationScoped
public class SegredoService {

    static final int IV_BYTES = 12;
    static final int TAG_BITS = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    @Inject
    EntityManager em;

    @ConfigProperty(name = "emitesimples.segredos.chave")
    Optional<String> chaveBase64;

    public String guardar(String valor) {
        exigirValor(valor);
        SecretKeySpec chave = chave();
        String id = UUID.randomUUID().toString();
        byte[] iv = novoIv();
        byte[] cifrado = cifrar(chave, iv, id, valor);
        Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery(
                "insert into segredo (id, valor_cifrado, iv, data_criacao, data_atualizacao) values (?1, ?2, ?3, ?4, ?4)")
                .setParameter(1, id).setParameter(2, cifrado).setParameter(3, iv).setParameter(4, agora).executeUpdate());
        return id;
    }

    public void substituir(String idSegredo, String valor) {
        exigirValor(valor);
        SecretKeySpec chave = chave();
        byte[] iv = novoIv();
        byte[] cifrado = cifrar(chave, iv, idSegredo, valor);
        Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
        int n = QuarkusTransaction.requiringNew().call(() -> em.createNativeQuery(
                "update segredo set valor_cifrado = ?2, iv = ?3, data_atualizacao = ?4 where id = ?1")
                .setParameter(1, idSegredo).setParameter(2, cifrado).setParameter(3, iv).setParameter(4, agora)
                .executeUpdate());
        if (n == 0) {
            throw new IllegalStateException("Segredo inexistente: " + idSegredo);
        }
    }

    /** Uso interno (ex.: o service de consulta de CNPJ). Nunca devolver o resultado por API. */
    public String ler(String id) {
        SecretKeySpec chave = chave();
        Object[] linha = QuarkusTransaction.requiringNew().call(() -> {
            var l = em.createNativeQuery("select valor_cifrado, iv from segredo where id = ?1").setParameter(1, id)
                    .getResultList();
            return l.isEmpty() ? null : (Object[]) l.get(0);
        });
        if (linha == null) {
            throw new IllegalStateException("Segredo inexistente: " + id);
        }
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, (byte[]) linha[1]));
            c.updateAAD(id.getBytes(StandardCharsets.UTF_8));
            return new String(c.doFinal((byte[]) linha[0]), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // sem a causa: nada do conteudo vai para o log
            throw new IllegalStateException("Nao foi possivel decifrar o segredo " + id
                    + " (chave-mestra diferente da usada para cifrar, ou dado corrompido).");
        }
    }

    public void remover(String id) {
        QuarkusTransaction.requiringNew().run(
                () -> em.createNativeQuery("delete from segredo where id = ?1").setParameter(1, id).executeUpdate());
    }

    // ------------------------------------------------------------------------------------------------ interno

    private static void exigirValor(String valor) {
        if (valor == null || valor.isEmpty()) {
            throw new IllegalArgumentException("O valor do segredo e obrigatorio.");
        }
    }

    private static byte[] novoIv() {
        byte[] iv = new byte[IV_BYTES];
        ALEATORIO.nextBytes(iv);
        return iv;
    }

    private static byte[] cifrar(SecretKeySpec chave, byte[] iv, String id, String valor) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            c.updateAAD(id.getBytes(StandardCharsets.UTF_8));
            return c.doFinal(valor.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao cifrar o segredo.");
        }
    }

    SecretKeySpec chave() {
        String b64 = chaveBase64.map(String::trim).orElse("");
        if (b64.isEmpty()) {
            throw erroChave("a chave-mestra dos segredos não está configurada");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(b64);
        } catch (IllegalArgumentException e) {
            throw erroChave("a chave-mestra não está em base64 válido");
        }
        if (bytes.length != 32) {
            int n = bytes.length;
            Arrays.fill(bytes, (byte) 0);
            throw erroChave("a chave-mestra deve ter 32 bytes (AES-256), mas tem " + n);
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private static ApiException erroChave(String motivo) {
        return new ApiException(500, "segredos_chave_invalida", "Cofre de segredos indisponível: " + motivo
                + ". Defina EMITESIMPLES_SEGREDOS_CHAVE (gere com: openssl rand -base64 32).");
    }

    @Override
    public String toString() {
        return "SegredoService";
    }
}
