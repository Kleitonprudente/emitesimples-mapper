// GERADO por frontend/zodSchema a partir de Banco. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const bancoSchema = z.object({
    codigo: z.string().max(4),
    ispb: z.string().max(8).optional(),
    nome: z.string().max(120),
    nomeExtenso: z.string().max(200).optional(),
    participaCompe: z.boolean(),
    integradoGateway: z.boolean(),
    urlPassoAPasso: z.string().max(500).optional(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
})

export type Banco = z.infer<typeof bancoSchema>
