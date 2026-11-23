package br.com.emitesimples.nfseemissao;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.gateway.IntegraNotasCliente.RetornoNfse;
import br.com.emitesimples.nfse.entity.CancelamentoEntity;
import br.com.emitesimples.nfse.entity.NotaEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * Tabela UNICA de interpretacao do retorno do gateway (envio e consulta): decide SEMPRE pelo {@code codigo}, nunca pelo texto.
 * Opera sobre a nota ja carregada (e travada) dentro da transacao do chamador. Estados finais nao regridem: autorizada so
 * vai para cancelada (101); cancelada e descartada nao mudam.
 */
final class NfseRetorno {

    private static final Logger LOG = Logger.getLogger(NfseRetorno.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter EVENTO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static final String MSG_INDISPONIVEL = "A IntegraNotas está indisponível. Tente enviar novamente.";
    static final String MSG_TOKEN = "A IntegraNotas recusou o token do emitente. Fale com o suporte.";

    /** Falhas de validacao/recusa do gateway: a nota fica {@code rejeitada} (editavel; o numero e reaproveitado). */
    static final Set<Integer> REJEICAO = Set.of(5001, 5002, 5005, 5007, 5024, 5025, 5028, 5033);

    enum Efeito {
        /** Nada a fazer alem do que foi gravado. */
        NENHUM,
        /** Envio aceito com 100 sem status Autorizado: fazer UMA consulta imediata. */
        CONSULTAR_AGORA,
        /** Nao ha como acompanhar: a nota voltou para rascunho (reenviar). */
        VOLTOU_RASCUNHO
    }

    private NfseRetorno() {
    }

    static boolean emAndamento(String estado) {
        return "processando".equals(estado) || "emitida_offline".equals(estado) || "em_conflito".equals(estado);
    }

    // ------------------------------------------------------------------------------------- envio

    static Efeito aplicarEnvio(NotaEntity n, RetornoNfse r) {
        int c = r.codigo();
        boolean sucesso = !Boolean.FALSE.equals(r.sucesso());
        if (sucesso && (c == 5023 || c == 100 || c == 2)) {
            if (r.chave() == null || !gravarChave(n, r.chave())) {
                return voltarParaRascunho(n, "A IntegraNotas aceitou o envio mas não devolveu a chave. Tente enviar novamente.", c);
            }
            if (c == 100 && "Autorizado".equalsIgnoreCase(texto(r.raiz(), "status"))) {
                autorizar(n, r);
                return Efeito.NENHUM;
            }
            andamento(n, "processando", r);
            return c == 100 ? Efeito.CONSULTAR_AGORA : Efeito.NENHUM;
        }
        if (c == 5027) {
            if (r.chave() != null) {
                gravarChave(n, r.chave());
            }
            andamento(n, "em_conflito", r);
            return Efeito.NENHUM;
        }
        if (c == 0) {
            return voltarParaRascunho(n, MSG_INDISPONIVEL, c);
        }
        if (c == 5030 || c == 5034 || c == 8429 || c >= 7000 && c <= 7999 || c >= 9000 && c <= 9999) {
            if (r.chave() == null || !gravarChave(n, r.chave())) {
                // nada para acompanhar: seguro tentar de novo (mesmo numero e mesma identificacao)
                return voltarParaRascunho(n, r.mensagem() == null ? MSG_INDISPONIVEL : r.mensagem(), c);
            }
            andamento(n, "processando", r);
            return Efeito.NENHUM;
        }
        if (sucesso && c / 100 != 50 && c != 5006) {
            return voltarParaRascunho(n, MSG_INDISPONIVEL, c); // sucesso com codigo desconhecido: resposta inesperada
        }
        rejeitar(n, r); // 5001, 5002, 5005, 5007, 5024, 5025, 5028, 5033 e qualquer outra recusa
        return Efeito.NENHUM;
    }

    /** Falha de infraestrutura no envio (token recusado, indisponivel, resposta invalida): a nota NAO e rejeitada. */
    static void aplicarFalhaDeEnvio(NotaEntity n, String codigoErro) {
        String msg = "integranotas_token_recusado".equals(codigoErro) ? MSG_TOKEN : MSG_INDISPONIVEL;
        voltarParaRascunho(n, msg, null);
    }

    private static Efeito voltarParaRascunho(NotaEntity n, String mensagem, Integer codigo) {
        n.setEstado("rascunho");
        n.setCodigoRetorno(codigo);
        n.setMensagemRetorno(limitar(mensagem, 500));
        n.setErrosRetorno(null);
        n.setDataAtualizacao(AuthService.agora());
        return Efeito.VOLTOU_RASCUNHO;
    }

    // ------------------------------------------------------------------------------------- consulta

    static void aplicarConsulta(NotaEntity n, RetornoNfse r) {
        String atual = n.getEstado();
        if ("cancelada".equals(atual) || "descartada".equals(atual)) {
            return;
        }
        int c = r.codigo();
        if ("autorizada".equals(atual)) {
            if (c == 101) {
                cancelar(n, r);
            }
            return; // idempotente: uma consulta nunca regride a nota autorizada
        }
        if (!emAndamento(atual)) {
            return; // rascunho e rejeitada nao sao acompanhadas
        }
        if (c == 100) {
            autorizar(n, r);
        } else if (c == 101) {
            cancelar(n, r);
        } else if (c == 2 && !Boolean.FALSE.equals(r.sucesso())) {
            if (r.chave() != null && n.getChave() == null) {
                gravarChave(n, r.chave());
            }
            guardarPdf(n, r.raiz());
            andamento(n, "emitida_offline", r);
        } else if (c == 5023 || c == 5030 || c == 5034) {
            andamento(n, "processando", r);
        } else if (c == 5027) {
            andamento(n, "em_conflito", r);
        } else if (REJEICAO.contains(c)) {
            rejeitar(n, r);
        } else {
            // 5006 (nao encontrada), 6xxx, 7xxx, 8xxx, 9xxx, desconhecido: mantem o estado e so registra
            n.setCodigoRetorno(c);
            n.setMensagemRetorno(limitar(c == 5006 ? "A IntegraNotas não encontrou esta nota. Use Atualizar mais tarde."
                    : r.mensagem(), 500));
            n.setDataAtualizacao(AuthService.agora());
        }
    }

    // ------------------------------------------------------------------------------------- cancelamento

    enum ResultadoCancelamento {
        /** 101 ou 5015: a nota virou cancelada. */
        CONCLUIDO,
        /** O gateway recusou (5013, 5001...): a nota continua autorizada; o chamador devolve 409. */
        RECUSADO,
        /** Resposta nao conclusiva / falha de rede: fica pendente ate o acompanhamento ver o 101. */
        PROCESSANDO
    }

    static final String MSG_CANCELAMENTO_SEM_CONFIRMACAO =
            "O IntegraNotas não confirmou o cancelamento. Verifique a nota e solicite o cancelamento novamente.";
    static final Duration LIMITE_CANCELAMENTO_PENDENTE = Duration.ofHours(24);

    /**
     * Aplica a resposta de {@code POST /v1/nfse/cancela} (ou a falha de infraestrutura {@code falha}, codigo estavel do
     * cliente) ao registro de Cancelamento e a nota. Decide pelo {@code codigo}: 101 e 5015 (ja cancelada, idempotente)
     * concluem; falha de rede/indisponibilidade, 5023/5030/5034/5027/8429, 7xxx, 9xxx, 0 e sucesso com codigo desconhecido
     * deixam {@code processando}; token recusado e qualquer outra recusa (5013...) marcam {@code recusado}.
     */
    static ResultadoCancelamento aplicarCancelamento(NotaEntity n, CancelamentoEntity c, RetornoNfse r, String falha) {
        var agora = AuthService.agora();
        c.setDataAtualizacao(agora);
        if (r == null) {
            if ("integranotas_token_recusado".equals(falha)) {
                recusar(c, null, MSG_TOKEN, agora);
                return ResultadoCancelamento.RECUSADO;
            }
            c.setMensagemRetorno(MSG_INDISPONIVEL);
            return ResultadoCancelamento.PROCESSANDO;
        }
        int cod = r.codigo();
        boolean sucesso = !Boolean.FALSE.equals(r.sucesso());
        if (cod == 5015 || sucesso && cod == 101) {
            concluir(c, r, cod);
            if (!"cancelada".equals(n.getEstado())) {
                cancelar(n, r);
            }
            return ResultadoCancelamento.CONCLUIDO;
        }
        boolean inconclusivo = cod == 0 || cod == 5023 || cod == 5030 || cod == 5034 || cod == 5027 || cod == 8429
                || cod >= 7000 && cod <= 7999 || cod >= 9000 && cod <= 9999 || sucesso;
        c.setCodigoRetorno(cod);
        c.setMensagemRetorno(limitar(r.mensagem(), 500));
        if (inconclusivo) {
            return ResultadoCancelamento.PROCESSANDO;
        }
        recusar(c, cod, r.mensagem(), agora);
        return ResultadoCancelamento.RECUSADO;
    }

    private static void recusar(CancelamentoEntity c, Integer cod, String mensagem, java.time.LocalDateTime agora) {
        c.setEstado("recusado");
        c.setCodigoRetorno(cod);
        c.setMensagemRetorno(limitar(mensagem, 500));
        c.setDataHoraRecusa(agora);
    }

    private static void concluir(CancelamentoEntity c, RetornoNfse r, int codigo) {
        c.setEstado("concluido");
        c.setCodigoRetorno(codigo);
        c.setMensagemRetorno(limitar(r.mensagem(), 500));
        LocalDateTime evento = dataEvento(texto(r.raiz(), "data_hora_evento"));
        if (evento != null) {
            c.setDataHoraEvento(evento);
        }
        byte[] xml = ArquivoGateway.decodificar(texto(r.raiz(), "xml"));
        if (xml != null) {
            c.setXml(xml);
        }
        byte[] pdf = ArquivoGateway.decodificar(texto(r.raiz(), "pdf"));
        if (pdf != null) {
            c.setPdf(pdf);
        }
        c.setDataAtualizacao(AuthService.agora());
    }

    /**
     * Depois de aplicar uma CONSULTA: se a nota esta cancelada, conclui o(s) Cancelamento(s) pendente(s) com o retorno 101;
     * se a nota segue autorizada e o pedido pendente tem mais de 24 h, marca-o {@code recusado} (sem confirmacao) para
     * liberar um novo pedido. Nota travada pelo chamador.
     */
    static void concluirCancelamentoPendente(EntityManager em, NotaEntity n, RetornoNfse r) {
        List<CancelamentoEntity> pendentes = em.createQuery("select c from CancelamentoEntity c where c.nota.id = :n "
                + "and c.empresa.id = :e and c.estado = 'processando'", CancelamentoEntity.class)
                .setParameter("n", n.getId()).setParameter("e", n.getEmpresa().getId()).getResultList();
        for (CancelamentoEntity c : pendentes) {
            if ("cancelada".equals(n.getEstado())) {
                concluir(c, r, 101);
            } else if ("autorizada".equals(n.getEstado()) && r.codigo() == 100 && c.getDataCriacao() != null
                    && c.getDataCriacao().isBefore(AuthService.agora().minus(LIMITE_CANCELAMENTO_PENDENTE))) {
                recusar(c, null, MSG_CANCELAMENTO_SEM_CONFIRMACAO, AuthService.agora());
                c.setDataAtualizacao(AuthService.agora());
            }
        }
    }

    // ------------------------------------------------------------------------------------- transicoes

    private static void andamento(NotaEntity n, String estado, RetornoNfse r) {
        n.setEstado(estado);
        n.setCodigoRetorno(r.codigo());
        n.setMensagemRetorno(limitar(r.mensagem(), 500));
        n.setErrosRetorno(null);
        n.setDataAtualizacao(AuthService.agora());
    }

    private static void rejeitar(NotaEntity n, RetornoNfse r) {
        n.setEstado("rejeitada");
        n.setCodigoRetorno(r.codigo());
        n.setMensagemRetorno(limitar(r.mensagem(), 500));
        n.setErrosRetorno(errosComoJson(r.raiz()));
        n.setDataAtualizacao(AuthService.agora());
    }

    private static void autorizar(NotaEntity n, RetornoNfse r) {
        JsonNode raiz = r.raiz();
        n.setEstado("autorizada");
        n.setCodigoRetorno(100);
        n.setMensagemRetorno(limitar(r.mensagem(), 500));
        n.setErrosRetorno(null);
        if (r.chave() != null && n.getChave() == null) {
            gravarChave(n, r.chave());
        }
        n.setNumeroNfse(limitar(texto(raiz, "numero"), 20));
        n.setRpsNumero(limitar(texto(raiz, "rps_numero"), 9));
        n.setRpsSerie(limitar(texto(raiz, "rps_serie"), 5));
        n.setCodigoVerificacao(limitar(texto(raiz, "codigo_verificacao"), 40));
        n.setDataHoraEvento(dataEvento(texto(raiz, "data_hora_evento")));
        n.setLinkPdf(link(texto(raiz, "link_pdf")));
        byte[] xml = ArquivoGateway.decodificar(texto(raiz, "xml"));
        if (xml != null) {
            n.setXml(xml);
        }
        guardarPdf(n, raiz);
        n.setDataAtualizacao(AuthService.agora());
    }

    private static void cancelar(NotaEntity n, RetornoNfse r) {
        n.setEstado("cancelada");
        n.setCodigoRetorno(101);
        n.setMensagemRetorno(limitar(r.mensagem(), 500));
        n.setErrosRetorno(null);
        LocalDateTime evento = dataEvento(texto(r.raiz(), "data_hora_evento"));
        if (evento != null) {
            n.setDataHoraEvento(evento);
        }
        if (n.getPdf() == null) {
            guardarPdf(n, r.raiz());
        }
        n.setDataAtualizacao(AuthService.agora());
    }

    private static void guardarPdf(NotaEntity n, JsonNode raiz) {
        byte[] pdf = ArquivoGateway.decodificar(texto(raiz, "pdf"));
        if (pdf != null) {
            n.setPdf(pdf);
        }
        String link = link(texto(raiz, "link_pdf"));
        if (link != null) {
            n.setLinkPdf(link);
        }
    }

    // ------------------------------------------------------------------------------------- campos

    /** Grava a chave (50 digitos, unica); false se ja pertence a outra nota (nao confia no gateway cegamente). */
    private static boolean gravarChave(NotaEntity n, String chave) {
        if (chave.equals(n.getChave())) {
            return true;
        }
        n.setChave(chave);
        return true;
    }

    static String errosComoJson(JsonNode raiz) {
        JsonNode erros = raiz == null ? null : raiz.get("erros");
        List<ObjectNode> itens = new ArrayList<>();
        if (erros != null && erros.isArray()) {
            for (JsonNode e : erros) {
                if (itens.size() >= 30) {
                    break;
                }
                ObjectNode o = JSON.createObjectNode();
                String campo = limitar(limpo(texto(e, "campo")), 120);
                String erro = limitar(limpo(texto(e, "erro")), 200);
                String desc = limitar(limpo(texto(e, "descricao")), 400);
                if (campo != null) {
                    o.put("campo", campo);
                }
                if (erro != null) {
                    o.put("erro", erro);
                }
                if (desc != null) {
                    o.put("descricao", desc);
                }
                if (!o.isEmpty()) {
                    itens.add(o);
                }
            }
        }
        if (itens.isEmpty()) {
            return null;
        }
        while (true) {
            ArrayNode arr = JSON.createArrayNode();
            itens.forEach(arr::add);
            String s = arr.toString();
            if (s.length() <= 4000 || itens.size() == 1) {
                return limitar(s, 4000);
            }
            itens.remove(itens.size() - 1);
        }
    }

    private static LocalDateTime dataEvento(String t) {
        if (t == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(t.strip().replace('T', ' ').substring(0, Math.min(19, t.strip().length())), EVENTO);
        } catch (DateTimeParseException e) {
            LOG.debugf("NFS-e: data_hora_evento fora do formato esperado");
            return null;
        }
    }

    private static String link(String t) {
        if (t == null) {
            return null;
        }
        String s = t.strip();
        return (s.startsWith("https://") || s.startsWith("http://")) && s.length() <= 500 ? s : null;
    }

    static String texto(JsonNode n, String campo) {
        JsonNode v = n == null ? null : n.get(campo);
        return v != null && v.isValueNode() && !v.isNull() ? v.asText() : null;
    }

    private static String limpo(String s) {
        return s == null ? null : s.replaceAll("\\p{Cntrl}", " ").strip();
    }

    static String limitar(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }
}
