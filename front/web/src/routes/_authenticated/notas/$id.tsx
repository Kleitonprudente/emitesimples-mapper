import { createFileRoute } from '@tanstack/react-router'
import { NotaDetalhe } from '@/features/notas/NotaDetalhe'

function NotaDetalhePage() {
  const { id } = Route.useParams()
  return <NotaDetalhe id={id} />
}

export const Route = createFileRoute('/_authenticated/notas/$id')({
  component: NotaDetalhePage,
})
