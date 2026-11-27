// GERADO por frontend/zodSchema a partir de ContaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const tipoValues = ['CAIXA', 'BANCO', 'CARTEIRA'] as const
export const tipoLabels: Record<string, string> = {
    'CAIXA': 'Caixa',
    'BANCO': 'Conta em banco',
    'CARTEIRA': 'Carteira digital',
}

export const contaFinanceiraSchema = z.object({
    nome: z.string().max(100),
    tipo: z.enum(tipoValues),
    ativo: z.boolean(),
    codigoBanco: z.string().max(4).optional(),
    agencia: z.string().max(10).optional(),
    dadosDoBanco: z.string().max(150).optional(),
    saldoInicial: z.number(),
    dataSaldoInicial: z.string(),
    padraoDinheiro: z.boolean(),
    padraoPix: z.boolean(),
    padraoBoleto: z.boolean(),
    padraoCartao: z.boolean(),
    padraoTransferencia: z.boolean(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    empresa: z.looseObject({ id: z.string() }).optional(),
})

export type ContaFinanceira = z.infer<typeof contaFinanceiraSchema>
