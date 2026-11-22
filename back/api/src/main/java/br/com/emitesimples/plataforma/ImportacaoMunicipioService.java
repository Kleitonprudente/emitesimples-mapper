package br.com.emitesimples.plataforma;

import br.com.emitesimples.auth.AuthService;
import br.com.emitesimples.cadastros.entity.ImportacaoMunicipioEntity;
import br.com.emitesimples.cadastros.entity.MunicipioEntity;
import br.com.emitesimples.plataforma.ArquivoMunicipios.Linha;
import br.com.emitesimples.plataforma.ArquivoMunicipios.Lido;
import br.com.emitesimples.plataforma.PlataformaDtos.ImportacaoResumo;
import br.com.emitesimples.plataforma.PlataformaDtos.Rejeicao;
import br.com.emitesimples.plataforma.PlataformaDtos.UsuarioRef;
import br.com.emitesimples.security.ApiException;
import br.com.emitesimples.usuarios.entity.UsuarioEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;

/**
 * Importacao da tabela de municipios (docs/carga-municipios.md). Atualiza ou insere pelo codigo IBGE, nunca apaga.
 * Linhas invalidas sao rejeitadas e relatadas (as validas sao aplicadas); a aplicacao e o registro do historico
 * (ImportacaoMunicipio) acontecem na MESMA transacao: qualquer erro inesperado desfaz tudo. Uma falha de arquivo
 * (cabecalho, vazio) ou inesperada grava um historico estado=falhou em transacao propria.
 */
@ApplicationScoped
public class ImportacaoMunicipioService {

    private static final Logger LOG = Logger.getLogger(ImportacaoMunicipioService.class);

    static final String EM_ANDAMENTO = "em_andamento";
    static final String CONCLUIDA = "concluida";
    static final String CONCLUIDA_COM_REJEICOES = "concluida_com_rejeicoes";
    static final String FALHOU = "falhou";
    static final int MAX_REJEICOES_NA_RESPOSTA = 1000;
    private static final int MAX_RESUMO = 4000;
    private static final long CHAVE_TRAVA = 727_401_001L;

    static final Set<String> UFS = Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO", "EX");

    @Inject
    EntityManager em;

    public ImportacaoResumo importar(String usuarioId, String nomeArquivo, byte[] bytes) {
        LocalDateTime inicio = AuthService.agora();
        String nome = limpar(nomeArquivo);
        Lido lido;
        try {
            lido = ArquivoMunicipios.ler(bytes);
        } catch (ApiException e) {
            registrarFalha(usuarioId, nome, null, null, inicio, e.getMessage());
            throw e;
        }
        try {
            return QuarkusTransaction.requiringNew().call(() -> aplicar(usuarioId, nome, lido, inicio));
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            LOG.error("Falha inesperada na importação de municípios; nada foi gravado.", e);
            registrarFalha(usuarioId, nome, lido.codificacao(), lido.separador(), inicio,
                    "Erro interno ao processar o arquivo. Nada foi alterado.");
            throw e;
        }
    }

    private ImportacaoResumo aplicar(String usuarioId, String nomeArquivo, Lido lido, LocalDateTime inicio) {
        // uma importacao por vez (a unicidade do codigo e do par nome+UF e do banco; isto evita corrida entre dois envios)
        em.createNativeQuery("select count(*) from (select pg_advisory_xact_lock(" + CHAVE_TRAVA + ")) t").getSingleResult();

        Map<String, MunicipioEntity> porCodigo = new HashMap<>();
        Map<String, String> codigoPorNomeUf = new HashMap<>();
        for (MunicipioEntity m : em.createQuery("select m from MunicipioEntity m", MunicipioEntity.class).getResultList()) {
            porCodigo.put(m.getCodigoIbge(), m);
            codigoPorNomeUf.put(chaveNomeUf(m.getNome(), m.getUf()), m.getCodigoIbge());
        }

        List<Rejeicao> rejeicoes = new ArrayList<>();
        Map<String, Integer> vistos = new HashMap<>();
        int criados = 0;
        int atualizados = 0;
        int iguais = 0;
        LocalDateTime agora = AuthService.agora();

        for (Linha l : lido.linhas()) {
            String uf = l.uf().toUpperCase(java.util.Locale.ROOT);
            String codigo = l.codigo();
            String nome = l.nome();
            String motivo = validar(codigo, nome, uf);
            if (motivo == null) {
                if ("EX".equals(uf)) {
                    nome = "Exterior"; // o IBGE nao da nome ao exterior (docs/carga-municipios.md)
                }
                Integer primeira = vistos.putIfAbsent(codigo, l.numero());
                if (primeira != null) {
                    motivo = "Código IBGE repetido no arquivo (primeira ocorrência na linha " + primeira + ").";
                }
            }
            if (motivo == null) {
                String dono = codigoPorNomeUf.get(chaveNomeUf(nome, uf));
                if (dono != null && !dono.equals(codigo)) {
                    motivo = "Já existe outro município com o mesmo nome e UF (código " + dono + ").";
                }
            }
            if (motivo != null) {
                rejeicoes.add(new Rejeicao(l.numero(), motivo));
                continue;
            }

            MunicipioEntity atual = porCodigo.get(codigo);
            if (atual == null) {
                MunicipioEntity novo = MunicipioEntity.builderOf()
                        .withCodigoIbge(codigo).withNome(nome).withUf(uf)
                        .withDataCriacao(agora).withDataAtualizacao(agora).build();
                em.persist(novo);
                porCodigo.put(codigo, novo);
                codigoPorNomeUf.put(chaveNomeUf(nome, uf), codigo);
                criados++;
            } else if (atual.getNome().equals(nome) && atual.getUf().equals(uf)) {
                iguais++;
            } else {
                codigoPorNomeUf.remove(chaveNomeUf(atual.getNome(), atual.getUf()));
                atual.setNome(nome);
                atual.setUf(uf);
                atual.setDataAtualizacao(agora);
                codigoPorNomeUf.put(chaveNomeUf(nome, uf), codigo);
                // grava ja: o Hibernate faz os INSERTs antes dos UPDATEs e o indice unico (nome, uf) nao e adiavel
                em.flush();
                atualizados++;
            }
        }

        boolean comRejeicoes = !rejeicoes.isEmpty();
        ImportacaoMunicipioEntity h = ImportacaoMunicipioEntity.builderOf()
                .withNomeArquivo(nomeArquivo)
                .withCodificacaoDetectada(lido.codificacao())
                .withSeparadorDetectado(lido.separador())
                .withEstado(comRejeicoes ? CONCLUIDA_COM_REJEICOES : CONCLUIDA)
                .withDataHoraInicio(inicio)
                .withDataHoraFim(AuthService.agora())
                .withLinhasLidas(lido.linhas().size())
                .withLinhasInseridas(criados)
                .withLinhasAtualizadas(atualizados)
                .withLinhasIguais(iguais)
                .withLinhasRejeitadas(rejeicoes.size())
                .withResumoRejeicoes(resumo(rejeicoes))
                .withDataCriacao(agora).withDataAtualizacao(agora)
                .withUsuario(em.getReference(UsuarioEntity.class, usuarioId))
                .build();
        em.persist(h);
        em.flush();
        return paraResumo(h, null, rejeicoes);
    }

    /** Devolve o motivo da rejeicao ou null se a linha e aceitavel (regra 2 e 3 do documento). */
    static String validar(String codigo, String nome, String uf) {
        if (!UFS.contains(uf)) {
            return "UF inválida (use uma das 27 UFs ou EX).";
        }
        if ("EX".equals(uf)) {
            return "0".equals(codigo) ? null : "UF EX exige o código IBGE 0.";
        }
        if (!codigo.matches("\\d{7}")) {
            return "Código IBGE fora do padrão (7 dígitos).";
        }
        if (nome.isBlank()) {
            return "Nome do município vazio.";
        }
        if (nome.length() > 100) {
            return "Nome do município com mais de 100 caracteres.";
        }
        return null;
    }

    private static String chaveNomeUf(String nome, String uf) {
        return uf + "|" + nome;
    }

    private static String resumo(List<Rejeicao> rejeicoes) {
        if (rejeicoes.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rejeicoes.size(); i++) {
            String parte = "linha " + rejeicoes.get(i).linha() + ": " + rejeicoes.get(i).motivo();
            String sep = sb.length() == 0 ? "" : "\n";
            String reticencias = "\n... (" + (rejeicoes.size() - i) + " rejeição(ões) a mais)";
            if (sb.length() + sep.length() + parte.length() + reticencias.length() > MAX_RESUMO) {
                sb.append(reticencias);
                break;
            }
            sb.append(sep).append(parte);
        }
        return sb.toString();
    }

    private void registrarFalha(String usuarioId, String nomeArquivo, String codificacao, String separador,
                                LocalDateTime inicio, String motivo) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                LocalDateTime agora = AuthService.agora();
                em.persist(ImportacaoMunicipioEntity.builderOf()
                        .withNomeArquivo(nomeArquivo)
                        .withCodificacaoDetectada(codificacao)
                        .withSeparadorDetectado(separador)
                        .withEstado(FALHOU)
                        .withDataHoraInicio(inicio).withDataHoraFim(agora)
                        .withLinhasLidas(0).withLinhasInseridas(0).withLinhasAtualizadas(0)
                        .withLinhasIguais(0).withLinhasRejeitadas(0)
                        .withResumoRejeicoes(motivo)
                        .withDataCriacao(agora).withDataAtualizacao(agora)
                        .withUsuario(em.getReference(UsuarioEntity.class, usuarioId))
                        .build());
            });
        } catch (RuntimeException e) {
            LOG.warn("Não foi possível registrar o histórico da importação que falhou.");
        }
    }

    /** Ultimas importacoes, da mais recente para a mais antiga. */
    public List<ImportacaoResumo> ultimas(int limite) {
        return QuarkusTransaction.requiringNew().call(() -> em.createQuery(
                        "select i from ImportacaoMunicipioEntity i join fetch i.usuario "
                                + "order by i.dataHoraInicio desc, i.id desc", ImportacaoMunicipioEntity.class)
                .setMaxResults(limite).getResultList().stream()
                .map(i -> paraResumo(i, new UsuarioRef(i.getUsuario().getId(), i.getUsuario().getNome(),
                        i.getUsuario().getEmail()), null))
                .toList());
    }

    private static ImportacaoResumo paraResumo(ImportacaoMunicipioEntity h, UsuarioRef usuario, List<Rejeicao> rejeicoes) {
        List<Rejeicao> lista = null;
        int omitidas = 0;
        if (rejeicoes != null) {
            lista = rejeicoes.size() > MAX_REJEICOES_NA_RESPOSTA
                    ? rejeicoes.subList(0, MAX_REJEICOES_NA_RESPOSTA) : rejeicoes;
            omitidas = rejeicoes.size() - lista.size();
        }
        return new ImportacaoResumo(h.getId(), h.getNomeArquivo(), h.getEstado(), h.getCodificacaoDetectada(),
                h.getSeparadorDetectado(), h.getDataHoraInicio(), h.getDataHoraFim(), h.getLinhasLidas(),
                h.getLinhasInseridas(), h.getLinhasAtualizadas(), h.getLinhasIguais(), h.getLinhasRejeitadas(),
                lista, omitidas > 0 ? omitidas : null, rejeicoes == null ? h.getResumoRejeicoes() : null, usuario);
    }

    private static String limpar(String nome) {
        if (nome == null || nome.isBlank()) {
            return "municipios.csv";
        }
        String n = nome.replaceAll("[\\p{Cntrl}]", "");
        int barra = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        n = n.substring(barra + 1);
        return n.length() > 255 ? n.substring(0, 255) : n;
    }
}
