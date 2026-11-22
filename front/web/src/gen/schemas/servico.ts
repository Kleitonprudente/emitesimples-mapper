// GERADO por frontend/zodSchema a partir de Servico. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const tipoOperacaoIssValues = [1, 2, 3, 4] as const
export const tipoOperacaoIssLabels: Record<string, string> = {
    '1': 'Tributável',
    '2': 'Imunidade',
    '3': 'Exportação',
    '4': 'Não incidência',
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
    cstPisCofins: z.string().max(2).optional(),
    percentualTributosSimples: z.number().optional(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    empresa: z.looseObject({ id: z.string() }).optional(),
})

export type Servico = z.infer<typeof servicoSchema>
