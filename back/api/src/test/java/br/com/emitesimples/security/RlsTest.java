package br.com.emitesimples.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.emitesimples.apoio.BaseApiTest;
import br.com.emitesimples.tenant.ContextoTenantBanco;
import io.agroal.api.AgroalDataSource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PROVA DA RLS (migracao V3). A camada Java gerada continua vulneravel (POST/PUT gravam a empresa do corpo); estes
 * testes provam que o BANCO barra, conectado com um role comum de verdade (emitesimples_app: nao dono, nao
 * superusuario, sem BYPASSRLS). As conferencias de "nada foi criado" usam SQL como SUPERUSUARIO (que ignora a RLS).
 */
@QuarkusTest
class RlsTest extends BaseApiTest {

    static final String ROLE_APP = "emitesimples_app";

    @Inject
    AgroalDataSource ds;

    @Inject
    ContextoTenantBanco contexto;

    static String empA, empB, pesA, pesB, srvA, srvB, conA, conB, notA, notB;
    static String tokenA, tokenB, tokenPlat;

    @BeforeEach
    void preparar() {
        if (empA != null) {
            return;
        }
        empA = dados.empresaAtiva();
        empB = dados.empresaAtiva();
        pesA = dados.pessoa(empA, "RLS Pessoa A");
        pesB = dados.pessoa(empB, "RLS Pessoa B");
        srvA = dados.servico(empA, "RLS Servico A");
        srvB = dados.servico(empB, "RLS Servico B");
        conA = dados.contato(empA, pesA, "RLS Contato A");
        conB = dados.contato(empB, pesB, "RLS Contato B");
        notA = dados.nota(empA, pesA, srvA);
        notB = dados.nota(empB, pesB, srvB);
        tokenA = token(dados.usuarioComPapel(empA, "operador").email());
        tokenB = token(dados.usuarioComPapel(empB, "operador").email());
        tokenPlat = token(dados.usuario(true, true).email());
    }

    // --------------------------------------------------------------------------------------------- conexoes de apoio

    private static String url() {
        return ConfigProvider.getConfig().getValue("quarkus.datasource.jdbc.url", String.class);
    }

    /** Conexao como DONO/superusuario do container de teste: ignora a RLS. So para conferir o estado real. */
    private static Connection superusuario() throws SQLException {
        return DriverManager.getConnection(url(), "dono_teste", "dono-teste-efemero");
    }

    /** Conexao direta como o role da aplicacao (sem passar pelo pool nem pelo interceptor). */
    private static Connection comoApp() throws SQLException {
        return DriverManager.getConnection(url(), ROLE_APP, "app-teste-efemero");
    }

    private static long contarSuper(String sql, Object... params) {
        try (Connection c = superusuario(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String escalarSuper(String sql, Object... params) {
        try (Connection c = superusuario(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Abre transacao como o role da app com o escopo dado (null = sem configuracao nenhuma). */
    private static Connection appComEscopo(String empresaId, Boolean irrestrito) throws SQLException {
        Connection c = comoApp();
        c.setAutoCommit(false);
        if (empresaId != null) {
            try (PreparedStatement ps = c.prepareStatement("select set_config('app.empresa_id', ?, true)")) {
                ps.setString(1, empresaId);
                ps.execute();
            }
        }
        if (irrestrito != null) {
            try (PreparedStatement ps = c.prepareStatement("select set_config('app.irrestrito', ?, true)")) {
                ps.setString(1, irrestrito ? "true" : "false");
                ps.execute();
            }
        }
        return c;
    }

    private static long contar(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static final List<String> TABELAS_EMPRESA = List.of("assinaturaempresa", "atividadeempresa", "cancelamento",
            "chamadagateway", "clienteacesso", "confirmacaocadastro", "contato", "eventowebhook", "faturacrm",
            "historicosituacaoempresa", "nota", "pessoa", "qsaempresa", "servico", "tokenrenovacao", "vinculousuario");

    // ------------------------------------------------------------------------------------ (b) SQL direto como a app

    @Test
    void oRoleDaAplicacaoNaoEhSuperusuarioNemDonoNemBypassrls() throws Exception {
        try (Connection c = comoApp()) {
            assertEquals(ROLE_APP, escalar(c, "select current_user"));
            assertEquals("false", escalar(c, "select rolsuper::text from pg_roles where rolname = current_user"));
            assertEquals("false", escalar(c, "select rolbypassrls::text from pg_roles where rolname = current_user"));
            assertEquals("0", escalar(c, "select count(*) from pg_tables where schemaname = 'public' and tableowner = current_user"));
        }
        assertEquals("true", escalarSuper("select rolsuper::text from pg_roles where rolname = 'dono_teste'"),
                "o dono do container de teste e superusuario: justamente por isso o runtime nao pode usa-lo");
    }

    private static String escalar(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    @Test
    void todasAsTabelasDeEmpresaTemRlsForcadaEPolitica() {
        for (String t : TABELAS_EMPRESA) {
            assertEquals("true,true", escalarSuper("select relrowsecurity::text || ',' || relforcerowsecurity::text from pg_class where relname = ?", t),
                    "RLS ativa e forcada em " + t);
            assertEquals(1, contarSuper("select count(*) from pg_policies where tablename = ? and policyname = 'isolamento_empresa'", t));
        }
        assertEquals(1, contarSuper("select count(*) from pg_class where relname = 'empresa' and relrowsecurity and relforcerowsecurity"));
        for (String global : List.of("usuario", "municipio", "plano", "importacaomunicipio")) {
            assertEquals(0, contarSuper("select count(*) from pg_class where relname = ? and relrowsecurity", global),
                    global + " e global e nao tem RLS");
        }
    }

    @Test
    void semConfiguracaoNenhumaLinhaFalhaFechado() throws Exception {
        try (Connection c = appComEscopo(null, null)) {
            for (String t : TABELAS_EMPRESA) {
                assertEquals(0, contar(c, "select count(*) from " + t), t + " sem configuracao");
            }
            assertEquals(0, contar(c, "select count(*) from empresa"));
            // globais continuam visiveis
            assertTrue(contar(c, "select count(*) from municipio") > 0);
            assertTrue(contar(c, "select count(*) from usuario") > 0);
        }
        // configuracao vazia e irrestrito falso/lixo tambem nao liberam nada, e nunca dao erro de cast
        for (String lixo : List.of("", "nao-e-uuid", "' or '1'='1", "A", "TRUE", "1")) {
            try (Connection c = appComEscopo("", null)) {
                try (PreparedStatement ps = c.prepareStatement("select set_config('app.irrestrito', ?, true)")) {
                    ps.setString(1, lixo);
                    ps.execute();
                }
                assertEquals(0, contar(c, "select count(*) from pessoa"), "irrestrito='" + lixo + "' nao libera");
                assertEquals(0, contar(c, "select count(*) from empresa"));
            }
        }
    }

    @Test
    void comEmpresaAVeSoAComIrrestritoVeTudo() throws Exception {
        long totalPessoas = contarSuper("select count(*) from pessoa");
        assertTrue(totalPessoas >= 2);
        try (Connection c = appComEscopo(empA, false)) {
            assertEquals(1, contar(c, "select count(*) from pessoa where id = '" + pesA + "'"));
            assertEquals(0, contar(c, "select count(*) from pessoa where id = '" + pesB + "'"));
            assertEquals(0, contar(c, "select count(*) from pessoa where empresa_id <> '" + empA + "'"));
            assertEquals(0, contar(c, "select count(*) from servico where empresa_id <> '" + empA + "'"));
            assertEquals(0, contar(c, "select count(*) from contato where empresa_id <> '" + empA + "'"));
            assertEquals(0, contar(c, "select count(*) from nota where empresa_id <> '" + empA + "'"));
            assertEquals(0, contar(c, "select count(*) from vinculousuario where empresa_id <> '" + empA + "'"));
            assertEquals(0, contar(c, "select count(*) from tokenrenovacao where empresa_id <> '" + empA + "'"));
            assertEquals(1, contar(c, "select count(*) from empresa"), "a empresa so ve a si mesma");
            assertEquals(1, contar(c, "select count(*) from empresa where id = '" + empA + "'"));
        }
        try (Connection c = appComEscopo(empB, false)) {
            assertEquals(0, contar(c, "select count(*) from pessoa where id = '" + pesA + "'"));
            assertEquals(1, contar(c, "select count(*) from pessoa where id = '" + pesB + "'"));
        }
        try (Connection c = appComEscopo("", true)) {
            assertEquals(totalPessoas, contar(c, "select count(*) from pessoa"));
            assertEquals(contarSuper("select count(*) from empresa"), contar(c, "select count(*) from empresa"));
            assertEquals(contarSuper("select count(*) from nota"), contar(c, "select count(*) from nota"));
        }
    }

    @Test
    void sqlDiretoNaoGravaNemMoveLinhaParaOutraEmpresa() throws Exception {
        String novo = UUID.randomUUID().toString();
        try (Connection c = appComEscopo(empA, false)) {
            // INSERT na B estando como A: WITH CHECK barra (42501)
            SQLException e = assertThrows(SQLException.class, () -> executar(c,
                    "insert into pessoa (id, empresa_id, tipo_pessoa, nome) values ('" + novo + "', '" + empB + "', 'fisica', 'x')"));
            assertEquals("42501", e.getSQLState());
        }
        try (Connection c = appComEscopo(empA, false)) {
            // INSERT na A passa
            assertEquals(1, executar(c, "insert into pessoa (id, empresa_id, tipo_pessoa, nome) values ('"
                    + UUID.randomUUID() + "', '" + empA + "', 'fisica', 'ok')"));
            c.rollback();
        }
        try (Connection c = appComEscopo(empA, false)) {
            // UPDATE movendo linha da A para a B: WITH CHECK barra
            SQLException e = assertThrows(SQLException.class, () -> executar(c,
                    "update pessoa set empresa_id = '" + empB + "' where id = '" + pesA + "'"));
            assertEquals("42501", e.getSQLState());
        }
        try (Connection c = appComEscopo(empA, false)) {
            // UPDATE/DELETE em linha da B: USING esconde (0 linhas afetadas)
            assertEquals(0, executar(c, "update pessoa set nome = 'INVADIDA' where id = '" + pesB + "'"));
            assertEquals(0, executar(c, "delete from pessoa where id = '" + pesB + "'"));
            assertEquals(0, executar(c, "update empresa set razao_social = 'INVADIDA' where id = '" + empB + "'"));
        }
        assertEquals(0, contarSuper("select count(*) from pessoa where id = ?", novo));
        assertEquals("RLS Pessoa B", escalarSuper("select nome from pessoa where id = ?", pesB));
        assertEquals(empA, escalarSuper("select empresa_id from pessoa where id = ?", pesA));
        // sem configuracao nem INSERT
        try (Connection c = appComEscopo(null, null)) {
            SQLException e = assertThrows(SQLException.class, () -> executar(c,
                    "insert into servico (id, empresa_id, nome) values ('" + UUID.randomUUID() + "', '" + empA + "', 'x')"));
            assertEquals("42501", e.getSQLState());
        }
    }

    private static int executar(Connection c, String sql) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------------- (a) PROVA CENTRAL via HTTP

    private String versao(String tabela, String id) {
        String v = escalarSuper("select version::text from " + tabela + " where id = ?", id);
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                LocalDateTime.parse(v.replace(' ', 'T')).truncatedTo(ChronoUnit.SECONDS));
    }

    private Map<String, Object> ref(String tabela, String id) {
        return Map.of("id", id, "version", versao(tabela, id));
    }

    private Map<String, Object> corpoServico(String nome, String empresaId) {
        return Map.of("nome", nome, "ativo", true, "issRetido", false, "tipoOperacaoIss", 1,
                "empresa", ref("empresa", empresaId));
    }

    private Map<String, Object> corpoPessoa(String nome, String empresaId) {
        return Map.of("tipoPessoa", "fisica", "nome", nome, "empresa", ref("empresa", empresaId));
    }

    private Map<String, Object> corpoContato(String nome, String pessoaId, String empresaId) {
        return Map.of("tipoContato", "email", "nome", nome, "email", "x@teste.local",
                "dataCriacao", "2026-01-01T00:00:00", "dataAtualizacao", "2026-01-01T00:00:00",
                "pessoa", ref("pessoa", pessoaId), "empresa", ref("empresa", empresaId));
    }

    @Test
    void postDePessoaServicoEContatoNaEmpresaDeOutroTenantFalhaENadaEhCriadoNaB() {
        // controles: o MESMO corpo apontando para a propria empresa funciona (logo, a falha abaixo e a RLS, nao o corpo)
        assertEquals(200, chamar("POST", "/api/Pessoa", tokenA, corpoPessoa("Controle A", empA)).statusCode());
        assertEquals(200, chamar("POST", "/api/Servico", tokenA, corpoServico("Controle A", empA)).statusCode());

        long pessoasB = contarSuper("select count(*) from pessoa where empresa_id = ?", empB);
        long servicosB = contarSuper("select count(*) from servico where empresa_id = ?", empB);
        long contatosB = contarSuper("select count(*) from contato where empresa_id = ?", empB);

        Response rp = chamar("POST", "/api/Pessoa", tokenA, corpoPessoa("INJETADA", empB));
        Response rs = chamar("POST", "/api/Servico", tokenA, corpoServico("INJETADO", empB));
        Response rc = chamar("POST", "/api/Contato", tokenA, corpoContato("INJETADO", pesB, empB));
        // Contato: a escrita direta e negada pela politica de acesso (403); a RLS de contato segue provada nos testes de SQL direto
        assertEquals(403, rc.statusCode(), rc.asString());
        for (Response r : List.of(rp, rs)) {
            assertEquals(403, r.statusCode(), r.asString());
            // a camada Java gerada barra primeiro (acesso_negado); a RLS e a segunda camada, provada nos testes de SQL direto
            assertEquals("acesso_negado", r.jsonPath().getString("codigo"));
            assertFalse(r.asString().toLowerCase().contains("row-level") || r.asString().toLowerCase().contains("policy"),
                    "a resposta nao pode vazar detalhe do banco: " + r.asString());
        }
        assertEquals(pessoasB, contarSuper("select count(*) from pessoa where empresa_id = ?", empB));
        assertEquals(servicosB, contarSuper("select count(*) from servico where empresa_id = ?", empB));
        assertEquals(contatosB, contarSuper("select count(*) from contato where empresa_id = ?", empB));
        assertEquals(0, contarSuper("select count(*) from pessoa where nome = 'INJETADA'"));
        assertEquals(0, contarSuper("select count(*) from servico where nome = 'INJETADO'"));
        assertEquals(0, contarSuper("select count(*) from contato where nome = 'INJETADO'"));
        // o contrario (B para A) tambem
        assertEquals(403, chamar("POST", "/api/Pessoa", tokenB, corpoPessoa("INJETADA2", empA)).statusCode());
        assertEquals(0, contarSuper("select count(*) from pessoa where nome = 'INJETADA2'"));
    }

    @Test
    void putNaoMoveRegistroParaOutraEmpresaEmPessoaServicoEContato() {
        String p = dados.pessoa(empA, "Vai mudar");
        String s = dados.servico(empA, "Vai mudar");
        String c = dados.contato(empA, pesA, "Vai mudar");

        // (o PUT gerado de Pessoa/Servico/Contato tambem copia a empresa aninhada do corpo; ver README, achados)
        Response rp = chamar("PUT", "/api/Pessoa/id/" + p, tokenA,
                Map.of("tipoPessoa", "fisica", "nome", "Movida", "empresa", ref("empresa", empB)));
        Response rs = chamar("PUT", "/api/Servico/id/" + s, tokenA, corpoServico("Movido", empB));
        Response rc = chamar("PUT", "/api/Contato/id/" + c, tokenA, corpoContato("Movido", pesA, empB));
        for (Response r : List.of(rp, rs, rc)) {
            assertTrue(r.statusCode() >= 400, "o PUT que troca a empresa tem de falhar; veio " + r.statusCode() + " " + r.asString());
        }
        assertEquals(empA, escalarSuper("select empresa_id from pessoa where id = ?", p));
        assertEquals(empA, escalarSuper("select empresa_id from servico where id = ?", s));
        assertEquals(empA, escalarSuper("select empresa_id from contato where id = ?", c));
    }

    // ---------------------------------------------------------------------- (c) nao vaza entre requisicoes seguidas

    private List<String> idsDe(String entidade, String tk) {
        return comToken(tk).get("/api/" + entidade).then().statusCode(200).extract().jsonPath().getList("id");
    }

    @Test
    void naoVazaEntreRequisicoesSeguidas() {
        for (int i = 0; i < 6; i++) {
            List<String> a = idsDe("Pessoa", tokenA);
            assertTrue(a.contains(pesA) && !a.contains(pesB), "A so ve A");
            List<String> b = idsDe("Pessoa", tokenB);
            assertTrue(b.contains(pesB) && !b.contains(pesA), "B so ve B");
            // uma chamada publica que roda como SISTEMA (login) entre elas nao pode deixar escopo para a seguinte
            assertEquals(401, login("naoexiste" + i + "@teste.local", "qualquer-senha-123").statusCode());
            List<String> a2 = idsDe("Servico", tokenA);
            assertTrue(a2.contains(srvA) && !a2.contains(srvB), "A so ve A depois do login");
            // sem token: nunca chega ao banco
            assertEquals(401, status("GET", "/api/Pessoa", null, null));
            List<String> b2 = idsDe("Nota", tokenB);
            assertTrue(b2.contains(notB) && !b2.contains(notA));
            // o admin da plataforma entre elas tambem nao deixa rastro
            assertTrue(idsDe("Pessoa", tokenPlat).containsAll(List.of(pesA, pesB)));
            List<String> a3 = idsDe("Pessoa", tokenA);
            assertFalse(a3.contains(pesB), "A nao herda o irrestrito do admin");
        }
    }

    @Test
    void requisicoesConcorrentesDeEmpresasDiferentesNaoSeMisturam() throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> fs = new ArrayList<>();
            for (int i = 0; i < 80; i++) {
                final boolean daA = i % 2 == 0;
                fs.add(ex.submit(() -> {
                    List<String> ids = idsDe("Pessoa", daA ? tokenA : tokenB);
                    return daA ? ids.contains(pesA) && !ids.contains(pesB) : ids.contains(pesB) && !ids.contains(pesA);
                }));
            }
            for (Future<Boolean> f : fs) {
                assertTrue(f.get(), "uma requisicao viu dado da outra empresa (ou nao viu o proprio)");
            }
        } finally {
            ex.shutdownNow();
        }
    }

    @Test
    void conexaoReutilizadaDoPoolNaoHerdaOEscopoAnterior() throws Exception {
        // fora de transacao (autocommit) e dentro dela, na MESMA conexao fisica (mesmo backend pid)
        String[] sobA = contexto.comoEmpresa(empA, this::lerEscopo);
        assertEquals(empA, sobA[0]);
        assertEquals("false", sobA[1]);
        String[] sobB = contexto.comoEmpresa(empB, this::lerEscopo);
        assertEquals(empB, sobB[0]);
        String[] sobSistema = contexto.comoSistema(this::lerEscopo);
        assertEquals("true", sobSistema[1]);
        String[] sobA2 = contexto.comoEmpresa(empA, this::lerEscopo);
        assertEquals("false", sobA2[1], "o irrestrito do sistema nao pode sobrar");
        assertEquals(sobA[2], sobB[2], "as leituras sequenciais devem ter reutilizado a mesma conexao fisica (pid)");
        assertEquals(sobA[2], sobSistema[2]);

        // dentro de transacao: is_local, some no commit
        String[] dentro = contexto.comoEmpresa(empB, () -> QuarkusTransaction.requiringNew().call(this::lerEscopo));
        assertEquals(empB, dentro[0]);
        assertEquals("false", dentro[1]);
        String[] depois = contexto.comoEmpresa(empA, this::lerEscopo);
        assertEquals(empA, depois[0]);
    }

    @Test
    void blocoExplicitoDentroDeTransacaoAtivaEhRecusado() {
        assertThrows(IllegalStateException.class, () -> QuarkusTransaction.requiringNew()
                .run(() -> contexto.comoSistema(() -> "x")));
    }

    /** {empresa_id, irrestrito, pid}: o que o BANCO enxerga na conexao entregue pelo pool. */
    private String[] lerEscopo() {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement("select current_setting('app.empresa_id', true), "
                        + "current_setting('app.irrestrito', true), pg_backend_pid()::text");
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return new String[] {rs.getString(1), rs.getString(2), rs.getString(3)};
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void foraDeRequisicaoRodaComoSistemaControlado() {
        // dados de teste, bootstrap e startup nao tem requisicao HTTP: enxergam tudo (sistema)
        String[] e = lerEscopo();
        assertEquals("true", e[1]);
    }

    // ----------------------------------------------------------------- (e) administrador da plataforma continua

    @Test
    void adminDaPlataformaCriaMunicipioEEmpresaEVeTudo() {
        String ibge = String.valueOf(1000000 + (int) (Math.random() * 8999999));
        Response m = chamar("POST", "/api/Municipio", tokenPlat,
                Map.of("codigoIbge", ibge, "nome", "Cidade RLS " + UUID.randomUUID(), "uf", "SP"));
        assertEquals(200, m.statusCode(), m.asString());
        String municipioId = m.jsonPath().getString("id");
        assertEquals(1, contarSuper("select count(*) from municipio where id = ?", municipioId));

        String doc = String.valueOf(10000000000000L + (long) (Math.random() * 89999999999999L));
        Response e = chamar("POST", "/api/Empresa", tokenPlat, Map.of("razaoSocial", "Empresa criada pelo admin",
                "nomeFantasia", "Admin", "documento", doc, "ambiente", "homologacao", "regimeTributario", 1,
                "situacao", "ativa", "municipio", ref("municipio", municipioId)));
        assertEquals(200, e.statusCode(), e.asString());
        String novaEmpresa = e.jsonPath().getString("id");
        assertEquals(1, contarSuper("select count(*) from empresa where id = ?", novaEmpresa));

        // operador comum nao cria Empresa nem Municipio (Java) e, mesmo que criasse, a RLS impediria
        assertEquals(403, status("POST", "/api/Empresa", tokenA, Map.of("razaoSocial", "x")));

        // admin ve tudo, de varias empresas, e grava em qualquer uma
        List<String> pessoas = idsDe("Pessoa", tokenPlat);
        assertTrue(pessoas.contains(pesA) && pessoas.contains(pesB));
        Response gravada = chamar("POST", "/api/Pessoa", tokenPlat, corpoPessoa("Pelo admin na B", empB));
        assertEquals(200, gravada.statusCode(), gravada.asString());
        assertEquals(empB, escalarSuper("select empresa_id from pessoa where id = ?", gravada.jsonPath().getString("id")));
        comToken(tokenPlat).get("/api/Empresa/id/" + novaEmpresa).then().statusCode(200);
    }

    @Test
    void authContinuaFuncionandoComRlsLoginRefreshSenhaLogout() {
        var u = dados.usuarioComPapel(empA, "operador");
        Response l = login(u.email(), SENHA);
        assertEquals(200, l.statusCode(), l.asString());
        Response r = refresh(refreshCookie(l));
        assertEquals(200, r.statusCode(), r.asString());
        assertEquals(204, logout(refreshCookie(r)).statusCode());
        assertEquals(200, comToken(r.jsonPath().getString("accessToken")).get("/api/auth/me").statusCode());
    }
}
