// GERADO por frontend/screenForm a partir de EditarPessoa. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { pessoaSchema, tipoPessoaValues, tipoPessoaLabels } from '@/gen/schemas/pessoa'

const SUBMIT_LABEL = 'Salvar'

const schema = pessoaSchema.pick({ tipoPessoa: true, documento: true, nome: true, nomeFantasia: true, inscricaoMunicipal: true, rgInscricaoEstadual: true, tipoDestinatario: true, consumidorFinal: true, email: true, telefone: true, cep: true, logradouro: true, numeroEndereco: true, complemento: true, bairro: true, codigoPais: true, codigoPostal: true, nomeMunicipioExterior: true, nomeProvincia: true })
export type EditarPessoaValues = z.infer<typeof schema>

const DEFAULTS: Partial<EditarPessoaValues> = {
  consumidorFinal: true,
}

const FIELDS: FieldLayout = [
  [
    { name: 'tipoPessoa', label: 'Tipo de Pessoa', options: tipoPessoaValues.map((value) => ({ value, label: tipoPessoaLabels[value] })), required: true },
    { name: 'documento', label: 'CNPJ / CPF', maxLength: 14 },
  ],
  [
    { name: 'nome', label: 'Nome / Razão Social', maxLength: 300, required: true, width: 60 },
    { name: 'nomeFantasia', label: 'Nome Fantasia', maxLength: 300, width: 40 },
  ],
  [
    { name: 'inscricaoMunicipal', label: 'Inscrição Municipal', maxLength: 15 },
    { name: 'rgInscricaoEstadual', label: 'RG / Inscrição Estadual', maxLength: 20 },
  ],
  [
    // @TODO Pessoa.tipoDestinatario: enum numerico ainda sem select tipado no runtime (FieldSpec.options so aceita string); campo omitido
    { name: 'consumidorFinal', label: 'Consumidor Final', kind: 'boolean' },
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
    { name: 'numeroEndereco', label: 'Número do Endereço', maxLength: 60, required: true, width: 25 },
    { name: 'complemento', label: 'Complemento', maxLength: 256, width: 75 },
  ],
  [
    { name: 'bairro', label: 'Bairro', maxLength: 60, required: true },
  ],
  [
    { name: 'codigoPais', label: 'Código do País', maxLength: 4 },
    { name: 'codigoPostal', label: 'Código Postal do Exterior', maxLength: 11 },
  ],
  [
    { name: 'nomeMunicipioExterior', label: 'Município no Exterior', maxLength: 60 },
    { name: 'nomeProvincia', label: 'Província no Exterior', maxLength: 60 },
  ],
]

export interface EditarPessoaFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: EditarPessoaValues
  onSubmit: (values: EditarPessoaValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function EditarPessoaForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: EditarPessoaFormProps) {
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
