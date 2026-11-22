// GERADO por frontend/screenRoute a partir de FormularioPessoa. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/pessoas/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioPessoaScreen } from '@/features/cadastros/pessoa/FormularioPessoaScreen'

function FormularioPessoaPage() {
  const { id } = Route.useParams()
  return <FormularioPessoaScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/pessoas/$id')({
  component: FormularioPessoaPage,
})
