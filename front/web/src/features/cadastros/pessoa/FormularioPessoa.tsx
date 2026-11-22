// GERADO por frontend/screenForm a partir de FormularioPessoa. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { fkSource } from '@/lib/fk-sources'
import { pessoaSchema, tipoPessoaValues, tipoPessoaLabels, tipoDestinatarioValues, tipoDestinatarioLabels } from '@/gen/schemas/pessoa'

const SUBMIT_LABEL = 'Salvar'

const schema = pessoaSchema.pick({ tipoPessoa: true, documento: true, inscricaoMunicipal: true, rgInscricaoEstadual: true, nome: true, nomeFantasia: true, tipoDestinatario: true, consumidorFinal: true, email: true, telefone: true, cep: true, logradouro: true, numeroEndereco: true, complemento: true, bairro: true, cidade: true, codigoPais: true, codigoPostal: true, nomeMunicipioExterior: true, nomeProvincia: true })
export type FormularioPessoaValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioPessoaFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioPessoaValues> = {
  consumidorFinal: true,
}

const FIELDS: FieldLayout = [
  { section: 'Identificação' },
  [
    { name: 'tipoPessoa', label: 'Tipo de Pessoa', options: tipoPessoaValues.map((value) => ({ value, label: tipoPessoaLabels[value] })), required: true, size: 's' },
    { name: 'documento', label: 'CNPJ / CPF', maxLength: 14, size: 's' },
    { name: 'inscricaoMunicipal', label: 'Inscrição Municipal', maxLength: 15, size: 's' },
    { name: 'rgInscricaoEstadual', label: 'RG / Inscrição Estadual', maxLength: 20, size: 's' },
  ],
  [
    { name: 'nome', label: 'Nome / Razão Social', maxLength: 300, required: true, size: 'l' },
    { name: 'nomeFantasia', label: 'Nome Fantasia', maxLength: 300, size: 'l' },
  ],
  [
    { name: 'tipoDestinatario', label: 'Tipo do Destinatário', options: tipoDestinatarioValues.map((value) => ({ value, label: tipoDestinatarioLabels[value] })), size: 'l' },
    { name: 'consumidorFinal', label: 'Consumidor Final', kind: 'boolean', size: 'l' },
  ],
  { section: 'Contato' },
  [
    { name: 'email', label: 'E-mail', maxLength: 60, size: 'xl' },
    { name: 'telefone', label: 'Telefone', maxLength: 20, mask: 'phone', size: 'm' },
  ],
  { section: 'Endereço' },
  [
    { name: 'cep', label: 'CEP', maxLength: 8, mask: 'postalCode', size: 's', hint: 'Ao sair do campo, o endereço é preenchido.', lookup: 'cep' },
    { name: 'logradouro', label: 'Logradouro', maxLength: 255, required: true, size: 'l' },
    { name: 'numeroEndereco', label: 'Número do Endereço', maxLength: 60, required: true, size: 's' },
  ],
  [
    { name: 'complemento', label: 'Complemento', maxLength: 256, size: 'm' },
    { name: 'bairro', label: 'Bairro', maxLength: 60, required: true, size: 'm' },
    { name: 'cidade', label: 'Município', fk: fkSource('Municipio'), size: 'm' },
  ],
  { section: 'Pessoa estrangeira (exportação)', collapsible: true },
  [
    { name: 'codigoPais', label: 'Código do País', maxLength: 4, size: 'm' },
    { name: 'codigoPostal', label: 'Código Postal do Exterior', maxLength: 11, size: 'xl' },
  ],
  [
    { name: 'nomeMunicipioExterior', label: 'Município no Exterior', maxLength: 60, size: 'l' },
    { name: 'nomeProvincia', label: 'Província no Exterior', maxLength: 60, size: 'l' },
  ],
]

export interface FormularioPessoaFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioPessoaValues
  onSubmit: (values: FormularioPessoaValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioPessoaForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioPessoaFormProps) {
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
