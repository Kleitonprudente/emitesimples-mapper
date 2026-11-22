// GERADO por frontend/screenRoute a partir de FormularioCliente. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/clientes/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioClienteScreen } from '@/features/cadastros/cliente/FormularioClienteScreen'

function FormularioClientePage() {
  const { id } = Route.useParams()
  return <FormularioClienteScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/clientes/$id')({
  component: FormularioClientePage,
})
