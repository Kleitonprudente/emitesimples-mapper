-- V21: modulo financeiro, FASE 7 (cobranca por gateway: boleto, Pix, link). docs/financeiro/README.md, 9.5; referencia §9.2.
-- CREDENCIALCOBRANCA: uma por conta; token e segredo do webhook ficam no cofre de segredos (so a referencia aqui).
create table CREDENCIALCOBRANCA (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    CONTA_ID varchar(36) not null references CONTAFINANCEIRA (ID),
    PROVEDOR varchar(20) not null,
    ENDPOINT varchar(255),
    SELLER_ID varchar(100) not null,
    REFERENCIA_TOKEN varchar(36),
    REFERENCIA_SEGREDO_WEBHOOK varchar(36),
    ATIVO boolean not null default true,
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_credencialcobranca_provedor check (PROVEDOR in ('FALSO')),
    constraint ck_credencialcobranca_endpoint check (ENDPOINT is null or ENDPOINT ~ '^https://')
);
create unique index ux_credencialcobranca_conta on CREDENCIALCOBRANCA (CONTA_ID);
-- unico no sistema inteiro (a tabela tem RLS, o indice nao): o dinheiro nunca cai na conta errada
create unique index ux_credencialcobranca_seller on CREDENCIALCOBRANCA (PROVEDOR, SELLER_ID);

-- COBRANCADESTINO: GLOBAL (sem RLS) para achar a empresa quando o webhook chega sem sessao. So o hash do segredo.
create table COBRANCADESTINO (
    ID varchar(36) not null primary key,
    CREDENCIAL_ID varchar(36) not null references CREDENCIALCOBRANCA (ID),
    ID_EMPRESA varchar(36) not null,
    SEGREDO_HASH varchar(64) not null,
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6)
);
create unique index ux_cobrancadestino_hash on COBRANCADESTINO (SEGREDO_HASH);
create unique index ux_cobrancadestino_credencial on COBRANCADESTINO (CREDENCIAL_ID);

-- COBRANCA: uma viva por titulo (indice parcial); estado so muda pelo sincronizador depois de criada.
create table COBRANCA (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    TITULO_ID varchar(36) not null references TITULO (ID),
    CONTA_ID varchar(36) not null references CONTAFINANCEIRA (ID),
    PROVEDOR varchar(20) not null,
    TIPO varchar(10) not null,
    ESTADO varchar(20) not null default 'CRIANDO',
    VALOR numeric(15,2) not null,
    VENCIMENTO date not null,
    MULTA_PERCENTUAL numeric(5,2),
    JUROS_MENSAL_PERCENTUAL numeric(5,2),
    ID_EXTERNO varchar(100),
    LINHA_DIGITAVEL varchar(60),
    CODIGO_DE_BARRAS varchar(60),
    URL_DO_BOLETO varchar(500),
    PIX_COPIA_E_COLA varchar(500),
    URL_DO_PIX varchar(500),
    URL_DO_LINK varchar(500),
    PAGO_EM timestamp(6),
    VALOR_PAGO numeric(15,2),
    MEIO_PAGO varchar(10),
    TARIFA numeric(15,2),
    ID_BAIXA_MOVIMENTO varchar(36),
    ID_TARIFA_MOVIMENTO varchar(36),
    MOTIVO varchar(200),
    RESPOSTA_DO_PROVEDOR varchar(200),
    ULTIMA_CONSULTA_EM timestamp(6),
    ID_CRIADO_POR varchar(36),
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_cobranca_tipo check (TIPO in ('BOLETO', 'PIX', 'LINK')),
    constraint ck_cobranca_estado check (ESTADO in ('CRIANDO', 'AGUARDANDO', 'INCERTA', 'PAGA', 'PAGA_PARA_REVISAR', 'RECUSADA', 'CANCELADA',
        'EXPIRADA', 'DESCARTADA')),
    constraint ck_cobranca_valor check (VALOR > 0 and VALOR <= 999999999.99),
    constraint ck_cobranca_tarifa check (TARIFA is null or TARIFA >= 0)
);
create unique index ux_cobranca_viva on COBRANCA (TITULO_ID) where ESTADO in ('CRIANDO', 'AGUARDANDO', 'INCERTA');
create unique index ux_cobranca_externo on COBRANCA (EMPRESA_ID, PROVEDOR, ID_EXTERNO) where ID_EXTERNO is not null;
create index ix_cobranca_varredura on COBRANCA (ESTADO, TIPO, ULTIMA_CONSULTA_EM);

DO $rls$
DECLARE
    t text;
    teste text := 'current_setting(''app.irrestrito'', true) = ''true''';
BEGIN
    FOREACH t IN ARRAY ARRAY['credencialcobranca', 'cobranca'] LOOP
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
        FOREACH t IN ARRAY ARRAY['credencialcobranca', 'cobrancadestino', 'cobranca'] LOOP
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.%I TO emitesimples_app', current_schema(), t);
        END LOOP;
    END IF;
END
$grants$;
