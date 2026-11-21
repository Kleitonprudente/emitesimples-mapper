import { beforeEach, describe, expect, it } from 'vitest'
import { handleMock, resetMock } from '@/mocks/registry'

async function call(method: string, path: string, body?: unknown, token: string | null = null, query?: Record<string, string>) {
  const res = await handleMock({ method, path, body, token, query })
  const text = await res.text()
  return { status: res.status, body: text ? JSON.parse(text) : undefined, headers: res.headers }
}

async function loginToken() {
  const r = await call('POST', '/auth/login', { email: 'admin@exemplo.com.br', senha: 'senha1234' })
  return r.body.accessToken as string
}

describe('mock registry', () => {
  beforeEach(() => resetMock())

  it('login valido devolve o contrato (sem refreshToken no corpo)', async () => {
    const r = await call('POST', '/auth/login', { email: 'admin@exemplo.com.br', senha: 'senha1234' })
    expect(r.status).toBe(200)
    expect(Object.keys(r.body).sort()).toEqual(['accessToken', 'empresa', 'expiresIn', 'papel', 'plataformaAdmin', 'usuario'])
    expect(r.body.papel).toBe('administrador')
  })

  it('credenciais invalidas, empresa obrigatoria (409) e muitas tentativas (429)', async () => {
    expect((await call('POST', '/auth/login', { email: 'admin@exemplo.com.br', senha: 'x' })).status).toBe(401)
    const m = await call('POST', '/auth/login', { email: 'multi@exemplo.com.br', senha: 'senha1234' })
    expect(m.status).toBe(409)
    expect(m.body.codigo).toBe('empresa_obrigatoria')
    expect(m.body.detalhes.empresas).toHaveLength(2)
    const ok = await call('POST', '/auth/login', { email: 'multi@exemplo.com.br', senha: 'senha1234', empresaId: 'emp-2' })
    expect(ok.body.papel).toBe('operador')
    const t = await call('POST', '/auth/login', { email: 'bloqueado@exemplo.com.br', senha: 'x' })
    expect(t.status).toBe(429)
    expect(t.headers.get('Retry-After')).toBe('90')
  })

  it('refresh usa o "cookie" (sem corpo); sem login -> 401; logout limpa', async () => {
    expect((await call('POST', '/auth/refresh')).status).toBe(401)
    await loginToken()
    expect((await call('POST', '/auth/refresh')).status).toBe(200)
    expect((await call('POST', '/auth/logout')).status).toBe(204)
    expect((await call('POST', '/auth/refresh')).status).toBe(401)
  })

  it('entidade exige token e implementa lista, count e from/to', async () => {
    expect((await call('GET', '/Pessoa')).status).toBe(401)
    const token = await loginToken()
    const all = await call('GET', '/Pessoa', undefined, token)
    expect(Array.isArray(all.body)).toBe(true)
    expect(all.body.length).toBeLessThanOrEqual(100)
    const count = await call('GET', '/Pessoa/count', undefined, token)
    expect(count.body.count).toBe(37)
    const page = await call('GET', '/Pessoa/0/9', undefined, token, { sortBy: 'nome', sortDirection: 'desc' })
    expect(page.body).toHaveLength(10)
    const filtered = await call('GET', '/Pessoa/count', undefined, token, { tipoPessoa: 'JURIDICA' })
    expect(filtered.body.count).toBeGreaterThan(0)
    expect(filtered.body.count).toBeLessThan(37)
  })

  it('create/update/delete por id e erro de documento duplicado', async () => {
    const token = await loginToken()
    const nova = { tipoPessoa: 'FISICA', documento: '52998224725', nome: 'Teste' }
    const c = await call('POST', '/Pessoa', nova, token)
    expect(c.status).toBe(200)
    expect((await call('POST', '/Pessoa', nova, token)).body.codigo).toBe('documento_duplicado')
    const u = await call('PUT', `/Pessoa/id/${c.body.id}`, { ...nova, nome: 'Teste 2' }, token)
    expect(u.body.nome).toBe('Teste 2')
    expect((await call('DELETE', `/Pessoa/id/${c.body.id}`, undefined, token)).status).toBe(204)
    expect((await call('GET', `/Pessoa/id/${c.body.id}`, undefined, token)).status).toBe(404)
  })

  it('troca de senha valida 8 a 72 bytes', async () => {
    const token = await loginToken()
    expect((await call('POST', '/auth/senha', { senhaAtual: 'senha1234', novaSenha: 'curta' }, token)).status).toBe(400)
    expect((await call('POST', '/auth/senha', { senhaAtual: 'senha1234', novaSenha: 'a'.repeat(73) }, token)).status).toBe(400)
    expect((await call('POST', '/auth/senha', { senhaAtual: 'errada', novaSenha: 'novasenha123' }, token)).status).toBe(403)
    expect((await call('POST', '/auth/senha', { senhaAtual: 'senha1234', novaSenha: 'novasenha123' }, token)).status).toBe(204)
  })
})
