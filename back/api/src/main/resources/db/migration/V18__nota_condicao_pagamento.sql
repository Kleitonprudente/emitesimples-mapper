-- V18: condicao de pagamento na nota (docs/financeiro/README.md, 9.1, E1). Id bruto da CondicaoPagamento do financeiro (CN-REL-002):
-- sem FK, porque o contexto nfse nao depende do financeiro; quem confere a condicao (existe, e da empresa, esta ativa) e o financeiro,
-- ao gerar os titulos da nota autorizada. Opcional: sem ela, 1 titulo a vista (como antes).
alter table NOTA add column ID_CONDICAO_PAGAMENTO varchar(36);
