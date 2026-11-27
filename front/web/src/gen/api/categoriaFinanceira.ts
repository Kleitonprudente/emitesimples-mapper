// GERADO por frontend/apiClient a partir de CategoriaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { CategoriaFinanceira } from '@/gen/schemas/categoriaFinanceira'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type CategoriaFinanceiraRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type CategoriaFinanceiraRow = CategoriaFinanceira & { id: string; categoriaPai?: CategoriaFinanceiraRef; empresa?: CategoriaFinanceiraRef }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type CategoriaFinanceiraInput = Partial<Omit<CategoriaFinanceiraRow, 'id'>>

const BASE = '/CategoriaFinanceira'

export const categoriaFinanceiraKeys = {
  all: ['CategoriaFinanceira'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['CategoriaFinanceira', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['CategoriaFinanceira', 'id', id] as const,
}

/** Lista paginada no servidor: GET /CategoriaFinanceira/from/to + GET /CategoriaFinanceira/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useCategoriaFinanceiras(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: categoriaFinanceiraKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<CategoriaFinanceiraRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useCategoriaFinanceira(id: string | null) {
  return useQuery({ queryKey: categoriaFinanceiraKeys.one(id ?? ''), queryFn: () => api<CategoriaFinanceiraRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarCategoriaFinanceira() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: CategoriaFinanceiraInput) => api<CategoriaFinanceiraRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: categoriaFinanceiraKeys.all }),
  })
}

export function useAtualizarCategoriaFinanceira(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: CategoriaFinanceiraInput) => api<CategoriaFinanceiraRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: categoriaFinanceiraKeys.all }),
  })
}

export function useRemoverCategoriaFinanceira() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: categoriaFinanceiraKeys.all }),
  })
}
