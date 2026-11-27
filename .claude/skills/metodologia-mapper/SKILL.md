---
name: metodologia-mapper
description: Fluxo obrigatório do Emite Simples para QUALQUER pedido de funcionalidade, função, alteração, correção ou campo novo (back ou front). Conduz o pedido pela metodologia Mapper Idea - documentação, mapa (.mi), gerador (.mi), geração e verificação - em vez de escrever código direto. Use sempre que o usuário pedir "cria", "adiciona", "altera", "muda", "corrige", "nova tela", "novo campo", "nova regra", "nova entidade" ou qualquer mudança de comportamento no sistema.
when_to_use: Em todo pedido que muda o que o sistema faz ou mostra (entidade, atributo, enum, relação, regra, endpoint, tela, janela, formulário, lista, papel, migração). Também quando o usuário disser /metodologia-mapper, /mapper ou "use a metodologia do mapper". Não se aplica a perguntas só de leitura/explicação.
argument-hint: [descrição do pedido]
allowed-tools: Read Glob Grep Edit Write Bash Skill Agent AskUserQuestion
---

# Metodologia Mapper no Emite Simples

Todo pedido de mudança passa por esta esteira, **nesta ordem de autoridade**:

**documentação → mapa de negócio → gerador (mapa de arquitetura) → código gerado → código à mão (só onde é permitido)**

Código gerado nunca é o ponto de partida nem o ponto de conserto. Se a saída está errada, o erro está acima dela.

## Passo 0 - Classifique o pedido (antes de tocar em qualquer arquivo)

Leia o pedido e diga ao usuário, em uma ou duas linhas, em qual camada ele mora:

| O pedido muda...                                              | Camada onde se trabalha                                        |
|---------------------------------------------------------------|----------------------------------------------------------------|
| regra de negócio, história, comportamento esperado            | `docs/historias-*.md` (e os guias, se a regra for de método)   |
| entidade, atributo, enum, relação, `required`, `cn`, valores  | mapa de negócio `back/mapas/<Contexto>/*.mi` (+ `main.mi`)     |
| o *formato* do código gerado (Java, OpenAPI, zod, api client, tela) | gerador `back/mapas/generators/quarkus/*.mi` ou `front/mapas/generators/frontend/*` |
| tela, lista, formulário, filtro, ação, papel da janela        | mapa de janela `front/mapas/window/<contexto>/<feature>/*.mi`  |
| integração externa, segurança, tenant, emissão, gateway       | código à mão nos pacotes preservados (ver abaixo)              |
| esquema do banco                                              | nova migração Flyway `V<n+1>__*.sql` (nunca alterar uma aplicada) |

Um pedido costuma atravessar várias camadas (ex.: campo novo = história + mapa + migração + janela). Liste todas.

## Passo 1 - Documentação primeiro

1. Procure a regra em `docs/` e `front/docs/` (histórias, `domain-modeling-guide.md`, `generator-authoring-guide.md`,
   `padroes-de-ui.md`, `catalogo-componentes.md`, `integranotas-*.md`).
2. Se a regra não está documentada, ou está diferente do pedido: proponha o texto da história/regra e **confirme com o
   usuário** antes de mapear.
3. Se dois documentos divergem, ou doc × mapa × gerador × código divergem: **relate a divergência** (o que diz cada um)
   e pergunte. Nunca resolva em silêncio.

## Passo 2 - Mapa de negócio (`back/mapas`)

- Carregue a skill `mapperidea` (sintaxe `.mi`) e siga `docs/domain-modeling-guide.md` (fases, convenções CN-REL-001,
  CN-REL-002, CN-003 com `[e] cn`, matriz de ícones de relenvio, metadados completos, registro no `main.mi`).
- Para modelagem maior (entidade nova, contexto novo, várias histórias), delegue ao agente `domain-mapper` **só se o
  usuário pedir subagente**; senão faça você mesmo seguindo o guia.
- Enum: formato da seção 9.7 do guia (valores em MAIÚSCULAS, descrição longa como rótulo).
- O back é a fonte da verdade do domínio. Depois de mexer, sincronize o front:
  `front/mapas/scripts/sincronizar-dominio.sh` e confira com `--verificar`. Se surgiu/sumiu `[b]`, ajuste também
  `front/mapas/main.mi` à mão.

## Passo 3 - Gerador (só se o formato da saída precisa mudar)

- Siga `docs/generator-authoring-guide.md`. **Regra zero: struct-first** - olhe o DOM antes de escrever `match`:
  `mi generate <projeto> struct xml className=<Entidade> packageName=br.com.emitesimples.domain.<ctx>`.
- Ramifique pelo dicionário (nunca por literal), `match` mutuamente exclusivos, template TODO-on-unhandled por último.
- Não invente propriedade, função de dialeto nem padrão que o guia não documente. Se o gerador não consegue produzir o
  que o mapa pede: **pare e pergunte**.
- Para trabalho grande de gerador, o agente `generator-author` existe (de novo: só se o usuário pedir subagente).

## Passo 4 - Telas (front)

- Carregue a skill `padroes-ui`. Leia `front/docs/padroes-de-ui.md` e use **só** componentes de
  `front/docs/catalogo-componentes.md`.
- Tela nasce da janela `.mi` em `front/mapas/window/...` + registro em `front/mapas/main.mi` + entrada no
  `front/mapas/scripts/gerar.sh` (funções `dominio` / `telas`).
- Cadastro = lista + formulário único (`/x/$id`, `novo` cria); todo `field` de `row` tem `size`; nunca excluir, só desativar.

## Passo 5 - Publicar e gerar (com autorização)

- `mi` **só no Git Bash** (no PowerShell é `Move-Item`).
- `mi push emitesimples` / `mi push emitesimples-front`: **só com a frase de autorização explícita do usuário, um push
  por autorização.** Peça e espere. `mi check`/`mi load` não provam nada; a prova é o `struct`.
- Back: `back/api/scripts/gerar.sh`. Front: pare o Vite, depois `front/mapas/scripts/gerar.sh`.
- Confira a saída: os scripts acusam `EMI####`, `ERRO NO MODELO`, `TRUNCADA`, `{{`, arquivo vazio. Erro na saída =
  volte ao mapa ou ao gerador; **nunca** corrija o arquivo gerado.

## Passo 6 - Código à mão (só onde é permitido)

- Back: `auth, security, cep, plataforma, gateway, nfseconfig, nfseemissao, financeiroop, exceptions, results, tenant`.
- Front: `runtime/`, `lib/`, `mocks/`, `features/notas`, `features/admin`, `features/empresa-config` e telas sem o
  cabeçalho `GERADO por`.
- Banco: nova migração Flyway (`back/api/src/main/resources/db/migration`), numerada depois da última.
- **Proibido editar**: `back/api/src/main/java/br/com/emitesimples/{configuracao,cadastros,usuarios,nfse,integranotas,admin,financeiro}/**`,
  `back/api/docs/openapi/**`, `front/web/src/gen/**`, `routeTree.gen.ts`, arquivos `GERADO por`, `front/mapas/dominio/**`.
  O hook `scripts/bloquear-codigo-gerado.sh` bloqueia; não contorne (o escape é só do usuário).

## Passo 7 - Verificar antes de entregar

- Back: `mvn -B test` (em `back/api`).
- Front (PowerShell, diretório com `C:\` maiúsculo, dentro de `front/web`, nunca da raiz):
  `npx tsc -b; npx oxlint; npx vitest run; npm run verificar:ui`.
- Testes novos e `docs/homologacao` ficam só no local (estão no `.gitignore`).
- Relate o resultado real (falhou = mostre a saída).

## Quando PARAR e PERGUNTAR (com opções e sua recomendação)

- O guia não cobre o caso; dois documentos divergem; o gerador não produz o que o mapa pede.
- A decisão muda contrato: API, banco, valores gravados de enum, papéis.
- Há mais de um caminho razoável (ex.: resolver no mapa × no gerador × à mão).
- Precisa de `mi push`, commit, push ao GitHub ou nota em homologação.

## Nunca

- Editar ou "ajustar" código gerado, nem desfazer saída para esconder divergência.
- Criar componente visual fora do catálogo.
- Emitir/cancelar nota real (só homologação, e só a pedido). Tocar containers `erp-*`. Apagar empresa (só desativar).
  Segredo em entidade, resposta ou log.
- Commit/push sem pedido do usuário.

## Formato da resposta ao usuário

Ao receber o pedido, antes de editar, apresente em poucas linhas:

1. **Camadas afetadas** (doc / mapa / gerador / janela / à mão / migração) e os arquivos.
2. **Dúvidas ou divergências** encontradas (se houver, pergunte e espere).
3. **Plano** na ordem da esteira, marcando os passos que exigem autorização (push, gerar com Vite parado).

Depois execute camada por camada e, ao final, informe o que foi gerado, o que foi escrito à mão e o resultado dos testes.
