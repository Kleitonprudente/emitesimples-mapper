import { beforeEach, describe, expect, it } from 'vitest'
import { handleMock, resetMock, setMockLatency } from '@/mocks/registry'

setMockLatency(0)

async function call(method: string, path: string, body?: unknown, token: string | null = null) {
  const res = await handleMock({ method, path, body, token })
  const text = await res.text()
  return { status: res.status, body: text ? JSON.parse(text) : undefined }
}
async function login(email: string) {
  return (await call('POST', '/auth/login', { email, senha: 'senha1234' })).body.accessToken as string
}

const URL_C = '/Pessoa/pes-0001/contatos'
const NOVO = { tipoContato: 'FISCAL', nome: 'Rita Souza', whatsapp: '11911112222', email: 'rita@exemplo.com.br' }

describe('mock de /Pessoa/{id}/contatos', () => {
  let token: string
  beforeEach(async () => {
    resetMock()
    token = await login('admin@exemplo.com.br')
  })

  it('exige token', async () => {
    expect((await call('GET', URL_C)).status).toBe(401)
  })

  it('GET traz 2 contatos semeados ordenados por nome; outra pessoa vem vazia; pessoa inexistente 404', async () => {
    const r = await call('GET', URL_C, undefined, token)
    expect(r.body.map((c: { nome: string }) => c.nome)).toEqual(['Marta Lima', 'Paulo Nunes'])
    expect(r.body[0]).toMatchObject({ id: 'con-0001', tipoContato: 'FINANCEIRO' })
    expect((await call('GET', '/Pessoa/pes-0002/contatos', undefined, token)).body).toEqual([])
    expect((await call('GET', '/Pessoa/nada/contatos', undefined, token)).status).toBe(404)
  })

  it('PUT substitui a lista inteira: mantem ids, gera ids novos, remove os ausentes e normaliza vazios para null', async () => {
    const r = await call('PUT', URL_C, [{ id: 'con-0002', tipoContato: 'SOCIO', nome: 'Paulo N.' }, NOVO, { tipoContato: 'OUTRO', nome: 'Zé' }], token)
    expect(r.status).toBe(200)
    expect(r.body).toEqual([
      { id: 'con-0002', tipoContato: 'SOCIO', nome: 'Paulo N.', whatsapp: null, email: null },
      { id: 'con-0003', ...NOVO },
      { id: 'con-0004', tipoContato: 'OUTRO', nome: 'Zé', whatsapp: null, email: null },
    ])
    expect((await call('GET', URL_C, undefined, token)).body).toHaveLength(3)
    expect((await call('PUT', URL_C, [], token)).body).toEqual([])
    expect((await call('GET', URL_C, undefined, token)).body).toEqual([])
  })

  it('PUT invalido: 400 requisicao_invalida com detalhes.campos e nada e gravado (tudo ou nada)', async () => {
    const r = await call('PUT', URL_C, [NOVO, { tipoContato: 'chefe', nome: '' }, { tipoContato: 'FISCAL', nome: 'X', email: 'ruim' }, { id: 'con-9999', tipoContato: 'FISCAL', nome: 'Y' }], token)
    expect(r.status).toBe(400)
    expect(r.body.codigo).toBe('requisicao_invalida')
    expect(Object.keys(r.body.detalhes.campos).sort()).toEqual(['contatos[1].nome', 'contatos[1].tipoContato', 'contatos[2].email', 'contatos[3].id'])
    expect((await call('GET', URL_C, undefined, token)).body).toHaveLength(2)
    expect((await call('PUT', URL_C, { nao: 'lista' }, token)).status).toBe(400)
  })

  it('PUT em pessoa inexistente: 404', async () => {
    expect((await call('PUT', '/Pessoa/nada/contatos', [NOVO], token)).status).toBe(404)
  })

  it('operador escreve; consulta le mas recebe 403 no PUT', async () => {
    expect((await call('PUT', URL_C, [NOVO], await login('operador@exemplo.com.br'))).status).toBe(200)
    const ro = await login('consulta@exemplo.com.br')
    expect((await call('GET', URL_C, undefined, ro)).status).toBe(200)
    expect((await call('PUT', URL_C, [NOVO], ro)).status).toBe(403)
  })

  it('resetMock restaura as sementes', async () => {
    await call('PUT', URL_C, [], token)
    resetMock()
    token = await login('admin@exemplo.com.br')
    expect((await call('GET', URL_C, undefined, token)).body).toHaveLength(2)
  })
})
