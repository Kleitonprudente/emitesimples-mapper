-- Executado pelo Dev Services ANTES do Flyway, como o usuario dono/superusuario do container de TESTE.
-- Cria o role de runtime SEM privilegios especiais, para a RLS ser provada de verdade (superusuario ignora RLS).
-- A senha abaixo so existe no container efemero dos testes.
CREATE ROLE emitesimples_app LOGIN PASSWORD 'app-teste-efemero' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
