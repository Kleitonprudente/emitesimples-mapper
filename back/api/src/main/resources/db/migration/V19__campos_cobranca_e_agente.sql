-- V19: campos novos das pendencias do financeiro (docs/financeiro/README.md, 9.3 e 9.4).
-- Fase 6: a pessoa pode pedir para nao receber cobranca nem lembrete por e-mail.
alter table PESSOA add column NAO_ENVIAR_COBRANCA boolean not null default false;

-- B3: titulo lancado por agente (cliente de acesso) guarda quem lancou e nasce sem aprovacao.
alter table TITULO add column ID_CRIADO_POR_CLIENTE varchar(36);
create index ix_titulo_rascunho_agente on TITULO (EMPRESA_ID, ID_CRIADO_POR) where APROVADO_EM is null and SITUACAO <> 'CANCELADO';

-- B3: o cliente de acesso e de um usuario (age por ele, so com os escopos dados). A tabela nunca teve escrita (sem endpoint ate aqui).
alter table CLIENTEACESSO add column USUARIO_ID varchar(36) references USUARIO (ID);
alter table CLIENTEACESSO alter column USUARIO_ID set not null;
create unique index if not exists ux_clienteacesso_client_id on CLIENTEACESSO (CLIENT_ID);
