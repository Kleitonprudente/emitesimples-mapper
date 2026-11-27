# Prompt para a IA — implementar o Módulo Financeiro (Java)

> **Como usar:** preencha a parte "Dados do projeto" (troque o que está entre `[colchetes]`). Na primeira conversa, envie o **Prompt inicial** junto com o arquivo `modulo-financeiro.md`. Depois mande **uma fase por vez**: só passe para a próxima quando a anterior compilar, os testes passarem e você tiver revisado.

---

## Prompt inicial (enviar uma vez, com o arquivo `modulo-financeiro.md` anexado)

```
Você vai implementar o Módulo Financeiro (contas a receber, contas a pagar, caixa/bancos, extrato e cobrança) no meu projeto Java.

A especificação completa está no arquivo anexo `modulo-financeiro.md`. Ela é a FONTE DA VERDADE das regras de negócio: campos, limites, validações, mensagens de erro, cálculos e restrições do banco. Não invente regra que não está lá. Se algo estiver ambíguo ou conflitar com o meu projeto, PERGUNTE antes de decidir.

## Dados do projeto
- Linguagem/framework: [Java 21, Spring Boot 3.x]
- Banco: [PostgreSQL 16]; migrations com [Flyway / Liquibase]
- Persistência: [Spring Data JPA / Hibernate]
- Front-end: [Angular / React / Thymeleaf / JSF / só API REST]
- Pacote raiz: [br.com.empresa.erp]
- Multiempresa: [cada empresa tem tenant_id; o tenant atual vem de ...] (ou: [o sistema é de uma empresa só])
- Usuário logado e permissões: [Spring Security; o usuário atual vem de ...; permissões por ...]
- Cadastro de clientes/fornecedores já existente: [classe/tabela ..., campo que diz se é cliente ou fornecedor ...]
- Nota fiscal/venda já existente: [classe/evento que avisa nota autorizada/cancelada ..., ou "não há ainda"]
- Padrão de código a seguir: [veja o módulo/pacote ... como exemplo]
- Testes: [JUnit 5 + Testcontainers / H2 / ...]

## Regras de trabalho
1. Trabalhe em FASES. Agora só leia a especificação e o meu projeto e me responda:
   a) o que entendeu da arquitetura do meu projeto (pacotes, como as entidades, os serviços e os controllers são feitos);
   b) como vai encaixar o módulo (pacotes e classes principais);
   c) dúvidas e conflitos entre a especificação e o projeto, cada uma com a sua recomendação.
   NÃO escreva código nesta primeira resposta.
2. Em cada fase: escreva as migrations, o código e os testes; rode a compilação e os testes; me mostre o que foi criado e o que ficou pendente.
3. Regras que não se quebram (detalhes no .md):
   - Dinheiro em centavos `long` (BIGINT). Nunca double/float. Percentuais em BigDecimal; arredondamento HALF_EVEN, uma vez só.
   - Regra de negócio fica na camada de serviço/caso de uso, nunca no controller nem na entidade.
   - Só o `LivroDeMovimentos` grava na tabela `movimentos`. O livro é imutável (o banco recusa UPDATE/DELETE); erro se corrige com estorno.
   - Título travado com lock pessimista em toda baixa e estorno.
   - Toda consulta filtrada pela empresa atual; nunca confie em id vindo da tela sem conferir a empresa.
   - Os CHECK e índices únicos parciais da especificação vão nas migrations.
   - Mensagens de erro em português, iguais ou equivalentes às da especificação, dizendo o que o usuário deve fazer.
   - Chamada HTTP a provedor externo nunca dentro da transação do banco.
4. Não implemente nada de uma fase futura sem eu pedir.
```

---

## Fase 1 — Base e cadastros

```
FASE 1: base e cadastros. Siga as seções 2, 3 e 4 do `modulo-financeiro.md`.

Implemente:
- Utilitário de dinheiro: converter texto "1.234,56" <-> centavos, formatar, ratear (resíduo na última parcela), porcentagem e porcentagem proporcional (HALF_EVEN), teto de 99.999.999.999 centavos.
- Enums: Forma, TipoDeConta, TipoDeCategoria, TipoDeTitulo, SituacaoDoTitulo, TipoDeMovimento, SentidoDoMovimento.
- Migrations, entidades, repositórios, serviços e API REST de: Conta financeira (§4.1), Categoria (§4.2, com pai/filha de um nível só e as dicas), Condição de pagamento (§4.3), Configuração financeira (§4.4) e Bancos (§4.5).
- Serviço idempotente `PadroesDoFinanceiro.garantir()` (§4.6).
- Regras: nome único, uma conta padrão por forma (desmarcar a anterior), saldo inicial e data travados quando a conta tiver movimentos (deixe o ponto preparado para a Fase 3), "receber na emissão" só à vista e com conta padrão.
- Testes dessas regras e do isolamento entre empresas.

Ainda NÃO faça títulos, movimentos nem telas.
```

## Fase 2 — Títulos (contas a receber e a pagar)

```
FASE 2: títulos. Siga a seção 5 do `modulo-financeiro.md` (e a §10 para permissões e risco).

Implemente:
- Migration e entidade `Titulo` com todos os campos e CHECK/índices da §5.
- `GeradorDeParcelas` e os casos de uso: CriarTitulo (parcelado OU recorrente, §5.1), AtualizarTitulo (§5.2), CancelarTitulo (§5.3), AprovarTitulo (§5.4).
- Validações: pessoa com o papel certo (cliente no receber, fornecedor no pagar), categoria ativa, do tipo certo e sem filhas; valor só muda em título manual sem baixa; não editar pago/cancelado.
- Pré-preenchimento com os padrões da configuração (§4.4), só como valor inicial.
- Consultas: lista com os filtros da §7 (em aberto, parciais, vencidos, pagos, cancelados, para revisão), período, pessoa e categoria; totais; resumo.
- API REST e testes (parcelamento 100,00 em 3x = 33,33/33,33/33,34; recorrente de 31/01; edição e cancelamento).

O cancelamento com baixa deve ser recusado; deixe a checagem de "tem baixa" usando `quitado_centavos` (a baixa vem na Fase 3).
```

## Fase 3 — Livro de movimentos, baixa, estorno e extrato

```
FASE 3: livro de movimentos. Siga as seções 6 e 7 do `modulo-financeiro.md`.

Implemente:
- Migration de `movimentos` com TODOS os CHECK, FKs compostas (tenant_id + id) e índices da §6. Impeça UPDATE e DELETE no banco (trigger ou REVOKE) e marque a entidade como imutável.
- `LivroDeMovimentos`, o único que grava no livro: baixar (§6.1), estornar (§6.3, com pg_advisory_xact_lock), lançar avulso (§6.4), transferir (§6.5) e calcular saldo.
- `CalculadoraDeAcrescimos` (§6.2): sugestão de juros e multa só para título a receber, com carência e sobrescrita do título.
- Casos de uso: BaixarTitulo, BaixarTitulosEmLote, EstornarMovimento (pede senha recente), LancarMovimento, TransferirEntreContas, todos críticos.
- Trava de saldo inicial e data da conta quando houver movimentos (pendência da Fase 1).
- Extrato com saldo corrente por SUM() OVER, totais por forma, saldo das contas e fluxo de caixa previsto x realizado (§7).
- Testes da §13 referentes a baixa, juros/multa (exemplo de R$ 1.023,33), estorno (inclusive concorrente), transferência, imutabilidade e isolamento.
```

## Fase 4 — Telas

```
FASE 4: telas. Siga a seção 11 do `modulo-financeiro.md`, no padrão visual do meu projeto ([tecnologia de front], use [tela X] como modelo).

Crie as telas do menu "Financeiro": Contas a receber e Contas a pagar (a mesma lista, o tipo vem da rota), formulário e detalhe do título, modal de baixa (juros e multa sugeridos e editáveis, total ao vivo), baixa em lote, Extrato (com lançamento avulso e transferência), Contas, Categorias (seleção agrupada por pai, só filhas selecionáveis), Condições de pagamento e Configuração do financeiro.
- Valores na tela em reais com máscara, convertidos para centavos antes de enviar.
- Botões e ações conforme as permissões da §10; ações críticas com confirmação.
- A tela só chama a API/serviço; nenhuma regra de negócio no front.
```

## Fase 5 — Títulos gerados pela nota fiscal / venda

```
FASE 5: integração com documentos. Siga a seção 8 do `modulo-financeiro.md`.

No meu projeto, o documento é [nota fiscal / venda], em [classe/pacote]. Ele deve publicar os eventos `DocumentoAutorizado` e `DocumentoCancelado` (o módulo de documentos NÃO pode depender do financeiro).

Implemente:
- Os eventos e a publicação no ponto em que o documento é autorizado ou cancelado.
- Listeners com @TransactionalEventListener(AFTER_COMMIT) que chamam `TitulosDosDocumentos`: gerar (idempotente, condição do documento ou à vista, "receber na emissão" só se quem emitiu pode baixar) e cancelar (sem baixa = cancela; com baixa = para_revisao).
- Tabela `documentos_cancelados` e o advisory lock por documento, para a ordem dos eventos não importar.
- Testes: evento repetido não duplica; cancelamento antes da geração não deixa título ativo; cancelamento com baixa marca revisão.
```

## Fase 6 — Cobrança por e-mail e lembretes

```
FASE 6: cobrança por e-mail. Siga a seção 9.1 do `modulo-financeiro.md`.

Implemente:
- Caso de uso crítico "Enviar cobrança" (e-mail ao cliente com valor, vencimento e "como pagar" pela forma prevista e pelos dados da conta padrão).
- Lembretes automáticos (job diário, desligados por padrão, dias antes/depois da configuração), tabela `lembretes_de_cobranca` (cada aviso uma vez só).
- Tabela `cobrancas_enviadas` com no máximo uma cobrança por cliente por dia (reserva antes de enviar; libera se falhar).
- Não enviar para título sem aprovação, em revisão ou cliente que não quer cobrança.
- Envio de e-mail pelo serviço de e-mail que o projeto já usa: [...].
- Testes com servidor de e-mail falso.
```

## Fase 7 — Cobrança por gateway (boleto, Pix, link)

```
FASE 7: cobrança por gateway. Siga a seção 9.2 do `modulo-financeiro.md`.

Provedor: [nome do banco/gateway]. Documentação da API anexa: [arquivo/link]. Ambiente de teste: [sandbox/homologação]. (As credenciais NÃO vão no código nem no prompt: ficam em [variável de ambiente / cofre].)

Implemente:
- A porta `GatewayDeCobranca` e um adaptador para este provedor (único lugar que conhece a API), mais um `GatewayFalso` para os testes. Os testes nunca chamam o provedor de verdade.
- Tabelas `credenciais_de_cobranca` (token e segredo criptografados), `cobranca_destinos` (só o hash do segredo) e `cobrancas`, com o índice de uma cobrança viva por título.
- Criar cobrança (crítico; chamada ao provedor FORA da transação, nunca repetida; sem resposta = `incerta`), consultar e descartar.
- `SincronizadorDeCobranca`: único que muda o estado; dá a baixa pelo LivroDeMovimentos (idempotente), excedente vira juros, tarifa vira saída avulsa em "Tarifas", pagamento estranho vai para `paga_para_revisar`.
- Webhook autenticado pelo segredo (só identifica a cobrança e manda consultar; nunca é fonte de verdade), varredor agendado e botão "Atualizar situação".
- Cobrança automática ao lançar título a receber com forma boleto/Pix (job após o commit, uma tentativa só).
- Recusa do provedor mapeada pelo CÓDIGO (não pelo texto) para uma frase em português; o texto do provedor é filtrado (sem CPF/CNPJ, e-mail, telefone, links) e nunca vai a log.
- Tela da aba "Integração" da conta e o quadro de cobrança no detalhe do título.
```

---

## Dicas

- Se a IA começar a escrever código na primeira resposta, peça para parar e mandar só a análise.
- Ao fim de cada fase, peça: *"Liste o que da seção X do .md ficou de fora ou diferente, e por quê."* Isso pega regra esquecida.
- Se mudar alguma regra (ex.: carência de 5 dias), altere também o `modulo-financeiro.md`, para a IA não "voltar" à regra antiga nas fases seguintes.
- A Fase 7 depende do provedor escolhido; sem a documentação da API dele, peça só a porta e o `GatewayFalso`.
