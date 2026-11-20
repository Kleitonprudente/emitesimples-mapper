# Memória — domain-mapper (emitesimples)

Tier B. Fontes principais: `docs/integranotas-nfse-v2.md` (NFS-e: envio v2, consulta e cancelamento v1) e
`docs/integranotas-softhouse.md` (cadastro de emitente e certificado digital), colados pelo usuário em 2026-11-20.

## Decisões do usuário (2026-11-20)
- Pacote base `br.com.emitesimples`. Nome do projeto no CLI: `emitesimples`. Pasta dos mapas: `mapas/`.
- Contextos, nesta ordem: Cadastro (Emitente, Tomador, Servico, Municipio) → Emissao (Nota, Cancelamento) →
  Integracao (gateway, webhook, certificado).
- Emitente = empresa usuária. Tomador = cadastro de pessoa. Municipio nasce com IBGE. Sem entidade RPS.
- Stack alvo: Quarkus (Java 21).

## O que a API v2 exige no envio (resumo; detalhe no doc)
- Raiz obrigatória: `data_emissao`, `numero` (1 a 9 dígitos), `serie` (1 a 5 dígitos).
- Tomador: `razao_social` e endereço (`logradouro`, `numero`, `bairro`, `uf`); `cnpj` ou `cpf`.
- Serviço: `codigo` (6 dígitos, LC 116), `discriminacao` (até 2000), `valor_servicos` (mínimo 0.01).
- `tributos_nacionais.cst` e o bloco `ibs_cbs` têm campos obrigatórios; se os blocos são obrigatórios por si, o schema
  não diz: confirmar em homologação antes de tornar opcional no mapa.
- O prestador não vai no corpo: vem do token.
- `identificacao` (1 a 40) é a chave de correlação do sistema emissor: guardar na Nota.
- Percentuais em pontos (5.00 = 5%). Quebra de linha só `\n`.

## Retorno da API que o mapa precisa comportar
`chave`, `numero`, `codigo_verificacao`, `data_hora_evento`, `status`, `xml`, `pdf`, `link_pdf` (às vezes ausente),
e `rps_numero`/`rps_serie` (atributos opcionais da Nota, não entidade).

## Cancelamento e ciclo da nota (confirmado pelo usuário em 2026-11-20)
- Envio `POST /v2/nfse` devolve `5023` (em processamento) com a `chave`; consulta `GET /v1/nfse/{chave}` devolve `100`
  (autorizada); cancelamento `POST /v1/nfse/cancela` devolve `101`. A versão da API varia por operação.
- Cancelamento exige `chave`, `codigo_cancelamento` (1 Erro de Emissão, 2 Serviço não concluído, 9 Outros) e
  `motivo_cancelamento` (15 a 255 caracteres). Devolve `data_hora_evento`, `xml` e `pdf` (Base64).
- Estados da Nota a comportar: processando, emitida offline (código 2: aceita, aguardando a prefeitura, sem XML nem
  protocolo), autorizada, cancelada, rejeitada, em conflito. Erro de campo (5001/5002) traz `erros[]` (campo, erro,
  descrição, detalhes); a `chave` pode vir mesmo em falha. Gravar a `chave` no primeiro retorno.
- `data_hora_evento` do retorno: `AAAA-MM-DD HH:MM:SS`, sem fuso. `link_pdf` é opcional. A `chave` tem 50 dígitos (padrão nacional); ignore menções a 44.

## Emitente (confirmado pelo usuário em 2026-11-20; detalhe no doc)
- Cadastro: `POST /v1/soft/emitente`, com o Token da Softhouse. Devolve o Token do Emitente (JWT), por ambiente.
- Obrigatórios: nome fantasia, razão social, regime tributário (1 Simples, 2 Simples excesso de sublimite, 3 Normal,
  4 MEI), telefone, endereço completo com `cmun` (IBGE) e CEP. Identificação por CNPJ ou CPF.
- Opcionais úteis: apelido, inscrição municipal, e-mail, logo, regime do IBS/CBS, URL do webhook.
- Segredos que o emitente carrega e que **não viram atributo**, só referência ao cofre: Token do Emitente, credenciais da
  prefeitura (login, senha, client id, client secret) e a senha do certificado. O Token da Softhouse é config do sistema.
- O webhook é registrado no cadastro; a URL precisa responder 200 para ser aceita.

## Certificado e decisões (2026-11-20)
- `/v1/certificado` recebe `certificado` (Base64) e `senha` (1 a 60; só letras e números; sem acento). Erros 5003, 5011, 5029.
  Mapa: só metadados do certificado (validade, situação); arquivo e senha são referência ao cofre.
- Emitente por CNPJ **ou** CPF. `regime_cibs` e o bloco `ibs_cbs` do envio: opcionais por ora.

## Não confirmado
Corpo da notificação do webhook, compactação do XML/PDF dele; rotas auxiliares da v2; quais blocos do envio são obrigatórios por si; prazos de cancelamento por município.

## Regras
- Ambiente (homologação ou produção) é campo de controle. Segredos (token, senha do certificado) nunca em entidade.
- Nenhum agente emite nota, exceto para testar, em homologação.
