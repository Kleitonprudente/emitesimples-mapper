# Emite Simples

Emissor de **NFS-e** para MEI e ME, integrado ao **IntegraNotas**. O núcleo do projeto é o **mapa do Mapper Idea**
(modelo de negócio, geradores e telas); o código do back e do front é gerado a partir dele, mais o que é escrito à mão
(segurança, integrações, runtime do front).

> **Estado:** a emissão funciona de ponta a ponta contra um gateway simulado e já foi exercitada em **homologação** do
> IntegraNotas. **Produção está bloqueada por padrão.** Cancelamento, e-mail da nota e substituição ainda não existem.

## O que já existe

| Área | O que faz |
|---|---|
| **Cadastros** | Clientes e Fornecedores (a mesma entidade `Pessoa`): documento primeiro, consulta de **CNPJ** (CNPJ.ws) e de **CEP**, lista de contatos com os sócios do CNPJ. Serviços. Municípios (importação do CSV do IBGE). |
| **Configuração** | **NFS-e:** habilitar a emissão, série e último número; cadastra o emitente no IntegraNotas. **Empresa:** dados da empresa e envio do certificado digital A1 (`.pfx`). |
| **Notas Fiscais** | **NFS-e:** lista, nova nota (cliente, serviço, valor), envio, acompanhamento da autorização, correção e reenvio mantendo o número, PDF. |
| **Administração** (plataforma) | Empresas, usuários e vínculos, consulta de CNPJ, **Integração IntegraNotas** (token da softhouse por ambiente), importação de municípios, guia de estilo (`/admin/theme`). |

## Estrutura

```
back/
  mapas/        mapa de negócio (projeto `emitesimples` no CLI mi) e geradores Quarkus
  api/          projeto Quarkus: código gerado + o escrito à mão (auth, cofre, CNPJ, NFS-e, webhook)
front/
  mapas/        mapa das telas e geradores do front (projeto `emitesimples-front`); domínio copiado do back
  web/          app Vite + React + TypeScript (shadcn/ui, TanStack): runtime à mão + telas geradas
  docs/         padrões de UI e catálogo de componentes
docs/           histórias de usuário, documentos do IntegraNotas, guias de modelagem e de geradores
scripts/        hooks usados pelos agentes (limitam a escrita ao projeto)
.claude/        agentes (domain-mapper, generator-author, cli-runner) e skills (mapperidea, padroes-ui)
```

## Pré-requisitos

- **JDK 21 ou 25** e **Maven 3.9+** (o `pom.xml` compila com `release 21`).
- **Node 24** e npm.
- **Docker** (banco de dev e testes do back).
- O CLI **`mi`** no **Git Bash** (no PowerShell, `mi` é um alias de `Move-Item`), só para gerar a partir dos mapas.

## Rodar em desenvolvimento

```sh
# 1) banco persistente de dev (Postgres 18 na porta 5433, volume emitesimples-dev-pgdata)
cd back/api && docker compose -f dev/compose.yaml up -d --wait

# 2) back (porta 8080)  — o Flyway migra o banco e cria o administrador de dev
mvn quarkus:dev

# 3) front (porta 5173; o proxy /api aponta para o back)
cd front/web && npm install && npm run dev        # ou: npm run dev:mock  (sem back, dados simulados)
```

- Administrador da plataforma de **dev**: `plataforma@exemplo.com.br` / `senha1234` (só no perfil dev). No modo mock
  existem também `admin@exemplo.com.br` (administrador de empresa) e outros, todos com a senha `senha1234`.
- O usuário da plataforma **não tem empresa**: ele vê a Administração, mas não os cadastros nem as notas. Para esses,
  crie uma empresa e um usuário vinculado em Administração.
- Desligar o banco mantendo os dados: `docker compose -f dev/compose.yaml down`. Apagar tudo: `down -v`.

## Testes e verificações

```sh
# back (precisa do Docker; o banco de teste é efêmero)
cd back/api && mvn -B test

# front
cd front/web
npx tsc -b && npx oxlint && npx vitest run && npm run verificar:ui && npm run build
```

> No Windows, rode o **Vitest** a partir do PowerShell com o diretório em `C:\` (maiúsculo). No Git Bash com `c:`
> minúsculo ele falha com "failed to find the current suite".
> Não rode o Vitest da raiz do repositório: ele cria `node_modules/` de cache (está no `.gitignore`).

`npm run verificar:ui` confere o padrão de UI do projeto (sem componente visual novo, imports permitidos, sem cor fixa,
formulários com linhas somando 12). Roda também no fim do `gerar.sh` do front.

## Gerar código a partir dos mapas

O `mi generate` lê o mapa **da nuvem**, então só vale depois de um `mi push`. **O `mi push` publica na conta em nuvem do
Mapper Idea e só deve ser feito com autorização explícita.**

```sh
# back (Git Bash)
cd back/api && ./scripts/gerar.sh                 # gera Java + OpenAPI; NÃO faz push; falha se achar marcador de erro

# front (Git Bash; desligue o `npm run dev` antes: o Vite sobrescreve rotas geradas)
cd front/mapas && ./scripts/sincronizar-dominio.sh   # copia o domínio do back para o front
mi push emitesimples-front                           # só com autorização
./scripts/gerar.sh                                   # gera telas; roda o verificador de UI no fim
```

- **Nunca edite código gerado** (`back/api/.../{configuracao,cadastros,usuarios,nfse,integranotas,admin}`, `front/web/src/gen`,
  `src/features/cadastros`, `src/features/configuracao`, rotas geradas): ele é sobrescrito por inteiro. Corrija o mapa ou o gerador.
- O que é escrito à mão no back vive em pacotes que a geração nunca apaga (`auth`, `security`, `cep`, `plataforma`,
  `gateway`, `nfseconfig`, `nfseemissao`, `exceptions`, `results`, `tenant`).

## Padrões de UI

Cadastro = lista + **um** formulário em página única (`/x/novo` e `/x/$id`), grade de 12 colunas, só componentes
catalogados. **Não se criam componentes visuais novos.** Leia `front/docs/padroes-de-ui.md` e
`front/docs/catalogo-componentes.md`; a vitrine viva está em `/admin/theme`.

## Integração com o IntegraNotas

- **Homologação** (`https://hom-api.integranotas.com.br`) por padrão. **Produção está bloqueada** (`409`) a menos que o
  servidor a libere explicitamente. Nunca emita em produção para testar.
- Tokens (softhouse, emitente) e o certificado ficam **cifrados no cofre** (AES-256-GCM), nunca em entidade, resposta ou log.
- O corpo do envio v2 é validado de forma estrita pelo gateway; os ajustes vão num ponto só (`NfsePayloadMapper`).
  Já confirmado em homologação: `data_competencia` com data, hora e fuso; `regime_apuracao` e
  `tributos_totais.percentual_tributos_simples_nacional` para o Simples Nacional.
- O que ainda está **a confirmar** (rotas da lista de emitentes e do webhook, assinatura do webhook, formato do certificado)
  está listado em `back/api/README.md` e em `docs/integranotas-*.md`.

## Produção: o que configurar

Nada de dev serve em produção. Defina por variável de ambiente (valores de dev estão no `application.properties`
**só** para os perfis dev e test):

`EMITESIMPLES_SEGREDOS_CHAVE` (chave-mestra do cofre; `openssl rand -base64 32`), `EMITESIMPLES_JWT_PRIVATE_KEY` e
`EMITESIMPLES_JWT_PUBLIC_KEY`, `QUARKUS_DATASOURCE_JDBC_URL/USERNAME/PASSWORD` (role comum, com RLS),
`QUARKUS_FLYWAY_USERNAME/PASSWORD` (dono das tabelas), `EMITESIMPLES_ADMIN_EMAIL` e `EMITESIMPLES_ADMIN_SENHA`
(administrador inicial) e `EMITESIMPLES_COOKIE_SECURE=true`. Detalhes em `back/api/README.md`.

## Branches

| Branch | Para quê |
|---|---|
| `main` | estável: o que foi validado e pode virar produção |
| `qa` | homologação/testes: recebe o trabalho novo antes de ir para a `main` |

Fluxo sugerido: `feature/*` → PR para `qa` → validação em homologação → PR de `qa` para `main`.

## Regras do Mapper Idea

- **O back e o front são projetos do Mapper Idea distintos.** A *home* de cada um é a pasta do seu `main.mi`
  (`back/mapas/` e `front/mapas/`). O `mi push` envia tudo o que está na home, então os dois **nunca dividem pasta**.
- **O CLI guarda o caminho absoluto da home** em `~/.mapperidea/<projeto>/config.js`. Se a pasta for movida, registre de
  novo com `mi init <projeto> main.mi` (dentro da nova pasta). **O `mi init` apaga o `lastPush`**, que liga ao que está na
  nuvem: depois dele `struct` e `generate` falham até um novo `mi push`. O `check` e o `load` imprimem sucesso mesmo assim,
  então o sucesso deles não prova nada.
- A pasta `.freemapper/` guarda o estado de sincronização do Mapper e **fica versionada**.

## Documentação

- `docs/` — histórias de usuário (`historias-*.md`), IntegraNotas (`integranotas-*.md`), modelagem
  (`domain-modeling-guide.md`), geradores (`generator-authoring-guide.md`, `pack-quarkus-contrato.md`).
- `back/api/README.md` — API, segurança (RLS, cofre, matriz de acesso), contratos de cada endpoint.
- `front/mapas/README.md` — mapa das telas, propriedades suportadas pelos geradores, regras impostas.
- `front/docs/` — padrões de UI e catálogo de componentes.
