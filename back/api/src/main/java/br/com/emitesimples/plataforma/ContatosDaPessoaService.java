package br.com.emitesimples.plataforma;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.ContatoEntity;
import br.com.emitesimples.cadastros.entity.PessoaEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lista e substitui os contatos de uma Pessoa. A empresa e SEMPRE a da sessao (a Pessoa precisa ser dela; o corpo nunca
 * informa empresa). O administrador da plataforma (irrestrito, sem empresa na sessao) enxerga qualquer Pessoa e os
 * contatos herdam a empresa dela. O banco aplica a RLS por cima (conexao com o escopo do JWT).
 */
@ApplicationScoped
public class ContatosDaPessoaService {

    public static final int MAXIMO = 50;
    static final Set<String> TIPOS = Set.of("socio", "financeiro", "comercial", "fiscal", "outro");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+'-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");
    private static final Pattern FONE = Pattern.compile("^[0-9 ()+.\\-]+$");

    public record ContatoDto(String id, String tipoContato, String nome, String whatsapp, String email) {
    }

    public record ContatoEntrada(String id, String tipoContato, String nome, String whatsapp, String email) {
    }

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    public List<ContatoDto> listar(String pessoaId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            PessoaEntity p = pessoa(pessoaId, false);
            return existentes(p);
        }).stream().map(ContatosDaPessoaService::dto).toList();
    }

    public List<ContatoDto> substituir(String pessoaId, List<ContatoEntrada> corpo) {
        List<ContatoEntrada> itens = validar(corpo);
        return QuarkusTransaction.requiringNew().call(() -> {
            PessoaEntity p = pessoa(pessoaId, true);
            Map<String, ContatoEntity> atuais = new LinkedHashMap<>();
            for (ContatoEntity c : existentes(p)) {
                atuais.put(c.getId(), c);
            }
            List<Map<String, String>> erros = new ArrayList<>();
            for (int i = 0; i < itens.size(); i++) {
                String id = itens.get(i).id();
                if (id != null && !atuais.containsKey(id)) {
                    erros.add(erro("contatos[" + i + "].id", "Contato não encontrado nesta pessoa."));
                }
            }
            if (!erros.isEmpty()) {
                throw invalido(erros);
            }
            Set<String> mantidos = new java.util.HashSet<>();
            itens.forEach(i -> {
                if (i.id() != null) {
                    mantidos.add(i.id());
                }
            });
            for (ContatoEntity c : atuais.values()) {
                if (!mantidos.contains(c.getId())) {
                    em.remove(c);
                }
            }
            var agora = AuthService.agora();
            for (ContatoEntrada i : itens) {
                if (i.id() == null) {
                    ContatoEntity c = new ContatoEntity();
                    c.setTipoContato(i.tipoContato());
                    c.setNome(i.nome());
                    c.setWhatsapp(i.whatsapp());
                    c.setEmail(i.email());
                    c.setPessoa(p);
                    c.setEmpresa(p.getEmpresa());
                    c.setDataCriacao(agora);
                    c.setDataAtualizacao(agora);
                    em.persist(c);
                } else {
                    ContatoEntity c = atuais.get(i.id());
                    boolean mudou = !java.util.Objects.equals(c.getTipoContato(), i.tipoContato())
                            || !java.util.Objects.equals(c.getNome(), i.nome())
                            || !java.util.Objects.equals(c.getWhatsapp(), i.whatsapp())
                            || !java.util.Objects.equals(c.getEmail(), i.email());
                    if (mudou) {
                        c.setTipoContato(i.tipoContato());
                        c.setNome(i.nome());
                        c.setWhatsapp(i.whatsapp());
                        c.setEmail(i.email());
                        c.setDataAtualizacao(agora);
                    }
                }
            }
            em.flush();
            em.clear();
            return existentes(em.getReference(PessoaEntity.class, p.getId()));
        }).stream().map(ContatosDaPessoaService::dto).toList();
    }

    // ------------------------------------------------------------------------------------------ internos

    private PessoaEntity pessoa(String id, boolean travar) {
        String jpql = "select p from PessoaEntity p where p.id = :id" + (escopo.irrestrito() ? "" : " and p.empresa.id = :e");
        var q = em.createQuery(jpql, PessoaEntity.class).setParameter("id", id);
        if (!escopo.irrestrito()) {
            q.setParameter("e", escopo.exigir("empresa_id"));
        }
        if (travar) {
            q.setLockMode(LockModeType.PESSIMISTIC_WRITE); // serializa PUTs concorrentes da mesma pessoa
        }
        return q.getResultStream().findFirst()
                .orElseThrow(() -> new ApiException(404, "nao_encontrado", "Pessoa não encontrada."));
    }

    private List<ContatoEntity> existentes(PessoaEntity p) {
        return em.createQuery("select c from ContatoEntity c where c.pessoa.id = :p order by lower(c.nome), c.id",
                ContatoEntity.class).setParameter("p", p.getId()).getResultList();
    }

    private static ContatoDto dto(ContatoEntity c) {
        return new ContatoDto(c.getId(), c.getTipoContato(), c.getNome(), c.getWhatsapp(), c.getEmail());
    }

    private static Map<String, String> erro(String campo, String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", campo);
        m.put("mensagem", mensagem);
        return m;
    }

    private static ApiException invalido(List<Map<String, String>> campos) {
        return new ApiException(400, "requisicao_invalida", "Há campos inválidos na requisição.", Map.of("campos", campos));
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Valida e normaliza a lista inteira (sem tocar no banco). Erros apontam o item: contatos[2].nome. */
    static List<ContatoEntrada> validar(List<ContatoEntrada> corpo) {
        List<Map<String, String>> erros = new ArrayList<>();
        if (corpo == null) {
            throw invalido(List.of(erro("contatos", "Envie a lista de contatos (use [] para nenhum).")));
        }
        if (corpo.size() > MAXIMO) {
            throw invalido(List.of(erro("contatos", "No máximo " + MAXIMO + " contatos por pessoa.")));
        }
        List<ContatoEntrada> saida = new ArrayList<>();
        Map<String, Integer> ids = new HashMap<>();
        for (int i = 0; i < corpo.size(); i++) {
            String p = "contatos[" + i + "].";
            ContatoEntrada c = corpo.get(i);
            if (c == null) {
                erros.add(erro("contatos[" + i + "]", "Contato vazio."));
                continue;
            }
            String id = vazioParaNulo(c.id());
            if (id != null && ids.put(id, i) != null) {
                erros.add(erro(p + "id", "Contato repetido na lista."));
            }
            String tipo = c.tipoContato() == null ? null : c.tipoContato().trim();
            if (tipo == null || !TIPOS.contains(tipo)) {
                erros.add(erro(p + "tipoContato", "Use socio, financeiro, comercial, fiscal ou outro."));
            }
            String nome = vazioParaNulo(c.nome());
            if (nome == null) {
                erros.add(erro(p + "nome", "Informe o nome."));
            } else if (nome.length() > 300) {
                erros.add(erro(p + "nome", "No máximo 300 caracteres."));
            }
            String fone = vazioParaNulo(c.whatsapp());
            if (fone != null) {
                if (!FONE.matcher(fone).matches()) {
                    erros.add(erro(p + "whatsapp", "Use apenas dígitos e ( ) + - . espaço."));
                    fone = null;
                } else {
                    fone = fone.replaceAll("[^0-9]", "");
                    if (fone.isEmpty()) {
                        erros.add(erro(p + "whatsapp", "Informe ao menos um dígito."));
                        fone = null;
                    } else if (fone.length() > 20) {
                        erros.add(erro(p + "whatsapp", "No máximo 20 dígitos."));
                    }
                }
            }
            String email = vazioParaNulo(c.email());
            if (email != null && (email.length() > 60 || !EMAIL.matcher(email).matches())) {
                erros.add(erro(p + "email", email.length() > 60 ? "No máximo 60 caracteres." : "E-mail inválido."));
            }
            saida.add(new ContatoEntrada(id, tipo, nome, fone, email));
        }
        if (!erros.isEmpty()) {
            throw invalido(erros);
        }
        return saida;
    }
}
