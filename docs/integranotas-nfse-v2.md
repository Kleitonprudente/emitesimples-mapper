# IntegraNotas — NFS-e (envio v2, consulta e cancelamento v1)

**Fonte**: schemas e respostas colados pelo usuário a partir da documentação oficial em 2026-11-20 (knowledge-tier B),
mais o SDK Java oficial `cloud-dfe/sdk-java` e a leitura de `https://integranotas.com.br/doc/nfse` (tier C) onde indicado.
**Auth**: header `Authorization: <token>` cru, sem `Bearer`. Token do Emitente, por ambiente.
**Ambiente**: homologação `https://hom-api.integranotas.com.br`; produção `https://api.integranotas.com.br` — **nunca
para teste**. A emissão não é simulada: em homologação vai ao ambiente de teste real da prefeitura.
**Cadastro de emitente e certificado digital** estão em `docs/integranotas-softhouse.md`.

> Resumo estrutural fiel, não cópia integral. Tabelas de enum extensas (CST PIS/COFINS, mecanismos de fomento, tipos de
> dedução) estão na documentação oficial. O que **não** está confirmado fica listado no fim. Não invente; pergunte.

## Operações e versão da API (a versão é por operação, não global)

| Operação | Método e rota (homologação) | Versão | Fonte |
|---|---|---|---|
| Envio | `POST /v2/nfse` | **v2** | usuário |
| Consulta por chave | `GET /v1/nfse/{chave}` | **v1** | usuário |
| Cancelamento | `POST /v1/nfse/cancela` | **v1** | usuário |
| Substituição | `POST /nfse/substitui` | não confirmada na v2 | doc + SDK |
| PDF | `GET /nfse/pdf/{chave}` | não confirmada | SDK |
| Resolver conflito | `GET /nfse/resolve/{chave}` | não confirmada | SDK |
| Info do município | `GET /nfse/info/{ibge}` (nacional: IBGE `9999999`) | não confirmada | SDK + doc |
| Prévia (PDF) | `POST /nfse/preview` | não confirmada | doc + SDK |
| Busca na base do gateway | `POST /nfse/busca` | não confirmada | doc + SDK |
| Busca na prefeitura | `POST /nfse/consulta` | não confirmada | doc + SDK |
| Backup por e-mail | `POST /nfse/backup` | não confirmada | doc + SDK |
| Processar offline | `GET /nfse/offline` | não confirmada | doc + SDK |

O cliente **não pode ter uma única versão global**. As rotas sem versão confirmada seguem os caminhos do SDK Java
(master, v1) e a documentação as lista sem `{chave}`/`{ibge}`; confirmar em homologação antes de gerar código.
Rotas `*_depreciada` e o payload v1 com `itens` **não entram em código novo**.

## A chave da NFS-e

**Tem 50 dígitos** (padrão nacional, após a reforma tributária), por decisão do usuário. Menções a 44 dígitos na
documentação (consulta, cancelamento) e no SDK (`checkKey`) são do padrão antigo: **ignorar**. O exemplo de 37 dígitos é
de preenchimento.

## Envio — `POST /v2/nfse`

### Convenções de formato

- Valores monetários: `number` (`100.00`). Percentuais em **pontos percentuais**: 5% é `5.00`, nunca `0.05`.
- Datas e horas: ISO com fuso (`2020-05-16T12:33:20-03:00`). Datas simples: `2025-12-31`.
- Quebra de linha: apenas `\n`. Tamanho máximo do corpo: 1,2 MB (erro `8000`).
- Campos opcionais aceitam `null`. `required: true` dentro de um bloco opcional vale **se o bloco for enviado**; o
  schema não diz quais blocos são obrigatórios por si — confirmar em homologação.
- O **prestador (emitente) não vai no corpo**: é identificado pelo token.

### Raiz

| Campo | Obrig. | Observação |
|---|---|---|
| `data_emissao` | sim | (dhEmi) |
| `numero` | sim | numeração da DPS, string de 1 a 9 dígitos (nDPS) |
| `serie` | sim | série da DPS, string de 1 a 5 dígitos |
| `identificacao` | não | id único do sistema emissor, 1 a 40 caracteres — chave de correlação própria |
| `data_competencia` | não | padrão = `data_emissao` |
| `tipo_emitente` | não | 1 Prestador (padrão), 2 Tomador, 3 Intermediário |
| `regime_apuracao` | não | 1, 2 ou 3 (regApTribSN) |
| `regime_tributacao` | não | 0 Nenhum (padrão), 1 a 6, 9 |
| `informacoes_complementares` | não | até 2000 caracteres |
| `contingencia` | — | `data` e `motivo` (15 a 256 caracteres), ambos obrigatórios se enviado |
| `emissao_tomador` | — | `tipo` (1 a 4) e `chave` (50 dígitos), se emitida pelo tomador/intermediário |

### Blocos

- **`informacoes`**: `documento_tecnico`, `documento_ref`, `numero_pedido`, `itens_pedido_compra[].item`.
- **`tomador`**: `cnpj` ou `cpf` (informar um), `nif`, `dispensa_nif`, `caepf`, `im`, **`razao_social` (obrig.)**,
  `telefone`, `email` (60), `tipo_ente_governamental`, `tipo_destinatario`, `endereco`.
- **Tomador — tamanhos e valores do schema oficial**: `razao_social` 1 a 300; `nif` 1 a 40; `dispensa_nif` 0 Não informado
  na nota de origem, 1 Dispensado, 2 Não exigência; `caepf` 14; `im` 1 a 15; `telefone` 6 a 20 dígitos; `email` até 60;
  `tipo_ente_governamental` 1 União, 2 Estado, 3 Distrito Federal, 4 Município; `tipo_destinatario` (indDest) **0** = o
  destinatário é o próprio tomador/adquirente (padrão), **1** = o destinatário não é o próprio adquirente (outra pessoa ou
  outro estabelecimento). Endereço: `logradouro` 1 a 255, `numero` 1 a 60, `complemento` 1 a 256, `bairro` 1 a 60, `cep` 8,
  `codigo_municipio` 7; exportação: `codigo_pais` **3 a 4 dígitos**, `codigo_postal` 1 a 11, `nome_municipio` 1 a 60,
  `nome_provincia` 1 a 60.
- **`endereco`** (padrão repetido em tomador, destinatário, intermediário, emitente de dedução):
  **`logradouro`, `numero`, `bairro`, `uf` obrigatórios**; `complemento`, `codigo_municipio` (IBGE, 7 dígitos), `cep`
  (8), e para exportação `codigo_pais`, `codigo_postal`, `nome_municipio`, `nome_provincia`. `uf` = `EX` em exportação.
- **`intermediario`**, **`obra`**, **`evento`**, **`locacao`**, **`exterior`**: blocos opcionais de casos específicos.
- **`servico`**
  - **`codigo` (obrig.)**: 6 dígitos, item da LC 116/2003 (cTribNac).
  - **`discriminacao` (obrig.)**: até 2000 caracteres.
  - **`valor_servicos` (obrig.)**: mínimo 0.01.
  - `descricao` (200, só para o PDF), `codigo_tributacao_municipio` (3 dígitos), `codigo_nbs` (9), `codigo_interno` (20),
    `valor_recebido`, `valor_desconto_condicionado`, `valor_desconto_incondicionado`.
  - `endereco_local_prestacao`: `codigo_municipio_prestacao` (IBGE; padrão = município do prestador), `codigo_pais`.
  - **`tributos_municipais`**: `tipo_operacao` (1 tributável padrão, 2 imunidade, 3 exportação, 4 não incidência),
    `aliquota_iss` (0 a 100), `iss_retido` (boolean, padrão false), `responsavel_retencao` (2 tomador, 3 intermediário),
    `codigo_pais`, `imunidade.tipo` (0 a 5), `beneficio` (`numero` 14 dígitos, `valor_reducao`, `percentual_reducao`),
    `suspensao` (`tipo` 6 ou 7, `numero` 30 dígitos).
  - **`tributos_nacionais`**: **`cst` (obrig., PIS/COFINS)**, `tipo_retencao` (0 a 9, padrão 2), `valor_base_calculo`,
    `aliquota_pis`, `valor_pis`, `aliquota_cofins`, `valor_cofins`, `valor_inss`, `valor_ir`, `valor_csll`.
  - **`tributos_totais`**: percentuais e valores de tributos federais, estaduais, municipais e do Simples Nacional.
  - **`ibs_cbs`** (reforma tributária; **opcional no modelo por decisão do usuário**): obrigatórios dentro do bloco —
    `consumidor_final` (0/1), `codigo_fornecimento` (6 dígitos), `situacao_tributaria` (3), `classificacao_tributaria`
    (6), `regular.situacao_tributaria`, `regular.classificacao_tributaria`, `diferimento` (`percentual_ibs_uf`,
    `percentual_ibs_municipio`, `percentual_cbs`). Opcionais: `tipo_operacao`, `codigo_credito_presumido`,
    `nfse_referenciadas[].chave`, `destinatario`, `imovel`, `ressarcimento.documentos[]`.
  - **`deducoes`**: `percentual`, `valor`, `itens[]` (nfse_nacional, nfe, nfse_municipal, nf, tipo_deducao 1 a 99,
    `valor_dedutivel`, `valor_deducao`, `emitente`...).

### Mudanças já documentadas de v1 para v2

Sem `itens`. `valor_aliquota` → `servico.tributos_municipais.aliquota_iss`; `valor_base_calculo` →
`tributos_municipais.valor_base_calculo_iss`; `exigibilidade_iss` → `tributos_municipais.suspensao.tipo`;
`numero_processo` → `suspensao.numero`; `servico.codigo_municipio[_prestacao]`/`codigo_pais` →
`servico.endereco_local_prestacao.*`.

## Respostas (HTTP 200)

**Envio aceito, em processamento** — `codigo` 5023. Não é erro nem autorização: guardar a `chave` e consultar depois (ou
aguardar o webhook). **Não reemitir.**

```json
{ "sucesso": true, "codigo": 5023,
  "mensagem": "Lote em processamento, aguarde e tente novamente mais tarde.", "chave": "<50 dígitos>" }
```

**Autorizada** — `codigo` 100 (devolvida pela consulta):

```json
{ "sucesso": true, "codigo": 100, "mensagem": "Autorizado o uso da NFS-e", "status": "Autorizado",
  "numero": "10015", "rps_numero": "10015", "rps_serie": "10", "identificacao": "1581978549",
  "chave": "…", "codigo_verificacao": "82 55 B2", "data_hora_evento": "2019-08-30 11:25:22",
  "xml": "<Base64>", "pdf": "<Base64>", "link_pdf": "<url, opcional>" }
```

- `data_hora_evento` vem como `AAAA-MM-DD HH:MM:SS`, **sem fuso e sem `T`**, diferente do formato do envio.
- `codigo_verificacao` pode conter espaços. `link_pdf` é opcional (alguns provedores não o devolvem); o `pdf` Base64 sempre vem.
- `rps_numero` e `rps_serie` voltam preenchidos mesmo no layout nacional; `identificacao` é o valor enviado no envio.
- Enquanto processa, a consulta devolve `5023` de novo.

**Offline** — `codigo` 2: `{ sucesso: true, codigo: 2, mensagem, chave, data_hora_evento: null, protocolo: null, pdf: <Base64> }`.
Aceita, mas **não autorizada**: o gateway reenvia quando a prefeitura voltar. Sem XML nem protocolo. Reconciliar
com `GET /nfse/offline`.

**Erro** — `sucesso: false`: `{ codigo, mensagem, chave }`; a `chave` pode vir mesmo na falha. `5001` (JSON) e `5002`
(geração do XML) trazem `erros[]` com `campo`, `erro`, `descricao`, `detalhes`. Os exemplos de `5001` e `5002` da
documentação são idênticos (preenchimento).

## Cancelamento — `POST /v1/nfse/cancela`

| Campo | Obrig. | Regra |
|---|---|---|
| `chave` | sim | string de 50 dígitos, a chave gerada na criação |
| `codigo_cancelamento` | sim | `1` Erro de Emissão, `2` Serviço não concluído, `9` Outros |
| `motivo_cancelamento` | sim | texto de 15 a 255 caracteres |

Sucesso: `{ sucesso: true, codigo: 101, mensagem: "Homologado o cancelamento da NFS-e", data_hora_evento:
"AAAA-MM-DD HH:MM:SS", xml: <Base64>, pdf: <Base64> }`. Só se cancela nota autorizada (`5013` se não estiver; `5015` se
já cancelada, tratar como sucesso idempotente). Prazos e regras por município **não** estão documentados.

## Códigos de retorno (documentação oficial, tier C)

Decidir **sempre pelo `codigo`**, nunca pelo texto da mensagem.

| Código | Significado e ação |
|---|---|
| 100 / 101 / 103 / 135 | autorizada / cancelada / encerrada / evento registrado |
| 2 | emissão offline: aguardar, reconciliar depois |
| 5023 | em processamento: consultar depois |
| 5030 | pendente de autorização |
| 5024 / 5025 / 5033 | documento encerrado/negado / recusado pelo autorizador |
| 5028 | prefeitura rejeitou o valor (corrigir os valores) |
| 5027 | **conflito**: não reemitir; consultar ou `resolve` (risco de duplicar) |
| 5034 | autorizador sem resposta em 5 tentativas; o gateway tenta de novo em 1 h: não reenviar |
| 5001 / 5002 / 5005 / 5007 | campos inválidos / erro na geração do XML / campo obrigatório ausente / dado errado |
| 5013 / 5015 / 5006 | cancelar nota não autorizada / já cancelada / não encontrada |
| 5020 | município não atendido pelo gateway |
| 6000 a 6003 | falha de autenticação (token ausente, inválido, ambiente trocado) |
| 8000 | corpo acima de 1,2 MB |
| 8429 | excesso de requisições, bloqueio: reduzir a taxa |
| 8430 | falha ao enviar webhook |
| 9001 a 9003 / 7001 a 7099 | prefeitura devolveu XML vazio/inválido / falhas de comunicação: tentar depois ou suporte |
| 0000 | manutenção |

## Webhook (resultado da emissão)

Confirmado: método POST; responder HTTP 200 em até 3 s; no máximo 3 tentativas; um documento por chamada; XML e PDF
"compactados e em Base64" (algoritmo não dito; os exemplos são texto puro: detectar o formato pelos primeiros bytes);
**sem validação de autenticação por header**; disparado na autorização e na busca de documentos, que roda a cada 1 hora;
logs em "Gestão". A URL é registrada no cadastro do emitente (ver `docs/integranotas-softhouse.md`).
Assinatura (`signature`): pelo SDK Java, HMAC-SHA256 + AES/CBC, expira em 300 s, validada com o token do ambiente; não
confirmado para a v2. Envelope comum: `origem`, `cnpj_cpf`, `signature`, `tipo`.
**As páginas "Notificação com sucesso" e "Notificação com falha" ainda não foram lidas**: o corpo do webhook de emissão
está pendente. As páginas de DF-e (documentos e eventos, com `nsu`) tratam de distribuição/captura de documentos em que a
empresa é destinatária (`dfe_nfse`); não são o resultado da emissão própria.

## Não confirmado — perguntar ao usuário

Corpo do webhook de emissão e das opções `origem`/`tipo`/`relacao`; versão e parâmetros das rotas auxiliares (`pdf`,
`resolve`, `info`, `preview`, `busca`, `consulta`, `backup`, `offline`, `substitui`); corpo da substituição e das buscas;
quais blocos do envio são obrigatórios por si; prazos de cancelamento por município; limites numéricos de taxa.
