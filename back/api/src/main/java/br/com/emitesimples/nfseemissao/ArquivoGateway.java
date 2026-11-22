package br.com.emitesimples.nfseemissao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * Decodifica xml/pdf devolvidos pelo gateway: Base64 (a doc diz "compactados e em Base64", sem dizer o algoritmo, e os
 * exemplos sao texto puro), com deteccao pelos primeiros bytes: gzip ({@code 1f 8b}) e zip ({@code PK 03 04}, primeira
 * entrada). Todo resultado e limitado a {@link #MAX_BYTES} DEPOIS de descompactar (defesa contra zip bomb): acima disso, ou
 * qualquer falha, devolve null (o campo fica sem arquivo; nada do conteudo vai para log).
 */
final class ArquivoGateway {

    static final int MAX_BYTES = 2 * 1024 * 1024;

    private ArquivoGateway() {
    }

    static byte[] decodificar(String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            return null;
        }
        byte[] bruto;
        String t = conteudo.strip();
        // limite grosseiro antes de decodificar: Base64 de 2 MB tem ~2,8 MB de texto
        if (t.length() > MAX_BYTES * 4L / 3 + 1024) {
            return null;
        }
        if (t.startsWith("<") || t.startsWith("%PDF")) {
            bruto = t.getBytes(StandardCharsets.UTF_8); // exemplos da doc: texto puro (Base64 nunca comeca assim)
        } else {
            try {
                bruto = Base64.getMimeDecoder().decode(t);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        try {
            if (bruto.length >= 2 && (bruto[0] & 0xff) == 0x1f && (bruto[1] & 0xff) == 0x8b) {
                try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(bruto))) {
                    return limitado(in);
                }
            }
            if (bruto.length >= 4 && bruto[0] == 'P' && bruto[1] == 'K' && bruto[2] == 3 && bruto[3] == 4) {
                try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bruto))) {
                    if (in.getNextEntry() == null) {
                        return null;
                    }
                    return limitado(in);
                }
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        return bruto.length > MAX_BYTES || bruto.length == 0 ? null : bruto;
    }

    private static byte[] limitado(InputStream in) throws IOException {
        byte[] r = in.readNBytes(MAX_BYTES + 1);
        return r.length > MAX_BYTES || r.length == 0 ? null : r;
    }
}
