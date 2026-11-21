import { API_BASE } from '@/config/env'

/* ---------------------------------------------------------------------------
 * lib/api.ts: UNICO ponto de fetch do front.
 *  - credentials 'include' (o refresh token e cookie httpOnly; o JS nunca o ve)
 *  - Authorization Bearer com o accessToken, que vive SO EM MEMORIA
 *  - 401: tenta 1 refresh (fila: chamadas concorrentes esperam o mesmo) e repete 1 vez
 *  - falhou: limpa a sessao e chama o handler de "sessao expirada" (vai para /login?redirect=)
 *  - erros viram ApiError(status, codigo, mensagem, detalhes)
 *  - modo mock: o registry intercepta ANTES do fetch e devolve uma Response normal
 * ------------------------------------------------------------------------- */

export class ApiError extends Error {
  readonly status: number
  readonly codigo: string
  readonly detalhes?: Record<string, unknown>
  /** Segundos de espera (cabecalho Retry-After), quando houver. */
  readonly retryAfter?: number

  constructor(status: number, codigo: string, mensagem: string, detalhes?: Record<string, unknown>, retryAfter?: number) {
    super(mensagem)
    this.name = 'ApiError'
    this.status = status
    this.codigo = codigo
    this.detalhes = detalhes
    this.retryAfter = retryAfter
  }

  /** Alias em pt-BR de `message`. */
  get mensagem(): string {
    return this.message
  }
}

export type Papel = 'administrador' | 'operador' | 'consulta'

export interface SessionData {
  usuario: { id: string; nome: string; email: string }
  empresa: { id: string; razaoSocial: string; nomeFantasia: string | null } | null
  papel: Papel | null
  plataformaAdmin: boolean
}

export interface LoginResponse extends SessionData {
  accessToken: string
  expiresIn: number
}

/* ----- estado em memoria ----- */

let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}
export function setAccessToken(token: string | null): void {
  accessToken = token
}

/** Chamado quando um refresh entrega uma sessao nova. */
let onSession: (s: SessionData) => void = () => {}
/** Chamado quando a sessao acabou (limpar estado e cache). */
let onSessionEnd: () => void = () => {}
/** Navegacao para o login. Em runtime e trocado pelo router (main.tsx). */
let onUnauthorized: (redirect: string) => void = (redirect) => {
  window.location.assign(`/login?redirect=${encodeURIComponent(redirect)}`)
}

export function configureApi(handlers: {
  onSession?: (s: SessionData) => void
  onSessionEnd?: () => void
  onUnauthorized?: (redirect: string) => void
}): void {
  if (handlers.onSession) onSession = handlers.onSession
  if (handlers.onSessionEnd) onSessionEnd = handlers.onSessionEnd
  if (handlers.onUnauthorized) onUnauthorized = handlers.onUnauthorized
}

/* ----- nucleo ----- */

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  query?: Record<string, string | number | boolean | null | undefined>
  signal?: AbortSignal
  /** Rotas publicas de auth (login/refresh/logout): sem Bearer e sem ciclo de refresh. */
  skipAuth?: boolean
}

const MENSAGEM_PADRAO = 'Não foi possível concluir a operação. Tente novamente.'

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const base = `${API_BASE}${path.startsWith('/') ? path : `/${path}`}`
  if (!query) return base
  const params = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== '') params.set(k, String(v))
  }
  const qs = params.toString()
  return qs ? `${base}?${qs}` : base
}

async function send(path: string, opts: RequestOptions, token: string | null): Promise<Response> {
  const method = opts.method ?? 'GET'
  // Comparacao literal com a variavel do Vite: em build nao-mock o bloco e removido e o chunk do mock nem e gerado.
  if (import.meta.env.VITE_APP_ENV === 'mock') {
    const { handleMock } = await import('@/mocks/registry')
    return handleMock({ method, path, query: opts.query, body: opts.body, token })
  }
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (opts.body !== undefined) headers['Content-Type'] = 'application/json'
  if (token && !opts.skipAuth) headers.Authorization = `Bearer ${token}`
  try {
    return await fetch(buildUrl(path, opts.query), {
      method,
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
      credentials: 'include',
      signal: opts.signal,
    })
  } catch (e) {
    if (e instanceof DOMException && e.name === 'AbortError') throw e
    throw new ApiError(0, 'sem_conexao', 'Sem conexão com o servidor. Verifique sua internet e tente novamente.')
  }
}

async function toError(res: Response): Promise<ApiError> {
  let body: { codigo?: string; mensagem?: string; detalhes?: Record<string, unknown> } | null = null
  try {
    body = await res.json()
  } catch {
    body = null
  }
  const ra = Number(res.headers.get('Retry-After'))
  return new ApiError(
    res.status,
    body?.codigo ?? 'erro_desconhecido',
    body?.mensagem ?? MENSAGEM_PADRAO,
    body?.detalhes,
    Number.isFinite(ra) && ra > 0 ? ra : undefined,
  )
}

async function parse<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/* ----- refresh com fila ----- */

let refreshing: Promise<SessionData | null> | null = null

/**
 * Renova o accessToken usando o cookie de refresh. Chamadas concorrentes compartilham a MESMA requisicao.
 * Devolve a sessao ou null (sem cookie/expirado). Nao navega: quem decide e o chamador.
 */
export function refreshSession(): Promise<SessionData | null> {
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const res = await send('/auth/refresh', { method: 'POST', skipAuth: true }, null)
        if (!res.ok) {
          setAccessToken(null)
          return null
        }
        const data = await parse<LoginResponse>(res)
        setAccessToken(data.accessToken)
        const { accessToken: _t, expiresIn: _e, ...session } = data
        onSession(session)
        return session
      } catch {
        setAccessToken(null)
        return null
      } finally {
        refreshing = null
      }
    })()
  }
  return refreshing
}

function endSession(): void {
  setAccessToken(null)
  onSessionEnd()
  const redirect = `${window.location.pathname}${window.location.search}`
  onUnauthorized(redirect)
}

export async function api<T = unknown>(path: string, opts: RequestOptions = {}): Promise<T> {
  let res = await send(path, opts, accessToken)

  if (res.status === 401 && !opts.skipAuth) {
    const session = await refreshSession()
    if (session) {
      res = await send(path, opts, accessToken)
    }
    if (!session || res.status === 401) {
      endSession()
      throw await toError(res)
    }
  }

  if (!res.ok) throw await toError(res)
  return parse<T>(res)
}
