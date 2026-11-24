// GERADO por frontend/zodSchema a partir de Pessoa. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const tipoPessoaValues = ['FISICA', 'JURIDICA'] as const
export const tipoPessoaLabels: Record<string, string> = {
    'FISICA': 'Pessoa física',
    'JURIDICA': 'Pessoa jurídica',
}

export const tipoDestinatarioValues = [0, 1] as const
export const tipoDestinatarioLabels: Record<string, string> = {
    '0': 'Próprio tomador',
    '1': 'Outro destinatário',
}

export const pessoaSchema = z.object({
    tipoPessoa: z.enum(tipoPessoaValues),
    documento: z.string().max(14).optional(),
    nome: z.string().max(300),
    nomeFantasia: z.string().max(300).optional(),
    inscricaoMunicipal: z.string().max(15).optional(),
    rgInscricaoEstadual: z.string().max(20).optional(),
    tipoDestinatario: z.number().int().refine((v) => (tipoDestinatarioValues as readonly number[]).includes(v)).optional(),
    consumidorFinal: z.boolean().optional(),
    cliente: z.boolean(),
    fornecedor: z.boolean(),
    telefone: z.string().max(20).optional(),
    email: z.string().max(60).optional(),
    logradouro: z.string().max(255),
    numeroEndereco: z.string().max(60),
    complemento: z.string().max(256).optional(),
    bairro: z.string().max(60),
    cep: z.string().max(8).optional(),
    codigoPais: z.string().max(4).optional(),
    codigoPostal: z.string().max(11).optional(),
    nomeMunicipioExterior: z.string().max(60).optional(),
    nomeProvincia: z.string().max(60).optional(),
    id: z.string().max(36),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    empresa: z.looseObject({ id: z.string() }).optional(),
    cidade: z.looseObject({ id: z.string() }).optional(),
    // relacao to-many omitida do schema: contatos (Contato, oneToMany)
})

export type Pessoa = z.infer<typeof pessoaSchema>
