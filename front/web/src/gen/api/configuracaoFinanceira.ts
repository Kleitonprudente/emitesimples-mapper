// GERADO por frontend/apiClient a partir de ConfiguracaoFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { ConfiguracaoFinanceira } from '@/gen/schemas/configuracaoFinanceira'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type ConfiguracaoFinanceiraRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type ConfiguracaoFinanceiraRow = ConfiguracaoFinanceira & { id: string; categoriaPadraoReceita?: ConfiguracaoFinanceiraRef; categoriaPadraoDespesa?: ConfiguracaoFinanceiraRef; empresa?: ConfiguracaoFinanceiraRef }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type ConfiguracaoFinanceiraInput = Partial<Omit<ConfiguracaoFinanceiraRow, 'id'>>

const BASE = '/ConfiguracaoFinanceira'

export const configuracaoFinanceiraKeys = {
  all: ['ConfiguracaoFinanceira'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['ConfiguracaoFinanceira', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['ConfiguracaoFinanceira', 'id', id] as const,
}

/** Lista paginada no servidor: GET /ConfiguracaoFinanceira/from/to + GET /ConfiguracaoFinanceira/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useConfiguracaoFinanceiras(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: configuracaoFinanceiraKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<ConfiguracaoFinanceiraRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useConfiguracaoFinanceira(id: string | null) {
  return useQuery({ queryKey: configuracaoFinanceiraKeys.one(id ?? ''), queryFn: () => api<ConfiguracaoFinanceiraRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarConfiguracaoFinanceira() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: ConfiguracaoFinanceiraInput) => api<ConfiguracaoFinanceiraRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: configuracaoFinanceiraKeys.all }),
  })
}

export function useAtualizarConfiguracaoFinanceira(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: ConfiguracaoFinanceiraInput) => api<ConfiguracaoFinanceiraRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: configuracaoFinanceiraKeys.all }),
  })
}

export function useRemoverConfiguracaoFinanceira() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: configuracaoFinanceiraKeys.all }),
  })
}
