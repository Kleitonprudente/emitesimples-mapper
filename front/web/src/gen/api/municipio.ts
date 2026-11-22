// GERADO por frontend/apiClient a partir de Municipio. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { Municipio } from '@/gen/schemas/municipio'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type MunicipioRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type MunicipioRow = Municipio & { id: string }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type MunicipioInput = Partial<Omit<MunicipioRow, 'id'>>

const BASE = '/Municipio'

export const municipioKeys = {
  all: ['Municipio'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['Municipio', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['Municipio', 'id', id] as const,
}

/** Lista paginada no servidor: GET /Municipio/from/to + GET /Municipio/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useMunicipios(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: municipioKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<MunicipioRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useMunicipio(id: string | null) {
  return useQuery({ queryKey: municipioKeys.one(id ?? ''), queryFn: () => api<MunicipioRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarMunicipio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: MunicipioInput) => api<MunicipioRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: municipioKeys.all }),
  })
}

export function useAtualizarMunicipio(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: MunicipioInput) => api<MunicipioRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: municipioKeys.all }),
  })
}

export function useRemoverMunicipio() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: municipioKeys.all }),
  })
}
