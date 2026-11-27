// GERADO por frontend/screenRoute a partir de FormularioCategoriaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/categorias/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioCategoriaFinanceiraScreen } from '@/features/financeiro/categoria/FormularioCategoriaFinanceiraScreen'

function FormularioCategoriaFinanceiraPage() {
  const { id } = Route.useParams()
  return <FormularioCategoriaFinanceiraScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/financeiro/categorias/$id')({
  component: FormularioCategoriaFinanceiraPage,
})
