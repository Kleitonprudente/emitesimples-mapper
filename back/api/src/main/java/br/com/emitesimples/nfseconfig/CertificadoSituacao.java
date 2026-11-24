package br.com.emitesimples.nfseconfig;

import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Leitura unica da situacao do certificado da empresa (usada por {@code GET /api/configuracao/empresa}, pelo POST do
 * certificado e por {@code GET /api/configuracao/nfse}, para os tres concordarem). Valores gravados:
 * {@code NAO_ENVIADO | VALIDO | VENCIDO | INVALIDO | NAO_PERTENCE}. Sem certificado e sem situacao gravada: {@code NAO_ENVIADO}
 * (e {@code pendenteDeEnvio=false}, pois nao ha o que enviar).
 */
public final class CertificadoSituacao {

    public static final String NAO_ENVIADO = "NAO_ENVIADO";
    public static final String VALIDO = "VALIDO";
    public static final String VENCIDO = "VENCIDO";
    public static final String INVALIDO = "INVALIDO";
    public static final String NAO_PERTENCE = "NAO_PERTENCE";
    static final ZoneId BRASIL = ZoneId.of("America/Sao_Paulo");

    private CertificadoSituacao() {
    }

    public static LocalDate hoje() {
        return LocalDate.now(BRASIL);
    }

    public static boolean guardado(EmpresaEntity e) {
        return e.getReferenciaCertificado() != null;
    }

    /** Situacao gravada; ou {@code VENCIDO} se a validade ja passou e a situacao ainda era VALIDO/NAO_ENVIADO. */
    public static String situacao(EmpresaEntity e) {
        String s = e.getSituacaoCertificado() == null ? NAO_ENVIADO : e.getSituacaoCertificado();
        LocalDate v = e.getValidadeCertificado();
        if (v != null && v.isBefore(hoje()) && (VALIDO.equals(s) || NAO_ENVIADO.equals(s))) {
            return VENCIDO;
        }
        return s;
    }

    /** Guardado e a situacao GRAVADA (nao a vencida por data) nao e valido: ha o que enviar (de novo) ao gateway. */
    public static boolean pendenteDeEnvio(EmpresaEntity e) {
        return guardado(e) && !VALIDO.equals(e.getSituacaoCertificado());
    }

    /** Dias ate o vencimento (negativo se ja venceu); null sem validade. */
    public static Long venceEmDias(EmpresaEntity e) {
        return e.getValidadeCertificado() == null ? null : ChronoUnit.DAYS.between(hoje(), e.getValidadeCertificado());
    }
}
