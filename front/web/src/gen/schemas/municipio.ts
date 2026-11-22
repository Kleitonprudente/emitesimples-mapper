// GERADO por frontend/zodSchema a partir de Municipio. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const municipioSchema = z.object({
    codigoIbge: z.string().max(7),
    nome: z.string().max(100),
    uf: z.string().max(2),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
})

export type Municipio = z.infer<typeof municipioSchema>
