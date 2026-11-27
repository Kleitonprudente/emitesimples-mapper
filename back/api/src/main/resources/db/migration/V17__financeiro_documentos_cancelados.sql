-- V17: modulo financeiro, FASE 5 (titulos gerados pela NFS-e). docs/financeiro/README.md, secao 6 (E3, E4, F5-H1 e F5-H2).
-- Tabela TECNICA, fora do mapa (como o segredo da V4): a marca de que o documento foi cancelado, para a ORDEM dos eventos nao importar
-- (cancelamento que chega antes da geracao impede o titulo). Escrita so pelo financeiroop.TitulosDosDocumentos, por SQL nativo.
create table DOCUMENTOCANCELADO (
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    ORIGEM_TIPO varchar(10) not null,
    ID_ORIGEM varchar(36) not null,
    CANCELADO_EM timestamp(6) not null,
    primary key (EMPRESA_ID, ORIGEM_TIPO, ID_ORIGEM),
    constraint ck_documentocancelado_origem check (ORIGEM_TIPO in ('NFSE'))
);

ALTER TABLE documentocancelado ENABLE ROW LEVEL SECURITY;
ALTER TABLE documentocancelado FORCE ROW LEVEL SECURITY;
CREATE POLICY isolamento_empresa ON documentocancelado
    USING (empresa_id = current_setting('app.empresa_id', true) OR current_setting('app.irrestrito', true) = 'true')
    WITH CHECK (empresa_id = current_setting('app.empresa_id', true) OR current_setting('app.irrestrito', true) = 'true');

DO $grants$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        EXECUTE format('GRANT SELECT, INSERT ON %I.documentocancelado TO emitesimples_app', current_schema());
    END IF;
END
$grants$;
