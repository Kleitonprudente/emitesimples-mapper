// GERADO por frontend/screenRoute a partir de GestaoFornecedores. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/fornecedores/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { GestaoFornecedoresScreen } from '@/features/cadastros/fornecedor/GestaoFornecedoresScreen'

export const Route = createFileRoute('/_authenticated/fornecedores/')({
  component: GestaoFornecedoresScreen,
})
