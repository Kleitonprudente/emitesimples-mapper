# Padrões de UI do Emite Simples

Regras para qualquer tela do `front/web`. A ideia é que duas telas diferentes pareçam e se comportem como a mesma
aplicação. Onde há código do runtime (`front/web/src/runtime`) ou do gerador, ele é a fonte da verdade: este documento
descreve o que ele já faz e o que se espera de telas novas.

> **Regra de ouro:** telas só usam componentes do catálogo (`catalogo-componentes.md`). Não se criam componentes novos.
> Veja a seção 6.
>
> Como as telas nascem: o mapa (`front/mapas/window/...`) descreve a tela, os geradores (`generators/frontend`) escrevem
> `src/gen` e `src/features`, e o `runtime/` é escrito à mão. **Nunca edite código gerado**: ajuste o mapa, o gerador ou o
> runtime. Veja `front/mapas/README.md` e `plano-metodologia-frontend-mapper.md`.

## 1. Anatomia de um cadastro (CRUD)

Todo cadastro tem **duas telas e um formulário**:

| Peça | Rota | Responsabilidade |
|---|---|---|
| Lista (`window.list`) | `/entidades` | Buscar, filtrar, paginar, ordenar; botão **Nova ...**; lápis por linha |
| Formulário (`window.editor`) | `/entidades/$id` | **Criar e editar** na mesma página: `id = novo` cria, qualquer outro id edita |

- **Não há diálogo para criar nem tela separada de edição.** Criar e editar são a mesma página, com o mesmo formulário e a
  mesma validação. A lista só navega.
- O formulário é um componente reutilizável (`FormularioXForm`) que só compõe o `AutoForm`. Quem o hospeda (página,
  passo de assistente) decide o resto.
- Depois de salvar, voltamos à lista com um aviso de sucesso. Cancelar volta sem salvar.
- **Nunca exclua.** O back não apaga empresa, pessoa, município etc. Quando houver o conceito, é desativar (campo de
  situação), e a ação aparece como edição.
- Links diretos funcionam e sobrevivem a F5: por isso a edição tem URL própria.

## 2. Lista

- Uma tabela paginada no servidor (`DataTable`): **busca** por um campo textual principal, **filtros facetados** para
  enums de poucos valores, ordenação por coluna, 20 por página por padrão. O back pagina por `/{from}/{to}` (máx. 100) e
  tem `/count`.
- Colunas: poucas e úteis (3 a 6). Documento com máscara (`documento` escolhe CPF ou CNPJ), telefone com máscara, enum como
  **badge**, o resto como texto. Não mostre ids, datas de controle nem campos técnicos.
- O **rótulo do enum é a descrição longa do domínio** (ex.: "Pessoa jurídica (documento CNPJ, 14 dígitos)"), tanto na
  coluna quanto no formulário.
- Estado vazio: mensagem em frase completa mais a ação de criar ("Nenhuma pessoa cadastrada ainda." + **Nova pessoa**).
- Carregando: `LoadingState` (esqueleto). Erro: `ErrorState` com **Tentar novamente**. Nunca uma tela em branco.
- Ação primária (criar) à direita da barra de ferramentas, ação de linha (editar) como ícone. Ação do tipo `link` serve
  para ir a outra tela (ex.: **Importar CSV do IBGE**), em botão `outline`.

## 3. Formulário

- Montado pelo `AutoForm` a partir de `FieldSpec`s. **Uma linha é um grupo de campos relacionados**; em telas estreitas as
  linhas empilham. Proporção dentro da linha por `size` na grade de 12 colunas (ver "Padrão de grade" abaixo).
- **Seções com título** (Identificação, Contato, Endereço, Tributação...) separam blocos longos. Ordem: o que identifica
  primeiro, o que é raro por último.
- Rótulo sempre visível, acima do campo, **no texto do domínio** (campo `title` do mapa). Obrigatório com asterisco.
  Texto de ajuda fixo (`hint`) ligado ao campo por `aria-describedby`, não placeholder.
- **Máscara é só apresentação**: o valor do formulário e o enviado ao back são **dígitos crus** (`cpf`, `cnpj`,
  `documento`, `phone`, `postalCode`).
- Tipos: texto, e-mail, senha, textarea, número, moeda (prefixo R$), booleano (interruptor), data, enum (select tipado,
  aceita valores numéricos), FK (combobox pesquisável com botão de limpar).
- **Padrão de grade (12 colunas).** Cada linha é um array de `FieldSpec` renderizado em `grid grid-cols-12 gap-4`
  (`items-start`, para a mensagem de erro não desalinhar a linha). Cada campo tem `size` (`FieldSize`); em telas pequenas
  todo campo ocupa 12 colunas e entre `sm` e `md` os menores que `full` ocupam 6.

  | `size` | Colunas (md+) | Automático para |
  |---|---|---|
  | `xs` | 2 | texto com `maxLength` ≤ 5 |
  | `s` | 3 | data, número, moeda, máscara (cpf, cnpj, documento, phone, postalCode), booleano, texto ≤ 15 |
  | `m` | 4 | enum (lista de opções), texto ≤ 30, texto sem `maxLength` |
  | `l` | 6 | FK (combobox), texto ≤ 100 |
  | `xl` | 8 | texto ≤ 255 |
  | `full` | 12 | textarea, texto > 255 |

  - **`size` omitido** = tamanho automático da tabela acima; `size` explícito sempre vence. Use `size` quando o desenho
    da linha pedir (ex.: Razão Social e Nome Fantasia em `l` + `l`).
  - **A linha deve somar 12.** Em desenvolvimento, soma diferente de 12 gera `console.warn` com os nomes dos campos. Menos
    que 12 é aceito e a linha **fica alinhada à esquerda** (sobra à direita; não se centraliza, decisão de avaliação); mais
    que 12 quebra o excedente para a linha de baixo (erro de desenho).
  - `width` (peso relativo) está **depreciado**: linhas que só usam `width` e nenhum `size` mantêm o layout flex antigo.
  - **Linhas padrão:** endereço `s | l | s` (CEP, Logradouro, Número) e `m | m | m` (Complemento, Bairro, Município);
    contato `l | s | s` (E-mail, WhatsApp, Telefone); valores `s | s | s | s` (3+3+3+3); identificação
    `s | s | s | s` (Tipo, CNPJ/CPF, Inscrição Municipal, RG/IE) e `l | l` (Razão Social, Nome Fantasia).
  - **Seções:** item `{ section, description?, collapsible?, defaultOpen? }` no layout, seguido das linhas da seção.
    Cada seção vira um **Card** com título; a ordem é do que identifica ao que é raro. Layout sem seção (login, perfil,
    usuário) renderiza como formulário simples, sem Card.
  - **`collapsible: true`** (Accordion) só para seção **rara** (ex.: Pessoa estrangeira): nasce recolhida e abre sozinha
    se algum campo dela tiver valor ou erro; `defaultOpen` força aberta.
  - **Mais de 5 seções** viram **Tabs** (uma aba por seção); a aba com campo inválido mostra um ponto vermelho e sobe
    ao tentar enviar. Até 5, todas empilhadas.
  - **Textarea:** linhas por `maxLength` (≤ 200: 3; ≤ 2000: 6; maior: 8) e contador discreto `n/maxLength` abaixo.
  - **Switch (booleano)** fica alinhado com a caixa dos inputs vizinhos, sem rótulo duplicado.
  - **Barra de ações fixa** (`sticky bottom-0`, fundo e borda superior) quando o layout tem seções, com "Alterações não
    salvas" enquanto o formulário está sujo; o navegador também avisa (`beforeunload`) ao fechar a aba com alterações.
    Depois de salvar com sucesso o aviso some. Formulários simples mantêm o rodapé comum.
  - **Campo de lista (detalhes 1-N dentro do formulário).** `kind: 'list'` com `list: { item, itemLabel, addLabel, emptyText?, maxItems?, defaultItem? }`
    (ex.: Contatos de uma pessoa). O `name` é o do array no formulário (`contatos`) e o schema é `z.array(z.object(...))`. Entra no layout
    como uma linha de um campo (`size: 'full'`); o título e a descrição vêm da **seção** (o rótulo do campo não é repetido). Cada item é um
    bloco com título "{itemLabel} {n}", as linhas de `item` na mesma grade de 12 colunas e um botão **só com ícone** de remover
    (`aria-label` "Remover {itemLabel} {n}", com Tooltip); abaixo da lista, botão `outline` com `+` e o `addLabel` (desabilitado em `maxItems`).
    Vazio mostra `emptyText` (padrão "Nenhum {item} adicionado."). Erros aparecem sob cada campo do item; a seção ou aba com erro abre ao salvar.
    O item carregado do back traz `id`, que é **preservado** no envio mesmo sem campo visível; o envio leva `contatos` como array de objetos
    limpos (sem campos vazios), `[]` se a lista ficou vazia. O AutoForm só entrega o array: a lista de detalhes é salva por um **endpoint do pai que recebe a lista inteira** (convenção
    `PUT /{Entidade}/{id}/{relacao}`, ex.: `PUT /Pessoa/{id}/contatos`, que devolve a lista salva com ids, tudo ou nada). Não há sincronização
    item a item no cliente; erros 400 trazem `detalhes.campos` (ex.: `contatos[2].nome`).
    - **Quando usar:** detalhe 1-N **pequeno e sempre editado junto** do cadastro (contatos, telefones extras). Detalhe grande, com busca,
      paginação ou que tem tela própria **não** vira lista no formulário.
    - **Limites:** um nível só (item sem lista dentro de lista); item sem seções; use `maxItems` (ex.: 10) para listas que não devem crescer; os
      campos do item aceitam máscara, hint, enum e FK, mas não `lookup`.
    - **Sócios do CNPJ:** com um campo de lista `contatos` cujo item tem `tipoContato`, a consulta de CNPJ adiciona cada sócio do QSA como contato
      `{ tipoContato: 'socio', nome }` (sem duplicar nome já presente entre os contatos de sócio, ignorando acento e caixa; nunca apaga o que a pessoa
      preencheu) e o aviso do campo passa a dizer "N sócio(s) adicionado(s) em Contatos.". Sem esse campo, os sócios são ignorados.
  - Exemplos ao vivo em `/admin/theme` (seção "Padrão de formulário"), código em `features/admin/GuiaFormulario.tsx`.
- **Endereço padrão** (Pessoa, Empresa), nas linhas acima:
  1. CEP (máscara, dica "Ao sair do campo, o endereço é preenchido.", **botão de busca em destaque embutido**) | Logradouro | Número
  2. Complemento | Bairro | Município (combobox, rótulo "Guaíra - PR")
  O CEP dispara a consulta no **back** (`GET /api/cep/{cep}`) ao sair do campo com 8 dígitos, ao **clicar no botão de busca** ou
  ao dar **Enter** (o Enter não envia o formulário; botão e Enter consultam sempre, com CEP incompleto avisam "Informe os 8
  dígitos do CEP."): preenche logradouro, bairro e complemento (só se vazio) e o município (campo FK cuja fonte é `Municipio`). Município fora da tabela gera aviso
  "Município não encontrado na tabela; escolha manualmente"; erro do serviço aparece como mensagem do campo e **não trava**
  o formulário.
- **Campo com busca = padrão B (aprovado):** todo campo que dispara uma consulta (CEP hoje; qualquer busca futura) usa o
  `InputGroup` com **botão de busca em destaque colado à direita** (cor `brand-highlight`, ícone de lupa; vira spinner
  enquanto busca). Não use lupa solta nem botão de outro estilo. O estilo está em `SEARCH_BUTTON_CLASS`
  (`runtime/SearchInput.tsx`).
- Validação com Zod no cliente (mesma regra do schema gerado) e **o back decide**: erros 400/409 do servidor aparecem no
  topo do formulário (`serverError`) e, quando o back aponta campo, no campo.
- O botão de salvar mostra estado de envio e fica desabilitado enquanto envia. Salvar nunca duplica o registro.
- **O PUT substitui o registro inteiro.** A edição envia o registro carregado com as alterações por cima; campos que o
  formulário não mostra (referências de integração, datas) seguem como vieram. Campo esvaziado no formulário é apagado.

## 4. Papéis e visibilidade (só UX, o back decide)

- Papéis de empresa: `CONSULTA` < `OPERADOR` < `ADMINISTRADOR` (valor técnico em maiúsculo, como a API devolve na sessão e nos vínculos; os rótulos de tela seguem 'Consulta', 'Operador', 'Administrador'). `hasRole`/`canAct` também aceitam o `role` em minúsculo, porque as telas geradas ainda o emitem. A janela declara o papel mínimo (`role`); ações de criar e
  editar só aparecem para quem tem o papel (`canAct`).
- **Telas só da plataforma** (`platformOnly`): aparecem no menu e agem só para o administrador da plataforma, que **não tem
  papel de empresa**. Grupo **Administração** do menu.
- Esconder um botão nunca substitui a permissão no back. Teste sempre o 403.
- O menu vem do manifesto gerado (`gen/screens.ts`) mais os itens escritos à mão em `config/menu.ts`. Rotas com `$` não
  entram no menu.

## 5. Feedback

- Sucesso: aviso (toast) curto em português, "Registro salvo.". Erro: toast com a mensagem do back (`mensagem` do formato
  uniforme `{codigo, mensagem, detalhes}`), nunca texto técnico, nunca SQL.
- Erros previsíveis têm texto próprio: 401 volta ao login, 403 "sem permissão", 404 "não encontrado", 409 conflito,
  429 "muitas tentativas".
- Ações destrutivas ou irreversíveis pedem confirmação explícita em `AlertDialog` (nunca `Dialog` nem `window.confirm`). (Hoje não há exclusão; vale para o que vier: cancelar
  nota, desativar empresa.)
- **Emissão de nota é ato da pessoa**: o botão **Enviar** da nota é a aprovação. Nenhum fluxo automático ou de agente de IA
  emite nota; rascunho e aprovação por link são do agente de IA. Em desenvolvimento, só homologação.
- **Assistente de IA nas notas** (`features/notas/NotaDetalhe.tsx`): o estado `rascunho` (nota ou cancelamento) é **só do
  assistente**; a UI nunca prepara, só **aprova** (botão primário "Aprovar e emitir" / "Aprovar cancelamento") ou **descarta**,
  sempre com `AlertDialog`, conforme `podeAprovar`/`podeDescartar` do back. Emissão direta tem `falha_envio` (badge de aviso,
  reenvia como `rejeitada`, mesmo número). **Cancelar NFS-e é delegável**: administrador e plataforma sempre; operador só com
  `podeCancelarNfse` na sessão (leitura tolerante: ausente = só administrador). Sem a permissão o botão some e, no mês vigente,
  aparece "Cancelamento depende de permissão delegada pelo administrador."; a delegação é feita em Administração > vínculos
  (empresa e usuário), com a dica "Administradores sempre podem; operadores só com esta permissão."

## 6. Linguagem e aparência

- Português do Brasil, frases completas, sem jargão técnico na tela. Termos do domínio fiscal como o usuário os conhece
  (Município, Inscrição Municipal, Regime Tributário).
- shadcn/ui sobre Tailwind; componentes em `components/ui`.
- **Use somente os componentes catalogados** em `catalogo-componentes.md` (os 50 de `components/ui`, os do `runtime/` e a
  casca). **É proibido criar componente visual novo**: nada de componente próprio em tela, em `features/` ou em `routes/`,
  nem variante improvisada com classes soltas para imitar um componente que o catálogo já tem. Monte a tela combinando o
  que existe. Se algo realmente não puder ser feito com o catálogo, **pare e peça decisão à pessoa responsável**: um
  componente novo só entra por decisão explícita, e então é incluído no catálogo e na vitrine `/admin/theme` na mesma
  entrega.
- Botão só com ícone leva `aria-label` e `Tooltip` com o nome da ação. Aviso persistente na página usa `Alert`; abas usam `Tabs`; data usa `DatePicker`. O catálogo completo está em `front/docs/catalogo-componentes.md`.
- Ícones: nomes lucide registrados em `runtime/icons.tsx` (`resolveIcon`). Se um nome não existe, o mapa cai num ícone
  padrão: registre o ícone antes de usá-lo.
- Tema claro e escuro funcionam (tokens do tema). Não use cor fixa.
- Acessibilidade: todo campo tem rótulo ligado, erro e dica por `aria-describedby`, foco visível, navegação por teclado
  completa (combobox, diálogo, tabela). Tela não depende só de cor.
- Responsivo: nada de rolagem horizontal da página; tabelas rolam dentro do contêiner.

## 7. Como criar uma tela nova

1. O domínio já existe em `front/mapas/dominio/` (cópia somente leitura do back). Se o campo não está lá, é mudança no
   back primeiro.
2. Escreva a janela no mapa seguindo os modelos: `window/cadastros/pessoa/{GestaoPessoas,FormularioPessoa}.mi`
   (endereço com CEP, seções), `window/cadastros/municipio/*` (`platformOnly`, ação `link`).
3. Registre no `main.mi` e acrescente a chamada `entidade ...` em `scripts/gerar.sh`.
4. `mi push emitesimples-front` (**só com autorização explícita da pessoa responsável, um push por autorização**), depois
   `bash front/mapas/scripts/gerar.sh` no Git Bash. Desligue o `npm run dev` antes de gerar: o Vite sobrescreve rotas
   geradas pelo modelo padrão do TanStack se as pegar no meio da escrita.
5. Rode `tsc -b`, `oxlint`, `vitest run` e `npm run build`. Vitest só funciona com o cwd `C:\` maiúsculo (PowerShell).
6. Regere duas vezes: a segunda saída precisa ser idêntica (o gerador é determinístico).
7. Abra a tela no navegador (`npm run dev:mock` não precisa do back; com o back, use o login de dev).

## 8. O que o gerador ainda não cobre

Marque com `@TODO` no mapa em vez de improvisar na tela: relação 1-N (itens da nota), upload de arquivo em formulário,
campo calculado, ações em lote. O gerador deixa `@TODO` listado ao final do `gerar.sh`.

## 9. Verificação automática do padrão

O script `web/scripts/verificar-padrao-ui.mjs` (Node puro) confere o código de `web/src` contra este padrão. Rode
`npm run verificar:ui` em `front/web`: imprime as violações agrupadas por regra e sai com código 1, ou imprime
`Padrão de UI: OK (N arquivos verificados)` e sai com 0. O mesmo `verificar()` roda como teste do vitest
(`src/test/padrao-ui.test.ts`, que também testa cada regra com arquivos temporários); o `gerar.sh` deve executar
`npm run verificar:ui` ao final da geração.

Regras (ignoram `*.test.*`, `routeTree.gen.ts`, `components/ui/**`, `test/**` e `mocks/**`):

- **R1 Sem componente visual novo**: nenhum `.tsx` declara componente (nome em maiúscula que devolve JSX), exceto arquivos
  gerados (`// GERADO por` na primeira linha), rotas (`routes/**`) e as telas escritas à mão listadas em `TELAS_A_MAO`.
  Em `components/` (fora de `ui`) e `runtime/` só valem os arquivos de `COMPONENTES_CATALOGADOS`; um `.tsx` novo ali falha.
- **R2 Imports permitidos nas telas** (`features`, `routes`, `gen`, `examples`): `@/components/ui/*`, `@/runtime/*`,
  `@/lib/*`, `@/gen/*`, `@/features/*`, `@/config/*`, `@/hooks/*`, `@/components/{AppShell,EnvBadge,ThemeToggle}`, imports
  relativos e os pacotes react, react-dom, `@tanstack/*`, react-hook-form (e `@hookform/resolvers`), zod, lucide-react,
  sonner, date-fns e recharts (só em `features/admin`). Radix direto é proibido: use `components/ui`.
- **R3 Sem cor fixa**: nada de utilitário de cor da paleta do Tailwind (`bg-red-500`), `text-white`, `bg-white`,
  `bg-black`, `text-black`, `#hex`, `rgb()` ou `hsl()` em classe ou estilo. Exceção: a vitrine (`GuiaDeEstilo*`,
  `GuiaComponentes*`, `GuiaFormulario`).
- **R4 Formulário gerado**: em cada `Formulario*.tsx` gerado, todo campo de `FIELDS` tem `size` e cada linha soma 12
  (xs=2, s=3, m=4, l=6, xl=8, full=12); toda seção tem título.
- **R5 Cadastro sem criação separada**: `Gestao*Screen.tsx` gerado não usa `Dialog`, `useState` nem `useCriar*` (a criação
  é a rota `/x/novo`); não existem `Criar*.tsx` nem `Editar*.tsx` em `features`.
- **R6 Botão só com ícone** (`size="icon"`) leva `aria-label` na mesma tag.

**Registrar uma tela escrita à mão legítima**: só por decisão explícita da pessoa responsável. Acrescente o caminho
(relativo a `src/`) em `TELAS_A_MAO` no script, com um comentário de justificativa, e inclua o componente no catálogo e na
vitrine na mesma entrega. Nunca amplie a lista para fazer a verificação passar.

Telas escritas à mão registradas hoje: vitrine (`GuiaDeEstilo*`, `GuiaComponentes*`, `GuiaFormulario`), `ImportarMunicipios`, `PlatformOnly`, `UsuariosAdmin` e `ConsultaCnpjAdmin` (configuração singleton da plataforma, `/admin/consulta-cnpj`, fora do padrão lista+formulário; inclui a seção "Testar consulta" com `SearchInput` padrão B, que consulta o CNPJ.ws com a configuração já salva).
