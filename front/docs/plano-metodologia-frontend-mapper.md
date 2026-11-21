# Plano — Frontend gerado a partir de mapas Mapper Idea (schema-first + runtime à mão)

> **Para quem é:** um projeto que já tem o skill do Mapper Idea configurado (`mi push`, `mi generate`,
> `struct`, mapas de domínio `[b]` funcionando) e quer construir o frontend com a mesma metodologia.
> Este documento é **autocontido**: não depende de nenhum outro repositório. Onde aparecer `<projeto>`,
> `<org>`, `<Entidade>` ou `<Tela>`, troque pelos nomes do seu projeto.
>
> **Como usar:** leia tudo, depois compare com o estado atual do projeto (stack, backend, auth, design
> system) e diga o que se aplica direto, o que precisa adaptar e o que não se aplica, **antes** de
> escrever código. Depois execute as fases da §10 em ordem, com o critério de aceite de cada uma.

---

## 1. Princípio central — inverter a proporção gerado × runtime

O erro clássico de geração de front é **gerar código gordo**: um arquivo por operação CRUD, CSS e lógica de
renderização repetidos em cada tela, rotas e menu mantidos em listas paralelas. Quando o create e o update
divergem, ou quando alguém edita um arquivo gerado, a regeneração vira um problema.

A inversão:

```
ANTIGO:  mapa → gerador → muitos arquivos gordos por tela  (lógica de render DENTRO do gerado, duplicada)
NOVO:    mapa → gerador → ~3 artefatos finos por tela      (lógica de render na LIB DE RUNTIME, única)
```

- A **biblioteca de runtime** (`src/runtime/`) é escrita **à mão, uma vez**: tabela, formulário, estados,
  wizard. É onde mora a qualidade.
- O **gerador** emite artefatos **finos, legíveis e declarativos** que só *compõem* o runtime.
- Um único `<AutoForm>` atende create **e** edit, então a divergência entre eles deixa de existir.

**Regra de ouro:** arquivo gerado **nunca** se edita à mão. A customização acontece por três caminhos, nesta
ordem de preferência:
1. evoluir o **runtime** (beneficia todas as telas);
2. ajustar o **mapa de tela** (override declarativo: label, máscara, ordem, componente);
3. **ejetar** a tela: copiar o arquivo, assumi-lo à mão e marcar no mapa para o gerador parar de emiti-la.

Nó do mapa que o gerador ainda não trata vira `// @TODO <caminho>` na saída. O gerador **nunca quebra**, e as
lacunas ficam autodocumentadas.

---

## 2. Stack alvo

| Peça | Escolha | Por quê |
|---|---|---|
| Build/SPA | **Vite + React + TypeScript**, sem SSR | app 100% autenticado, sem SEO; menos superfície de servidor no front |
| Componentes | **shadcn/ui** (Radix + Tailwind v4), copiados para `components/ui/` | código no repo, auditável, acessível |
| Rotas | **TanStack Router** file-based, `beforeLoad` nas rotas protegidas | type-safe; valida sessão antes de montar |
| Estado de servidor | **TanStack Query** | cache/loading/erro/lifecycle; nada de Redux nem subscribe manual |
| Schema/forms | **Zod** (tipo via `z.infer`) + **React Hook Form** + `zodResolver` | uma fonte de validação, erro por campo |
| Tabela | **TanStack Table** | dentro do `<DataTable>` do runtime |
| Extras | `lucide-react` (ícones), `sonner` (toast), `oxlint`, `vitest` | |

Estado de UI local fica em `useState`/`useReducer` (ou Zustand, pontualmente). Atenção: **Vite 8 exige Node 20+**.
Fixe a versão em `.nvmrc`.

### 2.1 Segurança (o front não é fronteira de segurança)

Se o backend permitir, use o padrão **BFF**: o backend é o cliente OIDC confidencial e entrega ao navegador só
um **cookie httpOnly + Secure + SameSite=Strict**, então o JS nunca vê token.

- SPA e API na **mesma origem** (SPA em `/`, API em `/api/*`): sem CORS. CSRF ligado nas mutações.
- **Um** wrapper `src/lib/api.ts`: `credentials: 'include'`; 401 redireciona para o login do BFF com
  `?redirect=`; erro lança com `status`. Nenhuma URL hardcoded em lugar nenhum.
- Papéis vêm de `GET /api/auth/me` (`{nome, papeis, permissoes}`). O front esconde rota e botão **só por UX**;
  a autorização real é do backend.
- Dado pessoal: `staleTime`/`gcTime` curtos no QueryClient (ex.: 30s/60s), sem retry em 401/403,
  `queryClient.clear()` no logout, sem PII em `localStorage` nem em logs/telemetria, e mascaramento de
  preferência já na resposta da API.

Se o projeto usar outro modelo de auth, mantenha a regra: **todo acesso passa por `lib/api.ts`**. Isso é o que
permite o modo mock (§9) e trocar a auth sem tocar em tela.

---

## 3. Estrutura de pastas

```
frontend/src/
├── config/        env.ts (mock|dev|staging|prod), brand.ts (marca ativa)
├── lib/           api.ts, auth.ts, query-client.ts
├── runtime/       ✋ À MÃO — DataTable, AutoForm/FormField, CardList, OperationFlow, states, format
├── gen/           🤖 GERADO do domínio — schemas/<entidade>.ts, api/<entidade>.ts, screens.ts (manifesto)
├── features/      🤖 GERADO das telas — features/<contexto>/<entidade>/...   (+ telas ejetadas/custom)
├── components/ui/ shadcn (copiados)
├── mocks/         registry + fixtures + sessão fake
├── routes/        TanStack Router (file-based; _authenticated.tsx protege)
└── index.css      tokens (3 tiers) + Tailwind
```

Mapas no lado Mapper Idea:

```
mi/
├── main.mi                          # registra [p] domain, [p] window e os geradores
├── <contexto>/<Entidade>.mi         # mapas de DOMÍNIO (já existem)
├── window/<contexto>/<entidade>/<Tela>.mi   # mapas de TELA (novos)
└── generators/frontend/*.mi         # geradores de front (novos)
```

O caminho de pacotes do mapa de tela vira 1:1 a pasta gerada: `mi/window/vendas/pedido/GestaoPedidos.mi`
gera em `features/vendas/pedido/`. Assim a navegação mapa ↔ código é direta.

---

## 4. Modelo de 3 camadas

| Camada | O quê | Onde vive | Dono |
|---|---|---|---|
| **1 — Estrutura** | telas, campos ligados ao domínio, filtros, ações, navegação, estados | mapas de tela `.mi` | negócio/UX |
| **2 — Tokens/tema** | cores, raio, fontes, claro/escuro, marca | `index.css` (CSS vars) | designer |
| **3 — Tipo → componente** | qual controle shadcn renderiza cada tipo/enum/FK | `<FormField>` (runtime), guiado por um mapa de config | designer + dev |

Um mapa de tela **não** carrega estilo nem lógica imperativa: só estrutura, bind ao domínio e
interação/navegação.

---

## 5. Dialeto de mapa de tela (camada 1)

### 5.1 Convenção de nomes
- **Estrutura/layout em inglês:** `source`, `object`, `title`, `route`, `role`, `icon`, `columns`, `field`,
  `search`, `filters`, `filter`, `actions`, `action`, `form`, `rows`, `row`, `cols`, `col`, `emptyState`,
  `message`, `menu`, `group`, `item`, `screen`, `steps`, `step`; metadados `display`, `mask`, `sortable`,
  `component`, `readOnly`, `required`, `width`, `type`, `opens`, `navigate`, `condition`, `label`.
- **Negócio no idioma do domínio:** nome da tela (`GestaoPedidos`), bindings (`.numero`, `.status`),
  textos de `title`/`label`/`message`, papéis e valores de enum.

`[e] field [v] .status` se lê assim: `field` é estrutura e `.status` é negócio.

### 5.2 Estereótipos (ícones `Descriptor.window.*`)
O texto após `Descriptor.` vira o `@mode` da classe no DOM. Use os ícones que já existem; só crie um novo se
for necessário, seguindo o padrão `Descriptor.window.<x>`.

| Ícone | Uso | Regiões |
|---|---|---|
| `window.list` | lista/grade (ou cards, via `@ presentation cards`) | `columns`, `search`, `filters`, `actions`, `emptyState` |
| `window.dialog` | formulário modal | `form`, `actions` |
| `window.editor` | formulário em página (por id se a rota tem `$id`) | `form`, `actions` |
| `window.operation` + `step` | wizard multi-etapa | `master`, `steps` |
| `window.custom` | tela fora do padrão gerável (escrita à mão) / shell com `menu` | — |
| `window.report`, `window.iframe` | futuro (relatório, master-detail) | — |

### 5.3 Anatomia
```
[Descriptor.window.list] GestaoPedidos
    @
        [e] title
            [e] pt
                [v] Gestão de Pedidos
        [e] route
            [v] /pedidos
        [e] role
            [v] operador                 # papel (só UX)
        [e] icon
            [v] package                  # nome lucide
        [e] group
            [v] Vendas                   # grupo no menu
        [e] order
            [v] 10                       # ordem no menu
        [e] usedIn                       # opcional; ausente = todas as marcas (§8)
            [v] marcaA
    [g] source
        [r] object: Pedido()             # BIND: o [b] de domínio que a tela opera
    [e] columns
        [e] field
            [v] .numero
        [e] field
            [v] .documentoCliente
            @
                [e] mask
                    [v] cpf
                [e] sortable
                    [v] false
        [e] field
            [v] .status
            @
                [e] display
                    [v] badge
        [e] field
            [v] .dataCriacao
            @
                [e] display
                    [v] date
                [e] label
                    [e] pt
                        [v] Criado em
    [e] search
        @
            [e] placeholder
                [e] pt
                    [v] Buscar por número…
        [e] field
            [v] .numero
    [e] filters
        [e] filter
            [v] .status
            @
                [e] type
                    [v] faceted          # opções vêm do enum do domínio
    [e] actions
        [e] action
            [v] criarPedido
            @
                [e] type
                    [v] primary          # primary | secondary | row
                [e] label
                    [e] pt
                        [v] Novo pedido
                [e] icon
                    [v] plus
                [e] opens
                    [v] CriarPedido      # primary → abre editor SEM id (criação)
        [e] action
            [v] editarPedido
            @
                [e] type
                    [v] row
                [e] label
                    [e] pt
                        [v] Editar
                [e] opens
                    [v] EditarPedido     # row → abre editor COM o {id} da linha
        [e] action
            [v] cancelar
            @
                [e] type
                    [v] row
                [e] condition
                    [v] status != 'CANCELADO'   # declarativo; controla disabled/visível
                [e] navigate
                    [v] /pedidos/{id}/cancelar
    [e] emptyState
        @
            [e] message
                [e] pt
                    [v] Nenhum pedido ainda.
            [e] action
                [v] criarPedido          # referencia uma action declarada
```

Formulário (`window.dialog` / `window.editor`):
```
[Descriptor.window.dialog] CriarPedido
    @
        [e] title
            [e] pt
                [v] Novo pedido
    [g] source
        [r] object: Pedido()
    [e] form
        [e] rows
            [e] field
                [v] .cliente             # field direto em rows → linha inteira
            [e] row                      # 2+ campos na MESMA linha
                [e] field
                    [v] .documentoCliente
                    @
                        [e] mask
                            [v] cpf
                        [e] width
                            [v] 60       # peso de proporção (opcional)
                [e] field
                    [v] .tipo
            [e] field
                [v] .observacao
    [e] actions
        [e] action
            [v] salvar
            @
                [e] type
                    [v] primary
```

### 5.4 Regras de binding e metadados
- `field` referencia atributos do bean `object` por **caminho com ponto inicial** (`.nome`; `.relacao.campo`
  fica para o futuro). A resolução é **por nome contra o mapa de domínio**: o gerador puxa tipo, `values`
  (enum), `required`, `title/pt` e o tamanho de `Texto(n)`.
- **Enums e rótulos nunca se redeclaram na tela.** Vêm do schema gerado (`<attr>Values`/`<attr>Labels`).
- `display`: `text` | `badge` | `currency` | `date` | `dateTime`. `mask`: `cpf` | `cnpj` | `phone` | `postalCode`
  (o dado trafega cru; a máscara é só de apresentação). `component` força o controle (camada 3).
- **O subconjunto editável é decisão da tela, não do domínio**: o form lista os campos explicitamente.
  ⚠️ Não reuse `@ readOnly` do domínio para isso se os geradores de backend já usam `readOnly` para excluir
  campos de query/filtro/escrita. Se precisar marcar campos de sistema, crie um flag novo (ex.: `@ system`).

### 5.5 Composição: lista × editor
- **Modelo A (padrão): telas separadas.** A `window.list` abre uma `window.editor`/`window.dialog` separada via
  `opens`, passando o `{id}` em ações `row` e sem id em ações `primary`. O editor é reutilizável (página ou
  popup, de qualquer origem; uma lista pode ter vários editores).
- **Modelo B: combinada.** Uma `window.editor` com `columns` **e** `form`, para CRUD simples.

### 5.6 Wizard — `window.operation`
```
[Descriptor.window.operation] Ativacao
    @
        [e] title
            [e] pt
                [v] Ativação
        [e] route
            [v] /ativacao
    [g] master
        [r] object: Conta()              # agregado que o fluxo constrói
    [e] steps
        [e] step
            [v] plano
            @
                [e] opens
                    [v] EscolherPlano    # REUSA tela existente por referência (embute o <XForm> dela)
        [e] step
            [v] integracao
            @
                [e] optional
                    [v] true
                [e] condition
                    [v] plano.exigeIntegracao
                [e] custom
                    [v] ConectarIntegracao   # só-do-fluxo → vive no pacote do fluxo
```
**Regra de empacotamento:** empacote pelo maior escopo de reuso, e reuse **por referência** (`opens`), nunca
movendo a tela. Telas com vida fora do fluxo ficam no pacote do domínio; telas só-do-fluxo e a `operation`
ficam em `window/<fluxo>/`. A `operation` é **uma** entrada de menu; os steps não aparecem no menu.

### 5.7 Registro no `main.mi`
```
[p] window
    [p] vendas
        [p] pedido
            [Descriptor.window.list] GestaoPedidos
                #
                    window/vendas/pedido/GestaoPedidos.mi
            [Descriptor.window.dialog] CriarPedido
                #
                    window/vendas/pedido/CriarPedido.mi
```

### 5.8 Antes de escrever qualquer gerador: rode `struct`
`mi push <projeto>` + `mi generate <projeto> struct xml className=<Tela> packageName=<org>.window.<ctx>.<entidade>`.
Confirme a forma do DOM; **nunca escreva template de olho no mapa**. A forma esperada:
- `<class name=… package=… mode="window.list">`; o `@mode` vem do ícone.
- Regiões (`<columns>`, `<search>`, `<filters>`, `<actions>`, `<emptyState>`, `<form>`) são **filhas diretas** de `<class>`.
- Campo: `<field><value>.caminho</value><properties>…</properties></field>`. Ação: `<action><value>id</value><properties>…</properties></action>`.
- O layout do form é **preservado**: `form/rows/field` e `form/rows/row/field`.
- Bind: `attributes/attribute[@name='object']/@type`. Resolva cada `.campo` contra
  `/classes/class[@name=<type> and @mode='bean']/attributes/attribute[@name=…]`.

### 5.9 Checklist do mapa de tela
```
[ ] Estereótipo Descriptor.window.* existente e coerente com o papel da tela
[ ] Estrutura em inglês; negócio no idioma do domínio
[ ] @ title, route (list/editor/operation), role, icon
[ ] [g] source / [r] object aponta para um [b] existente
[ ] Todo field referencia um .caminho válido no bean
[ ] Enums/rótulos NÃO redeclarados
[ ] Ações com type; condicionais com condition
[ ] emptyState em window.list, referenciando uma action existente
[ ] Sem estilo/cor e sem lógica imperativa
[ ] Registrada no main.mi sob [p] window/<contexto>/<entidade>
```

---

## 6. Geradores (em `mi/generators/frontend/`, grupo `frontend` no `main.mi`)

Registre cada sub em `config/mapperidea/generators/frontend` no `main.mi`, no mesmo dialeto dos geradores
de backend que o projeto já tem (parameters `modelName`/`package`, dispatch por família de tipo nativo,
patterns + templates).

| Sub | Entrada | Saída | Emite |
|---|---|---|---|
| `zodSchema` | `[b]` de domínio | `gen/schemas/<entidade>.ts` | `import { z }`; `<attr>Values` + `<attr>Labels` por enum (rótulos de `title/pt`); `<entidade>Schema = z.object({...})` com `.max(n)` de `Texto(n)`, `.optional()` quando não `required`, `z.boolean()`/`z.number().int()`/`z.string()` por tipo; `export type <Entidade> = z.infer<...>`. Relações omitidas na v1. |
| `apiClient` | `[b]` de domínio | `gen/api/<entidade>.ts` | `type <E>Input = Partial<Omit<<E>,"id">>`; `<e>Keys` (query keys); hooks `use<E>s`, `use<E>(id)`, `useCriar<E>`, `useAtualizar<E>`, `useRemover<E>` sobre `lib/api.ts` (invalidação de keys nas mutations). Pluralização naïve na v1. |
| `screenColumns` | `window.list` | `features/…/columns.tsx` | `ColumnDef[]` resolvendo cada coluna contra o bean: header do domínio (ou `label`), `badge` + tom, máscara, data, `sortable`, coluna de ações de linha com `condition` → `disabled`. |
| `screenPage` | `window.list` | `features/…/<Tela>Screen.tsx` | página que compõe `<DataTable>` (columns, `use<E>s`, facets dos filtros, `searchPlaceholder`, `empty`) + cabeçalho com a ação primária num `<Dialog>` que renderiza o `<XForm>` gerado e liga `useCriar<E>` → toast → fecha. |
| `screenCards` | `window.list` + `presentation cards` | `features/…/<Tela>Screen.tsx` | idem com `<CardList>` (1ª coluna = título do card). |
| `screenForm` | `window.dialog`/`editor` | `features/…/<Tela>.tsx` | `pick` do schema gerado, `FieldSpec[]` (enum → select via Values/Labels; layout `rows`/`row`/`width`), `defaults`, e `<XForm>({onSubmit, values?, isSubmitting})` sobre `<AutoForm>`. **O `<XForm>` é a unidade reusável** (o dialog, a página e o step do wizard o embutem). |
| `screenEditorPage` / `screenEditorById` | `window.editor` | `features/…/<Tela>Screen.tsx` | página standalone de criação; ou, se a rota tem `$id`, carrega por `use<E>(id)` e salva com `useAtualizar<E>`. |
| `screenOperation` | `window.operation` | `features/…/<Tela>Screen.tsx` | monta `OperationStep[]` (step com `opens` importa e embute o `<XForm>` da tela-alvo; com `custom`, importa a tela à mão do pacote do fluxo) e renderiza `<OperationFlow>`. |
| `screenManifest` | todas as janelas | `gen/screens.ts` | `{name, route, title, stereo, icon?, group?, order?, brands}[]`. **Fonte única** de navegação (e de rotas, quando o routeTree passar a ser gerado), para que nav e rota nunca divirjam. |

Ordem de autoria: `struct` primeiro, depois o template, `mi generate` e, por fim, `typecheck + build` no
front. Comece cada gerador numa entidade/tela piloto e só generalize depois que ela compilar.

### 6.1 Runner único — `scripts/mapperidea/gen-frontend.sh`
```
gen-frontend.sh <Entidade>              # domínio → gen/schemas + gen/api   (opcional 2º arg: zodSchema|apiClient)
gen-frontend.sh <Tela>                  # tela   → features/<ctx>/<entidade>/   (detecta pelo caminho mi/window/)
gen-frontend.sh manifest                # → gen/screens.ts
```
Comportamento:
- **1 push → N generate.** `MI_SKIP_PUSH=1` pula o push quando você roda vários em sequência.
- Detecta a tela pelo caminho (`/window/`), deriva `package=<org>.window.<ctx>.<entidade>` e a pasta de saída
  do caminho após `mi/window/`.
- Lê o estereótipo (`[Descriptor.window.X]`), o `presentation` e a `route` (se tiver `$`, é editor por id)
  para escolher os subs.
- Gera em arquivo temporário e só move se a saída não estiver vazia (falha não apaga o arquivo bom).
- ⚠️ Portabilidade: no sed do macOS **não** use `\S`/`\d`/`\w` (a captura volta vazia sem erro). Use
  `[^[:space:]]`. `grep -P` também não existe lá. Teste o script nos dois SOs.
- Ao criar uma tela ou mudar `usedIn`/`route`/`title`, rode também `manifest`.

---

## 7. Biblioteca de runtime (à mão, uma vez — Fase 1)

Componentes genéricos, tipados e acessíveis sobre shadcn. O gerador **não** reimplementa nada disto.

- **`<DataTable columns query facets searchPlaceholder empty rowDetail? pageSize?>`**: TanStack Table com
  paginação, ordenação, busca, filtro facetado, **skeleton de loading**, **estado vazio** (com a ação) e
  **estado de erro**, todos de fábrica. Recebe o resultado do hook Query.
- **`<AutoForm schema fields defaults values? onSubmit isSubmitting>`**: RHF + `zodResolver`, erro por campo,
  **serve create e edit**, layout em grid a partir de `rows`/`row`/`width`.
- **`<FormField>`**: dispatch tipo/renderer → controle shadcn (camada 3, §8.2): texto → Input, texto longo →
  Textarea, número/decimal → Input numérico (moeda formatada), boolean → Switch, data → DatePicker,
  enum → Select, FK → Combobox com busca assíncrona; máscaras aplicadas na apresentação.
- **`<CardList>`**: alternativa visual à tabela.
- **`<OperationFlow steps initial onSaveDraft? onComplete>`** (wizard):
  ```ts
  interface OperationStep<M> {
    key: string; title: string; optional?: boolean
    isVisible?: (master: M) => boolean              // ← @ condition
    render: (ctx: { master: M; patch: (s: Partial<M>) => void; next: () => void }) => ReactNode
  }
  ```
  Cuida do passo atual, do mestre acumulado, de avançar/voltar/ir-para, do stepper, de retomar no 1º passo
  incompleto e dos guards de opcional/visível.
- **`<EmptyState>`, `<ErrorState>`, `<LoadingState>`** e um feedback de mutation padronizado (toast de
  sucesso/erro com mensagem útil, nunca um texto técnico).
- **`format.ts`**: `maskCPF`, `maskCNPJ`, `maskPhone`, moeda, data e data-hora, e o tom de badge por valor de enum.

Master-detail (futuro) usa RHF `useFieldArray`, e a persistência é **uma mutation atômica no backend**, nunca
uma cadeia de chamadas sem rollback no front.

---

## 8. Tema e componentes (camadas 2 e 3)

### 8.1 Tokens em 3 tiers (`index.css`)
- **Tier 1, paleta bruta** (`--xx-*`): as cores do brandbook, com nome próprio.
- **Tier 2, slots de marca** (`--brand-primary`, `--brand-accent`, `--brand-radius`, `--brand-font-body`,
  `--brand-font-heading`…), que referenciam o Tier 1. **É o único bloco que o designer edita para
  re-tematizar.**
- **Tier 3, tokens shadcn** (`--primary`, `--primary-foreground`, `--ring`, `--sidebar-*`, `--destructive`…),
  que referenciam o Tier 2. Tema claro e escuro (`.dark`/`[data-theme]`), cada cor com o seu `-foreground`
  em contraste adequado.
- **Zero cor literal em componente.** Cor só via token.
- Fonte licenciada vira a 1ª opção do slot, com fallback open-source (`@fontsource-variable/*`).

### 8.2 Tipo → componente
O mapa tipo/renderer → controle fica centralizado no `<FormField>` (opcionalmente declarado num
`config/maps/toShadcn` no `main.mi`, análogo aos mapas de tipo dos geradores de backend). Trocar esse mapa
troca o design system **sem tocar em tela**.

### 8.3 Multi-marca (se o projeto tiver mais de um produto)
Dois eixos que **não se misturam**:

| Eixo | O quê | Quando resolve | Mecanismo |
|---|---|---|---|
| `usedIn` (marca-produto) | **quais telas** o produto tem | build (hoje pode ser runtime no mock) | `@ usedIn` na tela; sem `usedIn`, a tela é compartilhada; o manifesto filtra o menu por marca; `<html data-brand>` troca o Tier 2 |
| white-label (por cliente) | **como** o produto se veste | runtime | override do Tier 2 por `data-tenant`, com valores vindos da API; sem build novo |

A marca **não** entra no caminho de pastas: uma árvore de telas só, com `.tsx` gerados agnósticos de marca.

### 8.4 Style guide vivo
Rota `/theme` (pública) mostrando paleta, cores semânticas, tons de status, tipografia, botões, campos, raio e
os componentes do runtime, com toggle claro/escuro e seletor de marca. É onde o designer vê o sistema reagir
a uma mudança de token.

---

## 9. Modo mock (o loop do designer)

- `src/config/env.ts` resolve `VITE_APP_ENV ∈ mock | dev | staging | prod` e expõe `IS_MOCK`, `API_BASE`
  (`/api`, com override opcional `VITE_API_BASE`) e o selo de ambiente.
- `npm run dev:mock` (`vite --mode mock`, lendo `.env.mock`) sobe **só o front**, sem backend nem IdP.
- `src/mocks/registry.ts` intercepta **dentro de `lib/api.ts`, antes do fetch** (sem service worker, sem
  processo extra). Inclui sessão fake em `/auth/me` (o `beforeLoad` passa), handlers por entidade com
  **fixtures realistas** (enums, documentos válidos, datas), **latência artificial de ~350ms** (para ver o
  skeleton) e `?empty=1` na URL para forçar o estado vazio.
- Selo **MOCK/DEV/STAGING** no cabeçalho; seletor de marca só no mock.
- Evolução: um gerador `dbMock` derivando fixtures do domínio (`values`/tipos → dados fake). Se um dia
  precisar de interceptação de rede, MSW entra atrás da mesma fronteira (`api.ts` → registry).

---

## 10. Fases (cada uma termina com `typecheck + build` verde)

| # | Fase | Entregáveis | Aceite |
|---|---|---|---|
| 0 | **Scaffold** | Vite + React + TS, shadcn, Tailwind v4, TanStack Router/Query, `lib/api.ts`, `query-client.ts`, `_authenticated.tsx`, `env.ts`, `.nvmrc` (20) | app sobe; rota protegida redireciona sem sessão |
| 1 | **Runtime + tokens** (à mão) | `<DataTable>`, `<AutoForm>`/`<FormField>`, estados, `format.ts`, tokens em 3 tiers, `/theme` | `/theme` mostra todos os componentes em claro/escuro; uma tela de exemplo à mão usa só o runtime |
| 2 | **Modo mock** | `env.ts`, registry, sessão fake, fixtures, latência, `?empty=1`, selo | `npm run dev:mock` navega sem backend; loading e vazio visíveis |
| 3 | **Geradores de domínio** | `zodSchema`, `apiClient`, `gen-frontend.sh <Entidade>` | numa entidade piloto, regenerar duas vezes dá **diff zero**; a tela de exemplo passa a consumir `gen/` |
| 4 | **Mapa de tela + geradores de lista/form** | dialeto §5 em `mi/window/`, `screenColumns`, `screenPage`, `screenForm`, runner detectando telas | lista piloto **100% gerada** (columns + form + page + schema + api), com o create funcionando contra o mock e/ou backend |
| 5 | **Manifesto + navegação** | `screenManifest`, nav lendo `gen/screens.ts` (grupo, ordem, papel, `usedIn`) | criar uma tela no mapa + `manifest` faz ela aparecer no menu sem editar código |
| 6 | **Editor por id + cards** | `screenEditorPage`, `screenEditorById`, `screenCards` | ação `row` com `opens` abre o editor carregado pelo id e salva com update |
| 7 | **Wizard** | `<OperationFlow>` (à mão) + `screenOperation` | fluxo piloto reusando ≥2 `<XForm>` de telas existentes via `opens`, sem duplicar tela |
| 8 | **Multi-marca / white-label** (se aplicável) | `usedIn` no manifesto, `data-brand`, override por tenant | trocar a marca re-tematiza e filtra o menu sem tocar em tela |
| 9 | **Futuro** | master-detail (`window.iframe` + `useFieldArray` + mutation atômica), `window.report`, join `.relacao.campo`, i18n plena (`title/en`), `dbMock` | — |

Sugestão de divisão de trabalho com agentes (se o projeto usar a tríade do Mapper Idea): o **mapeador de
negócio** escreve os mapas de tela (são visão de negócio/UX amarrada ao domínio), o **arquiteto de mapas**
escreve os geradores e o **runner** valida (`push`/`struct`/`generate`). Se as telas ficarem muito pesadas em
UX, destaque um `screen-mapper` dedicado.

---

## 11. Checklist de qualidade (critério de review de PR de front)

- [ ] Tela gerada é fina e só compõe o runtime; nenhum arquivo de `gen/` ou gerado em `features/` foi editado à mão
- [ ] Dados só via hooks gerados (`use<E>s`, `useCriar<E>`…); zero `fetch`/URL solta
- [ ] Enums e rótulos vindos do schema gerado; nada redeclarado
- [ ] Loading, vazio e erro tratados pelo runtime
- [ ] Zero hex/cor literal; só tokens
- [ ] Acessibilidade: ícone-botão com `aria-label`, foco visível, labels nos campos
- [ ] Rota e menu vindos da mesma fonte (manifesto)
- [ ] RBAC no front só como UX; autorização real no backend
- [ ] Sem PII em storage, log ou telemetria; logout limpa o cache
- [ ] Lacuna do gerador aparece como `// @TODO`, sem quebrar a geração
- [ ] Regenerar a tela dá diff zero (o gerador é determinístico)

---

## 12. Armadilhas já conhecidas

- **Escrever template sem rodar `struct`** sai errado: o DOM normalizado difere do texto do mapa.
- **Reusar `readOnly` do domínio** para definir campos de form quebra filtros/queries do backend. O que é
  editável é decisão da tela.
- **Payload de create como `Omit<E,"id">` estrito** obriga a tela a inventar defaults. Use `Partial` e deixe
  o zod do form validar os obrigatórios.
- **Wizard que embute a *Screen*** em vez do *Form* duplica cabeçalho e ações. O step embute o `<XForm>`.
- **Mover uma tela reusável para dentro do pacote do fluxo** quebra o reuso fora dele. Reuse por `opens`.
- **sed do macOS sem `\S`**: o script parece funcionar, mas escolhe o gerador errado sem dar erro. Use
  classes POSIX.
- **Node 18 com Vite 8** quebra `dev` e `build`. Fixe o Node 20.
- **Esquecer o `manifest`** depois de criar ou renomear uma tela: ela existe, mas não aparece no menu.
