package br.com.emitesimples.nfseemissao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Contratos de {@code /api/nfse/notas}. Nada de xml/pdf em nenhum deles (o PDF sai so em {@code /{id}/pdf}). */
public final class NfseEmissaoDtos {

    private NfseEmissaoDtos() {
    }

    public record PessoaResumo(String id, String nome, String documento) {
    }

    public record ServicoResumo(String id, String nome) {
    }

    /** Item da lista. */
    public record NotaItem(String id, String numero, String serie, String estado, LocalDateTime dataEmissao,
                           BigDecimal valorServicos, PessoaResumo pessoa, ServicoResumo servico, String chave,
                           String numeroNfse, Integer codigoRetorno, String mensagemRetorno, boolean temPdf,
                           LocalDateTime dataCriacao) {
    }

    /** Um erro de validacao devolvido pelo gateway (texto salvo parseado de forma tolerante). */
    public record ErroRetorno(String campo, String erro, String descricao) {
    }

    /** Detalhe: o item mais os campos de edicao e acompanhamento. {@code editavel}/{@code podeReenviar}: falha_envio e rejeitada. {@code podeCancelar}: considera a permissao do usuario da sessao. */
    public record NotaDetalhe(String id, String numero, String serie, String estado, LocalDateTime dataEmissao,
                              BigDecimal valorServicos, PessoaResumo pessoa, ServicoResumo servico, String chave,
                              String numeroNfse, Integer codigoRetorno, String mensagemRetorno, boolean temPdf,
                              LocalDateTime dataCriacao, String informacoesComplementares, String discriminacao,
                              String descricao, String codigoServico, List<ErroRetorno> errosRetorno, String linkPdf,
                              String codigoVerificacao, LocalDateTime dataHoraEvento, boolean editavel,
                              boolean podeReenviar, boolean podeAtualizar, CancelamentoInfo cancelamento,
                              boolean podeCancelar, boolean podeAprovar, boolean podeDescartar, Tributos tributos,
                              String idCondicaoPagamento) {
    }

    /**
     * Tributacao da nota (ISS e retencoes federais): docs/historias-emissao-nota.md, secao "Tributacao da nota". Na ENTRADA
     * ({@code tributos} de criar/enviar/aprovar): ausente ({@code null}) = vale o padrao do servico; presente = vale exatamente o
     * que veio, e campo em branco limpa (a tela manda sempre todos os campos). O preparo pelo assistente nao aceita
     * {@code tributos}: tributo so pela tela, por uma pessoa. No DETALHE: os valores gravados na nota.
     * Dinheiro em reais com ate 2 casas; aliquotas em pontos percentuais (5.00 = 5%); {@code cstPisCofins} texto de 2 digitos
     * da lista fechada; {@code tipoRetencao} 0 a 9. {@code valorBaseCalculoIss} e {@code valorIssRetido} sao SO calculados pelo
     * back (valor - desconto; base x aliquota): na entrada sao ignorados, no detalhe trazem o calculado.
     */
    public record Tributos(Boolean issRetido, BigDecimal aliquotaIss, BigDecimal valorBaseCalculoIss, BigDecimal valorIssRetido,
                           String cstPisCofins, Integer tipoRetencao, BigDecimal valorBaseCalculo, BigDecimal aliquotaPis,
                           BigDecimal aliquotaCofins, BigDecimal valorPis, BigDecimal valorCofins, BigDecimal valorInss,
                           BigDecimal valorIr, BigDecimal valorCsll, BigDecimal valorDescontoIncondicionado) {
    }

    /**
     * Ultimo pedido de cancelamento da nota ({@code estado}: rascunho, processando, concluido, recusado, expirado, descartado).
     * {@code podeAprovar}/{@code podeDescartar}: so rascunho e usuario com a permissao de cancelar. Sem xml/pdf.
     */
    public record CancelamentoInfo(String id, String estado, Integer codigoMotivo, String motivo, Integer codigoRetorno,
                                   String mensagemRetorno, LocalDateTime dataHoraEvento, LocalDateTime solicitadoEm,
                                   boolean podeAprovar, boolean podeDescartar) {
    }

    /** Corpo de POST /api/nfse/notas/{id}/cancelar: codigoMotivo 1, 2 ou 9; motivo de 15 a 255 caracteres (apos trim). */
    public record CancelarEntrada(Integer codigoMotivo, String motivo) {
    }

    /**
     * Corpo de POST /api/nfse/notas (pessoaId, servicoId e valorServicos obrigatorios). {@code idCondicaoPagamento}: opcional, id da
     * condicao de pagamento do financeiro (docs/financeiro/README.md, 9.1, E1); a nota so guarda, o financeiro confere ao gerar os titulos.
     */
    public record CriarEntrada(String pessoaId, String servicoId, BigDecimal valorServicos,
                               String informacoesComplementares, String discriminacao, Tributos tributos, String idCondicaoPagamento) {
    }

    /**
     * Corpo opcional de POST /api/nfse/notas/{id}/enviar: o que vier (nao nulo) altera a nota antes de reenviar.
     * {@code idCondicaoPagamento} em branco limpa a condicao.
     */
    public record EnviarEntrada(String pessoaId, String servicoId, BigDecimal valorServicos,
                                String informacoesComplementares, String discriminacao, Tributos tributos, String idCondicaoPagamento) {
    }

    public record Contagem(long count) {
    }
}
