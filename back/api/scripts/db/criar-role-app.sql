-- Cria o role de APLICACAO (runtime) do emitesimples. Rode UMA vez, como superusuario/administrador do banco,
-- ANTES da primeira subida (e antes ou depois do Flyway: a migracao V3 concede o DML se o role ja existir).
--
--   export EMITESIMPLES_DB_APP_PASSWORD='...'      # a senha vem do ambiente; nao fica neste arquivo nem em log
--   psql -v ON_ERROR_STOP=1 -d emitesimples -f scripts/db/criar-role-app.sql      # psql 15 ou superior (\getenv)
--
-- O role NAO e dono das tabelas, NAO e superusuario e NAO tem BYPASSRLS: por isso a RLS da V3 vale para ele.
-- Se o role ja existia e a V3 ja rodou, rode tambem os GRANTs abaixo.
\getenv app_senha EMITESIMPLES_DB_APP_PASSWORD
\if :{?app_senha}
\else
  \echo 'Defina EMITESIMPLES_DB_APP_PASSWORD'
  \quit
\endif

SELECT NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') AS criar \gset
\if :criar
  CREATE ROLE emitesimples_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS NOINHERIT;
\endif
ALTER ROLE emitesimples_app PASSWORD :'app_senha';

GRANT USAGE ON SCHEMA public TO emitesimples_app;
-- Grants nas tabelas existentes (a V3 faz o mesmo se o role existir quando ela roda):
DO $$
DECLARE t text;
BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history' LOOP
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO emitesimples_app', t);
    END LOOP;
END $$;
