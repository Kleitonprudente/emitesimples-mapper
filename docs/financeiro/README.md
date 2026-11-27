# Módulo Financeiro do Emite Simples

Contas a receber, contas a pagar, caixa/bancos, extrato e cobrança.

| Documento | Papel |
|---|---|
| `modulo-financeiro-referencia.md` | Especificação de referência (cópia fiel de `C:\Projetos\referencias`). Fonte das regras de negócio. |
| `fases-modulo-financeiro.md` | Roteiro das 7 fases (cópia fiel). Uma fase por vez. |
| **este arquivo** | **Adaptações ao Emite Simples e histórias por fase. Onde divergir da referência, vale ESTE arquivo.** |

Fluxo de trabalho: o da skill `metodologia-mapper` (documentação → mapa → gerador → geração → código à mão → testes).

---

## 1. Adaptações à referência (decididas pelo dono do projeto em 2026-11-26)

| # | Referência | Emite Simples | Motivo |
|---|---|---|---|
| A1 | Dinheiro em centavos `long`/BIGINT | `ValorMonetario(15,2)` → `BigDecimal` / `numeric(15,2)`. Teto continua R$ 999.999.999,99. Arredondamento HALF_EVEN, uma vez só, no fim do cálculo; escala 2. | Igual à Nota e ao Serviço e aos geradores. |
| A2 | Enum gravado em minúsculas (`receber`) | MAIÚSCULAS (`RECEBER`, `ABERTO`), formato da seção 9.7 do guia de modelagem, rótulo = `title/pt`. | Convenção do projeto desde a V10. |
| A3 | ID ULID CHAR(26) | UUID `varchar(36)` gerado (padrão do gerador). | Padrão do projeto. |
| A4 | `tenant_id` | Relação `[r] empresa: Empresa()` com `tenantKey` (coluna `EMPRESA_ID`), RLS da V3 e filtro do repositório gerado. | Padrão do projeto. |
| A5 | Spring, `@TransactionalEventListener` | Quarkus/CDI: `@Observes(during = TransactionPhase.AFTER_SUCCESS)` (Fase 5). | Stack do projeto. |
| A6 | ~25 permissões finas (`financeiro.titulo.baixar`...) | 3 papéis: **ADMINISTRADOR** = "Dono" (tudo); **OPERADOR** = "Operador" da §10 (ver, criar/editar títulos, baixar, ver extrato); **CONSULTA** = só leitura. Regras por caminho no `application.properties`. | Papéis do projeto (`security/Papeis`). |
| A7 | "Senha recente" em estorno e edição de conta | Ainda não existe no projeto. Por ora: só ADMINISTRADOR e confirmação com `AlertDialog` na tela. Senha recente fica pendente (decisão futura). | Não há reautenticação no projeto. |
| A8 | `pessoa_id` sem FK | Mantido: `[d] idPessoa: Texto(36)` (CN-REL-002), validado no serviço (existe e tem o papel cliente/fornecedor). | O histórico sobrevive a mudanças no cadastro. |
| A9 | Regras na camada de serviço | **Título e Movimento** não têm CRUD gerado (entram no `SEM_RESOURCE` do `back/api/scripts/gerar.sh`); leitura e ações em recurso escrito à mão. **Cadastros de apoio** (Conta, Categoria, Condição, Configuração, Banco) usam o CRUD gerado. | Decisão do dono. |
| A10 | Regras que olham outras linhas | **Trigger plpgsql na migração** (desmarcar conta padrão anterior, hierarquia da categoria, categoria padrão folha, "receber na emissão" exige conta padrão). Regras de uma linha: CHECK no banco e listener JPA (`META-INF/orm.xml`, padrão do `ServicoTributacaoListener`). Erros viram mensagem em português no `ErrosHandler` pelo nome da restrição, com o campo: nome duplicado (índice único) = **409 `conflito`**; demais regras = **400 `requisicao_invalida`**. | Decisão do dono: vale para qualquer caminho de gravação. |
| A11 | Pacotes `financeiro/dominio, aplicacao...` | Gerado: `br.com.emitesimples.financeiro` (contexto `financeiro` do mapa). Escrito à mão: `br.com.emitesimples.financeiroop`. | Mesmo padrão de `nfse` / `nfseemissao`. |
| A12 | Fuso único | `America/Sao_Paulo` para "hoje", "data futura" e "dias de atraso". | §2.5 da referência. |

Nomes no mapa (camelCase) e colunas (MAIÚSCULAS snake_case, do `cn`): `saldo_inicial_centavos` vira `saldoInicial` / `SALDO_INICIAL`; o sufixo `_centavos` some em todo o módulo (A1).

---

## 2. Fase 1: base e cadastros (§2, §3, §4 da referência)

### Entidades (contexto `financeiro`, `back/mapas/Financeiro/`)

| Entidade | Tabela | Tenant | CRUD gerado | Escrita |
|---|---|---|---|---|
| `ContaFinanceira` | `CONTAFINANCEIRA` | empresa | sim | POST/PUT/PATCH só ADMINISTRADOR |
| `CategoriaFinanceira` | `CATEGORIAFINANCEIRA` | empresa | sim | POST/PUT/PATCH só ADMINISTRADOR |
| `CondicaoPagamento` | `CONDICAOPAGAMENTO` | empresa | sim | POST/PUT/PATCH só ADMINISTRADOR |
| `ConfiguracaoFinanceira` | `CONFIGURACAOFINANCEIRA` | empresa (uma linha) | sim | PUT/PATCH só ADMINISTRADOR; **POST negado** (a linha nasce em `garantir`) |
| `Banco` | `BANCO` | **global** (sem RLS) | sim | POST/PUT/PATCH só o administrador da plataforma |

DELETE é negado em todas: nada se apaga, só se desativa (`ativo`). Leitura: qualquer papel da empresa.

Enumerações (MAIÚSCULAS): `Forma` = DINHEIRO, PIX, BOLETO, CARTAO, TRANSFERENCIA; `TipoDeConta` = CAIXA, BANCO, CARTEIRA;
`TipoDeCategoria` = RECEITA, DESPESA. As demais (§3) entram nas fases em que são usadas.

### Histórias

**F1-H1 Conta financeira.** Como administrador, cadastro onde o dinheiro está (caixa, banco, carteira).
- Nome obrigatório e único na empresa: "Já existe uma conta com este nome."
- Código do banco (opcional) precisa existir no cadastro de bancos: "Banco não encontrado. Confira o código do banco."
- Agência só com letras, números e hífen: "A agência só aceita letras, números e hífen."
- Saldo inicial pode ser negativo; dentro do teto. Data do saldo inicial obrigatória e **não futura**: "A data do saldo inicial não pode ser futura."
- Padrão por forma (`padraoDinheiro`, `padraoPix`, `padraoBoleto`, `padraoCartao`, `padraoTransferencia`): no máximo uma conta por forma;
  ao marcar, a anterior é desmarcada na mesma transação (trigger).
- Conta com movimentos: saldo inicial e data travados (o ponto fica preparado; a trava entra na Fase 3, junto do livro).

**F1-H2 Categoria financeira.** Como administrador, classifico receitas e despesas, com pai e filha (um nível só).
- Nome único por empresa e tipo: "Já existe uma categoria com este nome neste tipo."
- O pai é da mesma empresa, do mesmo tipo, ativo e de topo; ninguém é pai de si mesmo; quem tem filhas não vira filha nem muda de tipo.
- Só vira pai a categoria fora de uso (Fase 1: não é categoria padrão da configuração; Fases 2/3 acrescentam título e movimento).
- Inativar pai com filha ativa: "Inative as filhas antes."; ativar filha sob pai inativo: "Ative a categoria pai antes."
- Dicas: até 500 caracteres ("As dicas têm no máximo 500 caracteres.", conferido na `CategoriaFinanceiraListener`); `<` e `>` são
  removidos ao gravar. A tela avisa para não escrever dado pessoal.
- **Divergência conhecida (pendente, decisão do dono do projeto, 2026-11-27):** o domínio gerado declara `@Size` e `@NotNull`, mas os
  resources gerados não usam `@Valid`, então a API não aplica essas anotações (vale para todo CRUD gerado). Só `@Valid` no gerador
  quebraria os cadastros, porque o domínio marca `@NotNull` campos que o servidor preenche (`empresa`, datas de controle). Por ora,
  o limite das dicas é conferido à mão; a correção no gerador (`quarkus-resource.mi` + `quarkus-domain.mi`) fica para depois.
- Nome exibido "Pai › Filha" (tela, Fase 4).

**F1-H3 Condição de pagamento.** Como administrador, cadastro modelos de parcelamento.
- Nome único na empresa. Parcelas 1 a 48; intervalo 0 a 365 dias; dias até a 1ª parcela 0 a 365.
- "Receber na emissão" só com 1 parcela e 0 dias: "Receber na emissão só vale para 1 parcela à vista (0 dias)."; e exige conta padrão ativa
  da forma: "Marque uma conta como padrão desta forma antes de receber na emissão."

**F1-H4 Configuração financeira.** Uma linha por empresa, criada pelos padrões iniciais; o administrador edita.
- Multa e juros 0 a 999,99; carência 0 a 365; lembrete antes 0 a 30; lembretes depois: até 5 números de 1 ou 2 dígitos separados por vírgula.
- Categoria padrão de receita/despesa: ativa, do tipo certo e sem filhas.
- Os padrões só pré-preenchem o título novo (Fase 2).

**F1-H5 Bancos.** Tabela global dos bancos do Banco Central (código sem zeros à esquerda, único). Mantida pelo administrador da plataforma.
A importação do CSV público do Banco Central (que nunca apaga nem desmarca banco) fica **pendente**: não está no escopo pedido da Fase 1.

**F1-H6 Padrões iniciais.** `POST /api/financeiro/padroes` (ADMINISTRADOR ou OPERADOR; idempotente; serviço `PadroesDoFinanceiro.garantir()`):
cria a configuração (multa 2%, juros 1% a.m., carência 2 dias, lembrete 3 dias antes, depois "1,7,15") se não existe, travando-a, e cada grupo
**só se a empresa não tem nenhum registro dele**:
- Conta "Caixa" (CAIXA, saldo 0, abertura hoje, padrão de dinheiro);
- Receitas: Prestação de serviços, Outras receitas. Despesas: Aluguel, Tarifas, Impostos, Folha de pagamento, Fornecedores, Outras despesas;
- Condições: Dinheiro à vista (receber na emissão, só se houver conta padrão de dinheiro), Pix à vista, Boleto 1x (30 dias), Boleto 2x (30/60),
  Boleto 3x (30/60/90).
Receitas e despesas contam como grupos separados (por tipo). Marca `padroesCriadosEm`. Responde `{ configuracaoId, criados: [...] }`
(lista vazia na segunda chamada).

**F1-H7 Utilitário de dinheiro** (`financeiroop.Dinheiro`): texto "1.234,56" ↔ `BigDecimal`, formatação em reais, rateio com resíduo na última
parcela, porcentagem e porcentagem proporcional (HALF_EVEN, escala 2), teto R$ 999.999.999,99.

### Fora da Fase 1
Títulos, movimentos, telas, eventos da nota e cobrança (Fases 2 a 7). Importação do CSV de bancos. Senha recente (A7).

---

## 3. Fase 2: títulos a receber e a pagar (§5 e §10 da referência)

### Decisões da Fase 2 (dono do projeto, 2026-11-26)

| # | Referência | Emite Simples |
|---|---|---|
| B1 | "3 títulos ligados pelo mesmo lançamento", sem coluna que os ligue | Acréscimo: `idLancamento: Texto(36)`, o mesmo UUID em todas as parcelas (ou recorrências) de um lançamento manual. |
| B2 | Permissões da §10 | Ver: qualquer papel da empresa. Criar e editar: ADMINISTRADOR e OPERADOR. **Cancelar e aprovar: só ADMINISTRADOR.** |
| B3 | Título criado por agente/integração nasce sem aprovação | O back ainda não autentica agente (todo chamador é usuário humano): título lançado pela API nasce **aprovado**. `aprovar` existe para os títulos que nascerem sem aprovação (integração futura). O limite de 20 rascunhos pendentes entra junto da autenticação de agente. |
| B4 | Sem CRUD direto | `Titulo` entra no `SEM_RESOURCE` (A9); os endpoints são escritos à mão em `financeiroop`, sob `/api/financeiro/titulos`. **Divergência conhecida (aceita):** o gerador de OpenAPI (`openapi-financeiro.yaml`) documenta o CRUD de `Titulo`, que não existe, como já faz com TokenRenovacao/ConfirmacaoCadastro; os endpoints reais estão no OpenAPI de execução (`/q/openapi`). |
| B5 | `aprovado_por`, `criado_por` | `idAprovadoPor`, `idCriadoPor`: id bruto do usuário (CN-REL-002, referência de auditoria), sem relação com o contexto usuarios. |
| B6 | Cobrança viva no gateway bloqueia edição e é cancelada com o título | Fase 7. |

### Entidade `Titulo` (tabela `TITULO`)
Uma linha por parcela. `saldo = valor − quitado` e `vencido = situação ABERTO/PARCIAL e vencimento < hoje` são calculados, nunca gravados.
`quitado` só muda pelo livro (Fase 3). Enumerações: `TipoDeTitulo` = RECEBER, PAGAR; `SituacaoDoTitulo` = ABERTO, PARCIAL, PAGO, CANCELADO;
`OrigemDoTitulo` = MANUAL, NFSE; `formaPrevista` = `Forma`.

### Histórias

**F2-H1 Lançar título** (`POST /api/financeiro/titulos`, ADMINISTRADOR ou OPERADOR). Entrada da §5.1 (`tipo`, `idPessoa`, `descricao`,
`observacao`, `valor` = valor TOTAL, `vencimento` da 1ª parcela, `idCategoria`, `formaPrevista`, `parcelas` 1–48 padrão 1, `intervaloDias`
0–365 padrão 30, `repetirMeses` 1–24, `multaPercentual`, `jurosMensalPercentual`). Regras:
- Parcelado e recorrente são excludentes: "Escolha parcelado ou recorrente, não os dois."
- Parcelado: rateio com o resíduo na última parcela (100,00 em 3x = 33,33/33,33/33,34); vencimentos a cada `intervaloDias`.
- Recorrente: `repetirMeses` títulos de mesmo valor, no mesmo dia dos meses seguintes contado da 1ª data (31/01 → 28/02 → 31/03).
- Pessoa (opcional): existe na empresa e tem o papel certo: "Esta pessoa não está cadastrada como cliente." / "... como fornecedor."
- Categoria (opcional): ativa, do tipo certo (receber → RECEITA, pagar → DESPESA) e sem filhas.
- Tudo numa transação; `totalParcelas` = quantidade gerada; `origemTipo` = MANUAL; mesmo `idLancamento`; nasce aprovado (B3). Responde os títulos.

**F2-H2 Sugestões do título novo** (`GET /api/financeiro/titulos/sugestoes?tipo=`): os padrões da configuração (§4.4), só como valor
inicial: a receber, vencimento = hoje + `vencimentoPadraoDias`, observação e forma prevista padrão; categoria padrão do tipo.

**F2-H3 Editar título** (`PUT /api/financeiro/titulos/{id}`, ADMINISTRADOR ou OPERADOR; substitui os campos editáveis). Só ABERTO ou PARCIAL
("Título Pago: não pode ser editado."). Editáveis: descrição, observação, vencimento, categoria, forma prevista, multa e juros próprios.
Valor só em título MANUAL, sem baixa e ABERTO ("O valor só muda em título lançado à mão e sem nenhuma baixa."). Tipo e pessoa não mudam.

**F2-H4 Cancelar título** (`POST /api/financeiro/titulos/{id}/cancelar`, só ADMINISTRADOR, crítico). Já cancelado: "Este título já está
cancelado."; com baixa: "Estorne a baixa antes de cancelar." Marca CANCELADO e `canceladoEm`.

**F2-H5 Aprovar título** (`POST /api/financeiro/titulos/{id}/aprovar`, só ADMINISTRADOR, crítico). Preenche `aprovadoEm` e `idAprovadoPor`.
Já aprovado: "Este título já está aprovado."; cancelado: "Título cancelado não pode ser aprovado."

**F2-H6 Consultar** (qualquer papel). `GET /api/financeiro/titulos?tipo=&filtro=&de=&ate=&idPessoa=&idCategoria=&idLancamento=&pagina=&tamanho=`:
filtros da §7 (`EM_ABERTO`, `PARCIAIS`, `VENCIDOS`, `PAGOS`, `CANCELADOS`, `PARA_REVISAO`), período pelo vencimento, pessoa, categoria
(a categoria pai inclui as filhas), lançamento; ordem por vencimento e parcela; 50 por página (máx. 100); responde os itens com `saldo` e
`vencido`, o total de linhas e os totais do filtro (valor, quitado, saldo). `GET /api/financeiro/titulos/{id}`: o título.
`GET /api/financeiro/titulos/resumo`: por tipo, o saldo em aberto, o saldo vencido e a quantidade de vencidos.

**F2-H7 Categoria em uso.** A partir desta fase, categoria com título não vira pai (trigger da V14 estendida na V15).

**Respostas de erro dos títulos:** 400 `requisicao_invalida` com o campo (entrada inválida: pessoa sem o papel, categoria errada, valor
fora da faixa, parcelado e recorrente juntos); **409 `conflito`** quando a situação do título não permite (editar pago/cancelado, cancelar
cancelado ou com baixa, aprovar aprovado ou cancelado); 404 `nao_encontrado` para título de outra empresa (nunca 403, como o CRUD gerado).

---

## 4. Fase 3: livro de movimentos, baixa, estorno e extrato (§6 e §7 da referência)

### Decisões da Fase 3 (dono do projeto, 2026-11-26)

| # | Referência | Emite Simples |
|---|---|---|
| C1 | Fluxo de caixa previsto x realizado, sem agrupamento | **Por mês** dentro do período: entradas e saídas previstas (títulos por vencimento, saldo em aberto) e realizadas (livro sem transferências). |
| C2 | Baixa em lote "mesma regra", sem falar de acréscimos | Cada título quita o **saldo inteiro**; a receber recebe a **multa e os juros sugeridos** (§6.2); a pagar, sem acréscimo. Mesmo `idLote`. |
| C3 | Imutável no banco **e** entidade sem setters/@Immutable | **Só no banco**: trigger recusa UPDATE e DELETE em MOVIMENTO. O gerador de entidade não conhece `@Immutable` (divergência registrada, aceita). `Movimento` sem CRUD gerado (`SEM_RESOURCE`); só o `LivroDeMovimentos` grava. |
| C4 | Papéis da §10 | Baixar e baixar em lote: ADMINISTRADOR e OPERADOR. **Estornar, lançar avulso e transferir: só ADMINISTRADOR.** Extrato, saldos e fluxo: qualquer papel da empresa. |
| C5 | Estorno pede senha recente | Pendente (A7): só ADMINISTRADOR e confirmação na tela. |
| C6 | FK composta (tenant, conta), (tenant, título) | FK composta `(EMPRESA_ID, CONTA_ID)` e `(EMPRESA_ID, TITULO_ID)`, com índice único `(EMPRESA_ID, ID)` nas tabelas apontadas. |
| C7 | `lote_id`, `transferencia_id`, `estorno_de_id`, `criado_por` | `idLote`, `idTransferencia`, `idEstornoDe`, `idCriadoPor`: ids brutos (CN-REL-002). |

### Entidade `Movimento` (tabela `MOVIMENTO`)
O dinheiro de fato entrando ou saindo de uma conta numa data. `valor > 0`; o `sentido` dá o sinal. Linha de baixa fecha a conta:
`valor = quitado + juros + multa − desconto`; linha sem título tem as quatro colunas zeradas. Enumerações: `TipoDeMovimento` = RECEBIMENTO,
PAGAMENTO, ENTRADA_AVULSA, SAIDA_AVULSA, TRANSFERENCIA_ENTRADA, TRANSFERENCIA_SAIDA, ESTORNO; `SentidoDoMovimento` = ENTRADA, SAIDA.
Saldo da conta = saldo inicial + Σ(valor × sinal) até a data; nunca gravado.

### Histórias

**F3-H1 Baixar título** (`POST /api/financeiro/titulos/{id}/baixar`; ADMINISTRADOR, OPERADOR). Entrada: `idConta`, `data`, `forma`,
`quitado` (≥ 0,01), `juros`, `multa`, `desconto` (≥ 0), `observacao` (≤ 255). Trava o título; recusa: sem aprovação ("Este título está aguardando
aprovação: lance-o antes de dar baixa."), situação sem baixa ("Este título está Pago: não aceita baixa."), quitado acima do saldo ("O valor
quitado não pode passar do saldo do título. Juros e multa vão nos campos próprios."), desconto que zera a baixa ("Para abrir mão do título,
cancele-o."), conta inativa ("A conta X está inativa."), data futura ("A data não pode ser futura.") ou antes da abertura da conta ("A data não
pode ser antes da abertura da conta (dd/mm/aaaa)."). Grava RECEBIMENTO (entrada) ou PAGAMENTO (saída); `quitado` do título soma; PAGO se
quitou tudo, senão PARCIAL.

**F3-H2 Sugestão de juros e multa** (`GET /api/financeiro/titulos/{id}/sugestao-baixa?data=&quitado=`). Só a receber (a pagar devolve zero):
atraso em dias corridos; até a carência, zero; depois, multa = quitado × multa% e juros = quitado × juros% × dias / 30 (simples, sem a
multa), percentuais do título ou da configuração, HALF_EVEN uma vez. Exemplo: 1.000,00, 10 dias, multa 2%, juros 1% → 20,00 + 3,33 = 1.023,33.

**F3-H3 Baixa em lote** (`POST /api/financeiro/titulos/baixar-em-lote`; ADMINISTRADOR, OPERADOR). `idsTitulos` (1 a 100), `idConta`, `data`,
`forma`: cada título quita o saldo inteiro (C2), tudo numa transação, mesmo `idLote`; um título recusado recusa o lote, dizendo qual.

**F3-H4 Estornar movimento** (`POST /api/financeiro/movimentos/{id}/estornar`; só ADMINISTRADOR). Estorno não se estorna ("Um estorno não
se estorna: lance a operação de novo."); uma linha só se estorna uma vez ("Este movimento já foi estornado."), com `pg_advisory_xact_lock`
contra estornos simultâneos. Nova linha ESTORNO, sentido inverso, mesmos valores, `idEstornoDe`, descrição "Estorno: …", data de hoje (ou a
abertura da conta, se hoje for antes). Com título: `quitado` volta, situação ABERTO ou PARCIAL (se não cancelado). Transferência estorna as
duas pernas.

**F3-H5 Lançamento avulso** (`POST /api/financeiro/movimentos/avulso`; só ADMINISTRADOR). `idConta`, `sentido`, `valor`, `data`, `forma?`,
`idCategoria?` (folha ativa: receita na entrada, despesa na saída), `descricao?`.

**F3-H6 Transferência** (`POST /api/financeiro/movimentos/transferencia`; só ADMINISTRADOR). `idContaOrigem` ≠ `idContaDestino` ("A conta de
destino precisa ser diferente da de origem."), `valor`, `data`, `descricao?`: duas linhas com o mesmo `idTransferencia`.

**F3-H7 Extrato, saldos e fluxo** (qualquer papel). `GET /api/financeiro/extrato?idConta=&de=&ate=&pagina=`: 15 por página, saldo anterior,
linhas com **saldo corrente** (`SUM() OVER` no banco) e totais por forma no período (transferências numa linha própria; sem forma em "Sem forma
informada"). `GET /api/financeiro/saldos`: saldo atual de cada conta ativa e o total. `GET /api/financeiro/fluxo-de-caixa?de=&ate=`: por mês (C1).

**F3-H8 Travas.** Conta com movimentos: saldo inicial e data de abertura travados ("A conta já tem movimentos: o saldo inicial e a data de
abertura não mudam."). Categoria com movimento não vira pai. MOVIMENTO recusa UPDATE e DELETE no banco.

### Detalhes de implementação da Fase 3 (lacunas da referência, decididos na implementação)
- **Descrição da linha de baixa:** a `observacao` informada na baixa; sem ela, a descrição do título.
- **Extrato:** ordem por data, depois pela hora de gravação (o UUID não é cronológico como o ULID da referência). Sem `de`, desde a abertura;
  sem `ate`, até hoje. O estorno de uma transferência conta na linha de transferências dos totais por forma.
- **Fluxo de caixa (C1):** *previsto* = valor dos títulos não cancelados pelo mês de vencimento (a receber = entrada, a pagar = saída);
  *realizado* = livro sem transferências pelo mês da data (estorno entra pelo seu sentido). Período de até 24 meses.
- **Transferência sem descrição:** "Transferência para <destino>" / "Transferência de <origem>".

---

## 5. Fase 4: telas (§11 da referência)

### Decisão D1 (dono do projeto, 2026-11-26): mapa primeiro
As telas seguem a metodologia Mapper: nascem do **mapa de janela** (`front/mapas/window/financeiro/...`) e dos **geradores do front**;
nenhuma tela é escrita à mão nesta etapa. Só o que o gerador já sabe fazer (cadastro = `window.list` + `window.editor` sobre o CRUD
gerado) entra agora. O que não é CRUD fica **pendente (@TODO)**, para decidir depois se o gerador é estendido ou se a tela é escrita à mão
(com registro em `TELAS_A_MAO`).

### Telas geradas agora (menu "Financeiro"; Bancos no menu "Administração")

| Tela | Rota | Janelas | Papel mínimo |
|---|---|---|---|
| Contas | `/financeiro/contas`, `/financeiro/contas/$id` | `GestaoContasFinanceiras`, `FormularioContaFinanceira` | ADMINISTRADOR (A6: só ele escreve) |
| Categorias | `/financeiro/categorias`, `/financeiro/categorias/$id` | `GestaoCategoriasFinanceiras`, `FormularioCategoriaFinanceira` | ADMINISTRADOR |
| Condições de pagamento | `/financeiro/condicoes`, `/financeiro/condicoes/$id` | `GestaoCondicoesPagamento`, `FormularioCondicaoPagamento` | ADMINISTRADOR |
| Bancos | `/bancos`, `/bancos/$id` | `GestaoBancos`, `FormularioBanco` | `platformOnly` |

Runtime escrito à mão (permitido pelo guia): ícones `wallet`, `tags`, `calendar-clock` e `landmark` em `runtime/icons.tsx`; fonte de FK
`CategoriaFinanceira` (só ativas) em `lib/fk-sources.ts`. O domínio de `Titulo`, `Movimento` e `ConfiguracaoFinanceira` **não** é gerado
no front agora: os dois primeiros não têm CRUD (o apiClient gerado chamaria endpoints que não existem) e a configuração só entra com a tela.

### Pendente (@TODO): o gerador do front não cobre
- **Configuração do financeiro**: registro único por empresa (sem `$id`).
- **Contas a receber / a pagar**: lista sobre `GET /financeiro/titulos` (filtros, totais, seleção múltipla), formulário parcelado ou
  recorrente, detalhe com baixas e estorno, modal de baixa (juros e multa sugeridos, total ao vivo) e baixa em lote.
- **Extrato** (saldo corrente, totais por forma, lançamento avulso e transferência), **saldos** e **fluxo de caixa**.
- **Saldo atual na lista de contas** (campo calculado) e **seleção de categoria agrupada por pai** (só filhas selecionáveis).
- **Importar/exportar CSV** de contas, categorias e condições; importação do CSV de bancos do Banco Central.

### Decisão D2 (dono do projeto, 2026-11-26): pendências da Fase 4 pela metodologia
- **Configuração do financeiro**: o gerador ganha o **editor singleton** (`window.editor` com `singleton`, rota sem `$id`); a tela sai
  inteira do gerador. Sem configuração (empresa que nunca abriu o módulo), a tela diz para abrir Contas a receber, que cria os padrões.
- **Contas a receber, Contas a pagar, Título (novo e detalhe) e Extrato**: declarados no mapa como **`window.custom`** (rota, menu e
  papel no mapa; o gerador escreve a rota e o manifesto) e o componente é **escrito à mão** em `features/financeiro/`, registrado em
  `TELAS_A_MAO` por esta decisão, só com componentes do catálogo e o runtime. A API dessas telas fica em `features/financeiro/financeiroApi.ts`.
- Vocabulário novo documentado em `front/mapas/README.md` ("Editor singleton" e "window.custom").

### Acréscimos do back pedidos pelas telas (§11; 2026-11-26)
- `GET /api/financeiro/titulos?busca=`: busca na descrição do título (sem acento e sem caixa), junto com os demais filtros.
- `TituloDto.nomePessoa`: nome da pessoa do título, lido do cadastro na consulta (o título guarda só `idPessoa`, A8); `null` se a
  pessoa não existe mais.
- `GET /api/financeiro/titulos/{id}/baixas` (qualquer papel): as linhas do livro do título (baixas e estornos), em ordem de gravação,
  cada uma com `estornado` (já tem estorno), para o detalhe mostrar e estornar.

---

## 6. Fase 5: títulos gerados pela NFS-e (§8 da referência)

### Decisões da Fase 5 (E1 e E2: dono do projeto, 2026-11-26; E3 segue a referência §8; E4: proposta da implementação, pelo precedente da V4, a confirmar)

| # | Referência | Emite Simples |
|---|---|---|
| E1 | Condição de pagamento do documento (ou à vista sem condição) | A NFS-e **não tem condição de pagamento** (nem no mapa da Nota nem na configuração): toda nota autorizada gera **1 título a receber à vista**, vencimento na data de emissão. Condição na nota e "receber na emissão" ficam para depois (exigem mudar o mapa da Nota). |
| E2 | `valorCentavos` do documento | **Valor líquido** da nota, a mesma conta da tributação (`TributacaoNota`): valor dos serviços − desconto incondicionado − ISS retido (quando retido) − federais retidos (PIS, COFINS, INSS, IR, CSLL). Líquido zero não gera título. |
| E3 | Eventos e listener `AFTER_COMMIT` | O módulo da nota (`nfseemissao`) **publica** `DocumentoAutorizado` e `DocumentoCancelado` (CDI) no ponto único em que a nota vira AUTORIZADA ou CANCELADA (`NfseRetorno`) e **não conhece** o financeiro. O financeiro (`financeiroop.TitulosDosDocumentos`) observa com `@Observes(during = AFTER_SUCCESS)`: só depois do commit da nota, numa transação própria, como a empresa da nota. Falha no financeiro nunca desfaz nem trava a nota (fica no log). |
| E4 | Tabela `documentos_cancelados` | Tabela técnica `DOCUMENTOCANCELADO` (empresa, origem, documento, quando), escrita à mão na V17 e **fora do mapa** (como o `segredo` da V4): não é entidade de negócio, só a marca que torna a ordem dos eventos indiferente. |

### Histórias
**F5-H1 Nota autorizada gera o título.** Idempotente: sob `pg_advisory_xact_lock` por documento, não gera se o documento já foi cancelado
nem se já há título dele (e o índice único `(empresa, origem, documento, parcela)` da V15 é a rede). Título: a receber, pessoa da nota,
descrição "NFS-e nº <número>", valor líquido (E2), vencimento = emissão, categoria padrão de receita da configuração (ou "Prestação de
serviços", se existir como folha ativa), observação padrão a receber, origem NFSE com o id e o número da nota, nasce aprovado.

**F5-H2 Nota cancelada.** Grava a marca de cancelamento (sob o mesmo lock); título sem baixa é **cancelado**; título com baixa fica
**para revisão** (alguém decide entre estornar ou registrar devolução). Cancelamento que chega antes da geração impede o título.

---

## 7. Fases 6 e 7: adiadas (decisão do dono do projeto, 2026-11-26)
- **Fase 6 (cobrança por e-mail e lembretes):** o projeto ainda não tem serviço de e-mail (SMTP/provedor, remetente, credenciais) e a
  Pessoa não tem "não enviar cobrança". Volta quando o serviço de e-mail for escolhido.
- **Fase 7 (cobrança por gateway: boleto, Pix, link):** sem provedor escolhido, sem documentação da API e sem ambiente de teste. Volta
  com esses três; as credenciais nunca vão no código nem em entidade.

---

## 8. Complementos da Fase 4 (decisão do dono do projeto, 2026-11-26)

**F4-C1 Categoria agrupada por pai.** Evolução do runtime: a opção de FK ganha `group` (opcional) e o `Combobox` mostra os grupos.
A fonte de categoria do título e do lançamento avulso (`CategoriaReceita`, `CategoriaDespesa`) devolve **só as que aceitam lançamento**
(filhas e categorias de topo sem filhas), agrupadas pelo nome do pai; o pai aparece só como título do grupo e não se escolhe.

**F4-C2 Saldo atual na lista de contas.** Extensão do gerador de colunas: coluna **calculada** declarada no mapa da janela
(`[e] computed`, ver `front/mapas/README.md`), desenhada pelo componente de runtime `ComputedCell` a partir de uma fonte registrada em
`lib/computed-sources.ts`. A fonte `saldoDaConta` lê `GET /financeiro/saldos` (conta inativa: "—").

**F4-C3 Importar e exportar CSV** de contas, categorias e condições de pagamento (§10, §11.7).
- `GET /api/financeiro/csv/{contas|categorias|condicoes}`: exporta (qualquer papel). `POST` do mesmo caminho, com o arquivo em `multipart` (campo `arquivo`, como o certificado): importa
  (**só ADMINISTRADOR**, crítico).
- Formato: UTF-8, separador `;`, primeira linha com o cabeçalho abaixo; dinheiro "1.234,56"; data "dd/mm/aaaa"; sim/não; enums pelo valor
  (`BANCO`, `RECEITA`, `PIX`...).
  - contas: `nome;tipo;codigo_banco;agencia;dados_do_banco;saldo_inicial;data_saldo_inicial;ativo`
  - categorias: `nome;tipo;categoria_pai;dicas;ativo` (o pai pelo nome, do mesmo tipo; pode estar no próprio arquivo)
  - condicoes: `nome;forma;parcelas;dias_primeiro_vencimento;intervalo_dias;receber_na_emissao;ativo`
- Importar **nunca apaga nem altera**: só cria. Nome que já existe é **ignorado** (vai no relatório). **Tudo ou nada**: um erro em
  qualquer linha recusa o arquivo inteiro com "linha N: ..." (400), sem gravar nada. Até 1.000 linhas e 1 MB.
  Resposta: `{ criados, ignorados: [{ linha, nome, motivo }] }`.
- Tela: `window.custom` **Importar e exportar** (`/financeiro/csv`), escrita à mão (TELAS_A_MAO, por esta decisão), e ação `link` nas
  listas de Contas, Categorias e Condições.

**F4-C4 Carga dos bancos pelo CSV público do Banco Central** (§4.5; pendência F1-H5). `POST /api/financeiro/csv/bancos` (só o administrador
da plataforma), com o arquivo de participantes (multipart, campo `arquivo`) do Banco Central (cabeçalho com ISPB, nome reduzido, número do código, participa da
COMPE e nome extenso; separador vírgula ou ponto e vírgula; as colunas são achadas pelo nome no cabeçalho, sem acento e sem caixa).
Linha sem código numérico (participante sem COMPE) é ignorada; o código perde os zeros à esquerda (001 -> 1). **Cria ou atualiza pelo
código** (ISPB, nome, nome extenso, participa da COMPE) e **nunca apaga nem desmarca**: `integradoGateway` e o link de passo a passo não
mudam. Resposta: `{ criados, atualizados, ignorados }`. Na tela Importar e exportar, a seção aparece só para o administrador da plataforma,
e a lista de Bancos ganha a ação `link` "Importar CSV do Banco Central".

---

## 9. Pendências executadas (decisões do dono do projeto, 2026-11-27)

Ordem de execução: E1, A7, Fase 6, B3, Fase 7.

### 9.1 E1: condição de pagamento na nota (§8 da referência)
- **Nota** ganha `idCondicaoPagamento: Texto(36)`, opcional: id bruto da `CondicaoPagamento` (CN-REL-002), sem relação de objeto.
  Assim o contexto `nfse` continua sem depender do `financeiro` (E3). Migração `V18__nota_condicao_pagamento.sql`.
- Entra na emissão direta (`POST /api/nfse/notas`), no preparo do assistente (`/preparar`) e no envio/aprovação (`/enviar`,
  `/aprovar`, que ajustam campos). A nota só confere o formato (até 36 caracteres); quem confere a condição é o financeiro.
- `DocumentoAutorizado` ganha `idCondicaoPagamento`. O financeiro (`TitulosDosDocumentos`):
  - **sem condição, ou condição inexistente, de outra empresa ou inativa** → 1 título à vista, vencimento na data da nota (como antes);
  - **com condição** → `parcelas` títulos sobre o **valor líquido** (rateio com resíduo na última), 1º vencimento = data da nota +
    `diasPrimeiroVencimento`, os seguintes a cada `intervaloDias`; `formaPrevista` = forma da condição; mesmo `idLancamento`;
  - **"receber na emissão"** (condição à vista marcada) → já dá a baixa na conta padrão ativa da forma, na data da nota, **só se quem
    emitiu tem papel que baixa** (ADMINISTRADOR ou OPERADOR na empresa). Sem conta padrão ativa, data antes da abertura da conta ou
    sem permissão: o título fica em aberto (e o log diz por quê); a nota nunca é afetada.
- Tela da nota: campo "Condição de pagamento" (opcional, só condições ativas), no bloco de valores. O detalhe da nota mostra a condição.

### 9.2 A7 e C5: senha recente (§10 da referência)
- `POST /api/auth/reautenticar` `{senha}` (qualquer usuário autenticado): confere a senha com as travas de força bruta do login e
  devolve a sessão com um **token de acesso novo**, igual ao atual (empresa e papel relidos do vínculo) e com a claim
  `senha_recente_ate` = agora + 5 minutos. Não cria nem troca o refresh token. Senha errada: 400 `senha_invalida`, no campo `senha`.
- Exigem a senha recente (filtro `security/SenhaRecenteFiltro`): `POST /api/financeiro/movimentos/{id}/estornar` e todo `PUT`/`PATCH`
  em `/api/ContaFinanceira/...` (editar e inativar conta). Sem a claim, ou vencida: **403 `senha_recente_exigida`** ("Confirme sua
  senha para continuar."). O papel segue conferido como antes (só ADMINISTRADOR).
- Front: o `lib/api` reconhece o 403, abre o diálogo `runtime/SenhaRecente` (montado no `__root`), troca o token e repete a chamada
  uma vez. Por 5 minutos não pergunta de novo. Um token renovado pelo refresh volta a não ter a marca.

### 9.3 Fase 6: cobrança por e-mail e lembretes (§9.1 da referência)
- **Serviço de e-mail:** SMTP pelo Quarkus Mailer. Remetente, host, porta, usuário e senha **só por variável de ambiente**
  (`EMITESIMPLES_MAIL_FROM`, `EMITESIMPLES_SMTP_HOST`, `EMITESIMPLES_SMTP_PORT`, `EMITESIMPLES_SMTP_USUARIO`, `EMITESIMPLES_SMTP_SENHA`).
  Em dev e teste o mailer é falso (nada sai).
- **Pessoa** ganha `naoEnviarCobranca` (padrão não; V19), com a caixa "Não enviar cobrança por e-mail" nos formulários de Cliente e
  Pessoa (gerados).
- Entidades (mapa, sem CRUD gerado; V20): `LembreteCobranca` (título, tipo ANTES|DEPOIS, dias; único) e `CobrancaEnviada` (título,
  `idPessoa`, origem MANUAL|LEMBRETE, dia, e-mail; **único por empresa, pessoa e dia**).
- `POST /api/financeiro/titulos/{id}/enviar-cobranca` (**só ADMINISTRADOR**, crítico: mensagem a terceiro). Recusa (409, com código):
  título a pagar, sem aprovação, sem saldo, para revisão, sem cliente, cliente que não quer cobrança, cliente sem e-mail, cliente já
  cobrado hoje. A reserva do dia é gravada antes do e-mail e apagada se ele falhar (502 `email_nao_enviado`). E-mail em texto puro:
  valor (saldo), vencimento e "como pagar" (a cobrança viva do gateway, se houver; senão os dados da conta padrão da forma prevista).
  O e-mail nunca gera boleto nem Pix. `GET .../cobrancas-enviadas` lista as enviadas.
- **Lembretes:** job diário às 9h (São Paulo), só com `lembretesAtivos`; `lembreteDiasAntes` antes do vencimento e cada dia de
  `lembreteDiasDepois` depois; cada aviso uma vez (a marca só é gravada depois do envio); respeita o limite de um por cliente por dia.

### 9.4 B3: agentes e título lançado por agente (§5.4 e §10 da referência)
- **Cliente de acesso, fase 1** (mapa `usuarios/ClienteAcesso`, agora com `[r] usuario`, o dono; V19). O CRUD gerado de
  `ClienteAcesso` foi **retirado** (guardava o hash do segredo e ficava legível): só endpoints à mão.
- `GET|POST /api/agentes` e `POST /api/agentes/{id}/revogar` (**só ADMINISTRADOR** da empresa). Criar devolve o `clientId` e o
  **segredo uma vez só** (guarda só o hash bcrypt); escopos `consultar` e `preparar`; validade de 365 dias. Tela **Administração >
  Agentes** (`window.custom`).
- `POST /api/auth/agente/token` `{clientId, segredo}` (público, com as travas de força bruta): token de acesso curto com o grupo
  **AGENTE**, a empresa, `cliente_acesso` e `escopos`; `sub` = o usuário dono. Dono sem vínculo ativo, empresa inativa, agente revogado
  ou vencido: 401 `credencial_invalida`. Dono só CONSULTA: o agente perde o escopo `preparar`.
- `security/AgenteFiltro`: com token de agente, **GET** só com `consultar` (nunca agentes, auth, usuários, vínculos, clientes de
  acesso); **POST** só em `/api/financeiro/titulos` e `/api/nfse/notas/preparar`, com `preparar`. Todo o resto: 403
  `agente_sem_permissao`. O agente nunca aprova, baixa, estorna, cancela nem envia cobrança.
- Título lançado por agente nasce **sem aprovação** (`aprovadoEm` vazio), com `idCriadoPorCliente`; não aceita baixa, cobrança nem
  lembrete até uma pessoa aprovar. **No máximo 20** rascunhos de agente aguardando aprovação por usuário dono (409 `rascunhos_demais`).
- Nota preparada por agente grava `preparadoPorCliente`; aprovar e descartar seguem só de pessoa.

### 9.5 Fase 7: cobrança por gateway, boleto, Pix e link (§9.2 da referência)
- **Só a porta e o provedor falso** (decisão de 2026-11-27): `GatewayDeCobranca` (porta) e `GatewayFalso` (provedor `FALSO`, para
  testes e homologação interna). O adaptador de um provedor real entra depois, com a documentação e o sandbox dele.
- Entidades (mapa, sem CRUD gerado; V21): `CredencialCobranca` (uma por conta; `sellerId` único no sistema; token e segredo do
  webhook **só como referência ao cofre** SegredoService), `CobrancaDestino` (global, só o hash SHA-256 do segredo) e `Cobranca`
  (uma viva por título: índice parcial em CRIANDO, AGUARDANDO e INCERTA).
- Integração: `GET /api/financeiro/cobranca/credenciais` e `PUT /api/financeiro/cobranca/credenciais/{idConta}` (só ADMINISTRADOR).
  O token nunca volta; a URL do webhook (`/api/webhooks/cobranca/{segredo}`) aparece só quando o segredo é gerado. Tela
  **Financeiro > Integração de cobrança** (`window.custom`).
- `POST /api/financeiro/titulos/{id}/cobrancas` `{tipo, vencimento?}` (só ADMINISTRADOR): título a receber, aprovado, com saldo e
  cliente; boleto e Pix a partir de R$ 30,00; boleto vence de hoje em diante; Pix para hoje; link de 1 a 30 dias. Conta: a padrão da
  forma prevista com integração ativa, ou a única com integração; com duas ou mais e nenhuma padrão, recusa. A chamada ao provedor é
  feita **fora da transação, uma vez só**: sem resposta, `INCERTA`; recusa, `RECUSADA` com a frase nossa pelo código e o texto do
  provedor filtrado (sem CPF/CNPJ, e-mail, telefone, links).
- **Sincronizador** (único que muda o estado depois de criada): botão `POST /api/financeiro/cobrancas/{id}/atualizar`, webhook e
  varredor (a cada 10 min para Pix e link; boleto a cada 12 h). Pago: baixa pelo livro, uma vez só (`idBaixaMovimento`), excedente
  como juros, tarifa como saída avulsa em "Tarifas". Valor menor que o saldo ou título sem saldo: `PAGA_PARA_REVISAR`, sem baixa.
  `POST .../descartar` (só ADMINISTRADOR): só incerta ou recusada.
- **Cobrança automática ao lançar:** título a receber, aprovado, forma prevista boleto ou Pix e integração ativa; depois do commit,
  uma tentativa só. Recorrente e agente (não aprovado) não disparam. Falha nunca derruba o lançamento.
- Cancelar o título ou estornar a baixa **cancela as cobranças vivas** (a tela avisa para cancelar também no portal do provedor).
