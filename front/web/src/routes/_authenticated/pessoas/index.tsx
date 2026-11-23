import { createFileRoute } from '@tanstack/react-router'
import { PessoasExample } from '@/examples/PessoasExample'

export const Route = createFileRoute('/_authenticated/exemplos/pessoas')({
  component: PessoasExample,
})
