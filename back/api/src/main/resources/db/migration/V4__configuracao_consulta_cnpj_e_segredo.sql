-- V4: configuracao da consulta de CNPJ (CNPJ.ws) editavel pelo administrador da plataforma e cofre de segredos.
-- SEGREDO: valor cifrado (AES-256-GCM, feito na aplicacao); nao ha coluna de texto puro. Dado de PLATAFORMA: sem
-- EMPRESA_ID e sem RLS; o acesso e restrito pelo caminho (/api/admin/*, so plataforma_admin) e nenhum recurso gerado o expoe.
create table segredo (
    id varchar(36) not null primary key,
    valor_cifrado bytea not null,
    iv bytea not null,
    data_criacao timestamp(6) not null,
    data_atualizacao timestamp(6) not null
);

-- Singleton: uma unica linha, id fixo 'unica'. A referencia segredo_token_id permite trocar por cofre externo depois.
create table configuracao_consulta_cnpj (
    id varchar(36) not null primary key,
    url_base varchar(200) not null,
    segredo_token_id varchar(36) null references segredo (id),
    data_atualizacao timestamp(6) not null,
    atualizado_por varchar(36) null references usuario (id),
    constraint configuracao_consulta_cnpj_unica check (id = 'unica')
);

insert into configuracao_consulta_cnpj (id, url_base, data_atualizacao)
values ('unica', 'https://comercial.cnpj.ws/cnpj', now());

-- Permissoes do role de aplicacao (mesmo padrao da V3: so se o role existir)
DO $grants$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.segredo TO emitesimples_app', current_schema());
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.configuracao_consulta_cnpj TO emitesimples_app', current_schema());
    END IF;
END
$grants$;
