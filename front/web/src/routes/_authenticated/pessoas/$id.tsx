// GERADO por frontend/screenRoute a partir de EditarPessoa. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/pessoas/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { EditarPessoaScreen } from '@/features/cadastros/pessoa/EditarPessoaScreen'

function EditarPessoaPage() {
  const { id } = Route.useParams()
  return <EditarPessoaScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/pessoas/$id')({
  component: EditarPessoaPage,
})
