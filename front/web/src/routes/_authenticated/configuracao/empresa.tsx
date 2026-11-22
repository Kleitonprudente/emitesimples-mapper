import { createFileRoute } from '@tanstack/react-router'
import { EmpresaConfig } from '@/features/empresa-config/EmpresaConfig'

export const Route = createFileRoute('/_authenticated/configuracao/empresa')({
  component: EmpresaConfig,
})
