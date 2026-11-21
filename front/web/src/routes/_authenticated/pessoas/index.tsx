// GERADO por frontend/screenRoute a partir de GestaoPessoas. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/pessoas/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { GestaoPessoasScreen } from '@/features/cadastros/pessoa/GestaoPessoasScreen'

export const Route = createFileRoute('/_authenticated/pessoas/')({
  component: GestaoPessoasScreen,
})
