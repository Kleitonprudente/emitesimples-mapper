package br.com.emitesimples.plataforma;

import br.com.emitesimples.security.ApiException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Leitura do arquivo de municipios (docs/carga-municipios.md): detecta UTF-8 valido ou Windows-1252, detecta o
 * separador (; , ou tabulacao) e acha as colunas PELO NOME do cabecalho (sem acento, sem diferenca de caixa).
 * As colunas TOM sao ignoradas. Aceita campos entre aspas; nao aceita quebra de linha dentro de campo.
 */
final class ArquivoMunicipios {

    static final String COL_CODIGO = "CODIGO DO MUNICIPIO - IBGE";
    static final String COL_NOME = "MUNICIPIO - IBGE";
    static final String COL_UF = "UF";
    private static final char[] SEPARADORES = {';', ',', '\t'};

    /** Linha de dados crua; {@code numero} e a linha fisica no arquivo (o cabecalho e a linha 1). */
    record Linha(int numero, String codigo, String nome, String uf) {
    }

    record Lido(String codificacao, String separador, List<Linha> linhas) {
    }

    private ArquivoMunicipios() {
    }

    static Lido ler(byte[] bytes) {
        String codificacao;
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            codificacao = "UTF-8";
        } catch (CharacterCodingException e) {
            texto = new String(bytes, Charset.forName("windows-1252"));
            codificacao = "Windows-1252";
        }
        if (!texto.isEmpty() && texto.charAt(0) == '﻿') {
            texto = texto.substring(1);
        }
        String[] fisicas = texto.split("\r\n|\n|\r", -1);
        int i = 0;
        while (i < fisicas.length && fisicas[i].isBlank()) {
            i++;
        }
        if (i >= fisicas.length) {
            throw invalido("O arquivo está vazio.");
        }
        int linhaCabecalho = i;
        String cabecalho = fisicas[i];

        char sep = 0;
        int iCodigo = -1;
        int iNome = -1;
        int iUf = -1;
        for (char candidato : SEPARADORES) {
            List<String> cols = dividir(cabecalho, candidato);
            int c = indice(cols, COL_CODIGO);
            int n = indice(cols, COL_NOME);
            int u = indice(cols, COL_UF);
            if (c >= 0 && n >= 0 && u >= 0) {
                sep = candidato;
                iCodigo = c;
                iNome = n;
                iUf = u;
                break;
            }
        }
        if (sep == 0) {
            throw invalido("Cabeçalho não reconhecido. São obrigatórias as colunas \"CÓDIGO DO MUNICÍPIO - IBGE\", "
                    + "\"MUNICÍPIO - IBGE\" e \"UF\" (separador ponto e vírgula, vírgula ou tabulação).");
        }

        List<Linha> linhas = new ArrayList<>();
        for (int k = linhaCabecalho + 1; k < fisicas.length; k++) {
            if (fisicas[k].isBlank()) {
                continue;
            }
            List<String> cols = dividir(fisicas[k], sep);
            linhas.add(new Linha(k + 1, celula(cols, iCodigo), celula(cols, iNome), celula(cols, iUf)));
        }
        String nomeSep = sep == ';' ? ";" : sep == ',' ? "," : "tab"; // coluna SEPARADOR_DETECTADO: varchar(5)
        return new Lido(codificacao, nomeSep, linhas);
    }

    private static String celula(List<String> cols, int i) {
        return i < cols.size() ? cols.get(i).trim() : "";
    }

    private static int indice(List<String> cols, String nomeNormalizado) {
        for (int i = 0; i < cols.size(); i++) {
            if (normalizar(cols.get(i)).equals(nomeNormalizado)) {
                return i;
            }
        }
        return -1;
    }

    static String normalizar(String s) {
        String d = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return d.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** Divide uma linha pelo separador respeitando aspas ("" dentro de aspas e uma aspa literal). */
    static List<String> dividir(String linha, char sep) {
        List<String> out = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (aspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        aspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                aspas = true;
            } else if (c == sep) {
                out.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        out.add(atual.toString());
        return out;
    }

    private static ApiException invalido(String msg) {
        return new ApiException(400, "arquivo_invalido", msg);
    }
}
