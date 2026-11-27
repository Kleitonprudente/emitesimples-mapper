// GERADO por frontend/screenRoute a partir de FormularioBanco. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/bancos/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioBancoScreen } from '@/features/financeiro/banco/FormularioBancoScreen'

function FormularioBancoPage() {
  const { id } = Route.useParams()
  return <FormularioBancoScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/bancos/$id')({
  component: FormularioBancoPage,
})
