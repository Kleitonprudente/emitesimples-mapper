import { createFileRoute } from '@tanstack/react-router'
import { NotaNova } from '@/features/notas/NotaNova'

export const Route = createFileRoute('/_authenticated/notas/novo')({
  component: NotaNova,
})
