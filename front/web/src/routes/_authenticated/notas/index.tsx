import { createFileRoute } from '@tanstack/react-router'
import { NotasLista } from '@/features/notas/NotasLista'

export const Route = createFileRoute('/_authenticated/notas/')({
  component: NotasLista,
})
