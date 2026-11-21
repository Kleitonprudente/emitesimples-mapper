# front/mapas

Home do projeto Mapper Idea do front (`emitesimples-front` no CLI). Projeto separado do back (`emitesimples`).

```
main.mi                         registra config de tipos, dominio (copia), struct e as telas
dominio/<Contexto>/*.mi         COPIA SOMENTE-LEITURA de back/mapas/<Contexto>/ (nao edite aqui)
generators/struct.mi            copia do struct do back (necessario para validar com `struct`)
window/cadastros/pessoa/        mapas de tela: GestaoPessoas (list), CriarPessoa (dialog), EditarPessoa (editor)
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

## Lacunas encontradas

Dialeto (plano secao 5):
1. **Relacao `.cidade.nome` / FK combobox**: `.relacao.campo` e futuro. A cidade (`[r] cidade: Municipio()`) ficou FORA de
   Criar/Editar. Proposta: `field .cidade` com `@ component combobox`, `@ display` (`nome` + `uf`, escondendo `codigoIbge`) e
   `@ search` apontando `.nome` do Municipio, ex.: `[e] field [v] .cidade @ [e] component [v] combobox [e] lookup [e] label [v] .nome, .uf [e] search [v] .nome`.
   Tambem permitir `.cidade.nome`/`.cidade.uf` como coluna da lista.
2. **Mascara condicional**: `documento` deveria ser CPF ou CNPJ conforme `tipoPessoa`/tamanho. O dialeto so tem mask fixa
   (`cpf`|`cnpj`). A coluna e o campo ficaram SEM mascara. Proposta: `mask cpfCnpj` (decide por tipo/numero de digitos).
3. **Secoes no form**: `rows` nao tem titulo de secao. Identificacao/contato/endereco/exportacao estao so ordenados em linhas, sem cabecalho. Proposta: `[e] section` com `title/pt` dentro de `rows`.
4. **Campos de exportacao condicionais** (`codigoPais`, `codigoPostal`, `nomeMunicipioExterior`, `nomeProvincia`): sempre
   visiveis no editor. Falta `condition` em `field`/`section` (so existe em action/step).
5. **Lista `contatos` (`[o] contatos: Contato()`)**: edicao master-detalhe nao existe (`window.iframe`/`useFieldArray` e futuro). Fora do editor. A historia pede varios contatos com tipo, WhatsApp e e-mail.
6. **Busca em varios campos**: a historia e o dominio marcam `documento`, `nomeFantasia` como `searchable`; a busca da lista usa so `.nome` (pedido). Nao se sabe se `search` aceita varios `field`.
7. **Consulta CNPJ.ws / CEP BrasilAPI** (botoes que preenchem o form): sem construto no dialeto (acao custom no form).
8. **`filters` faceted**: `.tipoPessoa` tem `values`; o dado e `fisica`/`juridica`. Assumido que o gerador pega as opcoes do enum do dominio.
9. **Atributos de sistema** (`id`, `dataCriacao`, `dataAtualizacao`, `empresa`): excluidos por simples nao listagem no form (o subconjunto e decisao da tela). A `empresa` (tenant) e imposta pelo back.
10. `consumidorFinal`/regra "PJ com IE vira nao" e regra de aplicacao, nao de tela.
11. `window.dialog` com `role` e `window.editor` com `icon` sao usos nao exemplificados no plano (aceitos por analogia); o plano nao define `icon`/`label` para action de linha como usado aqui (`pencil`).

Dominio/CLI:
- O `main.mi` do back tem `config` (mapNativeTypes etc.) duplicado aqui; mudancas no back exigem repetir a mao (o script nao cobre `main.mi`).
- `generators/struct.mi` foi copiado alem do dominio (a tarefa listava so o dominio); o `struct` nao funciona sem ele.
- Papel `operador` e so UX; o catalogo de papeis do dominio nao foi conferido.

## Nao validado

Tudo o que depende de push: `struct` das 3 telas, resolucao dos `.campo` contra o dominio, aceitacao pelo servidor dos
estereotipos `Descriptor.window.*`, comentario `//` placeholder em `generators` e DOM real (os nomes de regioes e
propriedades acima seguem o plano, nao um `struct` real).
