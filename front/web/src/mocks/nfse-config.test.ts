import { beforeEach, describe, expect, it } from 'vitest'
import { ajustarNfseConfig } from '@/mocks/nfse-config'
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

const URL_N = '/configuracao/nfse'

describe('mock de configuracao da NFS-e', () => {
  let token: string
  beforeEach(async () => {
    resetMock()
    token = await login('admin@exemplo.com.br')
  })

  it('GET devolve o estado inicial sem token do emitente', async () => {
    const r = await call('GET', URL_N, undefined, token)
    expect(r.status).toBe(200)
    expect(r.body).toEqual({
      habilitada: false, serie: null, ultimoNumero: 0, proximoNumero: 1, ambiente: 'homologacao', statusCadastro: 'nao_cadastrado',
      dataCadastroGateway: null, emitenteTokenConfigurado: false, certificadoSituacao: 'nao_enviado', validadeCertificado: null, integracaoDisponivel: true,
    })
  })

  it('operador le mas nao grava (403); plataforma sem empresa nao le (403); sem token 401', async () => {
    const op = await login('operador@exemplo.com.br')
    expect((await call('GET', URL_N, undefined, op)).status).toBe(200)
    expect((await call('PUT', URL_N, { habilitada: false }, op)).status).toBe(403)
    const pl = await login('plataforma@exemplo.com.br')
    expect((await call('GET', URL_N, undefined, pl)).status).toBe(403)
    expect((await call('GET', URL_N)).status).toBe(401)
  })

  it('habilitar cadastra no gateway e devolve o corpo do GET', async () => {
    const r = await call('PUT', URL_N, { habilitada: true, serie: '1', ultimoNumero: 10 }, token)
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({ habilitada: true, serie: '1', ultimoNumero: 10, proximoNumero: 11, statusCadastro: 'cadastrado', emitenteTokenConfigurado: true })
    expect(r.body.dataCadastroGateway).toBeTruthy()
    expect(JSON.stringify(r.body)).not.toMatch(/"token"/i)
  })

  it('desabilitar mantem o cadastro e a numeracao', async () => {
    await call('PUT', URL_N, { habilitada: true, serie: '1', ultimoNumero: 10 }, token)
    const r = await call('PUT', URL_N, { habilitada: false }, token)
    expect(r.body).toMatchObject({ habilitada: false, serie: '1', ultimoNumero: 10, statusCadastro: 'cadastrado' })
  })

  it('valida payload (400 dados_invalidos)', async () => {
    for (const body of [{}, { habilitada: true }, { habilitada: true, serie: '123456', ultimoNumero: 0 }, { habilitada: true, serie: 'ab', ultimoNumero: 0 }, { habilitada: true, serie: '1', ultimoNumero: -1 }, { habilitada: true, serie: '1', ultimoNumero: 1.5 }, { habilitada: true, serie: '1', ultimoNumero: 1000000000 }]) {
      const r = await call('PUT', URL_N, body, token)
      expect([r.status, r.body.codigo]).toEqual([400, 'dados_invalidos'])
    }
  })

  it('integracao indisponivel: habilitar 409, desabilitar continua permitido', async () => {
    ajustarNfseConfig({ integracaoDisponivel: false, habilitada: true, serie: '1' })
    const r = await call('PUT', URL_N, { habilitada: true, serie: '1', ultimoNumero: 0 }, token)
    expect([r.status, r.body.codigo]).toEqual([409, 'integranotas_nao_configurada'])
    expect((await call('PUT', URL_N, { habilitada: false }, token)).status).toBe(200)
  })

  it('numeracao menor que a atual: 409 numeracao_invalida', async () => {
    await call('PUT', URL_N, { habilitada: true, serie: '1', ultimoNumero: 10 }, token)
    const r = await call('PUT', URL_N, { habilitada: true, serie: '1', ultimoNumero: 5 }, token)
    expect([r.status, r.body.codigo]).toEqual([409, 'numeracao_invalida'])
  })

  it.each([
    ['422', 422, 'cadastro_incompleto'],
    ['502', 502, 'integranotas_recusou'],
    ['503', 502, 'integranotas_token_recusado'],
    ['505', 502, 'integranotas_resposta_invalida'],
    ['504', 504, 'integranotas_indisponivel'],
  ])('serie %s simula %s %s e nao habilita', async (serie, status, codigo) => {
    const r = await call('PUT', URL_N, { habilitada: true, serie, ultimoNumero: 0 }, token)
    expect([r.status, r.body.codigo]).toEqual([status, codigo])
    if (codigo === 'cadastro_incompleto') expect(r.body.detalhes.campos.length).toBeGreaterThan(0)
    expect((await call('GET', URL_N, undefined, token)).body.habilitada).toBe(false)
  })
})
