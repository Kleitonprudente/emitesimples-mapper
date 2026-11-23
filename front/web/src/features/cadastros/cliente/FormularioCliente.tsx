// GERADO por frontend/screenForm a partir de FormularioCliente. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { fkSource } from '@/lib/fk-sources'
import { pessoaSchema, tipoPessoaValues, tipoPessoaLabels, tipoDestinatarioValues, tipoDestinatarioLabels } from '@/gen/schemas/pessoa'
import { contatoSchema, tipoContatoValues, tipoContatoLabels } from '@/gen/schemas/contato'

const SUBMIT_LABEL = 'Salvar'

/** Item de contatos: so os campos que o formulario edita, mais o id do registro existente (ausente nos itens novos). */
const contatoItemSchema = contatoSchema.pick({ tipoContato: true, nome: true, whatsapp: true, email: true }).extend({ id: z.string().optional() })

const schema = pessoaSchema.pick({ documento: true, tipoPessoa: true, consumidorFinal: true, nome: true, nomeFantasia: true, inscricaoMunicipal: true, rgInscricaoEstadual: true, tipoDestinatario: true, fornecedor: true, email: true, telefone: true, cep: true, logradouro: true, numeroEndereco: true, complemento: true, bairro: true, cidade: true, codigoPais: true, codigoPostal: true, nomeMunicipioExterior: true, nomeProvincia: true })
  .extend({ contatos: z.array(contatoItemSchema).max(50) })
export type FormularioClienteValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioClienteFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioClienteValues> = {
  consumidorFinal: true,
  fornecedor: false,
  contatos: [],
}

const FIELDS: FieldLayout = [
  { section: 'Identificação' },
  [
    { name: 'documento', label: 'CNPJ / CPF', maxLength: 14, mask: 'documento', size: 'm', hint: 'Digite o CNPJ ou CPF e clique em consultar: CNPJ preenche os dados pelo CNPJ.ws; CPF define pessoa física e consumidor final.', lookup: 'cnpj' },
    { name: 'tipoPessoa', label: 'Tipo de Pessoa', options: tipoPessoaValues.map((value) => ({ value, label: tipoPessoaLabels[value] })), required: true, size: 'm' },
    { name: 'consumidorFinal', label: 'Consumidor Final', kind: 'boolean', size: 'm' },
  ],
  [
    { name: 'nome', label: 'Nome / Razão Social', maxLength: 300, required: true, size: 'l' },
    { name: 'nomeFantasia', label: 'Nome Fantasia', maxLength: 300, size: 'l' },
  ],
  [
    { name: 'inscricaoMunicipal', label: 'Inscrição Municipal', maxLength: 15, size: 'm' },
    { name: 'rgInscricaoEstadual', label: 'RG / Inscrição Estadual', maxLength: 20, size: 'm' },
    { name: 'tipoDestinatario', label: 'Tipo do Destinatário', options: tipoDestinatarioValues.map((value) => ({ value, label: tipoDestinatarioLabels[value] })), size: 'm' },
  ],
  { section: 'Papel' },
  [
    { name: 'fornecedor', label: 'Também é fornecedor', kind: 'boolean', required: true, size: 'full' },
  ],
  { section: 'Contato principal' },
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
  { section: 'Contatos' },
  [
    {
      name: 'contatos', label: 'Contatos', kind: 'list', size: 'full',
      list: {
        itemLabel: 'Contato', addLabel: 'Adicionar contato', emptyText: 'Nenhum contato adicionado.', maxItems: 50,
        item: [
  [
    { name: 'tipoContato', label: 'Tipo de Contato', options: tipoContatoValues.map((value) => ({ value, label: tipoContatoLabels[value] })), required: true, size: 'm' },
    { name: 'nome', label: 'Nome do Contato', maxLength: 300, required: true, size: 'xl' },
  ],
  [
    { name: 'whatsapp', label: 'WhatsApp', maxLength: 20, mask: 'phone', size: 'm' },
    { name: 'email', label: 'E-mail', maxLength: 60, size: 'xl' },
  ],
        ],
      },
    },
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

export interface FormularioClienteFormProps {
  /** Modo edicao: valores existentes (sem nulos). */
  values?: FormularioClienteValues
  onSubmit: (values: FormularioClienteValues) => void | Promise<void>
  isSubmitting?: boolean
  onCancel?: () => void
  serverError?: unknown
  submitLabel?: string
}

/** Formulario reutilizavel (dialog, pagina ou step): so compoe o AutoForm do runtime. */
export function FormularioClienteForm({ values, onSubmit, isSubmitting, onCancel, serverError, submitLabel }: FormularioClienteFormProps) {
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
