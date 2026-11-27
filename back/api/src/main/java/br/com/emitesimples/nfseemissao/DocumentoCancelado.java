package br.com.emitesimples.nfseemissao;

/**
 * Evento CDI: um documento fiscal foi CANCELADO (docs/financeiro/README.md, secao 6, E3; referencia §8). Publicado pelo modulo da nota
 * no ponto unico em que a nota vira CANCELADA ({@code NfseRetorno}); o observador e idempotente e a ordem dos eventos nao importa.
 */
public record DocumentoCancelado(String tipo, String idEmpresa, String idDocumento) {
}
