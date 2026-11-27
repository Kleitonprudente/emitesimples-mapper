package br.com.emitesimples.financeiroop;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.financeiro.entity.CobrancaEntity;
import br.com.emitesimples.financeiro.entity.CobrancaEnviadaEntity;
import br.com.emitesimples.financeiro.entity.ConfiguracaoFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.LembreteCobrancaEntity;
import br.com.emitesimples.financeiro.entity.TituloEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

/**
 * Cobranca por e-mail e lembretes (docs/financeiro/README.md, 9.3, Fase 6; referencia §9.1).
 * <ul>
 * <li>Enviar cobranca (manual, critico, so ADMINISTRADOR): e-mail ao cliente com valor, vencimento e "como pagar" (a cobranca viva no
 * gateway, se houver; senao os dados da conta padrao da forma prevista). Nunca gera boleto nem Pix: so reaproveita.</li>
 * <li>Lembretes (job diario das 9h, so empresas com {@code lembretesAtivos}): {@code lembreteDiasAntes} antes do vencimento e em cada dia de
 * {@code lembreteDiasDepois} depois; cada aviso sai uma vez (LEMBRETECOBRANCA).</li>
 * <li>No maximo uma cobranca por cliente por dia (COBRANCAENVIADA): a reserva do dia e gravada ANTES do e-mail e apagada se ele falhar.</li>
 * <li>Nao envia para titulo a pagar, sem aprovacao, fora de ABERTO/PARCIAL, para revisao, nem para pessoa sem e-mail ou com
 * "nao enviar cobranca". O e-mail e texto puro (sem HTML: nada do cadastro vira marcacao).</li>
 * </ul>
 */
@ApplicationScoped
public class CobrancaPorEmail {

    private static final Logger LOG = Logger.getLogger(CobrancaPorEmail.class);
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Enviada(String id, String idTitulo, String origem, LocalDate dia, String email, LocalDateTime enviadoEm) {
    }

    /** O que vai no e-mail, montado dentro da transacao e enviado fora dela. */
    record Mensagem(String para, String assunto, String texto) {
    }

    @Inject
    EntityManager em;

    @Inject
    Mailer mailer;

    @Inject
    EscopoTenant escopo;

    @Inject
    JsonWebToken jwt;

    @Inject
    ContextoTenantBanco contexto;

    // ------------------------------------------------------------------------------------------------------------ manual

    public Enviada enviar(String idTitulo) {
        String empresa = escopo.exigir("empresa_id");
        String usuario = jwt.getSubject();
        LocalDate hoje = TitulosService.hoje();
        Object[] r = QuarkusTransaction.requiringNew().call(() -> reservar(empresa, idTitulo, "MANUAL", hoje, usuario));
        CobrancaEnviadaEntity reserva = (CobrancaEnviadaEntity) r[0];
        Mensagem m = (Mensagem) r[1];
        if (!mandar(m)) {
            QuarkusTransaction.requiringNew().run(() -> liberar(reserva.getId()));
            throw new ApiException(502, "email_nao_enviado", "Não foi possível enviar o e-mail agora. Tente de novo em alguns minutos.");
        }
        return dto(reserva);
    }

    public List<Enviada> enviadas(String idTitulo) {
        String empresa = escopo.exigir("empresa_id");
        return QuarkusTransaction.requiringNew().call(() -> {
            titulo(empresa, idTitulo);
            return em.createQuery("select c from CobrancaEnviadaEntity c where c.titulo.id = :t and c.empresa.id = :e order by c.enviadoEm desc",
                    CobrancaEnviadaEntity.class).setParameter("t", idTitulo).setParameter("e", empresa).getResultList().stream()
                    .map(CobrancaPorEmail::dto).toList();
        });
    }

    // ------------------------------------------------------------------------------------------------------------ lembretes

    /** Ciclo diario: para cada empresa com lembretes ligados, manda os avisos do dia. Devolve quantos e-mails sairam. */
    public int rodarLembretes(LocalDate hoje) {
        List<String> empresas = contexto.comoSistema(() -> QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select c.empresa.id from ConfiguracaoFinanceiraEntity c where c.lembretesAtivos = true", String.class).getResultList()));
        int enviados = 0;
        for (String empresa : empresas) {
            try {
                enviados += contexto.comoEmpresa(empresa, () -> lembretesDaEmpresa(empresa, hoje));
            } catch (RuntimeException e) {
                LOG.warnf("Lembretes da empresa %s falharam (%s).", empresa, e.getClass().getSimpleName());
            }
        }
        return enviados;
    }

    private int lembretesDaEmpresa(String empresa, LocalDate hoje) {
        ConfiguracaoFinanceiraEntity cfg = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                "select c from ConfiguracaoFinanceiraEntity c where c.empresa.id = :e", ConfiguracaoFinanceiraEntity.class)
                .setParameter("e", empresa).getResultStream().findFirst().orElse(null));
        if (cfg == null || !Boolean.TRUE.equals(cfg.getLembretesAtivos())) {
            return 0;
        }
        List<Object[]> avisos = new ArrayList<>(); // [tipo, dias, vencimento]
        if (cfg.getLembreteDiasAntes() != null && cfg.getLembreteDiasAntes() > 0) {
            avisos.add(new Object[] {"ANTES", cfg.getLembreteDiasAntes(), hoje.plusDays(cfg.getLembreteDiasAntes())});
        }
        for (int d : diasDepois(cfg.getLembreteDiasDepois())) {
            avisos.add(new Object[] {"DEPOIS", d, hoje.minusDays(d)});
        }
        int enviados = 0;
        for (Object[] a : avisos) {
            String tipo = (String) a[0];
            int dias = (Integer) a[1];
            List<String> ids = QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                    "select t.id from TituloEntity t where t.empresa.id = :e and t.tipo = 'RECEBER' and t.situacao in ('ABERTO', 'PARCIAL') "
                            + "and t.aprovadoEm is not null and t.paraRevisao = false and t.vencimento = :v and not exists (select l from "
                            + "LembreteCobrancaEntity l where l.titulo = t and l.tipo = :tipo and l.dias = :dias) order by t.vencimento, t.id",
                    String.class).setParameter("e", empresa).setParameter("v", (LocalDate) a[2]).setParameter("tipo", tipo)
                    .setParameter("dias", dias).getResultList());
            for (String id : ids) {
                if (lembrar(empresa, id, tipo, dias, hoje)) {
                    enviados++;
                }
            }
        }
        return enviados;
    }

    private boolean lembrar(String empresa, String idTitulo, String tipo, int dias, LocalDate hoje) {
        Object[] r;
        try {
            r = QuarkusTransaction.requiringNew().call(() -> reservar(empresa, idTitulo, "LEMBRETE", hoje, null));
        } catch (ApiException e) {
            LOG.debugf("Lembrete do titulo %s pulado: %s", idTitulo, e.getCodigo());
            return false; // sem e-mail, nao quer cobranca, ja cobrado hoje...
        }
        CobrancaEnviadaEntity reserva = (CobrancaEnviadaEntity) r[0];
        if (!mandar((Mensagem) r[1])) {
            QuarkusTransaction.requiringNew().run(() -> liberar(reserva.getId()));
            return false;
        }
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                LembreteCobrancaEntity l = new LembreteCobrancaEntity();
                l.setEmpresa(em.getReference(EmpresaEntity.class, empresa));
                l.setTitulo(em.getReference(TituloEntity.class, idTitulo));
                l.setTipo(tipo);
                l.setDias(dias);
                l.setEnviadoEm(AuthService.agora());
                l.setDataCriacao(AuthService.agora());
                l.setDataAtualizacao(AuthService.agora());
                em.persist(l);
                em.flush();
            });
        } catch (RuntimeException e) {
            LOG.warnf("Lembrete do titulo %s enviado, mas a marca nao foi gravada (%s).", idTitulo, e.getClass().getSimpleName());
        }
        return true;
    }

    /** "1,7,15" -> [1, 7, 15] (o CHECK da V14 garante o formato; ate 5 numeros de 1 ou 2 digitos). */
    static List<Integer> diasDepois(String texto) {
        Set<Integer> r = new LinkedHashSet<>();
        if (texto != null) {
            for (String p : texto.split(",")) {
                String s = p.strip();
                if (!s.isEmpty()) {
                    int d = Integer.parseInt(s);
                    if (d > 0) {
                        r.add(d);
                    }
                }
            }
        }
        return new ArrayList<>(r);
    }

    // ------------------------------------------------------------------------------------------------------------ apoio

    /**
     * Confere as regras, monta a mensagem e reserva o dia do cliente (o indice unico ux_cobrancaenviada_dia barra a segunda do dia).
     * Devolve [reserva, mensagem].
     */
    private Object[] reservar(String empresa, String idTitulo, String origem, LocalDate hoje, String usuario) {
        TituloEntity t = titulo(empresa, idTitulo);
        if (!"RECEBER".equals(t.getTipo())) {
            throw conflito("cobranca_so_a_receber", "Só se cobra título a receber.");
        }
        if (t.getAprovadoEm() == null) {
            throw conflito("titulo_sem_aprovacao", "Este título está aguardando aprovação: aprove-o antes de cobrar.");
        }
        if (!Set.of("ABERTO", "PARCIAL").contains(t.getSituacao())) {
            throw conflito("titulo_sem_saldo", "Este título está " + TitulosService.ROTULO_SITUACAO.get(t.getSituacao()) + ": não há o que cobrar.");
        }
        if (Boolean.TRUE.equals(t.getParaRevisao())) {
            throw conflito("titulo_para_revisao", "Este título está para revisão: resolva antes de cobrar.");
        }
        PessoaEntity p = t.getIdPessoa() == null ? null : em.find(PessoaEntity.class, t.getIdPessoa());
        if (p == null || p.getEmpresa() == null || !empresa.equals(p.getEmpresa().getId())) {
            throw conflito("titulo_sem_cliente", "Este título não tem cliente: informe o cliente antes de cobrar.");
        }
        if (Boolean.TRUE.equals(p.getNaoEnviarCobranca())) {
            throw conflito("cliente_nao_quer_cobranca", "Este cliente pediu para não receber cobrança por e-mail.");
        }
        String email = p.getEmail() == null ? null : p.getEmail().strip();
        if (email == null || email.isEmpty() || !email.contains("@")) {
            throw conflito("cliente_sem_email", "O cliente não tem e-mail no cadastro.");
        }
        Mensagem m = mensagem(empresa, t, p, email);
        var agora = AuthService.agora();
        CobrancaEnviadaEntity c = new CobrancaEnviadaEntity();
        c.setEmpresa(em.getReference(EmpresaEntity.class, empresa));
        c.setTitulo(t);
        c.setIdPessoa(p.getId());
        c.setOrigem(origem);
        c.setDia(hoje);
        c.setEmail(email);
        c.setEnviadoEm(agora);
        c.setIdEnviadoPor(usuario);
        c.setDataCriacao(agora);
        c.setDataAtualizacao(agora);
        try {
            em.persist(c);
            em.flush();
        } catch (PersistenceException e) {
            throw conflito("cobranca_ja_enviada_hoje", "Este cliente já recebeu uma cobrança hoje. Tente amanhã.");
        }
        return new Object[] {c, m};
    }

    private Mensagem mensagem(String empresa, TituloEntity t, PessoaEntity p, String email) {
        EmpresaEntity e = em.find(EmpresaEntity.class, empresa);
        String quem = e.getNomeFantasia() != null && !e.getNomeFantasia().isBlank() ? e.getNomeFantasia() : e.getRazaoSocial();
        BigDecimal saldo = Dinheiro.normalizar(t.getValor()).subtract(Dinheiro.normalizar(t.getQuitado()));
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(p.getNome()).append(".\n\n");
        sb.append(quem).append(" lembra que há um pagamento em aberto:\n\n");
        sb.append("- ").append(t.getDescricao()).append('\n');
        sb.append("- Valor: ").append(Dinheiro.formatar(saldo)).append('\n');
        sb.append("- Vencimento: ").append(t.getVencimento().format(BR)).append("\n\n");
        String comoPagar = comoPagar(empresa, t);
        if (comoPagar != null) {
            sb.append("Como pagar:\n").append(comoPagar).append("\n\n");
        }
        sb.append("Se você já pagou, desconsidere esta mensagem.\n\n").append(quem).append('\n');
        String assunto = "Cobrança: " + t.getDescricao() + " (vence em " + t.getVencimento().format(BR) + ")";
        return new Mensagem(email, assunto.length() > 200 ? assunto.substring(0, 200) : assunto, sb.toString());
    }

    /** A cobranca viva do gateway (se houver); senao os dados da conta padrao da forma prevista. Nunca gera cobranca nova. */
    private String comoPagar(String empresa, TituloEntity t) {
        List<CobrancaEntity> vivas = em.createQuery("select c from CobrancaEntity c where c.titulo.id = :t and c.estado = 'AGUARDANDO'",
                CobrancaEntity.class).setParameter("t", t.getId()).getResultList();
        if (!vivas.isEmpty()) {
            CobrancaEntity c = vivas.get(0);
            List<String> l = new ArrayList<>();
            if (c.getLinhaDigitavel() != null) {
                l.add("Boleto, linha digitável: " + c.getLinhaDigitavel());
            }
            if (https(c.getUrlDoBoleto())) {
                l.add("Boleto: " + c.getUrlDoBoleto());
            }
            if (c.getPixCopiaECola() != null) {
                l.add("Pix copia e cola: " + c.getPixCopiaECola());
            }
            if (https(c.getUrlDoLink())) {
                l.add("Link de pagamento: " + c.getUrlDoLink());
            }
            if (!l.isEmpty()) {
                return String.join("\n", l);
            }
        }
        String campo = t.getFormaPrevista() == null ? null : Map.of("DINHEIRO", "padraoDinheiro", "PIX", "padraoPix", "BOLETO", "padraoBoleto",
                "CARTAO", "padraoCartao", "TRANSFERENCIA", "padraoTransferencia").get(t.getFormaPrevista());
        if (campo == null) {
            return null;
        }
        return em.createQuery("select c from ContaFinanceiraEntity c where c.empresa.id = :e and c.ativo = true and c." + campo + " = true",
                ContaFinanceiraEntity.class).setParameter("e", empresa).getResultStream().map(ContaFinanceiraEntity::getDadosDoBanco)
                .filter(d -> d != null && !d.isBlank()).findFirst().orElse(null);
    }

    private static boolean https(String url) {
        return url != null && url.startsWith("https://");
    }

    private boolean mandar(Mensagem m) {
        try {
            mailer.send(Mail.withText(m.para(), m.assunto(), m.texto()));
            return true;
        } catch (RuntimeException e) {
            LOG.warnf("E-mail de cobranca nao enviado (%s).", e.getClass().getSimpleName()); // sem o endereco nem o texto no log
            return false;
        }
    }

    private void liberar(String idReserva) {
        CobrancaEnviadaEntity c = em.find(CobrancaEnviadaEntity.class, idReserva);
        if (c != null) {
            em.remove(c);
            em.flush();
        }
    }

    private TituloEntity titulo(String empresa, String id) {
        TituloEntity t = id == null || id.length() > 36 ? null : em.find(TituloEntity.class, id);
        if (t == null || t.getEmpresa() == null || !empresa.equals(t.getEmpresa().getId())) {
            throw new ApiException(404, "nao_encontrado", "Título não encontrado.");
        }
        return t;
    }

    private static ApiException conflito(String codigo, String mensagem) {
        return new ApiException(409, codigo, mensagem);
    }

    private static Enviada dto(CobrancaEnviadaEntity c) {
        return new Enviada(c.getId(), c.getTitulo().getId(), c.getOrigem(), c.getDia(), c.getEmail(), c.getEnviadoEm());
    }
}
