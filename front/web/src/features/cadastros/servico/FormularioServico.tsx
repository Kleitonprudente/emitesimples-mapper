// GERADO por frontend/screenForm a partir de FormularioServico. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { servicoSchema, cstPisCofinsValues, cstPisCofinsLabels, tipoRetencaoValues, tipoRetencaoLabels } from '@/gen/schemas/servico'

const SUBMIT_LABEL = 'Salvar'

const schema = servicoSchema.pick({ nome: true, ativo: true, codigoServico: true, codigoNbs: true, percentualTributosSimples: true, valorPadrao: true, discriminacaoPadrao: true, issRetido: true, aliquotaIss: true, codigoTributacaoMunicipio: true, cstPisCofins: true, tipoRetencao: true, aliquotaPis: true, aliquotaCofins: true })
export type FormularioServicoValues = z.infer<typeof schema>
/** Campos que o formulario edita; o resto do registro e preservado no PUT (que substitui o registro inteiro). */
export const FormularioServicoFieldNames: readonly string[] = Object.keys(schema.shape)

const DEFAULTS: Partial<FormularioServicoValues> = {
  ativo: true,
  issRetido: false,
}

const FIELDS: FieldLayout = [
  { section: 'Serviço' },
  [
    { name: 'nome', label: 'Nome do serviço', maxLength: 100, required: true, size: 'xl' },
    { name: 'ativo', label: 'Ativo', kind: 'boolean', required: true, size: 'm' },
  ],
  [
    { name: 'codigoServico', label: 'Código de tributação nacional', maxLength: 6, required: true, size: 's', hint: '6 dígitos, ex.: 010401.' },
    { name: 'codigoNbs', label: 'Código NBS', maxLength: 9, size: 's', hint: '9 dígitos, ex.: 115021000.' },
    { name: 'percentualTributosSimples', label: 'Tributos aprox. Simples (%)', kind: 'number', size: 's', hint: 'Ex.: 13,75.' },
    { name: 'valorPadrao', label: 'Valor padrão (R$)', kind: 'number', size: 's', hint: 'Sugerido na nota; ela pode mudar.' },
  ],
  [
    { name: 'discriminacaoPadrao', label: 'Descrição do serviço para a nota', maxLength: 2000, size: 'full' },
  ],
  { section: 'ISS da prefeitura (opcional)', description: 'Padrões que a nota copia ao escolher este serviço. Tudo opcional: em branco, a nota sai sem o ISS. Confirme com seu contador.', collapsible: true },
  [
    { name: 'issRetido', label: 'O cliente retém o ISS', kind: 'boolean', required: true, size: 'm' },
    { name: 'aliquotaIss', label: 'Alíquota do ISS (%)', kind: 'number', size: 'm', hint: 'Ex.: 5,00. No Simples Nacional, só vai na nota quando o cliente retém o ISS (regra do governo). Obrigatória com ISS retido.' },
    { name: 'codigoTributacaoMunicipio', label: 'Código de tributação municipal', maxLength: 3, size: 'm', hint: '3 dígitos, só se a sua prefeitura tiver código próprio; se não tiver, deixe em branco (o governo recusa o 000).' },
  ],
  { section: 'Impostos federais retidos pelo cliente (opcional)', description: 'Ex.: uma empresa contrata seu serviço de R$ 1.000,00 e te paga R$ 890,00, porque descontou R$ 110,00 de INSS (11%). Os valores retidos são informados na nota. Confirme com seu contador.', collapsible: true },
  [
    { name: 'cstPisCofins', label: 'CST do PIS/COFINS', options: cstPisCofinsValues.map((value) => ({ value, label: cstPisCofinsLabels[value] })), size: 'l', hint: 'Obrigatório com tipo de retenção ou alíquota de PIS/COFINS. Confirme o código com seu contador.' },
    { name: 'tipoRetencao', label: 'Tipo de retenção', options: tipoRetencaoValues.map((value) => ({ value, label: tipoRetencaoLabels[value] })), size: 'l', hint: 'Em branco, vale "PIS/COFINS não retidos".' },
  ],
  [
    { name: 'aliquotaPis', label: 'Alíquota do PIS (%)', kind: 'number', size: 'l' },
    { name: 'aliquotaCofins', label: 'Alíquota da COFINS (%)', kind: 'number', size: 'l' },
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
