import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type Papel } from '@/lib/api'
import { lerUsuario } from '@/features/admin/adminApi'
import type { FkSource } from '@/runtime/Combobox'

/* ---------------------------------------------------------------------------
 * Vinculos de usuario de UMA empresa (aba "Usuarios" do cadastro de Empresa; so plataforma).
 *   GET   /admin/empresas/{empresaId}/vinculos -> [{id, usuario:{id,nome,email,ativo}, papel, ativo, dataCriacao}]
 *   POST  /admin/empresas/{empresaId}/vinculos {usuarioId, papel, ativo?}  -> 201 (409 vinculo_ja_existe, 404 usuario_nao_encontrado, 400 papel invalido)
 *   PATCH /admin/empresas/{empresaId}/vinculos/{vinculoId} {papel?, ativo?}  (nunca exclui: so desativa)
 *   GET   /admin/usuarios/{from}/{to}?busca&ativo=true -> busca do usuario a vincular (rota ja usada por adminApi)
 * ------------------------------------------------------------------------- */

export interface VinculoDaEmpresa {
  id: string
  usuario: { id: string; nome: string; email: string; ativo: boolean }
  papel: Papel
  ativo: boolean
  dataCriacao?: string
}

const rec = (v: unknown): Record<string, unknown> => (v && typeof v === 'object' ? (v as Record<string, unknown>) : {})

export function lerVinculoDaEmpresa(raw: unknown): VinculoDaEmpresa {
  const r = rec(raw)
  const u = lerUsuario(r.usuario)
  return {
    id: String(r.id ?? ''),
    usuario: { id: u.id, nome: u.nome, email: u.email, ativo: u.ativo },
    papel: (typeof r.papel === 'string' ? r.papel : 'consulta') as Papel,
    ativo: r.ativo !== false,
    dataCriacao: typeof r.dataCriacao === 'string' ? r.dataCriacao : undefined,
  }
}

export const vinculosEmpresaKeys = {
  lista: (empresaId: string) => ['admin', 'empresas', empresaId, 'vinculos'] as const,
}

export function useVinculosDaEmpresa(empresaId: string) {
  return useQuery({
    queryKey: vinculosEmpresaKeys.lista(empresaId),
    queryFn: async () => ((await api<unknown[]>(`/admin/empresas/${empresaId}/vinculos`)) ?? []).map(lerVinculoDaEmpresa),
  })
}

export function useVincularUsuario(empresaId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { usuarioId: string; papel: Papel; ativo?: boolean }) => api<unknown>(`/admin/empresas/${empresaId}/vinculos`, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: vinculosEmpresaKeys.lista(empresaId) }),
  })
}

export function useAtualizarVinculoDaEmpresa(empresaId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { vinculoId: string; papel?: Papel; ativo?: boolean }) =>
      api<unknown>(`/admin/empresas/${empresaId}/vinculos/${v.vinculoId}`, { method: 'PATCH', body: { papel: v.papel, ativo: v.ativo } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: vinculosEmpresaKeys.lista(empresaId) }),
  })
}

/** FK de usuario ATIVO para o Combobox (busca por nome/e-mail); `excluirIds` tira quem ja esta vinculado. */
export function usuarioAtivoFk(empresaId: string, excluirIds: string[]): FkSource {
  return {
    key: `UsuarioVinculavel:${empresaId}:${excluirIds.join(',')}`,
    search: async (term) => {
      const rows = await api<unknown[]>('/admin/usuarios/0/29', { query: { busca: term.trim() || undefined, ativo: true, sortBy: 'nome' } })
      return (rows ?? [])
        .map(lerUsuario)
        .filter((u) => u.ativo && !excluirIds.includes(u.id))
        .map((u) => ({ id: u.id, label: `${u.nome} (${u.email})` }))
    },
  }
}
