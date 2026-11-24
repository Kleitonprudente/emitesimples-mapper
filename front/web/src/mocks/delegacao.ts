import type { PapelValor } from '@/lib/api'

/* ---------------------------------------------------------------------------
 * Delegacao de "cancelar NFS-e" no modo mock: UM estado compartilhado por (usuario, empresa), lido pela sessao (login/refresh/me),
 * pelo cancelamento de nota e escrito pelos mocks de vinculo (/admin/usuarios/{id}/vinculos e /admin/empresas/{id}/vinculos).
 * Administrador (e plataforma) sempre pode; operador so com a delegacao. Seed: Diego (usu-4) e operador delegado na emp-2 (operador@ da emp-1 nao e delegado).
 * ------------------------------------------------------------------------- */

const seed = (): Record<string, boolean> => ({ 'usu-4:emp-2': true })

let delegacoes = seed()

export function resetDelegacao(): void {
  delegacoes = seed()
}

export function definirDelegacao(usuarioId: string, empresaId: string, valor: boolean): void {
  delegacoes[`${usuarioId}:${empresaId}`] = valor
}

/** Valor armazenado (sem considerar o papel). */
export const delegacaoDe = (usuarioId: string, empresaId: string): boolean => delegacoes[`${usuarioId}:${empresaId}`] === true

/** Valor efetivo: administrador sempre true. */
export const podeCancelarNfseMock = (usuarioId: string, empresaId: string | null, papel: PapelValor | null): boolean =>
  papel === 'ADMINISTRADOR' || (!!empresaId && papel === 'OPERADOR' && delegacaoDe(usuarioId, empresaId))
