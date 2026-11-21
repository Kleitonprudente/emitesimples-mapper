package br.com.emitesimples.security;

/**
 * Erro de negócio/segurança com status HTTP, código estável para o front e detalhes opcionais.
 * É convertida em JSON {codigo, mensagem, detalhes?} por {@link ErrosHandler}.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String codigo;
    private final transient Object detalhes;

    public ApiException(int status, String codigo, String mensagem) {
        this(status, codigo, mensagem, null);
    }

    public ApiException(int status, String codigo, String mensagem, Object detalhes) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.detalhes = detalhes;
    }

    public int getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public Object getDetalhes() {
        return detalhes;
    }
}
