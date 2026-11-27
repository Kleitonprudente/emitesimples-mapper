// GERADO por frontend/screenForm a partir de FormularioCategoriaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { fkSource } from '@/lib/fk-sources'
import { categoriaFinanceiraSchema, tipoValues, tipoLabels } from '@/gen/schemas/categoriaFinanceira'

const SUBMIT_LABEL = 'Salvar'

const schema = categoriaFinanceiraSchema.pick({ nome: true, tipo: true, ativo: true, categoriaPai: true, dicas: true })
export type FormularioCategoriaFinanceiraValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioCategoriaFinanceiraFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioCategoriaFinanceiraValues> = {
  ativo: true,
}

const FIELDS: FieldLayout = [
  { section: 'Categoria' },
  [
    { name: 'nome', label: 'Nome', maxLength: 100, required: true, size: 'l' },
    { name: 'tipo', label: 'Tipo', options: tipoValues.map((value) => ({ value, label: tipoLabels[value] })), required: true, size: 'm' },
    { name: 'ativo', label: 'Ativa', kind: 'boolean', required: true, size: 'xs' },
  ],
  [
    { name: 'categoriaPai', label: 'Categoria Pai', fk: fkSource('CategoriaFinanceira'), size: 'full', hint: 'Opcional. Uma categoria de topo, ativa e do mesmo tipo. A categoria pai só agrupa: lançamentos usam as filhas.' },
  ],
  [
    { name: 'dicas', label: 'Para Quais Contas é Usada', maxLength: 500, size: 'full', hint: 'Palavras separadas por vírgula, usadas para sugerir a categoria pela descrição. Não escreva dado pessoal.' },
  ],
]

export interface FormularioCategoriaFinanceiraFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioCategoriaFinanceiraValues
  onSubmit: (values: FormularioCategoriaFinanceiraValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioCategoriaFinanceiraForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioCategoriaFinanceiraFormProps) {
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
