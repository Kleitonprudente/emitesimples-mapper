import { beforeEach, describe, expect, it } from 'vitest'
import { handleMock, resetMock, setMockLatency } from '@/mocks/registry'

setMockLatency(0)

async function call(method: string, path: string, body?: unknown, token: string | null = null) {
  const res = await handleMock({ method, path, body, token })
  const text = await res.text()
  return { status: res.status, body: text ? JSON.parse(text) : undefined }
}
async function login(email: string, empresaId?: string) {
  return (await call('POST', '/auth/login', { email, senha: 'senha1234', empresaId })).body.accessToken as string
}

const URL_V = '/admin/empresas/emp-1/vinculos'

describe('mock de vinculos da empresa', () => {
  let token: string
  beforeEach(async () => {
    resetMock()
    token = await login('plataforma@exemplo.com.br')
  })

  it('lista os 2 vinculos semeados com o usuario embutido', async () => {
    const r = await call('GET', URL_V, undefined, token)
    expect(r.status).toBe(200)
    expect(r.body).toHaveLength(2)
    expect(r.body[0]).toMatchObject({ papel: 'administrador', ativo: true, usuario: { id: 'usu-1', nome: 'Ana Ribeiro', email: 'admin@exemplo.com.br', ativo: true } })
  })

  it('exige plataforma (403) e empresa existente (404)', async () => {
    const t = await login('admin@exemplo.com.br')
    expect((await call('GET', URL_V, undefined, t)).status).toBe(403)
    token = await login('plataforma@exemplo.com.br')
    expect((await call('GET', '/admin/empresas/nao-existe/vinculos', undefined, token)).status).toBe(404)
  })

  it('POST vincula (201) e valida 404, 400 e 409', async () => {
    expect((await call('POST', URL_V, { usuarioId: 'usu-3', papel: 'consulta' }, token)).status).toBe(201)
    expect((await call('GET', URL_V, undefined, token)).body).toHaveLength(3)
    expect((await call('POST', URL_V, { usuarioId: 'usu-3', papel: 'consulta' }, token)).body.codigo).toBe('vinculo_ja_existe')
    expect((await call('POST', URL_V, { usuarioId: 'usu-3', papel: 'consulta' }, token)).status).toBe(409)
    expect((await call('POST', URL_V, { usuarioId: 'nao-existe', papel: 'consulta' }, token)).body.codigo).toBe('usuario_nao_encontrado')
    expect((await call('POST', URL_V, { usuarioId: 'usu-4', papel: 'chefe' }, token)).status).toBe(400)
  })

  it('PATCH muda papel e ativo, parcialmente, sem excluir', async () => {
    expect((await call('PATCH', `${URL_V}/vemp-2`, { papel: 'consulta' }, token)).status).toBe(200)
    expect((await call('PATCH', `${URL_V}/vemp-2`, { ativo: false }, token)).status).toBe(200)
    const v = (await call('GET', URL_V, undefined, token)).body.find((x: { id: string }) => x.id === 'vemp-2')
    expect(v).toMatchObject({ papel: 'consulta', ativo: false })
    expect((await call('PATCH', `${URL_V}/vemp-2`, { papel: 'x' }, token)).status).toBe(400)
    expect((await call('PATCH', `${URL_V}/nada`, { ativo: true }, token)).status).toBe(404)
  })
})
