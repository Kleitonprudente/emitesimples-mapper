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
    expect(r.body[0]).toMatchObject({ papel: 'ADMINISTRADOR', ativo: true, usuario: { id: 'usu-1', nome: 'Ana Ribeiro', email: 'admin@exemplo.com.br', ativo: true } })
  })

  it('exige plataforma (403) e empresa existente (404)', async () => {
    const t = await login('admin@exemplo.com.br')
    expect((await call('GET', URL_V, undefined, t)).status).toBe(403)
    token = await login('plataforma@exemplo.com.br')
    expect((await call('GET', '/admin/empresas/nao-existe/vinculos', undefined, token)).status).toBe(404)
  })

  it('POST vincula (201) e valida 404, 400 e 409', async () => {
    expect((await call('POST', URL_V, { usuarioId: 'usu-3', papel: 'CONSULTA' }, token)).status).toBe(201)
    expect((await call('GET', URL_V, undefined, token)).body).toHaveLength(3)
    expect((await call('POST', URL_V, { usuarioId: 'usu-3', papel: 'CONSULTA' }, token)).body.codigo).toBe('vinculo_ja_existe')
    expect((await call('POST', URL_V, { usuarioId: 'usu-3', papel: 'CONSULTA' }, token)).status).toBe(409)
    expect((await call('POST', URL_V, { usuarioId: 'nao-existe', papel: 'CONSULTA' }, token)).body.codigo).toBe('usuario_nao_encontrado')
    expect((await call('POST', URL_V, { usuarioId: 'usu-4', papel: 'chefe' }, token)).status).toBe(400)
  })

  it('PATCH muda papel e ativo, parcialmente, sem excluir', async () => {
    expect((await call('PATCH', `${URL_V}/vemp-2`, { papel: 'CONSULTA' }, token)).status).toBe(200)
    expect((await call('PATCH', `${URL_V}/vemp-2`, { ativo: false }, token)).status).toBe(200)
    const v = (await call('GET', URL_V, undefined, token)).body.find((x: { id: string }) => x.id === 'vemp-2')
    expect(v).toMatchObject({ papel: 'CONSULTA', ativo: false })
    expect((await call('PATCH', `${URL_V}/vemp-2`, { papel: 'x' }, token)).status).toBe(400)
    expect((await call('PATCH', `${URL_V}/nada`, { ativo: true }, token)).status).toBe(404)
  })
})

describe('mock de vinculos: delegacao de cancelar NFS-e (podeCancelarNfse)', () => {
  // UMA sessao por vez no mock: a plataforma age, depois conferimos a sessao do operador (novo login), e voltamos.
  const comoPlataforma = async (method: string, path: string, body?: unknown) => call(method, path, body, await login('plataforma@exemplo.com.br'))
  const sessaoDo = async (email: string, empresaId?: string) => (await call('POST', '/auth/login', { email, senha: 'senha1234', empresaId })).body.podeCancelarNfse as boolean

  beforeEach(() => resetMock())

  it('lista traz podeCancelarNfse: administrador true, operador false por padrao', async () => {
    const r = await comoPlataforma('GET', URL_V)
    expect(r.body.map((v: { papel: string; podeCancelarNfse: boolean }) => [v.papel, v.podeCancelarNfse])).toEqual([['ADMINISTRADOR', true], ['OPERADOR', false]])
  })

  it('POST com podeCancelarNfse e PATCH delegam e retiram, e a sessao do usuario reflete (estado compartilhado)', async () => {
    expect(await sessaoDo('operador@exemplo.com.br')).toBe(false)
    const novo = await comoPlataforma('POST', URL_V, { usuarioId: 'usu-3', papel: 'OPERADOR', podeCancelarNfse: true })
    expect(novo.body).toMatchObject({ papel: 'OPERADOR', podeCancelarNfse: true })
    expect((await comoPlataforma('PATCH', `${URL_V}/vemp-2`, { podeCancelarNfse: true })).body.podeCancelarNfse).toBe(true)
    expect(await sessaoDo('operador@exemplo.com.br')).toBe(true)
    expect((await comoPlataforma('PATCH', `${URL_V}/vemp-2`, { ativo: true })).body.podeCancelarNfse).toBe(true)
    expect((await comoPlataforma('PATCH', `${URL_V}/vemp-2`, { podeCancelarNfse: false })).body.podeCancelarNfse).toBe(false)
    expect(await sessaoDo('operador@exemplo.com.br')).toBe(false)
  })

  it('o vinculo por usuario (/admin/usuarios/{id}/vinculos) tambem le e grava a delegacao', async () => {
    const lista = await comoPlataforma('GET', '/admin/usuarios/usu-4/vinculos')
    expect(lista.body.map((v: { empresaId: string; podeCancelarNfse: boolean }) => [v.empresaId, v.podeCancelarNfse])).toEqual([['emp-1', true], ['emp-2', true]])
    const retirado = await comoPlataforma('PATCH', '/admin/usuarios/usu-4/vinculos/vin-usu-4-2', { papel: 'OPERADOR', ativo: true, podeCancelarNfse: false })
    expect(retirado.body.podeCancelarNfse).toBe(false)
    expect(await sessaoDo('multi@exemplo.com.br', 'emp-2')).toBe(false)
    const criado = await comoPlataforma('POST', '/admin/usuarios/usu-3/vinculos', { empresaId: 'emp-2', papel: 'OPERADOR', podeCancelarNfse: true })
    expect(criado.body).toMatchObject({ empresaId: 'emp-2', podeCancelarNfse: true })
  })
})
