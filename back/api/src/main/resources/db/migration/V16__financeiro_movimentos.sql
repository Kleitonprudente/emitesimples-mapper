-- V16: modulo financeiro, FASE 3 (livro de movimentos). Regras em docs/financeiro/README.md (secao 4, F3-H1 a F3-H8, decisoes C1 a C7) e
-- no mapa back/mapas/Financeiro/Movimento.mi. A tabela segue a entidade gerada (Hibernate validate). MOVIMENTO nao tem CRUD gerado: so o
-- LivroDeMovimentos (financeiroop) grava, e so INSERT: o banco recusa UPDATE e DELETE (C3). Erro se corrige com estorno.

-- FK composta com a empresa (C6): a conta e o titulo da linha sao, garantidamente, da mesma empresa da linha.
create unique index ux_contafinanceira_empresa_id on CONTAFINANCEIRA (EMPRESA_ID, ID);
create unique index ux_titulo_empresa_id on TITULO (EMPRESA_ID, ID);

create table MOVIMENTO (
    ID varchar(36) not null primary key,
    EMPRESA_ID varchar(36) not null references EMPRESA (ID),
    CONTA_ID varchar(36) not null,
    TITULO_ID varchar(36),
    CATEGORIA_ID varchar(36) references CATEGORIAFINANCEIRA (ID),
    TIPO varchar(24) not null,
    SENTIDO varchar(7) not null,
    DATA date not null,
    DESCRICAO varchar(255),
    FORMA varchar(20),
    VALOR numeric(15,2) not null,
    QUITADO numeric(15,2) not null default 0,
    JUROS numeric(15,2) not null default 0,
    MULTA numeric(15,2) not null default 0,
    DESCONTO numeric(15,2) not null default 0,
    ID_LOTE varchar(36),
    ID_TRANSFERENCIA varchar(36),
    ID_ESTORNO_DE varchar(36),
    ID_CRIADO_POR varchar(36),
    DATA_CRIACAO timestamp(6),
    VERSION timestamp(6),
    constraint fk_movimento_conta foreign key (EMPRESA_ID, CONTA_ID) references CONTAFINANCEIRA (EMPRESA_ID, ID),
    constraint fk_movimento_titulo foreign key (EMPRESA_ID, TITULO_ID) references TITULO (EMPRESA_ID, ID),
    constraint ck_movimento_tipo check (TIPO in ('RECEBIMENTO', 'PAGAMENTO', 'ENTRADA_AVULSA', 'SAIDA_AVULSA',
                                                 'TRANSFERENCIA_ENTRADA', 'TRANSFERENCIA_SAIDA', 'ESTORNO')),
    constraint ck_movimento_sentido check (SENTIDO in ('ENTRADA', 'SAIDA')),
    constraint ck_movimento_forma check (FORMA is null or FORMA in ('DINHEIRO', 'PIX', 'BOLETO', 'CARTAO', 'TRANSFERENCIA')),
    constraint ck_movimento_valor check (VALOR > 0 and VALOR <= 999999999.99),
    constraint ck_movimento_partes check (QUITADO >= 0 and JUROS >= 0 and MULTA >= 0 and DESCONTO >= 0),
    -- linha de baixa fecha a conta; linha sem titulo tem as quatro colunas zeradas
    constraint ck_movimento_fecha check (
        (TITULO_ID is null and QUITADO = 0 and JUROS = 0 and MULTA = 0 and DESCONTO = 0)
        or (TITULO_ID is not null and VALOR = QUITADO + JUROS + MULTA - DESCONTO)),
    constraint ck_movimento_baixa_tem_titulo check (TIPO not in ('RECEBIMENTO', 'PAGAMENTO') or TITULO_ID is not null),
    constraint ck_movimento_transferencia check (TIPO not in ('TRANSFERENCIA_ENTRADA', 'TRANSFERENCIA_SAIDA') or ID_TRANSFERENCIA is not null),
    constraint ck_movimento_estorno check ((TIPO = 'ESTORNO') = (ID_ESTORNO_DE is not null))
);
-- uma linha so se estorna uma vez
create unique index ux_movimento_estorno on MOVIMENTO (EMPRESA_ID, ID_ESTORNO_DE) where ID_ESTORNO_DE is not null;
create index ix_movimento_extrato on MOVIMENTO (EMPRESA_ID, CONTA_ID, DATA, ID);
create index ix_movimento_titulo on MOVIMENTO (EMPRESA_ID, TITULO_ID);
create index ix_movimento_transferencia on MOVIMENTO (ID_TRANSFERENCIA);
create index ix_movimento_categoria on MOVIMENTO (CATEGORIA_ID);

-- IMUTAVEL (C3): o livro so aceita INSERT. Vale para qualquer caminho, inclusive SQL direto.
create function fin_movimento_imutavel() returns trigger language plpgsql as $$
begin
    raise exception 'ck_movimento_imutavel' using errcode = '23514';
end
$$;
create trigger tg_movimento_imutavel before update or delete on MOVIMENTO
    for each row execute function fin_movimento_imutavel();

-- Conta com movimentos: saldo inicial e data de abertura travados (F3-H8; pendencia da Fase 1).
create function fin_conta_trava_saldo() returns trigger language plpgsql as $$
begin
    if (old.saldo_inicial is distinct from new.saldo_inicial or old.data_saldo_inicial is distinct from new.data_saldo_inicial)
       and exists (select 1 from movimento m where m.conta_id = new.id and m.empresa_id = new.empresa_id) then
        raise exception 'ck_conta_com_movimentos' using errcode = '23514';
    end if;
    return new;
end
$$;
create trigger tg_contafinanceira_trava_saldo before update on CONTAFINANCEIRA
    for each row execute function fin_conta_trava_saldo();

-- "Categoria em uso" passa a olhar tambem o livro (F3-H8). Mesma funcao da V15, com o acrescimo.
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
        -- so vira pai a categoria fora de uso: nao e padrao da configuracao (Fase 1), nao tem titulo (Fase 2) nem movimento (Fase 3)
        if exists (select 1 from configuracaofinanceira c
                   where c.categoria_padrao_receita_id = pai.id or c.categoria_padrao_despesa_id = pai.id)
           or exists (select 1 from titulo t where t.categoria_id = pai.id)
           or exists (select 1 from movimento m where m.categoria_id = pai.id) then
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

-- RLS (mesma regra da V3) e permissoes do role de aplicacao (o DELETE/UPDATE do GRANT e barrado pela trigger acima)
ALTER TABLE movimento ENABLE ROW LEVEL SECURITY;
ALTER TABLE movimento FORCE ROW LEVEL SECURITY;
CREATE POLICY isolamento_empresa ON movimento
    USING (empresa_id = current_setting('app.empresa_id', true) OR current_setting('app.irrestrito', true) = 'true')
    WITH CHECK (empresa_id = current_setting('app.empresa_id', true) OR current_setting('app.irrestrito', true) = 'true');

DO $grants$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'emitesimples_app') THEN
        EXECUTE format('GRANT SELECT, INSERT ON %I.movimento TO emitesimples_app', current_schema());
        EXECUTE format('REVOKE UPDATE, DELETE ON %I.movimento FROM emitesimples_app', current_schema());
    END IF;
END
$grants$;
