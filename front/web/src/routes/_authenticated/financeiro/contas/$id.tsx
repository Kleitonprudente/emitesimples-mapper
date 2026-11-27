// GERADO por frontend/screenRoute a partir de FormularioContaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/contas/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioContaFinanceiraScreen } from '@/features/financeiro/conta/FormularioContaFinanceiraScreen'

function FormularioContaFinanceiraPage() {
  const { id } = Route.useParams()
  return <FormularioContaFinanceiraScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/financeiro/contas/$id')({
  component: FormularioContaFinanceiraPage,
})
