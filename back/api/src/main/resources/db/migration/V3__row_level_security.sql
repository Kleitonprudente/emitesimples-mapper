-- V3: Row Level Security (RLS) como SEGUNDA camada de isolamento entre empresas (multi-tenant).
-- A primeira camada e o filtro do repositorio gerado (EscopoTenant). Esta camada vale mesmo se a Java falhar
-- (ex.: POST gerado de Pessoa/Servico/Contato que grava a empresa vinda no corpo).
--
-- Regra (USING e WITH CHECK): a linha e visivel/gravavel se
--     empresa_id = current_setting('app.empresa_id', true)      -- a aplicacao seta por transacao, a partir do JWT
--  OU current_setting('app.irrestrito', true) = 'true'           -- administrador da plataforma / codigo de sistema
-- Sem configuracao, current_setting(..., true) devolve NULL (ou '') e nenhuma linha passa: FALHA FECHADA.
-- Comparacao como TEXTO (as colunas sao varchar(36)): nunca ha erro de cast.
--
-- Globais SEM RLS (de proposito): USUARIO, MUNICIPIO, PLANO, IMPORTACAOMUNICIPIO (e flyway_schema_history).
-- Tabelas novas com EMPRESA_ID em migracoes futuras PRECISAM entrar aqui (ver README, secao RLS).
--
-- ATENCAO: superusuario e roles com BYPASSRLS ignoram RLS. FORCE cobre o DONO da tabela, mas nao o superusuario.
-- Por isso a aplicacao, em runtime, deve conectar com um role comum (ver scripts/db/criar-role-app.sql).

DO $rls$
DECLARE
    t text;
    tabelas_empresa text[] := ARRAY[
        'assinaturaempresa', 'atividadeempresa', 'cancelamento', 'chamadagateway', 'clienteacesso',
        'confirmacaocadastro', 'contato', 'eventowebhook', 'faturacrm', 'historicosituacaoempresa', 'nota',
        'pessoa', 'qsaempresa', 'servico', 'tokenrenovacao', 'vinculousuario'];
    teste text := 'current_setting(''app.irrestrito'', true) = ''true''';
BEGIN
    FOREACH t IN ARRAY tabelas_empresa LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format(
            'CREATE POLICY isolamento_empresa ON %I USING (empresa_id = current_setting(''app.empresa_id'', true) OR %s) '
            || 'WITH CHECK (empresa_id = current_setting(''app.empresa_id'', true) OR %s)', t, teste, teste);
    END LOOP;

    -- EMPRESA: a coluna que identifica o tenant e o proprio ID.
    ALTER TABLE empresa ENABLE ROW LEVEL SECURITY;
    ALTER TABLE empresa FORCE ROW LEVEL SECURITY;
    EXECUTE format(
        'CREATE POLICY isolamento_empresa ON empresa USING (id = current_setting(''app.empresa_id'', true) OR %s) '
        || 'WITH CHECK (id = current_setting(''app.empresa_id'', true) OR %s)', teste, teste);
END
$rls$;

-- Permissoes do role de aplicacao (criado FORA do Flyway: scripts/db/criar-role-app.sql; nos testes, pelo init script
-- do Dev Services). Minimo: DML nas tabelas do esquema, nunca DDL, nunca a tabela de historico do Flyway.
-- Se o role ainda nao existe, a migracao segue sem os GRANTs (o DBA os aplica ao criar o role).
DO $grants$
DECLARE
    t text;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        EXECUTE format('GRANT USAGE ON SCHEMA %I TO emitesimples_app', current_schema());
        FOR t IN SELECT tablename FROM pg_tables
                 WHERE schemaname = current_schema() AND tablename <> 'flyway_schema_history' LOOP
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.%I TO emitesimples_app', current_schema(), t);
        END LOOP;
        -- tabelas criadas por migracoes futuras (pelo mesmo dono) ja nascem com DML para o role
        EXECUTE format('ALTER DEFAULT PRIVILEGES IN SCHEMA %I GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO emitesimples_app',
                       current_schema());
    END IF;
END
$grants$;
