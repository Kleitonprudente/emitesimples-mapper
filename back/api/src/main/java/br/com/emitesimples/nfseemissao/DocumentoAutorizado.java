package br.com.emitesimples.nfseemissao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Evento CDI: um documento fiscal foi AUTORIZADO (docs/financeiro/README.md, secao 6, E3; referencia §8). Publicado pelo modulo da nota
 * no ponto unico em que a nota vira AUTORIZADA ({@code NfseRetorno}); quem reage (o financeiro) observa depois do commit. O modulo da
 * nota NAO conhece quem observa. Pode chegar repetido (consulta ou webhook repetidos): o observador e idempotente.
 *
 * @param tipo          tipo do documento ("NFSE")
 * @param valorLiquido  o que o cliente paga: servicos - desconto incondicionado - ISS retido (se retido) - federais retidos (E2)
 * @param idUsuario     quem emitiu (aprovou) a nota, ou null
 * @param idCondicaoPagamento condicao de pagamento informada na nota, ou null (docs/financeiro/README.md, 9.1, E1)
 */
public record DocumentoAutorizado(String tipo, String idEmpresa, String idDocumento, String numero, String idPessoa, BigDecimal valorLiquido,
        LocalDate data, String idUsuario, String idCondicaoPagamento) {
}
