import type { LoginResponse, Papel } from '@/lib/api'
import { adminRoutes, consultaCnpjRoute, getMunicipios, resetAdmin } from '@/mocks/admin'
import { CEPS_MOCK, EMPRESAS_MOCK, MOCK_EMAIL_BLOQUEADO, MOCK_SENHA, MOCK_USERS, buildPessoas, type MockUser, type Pessoa } from '@/mocks/fixtures'
import { contatosRoutes, resetContatos } from '@/mocks/contatos'
import { erro, json } from '@/mocks/http'
import { resetVinculosEmpresa, vinculosEmpresaRoutes } from '@/mocks/vinculos-empresa'

/* ---------------------------------------------------------------------------
 * Registry do modo mock. Chamado por lib/api.ts ANTES do fetch; devolve uma Response normal,
 * entao o resto do pipeline (refresh, ApiError, toError) e exatamente o de producao.
 * Sem service worker, sem processo extra.
 * ------------------------------------------------------------------------- */

export interface MockRequest {
  method: string
  path: string
  query?: Record<string, string | number | boolean | null | undefined>
  body?: unknown
  token: string | null
}

let latencyMs = 350
/** Para testes: sem latencia artificial. */
export function setMockLatency(ms: number) {
  latencyMs = ms
}

/** `?empty=1` na URL de carga forca listas vazias; `?empty=0` desliga. Lido uma vez (a navegacao interna perde a query). */
let forceEmpty = new URLSearchParams(typeof window === 'undefined' ? '' : window.location.search).get('empty') === '1'
export function setForceEmpty(v: boolean) {
  forceEmpty = v
}

/* ----- sessao fake (o "cookie" de refresh vira um item de sessionStorage; so ids, sem PII) ----- */

const COOKIE_KEY = 'mock_refresh'
interface Cookie {
  userId: string
  empresaId: string | null
}
function readCookie(): Cookie | null {
  try {
    const raw = sessionStorage.getItem(COOKIE_KEY)
    return raw ? (JSON.parse(raw) as Cookie) : null
  } catch {
    return null
  }
}
function writeCookie(c: Cookie | null) {
  try {
    if (c) sessionStorage.setItem(COOKIE_KEY, JSON.stringify(c))
    else sessionStorage.removeItem(COOKIE_KEY)
  } catch {
    /* sem storage: a sessao so dura ate recarregar */
  }
}

let currentToken: string | null = null
let tokenSeq = 0

function sessionFor(user: MockUser, empresaId: string | null): LoginResponse {
  const vinculo = user.vinculos.find((v) => v.empresaId === empresaId)
  const empresa = empresaId ? (EMPRESAS_MOCK[empresaId] ?? null) : null
  currentToken = `mock-token-${++tokenSeq}`
  writeCookie({ userId: user.id, empresaId })
  return {
    accessToken: currentToken,
    expiresIn: 900,
    usuario: { id: user.id, nome: user.nome, email: user.email },
    empresa,
    papel: (vinculo?.papel ?? null) as Papel | null,
    plataformaAdmin: !!user.plataformaAdmin,
  }
}

function auth(req: MockRequest): Response | null {
  const { method, path } = req
  const body = (req.body ?? {}) as Record<string, string | undefined>

  if (path === '/auth/login' && method === 'POST') {
    const email = (body.email ?? '').trim().toLowerCase()
    if (email === MOCK_EMAIL_BLOQUEADO) {
      return erro(429, 'muitas_tentativas', 'Muitas tentativas de login. Aguarde e tente novamente.', undefined, { 'Retry-After': '90' })
    }
    const user = MOCK_USERS.find((u) => u.email === email)
    if (!user || body.senha !== MOCK_SENHA) return erro(401, 'credenciais_invalidas', 'E-mail ou senha inválidos.')
    if (user.vinculos.length > 1 && !body.empresaId) {
      return erro(409, 'empresa_obrigatoria', 'Escolha a empresa para entrar.', {
        empresas: user.vinculos.map((v) => ({ id: v.empresaId, razaoSocial: EMPRESAS_MOCK[v.empresaId]!.razaoSocial, papel: v.papel })),
      })
    }
    const empresaId = body.empresaId ?? user.vinculos[0]?.empresaId ?? null
    if (empresaId && !user.vinculos.some((v) => v.empresaId === empresaId)) return erro(401, 'credenciais_invalidas', 'E-mail ou senha inválidos.')
    return json(200, sessionFor(user, empresaId))
  }

  if (path === '/auth/refresh' && method === 'POST') {
    const c = readCookie()
    const user = c && MOCK_USERS.find((u) => u.id === c.userId)
    if (!c || !user) return erro(401, 'sessao_invalida', 'Sua sessão expirou. Entre novamente.')
    return json(200, sessionFor(user, c.empresaId))
  }

  if (path === '/auth/logout' && method === 'POST') {
    writeCookie(null)
    currentToken = null
    return new Response(null, { status: 204 })
  }

  // demais rotas de auth exigem token
  if (!req.token || req.token !== currentToken) return erro(401, 'sessao_invalida', 'Sua sessão expirou. Entre novamente.')

  if (path === '/auth/me' && method === 'GET') {
    const c = readCookie()
    const user = c && MOCK_USERS.find((u) => u.id === c.userId)
    if (!c || !user) return erro(401, 'sessao_invalida', 'Sua sessão expirou. Entre novamente.')
    const { accessToken: _a, expiresIn: _e, ...me } = sessionFor(user, c.empresaId)
    return json(200, me)
  }

  if (path === '/auth/senha' && method === 'POST') {
    const nova = body.novaSenha ?? ''
    const bytes = new TextEncoder().encode(nova).length
    if (bytes < 8 || bytes > 72) return erro(400, 'senha_invalida', 'A nova senha deve ter de 8 a 72 caracteres.')
    if (body.senhaAtual !== MOCK_SENHA) return erro(403, 'senha_atual_invalida', 'A senha atual está incorreta.')
    return new Response(null, { status: 204 })
  }
  return null
}

/* ----- entidades de exemplo: Pessoa e Municipio ----- */

let pessoas: Pessoa[] = buildPessoas()
let seq = pessoas.length

type Q = Record<string, string | number | boolean | null | undefined>

function applyFilters<T extends object>(rows: T[], q: Q): T[] {
  let out = rows
  for (const [k, raw] of Object.entries(q)) {
    if (k === 'sortBy' || k === 'sortDirection' || raw === undefined || raw === null || raw === '') continue
    const v = String(raw)
    out = out.filter((r) => {
      const field = (r as Record<string, unknown>)[k]
      if (field === undefined || field === null) return false
      const s = String(field).toLowerCase()
      if (v.includes('*')) {
        const re = new RegExp(`^${v.toLowerCase().replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*')}$`)
        return re.test(s)
      }
      return s === v.toLowerCase()
    })
  }
  return out
}

function applySort<T extends object>(rows: T[], q: Q): T[] {
  const by = q.sortBy ? String(q.sortBy) : 'id'
  const dir = String(q.sortDirection ?? 'asc').toLowerCase() === 'desc' ? -1 : 1
  return [...rows].sort((a, b) => {
    const x = String((a as Record<string, unknown>)[by] ?? '')
    const y = String((b as Record<string, unknown>)[by] ?? '')
    return x.localeCompare(y, 'pt-BR', { numeric: true }) * dir
  })
}

function entity(req: MockRequest): Response | null {
  const { method, path } = req
  const q = req.query ?? {}
  const body = req.body as Partial<Pessoa> | undefined

  const cepRota = /^\/cep\/([^/]*)$/.exec(path)
  if (cepRota && method === 'GET') {
    const cep = decodeURIComponent(cepRota[1]!).replace(/\D/g, '')
    if (cep.length !== 8) return erro(400, 'cep_invalido', 'CEP inválido: informe 8 dígitos.')
    if (cep === '00000502') return erro(502, 'servico_cep_indisponivel', 'Serviço de consulta de CEP indisponível. Tente novamente em instantes.')
    if (cep === '00000429') return erro(429, 'muitas_tentativas', 'Muitas consultas de CEP. Aguarde e tente novamente.', undefined, { 'Retry-After': '30' })
    const achado = CEPS_MOCK[cep]
    return achado ? json(200, achado) : erro(404, 'cep_nao_encontrado', 'CEP não encontrado.')
  }

  // Combobox de Empresa (vinculos de usuario). Contrato provisorio: GET /Empresa?razaoSocial=*termo*.
  if (path === '/Empresa' && method === 'GET') {
    const rows = applySort(applyFilters(Object.values(EMPRESAS_MOCK), q), q)
    return json(200, rows.slice(0, 100))
  }

  if (path === '/Municipio' && method === 'GET') {
    const rows = forceEmpty ? [] : applySort(applyFilters(getMunicipios(), q), q)
    return json(200, rows.slice(0, 100))
  }

  if (path === '/Pessoa' && method === 'GET') {
    const rows = forceEmpty ? [] : applySort(applyFilters(pessoas, q), q)
    return json(200, rows.slice(0, 100))
  }
  if (path === '/Pessoa/count' && method === 'GET') {
    return json(200, { count: forceEmpty ? 0 : applyFilters(pessoas, q).length })
  }
  const range = /^\/Pessoa\/(\d+)\/(\d+)$/.exec(path)
  if (range && method === 'GET') {
    const from = Number(range[1])
    const to = Number(range[2])
    if (from >= to && from !== to) return erro(400, 'intervalo_invalido', 'Intervalo inválido.')
    if (to - from + 1 > 100) return erro(400, 'intervalo_invalido', 'No máximo 100 registros por consulta.')
    const rows = forceEmpty ? [] : applySort(applyFilters(pessoas, q), q)
    return json(200, rows.slice(from, to + 1))
  }

  if (path === '/Pessoa' && method === 'POST') {
    if (!body?.nome || !body.documento || !body.tipoPessoa) return erro(400, 'dados_invalidos', 'Informe tipo, documento e nome.')
    if (pessoas.some((p) => p.documento === body.documento)) return erro(409, 'documento_duplicado', 'Já existe uma pessoa com este documento.')
    const now = new Date().toISOString().slice(0, 19)
    const cidade = body.cidade?.id ? getMunicipios().find((m) => m.id === body.cidade!.id) : undefined
    const nova: Pessoa = { ...(body as Pessoa), id: `pes-${String(++seq).padStart(4, '0')}`, cidade: cidade ?? body.cidade, dataCriacao: now, dataAtualizacao: now }
    pessoas = [nova, ...pessoas]
    return json(200, nova)
  }

  const byId = /^\/Pessoa\/id\/([^/]+)$/.exec(path)
  if (byId) {
    const id = byId[1]!
    const idx = pessoas.findIndex((p) => p.id === id)
    if (idx < 0) return erro(404, 'nao_encontrado', 'Registro não encontrado.')
    if (method === 'GET') return json(200, pessoas[idx])
    if (method === 'DELETE') {
      pessoas = pessoas.filter((p) => p.id !== id)
      return new Response(null, { status: 204 })
    }
    if (method === 'PUT' || method === 'PATCH') {
      const cidade = body?.cidade?.id ? getMunicipios().find((m) => m.id === body.cidade!.id) : undefined
      const merged = { ...(method === 'PUT' ? {} : pessoas[idx]), ...body, cidade: cidade ?? body?.cidade, id, dataCriacao: pessoas[idx]!.dataCriacao, dataAtualizacao: new Date().toISOString().slice(0, 19) } as Pessoa
      pessoas = pessoas.map((p) => (p.id === id ? merged : p))
      // O back real responde 204 (sem corpo) ao PUT e 200 com o registro ao PATCH: o mock reproduz isso para a tela nao depender do retorno.
      return method === 'PUT' ? new Response(null, { status: 204 }) : json(200, merged)
    }
  }
  return null
}

const delay = (ms: number) => new Promise((r) => setTimeout(r, ms))

export async function handleMock(req: MockRequest): Promise<Response> {
  await delay(latencyMs)
  let res: Response | null
  if (req.path.startsWith('/auth/')) res = auth(req)
  else if (!req.token || req.token !== currentToken) res = erro(401, 'sessao_invalida', 'Sua sessão expirou. Entre novamente.')
  else {
    const c = readCookie()
    const user = c ? MOCK_USERS.find((u) => u.id === c.userId) : undefined
    const papel = (user?.vinculos.find((v) => v.empresaId === c?.empresaId)?.papel ?? null) as Papel | null
    res = consultaCnpjRoute(req, { plataformaAdmin: !!user?.plataformaAdmin, papel }) ?? contatosRoutes(req, { plataformaAdmin: !!user?.plataformaAdmin, papel, pessoaExiste: (id) => pessoas.some((p) => p.id === id) }) ?? (await vinculosEmpresaRoutes(req, { plataformaAdmin: !!user?.plataformaAdmin })) ?? (await adminRoutes(req, { plataformaAdmin: !!user?.plataformaAdmin, nome: user?.nome ?? '' })) ?? entity(req)
  }  return res ?? erro(404, 'nao_encontrado', 'Recurso não encontrado (mock).')
}

/** Para testes: volta ao estado inicial. */
export function resetMock() {
  pessoas = buildPessoas()
  seq = pessoas.length
  currentToken = null
  forceEmpty = false
  resetAdmin()
  resetVinculosEmpresa()
  resetContatos()
  writeCookie(null)
}
