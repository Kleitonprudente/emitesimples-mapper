# Cadastro de Empresa — ajustes, atividades, QSA e história do signup

**Fonte**: pedido do usuário em 2026-11-21. A `Empresa` (contexto `configuracao`) é a empresa usuária do sistema,
**emitente e pagante**: cada cadastro é uma conta.

## Ajustes na Empresa (decididos)

- **CNPJ e CPF viram o mesmo campo** (`documento`, Texto(14), só dígitos: 14 é CNPJ, 11 é CPF), como na `Pessoa`.
- **Adicionar** `rgInscricaoEstadual` (a inscrição estadual da empresa; mesmo nome do campo da `Pessoa`).
- **Retirar** `apelido`.

## Novas entidades (decididas)

- **`AtividadeEmpresa`**: `tipo` (principal ou secundaria), `codigoCnae` e `descricao`. Uma empresa tem uma atividade
  principal e várias secundárias.
- **`QsaEmpresa`** (quadro de sócios e administradores): `nome`, `cpf`, `qualificacao` e `dataEntradaSociedade`.
  Atenção, a conferir ao ler a API: a consulta pública de CNPJ pode devolver o CPF do sócio **mascarado**; por isso o `cpf`
  não deve ser validado como 11 dígitos.

## História: signup de uma empresa

1. O usuário abre a **página de signup** (pública, sem login), informa **CNPJ, e-mail e WhatsApp** e clica em **Confirmar**.
2. Ao confirmar, o sistema **consulta o CNPJ**, **cadastra a nova empresa** e **envia um link de confirmação por e-mail**.
3. O usuário abre o e-mail e clica no link; é redirecionado ao sistema, e **a confirmação do cadastro está feita**.
4. O usuário vê os **dados completos** da empresa para avaliar. Se quiser alterar algo, **só pode alterar a razão social, o
   nome (fantasia) e o endereço**. **Todo o resto fica bloqueado**, porque cada cadastro é uma empresa emitente e pagante.

## Regra de edição (decidida; é regra da aplicação, o mapa não a expressa)

O usuário da empresa edita **apenas**: `razaoSocial`, `nomeFantasia` e o endereço (`logradouro`, `numeroEndereco`,
`complemento`, `bairro`, `cep`, `municipio`). **Bloqueados para ele**: `documento`, `rgInscricaoEstadual`,
`inscricaoMunicipal`, regime tributário, atividades, QSA e tudo o que vem da consulta do CNPJ ou do gateway. O
administrador da plataforma e o suporte podem ter mais permissão (a definir). A propriedade `readOnly` do pack **não
serve**: ela é fixa por campo, e o suporte precisa poder alterar esses campos.

## Em aberto (ainda NÃO é requisito fechado; ver as perguntas feitas ao usuário)

O usuário disse: "aqui acho que vamos precisar de domínios e controles de admin/suporte para controlar as empresas
pagantes, planos e pagamentos". Isso envolve, no mínimo: o **estado da conta** (pendente de confirmação, ativa,
suspensa, cancelada), **planos**, **assinaturas**, **pagamentos/cobranças** e **ferramentas de suporte**. Nada disso foi
modelado: falta decidir as regras de cobrança, o meio de pagamento e quem faz o quê. O **login** do usuário que se
cadastra pelo signup (senha ou link) também não está definido.

## Decisões do usuário em 2026-11-21 (respostas às perguntas acima)

- **Só desativação**: a `Empresa` **nunca é apagada**, apenas **desativada**. É regra da aplicação: o recurso REST gerado de
  `Empresa` não deve expor a remoção. Isso vale como princípio para o sistema fiscal.
- **O que o signup cria**: a `Empresa` e o `Usuario` vinculado a ela (como administrador da empresa). **Falta um campo no
  signup: o nome do responsável**, que é o primeiro usuário. O **nome, o e-mail e o WhatsApp viram dados do `Usuario`**.
- **E-mail e WhatsApp também vão para o cadastro da `Empresa`**, porque várias integrações exigem os dois (IntegraNotas e os
  bancos, para boleto e PIX). Logo a `Empresa` precisa de `email` e `whatsapp`.
- **Estado da conta**: a empresa nasce **pendente de confirmação** e passa a **ativa** ao clicar no link do e-mail; o suporte
  pode **ativar e desativar manualmente** (com histórico de quem fez, quando e por quê).
- **Domínio admin / suporte** (requisito firme): gerenciar as empresas (ativar e desativar à mão), **criar planos e
  vinculá-los às empresas**, **enviar a empresa e o plano ao CRM** e **receber as faturas do CRM**. Ou seja: **a cobrança é do
  CRM**; este sistema não processa pagamento. O CRM é o da Marke (PerfexCRM).

### Ainda em aberto (não modelar sem resposta)

- O que cada **plano limita** (número de notas por mês? de usuários?), a periodicidade e se há período de teste.
- **Como o CRM e o sistema se falam**: o sistema empurra ao CRM e consulta de volta, ou o CRM avisa por webhook? Como a
  fatura chega (consulta periódica ou aviso)?
- O papel de **suporte** é o mesmo do administrador da plataforma, ou é separado (com menos poder)?
- O **login** do usuário do signup: senha ou link enviado por e-mail.
