# Histórias: emissão da NFS-e

**Fonte**: relato do usuário em 2026-11-21. O que não foi dito está em "Em aberto"; nada foi inventado.

## Duas formas de chegar à nota

| Quem opera | O que acontece | Aprovação |
|---|---|---|
| **Pessoa no sistema** | preenche a nota e clica em **Enviar** | o clique em Enviar **é** a aprovação explícita; não há rascunho a aprovar |
| **Agente de IA** (externo via MCP, ou assistente interno) | prepara um **rascunho** | a pessoa aprova num link, no sistema |

A etapa de rascunho e de aprovação por link existe **só para o agente**. Quem opera a tela envia direto.

## História principal: o usuário emite uma nota

1. O usuário acessa o sistema e clica em **Nota NFS-e**.
2. Preenche os dados (cliente, serviço, valores) e clica em **Enviar**.
3. Aguarda a nota retornar com a autorização.
4. **Autorizada**: aparecem as ações **Visualizar PDF** e **Enviar por e-mail**.
5. **Com erro**: o sistema mostra ao usuário **qual foi o erro** do gateway, o usuário ajusta os dados e **envia de novo**, quantas vezes for preciso, até a autorização.

## Consequências para o sistema (a confirmar na modelagem)

- A nota nasce e é enviada na mesma ação. O estado intermediário (aguardando retorno) existe e a tela precisa mostrá-lo.
- O erro devolvido pelo IntegraNotas precisa chegar ao usuário com a mensagem do gateway, legível, e a nota continua editável para reenvio.
- Um reenvio **não pode duplicar** a nota nem queimar numeração sem necessidade (relação com a unicidade `empresa + ambiente + série + número`).
- Em homologação o envio vai para o ambiente de homologação do IntegraNotas; a regra de produção do projeto continua valendo.

## Tributação da nota (decisões do dono, 2026-11-24)

**Fonte**: `C:\Projetos\tributacao-nfse-integranotas.md` (provado em homologação em 2026-11-18 e 2026-11-19) e o schema
`docs/integranotas-nfse-v2-schema.md`. Onde divergem, vale o homologado (ver "Divergências com o schema de 2026-11-24" em
`docs/integranotas-nfse-v2.md`).

Regras de negócio (implementação à mão; o mapa só descreve):

- **Tudo é opcional.** Cada grupo só vai com dado. Sem nenhum dado tributário, o envio é igual ao de antes **com uma única
  diferença**: o grupo `tributos_municipais` (que ia sempre com `tipo_operacao` e `iss_retido`) deixa de ir (ver "Nota simples" abaixo).
- **Dinheiro** em `ValorMonetario(15,2)` (reais, sem centavos inteiros); **alíquotas** em `Decimal(5,2)`, em pontos (5.00 = 5%).
- **Fora do modelo**: deduções, previdência social, CNAE, inscrição estadual do tomador, tipo de ente governamental e financeiro.
- **IA nunca informa tributo**: os campos tributários só se preenchem pela tela, por uma pessoa.
- **O serviço guarda padrões; a nota guarda os valores usados.** O serviço sugere ISS retido, alíquota, CST, tipo de retenção e alíquotas de PIS e COFINS. Coerência no cadastro do serviço:
  com ISS retido ligado, a alíquota do ISS é obrigatória; com tipo de retenção ou alíquota de PIS/COFINS, o CST é obrigatório.
- **Base do ISS e ISS retido são só calculados, nunca digitados** (decisão do dono, 2026-11-25, depois da homologação; substitui a base
  em %). A homologação mostrou que o governo **ignora** a base e o ISS informados e recalcula: base = valor dos serviços − desconto
  incondicionado (mínimo 0) e ISS = base × alíquota, 2 casas, meio para cima (`vBC = vServ − vDescIncond`, `vISSQN = vBC × pAliq`;
  homologação de 2026-11-25). O sistema faz a mesma conta, mostra os valores em R$ (somente leitura) e envia esses
  valores: o que a tela mostra é o que sai na nota autorizada. Para uma base menor, o caminho aceito é o **desconto incondicionado** (R$).
- **Regra E0625** (governo): o grupo `tributos_municipais` **só vai no envio** quando há dado de ISS (ISS retido ligado, alíquota,
  base ou valor retido; `iss_retido = false` sozinho não conta) **e** (regime de apuração diferente de 1 **ou** ISS retido). Sem
  regime, vale 1. Quando vai, leva sempre `aliquota_iss` (obrigatória, **nunca deduzida**) e `valor_base_calculo_iss`.
- **`tipo_operacao` deixou de ser enviado**: o campo não existe no schema de 2026-11-24. Os atributos `tipoOperacaoIss` (Serviço e
  Nota) continuam no mapa, sem efeito no envio.
- **Nota simples** (sem dado de ISS) **sai sem o grupo `tributos_municipais`**. Isso precisa ser **homologado** no IntegraNotas.
- **`tipo_retencao` vai no JSON como texto** (`"2"`), embora o mapa o guarde como `Inteiro(1)` com os valores 0 a 9. Em branco,
  o gateway assume 2.
- **CST do PIS/COFINS**: lista fechada de 33 códigos (texto de 2 dígitos, com zero à esquerda). O sistema **nunca escolhe** o CST;
  o grupo `tributos_nacionais` só vai com dado e então exige o CST.
- **Base federal em branco** = valor dos serviços, cheio (a base do ISS desconta o desconto incondicionado; a federal não).
- **Código de tributação municipal**: exatamente 3 dígitos, ou em branco; `000` é recusado (E0315).
- **Validações ao preparar ou reenviar a nota** (mensagem no campo, em linguagem simples, dizendo o que corrigir; as fiscais terminam
  com "Confirme com seu contador"):
  - grupo do ISS vai e falta a alíquota → "Informe a alíquota do ISS (por exemplo 5,00). Confirme com seu contador.";
  - qualquer campo federal preenchido e sem CST → "Escolha o CST do PIS/COFINS. Confirme o código com seu contador.";
  - base federal maior que o valor dos serviços; soma das retenções federais maior que o valor dos serviços;
  - desconto incondicionado + ISS retido + retenções federais maior que o valor dos serviços → "O desconto e os impostos retidos,
    somados, não podem passar do valor da nota."
- **Validações no cadastro do serviço**: com ISS retido ligado, a alíquota do ISS é obrigatória; com tipo de
  retenção ou alíquota de PIS/COFINS, o CST é obrigatório.
- As validações ficam descritas aqui e nos atributos; o mapa não tem regra condicional entre campos, então o código escrito à mão as implementa:
  as da nota em `nfseemissao/TributacaoNota`; as do cadastro do serviço em `nfseemissao/ServicoTributacaoListener` (listener JPA registrado em
  `back/api/src/main/resources/META-INF/orm.xml`, roda no POST, PUT e PATCH do recurso gerado, com o estado final do registro).
- **Empresa fora do Simples Nacional** (sem `regime_apuracao`): a regra E0625 e a lista de CST foram provadas só no Simples. Antes de
  liberar a tributação para regime normal, confirmar com o dono e o contador.

## Em aberto (preciso do usuário)

- **Cancelamento**: o relato não cobriu. Quem cancela, em que prazo, com que justificativa, e o que a tela mostra.
- **PDF**: abrir o PDF que o gateway devolve ou gerar o do sistema? O gateway fornece o endereço do PDF?
- **E-mail**: o sistema envia (com que remetente) ou o usuário baixa e envia? Para quem (e-mail do tomador, outro)? Pode editar o destinatário?
- **Consulta da nota** quando o retorno demora (autorização assíncrona): o usuário espera na tela, ou a lista atualiza depois?
- **Agente**: o fluxo de rascunho e aprovação por link fica para a fase do agente; confirmar o que se vê na tela de aprovação, e a rejeição.
