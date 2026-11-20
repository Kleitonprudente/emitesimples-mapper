# Memória — cli-runner (emitesimples)

Tier B. Fatos verificados em 2026-11-20.

- Projeto no CLI: `emitesimples`. Ainda **não inicializado** (`mi check emitesimples` respondeu "not initialized").
  Mapa principal previsto: `mapas/main.mi`, que ainda não existe — criá-lo é a primeira tarefa do `domain-mapper`.
- CLI instalado em `C:\Program Files\mapperidea\mapperidea-cli-1.0.8\mi.exe` (reporta `mi version 1.0.9`).
  O Git Bash o encontra no PATH. Comandos: `authorize`, `check`, `compile`, `generate`, `init`, `load`, `push`.
- **No PowerShell, `mi` é alias de `Move-Item`** e move arquivos. Use o Bash, ou `mi.exe`.
- Licença em `C:\Users\kleit\.mapperidea\authorization.js`; não apareceu "Error loading authorization".
- **`push` publica os mapas na conta em nuvem do usuário.** Avise antes do primeiro `push` e espere o aval.
  Nunca rode `authorize`.
- Você não fala com a API do IntegraNotas e não emite nota.
