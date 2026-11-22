// GERADO por frontend/screenRoute a partir de FormularioFornecedor. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/fornecedores/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioFornecedorScreen } from '@/features/cadastros/fornecedor/FormularioFornecedorScreen'

function FormularioFornecedorPage() {
  const { id } = Route.useParams()
  return <FormularioFornecedorScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/fornecedores/$id')({
  component: FormularioFornecedorPage,
})
