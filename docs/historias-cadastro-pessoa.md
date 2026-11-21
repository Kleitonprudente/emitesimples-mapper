# Cadastro de Pessoa — campos e histórias de usuário

**Fonte**: pedido do usuário em 2026-11-21. Este documento é a fonte de requisito do cadastro da `Pessoa`
(contexto `cadastros`). No mapa, a pessoa que toma o serviço na NFS-e é uma `Pessoa`.

## Campos pedidos

| Campo | Observação do usuário |
|---|---|
| CNPJ / CPF | **mesmo campo** (um só atributo de documento) |
| RG / inscrição estadual | **mesmo campo** (RG para pessoa física, inscrição estadual para pessoa jurídica) |
| Inscrição municipal | |
| Nome / razão social | **mesmo campo** |
| Fantasia | |
| Tipo destinatário | |
| Tipo pessoa | física ou jurídica |
| Consumidor final | sim ou não |
| Logradouro, número do endereço, complemento, bairro, CEP | |
| Cidade, UF | o código IBGE **não é visível ao usuário**: é obtido a partir da cidade e da UF informadas, por isso **o IBGE tem de estar direto na cidade** |
| E-mail, telefone | |
| Contatos | lista com **tipo de contato**, **WhatsApp** e **e-mail**; é possível adicionar **vários** contatos |

## Campos a retirar

`nif`, `dispensaNif`, `caepf` e `tipoEnteGovernamental`.

## História: cadastrar cliente pessoa jurídica

O usuário clica no menu **Cliente**, **Cadastrar**, seleciona **pessoa jurídica**, informa o **CNPJ** e clica em
**Consultar CNPJ**. O sistema consulta o CNPJ na API **CNPJ.ws**, recebe o retorno e preenche todos os dados: razão
social, fantasia, inscrição estadual ativa e o endereço completo. Se houver inscrição estadual, o campo **tipo
consumidor** é preenchido como **não**. Os **sócios do QSA** (quadro de sócios e administradores) são lançados em
**contatos**. O usuário confere os dados, ajusta os contatos e clica em **Salvar**.

## História: cadastrar cliente pessoa física

O usuário clica no menu **Cliente**, **Cadastrar**, seleciona **pessoa física**, digita o **CPF** e o **RG**, informa o
**nome**. O **tipo consumidor** já vem preenchido como **sim**. Informa o **contato** com tipo, WhatsApp e e-mail e
digita o **CEP**. O sistema consulta o CEP pela API da **BrasilAPI**, recebe os dados e preenche o endereço completo,
com a cidade. O usuário confere os dados e clica em **Salvar**.

## Regras que decorrem das histórias

- O **tipo de pessoa** define como se lê o documento: 14 dígitos é CNPJ (pessoa jurídica) e 11 dígitos é CPF (pessoa
  física). Ao enviar a NFS-e ao IntegraNotas, o sistema separa o documento em `cnpj` ou `cpf`, que lá são campos
  distintos.
- **Consumidor final**: pessoa física nasce como **sim**. Pessoa jurídica com inscrição estadual preenchida passa a
  **não**.
- **Cidade e IBGE**: as consultas de CNPJ e de CEP devolvem a cidade e a UF por nome. O código IBGE é buscado pela
  tabela de cidades (cidade e UF), que por isso precisa conter o IBGE de cada cidade e estar carregada.
- **Contatos**: os sócios do QSA entram como contatos, e o usuário pode ajustá-los antes de salvar.

## Integrações externas (a confirmar na implementação, fora do mapa)

- **CNPJ.ws** (consulta de CNPJ): formato exato da resposta, campos de inscrição estadual e do QSA, e limites de uso.
- **BrasilAPI** (consulta de CEP): formato da resposta e se ela devolve o código IBGE da cidade.

Nenhuma das duas foi lida ainda. Não presuma o formato das respostas.
