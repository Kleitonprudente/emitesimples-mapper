package br.com.emitesimples.financeiroop;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.configuracao.entity.EmpresaEntity;
import br.com.emitesimples.financeiro.entity.BancoEntity;
import br.com.emitesimples.financeiro.entity.CategoriaFinanceiraEntity;
import br.com.emitesimples.financeiro.entity.CondicaoPagamentoEntity;
import br.com.emitesimples.financeiro.entity.ContaFinanceiraEntity;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.tenant.EscopoTenant;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Importar e exportar CSV do financeiro (docs/financeiro/README.md, F4-C3 e F4-C4; referencia §4.5, §10 e §11.7).
 *
 * <ul>
 * <li>Contas, categorias e condicoes: UTF-8, separador ';', cabecalho fixo. Importar so CRIA (nome existente e ignorado), TUDO OU NADA
 * (erro em qualquer linha recusa o arquivo com "linha N: ..."), ate 1.000 linhas e 1 MB. As regras do banco (V14) valem por cima.</li>
 * <li>Bancos: o CSV publico do Banco Central, colunas achadas pelo nome; cria ou atualiza pelo codigo e nunca apaga nem desmarca.</li>
 * </ul>
 */
@ApplicationScoped
public class CsvFinanceiro {

    public static final int MAX_LINHAS = 1000;
    public static final int MAX_BYTES = 1024 * 1024;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<String> FORMAS = TitulosService.FORMAS;

    public static final Map<String, List<String>> CABECALHOS = Map.of(
            "contas", List.of("nome", "tipo", "codigo_banco", "agencia", "dados_do_banco", "saldo_inicial", "data_saldo_inicial", "ativo"),
            "categorias", List.of("nome", "tipo", "categoria_pai", "dicas", "ativo"),
            "condicoes", List.of("nome", "forma", "parcelas", "dias_primeiro_vencimento", "intervalo_dias", "receber_na_emissao", "ativo"));

    public record Ignorado(int linha, String nome, String motivo) {
    }

    public record ResultadoDaImportacao(int criados, List<Ignorado> ignorados) {
    }

    public record ResultadoDosBancos(int criados, int atualizados, int ignorados) {
    }

    @Inject
    EntityManager em;

    @Inject
    EscopoTenant escopo;

    // ------------------------------------------------------------------------------------------------------------ exportar

    public String exportar(String entidade) {
        String empresa = escopo.exigir("empresa_id");
        List<String> cab = cabecalho(entidade);
        return QuarkusTransaction.requiringNew().call(() -> {
            StringBuilder sb = new StringBuilder(String.join(";", cab)).append('\n');
            switch (entidade) {
                case "contas" -> {
                    for (ContaFinanceiraEntity c : em.createQuery("select c from ContaFinanceiraEntity c where c.empresa.id = :e order by c.nome",
                            ContaFinanceiraEntity.class).setParameter("e", empresa).getResultList()) {
                        linha(sb, c.getNome(), c.getTipo(), c.getCodigoBanco(), c.getAgencia(), c.getDadosDoBanco(), dinheiro(c.getSaldoInicial()),
                                c.getDataSaldoInicial() == null ? null : c.getDataSaldoInicial().format(BR), simNao(c.getAtivo()));
                    }
                }
                case "categorias" -> {
                    // pais primeiro: o arquivo exportado volta a importar sem depender da ordem
                    for (CategoriaFinanceiraEntity c : em.createQuery("select c from CategoriaFinanceiraEntity c left join fetch c.categoriaPai "
                            + "where c.empresa.id = :e order by case when c.categoriaPai is null then 0 else 1 end, c.tipo, c.nome",
                            CategoriaFinanceiraEntity.class).setParameter("e", empresa).getResultList()) {
                        linha(sb, c.getNome(), c.getTipo(), c.getCategoriaPai() == null ? null : c.getCategoriaPai().getNome(), c.getDicas(),
                                simNao(c.getAtivo()));
                    }
                }
                default -> {
                    for (CondicaoPagamentoEntity c : em.createQuery("select c from CondicaoPagamentoEntity c where c.empresa.id = :e order by c.nome",
                            CondicaoPagamentoEntity.class).setParameter("e", empresa).getResultList()) {
                        linha(sb, c.getNome(), c.getForma(), str(c.getParcelas()), str(c.getDiasPrimeiroVencimento()), str(c.getIntervaloDias()),
                                simNao(c.getReceberNaEmissao()), simNao(c.getAtivo()));
                    }
                }
            }
            return sb.toString();
        });
    }

    // ------------------------------------------------------------------------------------------------------------ importar

    public ResultadoDaImportacao importar(String entidade, String texto) {
        String empresa = escopo.exigir("empresa_id");
        List<String> cab = cabecalho(entidade);
        List<List<String>> linhas = ler(texto, ';');
        if (linhas.isEmpty()) {
            throw invalido("O arquivo está vazio.");
        }
        List<String> recebido = linhas.get(0).stream().map(CsvFinanceiro::chave).toList();
        if (!recebido.equals(cab)) {
            throw invalido("Cabeçalho inválido. Use: " + String.join(";", cab));
        }
        if (linhas.size() - 1 > MAX_LINHAS) {
            throw invalido("O arquivo tem mais de " + MAX_LINHAS + " linhas.");
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            EmpresaEntity emp = em.getReference(EmpresaEntity.class, empresa);
            Set<String> existentes = new java.util.HashSet<>(nomesExistentes(entidade, empresa));
            List<Ignorado> ignorados = new ArrayList<>();
            int criados = 0;
            var agora = AuthService.agora();
            // categorias: pais (sem categoria_pai) antes das filhas, para o pai do proprio arquivo ja existir
            List<Integer> ordem = new ArrayList<>();
            for (int i = 1; i < linhas.size(); i++) {
                ordem.add(i);
            }
            if ("categorias".equals(entidade)) {
                ordem.sort((a, b) -> Boolean.compare(!vazio(campo(linhas.get(a), 2)), !vazio(campo(linhas.get(b), 2))));
            }
            for (int i : ordem) {
                List<String> l = linhas.get(i);
                int n = i + 1; // numero da linha no arquivo (o cabecalho e a linha 1)
                if (l.stream().allMatch(CsvFinanceiro::vazio)) {
                    continue;
                }
                if (l.size() != cab.size()) {
                    throw invalido("linha " + n + ": esperava " + cab.size() + " colunas e veio " + l.size() + ".");
                }
                String nome = obrigatorio(n, "nome", l.get(0));
                String chave = "categorias".equals(entidade) ? l.get(1).trim().toUpperCase(Locale.ROOT) + "|" + nome : nome;
                if (existentes.contains(chave)) {
                    ignorados.add(new Ignorado(n, nome, "Já existe com este nome."));
                    continue;
                }
                Object entity = switch (entidade) {
                    case "contas" -> conta(n, l, emp, agora);
                    case "categorias" -> categoria(n, l, emp, empresa, agora);
                    default -> condicao(n, l, emp, agora);
                };
                try {
                    em.persist(entity);
                    em.flush();
                } catch (PersistenceException e) {
                    ApiException traduzida = RestricoesFinanceiro.traduzir(e);
                    throw invalido("linha " + n + ": " + (traduzida != null ? traduzida.getMessage() : "não foi possível gravar (confira os valores)."));
                } catch (ApiException e) {
                    throw invalido("linha " + n + ": " + e.getMessage());
                }
                existentes.add(chave);
                criados++;
            }
            return new ResultadoDaImportacao(criados, ignorados);
        });
    }

    private ContaFinanceiraEntity conta(int n, List<String> l, EmpresaEntity emp, java.time.LocalDateTime agora) {
        ContaFinanceiraEntity c = new ContaFinanceiraEntity();
        c.setEmpresa(emp);
        c.setNome(l.get(0).trim());
        c.setTipo(enumDe(n, "tipo", l.get(1), Set.of("CAIXA", "BANCO", "CARTEIRA")));
        c.setCodigoBanco(nulo(l.get(2)));
        c.setAgencia(nulo(l.get(3)));
        c.setDadosDoBanco(nulo(l.get(4)));
        c.setSaldoInicial(vazio(l.get(5)) ? Dinheiro.ZERO : dinheiroDe(n, "saldo_inicial", l.get(5)));
        c.setDataSaldoInicial(vazio(l.get(6)) ? TitulosService.hoje() : dataDe(n, "data_saldo_inicial", l.get(6)));
        c.setAtivo(simNaoDe(n, "ativo", l.get(7), true));
        c.setPadraoDinheiro(false);
        c.setPadraoPix(false);
        c.setPadraoBoleto(false);
        c.setPadraoCartao(false);
        c.setPadraoTransferencia(false);
        c.setDataCriacao(agora);
        c.setDataAtualizacao(agora);
        return c;
    }

    private CategoriaFinanceiraEntity categoria(int n, List<String> l, EmpresaEntity emp, String empresa, java.time.LocalDateTime agora) {
        CategoriaFinanceiraEntity c = new CategoriaFinanceiraEntity();
        c.setEmpresa(emp);
        c.setNome(l.get(0).trim());
        String tipo = enumDe(n, "tipo", l.get(1), Set.of("RECEITA", "DESPESA"));
        c.setTipo(tipo);
        if (!vazio(l.get(2))) {
            List<CategoriaFinanceiraEntity> pai = em.createQuery("select c from CategoriaFinanceiraEntity c where c.empresa.id = :e and c.tipo = :t "
                    + "and c.nome = :n", CategoriaFinanceiraEntity.class).setParameter("e", empresa).setParameter("t", tipo)
                    .setParameter("n", l.get(2).trim()).getResultList();
            if (pai.isEmpty()) {
                throw invalido("linha " + n + ": categoria pai \"" + l.get(2).trim() + "\" não encontrada neste tipo.");
            }
            c.setCategoriaPai(pai.get(0));
        }
        c.setDicas(CategoriaFinanceiraListener.limparDicas(nulo(l.get(3))));
        c.setAtivo(simNaoDe(n, "ativo", l.get(4), true));
        c.setDataCriacao(agora);
        c.setDataAtualizacao(agora);
        return c;
    }

    private CondicaoPagamentoEntity condicao(int n, List<String> l, EmpresaEntity emp, java.time.LocalDateTime agora) {
        CondicaoPagamentoEntity c = new CondicaoPagamentoEntity();
        c.setEmpresa(emp);
        c.setNome(l.get(0).trim());
        c.setForma(enumDe(n, "forma", l.get(1), FORMAS));
        c.setParcelas(inteiroDe(n, "parcelas", l.get(2), 1));
        c.setDiasPrimeiroVencimento(inteiroDe(n, "dias_primeiro_vencimento", l.get(3), 0));
        c.setIntervaloDias(inteiroDe(n, "intervalo_dias", l.get(4), 30));
        c.setReceberNaEmissao(simNaoDe(n, "receber_na_emissao", l.get(5), false));
        c.setAtivo(simNaoDe(n, "ativo", l.get(6), true));
        c.setDataCriacao(agora);
        c.setDataAtualizacao(agora);
        return c;
    }

    private List<String> nomesExistentes(String entidade, String empresa) {
        return switch (entidade) {
            case "contas" -> em.createQuery("select c.nome from ContaFinanceiraEntity c where c.empresa.id = :e", String.class)
                    .setParameter("e", empresa).getResultList();
            case "categorias" -> em.createQuery("select concat(c.tipo, '|', c.nome) from CategoriaFinanceiraEntity c where c.empresa.id = :e",
                    String.class).setParameter("e", empresa).getResultList();
            default -> em.createQuery("select c.nome from CondicaoPagamentoEntity c where c.empresa.id = :e", String.class)
                    .setParameter("e", empresa).getResultList();
        };
    }

    // ------------------------------------------------------------------------------------------------------------ bancos (F4-C4)

    public ResultadoDosBancos importarBancos(String texto) {
        if (texto == null || texto.isBlank()) {
            throw invalido("O arquivo está vazio.");
        }
        String primeira = texto.lines().findFirst().orElse("");
        char sep = primeira.chars().filter(ch -> ch == ';').count() > primeira.chars().filter(ch -> ch == ',').count() ? ';' : ',';
        List<List<String>> linhas = ler(texto, sep);
        List<String> cab = linhas.get(0).stream().map(CsvFinanceiro::chave).toList();
        int iIspb = indice(cab, "ispb");
        int iNome = indice(cab, "nome_reduzido");
        int iCodigo = indice(cab, "numero_codigo");
        int iCompe = indice(cab, "participa_da_compe");
        int iExtenso = indice(cab, "nome_extenso");
        if (iNome < 0 || iCodigo < 0) {
            throw invalido("Cabeçalho não reconhecido: o arquivo do Banco Central tem as colunas Nome_Reduzido e Número_Código.");
        }
        if (linhas.size() - 1 > 5000) {
            throw invalido("O arquivo tem linhas demais para o cadastro de bancos.");
        }
        return QuarkusTransaction.requiringNew().call(() -> {
            Map<String, BancoEntity> porCodigo = new HashMap<>();
            for (BancoEntity b : em.createQuery("select b from BancoEntity b", BancoEntity.class).getResultList()) {
                porCodigo.put(b.getCodigo(), b);
            }
            int criados = 0;
            int atualizados = 0;
            int ignorados = 0;
            var agora = AuthService.agora();
            for (int i = 1; i < linhas.size(); i++) {
                List<String> l = linhas.get(i);
                String bruto = campo(l, iCodigo).trim();
                if (!bruto.matches("\\d{1,4}") || bruto.matches("0+")) {
                    ignorados++; // participante sem codigo COMPE
                    continue;
                }
                String codigo = bruto.replaceFirst("^0+", "");
                String nome = cortar(campo(l, iNome).trim(), 120);
                if (nome.isEmpty()) {
                    ignorados++;
                    continue;
                }
                BancoEntity b = porCodigo.get(codigo);
                boolean novo = b == null;
                if (novo) {
                    b = new BancoEntity();
                    b.setCodigo(codigo);
                    b.setIntegradoGateway(false); // nunca desmarca: so o banco NOVO nasce sem integracao
                    b.setDataCriacao(agora);
                }
                b.setNome(nome);
                String ispb = campo(l, iIspb).trim();
                b.setIspb(ispb.matches("\\d{1,8}") ? String.format("%8s", ispb).replace(' ', '0') : b.getIspb());
                if (iExtenso >= 0 && !campo(l, iExtenso).isBlank()) {
                    b.setNomeExtenso(cortar(campo(l, iExtenso).trim(), 200));
                }
                if (iCompe >= 0) {
                    String compe = chave(campo(l, iCompe));
                    b.setParticipaCompe(compe.startsWith("s") || compe.equals("true") || compe.equals("1"));
                } else if (novo) {
                    b.setParticipaCompe(false);
                }
                b.setDataAtualizacao(agora);
                if (novo) {
                    em.persist(b);
                    porCodigo.put(codigo, b);
                    criados++;
                } else {
                    atualizados++;
                }
            }
            em.flush();
            return new ResultadoDosBancos(criados, atualizados, ignorados);
        });
    }

    // ------------------------------------------------------------------------------------------------------------ CSV e conversoes

    /** Leitor de CSV com aspas ("a;b" e "" dentro de aspas). Tira o BOM. */
    static List<List<String>> ler(String texto, char sep) {
        if (texto == null) {
            return List.of();
        }
        String t = texto.startsWith("﻿") ? texto.substring(1) : texto;
        List<List<String>> linhas = new ArrayList<>();
        List<String> atual = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (aspas) {
                if (ch == '"') {
                    if (i + 1 < t.length() && t.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        aspas = false;
                    }
                } else {
                    campo.append(ch);
                }
            } else if (ch == '"') {
                aspas = true;
            } else if (ch == sep) {
                atual.add(campo.toString());
                campo.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < t.length() && t.charAt(i + 1) == '\n') {
                    i++;
                }
                atual.add(campo.toString());
                campo.setLength(0);
                linhas.add(atual);
                atual = new ArrayList<>();
            } else {
                campo.append(ch);
            }
        }
        if (campo.length() > 0 || !atual.isEmpty()) {
            atual.add(campo.toString());
            linhas.add(atual);
        }
        return linhas;
    }

    private static void linha(StringBuilder sb, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            String c = campos[i] == null ? "" : campos[i];
            sb.append(c.contains(";") || c.contains("\"") || c.contains("\n") ? "\"" + c.replace("\"", "\"\"") + "\"" : c);
        }
        sb.append('\n');
    }

    /** Nome de coluna normalizado: sem acento, minusculo, espacos viram '_'. */
    static String chave(String s) {
        String n = Normalizer.normalize(s == null ? "" : s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static int indice(List<String> cab, String nome) {
        return cab.indexOf(nome);
    }

    private static List<String> cabecalho(String entidade) {
        List<String> c = CABECALHOS.get(entidade);
        if (c == null) {
            throw new ApiException(404, "nao_encontrado", "Use contas, categorias ou condicoes.");
        }
        return c;
    }

    private static String campo(List<String> l, int i) {
        return i >= 0 && i < l.size() && l.get(i) != null ? l.get(i) : "";
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    private static String nulo(String s) {
        return vazio(s) ? null : s.trim();
    }

    private static String cortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String simNao(Boolean b) {
        return Boolean.TRUE.equals(b) ? "sim" : "não";
    }

    private static String dinheiro(BigDecimal v) {
        return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR"))).format(Dinheiro.normalizar(v));
    }

    private static String obrigatorio(int n, String coluna, String v) {
        if (vazio(v)) {
            throw invalido("linha " + n + ": informe " + coluna + ".");
        }
        return v.trim();
    }

    private static String enumDe(int n, String coluna, String v, Set<String> validos) {
        String x = v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
        if (!validos.contains(x)) {
            throw invalido("linha " + n + ": " + coluna + " inválido (use " + String.join(", ", validos.stream().sorted().toList()) + ").");
        }
        return x;
    }

    private static BigDecimal dinheiroDe(int n, String coluna, String v) {
        try {
            return Dinheiro.deTexto(v);
        } catch (IllegalArgumentException e) {
            throw invalido("linha " + n + ": " + coluna + " inválido (use 1.234,56).");
        }
    }

    private static LocalDate dataDe(int n, String coluna, String v) {
        String x = v.trim();
        try {
            return x.contains("/") ? LocalDate.parse(x, BR) : LocalDate.parse(x);
        } catch (DateTimeParseException e) {
            throw invalido("linha " + n + ": " + coluna + " inválida (use dd/mm/aaaa).");
        }
    }

    private static Boolean simNaoDe(int n, String coluna, String v, boolean padrao) {
        if (vazio(v)) {
            return padrao;
        }
        String x = chave(v);
        if (Set.of("sim", "s", "true", "1").contains(x)) {
            return true;
        }
        if (Set.of("nao", "n", "false", "0").contains(x)) {
            return false;
        }
        throw invalido("linha " + n + ": " + coluna + " deve ser sim ou não.");
    }

    private static Integer inteiroDe(int n, String coluna, String v, int padrao) {
        if (vazio(v)) {
            return padrao;
        }
        try {
            return Integer.valueOf(v.trim());
        } catch (NumberFormatException e) {
            throw invalido("linha " + n + ": " + coluna + " deve ser um número inteiro.");
        }
    }

    static ApiException invalido(String mensagem) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("campo", "arquivo");
        m.put("mensagem", mensagem);
        return new ApiException(400, "requisicao_invalida", mensagem, Map.of("campos", List.of(m)));
    }
}
