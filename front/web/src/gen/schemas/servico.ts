// GERADO por frontend/zodSchema a partir de Servico. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const tipoOperacaoIssValues = [1, 2, 3, 4] as const
export const tipoOperacaoIssLabels: Record<string, string> = {
    '1': 'Tributável',
    '2': 'Imunidade',
    '3': 'Exportação',
    '4': 'Não incidência',
}

export const cstPisCofinsValues = ['01', '02', '03', '04', '05', '06', '07', '08', '09', '49', '50', '51', '52', '53', '54', '55', '56', '60', '61', '62', '63', '64', '65', '66', '67', '70', '71', '72', '73', '74', '75', '98', '99'] as const
export const cstPisCofinsLabels: Record<string, string> = {
    '01': 'Alíquota normal',
    '02': 'Alíquota diferenciada',
    '03': 'Alíquota por unidade',
    '04': 'Monofásica (alíquota zero)',
    '05': 'Substituição tributária',
    '06': 'Alíquota zero',
    '07': 'Isenta da contribuição',
    '08': 'Sem incidência',
    '09': 'Com suspensão',
    '49': 'Outras operações de saída',
    '50': 'Crédito: tributada no mercado interno',
    '51': 'Crédito: não tributada no mercado interno',
    '52': 'Crédito: exportação',
    '53': 'Crédito: tributada e não tributada no mercado interno',
    '54': 'Crédito: tributada no mercado interno e exportação',
    '55': 'Crédito: não tributada no mercado interno e exportação',
    '56': 'Crédito: tributada e não tributada no mercado interno e exportação',
    '60': 'Crédito presumido: tributada no mercado interno',
    '61': 'Crédito presumido: não tributada no mercado interno',
    '62': 'Crédito presumido: exportação',
    '63': 'Crédito presumido: tributada e não tributada no mercado interno',
    '64': 'Crédito presumido: tributada no mercado interno e exportação',
    '65': 'Crédito presumido: não tributada no mercado interno e exportação',
    '66': 'Crédito presumido: tributada e não tributada no mercado interno e exportação',
    '67': 'Crédito presumido: outras operações',
    '70': 'Aquisição sem direito a crédito',
    '71': 'Aquisição com isenção',
    '72': 'Aquisição com suspensão',
    '73': 'Aquisição a alíquota zero',
    '74': 'Aquisição sem incidência',
    '75': 'Aquisição por substituição tributária',
    '98': 'Outras operações de entrada',
    '99': 'Outras operações',
}

export const tipoRetencaoValues = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9] as const
export const tipoRetencaoLabels: Record<string, string> = {
    '0': 'PIS/COFINS/CSLL não retidos',
    '1': 'PIS/COFINS retido',
    '2': 'PIS/COFINS não retido',
    '3': 'PIS/COFINS/CSLL retidos',
    '4': 'PIS/COFINS retidos, CSLL não retido',
    '5': 'PIS retido, COFINS/CSLL não retidos',
    '6': 'COFINS retido, PIS/CSLL não retidos',
    '7': 'PIS não retido, COFINS/CSLL retidos',
    '8': 'PIS/COFINS não retidos, CSLL retido',
    '9': 'COFINS não retido, PIS/CSLL retidos',
}

export const servicoSchema = z.object({
    nome: z.string().max(100),
    codigoInterno: z.string().max(20).optional(),
    ativo: z.boolean(),
    codigoServico: z.string().max(6),
    codigoTributacaoMunicipio: z.string().max(3).optional(),
    codigoNbs: z.string().max(9).optional(),
    descricaoCurta: z.string().max(200).optional(),
    discriminacaoPadrao: z.string().max(2000).optional(),
    valorPadrao: z.number().optional(),
    aliquotaIss: z.number().optional(),
    issRetido: z.boolean(),
    tipoOperacaoIss: z.number().int().refine((v) => (tipoOperacaoIssValues as readonly number[]).includes(v)),
    cstPisCofins: z.enum(cstPisCofinsValues).optional(),
    tipoRetencao: z.number().int().refine((v) => (tipoRetencaoValues as readonly number[]).includes(v)).optional(),
    aliquotaPis: z.number().optional(),
    aliquotaCofins: z.number().optional(),
    percentualTributosSimples: z.number().optional(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    empresa: z.looseObject({ id: z.string() }).optional(),
})

export type Servico = z.infer<typeof servicoSchema>
