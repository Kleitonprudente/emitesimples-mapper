// GERADO por frontend/zodSchema a partir de Contato. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const tipoContatoValues = ['socio', 'financeiro', 'comercial', 'fiscal', 'outro'] as const
export const tipoContatoLabels: Record<string, string> = {
    'socio': 'Sócio ou administrador (vindo do QSA do CNPJ)',
    'financeiro': 'Contato do financeiro',
    'comercial': 'Contato comercial',
    'fiscal': 'Contato fiscal ou contábil',
    'outro': 'Outro tipo de contato',
}

export const contatoSchema = z.object({
    tipoContato: z.enum(tipoContatoValues),
    nome: z.string().max(300),
    whatsapp: z.string().max(20).optional(),
    email: z.string().max(60).optional(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    pessoa: z.looseObject({ id: z.string() }),
    empresa: z.looseObject({ id: z.string() }).optional(),
})

export type Contato = z.infer<typeof contatoSchema>
