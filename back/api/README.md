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
| `br.com.emitesimples.gateway` | cliente HTTP do IntegraNotas (`IntegraNotasCliente`) e webhook público (`WebhookIntegraNotasResource`, `WebhookToken`, `LimiteWebhook`) (à mão) |
| `br.com.emitesimples.nfseconfig` | `GET/PUT /api/configuracao/nfse`: habilitar a NFS-e e cadastrar o emitente (à mão) |
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
| `GET /api/auth/me` | autenticado | perfil, empresa, papel, `plataformaAdmin`, `podeCancelarNfse` (efetivo: plataforma e administrador true; operador conforme a delegação do vínculo). |
| `POST /api/auth/senha` `{senhaAtual?, novaSenha}` | autenticado | troca de senha; exige `senhaAtual` correta (403 `senha_atual_invalida`), exceto se o usuário ainda não tem senha. Revoga todos os refresh tokens do usuário. |

Resposta do login/refresh: `{accessToken, expiresIn, usuario{id,nome,email}, empresa{id,razaoSocial,nomeFantasia}|null, papel|null, plataformaAdmin, podeCancelarNfse}`.
Outros códigos: 400 `requisicao_invalida` (com `detalhes.campos[{campo,mensagem}]`) ou `senha_invalida`, 401 `credenciais_invalidas` / `sessao_invalida` / `nao_autenticado`, 403 `acesso_negado`, 404 `nao_encontrado`, 429 `muitas_tentativas` (com `Retry-After`), 500 `erro_interno` (sem pilha nem SQL).

- **Senha:** bcrypt (`BcryptUtil`), de **8 a 72 bytes** em UTF-8 (o bcrypt trunca em 72, então se recusa acima). Nunca vai a log.
- **Força bruta:** 5 falhas em 15 minutos por e-mail bloqueiam o e-mail por 15 minutos (429). O contador fica na **memória** da
  instância: com várias instâncias (ou após reinício) o limite efetivo é maior; em produção precisa de armazenamento
  compartilhado (Redis ou tabela). Quem sabe o e-mail alheio pode bloquear aquela conta de propósito.

### JWT

- RS256, issuer `emitesimples`, acesso de **15 minutos** (`emitesimples.auth.access-token-minutos`).
- Claims: `sub` = id do usuário, `upn` = e-mail, `groups` = [papel da empresa na sessão] + `PLATAFORMA_ADMIN` se for o caso,
  `empresa_id` (só com empresa na sessão), `plataforma_admin` (boolean `true`, só para o administrador da plataforma).
  O `EscopoTenant` lê `empresa_id` e `plataforma_admin`: não renomeie.
- **Valores de enum em MAIÚSCULAS (contrato, mapa seção 9.7 do guia de modelagem)**: todo enum de texto vai e volta como `SNAKE_CASE_MAIUSCULO` (ex.: `Nota.estado` = `AUTORIZADA`, `Empresa.situacao` = `PENDENTE_CONFIRMACAO`, `ambiente` = `HOMOLOGACAO`|`PRODUCAO`). Enums de Inteiro (regime tributário, motivo de cancelamento etc.) mantêm o código numérico. Os papéis também: `ADMINISTRADOR`|`OPERADOR`|`CONSULTA` na resposta da sessão (`papel`), na claim `groups` do JWT (`[papel]` + `PLATAFORMA_ADMIN` para o administrador da plataforma), nos `@RolesAllowed`, nos `roles-allowed` do `application.properties` e no `papel` dos vínculos (`400 papel_invalido` com valor em minúsculas). Constantes à mão: `security/Papeis`, `nfseemissao/EstadosNfse`, `nfseconfig/StatusCadastro`, `nfseconfig/CertificadoSituacao`. A claim booleana `plataforma_admin` (lida pelo `EscopoTenant`) continua minúscula: é nome de claim, não valor de enum. Códigos de erro da API (`nota_em_conflito` etc.) e escopos do assistente (`consultar`, `preparar`, `emitir`, `cancelar`) não são enums e não mudam. Migração `V10__enums_em_maiusculas.sql` converte os dados (inclui `CHAMADAGATEWAY.OPERACAO`: `enviar_nfse`/`consultar_nfse`/`cancelar_nfse` viraram `EMITIR`/`CONSULTAR`/`CANCELAR`, e `VERSAO_API` `V1`/`V2`) e recria o CHECK `ck_nota_numeracao`, o índice parcial `UX_ASSINATURAEMPRESA_VIGENTE` e o CHECK do ambiente de `configuracao_integranotas`. Sessões e refresh tokens emitidos antes da troca deixam de valer.
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
| `Pessoa`, `Servico` | autenticado | administrador, operador, plat.; `CONSULTA` não | negado |
| `Contato` (CRUD gerado) | autenticado | **negado a todos** (escrita só por `PUT /api/Pessoa/{id}/contatos`) | negado |
| `VinculoUsuario`, `ClienteAcesso` | autenticado | negado (vínculos: só por `/api/admin/usuarios/{id}/vinculos`; `ClienteAcesso` ainda sem endpoint à mão) | negado |
| `/api/cep/*` | autenticado (qualquer papel) | negado | negado |
| `/api/configuracao/nfse` (NFS-e da empresa) | administrador, operador, consulta (plat. sem empresa: 403) | só PUT, só administrador da empresa; POST/PATCH negados | negado |
| `/api/configuracao/empresa` (dados da empresa da sessão) | administrador, operador, consulta (plat. sem empresa: 403) | só PUT, só administrador da empresa; POST/PATCH negados | negado |
| `/api/configuracao/empresa/certificado` (certificado A1, multipart) | negado | só POST, só administrador da empresa (10/min por usuário); PUT/PATCH negados | negado |
| `POST /api/webhooks/integranotas/{empresaId}/{token}` | negado | **público** (anônimo; só POST; token HMAC no caminho) | negado; GET/PUT/PATCH e todo o resto de `/api/webhooks/*` negados |
| `/api/admin/*` (importação de municípios, usuários e vínculos por usuário e por empresa `/api/admin/empresas/{id}/vinculos`) | só plat. | só plat. | negado (usuário e vínculo nunca se apagam) |
| `/api/admin/configuracoes/integranotas` (config do IntegraNotas) | só plat. (GET lê; PUT grava) | só plat.: só PUT; POST/PATCH negados | negado |
| `/api/admin/configuracoes/consulta-cnpj` (config) e `.../testar` | só plat. (GET lê; PUT grava) | só plat.: PUT na config; POST só em `/testar` | negado |
| `/api/admin/consulta-cnpj/{cnpj}` (consulta para preencher o cadastro) | só plat. | negado | negado |
| `/api/consulta-cnpj/{cnpj}` (mesma consulta, para a empresa) | administrador, operador e plat. (`CONSULTA` não) | negado | negado |
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

### Administrador da plataforma: `/api/admin/*` (só `PLATAFORMA_ADMIN`; erros `{codigo, mensagem, detalhes?}`)

**Importação de municípios** (`docs/carga-municipios.md`):

| Endpoint | O que faz |
|---|---|
| `POST /api/admin/municipios/importacao` (`multipart/form-data`, campo `arquivo`) | importa o CSV. Detecta UTF-8 (com ou sem BOM) ou Windows-1252, separador `;` `,` ou tab, colunas **pelo nome** (`CÓDIGO DO MUNICÍPIO - IBGE`, `MUNICÍPIO - IBGE`, `UF`; sem acento/caixa; as TOM e colunas extras são ignoradas). Upsert por `codigoIbge`, nunca apaga. Limite de `emitesimples.municipios.max-bytes` (2 MB; 413 `arquivo_grande_demais`). |
| `GET /api/admin/municipios/importacoes?limite=20` | últimas importações (1 a 100), da mais recente para a mais antiga, com `usuario` e `resumoRejeicoes`. |

Resposta do POST (200): `{id, nomeArquivo, estado, codificacao, separador (";" "," "tab"), dataHoraInicio, dataHoraFim, linhasLidas, criados, atualizados, inalterados, totalRejeitados, rejeitados:[{linha,motivo}], rejeitadosOmitidos?}`
(`criados` = "inseridas" do documento, `inalterados` = "iguais"). `estado` = `CONCLUIDA` | `CONCLUIDA_COM_REJEICOES` | `FALHOU` (o ciclo da entidade). Erros: 400 `arquivo_obrigatorio`,
400 `arquivo_invalido` (vazio, cabeçalho sem as colunas obrigatórias; grava histórico `FALHOU`), 413, 415 `tipo_nao_suportado` (corpo que não é multipart).

- **Aceita** a linha se: IBGE com 7 dígitos (ou `Exterior`: UF `EX` e código `0`; o nome vira `Exterior`), UF entre as 27 e `EX`, nome não vazio (até 100). **Rejeita** (relatado com a linha física do arquivo, cabeçalho = 1) código fora do padrão, UF inválida, nome vazio, código repetido no arquivo (vale a primeira), `EX` com código diferente de 0 e, **além do documento**, "já existe outro município com o mesmo nome e UF" (o índice único `UX_MUNICIPIO_NOME_UF` do banco; sem isso o envio inteiro cairia em 500). UF em caixa baixa é normalizada para maiúscula.
- **Transação:** as linhas válidas e o registro de `ImportacaoMunicipio` entram na mesma transação (lock consultivo: um envio por vez); erro inesperado desfaz tudo (500 `erro_interno`) e grava um histórico `FALHOU` em transação própria. Linhas rejeitadas não desfazem as válidas.
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
(`ADMINISTRADOR`, `OPERADOR` ou `CONSULTA`, exatos), 400 `vinculos_duplicados`, 404 `empresa_nao_encontrada`, 409 `email_ja_cadastrado` (único sem diferença de caixa; o e-mail é gravado em minúsculas e sem espaços nas pontas).
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
| operador comum tentando gravar em outra empresa; token sem empresa; requisição sem escopo | **permissão dentro da mesma empresa** (papel `CONSULTA` gravando etc.): continua só no Java |
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

## Configuração da consulta de CNPJ (CNPJ.ws) e cofre de segredos

URL base e token da consulta de CNPJ são editáveis pelo administrador da plataforma (migração `V4`, código à mão em
`plataforma/`: `ConfiguracaoConsultaCnpjResource`, `ConfiguracaoConsultaCnpjService`, `SegredoService`).

**API** (só `PLATAFORMA_ADMIN`; as regras `admin-configuracoes*` do `application.properties` liberam GET/HEAD/PUT e negam o resto):

- `GET /api/admin/configuracoes/consulta-cnpj` -> `{ urlBase, tokenConfigurado, dataAtualizacao, atualizadoPor: {id, nome} | null }`.
  **O token nunca volta**, só `tokenConfigurado`.
- `PUT /api/admin/configuracoes/consulta-cnpj` com `{ urlBase, token? }` -> 204. `token` ausente (ou `null`) mantém o atual; texto não
  vazio grava/substitui (cifrado); `""` remove o token e apaga o segredo.
- 400 `url_invalida`: precisa ser `https://` com host, sem usuário/senha, sem query/fragmento, sem barra final dupla, até 200
  caracteres (a barra final simples é removida). Proteção SSRF básica: bloqueia `localhost`, IPs literais privados/loopback/link-local/
  CGNAT/ULA, formas obscuras de IP (decimal, hexadecimal), nomes sem ponto e nomes `.local`/`.internal`. Com
  `emitesimples.cnpj.permitir-url-local=true` (padrão `false`; `true` em dev/test) liberam-se `http(s)://localhost` e `127.0.0.1`.
  Limite: o host é checado como texto/IP literal, **sem resolver DNS**; um nome público que resolva para IP interno (e DNS rebinding)
  só é barrado no momento do teste da consulta (veja abaixo: o IP resolvido é validado a cada chamada).
- 400 `token_invalido`: mais de 500 caracteres ou com espaço/quebra de linha.
- Auditoria mínima: `atualizado_por` e `data_atualizacao` na tabela `configuracao_consulta_cnpj` (singleton, id `unica`).

**Teste da consulta (`POST /api/admin/configuracoes/consulta-cnpj/testar`)** (só `PLATAFORMA_ADMIN`; é o ÚNICO POST liberado em
`/api/admin/configuracoes/*`, por regra de caminho exato `admin-configuracoes-testar`; classes `ConsultaCnpjCliente`,
`ConsultaCnpjTesteService`, `LimiteTesteCnpj`). Corpo `{ "cnpj": "27865757000102" }` (aceita pontuação; normaliza para 14 dígitos e
confere os dígitos verificadores). O back chama `GET {urlBase}/{cnpj14}?token={token}` com a URL e o token **salvos** e devolve só um
resumo (200): `cnpj, razaoSocial, nomeFantasia, situacaoCadastral, dataInicioAtividade, porte, naturezaJuridica,
atividadePrincipal {codigo, descricao}, capitalSocial, endereco {logradouro, numero, complemento, bairro, cep, municipio, uf}, email,
telefone, simples, mei, quantidadeSocios, duracaoMs, urlConsultada`. Campo ausente no CNPJ.ws vira `null` (mapeamento tolerante, ignora
campos desconhecidos). **O token nunca aparece** em resposta, log, exceção nem em `urlConsultada` (ex.: `https://comercial.cnpj.ws/cnpj/27865757000102`).
**Cada teste consome uma consulta do plano do CNPJ.ws.**

| HTTP | `codigo` | Quando |
|---|---|---|
| 400 | `cnpj_invalido` | formato ou dígito verificador inválido |
| 409 | `token_nao_configurado` | não há token salvo |
| 409 | `url_base_nao_permitida` | URL salva reprovada na revalidação ou host resolve para IP interno |
| 404 | `cnpj_nao_encontrado` | CNPJ.ws respondeu 404 |
| 502 | `token_recusado` | CNPJ.ws respondeu 401/403 |
| 429 | `limite_consulta_excedido` | CNPJ.ws respondeu 429; `detalhes.esperaSegundos` se vier `Retry-After` em segundos |
| 504 | `servico_cnpj_indisponivel` | timeout, host não resolvido ou erro de rede |
| 502 | `resposta_invalida` | status inesperado (inclui 5xx e 3xx), JSON inválido, corpo > 1 MB |
| 429 | `muitas_tentativas` | limite PRÓPRIO: 10 testes/min por usuário (`emitesimples.cnpj.limite-teste-por-minuto`; janela fixa, em memória) |

**Defesas da chamada externa**: `HttpClient` com `followRedirects(NEVER)` (um 302 vira `resposta_invalida`); timeout de conexão 5 s
(`emitesimples.cnpj.timeout-conexao`) e de resposta 10 s (`emitesimples.cnpj.timeout-resposta`, também aplicado à leitura do corpo;
1 s em teste); corpo lido até 1 MB (checa `Content-Length` e corta o stream); só `https` (exceto com `permitir-url-local=true`); a URL
salva é revalidada a cada teste; **antes de chamar, o host é resolvido (`InetAddress.getAllByName`) e a chamada é recusada se QUALQUER
IP for loopback/privado/link-local/CGNAT/ULA/multicast** (mesma regra do `PUT`, método compartilhado `ipProibido`; desligada com
`permitir-url-local`). Log só com host, CNPJ mascarado (`27.865.757/****-**`) e status; a URL com token é montada só no instante da
chamada e falhas de rede registram apenas a classe da exceção. **Janela residual de DNS rebinding**: a verificação do IP e a conexão
do `HttpClient` resolvem o nome separadamente, então um DNS malicioso pode responder IP público na checagem e interno na conexão.
Mitigado (URL editável só pelo administrador da plataforma, sem redirecionamento, corpo nunca devolvido, limite de taxa), não eliminado;
fechar exige conectar no IP já validado ou um proxy de saída com lista de destinos permitidos. CNPJ alfanumérico (novo formato) não é
aceito: o contrato é de 14 dígitos.

**Cofre (`SegredoService`)**: `guardar`, `substituir`, `ler` (uso interno futuro) e `remover`. Cifra AES-256-GCM, IV aleatório de 12
bytes por gravação, id do segredo como dado associado (AAD); guarda `valor_cifrado` + `iv` na tabela `segredo` (sem coluna de texto
puro). O valor nunca vai para log, exceção ou `toString`; o corpo do PUT é lido como texto para que um erro de parse não cite o token.

**Chave-mestra**: `emitesimples.segredos.chave` (variável `EMITESIMPLES_SEGREDOS_CHAVE`), 32 bytes em base64. Gere com

```bash
openssl rand -base64 32
```

Em dev e test há chaves fixas e PÚBLICAS no `application.properties` (nunca use em produção). Fora delas não há padrão: a aplicação sobe,
mas guardar/ler um segredo falha com 500 `segredos_chave_invalida` e mensagem clara (chave ausente, não-base64 ou de tamanho errado).
**Perder ou trocar a chave torna os segredos já gravados ilegíveis** (grave o token de novo pela API).

**O cofre é provisório**: hoje cifra em tabela do próprio banco; a referência `segredo_token_id` permite trocar por um cofre externo
(KMS/Vault) sem mudar a API. **Falta**: o uso da consulta de CNPJ no signup público (a consulta para preencher o cadastro da Empresa já existe, só plataforma),
a rotação da chave-mestra (recifrar tudo com chave nova; hoje sem versão de chave) e fechar a janela de DNS rebinding.

## Integração IntegraNotas (configuração do admin)

Migração `V6` (código à mão em `plataforma/`: `ConfiguracaoIntegraNotasResource`, `IntegraNotasConfigService`; reaproveita o cofre
`SegredoService` e as defesas de host de `ConfiguracaoConsultaCnpjService`). Tabela singleton `configuracao_integranotas` (id `unica`,
sem RLS, sem recurso gerado) e as colunas novas `empresa.nfse_habilitada/serie_nfse/ultimo_numero_nfse` e `servico.percentual_tributos_simples`.

**API** (só `PLATAFORMA_ADMIN`; regras `admin-configuracoes-integranotas*`: GET/HEAD/PUT; POST/PATCH/DELETE negados):

- `GET /api/admin/configuracoes/integranotas` -> `{ ambienteAtivo, urlHomologacao, urlProducao, urlPublicaWebhook | null,
  tokenHomologacaoConfigurado, tokenProducaoConfigurado, dataAtualizacao, atualizadoPor: {id, nome} | null, producaoPermitida }`.
  **Tokens nunca voltam**, só os booleans.
- `PUT` mesmo caminho, corpo `{ ambienteAtivo?, urlHomologacao?, urlProducao?, urlPublicaWebhook?, tokenHomologacao?, tokenProducao? }` -> 204.
  Campo ausente (ou `null`) mantém. Token: texto não vazio grava/substitui (cifrado no cofre), `""` remove e apaga o segredo (um por ambiente).
  `urlPublicaWebhook: ""` remove. Tudo é validado antes de tocar no cofre/banco: pedido inválido não altera nada.

**Regras**

- **Produção bloqueada por padrão**: `ambienteAtivo=PRODUCAO` só é aceito com `emitesimples.integranotas.permitir-producao=true`
  (env `EMITESIMPLES_INTEGRANOTAS_PERMITIR_PRODUCAO`; padrão `false`, e `false` em dev/test); senão 409 `producao_bloqueada`
  ("Produção está bloqueada neste servidor."). `producaoPermitida` na leitura reflete a propriedade. `ambienteAtivo` fora de
  `HOMOLOGACAO`/`PRODUCAO`: 400 `ambiente_invalido`. O banco também tem CHECK nos dois valores.
- **URLs das APIs** (`urlHomologacao`, `urlProducao`; 400 `url_invalida`): `https`, host `integranotas.com.br` ou `*.integranotas.com.br`,
  sem usuário/senha, sem query/fragmento, até 200 caracteres, barra final removida, mesma proteção SSRF (IP literal privado/loopback/obscuro,
  nome sem ponto, `.local`/`.internal`). `emitesimples.integranotas.permitir-url-local=true` (padrão `false`; `true` em dev/test) libera
  `http(s)://localhost`/`127.0.0.1` para apontar a um stub. Não há como remover essas URLs (valor obrigatório; padrões
  `https://hom-api.integranotas.com.br` e `https://api.integranotas.com.br`). Host checado como texto/IP literal, sem resolver DNS.
- **URL pública do webhook** (`urlPublicaWebhook`, opcional): base pública DESTE back (ex.: `https://api.exemplo.com.br`); `https`, host
  público (nunca localhost/IP privado, nem em dev/test), sem usuário/query, até 200, barra final removida.
- **Tokens da softhouse** (400 `token_invalido`): sem espaço/quebra de linha, até 2000 caracteres. Nunca em resposta, log ou exceção (corpo lido
  como texto e interpretado à mão).
- Auditoria mínima: `atualizado_por` e `data_atualizacao`. Perder/trocar a chave-mestra do cofre torna os tokens ilegíveis (regrave-os).

**`IntegraNotasConfigService`** (para as próximas fases; nada disto vira API): `ambienteAtivo()`, `urlBase(ambiente)`,
`tokenSofthouse(ambiente)` (`Optional<String>`, decifrado do cofre; nunca logar), `urlPublicaWebhook()` (`Optional<String>`).
`ambiente` inválido lança `IllegalArgumentException`.

**Próximas fases**: cliente HTTP do IntegraNotas (usa o serviço acima; testes sempre contra stub, nunca a rede), credenciais da empresa
na softhouse, emissão/consulta/cancelamento de NFS-e com numeração (`ultimo_numero_nfse`/`serie_nfse`) e recebimento de webhooks na
`urlPublicaWebhook`. Hoje nada chama o IntegraNotas.

## Consulta de CNPJ para preencher o cadastro: `GET /api/admin/consulta-cnpj/{cnpj}`

Só `PLATAFORMA_ADMIN` (regras `admin-consulta-cnpj*`: só GET/HEAD; POST/PUT/PATCH/DELETE negados). Classes em `plataforma/`:
`ConsultaCnpjCadastroResource`, `ConsultaCnpjCadastroService`, `ConsultaCnpjCadastro`, `LimiteConsultaCnpj`. Reusa
`ConsultaCnpjCliente.consultarBruto` (devolve o JSON já parseado; mesmas defesas do `/testar`: sem redirecionamento, corpo ≤ 1 MB,
timeouts, IP resolvido validado, URL/token nunca em log ou erro) e a URL/token salvos. CNPJ com 14 dígitos, com ou sem pontuação
(use só dígitos ou `.` e `-`; a barra `/`, mesmo codificada, não chega ao recurso e dá 400), dígito verificador validado. CNPJ só mascarado em log. Sem cache; **cada chamada gasta uma
consulta do plano do CNPJ.ws**. Limite: 30/min por usuário (`emitesimples.cnpj.limite-consulta-por-minuto`, janela fixa em memória,
separado do limite de 10/min do teste) -> 429 `muitas_tentativas` com `Retry-After`.

200: `{ cnpj, razaoSocial, nomeFantasia, email, telefone, cep, logradouro, numeroEndereco, complemento, bairro, municipio, uf,
codigoIbge, regimeTributario, inscricaoEstadual, situacaoCadastral, dataInicioAtividade, socios }`; toda chave sempre presente, ausente = `null`
(`socios` é lista, vazia se não houver).

| Campo | Origem no CNPJ.ws |
|---|---|
| `razaoSocial` | `razao_social` |
| `nomeFantasia`, `email`, `complemento`, `bairro`, `situacaoCadastral`, `dataInicioAtividade` | `estabelecimento.*` (`nome_fantasia`, `email`, `complemento`, `bairro`, `situacao_cadastral`, `data_inicio_atividade`) |
| `telefone` | `estabelecimento.ddd1 + telefone1`, só dígitos |
| `cep` | `estabelecimento.cep`, só dígitos (8) |
| `logradouro` | `tipo_logradouro + ' ' + logradouro` |
| `numeroEndereco` | `estabelecimento.numero` |
| `uf` | `estabelecimento.estado.sigla` |
| `codigoIbge` | `estabelecimento.cidade.ibge_id` |
| `municipio` | `{id, nome, uf, codigoIbge}` da tabela `Municipio` com esse `codigo_ibge`; `null` se não existir (uf e codigoIbge continuam) |
| `inscricaoEstadual` | `estabelecimento.inscricoes_estaduais`: a de `ativo=true` cujo `estado.sigla` é a UF do estabelecimento; senão `null` |
| `socios` | `socios[]` -> `[{ nome, tipo | null, qualificacao | null }]` (`nome`, `tipo`, `qualificacao_socio.descricao`); item sem `nome` é ignorado; sem a lista, `[]`. O front por ora ignora (uso futuro: contatos) |
| `regimeTributario` | sugestão pelo `simples` (códigos do enum `regimeTributario` de `Empresa.mi`): `mei == "Sim"` -> 4 (MEI); senão `simples == "Sim"` -> 1 (Simples Nacional); senão `null`. Regime normal (3) e excesso de sublimite (2) **não** são deduzidos |

Erros (mesmos do `/testar`): 400 `cnpj_invalido`, 409 `token_nao_configurado`, 409 `url_base_nao_permitida`, 404 `cnpj_nao_encontrado`,
502 `token_recusado`, 429 `limite_consulta_excedido` (CNPJ.ws; `detalhes.esperaSegundos`), 504 `servico_cnpj_indisponivel`,
502 `resposta_invalida`, 429 `muitas_tentativas` (limite próprio).

### Mesma consulta para usuários da empresa: `GET /api/consulta-cnpj/{cnpj}`

Mesmo corpo, erros e defesas da rota admin (mesmo `ConsultaCnpjCadastroService`; recurso `ConsultaCnpjEmpresaResource`). Quem pode: papel
`ADMINISTRADOR` ou `OPERADOR` na empresa da sessão e `PLATAFORMA_ADMIN`; `CONSULTA` -> 403, anônimo -> 401. Regras `consulta-cnpj*` em
`application.properties`: só GET/HEAD; POST/PUT/PATCH/DELETE negados. **Consome a cota do CNPJ.ws da PLATAFORMA** (token único salvo pelo administrador
da plataforma), não uma cota da empresa. O limite de 30/min por usuário é o mesmo `LimiteConsultaCnpj`: o contador é **compartilhado** entre
`/api/consulta-cnpj` e `/api/admin/consulta-cnpj` (a chave é o usuário; 30 no total por minuto, somando as duas rotas).

## Vínculos de usuário vistos pela empresa: `/api/admin/empresas/{empresaId}/vinculos`

Só `PLATAFORMA_ADMIN` (regras `admin-empresas*`: GET/POST/PATCH; PUT e DELETE negados; vínculo nunca se apaga, só se desativa). Caminho
distinto do `/api/Empresa` gerado, que não muda. Classes: `VinculoEmpresaAdminResource`, `VinculoEmpresaAdminService`; criar e alterar
**delegam** ao `UsuarioAdminService` (mesmas regras e mensagens; desativar revoga os refresh tokens do usuário naquela empresa).

- `GET` -> `[ { id, usuario: {id, nome, email, ativo}, papel, ativo, dataCriacao, podeCancelarNfse } ]`, ordenado por nome do usuário. 404 `empresa_nao_encontrada`.
- `POST` `{ usuarioId, papel, ativo?, podeCancelarNfse? }` (delegação de cancelar NFS-e ao operador; padrão false) (`ativo` padrão true) -> 201 com o vínculo e `Location`. 409 `vinculo_ja_existe`,
  404 `usuario_nao_encontrado`, 404 `empresa_nao_encontrada`, 400 `papel_invalido` (administrador, operador, consulta), 400 campos obrigatórios.
- `PATCH /{vinculoId}` `{ papel?, ativo?, podeCancelarNfse? }` -> 200 com o vínculo. 404 `vinculo_nao_encontrado` (inclusive se pertence a outra empresa),
  404 `empresa_nao_encontrada`, 400 `papel_invalido`.

## Migração V5: Pessoa cliente e/ou fornecedor

`V5__pessoa_cliente_fornecedor.sql` acrescenta `cliente` e `fornecedor` (`boolean not null default false`) à `pessoa`; toda pessoa existente
vira `cliente = true` (o cadastro nasceu só para clientes); a mesma pessoa pode ser as duas coisas. Índices `ix_pessoa_empresa_cliente` e
`ix_pessoa_empresa_fornecedor` (`empresa_id`, coluna) para a listagem por empresa e papel. RLS e GRANTs (V3, por tabela) já cobrem a coluna nova.
O código gerado (entity/builder/mapper/resource) só ganha os campos depois do `mi push` e do `gerar.sh`; até lá o DEFAULT cobre as inserções.

## Contatos de uma Pessoa (`GET` e `PUT /api/Pessoa/{id}/contatos`, provado em `ContatosDaPessoaTest`)

Os contatos de um cliente/fornecedor são gerenciados **só** por estes dois endpoints (código à mão em `plataforma/ContatosDaPessoa*`).
`/api/Contato` (CRUD gerado) é **somente leitura**: POST, PUT, PATCH e DELETE são negados a todos (403; 401 sem token), por regra em
`application.properties`. Motivo: o gerado ignora o filtro `pessoa.id`, o PUT sobrescrevia os dados da Pessoa e nada era validado.

**`GET /api/Pessoa/{id}/contatos`** (administrador, operador, consulta e plataforma) devolve `[ {id, tipoContato, nome, whatsapp|null, email|null} ]`
ordenado por nome (sem diferenciar maiúsculas). Pessoa inexistente ou de outra empresa: 404 `nao_encontrado`.

**`PUT /api/Pessoa/{id}/contatos`** (administrador, operador e plataforma; `CONSULTA` 403) recebe a lista DESEJADA completa
`[ {id?, tipoContato, nome, whatsapp?, email?} ]` e devolve 200 com a lista salva (com ids, ordenada por nome). Tudo numa única
transação (tudo ou nada; a Pessoa é travada, então PUTs concorrentes se serializam):
- item sem `id` é inserido; com `id` da própria pessoa, atualizado (só se algo mudou); contato atual ausente da lista é apagado; `[]` apaga todos;
- `id` que não pertence àquela pessoa (de outra pessoa, de outra empresa ou inexistente) ou repetido: 400 `requisicao_invalida` em `contatos[i].id`, nada muda;
- reenviar a lista devolvida (com ids) não muda nada; reenviar itens sem id (corpo sem ids) recria sem duplicar (os antigos saem).

Validação (400 `requisicao_invalida`, `detalhes.campos: [{campo, mensagem}]`, todos os erros de uma vez, campo como `contatos[2].nome`):
`tipoContato` em socio|financeiro|comercial|fiscal|outro; `nome` obrigatório (após trim) até 300; `whatsapp` opcional, só dígitos e `( ) + - .` e espaço,
normalizado para dígitos (até 20); `email` opcional, formato válido, até 60; no máximo 50 itens (erro em `contatos`); corpo ausente 400. Vazio vira `null`.
A `empresa` é sempre a da sessão; `empresa` e `pessoa` no corpo são ignoradas. Administrador da plataforma (sem empresa na sessão): vê/altera qualquer
Pessoa e os contatos herdam a empresa dela.

Resta de lacuna do gerado (só GET direto de `/api/Contato`, que o front não usa): o filtro `pessoa.id` é ignorado (coberto por `ContatoPorPessoaTest`).

## NFS-e da empresa (habilitar e cadastrar o emitente)

Código à mão em `nfseconfig/` (API da empresa) e `gateway/` (cliente do IntegraNotas e webhook). **Não use os nomes `configuracao`, `cadastros`,
`usuarios`, `nfse`, `integranotas`, `admin` para código à mão: o `gerar.sh` apaga esses pacotes.** Provado em `NfseEmpresaTest` e
`WebhookIntegraNotasTest`, sempre contra o stub local `IntegraNotasStubResource` (nenhuma chamada externa).

### Cliente do gateway (`gateway/IntegraNotasCliente`)

`criarEmitente(urlBase, tokenSofthouse, EmitenteNovo) -> EmitenteCriado{token}` = `POST {urlBase}/v1/soft/emitente`, header
`Authorization: <token>` CRU. Corpo: `nome, razao, crt, telefone, rua, numero, bairro, cmun, municipio, uf, cep, cnpj|cpf, im?, email?, complemento?,
regime_cibs?, webhook?, documentos:{nfse:true}`. Mesmas defesas de `ConsultaCnpjCliente`: `java.net.http`, sem redirecionamento, conexão 5 s e
resposta 10 s (`emitesimples.integranotas.timeout-conexao/-resposta`, o corpo também tem relógio), corpo máx. 1 MB lido com limite, só https
(exceto `emitesimples.integranotas.permitir-url-local`, dev/test), todo IP resolvido é validado antes de conectar. Tokens só como parâmetro;
nunca em log, exceção ou `toString`; o corpo da resposta (traz o token do emitente) nunca é registrado.
Decide **sempre pelo `codigo`** do JSON, nunca pelo texto:

| Situação | Exceção (HTTP, `codigo`) |
|---|---|
| `codigo` 6000-6003 (ou HTTP 401/403) | 502 `integranotas_token_recusado` |
| `sucesso:false` | 502 `integranotas_recusou`: `mensagem` do gateway (limpa, 200 chars) e `detalhes.erros: ["campo: descricao"]` (máx. 10) + `detalhes.codigoGateway` |
| timeout, rede, DNS | 504 `integranotas_indisponivel` |
| JSON inválido, corpo > 1 MB, 3xx, 5xx, `sucesso` ausente, sucesso sem `token` | 502 `integranotas_resposta_invalida` |

Operações adicionais sobre `chamar`/`interpretar`: `enviarCertificado` (ver "Certificado digital A1"). Envio, consulta e cancelamento de NFS-e: ainda não implementados.

### API da empresa: `GET` e `PUT /api/configuracao/nfse`

`GET` (administrador, operador, consulta) e `PUT` (**só administrador da empresa**). POST/PATCH/DELETE negados. `PLATAFORMA_ADMIN` sem empresa na
sessão: 403 `empresa_nao_selecionada` (no PUT, 403 pelo papel). A empresa é sempre a do token; `empresa`/`empresaId` no corpo são ignorados.

`GET` -> `{ habilitada, serie|null, ultimoNumero, proximoNumero, ambiente, statusCadastro, dataCadastroGateway|null, emitenteTokenConfigurado,
certificadoSituacao, validadeCertificado|null, integracaoDisponivel }`. `PUT {habilitada, serie?, ultimoNumero?}` -> 200 com o mesmo corpo.
O token do emitente **nunca** sai por API (só o boolean). `certificadoSituacao` agora é sempre preenchido (`NAO_ENVIADO` sem certificado) e a resposta do PUT pode trazer `avisos` (ver "Certificado digital A1").

**Habilitar** (`habilitada:true`), nesta ordem de validação:
1. `serie` `^[0-9]{1,5}$` e `ultimoNumero` 0..999999999: senão 400 `requisicao_invalida` (`detalhes.campos`).
2. Se o emitente precisa ser criado e não há token da softhouse para o ambiente ativo: 409 `integranotas_nao_configurada`.
3. Dados da empresa (razão social, fantasia, documento 11/14 dígitos, regime 1-4, telefone 6-14 dígitos, logradouro, número, bairro, CEP 8, município
   com IBGE 7 e UF): senão 422 `cadastro_incompleto`, `detalhes.campos [{campo, mensagem}]`, "Complete os dados da empresa para habilitar a NFS-e."
4. Numeração (abaixo): 409 `numeracao_invalida`.
5. **Garantir o emitente** no gateway quando é preciso (habilitando de novo `false -> true`, `statusCadastro != cadastrado` ou ambiente diferente; gravar série/número com a
   NFS-e já habilitada e emitente cadastrado **não** chama o gateway). Dados enviados: nome=fantasia, razao, crt=regimeTributario, cmun=IBGE, telefone só dígitos, `regime_cibs` se a
   empresa tem; cnpj ou cpf (só no cadastro). Fluxo:
   - **Há token do emitente no cofre** (mesmo ambiente): `PUT /v1/emitente` (token do EMITENTE, sem cnpj/cpf/`documentos`) sincroniza os dados. Validação recusada (5001...):
     502 `integranotas_recusou` com `detalhes.erros`, não habilita (status continua `CADASTRADO`). Token do emitente recusado (6000-6003, ou ilegível no cofre) e há token da softhouse:
     **recupera** (abaixo); sem token da softhouse: 502 `integranotas_token_recusado`. Indisponível / resposta inválida: mesmos erros estáveis, não habilita.
   - **Sem token**: `POST /v1/soft/emitente` (cadastro, com `webhook` se há URL pública). Sucesso: token no cofre (id em `referenciaTokenEmpresa`), `statusCadastro=cadastrado`,
     `dataCadastroGateway=agora`, `ambiente`=ativo. Se o gateway diz que o emitente **já existe** (código 5008 OU mensagem com "já existe"/"já cadastrado", com ou sem acento;
     detecção num lugar só: `IntegraNotasCliente.emitenteJaExiste`) -> **recupera**. Outra falha: `statusCadastro=erro`, não habilita, 502/504 com o código estável; novo PUT recupera.
   - **Recuperar**: `listarEmitentes` com o token da softhouse (sem ele: 409 `integranotas_nao_configurada`); acha pelo CNPJ/CPF (só dígitos, comparação normalizada), guarda **só** o
     token do emitente casado no cofre (substitui e apaga o anterior), `statusCadastro=cadastrado`, `dataCadastroGateway=agora`, e sincroniza com `PUT /v1/emitente` (falha de validação
     bloqueia, mas o token já fica registrado). Não achou, veio sem token ou a listagem falhou: 409 `emitente_existente_sem_token` ("Este CNPJ já está cadastrado na IntegraNotas, mas o
     token do emitente não pôde ser obtido. Confira o token da softhouse ou fale com o suporte.") e `statusCadastro=erro`.
   - **Webhook** (best-effort): garantido o emitente sem criá-lo agora e havendo URL pública, `POST /v1/emitente/webhook {webhook}`. Falha NÃO bloqueia: a resposta traz
     `avisos: ["webhook_nao_atualizado"]` (campo opcional, só aparece se houver aviso; junto dos avisos do certificado).
   - **Rotas A CONFIRMAR EM HOMOLOGAÇÃO** (constantes em `IntegraNotasCliente`): listagem `GET /v1/soft/emitente` (token da softhouse; resposta `{emitentes:[{cnpj, token, ...}]}`) e
     webhook `POST /v1/emitente/webhook`. Confirmada pelo usuário: `PUT /v1/emitente`. **Não** use "Gera novo token" para recuperar: ele invalida o token antigo.
   - A lista contém tokens de OUTROS emitentes: nada dela vai para log, resposta ou banco (só o token do CNPJ casado é guardado, cifrado).
6. Grava `nfseHabilitada=true`, `serieNfse`, `ultimoNumeroNfse`.

**Numeração**: `ultimoNumero` nunca menor que o atual nem que o maior número de nota da empresa na série (tabela `nota`; sem notas, a lista é vazia).
Mudar de série só se a empresa não tem nenhuma nota (a numeração da série nova é livre). **Desabilitar** (`false`) só grava a flag: mantém emitente, token e série.
Reabilitar não recria o emitente. Trocar o ambiente ativo (hoje só homologação é possível) cadastra um emitente novo no ambiente e substitui a referência do token.

**Transação e concorrência**: a chamada externa fica **fora** de transação do banco: (1) transação curta com lock na linha da empresa valida; (2) chamada;
(3) token ao cofre; (4) transação curta com lock grava (e revalida a numeração). PUTs concorrentes da mesma empresa se serializam num mutex em memória
(64 faixas por hash do id, espera até 30 s, senão 409 `operacao_em_andamento`) além do lock da linha; provado com 4 PUTs simultâneos = 1 emitente.
A escrita da Empresa é à mão (EntityManager, RLS ativa); o `/api/Empresa` gerado continua só plataforma.

### Webhook: `POST /api/webhooks/integranotas/{empresaId}/{token}`

**Público** (regra `webhook-integranotas`, só POST nesse caminho; qualquer outro método, e todo o resto de `/api/webhooks/*`, é negado: 401 anônimo, 403 logado).
- `token` = HMAC-SHA256 de `empresaId` em base64url (43 caracteres), com sub-chave derivada da chave-mestra do cofre (`WebhookToken.gerar`, também usado para montar a URL
  registrada no cadastro do emitente). Comparação em tempo constante. Inválido, empresa inexistente ou de outra empresa: 404 `nao_encontrado`, sem dizer o motivo.
- Corpo até 4 MB (413 `corpo_grande_demais`; teto global do servidor 6 MB). Token confere: **200 `{ok:true}`** sempre (corpo que não é objeto JSON incluso). A chamada de
  teste do gateway (`origem` = `TESTE`) responde 200 e **não** grava evento.
- **Persiste** em `EventoWebhook`: `origem, cnpjCpf (só dígitos), tipo, chave, sucesso, codigo, mensagem (500), identificacao, dataRecebimento,
  resultadoAssinatura=nao_verificada, estadoProcessamento=processado (casou com nota da empresa) ou ignorado, ambiente` (o da empresa), `empresa`. **Nunca** xml, pdf, `signature` nem o corpo inteiro. Campos longos
  são truncados ao tamanho da coluna. O banco é aberto como a empresa do caminho (`comoEmpresa`, RLS), não como sistema.
- Limite de taxa em janela de 1 min, em memória: por IP de origem (`emitesimples.webhook.limite-por-ip-por-minuto`, padrão 120, **antes** do token) e por empresa
  (`...-por-empresa-por-minuto`, 120, depois do token); excesso: 429 + `Retry-After`. Testes usam 20 e 5. Nenhum token/URL/corpo em log.
- O evento agora é gatilho de consulta (ver "Emissão de NFS-e"). **O que falta**: verificar a assinatura (`signature`: HMAC-SHA256 + AES/CBC, ainda não confirmada para a v2),
  conhecer o corpo real da notificação (a documentação ainda não foi lida), descompactar xml/pdf, reprocessar eventos pendentes.

### Códigos de erro desta fase

`integranotas_nao_configurada` (409), `cadastro_incompleto` (422), `numeracao_invalida` (409), `operacao_em_andamento` (409), `empresa_nao_selecionada` (403),
`emitente_existente_sem_token` (409), `url_webhook_muito_longa` (409), `url_base_nao_permitida` (409), `integranotas_token_recusado|recusou|resposta_invalida` (502), `integranotas_indisponivel` (504),
`corpo_grande_demais` (413), 429 no webhook, `requisicao_invalida` (400).

### Riscos e decisões

- **Várias instâncias do back**: o mutex é por JVM; o lock da linha só cobre as transações curtas. Duas instâncias podem chamar `criarEmitente` ao mesmo tempo para a mesma empresa
  (o segundo cadastro no gateway pode duplicar ou falhar). Fechar de vez exige uma marca no banco (ex.: status `cadastrando`) numa migração futura.
- **Token perdido**: se o gateway cria o emitente e a gravação final falha, o token fica no cofre sem referência (o id vai no log, nunca o valor) para o suporte recuperar.
- **IP de origem do webhook** vem do socket; atrás de proxy/balanceador todos parecem um IP só (ajustar `quarkus.http.proxy.*` com um proxy confiável) e o limite por IP vira global.
- A URL do webhook é uma capacidade (quem a conhece chama o webhook): trate como segredo; trocar a chave-mestra do cofre invalida as URLs já registradas.
- Sem assinatura verificada, um evento aceito só prova que quem chamou conhece a URL: por isso nasce `pendente`/`nao_verificada` e nada é processado ainda.
- `integracaoDisponivel` só é exigido quando é preciso criar o emitente (reabilitar com emitente existente funciona mesmo sem o token da softhouse).

## Dados da empresa (administrador da empresa)

Código à mão em `nfseconfig/EmpresaDadosResource` e `EmpresaDadosService`. Provado em `EmpresaDadosTest`. A empresa é sempre a do token; a escrita é à
mão (EntityManager sob a RLS da empresa); o `/api/Empresa` gerado continua só plataforma (403 para o administrador da empresa).
`PLATAFORMA_ADMIN` sem empresa na sessão: 403 `empresa_nao_selecionada` (no PUT, 403 pelo papel).

**Sincronização com o gateway** (`PUT /api/configuracao/empresa`): depois de salvar, se `statusCadastro=cadastrado` e há token do emitente, tenta `PUT /v1/emitente` (best-effort). O salvamento nunca falha por isso; se o gateway não atualizar, a resposta traz `avisos: ["emitente_nao_atualizado"]` (campo opcional).

**`GET /api/configuracao/empresa`** (administrador, operador, consulta) -> `{ razaoSocial, nomeFantasia, documento, inscricaoMunicipal|null,
rgInscricaoEstadual|null, email|null, whatsapp|null, telefone|null, cep, logradouro, numeroEndereco, complemento|null, bairro,
municipio: {id, nome, uf, codigoIbge}|null, regimeTributario, regimeIbsCbs|null, documentoEditavel, certificado: {situacao, validade|null,
pendenteDeEnvio, venceEmDias|null} }`. `documentoEditavel=false` quando `statusCadastro='cadastrado'` (o emitente já existe no gateway).

**`PUT /api/configuracao/empresa`** (**só administrador**; POST/PATCH/DELETE negados) -> 200 com o mesmo corpo do GET. O PUT substitui os campos
editáveis (envie todos): `razaoSocial` 1-60, `nomeFantasia` 1-60, `documento` 11 ou 14 dígitos (pontuação aceita e removida), `inscricaoMunicipal` ≤ 20,
`rgInscricaoEstadual` ≤ 20, `email` ≤ 60 válido, `whatsapp` ≤ 20, `telefone` 6-14 dígitos (normalizado para só dígitos), `cep` 8 dígitos (normalizado),
`logradouro`/`numeroEndereco`/`complemento`/`bairro` ≤ 60, `municipio {id}` existente, `regimeTributario` 1..4, `regimeIbsCbs` 0..4 ou null.
Obrigatórios: razão, fantasia, documento, cep, logradouro, número, bairro, município, regime tributário; os demais vazios viram `null`.
**Não altera** ambiente, situação, referências do cofre, `statusCadastro`/gateway, certificado nem `nfse*` (campos extras no corpo são ignorados).

| Erro | HTTP / `codigo` |
|---|---|
| campos inválidos (todos de uma vez) | 400 `requisicao_invalida`, `detalhes.campos [{campo, mensagem}]` (inclui "Município não encontrado.") |
| documento diferente do atual com `documentoEditavel=false` (depois dos erros de campo) | 409 `documento_bloqueado` (mesmo documento ou ausente passa) |
| documento já usado por outra empresa no mesmo ambiente (índice único; o texto da constraint nunca vaza) | 409 `documento_duplicado` |

## Certificado digital A1 (`POST /api/configuracao/empresa/certificado`)

Código à mão em `nfseconfig/Certificado*` e `gateway/IntegraNotasCliente.enviarCertificado`. Provado em `CertificadoTest` (stub HTTP local do IntegraNotas,
PFX de teste em `src/test/resources/certificados/` com os comandos `keytool` no README da pasta). **Só administrador da empresa**; GET/PUT/PATCH/DELETE
negados no caminho. Multipart: `arquivo` (.pfx ou .p12) e `senha` (texto). Limite 10/min por usuário (429 `muitas_tentativas` + `Retry-After`; conta também as
tentativas inválidas). O corpo multipart não é logado; arquivo e senha nunca vão para log, exceção, resposta ou `toString`.

**Passos (e o erro de cada um)**
1. Forma: arquivo ausente 400 `arquivo_obrigatorio`; > 256 KB (`emitesimples.certificado.max-bytes`) 413 `arquivo_grande`; nome sem `.pfx`/`.p12` 400 `arquivo_tipo_invalido`;
   senha fora de `^[A-Za-z0-9]{1,60}$` 400 `certificado_senha_invalida` ("A senha do certificado deve ter só letras e números, sem acentos, até 60 caracteres. A IntegraNotas não
   aceita outros caracteres."), **antes** de abrir o arquivo.
2. Abre como PKCS12 (`java.security.KeyStore`) em memória: senha errada / falha de integridade 400 `certificado_senha_incorreta`; não é PKCS12 ou sem chave privada 400 `certificado_invalido`.
3. Certificado FINAL (o da chave privada): `notAfter` no passado 400 `certificado_vencido`; `notBefore` no futuro 400 `certificado_invalido`.
4. CNPJ do titular: 14 dígitos no Subject (puro ou formatado) e/ou na SAN (otherName 2.16.76.1.3.3). Achou e a empresa tem CNPJ diferente: 400 `certificado_nao_pertence`
   ("O certificado pertence a outro CNPJ."). Não achou nenhum (ou a empresa é CPF): aceita e responde `avisos: ["nao_foi_possivel_conferir_cnpj"]`.
5. Guarda **um** segredo cifrado (AES-256-GCM, `SegredoService`) com JSON `{"pfx":"<base64>","senha":"..."}`; o id vai em `referenciaCertificado` (o anterior é apagado);
   grava `validadeCertificado` (data do `notAfter`, fuso de Brasília) e `situacaoCertificado='nao_enviado'`. Só metadados ficam na linha da empresa.
6. Se `nfseHabilitada` **e** o emitente tem token no cofre: envia ao gateway e grava a situação. Senão fica **pendente** (200, `pendenteDeEnvio:true`) e é enviado quando a NFS-e for habilitada.

**Resposta 200**: `{ situacao, validade, pendenteDeEnvio, venceEmDias, avisos: [...] }`. `situacao`: `NAO_ENVIADO` (guardado, o gateway ainda não aceitou) | `VALIDO` (aceito pelo gateway) | `VENCIDO` |
`INVALIDO` | `NAO_PERTENCE`. `pendenteDeEnvio` = guardado e situação gravada diferente de `VALIDO`. `venceEmDias` negativo se já venceu; uma situação gravada `VALIDO`/`NAO_ENVIADO` com validade no
passado é exibida como `VENCIDO`. O mesmo bloco (sem `avisos`) vem em `GET /api/configuracao/empresa` (`certificado`) e a situação/validade em `GET /api/configuracao/nfse` (`certificadoSituacao`, `validadeCertificado`):
os três usam `CertificadoSituacao`. Sem certificado a situação é `NAO_ENVIADO` com `pendenteDeEnvio:false`.

**Gateway** (`IntegraNotasCliente.enviarCertificado(urlBase, tokenEmitente, pfx, senha)`): `POST {urlBase}/v1/certificado`, JSON `{certificado: <Base64>, senha}`, `Authorization: <token do emitente>` CRU.
Mesmas defesas e códigos estáveis do `criarEmitente`; decide pelo `codigo`, nunca pelo texto.

| Retorno do gateway | Efeito |
|---|---|
| sucesso (`sucesso:true`, HTTP 200; `codigo` ausente ou 200) | situação `VALIDO` |
| `codigo` 5003 | 400 `certificado_invalido_gateway` ("A IntegraNotas não conseguiu abrir o certificado (arquivo corrompido ou senha incorreta)."), situação `INVALIDO` |
| 5011 | 400 `certificado_vencido_gateway`, situação `VENCIDO` |
| 5029 | 400 `certificado_nao_pertence_gateway`, situação `NAO_PERTENCE` |
| token recusado (6000-6003, HTTP 401/403) | 502 `integranotas_token_recusado` ("token do emitente"); situação **não muda** (`NAO_ENVIADO`) |
| timeout/rede | 504 `integranotas_indisponivel`; situação não muda |
| JSON inválido, 5xx, 3xx, corpo > 1 MB, `sucesso` ausente | 502 `integranotas_resposta_invalida`; situação não muda |

Nas falhas de transporte (3 últimas linhas) o certificado **segue guardado** (`detalhes.certificadoGuardado:true`) para nova tentativa (basta enviar de novo, ou habilitar a NFS-e).
Nas recusas do gateway a situação grava o motivo e o certificado também segue guardado.

**Envio pendente ao habilitar a NFS-e** (`PUT /api/configuracao/nfse` com `habilitada:true`): depois de habilitar com sucesso, se há certificado guardado com situação gravada ≠ `VALIDO`, envia-o
(lê do cofre só nesse instante). Falha **não desfaz** a habilitação: a resposta é 200 com o estado atual e o campo opcional novo `avisos` (ausente quando vazio): `certificado_nao_enviado`
(token recusado, indisponível, resposta inválida) ou `certificado_recusado` (5003/5011/5029; veja `certificadoSituacao`). Se o PUT cria um emitente novo (outro ambiente), um certificado `VALIDO`
volta a `NAO_ENVIADO` (o emitente novo não o conhece) e é reenviado.

**O que falta**: consultar o certificado no gateway (`GET /v1/certificado`, vencimento real), renovação/alerta de vencimento (hoje só `venceEmDias`), e confirmar na doc oficial o corpo de sucesso
(`codigo` pode vir ausente) e o formato do arquivo (a doc fala "conteúdo do arquivo em Base64"; enviamos o .pfx cru em Base64).

### Riscos e decisões (certificado)

- O cofre é provisório (chave-mestra em variável de ambiente): quem tem a chave e o banco decifra o PFX **e** a senha. O PFX de um A1 é uma chave privada: restringir quem lê a tabela `segredo`.
- Mudar o `documento` da empresa (permitido até o emitente ser cadastrado) **não** revalida um certificado já guardado; o gateway acusa 5029 no envio.
- "Integração disponível" para o certificado exige só o token do **emitente** e a URL do ambiente; o token da softhouse (usado só para criar o emitente) não é exigido.
- O mutex por empresa (`TravaEmpresa`) vale por JVM, como no cadastro do emitente; com várias instâncias dois envios simultâneos podem se cruzar (o último a gravar vence).
- Upload: o limite de 256 KB é conferido pelo tamanho informado pelo multipart e de novo depois da leitura; o arquivo temporário do upload é apagado pelo framework.

## Emissão de NFS-e (`/api/nfse/notas`, pacote `nfseemissao`)

Escrito à mão (o `gerar.sh` não apaga `nfseemissao` nem `gateway`). **Só homologação nesta fase**: com o ambiente ativo = produção, criar/enviar/acompanhar ficam
bloqueados (409 `emissao_producao_bloqueada`) salvo `emitesimples.nfse.permitir-emissao-producao=true` (padrão `false`, mantenha). Os testes usam **só o stub HTTP local**
(`IntegraNotasStubResource`); nunca chamam o gateway real.

### Contrato (sempre a empresa da sessão; leitura: administrador|operador|consulta; escrita: administrador|operador; plataforma sem empresa: 403 `empresa_nao_selecionada`)

| Rota | O que faz |
|---|---|
| `GET /api/nfse/notas/{from}/{to}?estado=&busca=` | lista (até 100 por chamada), `dataCriacao` desc; busca por número, nome da pessoa (sem diferenciar caixa; diferencia acentos) e chave; **sem xml/pdf** |
| `GET /api/nfse/notas/count?estado=&busca=` | `{count}` |
| `GET /api/nfse/notas/{id}` | item + `informacoesComplementares, discriminacao, descricao, codigoServico, errosRetorno [{campo,erro,descricao}]\|null, linkPdf, codigoVerificacao, dataHoraEvento, editavel, podeReenviar, podeAtualizar, cancelamento, podeCancelar, podeAprovar, podeDescartar` |
| `POST /api/nfse/notas` | `{pessoaId, servicoId, valorServicos, informacoesComplementares?, discriminacao?}`: cria **e envia**; 200 com o detalhe |
| `POST /api/nfse/notas/{id}/enviar` | corpo opcional com os mesmos campos (só o que vier muda); só `FALHA_ENVIO`/`REJEITADA` (`RASCUNHO` agora existe só para o assistente de IA) |
| `POST /api/nfse/notas/{id}/atualizar` | consulta o gateway agora e devolve o detalhe |
| `POST /api/nfse/notas/{id}/cancelar` | `{codigoMotivo: 1|2|9, motivo: 15..255 caracteres após trim}` (administrador, plataforma; **operador só com a delegação `podeCancelarNfse` do vínculo**, senão 403 `cancelamento_nao_delegado`; consulta: 403): cancela a nota **autorizada** no mês vigente da emissão; 200 com o detalhe (`estado` `CANCELADA` quando concluído). Ver "Cancelamento" abaixo |
| `GET /api/nfse/notas/{id}/cancelamento/pdf` | PDF do evento de cancelamento (`inline`); 404 `pdf_indisponivel` se não houver |
| `GET /api/nfse/notas/{id}/pdf` | `application/pdf` (`inline`) do PDF guardado; senão 404 `pdf_indisponivel` (o front usa `linkPdf`) |

Item da lista: `id, numero, serie, estado, dataEmissao, valorServicos, pessoa{id,nome,documento}, servico{id,nome}, chave, numeroNfse, codigoRetorno, mensagemRetorno, temPdf, dataCriacao`.
Erros: 409 `nfse_nao_habilitada`, `emissao_producao_bloqueada`, `emitente_nao_cadastrado`, `emitente_de_outro_ambiente`, `certificado_nao_valido`, `servico_inativo`,
`nota_em_processamento`, `nota_nao_editavel`, `nota_em_conflito`, `numeracao_esgotada`; 404 pessoa/serviço/nota de outra empresa ou inexistente; 422 `tomador_incompleto`
(`detalhes.campos`: documento, logradouro, numeroEndereco, bairro, cidade); 400 `requisicao_invalida` (`detalhes.campos`: `valorServicos` 0,01 a 9999999999999,99 com até 2 casas,
`informacoesComplementares` ≤ 2000, `discriminacao` ≤ 2000) e `discriminacao_obrigatoria`; 429 (30 criar/enviar por minuto por usuário, `emitesimples.nfse.limite-envios-por-minuto`);
502/504 só em `/atualizar` (token recusado, resposta inválida, indisponível).

### Cancelamento (`POST /api/nfse/notas/{id}/cancelar`)

Corpo `{codigoMotivo, motivo}`: `codigoMotivo` 1 (erro de emissão), 2 (serviço não concluído) ou 9 (outros); `motivo` de 15 a 255 caracteres contados após `trim`.
O detalhe da nota ganhou `cancelamento` (`null` ou `{estado, codigoMotivo, motivo, codigoRetorno, mensagemRetorno, dataHoraEvento, solicitadoEm}`, do pedido mais recente; `estado`
`PROCESSANDO`|`CONCLUIDO`|`RECUSADO`|`EXPIRADO`) e `podeCancelar` (`AUTORIZADA`, mês vigente, sem cancelamento `PROCESSANDO` e usuário da sessão com permissão: operador sem delegação = false; só `PROCESSANDO` bloqueia novo pedido). `podeAtualizar` também vale para `AUTORIZADA` com cancelamento `PROCESSANDO`.

- **Mês vigente**: mês/ano de `dataEmissao` == mês/ano de agora em `America/Sao_Paulo`; senão 409 `cancelamento_fora_do_mes` ("O cancelamento só é possível dentro do mês vigente da emissão da nota.") sem chamar o gateway. Regra do produto; prazos por município continuam não documentados.
- **Fluxo**: uma transação trava a nota, valida e grava o `Cancelamento` `PROCESSANDO` (um por vez: 409 `cancelamento_em_andamento`); o gateway (`POST /v1/nfse/cancela`, `{chave, codigo_cancelamento, motivo_cancelamento}`, token do emitente) é chamado **fora** da transação e a chamada vai para `ChamadaGateway` (`cancelar_nfse`, sem motivo nem token).
- **Retorno** (pelo `codigo`): 101 ou 5015 (já cancelada, idempotente) = `CANCELADA` + Cancelamento `CONCLUIDO` (xml/pdf do evento decodificados com `ArquivoGateway`); recusa (5013, 5001, 5006..., e token recusado) = Cancelamento `RECUSADO` com `codigoRetorno/mensagemRetorno` e **409 `cancelamento_recusado`** com a mensagem do gateway (a nota segue `AUTORIZADA`; dá para tentar de novo); resposta inconclusiva (5023, 5030, 5034, 5027, 8429, 7xxx, 9xxx, 0, sucesso com código desconhecido), timeout, rede ou resposta inválida = **200** com o Cancelamento `PROCESSANDO` (a nota segue `AUTORIZADA`).
- **Acompanhamento**: o job, o `/atualizar` e o webhook consultam também notas `AUTORIZADA` com cancelamento `PROCESSANDO`; ao receber 101 a nota vira `CANCELADA` e o Cancelamento `CONCLUIDO`. Pedido pendente há mais de 24 h em que o gateway ainda diz autorizada (100) vira `EXPIRADO` (mensagem "não confirmou", sem `dataHoraRecusa`, que fica só para `RECUSADO`) para liberar novo pedido.
- Outros erros: 404 nota; 409 `nota_nao_autorizada` (nota já `CANCELADA` = sucesso idempotente, devolve o detalhe sem chamar o gateway), `emissao_producao_bloqueada` (mesma regra da emissão), `emitente_nao_cadastrado`, `emitente_de_outro_ambiente`; 400 `requisicao_invalida` (`detalhes.campos`: `codigoMotivo`, `motivo`); 403/429 como nos demais POSTs (o limite de 30/min é o mesmo de criar/enviar).

### Assistente de IA: rascunhos (preparar, aprovar, descartar)

Fluxo do mapa (`CicloNota`, `CicloCancelamento`): o assistente só **prepara** um rascunho; uma **pessoa** aprova (envia) ou descarta. Papéis de todas as rotas abaixo: administrador, operador e plataforma_admin (plataforma sem empresa na sessão: 403 `empresa_nao_selecionada`); consulta: 403; anônimo: 401; outra empresa: 404.

| Rota | O que faz |
|---|---|
| `POST /api/nfse/notas/preparar` | corpo igual ao de `POST /api/nfse/notas`; 200 `NotaDetalhe` `RASCUNHO`. **Não** aloca número/série/`dataEmissao`, não chama o gateway, não consome o limite de envio. Valida pessoa/serviço da empresa (404), serviço ativo, valores, discriminação, tomador completo (422 `tomador_incompleto`), tributos aproximados do Simples e NFS-e habilitada; **não** exige certificado/emitente (isso é na aprovação). Copia o serviço para a nota. Grava `preparadoPorUsuario` (usuário da sessão); `preparadoPorCliente` e `dataHoraAprovacao` ficam `null` |
| `POST /api/nfse/notas/{id}/aprovar` | corpo opcional igual ao de `/enviar` (ajusta campos). Só `RASCUNHO` (senão 409 `nota_nao_e_rascunho`). Mesma lógica de criar+enviar: trava a nota e a Empresa, aloca número/série, `dataEmissao`/`dataCompetencia` agora, grava `aprovadoPor`/`dataHoraAprovacao`, envia ao gateway e aplica o retorno; `RASCUNHO` -> `PROCESSANDO`. Exige certificado/emitente/ambiente (409 como em criar). Consome o limite de envio. Duas aprovações simultâneas: uma envia, a outra recebe 409 |
| `POST /api/nfse/notas/{id}/descartar` | só `RASCUNHO` (409 `nota_nao_e_rascunho`); vira `DESCARTADA` e grava `descartadoPor`/`dataHoraDescarte`. Não toca numeração nem gateway |
| `POST /api/nfse/notas/{id}/cancelamentos/preparar` | `{codigoMotivo, motivo}`; 200 `NotaDetalhe` com `cancelamento.estado = rascunho` (a nota segue `AUTORIZADA`). Valida no preparo: nota autorizada com chave, mês vigente (409 `cancelamento_fora_do_mes`), motivo (400), nenhum cancelamento `PROCESSANDO` **ou** `RASCUNHO` (409 `cancelamento_em_andamento`), ambiente/emitente. Não envia ao gateway. Não exige a delegação de cancelar (quem aprova é que precisa) |
| `POST /api/nfse/cancelamentos/{id}/aprovar` | revalida tudo (o mês pode ter virado), exige a permissão de cancelar (administrador/plataforma; operador só com `podeCancelarNfse`, senão 403 `cancelamento_nao_delegado`), grava `aprovadoPor`/`dataHoraAprovacao`, `RASCUNHO` -> `PROCESSANDO` e envia como o cancelamento direto (101/5015 concluído, recusa 409 `cancelamento_recusado`, inconclusivo `PROCESSANDO`). Responde o `NotaDetalhe` da nota. Só rascunho (409 `cancelamento_nao_e_rascunho`) |
| `POST /api/nfse/cancelamentos/{id}/descartar` | mesma permissão de cancelar; `RASCUNHO` -> `DESCARTADO` (`descartadoPor`, `dataHoraDescarte`); responde o `NotaDetalhe` |

`NotaDetalhe` ganhou `podeAprovar` e `podeDescartar` (true só para `RASCUNHO` e papel que escreve). `cancelamento` (`CancelamentoInfo`) ganhou `id`, `podeAprovar` e `podeDescartar` (true só para cancelamento `RASCUNHO` e usuário com a permissão de cancelar); `estado` agora também `RASCUNHO` e `DESCARTADO`. `podeCancelar` (cancelamento direto) fica `false` enquanto houver cancelamento `RASCUNHO` ou `PROCESSANDO` (evita duplicidade; o `POST .../cancelar` direto também responde 409 `cancelamento_em_andamento`). O CRUD gerado `/api/Cancelamento` segue somente leitura. Política de acesso: `nfse-cancelamentos-*` em `application.properties` (só POST, administrador/operador/plataforma; o resto negado), provada em `PoliticaAcessoTest.rotasDoAssistenteDeNotasECancelamentos`; fluxo provado em `NfseEmissaoTest`.

**BLOQUEIO — descartar rascunho de NOTA**: o `CHECK ck_nota_numeracao` (V9) exige `numero/serie/dataEmissao` em todo estado diferente de `RASCUNHO`, então `rascunho -> descartada` (que não tem número) falha no banco (500 hoje). Falta uma migração V10 que troque o CHECK por `ESTADO IN ('rascunho','descartada') OR (NUMERO IS NOT NULL AND SERIE IS NOT NULL AND DATA_EMISSAO IS NOT NULL)`. O código de `descartar` está pronto; os testes dependentes estão `@Disabled` em `NfseEmissaoTest` até a V10. O descarte do rascunho de **cancelamento** não é afetado.

**PENDÊNCIA DE SEGURANÇA — autenticação do assistente**: ainda não existe autenticação do assistente de IA (a entidade `ClienteAcesso` existe, sem login/token). Hoje `preparar` roda com a sessão de uma **pessoa** e grava `preparadoPorUsuario` = essa pessoa; `preparadoPorCliente` fica sempre `null`, então o rastro não distingue "preparado pelo assistente" de "preparado pela própria pessoa". Antes de expor o assistente: credencial própria (cliente de acesso), escopo restrito a `preparar`/leitura (nunca aprovar/descartar), gravar `preparadoPorCliente` e rejeitar aprovação por quem não é pessoa humana autenticada.

### Estados e transições

`FALHA_ENVIO` (falha de infraestrutura no envio direto; reenviável com o mesmo número) · `RASCUNHO` (só assistente de IA, sem número) · `PROCESSANDO` · `EMITIDA_OFFLINE` · `AUTORIZADA` · `REJEITADA` · `EM_CONFLITO` · `CANCELADA` · `DESCARTADA` (rascunho descartado por uma pessoa). Cancelamento: `RASCUNHO` · `PROCESSANDO` · `CONCLUIDO` · `RECUSADO` · `EXPIRADO` · `DESCARTADO`.
Decide-se **sempre pelo `codigo`** (tabela única em `NfseRetorno`):

| Retorno | Envio | Consulta |
|---|---|---|
| 5023, ou 2, com `chave` | `PROCESSANDO` (guarda a chave) | 5023: continua; 2: `EMITIDA_OFFLINE` (PDF, sem XML) |
| 100 | com `status Autorizado`: `AUTORIZADA`; sem: `PROCESSANDO` + UMA consulta imediata | `AUTORIZADA` (numeroNfse, codigoVerificacao, dataHoraEvento, linkPdf, xml/pdf) |
| 101 | – | `CANCELADA` |
| 5001, 5002, 5005, 5007, 5024, 5025, 5028, 5033 (e outra recusa) | `REJEITADA` + `codigoRetorno/mensagemRetorno/errosRetorno` (JSON campo/erro/descricao, ≤ 4000) | idem (de `PROCESSANDO`/`offline`/`conflito`) |
| 5027 | `EM_CONFLITO` (não reenviar; usar Atualizar) | `EM_CONFLITO` |
| 5030, 5034, 8429, 7xxx, 9xxx | `PROCESSANDO` com mensagem se vier `chave`; **sem chave vai a `FALHA_ENVIO`** (nada a acompanhar; seguro repetir) | mantém o estado e grava a mensagem |
| 5006 | – | mantém e registra "não encontrada" |
| 6000 a 6003 / timeout / rede / JSON inválido / 5xx / 0 (manutenção) | **nunca rejeita**: `FALHA_ENVIO`, mensagem "A IntegraNotas está indisponível. Tente enviar novamente." (token recusado: mensagem própria), `podeReenviar=true`, **mesmo número**; HTTP 200 | mantém o estado (job ignora; `/atualizar` devolve 502/504) |

Estados finais não regridem: `AUTORIZADA` só vira `CANCELADA` (101); `CANCELADA`/`DESCARTADA` nunca mudam; `REJEITADA`/`FALHA_ENVIO`/`RASCUNHO` não são consultadas.
`editavel` = `FALHA_ENVIO`|`REJEITADA`; `podeReenviar` = `editavel`; `podeAtualizar` = `PROCESSANDO`|`EMITIDA_OFFLINE`|`EM_CONFLITO` com chave.
Reenviar: `PROCESSANDO`/`EMITIDA_OFFLINE` → 409 `nota_em_processamento`; `EM_CONFLITO` → 409 `nota_em_conflito`; `AUTORIZADA`/`CANCELADA`/`DESCARTADA` → 409 `nota_nao_editavel`.

### Numeração e idempotência

- Criar: **uma transação** com `SELECT ... FOR UPDATE` na linha da Empresa: `numero = ultimoNumeroNfse + 1`, incrementa `ultimoNumeroNfse`, grava a nota `PROCESSANDO` (série, ambiente, `dataEmissao` = agora em São Paulo, `dataCompetencia` = hoje). O número fica **na nota** e todo reenvio o reaproveita (reenvio nunca aloca número; só a `dataEmissao` é renovada).
  Unicidade (empresa, ambiente, série, número): já existe `UX_NOTA_NUMERACAO` (V2); **não foi preciso migração V7**.
- Snapshot do serviço na criação (código, tributação municipal, NBS, descrição, discriminação informada ou padrão, alíquota ISS, ISS retido, tipo de operação, CST — `'00'` se vazio — e o valor informado); se a troca de serviço ocorrer num reenvio, refaz-se o snapshot.
- Duplo clique: o reenvio trava a linha da nota (`FOR UPDATE`), troca o estado para `PROCESSANDO` e confirma **antes** de chamar o gateway; o segundo clique enxerga `PROCESSANDO` e recebe 409. A chamada ao gateway é sempre **fora** da transação.
- Cada chamada gera uma `ChamadaGateway` (operação, método, rota, versão, HTTP, código, mensagem, duração, chave, identificação, resumo curto). Nunca payload, resposta completa, xml/pdf nem token.

### Payload do envio v2 (`NfsePayloadMapper`, único ponto de montagem)

`POST {urlBase}/v2/nfse` com o token do **emitente** (cofre; header `Authorization` cru). Raiz: `data_emissao` (ISO, offset `-03:00`), `numero`, `serie`, `identificacao` (id da nota sem hífens, 32 caracteres),
`data_competencia`, `informacoes_complementares?`, `tomador{cnpj|cpf, razao_social, im?, telefone?, email?, tipo_destinatario?, endereco{logradouro, numero, complemento?, bairro, cep?, codigo_municipio, uf}}`,
`servico{codigo, discriminacao, descricao?, codigo_tributacao_municipio?, codigo_nbs?, valor_servicos, tributos_municipais{tipo_operacao, aliquota_iss?, iss_retido}}`.
Opcionais ausentes são omitidos. O prestador não vai no corpo. **Não** se envia `ibs_cbs`; `tributos_nacionais` só se o CST ≠ `'00'` ou houver valores; `tributos_totais` só com
`emitesimples.integranotas.enviar-tributos-totais=true` (padrão `false`).
**A CONFIRMAR EM HOMOLOGAÇÃO**: (1) nome do campo do percentual do Simples em `tributos_totais` (hoje `percentual_simples_nacional`, constante `CAMPO_PERCENTUAL_SIMPLES`);
(2) se `numero` sem zeros à esquerda é aceito; (3) `tipo_destinatario` para tomador PF; (4) quais blocos o gateway exige por si; (5) tolerância do gateway à `data_emissao` (renovada a cada reenvio).

### Acompanhamento (job + atualizar + webhook como gatilho)

- **Job** `NfseAcompanhamentoJob` (`@Scheduled every=emitesimples.nfse.acompanhamento.intervalo`, padrão 10 s, `concurrentExecution=SKIP`; `...acompanhamento.habilitado` é `true` em dev e `false` em teste, e os testes chamam `NfseAcompanhamentoService.executarCiclo()`):
  consulta `GET {urlBase}/v1/nfse/{chave}` das notas `PROCESSANDO`/`EMITIDA_OFFLINE` com chave, no máx. `...max-consultas-por-ciclo` (20); backoff por nota **em memória** (10 s nos primeiros 2 min, 60 s depois); após 24 h
  para de consultar e grava "Sem retorno do gateway; use Atualizar". Sem sessão HTTP: a única leitura entre empresas (ids das notas pendentes e das velhas) roda `comoSistema`; cada consulta e cada marcação roda `comoEmpresa(empresaId)` (RLS da empresa).
  Só consulta quando `nota.ambiente == empresa.ambiente == ambiente ativo` e a produção não está bloqueada.
- **`POST /atualizar`**: consulta na hora (nota sem acompanhamento possível devolve o detalhe sem chamar o gateway).
- **Webhook** (`WebhookIntegraNotasService`): o evento é **só gatilho**. Casa por `chave` ou `identificacao` (id da nota, com ou sem hífens) **dentro da empresa do caminho**; casou: evento `PROCESSADO` com a nota vinculada e uma consulta ao gateway (a autoridade) é agendada fora da requisição (pool de 2 threads, fila de 200);
  não casou: `IGNORADO`. O conteúdo (inclusive "autorizada") nunca altera a nota. Reentrega do mesmo evento (índice único de deduplicação empresa/ambiente/chave/código) não regrava, mas aciona o gatilho de novo (antes dava 500).
- Resposta de consulta com `identificacao` diferente da nota é ignorada. xml/pdf: Base64, gzip ou zip detectados pelos primeiros bytes (ou texto puro), limite de 2 MB **depois** de descompactar.

### Segurança, limites e riscos

Token do emitente, payload, xml, pdf e dados do tomador não vão para log (provado em `NfseEmissaoTest`). Toda consulta filtra por `empresa.id` (administrador da plataforma com empresa na sessão também). Limites: 30 criar/enviar por minuto por usuário (em memória, N× com várias instâncias);
corpo de consulta do gateway até 6 MB; 1 MB nos demais.
Riscos: (a) **timeout no envio**: se o gateway recebeu mas a resposta se perdeu, a nota fica em `FALHA_ENVIO`; o reenvio usa o mesmo número e a mesma `identificacao`, e o gateway deve responder 5027 (conflito) ou a mesma chave, sem nota duplicada; (b) o backoff é em memória (reinício consulta tudo de novo, sem dano);
(c) a trava de duplo clique é por linha do banco e vale entre instâncias; o limite por minuto e o pool do webhook não; (d) o job usa `dataCriacao` para a janela de 24 h; (e) busca por nome diferencia acentos; (f) os campos de `tributos_totais` são hipótese; (g) com o gateway devolvendo 7xxx/9xxx com chave e sem webhook, a nota fica `PROCESSANDO` até o job conseguir uma resposta final (24 h, depois "use Atualizar").

### O que falta

Cancelamento (`POST /v1/nfse/cancela`), envio de e-mail da nota, substituição, reconciliação das NFS-e offline via `GET /nfse/offline`, contingência, verificação de assinatura do webhook, tomador no exterior (código de país/NIF), blocos de obra/evento/deduções/IBS-CBS, paginação por cursor, autenticação do assistente de IA.

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
