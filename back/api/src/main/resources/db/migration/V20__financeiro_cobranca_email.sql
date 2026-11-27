-- V20: modulo financeiro, FASE 6 (cobranca por e-mail e lembretes). docs/financeiro/README.md, 9.3; referencia §9.1.
-- LEMBRETECOBRANCA: cada lembrete sai uma vez so (indice unico titulo, tipo, dias).
create table LEMBRETECOBRANCA (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    TITULO_ID varchar(36) not null references TITULO (ID),
    TIPO varchar(10) not null,
    DIAS integer not null,
    ENVIADO_EM timestamp(6) not null,
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_lembretecobranca_tipo check (TIPO in ('ANTES', 'DEPOIS')),
    constraint ck_lembretecobranca_dias check (DIAS between 0 and 99)
);
create unique index ux_lembretecobranca on LEMBRETECOBRANCA (TITULO_ID, TIPO, DIAS);

-- COBRANCAENVIADA: no maximo uma cobranca por cliente por dia (o envio reserva o dia antes de mandar; se falhar, apaga a reserva).
create table COBRANCAENVIADA (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    TITULO_ID varchar(36) not null references TITULO (ID),
    ID_PESSOA varchar(36) not null,
    ORIGEM varchar(10) not null,
    DIA date not null,
    EMAIL varchar(255) not null,
    ENVIADO_EM timestamp(6) not null,
    ID_ENVIADO_POR varchar(36),
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_cobrancaenviada_origem check (ORIGEM in ('MANUAL', 'LEMBRETE'))
);
create unique index ux_cobrancaenviada_dia on COBRANCAENVIADA (EMPRESA_ID, ID_PESSOA, DIA);
create index ix_cobrancaenviada_titulo on COBRANCAENVIADA (TITULO_ID);

DO $rls$
DECLARE
    t text;
    teste text := 'current_setting(''app.irrestrito'', true) = ''true''';
BEGIN
    FOREACH t IN ARRAY ARRAY['lembretecobranca', 'cobrancaenviada'] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format(
            'CREATE POLICY isolamento_empresa ON %I USING (empresa_id = current_setting(''app.empresa_id'', true) OR %s) '
            || 'WITH CHECK (empresa_id = current_setting(''app.empresa_id'', true) OR %s)', t, teste, teste);
    END LOOP;
END
$rls$;

DO $grants$
DECLARE
    t text;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        FOREACH t IN ARRAY ARRAY['lembretecobranca', 'cobrancaenviada'] LOOP
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.%I TO emitesimples_app', current_schema(), t);
        END LOOP;
    END IF;
END
$grants$;
