import { useSyncExternalStore } from 'react'
import { api, configureApi, refreshSession, setAccessToken, type LoginResponse, type PapelValor, type Papel, type SessionData } from '@/lib/api'
import { queryClient } from '@/lib/query-client'

/* Sessao do usuario: dados de exibicao em memoria (o token fica em lib/api.ts, tambem so em memoria).
 * Nada disto vai para localStorage/sessionStorage. */

let session: SessionData | null = null
const listeners = new Set<() => void>()

function setSession(s: SessionData | null): void {
  session = s
  listeners.forEach((l) => l())
}

configureApi({
  onSession: setSession,
  onSessionEnd: () => {
    setSession(null)
    queryClient.clear()
  },
})

export function getSession(): SessionData | null {
  return session
}

export function useSession(): SessionData | null {
  return useSyncExternalStore(
    (cb) => {
      listeners.add(cb)
      return () => listeners.delete(cb)
    },
    () => session,
    () => session,
  )
}

/** Ao carregar a pagina: tenta restaurar a sessao pelo cookie de refresh, uma unica vez. */
let restoring: Promise<SessionData | null> | null = null
export function restoreSession(): Promise<SessionData | null> {
  if (session) return Promise.resolve(session)
  restoring ??= refreshSession().finally(() => {
    restoring = null
  })
  return restoring
}

export interface LoginInput {
  email: string
  senha: string
  empresaId?: string
}

export async function login(input: LoginInput): Promise<SessionData> {
  const data = await api<LoginResponse>('/auth/login', { method: 'POST', body: input, skipAuth: true })
  setAccessToken(data.accessToken)
  const { accessToken: _t, expiresIn: _e, ...s } = data
  queryClient.clear()
  setSession(s)
  return s
}

export async function logout(): Promise<void> {
  try {
    await api('/auth/logout', { method: 'POST', skipAuth: true })
  } catch {
    // Mesmo se o servidor falhar, a sessao local acaba.
  }
  setAccessToken(null)
  setSession(null)
  queryClient.clear()
}

export async function trocarSenha(input: { senhaAtual?: string; novaSenha: string }): Promise<void> {
  await api('/auth/senha', { method: 'POST', body: input })
}

const RANK: Record<PapelValor, number> = { CONSULTA: 1, OPERADOR: 2, ADMINISTRADOR: 3 }

/** Papel minimo exigido. So UX: o back decide. */
export function hasRole(s: SessionData | null, min: Papel): boolean {
  return !!s?.papel && RANK[s.papel] >= RANK[min]
}

/** Pode cancelar NFS-e? Leitura tolerante: sem o campo na sessao, so administrador (e plataforma). So UX: o back decide (403 cancelamento_nao_delegado). */
export function podeCancelarNfse(s: SessionData | null): boolean {
  if (!s) return false
  if (typeof s.podeCancelarNfse === 'boolean') return s.podeCancelarNfse
  return s.plataformaAdmin || s.papel === 'ADMINISTRADOR'
}

export const PAPEL_LABEL: Record<PapelValor, string> = {
  ADMINISTRADOR: 'Administrador',
  OPERADOR: 'Operador',
  CONSULTA: 'Consulta',
}

/**
 * Pode agir na tela? Tela so da plataforma (`platformOnly`): vale o administrador da plataforma, que nao tem papel de empresa.
 * Demais telas: papel minimo da empresa. So UX: o back decide.
 */
export function canAct(s: SessionData | null, min: Papel, platformOnly = false): boolean {
  return platformOnly ? !!s?.plataformaAdmin : hasRole(s, min)
}
