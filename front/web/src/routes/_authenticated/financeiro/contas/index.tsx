// GERADO por frontend/screenRoute a partir de GestaoContasFinanceiras. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/contas/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { GestaoContasFinanceirasScreen } from '@/features/financeiro/conta/GestaoContasFinanceirasScreen'

export const Route = createFileRoute('/_authenticated/financeiro/contas/')({
  component: GestaoContasFinanceirasScreen,
})
