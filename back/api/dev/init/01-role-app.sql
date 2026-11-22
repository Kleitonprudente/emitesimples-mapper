-- Roda so na primeira criacao do volume. Role de APLICACAO (runtime): sem superuser e sem BYPASSRLS, entao a RLS vale.
-- O Flyway roda como dono (emitesimples) e a V3 concede o DML a este role porque ele ja existe.
CREATE ROLE emitesimples_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS NOINHERIT PASSWORD 'dev-app';
GRANT USAGE ON SCHEMA public TO emitesimples_app;
