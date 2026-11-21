package br.com.emitesimples.auth;

import br.com.emitesimples.security.ApiException;
import io.quarkus.elytron.security.common.BcryptUtil;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Política de senha. O bcrypt TRUNCA em 72 bytes, então a senha precisa ter de 8 a 72 BYTES em UTF-8 (recusamos
 * acima disso em vez de truncar em silêncio). Nunca registra a senha em log.
 */
public final class SenhaPolitica {

    public static final int MIN_BYTES = 8;
    public static final int MAX_BYTES = 72;

    private SenhaPolitica() {
    }

    public static int bytes(String senha) {
        return senha.getBytes(StandardCharsets.UTF_8).length;
    }

    /** Lança 400 se a senha nova não atende. */
    public static void validarNova(String senha) {
        if (senha == null || senha.isBlank()) {
            throw invalida("A nova senha é obrigatória.");
        }
        int n = bytes(senha);
        if (n < MIN_BYTES) {
            throw invalida("A senha deve ter no mínimo " + MIN_BYTES + " caracteres.");
        }
        if (n > MAX_BYTES) {
            throw invalida("A senha deve ter no máximo " + MAX_BYTES + " bytes (em UTF-8).");
        }
    }

    private static ApiException invalida(String msg) {
        return new ApiException(400, "senha_invalida", msg,
                Map.of("campos", List.of(Map.of("campo", "novaSenha", "mensagem", msg))));
    }

    public static String hash(String senha) {
        return BcryptUtil.bcryptHash(senha);
    }

    /** Compara em tempo constante do bcrypt; hash malformado conta como diferente. */
    public static boolean confere(String senha, String hash) {
        try {
            return BcryptUtil.matches(senha, hash);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
