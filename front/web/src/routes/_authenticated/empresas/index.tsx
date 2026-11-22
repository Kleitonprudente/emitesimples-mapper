// GERADO por frontend/screenRoute a partir de GestaoEmpresas. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/empresas/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { GestaoEmpresasScreen } from '@/features/configuracao/empresa/GestaoEmpresasScreen'

export const Route = createFileRoute('/_authenticated/empresas/')({
  component: GestaoEmpresasScreen,
})
