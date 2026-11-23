-- V7: toda Pessoa tem ao menos um papel (cliente, fornecedor ou ambos). Sem papel ela nao apareceria em Clientes nem em
-- Fornecedores. O front ja impede (formulario com papel padrao e regra de ao menos um); este CHECK e a rede de seguranca da API.
-- Pessoa que ja estivesse sem papel passa a cliente (papel padrao, o mesmo da V5).
UPDATE pessoa SET cliente = true WHERE NOT cliente AND NOT fornecedor;

ALTER TABLE pessoa ADD CONSTRAINT ck_pessoa_papel CHECK (cliente OR fornecedor);
