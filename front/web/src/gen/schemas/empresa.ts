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
    '0': 'Não informado, padrão, a tag não é enviada',
    '1': 'Simples Nacional com opção pelo regime regular do IBS e da CBS (crt 1 ou 2)',
    '2': 'Regime Normal com opção pendente pelo Simples Nacional, sem regime regular do IBS e da CBS (crt 3)',
    '3': 'Regime Normal com opção pendente pelo Simples Nacional com excesso de sublimite, sem regime regular do IBS e da CBS (crt 3)',
    '4': 'Regime Normal com opção pendente pelo Simples Nacional, com regime regular do IBS e da CBS (crt 3)',
}

export const situacaoValues = ['pendente_confirmacao', 'ativa', 'desativada'] as const
export const situacaoLabels: Record<string, string> = {
    'pendente_confirmacao': 'Pendente de confirmação, padrão: nasce assim no signup',
    'ativa': 'Ativa, depois do clique no link do e-mail ou ativada pelo suporte',
    'desativada': 'Desativada à mão pelo suporte; nunca apagada',
}

export const statusCadastroValues = ['nao_cadastrado', 'cadastrado', 'erro'] as const
export const statusCadastroLabels: Record<string, string> = {
    'nao_cadastrado': 'Ainda não cadastrado no gateway',
    'cadastrado': 'Cadastrado no gateway, token do emitente (nome no IntegraNotas) guardado no cofre',
    'erro': 'Cadastro recusado pelo gateway',
}

export const situacaoCertificadoValues = ['nao_enviado', 'valido', 'vencido', 'invalido', 'nao_pertence'] as const
export const situacaoCertificadoLabels: Record<string, string> = {
    'nao_enviado': 'Nenhum certificado enviado',
    'valido': 'Certificado aceito e dentro da validade',
    'vencido': 'Certificado vencido, erro 5011 do gateway',
    'invalido': 'Corrompido ou senha errada, erro 5003 do gateway',
    'nao_pertence': 'Não pertence ao documento da empresa, erro 5029 do gateway',
}

export const ambienteValues = ['homologacao', 'producao'] as const
export const ambienteLabels: Record<string, string> = {
    'homologacao': 'Homologação, ambiente de testes',
    'producao': 'Produção, ambiente real',
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
