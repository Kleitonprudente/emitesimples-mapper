# front/mapas

Home do projeto Mapper Idea do front (`emitesimples-front` no CLI). Projeto separado do back (`emitesimples`).

```
main.mi                         registra config de tipos, dominio (copia), struct e as telas
dominio/<Contexto>/*.mi         COPIA SOMENTE-LEITURA de back/mapas/<Contexto>/ (nao edite aqui)
generators/struct.mi            copia do struct do back (necessario para validar com `struct`)
window/cadastros/pessoa/        mapas de tela: GestaoPessoas (list) e FormularioPessoa (editor, formulario UNICO)
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
- `hint/pt` -> `hint: '...'` (texto de ajuda); `lookup` (valor `cep`) -> `lookup: 'cep'`.
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
| `campo <.nome> com lookup '<x>' em <Janela> (so 'cep' e aceito)` | `lookup` so aceita `cep`; remova ou troque. |

`screenPage` (lista):

| Mensagem | Como corrigir |
|---|---|
| `lista <Lista> abre '<X>' fora da pagina unica: cadastro deve criar na pagina unica: opens deve apontar para window.editor com rota /x/$id` | a acao `primary` (ou o `opens` da lista) aponta para `window.dialog`, ou para `window.editor` cuja rota nao tem `$`. Aponte para o editor do cadastro (`/pessoas/$id`). |

Padrao: criar e editar em pagina unica. O ramo Dialog do `screenPage` ainda existe no gerador, mas o caminho "criar em dialog a
partir de uma lista" agora e ERRO (o codigo e gerado, junto do erro, so para diagnostico).

## Propriedades novas (Municipio)

- `platformOnly` (valor `true`) na janela: `screenManifest` emite `platformOnly: true` no `ScreenEntry` (campo opcional
  `platformOnly?: boolean`). E so UX de menu (administrador da plataforma); o back decide. Independe de `role`.
- Acao `type = link` com `route`: `screenPage` renderiza `<Button variant="outline" asChild><Link to="<route>">` com o icone
  e o label da acao, na barra de ferramentas ao lado do botao de criar (`toolbarActions`). Nao abre dialog nem usa `opens`.
  Sem acao `link` a saida da lista e identica a anterior (Pessoa nao muda).
- `gerar.sh` usa a funcao `entidade <Entidade> <contexto> <slug> <rota> <Lista> <Formulario>`; Pessoa e Municipio saem dela.
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
5. **Lista `contatos` (`[o] contatos: Contato()`)**: edicao master-detalhe nao existe (`window.iframe`/`useFieldArray` e futuro). Fora do editor. A historia pede varios contatos com tipo, WhatsApp e e-mail.
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
