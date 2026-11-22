import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api, configureApi, getAccessToken, setAccessToken } from '@/lib/api'

const sessao = {
  usuario: { id: 'u1', nome: 'Ana', email: 'ana@exemplo.com.br' },
  empresa: null,
  papel: null,
  plataformaAdmin: false,
}
const ok = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
const refreshOk = (token: string) => ok({ ...sessao, accessToken: token, expiresIn: 900 })
const erro = (status: number, codigo: string, headers: Record<string, string> = {}) =>
  new Response(JSON.stringify({ codigo, mensagem: `msg ${codigo}` }), { status, headers: { 'Content-Type': 'application/json', ...headers } })

const onUnauthorized = vi.fn()
const onSessionEnd = vi.fn()
const onSession = vi.fn()
let fetchMock: ReturnType<typeof vi.fn>

function authHeader(call: unknown[]): string | undefined {
  return ((call[1] as RequestInit).headers as Record<string, string>).Authorization
}

beforeEach(() => {
  fetchMock = vi.fn()
  vi.stubGlobal('fetch', fetchMock)
  onUnauthorized.mockReset()
  onSessionEnd.mockReset()
  onSession.mockReset()
  configureApi({ onUnauthorized, onSessionEnd, onSession })
  setAccessToken('t-old')
})
afterEach(() => {
  vi.unstubAllGlobals()
  setAccessToken(null)
})

describe('lib/api', () => {
  it('envia Bearer, credentials include e base /api', async () => {
    fetchMock.mockResolvedValueOnce(ok([{ id: 1 }]))
    const r = await api('/Pessoa', { query: { nome: '*a*', vazio: '' } })
    expect(r).toEqual([{ id: 1 }])
    const [url, init] = fetchMock.mock.calls[0]!
    expect(url).toBe(`/api/Pessoa?nome=${encodeURIComponent('*a*')}`)
    expect((init as RequestInit).credentials).toBe('include')
    expect(authHeader(fetchMock.mock.calls[0]!)).toBe('Bearer t-old')
  })

  it('em 401 renova uma vez e repete a chamada com o novo token', async () => {
    fetchMock
      .mockResolvedValueOnce(erro(401, 'sessao_invalida'))
      .mockResolvedValueOnce(refreshOk('t-new'))
      .mockResolvedValueOnce(ok({ ok: true }))
    const r = await api('/Pessoa')
    expect(r).toEqual({ ok: true })
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(fetchMock.mock.calls[1]![0]).toBe('/api/auth/refresh')
    expect((fetchMock.mock.calls[1]![1] as RequestInit).body).toBeUndefined() // sem corpo: usa o cookie
    expect(authHeader(fetchMock.mock.calls[2]!)).toBe('Bearer t-new')
    expect(getAccessToken()).toBe('t-new')
    expect(onSession).toHaveBeenCalledTimes(1)
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('chamadas concorrentes em 401 esperam o MESMO refresh (fila)', async () => {
    let release!: (r: Response) => void
    const refreshPromise = new Promise<Response>((res) => (release = res))
    fetchMock.mockImplementation((url: string, init: RequestInit) => {
      if (url === '/api/auth/refresh') return refreshPromise
      const bearer = (init.headers as Record<string, string>).Authorization
      return Promise.resolve(bearer === 'Bearer t-new' ? ok({ url }) : erro(401, 'sessao_invalida'))
    })
    const all = Promise.all([api('/A'), api('/B'), api('/C')])
    await vi.waitFor(() => expect(fetchMock.mock.calls.filter((c) => c[0] === '/api/auth/refresh').length).toBe(1))
    release(refreshOk('t-new'))
    const res = await all
    expect(res).toEqual([{ url: '/api/A' }, { url: '/api/B' }, { url: '/api/C' }])
    expect(fetchMock.mock.calls.filter((c) => c[0] === '/api/auth/refresh')).toHaveLength(1)
  })

  it('refresh falhou: limpa a sessao, vai para o login e lanca ApiError', async () => {
    window.history.pushState({}, '', '/pessoas?x=1')
    fetchMock.mockResolvedValueOnce(erro(401, 'sessao_invalida')).mockResolvedValueOnce(erro(401, 'sessao_invalida'))
    await expect(api('/Pessoa')).rejects.toMatchObject({ status: 401, codigo: 'sessao_invalida' })
    expect(getAccessToken()).toBeNull()
    expect(onSessionEnd).toHaveBeenCalledTimes(1)
    expect(onUnauthorized).toHaveBeenCalledWith('/pessoas?x=1')
    expect(fetchMock).toHaveBeenCalledTimes(2) // sem laco: nao repete a chamada original
  })

  it('401 de novo depois de renovar tambem encerra a sessao (so uma tentativa)', async () => {
    fetchMock
      .mockResolvedValueOnce(erro(401, 'sessao_invalida'))
      .mockResolvedValueOnce(refreshOk('t-new'))
      .mockResolvedValueOnce(erro(401, 'sessao_invalida'))
    await expect(api('/Pessoa')).rejects.toBeInstanceOf(ApiError)
    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(onUnauthorized).toHaveBeenCalledTimes(1)
  })

  it('login (skipAuth) com 401 NAO dispara refresh nem redireciona', async () => {
    fetchMock.mockResolvedValueOnce(erro(401, 'credenciais_invalidas'))
    await expect(api('/auth/login', { method: 'POST', body: { email: 'a', senha: 'b' }, skipAuth: true })).rejects.toMatchObject({ codigo: 'credenciais_invalidas' })
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(onUnauthorized).not.toHaveBeenCalled()
    expect(authHeader(fetchMock.mock.calls[0]!)).toBeUndefined()
  })

  it('erros viram ApiError com codigo, mensagem, detalhes e Retry-After', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({ codigo: 'empresa_obrigatoria', mensagem: 'Escolha', detalhes: { empresas: [{ id: 'e1' }] } }), { status: 409 }),
    )
    const e1 = (await api('/auth/login', { method: 'POST', skipAuth: true }).catch((e: unknown) => e)) as ApiError
    expect(e1).toBeInstanceOf(ApiError)
    expect(e1).toMatchObject({ status: 409, codigo: 'empresa_obrigatoria', mensagem: 'Escolha', detalhes: { empresas: [{ id: 'e1' }] } })

    fetchMock.mockResolvedValueOnce(erro(429, 'muitas_tentativas', { 'Retry-After': '90' }))
    const e2 = (await api('/auth/login', { method: 'POST', skipAuth: true }).catch((e: unknown) => e)) as ApiError
    expect(e2.retryAfter).toBe(90)
  })

  it('corpo de erro nao-JSON vira mensagem generica, sem texto tecnico', async () => {
    fetchMock.mockResolvedValueOnce(new Response('Internal Server Error: stacktrace', { status: 500 }))
    const e = (await api('/Pessoa').catch((x: unknown) => x)) as ApiError
    expect(e.codigo).toBe('erro_desconhecido')
    expect(e.mensagem).not.toMatch(/stacktrace|Internal/i)
  })

  it('falha de rede vira ApiError sem_conexao', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))
    await expect(api('/Pessoa')).rejects.toMatchObject({ status: 0, codigo: 'sem_conexao' })
  })

  it('204 devolve undefined', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))
    expect(await api('/Pessoa/id/1', { method: 'DELETE' })).toBeUndefined()
  })

  it('FormData segue como multipart: sem Content-Type forcado e sem JSON.stringify', async () => {
    fetchMock.mockResolvedValueOnce(ok({ id: 'i1' }))
    const form = new FormData()
    form.append('arquivo', new File(['a;b'], 'm.csv'))
    await api('/admin/municipios/importacao', { method: 'POST', body: form })
    const init = fetchMock.mock.calls[0]![1] as RequestInit
    expect(init.body).toBe(form)
    expect((init.headers as Record<string, string>)['Content-Type']).toBeUndefined()
    expect(authHeader(fetchMock.mock.calls[0]!)).toBe('Bearer t-old')
  })

  it('corpo objeto continua JSON com Content-Type', async () => {
    fetchMock.mockResolvedValueOnce(ok({}))
    await api('/Pessoa', { method: 'POST', body: { nome: 'A' } })
    const init = fetchMock.mock.calls[0]![1] as RequestInit
    expect(init.body).toBe('{"nome":"A"}')
    expect((init.headers as Record<string, string>)['Content-Type']).toBe('application/json')
  })
})