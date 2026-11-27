// GERADO por frontend/screenRoute a partir de GestaoBancos. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/bancos/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { GestaoBancosScreen } from '@/features/financeiro/banco/GestaoBancosScreen'

export const Route = createFileRoute('/_authenticated/bancos/')({
  component: GestaoBancosScreen,
})
