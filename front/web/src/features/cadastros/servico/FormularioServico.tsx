// GERADO por frontend/screenForm a partir de FormularioServico. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { servicoSchema } from '@/gen/schemas/servico'

const SUBMIT_LABEL = 'Salvar'

const schema = servicoSchema.pick({ nome: true, ativo: true, codigoServico: true, codigoNbs: true, percentualTributosSimples: true, discriminacaoPadrao: true })
export type FormularioServicoValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioServicoFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioServicoValues> = {
  ativo: true,
}

const FIELDS: FieldLayout = [
  { section: 'Serviço' },
  [
    { name: 'nome', label: 'Nome do serviço', maxLength: 100, required: true, size: 'xl' },
    { name: 'ativo', label: 'Ativo', kind: 'boolean', required: true, size: 'm' },
  ],
  [
    { name: 'codigoServico', label: 'Código de tributação nacional', maxLength: 6, required: true, size: 'm', hint: '6 dígitos, ex.: 010401.' },
    { name: 'codigoNbs', label: 'Código NBS', maxLength: 9, size: 'm', hint: '9 dígitos, ex.: 115021000.' },
    { name: 'percentualTributosSimples', label: 'Tributos aprox. Simples (%)', kind: 'number', size: 'm', hint: 'Ex.: 13,75.' },
  ],
  [
    { name: 'discriminacaoPadrao', label: 'Descrição do serviço para a nota', maxLength: 2000, size: 'full' },
  ],
]

export interface FormularioServicoFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioServicoValues
  onSubmit: (values: FormularioServicoValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioServicoForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioServicoFormProps) {
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
