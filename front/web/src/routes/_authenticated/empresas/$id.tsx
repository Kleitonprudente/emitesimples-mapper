// GERADO por frontend/screenRoute a partir de FormularioEmpresa. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/empresas/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioEmpresaScreen } from '@/features/configuracao/empresa/FormularioEmpresaScreen'

function FormularioEmpresaPage() {
  const { id } = Route.useParams()
  return <FormularioEmpresaScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/empresas/$id')({
  component: FormularioEmpresaPage,
})
