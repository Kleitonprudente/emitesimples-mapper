# Memória — domain-mapper (emitesimples)

Tier B. Fontes principais: `docs/integranotas-nfse-v2.md` (NFS-e: envio v2, consulta e cancelamento v1) e
`docs/integranotas-softhouse.md` (cadastro de emitente e certificado digital), colados pelo usuário em 2026-11-20.

## Decisões do usuário (2026-11-20)
- Pacote base `br.com.emitesimples`. Nome do projeto no CLI: `emitesimples`. Pasta dos mapas: `mapas/`.
- **Renomeado em 2026-11-20 (pedido do usuário)**: `Emitente` → `Empresa` (no novo contexto `configuracao`), `Tomador` →
  `Pessoa`; contextos `cadastro` → `cadastros`, `acesso` → `usuarios`, `emissao` → `nfse`, `integracao` → `integranotas`. O IntegraNotas continua
  dizendo "emitente" nas rotas e na documentação: no mapa o termo é `Empresa`; nos docs e neste resumo da API, "emitente" é
  o conceito do gateway. As seções abaixo que dizem Emitente/Tomador foram escritas antes do renome.
- Contextos, nesta ordem de dependência: configuracao (Empresa) e cadastros (Municipio, Pessoa, Servico), que dependem um do outro → usuarios
  (Usuario, VinculoUsuario, ClienteAcesso, TokenRenovacao) → nfse (Nota, Cancelamento) → integranotas (ChamadaGateway,
  EventoWebhook). **Dependência mútua aceita pelo usuário**: a Empresa (configuracao) referencia o Municipio (cadastros) e
  Pessoa e Servico (cadastros) referenciam a Empresa. Modele Municipio e Empresa juntos.
- Empresa = empresa usuária. Pessoa = cadastro de pessoa (atua como tomador na NFS-e). Municipio nasce com IBGE. Sem entidade RPS.
- Emitir e cancelar exigem confirmação humana: Nota nasce em `rascunho`, Cancelamento em `aguardando_aprovacao`; só uma
  pessoa aprova (Nota: administrador ou operador; Cancelamento: administrador).
- Stack alvo: Quarkus (Java 21).

## O que a API v2 exige no envio (resumo; detalhe no doc)
- Raiz obrigatória: `data_emissao`, `numero` (1 a 9 dígitos), `serie` (1 a 5 dígitos).
- Tomador (hoje a entidade `Pessoa`): `razao_social` e endereço (`logradouro`, `numero`, `bairro`, `uf`); `cnpj` ou `cpf`.
- Serviço: `codigo` (6 dígitos, LC 116), `discriminacao` (até 2000), `valor_servicos` (mínimo 0.01).
- `tributos_nacionais.cst` e o bloco `ibs_cbs` têm campos obrigatórios; se os blocos são obrigatórios por si, o schema
  não diz: confirmar em homologação antes de tornar opcional no mapa.
- O prestador não vai no corpo: vem do token.
- `identificacao` (1 a 40) é a chave de correlação do sistema emissor: guardar na Nota.
- Percentuais em pontos (5.00 = 5%). Quebra de linha só `\n`.

## Retorno da API que o mapa precisa comportar
`chave`, `numero`, `codigo_verificacao`, `data_hora_evento`, `status`, `xml`, `pdf`, `link_pdf` (às vezes ausente),
e `rps_numero`/`rps_serie` (atributos opcionais da Nota, não entidade).

## Cancelamento e ciclo da nota (confirmado pelo usuário em 2026-11-20)
- Envio `POST /v2/nfse` devolve `5023` (em processamento) com a `chave`; consulta `GET /v1/nfse/{chave}` devolve `100`
  (autorizada); cancelamento `POST /v1/nfse/cancela` devolve `101`. A versão da API varia por operação.
- Cancelamento exige `chave`, `codigo_cancelamento` (1 Erro de Emissão, 2 Serviço não concluído, 9 Outros) e
  `motivo_cancelamento` (15 a 255 caracteres). Devolve `data_hora_evento`, `xml` e `pdf` (Base64).
- Estados da Nota a comportar: processando, emitida offline (código 2: aceita, aguardando a prefeitura, sem XML nem
  protocolo), autorizada, cancelada, rejeitada, em conflito. Erro de campo (5001/5002) traz `erros[]` (campo, erro,
  descrição, detalhes); a `chave` pode vir mesmo em falha. Gravar a `chave` no primeiro retorno.
- `data_hora_evento` do retorno: `AAAA-MM-DD HH:MM:SS`, sem fuso. `link_pdf` é opcional. A `chave` tem 50 dígitos (padrão nacional); ignore menções a 44.

## Emitente (confirmado pelo usuário em 2026-11-20; detalhe no doc)
- Cadastro: `POST /v1/soft/emitente`, com o Token da Softhouse. Devolve o Token do Emitente (JWT), por ambiente.
- Obrigatórios: nome fantasia, razão social, regime tributário (1 Simples, 2 Simples excesso de sublimite, 3 Normal,
  4 MEI), telefone, endereço completo com `cmun` (IBGE) e CEP. Identificação por CNPJ ou CPF.
- Opcionais úteis: apelido, inscrição municipal, e-mail, logo, regime do IBS/CBS, URL do webhook.
- Segredos que o emitente carrega e que **não viram atributo**, só referência ao cofre: Token do Emitente, credenciais da
  prefeitura (login, senha, client id, client secret) e a senha do certificado. O Token da Softhouse é config do sistema.
- O webhook é registrado no cadastro; a URL precisa responder 200 para ser aceita.

## Certificado e decisões (2026-11-20)
- `/v1/certificado` recebe `certificado` (Base64) e `senha` (1 a 60; só letras e números; sem acento). Erros 5003, 5011, 5029.
  Mapa: só metadados do certificado (validade, situação); arquivo e senha são referência ao cofre.
- Emitente por CNPJ **ou** CPF. `regime_cibs` e o bloco `ibs_cbs` do envio: opcionais por ora.
- Banco PostgreSQL. Multi-tenant por empresa (decisão do usuário em 2026-11-20): "cada emissor só emite sua nota".
  `Empresa` é o tenant; Servico, Pessoa, Nota, Cancelamento e as do integranotas e usuarios (exceto Usuario) levam
  `tenantKey` na relação `[r] empresa`; Municipio é compartilhado. **Pessoa é por empresa**: o mesmo CNPJ/CPF pode se
  repetir entre empresas.
- Autenticação (decisão do usuário, revista em 2026-11-20): o back-end Quarkus **emite os próprios JWT** (fase 1: front-end
  e agentes próprios; OAuth do MCP de terceiros adiado). O contexto `usuarios` já está modelado: Usuario (global, sem
  tenant), VinculoUsuario, ClienteAcesso e TokenRenovacao. Senha só como hash, segredo de cliente só como hash, e **token
  nunca em entidade**.
- Pack Quarkus: pacote no padrão `<raiz>.domain.<contexto>`; `id: Texto(36)` em toda entidade; `cn` também nas
  relações; `Inteiro(n)` com tamanho; sem `:` em description de pacote.

## Cadastro de Pessoa (pedido do usuário em 2026-11-21; fonte: `docs/historias-cadastro-pessoa.md`)
- `Pessoa` (13 entidades no mapa): `documento` único (Texto(14), só dígitos; 14 é CNPJ, 11 é CPF; no envio ao IntegraNotas
  o sistema separa em `cnpj` ou `cpf`, que lá são campos distintos, mas só UM é enviado), `rgInscricaoEstadual` único,
  `nome` (= razão social), `nomeFantasia`, `inscricaoMunicipal`, `tipoPessoa` (`fisica` ou `juridica`), `consumidorFinal`
  (Boolean, defaultValue true; pessoa jurídica com inscrição estadual vira não: regra de aplicação), `tipoDestinatario`,
  endereço, e-mail e telefone. Retirados: nif, dispensaNif, caepf, tipoEnteGovernamental. **Mantidos** os 4 campos de
  exportação (decisão do usuário).
- **Cidade**: relação `[r] cidade` (opcional, por causa da exportação) para `Municipio`, que guarda nome, UF e o **IBGE**. O
  IBGE NÃO é visível ao usuário: sai de cidade+UF. A Pessoa não tem `uf` nem IBGE. (nome, UF) é único (só documentado).
- **Contato** (`[m] pessoa`, coleção `[o] contatos`; `[r] empresa` com tenantKey): `tipoContato` (socio, financeiro,
  comercial, fiscal, outro: confirmados pelo usuário), `nome` (acrescentado porque os sócios do QSA do CNPJ.ws entram como
  contatos), `whatsapp`, `email`. Vários contatos por pessoa.
- Retrato da pessoa na Nota: `documentoPessoa` (documento único, decisão do usuário) e `razaoSocialPessoa`.
- Pendente (fora do mapa): carga da tabela de cidades com as ~5.570 cidades e o IBGE; integrações CNPJ.ws e BrasilAPI (CEP)
  ainda não lidas.
- Estado: `mvn compile` e `mvn package` passam com 13 entidades, 82 fontes (2026-11-21).

## Cidades administráveis (decisão do usuário em 2026-11-21; especificação em `docs/carga-municipios.md`)
- A tabela `Municipio` é controlada pelo **administrador da plataforma**, por upload de arquivo novo e correção manual. A
  importação atualiza ou insere pelo código IBGE e **nunca apaga**. O registro especial **`Exterior`** (UF `EX`, IBGE `0`,
  nome `Exterior`) FICA, para notas ao exterior; o `0` nunca vai ao IntegraNotas (envio com `uf = EX` e sem código do
  município); a Empresa não pode usá-lo.
- `ImportacaoMunicipio` (14ª entidade, contexto `cadastros`): histórico dos envios, com `[r] usuario` obrigatório (sem
  tenantKey, sem ambiente), totais `Inteiro(9)`, `estado` (em_andamento, concluida, concluida_com_rejeicoes, falhou) e
  resumo das rejeições. Cria o laço de dependência cadastros, usuarios, configuracao, cadastros.
- **Flyway é o dono do esquema do banco**; o Hibernate só valida. Unicidade (`codigo_ibge`; `nome`+`uf`) vai em migração.

## Não confirmado
Corpo da notificação do webhook, compactação do XML/PDF dele; rotas auxiliares da v2; quais blocos do envio são obrigatórios por si; prazos de cancelamento por município.

## Regras
- Ambiente (homologação ou produção) é campo de controle. Segredos (token, senha do certificado) nunca em entidade.
- **NUNCA nomeie um atributo como `<nomeDaRelação><Campo>`** (descoberto no primeiro `mvn compile`, 2026-11-20). Para cada
  campo pesquisável da classe alvo de uma relação `[r] pessoa`, os geradores `repository` e `resource` criam um parâmetro
  de filtro `pessoaCnpj`, `pessoaCpf`, `pessoaRazaoSocial`; um atributo da mesma classe com esse nome gera "variable is
  already defined" e o Java não compila. O `struct` e o `generate` NÃO pegam isso: só o `javac`. Por isso o retrato da pessoa
  na Nota se chama `cnpjPessoa`, `cpfPessoa`, `razaoSocialPessoa` (o sufixo evita o prefixo da relação), e a relação da
  Nota com a Pessoa é `[r] pessoa` (antes `tomador`). **Confirmado: `mvn compile` e `mvn package` passam (2026-11-20).**
- Nenhum agente emite nota, exceto para testar, em homologação.
