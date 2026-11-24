-- V10: enums de TEXTO em MAIUSCULAS (SNAKE_CASE_MAIUSCULO), secao 9.7 do guia de modelagem. O valor gravado passa a ser o nome do
-- [e] filho de [e] values do mapa (ex.: 'autorizada' -> 'AUTORIZADA', 'pendenteConfirmacao'/'pendente_confirmacao' ->
-- 'PENDENTE_CONFIRMACAO'). Enums de Inteiro (regime tributario, tipo de destinatario etc.) NAO mudam. Papel do vinculo tambem
-- (ADMINISTRADOR|OPERADOR|CONSULTA), igual ao grupo do JWT e aos roles-allowed.
-- A conversao generica e: camelCase -> snake_case, depois UPPER. Excecao documentada: CHAMADAGATEWAY.OPERACAO, onde o codigo
-- antigo gravava 'enviar_nfse'/'consultar_nfse'/'cancelar_nfse' (nomes fora do enum do mapa) -> EMITIR/CONSULTAR/CANCELAR.
-- Ordem segura: dropar CHECK e indice parcial que citam valores antigos, converter, recriar com os valores novos.

-- 1) Objetos que citam valores antigos
ALTER TABLE NOTA DROP CONSTRAINT ck_nota_numeracao;
DROP INDEX UX_ASSINATURAEMPRESA_VIGENTE;
ALTER TABLE configuracao_integranotas DROP CONSTRAINT configuracao_integranotas_ambiente;

-- 2) Conversao dos valores gravados
UPDATE ASSINATURAEMPRESA SET ESTADO = upper(regexp_replace(ESTADO, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE ESTADO IS NOT NULL;
UPDATE FATURACRM SET ESTADO = upper(regexp_replace(ESTADO, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE ESTADO IS NOT NULL;
UPDATE EMPRESA SET
    SITUACAO = upper(regexp_replace(SITUACAO, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    STATUS_CADASTRO = upper(regexp_replace(STATUS_CADASTRO, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    SITUACAO_CERTIFICADO = upper(regexp_replace(SITUACAO_CERTIFICADO, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    AMBIENTE = upper(regexp_replace(AMBIENTE, '([a-z0-9])([A-Z])', '\1_\2', 'g'));
UPDATE HISTORICOSITUACAOEMPRESA SET
    SITUACAO_ANTERIOR = upper(regexp_replace(SITUACAO_ANTERIOR, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    SITUACAO_NOVA = upper(regexp_replace(SITUACAO_NOVA, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    ORIGEM = upper(regexp_replace(ORIGEM, '([a-z0-9])([A-Z])', '\1_\2', 'g'));
UPDATE PLANO SET PERIODICIDADE = upper(regexp_replace(PERIODICIDADE, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE PERIODICIDADE IS NOT NULL;
UPDATE IMPORTACAOMUNICIPIO SET ESTADO = upper(regexp_replace(ESTADO, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE ESTADO IS NOT NULL;
UPDATE PESSOA SET TIPO_PESSOA = upper(regexp_replace(TIPO_PESSOA, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE TIPO_PESSOA IS NOT NULL;
UPDATE ATIVIDADEEMPRESA SET TIPO = upper(regexp_replace(TIPO, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE TIPO IS NOT NULL;
UPDATE CONTATO SET TIPO_CONTATO = upper(regexp_replace(TIPO_CONTATO, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE TIPO_CONTATO IS NOT NULL;
UPDATE CONFIRMACAOCADASTRO SET ESTADO = upper(regexp_replace(ESTADO, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE ESTADO IS NOT NULL;
UPDATE VINCULOUSUARIO SET PAPEL = upper(regexp_replace(PAPEL, '([a-z0-9])([A-Z])', '\1_\2', 'g')) WHERE PAPEL IS NOT NULL;
UPDATE NOTA SET
    AMBIENTE = upper(regexp_replace(AMBIENTE, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    ESTADO = upper(regexp_replace(ESTADO, '([a-z0-9])([A-Z])', '\1_\2', 'g'));
UPDATE CANCELAMENTO SET
    AMBIENTE = upper(regexp_replace(AMBIENTE, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    ESTADO = upper(regexp_replace(ESTADO, '([a-z0-9])([A-Z])', '\1_\2', 'g'));
UPDATE CHAMADAGATEWAY SET
    AMBIENTE = upper(regexp_replace(AMBIENTE, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    OPERACAO = CASE OPERACAO
        WHEN 'enviar_nfse' THEN 'EMITIR'
        WHEN 'consultar_nfse' THEN 'CONSULTAR'
        WHEN 'cancelar_nfse' THEN 'CANCELAR'
        ELSE upper(regexp_replace(OPERACAO, '([a-z0-9])([A-Z])', '\1_\2', 'g')) END,
    VERSAO_API = upper(VERSAO_API),
    METODO_HTTP = upper(METODO_HTTP);
UPDATE EVENTOWEBHOOK SET
    AMBIENTE = upper(regexp_replace(AMBIENTE, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    ESTADO_PROCESSAMENTO = upper(regexp_replace(ESTADO_PROCESSAMENTO, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
    RESULTADO_ASSINATURA = upper(regexp_replace(RESULTADO_ASSINATURA, '([a-z0-9])([A-Z])', '\1_\2', 'g'));
-- configuracao da plataforma (tabela escrita a mao, V6): o ambiente ativo segue o mesmo padrao de valor
UPDATE configuracao_integranotas SET ambiente_ativo = upper(ambiente_ativo);
ALTER TABLE configuracao_integranotas ALTER COLUMN ambiente_ativo SET DEFAULT 'HOMOLOGACAO';

-- 3) Recria os objetos com os valores novos
ALTER TABLE NOTA ADD CONSTRAINT ck_nota_numeracao
    CHECK (ESTADO IN ('RASCUNHO', 'DESCARTADA') OR (NUMERO IS NOT NULL AND SERIE IS NOT NULL AND DATA_EMISSAO IS NOT NULL));
CREATE UNIQUE INDEX UX_ASSINATURAEMPRESA_VIGENTE ON ASSINATURAEMPRESA (EMPRESA_ID) WHERE ESTADO IN ('ATIVA', 'SUSPENSA');
ALTER TABLE configuracao_integranotas ADD CONSTRAINT configuracao_integranotas_ambiente
    CHECK (ambiente_ativo IN ('HOMOLOGACAO', 'PRODUCAO'));
