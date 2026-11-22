-- V6: NFS-e (habilitacao e numeracao por empresa), percentual de tributos do Simples no servico e configuracao do
-- IntegraNotas (singleton de PLATAFORMA: sem EMPRESA_ID e sem RLS; acesso restrito por /api/admin/*, so plataforma_admin).
-- RLS (V3) e GRANTs (V3, por tabela) de EMPRESA e SERVICO ja cobrem a tabela inteira: coluna nova nao exige nada aqui.
ALTER TABLE EMPRESA
    ADD COLUMN NFSE_HABILITADA boolean NOT NULL DEFAULT false,
    ADD COLUMN SERIE_NFSE varchar(5),
    ADD COLUMN ULTIMO_NUMERO_NFSE integer NOT NULL DEFAULT 0;

ALTER TABLE SERVICO
    ADD COLUMN PERCENTUAL_TRIBUTOS_SIMPLES numeric(5,2);

-- Singleton: uma unica linha, id fixo 'unica'. Os tokens da softhouse (um por ambiente) ficam no cofre (segredo), cifrados.
create table configuracao_integranotas (
    id varchar(36) not null primary key,
    ambiente_ativo varchar(15) not null default 'homologacao',
    url_homologacao varchar(200) not null default 'https://hom-api.integranotas.com.br',
    url_producao varchar(200) not null default 'https://api.integranotas.com.br',
    url_publica_webhook varchar(200) null,
    segredo_softhouse_hom_id varchar(36) null references segredo (id),
    segredo_softhouse_prod_id varchar(36) null references segredo (id),
    data_atualizacao timestamp(6) not null,
    atualizado_por varchar(36) null references usuario (id),
    constraint configuracao_integranotas_unica check (id = 'unica'),
    constraint configuracao_integranotas_ambiente check (ambiente_ativo in ('homologacao', 'producao'))
);

insert into configuracao_integranotas (id, data_atualizacao) values ('unica', now());

-- Permissoes do role de aplicacao (mesmo padrao da V4: so se o role existir)
DO $grants$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.configuracao_integranotas TO emitesimples_app', current_schema());
    END IF;
END
$grants$;
