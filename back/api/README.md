# emitesimples-api

Projeto Quarkus que **compila e (no futuro) executa** o código gerado a partir do mapa de negócio
(o mapa está em `../mapas`, no mesmo repositório). Hoje o objetivo é **provar que o Java gerado compila**.

## Regras

- **Não edite o código gerado.** Ele é sobrescrito por inteiro a cada geração. Corrija o mapa (`domain-mapper`) ou o
  gerador (`generator-author`) em `../mapas`.
- O que é escrito à mão fica em `exceptions/`, `results/`, `tenant/`, `auth/`, `security/`, `cep/` e `plataforma/` (em `src/main/java/br/com/emitesimples/`) e
  nunca é apagado pelo script de geração.
- **Nunca** aponte para a produção do IntegraNotas por padrão. O padrão é homologação.

## Pré-requisitos

- **JDK 21 ou 25** (o `pom.xml` compila com `release 21`, então serve para os dois) e **Maven 3.9 ou superior**.
  **Verificado em 2026-11-20** com JDK 25.0.5.1, Maven 3.10.1 e Quarkus 3.33.4.2 (LTS): `mvn -B compile` e
  `mvn -B package -DskipTests` terminam em BUILD SUCCESS, com 76 fontes (72 geradas e 4 escritas à mão).
- O CLI `mi` no Git Bash, para gerar.

## Gerar e compilar

```sh
# no Git Bash, na raiz deste projeto
./scripts/gerar.sh          # gera 126 arquivos Java (21 entidades) + 6 OpenAPI a partir do mapa na nuvem; NAO faz push
mvn -B clean compile        # prova que o Java gerado compila
```

O `gerar.sh` falha se achar marcador de erro (`EMI`, `@TODO`, `TRUNCADA`) ou arquivo vazio, porque o `mi generate`
devolve 0 mesmo quando falha.

## Estrutura

| Pacote | Conteúdo |
|---|---|
| `br.com.emitesimples.<contexto>.{domain,entity,builder,repository,mapper,resource}` | **gerado**: `configuracao`, `cadastros`, `usuarios`, `nfse`, `integranotas` |
| `br.com.emitesimples.exceptions` | `NotFoundException`, `InvalidInputException` (escritas à mão) |
| `br.com.emitesimples.results` | `CountResult` (escrita à mão) |
| `br.com.emitesimples.tenant` | `EscopoTenant`: filtro multi-tenant por empresa; lê as claims `empresa_id` e `plataforma_admin` do JWT. `ContextoTenantBanco` e `RlsPoolInterceptor`: escopo da RLS no banco (seção RLS) |
| `br.com.emitesimples.auth` | login, refresh, logout, perfil, troca de senha, força bruta, administrador inicial (à mão) |
| `br.com.emitesimples.cep` | consulta de CEP: `CepResource`, `CepService` (cache e cruzamento com `Municipio`), `ViaCepCliente` (java.net.http), `LimiteTaxa` (à mão) |
| `br.com.emitesimples.plataforma` | área do administrador da plataforma: importação de municípios (`ArquivoMunicipios`, `ImportacaoMunicipioService`) e usuários/vínculos (`UsuarioAdminService`) (à mão). **Não use o pacote `admin/`**: o `gerar.sh` o apaga (é o contexto gerado `admin`) |
| `br.com.emitesimples.security` | emissão do JWT, erros uniformes (`ErrosHandler`), 401 em JSON (`DesafioJsonMechanism`) (à mão) |
| `docs/openapi/` | OpenAPI gerado por contexto |

## Autenticação e segurança

Tudo abaixo é escrito à mão (`auth/`, `security/` e `application.properties`) e **não toca no código gerado**. Prova:
`mvn -B test` (testes `@QuarkusTest` contra PostgreSQL real, via Dev Services com a imagem local `postgres:17-alpine`).

### Endpoints (todos sob `/api`, JSON; erros sempre `{codigo, mensagem, detalhes?}`)

| Método e caminho | Acesso | O que faz |
|---|---|---|
| `POST /api/auth/login` `{email, senha, empresaId?}` | público | e-mail + senha. 401 genérico para qualquer falha de credencial. Um vínculo ativo em empresa ativa: usa essa. Vários: exige `empresaId` (409 `empresa_obrigatoria` com `detalhes.empresas` = `[{id, razaoSocial, papel}]`). Administrador da plataforma pode entrar sem empresa. |
| `POST /api/auth/refresh` (sem corpo; cookie `refresh_token` + cabeçalho `X-Requested-With: emitesimples`) | público | novo accessToken e **rotação do cookie**. Sem cookie, token inválido/expirado ou **reuso** (token já usado ou revogado reapresentado: revoga todos os tokens ativos do usuário naquela empresa) = 401 `sessao_invalida` e o cookie é apagado. Sem o cabeçalho: 403 `cabecalho_obrigatorio`. |
| `POST /api/auth/logout` (sem corpo; cookie + cabeçalho `X-Requested-With: emitesimples`) | público | revoga o token do cookie e apaga o cookie (204, idempotente; sem cookie também 204). Sem o cabeçalho: 403. |
| `GET /api/auth/me` | autenticado | perfil, empresa, papel, `plataformaAdmin`. |
| `POST /api/auth/senha` `{senhaAtual?, novaSenha}` | autenticado | troca de senha; exige `senhaAtual` correta (403 `senha_atual_invalida`), exceto se o usuário ainda não tem senha. Revoga todos os refresh tokens do usuário. |

Resposta do login/refresh: `{accessToken, expiresIn, usuario{id,nome,email}, empresa{id,razaoSocial,nomeFantasia}|null, papel|null, plataformaAdmin}`.
Outros códigos: 400 `requisicao_invalida` (com `detalhes.campos[{campo,mensagem}]`) ou `senha_invalida`, 401 `credenciais_invalidas` / `sessao_invalida` / `nao_autenticado`, 403 `acesso_negado`, 404 `nao_encontrado`, 429 `muitas_tentativas` (com `Retry-After`), 500 `erro_interno` (sem pilha nem SQL).

- **Senha:** bcrypt (`BcryptUtil`), de **8 a 72 bytes** em UTF-8 (o bcrypt trunca em 72, então se recusa acima). Nunca vai a log.
- **Força bruta:** 5 falhas em 15 minutos por e-mail bloqueiam o e-mail por 15 minutos (429). O contador fica na **memória** da
  instância: com várias instâncias (ou após reinício) o limite efetivo é maior; em produção precisa de armazenamento
  compartilhado (Redis ou tabela). Quem sabe o e-mail alheio pode bloquear aquela conta de propósito.

### JWT

- RS256, issuer `emitesimples`, acesso de **15 minutos** (`emitesimples.auth.access-token-minutos`).
- Claims: `sub` = id do usuário, `upn` = e-mail, `groups` = [papel da empresa na sessão] + `plataforma_admin` se for o caso,
  `empresa_id` (só com empresa na sessão), `plataforma_admin` (boolean `true`, só para o administrador da plataforma).
  O `EscopoTenant` lê `empresa_id` e `plataforma_admin`: não renomeie.
- **Refresh token:** opaco, 32 bytes aleatórios em Base64URL; no banco só o SHA-256 (hex). Vale 7 dias. Cada token pertence a
  (usuário, empresa). Troca de senha e logout revogam; o access token já emitido continua válido até expirar (até 15 min).
- **Administrador da plataforma sem empresa não recebe refresh token** (sem `Set-Cookie`): `TokenRenovacao.empresa` é
  `not null` na V1 e não se inventa empresa. Ele renova entrando de novo (login). PENDENTE de decisão do usuário.

### Refresh token em cookie httpOnly

O refresh token **nunca** vai no corpo (resposta nem requisição): só em cookie, inacessível ao JavaScript (some o roubo por XSS).

- **Cookie:** `refresh_token=<token opaco>; HttpOnly; SameSite=Strict; Path=/api/auth; Max-Age=604800` (7 dias, `emitesimples.auth.refresh-token-dias`) e
  `Secure` (padrão ligado; `emitesimples.auth.cookie-secure` / `EMITESIMPLES_COOKIE_SECURE`; desligado nos perfis dev e test, que usam http).
  Login e refresh definem o cookie (o refresh **rotaciona**: novo valor a cada uso). Logout, troca de senha e todo 401 `sessao_invalida` do refresh
  o apagam (`Max-Age=0`, mesmos Path/SameSite/HttpOnly/Secure para o navegador aceitar a remoção). Admin da plataforma sem empresa: sem cookie.
- **Path restrito:** o navegador só envia o cookie para `/api/auth/*` (login, refresh, logout, me, senha); as demais chamadas usam só o Bearer.
- **CSRF (três camadas):** (1) `SameSite=Strict` (o navegador não envia o cookie em requisição vinda de outro site); (2) refresh e logout só
  aceitam `POST` e o cookie tem Path restrito; (3) cabeçalho obrigatório `X-Requested-With: emitesimples` em refresh e logout (403
  `cabecalho_obrigatorio` sem ele, antes de qualquer efeito): um formulário ou `<img>` cross-site não consegue enviá-lo, e um `fetch` cross-site
  com cabeçalho custom exige preflight CORS, que só passa para as origens configuradas. O front envia o cabeçalho em todas as chamadas.
- **CORS:** `X-Requested-With` entra em `Access-Control-Allow-Headers` e `Access-Control-Allow-Credentials: true` só vale para as origens
  explícitas de `EMITESIMPLES_CORS_ORIGINS` (nunca `*`). Com o proxy do Vite (mesma origem) nada disso é exercitado; o front cross-origin
  precisaria de `credentials: 'include'`.
- **Riscos que restam:** XSS ainda pode usar a sessão enquanto a aba estiver aberta (chamar o refresh com o cookie do próprio navegador);
  só deixa de poder **ler/exfiltrar** o token. Em produção o front e a API devem ficar no mesmo site (mesmo domínio registrável) e em HTTPS,
  senão `SameSite=Strict` e `Secure` quebram o fluxo ou perdem o sentido. Atrás de proxy TLS o `Secure` é só atributo do cookie (o Quarkus não
  precisa saber que é https), mas o proxy deve repassar o `Set-Cookie` sem alterar. Cookie sem o atributo `Secure` nunca deve existir em produção.

### Chaves de desenvolvimento

```sh
./scripts/gerar-chaves-dev.sh     # gera keys/private.pem (PKCS#8) e keys/public.pem; NUNCA commitar nem usar em produção
```

`keys/` e `*.pem` estão no `.gitignore`. Os perfis `dev` e `prod` leem `keys/private.pem` e `keys/public.pem`
(sobrescreva com `EMITESIMPLES_JWT_PRIVATE_KEY` e `EMITESIMPLES_JWT_PUBLIC_KEY`). Os **testes** geram o par em memória.

### Administrador inicial

Ao subir, se não existir nenhum usuário com `administradorPlataforma` e as variáveis `EMITESIMPLES_ADMIN_EMAIL` e
`EMITESIMPLES_ADMIN_SENHA` estiverem definidas, cria esse usuário (ativo, bcrypt, sem vínculo). Idempotente. Sem as variáveis e
sem administrador, registra um WARN (sem a senha). Depois de criado, troque a senha por `POST /api/auth/senha` e retire a variável.

### Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `EMITESIMPLES_CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000` | origens do front permitidas (CORS) |
| `EMITESIMPLES_COOKIE_SECURE` | `true` (e `false` em dev e test) | atributo `Secure` do cookie do refresh; só desligue em http puro de desenvolvimento |
| `EMITESIMPLES_ADMIN_EMAIL`, `EMITESIMPLES_ADMIN_SENHA` | vazias | administrador inicial |
| `EMITESIMPLES_CEP_URL_BASE` | `https://viacep.com.br/ws` | URL base do ViaCEP (os testes usam um servidor simulado local) |
| `EMITESIMPLES_JWT_PRIVATE_KEY`, `EMITESIMPLES_JWT_PUBLIC_KEY` | `keys/private.pem`, `keys/public.pem` | caminhos das chaves (dev e prod) |
| `QUARKUS_DATASOURCE_JDBC_URL`, `..._USERNAME`, `..._PASSWORD` | sem padrão | banco (fora de dev/test); usuário = role comum `emitesimples_app` (RLS) |
| `QUARKUS_FLYWAY_USERNAME`, `QUARKUS_FLYWAY_PASSWORD` | sem padrão | dono das tabelas, só para migrar (RLS) |
| `EMITESIMPLES_DB_APP_PASSWORD` | sem padrão | só para `scripts/db/criar-role-app.sql` (cria o role de aplicação) |

### Matriz de acesso (provada em `PoliticaAcessoTest`)

Papéis vêm da claim `groups`. "plat." = administrador da plataforma. Fora de `/api` e de `/q/health`, `/q/openapi`, `/q/swagger-ui`
tudo é negado (`padrao-negar`), e um endpoint novo sob `/api` nasce **fechado para escrita**.

| Recurso | GET | POST, PUT, PATCH | DELETE |
|---|---|---|---|
| `/api/auth/login`, `refresh`, `logout` | público (só POST; refresh e logout exigem `X-Requested-With: emitesimples`) | | |
| `/q/health*`, `/q/openapi` | público | | |
| `Nota`, `Cancelamento`, `ChamadaGateway`, `EventoWebhook`, `FaturaCrm`, `HistoricoSituacaoEmpresa`, `ImportacaoMunicipio` | autenticado (filtrado por empresa) | **negado a todos**, inclusive plat. | negado |
| `Usuario` (global, sem filtro de empresa) | só plat. | só plat. | negado |
| `Municipio`, `Plano`, `AssinaturaEmpresa`, `Empresa`, `AtividadeEmpresa`, `QsaEmpresa` | autenticado | só plat. | negado |
| `Pessoa`, `Servico` | autenticado | administrador, operador, plat.; `consulta` não | negado |
| `Contato` | autenticado | administrador, operador, plat. | administrador, operador, plat. |
| `VinculoUsuario`, `ClienteAcesso` | autenticado | negado (vínculos: só por `/api/admin/usuarios/{id}/vinculos`; `ClienteAcesso` ainda sem endpoint à mão) | negado |
| `/api/cep/*` | autenticado (qualquer papel) | negado | negado |
| `/api/admin/*` (importação de municípios, usuários e vínculos) | só plat. | só plat. | negado (usuário e vínculo nunca se apagam) |
| outro caminho sob `/api` | autenticado | negado | negado |

**Como o Quarkus combina as regras** (provado, ver comentário em `application.properties`): vence o caminho mais específico;
no mesmo caminho vale a regra que declara o método. Se o caminho específico casa mas nenhuma regra dele declara o método,
**não** volta para `/api/*`: cai no `padrao-negar`. Por isso cada entidade tem regra explícita para todos os métodos.

### CEP (`GET /api/cep/{cep}`)

Qualquer usuário autenticado. O **back** chama o ViaCEP (`{emitesimples.cep.url-base}/{cep}/json/`, padrão `https://viacep.com.br/ws`,
`EMITESIMPLES_CEP_URL_BASE`) com `java.net.http` (timeout total `emitesimples.cep.timeout-ms`=3000, sem redirecionamento, resposta
limitada a 16 KB). Aceita 8 dígitos com ou sem hífen (`01001000`, `01001-000`).

- 200 `{cep (8 dígitos), logradouro, complemento, bairro, uf, codigoIbge, municipio: {id, nome, uf, codigoIbge} | null}`; `municipio` é a
  linha de `Municipio` com esse IBGE (sempre consultada na hora; `null` se a tabela ainda não a tem). Campos ausentes no ViaCEP viram `""`.
- 400 `cep_invalido` (formato); 404 `cep_nao_encontrado` (ViaCEP devolve `{"erro": true}` ou `"true"`); 502 `servico_cep_indisponivel`
  (timeout, falha de rede, HTTP diferente de 200, corpo que não é JSON; mensagem genérica, sem URL nem pilha); 429 `muitas_tentativas` com `Retry-After`.
- **Cache** em memória (LRU de 5.000 CEPs): achado 24 h, não achado 1 h; falha nunca entra no cache. **Limite por usuário:** 30 consultas por
  minuto (janela fixa, `emitesimples.cep.limite-por-minuto`), contadas também quando a resposta vem do cache. Ambos na memória da instância
  (com várias instâncias o limite efetivo é N x; mesma limitação do `ForcaBruta`).
- O CEP nunca vai a log completo (só os 5 primeiros dígitos, em DEBUG). Testes: `ViaCepStubResource` sobe um ViaCEP simulado local; nenhum teste chama a internet.

### Administrador da plataforma: `/api/admin/*` (só `plataforma_admin`; erros `{codigo, mensagem, detalhes?}`)

**Importação de municípios** (`docs/carga-municipios.md`):

| Endpoint | O que faz |
|---|---|
| `POST /api/admin/municipios/importacao` (`multipart/form-data`, campo `arquivo`) | importa o CSV. Detecta UTF-8 (com ou sem BOM) ou Windows-1252, separador `;` `,` ou tab, colunas **pelo nome** (`CÓDIGO DO MUNICÍPIO - IBGE`, `MUNICÍPIO - IBGE`, `UF`; sem acento/caixa; as TOM e colunas extras são ignoradas). Upsert por `codigoIbge`, nunca apaga. Limite de `emitesimples.municipios.max-bytes` (2 MB; 413 `arquivo_grande_demais`). |
| `GET /api/admin/municipios/importacoes?limite=20` | últimas importações (1 a 100), da mais recente para a mais antiga, com `usuario` e `resumoRejeicoes`. |

Resposta do POST (200): `{id, nomeArquivo, estado, codificacao, separador (";" "," "tab"), dataHoraInicio, dataHoraFim, linhasLidas, criados, atualizados, inalterados, totalRejeitados, rejeitados:[{linha,motivo}], rejeitadosOmitidos?}`
(`criados` = "inseridas" do documento, `inalterados` = "iguais"). `estado` = `concluida` | `concluida_com_rejeicoes` | `falhou` (o ciclo da entidade). Erros: 400 `arquivo_obrigatorio`,
400 `arquivo_invalido` (vazio, cabeçalho sem as colunas obrigatórias; grava histórico `falhou`), 413, 415 `tipo_nao_suportado` (corpo que não é multipart).

- **Aceita** a linha se: IBGE com 7 dígitos (ou `Exterior`: UF `EX` e código `0`; o nome vira `Exterior`), UF entre as 27 e `EX`, nome não vazio (até 100). **Rejeita** (relatado com a linha física do arquivo, cabeçalho = 1) código fora do padrão, UF inválida, nome vazio, código repetido no arquivo (vale a primeira), `EX` com código diferente de 0 e, **além do documento**, "já existe outro município com o mesmo nome e UF" (o índice único `UX_MUNICIPIO_NOME_UF` do banco; sem isso o envio inteiro cairia em 500). UF em caixa baixa é normalizada para maiúscula.
- **Transação:** as linhas válidas e o registro de `ImportacaoMunicipio` entram na mesma transação (lock consultivo: um envio por vez); erro inesperado desfaz tudo (500 `erro_interno`) e grava um histórico `falhou` em transação própria. Linhas rejeitadas não desfazem as válidas.
- Campos entre aspas são aceitos; quebra de linha dentro de campo não.
- `GET /api/ImportacaoMunicipio` (gerado) continua legível por qualquer usuário autenticado. A correção manual de município (`POST`/`PUT`/`PATCH /api/Municipio`) é só do administrador da plataforma; `DELETE` é negado a todos.

**Usuários e vínculos** (`/api/admin/usuarios`). Nunca devolve `senhaHash` nem hash de token (`temSenha` diz só se há senha). **Não existe DELETE**: usuário e vínculo só se desativam.
`UsuarioAdmin` = `{id, nome, email, whatsapp, ativo, administradorPlataforma, temSenha, dataUltimoAcesso, dataCriacao, dataAtualizacao, vinculos?}`.
`VinculoAdmin` = `{id, usuarioId, empresa:{id, razaoSocial, nomeFantasia, situacao}, papel, ativo, dataCriacao, dataAtualizacao}`.

| Endpoint | Corpo | Resposta |
|---|---|---|
| `GET /api/admin/usuarios?busca=&ativo=` | | até 100 `UsuarioAdmin` por nome |
| `GET /api/admin/usuarios/{from}/{to}?busca=&ativo=` e `GET /api/admin/usuarios/count?busca=&ativo=` | | faixa (máx. 100; 400 se `from` > `to`) e `{count}`. `busca` casa nome (sem acento/caixa) ou e-mail |
| `GET /api/admin/usuarios/{id}` | | `UsuarioAdmin`; 404 `usuario_nao_encontrado` |
| `POST /api/admin/usuarios` | `{nome, email, whatsapp?, senha, ativo? (padrão true), vinculos?:[{empresaId, papel, ativo?}]}` | 201 + `Location` + `UsuarioAdmin` com `vinculos`. Tudo ou nada. Nunca cria administrador da plataforma. |
| `PATCH /api/admin/usuarios/{id}` | `{nome?, whatsapp?, ativo?}` (ausente = não muda; `whatsapp:""` apaga) | `UsuarioAdmin`. 400 `auto_desativacao` se o admin desativa a si mesmo |
| `POST /api/admin/usuarios/{id}/senha` | `{senha}` (temporária) | 204; troca o hash, **revoga todos os refresh tokens** e zera o bloqueio de força bruta do e-mail |
| `GET /api/admin/usuarios/{id}/vinculos` | | lista de `VinculoAdmin` (ativos e inativos) |
| `POST /api/admin/usuarios/{id}/vinculos` | `{empresaId, papel, ativo?}` | 201 + `VinculoAdmin`; 409 `vinculo_ja_existe` (um por par usuário/empresa: reative ou altere o existente) |
| `PATCH /api/admin/usuarios/{id}/vinculos/{vinculoId}` | `{papel?, ativo?}` | `VinculoAdmin`; `ativo:false` revoga os refresh tokens daquele usuário naquela empresa; 404 `vinculo_nao_encontrado` |

Códigos: 400 `requisicao_invalida` (`detalhes.campos`), 400 `senha_invalida` (política de 8 a 72 bytes; `detalhes.campos[0].campo = "senha"`), 400 `papel_invalido`
(`administrador`, `operador` ou `consulta`, exatos), 400 `vinculos_duplicados`, 404 `empresa_nao_encontrada`, 409 `email_ja_cadastrado` (único sem diferença de caixa; o e-mail é gravado em minúsculas e sem espaços nas pontas).
O usuário criado entra com a senha temporária; **o fluxo "o usuário define a própria senha" continua em aberto** (não há troca forçada no primeiro acesso). Esses serviços rodam com o escopo do **próprio token** do administrador
(`plataforma_admin` vira irrestrito na RLS), e não em `comoSistema`: se alguém sem a claim chegasse ali, o banco ainda barraria `VINCULOUSUARIO` e `TOKENRENOVACAO`.

### OpenAPI

`GET /q/openapi` (YAML; `Accept: application/json` para JSON) lista os endpoints reais, inclusive `/api/auth/*`, com o esquema
Bearer JWT (`bearerAuth`). A Swagger UI (`/q/swagger-ui`) só existe em dev e test.

## Row Level Security (RLS): segunda camada de isolamento entre empresas

A primeira camada é o filtro do repositório gerado (`EscopoTenant`). A segunda é o **PostgreSQL**: a migração
`V3__row_level_security.sql` liga RLS (`ENABLE` + `FORCE`) nas 16 tabelas com `EMPRESA_ID` (`ASSINATURAEMPRESA`,
`ATIVIDADEEMPRESA`, `CANCELAMENTO`, `CHAMADAGATEWAY`, `CLIENTEACESSO`, `CONFIRMACAOCADASTRO`, `CONTATO`, `EVENTOWEBHOOK`,
`FATURACRM`, `HISTORICOSITUACAOEMPRESA`, `NOTA`, `PESSOA`, `QSAEMPRESA`, `SERVICO`, `TOKENRENOVACAO`, `VINCULOUSUARIO`) e em
`EMPRESA` (aí a coluna é o próprio `ID`). A política `isolamento_empresa` (`USING` e `WITH CHECK`) libera a linha se
`empresa_id = current_setting('app.empresa_id', true)` **ou** `current_setting('app.irrestrito', true) = 'true'`.
Sem configuração nenhuma linha passa (falha fechada); a comparação é por texto, então lixo na configuração nunca dá erro de cast.

**Motivo:** o POST gerado de `Pessoa`/`Servico`/`Contato` grava a empresa que vier no corpo. A RLS barra no banco, mesmo com a
camada Java ainda vulnerável: o operador da empresa A que manda `empresa` da B recebe **403 `isolamento_empresa`** e nada é gravado.

### O que cobre e o que não cobre

| Cobre | Não cobre |
|---|---|
| leitura, INSERT, UPDATE (inclusive mover linha de empresa) e DELETE nas 17 tabelas acima, por qualquer caminho que use o pool (Hibernate, Panache, JDBC) | `USUARIO`, `MUNICIPIO`, `PLANO`, `IMPORTACAOMUNICIPIO` (globais, sem RLS de propósito): quem alcança `USUARIO` vê hashes de senha de todos; a proteção é a matriz de acesso do Java |
| operador comum tentando gravar em outra empresa; token sem empresa; requisição sem escopo | **permissão dentro da mesma empresa** (papel `consulta` gravando etc.): continua só no Java |
| vazamento entre requisições na mesma conexão do pool | superusuário e roles com `BYPASSRLS` (ignoram RLS; `FORCE` cobre só o dono): por isso o runtime usa role comum |
| | tabela nova com `EMPRESA_ID` criada em migração futura: **precisa** entrar numa migração com `ENABLE`/`FORCE`/política (fica sem RLS até lá) |
| | código que roda em `ContextoTenantBanco.comoSistema` (e quem administra o banco) |

### Papéis do banco (dois usuários)

| Papel | Uso | Configuração |
|---|---|---|
| **dono** (dono das tabelas) | só o Flyway (DDL e `V3`) | `QUARKUS_FLYWAY_USERNAME`, `QUARKUS_FLYWAY_PASSWORD` |
| **`emitesimples_app`** (runtime) | a aplicação: só DML, `NOSUPERUSER`, `NOBYPASSRLS`, não é dono | `QUARKUS_DATASOURCE_USERNAME`, `QUARKUS_DATASOURCE_PASSWORD` |

Crie o role de aplicação uma vez, como administrador do banco (a senha vem do ambiente, nunca do arquivo nem de log):

```sh
export EMITESIMPLES_DB_APP_PASSWORD='...'
psql -v ON_ERROR_STOP=1 -d emitesimples -f scripts/db/criar-role-app.sql     # psql 15 ou superior
```

A `V3` concede `SELECT, INSERT, UPDATE, DELETE` nas tabelas (nunca na `flyway_schema_history`, nunca DDL) e deixa
`ALTER DEFAULT PRIVILEGES` para tabelas futuras do mesmo dono, **se o role já existir** quando ela roda. Se o role for criado depois,
o próprio script acima refaz os `GRANT`s (as default privileges ficam por conta de uma nova migração). Sem `QUARKUS_FLYWAY_USERNAME`
o Flyway usa o usuário do datasource (o role comum) e a migração falha alto: é intencional. **Nunca** aponte o runtime para o
superusuário: a RLS some sem erro nenhum.

### Como o escopo chega ao banco

`tenant/RlsPoolInterceptor` (um `AgroalPoolInterceptor`) roda em **toda** conexão entregue pelo pool, antes de qualquer SQL da
aplicação, e executa `set_config('app.empresa_id', <claim empresa_id ou ''>, ...)` e `set_config('app.irrestrito', 'true'|'false', ...)`.
Ao devolver a conexão ao pool zera os dois. Cobre as transações e as leituras sem `@Transactional` (os `GET` gerados não abrem
transação). Detalhe: dentro de transação (autocommit desligado) usa `is_local=true` (morre no commit/rollback); fora de transação
(autocommit) `is_local=true` seria descartado no mesmo instante, então vale na sessão, é sobrescrito a cada aquisição e zerado na
devolução (desvio consciente do pedido de usar sempre `is_local=true`).

`tenant/ContextoTenantBanco` decide o escopo, nesta ordem: (1) bloco explícito; (2) requisição HTTP em curso: claims do JWT
(`empresa_id`, `plataforma_admin`), sem token nada é liberado; (3) **sem requisição HTTP** (startup, `AdminBootstrap`, jobs, testes
que montam dados): sistema (irrestrito). Ponto de extensão para webhook e IntegraNotas futuros:

```java
@Inject ContextoTenantBanco contexto;
contexto.comoEmpresa(empresaId, () -> servico.processar(evento));   // restrito a UMA empresa (ex.: webhook que resolveu a empresa)
contexto.comoSistema(() -> job.rodar());                            // irrestrito: só código confiável (versões rodarComo* para Runnable)
```

O bloco precisa ser aberto **antes** da transação (a conexão e o escopo são fixados na aquisição); abri-lo dentro de transação
ativa lança `IllegalStateException`. O erro `42501` do banco vira 403 com `codigo: isolamento_empresa` (`ErrosHandler`).

### Como o admin e o auth contornam (decisão e risco)

- **Administrador da plataforma:** a claim `plataforma_admin=true` do JWT vira `app.irrestrito='true'`: enxerga e grava tudo
  (cria `Empresa`, `Municipio`, grava em qualquer empresa). O risco é o de sempre: quem roubar esse token.
- **Login, refresh, logout e troca de senha** (`auth/AuthService`) leem `VinculoUsuario`, `TokenRenovacao` e `Empresa` antes de
  haver empresa na sessão. Decisão (a mais simples e segura): esses métodos rodam em `contexto.comoSistema(...)`; o irrestrito vale
  só dentro dessas transações, só em código à mão revisado em `auth/`. Alternativa descartada: política própria por
  `app.usuario_id` (mais precisa, mas exige política extra por tabela, e o refresh parte de um token, não de um usuário).
  **Risco:** um bug de SQL nesses métodos (ex.: injeção) enxerga todas as empresas; as consultas são parametrizadas e a entrada
  livre é só e-mail, hash e ids. `me` roda com o escopo do próprio token.
- Webhook e IntegraNotas: ainda não existem; devem usar `comoEmpresa` (nunca `comoSistema`) assim que souberem a empresa.
- Código novo que leia `VinculoUsuario`/`TokenRenovacao` sem token recebe **zero linhas** (falha fechada) até usar `comoSistema`.

### Prova (`RlsTest`)

Role de runtime real (não superusuário, não dono, sem BYPASSRLS) no banco de teste. POST de `Pessoa`, `Servico` e `Contato`
do operador A com empresa da B falha (403) e nada é criado (conferido por SQL como superusuário); PUT que move registro; SQL direto
como o role com `app.empresa_id=A` só vê A, sem configuração vê zero, irrestrito vê tudo, INSERT/UPDATE cruzado dá `42501`; A, B,
login e admin intercalados e 80 requisições concorrentes sem vazamento; mesma conexão física (`pid`) reutilizada sem herdar o
escopo; admin cria `Municipio` e `Empresa` e vê tudo. **Mutação manual:** apontando o runtime para o superusuário
(`-Dquarkus.datasource.username=dono_teste -Dquarkus.datasource.password=dono-teste-efemero`) o POST cruzado passa e o `RlsTest`
falha: a prova depende de verdade do role comum. Nos testes o Dev Services cria o container com um dono (superusuário do container
efêmero), `src/test/resources/db/init-role-app-teste.sql` cria `emitesimples_app`; as senhas de teste só existem no container descartável.

### Pendente

- Signup e confirmação de e-mail (serão públicos: acrescentar em `application.properties`, seção "PUBLICO").
- Endpoint à mão para `ClienteAcesso` (segredos): hoje a escrita está negada. (`VinculoUsuario` já tem: `/api/admin/usuarios/{id}/vinculos`.)
- Refresh token para administrador da plataforma sem empresa (depende de relaxar `TokenRenovacao.empresa` no mapa).
- Força bruta compartilhada entre instâncias e limite por IP.
- **Achados no código gerado (com o `generator-author`)**: o POST de `Pessoa`/`Servico`/`Contato` grava a empresa que vier no
  body (cria registro em OUTRA empresa; **hoje barrado só pela RLS**, 403 `isolamento_empresa`); o PUT gerado copia a `empresa`
  aninhada do corpo para a Empresa real (um PUT de `Pessoa` com `empresa` parcial tenta zerar o `documento` da própria Empresa: 500); violação de unicidade vira 500 em vez de 409; listagens fixam 100 registros;
  parâmetros de consulta de relações com nome repetido (`nome`, `documento`...).

## Pontos em aberto

- **Subir a aplicação** exige banco PostgreSQL (com a extensão `unaccent`) e as chaves JWT; ver `application.properties`. Para
  compilar não é preciso nada disso. Em dev use o banco persistente da seção abaixo.

## Banco de desenvolvimento persistente

`dev/compose.yaml` sobe um Postgres 18 só do projeto (contêiner `emitesimples-dev-postgres`, porta **5433**, volume
`emitesimples-dev-pgdata`), com o dono `emitesimples` (Flyway) e o role de aplicação `emitesimples_app` (sem BYPASSRLS):
no perfil dev a RLS vale, como em produção. Os dados sobrevivem a reinícios do back.

```bash
cd back/api
docker compose -f dev/compose.yaml up -d --wait   # sobe o banco
mvn quarkus:dev                                   # sobe o back (cria o admin de dev, abaixo)
docker compose -f dev/compose.yaml down           # desliga e MANTÉM os dados
docker compose -f dev/compose.yaml down -v        # desliga e APAGA os dados
```

Administrador de dev (só no perfil dev; sobrescrevível por `EMITESIMPLES_ADMIN_EMAIL`/`EMITESIMPLES_ADMIN_SENHA`):
`plataforma@exemplo.com.br` / `senha1234`. As senhas do banco em `dev/` e no `application.properties` são só de dev.
Os testes (`mvn test`) continuam usando o Dev Services efêmero, sem tocar nesse banco.
