// GERADO por frontend/screenRoute a partir de AgentesDeAcesso. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/agentes/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { AgentesDeAcessoScreen } from '@/features/configuracao/agentes/AgentesDeAcesso'

// window.custom: a tela e declarada no mapa e o componente e escrito a mao (front/mapas/README.md, "window.custom").
export const Route = createFileRoute('/_authenticated/agentes/')({
  component: AgentesDeAcessoScreen,
})
