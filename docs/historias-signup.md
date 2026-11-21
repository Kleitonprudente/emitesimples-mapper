# História: cadastro (signup) e primeiro acesso

**Fonte**: relato do usuário em 2026-11-21. O que não foi dito está em "Em aberto"; nada foi inventado.
Complementa `historias-cadastro-empresa.md`.

## Fluxo

1. O usuário clica em **Signup**.
2. Preenche **CNPJ, nome, e-mail e WhatsApp** e clica em **Cadastrar** (enviar link de confirmação).
3. O sistema recebe o cadastro, **consulta o CNPJ** (CNPJ.ws), **preenche todos os dados da empresa** e **envia o link de confirmação** para o e-mail informado.
4. O usuário recebe o e-mail e clica no link.
5. A tela mostra os **dados da empresa** já preenchidos, e o usuário **termina de informar o que falta**: **certificado digital, logo** e outros.

## Consequências para o sistema (a confirmar)

- O signup é **público** (sem login) e precisa de proteção contra abuso (limite por IP, CNPJ repetido).
- O link de confirmação é de uso único, com validade (entidade `ConfirmacaoCadastro`, só o hash do token no banco).
- Antes da confirmação a empresa fica **pendente** e não emite nota.
- A tela pós-confirmação edita a **Empresa** com o papel de administrador da empresa recém-criada.
- Certificado digital é segredo: nunca em log, nunca em entidade legível; precisa de endpoint de envio de arquivo e de armazenamento próprio.

## Em aberto (preciso do usuário)

- Como o usuário **define a senha** e entra pela primeira vez: pelo próprio link de confirmação (premissa atual) ou por e-mail separado?
- **"E outros"**: quais dados faltam além de certificado e logo (inscrição municipal, regime tributário, série e numeração, serviços padrão?).
- Se a consulta do CNPJ **falhar ou o CNPJ não existir**: o que o usuário vê?
- CNPJ já cadastrado: bloquear, ou reenviar o link?
- **Empresa não ativa** (certificado ainda ausente): o que o sistema libera até lá?
- O WhatsApp é do **responsável** (usuário) ou da empresa?
