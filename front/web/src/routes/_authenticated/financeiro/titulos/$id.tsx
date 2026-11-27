// GERADO por frontend/screenRoute a partir de TituloFinanceiro. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/titulos/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { TituloScreen } from '@/features/financeiro/titulos/TituloPagina'

// window.custom: a tela e declarada no mapa e o componente e escrito a mao (front/mapas/README.md, "window.custom").
function TituloFinanceiroPage() {
  const { id } = Route.useParams()
  return <TituloScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/financeiro/titulos/$id')({
  component: TituloFinanceiroPage,
})
