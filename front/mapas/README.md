# front/mapas

Home do projeto Mapper Idea do front (`emitesimples-front` no CLI). Projeto separado do back (`emitesimples`).

```
main.mi                         registra config de tipos, dominio (copia), struct e as telas
dominio/<Contexto>/*.mi         COPIA SOMENTE-LEITURA de back/mapas/<Contexto>/ (nao edite aqui)
generators/struct.mi            copia do struct do back (necessario para validar com `struct`)
window/cadastros/pessoa/        mapas de tela: GestaoPessoas (list) e FormularioPessoa (editor, formulario UNICO)
window/cadastros/municipio/     GestaoMunicipios (list, platformOnly, sem exclusao) e FormularioMunicipio (editor)
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
- acao `primary` (`criarPessoa`, `opens FormularioPessoa`): `screenPage` abre o form num `Dialog` (titulo = label da acao),
  sem `values`, ligado a `useCriarPessoa`;
- acao `row` (`editarPessoa`, `opens FormularioPessoa`): navega para `/pessoas/$id`, que renderiza o mesmo form preenchido
  (`screenEditorById`, `useAtualizarPessoa`).

Propriedades do campo no layout do form (`form/rows//field/@`):
- `width`, `mask`, `label/pt`: como antes. `label/pt` tambem vale para relacao (senao usa o `title/pt` da relacao no dominio).
- `hint/pt` -> `hint: '...'` (texto de ajuda); `lookup` (valor `cep`) -> `lookup: 'cep'`.
- campo de relacao to-one (`[r]`, mode oneToOne/manyToOne) -> `fk: fkSource('<Entidade>')` (import de `@/lib/fk-sources`);
  o zod da relacao e `z.looseObject({ id: z.string() })`, opcional exceto relacao `required` que nao seja `tenantKey`.
- `[e] section` + `[v] Titulo` direto em `rows` (irmao de `row`/`field`): marcador de secao, hoje so emite comentario
  `// --- Titulo ---` no FIELDS (o runtime ainda nao tem cabecalho de secao).

## Propriedades novas (Municipio)

- `platformOnly` (valor `true`) na janela: `screenManifest` emite `platformOnly: true` no `ScreenEntry` (campo opcional
  `platformOnly?: boolean`). E so UX de menu (administrador da plataforma); o back decide. Independe de `role`.
- Acao `type = link` com `route`: `screenPage` renderiza `<Button variant="outline" asChild><Link to="<route>">` com o icone
  e o label da acao, na barra de ferramentas ao lado do botao de criar (`toolbarActions`). Nao abre dialog nem usa `opens`.
  Sem acao `link` a saida da lista e identica a anterior (Pessoa nao muda).
- `gerar.sh` usa a funcao `entidade <Entidade> <contexto> <slug> <rota> <Lista> <Formulario>`; Pessoa e Municipio saem dela.
- Icone `map-pin` nao existe em `runtime/icons.tsx`; as janelas de Municipio usam `settings`. Para trocar, acrescente o icone la.

## Lacunas encontradas

Dialeto (plano secao 5):
1. **Relacao `.cidade.nome` na LISTA**: `.relacao.campo` como coluna segue futuro (o col-todo permanece). No form a relacao
   to-one ja funciona via `fk: fkSource(...)` (ver acima); o rotulo exibido do Municipio e responsabilidade do `fkSource`
   do runtime (`nome` + `uf`), nao do mapa. Nao ha como o mapa escolher quais campos da entidade-alvo aparecem/buscam.
2. **Mascara condicional**: `documento` deveria ser CPF ou CNPJ conforme `tipoPessoa`/tamanho. O dialeto so tem mask fixa
   (`cpf`|`cnpj`). A coluna e o campo ficaram SEM mascara. Proposta: `mask cpfCnpj` (decide por tipo/numero de digitos).
3. **Secoes no form**: `[e] section` + `[v] Titulo` ja existe no mapa/gerador, mas so vira comentario no `FIELDS`. Falta o runtime
   aceitar um item de layout de cabecalho (proposta: `FieldLayout` ganhar `{ section: string }` e o gerador emiti-lo no lugar do comentario).
   Tambem o nome (`section` com `[v]` direto, nao `title/pt`) e convencao nossa, nao do plano.
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
