# Memória — cli-runner (emitesimples)

Tier B. Fatos verificados em 2026-11-20.

- Projeto no CLI: `emitesimples`. **Inicializado e enviado em 2026-11-20**: `mi init` em `mapas/` (a home do projeto), `push` autorizado pelo usuário. A escada check, load, compile dos 8 sub-geradores, struct e generate passou para `Municipio` e `CurrentSchema`, sem `EMI`. Todo novo `push` ainda publica na nuvem: avise.
  Mapa principal: `mapas/main.mi` (existe). 12 entidades em 5 contextos sob `domain`: configuracao (Empresa),
  cadastros (Municipio, Pessoa, Servico), usuarios (Usuario, VinculoUsuario, ClienteAcesso, TokenRenovacao), nfse (Nota,
  Cancelamento), integranotas (ChamadaGateway, EventoWebhook). Renomeado em 2026-11-20 (era Emitente/Tomador e os
  contextos cadastro/emissao/integracao/acesso); a nuvem só reflete o renome depois de um novo `push`.
- CLI instalado em `C:\Program Files\mapperidea\mapperidea-cli-1.0.8\mi.exe` (reporta `mi version 1.0.9`).
  O Git Bash o encontra no PATH. Comandos: `authorize`, `check`, `compile`, `generate`, `init`, `load`, `push`.
- **No PowerShell, `mi` é alias de `Move-Item`** e move arquivos. Use o Bash, ou `mi.exe`.
- Licença em `C:\Users\kleit\.mapperidea\authorization.js`; não apareceu "Error loading authorization".
- **`push` publica os mapas na conta em nuvem do usuário.** Avise antes do primeiro `push` e espere o aval.
  Nunca rode `authorize`.
- Você não fala com a API do IntegraNotas e não emite nota.
- Parâmetros do `generate` divergem por gerador: `struct` usa `className`+`packageName`; os `quarkus` usam `modelName`+`package` (pacote COMPLETO do DOM, ex.: `br.com.emitesimples.domain.cadastros`). Param errado = saída vazia, sem erro. `swagger restAPI` usa `packageName`.
- Validação 4 (2026-11-20): segundo push feito (ValorTeste desregistrado, EMI9001 do swagger corrigido); escada e 8 compiles OK. Não há mais push autorizado sem novo aval.
- **Prova de compilação (2026-11-20)**: o código gerado vive no projeto irmão `emitesimples-api` (`scripts/gerar.sh` gera 72
  arquivos Java + 5 OpenAPI a partir do mapa na nuvem, sem push). Com JDK 25.0.5.1 + Maven 3.10.1 + Quarkus 3.33.4.2,
  `mvn -B compile` e `mvn -B package -DskipTests` dão BUILD SUCCESS (76 fontes). Só o `javac` pega erros que o `struct` e o
  `generate` não pegam (ex.: parâmetros duplicados `<relação><Campo>`). Para rodar o Maven sem mexer no PATH do Windows:
  `export JAVA_HOME="/c/Program Files/Java/jdk-25.0.5.1"; export PATH="$JAVA_HOME/bin:/c/Program Files/apache-maven/apache-maven-3.10.1/bin:$PATH"`.
  Se o `clean` falhar com "Failed to delete target\classes", é lock de processo: apague `target/` e rode sem `clean`.
