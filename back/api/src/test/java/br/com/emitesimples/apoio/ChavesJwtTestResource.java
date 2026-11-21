package br.com.emitesimples.apoio;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Gera o par RSA dos testes EM MEMÓRIA (nenhum arquivo) e o entrega como propriedades inline.
 * Os testes não dependem de keys/ nem de nada fora do repositório.
 */
public class ChavesJwtTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        try {
            KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
            g.initialize(2048);
            KeyPair par = g.generateKeyPair();
            Map<String, String> p = new HashMap<>();
            p.put("smallrye.jwt.sign.key", pem("PRIVATE KEY", par.getPrivate().getEncoded()));
            p.put("mp.jwt.verify.publickey", pem("PUBLIC KEY", par.getPublic().getEncoded()));
            return p;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar o par RSA dos testes", e);
        }
    }

    private static String pem(String tipo, byte[] der) {
        String b64 = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(der);
        return "-----BEGIN " + tipo + "-----\n" + b64 + "\n-----END " + tipo + "-----";
    }

    @Override
    public void stop() {
    }
}
