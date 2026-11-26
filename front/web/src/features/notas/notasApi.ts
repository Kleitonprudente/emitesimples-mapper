import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'
import type { BadgeTone } from '@/components/ui/badge'
import { ApiError, api } from '@/lib/api'
import { cstPisCofinsLabels, cstPisCofinsValues, tipoRetencaoLabels, tipoRetencaoValues } from '@/gen/schemas/servico'
import { fkSource } from '@/lib/fk-sources'
import type { FieldLayout } from '@/runtime/AutoForm'
import type { TableState } from '@/runtime/table-state'

/* ---------------------------------------------------------------------------
 * notasApi.ts: chamadas, tipos, formulario e mensagens da tela Notas Fiscais > NFS-e. Contrato (back em paralelo; todos exigem empresa):
 *   GET  /nfse/notas/{from}/{to}?estado=&busca=  -> NotaItem[] (mais recentes primeiro, ate 100)
 *   GET  /nfse/notas/count?estado=&busca=        -> {count}
 *   GET  /nfse/notas/{id}                        -> NotaDetalhe
 *   POST /nfse/notas {pessoaId, servicoId, valorServicos, informacoesComplementares?, discriminacao?} -> 200 NotaDetalhe (cria E envia)
 *   POST /nfse/notas/{id}/enviar  (corpo opcional, mesmos campos: aplica e reenvia, mantendo o numero) -> NotaDetalhe
 *   POST /nfse/notas/{id}/atualizar              -> NotaDetalhe (consulta o gateway agora; tambem consulta um cancelamento em processamento)
 *   POST /nfse/notas/{id}/cancelar {codigoMotivo: 1|2|9, motivo: string 15..255 apos trim} (operador+) -> 200 NotaDetalhe
 *        Cancelar so vale para nota `autorizada`, dentro do MES VIGENTE da emissao (o back decide; o cliente so adianta o aviso).
 *        1 = Erro de emissao, 2 = Servico nao concluido, 9 = Outros. Concluido: estado `cancelada`; pendente: a nota segue `autorizada`
 *        com `cancelamento.estado = 'PROCESSANDO'` ate /atualizar (ou o webhook) concluir.
 *        NotaDetalhe ganha `cancelamento` ({estado: 'PROCESSANDO'|'CONCLUIDO'|'RECUSADO', codigoMotivo, motivo, codigoRetorno,
 *        mensagemRetorno, dataHoraEvento, solicitadoEm} | null) e `podeCancelar` (boolean; ausente = o cliente calcula).
 *        Delegacao: operador so cancela com `podeCancelarNfse` na sessao; sem ela 403 `cancelamento_nao_delegado` (mensagem do back).
 *   Assistente (a UI NAO prepara; so aprova ou descarta o que o assistente preparou):
 *   POST /nfse/notas/preparar (corpo de criar) -> NotaDetalhe `rascunho`; POST /nfse/notas/{id}/aprovar (corpo opcional como /enviar) -> processando;
 *   POST /nfse/notas/{id}/descartar -> `descartada`; POST /nfse/notas/{id}/cancelamentos/preparar {codigoMotivo, motivo} -> cancelamento `rascunho`;
 *   POST /nfse/cancelamentos/{id}/aprovar | /descartar -> NotaDetalhe. NotaDetalhe: `podeAprovar`, `podeDescartar`; `cancelamento`: `id`, `podeAprovar`, `podeDescartar`.
 *   GET  /nfse/notas/{id}/pdf                    -> application/pdf (404 pdf_indisponivel)
 * Tributacao (docs/historias-emissao-nota.md, "Tributacao da nota"): criar/enviar/aprovar levam `tributos` (Tributos). A TELA manda
 *   SEMPRE o objeto inteiro: campo em branco limpa, inclusive o padrao do servico (a tela ja copia os padroes ao escolher o servico).
 *   Sem `tributos` o back usa os padroes do servico. NotaDetalhe traz `tributos` gravados. Erros por campo com o nome do Tributos.
 * Erros {codigo, mensagem, detalhes?}: 409 nfse_nao_habilitada|emitente_nao_cadastrado|emitente_de_outro_ambiente|certificado_nao_valido|
 *   servico_inativo|nota_em_processamento|nota_nao_editavel|nota_em_conflito; cancelar: 409 nota_nao_autorizada|
 *   cancelamento_fora_do_mes|cancelamento_em_andamento|cancelamento_recusado (mensagem do gateway)|emissao_producao_bloqueada; 422 tomador_incompleto (detalhes.campos);
 *   400 requisicao_invalida (detalhes.campos)|discriminacao_obrigatoria; 404; 403; 429. Leitura tolerante (campo ausente = padrao seguro).
 * ------------------------------------------------------------------------- */

/** `rascunho` e SO do assistente de IA (nunca na emissao direta); `falha_envio` = falha de infraestrutura no envio direto (reenviavel, mesmo numero). */
export type EstadoNota = 'RASCUNHO' | 'FALHA_ENVIO' | 'PROCESSANDO' | 'EMITIDA_OFFLINE' | 'AUTORIZADA' | 'REJEITADA' | 'EM_CONFLITO' | 'CANCELADA' | 'DESCARTADA'

export const ESTADOS: readonly EstadoNota[] = ['RASCUNHO', 'FALHA_ENVIO', 'PROCESSANDO', 'EMITIDA_OFFLINE', 'AUTORIZADA', 'REJEITADA', 'EM_CONFLITO', 'CANCELADA', 'DESCARTADA']

export const ESTADO: Record<EstadoNota, { rotulo: string; tone: BadgeTone }> = {
  RASCUNHO: { rotulo: 'Rascunho (assistente)', tone: 'neutral' },
  FALHA_ENVIO: { rotulo: 'Falha no envio', tone: 'warning' },
  PROCESSANDO: { rotulo: 'Processando', tone: 'info' },
  EMITIDA_OFFLINE: { rotulo: 'Emitida offline', tone: 'info' },
  AUTORIZADA: { rotulo: 'Autorizada', tone: 'success' },
  REJEITADA: { rotulo: 'Rejeitada', tone: 'danger' },
  EM_CONFLITO: { rotulo: 'Em conflito', tone: 'warning' },
  CANCELADA: { rotulo: 'Cancelada', tone: 'neutral' },
  DESCARTADA: { rotulo: 'Descartada', tone: 'neutral' },
}

export interface NotaItem {
  id: string
  numero: number
  serie: string
  estado: EstadoNota
  dataEmissao?: string
  valorServicos: number
  pessoa: { id: string; nome: string; documento?: string }
  servico: { id: string; nome: string }
  chave?: string
  numeroNfse?: string
  codigoRetorno?: string
  mensagemRetorno?: string
  temPdf: boolean
  dataCriacao?: string
}

export interface ErroRetorno {
  campo: string
  erro: string
  descricao: string
}

export interface NotaDetalhe extends NotaItem {
  informacoesComplementares?: string
  discriminacao: string
  descricao?: string
  codigoServico?: string
  errosRetorno: ErroRetorno[]
  linkPdf?: string
  codigoVerificacao?: string
  dataHoraEvento?: string
  editavel: boolean
  podeReenviar: boolean
  podeAtualizar: boolean
  cancelamento: Cancelamento | null
  /** Ausente no back = undefined (o cliente calcula). */
  podeCancelar?: boolean
  /** Rascunho do assistente: quem pode emitir aprova (envia) ou descarta. */
  podeAprovar: boolean
  podeDescartar: boolean
  tributos: Tributos
}

/** Tributacao da nota (ISS e retencoes federais). Dinheiro em reais; aliquotas em pontos percentuais (5 = 5%). */
export interface Tributos {
  issRetido: boolean
  aliquotaIss?: number
  /** Base do ISS em R$ = valor - desconto (calculada; somente leitura na tela; o governo faz a mesma conta). */
  valorBaseCalculoIss?: number
  /** ISS retido em R$ = base x aliquota (calculado; somente leitura na tela). */
  valorIssRetido?: number
  cstPisCofins?: string
  tipoRetencao?: number
  valorBaseCalculo?: number
  aliquotaPis?: number
  aliquotaCofins?: number
  valorPis?: number
  valorCofins?: number
  valorInss?: number
  valorIr?: number
  valorCsll?: number
  valorDescontoIncondicionado?: number
}

/** Campos numericos de Tributos (todos opcionais). */
const CAMPOS_NUMERICOS = [
  'aliquotaIss', 'valorBaseCalculoIss', 'valorIssRetido', 'tipoRetencao', 'valorBaseCalculo', 'aliquotaPis', 'aliquotaCofins',
  'valorPis', 'valorCofins', 'valorInss', 'valorIr', 'valorCsll', 'valorDescontoIncondicionado',
] as const

export type EstadoCancelamento = 'RASCUNHO' | 'PROCESSANDO' | 'CONCLUIDO' | 'RECUSADO' | 'EXPIRADO' | 'DESCARTADO'

export const CANCELAMENTO: Record<EstadoCancelamento, { rotulo: string; tone: BadgeTone }> = {
  RASCUNHO: { rotulo: 'Rascunho (assistente)', tone: 'neutral' },
  PROCESSANDO: { rotulo: 'Em processamento', tone: 'info' },
  CONCLUIDO: { rotulo: 'Concluído', tone: 'success' },
  RECUSADO: { rotulo: 'Recusado', tone: 'danger' },
  EXPIRADO: { rotulo: 'Expirado: sem confirmação', tone: 'warning' },
  DESCARTADO: { rotulo: 'Descartado', tone: 'neutral' },
}
export type CodigoMotivo = 1 | 2 | 9

export interface Cancelamento {
  id?: string
  estado: EstadoCancelamento
  codigoMotivo?: number
  motivo?: string
  codigoRetorno?: string
  mensagemRetorno?: string
  dataHoraEvento?: string
  solicitadoEm?: string
  podeAprovar: boolean
  podeDescartar: boolean
}

export interface CorpoNota {
  pessoaId: string
  servicoId: string
  valorServicos: number
  informacoesComplementares?: string
  discriminacao?: string
  tributos?: { [K in keyof Tributos]: Tributos[K] | null }
}

const rec = (v: unknown): Record<string, unknown> => (v && typeof v === 'object' ? (v as Record<string, unknown>) : {})
const str = (v: unknown): string | undefined => (typeof v === 'string' && v !== '' ? v : undefined)
const num = (v: unknown): number => (typeof v === 'number' && Number.isFinite(v) ? v : 0)
const numOpt = (v: unknown): number | undefined => (typeof v === 'number' && Number.isFinite(v) ? v : undefined)

export function lerTributos(raw: unknown): Tributos {
  const r = rec(raw)
  const t: Tributos = { issRetido: r.issRetido === true, cstPisCofins: str(r.cstPisCofins) }
  for (const c of CAMPOS_NUMERICOS) t[c] = numOpt(r[c])
  return t
}

export function lerNota(raw: unknown): NotaItem {
  const r = rec(raw)
  const p = rec(r.pessoa)
  const s = rec(r.servico)
  return {
    id: String(r.id ?? ''),
    numero: num(r.numero),
    serie: str(r.serie) ?? '',
    estado: ESTADOS.includes(r.estado as EstadoNota) ? (r.estado as EstadoNota) : 'RASCUNHO',
    dataEmissao: str(r.dataEmissao),
    valorServicos: num(r.valorServicos),
    pessoa: { id: String(p.id ?? ''), nome: str(p.nome) ?? '', documento: str(p.documento) },
    servico: { id: String(s.id ?? ''), nome: str(s.nome) ?? '' },
    chave: str(r.chave),
    numeroNfse: str(r.numeroNfse),
    codigoRetorno: str(r.codigoRetorno),
    mensagemRetorno: str(r.mensagemRetorno),
    temPdf: r.temPdf === true,
    dataCriacao: str(r.dataCriacao),
  }
}

const ESTADOS_CANCELAMENTO = Object.keys(CANCELAMENTO) as EstadoCancelamento[]

function lerCancelamento(raw: unknown): Cancelamento | null {
  if (!raw || typeof raw !== 'object') return null
  const c = rec(raw)
  if (!ESTADOS_CANCELAMENTO.includes(c.estado as EstadoCancelamento)) return null
  return {
    id: str(c.id),
    estado: c.estado as EstadoCancelamento,
    codigoMotivo: typeof c.codigoMotivo === 'number' ? c.codigoMotivo : undefined,
    motivo: str(c.motivo),
    codigoRetorno: str(c.codigoRetorno),
    mensagemRetorno: str(c.mensagemRetorno),
    dataHoraEvento: str(c.dataHoraEvento),
    solicitadoEm: str(c.solicitadoEm),
    podeAprovar: c.podeAprovar === true,
    podeDescartar: c.podeDescartar === true,
  }
}

export function lerDetalhe(raw: unknown): NotaDetalhe {
  const r = rec(raw)
  return {
    ...lerNota(raw),
    informacoesComplementares: str(r.informacoesComplementares),
    discriminacao: str(r.discriminacao) ?? '',
    descricao: str(r.descricao),
    codigoServico: str(r.codigoServico),
    errosRetorno: Array.isArray(r.errosRetorno)
      ? r.errosRetorno.map((e) => ({ campo: str(rec(e).campo) ?? '', erro: str(rec(e).erro) ?? '', descricao: str(rec(e).descricao) ?? '' }))
      : [],
    linkPdf: str(r.linkPdf),
    codigoVerificacao: str(r.codigoVerificacao),
    dataHoraEvento: str(r.dataHoraEvento),
    editavel: r.editavel === true,
    podeReenviar: r.podeReenviar === true,
    podeAtualizar: r.podeAtualizar === true,
    cancelamento: lerCancelamento(r.cancelamento),
    podeCancelar: typeof r.podeCancelar === 'boolean' ? r.podeCancelar : undefined,
    podeAprovar: r.podeAprovar === true,
    podeDescartar: r.podeDescartar === true,
    tributos: lerTributos(r.tributos),
  }
}

export const aguardaPrefeitura = (e: EstadoNota): boolean => e === 'PROCESSANDO' || e === 'EMITIDA_OFFLINE'

export const cancelamentoEmProcessamento = (n: Pick<NotaDetalhe, 'cancelamento'>): boolean => n.cancelamento?.estado === 'PROCESSANDO'

/* ----- cancelamento: regras do cliente (so UX; o back decide) ----- */

export const MOTIVOS_CANCELAMENTO: readonly { codigo: CodigoMotivo; rotulo: string }[] = [
  { codigo: 1, rotulo: 'Erro de emissão' },
  { codigo: 2, rotulo: 'Serviço não concluído' },
  { codigo: 9, rotulo: 'Outros' },
]
export const rotuloMotivo = (codigo?: number): string | undefined => MOTIVOS_CANCELAMENTO.find((m) => m.codigo === codigo)?.rotulo

export const JUSTIFICATIVA_MIN = 15
export const JUSTIFICATIVA_MAX = 255
const MSG_JUSTIFICATIVA = `A justificativa deve ter de ${JUSTIFICATIVA_MIN} a ${JUSTIFICATIVA_MAX} caracteres.`

export const cancelarSchema = z.object({
  codigoMotivo: z.union([z.literal(1), z.literal(2), z.literal(9)], { error: 'Escolha o tipo do motivo.' }),
  motivo: z.string().trim().min(JUSTIFICATIVA_MIN, MSG_JUSTIFICATIVA).max(JUSTIFICATIVA_MAX, MSG_JUSTIFICATIVA),
})
export type CorpoCancelamento = z.infer<typeof cancelarSchema>

export const MENSAGEM_NAO_DELEGADO = 'Seu usuário não tem permissão para cancelar notas. Peça ao administrador.'
export const MENSAGEM_FORA_DO_MES = 'O cancelamento só é possível dentro do mês vigente da emissão da nota.'
const FUSO = 'America/Sao_Paulo'

/** Ano-mes (AAAA-MM) de um instante no fuso de Sao Paulo. Texto sem fuso ("2026-11-10T14:00:00") ja e horario local de Sao Paulo. */
function anoMes(v: string | Date): string | undefined {
  if (typeof v === 'string' && !/(Z|[+-]\d{2}:?\d{2})$/i.test(v)) return /^\d{4}-\d{2}/.test(v) ? v.slice(0, 7) : undefined
  const d = typeof v === 'string' ? new Date(v) : v
  if (Number.isNaN(d.getTime())) return undefined
  const p = new Intl.DateTimeFormat('en-CA', { timeZone: FUSO, year: 'numeric', month: '2-digit' }).formatToParts(d)
  return `${p.find((x) => x.type === 'year')?.value}-${p.find((x) => x.type === 'month')?.value}`
}

/** A emissao foi no mes corrente (fuso America/Sao_Paulo)? Sem data valida, assume que sim e deixa o back decidir. */
export function dentroDoMesVigente(dataEmissao: string | undefined, agora: Date = new Date()): boolean {
  const emitido = dataEmissao ? anoMes(dataEmissao) : undefined
  return !emitido || emitido === anoMes(agora)
}

/* ----- consultas ----- */

export const notasKeys = {
  all: ['nfse', 'notas'] as const,
  list: (s: TableState) => ['nfse', 'notas', 'list', s] as const,
  one: (id: string) => ['nfse', 'notas', 'id', id] as const,
}

/** Intervalos de atualizacao automatica (ms). */
export const INTERVALO_LISTA_MS = 10_000
export const INTERVALO_DETALHE_MS = 3_000

/** Lista paginada no servidor; recarrega sozinha a cada 10 s enquanto houver nota `processando`. */
export function useNotas(state: TableState) {
  const from = state.page * state.pageSize
  const to = from + state.pageSize - 1
  const query = { estado: state.filters.estado || undefined, busca: state.search.trim() || undefined }
  const q = useQuery({
    queryKey: notasKeys.list(state),
    queryFn: async () => {
      const [data, count] = await Promise.all([api<unknown[]>(`/nfse/notas/${from}/${to}`, { query }), api<{ count: number }>('/nfse/notas/count', { query })])
      return { data: data.map(lerNota), count: num(count?.count) }
    },
    placeholderData: keepPreviousData,
    refetchInterval: (cq) => (cq.state.data?.data.some((n) => n.estado === 'PROCESSANDO') ? INTERVALO_LISTA_MS : false),
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

/** Detalhe; consulta a cada 3 s enquanto a nota aguarda a prefeitura (processando / emitida offline). */
export function useNota(id: string) {
  return useQuery({
    queryKey: notasKeys.one(id),
    queryFn: async () => lerDetalhe(await api<unknown>(`/nfse/notas/${encodeURIComponent(id)}`)),
    refetchInterval: (cq) => (cq.state.data && (aguardaPrefeitura(cq.state.data.estado) || cancelamentoEmProcessamento(cq.state.data)) ? INTERVALO_DETALHE_MS : false),
  })
}

export function useCriarNota() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: CorpoNota) => lerDetalhe(await api<unknown>('/nfse/notas', { method: 'POST', body: v })),
    onSuccess: (nota) => {
      qc.setQueryData(notasKeys.one(nota.id), nota)
      return qc.invalidateQueries({ queryKey: ['nfse', 'notas', 'list'] })
    },
  })
}

export function useReenviarNota(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: CorpoNota) => lerDetalhe(await api<unknown>(`/nfse/notas/${encodeURIComponent(id)}/enviar`, { method: 'POST', body: v })),
    onSuccess: (nota) => {
      qc.setQueryData(notasKeys.one(id), nota)
      return qc.invalidateQueries({ queryKey: ['nfse', 'notas', 'list'] })
    },
  })
}

export function useAtualizarNota(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => lerDetalhe(await api<unknown>(`/nfse/notas/${encodeURIComponent(id)}/atualizar`, { method: 'POST' })),
    onSuccess: (nota) => {
      qc.setQueryData(notasKeys.one(id), nota)
      return qc.invalidateQueries({ queryKey: ['nfse', 'notas', 'list'] })
    },
  })
}

export function useCancelarNota(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (v: CorpoCancelamento) => lerDetalhe(await api<unknown>(`/nfse/notas/${encodeURIComponent(id)}/cancelar`, { method: 'POST', body: v })),
    onSuccess: (nota) => {
      qc.setQueryData(notasKeys.one(id), nota)
      return qc.invalidateQueries({ queryKey: ['nfse', 'notas', 'list'] })
    },
  })
}

/** Aprovar (envia a nota preparada pelo assistente) ou descartar. Atualiza o cache da nota aberta com o detalhe devolvido. */
function useAcaoDaNota(notaId: string, acao: 'aprovar' | 'descartar') {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => lerDetalhe(await api<unknown>(`/nfse/notas/${encodeURIComponent(notaId)}/${acao}`, { method: 'POST' })),
    onSuccess: (nota) => {
      qc.setQueryData(notasKeys.one(notaId), nota)
      return qc.invalidateQueries({ queryKey: ['nfse', 'notas', 'list'] })
    },
  })
}
export const useAprovarNota = (notaId: string) => useAcaoDaNota(notaId, 'aprovar')
export const useDescartarNota = (notaId: string) => useAcaoDaNota(notaId, 'descartar')

/** Aprovar ou descartar o cancelamento preparado pelo assistente; a rota usa o id do CANCELAMENTO e devolve a nota. */
function useAcaoDoCancelamento(notaId: string, cancelamentoId: string | undefined, acao: 'aprovar' | 'descartar') {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => lerDetalhe(await api<unknown>(`/nfse/cancelamentos/${encodeURIComponent(cancelamentoId ?? '')}/${acao}`, { method: 'POST' })),
    onSuccess: (nota) => {
      qc.setQueryData(notasKeys.one(notaId), nota)
      return qc.invalidateQueries({ queryKey: ['nfse', 'notas', 'list'] })
    },
  })
}
export const useAprovarCancelamento = (notaId: string, cancelamentoId: string | undefined) => useAcaoDoCancelamento(notaId, cancelamentoId, 'aprovar')
export const useDescartarCancelamento = (notaId: string, cancelamentoId: string | undefined) => useAcaoDoCancelamento(notaId, cancelamentoId, 'descartar')

export const urlPdf = (id: string) => `/nfse/notas/${encodeURIComponent(id)}/pdf`

/** Texto padrao do servico (discriminacaoPadrao do cadastro) para preencher a descricao da nota. */
export async function buscarDiscriminacaoPadrao(servicoId: string): Promise<string> {
  return (await buscarPadroesDoServico(servicoId)).discriminacaoPadrao
}

/** Padroes do cadastro do servico que a nota copia ao escolher o servico. */
export interface PadroesDoServico {
  discriminacaoPadrao: string
  valorPadrao?: number
  issRetido: boolean
  aliquotaIss?: number
  cstPisCofins?: string
  tipoRetencao?: number
  aliquotaPis?: number
  aliquotaCofins?: number
}

export async function buscarPadroesDoServico(servicoId: string): Promise<PadroesDoServico> {
  const s = rec(await api<unknown>(`/Servico/id/${encodeURIComponent(servicoId)}`))
  const cst = str(s.cstPisCofins)
  return {
    discriminacaoPadrao: str(s.discriminacaoPadrao) ?? '',
    valorPadrao: numOpt(s.valorPadrao),
    issRetido: s.issRetido === true,
    aliquotaIss: numOpt(s.aliquotaIss),
    cstPisCofins: cst === '00' ? undefined : cst, // 00 = legado "sem CST"
    tipoRetencao: numOpt(s.tipoRetencao),
    aliquotaPis: numOpt(s.aliquotaPis),
    aliquotaCofins: numOpt(s.aliquotaCofins),
  }
}

/**
 * Valores de tributacao que a tela poe no formulario ao escolher o servico. O % da base do ISS do servico vale para qualquer valor
 * de nota; valores em reais das retencoes, desconto e ISS retido em reais nunca vem do servico (ficam vazios).
 */
export function tributosDoServico(s: PadroesDoServico): Partial<NotaValues> {
  return {
    issRetido: s.issRetido,
    aliquotaIss: s.aliquotaIss,
    valorIssRetido: undefined,
    cstPisCofins: s.cstPisCofins as NotaValues['cstPisCofins'],
    tipoRetencao: s.tipoRetencao,
    aliquotaPis: s.aliquotaPis,
    aliquotaCofins: s.aliquotaCofins,
  }
}

const centavos = (v: number): number => Math.round(v * 100)

/** Base do ISS em R$ = valor - desconto, minimo 0 (mesma conta do back e do governo: vBC = vServ - vDescIncond). */
export function baseIssPadrao(valor?: number, desconto?: number): number | undefined {
  if (valor === undefined) return undefined
  return Math.max(0, centavos(valor) - centavos(desconto ?? 0)) / 100
}

/** ISS retido = base x aliquota / 100, meio para cima (mesma conta do back e do governo: vISSQN = vBC x pAliq), em reais. */
export function issRetidoPadrao(base?: number, aliquota?: number): number | undefined {
  if (base === undefined || aliquota === undefined) return undefined
  const c = centavos(base) * Math.round(aliquota * 100) // centavos x centesimos de ponto
  return Math.floor((c + 5000) / 10000) / 100
}

const reais = (v: number): string => v.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/* ----- formulario (nova nota e edicao para reenvio) ----- */

const fk = z.looseObject({ id: z.string().min(1) })

const dinheiro = z.number().min(0, 'Informe um valor de zero para cima.').optional()
const percentual = z.number().min(0, 'Use de 0 a 100.').max(100, 'Use de 0 a 100.').optional()

export const notaSchema = z.object({
  pessoa: fk,
  servico: fk,
  valorServicos: z.number().min(0.01, 'O valor deve ser de pelo menos R$ 0,01.'),
  valorDescontoIncondicionado: dinheiro,
  discriminacao: z.string().trim().min(1, 'Campo obrigatório').max(2000),
  informacoesComplementares: z.string().max(2000).optional(),
  issRetido: z.boolean().optional(),
  aliquotaIss: percentual,
  valorBaseCalculoIss: dinheiro,
  valorIssRetido: dinheiro,
  cstPisCofins: z.enum(cstPisCofinsValues).optional(),
  tipoRetencao: z.number().int().refine((v) => (tipoRetencaoValues as readonly number[]).includes(v)).optional(),
  valorBaseCalculo: dinheiro,
  aliquotaPis: percentual,
  aliquotaCofins: percentual,
  valorPis: dinheiro,
  valorCofins: dinheiro,
  valorInss: dinheiro,
  valorIr: dinheiro,
  valorCsll: dinheiro,
})

export type NotaValues = z.infer<typeof notaSchema>

/** Contas que a tela mostra (o back faz as mesmas). Base e ISS so com dado de ISS (retencao ou aliquota). */
export interface ContasDaNota {
  baseIss?: number
  issRetido?: number
  baseFederal?: number
}

export function contasDaNota(v: Partial<NotaValues>): ContasDaNota {
  const temIss = v.issRetido === true || v.aliquotaIss !== undefined
  const baseIss = temIss ? baseIssPadrao(v.valorServicos, v.valorDescontoIncondicionado) : undefined
  return {
    baseIss,
    issRetido: v.issRetido ? issRetidoPadrao(baseIss, v.aliquotaIss) : undefined,
    baseFederal: v.valorServicos,
  }
}

const emCinza = (v?: number): string | undefined => (v === undefined ? undefined : reais(v))

/** Layout da nota; `contas` poe em cinza (placeholder) o que o sistema calcula quando o campo fica em branco. */
export function layoutDaNota(contas: ContasDaNota = {}): FieldLayout {
  return [
    { section: 'Nota', description: 'Escolha o cliente e o serviço. A nota só é emitida quando você clica em enviar.' },
    [
      { name: 'pessoa', label: 'Cliente', kind: 'fk', fk: fkSource('Cliente'), required: true, size: 'l' },
      { name: 'servico', label: 'Serviço', kind: 'fk', fk: fkSource('Servico'), required: true, size: 'l' },
    ],
    [
      { name: 'valorServicos', label: 'Valor dos serviços', kind: 'currency', required: true, size: 'l' },
      { name: 'valorDescontoIncondicionado', label: 'Desconto incondicionado', kind: 'currency', size: 'l', hint: 'Opcional. Desconto dado sem condição; diminui a base do ISS.' },
    ],
    [{ name: 'discriminacao', label: 'Descrição do serviço', kind: 'textarea', required: true, maxLength: 2000, size: 'full', hint: 'Preenchida com o texto padrão do serviço; você pode editar.' }],
    [{ name: 'informacoesComplementares', label: 'Informações complementares', kind: 'textarea', maxLength: 2000, size: 'full' }],
    {
      section: 'ISS da prefeitura (opcional)',
      description: 'Vem preenchido com os padrões do serviço. Em branco, nada do ISS vai na nota. Confirme com seu contador.',
      collapsible: true,
    },
    [
      { name: 'issRetido', label: 'O cliente retém o ISS', kind: 'boolean', size: 's' },
      { name: 'aliquotaIss', label: 'Alíquota do ISS (%)', kind: 'number', size: 's', hint: 'Ex.: 5,00. No Simples Nacional, só vai na nota quando o cliente retém o ISS (regra do governo).' },
      { name: 'valorBaseCalculoIss', label: 'Base de cálculo do ISS', kind: 'currency', size: 's', readOnly: true, hint: 'Calculada: valor − desconto (como a prefeitura calcula).' },
      { name: 'valorIssRetido', label: 'Valor do ISS retido', kind: 'currency', size: 's', readOnly: true, hint: 'Calculado: base × alíquota.' },
    ],
    {
      section: 'Impostos federais retidos pelo cliente (opcional)',
      description: 'Ex.: uma empresa contrata seu serviço de R$ 1.000,00 e te paga R$ 890,00, porque descontou R$ 110,00 de INSS (11%). Confirme com seu contador.',
      collapsible: true,
    },
    [
      { name: 'cstPisCofins', label: 'CST do PIS/COFINS', options: cstPisCofinsValues.map((value) => ({ value, label: cstPisCofinsLabels[value] })), size: 'l', hint: 'Obrigatório quando qualquer campo deste quadro tem dado.' },
      { name: 'tipoRetencao', label: 'Tipo de retenção', options: tipoRetencaoValues.map((value) => ({ value, label: tipoRetencaoLabels[value] })), size: 'l', hint: 'Em branco, vale "PIS/COFINS não retidos".' },
    ],
    [
      { name: 'valorBaseCalculo', label: 'Base de cálculo federal', kind: 'currency', size: 's', placeholder: emCinza(contas.baseFederal), hint: 'Em branco: o valor da nota.' },
      { name: 'aliquotaPis', label: 'Alíquota do PIS (%)', kind: 'number', size: 's' },
      { name: 'aliquotaCofins', label: 'Alíquota da COFINS (%)', kind: 'number', size: 's' },
      { name: 'valorPis', label: 'PIS retido', kind: 'currency', size: 's' },
    ],
    [
      { name: 'valorCofins', label: 'COFINS retida', kind: 'currency', size: 's' },
      { name: 'valorInss', label: 'INSS retido', kind: 'currency', size: 's' },
      { name: 'valorIr', label: 'IR retido', kind: 'currency', size: 's' },
      { name: 'valorCsll', label: 'CSLL retida', kind: 'currency', size: 's' },
    ],
  ]
}

export const notaLayout: FieldLayout = layoutDaNota()

export function corpoDaNota(v: NotaValues): CorpoNota {
  const corpo: CorpoNota = { pessoaId: v.pessoa.id, servicoId: v.servico.id, valorServicos: v.valorServicos, discriminacao: v.discriminacao.trim() }
  const info = (v.informacoesComplementares ?? '').trim()
  if (info !== '') corpo.informacoesComplementares = info
  // a tela manda SEMPRE todos os campos: em branco limpa (inclusive o padrao do servico)
  const tributos: NonNullable<CorpoNota['tributos']> = { issRetido: v.issRetido === true, cstPisCofins: v.cstPisCofins ?? null }
  for (const c of CAMPOS_NUMERICOS) tributos[c] = v[c] ?? null
  // base e ISS retido sao sempre calculados pelo back (o governo ignora o informado)
  tributos.valorBaseCalculoIss = null
  tributos.valorIssRetido = null
  corpo.tributos = tributos
  return corpo
}

export function valoresDaNota(n: NotaDetalhe): NotaValues {
  return {
    pessoa: { id: n.pessoa.id, nome: n.pessoa.nome, documento: n.pessoa.documento },
    servico: { id: n.servico.id, nome: n.servico.nome },
    valorServicos: n.valorServicos,
    discriminacao: n.discriminacao,
    informacoesComplementares: n.informacoesComplementares,
    ...n.tributos,
    cstPisCofins: n.tributos.cstPisCofins as NotaValues['cstPisCofins'],
  }
}

/* ----- mensagens ----- */

const CAMPOS: Record<string, string> = {
  documento: 'CPF/CNPJ',
  nome: 'nome',
  email: 'e-mail',
  telefone: 'telefone',
  cep: 'CEP',
  logradouro: 'logradouro',
  numeroEndereco: 'número do endereço',
  bairro: 'bairro',
  cidade: 'município',
  municipio: 'município',
  codigoMunicipio: 'município',
  uf: 'UF',
  pessoaId: 'cliente',
  servicoId: 'serviço',
  valorServicos: 'valor dos serviços',
  discriminacao: 'descrição do serviço',
  informacoesComplementares: 'informações complementares',
  tributos: 'tributos',
  issRetido: 'ISS retido',
  aliquotaIss: 'alíquota do ISS',
  valorBaseCalculoIss: 'base de cálculo do ISS',
  valorIssRetido: 'valor do ISS retido',
  cstPisCofins: 'CST do PIS/COFINS',
  tipoRetencao: 'tipo de retenção',
  valorBaseCalculo: 'base de cálculo federal',
  aliquotaPis: 'alíquota do PIS',
  aliquotaCofins: 'alíquota da COFINS',
  valorPis: 'PIS retido',
  valorCofins: 'COFINS retida',
  valorInss: 'INSS retido',
  valorIr: 'IR retido',
  valorCsll: 'CSLL retida',
  valorDescontoIncondicionado: 'desconto incondicionado',
  codigoTributacaoMunicipio: 'código de tributação municipal do serviço',
  codigoMotivo: 'tipo do motivo',
  motivo: 'justificativa',
}

function nomesDosCampos(detalhes?: Record<string, unknown>): string[] {
  const campos = detalhes?.campos
  const lista: unknown[] = Array.isArray(campos) ? campos : campos && typeof campos === 'object' ? Object.keys(campos) : []
  const nomes = lista
    .map((c) => (typeof c === 'string' ? c : (str(rec(c).campo) ?? str(rec(c).mensagem))))
    .filter((c): c is string => !!c)
    .map((c) => CAMPOS[c] ?? c)
  return [...new Set(nomes)]
}

/** Mensagem amigavel por codigo de erro; codigo desconhecido cai na mensagem do back. */
export function mensagemErroNota(e: ApiError): string {
  switch (e.codigo) {
    case 'nfse_nao_habilitada':
      return 'A emissão de NFS-e não está habilitada. Habilite em Configuração > NFS-e.'
    case 'emitente_nao_cadastrado':
      return 'A empresa ainda não está cadastrada como emitente na IntegraNotas. Conclua em Configuração > NFS-e.'
    case 'emitente_de_outro_ambiente':
      return 'O cadastro do emitente é de outro ambiente (homologação ou produção). Refaça o cadastro em Configuração > NFS-e.'
    case 'certificado_nao_valido':
      return 'O certificado digital não está válido. Envie em Configuração > Empresa.'
    case 'servico_inativo':
      return 'O serviço escolhido está inativo. Escolha outro serviço.'
    case 'nota_em_processamento':
      return 'A nota está em processamento. Aguarde a resposta da prefeitura.'
    case 'nota_nao_editavel':
      return 'Esta nota não pode mais ser editada.'
    case 'nota_em_conflito':
      return 'A prefeitura indicou conflito nesta nota. Não reenvie: use Atualizar agora.'
    case 'cancelamento_nao_delegado':
      return e.mensagem || MENSAGEM_NAO_DELEGADO
    case 'nota_nao_autorizada':
      return 'Só é possível cancelar uma nota autorizada.'
    case 'cancelamento_fora_do_mes':
      return MENSAGEM_FORA_DO_MES
    case 'cancelamento_em_andamento':
      return 'Já existe um cancelamento em andamento para esta nota. Use Atualizar agora para consultar.'
    case 'cancelamento_recusado':
      return e.mensagem || 'A prefeitura recusou o cancelamento da nota.'
    case 'emissao_producao_bloqueada':
      return 'Esta operação está bloqueada no ambiente de produção.'
    case 'discriminacao_obrigatoria':
      return 'Informe a descrição do serviço.'
    case 'tomador_incompleto': {
      const nomes = nomesDosCampos(e.detalhes)
      return `${nomes.length > 0 ? `Faltam dados do cliente: ${nomes.join(', ')}.` : 'Faltam dados do cliente.'} Complete o cadastro do cliente.`
    }
    case 'requisicao_invalida': {
      const nomes = nomesDosCampos(e.detalhes)
      return nomes.length > 0 ? `Revise os campos: ${nomes.join(', ')}.` : e.mensagem
    }
    default:
      if (e.status === 403) return 'Você não tem permissão para esta ação.'
      if (e.status === 404) return 'Nota não encontrada.'
      return e.mensagem
  }
}

/** Troca a mensagem do ApiError pela amigavel (para o `serverError` do AutoForm e dos Alerts); outros erros passam como vieram. */
export function erroAmigavel(e: unknown): unknown {
  if (!(e instanceof ApiError)) return e
  return new ApiError(e.status, e.codigo, mensagemErroNota(e), e.detalhes, e.retryAfter)
}
