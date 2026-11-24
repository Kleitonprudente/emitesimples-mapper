// GERADO por frontend/zodSchema a partir de Empresa. Nao edite: a saida e sobrescrita por inteiro.
import { z } from 'zod'

export const regimeTributarioValues = [1, 2, 3, 4] as const
export const regimeTributarioLabels: Record<string, string> = {
    '1': 'Simples Nacional',
    '2': 'Simples Nacional com excesso de sublimite',
    '3': 'Regime Normal',
    '4': 'MEI',
}

export const regimeIbsCbsValues = [0, 1, 2, 3, 4] as const
export const regimeIbsCbsLabels: Record<string, string> = {
    '0': 'Não informado',
    '1': 'Simples Nacional com opção pelo regime regular',
    '2': 'Regime Normal, opção pelo Simples pendente, sem regime regular',
    '3': 'Regime Normal, excesso de sublimite pendente, sem regime regular',
    '4': 'Regime Normal, opção pelo Simples pendente, com regime regular',
}

export const situacaoValues = ['PENDENTE_CONFIRMACAO', 'ATIVA', 'DESATIVADA'] as const
export const situacaoLabels: Record<string, string> = {
    'PENDENTE_CONFIRMACAO': 'Pendente de confirmação',
    'ATIVA': 'Ativa',
    'DESATIVADA': 'Desativada',
}

export const statusCadastroValues = ['NAO_CADASTRADO', 'CADASTRADO', 'ERRO'] as const
export const statusCadastroLabels: Record<string, string> = {
    'NAO_CADASTRADO': 'Não cadastrado',
    'CADASTRADO': 'Cadastrado',
    'ERRO': 'Erro no cadastro',
}

export const situacaoCertificadoValues = ['NAO_ENVIADO', 'VALIDO', 'VENCIDO', 'INVALIDO', 'NAO_PERTENCE'] as const
export const situacaoCertificadoLabels: Record<string, string> = {
    'NAO_ENVIADO': 'Não enviado',
    'VALIDO': 'Válido',
    'VENCIDO': 'Vencido',
    'INVALIDO': 'Inválido',
    'NAO_PERTENCE': 'Não pertence à empresa',
}

export const ambienteValues = ['HOMOLOGACAO', 'PRODUCAO'] as const
export const ambienteLabels: Record<string, string> = {
    'HOMOLOGACAO': 'Homologação',
    'PRODUCAO': 'Produção',
}

export const empresaSchema = z.object({
    nomeFantasia: z.string().max(60),
    razaoSocial: z.string().max(60),
    documento: z.string().max(14),
    rgInscricaoEstadual: z.string().max(20).optional(),
    inscricaoMunicipal: z.string().max(20).optional(),
    email: z.string().max(60),
    whatsapp: z.string().max(20),
    telefone: z.string().max(14),
    logradouro: z.string().max(60),
    numeroEndereco: z.string().max(60),
    complemento: z.string().max(60).optional(),
    bairro: z.string().max(60),
    cep: z.string().max(8),
    regimeTributario: z.number().int().refine((v) => (regimeTributarioValues as readonly number[]).includes(v)),
    regimeIbsCbs: z.number().int().refine((v) => (regimeIbsCbsValues as readonly number[]).includes(v)).optional(),
    logo: z.string().optional(),
    urlWebhook: z.string().max(200).optional(),
    situacao: z.enum(situacaoValues),
    dataConfirmacao: z.string().optional(),
    referenciaCrm: z.string().max(50).optional(),
    nfseHabilitada: z.boolean(),
    serieNfse: z.string().max(5).optional(),
    ultimoNumeroNfse: z.number().int(),
    statusCadastro: z.enum(statusCadastroValues),
    dataCadastroGateway: z.string().optional(),
    referenciaTokenEmpresa: z.string().max(100).optional(),
    referenciaCredenciaisPrefeitura: z.string().max(100).optional(),
    referenciaCertificado: z.string().max(100).optional(),
    validadeCertificado: z.string().optional(),
    situacaoCertificado: z.enum(situacaoCertificadoValues),
    id: z.string().max(36),
    ambiente: z.enum(ambienteValues),
    dataCriacao: z.string(),
    dataAtualizacao: z.string(),
    // relacao to-many omitida do schema: atividades (AtividadeEmpresa, oneToMany)
    // relacao to-many omitida do schema: qsa (QsaEmpresa, oneToMany)
    municipio: z.looseObject({ id: z.string() }),
})

export type Empresa = z.infer<typeof empresaSchema>
