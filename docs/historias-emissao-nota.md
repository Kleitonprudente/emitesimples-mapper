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

## Em aberto (preciso do usuário)

- **Cancelamento**: o relato não cobriu. Quem cancela, em que prazo, com que justificativa, e o que a tela mostra.
- **PDF**: abrir o PDF que o gateway devolve ou gerar o do sistema? O gateway fornece o endereço do PDF?
- **E-mail**: o sistema envia (com que remetente) ou o usuário baixa e envia? Para quem (e-mail do tomador, outro)? Pode editar o destinatário?
- **Consulta da nota** quando o retorno demora (autorização assíncrona): o usuário espera na tela, ou a lista atualiza depois?
- **Agente**: o fluxo de rascunho e aprovação por link fica para a fase do agente; confirmar o que se vê na tela de aprovação, e a rejeição.
