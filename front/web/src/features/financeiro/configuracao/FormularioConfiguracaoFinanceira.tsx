// GERADO por frontend/screenForm a partir de FormularioConfiguracaoFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { fkSource } from '@/lib/fk-sources'
import { configuracaoFinanceiraSchema, formaPrevistaPadraoReceberValues, formaPrevistaPadraoReceberLabels } from '@/gen/schemas/configuracaoFinanceira'

const SUBMIT_LABEL = 'Salvar'

const schema = configuracaoFinanceiraSchema.pick({ multaPercentual: true, jurosMensalPercentual: true, carenciaDias: true, lembretesAtivos: true, lembreteDiasAntes: true, lembreteDiasDepois: true, vencimentoPadraoDias: true, formaPrevistaPadraoReceber: true, categoriaPadraoReceita: true, categoriaPadraoDespesa: true, observacaoPadraoReceber: true })
export type FormularioConfiguracaoFinanceiraValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioConfiguracaoFinanceiraFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioConfiguracaoFinanceiraValues> = {
  multaPercentual: 2.00,
  jurosMensalPercentual: 1.00,
  carenciaDias: 2,
  lembretesAtivos: false,
  lembreteDiasAntes: 3,
  lembreteDiasDepois: '1,7,15',
  vencimentoPadraoDias: 0,
}

const FIELDS: FieldLayout = [
  { section: 'Multa e juros dos atrasos', description: 'Valem para os títulos a receber; cada título pode ter os seus.' },
  [
    { name: 'multaPercentual', label: 'Multa (%)', kind: 'number', required: true, size: 'm', hint: 'Multa única sobre o valor pago com atraso. Ex.: 2,00.' },
    { name: 'jurosMensalPercentual', label: 'Juros ao Mês (%)', kind: 'number', required: true, size: 'm', hint: 'Juros simples ao mês. Ex.: 1,00.' },
    { name: 'carenciaDias', label: 'Carência (dias)', kind: 'number', required: true, size: 'm', hint: 'Até o vencimento mais estes dias, não se cobra nada.' },
  ],
  { section: 'Lembretes de cobrança', description: 'E-mails automáticos ao cliente, antes e depois do vencimento.' },
  [
    { name: 'lembretesAtivos', label: 'Lembretes Automáticos', kind: 'boolean', required: true, size: 'm' },
    { name: 'lembreteDiasAntes', label: 'Avisar Dias Antes', kind: 'number', required: true, size: 'm', hint: 'De 0 a 30.' },
    { name: 'lembreteDiasDepois', label: 'Avisar Dias Depois', maxLength: 40, size: 'm', hint: 'Até 5 números separados por vírgula. Ex.: 1,7,15.' },
  ],
  { section: 'Padrões do título novo', description: 'Só preenchem o formulário de um título novo; a pessoa pode mudar.' },
  [
    { name: 'vencimentoPadraoDias', label: 'Vencimento Padrão (dias)', kind: 'number', required: true, size: 'l', hint: 'Só a receber: vencimento = hoje mais estes dias.' },
    { name: 'formaPrevistaPadraoReceber', label: 'Forma Prevista Padrão a Receber', options: formaPrevistaPadraoReceberValues.map((value) => ({ value, label: formaPrevistaPadraoReceberLabels[value] })), size: 'l' },
  ],
  [
    { name: 'categoriaPadraoReceita', label: 'Categoria Padrão de Receita', fk: fkSource('CategoriaFinanceira'), size: 'l', hint: 'Categoria de receita sem filhas.' },
    { name: 'categoriaPadraoDespesa', label: 'Categoria Padrão de Despesa', fk: fkSource('CategoriaFinanceira'), size: 'l', hint: 'Categoria de despesa sem filhas.' },
  ],
  [
    { name: 'observacaoPadraoReceber', label: 'Observação Padrão a Receber', maxLength: 255, size: 'full', hint: 'Instruções de pagamento do título a receber.' },
  ],
]

export interface FormularioConfiguracaoFinanceiraFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioConfiguracaoFinanceiraValues
  onSubmit: (values: FormularioConfiguracaoFinanceiraValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioConfiguracaoFinanceiraForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioConfiguracaoFinanceiraFormProps) {
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
