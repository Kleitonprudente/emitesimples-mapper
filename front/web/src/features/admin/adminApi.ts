import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type Papel } from '@/lib/api'
import type { FkSource } from '@/runtime/Combobox'
import type { TableState } from '@/runtime/table-state'

/* ---------------------------------------------------------------------------
 * adminApi.ts: TODAS as chamadas das telas de administrador da plataforma ficam AQUI.
 * Contrato CONFIRMADO com o back (back/api/README.md, secao admin): ajuste so este arquivo quando ele mudar.
 *
 *   Usuarios
 *     GET  /admin/usuarios/{from}/{to}?busca&sortBy&sortDirection  -> [{id, nome, email, whatsapp?, ativo?, dataCriacao?}]
 *     GET  /admin/usuarios/count?busca                             -> {count}
 *     POST /admin/usuarios {nome, email, whatsapp?, senha, ativo, vinculos?:[{empresaId, papel}]}
 *     PATCH /admin/usuarios/{id} {nome?, whatsapp?, ativo?}        (whatsapp '' apaga; 400 auto_desativacao)
 *     POST /admin/usuarios/{id}/senha {senha}                       -> 204
 *     GET  /admin/usuarios/{id}/vinculos                           -> [{id, empresaId, empresa?:{razaoSocial}, papel, ativo}]
 *     POST /admin/usuarios/{id}/vinculos {empresaId, papel}
 *     PATCH /admin/usuarios/{id}/vinculos/{vinculoId} {papel?, ativo?}   (nunca exclui: so desativa)
 *     GET  /Empresa?razaoSocial=*termo*&sortBy=razaoSocial         -> [{id, razaoSocial}]   (Combobox de empresa)
 *   Municipios
 *     POST /admin/municipios/importacao  multipart, campo `arquivo` -> {id, linhasLidas, criados, atualizados, inalterados, rejeitados:[{linha, motivo}], totalRejeitados, nomeArquivo, estado, dataHoraInicio}
 *     GET  /admin/municipios/importacoes?limite=20                 -> [{id, nomeArquivo, dataHoraInicio, usuario:{nome}, linhasLidas, ..., totalRejeitados}]
 *
 * As respostas sao lidas de forma TOLERANTE (campo ausente = omitido na tela).
 * Senhas: so trafegam no corpo do envio; nunca sao guardadas nem exibidas.
 * ------------------------------------------------------------------------- */

const rec = (v: unknown): Record<string, unknown> => (v && typeof v === 'object' ? (v as Record<string, unknown>) : {})
const str = (v: unknown): string | undefined => (typeof v === 'string' && v !== '' ? v : v == null ? undefined : String(v))
const num = (v: unknown): number | undefined => (typeof v === 'number' && Number.isFinite(v) ? v : undefined)

/* ============================== usuarios ============================== */

export interface Usuario {
  id: string
  nome: string
  email: string
  whatsapp?: string
  /** Ausente na resposta = ativo. */
  ativo: boolean
  dataCriacao?: string
}

export function lerUsuario(raw: unknown): Usuario {
  const r = rec(raw)
  return {
    id: String(r.id ?? ''),
    nome: str(r.nome) ?? '',
    email: str(r.email) ?? '',
    whatsapp: str(r.whatsapp),
    ativo: r.ativo !== false,
    dataCriacao: str(r.dataCriacao),
  }
}

export interface Vinculo {
  id: string
  empresaId: string
  empresaNome: string
  papel: Papel
  ativo: boolean
}

export function lerVinculo(raw: unknown): Vinculo {
  const r = rec(raw)
  const empresa = rec(r.empresa)
  const empresaId = str(r.empresaId) ?? str(empresa.id) ?? ''
  return {
    id: String(r.id ?? ''),
    empresaId,
    empresaNome: str(empresa.razaoSocial) ?? str(r.empresaRazaoSocial) ?? str(r.razaoSocial) ?? empresaId,
    papel: (str(r.papel) ?? 'consulta') as Papel,
    ativo: r.ativo !== false,
  }
}

export const PAPEIS: { value: Papel; label: string }[] = [
  { value: 'administrador', label: 'Administrador' },
  { value: 'operador', label: 'Operador' },
  { value: 'consulta', label: 'Consulta' },
]

export const adminKeys = {
  usuarios: ['admin', 'usuarios'] as const,
  lista: (s: TableState) => ['admin', 'usuarios', 'lista', s] as const,
  vinculos: (id: string) => ['admin', 'usuarios', id, 'vinculos'] as const,
  importacoes: ['admin', 'municipios', 'importacoes'] as const,
}

/** Lista paginada no servidor: GET /admin/usuarios/{from}/{to} + /count, com `busca` por nome/e-mail. */
export function useUsuarios(state: TableState) {
  const from = state.page * state.pageSize
  const to = from + state.pageSize - 1
  const busca = state.search.trim() || undefined
  const q = useQuery({
    queryKey: adminKeys.lista(state),
    queryFn: async () => {
      const [rows, total] = await Promise.all([
        api<unknown[]>(`/admin/usuarios/${from}/${to}`, { query: { busca, sortBy: state.sortBy, sortDirection: state.sortBy ? state.sortDirection : undefined } }),
        api<{ count: number }>('/admin/usuarios/count', { query: { busca } }),
      ])
      return { data: (rows ?? []).map(lerUsuario), count: total.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export interface NovoUsuario {
  nome: string
  email: string
  whatsapp?: string
  senha: string
  ativo: boolean
  vinculos?: { empresaId: string; papel: Papel }[]
}

export function useCriarUsuario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: NovoUsuario) => api<unknown>('/admin/usuarios', { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.usuarios }),
  })
}

export function useEditarUsuario(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { nome: string; whatsapp?: string; ativo: boolean }) =>
      api<unknown>(`/admin/usuarios/${id}`, { method: 'PATCH', body: { nome: v.nome, whatsapp: v.whatsapp ?? '', ativo: v.ativo } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.usuarios }),
  })
}

export function useRedefinirSenha(id: string) {
  return useMutation({
    mutationFn: (novaSenha: string) => api<void>(`/admin/usuarios/${id}/senha`, { method: 'POST', body: { senha: novaSenha } }),
  })
}

export function useVinculos(usuarioId: string) {
  return useQuery({
    queryKey: adminKeys.vinculos(usuarioId),
    queryFn: async () => ((await api<unknown[]>(`/admin/usuarios/${usuarioId}/vinculos`)) ?? []).map(lerVinculo),
  })
}

export function useAdicionarVinculo(usuarioId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { empresaId: string; papel: Papel }) => api<unknown>(`/admin/usuarios/${usuarioId}/vinculos`, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.vinculos(usuarioId) }),
  })
}

export function useAtualizarVinculo(usuarioId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { vinculoId: string; papel: Papel; ativo: boolean }) =>
      api<unknown>(`/admin/usuarios/${usuarioId}/vinculos/${v.vinculoId}`, { method: 'PATCH', body: { papel: v.papel, ativo: v.ativo } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.vinculos(usuarioId) }),
  })
}

/** FK de Empresa (Combobox dos vinculos). */
export const empresaFk: FkSource = {
  key: 'Empresa',
  search: async (term) => {
    const rows = await api<{ id: string; razaoSocial: string }[]>('/Empresa', { query: { razaoSocial: term.trim() ? `*${term.trim()}*` : undefined, sortBy: 'razaoSocial' } })
    return rows.map((e) => ({ id: e.id, label: e.razaoSocial }))
  },
  labelOf: (v) => str(v.razaoSocial),
}

/* ======================= importacao de municipios ======================= */

export interface Rejeitado {
  linha?: number
  motivo: string
}

export interface Importacao {
  id: string
  dataHora?: string
  arquivo?: string
  usuario?: string
  linhasLidas?: number
  criados?: number
  atualizados?: number
  inalterados?: number
  /** Quantidade de linhas rejeitadas (do total devolvido, mesmo se a lista nao vier). */
  totalRejeitados?: number
  /** Detalhe (linha e motivo); so a resposta da importacao traz. */
  rejeitados: Rejeitado[]
}

/** Aceita `rejeitados` como lista ou como contagem, e campos ausentes. */
export function lerImportacao(raw: unknown): Importacao {
  const r = rec(raw)
  const lista = Array.isArray(r.rejeitados) ? r.rejeitados.map((x) => ({ linha: num(rec(x).linha), motivo: str(rec(x).motivo) ?? 'Linha rejeitada.' })) : []
  return {
    id: String(r.id ?? ''),
    dataHora: str(r.dataHoraInicio) ?? str(r.dataHora) ?? str(r.criadoEm) ?? str(r.dataCriacao) ?? str(r.data),
    arquivo: str(r.arquivo) ?? str(r.nomeArquivo),
    usuario: str(rec(r.usuario).nome) ?? str(r.usuario) ?? str(r.usuarioNome),
    linhasLidas: num(r.linhasLidas),
    criados: num(r.criados),
    atualizados: num(r.atualizados),
    inalterados: num(r.inalterados),
    totalRejeitados: num(r.totalRejeitados) ?? (Array.isArray(r.rejeitados) ? lista.length : num(r.rejeitados)),
    rejeitados: lista,
  }
}

export function useImportacoes() {
  return useQuery({
    queryKey: adminKeys.importacoes,
    queryFn: async () => ((await api<unknown[]>('/admin/municipios/importacoes')) ?? []).map(lerImportacao),
  })
}

export function useImportarMunicipios() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (arquivo: File) => {
      const form = new FormData()
      form.append('arquivo', arquivo)
      return lerImportacao(await api<unknown>('/admin/municipios/importacao', { method: 'POST', body: form }))
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.importacoes }),
  })
}
