import { createFileRoute } from '@tanstack/react-router'
import { ImportarMunicipios } from '@/features/admin/ImportarMunicipios'

export const Route = createFileRoute('/_authenticated/admin/municipios')({
  component: ImportarMunicipios,
})
