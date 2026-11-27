// GERADO por frontend/apiClient a partir de CondicaoPagamento. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { CondicaoPagamento } from '@/gen/schemas/condicaoPagamento'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type CondicaoPagamentoRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type CondicaoPagamentoRow = CondicaoPagamento & { id: string; empresa?: CondicaoPagamentoRef }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type CondicaoPagamentoInput = Partial<Omit<CondicaoPagamentoRow, 'id'>>

const BASE = '/CondicaoPagamento'

export const condicaoPagamentoKeys = {
  all: ['CondicaoPagamento'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['CondicaoPagamento', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['CondicaoPagamento', 'id', id] as const,
}

/** Lista paginada no servidor: GET /CondicaoPagamento/from/to + GET /CondicaoPagamento/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useCondicaoPagamentos(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: condicaoPagamentoKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<CondicaoPagamentoRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useCondicaoPagamento(id: string | null) {
  return useQuery({ queryKey: condicaoPagamentoKeys.one(id ?? ''), queryFn: () => api<CondicaoPagamentoRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarCondicaoPagamento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: CondicaoPagamentoInput) => api<CondicaoPagamentoRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: condicaoPagamentoKeys.all }),
  })
}

export function useAtualizarCondicaoPagamento(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: CondicaoPagamentoInput) => api<CondicaoPagamentoRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: condicaoPagamentoKeys.all }),
  })
}

export function useRemoverCondicaoPagamento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: condicaoPagamentoKeys.all }),
  })
}
