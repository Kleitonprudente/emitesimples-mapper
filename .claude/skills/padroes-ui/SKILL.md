---
name: padroes-ui
description: Padrões de UI do front do Emite Simples (cadastro CRUD, lista, formulário, endereço com CEP, papéis, feedback, linguagem). Use ao criar ou alterar qualquer tela em front/web ou janela em front/mapas.
when_to_use: Ao criar tela, janela .mi, formulário, lista ou componente do front; ao decidir como uma tela deve se comportar; ao revisar UI.
allowed-tools: Read Glob Grep Edit Write Bash
---

# Padrões de UI

Leia `front/docs/padroes-de-ui.md` inteiro antes de criar ou mudar tela. Resumo das regras que mais se erram:

- Cadastro = **lista + um formulário em página única** (`/x/$id`, `novo` cria). Nada de diálogo para criar nem tela separada de edição. Nunca excluir: só desativar.
- Código gerado (`src/gen`, `src/features`, rotas) **nunca se edita**: mude o mapa, o gerador ou o `runtime/`.
- Endereço: CEP | Logradouro | Número, depois Complemento | Bairro | Município (combobox). CEP consulta o back.
- Máscara é só apresentação; o valor trafega em dígitos crus. Rótulo de enum é a descrição longa do domínio.
- PUT substitui o registro: a edição preserva o que o formulário não mostra.
- Papéis só escondem botão (UX); o back decide. Telas `platformOnly` agem só para o administrador da plataforma.
- `mi push` só com autorização explícita (um por autorização); desligue o Vite antes de `gerar.sh`.
- **Use SOMENTE os componentes catalogados** (`front/docs/catalogo-componentes.md`). **Não crie componente visual novo**, nem variante improvisada que imite um do catálogo. Se o catálogo não resolver, pare e peça decisão à pessoa; componente novo só entra por decisão explícita, junto com catálogo e vitrine `/admin/theme`.
- Confirmação de ação irreversível = `AlertDialog`; botão só com ícone = `Tooltip` + `aria-label`. Catálogo: `front/docs/catalogo-componentes.md` (50 primitivos em `components/ui`, vitrine em `/admin/theme`).
- Nota fiscal só é emitida pela pessoa (botão Enviar); em dev, só homologação.
- **Verificação automática**: `npm run verificar:ui` (front/web) confere as regras R1-R6 (sem componente novo, imports, sem cor fixa, formulário soma 12, cadastro sem diálogo, botão-ícone com `aria-label`); também roda no vitest. Tela escrita à mão só entra em `TELAS_A_MAO` por decisão explícita. Veja a seção 9 de `front/docs/padroes-de-ui.md`.
