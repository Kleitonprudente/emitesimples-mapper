// GERADO por frontend/screenRoute a partir de FormularioMunicipio. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/municipios/$id.tsx
import { createFileRoute } from '@tanstack/react-router'
import { FormularioMunicipioScreen } from '@/features/cadastros/municipio/FormularioMunicipioScreen'

function FormularioMunicipioPage() {
  const { id } = Route.useParams()
  return <FormularioMunicipioScreen id={ id } />
}

export const Route = createFileRoute('/_authenticated/municipios/$id')({
  component: FormularioMunicipioPage,
})
