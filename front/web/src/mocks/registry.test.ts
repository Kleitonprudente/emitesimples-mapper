import { beforeEach, describe, expect, it } from 'vitest'
import { handleMock, resetMock, setMockLatency } from '@/mocks/registry'

setMockLatency(0)

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
    expect(Object.keys(r.body).sort()).toEqual(['accessToken', 'empresa', 'expiresIn', 'papel', 'plataformaAdmin', 'podeCancelarNfse', 'usuario'])
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
    const filtered = await call('GET', '/Pessoa/count', undefined, token, { tipoPessoa: 'juridica' })
    expect(filtered.body.count).toBeGreaterThan(0)
    expect(filtered.body.count).toBeLessThan(37)
  })

  it('create/update/delete por id e erro de documento duplicado', async () => {
    const token = await loginToken()
    const nova = { tipoPessoa: 'fisica', documento: '52998224725', nome: 'Teste' }
    const c = await call('POST', '/Pessoa', nova, token)
    expect(c.status).toBe(200)
    expect((await call('POST', '/Pessoa', nova, token)).body.codigo).toBe('documento_duplicado')
    const u = await call('PUT', `/Pessoa/id/${c.body.id}`, { ...nova, nome: 'Teste 2' }, token)
    expect(u.status).toBe(204)
    expect((await call('GET', `/Pessoa/id/${c.body.id}`, undefined, token)).body.nome).toBe('Teste 2')
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

async function plataformaToken() {
  const r = await call('POST', '/auth/login', { email: 'plataforma@exemplo.com.br', senha: 'senha1234' })
  return r.body.accessToken as string
}

describe('mock registry: CEP e Municipio', () => {
  beforeEach(() => resetMock())

  it('GET /cep/{cep}: CEP conhecido devolve o endereco e o municipio da fixture (Guaira - PR)', async () => {
    const token = await loginToken()
    const r = await call('GET', '/cep/85980000', undefined, token)
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({ cep: '85980000', logradouro: 'Rua Quinze de Novembro', bairro: 'Centro', uf: 'PR', codigoIbge: '4108809' })
    expect(r.body.municipio).toEqual({ id: 'mun-4108809', nome: 'Guaíra', uf: 'PR', codigoIbge: '4108809' })
    expect((await call('GET', '/cep/85.980-000', undefined, token)).status).toBe(200)
  })

  it('CEP sem municipio na tabela -> municipio null; 404; cep_invalido; 502; 429', async () => {
    const token = await loginToken()
    expect((await call('GET', '/cep/69900001', undefined, token)).body.municipio).toBeNull()
    const nf = await call('GET', '/cep/00000000', undefined, token)
    expect([nf.status, nf.body.codigo]).toEqual([404, 'cep_nao_encontrado'])
    const inv = await call('GET', '/cep/123', undefined, token)
    expect([inv.status, inv.body.codigo]).toEqual([400, 'cep_invalido'])
    expect((await call('GET', '/cep/00000502', undefined, token)).body.codigo).toBe('servico_cep_indisponivel')
    const rl = await call('GET', '/cep/00000429', undefined, token)
    expect([rl.status, rl.headers.get('Retry-After')]).toEqual([429, '30'])
    expect((await call('GET', '/cep/85980000')).status).toBe(401)
  })

  it('GET /Municipio acha Guaira por nome com curinga', async () => {
    const token = await loginToken()
    const r = await call('GET', '/Municipio', undefined, token, { nome: '*gua*', sortBy: 'nome' })
    expect(r.body.map((m: { nome: string }) => m.nome)).toEqual(['Guaíra'])
  })
})

describe('mock registry: admin da plataforma', () => {
  beforeEach(() => resetMock())

  it('so administrador da plataforma acessa /admin (403 para os demais)', async () => {
    const token = await loginToken()
    const r = await call('GET', '/admin/usuarios/count', undefined, token)
    expect([r.status, r.body.codigo]).toEqual([403, 'acesso_negado'])
    expect((await call('GET', '/admin/municipios/importacoes', undefined, token)).status).toBe(403)
  })

  it('usuarios: lista paginada, count, busca e ordenacao', async () => {
    const token = await plataformaToken()
    const total = (await call('GET', '/admin/usuarios/count', undefined, token)).body.count as number
    expect(total).toBeGreaterThan(20)
    const p1 = await call('GET', '/admin/usuarios/0/9', undefined, token, { sortBy: 'nome' })
    expect(p1.body).toHaveLength(10)
    const busca = await call('GET', '/admin/usuarios/count', undefined, token, { busca: 'multi@' })
    expect(busca.body.count).toBe(1)
    const lista = await call('GET', '/admin/usuarios/0/9', undefined, token, { busca: 'DIEGO' })
    expect(lista.body.map((u: { nome: string }) => u.nome)).toEqual(['Diego Multiempresa'])
  })

  it('usuarios: criar (validacoes e e-mail duplicado), editar, senha e vinculos sem exclusao', async () => {
    const token = await plataformaToken()
    const novo = { nome: 'Nova Pessoa', email: 'nova@exemplo.com.br', senha: 'temporaria1', ativo: true, vinculos: [{ empresaId: 'emp-1', papel: 'operador' }] }
    expect((await call('POST', '/admin/usuarios', { ...novo, senha: 'curta' }, token)).body.codigo).toBe('senha_invalida')
    const c = await call('POST', '/admin/usuarios', novo, token)
    expect(c.status).toBe(200)
    expect((await call('POST', '/admin/usuarios', novo, token)).body.codigo).toBe('email_duplicado')
    const id = c.body.id as string
    const u = await call('PATCH', `/admin/usuarios/${id}`, { nome: 'Nova Editada', whatsapp: '11999990000', ativo: false }, token)
    expect(u.body).toMatchObject({ nome: 'Nova Editada', ativo: false })
    expect((await call('POST', `/admin/usuarios/${id}/senha`, { senha: 'outrasenha1' }, token)).status).toBe(204)
    expect((await call('POST', `/admin/usuarios/${id}/senha`, { senha: 'x' }, token)).status).toBe(400)

    const v1 = await call('GET', `/admin/usuarios/${id}/vinculos`, undefined, token)
    expect(v1.body).toHaveLength(1)
    expect(v1.body[0].empresa.razaoSocial).toContain('Horizonte')
    expect((await call('POST', `/admin/usuarios/${id}/vinculos`, { empresaId: 'emp-1', papel: 'consulta' }, token)).body.codigo).toBe('vinculo_duplicado')
    const v2 = await call('POST', `/admin/usuarios/${id}/vinculos`, { empresaId: 'emp-2', papel: 'consulta' }, token)
    expect(v2.status).toBe(200)
    const off = await call('PATCH', `/admin/usuarios/${id}/vinculos/${v2.body.id}`, { papel: 'consulta', ativo: false }, token)
    expect(off.body.ativo).toBe(false)
    expect((await call('GET', `/admin/usuarios/${id}/vinculos`, undefined, token)).body).toHaveLength(2) // desativa, nunca exclui
    expect((await call('GET', '/admin/usuarios/nao-existe/vinculos', undefined, token)).status).toBe(404)
  })

  it('GET /Empresa filtra por razaoSocial (Combobox de empresa)', async () => {
    const token = await plataformaToken()
    const r = await call('GET', '/Empresa', undefined, token, { razaoSocial: '*bella*' })
    expect(r.body).toHaveLength(1)
  })

  it('importacao de municipios: resumo (criados, atualizados, inalterados, rejeitados) e historico', async () => {
    const token = await plataformaToken()
    const csv = ['codigoIbge;nome;uf', '4108809;Guaíra;PR', '4106902;Curitiba Nova;PR', '4100103;Abatiá;PR', '123;Curto;PR', '4100202;;PR'].join('\n')
    const form = new FormData()
    form.append('arquivo', new File([csv], 'municipios.csv', { type: 'text/csv' }))
    const r = await call('POST', '/admin/municipios/importacao', form, token)
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({ linhasLidas: 5, criados: 1, atualizados: 1, inalterados: 1 })
    expect(r.body.rejeitados).toEqual([
      { linha: 5, motivo: expect.stringContaining('IBGE') },
      { linha: 6, motivo: expect.stringContaining('Nome') },
    ])
    const hist = await call('GET', '/admin/municipios/importacoes', undefined, token)
    expect(hist.body[0]).toMatchObject({ id: r.body.id, arquivo: 'municipios.csv', rejeitados: 2 })
    const busca = await call('GET', '/Municipio', undefined, token, { nome: '*abati*' })
    expect(busca.body).toHaveLength(1)
    expect((await call('POST', '/admin/municipios/importacao', new FormData(), token)).body.codigo).toBe('arquivo_obrigatorio')
  })
})
describe('mock registry: Pessoa cliente/fornecedor', () => {
  beforeEach(() => resetMock())

  it('filtra por cliente=true e fornecedor=true em lista, faixa e count', async () => {
    const token = await loginToken()
    const clientes = await call('GET', '/Pessoa/count', undefined, token, { cliente: 'true' })
    const fornecedores = await call('GET', '/Pessoa/count', undefined, token, { fornecedor: 'true' })
    expect(clientes.body.count).toBe(36)
    expect(fornecedores.body.count).toBe(2)
    const lista = await call('GET', '/Pessoa', undefined, token, { fornecedor: 'true' })
    expect(lista.body).toHaveLength(2)
    expect(lista.body.every((p: { fornecedor: boolean }) => p.fornecedor)).toBe(true)
    expect(lista.body.filter((p: { cliente: boolean }) => p.cliente)).toHaveLength(1) // um e ambos
    const faixa = await call('GET', '/Pessoa/0/9', undefined, token, { cliente: 'true' })
    expect(faixa.body).toHaveLength(10)
    expect(faixa.body.every((p: { cliente: boolean }) => p.cliente)).toBe(true)
    const nome = await call('GET', '/Pessoa/count', undefined, token, { cliente: 'true', nome: 'Carlos*' })
    expect(nome.body.count).toBeGreaterThan(0)
  })

  it('POST e PUT preservam e devolvem cliente/fornecedor', async () => {
    const token = await loginToken()
    const nova = { tipoPessoa: 'juridica', documento: '45723174000110', nome: 'Fornecedora', cliente: false, fornecedor: true }
    const c = await call('POST', '/Pessoa', nova, token)
    expect(c.body).toMatchObject({ cliente: false, fornecedor: true })
    const u = await call('PUT', `/Pessoa/id/${c.body.id}`, { ...nova, cliente: true }, token)
    expect(u.status).toBe(204)
    const g = await call('GET', `/Pessoa/id/${c.body.id}`, undefined, token)
    expect(g.body).toMatchObject({ cliente: true, fornecedor: true })
  })
})

describe('mock registry: GET /consulta-cnpj/{cnpj} (usuarios da empresa)', () => {
  beforeEach(() => resetMock())
  const login = async (email: string) => ((await call('POST', '/auth/login', { email, senha: 'senha1234' })).body.accessToken as string)
  const configurar = async () => {
    const t = await login('plataforma@exemplo.com.br')
    await call('PUT', '/admin/configuracoes/consulta-cnpj', { urlBase: 'https://x.com/cnpj', token: 'abc' }, t)
    return t
  }

  it('administrador, operador e plataforma consultam; consulta recebe 403', async () => {
    await configurar()
    for (const email of ['admin@exemplo.com.br', 'operador@exemplo.com.br', 'plataforma@exemplo.com.br']) {
      const t = await login(email)
      const r = await call('GET', '/consulta-cnpj/27865757000102', undefined, t)
      expect([email, r.status]).toEqual([email, 200])
      expect(r.body.socios).toEqual([{ nome: 'Maria Souza', tipo: 'Pessoa Física', qualificacao: 'Sócio-Administrador' }])
    }
    const c = await login('consulta@exemplo.com.br')
    expect((await call('GET', '/consulta-cnpj/27865757000102', undefined, c)).status).toBe(403)
  })

  it('erros: 400, 409 sem token, 404, 502, 429; rota /admin antiga segue valendo so para plataforma', async () => {
    const t = await login('admin@exemplo.com.br')
    expect((await call('GET', '/consulta-cnpj/123', undefined, t)).body.codigo).toBe('cnpj_invalido')
    expect((await call('GET', '/consulta-cnpj/27865757000102', undefined, t)).body.codigo).toBe('token_nao_configurado')
    expect((await call('GET', '/admin/consulta-cnpj/27865757000102', undefined, t)).status).toBe(403)
    const p = await configurar() // o mock guarda um so token: este login invalida o do administrador
    expect((await call('GET', '/admin/consulta-cnpj/27865757000102', undefined, p)).status).toBe(200)
    const t2 = await login('admin@exemplo.com.br')
    expect((await call('GET', '/consulta-cnpj/00000000000191', undefined, t2)).status).toBe(404)
    expect((await call('GET', '/consulta-cnpj/11222333000181', undefined, t2)).status).toBe(502)
    expect((await call('GET', '/consulta-cnpj/11444777000161', undefined, t2)).status).toBe(429)
  })
})

describe('mock registry: integracao IntegraNotas', () => {
  beforeEach(() => resetMock())
  const URL_CFG = '/admin/configuracoes/integranotas'
  const plataforma = async () => (await call('POST', '/auth/login', { email: 'plataforma@exemplo.com.br', senha: 'senha1234' })).body.accessToken as string

  it('403 para quem nao e plataforma', async () => {
    expect((await call('GET', URL_CFG, undefined, await loginToken())).status).toBe(403)
  })

  it('estado inicial: homologacao, producao bloqueada, sem tokens', async () => {
    const r = await call('GET', URL_CFG, undefined, await plataforma())
    expect(r.body).toMatchObject({ ambienteAtivo: 'homologacao', producaoPermitida: false, tokenHomologacaoConfigurado: false, tokenProducaoConfigurado: false, urlPublicaWebhook: null, atualizadoPor: null })
  })

  it('valida URLs, ambiente e producao bloqueada (409)', async () => {
    const t = await plataforma()
    for (const ruim of ['http://api.integranotas.com.br', 'https://evil.com', 'https://api.integranotas.com.br?a=1', 'https://u:p@api.integranotas.com.br']) {
      expect((await call('PUT', URL_CFG, { urlProducao: ruim }, t)).body.codigo).toBe('url_invalida')
    }
    expect((await call('PUT', URL_CFG, { urlPublicaWebhook: 'http://x.com.br' }, t)).body.codigo).toBe('url_invalida')
    expect((await call('PUT', URL_CFG, { ambienteAtivo: 'teste' }, t)).body.codigo).toBe('ambiente_invalido')
    expect((await call('PUT', URL_CFG, { tokenProducao: 5 }, t)).body.codigo).toBe('token_invalido')
    const bloq = await call('PUT', URL_CFG, { ambienteAtivo: 'producao' }, t)
    expect([bloq.status, bloq.body.codigo]).toEqual([409, 'producao_bloqueada'])
  })

  it('ausente mantem, texto grava, vazio remove; tokens nunca voltam', async () => {
    const t = await plataforma()
    expect((await call('PUT', URL_CFG, { tokenHomologacao: 'seg-h', tokenProducao: 'seg-p', urlPublicaWebhook: 'https://api.empresa.com.br' }, t)).status).toBe(204)
    const g1 = await call('GET', URL_CFG, undefined, t)
    expect(g1.body).toMatchObject({ tokenHomologacaoConfigurado: true, tokenProducaoConfigurado: true, urlPublicaWebhook: 'https://api.empresa.com.br' })
    expect(JSON.stringify(g1.body)).not.toContain('seg-')
    expect(g1.body.atualizadoPor.nome).toBeTruthy()
    await call('PUT', URL_CFG, { urlHomologacao: 'https://x.homologacao.integranotas.com.br' }, t)
    expect((await call('GET', URL_CFG, undefined, t)).body).toMatchObject({ tokenHomologacaoConfigurado: true, urlPublicaWebhook: 'https://api.empresa.com.br' })
    await call('PUT', URL_CFG, { tokenHomologacao: '', urlPublicaWebhook: '' }, t)
    expect((await call('GET', URL_CFG, undefined, t)).body).toMatchObject({ tokenHomologacaoConfigurado: false, tokenProducaoConfigurado: true, urlPublicaWebhook: null })
  })
})

describe('mock registry: consulta de CNPJ', () => {
  beforeEach(() => resetMock())
  const URL_CFG = '/admin/configuracoes/consulta-cnpj'

  it('403 para quem nao e plataforma', async () => {
    const token = await loginToken()
    expect((await call('GET', URL_CFG, undefined, token)).status).toBe(403)
  })

  it('valida https, aplica a semantica do token e nunca devolve o token', async () => {
    const r = await call('POST', '/auth/login', { email: 'plataforma@exemplo.com.br', senha: 'senha1234' })
    const token = r.body.accessToken as string
    const inicial = await call('GET', URL_CFG, undefined, token)
    expect(inicial.body).toMatchObject({ tokenConfigurado: false, atualizadoPor: null })
    for (const ruim of ['http://x.com/cnpj', 'https://x.com/cnpj/', 'https://u:p@x.com/cnpj', 'https://x.com/cnpj?a=1']) {
      expect((await call('PUT', URL_CFG, { urlBase: ruim }, token)).body.codigo).toBe('url_invalida')
    }
    expect((await call('PUT', URL_CFG, { urlBase: 'https://x.com/cnpj', token: 'abc' }, token)).status).toBe(204)
    const g1 = await call('GET', URL_CFG, undefined, token)
    expect(g1.body).toMatchObject({ urlBase: 'https://x.com/cnpj', tokenConfigurado: true })
    expect(JSON.stringify(g1.body)).not.toContain('abc')
    expect(g1.body.atualizadoPor.nome).toBeTruthy()
    await call('PUT', URL_CFG, { urlBase: 'https://y.com/cnpj' }, token) // ausente = mantem
    expect((await call('GET', URL_CFG, undefined, token)).body.tokenConfigurado).toBe(true)
    await call('PUT', URL_CFG, { urlBase: 'https://y.com/cnpj', token: '' }, token) // vazio = remove
    expect((await call('GET', URL_CFG, undefined, token)).body.tokenConfigurado).toBe(false)
  })

  it('testar: valida CNPJ, exige token e devolve resumo/erros sem vazar o token', async () => {
    const r = await call('POST', '/auth/login', { email: 'plataforma@exemplo.com.br', senha: 'senha1234' })
    const token = r.body.accessToken as string
    const testar = (cnpj: unknown) => call('POST', `${URL_CFG}/testar`, { cnpj }, token)
    expect((await testar('123')).body.codigo).toBe('cnpj_invalido')
    expect((await testar('11111111111111')).status).toBe(400)
    const semToken = await testar('27865757000102')
    expect([semToken.status, semToken.body.codigo]).toEqual([409, 'token_nao_configurado'])
    await call('PUT', URL_CFG, { urlBase: 'https://x.com/cnpj', token: 'segredo-xyz' }, token)
    const ok = await testar('27865757000102')
    expect(ok.status).toBe(200)
    expect(ok.body).toMatchObject({
      cnpj: '27865757000102',
      atividadePrincipal: { codigo: '6201-5/01' },
      endereco: { municipio: 'Guaíra', uf: 'PR' },
      urlConsultada: 'https://x.com/cnpj/27865757000102',
    })
    expect(JSON.stringify(ok.body)).not.toContain('segredo-xyz')
    expect((await testar('00000000000191')).body.codigo).toBe('cnpj_nao_encontrado')
    expect((await testar('00000000000191')).status).toBe(404)
    const recusado = await testar('11222333000181')
    expect([recusado.status, recusado.body.codigo]).toEqual([502, 'token_recusado'])
    const limite = await testar('11444777000161')
    expect([limite.status, limite.body.codigo]).toEqual([429, 'limite_consulta_excedido'])
    const generico = await testar('45723174000110')
    expect(generico.status).toBe(200)
    expect(generico.body.razaoSocial).toBeTruthy()
    expect(JSON.stringify(generico.body)).not.toContain('segredo-xyz')
  })
})
