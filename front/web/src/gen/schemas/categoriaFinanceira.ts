// GERADO por frontend/zodSchema a partir de CategoriaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const tipoValues = ['RECEITA', 'DESPESA'] as const
export const tipoLabels: Record<string, string> = {
    'RECEITA': 'Receita',
    'DESPESA': 'Despesa',
}

export const categoriaFinanceiraSchema = z.object({
    nome: z.string().max(100),
    tipo: z.enum(tipoValues),
    dicas: z.string().max(500).optional(),
    ativo: z.boolean(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    categoriaPai: z.looseObject({ id: z.string() }).optional(),
    empresa: z.looseObject({ id: z.string() }).optional(),
})

export type CategoriaFinanceira = z.infer<typeof categoriaFinanceiraSchema>
