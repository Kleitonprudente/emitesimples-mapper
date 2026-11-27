# Módulo Financeiro — especificação de referência (para implementação em Java)

> Documento de referência extraído do módulo Financeiro do Emite Simples (ERP para MEI/ME). Descreve **conceitos, campos, regras e fluxos** de forma independente de linguagem, com sugestões de mapeamento para Java (Spring Boot + JPA + PostgreSQL). Onde houver divergência com uma regra de negócio do novo projeto, vale a decisão do dono do novo projeto.

---

## 1. Visão geral

O módulo cobre **contas a receber, contas a pagar, caixa/bancos e extrato** para pequenas empresas.

Dois conceitos sustentam tudo:

| Conceito | O que é | Analogia |
|---|---|---|
| **Título** | A *promessa* de receber ou pagar um valor numa data (uma parcela = um título). | "Conta a receber / conta a pagar" |
| **Movimento** (livro) | O *dinheiro de fato* entrando ou saindo de uma conta, numa data. | "Linha do extrato" |

Um título só muda de situação quando um movimento de **baixa** é gravado no livro (ou quando é cancelado). O saldo de uma conta é **sempre calculado** a partir do livro (saldo inicial + entradas − saídas); nunca se guarda saldo num campo.

Cadastros de apoio: **Conta financeira** (onde o dinheiro está), **Categoria** (receita/despesa, para relatório), **Condição de pagamento** (como parcelar), **Configuração financeira** (multa, juros, carência e padrões da empresa).

Partes opcionais (podem ser fases posteriores): cobrança por e-mail e lembretes; cobrança por gateway (boleto/Pix/link) com baixa automática; geração automática de títulos a partir da nota fiscal.

---

## 2. Convenções obrigatórias

1. **Dinheiro em centavos inteiros** (`long` em Java, `BIGINT` no banco). Nunca `double`/`float`. Na tela, converter de "1.234,56" para `123456`.
   - Teto: `99_999_999_999` centavos (R$ 999.999.999,99) em todo campo de valor.
2. **Percentuais** (multa, juros) como `NUMERIC(5,2)` no banco e `BigDecimal` em Java (0 a 999,99).
3. **Arredondamento único, NBR 5891** → em Java, `RoundingMode.HALF_EVEN`. Arredonde **uma vez**, no fim do cálculo.
4. **Datas de negócio** (`vencimento`, `data` do movimento, `data_saldo_inicial`) são `LocalDate` / `DATE`, sem hora. Carimbos (`created_at`, `aprovado_em`, `cancelado_em`) são `OffsetDateTime` / `TIMESTAMPTZ`.
5. **Um fuso só** (ex.: `America/Sao_Paulo`) para "hoje", "data futura" e "dias de atraso". Configure o mesmo fuso na JVM/app e na sessão do banco.
6. **IDs**: ULID de 26 caracteres (`CHAR(26)`). Em Java: biblioteca `ulid-creator` (`UlidCreator.getMonotonicUlid().toString()`). UUID também serve, se o projeto já usa.
7. **Multiempresa**: toda tabela de negócio tem `tenant_id`. Toda consulta filtra pela empresa do contexto (no Postgres, preferir RLS; em JPA, no mínimo um `@Filter` do Hibernate ou `tenant_id` em todo repositório). **Nunca confiar em id vindo da tela** sem conferir que pertence à empresa.
8. Toda operação de negócio passa por um **serviço/caso de uso** (não no controller): valida, trava, grava, audita.

---

## 3. Enumerações

```java
public enum Forma {            // COMO o dinheiro entra/sai
    DINHEIRO("dinheiro", "Dinheiro"),
    PIX("pix", "Pix"),
    BOLETO("boleto", "Boleto"),
    CARTAO("cartao", "Cartão"),
    TRANSFERENCIA("transferencia", "Transferência");
}

public enum TipoDeConta { CAIXA("caixa"), BANCO("banco"), CARTEIRA("carteira"); }

public enum TipoDeCategoria { RECEITA("receita"), DESPESA("despesa"); }

public enum TipoDeTitulo { RECEBER("receber", "A receber"), PAGAR("pagar", "A pagar"); }

public enum SituacaoDoTitulo {
    ABERTO, PARCIAL, PAGO, CANCELADO;
    public boolean aceitaBaixa() { return this == ABERTO || this == PARCIAL; }
}

public enum TipoDeMovimento {
    RECEBIMENTO, PAGAMENTO,                       // baixa de título (sempre com titulo_id)
    ENTRADA_AVULSA, SAIDA_AVULSA,                 // dinheiro sem título
    TRANSFERENCIA_ENTRADA, TRANSFERENCIA_SAIDA,   // as duas pernas de uma transferência
    ESTORNO                                       // anula uma linha anterior
}

public enum SentidoDoMovimento {
    ENTRADA, SAIDA;
    public int sinal() { return this == ENTRADA ? 1 : -1; }
    public SentidoDoMovimento inverso() { return this == ENTRADA ? SAIDA : ENTRADA; }
}

public enum OrigemDoTitulo { MANUAL, NFSE /* ou outro documento */ }
```

Grave o **valor em minúsculas** (`"receber"`, `"aberto"`) no banco com `@Enumerated(EnumType.STRING)` + `AttributeConverter`, ou só `EnumType.STRING` se aceitar maiúsculas. Proteja com `CHECK` no banco.

**Cores sugeridas** da situação: Aberto = azul, Parcial = âmbar, Pago = verde, Cancelado = cinza.

---

## 4. Cadastros e campos

### 4.1 Conta financeira (`contas_financeiras`)

Onde o dinheiro está: o caixa da loja, uma conta no banco, uma carteira digital.

| Campo | Tipo (SQL / Java) | Obrig. | Regras e significado |
|---|---|---|---|
| `id` | CHAR(26) / String | sim | ULID |
| `tenant_id` | CHAR(26) / String | sim | empresa |
| `nome` | VARCHAR(100) / String | sim | **único por empresa** ("Já existe uma conta com este nome.") |
| `tipo` | VARCHAR(10) / TipoDeConta | sim | `caixa`, `banco`, `carteira` |
| `codigo_banco` | VARCHAR(4) / String | não | código do Banco Central sem zeros à esquerda; deve existir na tabela `bancos` |
| `agencia` | VARCHAR(10) / String | não | só `[0-9A-Za-z-]` |
| `dados_do_banco` | VARCHAR(150) / String | não | texto livre (nº da conta, chave Pix...) usado no "como pagar" do e-mail de cobrança |
| `saldo_inicial_centavos` | BIGINT / long | sim | pode ser **negativo** (conta no cheque especial); padrão 0 |
| `data_saldo_inicial` | DATE / LocalDate | sim | **não pode ser futura**. Nenhum movimento pode ter data anterior a ela |
| `ativo` | BOOLEAN | sim | inativa não recebe movimento novo ("A conta X está inativa.") |
| `padrao_dinheiro`, `padrao_pix`, `padrao_boleto`, `padrao_cartao`, `padrao_transferencia` | BOOLEAN | sim | marca a **conta padrão de cada forma**; **no máximo uma conta por forma** (índice único parcial `WHERE padrao_x`) |
| `created_at`, `updated_at` | TIMESTAMPTZ | | |

Regras:
- Na tela, "padrão de" é uma lista de formas (`padrao_de: ["dinheiro","pix"]`). Ao marcar uma conta como padrão de uma forma, **desmarcar** a anterior na mesma transação.
- **Conta com movimentos**: saldo inicial e data de abertura ficam **travados** (mudar reescreveria o histórico).
- Alterar conta é operação **crítica** (pede senha recente), pois mexe em dados bancários.
- Saldo atual = `saldo_inicial_centavos + Σ(valor × sinal)` dos movimentos até a data.

### 4.2 Categoria financeira (`categorias_financeiras`)

Classifica receitas e despesas para relatórios.

| Campo | Tipo | Obrig. | Regras |
|---|---|---|---|
| `id`, `tenant_id` | CHAR(26) | sim | |
| `nome` | VARCHAR(100) | sim | **único por (empresa, tipo, nome)** |
| `tipo` | VARCHAR(10) / TipoDeCategoria | sim | `receita` ou `despesa` |
| `categoria_pai_id` | CHAR(26) | não | FK para a própria tabela, `ON DELETE RESTRICT`. Nulo = categoria de topo |
| `dicas` | VARCHAR(500) | não | "para quais contas é usada" (palavras separadas por vírgula, `;` ou quebra de linha). Usada para **sugerir a categoria** pela descrição. Remover `<` e `>`; avisar para não escrever dado pessoal |
| `ativo` | BOOLEAN | sim | |

Regras da hierarquia (**um nível só**, imposto no serviço):
- O pai é da mesma empresa, **do mesmo tipo**, **ativo** e **de topo** (não tem pai).
- Ninguém é pai de si mesmo; quem tem filhas não vira filha nem muda de tipo.
- **Só vira pai a categoria que não está em uso** (sem título, sem movimento, não é categoria padrão da configuração).
- **Título, movimento avulso e categoria padrão só usam categoria SEM filhas** ("folha"). Pai é só agrupador.
- Inativar um pai com filha ativa é bloqueado ("inative as filhas antes"); ativar filha sob pai inativo também.
- Nome exibido: `"Pai › Filha"`.

Sugestão automática pela descrição (opcional): normalize (sem acento, minúsculas), compare **palavras inteiras** da descrição com as dicas (dica com ≥ 3 letras, 1 ponto por dica casada). Vence quem tiver mais pontos; **empate ou zero = sem sugestão**. Nunca sobrescrever uma escolha manual.

### 4.3 Condição de pagamento (`condicoes_de_pagamento`)

Modelo de parcelamento reutilizável (usada principalmente para gerar títulos a partir de uma venda/nota).

| Campo | Tipo | Obrig. | Regras |
|---|---|---|---|
| `id`, `tenant_id` | CHAR(26) | sim | |
| `nome` | VARCHAR(100) | sim | único por empresa |
| `forma` | VARCHAR(20) / Forma | sim | |
| `parcelas` | SMALLINT | sim | 1 a 48 |
| `intervalo_dias` | SMALLINT | sim | 0 a 365 — dias entre uma parcela e a próxima |
| `dias_primeiro_vencimento` | SMALLINT | sim | 0 a 365 — dias da data-base até a 1ª parcela |
| `receber_na_emissao` | BOOLEAN | não | já dá baixa no ato (venda à vista). **Só vale com 1 parcela e 0 dias**; exige uma **conta padrão** para a forma |
| `ativo` | BOOLEAN | sim | |

> Não existe "condição padrão" global: cada módulo que usa condição guarda a sua padrão na própria configuração (ex.: a configuração da nota fiscal tem `condicao_de_pagamento_padrao_id`).

### 4.4 Configuração financeira (`configuracoes_financeiras`) — uma linha por empresa

| Campo | Tipo | Padrão | Regras |
|---|---|---|---|
| `tenant_id` | CHAR(26) PK | | |
| `multa_percentual` | NUMERIC(5,2) | 2,00 | 0 a 999,99 |
| `juros_mensal_percentual` | NUMERIC(5,2) | 1,00 | 0 a 999,99 (juros **simples ao mês**) |
| `carencia_dias` | SMALLINT | 2 | 0 a 365 — até venc.+carência não cobra nada |
| `lembretes_ativos` | BOOLEAN | false | e-mails automáticos de cobrança |
| `lembrete_dias_antes` | SMALLINT | 3 | 0 a 30 |
| `lembrete_dias_depois` | VARCHAR(40) | "1,7,15" | até 5 números de 1–2 dígitos separados por vírgula (`^(\d{1,2}(\s*,\s*\d{1,2}){0,4})?$`) |
| `vencimento_padrao_dias` | SMALLINT | 0 | só a receber; pré-preenche o vencimento (hoje + N) |
| `observacao_padrao_receber` | VARCHAR(255) | null | pré-preenche a observação do título a receber |
| `categoria_padrao_receita_id` | CHAR(26) | null | categoria ativa, do tipo receita, sem filhas |
| `categoria_padrao_despesa_id` | CHAR(26) | null | idem, despesa |
| `forma_prevista_padrao_receber` | VARCHAR(20) | null | uma `Forma`; pré-preenche o título a receber |
| `padroes_criados_em` | TIMESTAMPTZ | null | marca de que os cadastros iniciais já foram criados |

Os "padrões" **só pré-preenchem o formulário** de título novo; a pessoa pode apagar.

### 4.5 Cadastro de bancos (`bancos`) — tabela global, sem `tenant_id`

| Campo | Tipo | Regras |
|---|---|---|
| `codigo` | VARCHAR(4) único | código Banco Central sem zeros à esquerda (ex.: `1`, `341`) |
| `ispb` | CHAR(8) | |
| `nome`, `nome_extenso` | VARCHAR(120/200) | |
| `participa_compe` | BOOLEAN | |
| `integrado_gateway` | BOOLEAN | se o banco aceita cobrança pelo gateway |
| `url_passo_a_passo` | VARCHAR(500) | link https de ajuda para integrar |

Carga pelo CSV público do Banco Central (importação que **nunca apaga** nem desmarca bancos).

### 4.6 Padrões iniciais (criados na 1ª vez que a empresa usa o módulo)

Serviço idempotente (`garantirPadroes()`), com lock na linha de configuração; **cada grupo só é criado se a empresa ainda não tem nenhum registro dele**:

- **Conta:** "Caixa" (tipo caixa, saldo 0, abertura hoje, padrão de dinheiro).
- **Categorias de receita:** Prestação de serviços, Outras receitas.
- **Categorias de despesa:** Aluguel, Tarifas, Impostos, Folha de pagamento, Fornecedores, Outras despesas.
- **Condições:** Dinheiro à vista (receber na emissão), Pix à vista, Boleto 1x (30 dias), Boleto 2x (30/60), Boleto 3x (30/60/90).
- **Configuração:** multa 2%, juros 1% a.m., carência 2 dias.

---

## 5. Título (`titulos`)

Uma linha por parcela. Parcelado em 3x = 3 títulos ligados pelo mesmo lançamento (`parcela` 1..3, `total_parcelas` 3).

| Campo | Tipo | Obrig. | Regras e significado |
|---|---|---|---|
| `id`, `tenant_id` | CHAR(26) | sim | |
| `tipo` | VARCHAR(7) / TipoDeTitulo | sim | `receber` ou `pagar` |
| `pessoa_id` | CHAR(26) | não | cliente (receber) ou fornecedor (pagar). **Sem FK** de propósito, para o histórico sobreviver se o cadastro mudar. Validar: existe e tem o papel certo ("Esta pessoa não está cadastrada como cliente/fornecedor.") |
| `descricao` | VARCHAR(255) | sim | |
| `observacao` | TEXT (máx. 255 na validação) | não | instruções de pagamento; vai impressa no boleto |
| `valor_centavos` | BIGINT | sim | > 0 e ≤ teto |
| `quitado_centavos` | BIGINT | sim | padrão 0; `0 ≤ quitado ≤ valor`. **Só o livro altera** |
| `vencimento` | DATE | sim | |
| `categoria_id` | CHAR(26) | não | ativa, do tipo certo (receber → receita; pagar → despesa), sem filhas |
| `forma_prevista` | VARCHAR(20) / Forma | não | como se espera receber/pagar (informativo; no receber com boleto/Pix pode disparar a cobrança automática, §9) |
| `situacao` | VARCHAR(10) | sim | `aberto` (padrão), `parcial`, `pago`, `cancelado` |
| `parcela` | SMALLINT | sim | ≥ 1 |
| `total_parcelas` | SMALLINT | sim | ≥ `parcela` |
| `origem_tipo` | VARCHAR(10) | sim | `manual` ou o tipo do documento (`nfse`...) |
| `origem_id` | CHAR(26) | não | id do documento que gerou |
| `origem_numero` | VARCHAR(40) | não | número legível do documento (ex.: nº da nota) |
| `multa_percentual` | NUMERIC(5,2) | não | **sobrescreve** a da empresa só para este título |
| `juros_mensal_percentual` | NUMERIC(5,2) | não | idem |
| `para_revisao` | BOOLEAN | sim | documento de origem cancelado com o título já baixado: alguém precisa decidir (estornar ou devolver) |
| `cancelado_em` | TIMESTAMPTZ | não | |
| `aprovado_em`, `aprovado_por` | TIMESTAMPTZ, CHAR(26) | não | título criado por agente de IA/integração nasce **sem aprovação**; não aceita baixa nem entra em lembretes até alguém aprovar |
| `criado_por` | CHAR(26) | não | usuário |
| `created_at`, `updated_at` | TIMESTAMPTZ | | |

Restrições no banco:
```sql
CHECK (tipo IN ('receber','pagar'))
CHECK (situacao IN ('aberto','parcial','pago','cancelado'))
CHECK (valor_centavos > 0 AND quitado_centavos >= 0 AND quitado_centavos <= valor_centavos)
CHECK (parcela >= 1 AND total_parcelas >= parcela)
CREATE UNIQUE INDEX titulos_origem_unica ON titulos (tenant_id, origem_tipo, origem_id, parcela) WHERE origem_id IS NOT NULL;
CREATE INDEX ON titulos (tenant_id, tipo, situacao, vencimento);
CREATE INDEX ON titulos (tenant_id, pessoa_id);
```

Campos calculados (não gravar):
- `saldo = valor_centavos − quitado_centavos`
- `vencido = situacao ∈ {aberto, parcial} && vencimento < hoje`

### 5.1 Lançar título (criar)

Entrada (`CriarTituloRequest`):

| Campo | Regra |
|---|---|
| `tipo` | obrigatório |
| `pessoaId` | opcional |
| `descricao` | obrigatório, ≤ 255 |
| `observacao` | opcional, ≤ 255 |
| `valorCentavos` | obrigatório, 1..teto. **Valor total** (é dividido se parcelado) |
| `vencimento` | obrigatório (`yyyy-MM-dd`) — vencimento da 1ª parcela |
| `categoriaId` | opcional |
| `formaPrevista` | opcional |
| `parcelas` | opcional, 1..48 (padrão 1) |
| `intervaloDias` | opcional, 0..365 (padrão 30) |
| `repetirMeses` | opcional, 1..24 — **recorrente** (ex.: aluguel) |
| `multaPercentual`, `jurosMensalPercentual` | opcionais, 0..999,99 |

Regras:
1. **Parcelado e recorrente são excludentes** ("Escolha parcelado ou recorrente, não os dois.").
2. **Parcelado**: divide o total; o **resíduo de centavos vai na última parcela** (R$ 100,00 em 3x = 33,33 / 33,33 / 33,34). Vencimentos: 1ª na data informada; seguintes a cada `intervaloDias`. Cada parcela precisa de ≥ 1 centavo.
3. **Recorrente**: N títulos com o **mesmo valor**, vencendo no mesmo dia dos meses seguintes (sem "estourar" o mês: 31/01 → 28/02 → 31/03; em Java `plusMonths` já faz isso).
4. Tudo numa transação; `total_parcelas = quantidade gerada`; `origem_tipo = manual`.
5. Título criado por humano nasce **aprovado** (`aprovado_em = agora`). Por agente/integração automática, nasce **sem aprovação** (limite de 20 rascunhos pendentes por usuário, para evitar inundação).
6. Risco: **rascunho** (lançar não mexe em dinheiro).

```java
// Rateio com resíduo na última parcela
static long[] ratear(long total, int partes) {
    long base = total / partes, resto = total % partes;
    long[] v = new long[partes];
    Arrays.fill(v, base);
    v[partes - 1] += resto;
    return v;
}
```

### 5.2 Editar título

- Só em situação **aberto ou parcial** ("Título Pago: não pode ser editado.").
- Pode mudar: descrição, observação, vencimento, categoria, forma prevista, multa/juros próprios.
- **Valor só muda** se `origem_tipo = manual`, `quitado = 0` e situação `aberto` ("O valor só muda em título lançado à mão e sem nenhuma baixa.").
- Título com **cobrança viva no gateway** (boleto/Pix gerado e não pago) **não se edita** — senão o boleto fica com valor diferente do título.
- Categoria nova: mesmo tipo do título, ativa, sem filhas.

### 5.3 Cancelar título

- Já cancelado → erro. **Com baixa** (`quitado > 0`) → "Estorne a baixa antes de cancelar."
- Marca `situacao = cancelado`, `cancelado_em = agora`.
- Cancela as cobranças vivas do gateway do título.
- Risco **crítico** (confirmação humana).

### 5.4 Aprovar título (vindo de IA/integração)

Preenche `aprovado_em`/`aprovado_por`. Crítico, só humano. A partir daí aceita baixa, entra em lembretes e pode gerar cobrança automática.

---

## 6. Livro de movimentos (`movimentos`)

**O coração do módulo.** Regras de ouro:

1. **Só um serviço grava aqui** (`LivroDeMovimentos`): baixar, estornar, lançar avulso, transferir. Nenhum controller/repositório faz `save` direto.
2. **Imutável**: o banco recusa `UPDATE` e `DELETE` (trigger ou revogar o privilégio do usuário da aplicação). **Erro se corrige com estorno.**
3. Toda operação numa transação; título travado com `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)` no JPA).

| Campo | Tipo | Regras |
|---|---|---|
| `id`, `tenant_id` | CHAR(26) | |
| `conta_id` | CHAR(26) | FK composta `(tenant_id, conta_id)` → conta da mesma empresa |
| `data` | DATE | **nunca futura**; **nunca antes de `data_saldo_inicial`** da conta |
| `tipo` | VARCHAR(24) / TipoDeMovimento | |
| `sentido` | VARCHAR(7) | `entrada` / `saida` |
| `valor_centavos` | BIGINT | **> 0** (o sentido dá o sinal) |
| `forma` | VARCHAR(20) | opcional |
| `categoria_id` | CHAR(26) | opcional |
| `descricao` | VARCHAR(255) | |
| `titulo_id` | CHAR(26) | obrigatório em `recebimento`/`pagamento`; FK composta com tenant |
| `quitado_centavos` | BIGINT ≥ 0 | quanto do principal do título esta linha quitou |
| `juros_centavos` | BIGINT ≥ 0 | |
| `multa_centavos` | BIGINT ≥ 0 | |
| `desconto_centavos` | BIGINT ≥ 0 | |
| `lote_id` | CHAR(26) | agrupa baixas feitas em lote |
| `transferencia_id` | CHAR(26) | liga as duas pernas de uma transferência |
| `estorno_de_id` | CHAR(26) | linha que este estorno anula; **único** (uma linha só se estorna uma vez) |
| `criado_por` | CHAR(26) | |
| `created_at` | TIMESTAMPTZ | (não tem `updated_at`: é imutável) |

Restrições:
```sql
CHECK (tipo IN ('recebimento','pagamento','entrada_avulsa','saida_avulsa','transferencia_entrada','transferencia_saida','estorno'))
CHECK (sentido IN ('entrada','saida'))
CHECK (valor_centavos > 0)
CHECK (quitado_centavos >= 0 AND juros_centavos >= 0 AND multa_centavos >= 0 AND desconto_centavos >= 0)
-- Linha de baixa fecha a conta; linha sem título tem as quatro colunas zeradas:
CHECK ((titulo_id IS NULL AND quitado_centavos = 0 AND juros_centavos = 0 AND multa_centavos = 0 AND desconto_centavos = 0)
    OR (titulo_id IS NOT NULL AND valor_centavos = quitado_centavos + juros_centavos + multa_centavos - desconto_centavos))
CHECK (tipo NOT IN ('recebimento','pagamento') OR titulo_id IS NOT NULL)
CHECK (tipo NOT IN ('transferencia_entrada','transferencia_saida') OR transferencia_id IS NOT NULL)
CREATE UNIQUE INDEX movimentos_estorno_unico ON movimentos (tenant_id, estorno_de_id) WHERE estorno_de_id IS NOT NULL;
CREATE INDEX ON movimentos (tenant_id, conta_id, data, id);
CREATE INDEX ON movimentos (tenant_id, titulo_id);
```

### 6.1 Baixar título (receber/pagar)

Entrada: `tituloId`, `contaId`, `data`, `forma` (obrigatórios), `quitadoCentavos` (≥ 1), `jurosCentavos`, `multaCentavos`, `descontoCentavos` (≥ 0, opcionais), `observacao` (≤ 255).

Passos (numa transação):
1. Trava o título (`FOR UPDATE`). Não encontrado → erro.
2. Sem aprovação → "Este título está aguardando aprovação: lance-o antes de dar baixa."
3. Situação não aceita baixa (pago/cancelado) → "Este título está Pago: não aceita baixa."
4. `quitado ≤ saldo` ("O valor quitado não pode passar do saldo do título. Juros e multa vão nos campos próprios.").
5. Desconto não pode zerar a baixa ("para abrir mão do título, cancele-o").
6. Conta ativa; data não futura e não anterior à abertura da conta.
7. Grava o movimento: `tipo = recebimento` (receber, sentido entrada) ou `pagamento` (pagar, sentido saída); `valor = quitado + juros + multa − desconto`.
8. `titulo.quitado += quitado`; `situacao = quitado >= valor ? PAGO : PARCIAL`.
9. Auditoria.

Risco **crítico**. **Baixa em lote**: mesma regra para vários títulos, cada um quitando o saldo inteiro, todos com o mesmo `lote_id`.

### 6.2 Juros e multa (sugestão na baixa de título a receber)

O sistema **sugere**; a pessoa pode editar livremente (sem trava). Contas **a pagar** não usam: os valores vêm do boleto do fornecedor e são digitados.

```
diasAtraso = max(0, dataBaixa − vencimento)            // em dias corridos
se diasAtraso <= carencia_dias  →  juros = 0, multa = 0
senão:
   multa% = titulo.multa_percentual ?? config.multa_percentual
   juros% = titulo.juros_mensal_percentual ?? config.juros_mensal_percentual
   multa  = round(quitado × multa% / 100)                         // única, sobre o que se quita AGORA
   juros  = round(quitado × juros% / 100 × diasAtraso / 30)       // simples, mês de 30 dias, retroativo ao dia seguinte ao vencimento, SEM a multa
```

Exemplo: título de R$ 1.000,00, vencimento 10/01, baixa 20/01, carência 2, multa 2%, juros 1% a.m.
→ atraso 10 dias (> 2): multa = R$ 20,00; juros = 1000 × 1% × 10/30 = R$ 3,33; **valor recebido = R$ 1.023,33**.

Por calcular sobre o valor quitado *nesta* baixa, uma baixa parcial não cobra multa duas vezes sobre o mesmo valor.

```java
static long porcentagemProporcional(long centavos, BigDecimal pct, long num, long den) {
    return new BigDecimal(centavos).multiply(pct).multiply(BigDecimal.valueOf(num))
        .divide(BigDecimal.valueOf(100L * den), 0, RoundingMode.HALF_EVEN).longValueExact();
}
```

### 6.3 Estornar movimento

- Um **estorno não se estorna** ("lance a operação de novo"). Uma linha só se estorna **uma vez**.
- Concorrência: `SELECT pg_advisory_xact_lock(hashtextextended('estorno:' || id, 0))` antes de conferir se já foi estornada (a linha original não pode ser travada com `FOR UPDATE`, pois a aplicação não tem `UPDATE` no livro).
- Cria nova linha `tipo = estorno`, **sentido inverso**, mesmo valor/forma/categoria/título/quitado/juros/multa/desconto/lote/transferência, `estorno_de_id = original.id`, descrição `"Estorno: <descrição original>"`, data = hoje (ou a abertura da conta, se hoje for antes).
- Se tinha título: `quitado −= original.quitado`; se não cancelado, `situacao = quitado == 0 ? ABERTO : PARCIAL`.
- **Transferência estorna as duas pernas** juntas.
- Risco crítico **+ senha recente**.

### 6.4 Lançamento avulso

Entrada/saída sem título (ex.: aporte do sócio, tarifa bancária). `contaId`, `sentido`, `valorCentavos > 0`, `data`, `forma?`, `categoriaId?` (sem filhas), `descricao?`. Tipo `entrada_avulsa`/`saida_avulsa`. Crítico.

### 6.5 Transferência entre contas

`contaOrigemId ≠ contaDestinoId` ("A conta de destino precisa ser diferente da de origem."), `valor > 0`, `data`. Gera **duas linhas** com o mesmo `transferencia_id`: `transferencia_saida` na origem e `transferencia_entrada` no destino. Crítico. Não entra em relatórios de receita/despesa nem no fluxo de caixa "realizado".

---

## 7. Extrato, saldos e relatórios

- **Extrato** ("Movimentação das contas"): filtro por conta e período; paginado (15 por página); **saldo corrente** calculado no banco com janela:
  ```sql
  SELECT m.*, :saldoAnterior + SUM(CASE WHEN sentido='entrada' THEN valor_centavos ELSE -valor_centavos END)
           OVER (ORDER BY data, id) AS saldo_corrente
  FROM movimentos m WHERE tenant_id = :t AND conta_id = :c AND data BETWEEN :de AND :ate
  ```
- **Totais por forma** no período (agregado no banco); transferências numa linha própria; movimento sem forma em "Sem forma informada".
- **Saldo das contas**: saldo inicial + soma do livro até hoje, por conta ativa.
- **Filtros de título** (a mesma definição na lista e no relatório):

| Filtro | Condição |
|---|---|
| Em aberto | situação `aberto` ou `parcial` |
| Parciais | `parcial` |
| Vencidos | `aberto`/`parcial` e `vencimento < hoje` |
| Pagos | `pago` |
| Cancelados | `cancelado` |
| Para revisão | `para_revisao = true` |

- **Fluxo de caixa**: *previsto* = títulos por vencimento; *realizado* = livro **sem** transferências.
- **Painel (widget)**: a receber/a pagar em aberto, vencidos, saldo total.

---

## 8. Integração com a nota fiscal / vendas (eventos)

O módulo de documentos (nota fiscal, venda) **não conhece** o financeiro: ele publica eventos e o financeiro reage.

| Evento | Dados | Reação do financeiro |
|---|---|---|
| `DocumentoAutorizado` | tipo e id do documento, número, `pessoaId`, `valorCentavos`, data, `condicaoDePagamentoId?`, `usuarioId` | gera os títulos a receber |
| `DocumentoCancelado` | tipo e id do documento | cancela ou marca revisão |

**Gerar títulos do documento** (idempotente):
1. Usa a condição que veio no documento (se ainda ativa). **Sem condição → 1 título à vista** com vencimento na data do documento.
2. Gera parcelas pela condição (`dias_primeiro_vencimento`, `intervalo_dias`), `origem_tipo/origem_id/origem_numero` preenchidos, `forma_prevista = condicao.forma`, categoria = padrão de receita (ou "Prestação de serviços"), observação = padrão a receber. Nasce aprovado.
3. O índice único `(tenant_id, origem_tipo, origem_id, parcela)` impede gerar duas vezes.
4. **Receber na emissão** (condição à vista marcada): já dá baixa na conta padrão da forma — **só se quem emitiu tem permissão de baixar**.
5. **Ordem dos eventos não importa**: o cancelamento grava uma marca em `documentos_cancelados (tenant_id, origem_tipo, origem_id)` e a geração consulta essa marca; os dois sob `pg_advisory_xact_lock` por documento. Documento cancelado **nunca** ganha título ativo.

**Cancelamento do documento**: título sem baixa → cancelado; título com baixa → `para_revisao = true` (humano decide estornar ou registrar devolução).

Em Java: `ApplicationEventPublisher` + `@TransactionalEventListener(phase = AFTER_COMMIT)` (ou fila/Kafka), com o listener idempotente.

---

## 9. Cobrança (opcional, fases posteriores)

### 9.1 Cobrança por e-mail e lembretes

- **Enviar cobrança** (manual): e-mail ao cliente com valor, vencimento e "como pagar" (pela forma prevista e pelos `dados_do_banco` da conta padrão da forma, ou o boleto/Pix vivo do gateway). Crítico (mensagem a terceiro, só humano).
- **Lembretes** (job diário às 9h por empresa): desligados por padrão; `lembrete_dias_antes` do vencimento e em cada dia de `lembrete_dias_depois`. Cada aviso sai **uma vez** — tabela `lembretes_de_cobranca (titulo_id, tipo['antes'|'depois'], dias)` com índice único.
- **No máximo uma cobrança por cliente por dia** (manual ou lembrete): tabela `cobrancas_enviadas (tenant_id, titulo_id, pessoa_id, origem['manual'|'lembrete'], dia)` com índice único `(tenant_id, pessoa_id, dia)`. O envio reserva o dia antes de mandar; se falhar, libera.
- Não envia para: título sem aprovação, `para_revisao`, cliente com "não enviar cobrança" marcado.
- **E-mail nunca gera boleto/Pix** — só reaproveita um já gerado.

### 9.2 Cobrança por gateway (boleto, Pix, link de pagamento)

Isolar o provedor atrás de uma **porta** (interface); só o adaptador fala com a API do provedor; nos testes, um `GatewayFalso`.

```java
public interface GatewayDeCobranca {
    RespostaDaCobranca criar(CredencialDoGateway cred, CobrancaParaCriar pedido);
    RespostaDaCobranca consultar(CredencialDoGateway cred, String idExterno, MeioDeCobranca meio);
}
```

**Credencial por conta** (`credenciais_de_cobranca`): `conta_id` (único), `provedor`, `endpoint`, `seller_id` (único no sistema inteiro: o dinheiro nunca pode cair na conta errada), `token` e `webhook_segredo` **criptografados** (nunca em log/resposta), `ativo`. Tabela global `cobranca_destinos (credencial_id, tenant_id, segredo_hash)` para achar a empresa quando o webhook chega sem sessão (guarda só o hash SHA-256 do segredo).

**Cobrança** (`cobrancas`):

| Campo | Significado |
|---|---|
| `titulo_id`, `conta_id`, `provedor` | |
| `tipo` | `boleto`, `pix`, `link` |
| `estado` | ver tabela abaixo |
| `valor_centavos` (> 0), `vencimento` | valor = saldo do título |
| `multa_percentual`, `juros_mensal_percentual` | do título ou da empresa |
| `id_externo` | id no provedor (único por empresa) |
| `linha_digitavel`, `codigo_de_barras`, `url_do_boleto`, `pix_copia_e_cola`, `url_do_pix`, `url_do_link` | dados de pagamento (só https vira link na tela) |
| `pago_em`, `valor_pago_centavos`, `meio_pago` | preenchidos na confirmação |
| `tarifa_centavos` | **nulo = provedor não informou** (nunca inventar) |
| `baixa_movimento_id`, `tarifa_movimento_id` | movimentos gerados (idempotência) |
| `motivo` (200), `resposta_do_provedor` (200) | frase nossa da recusa + texto do provedor filtrado (sem CPF/CNPJ, e-mail, telefone, links) |
| `ultima_consulta_em`, `criado_por` | |

**Estados**:

| Estado | Rótulo | Viva? | Significado |
|---|---|---|---|
| `criando` | Gerando | sim | pedido em andamento |
| `aguardando` | Aguardando pagamento | sim | gerada |
| `incerta` | Confira no portal | sim | a resposta se perdeu (timeout); **nunca reenviar sem consultar** |
| `paga` | Paga | | baixou o título |
| `paga_para_revisar` | Paga: revisar | | pago fora do esperado; **não baixa sozinho** |
| `recusada` | Recusada | | provedor recusou (mostrar motivo e campo) |
| `cancelada`, `expirada`, `descartada` | | | desfechos |

**Uma cobrança viva por título**: `CREATE UNIQUE INDEX ... ON cobrancas (titulo_id) WHERE estado IN ('criando','aguardando','incerta')`.

Regras:
- **Criar cobrança** é crítico e humano. Boleto: vencimento de hoje em diante. Pix: para pagar agora. Link: 1 a 30 dias. **Valor mínimo** (ex.: R$ 30,00 para boleto e Pix).
- **A chamada ao provedor fica FORA da transação do banco** e **nunca é repetida automaticamente**: sem resposta → `incerta`.
- **Sincronizador** (único que muda o estado depois de criada): consulta o provedor fora da transação; depois trava cobrança e título, dá a **baixa pelo livro** (idempotente por `baixa_movimento_id`); valor pago acima do saldo vira **juros**; tarifa vira **saída avulsa** na categoria "Tarifas". Pagamento estranho → `paga_para_revisar`.
- Gatilhos do sincronizador: **webhook** (só identifica a cobrança; nunca é fonte de verdade — sempre consulta o provedor), botão "Atualizar situação" e **varredor** agendado (Pix/link a cada 10 min; boleto 2x por dia; expiração 1x por dia).
- **Cobrança automática ao lançar** (regra do sistema): título **a receber**, aprovado, com forma prevista **boleto ou Pix**, empresa com a cobrança ativa em alguma conta → gera a cobrança sozinho (job após o commit, uma tentativa só, nunca repete). Conta: a padrão da forma, ou a única com cobrança; com duas ou mais e nenhuma padrão, **não gera** e avisa. Recorrente e importação em massa **não disparam**. Falha não derruba o lançamento: fica registrada e o título avisa.
- Cancelar o título ou estornar a baixa **cancela as cobranças vivas** (e avisa para cancelar no portal do provedor).

---

## 10. Permissões e risco das operações

Risco: **Leitura**; **Rascunho** (não mexe em dinheiro); **Crítico** (mexe em dinheiro ou é irreversível: sempre confirmação humana, nunca por agente automático/IA, auditado).

| Operação | Risco | Permissão |
|---|---|---|
| Listar/resumir títulos | Leitura | `financeiro.titulo.ver` |
| Criar título | Rascunho | `financeiro.titulo.criar` |
| Editar título | Rascunho | `financeiro.titulo.editar` |
| Aprovar título | Crítico | `financeiro.titulo.aprovar` |
| Baixar / baixar em lote | Crítico | `financeiro.titulo.baixar` |
| Cancelar título | Crítico | `financeiro.titulo.cancelar` |
| Enviar cobrança por e-mail | Crítico | `financeiro.titulo.cobrar` |
| Lançar avulso | Crítico | `financeiro.movimento.lancar` |
| Transferir | Crítico | `financeiro.movimento.transferir` |
| Estornar | Crítico + senha recente | `financeiro.movimento.estornar` |
| Ver extrato e saldos | Leitura | `financeiro.movimento.ver` |
| Conta: criar / inativar | Rascunho | `financeiro.conta.criar` / `.inativar` |
| Conta: editar | Crítico + senha recente | `financeiro.conta.editar` |
| Conta: configurar integração do gateway | Crítico | `financeiro.cobranca.configurar` |
| Categoria / condição: ver, criar, editar, inativar | Leitura / Rascunho | `financeiro.categoria.*`, `financeiro.condicao.*` |
| Importar CSV (contas, categorias, condições) | Crítico | `financeiro.<x>.importar` |
| Exportar CSV | Leitura | `financeiro.<x>.exportar` |
| Configuração financeira | Rascunho | `financeiro.configuracao.editar` |
| Cobrança no gateway: criar / descartar / consultar | Crítico / Rascunho / Rascunho | `financeiro.cobranca.criar` / `.criar` / `.ver` |

Sugestão de grupos: **Dono** = tudo; **Operador** = ver, criar/editar títulos, baixar, ver extrato (sem estornar, sem editar conta, sem configurar).

---

## 11. Telas (menu "Financeiro")

1. **Contas a receber** e **Contas a pagar** — a mesma lista, o tipo vem da rota. Colunas: vencimento, pessoa, descrição, parcela (`2/3`), valor, saldo, situação (badge), forma prevista. Filtros da §7, período, pessoa, categoria. Ações por linha: ver, editar, baixar, cancelar. Seleção múltipla: baixar em lote. Totais do filtro no rodapé.
2. **Formulário do título** — tipo, pessoa, descrição, valor (máscara de moeda), vencimento, categoria (agrupada por pai, só folhas selecionáveis), forma prevista, parcelado *ou* recorrente, multa/juros próprios (avançado), observação. Pré-preenche os padrões da configuração.
3. **Detalhe do título** — dados, baixas (com estornar), cobranças do gateway, histórico de alterações, ações (baixar, cancelar, enviar cobrança, gerar boleto/Pix, aprovar).
4. **Modal de baixa** — conta (pré-seleciona a padrão da forma), data (hoje), forma, valor quitado (saldo), juros e multa **sugeridos** e editáveis, desconto, total calculado ao vivo.
5. **Extrato** — conta, período, linhas com saldo corrente, totais por forma; botões "Lançamento avulso" e "Transferência".
6. **Contas** — lista com saldo atual e selo "Cobrança ativa"; formulário (com aba "Integração" do gateway).
7. **Categorias**, **Condições de pagamento** — cadastros com importar/exportar CSV.
8. **Configuração do financeiro** — multa, juros, carência, lembretes, padrões dos títulos.

Mensagens sempre em português claro, dizendo **o que fazer** (ex.: "Estorne a baixa antes de cancelar.").

---

## 12. Sugestão de estrutura em Java

```
financeiro/
  dominio/        Titulo, Movimento, Conta, Categoria, Condicao, ConfiguracaoFinanceira, enums, Dinheiro
  aplicacao/      CriarTitulo, AtualizarTitulo, CancelarTitulo, AprovarTitulo,
                  BaixarTitulo, BaixarEmLote, EstornarMovimento, LancarAvulso, TransferirEntreContas,
                  LivroDeMovimentos (único que grava em movimentos), CalculadoraDeAcrescimos,
                  GeradorDeParcelas, PadroesDoFinanceiro, TitulosDosDocumentos (listener)
  cobranca/       GatewayDeCobranca (porta), adaptadores, SincronizadorDeCobranca, jobs
  infra/          repositórios JPA, migrations Flyway/Liquibase
  web/            controllers REST (só chamam a camada de aplicação)
```

Pontos de atenção em Java:
- `long` para centavos; `BigDecimal` + `RoundingMode.HALF_EVEN` só nos percentuais; `Math.addExact` para detectar estouro.
- `@Transactional` nos casos de uso; `@Lock(LockModeType.PESSIMISTIC_WRITE)` para travar o título.
- Entidade `Movimento` **sem setters** (imutável) e `@Immutable` do Hibernate; no banco, `REVOKE UPDATE, DELETE ON movimentos FROM app_user` ou trigger que lança erro.
- Validação de entrada com Bean Validation (`@NotNull`, `@Size(max=255)`, `@Min(1)`, `@Max(99_999_999_999L)`); regras que dependem do banco (nome único, categoria folha, saldo) no serviço, com `ValidationException` por campo.
- Chamadas HTTP ao gateway **fora** do `@Transactional` (separe em dois métodos/beans).
- Migrations Flyway com os `CHECK` e índices parciais acima (JPA não gera isso sozinho).

---

## 13. Casos de teste mínimos

- Parcelar R$ 100,00 em 3x → 33,33 / 33,33 / 33,34, vencimentos corretos.
- Recorrente de 31/01 em 3 meses → 31/01, 28/02 (ou 29/02), 31/03.
- Baixa parcial → situação `parcial`; baixa do resto → `pago`.
- Baixa acima do saldo é recusada; baixa com data futura ou anterior à abertura da conta é recusada.
- Juros/multa: dentro da carência = 0; exemplo da §6.2 = R$ 1.023,33.
- Estorno volta o título para `parcial`/`aberto`; segundo estorno da mesma linha é recusado; estorno de estorno é recusado; dois estornos simultâneos → só um passa.
- Transferência gera duas linhas e não altera o saldo total; estorno anula as duas.
- Cancelar título com baixa é recusado.
- `UPDATE`/`DELETE` em `movimentos` falha no banco.
- Empresa A não vê nem baixa título da empresa B.
- Documento autorizado gera títulos uma vez só (evento repetido não duplica); cancelamento antes da geração → nenhum título ativo.
- Título criado por agente não aceita baixa antes de aprovado.
- Uma só cobrança viva por título; timeout na criação → `incerta`, sem reenvio.
