# Emite Simples: front-end web

SPA (Vite + React + TypeScript strict) do emissor de NFS-e. Interface em pt-BR. Sem SSR: o app inteiro é autenticado.

Esta pasta contém a **base escrita à mão**: runtime, tokens, auth, modo mock, login, shell e perfil. As pastas
`src/gen/` e `src/features/` ficam **vazias** (`.gitkeep`) para o gerador (outro trabalho). Plano completo em
`../docs/plano-metodologia-frontend-mapper.md`.

## Como rodar

Requisitos: Node 22.12+ ou 24 (`.nvmrc` = 24; Vite 8 exige `^20.19 || >=22.12`, Vitest 5 exige `^22.12 || ^24`).

```bash
npm install
npm run dev:mock    # SO o front, sem back: dados e sessao fakes (http://localhost:5173)
npm run dev         # contra o back real: proxy /api -> http://localhost:8080
npm run build       # tsc --noEmit + vite build (modo production, VITE_APP_ENV=prod)
npm run build:staging
npm run typecheck   # tsc --noEmit
npm run lint        # oxlint
npm run test        # vitest run   (test:watch para modo interativo)
```

Ambientes (`VITE_APP_ENV`, em `.env.mock`, `.env.development`, `.env.staging`, `.env.production`): `mock | dev | staging | prod`.
Valor inválido vira `prod`. Selo MOCK/DEV/STAGING aparece no cabeçalho, no login e no `/theme` (prod não mostra).
`VITE_API_PROXY_TARGET` troca o alvo do proxy do `vite dev` (padrão `http://localhost:8080`). O front chama sempre `/api` na
mesma origem, sem CORS e sem URL absoluta no código.

### Modo mock

`npm run dev:mock`. O registry (`src/mocks/registry.ts`) intercepta **dentro de `lib/api.ts`, antes do fetch**, e devolve uma
`Response` normal; então refresh, `ApiError` e redirecionamento funcionam como em produção. Latência artificial de ~350 ms
(para ver o skeleton). `?empty=1` na URL de carga força listas vazias.

Usuários do mock (senha `senha1234`): `admin@`, `operador@`, `consulta@`, `multi@` (2 empresas: testa a escolha de empresa),
`plataforma@` (admin da plataforma sem empresa), todos `@exemplo.com.br`. `bloqueado@exemplo.com.br` sempre responde 429
(Retry-After 90). Entidades mock: `Pessoa` (37 registros, CPF/CNPJ válidos; lista, `/count`, `/{from}/{to}`, `/id/{id}`
GET/PUT/PATCH/DELETE, POST) e `Municipio` (para o Combobox de FK). O "cookie" de refresh do mock é um item de `sessionStorage`
com ids (sem PII), só para recarregar a página sem perder a sessão.

## Estrutura

```
src/
  config/      env.ts (ambiente, API_BASE, selo) · menu.ts (UNICO manifesto do menu; vira gen/screens.ts depois)
  lib/         api.ts (UNICO fetch) · auth.ts (sessao em memoria, login/logout/senha, papeis) · query-client.ts · theme.ts · utils.ts
  runtime/     À MÃO: DataTable, AutoForm, FormField, Combobox (FK), CardList, states, feedback, format, table-state, icons
  components/  ui/ (shadcn copiado, Radix) · AppShell · EnvBadge · ThemeToggle
  mocks/       registry.ts · fixtures.ts
  routes/      TanStack Router file-based: __root, login, theme (publica), _authenticated (+ index, perfil, exemplos/pessoas)
  examples/    tela de exemplo escrita à mão (so runtime): Pessoas
  gen/ features/   vazios (.gitkeep), reservados ao gerador
  index.css    tokens em 3 tiers + Tailwind v4
```

`src/routeTree.gen.ts` é gerado pelo plugin do TanStack Router a cada `dev`/`build`; não editar.

### Tokens e tema

`index.css`: Tier 1 paleta bruta (`--xx-*`), Tier 2 slots de marca (`--brand-*`, **único bloco a editar para re-tematizar**),
Tier 3 tokens shadcn (`--primary`, `--sidebar-*`, `--success`...). Claro/escuro via classe `dark` no `<html>`. Componentes só
usam tokens (nenhuma cor literal fora de `index.css`). A rota pública `/theme` mostra paleta, cores, botões, campos, DataTable
nos 4 estados, CardList, estados e toasts.

### Runtime (`src/runtime`)

- `DataTable`: paginação **no servidor**, ordenação (`sortBy`/`sortDirection`), busca com debounce, filtros facetados, skeleton, vazio
  (com a ação), vazio-com-filtro, erro com "tentar novamente". Recebe `result` (forma do hook do Query), `state` e `onStateChange`;
  `useTableState` + `toListRequest` traduzem o estado para o contrato do back.
- `AutoForm`/`FormField`: um componente para create **e** edit (`values` ativa a edição); RHF + zod; erro por campo; tipo → controle
  (texto, e-mail, senha, textarea, número, moeda, boolean/Switch, data, enum/Select, FK/Combobox). Máscaras `cpf`, `cnpj`,
  `documento`, `phone`, `postalCode` **só na apresentação** (o valor do formulário e o enviado são só dígitos). FK envia **só `{id}`**.
  Campo obrigatório vazio mostra "Campo obrigatório" (nunca texto técnico do zod; locale `pt`).
- Feedback: `notifyError`/`mutationFeedback` mostram a mensagem do `ApiError` e o código; erro inesperado vira texto genérico.

## Contrato de auth (o back é a fonte)

- `POST /api/auth/login {email, senha, empresaId?}` → 200 `{accessToken, expiresIn, usuario, empresa|null, papel|null, plataformaAdmin}`.
  O **refresh token não vem no corpo**: cookie httpOnly `refresh_token` (Secure em prod, SameSite=Strict, Path=/api/auth).
  409 `empresa_obrigatoria` com `detalhes.empresas=[{id, razaoSocial, papel}]` → o login mostra a escolha e reenvia com `empresaId`.
- `POST /api/auth/refresh` sem corpo (usa o cookie) → mesmo formato; 401 = sessão acabou → `/login`.
- `POST /api/auth/logout` (204), `GET /api/auth/me`, `POST /api/auth/senha {senhaAtual?, novaSenha}`.
- Erros: `{codigo, mensagem, detalhes?}`; 401 `credenciais_invalidas`/`sessao_invalida`, 403 `acesso_negado`, 429 `muitas_tentativas` (+`Retry-After`).

No front: o `accessToken` fica **só em memória** (`lib/api.ts`); nunca em localStorage/sessionStorage. Ao carregar a página,
`_authenticated.beforeLoad` chama `restoreSession()` (um `/auth/refresh`); sem sessão redireciona para `/login?redirect=`.
`lib/api.ts` injeta `Authorization: Bearer`, e em 401 faz **um** refresh (chamadas concorrentes compartilham a mesma requisição) e repete
a chamada **uma** vez; se falhar, limpa a sessão e o cache e vai para `/login?redirect=`. Login/refresh/logout usam `skipAuth`
(sem ciclo de refresh). QueryClient: `staleTime` 30 s, `gcTime` 60 s, sem retry em 401/403, `queryClient.clear()` no login/logout.
Papéis (`administrador > operador > consulta`) e `plataformaAdmin` (flag separada) só **escondem** itens por UX; o back decide.

Após trocar a senha em `/perfil` o front encerra a sessão local e manda para o login, porque o back revoga todos os refresh tokens.

## Como o back é consumido

Tudo passa por `api<T>(path, {method, body, query})` com base `/api` (nunca `fetch` em outro lugar).

- Lista: `GET /api/<Entidade>` devolve **array** (máx. 100, sem total), com `sortBy`, `sortDirection` e filtros por campo; `*` é curinga
  (`nome=*ana*`). Total: `GET /api/<Entidade>/count` → `{count}` (`CountResult`).
- Página: `GET /api/<Entidade>/{from}/{to}`, intervalo **inclusivo** (offset `from`, tamanho `to-from+1`, máx. 100).
- Por id: `GET|PUT|PATCH|DELETE /api/<Entidade>/id/{id}`; criação `POST /api/<Entidade>`.

## Testes

`npm run test` (vitest + Testing Library, jsdom): `lib/api` (Bearer, renovação com fila, falha vai para o login, ApiError, 429, rede),
`format`, `AutoForm` (erro por campo, create/edit, máscara, FK só `{id}`, erro do servidor), `DataTable` (skeleton, vazio, erro,
ordenação, paginação) e `toListRequest`, registry do mock (contrato de auth e Pessoa), menu por papel.
