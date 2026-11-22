// GERADO por frontend/screenForm a partir de FormularioEmpresa. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { fkSource } from '@/lib/fk-sources'
import { empresaSchema, regimeTributarioValues, regimeTributarioLabels, regimeIbsCbsValues, regimeIbsCbsLabels, situacaoValues, situacaoLabels } from '@/gen/schemas/empresa'

const SUBMIT_LABEL = 'Salvar'

const schema = empresaSchema.pick({ razaoSocial: true, nomeFantasia: true, documento: true, inscricaoMunicipal: true, rgInscricaoEstadual: true, email: true, whatsapp: true, telefone: true, cep: true, logradouro: true, numeroEndereco: true, complemento: true, bairro: true, municipio: true, regimeTributario: true, regimeIbsCbs: true, situacao: true, urlWebhook: true })
export type FormularioEmpresaValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioEmpresaFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioEmpresaValues> = {
  situacao: 'pendente_confirmacao',
}

const FIELDS: FieldLayout = [
  { section: 'Identificação' },
  [
    { name: 'razaoSocial', label: 'Razão Social', maxLength: 60, required: true, size: 'l' },
    { name: 'nomeFantasia', label: 'Nome Fantasia', maxLength: 60, required: true, size: 'l' },
  ],
  [
    { name: 'documento', label: 'CNPJ / CPF', maxLength: 14, required: true, size: 'm' },
    { name: 'inscricaoMunicipal', label: 'Inscrição Municipal', maxLength: 20, size: 'm' },
    { name: 'rgInscricaoEstadual', label: 'RG / Inscrição Estadual', maxLength: 20, size: 'm' },
  ],
  { section: 'Contato' },
  [
    { name: 'email', label: 'E-mail', maxLength: 60, required: true, size: 'l' },
    { name: 'whatsapp', label: 'WhatsApp', maxLength: 20, required: true, mask: 'phone', size: 's' },
    { name: 'telefone', label: 'Telefone', maxLength: 14, required: true, mask: 'phone', size: 's' },
  ],
  { section: 'Endereço' },
  [
    { name: 'cep', label: 'CEP', maxLength: 8, required: true, mask: 'postalCode', size: 's', hint: 'Ao sair do campo, o endereço é preenchido.', lookup: 'cep' },
    { name: 'logradouro', label: 'Logradouro', maxLength: 60, required: true, size: 'l' },
    { name: 'numeroEndereco', label: 'Número do Endereço', maxLength: 60, required: true, size: 's' },
  ],
  [
    { name: 'complemento', label: 'Complemento', maxLength: 60, size: 'm' },
    { name: 'bairro', label: 'Bairro', maxLength: 60, required: true, size: 'm' },
    { name: 'municipio', label: 'Município', fk: fkSource('Municipio'), required: true, size: 'm' },
  ],
  { section: 'Tributação' },
  [
    { name: 'regimeTributario', label: 'Regime Tributário', options: regimeTributarioValues.map((value) => ({ value, label: regimeTributarioLabels[value] })), required: true, size: 'l' },
    { name: 'regimeIbsCbs', label: 'Regime do IBS/CBS', options: regimeIbsCbsValues.map((value) => ({ value, label: regimeIbsCbsLabels[value] })), size: 'l' },
  ],
  { section: 'Conta' },
  [
    { name: 'situacao', label: 'Situação da Conta', options: situacaoValues.map((value) => ({ value, label: situacaoLabels[value] })), required: true, size: 'm' },
    { name: 'urlWebhook', label: 'URL do Webhook', maxLength: 200, size: 'xl' },
  ],
]

export interface FormularioEmpresaFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioEmpresaValues
  onSubmit: (values: FormularioEmpresaValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioEmpresaForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioEmpresaFormProps) {
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
