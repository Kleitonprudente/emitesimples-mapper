-- V5: Pessoa passa a ser cliente, fornecedor ou as duas coisas (atributos Boolean obrigatorios do mapa, default false).
-- Toda pessoa que ja existia foi cadastrada como cliente (o cadastro nasceu so para clientes).
-- RLS (V3) e GRANTs (V3, por tabela) ja cobrem a tabela inteira: coluna nova nao exige nada aqui.
ALTER TABLE pessoa
    ADD COLUMN cliente boolean NOT NULL DEFAULT false,
    ADD COLUMN fornecedor boolean NOT NULL DEFAULT false;

UPDATE pessoa SET cliente = true;

-- Listagem filtrada por empresa e papel (cliente/fornecedor). empresa_id e a coluna de tenant (padrao da V2).
CREATE INDEX ix_pessoa_empresa_cliente ON pessoa (empresa_id, cliente);
CREATE INDEX ix_pessoa_empresa_fornecedor ON pessoa (empresa_id, fornecedor);
