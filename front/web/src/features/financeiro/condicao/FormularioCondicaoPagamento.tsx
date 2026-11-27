// GERADO por frontend/screenForm a partir de FormularioCondicaoPagamento. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { condicaoPagamentoSchema, formaValues, formaLabels } from '@/gen/schemas/condicaoPagamento'

const SUBMIT_LABEL = 'Salvar'

const schema = condicaoPagamentoSchema.pick({ nome: true, forma: true, ativo: true, parcelas: true, diasPrimeiroVencimento: true, intervaloDias: true, receberNaEmissao: true })
export type FormularioCondicaoPagamentoValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioCondicaoPagamentoFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioCondicaoPagamentoValues> = {
  ativo: true,
  parcelas: 1,
  diasPrimeiroVencimento: 0,
  intervaloDias: 30,
  receberNaEmissao: false,
}

const FIELDS: FieldLayout = [
  { section: 'Condição' },
  [
    { name: 'nome', label: 'Nome', maxLength: 100, required: true, size: 'l', hint: 'Ex.: Boleto 3x (30/60/90).' },
    { name: 'forma', label: 'Forma de Pagamento', options: formaValues.map((value) => ({ value, label: formaLabels[value] })), required: true, size: 'm' },
    { name: 'ativo', label: 'Ativa', kind: 'boolean', required: true, size: 'xs' },
  ],
  { section: 'Parcelamento' },
  [
    { name: 'parcelas', label: 'Parcelas', kind: 'number', required: true, size: 's', hint: 'De 1 a 48.' },
    { name: 'diasPrimeiroVencimento', label: 'Dias até a 1ª Parcela', kind: 'number', required: true, size: 's', hint: '0 = à vista.' },
    { name: 'intervaloDias', label: 'Intervalo entre Parcelas (dias)', kind: 'number', required: true, size: 's', hint: 'De 0 a 365.' },
    { name: 'receberNaEmissao', label: 'Receber na Emissão', kind: 'boolean', required: true, size: 's', hint: 'Só com 1 parcela e 0 dias; exige conta padrão da forma.' },
  ],
]

export interface FormularioCondicaoPagamentoFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioCondicaoPagamentoValues
  onSubmit: (values: FormularioCondicaoPagamentoValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioCondicaoPagamentoForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioCondicaoPagamentoFormProps) {
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
