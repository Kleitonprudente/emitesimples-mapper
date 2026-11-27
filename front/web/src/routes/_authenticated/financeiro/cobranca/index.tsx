// GERADO por frontend/screenRoute a partir de IntegracaoCobranca. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/cobranca/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { IntegracaoCobrancaScreen } from '@/features/financeiro/cobranca/IntegracaoCobranca'

// window.custom: a tela e declarada no mapa e o componente e escrito a mao (front/mapas/README.md, "window.custom").
export const Route = createFileRoute('/_authenticated/financeiro/cobranca/')({
  component: IntegracaoCobrancaScreen,
})
