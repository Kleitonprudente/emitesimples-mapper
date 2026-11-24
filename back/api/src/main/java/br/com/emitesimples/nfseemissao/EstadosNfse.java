package br.com.emitesimples.nfseemissao;

/**
 * Valores tecnicos dos estados de Nota (CicloNota) e de Cancelamento (CicloCancelamento) conforme o mapa (secao 9.7:
 * MAIUSCULAS). Escrito a mao; as transicoes ficam no servico/retorno. Valores iguais nos dois ciclos compartilham a constante.
 */
public final class EstadosNfse {

    // Nota e Cancelamento
    public static final String RASCUNHO = "RASCUNHO";
    public static final String PROCESSANDO = "PROCESSANDO";
    // Nota
    public static final String EMITIDA_OFFLINE = "EMITIDA_OFFLINE";
    public static final String AUTORIZADA = "AUTORIZADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String REJEITADA = "REJEITADA";
    public static final String FALHA_ENVIO = "FALHA_ENVIO";
    public static final String EM_CONFLITO = "EM_CONFLITO";
    public static final String DESCARTADA = "DESCARTADA";
    // Cancelamento
    public static final String CONCLUIDO = "CONCLUIDO";
    public static final String RECUSADO = "RECUSADO";
    public static final String EXPIRADO = "EXPIRADO";
    public static final String DESCARTADO = "DESCARTADO";

    private EstadosNfse() {
    }
}
