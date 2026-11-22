// GERADO por frontend/apiClient a partir de Empresa. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { Empresa } from '@/gen/schemas/empresa'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type EmpresaRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type EmpresaRow = Empresa & { id: string; atividades?: EmpresaRef[]; qsa?: EmpresaRef[]; municipio?: EmpresaRef }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type EmpresaInput = Partial<Omit<EmpresaRow, 'id'>>

const BASE = '/Empresa'

export const empresaKeys = {
  all: ['Empresa'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['Empresa', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['Empresa', 'id', id] as const,
}

/** Lista paginada no servidor: GET /Empresa/from/to + GET /Empresa/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function useEmpresas(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: empresaKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<EmpresaRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function useEmpresa(id: string | null) {
  return useQuery({ queryKey: empresaKeys.one(id ?? ''), queryFn: () => api<EmpresaRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarEmpresa() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: EmpresaInput) => api<EmpresaRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: empresaKeys.all }),
  })
}

export function useAtualizarEmpresa(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: EmpresaInput) => api<EmpresaRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: empresaKeys.all }),
  })
}

export function useRemoverEmpresa() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: empresaKeys.all }),
  })
}
