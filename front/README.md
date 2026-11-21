# front

Reservada para o **mapa do front-end**, que será gerado depois. Nada aqui ainda.

Quando chegar a hora, o front será **um projeto do Mapper Idea separado** do back: com o próprio `main.mi`, os próprios
geradores e um nome próprio no CLI (por exemplo `emitesimples-front`). A pasta do `main.mi` é a *home* do projeto no CLI,
e o `mi push` envia tudo o que está dentro dela. Por isso o back e o front **não podem dividir a mesma pasta**: um
`push` do back não pode levar arquivos do front, nem o contrário.

Estrutura prevista quando o front existir:

```
front/
  mapas/        <- home do projeto do front no CLI (main.mi, telas, geradores)
```

O que for **compartilhado** entre back e front (a linguagem do negócio, as histórias de usuário, os documentos do
IntegraNotas) fica em `../docs/`, e não aqui.
