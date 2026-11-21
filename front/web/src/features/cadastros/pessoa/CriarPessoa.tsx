// GERADO por frontend/screenForm a partir de CriarPessoa. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { pessoaSchema, tipoPessoaValues, tipoPessoaLabels } from '@/gen/schemas/pessoa'

const SUBMIT_LABEL = 'Salvar'

const schema = pessoaSchema.pick({ tipoPessoa: true, documento: true, nome: true, email: true, telefone: true, cep: true, logradouro: true, numeroEndereco: true, bairro: true })
export type CriarPessoaValues = z.infer<typeof schema>

const DEFAULTS: Partial<CriarPessoaValues> = {
}

const FIELDS: FieldLayout = [
  [
    { name: 'tipoPessoa', label: 'Tipo de Pessoa', options: tipoPessoaValues.map((value) => ({ value, label: tipoPessoaLabels[value] })), required: true },
    { name: 'documento', label: 'CNPJ / CPF', maxLength: 14 },
  ],
  [
    { name: 'nome', label: 'Nome / Razão Social', maxLength: 300, required: true },
  ],
  [
    { name: 'email', label: 'E-mail', maxLength: 60 },
    { name: 'telefone', label: 'Telefone', maxLength: 20, mask: 'phone' },
  ],
  [
    { name: 'cep', label: 'CEP', maxLength: 8, mask: 'postalCode', width: 30 },
    { name: 'logradouro', label: 'Logradouro', maxLength: 255, required: true, width: 70 },
  ],
  [
    { name: 'numeroEndereco', label: 'Número do Endereço', maxLength: 60, required: true, width: 30 },
    { name: 'bairro', label: 'Bairro', maxLength: 60, required: true, width: 70 },
  ],
]

export interface CriarPessoaFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: CriarPessoaValues
  onSubmit: (values: CriarPessoaValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function CriarPessoaForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: CriarPessoaFormProps) {
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
