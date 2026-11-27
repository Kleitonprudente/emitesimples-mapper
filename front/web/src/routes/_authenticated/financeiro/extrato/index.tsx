// GERADO por frontend/screenRoute a partir de ExtratoFinanceiro. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/extrato/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { ExtratoScreen } from '@/features/financeiro/extrato/Extrato'

// window.custom: a tela e declarada no mapa e o componente e escrito a mao (front/mapas/README.md, "window.custom").
export const Route = createFileRoute('/_authenticated/financeiro/extrato/')({
  component: ExtratoScreen,
})
