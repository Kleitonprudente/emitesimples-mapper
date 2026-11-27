// GERADO por frontend/screenForm a partir de FormularioContaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { contaFinanceiraSchema, tipoValues, tipoLabels } from '@/gen/schemas/contaFinanceira'

const SUBMIT_LABEL = 'Salvar'

const schema = contaFinanceiraSchema.pick({ nome: true, tipo: true, ativo: true, saldoInicial: true, dataSaldoInicial: true, codigoBanco: true, agencia: true, dadosDoBanco: true, padraoDinheiro: true, padraoPix: true, padraoBoleto: true, padraoCartao: true, padraoTransferencia: true })
export type FormularioContaFinanceiraValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioContaFinanceiraFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioContaFinanceiraValues> = {
  ativo: true,
  saldoInicial: 0,
  padraoDinheiro: false,
  padraoPix: false,
  padraoBoleto: false,
  padraoCartao: false,
  padraoTransferencia: false,
}

const FIELDS: FieldLayout = [
  { section: 'Conta' },
  [
    { name: 'nome', label: 'Nome da conta', maxLength: 100, required: true, size: 'l', hint: 'Ex.: Caixa da loja, Banco do Brasil, Carteira Pix.' },
    { name: 'tipo', label: 'Tipo', options: tipoValues.map((value) => ({ value, label: tipoLabels[value] })), required: true, size: 'm' },
    { name: 'ativo', label: 'Ativa', kind: 'boolean', required: true, size: 'xs' },
  ],
  [
    { name: 'saldoInicial', label: 'Saldo inicial (R$)', kind: 'number', required: true, size: 'l', hint: 'Pode ser negativo (cheque especial). Não muda depois do primeiro movimento.' },
    { name: 'dataSaldoInicial', label: 'Data do saldo inicial', kind: 'date', required: true, size: 'l', hint: 'Não pode ser futura. Não muda depois do primeiro movimento.' },
  ],
  { section: 'Dados bancários', description: 'Opcional. Os dados para pagamento aparecem no e-mail de cobrança.' },
  [
    { name: 'codigoBanco', label: 'Código do Banco', maxLength: 4, size: 's', hint: 'Código do Banco Central, sem zeros à esquerda (ex.: 1, 341).' },
    { name: 'agencia', label: 'Agência', maxLength: 10, size: 's' },
    { name: 'dadosDoBanco', label: 'Dados para Pagamento', maxLength: 150, size: 'l', hint: 'Número da conta, chave Pix etc.' },
  ],
  { section: 'Conta padrão por forma de pagamento', description: 'Cada forma tem no máximo uma conta padrão: marcar esta desmarca a anterior.' },
  [
    { name: 'padraoDinheiro', label: 'Padrão para Dinheiro', kind: 'boolean', required: true, size: 'm' },
    { name: 'padraoPix', label: 'Padrão para Pix', kind: 'boolean', required: true, size: 'm' },
    { name: 'padraoBoleto', label: 'Padrão para Boleto', kind: 'boolean', required: true, size: 'm' },
  ],
  [
    { name: 'padraoCartao', label: 'Padrão para Cartão', kind: 'boolean', required: true, size: 'l' },
    { name: 'padraoTransferencia', label: 'Padrão para Transferência', kind: 'boolean', required: true, size: 'l' },
  ],
]

export interface FormularioContaFinanceiraFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioContaFinanceiraValues
  onSubmit: (values: FormularioContaFinanceiraValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioContaFinanceiraForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioContaFinanceiraFormProps) {
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
