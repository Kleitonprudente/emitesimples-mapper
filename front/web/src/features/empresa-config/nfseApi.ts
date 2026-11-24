import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'

/* ---------------------------------------------------------------------------
 * nfseApi.ts: chamadas da tela Configuracao > NFS-e da EMPRESA da sessao. Contrato (back em paralelo):
 *   GET /configuracao/nfse -> {habilitada, serie|null, ultimoNumero, proximoNumero, ambiente, statusCadastro, dataCadastroGateway|null,
 *                              emitenteTokenConfigurado, certificadoSituacao, validadeCertificado|null, integracaoDisponivel}
 *   PUT /configuracao/nfse {habilitada, serie?, ultimoNumero?} -> 200 com o corpo do GET
 *   erros: 409 integranotas_nao_configurada|numeracao_invalida; 422 cadastro_incompleto (detalhes.campos [{campo, mensagem}]);
 *          502 integranotas_recusou|integranotas_token_recusado|integranotas_resposta_invalida; 504 integranotas_indisponivel; 403; 401
 * O token do emitente NUNCA vem do back. Leitura tolerante (campo ausente = padrao seguro).
 * ------------------------------------------------------------------------- */

export type AmbienteNfse = 'HOMOLOGACAO' | 'PRODUCAO'
export type StatusCadastroNfse = 'NAO_CADASTRADO' | 'CADASTRADO' | 'ERRO'
export type CertificadoSituacao = 'NAO_ENVIADO' | 'VALIDO' | 'VENCIDO' | 'INVALIDO' | 'NAO_PERTENCE'

export interface ConfigNfse {
  habilitada: boolean
  serie: string
  ultimoNumero: number
  proximoNumero: number
  ambiente: AmbienteNfse
  statusCadastro: StatusCadastroNfse
  dataCadastroGateway?: string
  emitenteTokenConfigurado: boolean
  certificadoSituacao: CertificadoSituacao
  validadeCertificado?: string
  integracaoDisponivel: boolean
}

export interface SalvarNfse {
  habilitada: boolean
  serie?: string
  ultimoNumero?: number
}

const rec = (v: unknown): Record<string, unknown> => (v && typeof v === 'object' ? (v as Record<string, unknown>) : {})
const str = (v: unknown): string | undefined => (typeof v === 'string' && v !== '' ? v : undefined)
const num = (v: unknown): number => (typeof v === 'number' && Number.isFinite(v) ? v : 0)
const oneOf = <T extends string>(v: unknown, opts: readonly T[], padrao: T): T => (opts.includes(v as T) ? (v as T) : padrao)

export function lerConfigNfse(raw: unknown): ConfigNfse {
  const r = rec(raw)
  const ultimo = num(r.ultimoNumero)
  return {
    habilitada: r.habilitada === true,
    serie: str(r.serie) ?? '',
    ultimoNumero: ultimo,
    proximoNumero: typeof r.proximoNumero === 'number' ? r.proximoNumero : ultimo + 1,
    ambiente: oneOf(r.ambiente, ['HOMOLOGACAO', 'PRODUCAO'] as const, 'HOMOLOGACAO'),
    statusCadastro: oneOf(r.statusCadastro, ['NAO_CADASTRADO', 'CADASTRADO', 'ERRO'] as const, 'NAO_CADASTRADO'),
    dataCadastroGateway: str(r.dataCadastroGateway),
    emitenteTokenConfigurado: r.emitenteTokenConfigurado === true,
    certificadoSituacao: oneOf(r.certificadoSituacao, ['NAO_ENVIADO', 'VALIDO', 'VENCIDO', 'INVALIDO', 'NAO_PERTENCE'] as const, 'NAO_ENVIADO'),
    validadeCertificado: str(r.validadeCertificado),
    integracaoDisponivel: r.integracaoDisponivel === true,
  }
}

export const nfseKeys = { config: ['configuracao', 'nfse'] as const }

export function useConfigNfse() {
  return useQuery({
    queryKey: nfseKeys.config,
    queryFn: async () => lerConfigNfse(await api<unknown>('/configuracao/nfse')),
  })
}

export function useSalvarConfigNfse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: SalvarNfse) => lerConfigNfse(await api<unknown>('/configuracao/nfse', { method: 'PUT', body: v })),
    onSuccess: () => qc.invalidateQueries({ queryKey: nfseKeys.config }),
  })
}
