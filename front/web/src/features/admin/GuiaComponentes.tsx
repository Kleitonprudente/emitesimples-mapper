import { GuiaDados } from './GuiaComponentesDados'
import { GuiaFeedback, GuiaLayout } from './GuiaComponentesFeedback'
import { GuiaFormulario } from './GuiaComponentesFormulario'
import { GuiaNavegacao } from './GuiaComponentesNavegacao'
import { GuiaSobreposicoes } from './GuiaComponentesSobreposicoes'

/** Catalogo de todos os componentes de src/components/ui, um exemplo compacto de cada. */
export function GuiaComponentes() {
  return (
    <div className="space-y-8">
      <GuiaNavegacao />
      <GuiaFormulario />
      <GuiaSobreposicoes />
      <GuiaDados />
      <GuiaFeedback />
      <GuiaLayout />
    </div>
  )
}
