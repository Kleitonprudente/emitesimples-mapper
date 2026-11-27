package br.com.emitesimples.financeiroop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Porta do provedor de cobranca (docs/financeiro/README.md, 9.5, Fase 7; referencia §9.2). So o adaptador do provedor fala com a API
 * dele; o resto do sistema so conhece esta interface. Nos testes, o {@link GatewayFalso}.
 * <p>
 * Contrato: {@code criar} e {@code consultar} rodam FORA da transacao do banco. Timeout ou falha de rede lanca
 * {@link GatewayIndisponivel} (a cobranca vira INCERTA e nunca e reenviada sem consulta). Recusa do provedor volta como resposta
 * {@code RECUSADA} com o CODIGO do provedor (a frase em portugues e nossa, pelo codigo).
 */
public interface GatewayDeCobranca {

    /** Nome do provedor (o mesmo de CredencialCobranca.provedor). */
    String provedor();

    RespostaDaCobranca criar(Credencial credencial, CobrancaParaCriar pedido);

    RespostaDaCobranca consultar(Credencial credencial, String idExterno, String tipo);

    /** O que o adaptador recebe da credencial: o token ja lido do cofre (nunca vai a log). */
    record Credencial(String provedor, String endpoint, String sellerId, String token) {
        @Override
        public String toString() {
            return "Credencial[" + provedor + ", " + sellerId + "]";
        }
    }

    record Pagador(String nome, String documento, String email) {
    }

    record CobrancaParaCriar(String idCobranca, String tipo, BigDecimal valor, LocalDate vencimento, BigDecimal multaPercentual,
            BigDecimal jurosMensalPercentual, String descricao, Pagador pagador) {
    }

    /**
     * {@code situacao}: AGUARDANDO, PAGA, CANCELADA, EXPIRADA ou RECUSADA. Na RECUSADA, {@code codigoRecusa} e o codigo do provedor.
     * {@code tarifa} nula = o provedor nao informou.
     */
    record RespostaDaCobranca(String situacao, String idExterno, String linhaDigitavel, String codigoDeBarras, String urlDoBoleto,
            String pixCopiaECola, String urlDoPix, String urlDoLink, LocalDateTime pagoEm, BigDecimal valorPago, String meioPago,
            BigDecimal tarifa, String codigoRecusa, String textoDoProvedor) {
    }

    /** Sem resposta do provedor (timeout, rede): nunca repetir sem consultar. */
    class GatewayIndisponivel extends RuntimeException {
        public GatewayIndisponivel(String mensagem) {
            super(mensagem);
        }
    }
}
