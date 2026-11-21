# Exemplo de prompt — pedir uma tela nova ao Claude (projeto com o plano de frontend configurado)

> Pré-requisito: o projeto já segue `plano-metodologia-frontend-mapper.md` (runtime pronto, geradores de
> front registrados, `gen-frontend.sh` funcionando). Troque os trechos entre `< >`.

---

## Versão completa (tela nova sobre uma entidade de domínio existente)

```
Quero uma tela nova seguindo o plano de frontend do projeto (docs/plano-metodologia-frontend-mapper.md).

O que a tela faz:
<Ex.: o operador vê a lista de pedidos, busca por número, filtra por status, cria um pedido novo
num modal e edita um pedido existente numa página própria. "Cancelar" só fica habilitado quando o
status não é CANCELADO.>

Quem usa: papel <operador>. Menu: grupo <Vendas>, ordem <20>. Marca: <todas | marcaA>.

Antes de escrever qualquer coisa:
1. Leia o mapa de domínio da entidade (<mi/vendas/Pedido.mi>) e o registro no mi/main.mi. Liste
   os atributos que a tela vai usar, com tipo, se são obrigatórios e se são enum (com os valores).
   Se algum campo que eu citei NÃO existir no domínio, pare e me diga. Não invente atributo nem
   redeclare enum na tela.
2. Veja as telas que já existem em mi/window/ para essa entidade/contexto e me diga se dá para
   reusar alguma (ex.: um dialog de criação que já existe) em vez de criar outra.
3. Me proponha, em texto, os mapas de tela que vai criar: estereótipo (window.list / dialog /
   editor), rota, colunas (com display/mask), busca, filtros, ações (type, opens/navigate,
   condition), emptyState e o layout do form (rows/row). Espere meu ok.

Depois do meu ok:
4. Escreva os mapas em mi/window/<contexto>/<entidade>/ e registre-os no main.mi.
5. Valide com mi push e rode o struct de cada tela para conferir o DOM.
6. Gere com scripts/mapperidea/gen-frontend.sh: primeiro a entidade (se gen/schemas e gen/api
   ainda não existirem ou estiverem desatualizados), depois cada tela e por fim o manifest.
7. Rode npm run typecheck e npm run build (Node 20). Suba npm run dev:mock e confira a tela:
   lista, busca, filtro, estado vazio (?empty=1), criar e editar.

Regras:
- Não edite à mão nada que foi gerado (gen/ e o que o gerador escreveu em features/). Se a tela
  precisar de algo que o gerador não faz, me diga qual é a lacuna e proponha: evoluir o gerador,
  evoluir o runtime ou ejetar a tela. Não resolva no arquivo gerado.
- Se o gerador emitir // @TODO, me liste cada um.
- Sem cor literal, sem fetch fora dos hooks gerados.

No final, me mostre: os mapas criados, os arquivos gerados, os comandos que rodou e o resultado
de cada um, e o que ficou pendente.
```

---

## Versões curtas (para o dia a dia, depois que o fluxo já é conhecido)

**Lista + criação:**
```
Cria a tela GestaoPedidos sobre o domínio Pedido (mi/vendas/Pedido.mi), seguindo o plano de
frontend: lista com número, cliente, status (badge) e data de criação; busca por número; filtro
por status; botão "Novo pedido" abrindo um dialog CriarPedido com cliente, documento (cpf) e tipo
na mesma linha, e observação. Papel operador, menu Vendas. Confira os atributos no domínio antes,
me mostre os mapas propostos, e depois gere, rode o manifest, typecheck e build.
```

**Editor por id para uma lista que já existe:**
```
Adiciona a ação "Editar" na GestaoPedidos abrindo um editor por id (EditarPedido, rota
/pedidos/$id) com os mesmos campos do CriarPedido mais o status. Reuse o que der, regenere a
lista, o editor e o manifest, e valide no dev:mock.
```

**Wizard reusando telas existentes:**
```
Cria a window.operation Ativacao (rota /ativacao) com o mestre Conta: passo 1 reusa EscolherPlano
(opens), passo 2 reusa DadosEmpresa (opens), passo 3 é custom ConectarIntegracao (opcional, só se
plano.exigeIntegracao). Não mova as telas reusadas de pacote. Gere com o screenOperation e me diga
quais passos custom ficaram para escrever à mão.
```

---

## Por que o prompt é assim

| Trecho do prompt | Evita |
|---|---|
| "Leia o domínio e liste os atributos; pare se faltar campo" | tela com campo inventado, que compila mas não existe na API |
| "Não redeclare enum" | rótulos divergindo entre telas |
| "Veja as telas existentes antes" | um segundo dialog de criação para a mesma entidade |
| "Proponha os mapas e espere meu ok" | retrabalho no mapa depois de já ter gerado |
| `push` + `struct` antes de gerar | gerador consumindo um DOM diferente do esperado |
| "Não edite o gerado; aponte a lacuna" | correção perdida na próxima regeneração |
| `manifest` no fim | tela que existe mas não aparece no menu |
| `dev:mock` + `?empty=1` | entregar sem ver os estados de loading/vazio |
