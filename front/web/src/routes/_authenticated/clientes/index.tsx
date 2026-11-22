// GERADO por frontend/screenRoute a partir de GestaoClientes. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/clientes/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { GestaoClientesScreen } from '@/features/cadastros/cliente/GestaoClientesScreen'

export const Route = createFileRoute('/_authenticated/clientes/')({
  component: GestaoClientesScreen,
})
