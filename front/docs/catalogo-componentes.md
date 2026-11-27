# Catálogo de componentes visuais do front

Inventário do que já existe em `front/web/src` para montar telas. **Antes de criar um componente, procure aqui.** As
regras de uso estão em `padroes-de-ui.md`. A rota `/admin/theme` (menu Administração > Guia de estilo, só administrador da plataforma; componente `features/admin/GuiaDeEstilo.tsx`) é a vitrine viva: mostra os
componentes abaixo renderizados nos temas claro e escuro. Os 50 primitivos têm um exemplo cada na seção final, em `features/admin/GuiaComponentes*.tsx`.

Pilha: Vite 8, React, TypeScript, Tailwind 4 (`@tailwindcss/vite`), shadcn/ui sobre Radix (`radix-ui`), ícones `lucide-react`,
avisos `sonner`, tabela `@tanstack/react-table`, formulários `react-hook-form` + `zod`, gráficos `recharts`, `class-variance-authority`
para variantes. O Vite não traz componentes visuais: tudo vem destas bibliotecas e do código do projeto.

## 1. Primitivos (shadcn/ui) — `src/components/ui`

Cópias do shadcn mantidas no projeto (podem ser ajustadas). São 50 componentes, todos exemplificados em
`features/admin/GuiaComponentes.tsx` (seção "Catálogo completo de componentes" da vitrine). Acrescente um novo componente
do shadcn só quando preciso, no mesmo estilo. O hook `hooks/use-mobile.ts` (`useIsMobile`) apoia o Sidebar.

### Formulário e entrada

| Componente | Arquivo | Partes e variantes |
|---|---|---|
| **Input** | `input.tsx` | campo de texto (inclui `type="search"`, `password`...) |
| **Textarea** | `textarea.tsx` | texto longo |
| **Label** | `label.tsx` | rótulo ligado ao controle |
| **Select** | `select.tsx` | Select, Value, Group, Trigger, Content, Item |
| **NativeSelect** | `native-select.tsx` | NativeSelect, NativeSelectOption, NativeSelectOptGroup (`<select>` nativo, bom no celular) |
| **Combobox (lista estática)** | `combobox.tsx` | `options` (`{value, label}`), `value`, `onValueChange`, `placeholder`, `searchPlaceholder`, `emptyText`. Popover + Command; **não** é o FK assíncrono (veja `runtime/Combobox`) |
| **Checkbox** | `checkbox.tsx` | `checked`, `onCheckedChange` |
| **RadioGroup** | `radio-group.tsx` | RadioGroup, RadioGroupItem |
| **Switch** | `switch.tsx` | interruptor (campo booleano) |
| **Slider** | `slider.tsx` | `value` (array), `min`, `max`, `step` |
| **Toggle** | `toggle.tsx` | `variant`: default, outline. `size`: default, sm, lg. `toggleVariants` exportado |
| **ToggleGroup** | `toggle-group.tsx` | ToggleGroup (`type` single ou multiple, `variant`, `size`), ToggleGroupItem |
| **ButtonGroup** | `button-group.tsx` | ButtonGroup (`orientation`), ButtonGroupText, ButtonGroupSeparator |
| **InputGroup** | `input-group.tsx` | InputGroup, InputGroupInput, InputGroupAddon (`align`: start, end), InputGroupButton. Base do `SearchInput` |
| **InputOTP** | `input-otp.tsx` | InputOTP (`maxLength`), InputOTPGroup, InputOTPSlot (`index`), InputOTPSeparator. Dependência `input-otp` |
| **DatePicker** | `date-picker.tsx` | `value` (Date), `onChange`, `placeholder`; exibe dd/MM/aaaa. Popover + Calendar |
| **Calendar** | `calendar.tsx` | `react-day-picker` 10 (`mode` single, multiple, range), localizado em pt-BR, `captionLayout` |
| **Field** | `field.tsx` | FieldSet, FieldLegend, FieldGroup, Field (`orientation`: vertical, horizontal, responsive), FieldContent, FieldLabel, FieldTitle, FieldDescription, FieldSeparator, FieldError |
| **Form** | `form.tsx` | Form, FormField, FormItem, FormLabel, FormControl, FormDescription, FormMessage (react-hook-form; `useFormField`) |

### Navegação

| Componente | Arquivo | Partes e variantes |
|---|---|---|
| **Tabs** | `tabs.tsx` | Tabs, TabsList, TabsTrigger, TabsContent |
| **Accordion** | `accordion.tsx` | Accordion (`type` single ou multiple), AccordionItem, AccordionTrigger, AccordionContent |
| **Collapsible** | `collapsible.tsx` | Collapsible, CollapsibleTrigger, CollapsibleContent |
| **Breadcrumb** | `breadcrumb.tsx` | Breadcrumb, List, Item, Link (`asChild`), Page, Separator, Ellipsis |
| **Pagination** | `pagination.tsx` | Pagination, Content, Item, Link (`isActive`), Previous, Next, Ellipsis |
| **Menubar** | `menubar.tsx` | Menubar, Menu, Trigger, Content, Item, CheckboxItem, RadioItem, Label, Separator, Shortcut, Sub |
| **NavigationMenu** | `navigation-menu.tsx` | NavigationMenu (`viewport`), List, Item, Trigger, Content, Link, Viewport, Indicator; `navigationMenuTriggerStyle` |
| **Command** | `command.tsx` | Command, CommandDialog, Input, List, Empty, Group, Item, Separator, Shortcut (dependência `cmdk`) |
| **Kbd** | `kbd.tsx` | Kbd, KbdGroup (atalho de teclado) |

### Sobreposições

| Componente | Arquivo | Partes e variantes |
|---|---|---|
| **Dialog** | `dialog.tsx` | Dialog, DialogTrigger, DialogClose, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter |
| **AlertDialog** | `alert-dialog.tsx` | AlertDialog, Trigger, Content, Header, Title, Description, Footer, Action, Cancel (confirmação de ação irreversível) |
| **Sheet** | `sheet.tsx` | Sheet, Trigger, Close, Content (`side`: top, right, bottom, left), Header, Title, Description, Footer |
| **Drawer** | `drawer.tsx` | Drawer, Trigger, Close, Content, Header, Title, Description, Footer (dependência `vaul`; gaveta de baixo, arrasta no celular) |
| **Popover** | `popover.tsx` | Popover, Trigger, Anchor, Content (base do Combobox e do DatePicker) |
| **Tooltip** | `tooltip.tsx` | Tooltip, Trigger, Content, TooltipProvider (já embutido em Tooltip) |
| **HoverCard** | `hover-card.tsx` | HoverCard, Trigger, Content |
| **DropdownMenu** | `dropdown-menu.tsx` | DropdownMenu, Trigger, Content, Item, Label, Separator |
| **ContextMenu** | `context-menu.tsx` | ContextMenu, Trigger, Content, Item, CheckboxItem, RadioItem, Label, Separator, Shortcut, Sub |

### Exibição de dados

| Componente | Arquivo | Partes e variantes |
|---|---|---|
| **Table** | `table.tsx` | Table, TableHeader, TableBody, TableRow, TableHead, TableCell |
| **Badge** | `badge.tsx` | `tone`: neutral, primary, success, warning, danger, info, outline |
| **Card** | `card.tsx` | Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter |
| **Avatar** | `avatar.tsx` | Avatar, AvatarImage, AvatarFallback |
| **AspectRatio** | `aspect-ratio.tsx` | `ratio` |
| **Item** | `item.tsx` | ItemGroup, Item (`variant`: default, outline, muted; `size`: default, sm), ItemMedia (`variant`: default, icon, image), ItemContent, ItemTitle, ItemDescription, ItemActions, ItemHeader, ItemFooter, ItemSeparator |
| **Carousel** | `carousel.tsx` | Carousel (`opts`, `orientation`, `setApi`), Content, Item, Previous, Next (dependência `embla-carousel-react`) |
| **Chart** | `chart.tsx` | `ChartConfig`, ChartContainer, ChartTooltip(Content), ChartLegend(Content) sobre `recharts` 3. Cores por `var(--primary)`, `var(--success)`, `var(--info)` e `var(--color-<chave>)` |

### Feedback e estado

| Componente | Arquivo | Partes e variantes |
|---|---|---|
| **Alert** | `alert.tsx` | Alert (`variant`: default, destructive, success, warning, info), AlertTitle, AlertDescription (aviso persistente na página) |
| **Progress** | `progress.tsx` | `value` de 0 a 100 |
| **Spinner** | `spinner.tsx` | indicador de carregamento inline |
| **Skeleton** | `skeleton.tsx` | bloco de carregamento |
| **Empty** | `empty.tsx` | Empty, EmptyHeader, EmptyMedia (`variant`: default, icon), EmptyTitle, EmptyDescription, EmptyContent |
| **Toaster** | `sonner.tsx` | contêiner dos avisos; dispare com `notifySuccess` e `notifyError` |

### Layout

| Componente | Arquivo | Partes e variantes |
|---|---|---|
| **Separator** | `separator.tsx` | divisória horizontal ou vertical |
| **ScrollArea** | `scroll-area.tsx` | ScrollArea, ScrollBar (`orientation`) |
| **Resizable** | `resizable.tsx` | ResizablePanelGroup (`orientation`), ResizablePanel (`defaultSize`, `minSize`), ResizableHandle (`withHandle`); dependência `react-resizable-panels` 4 |
| **Sidebar** | `sidebar.tsx` | SidebarProvider, Sidebar (`side`, `variant`, `collapsible`), Trigger, Rail, Inset, Header, Footer, Content, Group(Label, Content), Menu, MenuItem, MenuButton (`isActive`), MenuSub, MenuBadge, MenuSkeleton; `useSidebar`. O AppShell ainda não o usa; o provider grava um cookie de estado |
| **Button** | `button.tsx` | `variant`: default, destructive, outline, secondary, ghost, link. `size`: default, sm, lg, icon. `asChild` (use com `Link`). `buttonVariants` exportado |

(Button fica aqui por ser a peça básica de ação; Card e Badge aparecem em Exibição de dados.)

### Dependências adicionadas, com versão exata

Instaladas sem `^`: `react-day-picker` 10.0.2 e `date-fns` 4.4.0 (Calendar e DatePicker), `cmdk` 1.1.1 (Command),
`recharts` 3.11.0 (Chart), `vaul` 1.1.2 (Drawer), `embla-carousel-react` 8.6.0 (Carousel), `input-otp` 1.5.0 (InputOTP) e
`react-resizable-panels` 4.24.1 (Resizable). Em testes (jsdom), `src/test/setup.ts` simula `matchMedia`, `ResizeObserver`,
`IntersectionObserver` e `scrollIntoView`; o Chart avisa que a área tem largura 0, o que é esperado no jsdom.
## 2. Componentes de composição do runtime — `src/runtime`

Escritos à mão, são o que as telas geradas usam. **Telas novas devem compor estes, não os primitivos diretamente.**

| Componente | Arquivo | Para que serve | Props principais |
|---|---|---|---|
| **AutoForm** | `AutoForm.tsx` | Formulário completo a partir de schema Zod e layout em linhas, com seções, erro do servidor, CEP e envio | `schema`, `fields` (FieldLayout: linhas e itens de seção `{ section, description?, collapsible?, defaultOpen? }`; seções em Card, Accordion se collapsible, Tabs se mais de 5; barra de ações fixa e aviso de alterações não salvas quando há seções), `defaults`, `values` (edição), `onSubmit`, `isSubmitting`, `submitLabel`, `onCancel`, `serverError`, `clearAfterSubmit` |
| **FormField** | `FormField.tsx` | Um campo do formulário, escolhido por `kind` | `FieldSpec`: `name`, `label`, `kind` (text, email, password, textarea, number, currency, boolean, date, enum, fk, **list**), `mask`, `required`, `hint`, `lookup: 'cep'`, `options`, `fk`, `size` (`FieldSize`: xs, s, m, l, xl, full = 2, 3, 4, 6, 8, 12 colunas de 12; automático por tipo e `maxLength` quando omitido), `width` (depreciado), `maxLength`, `readOnly`, `placeholder`, `description`, `list` (só com `kind: 'list'`: `{ item: FieldLayout, itemLabel, addLabel, emptyText?, maxItems?, defaultItem? }`; detalhe 1-N dentro do formulário, valor = array de `{ id?, ...campos }` com `id` preservado e salvo por `PUT /{Entidade}/{id}/{relacao}` com a lista inteira; com item `tipoContato`, os sócios do CNPJ viram contatos) |
| **Combobox (FK)** | `Combobox.tsx` | Seleção pesquisável e **assíncrona** de registro relacionado (FK; diferente de `ui/combobox.tsx`, que é lista estática), com botão de limpar | `source` (FkSource), `value`, `onChange`, `placeholder`, `invalid`, `describedBy`. Fontes prontas em `lib/fk-sources.ts` (`fkSource('Municipio')`) Opção com `group` (opcional) aparece sob o título do grupo, que não se escolhe (ex.: categoria "Pai › Filha"). |
| **DataTable** | `DataTable.tsx` | Lista paginada no servidor: busca com debounce, filtros facetados, ordenação, tamanho da página, estados de vazio, erro e carregamento | `columns`, `result`, `state`, `onStateChange`, `facets`, `searchPlaceholder`, `empty`, `toolbarActions`, `pageSizeOptions`, `getRowId` |
| **CardList** | `CardList.tsx` | Alternativa em cartões à tabela, para telas estreitas | `result`, `getKey`, `title`, `fields`, `actions`, `empty` |
| **ComputedCell** | `ComputedCell.tsx` | Valor calculado numa célula de lista (coluna `computed` do mapa), lido de uma fonte registrada em `lib/computed-sources.ts`; "—" sem valor | `source`, `id` |
| **SenhaRecente** | `SenhaRecente.tsx` | Diálogo global de confirmação de senha (A7, docs/financeiro/README.md 9.2). Montado uma vez no `__root`; a `api` o abre sozinha no 403 `senha_recente_exigida` e repete a chamada. Nenhuma tela o usa diretamente | — |
| **SearchInput** | `SearchInput.tsx` | Campo de busca no **padrão B** (botão de busca em destaque embutido, X para limpar), sobre `ui/input-group.tsx`. Exporta `SEARCH_BUTTON_CLASS`, usado também pelo campo CEP do `FormField` (`lookup: 'cep'`: botão, Enter e sair do campo consultam). A busca do DataTable ainda tem campo próprio | `onSearch`, `variant` (icon, button), `placeholder`, `clearable`, `value` |
| **EmptyState** | `states.tsx` | Nada a mostrar, com ação opcional | `title`, `description`, `action`, `icon` |
| **ErrorState** | `states.tsx` | Falha ao carregar, com "Tentar novamente" | `error`, `onRetry` |
| **LoadingState** | `states.tsx` | Carregando | `label` |

Apoio (sem visual próprio):

| Módulo | Para que serve |
|---|---|
| `table-state.ts` | `useTableState`: página, tamanho, ordenação, busca e filtros; calcula `from/to` da API paginada |
| `feedback.ts` | `notifySuccess`, `notifyError`, `errorMessage`, `mutationFeedback` (avisos de sucesso e erro de uma mutation) |
| `format.ts` | `formatCurrency`, `formatDate`, `formatDateTime`; máscaras `maskCPF`, `maskCNPJ`, `maskDocumento`, `maskPhone`, `maskCEP`; `MaskKind`: cpf, cnpj, documento, phone, postalCode |
| `icons.tsx` | `resolveIcon(nome)`: ícones lucide **registrados** (um nome não registrado cai num ícone padrão) |

Ícones registrados hoje: `home`, `users`, `user`, `user-cog`, `user-pen`, `pencil`, `plus`, `upload`, `package`, `file-text`,
`settings`, `palette`, `shopping-cart`. Falta, por exemplo, `building` e `map-pin`: registre antes de usar nos mapas.

## 3. Casca e peças globais — `src/components`

| Componente | O que faz |
|---|---|
| **AppShell** | Layout autenticado: barra lateral com menu agrupado (vem de `config/menu.ts` + manifesto gerado), cabeçalho, conteúdo |
| **ThemeToggle** | Botão que alterna entre tema claro e escuro (sol e lua) |
| **EnvBadge** | Selo de ambiente (MOCK, DEV, STAGING); não aparece em produção |

Telas de aplicação à mão: `routes/login.tsx`, `routes/_authenticated/perfil.tsx` (dados do usuário e troca de senha),
`routes/_authenticated/index.tsx` (início), `features/admin/*` (Importar municípios, Usuários) e a vitrine `features/admin/GuiaDeEstilo.tsx` (rota `/admin/theme`, com o catálogo em `GuiaComponentes.tsx`).

## 4. Tema e tokens — `src/index.css`

- Cores em OKLCH, em camadas: paleta `--xx-*` (neutros, azul, teal, verde, âmbar, vermelho, céu) → marca `--brand-*`
  (`primary`, `accent`, `radius`, fontes) → tokens semânticos do shadcn (`background`, `foreground`, `card`, `muted`,
  `border`, `primary`, `destructive`, `success`, `warning`, `info`...). **Use os tokens semânticos nas telas, nunca a paleta.**
- Tema claro e escuro (classe `.dark`, controlada pelo `ThemeToggle`). Fonte: Inter Variable. Raio base 0,625rem.
- Para trocar a marca, altere só os `--brand-*`.

## 5. Quando usar o quê

| Preciso de... | Use |
|---|---|
| Lista de registros | `DataTable` (+ `CardList` em telas estreitas) |
| Criar ou editar registro | `AutoForm` via `Formulario<X>Form` gerado |
| Escolher registro relacionado | campo `fk` (Combobox) |
| Escolher entre poucos valores | campo `enum` (Select) |
| Situação com cor | `Badge` com `tone` |
| Confirmar ação irreversível | `AlertDialog` |
| Formulário curto em janela | `Dialog` (ou `Sheet` / `Drawer` para painel lateral e inferior) |
| Alternar entre visões | `Tabs` |
| Botão só com ícone | `Tooltip` + `aria-label` |
| Aviso persistente na página | `Alert` |
| Data | `DatePicker` (`Calendar` para calendário solto) |
| Sim ou não | `Switch` (efeito imediato) ou `Checkbox` (marcado e enviado com o formulário) |
| Uma opção entre 2 a 5 | `RadioGroup` (ou `ToggleGroup` como filtro) |
| Uma opção entre muitas, lista fixa | `Select` (`Combobox` de `ui` se precisar buscar; `NativeSelect` no celular) |
| Menu de ações de uma linha | `DropdownMenu` (`ContextMenu` no botão direito) |
| Busca global de ações | `Command` / `CommandDialog` |
| Progresso de tarefa | `Progress`; espera curta inline: `Spinner` |
| Lista vazia em bloco | `Empty` (`EmptyState` no runtime) |
| Aviso de resultado | `notifySuccess` / `notifyError` |
| Carregando, vazio ou erro | `LoadingState`, `EmptyState`, `ErrorState` |
| Navegar para outra tela | `Button asChild` + `Link` |

## 6. Como manter este catálogo

Ao criar ou alterar componente em `components/ui`, `runtime` ou `components`, atualize a tabela correspondente e a vitrine
`/admin/theme`. Os testes de componente (`*.test.tsx`) documentam o comportamento esperado.
