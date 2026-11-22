// GERADO por frontend/screenRoute a partir de GestaoMunicipios. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/municipios/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { GestaoMunicipiosScreen } from '@/features/cadastros/municipio/GestaoMunicipiosScreen'

export const Route = createFileRoute('/_authenticated/municipios/')({
  component: GestaoMunicipiosScreen,
})
