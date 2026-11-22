// GERADO por frontend/screenForm a partir de FormularioMunicipio. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { municipioSchema } from '@/gen/schemas/municipio'

const SUBMIT_LABEL = 'Salvar'

const schema = municipioSchema.pick({ codigoIbge: true, nome: true, uf: true })
export type FormularioMunicipioValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioMunicipioFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioMunicipioValues> = {
}

const FIELDS: FieldLayout = [
  [
    { name: 'codigoIbge', label: 'Código IBGE', maxLength: 7, required: true, width: 25 },
    { name: 'nome', label: 'Nome', maxLength: 100, required: true, width: 55 },
    { name: 'uf', label: 'UF', maxLength: 2, required: true, width: 20 },
  ],
]

export interface FormularioMunicipioFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioMunicipioValues
  onSubmit: (values: FormularioMunicipioValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioMunicipioForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioMunicipioFormProps) {
  return (
    <AutoForm
      schema={schema}
      fields={FIELDS}
      defaults={DEFAULTS}
      values={values}
      isSubmitting={isSubmitting}
      serverError={serverError}
      onCancel={onCancel}
      onSubmit={onSubmit}
      submitLabel={submitLabel ?? SUBMIT_LABEL}
    />
  )
}
