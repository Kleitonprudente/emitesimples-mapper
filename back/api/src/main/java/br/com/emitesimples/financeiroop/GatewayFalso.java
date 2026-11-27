package br.com.emitesimples.financeiroop;

import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provedor de teste (FALSO; docs/financeiro/README.md, 9.5): responde na hora, sem chamar ninguem, e guarda as cobrancas em memoria.
 * Os testes controlam o que ele faz: {@link #proximaCriacao} (OK, RECUSAR, SEM_RESPOSTA) e {@link #pagar}/{@link #expirar}.
 * Nunca e usado com dinheiro de verdade: a credencial FALSO so serve para testes e homologacao interna.
 */
@ApplicationScoped
public class GatewayFalso implements GatewayDeCobranca {

    public enum Modo { OK, RECUSAR, SEM_RESPOSTA }

    private final Map<String, RespostaDaCobranca> cobrancas = new ConcurrentHashMap<>();
    private volatile Modo proximaCriacao = Modo.OK;
    private volatile String codigoDaRecusa = "VALOR_MINIMO";

    @Override
    public String provedor() {
        return "FALSO";
    }

    /** Proxima chamada de criar: OK, recusa com o codigo dado ou sem resposta (timeout). Volta para OK depois. */
    public void proximaCriacao(Modo modo, String codigo) {
        this.proximaCriacao = modo;
        this.codigoDaRecusa = codigo;
    }

    @Override
    public RespostaDaCobranca criar(Credencial credencial, CobrancaParaCriar pedido) {
        Modo m = proximaCriacao;
        proximaCriacao = Modo.OK;
        if (m == Modo.SEM_RESPOSTA) {
            String id = "falso-" + UUID.randomUUID();
            cobrancas.put(id, resposta("AGUARDANDO", id, pedido.tipo(), null, null, null)); // o provedor criou, mas a resposta se perdeu
            throw new GatewayIndisponivel("tempo esgotado");
        }
        if (m == Modo.RECUSAR) {
            return new RespostaDaCobranca("RECUSADA", null, null, null, null, null, null, null, null, null, null, null, codigoDaRecusa,
                    "Recusado pelo provedor de teste: cliente 123.456.789-09, contato teste@exemplo.com, veja https://falso.local/x");
        }
        String id = "falso-" + UUID.randomUUID();
        RespostaDaCobranca r = resposta("AGUARDANDO", id, pedido.tipo(), null, null, null);
        cobrancas.put(id, r);
        return r;
    }

    @Override
    public RespostaDaCobranca consultar(Credencial credencial, String idExterno, String tipo) {
        RespostaDaCobranca r = idExterno == null ? null : cobrancas.get(idExterno);
        if (r == null) {
            throw new GatewayIndisponivel("cobranca desconhecida no provedor de teste");
        }
        return r;
    }

    /** Simula o pagamento (o webhook ou a consulta seguinte veem PAGA). */
    public void pagar(String idExterno, BigDecimal valorPago, BigDecimal tarifa) {
        RespostaDaCobranca a = cobrancas.get(idExterno);
        cobrancas.put(idExterno, resposta("PAGA", idExterno, a == null ? "PIX" : meio(a), LocalDateTime.now(), valorPago, tarifa));
    }

    public void expirar(String idExterno) {
        RespostaDaCobranca a = cobrancas.get(idExterno);
        cobrancas.put(idExterno, resposta("EXPIRADA", idExterno, a == null ? "PIX" : meio(a), null, null, null));
    }

    /** O ultimo id criado pelo falso (para os testes que precisam achar a cobranca perdida no timeout). */
    public java.util.Set<String> ids() {
        return cobrancas.keySet();
    }

    private static String meio(RespostaDaCobranca r) {
        return r.meioPago() != null ? r.meioPago() : r.linhaDigitavel() != null ? "BOLETO" : r.pixCopiaECola() != null ? "PIX" : "LINK";
    }

    private static RespostaDaCobranca resposta(String situacao, String id, String tipo, LocalDateTime pagoEm, BigDecimal valorPago, BigDecimal tarifa) {
        boolean boleto = "BOLETO".equals(tipo);
        boolean pix = "PIX".equals(tipo);
        return new RespostaDaCobranca(situacao, id, boleto ? "34191.79001 01043.510047 91020.150008 1 00000000000000" : null,
                boleto ? "34191000000000000001790010435100479102015000" : null, boleto ? "https://falso.local/boleto/" + id : null,
                pix ? "00020126580014br.gov.bcb.pix-falso-" + id : null, pix ? "https://falso.local/pix/" + id : null,
                "LINK".equals(tipo) ? "https://falso.local/link/" + id : null, pagoEm, valorPago, pagoEm == null ? null : tipo, tarifa, null, null);
    }
}
