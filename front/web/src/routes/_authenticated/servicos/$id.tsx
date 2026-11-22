// GERADO por frontend/screenRoute a partir de FormularioServico. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/servicos/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioServicoScreen } from '@/features/cadastros/servico/FormularioServicoScreen'

function FormularioServicoPage() {
  const { id } = Route.useParams()
  return <FormularioServicoScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/servicos/$id')({
  component: FormularioServicoPage,
})
