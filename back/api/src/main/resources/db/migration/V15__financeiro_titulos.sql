-- V15: modulo financeiro, FASE 2 (titulos a receber e a pagar). Regras em docs/financeiro/README.md (secao 3, F2-H1 a F2-H7) e no mapa
-- back/mapas/Financeiro/Titulo.mi. A tabela segue o que a entidade gerada espera (Hibernate validate). TITULO nao tem CRUD gerado: so os
-- servicos da financeiroop gravam aqui. Os nomes das restricoes sao traduzidos pela financeiroop/RestricoesFinanceiro.

create table TITULO (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    TIPO varchar(7) not null,
    DESCRICAO varchar(255) not null,
    OBSERVACAO varchar(255),
    ID_PESSOA varchar(36),
    VALOR numeric(15,2) not null,
    QUITADO numeric(15,2) not null default 0,
    MULTA_PERCENTUAL numeric(5,2),
    JUROS_MENSAL_PERCENTUAL numeric(5,2),
    VENCIMENTO date not null,
    FORMA_PREVISTA varchar(20),
    PARCELA integer not null default 1,
    TOTAL_PARCELAS integer not null default 1,
    ID_LANCAMENTO varchar(36),
    ORIGEM_TIPO varchar(10) not null default 'MANUAL',
    ID_ORIGEM varchar(36),
    ORIGEM_NUMERO varchar(40),
    SITUACAO varchar(10) not null default 'ABERTO',
    PARA_REVISAO boolean not null default false,
    CANCELADO_EM timestamp(6),
    APROVADO_EM timestamp(6),
    ID_APROVADO_POR varchar(36),
    ID_CRIADO_POR varchar(36),
    CATEGORIA_ID varchar(36) references CATEGORIAFINANCEIRA (ID),
    DATA_CRIACAO timestamp(6),
    DATA_ATUALIZACAO timestamp(6),
    VERSION timestamp(6),
    constraint ck_titulo_tipo check (TIPO in ('RECEBER', 'PAGAR')),
    constraint ck_titulo_situacao check (SITUACAO in ('ABERTO', 'PARCIAL', 'PAGO', 'CANCELADO')),
    constraint ck_titulo_origem check (ORIGEM_TIPO in ('MANUAL', 'NFSE')),
    constraint ck_titulo_forma check (FORMA_PREVISTA is null or FORMA_PREVISTA in ('DINHEIRO', 'PIX', 'BOLETO', 'CARTAO', 'TRANSFERENCIA')),
    constraint ck_titulo_valor check (VALOR > 0 and VALOR <= 999999999.99 and QUITADO >= 0 and QUITADO <= VALOR),
    constraint ck_titulo_parcela check (PARCELA >= 1 and TOTAL_PARCELAS >= PARCELA),
    constraint ck_titulo_multa check (MULTA_PERCENTUAL is null or MULTA_PERCENTUAL between 0 and 999.99),
    constraint ck_titulo_juros check (JUROS_MENSAL_PERCENTUAL is null or JUROS_MENSAL_PERCENTUAL between 0 and 999.99),
    constraint ck_titulo_cancelado check ((SITUACAO = 'CANCELADO') = (CANCELADO_EM is not null))
);
-- o documento de origem nunca gera a mesma parcela duas vezes (Fase 5)
create unique index ux_titulo_origem on TITULO (EMPRESA_ID, ORIGEM_TIPO, ID_ORIGEM, PARCELA) where ID_ORIGEM is not null;
create index ix_titulo_lista on TITULO (EMPRESA_ID, TIPO, SITUACAO, VENCIMENTO);
create index ix_titulo_pessoa on TITULO (EMPRESA_ID, ID_PESSOA);
create index ix_titulo_lancamento on TITULO (ID_LANCAMENTO);
create index ix_titulo_categoria on TITULO (CATEGORIA_ID);

-- "Categoria em uso" passa a olhar tambem os titulos (F2-H7): categoria com titulo nao vira pai. Mesma funcao da V14, com o acrescimo.
-- FASE 3: acrescentar MOVIMENTO aqui (CREATE OR REPLACE numa migracao nova).
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
        if not pai.ativo and (new.ativo or tg_op = 'INSERT' or old.categoria_pai_id is distinct from new.categoria_pai_id) then
            raise exception 'ck_categoria_pai_inativo' using errcode = '23514';
        end if;
        -- so vira pai a categoria fora de uso: nao e padrao da configuracao (Fase 1) e nao tem titulo (Fase 2)
        if exists (select 1 from configuracaofinanceira c
                   where c.categoria_padrao_receita_id = pai.id or c.categoria_padrao_despesa_id = pai.id)
           or exists (select 1 from titulo t where t.categoria_id = pai.id) then
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

-- RLS (mesma regra da V3) e permissoes do role de aplicacao (mesmo padrao da V14)
ALTER TABLE titulo ENABLE ROW LEVEL SECURITY;
ALTER TABLE titulo FORCE ROW LEVEL SECURITY;
CREATE POLICY isolamento_empresa ON titulo
    USING (empresa_id = current_setting('app.empresa_id', true) OR current_setting('app.irrestrito', true) = 'true')
    WITH CHECK (empresa_id = current_setting('app.empresa_id', true) OR current_setting('app.irrestrito', true) = 'true');

DO $grants$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON %I.titulo TO emitesimples_app', current_schema());
    END IF;
END
$grants$;
