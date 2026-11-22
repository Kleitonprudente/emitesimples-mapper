import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'
import { api } from '@/lib/api'
import { fkSource } from '@/lib/fk-sources'
import type { FkSource } from '@/runtime/Combobox'
import { toListRequest, type TableState } from '@/runtime/table-state'

/* ESCRITO A MAO, so para provar o runtime. Quando os geradores de dominio existirem, isto vira `gen/schemas` + `gen/api`. */

export const TIPOS_PESSOA = [
  { value: 'fisica', label: 'Pessoa física' },
  { value: 'juridica', label: 'Pessoa jurídica' },
]

const digits = (s: string) => s.replace(/\D/g, '')

export const pessoaSchema = z.object({
  tipoPessoa: z.enum(['fisica', 'juridica'], { error: 'Escolha o tipo de pessoa.' }),
  documento: z.string().refine((s) => [11, 14].includes(digits(s).length), 'Informe um CPF (11 dígitos) ou CNPJ (14 dígitos).'),
  nome: z.string().min(1, 'Informe o nome.').max(200, 'Máximo de 200 caracteres.'),
  email: z.email('Informe um e-mail válido.').optional(),
  telefone: z.string().regex(/^\d{10,11}$/, 'Telefone com DDD (10 ou 11 dígitos).').optional(),
  cep: z.string().regex(/^\d{8}$/, 'CEP com 8 dígitos.').optional(),
  cidade: z.object({ id: z.string() }).optional(),
})
export type PessoaInput = z.infer<typeof pessoaSchema>

export interface PessoaRow extends PessoaInput {
  id: string
  dataCriacao: string
  cidade?: { id: string; nome?: string; uf?: string }
}

export const pessoaKeys = {
  all: ['Pessoa'] as const,
  list: (s: TableState) => ['Pessoa', 'list', s] as const,
  one: (id: string) => ['Pessoa', 'id', id] as const,
}

/** Lista paginada no servidor: GET /Pessoa/{from}/{to} + GET /Pessoa/count. */
export function usePessoas(state: TableState) {
  const req = toListRequest(state, 'nome')
  const q = useQuery({
    queryKey: pessoaKeys.list(state),
    queryFn: async () => {
      const [data, count] = await Promise.all([
        api<PessoaRow[]>(`/Pessoa${req.rangePath}`, { query: req.listQuery }),
        api<{ count: number }>('/Pessoa/count', { query: req.countQuery }),
      ])
      return { data, count: count.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export function usePessoa(id: string | null) {
  return useQuery({ queryKey: pessoaKeys.one(id ?? ''), queryFn: () => api<PessoaRow>(`/Pessoa/id/${id}`), enabled: !!id })
}

export function useSalvarPessoa(id: string | null) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: PessoaInput) => (id ? api<PessoaRow>(`/Pessoa/id/${id}`, { method: 'PUT', body: v }) : api<PessoaRow>('/Pessoa', { method: 'POST', body: v })),
    onSuccess: () => qc.invalidateQueries({ queryKey: pessoaKeys.all }),
  })
}

export function useRemoverPessoa() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api<void>(`/Pessoa/id/${id}`, { method: 'DELETE' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: pessoaKeys.all }),
  })
}

/** FK Pessoa.cidade -> Municipio, com busca assincrona por nome (registro central em lib/fk-sources). */
export const municipioFk: FkSource = fkSource('Municipio')
