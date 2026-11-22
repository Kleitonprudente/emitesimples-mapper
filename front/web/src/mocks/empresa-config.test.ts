import { beforeEach, describe, expect, it } from 'vitest'
import { ajustarEmpresaConfig, DOCUMENTO_DUPLICADO_MOCK } from '@/mocks/empresa-config'
import { ajustarNfseConfig } from '@/mocks/nfse-config'
import { handleMock, resetMock, setMockLatency } from '@/mocks/registry'
import { isValidCnpj } from '@/runtime/format'

setMockLatency(0)

async function call(method: string, path: string, body?: unknown, token: string | null = null) {
  const res = await handleMock({ method, path, body, token })
  const text = await res.text()
  return { status: res.status, body: text ? JSON.parse(text) : undefined }
}
async function login(email: string) {
  return (await call('POST', '/auth/login', { email, senha: 'senha1234' })).body.accessToken as string
}

const URL_E = '/configuracao/empresa'
const URL_C = '/configuracao/empresa/certificado'

function form(nome: string, senha: string | null = 'Senha123', bytes = 10): FormData {
  const f = new FormData()
  f.append('arquivo', new File([new Uint8Array(bytes)], nome))
  if (senha !== null) f.append('senha', senha)
  return f
}

describe('mock de Configuracao > Empresa', () => {
  let token: string
  let corpo: Record<string, unknown>
  beforeEach(async () => {
    resetMock()
    token = await login('admin@exemplo.com.br')
    const { body } = await call('GET', URL_E, undefined, token)
    const { documentoEditavel: _d, certificado: _c, ...dados } = body
    corpo = { ...dados, municipio: { id: body.municipio.id } }
  })

  it('os documentos de teste sao CNPJs validos', () => {
    expect(isValidCnpj('11222333000181')).toBe(true)
    expect(isValidCnpj(DOCUMENTO_DUPLICADO_MOCK)).toBe(true)
  })

  it('GET devolve os dados, documento editavel e certificado nao enviado', async () => {
    const r = await call('GET', URL_E, undefined, token)
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({
      razaoSocial: 'Horizonte Serviços Contábeis Ltda',
      documento: '11222333000181',
      municipio: { nome: 'Guaíra', uf: 'PR', codigoIbge: '4108809' },
      regimeTributario: 1,
      regimeIbsCbs: null,
      documentoEditavel: true,
      certificado: { situacao: 'nao_enviado', validade: null, pendenteDeEnvio: false, venceEmDias: null },
    })
  })

  it('operador le mas nao grava (403); plataforma sem empresa 403; sem token 401', async () => {
    const op = await login('operador@exemplo.com.br')
    expect((await call('GET', URL_E, undefined, op)).status).toBe(200)
    expect((await call('PUT', URL_E, corpo, op)).status).toBe(403)
    expect((await call('POST', URL_C, form('a.pfx'), op)).status).toBe(403)
    const pl = await login('plataforma@exemplo.com.br')
    expect((await call('GET', URL_E, undefined, pl)).status).toBe(403)
    expect((await call('GET', URL_E)).status).toBe(401)
  })

  it('PUT salva, resolve o municipio e devolve o corpo do GET', async () => {
    const r = await call('PUT', URL_E, { ...corpo, nomeFantasia: 'Novo Nome', complemento: 'Sala 2', municipio: { id: 'mun-4106902' }, regimeIbsCbs: 2 }, token)
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({ nomeFantasia: 'Novo Nome', complemento: 'Sala 2', municipio: { nome: 'Curitiba' }, regimeIbsCbs: 2 })
    expect((await call('GET', URL_E, undefined, token)).body.nomeFantasia).toBe('Novo Nome')
  })

  it('PUT invalido: 400 requisicao_invalida com detalhes.campos', async () => {
    const r = await call('PUT', URL_E, { ...corpo, razaoSocial: '', cep: '123', email: 'x', municipio: { id: 'nao-existe' }, regimeTributario: 9 }, token)
    expect([r.status, r.body.codigo]).toEqual([400, 'requisicao_invalida'])
    expect(r.body.detalhes.campos.map((c: { campo: string }) => c.campo).sort()).toEqual(['cep', 'email', 'municipio', 'razaoSocial', 'regimeTributario'])
  })

  it('documento: editavel muda e valida; duplicado 409; bloqueado 409', async () => {
    const novo = '45723174000110'
    expect(isValidCnpj(novo)).toBe(true)
    expect((await call('PUT', URL_E, { ...corpo, documento: novo }, token)).body.documento).toBe(novo)
    expect((await call('PUT', URL_E, { ...corpo, documento: '12345678000100' }, token)).body.detalhes.campos[0].campo).toBe('documento')
    expect((await call('PUT', URL_E, { ...corpo, documento: DOCUMENTO_DUPLICADO_MOCK }, token)).body.codigo).toBe('documento_duplicado')
    ajustarEmpresaConfig({ documentoBloqueado: true })
    const r = await call('PUT', URL_E, { ...corpo, documento: DOCUMENTO_DUPLICADO_MOCK }, token)
    expect([r.status, r.body.codigo]).toEqual([409, 'documento_bloqueado'])
    expect((await call('GET', URL_E, undefined, token)).body.documentoEditavel).toBe(false)
  })

  it('NFS-e habilitada bloqueia o documento', async () => {
    ajustarNfseConfig({ habilitada: true })
    expect((await call('GET', URL_E, undefined, token)).body.documentoEditavel).toBe(false)
    expect((await call('PUT', URL_E, { ...corpo, documento: DOCUMENTO_DUPLICADO_MOCK }, token)).body.codigo).toBe('documento_bloqueado')
  })

  it('certificado sem NFS-e: guardado e pendente; ao habilitar a NFS-e vira valido', async () => {
    const r = await call('POST', URL_C, form('empresa.pfx'), token)
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({ situacao: 'nao_enviado', pendenteDeEnvio: true, venceEmDias: 365, avisos: [] })
    expect(r.body.validade).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    expect((await call('GET', URL_E, undefined, token)).body.certificado).toMatchObject({ pendenteDeEnvio: true })
    ajustarNfseConfig({ habilitada: true })
    expect((await call('GET', URL_E, undefined, token)).body.certificado).toMatchObject({ situacao: 'valido', pendenteDeEnvio: false })
  })

  it('certificado com NFS-e habilitada: valido; vence15 e semcnpj', async () => {
    ajustarNfseConfig({ habilitada: true })
    const r = await call('POST', URL_C, form('vence15.P12'), token)
    expect(r.body).toMatchObject({ situacao: 'valido', pendenteDeEnvio: false, venceEmDias: 15 })
    expect((await call('POST', URL_C, form('semcnpj.pfx'), token)).body.avisos).toEqual(['nao_foi_possivel_conferir_cnpj'])
  })

  it.each([
    ['vencido.pfx', 400, 'certificado_vencido'],
    ['outro.pfx', 400, 'certificado_nao_pertence'],
    ['senhaerrada.pfx', 400, 'certificado_senha_incorreta'],
    ['invalido.pfx', 400, 'certificado_invalido'],
    ['gateway.pfx', 400, 'certificado_invalido_gateway'],
    ['grande.pfx', 413, 'arquivo_grande'],
    ['muitas.pfx', 429, 'muitas_tentativas'],
    ['token.pfx', 409, 'integranotas_token_recusado'],
    ['indisponivel.pfx', 504, 'integranotas_indisponivel'],
    ['respostainvalida.pfx', 502, 'integranotas_resposta_invalida'],
  ])('certificado %s -> %i %s', async (nome, status, codigo) => {
    const r = await call('POST', URL_C, form(nome), token)
    expect([r.status, r.body.codigo]).toEqual([status, codigo])
  })

  it('certificado: valida senha, extensao, tamanho e arquivo ausente', async () => {
    expect((await call('POST', URL_C, form('a.pfx', 'senha com espaço'), token)).body.codigo).toBe('certificado_senha_invalida')
    expect((await call('POST', URL_C, form('a.pfx', 'Sênha1'), token)).body.codigo).toBe('certificado_senha_invalida')
    expect((await call('POST', URL_C, form('a.pfx', 'a'.repeat(61)), token)).body.codigo).toBe('certificado_senha_invalida')
    expect((await call('POST', URL_C, form('a.pfx', null), token)).body.codigo).toBe('certificado_senha_invalida')
    expect((await call('POST', URL_C, form('a.txt'), token)).body.codigo).toBe('certificado_invalido')
    expect((await call('POST', URL_C, form('a.pfx', 'Senha123', 256 * 1024 + 1), token)).status).toBe(413)
    expect((await call('POST', URL_C, new FormData(), token)).body.codigo).toBe('certificado_invalido')
  })

  it('o arquivo e a senha nunca voltam; falha nao altera o estado', async () => {
    await call('POST', URL_C, form('vencido.pfx', 'Segredo99'), token)
    const g = await call('GET', URL_E, undefined, token)
    expect(g.body.certificado.situacao).toBe('nao_enviado')
    const ok = await call('POST', URL_C, form('ok.pfx', 'Segredo99'), token)
    expect(JSON.stringify([ok.body, (await call('GET', URL_E, undefined, token)).body])).not.toMatch(/Segredo99|senha|arquivo/i)
  })
})
