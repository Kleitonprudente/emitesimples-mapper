# IntegraNotas — Softhouse: cadastro de emitente e certificado digital

**Fonte**: schemas e respostas colados pelo usuário a partir da documentação oficial em 2026-11-20 (knowledge-tier B).
**Ambiente**: homologação `https://hom-api.integranotas.com.br`; produção `https://api.integranotas.com.br` — **nunca
para teste**. Emitente e token são independentes por ambiente: cadastra-se e configura-se cada um separadamente.
**Auth**: header `Authorization: <token>` cru, sem `Bearer`.
A emissão, a consulta e o cancelamento de NFS-e estão em `docs/integranotas-nfse-v2.md`.

## Quem usa qual token

- **Token da Softhouse** (admin, por ambiente): cria o emitente. Fica **no servidor, como segredo de configuração**,
  nunca no cliente, no mapa nem em log.
- **Token do Emitente** (JWT, por ambiente): devolvido pelo cadastro; é a credencial do envio, da consulta e do
  cancelamento. É segredo: o mapa guarda só uma **referência ao cofre**.

## Cadastro do emitente — `POST /v1/soft/emitente`

Rota **v1**, com o Token da Softhouse. Resposta de sucesso:
`{ "sucesso": true, "codigo": 200, "mensagem": "Sucesso.", "token": "<JWT>" }` — o `token` é o Token do Emitente.

**Obrigatórios**: `nome` (fantasia, 2 a 60), `razao` (2 a 60), `crt` (1 Simples Nacional, 2 Simples com excesso de
sublimite, 3 Regime Normal, 4 MEI), `telefone` (6 a 14 dígitos), `rua` (1 a 60), `numero` (1 a 60), `bairro` (2 a 60),
`cmun` (IBGE, 7 dígitos), `municipio` (1 a 60), `uf` (2), `cep` (8 dígitos).

**Opcionais relevantes à NFS-e**: `cnpj` (14) **ou** `cpf` (11), um ou outro, por decisão do usuário — o schema marca os
dois como não obrigatórios, mas um deles identifica a empresa; `apelido` (1 a 100), `im` (1 a 20), `email` (60),
`complemento` (60), `regime_cibs` (0 a 4, regime do IBS/CBS; **opcional no modelo por decisão do usuário**), `logo` (JPG
ou PNG em Base64, proporcional como 100x100, até 1 MB), `webhook` (URL de 1 a 200 caracteres), `documentos.nfse`
(boolean, habilita NFS-e) e `documentos.dfe_nfse` (captura de NFS-e). `cnae` só vale para NF-e/NFC-e: na NFS-e o código
do serviço vai no envio.

**Credenciais da prefeitura** (segredos, exigidas em alguns municípios, 1 a 1000 caracteres cada): `login_prefeitura`,
`senha_prefeitura`, `client_id_prefeitura`, `client_secret_prefeitura`. Não viram atributo do mapa.

**Webhook**: a URL é registrada **neste cadastro**, no campo `webhook`, e só é aceita se responder HTTP 200 (o gateway a
valida). O endpoint precisa estar no ar e público no momento do cadastro. O corpo da notificação está em
`docs/integranotas-nfse-v2.md` (pendente).

**Fora do escopo do emissor de NFS-e** (existem no schema; ignorar): `ie`, `suframa`, `csc`, `cscid`, `tar`,
`ciot_chave_acesso`, `vpo_chave_acesso` e os flags de NF-e, NFC-e, MDF-e, CT-e, CT-e OS, BP-e, GNR-e, averbação, NFCom,
DC-e, CIOT, VPO, NF-e ABI, captura de NF-e e de CT-e. O certificado digital **não** vai neste cadastro.

## Certificado digital — `/v1/certificado`

Rota **v1**. O método não veio no texto; o SDK Java oficial usa `POST` para enviar e `GET` para consultar. A autenticação
provável é o Token do Emitente (por ambiente) — inferência, confirmar em homologação.

| Campo | Obrig. | Regra |
|---|---|---|
| `certificado` | sim | conteúdo do arquivo do certificado em **Base64** (o texto não diz o formato; o SDK trata o certificado A1) |
| `senha` | sim | de 1 a 60 caracteres, **só letras e números, sem acentos** |

Consequências: validar a senha **antes** de enviar (senha com símbolo ou acento é recusada pelo gateway); a senha e o
arquivo são segredos, passam pelo sistema sem serem gravados em claro nem em log; o mapa guarda no máximo metadados
(validade, situação). Erros documentados: `5003` (corrompido ou senha errada), `5011` (vencido), `5029` (não pertence ao
CNPJ/CPF do emitente).

## `regime_cibs` — regime de apuração do IBS e da CBS (tpRegimeIBSCBS)

Opcional. Valores do schema oficial:

| Valor | Significado |
|---|---|
| `0` | Não informado (padrão; a tag não é enviada) |
| `1` | Simples Nacional com opção pelo regime regular do IBS e da CBS (`crt` 1 ou 2) |
| `2` | Regime Normal com opção pendente pelo Simples Nacional, sem regime regular do IBS e da CBS (`crt` 3) |
| `3` | Regime Normal com opção pendente pelo Simples Nacional com excesso de sublimite, sem regime regular do IBS e da CBS (`crt` 3) |
| `4` | Regime Normal com opção pendente pelo Simples Nacional, com regime regular do IBS e da CBS (`crt` 3) |

## Decisões do usuário

- Emitente é a empresa usuária do sistema, identificada por CNPJ **ou** CPF.
- `regime_cibs` e o bloco `ibs_cbs` do envio são atributos **opcionais** por ora.

## Não confirmado

Método e autenticação exatos de `/v1/certificado`; formato do arquivo do certificado; rota e corpo de atualização do
emitente (a documentação cita `emitente/atualiza`, sem método); se o cadastro rejeita emitente sem CNPJ nem CPF.
