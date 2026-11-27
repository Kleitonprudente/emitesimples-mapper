// GERADO por frontend/screenRoute a partir de ContasAReceber. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/receber/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { TitulosReceberScreen } from '@/features/financeiro/titulos/TitulosLista'

// window.custom: a tela e declarada no mapa e o componente e escrito a mao (front/mapas/README.md, "window.custom").
export const Route = createFileRoute('/_authenticated/financeiro/receber/')({
  component: TitulosReceberScreen,
})
