-- V2: extensao, unicidades e indices que o MAPA NAO EXPRESSA (o gerador nao os produz). Cada regra abaixo esta
-- documentada na description da entidade correspondente no mapa (back/mapas). Regerada com a V1 ate existir um banco real.

-- A busca gerada usa unaccent() (nondeterministicSearchField). Extensao confiavel no PostgreSQL 13 ou superior.
CREATE EXTENSION IF NOT EXISTS unaccent;

-- ---------------------------------------------------------------------------------------------------------------
-- Municipio: tabela administravel (docs/carga-municipios.md). A importacao atualiza ou insere pelo codigo IBGE.
-- O registro especial Exterior (UF EX) tem codigo IBGE '0'.
-- ---------------------------------------------------------------------------------------------------------------
ALTER TABLE MUNICIPIO ALTER COLUMN CODIGO_IBGE SET NOT NULL;
ALTER TABLE MUNICIPIO ALTER COLUMN NOME SET NOT NULL;
ALTER TABLE MUNICIPIO ALTER COLUMN UF SET NOT NULL;
CREATE UNIQUE INDEX UX_MUNICIPIO_CODIGO_IBGE ON MUNICIPIO (CODIGO_IBGE);
CREATE UNIQUE INDEX UX_MUNICIPIO_NOME_UF ON MUNICIPIO (NOME, UF);

-- ---------------------------------------------------------------------------------------------------------------
-- Unicidades documentadas no mapa
-- ---------------------------------------------------------------------------------------------------------------
-- Empresa: o mesmo documento (CNPJ ou CPF) tem um registro por ambiente (homologacao e producao), nunca dois no mesmo.
-- A empresa nunca e apagada, so desativada (situacao).
ALTER TABLE EMPRESA ALTER COLUMN DOCUMENTO SET NOT NULL;
CREATE UNIQUE INDEX UX_EMPRESA_DOCUMENTO_AMBIENTE ON EMPRESA (DOCUMENTO, AMBIENTE);

-- Pessoa e por empresa: o mesmo documento pode se repetir entre empresas, nao dentro de uma.
CREATE UNIQUE INDEX UX_PESSOA_EMPRESA_DOCUMENTO ON PESSOA (EMPRESA_ID, DOCUMENTO) WHERE DOCUMENTO IS NOT NULL;

-- Nota: numeracao da DPS (serie e numero) unica por empresa e ambiente. A chave de 50 digitos e unica por NFS-e.
-- (O campo identificacao do envio nao e coluna: o envio manda o id da Nota.)
CREATE UNIQUE INDEX UX_NOTA_NUMERACAO ON NOTA (EMPRESA_ID, AMBIENTE, SERIE, NUMERO);
CREATE UNIQUE INDEX UX_NOTA_CHAVE ON NOTA (CHAVE) WHERE CHAVE IS NOT NULL;

-- Webhook: a mesma notificacao pode chegar ate 3 vezes; a chave de deduplicacao e (empresa, ambiente, chave, codigo).
CREATE UNIQUE INDEX UX_EVENTOWEBHOOK_DEDUP ON EVENTOWEBHOOK (EMPRESA_ID, AMBIENTE, CHAVE, CODIGO);

-- Usuarios e acesso
CREATE UNIQUE INDEX UX_USUARIO_EMAIL ON USUARIO (LOWER(EMAIL));
CREATE UNIQUE INDEX UX_VINCULOUSUARIO_USUARIO_EMPRESA ON VINCULOUSUARIO (USUARIO_ID, EMPRESA_ID);
CREATE UNIQUE INDEX UX_CLIENTEACESSO_CLIENT_ID ON CLIENTEACESSO (CLIENT_ID);
CREATE UNIQUE INDEX UX_TOKENRENOVACAO_TOKEN_HASH ON TOKENRENOVACAO (TOKEN_HASH);
CREATE UNIQUE INDEX UX_CONFIRMACAOCADASTRO_TOKEN_HASH ON CONFIRMACAOCADASTRO (TOKEN_HASH);

-- Admin: a fatura do CRM e identificada pelo id dela no CRM.
CREATE UNIQUE INDEX UX_FATURACRM_CRM_FATURA_ID ON FATURACRM (CRM_FATURA_ID);

-- Assinatura: no maximo UMA vigente por empresa. PROVISORIO: a assinatura 'suspensa' ocupa a vaga (a empresa continua com
-- o plano, so sem acesso); so 'cancelada' a libera. O usuario ainda nao confirmou; se mudar, esta e a unica linha a trocar.
CREATE UNIQUE INDEX UX_ASSINATURAEMPRESA_VIGENTE ON ASSINATURAEMPRESA (EMPRESA_ID) WHERE ESTADO IN ('ativa', 'suspensa');

-- ---------------------------------------------------------------------------------------------------------------
-- Indices de desempenho. O PostgreSQL NAO indexa chaves estrangeiras sozinho, e toda consulta de uma empresa filtra
-- por EMPRESA_ID. PESSOA, NOTA e EVENTOWEBHOOK ja tem EMPRESA_ID como primeira coluna de um indice unico acima.
-- ---------------------------------------------------------------------------------------------------------------
CREATE INDEX IX_ATIVIDADEEMPRESA_EMPRESA ON ATIVIDADEEMPRESA (EMPRESA_ID);
CREATE INDEX IX_QSAEMPRESA_EMPRESA ON QSAEMPRESA (EMPRESA_ID);
CREATE INDEX IX_CANCELAMENTO_EMPRESA ON CANCELAMENTO (EMPRESA_ID);
CREATE INDEX IX_CANCELAMENTO_NOTA ON CANCELAMENTO (NOTA_ID);
CREATE INDEX IX_CHAMADAGATEWAY_EMPRESA ON CHAMADAGATEWAY (EMPRESA_ID);
CREATE INDEX IX_CHAMADAGATEWAY_NOTA ON CHAMADAGATEWAY (NOTA_ID);
CREATE INDEX IX_CLIENTEACESSO_EMPRESA ON CLIENTEACESSO (EMPRESA_ID);
CREATE INDEX IX_CONFIRMACAOCADASTRO_EMPRESA ON CONFIRMACAOCADASTRO (EMPRESA_ID);
CREATE INDEX IX_CONFIRMACAOCADASTRO_USUARIO ON CONFIRMACAOCADASTRO (USUARIO_ID);
CREATE INDEX IX_CONTATO_EMPRESA ON CONTATO (EMPRESA_ID);
CREATE INDEX IX_CONTATO_PESSOA ON CONTATO (PESSOA_ID);
CREATE INDEX IX_EMPRESA_MUNICIPIO ON EMPRESA (MUNICIPIO_ID);
CREATE INDEX IX_EVENTOWEBHOOK_NOTA ON EVENTOWEBHOOK (NOTA_ID);
CREATE INDEX IX_FATURACRM_EMPRESA ON FATURACRM (EMPRESA_ID);
CREATE INDEX IX_FATURACRM_ASSINATURA ON FATURACRM (ASSINATURA_ID);
CREATE INDEX IX_HISTORICOSITUACAOEMPRESA_EMPRESA ON HISTORICOSITUACAOEMPRESA (EMPRESA_ID);
CREATE INDEX IX_HISTORICOSITUACAOEMPRESA_RESPONSAVEL ON HISTORICOSITUACAOEMPRESA (RESPONSAVEL_ID);
CREATE INDEX IX_IMPORTACAOMUNICIPIO_USUARIO ON IMPORTACAOMUNICIPIO (USUARIO_ID);
CREATE INDEX IX_ASSINATURAEMPRESA_PLANO ON ASSINATURAEMPRESA (PLANO_ID);
CREATE INDEX IX_NOTA_PESSOA ON NOTA (PESSOA_ID);
CREATE INDEX IX_NOTA_SERVICO ON NOTA (SERVICO_ID);
CREATE INDEX IX_PESSOA_CIDADE ON PESSOA (CIDADE_ID);
CREATE INDEX IX_SERVICO_EMPRESA ON SERVICO (EMPRESA_ID);
CREATE INDEX IX_TOKENRENOVACAO_USUARIO ON TOKENRENOVACAO (USUARIO_ID);
CREATE INDEX IX_VINCULOUSUARIO_EMPRESA ON VINCULOUSUARIO (EMPRESA_ID);
