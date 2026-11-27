// GERADO por frontend/apiClient a partir de Banco. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { Banco } from '@/gen/schemas/banco'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type BancoRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type BancoRow = Banco & { id: string }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type BancoInput = Partial<Omit<BancoRow, 'id'>>

const BASE = '/Banco'

export const bancoKeys = {
  all: ['Banco'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['Banco', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['Banco', 'id', id] as const,
}

/** Lista paginada no servidor: GET /Banco/from/to + GET /Banco/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useBancos(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: bancoKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<BancoRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useBanco(id: string | null) {
  return useQuery({ queryKey: bancoKeys.one(id ?? ''), queryFn: () => api<BancoRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarBanco() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: BancoInput) => api<BancoRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bancoKeys.all }),
  })
}

export function useAtualizarBanco(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: BancoInput) => api<BancoRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bancoKeys.all }),
  })
}

export function useRemoverBanco() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: bancoKeys.all }),
  })
}
