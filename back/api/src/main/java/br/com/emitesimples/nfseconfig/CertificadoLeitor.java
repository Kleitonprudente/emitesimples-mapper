package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.security.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.security.auth.x500.X500Principal;

/**
 * Abre o .pfx (PKCS12) em memoria e extrai so o que o sistema precisa: validade e CNPJs do titular. A senha e o arquivo nunca
 * vao para excecao nem log (as causas das excecoes do JDK sao descartadas). Nada e gravado aqui.
 */
final class CertificadoLeitor {

    /** Validade (data do notAfter) e CNPJs achados no titular (Subject e SAN otherName 2.16.76.1.3.3). */
    record Lido(LocalDate validade, Set<String> cnpjs) {
    }

    private static final String MSG_INVALIDO = "O arquivo não é um certificado A1 (.pfx) válido.";
    private static final Pattern CNPJ_PURO = Pattern.compile("(?<!\\d)\\d{14}(?!\\d)");
    private static final Pattern CNPJ_FORMATADO = Pattern.compile("(?<!\\d)\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}(?!\\d)");
    /** DER: tag OID (06), tamanho 5, 2.16.76.1.3.3. */
    private static final byte[] OID_CNPJ = {0x06, 0x05, 0x60, 0x4C, 0x01, 0x03, 0x03};

    private CertificadoLeitor() {
    }

    static Lido ler(byte[] pfx, String senha, Instant agora) {
        char[] pw = senha.toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try {
                ks.load(new ByteArrayInputStream(pfx), pw);
            } catch (IOException e) {
                throw senhaIncorreta(e) ? senhaIncorreta() : invalido(MSG_INVALIDO);
            } catch (GeneralSecurityException | RuntimeException e) {
                throw invalido(MSG_INVALIDO);
            }
            X509Certificate c = certificadoFinal(ks, pw);
            Instant fim = c.getNotAfter().toInstant();
            if (fim.isBefore(agora)) {
                throw new ApiException(400, "certificado_vencido", "O certificado está vencido.");
            }
            if (c.getNotBefore().toInstant().isAfter(agora)) {
                throw invalido("O certificado ainda não está válido (início da validade no futuro).");
            }
            LocalDate validade = fim.atZone(CertificadoSituacao.BRASIL).toLocalDate();
            return new Lido(validade, cnpjsDoTitular(c));
        } catch (ApiException e) {
            throw e;
        } catch (GeneralSecurityException | RuntimeException e) {
            throw invalido(MSG_INVALIDO);
        } finally {
            Arrays.fill(pw, '\0');
        }
    }

    /** O certificado FINAL: o da entrada com chave privada (primeiro da cadeia). */
    private static X509Certificate certificadoFinal(KeyStore ks, char[] pw) throws GeneralSecurityException {
        for (String alias : Collections.list(ks.aliases())) {
            if (!ks.isKeyEntry(alias)) {
                continue;
            }
            Key chave;
            try {
                chave = ks.getKey(alias, pw);
            } catch (UnrecoverableKeyException e) {
                throw senhaIncorreta();
            }
            Certificate cert = ks.getCertificate(alias);
            if (chave != null && cert instanceof X509Certificate x) {
                return x;
            }
        }
        throw invalido("O arquivo não tem chave privada e certificado.");
    }

    /** CNPJs (14 digitos) no Subject e na SAN. Vazio se nao achou nenhum. */
    static Set<String> cnpjsDoTitular(X509Certificate c) {
        Set<String> r = new LinkedHashSet<>();
        coletar(c.getSubjectX500Principal().getName(X500Principal.RFC2253), r);
        try {
            Collection<List<?>> sans = c.getSubjectAlternativeNames();
            if (sans != null) {
                for (List<?> san : sans) {
                    if (san.size() >= 2 && Integer.valueOf(0).equals(san.get(0)) && san.get(1) instanceof byte[] der) {
                        cnpjDoOtherName(der, r);
                    }
                }
            }
        } catch (GeneralSecurityException | RuntimeException e) {
            // SAN ilegivel: vale o que o Subject trouxe
        }
        return r;
    }

    private static void coletar(String texto, Set<String> destino) {
        Matcher f = CNPJ_FORMATADO.matcher(texto);
        while (f.find()) {
            destino.add(f.group().replaceAll("\\D", ""));
        }
        Matcher p = CNPJ_PURO.matcher(texto);
        while (p.find()) {
            destino.add(p.group());
        }
    }

    /** Procura a OID 2.16.76.1.3.3 no DER do otherName e le os 14 digitos que vem logo depois (PrintableString/UTF8String). */
    static void cnpjDoOtherName(byte[] der, Set<String> destino) {
        outer:
        for (int i = 0; i + OID_CNPJ.length <= der.length; i++) {
            for (int j = 0; j < OID_CNPJ.length; j++) {
                if (der[i + j] != OID_CNPJ[j]) {
                    continue outer;
                }
            }
            int ini = i + OID_CNPJ.length;
            int fim = Math.min(der.length, ini + 40);
            Matcher m = CNPJ_PURO.matcher(new String(der, ini, fim - ini, StandardCharsets.ISO_8859_1));
            if (m.find()) {
                destino.add(m.group());
            }
            return;
        }
    }

    private static boolean senhaIncorreta(IOException e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof UnrecoverableKeyException) {
                return true;
            }
            String m = t.getMessage();
            if (m != null && (m.contains("password was incorrect") || m.contains("Given final block not properly padded"))) {
                return true;
            }
        }
        return false;
    }

    private static ApiException senhaIncorreta() {
        return new ApiException(400, "certificado_senha_incorreta", "A senha do certificado está incorreta.");
    }

    private static ApiException invalido(String mensagem) {
        return new ApiException(400, "certificado_invalido", mensagem);
    }
}
