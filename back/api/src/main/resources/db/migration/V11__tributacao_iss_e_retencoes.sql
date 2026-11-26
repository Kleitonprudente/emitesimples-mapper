-- V11: tributacao da NFS-e (ISS e retencoes federais), conforme docs/historias-emissao-nota.md, secao "Tributacao da nota".
-- Todas as colunas sao NULAVEIS e sem default: sem dado tributario, nada muda nas notas e servicos existentes.
-- NOTA.CST_PIS_COFINS e NOTA.TIPO_RETENCAO ja aceitavam nulo e nao tem default no banco (o "required" e o "defaultValue 2"
-- que sairam do mapa so existiam na aplicacao), entao nao precisam de alteracao aqui.

-- Servico: padroes que a nota copia ao ser preparada
ALTER TABLE SERVICO ADD COLUMN BASE_CALCULO_ISS numeric(15,2);
ALTER TABLE SERVICO ADD COLUMN TIPO_RETENCAO integer;
ALTER TABLE SERVICO ADD COLUMN ALIQUOTA_PIS numeric(5,2);
ALTER TABLE SERVICO ADD COLUMN ALIQUOTA_COFINS numeric(5,2);

-- Nota: valores do ISS usados nesta nota
ALTER TABLE NOTA ADD COLUMN VALOR_BASE_CALCULO_ISS numeric(15,2);
ALTER TABLE NOTA ADD COLUMN VALOR_ISS_RETIDO numeric(15,2);
