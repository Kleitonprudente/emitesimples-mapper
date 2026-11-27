// GERADO por frontend/zodSchema a partir de CondicaoPagamento. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const formaValues = ['DINHEIRO', 'PIX', 'BOLETO', 'CARTAO', 'TRANSFERENCIA'] as const
export const formaLabels: Record<string, string> = {
    'DINHEIRO': 'Dinheiro',
    'PIX': 'Pix',
    'BOLETO': 'Boleto',
    'CARTAO': 'Cartão',
    'TRANSFERENCIA': 'Transferência',
}

export const condicaoPagamentoSchema = z.object({
    nome: z.string().max(100),
    forma: z.enum(formaValues),
    ativo: z.boolean(),
    parcelas: z.number().int(),
    diasPrimeiroVencimento: z.number().int(),
    intervaloDias: z.number().int(),
    receberNaEmissao: z.boolean(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    empresa: z.looseObject({ id: z.string() }).optional(),
})

export type CondicaoPagamento = z.infer<typeof condicaoPagamentoSchema>
