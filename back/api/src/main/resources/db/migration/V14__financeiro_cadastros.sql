-- V14: modulo financeiro, FASE 1 (cadastros). Regras em docs/financeiro/README.md (F1-H1 a F1-H5) e no mapa back/mapas/Financeiro.
-- As tabelas seguem o que a entidade gerada espera (Hibernate em modo validate): tabela = nome da classe, colunas = cn em
-- maiusculas, VERSION do lock otimista. O que o mapa NAO expressa (CHECK, unicidade, FK por codigo, regras entre linhas) mora aqui.
--
-- Erros de regra: cada CHECK, indice unico e trigger tem um NOME que o ErrosHandler traduz para a mensagem em portugues
-- (financeiroop/RestricoesFinanceiro). Trigger levanta SQLSTATE 23514 com o nome da regra na mensagem. Mudou o nome aqui,
-- mude la.

-- ---------------------------------------------------------------------------------------------------------------
-- BANCO: dado de referencia GLOBAL (sem EMPRESA_ID e sem RLS), como MUNICIPIO. Codigo do Banco Central sem zeros a esquerda.
-- ---------------------------------------------------------------------------------------------------------------
create table BANCO (
    ID varchar(36) not null primary key,
    CODIGO varchar(4) not null,
    ISPB varchar(8),
    NOME varchar(120) not null,
    NOME_EXTENSO varchar(200),
    PARTICIPA_COMPE boolean not null default false,
    INTEGRADO_GATEWAY boolean not null default false,
    URL_PASSO_A_PASSO varchar(500),
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_banco_codigo check (CODIGO ~ '^[1-9][0-9]{0,3}$'),
    constraint ck_banco_url check (URL_PASSO_A_PASSO is null or URL_PASSO_A_PASSO ~ '^https://')
);
create unique index ux_banco_codigo on BANCO (CODIGO);

-- ---------------------------------------------------------------------------------------------------------------
-- CONTAFINANCEIRA (F1-H1)
-- ---------------------------------------------------------------------------------------------------------------
create table CONTAFINANCEIRA (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    NOME varchar(100) not null,
    TIPO varchar(10) not null,
    ATIVO boolean not null default true,
    CODIGO_BANCO varchar(4),
    AGENCIA varchar(10),
    DADOS_DO_BANCO varchar(150),
    SALDO_INICIAL numeric(15,2) not null default 0,
    DATA_SALDO_INICIAL date not null,
    PADRAO_DINHEIRO boolean not null default false,
    PADRAO_PIX boolean not null default false,
    PADRAO_BOLETO boolean not null default false,
    PADRAO_CARTAO boolean not null default false,
    PADRAO_TRANSFERENCIA boolean not null default false,
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_contafinanceira_tipo check (TIPO in ('CAIXA', 'BANCO', 'CARTEIRA')),
    constraint ck_contafinanceira_agencia check (AGENCIA is null or AGENCIA ~ '^[0-9A-Za-z-]+$'),
    constraint ck_contafinanceira_saldo_teto check (SALDO_INICIAL between -999999999.99 and 999999999.99),
    constraint fk_contafinanceira_banco foreign key (CODIGO_BANCO) references BANCO (CODIGO)
);
create unique index ux_contafinanceira_nome on CONTAFINANCEIRA (EMPRESA_ID, NOME);
-- no maximo uma conta padrao por forma (a trigger abaixo desmarca a anterior antes; o indice e a rede de seguranca)
create unique index ux_contafinanceira_padrao_dinheiro on CONTAFINANCEIRA (EMPRESA_ID) where PADRAO_DINHEIRO;
create unique index ux_contafinanceira_padrao_pix on CONTAFINANCEIRA (EMPRESA_ID) where PADRAO_PIX;
create unique index ux_contafinanceira_padrao_boleto on CONTAFINANCEIRA (EMPRESA_ID) where PADRAO_BOLETO;
create unique index ux_contafinanceira_padrao_cartao on CONTAFINANCEIRA (EMPRESA_ID) where PADRAO_CARTAO;
create unique index ux_contafinanceira_padrao_transferencia on CONTAFINANCEIRA (EMPRESA_ID) where PADRAO_TRANSFERENCIA;
create index ix_contafinanceira_banco on CONTAFINANCEIRA (CODIGO_BANCO);

-- Ao marcar uma conta como padrao de uma forma, desmarca a anterior da mesma empresa NA MESMA TRANSACAO.
-- So no nivel 1 (o UPDATE das outras contas dispara esta trigger de novo; ali nao ha o que desmarcar).
create function fin_conta_desmarca_padrao() returns trigger language plpgsql as $$
begin
    if pg_trigger_depth() > 1 then
        return new;
    end if;
    if new.padrao_dinheiro then
        update contafinanceira set padrao_dinheiro = false where empresa_id = new.empresa_id and id <> new.id and padrao_dinheiro;
    end if;
    if new.padrao_pix then
        update contafinanceira set padrao_pix = false where empresa_id = new.empresa_id and id <> new.id and padrao_pix;
    end if;
    if new.padrao_boleto then
        update contafinanceira set padrao_boleto = false where empresa_id = new.empresa_id and id <> new.id and padrao_boleto;
    end if;
    if new.padrao_cartao then
        update contafinanceira set padrao_cartao = false where empresa_id = new.empresa_id and id <> new.id and padrao_cartao;
    end if;
    if new.padrao_transferencia then
        update contafinanceira set padrao_transferencia = false where empresa_id = new.empresa_id and id <> new.id and padrao_transferencia;
    end if;
    return new;
end
$$;
create trigger tg_contafinanceira_padrao before insert or update on CONTAFINANCEIRA
    for each row execute function fin_conta_desmarca_padrao();
-- FASE 3: a trava de SALDO_INICIAL e DATA_SALDO_INICIAL de conta com movimentos entra em uma trigger nova, junto da tabela MOVIMENTO.

-- ---------------------------------------------------------------------------------------------------------------
-- CATEGORIAFINANCEIRA (F1-H2): hierarquia de um nivel
-- ---------------------------------------------------------------------------------------------------------------
create table CATEGORIAFINANCEIRA (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    NOME varchar(100) not null,
    TIPO varchar(10) not null,
    DICAS varchar(500),
    ATIVO boolean not null default true,
    CATEGORIA_PAI_ID varchar(36) references CATEGORIAFINANCEIRA (ID) on delete restrict,
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_categoriafinanceira_tipo check (TIPO in ('RECEITA', 'DESPESA')),
    constraint ck_categoriafinanceira_pai_proprio check (CATEGORIA_PAI_ID is null or CATEGORIA_PAI_ID <> ID),
    constraint ck_categoriafinanceira_dicas check (DICAS is null or DICAS !~ '[<>]')
);
create unique index ux_categoriafinanceira_nome on CATEGORIAFINANCEIRA (EMPRESA_ID, TIPO, NOME);
create index ix_categoriafinanceira_pai on CATEGORIAFINANCEIRA (CATEGORIA_PAI_ID);

-- CONFIGURACAOFINANCEIRA vem antes da trigger da categoria, que a consulta ("categoria em uso").
create table CONFIGURACAOFINANCEIRA (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    MULTA_PERCENTUAL numeric(5,2) not null default 2.00,
    JUROS_MENSAL_PERCENTUAL numeric(5,2) not null default 1.00,
    CARENCIA_DIAS integer not null default 2,
    LEMBRETES_ATIVOS boolean not null default false,
    LEMBRETE_DIAS_ANTES integer not null default 3,
    LEMBRETE_DIAS_DEPOIS varchar(40) default '1,7,15',
    VENCIMENTO_PADRAO_DIAS integer not null default 0,
    OBSERVACAO_PADRAO_RECEBER varchar(255),
    FORMA_PREVISTA_PADRAO_RECEBER varchar(20),
    PADROES_CRIADOS_EM timestamp(6),
    CATEGORIA_PADRAO_RECEITA_ID varchar(36) references CATEGORIAFINANCEIRA (ID),
    CATEGORIA_PADRAO_DESPESA_ID varchar(36) references CATEGORIAFINANCEIRA (ID),
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_configuracaofinanceira_multa check (MULTA_PERCENTUAL between 0 and 999.99),
    constraint ck_configuracaofinanceira_juros check (JUROS_MENSAL_PERCENTUAL between 0 and 999.99),
    constraint ck_configuracaofinanceira_carencia check (CARENCIA_DIAS between 0 and 365),
    constraint ck_configuracaofinanceira_lembrete_antes check (LEMBRETE_DIAS_ANTES between 0 and 30),
    constraint ck_configuracaofinanceira_lembrete_depois check (
        LEMBRETE_DIAS_DEPOIS is null or LEMBRETE_DIAS_DEPOIS ~ '^([0-9]{1,2}([[:space:]]*,[[:space:]]*[0-9]{1,2}){0,4})?$'),
    constraint ck_configuracaofinanceira_vencimento check (VENCIMENTO_PADRAO_DIAS >= 0),
    constraint ck_configuracaofinanceira_forma check (
        FORMA_PREVISTA_PADRAO_RECEBER is null
        or FORMA_PREVISTA_PADRAO_RECEBER in ('DINHEIRO', 'PIX', 'BOLETO', 'CARTAO', 'TRANSFERENCIA'))
);
create unique index ux_configuracaofinanceira_empresa on CONFIGURACAOFINANCEIRA (EMPRESA_ID);
create index ix_configuracaofinanceira_cat_receita on CONFIGURACAOFINANCEIRA (CATEGORIA_PADRAO_RECEITA_ID);
create index ix_configuracaofinanceira_cat_despesa on CONFIGURACAOFINANCEIRA (CATEGORIA_PADRAO_DESPESA_ID);

-- Regras da hierarquia (F1-H2). FASES 2 e 3: "categoria em uso" passa a olhar tambem TITULO e MOVIMENTO
-- (CREATE OR REPLACE desta funcao numa migracao nova).
create or replace function fin_categoria_hierarquia() returns trigger language plpgsql as $$
declare
    pai record;
    tem_filhas boolean;
begin
    select exists (select 1 from categoriafinanceira f where f.categoria_pai_id = new.id) into tem_filhas;

    if new.categoria_pai_id is not null then
        select id, empresa_id, tipo, ativo, categoria_pai_id into pai from categoriafinanceira where id = new.categoria_pai_id;
        if not found or pai.empresa_id <> new.empresa_id then
            raise exception 'ck_categoria_pai_empresa' using errcode = '23514';
        end if;
        if pai.tipo <> new.tipo then
            raise exception 'ck_categoria_pai_tipo' using errcode = '23514';
        end if;
        if pai.categoria_pai_id is not null then
            raise exception 'ck_categoria_pai_nivel' using errcode = '23514';
        end if;
        if tem_filhas then
            raise exception 'ck_categoria_com_filhas_vira_filha' using errcode = '23514';
        end if;
        -- pai inativo: recusa vincular a ele e recusa ativar a filha sob ele (filha inativa sob pai inativo pode ser editada)
        if not pai.ativo and (new.ativo or tg_op = 'INSERT' or old.categoria_pai_id is distinct from new.categoria_pai_id) then
            raise exception 'ck_categoria_pai_inativo' using errcode = '23514';
        end if;
        -- so vira pai a categoria fora de uso (Fase 1: nao e categoria padrao da configuracao)
        if exists (select 1 from configuracaofinanceira c
                   where c.categoria_padrao_receita_id = pai.id or c.categoria_padrao_despesa_id = pai.id) then
            raise exception 'ck_categoria_pai_em_uso' using errcode = '23514';
        end if;
    end if;

    if tg_op = 'UPDATE' and tem_filhas then
        if old.tipo <> new.tipo then
            raise exception 'ck_categoria_tipo_com_filhas' using errcode = '23514';
        end if;
        if old.ativo and not new.ativo
           and exists (select 1 from categoriafinanceira f where f.categoria_pai_id = new.id and f.ativo) then
            raise exception 'ck_categoria_inativar_pai' using errcode = '23514';
        end if;
    end if;
    return new;
end
$$;
create trigger tg_categoriafinanceira_hierarquia before insert or update on CATEGORIAFINANCEIRA
    for each row execute function fin_categoria_hierarquia();

-- Categoria padrao da configuracao: da mesma empresa, ativa, do tipo certo e sem filhas (F1-H4).
create function fin_configuracao_categorias() returns trigger language plpgsql as $$
declare
    cat record;
begin
    if new.categoria_padrao_receita_id is not null then
        select id, empresa_id, tipo, ativo into cat from categoriafinanceira where id = new.categoria_padrao_receita_id;
        if not found or cat.empresa_id <> new.empresa_id or cat.tipo <> 'RECEITA' or not cat.ativo
           or exists (select 1 from categoriafinanceira f where f.categoria_pai_id = cat.id) then
            raise exception 'ck_configuracao_categoria_receita' using errcode = '23514';
        end if;
    end if;
    if new.categoria_padrao_despesa_id is not null then
        select id, empresa_id, tipo, ativo into cat from categoriafinanceira where id = new.categoria_padrao_despesa_id;
        if not found or cat.empresa_id <> new.empresa_id or cat.tipo <> 'DESPESA' or not cat.ativo
           or exists (select 1 from categoriafinanceira f where f.categoria_pai_id = cat.id) then
            raise exception 'ck_configuracao_categoria_despesa' using errcode = '23514';
        end if;
    end if;
    return new;
end
$$;
create trigger tg_configuracaofinanceira_categorias before insert or update on CONFIGURACAOFINANCEIRA
    for each row execute function fin_configuracao_categorias();

-- ---------------------------------------------------------------------------------------------------------------
-- CONDICAOPAGAMENTO (F1-H3)
-- ---------------------------------------------------------------------------------------------------------------
create table CONDICAOPAGAMENTO (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    NOME varchar(100) not null,
    FORMA varchar(20) not null,
    ATIVO boolean not null default true,
    PARCELAS integer not null default 1,
    DIAS_PRIMEIRO_VENCIMENTO integer not null default 0,
    INTERVALO_DIAS integer not null default 30,
    RECEBER_NA_EMISSAO boolean not null default false,
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_condicaopagamento_forma check (FORMA in ('DINHEIRO', 'PIX', 'BOLETO', 'CARTAO', 'TRANSFERENCIA')),
    constraint ck_condicaopagamento_parcelas check (PARCELAS between 1 and 48),
    constraint ck_condicaopagamento_intervalo check (INTERVALO_DIAS between 0 and 365),
    constraint ck_condicaopagamento_primeiro check (DIAS_PRIMEIRO_VENCIMENTO between 0 and 365),
    constraint ck_condicaopagamento_receber_na_emissao check (
        not RECEBER_NA_EMISSAO or (PARCELAS = 1 and DIAS_PRIMEIRO_VENCIMENTO = 0))
);
create unique index ux_condicaopagamento_nome on CONDICAOPAGAMENTO (EMPRESA_ID, NOME);

-- "Receber na emissao" exige conta padrao ATIVA da forma, na mesma empresa.
create function fin_condicao_conta_padrao() returns trigger language plpgsql as $$
begin
    if new.receber_na_emissao and not exists (
        select 1 from contafinanceira c
        where c.empresa_id = new.empresa_id and c.ativo
          and case new.forma
                  when 'DINHEIRO' then c.padrao_dinheiro
                  when 'PIX' then c.padrao_pix
                  when 'BOLETO' then c.padrao_boleto
                  when 'CARTAO' then c.padrao_cartao
                  when 'TRANSFERENCIA' then c.padrao_transferencia
                  else false
              end) then
        raise exception 'ck_condicao_sem_conta_padrao' using errcode = '23514';
    end if;
    return new;
end
$$;
create trigger tg_condicaopagamento_conta_padrao before insert or update on CONDICAOPAGAMENTO
    for each row execute function fin_condicao_conta_padrao();

-- ---------------------------------------------------------------------------------------------------------------
-- RLS (mesma regra da V3) nas tabelas da empresa. BANCO e global: sem RLS, de proposito.
-- ---------------------------------------------------------------------------------------------------------------
DO $rls$
DECLARE
    t text;
    tabelas text[] := ARRAY['contafinanceira', 'categoriafinanceira', 'condicaopagamento', 'configuracaofinanceira'];
    teste text := 'current_setting(''app.irrestrito'', true) = ''true''';
BEGIN
    FOREACH t IN ARRAY tabelas LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', t);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', t);
        EXECUTE format(
            'CREATE POLICY isolamento_empresa ON %I USING (empresa_id = current_setting(''app.empresa_id'', true) OR %s) '
            || 'WITH CHECK (empresa_id = current_setting(''app.empresa_id'', true) OR %s)', t, teste, teste);
    END LOOP;
END
$rls$;

-- Permissoes do role de aplicacao (mesmo padrao da V3/V4: so se o role existir)
DO $grants$
DECLARE
    t text;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        FOREACH t IN ARRAY ARRAY['banco', 'contafinanceira', 'categoriafinanceira', 'condicaopagamento', 'configuracaofinanceira'] LOOP
            EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.%I TO emitesimples_app', current_schema(), t);
        END LOOP;
    END IF;
END
$grants$;
