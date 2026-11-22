// GERADO por frontend/apiClient a partir de Servico. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { Servico } from '@/gen/schemas/servico'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type ServicoRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type ServicoRow = Servico & { id: string; empresa?: ServicoRef }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type ServicoInput = Partial<Omit<ServicoRow, 'id'>>

const BASE = '/Servico'

export const servicoKeys = {
  all: ['Servico'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['Servico', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['Servico', 'id', id] as const,
}

/** Lista paginada no servidor: GET /Servico/from/to + GET /Servico/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useServicos(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: servicoKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<ServicoRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useServico(id: string | null) {
  return useQuery({ queryKey: servicoKeys.one(id ?? ''), queryFn: () => api<ServicoRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarServico() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: ServicoInput) => api<ServicoRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: servicoKeys.all }),
  })
}

export function useAtualizarServico(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: ServicoInput) => api<ServicoRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: servicoKeys.all }),
  })
}

export function useRemoverServico() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: servicoKeys.all }),
  })
}
