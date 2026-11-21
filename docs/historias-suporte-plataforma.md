# Histórias: suporte da plataforma (administrador da plataforma)

**Fonte**: relato do usuário em 2026-11-21. Confirmado: o suporte **ativa e desativa empresa, troca o plano e vê as faturas**.

## O que o suporte faz

1. **Ativar e desativar empresa.** Nunca apagar: só desativar. Cada mudança de situação grava um `HistoricoSituacaoEmpresa` (quem, quando, motivo).
2. **Trocar o plano** de uma empresa (`AssinaturaEmpresa`). No máximo uma assinatura vigente por empresa.
3. **Ver as faturas** (`FaturaCrm`, espelho do que está no CRM). Só leitura.

## Fora do dia a dia citado, a decidir

- Ver notas e dados de uma empresa específica (modo irrestrito), para dar suporte.
- Corrigir a tabela de municípios e importar a carga do IBGE (`docs/carga-municipios.md`).
- Gerenciar usuários e vínculos (papéis) de uma empresa.

## Em aberto

- Motivo obrigatório ao desativar? Quem é avisado?
- Empresa desativada: o usuário consegue entrar e só ler, ou o login é barrado?
- A troca de plano vale na hora ou no próximo ciclo? A assinatura `suspensa` ocupa a vaga (provisório, ver V2)?
