import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'

/* ---------------------------------------------------------------------------
 * empresaApi.ts: chamadas da tela Configuracao > Empresa da EMPRESA da sessao. Contrato (back em paralelo):
 *   GET /configuracao/empresa -> {razaoSocial, nomeFantasia, documento, inscricaoMunicipal|null, rgInscricaoEstadual|null, email|null,
 *        whatsapp|null, telefone|null, cep, logradouro, numeroEndereco, complemento|null, bairro, municipio:{id,nome,uf,codigoIbge}|null,
 *        regimeTributario 1..4, regimeIbsCbs|null, documentoEditavel,
 *        certificado:{situacao, validade|null, pendenteDeEnvio, venceEmDias|null}}
 *   PUT /configuracao/empresa (so administrador; documento so se documentoEditavel; municipio como {id}) -> 200 com o corpo do GET
 *        erros: 400 requisicao_invalida (detalhes.campos [{campo, mensagem}]); 409 documento_bloqueado|documento_duplicado; 403; 401
 *   POST /configuracao/empresa/certificado multipart (`arquivo` .pfx, `senha`; so administrador) -> 200 {situacao, validade, pendenteDeEnvio, venceEmDias, avisos[]}
 *        erros: 400 certificado_senha_invalida|certificado_senha_incorreta|certificado_invalido|certificado_vencido|certificado_nao_pertence|
 *               certificado_invalido_gateway; 413 arquivo_grande; 429 muitas_tentativas; 409/502/504 integranotas_*
 * O arquivo e a senha NUNCA voltam do back. Leitura tolerante (campo ausente = padrao seguro).
 * ------------------------------------------------------------------------- */

export type CertificadoSituacao = 'NAO_ENVIADO' | 'VALIDO' | 'VENCIDO' | 'INVALIDO' | 'NAO_PERTENCE'

export interface MunicipioRef {
  id: string
  nome?: string
  uf?: string
  codigoIbge?: string
}

export interface CertificadoStatus {
  situacao: CertificadoSituacao
  validade?: string
  pendenteDeEnvio: boolean
  venceEmDias?: number
}

export interface EmpresaConfig {
  razaoSocial: string
  nomeFantasia: string
  documento: string
  inscricaoMunicipal?: string
  rgInscricaoEstadual?: string
  email?: string
  whatsapp?: string
  telefone?: string
  cep: string
  logradouro: string
  numeroEndereco: string
  complemento?: string
  bairro: string
  municipio?: MunicipioRef
  regimeTributario: number
  regimeIbsCbs?: number
  documentoEditavel: boolean
  certificado: CertificadoStatus
}

/** Campos editaveis enviados no PUT. Opcionais limpos vao como null; `documento` so quando `documentoEditavel`. */
export interface SalvarEmpresa {
  razaoSocial: string
  nomeFantasia: string
  documento?: string
  inscricaoMunicipal: string | null
  rgInscricaoEstadual: string | null
  email: string | null
  whatsapp: string | null
  telefone: string | null
  cep: string
  logradouro: string
  numeroEndereco: string
  complemento: string | null
  bairro: string
  municipio: { id: string }
  regimeTributario: number
  regimeIbsCbs: number | null
}

export interface CertificadoEnviado extends CertificadoStatus {
  avisos: string[]
}

export interface EnviarCertificadoInput {
  arquivo: File
  /** Zerada assim que o corpo e montado: a senha nao pode ficar no estado da mutation. */
  senha: string
}

const SITUACOES = ['NAO_ENVIADO', 'VALIDO', 'VENCIDO', 'INVALIDO', 'NAO_PERTENCE'] as const

const rec = (v: unknown): Record<string, unknown> => (v && typeof v === 'object' ? (v as Record<string, unknown>) : {})
const str = (v: unknown): string | undefined => (typeof v === 'string' && v !== '' ? v : undefined)
const text = (v: unknown): string => str(v) ?? ''
const intOrUndef = (v: unknown): number | undefined => (typeof v === 'number' && Number.isFinite(v) ? v : undefined)

function lerStatus(raw: unknown): CertificadoStatus {
  const r = rec(raw)
  return {
    situacao: SITUACOES.includes(r.situacao as CertificadoSituacao) ? (r.situacao as CertificadoSituacao) : 'NAO_ENVIADO',
    validade: str(r.validade),
    pendenteDeEnvio: r.pendenteDeEnvio === true,
    venceEmDias: intOrUndef(r.venceEmDias),
  }
}

export function lerEmpresaConfig(raw: unknown): EmpresaConfig {
  const r = rec(raw)
  const m = rec(r.municipio)
  return {
    razaoSocial: text(r.razaoSocial),
    nomeFantasia: text(r.nomeFantasia),
    documento: text(r.documento),
    inscricaoMunicipal: str(r.inscricaoMunicipal),
    rgInscricaoEstadual: str(r.rgInscricaoEstadual),
    email: str(r.email),
    whatsapp: str(r.whatsapp),
    telefone: str(r.telefone),
    cep: text(r.cep),
    logradouro: text(r.logradouro),
    numeroEndereco: text(r.numeroEndereco),
    complemento: str(r.complemento),
    bairro: text(r.bairro),
    municipio: str(m.id) ? { id: m.id as string, nome: str(m.nome), uf: str(m.uf), codigoIbge: str(m.codigoIbge) } : undefined,
    regimeTributario: intOrUndef(r.regimeTributario) ?? 1,
    regimeIbsCbs: intOrUndef(r.regimeIbsCbs),
    documentoEditavel: r.documentoEditavel === true,
    certificado: lerStatus(r.certificado),
  }
}

export function lerCertificadoEnviado(raw: unknown): CertificadoEnviado {
  const r = rec(raw)
  return { ...lerStatus(raw), avisos: Array.isArray(r.avisos) ? r.avisos.filter((a): a is string => typeof a === 'string') : [] }
}

export const empresaKeys = { config: ['configuracao', 'empresa'] as const }

export function useEmpresaConfig() {
  return useQuery({
    queryKey: empresaKeys.config,
    queryFn: async () => lerEmpresaConfig(await api<unknown>('/configuracao/empresa')),
  })
}

export function useSalvarEmpresaConfig() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: SalvarEmpresa) => lerEmpresaConfig(await api<unknown>('/configuracao/empresa', { method: 'PUT', body: v })),
    onSuccess: () => qc.invalidateQueries({ queryKey: empresaKeys.config }),
  })
}

export function useEnviarCertificado() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: EnviarCertificadoInput) => {
      const form = new FormData()
      form.append('arquivo', v.arquivo)
      form.append('senha', v.senha)
      v.senha = '' // a mutation guarda `variables`: a senha so existe no corpo desta chamada
      return lerCertificadoEnviado(await api<unknown>('/configuracao/empresa/certificado', { method: 'POST', body: form }))
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: empresaKeys.config }),
  })
}
