// GERADO por frontend/screenForm a partir de FormularioBanco. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { bancoSchema } from '@/gen/schemas/banco'

const SUBMIT_LABEL = 'Salvar'

const schema = bancoSchema.pick({ codigo: true, ispb: true, nome: true, nomeExtenso: true, participaCompe: true, integradoGateway: true, urlPassoAPasso: true })
export type FormularioBancoValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioBancoFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioBancoValues> = {
  participaCompe: false,
  integradoGateway: false,
}

const FIELDS: FieldLayout = [
  { section: 'Banco' },
  [
    { name: 'codigo', label: 'Código', maxLength: 4, required: true, size: 's', hint: 'Sem zeros à esquerda (ex.: 1, 341).' },
    { name: 'ispb', label: 'ISPB', maxLength: 8, size: 's' },
    { name: 'nome', label: 'Nome', maxLength: 120, required: true, size: 'l' },
  ],
  [
    { name: 'nomeExtenso', label: 'Nome Completo', maxLength: 200, size: 'xl' },
    { name: 'participaCompe', label: 'Participa da COMPE', kind: 'boolean', required: true, size: 'm' },
  ],
  [
    { name: 'integradoGateway', label: 'Aceita Cobrança Integrada', kind: 'boolean', required: true, size: 'm' },
    { name: 'urlPassoAPasso', label: 'Link do Passo a Passo', maxLength: 500, size: 'xl', hint: 'Link https de ajuda para integrar a cobrança.' },
  ],
]

export interface FormularioBancoFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioBancoValues
  onSubmit: (values: FormularioBancoValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioBancoForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioBancoFormProps) {
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
