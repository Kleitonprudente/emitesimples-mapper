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

    /** Detalhe: o item mais os campos de edicao e acompanhamento. */
    public record NotaDetalhe(String id, String numero, String serie, String estado, LocalDateTime dataEmissao,
                              BigDecimal valorServicos, PessoaResumo pessoa, ServicoResumo servico, String chave,
                              String numeroNfse, Integer codigoRetorno, String mensagemRetorno, boolean temPdf,
                              LocalDateTime dataCriacao, String informacoesComplementares, String discriminacao,
                              String descricao, String codigoServico, List<ErroRetorno> errosRetorno, String linkPdf,
                              String codigoVerificacao, LocalDateTime dataHoraEvento, boolean editavel,
                              boolean podeReenviar, boolean podeAtualizar) {
    }

    /** Corpo de POST /api/nfse/notas (pessoaId, servicoId e valorServicos obrigatorios). */
    public record CriarEntrada(String pessoaId, String servicoId, BigDecimal valorServicos,
                               String informacoesComplementares, String discriminacao) {
    }

    /** Corpo opcional de POST /api/nfse/notas/{id}/enviar: o que vier (nao nulo) altera a nota antes de reenviar. */
    public record EnviarEntrada(String pessoaId, String servicoId, BigDecimal valorServicos,
                                String informacoesComplementares, String discriminacao) {
    }

    public record Contagem(long count) {
    }
}
