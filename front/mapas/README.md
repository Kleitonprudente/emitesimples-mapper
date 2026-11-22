# front/mapas

Home do projeto Mapper Idea do front (`emitesimples-front` no CLI). Projeto separado do back (`emitesimples`).

```
main.mi                         registra config de tipos, dominio (copia), struct e as telas
dominio/<Contexto>/*.mi         COPIA SOMENTE-LEITURA de back/mapas/<Contexto>/ (nao edite aqui)
generators/struct.mi            copia do struct do back (necessario para validar com `struct`)
window/cadastros/cliente/       GestaoClientes (list, fixedFilter .cliente) e FormularioCliente (editor, fixedValues .cliente)
window/cadastros/fornecedor/    GestaoFornecedores (list, fixedFilter .fornecedor) e FormularioFornecedor (editor, fixedValues .fornecedor)
window/cadastros/municipio/     GestaoMunicipios (list, platformOnly, sem exclusao) e FormularioMunicipio (editor)
window/configuracao/empresa/    GestaoEmpresas (list, platformOnly, sem exclusao) e FormularioEmpresa (editor, platformOnly)
scripts/sincronizar-dominio.sh  copia/verifica o dominio
```

Por que o dominio esta aqui: o mapa de tela resolve `.campo` contra o `[b]` do dominio no MESMO projeto. O
`main.mi` usa o mesmo esquema de pacotes do back (`br.com.emitesimples.domain.<contexto>`); os geradores quarkus e
swagger e o bean `CurrentSchema` nao foram trazidos. O back e a fonte da verdade: corrija o dominio la e sincronize.

## Sincronizar o dominio (Git Bash)

```bash
front/mapas/scripts/sincronizar-dominio.sh              # copia back -> front (byte a byte)
front/mapas/scripts/sincronizar-dominio.sh --verificar  # nao copia; exit 1 listando divergentes/orfaos
```

Cobre os 6 contextos (Configuracao, Cadastros, Usuarios, Nfse, IntegraNotas, Admin) e `generators/struct.mi`. Se o
back ganhar ou perder um `[b]`, ajuste tambem o registro em `main.mi` (o script nao edita o `main.mi`). Use `--verificar` em CI.

## Como validar (passos FUTUROS, so com autorizacao do usuario)

Use sempre Git Bash (no PowerShell `mi` e alias de `Move-Item`). O `mi init` ja foi feito (local, sem lastPush).

- `mi push emitesimples-front` publica na nuvem: so com autorizacao explicita.
- `mi check` / `mi load` imprimem sucesso mesmo sem validar; nao sao prova. A prova e o `struct`.
- `mi generate emitesimples-front struct xml className=<Tela> packageName=br.com.emitesimples.window.cadastros.pessoa`

Se mover a pasta, rode `mi init emitesimples-front main.mi` de novo dentro dela (apaga o `lastPush`).

## Formulario unico (convencao)

Um conceito, uma janela, um arquivo: cada entidade tem UMA janela de formulario (`FormularioPessoa`, estereotipo
`Descriptor.window.editor` com `route` `/pessoas/$id`). Escolheu-se `window.editor` e nao um `window.form` novo porque o
editor ja e aceito pelo servidor e ja dispara `screenRoute`/`screenEditorById`. O gerador `screenForm` produz
`FormularioPessoa.tsx` (`FormularioPessoaForm`, com `values?`) e a lista referencia a MESMA janela em duas acoes:
- acao `primary` (`criarPessoa`, `opens FormularioPessoa`): `screenPage` gera um botao que navega para a rota do editor com
  `params: { id: 'novo' }` (sem Dialog); `canAct(session, role, platformOnly)` como antes;
- acao `row` (`editarPessoa`, `opens FormularioPessoa`): navega para `/pessoas/$id` com o id da linha.

`screenEditorById` limita a largura (`max-w-5xl` no container) e, no modo edicao, quando o dominio tem `dataCriacao` e
`dataAtualizacao`, emite apos o formulario a linha `Criado em ... · Atualizado em ...` (`formatDateTime` de `@/runtime/format`).

Criar e editar usam a MESMA pagina de rota (`/pessoas/$id`): o id literal `novo` significa CRIAR (`/pessoas/novo`), qualquer
outro id edita. `screenEditorById` gera `FormularioPessoaScreen` como despachante entre dois componentes (hooks fixos por
componente): `FormularioPessoaNovo` (`useCriarPessoa`, form vazio, titulo = label da acao primary da lista, ex. "Nova pessoa";
sem a acao, "Novo <Bean>") e `FormularioPessoaEditar` (`usePessoa(id)`, `useAtualizarPessoa`, merge `{ ...preservar, ...v }`
so aqui). Ambos voltam a lista em sucesso/cancelamento (`mutationFeedback`). `screenRoute` nao mudou.
O ramo Dialog do `screenPage` so sobrevive no gerador: acao `primary` que `opens` um `window.dialog`, ou um `window.editor` sem `$`
na rota, agora e ERRO NO MODELO (ver "Regras impostas pelo gerador"); nenhum mapa atual usa.

Propriedades do campo no layout do form (`form/rows//field/@`):
- `size` (`xs|s|m|l|xl|full`) OBRIGATORIO em todo campo de `row` -> `size: 'l'` no FieldSpec (grade de 12 colunas, tabela abaixo);
  faltando ou invalido e ERRO NO MODELO. (O tamanho automatico do runtime fica so como rede de seguranca.) `width` esta DEPRECADO: mapa antigo que ainda o
  declare e aceito, mas ele e ignorado (nao emite nada); remova.
- `mask`, `label/pt`: como antes. `label/pt` tambem vale para relacao (senao usa o `title/pt` da relacao no dominio).
- `hint/pt` -> `hint: '...'` (texto de ajuda); `lookup` (valor `cep` ou `cnpj`) -> `lookup: 'cep'` / `lookup: 'cnpj'`
  (botao de busca padrao B; `cnpj` e usado em `FormularioEmpresa.documento`, junto de `mask documento`, que o gerador emite como `mask: 'documento'`).
- campo de relacao to-one (`[r]`, mode oneToOne/manyToOne) -> `fk: fkSource('<Entidade>')` (import de `@/lib/fk-sources`);
  o zod da relacao e `z.looseObject({ id: z.string() })`, opcional exceto relacao `required` que nao seja `tenantKey`.
- `[e] section` + `[v] Titulo` direto em `rows` (irmao de `row`/`field`): secao; emite o item de layout
  `{ section: 'Titulo' }` no FIELDS, antes das linhas da secao. Propriedades da secao (bloco `@` filho da secao):
  `description/pt` -> `description: '...'`; `collapsible` = `true` -> `collapsible: true`; `defaultOpen` = `true` ->
  `defaultOpen: true` (so faz sentido com `collapsible`). Sem propriedades, so `{ section: 'Titulo' }`.

### Grade de 12 colunas (regra "a linha soma 12")

Cada `row` e uma linha da grade; a soma dos `size` dos campos da linha deve dar EXATAMENTE 12 (imposto pelo gerador `screenForm`, que
e a fonte unica desta tabela; o verificador do front le a mesma tabela):

| size | colunas | uso tipico |
|---|---|---|
| `xs` | 2 | UF, codigos curtos |
| `s` | 3 | documento, CEP, numero, telefone, data, booleano |
| `m` | 4 | complemento, bairro, municipio, enum |
| `l` | 6 | nome, e-mail, FK, logradouro |
| `xl` | 8 | URL, textos longos |
| `full` | 12 | textarea, linha inteira |

Exemplos: Identificacao `[s s s s]`; `[l l]`; Endereco `[s l s]` e `[m m m]`; Municipio `[m l xs]`.

## Regras impostas pelo gerador

O padrao de UI (`front/docs/padroes-de-ui.md`) e imposto na geracao: o gerador escreve `// ERRO NO MODELO: ...` no topo da saida
(logo apos a linha `GERADO por ...`) e o `scripts/gerar.sh` recusa o arquivo (FALHA, nada e gravado). Corrija o MAPA, nunca a saida.
O verificador `web/scripts/verificar-padrao-ui.mjs` (se existir) roda ao final do `gerar.sh`; falha dele faz o script sair com 1.

`screenForm` (formulario):

| Mensagem | Como corrigir |
|---|---|
| `campo <.nome> sem size em <Janela> (declare xs\|s\|m\|l\|xl\|full)` | declare `size` no `field` (tabela acima). Todo campo de `row` exige. |
| `campo <.nome> com size '<x>' invalido em <Janela> ...` | use um de `xs, s, m, l, xl, full`. |
| `linha [<campos>] soma N colunas (esperado 12) em <Janela>` | ajuste os `size` da linha (ou mova campo de linha) ate somar 12. So e emitido quando todos os campos da linha tem size valido. |
| `secao sem titulo em <Janela> (informe o titulo da secao)` | coloque o titulo em `[v]` direto na `section`. |
| `campo <.nome> com lookup '<x>' em <Janela> (so 'cep' e 'cnpj' sao aceitos)` | `lookup` so aceita `cep` ou `cnpj`; remova ou troque. |

`screenEditorById` (abas extras; veja "Abas extras no editor"):

| Mensagem | Como corrigir |
|---|---|
| `aba '<T>' sem component em <Janela> (declare o nome do componente escrito a mao)` | declare `[e] component` no bloco `@` da `tab`. |
| `aba '<T>' sem from em <Janela> (declare o caminho de import em @/features/)` | declare `[e] from` no bloco `@` da `tab`. |
| `aba '<T>' com from '<x>' invalido em <Janela> (so componentes escritos a mao em @/features/ sao aceitos)` | `from` deve comecar com `@/features/`. |

`screenPage` (lista):

| Mensagem | Como corrigir |
|---|---|
| `lista <Lista> abre '<X>' fora da pagina unica: cadastro deve criar na pagina unica: opens deve apontar para window.editor com rota /x/$id` | a acao `primary` (ou o `opens` da lista) aponta para `window.dialog`, ou para `window.editor` cuja rota nao tem `$`. Aponte para o editor do cadastro (`/pessoas/$id`). |

Padrao: criar e editar em pagina unica. O ramo Dialog do `screenPage` ainda existe no gerador, mas o caminho "criar em dialog a
partir de uma lista" agora e ERRO (o codigo e gerado, junto do erro, so para diagnostico).

## Dois cadastros sobre a mesma entidade (Clientes e Fornecedores sobre Pessoa)

Uma entidade pode ter varias visoes. `Pessoa` tem os booleanos `cliente` e `fornecedor` (a mesma pessoa pode ser os dois), e o
menu Cadastros tem duas telas: Clientes (`/clientes`, `/clientes/$id`) e Fornecedores (`/fornecedores`, `/fornecedores/$id`),
cada uma com seu par lista + formulario. O zodSchema e o apiClient de Pessoa saem UMA vez (`gen/schemas/pessoa.ts`, `gen/api/pessoa.ts`);
`gerar.sh` usa `dominio Pessoa cadastros` e duas chamadas `telas ...` (`entidade` = `dominio` + `telas`, usada por Municipio e Empresa).

### `fixedFilter` (janela `window.list`)

```
[e] fixedFilter
    [e] field
        [v] .cliente
        @
            [e] value
                [v] true
```
Cada `field` e um atributo escalar do bean da `source`; `value` e o valor LITERAL (`true`|`false`|numero|texto). O `screenPage` emite
`usePessoas(state, 'nome', { cliente: true })`: o filtro entra na querystring da lista e do `/count` e na chave de cache
(o `apiClient` ganhou o 3o parametro opcional `filtros?: Record<string, string | number | boolean>`; chamadas antigas nao mudam).
O filtro fixo vence um filtro de mesmo nome da tabela. Sem `fixedFilter` a saida da lista e identica.

### `fixedValues` (janela `window.editor`)

Mesmo formato. Os campos fixos NAO aparecem no formulario (nao os liste em `form`). O `screenEditorById` emite
`const FIXED_VALUES = { cliente: true }` e, so no CRIAR, `criar.mutateAsync({ ...v, ...FIXED_VALUES })`. Na EDICAO nao sobrescreve: o PUT
preserva o que o registro ja tem (a pessoa pode ser cliente e fornecedor). Sem `fixedValues` a saida e identica. O titulo do modo novo
continua sendo o label da acao `primary` da lista ("Novo cliente").

### ERRO NO MODELO novos

| Gerador | Mensagem | Como corrigir |
|---|---|---|
| `screenPage` | `fixedFilter <.campo> sem atributo escalar correspondente em <Bean> na lista <Janela> ...` | o campo deve ser atributo (`directToField`) do bean da `source`. |
| `screenPage` | `fixedFilter <.campo> sem value em <Janela> (declare o valor literal: ...)` | declare `[e] value` no `@` do `field`. |
| `screenEditorById` | `fixedValues <.campo> sem atributo escalar correspondente em <Bean> no editor <Janela> ...` | idem. |
| `screenEditorById` | `fixedValues <.campo> sem value em <Janela> ...` | idem. |
| `screenEditorById` | `fixedValues <.campo> tambem aparece no formulario de <Janela> ...` | tire o campo de `form`: campo fixo nao e editavel. |

## Propriedades novas (Municipio)

- `platformOnly` (valor `true`) na janela: `screenManifest` emite `platformOnly: true` no `ScreenEntry` (campo opcional
  `platformOnly?: boolean`). E so UX de menu (administrador da plataforma); o back decide. Independe de `role`.
- Acao `type = link` com `route`: `screenPage` renderiza `<Button variant="outline" asChild><Link to="<route>">` com o icone
  e o label da acao, na barra de ferramentas ao lado do botao de criar (`toolbarActions`). Nao abre dialog nem usa `opens`.
  Sem acao `link` a saida da lista e identica a anterior (Pessoa nao muda).
- `gerar.sh` usa `entidade <Entidade> <contexto> <slug> <rota> <Lista> <Formulario>` (= `dominio` + `telas`); Municipio e Empresa saem dela. Pessoa usa `dominio` + duas `telas` (Clientes e Fornecedores).
- Icone `map-pin` nao existe em `runtime/icons.tsx`; as janelas de Municipio usam `settings`. Para trocar, acrescente o icone la.

## Empresa (tenant, so administrador da plataforma)

- `GestaoEmpresas` (`/empresas`, grupo Administracao, order 78, `platformOnly`, icone `settings` porque `building` nao existe em
  `runtime/icons.tsx`) e `FormularioEmpresa` (`/empresas/$id`, `platformOnly`). Sem acao de excluir: a empresa nunca e apagada,
  so desativada pelo campo `situacao` (no formulario, secao Conta).
- Fora do formulario de proposito: `storageOnly` (`referenciaTokenEmpresa`, `referenciaCredenciaisPrefeitura`,
  `referenciaCertificado`), `logo`, controle (`id`, `ambiente`, datas), gateway e certificado.
- `gerar.sh`: `entidade Empresa configuracao empresa empresas GestaoEmpresas FormularioEmpresa` (a funcao ja deriva o pacote de dominio
  de `<contexto>`; Pessoa e Municipio nao mudam).
- Lookup de CEP: o runtime (`AutoForm`) agora preenche o campo FK cuja fonte e `Municipio` (`fk.key`), seja `cidade` (Pessoa) ou
  `municipio` (Empresa); sem fk declarado, cai em `cidade`.

## Lista de detalhes 1-N no formulario (`list`)

O mapa ja declara a relacao `[o]` (ex.: `Pessoa.contatos: Contato()`, inversa `[m] pessoa` em `Contato`). No `form/rows` do editor:

```
[e] list
    [v] .contatos                 // nome da relacao [o] da source
    @
        [e] label / [e] pt / [v] Contatos       // opcional (senao title/pt da relacao)
        [e] itemLabel / [v] Contato             // obrigatorio
        [e] addLabel  / [v] Adicionar contato   // obrigatorio
        [e] emptyText / [v] Nenhum contato adicionado.   // opcional
        [e] maxItems  / [v] 10                  // opcional, 1 a 100
    [e] rows                       // campos resolvidos contra o [b] do TIPO da relacao (Contato), nao da source
        [e] row
            [e] field ...          // mesmas regras: size obrigatorio, linha soma 12, mask, enum, fk
```
(`itemLabel`/`addLabel`/`emptyText`/`maxItems` tambem sao lidos como filho direto da `list`, sem `@`, caso o servidor descarte a chave no `@`.)

Saida: `screenForm` emite o campo `{ name: 'contatos', kind: 'list', size: 'full', list: { itemLabel, addLabel, emptyText?, maxItems?, item: [linhas] } }`
dentro de uma linha propria; o schema `…Schema.pick({...}).extend({ contatos: z.array(contatoItemSchema) })` com
`contatoItemSchema = contatoSchema.pick({...}).extend({ id: z.string().optional() })` (importa `contatoSchema` e os enums de `@/gen/schemas/contato`,
que o `gerar.sh` gera com `zodSchema modelName=Contato`); `DEFAULTS` com `contatos: []`; `FieldNames` inclui `contatos`.
So UMA `list` por formulario.

Contrato com o back (escrito a mao, NAO e o CRUD gerado de Contato): `GET /Pessoa/{id}/contatos` e `PUT /Pessoa/{id}/contatos` (lista desejada
completa, tudo ou nada). Convencao `GET|PUT /{Entidade}/{id}/{relacao}`. O `apiClient` emite, para cada `[o]` do bean, `use<Relacao>De<Bean>(id)`
(ex.: `useContatosDePessoa`) e `useSalvar<Relacao>De<Bean>()` (ex.: `useSalvarContatosDePessoa`, chamada `mutateAsync({ id, itens })`). Nao ha
`gen/api/contato.ts` nem `sincronizarDetalhes`. Efeito: `gen/api/empresa.ts` ganha hooks de `atividades` e `qsa` (sem uso; os endpoints nao existem no back).

`screenEditorById` com `list`: CRIAR salva o pai e depois faz o PUT dos itens; EDITAR carrega pai + itens (Loading/Error cobrem os dois), salva o pai
(sem a lista, `{ ...preservar, ...dados }`) e depois o PUT dos itens. **Falha parcial**: o pai ja esta salvo; toast `<Cliente> salvo, mas os contatos nao foram
salvos: <mensagem do back>. Revise na edicao.`; no criar navega para a edicao do salvo, no editar permanece. O nome (`Cliente`) sai do label da acao `primary` da lista.
Sem `list`, a saida das telas e identica a anterior.

ERRO NO MODELO novos (`screenForm`; `screenEditorById` repete multiplas list e relacao invalida):

| Mensagem | Como corrigir |
|---|---|
| `list <.x> sem relacao de detalhe [o] em <Bean> ...` | `.x` deve ser `[o]` do bean da source. |
| `list <.x>: <Item> nao tem relacao [m] para <Bean> ...` | o detalhe precisa da inversa `[m]` (a que carrega o pai). |
| `list <.x> sem itemLabel` / `sem addLabel` | declare no `@` da list. |
| `list <.x> com maxItems '<v>' invalido ...` | inteiro de 1 a 100. |
| `list <.x> sem campos` / `com list aninhada` | declare `rows/row/field`; nao aninhe list. |
| `mais de uma list em <Janela>` | so uma lista de detalhes por formulario. |
| campo/linha/secao dentro da list | mesmas regras de size, soma 12 e titulo de secao. |

## Abas extras no editor

A janela `window.editor` pode declarar, como filho direto (irmao de `form` e `actions`):

```
[e] mainTabTitle          // opcional; titulo da aba do formulario (padrao 'Dados')
    [v] Dados
[e] extraTabs
    [e] tab
        [v] Usuários      // titulo da aba
        @
            [e] component
                [v] VinculosDaEmpresa
            [e] from
                [v] @/features/admin/VinculosDaEmpresa
```

Saida em `Formulario<X>Screen` (so no `screenEditorById`): em EDICAO (`id !== 'novo'`) a pagina vira `Tabs` (`@/components/ui/tabs`),
com a aba do formulario primeiro e uma aba por `tab`, cada uma renderizando `<Componente <entidade>Id={id} />` (ex.: `empresaId`).
Em CRIACAO (`id === 'novo'`) os gatilhos extras saem `disabled` e aparece o texto "Salve a empresa para continuar.". Sem `extraTabs` a
saida e identica a anterior (Pessoa e Municipio nao mudam). O formulario continua com suas secoes: nao e preciso (nem permitido) criar
secoes para ativar abas.

Regras:
- O componente e escrito a mao em `src/features/admin/*` e **precisa estar em `TELAS_A_MAO`** de `web/scripts/verificar-padrao-ui.mjs`
  (decisao explicita, como qualquer tela a mao). O gerador nao cria componente visual: so emite o import e o JSX com
  `Tabs`, `TabsList`, `TabsTrigger`, `TabsContent` do catalogo.
- `from` deve comecar com `@/features/`; o componente recebe a prop `<entidade>Id` (primeira letra minuscula do bean + `Id`).
- Erros no modelo: ver tabela de `screenEditorById` acima.

## Lacunas encontradas

Dialeto (plano secao 5):
1. **Relacao `.cidade.nome` na LISTA**: `.relacao.campo` como coluna segue futuro (o col-todo permanece). No form a relacao
   to-one ja funciona via `fk: fkSource(...)` (ver acima); o rotulo exibido do Municipio e responsabilidade do `fkSource`
   do runtime (`nome` + `uf`), nao do mapa. Nao ha como o mapa escolher quais campos da entidade-alvo aparecem/buscam.
2. **Mascara condicional**: `documento` deveria ser CPF ou CNPJ conforme `tipoPessoa`/tamanho. O dialeto so tem mask fixa
   (`cpf`|`cnpj`). A coluna e o campo ficaram SEM mascara. Proposta: `mask cpfCnpj` (decide por tipo/numero de digitos).
3. **Secoes no form**: RESOLVIDO no gerador (emite `{ section: ... }`, com `collapsible`/`defaultOpen`/`description`); depende do runtime
   aceitar o item no `FieldLayout`. O nome (`section` com `[v]` direto, nao `title/pt`) e convencao nossa, nao do plano.
4. **Campos de exportacao condicionais** (`codigoPais`, `codigoPostal`, `nomeMunicipioExterior`, `nomeProvincia`): sempre
   visiveis no form (agora numa secao "Pessoa estrangeira (exportacao)" abaixo do endereco). Falta `condition` em `field`/`section` (so existe em action/step).
5. **Lista `contatos` (`[o] contatos: Contato()`)**: RESOLVIDO com `[e] list` no editor (ver "Lista de detalhes 1-N no formulario").
6. **Busca em varios campos**: a historia e o dominio marcam `documento`, `nomeFantasia` como `searchable`; a busca da lista usa so `.nome` (pedido). Nao se sabe se `search` aceita varios `field`.
7. **Consulta CNPJ.ws** (botao que preenche o form): sem construto no dialeto. A consulta de CEP foi coberta por convencao:
   propriedade de campo `lookup cep` + `hint` (nao constam do plano; o preenchimento dos irmaos por NOME e regra do runtime, o mapa
   nao declara quais campos sao preenchidos).
8. **`filters` faceted**: `.tipoPessoa` tem `values`; o dado e `fisica`/`juridica`. Assumido que o gerador pega as opcoes do enum do dominio.
9. **Atributos de sistema** (`id`, `dataCriacao`, `dataAtualizacao`, `empresa`): excluidos por simples nao listagem no form (o subconjunto e decisao da tela). A `empresa` (tenant) e imposta pelo back.
10. `consumidorFinal`/regra "PJ com IE vira nao" e regra de aplicacao, nao de tela.
11. `window.dialog` com `role` e `window.editor` com `icon` sao usos nao exemplificados no plano (aceitos por analogia); o plano nao define `icon`/`label` para action de linha como usado aqui (`pencil`).

Dominio/CLI:
- O `main.mi` do back tem `config` (mapNativeTypes etc.) duplicado aqui; mudancas no back exigem repetir a mao (o script nao cobre `main.mi`).
- `generators/struct.mi` foi copiado alem do dominio (a tarefa listava so o dominio); o `struct` nao funciona sem ele.
- Papel `operador` e so UX; o catalogo de papeis do dominio nao foi conferido.

## Nao validado

Tudo o que depende de push: `struct` das 4 telas (inclui as de Municipio e o DOM da acao `link`/`platformOnly`) (GestaoPessoas, FormularioPessoa), DOM de `section`/`hint`/`lookup`, resolucao dos `.campo` contra o dominio, aceitacao pelo servidor dos
estereotipos `Descriptor.window.*`, comentario `//` placeholder em `generators` e DOM real (os nomes de regioes e
propriedades acima seguem o plano, nao um `struct` real).
