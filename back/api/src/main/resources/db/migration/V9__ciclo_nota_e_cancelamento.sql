-- V9: ciclo de vida da Nota e do Cancelamento conforme o mapa (CicloNota e CicloCancelamento) e permissao delegada de cancelar.
-- NUMERO, SERIE e DATA_EMISSAO da NOTA ja eram colunas anulaveis (o mapa agora tambem as deixa opcionais): so o rascunho do
-- assistente de IA (e a nota descartada, que nunca foi enviada) nao os tem; ao virar processando (criacao direta ou aprovacao) sao
-- obrigatorios, o que o CHECK abaixo garante.

-- 1) Nota: 'rascunho' passa a existir so para o assistente. O que o codigo antigo deixava em 'rascunho' depois de falha de
--    infraestrutura no envio direto (numero ja alocado) vira 'falha_envio' (reenviavel com o mesmo numero).
UPDATE NOTA SET ESTADO = 'falha_envio' WHERE ESTADO = 'rascunho' AND NUMERO IS NOT NULL;

ALTER TABLE NOTA ADD CONSTRAINT ck_nota_numeracao
    CHECK (ESTADO IN ('rascunho', 'descartada') OR (NUMERO IS NOT NULL AND SERIE IS NOT NULL AND DATA_EMISSAO IS NOT NULL));

-- 2) Cancelamento: estados rascunho (so assistente), processando, concluido, recusado, expirado e descartado.
--    Os antigos do mapa nunca eram gravados pelo codigo; migrados por seguranca.
UPDATE CANCELAMENTO SET ESTADO = 'rascunho' WHERE ESTADO = 'aguardando_aprovacao';
UPDATE CANCELAMENTO SET ESTADO = 'processando' WHERE ESTADO = 'solicitado';
UPDATE CANCELAMENTO SET ESTADO = 'concluido' WHERE ESTADO = 'cancelado';
UPDATE CANCELAMENTO SET ESTADO = 'recusado' WHERE ESTADO = 'rejeitado';
-- pedido que o codigo antigo recusou por 24 h sem confirmacao (mensagem gravada pelo servico) passa a 'expirado'
UPDATE CANCELAMENTO SET ESTADO = 'expirado' WHERE ESTADO = 'recusado' AND MENSAGEM_RETORNO LIKE 'O IntegraNotas n%o confirmou o cancelamento%';

-- quem descarta o rascunho (espelha a Nota); recusado deixa de ter responsavel (quem recusa e o gateway ou o sistema)
ALTER TABLE CANCELAMENTO ADD COLUMN DATA_HORA_DESCARTE timestamp(6);
ALTER TABLE CANCELAMENTO ADD COLUMN DESCARTADO_POR_ID varchar(36);
ALTER TABLE CANCELAMENTO ADD CONSTRAINT fk_cancelamento_descartado_por FOREIGN KEY (DESCARTADO_POR_ID) REFERENCES USUARIO;
CREATE INDEX IX_CANCELAMENTO_DESCARTADO_POR ON CANCELAMENTO (DESCARTADO_POR_ID);
ALTER TABLE CANCELAMENTO DROP COLUMN RECUSADO_POR_ID; -- leva junto a FK

-- 3) Operador so cancela com a permissao delegada pelo administrador (o administrador sempre pode).
--    ATENCAO: o padrao e false; operadores que cancelavam antes (sem delegacao) perdem essa permissao ate o administrador delegar.
ALTER TABLE VINCULOUSUARIO ADD COLUMN PODE_CANCELAR_NFSE boolean NOT NULL DEFAULT false;
