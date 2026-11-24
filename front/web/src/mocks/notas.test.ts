import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ajustarNfseConfig } from '@/mocks/nfse-config'
import { TEMPO_AUTORIZACAO_MS, VALOR_CANCELAMENTO_PENDENTE, VALOR_CONFLITO, VALOR_REJEICAO } from '@/mocks/notas'
import { handleMock, resetMock, setMockLatency } from '@/mocks/registry'

setMockLatency(0)

async function call(method: string, path: string, body?: unknown, token: string | null = null, query?: Record<string, string>) {
  const res = await handleMock({ method, path, body, token, query })
  const text = await res.text()
  const isJson = res.headers.get('Content-Type')?.includes('json')
  return { status: res.status, body: text && isJson ? JSON.parse(text) : text, type: res.headers.get('Content-Type') }
}
async function login(email: string) {
  return (await call('POST', '/auth/login', { email, senha: 'senha1234' })).body.accessToken as string
}

const habilita = () => ajustarNfseConfig({ habilitada: true, serie: '1', ultimoNumero: 99, statusCadastro: 'cadastrado', certificadoSituacao: 'valido' })
const corpo = (extra: object = {}) => ({ pessoaId: 'pes-0001', servicoId: 'srv-0001', valorServicos: 100, ...extra })

describe('mock de notas NFS-e', () => {
  let token: string
  beforeEach(async () => {
    resetMock()
    token = await login('admin@exemplo.com.br')
  })
  afterEach(() => vi.useRealTimers())

  it('lista seeds, mais recentes primeiro, com count e filtros', async () => {
    const l = await call('GET', '/nfse/notas/0/9', undefined, token)
    expect(l.status).toBe(200)
    expect(l.body.length).toBe(7)
    const datas = l.body.map((n: { dataCriacao: string }) => n.dataCriacao)
    expect(datas).toEqual([...datas].sort().reverse())
    expect((await call('GET', '/nfse/notas/count', undefined, token)).body).toEqual({ count: 7 })
    const rej = await call('GET', '/nfse/notas/0/9', undefined, token, { estado: 'rejeitada' })
    expect(rej.body.map((n: { estado: string }) => n.estado)).toEqual(['rejeitada'])
    const busca = await call('GET', '/nfse/notas/count', undefined, token, { busca: l.body[0].pessoa.nome.slice(0, 5) })
    expect(busca.body.count).toBeGreaterThan(0)
  })

  it('exige empresa na sessao (plataforma 403) e token (401)', async () => {
    const pl = await login('plataforma@exemplo.com.br')
    expect((await call('GET', '/nfse/notas/0/9', undefined, pl)).status).toBe(403)
    expect((await call('GET', '/nfse/notas/0/9')).status).toBe(401)
  })

  it('consulta le mas nao cria, reenvia nem atualiza (403)', async () => {
    habilita()
    const c = await login('consulta@exemplo.com.br')
    expect((await call('GET', '/nfse/notas/nota-0001', undefined, c)).status).toBe(200)
    expect((await call('POST', '/nfse/notas', corpo(), c)).status).toBe(403)
    expect((await call('POST', '/nfse/notas/nota-0003/enviar', {}, c)).status).toBe(403)
    expect((await call('POST', '/nfse/notas/nota-0004/atualizar', undefined, c)).status).toBe(403)
  })

  it('sem NFS-e habilitada: 409 nfse_nao_habilitada', async () => {
    const r = await call('POST', '/nfse/notas', corpo(), token)
    expect([r.status, r.body.codigo]).toEqual([409, 'nfse_nao_habilitada'])
  })

  it('cria e envia: aloca o numero da serie, usa a discriminacao do servico e fica processando', async () => {
    habilita()
    const r = await call('POST', '/nfse/notas', corpo(), token)
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({ serie: '1', numero: 100, estado: 'processando', podeAtualizar: true, podeReenviar: false, discriminacao: 'Serviços de consultoria contábil prestados no período.' })
    const outra = await call('POST', '/nfse/notas', corpo({ discriminacao: 'Minha descrição' }), token)
    expect(outra.body).toMatchObject({ numero: 101, discriminacao: 'Minha descrição' })
    expect((await call('GET', '/nfse/notas/count', undefined, token)).body.count).toBe(9)
  })

  it('processando vira autorizada pelo tempo ou na 2a chamada de atualizar, com PDF valido', async () => {
    habilita()
    const a = (await call('POST', '/nfse/notas', corpo(), token)).body
    expect((await call('POST', `/nfse/notas/${a.id}/atualizar`, undefined, token)).body.estado).toBe('processando')
    const dois = await call('POST', `/nfse/notas/${a.id}/atualizar`, undefined, token)
    expect(dois.body).toMatchObject({ estado: 'autorizada', temPdf: true, podeAtualizar: false })
    const pdf = await call('GET', `/nfse/notas/${a.id}/pdf`, undefined, token)
    expect([pdf.status, pdf.type]).toEqual([200, 'application/pdf'])
    expect(pdf.body.startsWith('%PDF-1.4')).toBe(true)
    expect(pdf.body.trimEnd().endsWith('%%EOF')).toBe(true)

    vi.useFakeTimers({ toFake: ['Date'] })
    const b = (await call('POST', '/nfse/notas', corpo(), token)).body
    vi.setSystemTime(Date.now() + TEMPO_AUTORIZACAO_MS + 1)
    expect((await call('GET', `/nfse/notas/${b.id}`, undefined, token)).body.estado).toBe('autorizada')
  })

  it('PDF indisponivel: 404 pdf_indisponivel; nota inexistente: 404', async () => {
    const r = await call('GET', '/nfse/notas/nota-0006/pdf', undefined, token)
    expect([r.status, r.body.codigo]).toEqual([404, 'pdf_indisponivel'])
    expect((await call('GET', '/nfse/notas/nao-existe', undefined, token)).status).toBe(404)
  })

  it('valor magico rejeita (com errosRetorno) e conflita; reenviar corrigido mantem o numero', async () => {
    habilita()
    const rej = (await call('POST', '/nfse/notas', corpo({ valorServicos: VALOR_REJEICAO }), token)).body
    expect(rej).toMatchObject({ estado: 'rejeitada', podeReenviar: true, mensagemRetorno: expect.any(String) })
    expect(rej.errosRetorno.length).toBeGreaterThan(0)
    const ok = await call('POST', `/nfse/notas/${rej.id}/enviar`, corpo({ valorServicos: 120 }), token)
    expect(ok.body).toMatchObject({ estado: 'processando', numero: rej.numero, valorServicos: 120, errosRetorno: null })
    const conf = (await call('POST', '/nfse/notas', corpo({ valorServicos: VALOR_CONFLITO }), token)).body
    expect(conf).toMatchObject({ estado: 'em_conflito', podeReenviar: false, podeAtualizar: true })
  })

  it('enviar respeita o estado: processando 409, conflito 409, autorizada 409', async () => {
    habilita()
    const p = (await call('POST', '/nfse/notas', corpo(), token)).body
    expect((await call('POST', `/nfse/notas/${p.id}/enviar`, {}, token)).body.codigo).toBe('nota_em_processamento')
    expect((await call('POST', '/nfse/notas/nota-0004/enviar', {}, token)).body.codigo).toBe('nota_em_conflito')
    expect((await call('POST', '/nfse/notas/nota-0001/enviar', {}, token)).body.codigo).toBe('nota_nao_editavel')
  })

  it('erros de validacao e pre-condicao', async () => {
    habilita()
    const inv = await call('POST', '/nfse/notas', corpo({ valorServicos: 0 }), token)
    expect([inv.status, inv.body.codigo]).toEqual([400, 'requisicao_invalida'])
    expect(inv.body.detalhes.campos.valorServicos).toBeTruthy()
    expect((await call('POST', '/nfse/notas', corpo({ servicoId: 'srv-0004' }), token)).body.codigo).toBe('servico_inativo')
    expect((await call('POST', '/nfse/notas', corpo({ servicoId: 'srv-0003' }), token)).body.codigo).toBe('discriminacao_obrigatoria')
    ajustarNfseConfig({ certificadoSituacao: 'valido' })
    ajustarNfseConfig({ statusCadastro: 'nao_cadastrado' })
    expect((await call('POST', '/nfse/notas', corpo(), token)).body.codigo).toBe('emitente_nao_cadastrado')
  })

  it('cliente sem documento: 422 tomador_incompleto com os campos', async () => {
    habilita()
    await call('PUT', '/Pessoa/id/pes-0002', { nome: 'Sem Doc', tipoPessoa: 'fisica', documento: '' }, token)
    const r = await call('POST', '/nfse/notas', corpo({ pessoaId: 'pes-0002' }), token)
    expect([r.status, r.body.codigo]).toEqual([422, 'tomador_incompleto'])
    expect(r.body.detalhes.campos[0].campo).toBe('documento')
  })

  it('serve Servico para o combobox: so ativos, busca por nome e por id', async () => {
    const ativos = await call('GET', '/Servico/0/49', undefined, token, { ativo: 'true', sortBy: 'nome' })
    expect(ativos.body.map((s: { nome: string }) => s.nome)).not.toContain('Serviço descontinuado')
    const busca = await call('GET', '/Servico/0/49', undefined, token, { nome: '*soft*' })
    expect(busca.body).toHaveLength(1)
    expect((await call('GET', '/Servico/id/srv-0001', undefined, token)).body.discriminacaoPadrao).toBeTruthy()
    expect((await call('GET', '/Servico/id/x', undefined, token)).status).toBe(404)
  })

  it('o certificado enviado em Empresa passa a valer no GET /configuracao/nfse', async () => {
    ajustarNfseConfig({ habilitada: true, serie: '1', statusCadastro: 'cadastrado' })
    expect((await call('GET', '/configuracao/nfse', undefined, token)).body.certificadoSituacao).toBe('nao_enviado')
  })

  describe('cancelar', () => {
    const motivo = { codigoMotivo: 1, motivo: 'Valor emitido errado, nota refeita.' }
    const autorizada = async (extra: object = {}) => {
      habilita()
      const a = (await call('POST', '/nfse/notas', corpo(extra), token)).body
      await call('POST', `/nfse/notas/${a.id}/atualizar`, undefined, token)
      return (await call('POST', `/nfse/notas/${a.id}/atualizar`, undefined, token)).body
    }

    it('nota autorizada do mes vigente vira cancelada com o bloco de cancelamento', async () => {
      const a = await autorizada()
      expect([a.estado, a.podeCancelar]).toEqual(['autorizada', true])
      const r = await call('POST', `/nfse/notas/${a.id}/cancelar`, motivo, token)
      expect(r.status).toBe(200)
      expect(r.body).toMatchObject({ estado: 'cancelada', podeCancelar: false, cancelamento: { estado: 'concluido', codigoMotivo: 1, motivo: motivo.motivo } })
      expect((await call('GET', `/nfse/notas/${a.id}`, undefined, token)).body.estado).toBe('cancelada')
    })

    it('valida o corpo (15 a 255 apos trim, codigo 1|2|9) com 400 e campos', async () => {
      const a = await autorizada()
      const curto = await call('POST', `/nfse/notas/${a.id}/cancelar`, { codigoMotivo: 1, motivo: '   curto   ' }, token)
      expect([curto.status, curto.body.codigo]).toEqual([400, 'requisicao_invalida'])
      expect(curto.body.detalhes.campos.motivo).toBeTruthy()
      const longo = await call('POST', `/nfse/notas/${a.id}/cancelar`, { codigoMotivo: 9, motivo: 'x'.repeat(256) }, token)
      expect(longo.status).toBe(400)
      const codigo = await call('POST', `/nfse/notas/${a.id}/cancelar`, { codigoMotivo: 3, motivo: 'x'.repeat(15) }, token)
      expect(codigo.body.detalhes.campos.codigoMotivo).toBeTruthy()
      expect((await call('POST', `/nfse/notas/${a.id}/cancelar`, { codigoMotivo: 2, motivo: 'x'.repeat(255) }, token)).status).toBe(200)
    })

    it('fora do mes vigente: 409 cancelamento_fora_do_mes e a nota segue autorizada', async () => {
      const antes = (await call('GET', '/nfse/notas/nota-0002', undefined, token)).body
      expect([antes.estado, antes.podeCancelar]).toEqual(['autorizada', false])
      const r = await call('POST', '/nfse/notas/nota-0002/cancelar', motivo, token)
      expect([r.status, r.body.codigo]).toEqual([409, 'cancelamento_fora_do_mes'])
      expect((await call('GET', '/nfse/notas/nota-0002', undefined, token)).body.estado).toBe('autorizada')
    })

    it('nota que nao esta autorizada: 409 nota_nao_autorizada; consulta: 403; inexistente: 404', async () => {
      habilita()
      const p = (await call('POST', '/nfse/notas', corpo(), token)).body
      expect((await call('POST', `/nfse/notas/${p.id}/cancelar`, motivo, token)).body.codigo).toBe('nota_nao_autorizada')
      expect((await call('POST', '/nfse/notas/nota-0005/cancelar', motivo, token)).body.codigo).toBe('nota_nao_autorizada')
      expect((await call('POST', '/nfse/notas/nao-existe/cancelar', motivo, token)).status).toBe(404)
      const c = await login('consulta@exemplo.com.br')
      expect((await call('POST', '/nfse/notas/nota-0001/cancelar', motivo, c)).status).toBe(403)
    })

    it('valor 888.88 fica processando (nota segue autorizada) e conclui na 2a consulta; repetir da 409', async () => {
      const a = await autorizada({ valorServicos: VALOR_CANCELAMENTO_PENDENTE })
      const r = await call('POST', `/nfse/notas/${a.id}/cancelar`, motivo, token)
      expect(r.body).toMatchObject({ estado: 'autorizada', podeAtualizar: true, podeCancelar: false, cancelamento: { estado: 'processando' } })
      expect((await call('POST', `/nfse/notas/${a.id}/cancelar`, motivo, token)).body.codigo).toBe('cancelamento_em_andamento')
      expect((await call('POST', `/nfse/notas/${a.id}/atualizar`, undefined, token)).body.estado).toBe('autorizada')
      expect((await call('POST', `/nfse/notas/${a.id}/atualizar`, undefined, token)).body).toMatchObject({ estado: 'cancelada', cancelamento: { estado: 'concluido' } })
    })
  })
})

describe('mock de notas NFS-e: delegacao, estados novos e assistente', () => {
  // O mock tem UMA sessao por vez: cada chamada entra como o usuario pedido (login logo antes).
  const como = (email: string, empresaId?: string) => async (method: string, path: string, body?: unknown, query?: Record<string, string>) => {
    const t = (await call('POST', '/auth/login', { email, senha: 'senha1234', empresaId })).body.accessToken as string
    return call(method, path, body, t, query)
  }
  const admin = como('admin@exemplo.com.br')
  const operador = como('operador@exemplo.com.br')
  const consulta = como('consulta@exemplo.com.br')
  const delegado = como('multi@exemplo.com.br', 'emp-2')

  beforeEach(() => {
    resetMock()
    habilita()
  })
  afterEach(() => vi.useRealTimers())

  const motivo = { codigoMotivo: 1, motivo: 'Valor emitido errado, nota refeita.' }
  const autorizada = async () => {
    const a = (await admin('POST', '/nfse/notas', corpo())).body
    await admin('POST', `/nfse/notas/${a.id}/atualizar`)
    return (await admin('POST', `/nfse/notas/${a.id}/atualizar`)).body
  }

  it('sessao traz podeCancelarNfse: admin e plataforma sim, operador nao, operador delegado sim (login, refresh e /auth/me)', async () => {
    const dados = async (email: string, empresaId?: string) => (await call('POST', '/auth/login', { email, senha: 'senha1234', empresaId })).body
    expect((await dados('admin@exemplo.com.br')).podeCancelarNfse).toBe(true)
    expect((await dados('operador@exemplo.com.br')).podeCancelarNfse).toBe(false)
    expect((await dados('consulta@exemplo.com.br')).podeCancelarNfse).toBe(false)
    expect((await dados('plataforma@exemplo.com.br')).podeCancelarNfse).toBe(true)
    expect((await dados('multi@exemplo.com.br', 'emp-1')).podeCancelarNfse).toBe(true)
    const d = await dados('multi@exemplo.com.br', 'emp-2')
    expect([d.papel, d.podeCancelarNfse]).toEqual(['operador', true])
    const me = await call('GET', '/auth/me', undefined, d.accessToken)
    expect(me.body.podeCancelarNfse).toBe(true)
    const renovada = await call('POST', '/auth/refresh')
    expect(renovada.body.podeCancelarNfse).toBe(true)
  })

  it('seeds novos: falha_envio reenviavel com o mesmo numero e rascunho do assistente (nao reenviavel)', async () => {
    const filtrada = await operador('GET', '/nfse/notas/0/9', undefined, { estado: 'falha_envio' })
    expect(filtrada.body.map((n: { id: string }) => n.id)).toEqual(['nota-0007'])
    const falha = (await operador('GET', '/nfse/notas/nota-0007')).body
    expect(falha).toMatchObject({ estado: 'falha_envio', podeReenviar: true, editavel: true, podeAprovar: false })
    const reenviada = await operador('POST', '/nfse/notas/nota-0007/enviar', {})
    expect(reenviada.status).toBe(200)
    expect(reenviada.body).toMatchObject({ estado: 'processando', numero: falha.numero, serie: falha.serie })
    const rascunho = (await operador('GET', '/nfse/notas/nota-0006')).body
    expect(rascunho).toMatchObject({ estado: 'rascunho', podeReenviar: false, podeAprovar: true, podeDescartar: true })
    expect((await operador('POST', '/nfse/notas/nota-0006/enviar', {})).body.codigo).toBe('nota_nao_editavel')
  })

  it('cancelar: operador sem delegacao leva 403 cancelamento_nao_delegado e podeCancelar=false; administrador e operador delegado cancelam', async () => {
    const a = await autorizada()
    expect((await operador('GET', `/nfse/notas/${a.id}`)).body.podeCancelar).toBe(false)
    const r = await operador('POST', `/nfse/notas/${a.id}/cancelar`, motivo)
    expect([r.status, r.body.codigo]).toEqual([403, 'cancelamento_nao_delegado'])
    expect(r.body.mensagem).toBe('Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')
    expect((await admin('GET', `/nfse/notas/${a.id}`)).body.estado).toBe('autorizada')

    expect((await delegado('GET', `/nfse/notas/${a.id}`)).body.podeCancelar).toBe(true)
    expect((await delegado('POST', `/nfse/notas/${a.id}/cancelar`, motivo)).body.estado).toBe('cancelada')
    const b = await autorizada()
    expect((await admin('POST', `/nfse/notas/${b.id}/cancelar`, motivo)).body.estado).toBe('cancelada')
  })

  it('preparar cria o rascunho SEM numero, sem enviar; consulta 403; corpo invalido 400', async () => {
    const r = await operador('POST', '/nfse/notas/preparar', corpo())
    expect(r.status).toBe(200)
    expect(r.body).toMatchObject({ estado: 'rascunho', podeAprovar: true, podeDescartar: true, podeReenviar: false, serie: '', numero: 0 })
    expect((await operador('GET', '/nfse/notas/count', undefined, { estado: 'processando' })).body.count).toBe(0)
    expect((await consulta('POST', '/nfse/notas/preparar', corpo())).status).toBe(403)
    expect((await operador('POST', '/nfse/notas/preparar', corpo({ valorServicos: 0 }))).body.codigo).toBe('requisicao_invalida')
  })

  it('aprovar aloca o numero e envia o rascunho (processando); fora de rascunho da 409; consulta 403', async () => {
    const p = (await operador('POST', '/nfse/notas/preparar', corpo())).body
    expect((await consulta('POST', `/nfse/notas/${p.id}/aprovar`)).status).toBe(403)
    const r = await operador('POST', `/nfse/notas/${p.id}/aprovar`)
    expect(p.numero).toBe(0)
    expect(r.body).toMatchObject({ estado: 'processando', serie: '1', podeAprovar: false, podeDescartar: false })
    expect(r.body.numero).toBeGreaterThan(0)
    expect((await operador('POST', `/nfse/notas/${p.id}/aprovar`)).body.codigo).toBe('nota_nao_editavel')
    const rej = (await operador('POST', '/nfse/notas/preparar', corpo({ valorServicos: VALOR_REJEICAO }))).body
    expect((await operador('POST', `/nfse/notas/${rej.id}/aprovar`, {})).body.estado).toBe('rejeitada')
  })

  it('aprovar aceita corpo opcional com correcoes e revalida as pre-condicoes da empresa', async () => {
    const p = (await operador('POST', '/nfse/notas/preparar', corpo())).body
    const r = await operador('POST', `/nfse/notas/${p.id}/aprovar`, { valorServicos: 321 })
    expect(r.body).toMatchObject({ estado: 'processando', valorServicos: 321 })
    const q = (await operador('POST', '/nfse/notas/preparar', corpo())).body
    ajustarNfseConfig({ habilitada: false })
    expect((await operador('POST', `/nfse/notas/${q.id}/aprovar`)).body.codigo).toBe('nfse_nao_habilitada')
    expect((await operador('GET', `/nfse/notas/${q.id}`)).body.estado).toBe('rascunho')
  })

  it('descartar leva o rascunho a descartada; so rascunho; consulta 403', async () => {
    const p = (await operador('POST', '/nfse/notas/preparar', corpo())).body
    expect((await consulta('POST', `/nfse/notas/${p.id}/descartar`)).status).toBe(403)
    expect((await operador('POST', `/nfse/notas/${p.id}/descartar`)).body).toMatchObject({ estado: 'descartada', podeAprovar: false, podeDescartar: false })
    expect((await operador('POST', `/nfse/notas/${p.id}/descartar`)).body.codigo).toBe('nota_nao_editavel')
    expect((await operador('POST', '/nfse/notas/nota-0001/descartar')).status).toBe(409)
  })

  describe('cancelamento do assistente', () => {
    it('preparar gera cancelamento rascunho com id; operador sem delegacao so descarta, administrador aprova (conclui)', async () => {
      const a = await autorizada()
      const r = await operador('POST', `/nfse/notas/${a.id}/cancelamentos/preparar`, motivo)
      expect(r.status).toBe(200)
      expect(r.body.estado).toBe('autorizada')
      expect(r.body.cancelamento).toMatchObject({ estado: 'rascunho', codigoMotivo: 1, podeAprovar: false, podeDescartar: true })
      expect(r.body.cancelamento.id).toMatch(/^canc-/)
      expect(r.body.podeCancelar).toBe(false)
      const id = r.body.cancelamento.id as string
      const negado = await operador('POST', `/nfse/cancelamentos/${id}/aprovar`)
      expect([negado.status, negado.body.codigo]).toEqual([403, 'cancelamento_nao_delegado'])
      const visto = (await admin('GET', `/nfse/notas/${a.id}`)).body
      expect(visto.cancelamento).toMatchObject({ estado: 'rascunho', podeAprovar: true, podeDescartar: true })
      const ok = await admin('POST', `/nfse/cancelamentos/${id}/aprovar`)
      expect(ok.body).toMatchObject({ estado: 'cancelada', cancelamento: { estado: 'concluido', id } })
    })

    it('preparar valida o corpo e as regras: 400, nota nao autorizada, fora do mes, ja em andamento; consulta 403', async () => {
      const a = await autorizada()
      expect((await operador('POST', `/nfse/notas/${a.id}/cancelamentos/preparar`, { codigoMotivo: 1, motivo: 'curto' })).body.codigo).toBe('requisicao_invalida')
      expect((await operador('POST', '/nfse/notas/nota-0005/cancelamentos/preparar', motivo)).body.codigo).toBe('nota_nao_autorizada')
      expect((await operador('POST', '/nfse/notas/nota-0002/cancelamentos/preparar', motivo)).body.codigo).toBe('cancelamento_fora_do_mes')
      expect((await consulta('POST', `/nfse/notas/${a.id}/cancelamentos/preparar`, motivo)).status).toBe(403)
      expect((await operador('POST', `/nfse/notas/${a.id}/cancelamentos/preparar`, motivo)).status).toBe(200)
      expect((await operador('POST', `/nfse/notas/${a.id}/cancelamentos/preparar`, motivo)).body.codigo).toBe('cancelamento_em_andamento')
      expect((await admin('POST', `/nfse/notas/${a.id}/cancelar`, motivo)).body.codigo).toBe('cancelamento_em_andamento')
    })

    it('aprovar reavalia o mes vigente: virou o mes entre preparar e aprovar -> 409 cancelamento_fora_do_mes', async () => {
      vi.useFakeTimers({ toFake: ['Date'] })
      vi.setSystemTime(new Date('2026-11-23T15:00:00Z'))
      const a = await autorizada()
      const id = (await admin('POST', `/nfse/notas/${a.id}/cancelamentos/preparar`, motivo)).body.cancelamento.id as string
      vi.setSystemTime(new Date('2026-12-02T15:00:00Z'))
      const r = await admin('POST', `/nfse/cancelamentos/${id}/aprovar`)
      expect([r.status, r.body.codigo]).toEqual([409, 'cancelamento_fora_do_mes'])
      expect((await admin('GET', `/nfse/notas/${a.id}`)).body.cancelamento.estado).toBe('rascunho')
    })

    it('descartar leva a descartado e libera novo cancelamento; aprovar com 888.88 fica processando; so em rascunho', async () => {
      const a = await autorizada()
      const id = (await admin('POST', `/nfse/notas/${a.id}/cancelamentos/preparar`, motivo)).body.cancelamento.id as string
      const d = await operador('POST', `/nfse/cancelamentos/${id}/descartar`)
      expect(d.body).toMatchObject({ estado: 'autorizada', podeCancelar: false, cancelamento: { estado: 'descartado', podeAprovar: false, podeDescartar: false } })
      expect((await admin('POST', `/nfse/cancelamentos/${id}/descartar`)).body.codigo).toBe('cancelamento_nao_editavel')
      expect((await admin('POST', `/nfse/cancelamentos/${id}/aprovar`)).status).toBe(409)
      expect((await admin('POST', '/nfse/cancelamentos/nao-existe/aprovar')).status).toBe(404)
      expect((await admin('GET', `/nfse/notas/${a.id}`)).body.podeCancelar).toBe(true)

      const p = (await admin('POST', '/nfse/notas', corpo({ valorServicos: VALOR_CANCELAMENTO_PENDENTE }))).body
      await admin('POST', `/nfse/notas/${p.id}/atualizar`)
      await admin('POST', `/nfse/notas/${p.id}/atualizar`)
      const id2 = (await admin('POST', `/nfse/notas/${p.id}/cancelamentos/preparar`, motivo)).body.cancelamento.id as string
      const ok = await admin('POST', `/nfse/cancelamentos/${id2}/aprovar`)
      expect(ok.body).toMatchObject({ estado: 'autorizada', podeAtualizar: true, cancelamento: { estado: 'processando' } })
      expect((await admin('POST', `/nfse/notas/${p.id}/atualizar`)).body.cancelamento.estado).toBe('processando')
      expect((await admin('POST', `/nfse/notas/${p.id}/atualizar`)).body).toMatchObject({ estado: 'cancelada', cancelamento: { estado: 'concluido' } })
    })
  })
})
