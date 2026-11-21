# emitesimples-mapper

Repositório dos **mapas do Mapper Idea** do emissor de NFS-e (integração com o IntegraNotas). Aqui vivem o modelo de
negócio e os geradores. O **código gerado não mora aqui**: vai para projetos irmãos.

## Estrutura

```
back/
  mapas/              home do projeto `emitesimples` no CLI mi (back-end)
    main.mi           mapa principal
    Configuracao/ Cadastros/ Usuarios/ Nfse/ IntegraNotas/ Admin/    um arquivo .mi por entidade, por contexto
    generators/       geradores (pack Quarkus adaptado) e fixture de teste
front/                reservada para o mapa do front-end (projeto separado, ainda não criado)
docs/                 compartilhado: histórias de usuário, documentos do IntegraNotas, contrato do pack, guias
scripts/              hooks usados pelos agentes (limitam a escrita ao projeto)
.claude/
  agents/             domain-mapper, generator-author, cli-runner
  agent-memory/       memória de cada agente
  skills/mapperidea/  sintaxe e referência do Mapper Idea
```

## Regras

- **O back e o front são projetos do Mapper Idea distintos.** A *home* de cada um no CLI é a pasta do seu `main.mi`
  (`back/mapas/`, e no futuro `front/mapas/`). O `mi push` envia tudo o que está dentro da home, então os dois **nunca
  dividem pasta**.
- **O CLI guarda o caminho absoluto da home** em `~/.mapperidea/<projeto>/config.js`. Se a pasta for movida, o projeto
  precisa ser registrado de novo com `mi init <projeto> main.mi`, rodado dentro da nova pasta. **Atenção: o `mi init`
  apaga o `lastPush` do config, que é a ligação com a versão publicada na nuvem.** Depois dele, `struct` e `generate` falham
  ("Project ... not pushed to mapperidea cloud") até um novo `mi push` (que publica na conta em nuvem). Já o `check` e o `load`
  imprimem sucesso mesmo com esse aviso, então o sucesso deles não prova nada.
- Rode o `mi` no **Git Bash**. No PowerShell, `mi` é um alias de `Move-Item`.
- O `mi push` **publica na conta em nuvem** do Mapper Idea. Só com autorização explícita.

## Projetos irmãos (fora deste repositório)

| Pasta | O que é |
|---|---|
| `../emitesimples-api` | projeto Quarkus que compila o código gerado do back (`scripts/gerar.sh` gera; Flyway é o dono do esquema do banco) |
