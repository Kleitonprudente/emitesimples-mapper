// GERADO por frontend/screenRoute a partir de ContasAPagar. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/pagar/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { TitulosPagarScreen } from '@/features/financeiro/titulos/TitulosLista'

// window.custom: a tela e declarada no mapa e o componente e escrito a mao (front/mapas/README.md, "window.custom").
export const Route = createFileRoute('/_authenticated/financeiro/pagar/')({
  component: TitulosPagarScreen,
})
