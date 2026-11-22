import { createFileRoute } from '@tanstack/react-router'
import { UsuariosAdmin } from '@/features/admin/UsuariosAdmin'

export const Route = createFileRoute('/_authenticated/admin/usuarios')({
  component: UsuariosAdmin,
})
