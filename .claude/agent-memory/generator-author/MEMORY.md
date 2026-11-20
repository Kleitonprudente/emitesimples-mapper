# Memória — generator-author (emitesimples)

Tier B. Fontes: SDK Java oficial `cloud-dfe/sdk-java` (master, lido em 2026-11-20), documentação
`integranotas.com.br/doc/nfse` e os schemas colados pelo usuário (`docs/integranotas-nfse-v2.md` para a NFS-e e
`docs/integranotas-softhouse.md` para emitente e certificado).

## Verificado
- Hosts: homologação `https://hom-api.integranotas.com.br/v2`, produção `https://api.integranotas.com.br/v2`
  (o SDK monta `URL + "v" + apiVersion`; ambiente 1 = produção, 2 = homologação).
- Header `Authorization: <token>` cru, sem `Bearer`. Também `Accept` e `Content-Type: application/json`.
- Token do Emitente é por ambiente (homologação e produção independentes). **Correção (2026-11-20)**: o cadastro de
  emitente é `POST /v1/soft/emitente` e exige o **Token da Softhouse**; então o sistema precisa dele no servidor,
  como segredo de configuração, nunca no cliente, no mapa ou em log. O cadastro devolve o Token do Emitente (JWT).
- Webhook: `signature` = Base64 de IV(16) + HMAC-SHA256(32) + AES/CBC; chave AES = 16 primeiros caracteres do token
  completados com `0`; HMAC com o token inteiro; expira em 300 s. Responder 200 em até 3 s, no máximo 3 tentativas.
- Rotas no SDK (não mudam por versão): `POST /nfse`, `POST /nfse/preview`, `GET /nfse/{chave}`,
  `GET /nfse/pdf/{chave}`, `GET /nfse/resolve/{chave}`, `POST /nfse/cancela`, `POST /nfse/substitui`,
  `POST /nfse/busca`, `POST /nfse/consulta`, `GET /nfse/info/{ibge}`, `GET /nfse/offline`, `POST /certificado`.
- Códigos: 100 autorizada, 101 cancelada, 5023 em processamento, 5027 conflito, 5034 autorizador sem resposta
  (retry automático em 1 h), 5013 cancelar nota não autorizada, 5015 já cancelada, 8429 limite de requisições,
  6000 a 6003 falha de autenticação. Decidir por `codigo`, nunca pelo texto.
- A emissão **não é simulada**: em homologação vai para o ambiente de teste real da prefeitura.

## Armadilhas do SDK (não herdar)
- Não confere status HTTP, não tem retry, devolve `null` em qualquer erro, ignora o flag `debug`.
- `writeBytes` pode corromper caracteres acentuados: enviar UTF-8 explícito.
- O `checkKey` do SDK exige `chave` de 44 dígitos e **rejeitaria a chave real de 50**. Não reaproveitar a validação do SDK.

## Confirmado pelo usuário em 2026-11-20 (rotas de homologação)
- Envio `POST /v2/nfse` → `5023` com `chave`. Consulta `GET /v1/nfse/{chave}` → `100`. Cancelamento
  `POST /v1/nfse/cancela` → `101`. **Versões diferentes por operação.**
- Cancelamento: `chave`, `codigo_cancelamento` (1 Erro de Emissão, 2 Serviço não concluído, 9 Outros),
  `motivo_cancelamento` (15 a 255 caracteres). Resposta: `data_hora_evento`, `xml`, `pdf` (Base64).
- Consulta: `data_hora_evento` em `AAAA-MM-DD HH:MM:SS`; `codigo_verificacao` com espaços; `link_pdf` opcional.
- **A `chave` da NFS-e tem 50 dígitos** (padrão nacional, após a reforma; decisão do usuário em 2026-11-20). Menções a 44
  dígitos na documentação e no SDK são do padrão antigo: ignorar. O exemplo de 37 dígitos é de preenchimento.
- Resposta offline `codigo` 2: `chave`, `data_hora_evento` e `protocolo` nulos, `pdf` Base64, sem XML — não é autorização;
  tratar como pendente e reconciliar depois (`GET /nfse/offline`). Resposta de erro pode trazer `chave`; 5001/5002 trazem `erros[]`.
- Os exemplos de resposta 5001 e 5002 da doc são idênticos (preenchimento): não servem de vetor de teste.

- Cadastro de emitente `POST /v1/soft/emitente` (Token da Softhouse); certificado em `/v1/certificado` com `certificado`
  (Base64) e `senha` (só letras e números, sem acento, 1 a 60): o cliente valida a senha antes de enviar e nunca a loga.
  Método do certificado na v1: `POST` (SDK); autenticação provável com o Token do Emitente — confirmar.

## Não confirmado — perguntar ao usuário, não presumir
- Se os caminhos `info`, `pdf` e `resolve` da v2 usam o caminho do SDK (`/{chave}`) ou parâmetro de consulta.
- Corpo da notificação do webhook, algoritmo de compactação do XML/PDF dele e validação do `signature` na v2.
- O webhook é registrado no campo `webhook` do cadastro do emitente (URL que responda 200; o gateway valida).
- O menu de webhook tem 5 páginas: teste de comunicação, **notificação com sucesso**, **notificação com falha**, documentos
  do DF-e e eventos do DF-e. As duas de DF-e se anunciam no menu para "55, 57, 67 e NFS-e" (o aviso da página lista só
  55, 57, 67), mas tratam de **distribuição**: documentos em que a empresa é destinatária, com `nsu` (captura de NFS-e,
  `documentos.dfe_nfse`). **Não** são o aviso de resultado da emissão própria: esse corpo está em "com sucesso" e "com
  falha", ainda não vistas. Envelope comum: `origem`, `cnpj_cpf`, `signature`, `tipo`; XML/PDF "em base64 compactado".
- Os exemplos da documentação de webhook são de preenchimento: `xml`/`pdf` decodificam para texto puro (`<?xml`, `%PDF-`),
  e o `signature` de exemplo é o começo do XML repetido; a doc diz 5 a 20 caracteres mas o valor real é longo. O código
  detecta o formato pelos primeiros bytes (`1f 8b` gzip, `PK` zip, senão texto) e não fixa tamanho de `signature`.

## Regras do usuário
- Nenhum agente emite nota, exceto para testar o emissor, **em homologação**. Código gerado nunca aponta
  para produção por padrão.
- Emissão sem entidade RPS: `numero` e `serie` do envio são atributos da Nota.
