package br.com.emitesimples.security;

/** 429: bloqueio por excesso de tentativas. Leva o tempo restante para o cabeçalho Retry-After. */
public class LimiteExcedidoException extends ApiException {

    private final long retryAfterSegundos;

    public LimiteExcedidoException(long retryAfterSegundos) {
        super(429, "muitas_tentativas", "Muitas tentativas de acesso. Tente novamente mais tarde.");
        this.retryAfterSegundos = retryAfterSegundos;
    }

    public long getRetryAfterSegundos() {
        return retryAfterSegundos;
    }
}
