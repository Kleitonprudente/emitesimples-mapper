// GERADO por frontend/apiClient a partir de Pessoa. Nao edite: a saida e sobrescrita por inteiro.
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { toListRequest, type TableState } from '@/runtime/table-state'
import type { Pessoa } from '@/gen/schemas/pessoa'

/** Referencia a outra entidade: o back devolve o objeto e recebe so o id. */
export type PessoaRef = { id: string } & Record<string, unknown>

/** Registro como o back devolve: campos do schema mais as relacoes (omitidas do schema). */
export type PessoaRow = Pessoa & { id: string; empresa?: PessoaRef; cidade?: PessoaRef; contatos?: PessoaRef[] }

/** Corpo de criacao e atualizacao: Partial, o zod do formulario valida os obrigatorios. */
export type PessoaInput = Partial<Omit<PessoaRow, 'id'>>

const BASE = '/Pessoa'

export const pessoaKeys = {
  all: ['Pessoa'] as const,
  list: (s: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) => ['Pessoa', 'list', s, searchField, filtros] as const,
  one: (id: string) => ['Pessoa', 'id', id] as const,
}

/** Lista paginada no servidor: GET /Pessoa/from/to + GET /Pessoa/count. `filtros` (opcional) entra na querystring da lista e da contagem e na chave de cache. */
export function usePessoas(state: TableState, searchField?: string, filtros?: Record<string, string | number | boolean>) {
  const req = toListRequest(state, searchField)
  const q = useQuery({
    queryKey: pessoaKeys.list(state, searchField, filtros),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<PessoaRow[]>(`${BASE}${req.rangePath}`, { query: { ...req.listQuery, ...filtros } }),
        api<{ count: number }>(`${BASE}/count`, { query: { ...req.countQuery, ...filtros } }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function usePessoa(id: string | null) {
  return useQuery({ queryKey: pessoaKeys.one(id ?? ''), queryFn: () => api<PessoaRow>(`${BASE}/id/${id}`), enabled: !!id })
}

export function useCriarPessoa() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: PessoaInput) => api<PessoaRow>(BASE, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: pessoaKeys.all }),
  })
}

export function useAtualizarPessoa(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: PessoaInput) => api<PessoaRow>(`${BASE}/id/${id}`, { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: pessoaKeys.all }),
  })
}

export function useRemoverPessoa() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`${BASE}/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: pessoaKeys.all }),
  })
}
