// GERADO por frontend/apiClient a partir de ContaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { ContaFinanceira } from '@/gen/schemas/contaFinanceira'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type ContaFinanceiraRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type ContaFinanceiraRow = ContaFinanceira & { id: string; empresa?: ContaFinanceiraRef }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type ContaFinanceiraInput = Partial<Omit<ContaFinanceiraRow, 'id'>>

const BASE = '/ContaFinanceira'

export const contaFinanceiraKeys = {
  all: ['ContaFinanceira'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['ContaFinanceira', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['ContaFinanceira', 'id', id] as const,
}

/** Lista paginada no servidor: GET /ContaFinanceira/from/to + GET /ContaFinanceira/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useContaFinanceiras(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: contaFinanceiraKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<ContaFinanceiraRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useContaFinanceira(id: string | null) {
  return useQuery({ queryKey: contaFinanceiraKeys.one(id ?? ''), queryFn: () => api<ContaFinanceiraRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarContaFinanceira() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: ContaFinanceiraInput) => api<ContaFinanceiraRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: contaFinanceiraKeys.all }),
  })
}

export function useAtualizarContaFinanceira(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: ContaFinanceiraInput) => api<ContaFinanceiraRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: contaFinanceiraKeys.all }),
  })
}

export function useRemoverContaFinanceira() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: contaFinanceiraKeys.all }),
  })
}
