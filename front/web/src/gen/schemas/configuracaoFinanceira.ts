// GERADO por frontend/zodSchema a partir de ConfiguracaoFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const formaPrevistaPadraoReceberValues = ['DINHEIRO', 'PIX', 'BOLETO', 'CARTAO', 'TRANSFERENCIA'] as const
export const formaPrevistaPadraoReceberLabels: Record<string, string> = {
    'DINHEIRO': 'Dinheiro',
    'PIX': 'Pix',
    'BOLETO': 'Boleto',
    'CARTAO': 'Cartão',
    'TRANSFERENCIA': 'Transferência',
}

export const configuracaoFinanceiraSchema = z.object({
    multaPercentual: z.number(),
    jurosMensalPercentual: z.number(),
    carenciaDias: z.number().int(),
    lembretesAtivos: z.boolean(),
    lembreteDiasAntes: z.number().int(),
    lembreteDiasDepois: z.string().max(40).optional(),
    vencimentoPadraoDias: z.number().int(),
    observacaoPadraoReceber: z.string().max(255).optional(),
    formaPrevistaPadraoReceber: z.enum(formaPrevistaPadraoReceberValues).optional(),
    padroesCriadosEm: z.string().optional(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    categoriaPadraoReceita: z.looseObject({ id: z.string() }).optional(),
    categoriaPadraoDespesa: z.looseObject({ id: z.string() }).optional(),
    empresa: z.looseObject({ id: z.string() }).optional(),
})

export type ConfiguracaoFinanceira = z.infer<typeof configuracaoFinanceiraSchema>
