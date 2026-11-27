// GERADO por frontend/screenRoute a partir de FormularioCondicaoPagamento. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/condicoes/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioCondicaoPagamentoScreen } from '@/features/financeiro/condicao/FormularioCondicaoPagamentoScreen'

function FormularioCondicaoPagamentoPage() {
  const { id } = Route.useParams()
  return <FormularioCondicaoPagamentoScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/financeiro/condicoes/$id')({
  component: FormularioCondicaoPagamentoPage,
})
