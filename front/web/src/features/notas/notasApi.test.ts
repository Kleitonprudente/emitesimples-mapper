import { describe, expect, it } from 'vitest'
import {
  CANCELAMENTO,
  ESTADO,
  ESTADOS,
  baseIssPadrao,
  cancelarSchema,
  contasDaNota,
  corpoDaNota,
  dentroDoMesVigente,
  issRetidoPadrao,
  lerDetalhe,
  lerNota,
  mensagemErroNota,
  tributosDoServico,
  valoresDaNota,
  type NotaValues,
} from '@/features/notas/notasApi'
import { podeCancelarNfse } from '@/lib/auth'
import { ApiError } from '@/lib/api'

describe('cancelamento: regras do cliente', () => {
  it('dentroDoMesVigente compara ano-mes no fuso de Sao Paulo', () => {
    const agora = new Date('2026-11-23T15:00:00Z')
    expect(dentroDoMesVigente('2026-11-01T00:00:00', agora)).toBe(true)
    expect(dentroDoMesVigente('2026-10-31T23:59:59', agora)).toBe(false)
    expect(dentroDoMesVigente('2026-12-01T01:00:00Z', new Date('2026-11-30T20:00:00Z'))).toBe(true)
    expect(dentroDoMesVigente('2026-11-01T01:00:00Z', agora)).toBe(false)
    expect(dentroDoMesVigente(undefined, agora)).toBe(true)
    expect(dentroDoMesVigente('2026-12-01T01:00:00-03:00', new Date('2026-12-02T12:00:00Z'))).toBe(true)
  })

  it('schema valida 15..255 apos trim e o codigo 1|2|9', () => {
    const ok = (motivo: string, codigoMotivo = 1) => cancelarSchema.safeParse({ codigoMotivo, motivo }).success
    expect(ok('a'.repeat(14))).toBe(false)
    expect(ok(' ' + 'a'.repeat(14) + ' ')).toBe(false)
    expect(ok('a'.repeat(15))).toBe(true)
    expect(ok('a'.repeat(255))).toBe(true)
    expect(ok('a'.repeat(256))).toBe(false)
    expect(ok('a'.repeat(20), 3)).toBe(false)
    expect(cancelarSchema.parse({ codigoMotivo: 9, motivo: '  ' + 'a'.repeat(15) + '  ' }).motivo).toBe('a'.repeat(15))
  })

  it('leitura tolerante: cancelamento e podeCancelar ausentes', () => {
    const n = lerDetalhe({ id: 'x', estado: 'AUTORIZADA' })
    expect(n.cancelamento).toBeNull()
    expect(n.podeCancelar).toBeUndefined()
    expect(lerDetalhe({ id: 'x', podeCancelar: false, cancelamento: { estado: 'PROCESSANDO', codigoMotivo: 2, motivo: 'm' } })).toMatchObject({ podeCancelar: false, cancelamento: { estado: 'PROCESSANDO', codigoMotivo: 2 } })
    expect(lerDetalhe({ id: 'x', cancelamento: { estado: 'desconhecido' } }).cancelamento).toBeNull()
  })

  it('mensagens dos erros de cancelamento', () => {
    expect(mensagemErroNota(new ApiError(409, 'nota_nao_autorizada', 'x'))).toBe('Só é possível cancelar uma nota autorizada.')
    expect(mensagemErroNota(new ApiError(409, 'cancelamento_em_andamento', 'x'))).toContain('em andamento')
    expect(mensagemErroNota(new ApiError(409, 'cancelamento_recusado', 'Recusado pelo gateway.'))).toBe('Recusado pelo gateway.')
    expect(mensagemErroNota(new ApiError(400, 'requisicao_invalida', 'x', { campos: { motivo: 'curto' } }))).toBe('Revise os campos: justificativa.')
  })
})

describe('estados novos e delegacao', () => {
  it('rotulos e tons: falha_envio e aviso, rascunho e do assistente, descartada neutra', () => {
    expect(ESTADO.FALHA_ENVIO).toEqual({ rotulo: 'Falha no envio', tone: 'warning' })
    expect(ESTADO.RASCUNHO.rotulo).toBe('Rascunho (assistente)')
    expect(ESTADO.DESCARTADA.rotulo).toBe('Descartada')
    expect(ESTADOS).toEqual(expect.arrayContaining(['FALHA_ENVIO', 'RASCUNHO', 'DESCARTADA']))
    expect(lerNota({ id: 'x', estado: 'FALHA_ENVIO' }).estado).toBe('FALHA_ENVIO')
  })

  it('rotulos do cancelamento: rascunho, expirado e descartado', () => {
    expect(CANCELAMENTO.EXPIRADO.rotulo).toBe('Expirado: sem confirmação')
    expect(CANCELAMENTO.DESCARTADO.rotulo).toBe('Descartado')
    expect(CANCELAMENTO.RASCUNHO.rotulo).toBe('Rascunho (assistente)')
    for (const estado of ['RASCUNHO', 'PROCESSANDO', 'CONCLUIDO', 'RECUSADO', 'EXPIRADO', 'DESCARTADO']) expect(lerDetalhe({ id: 'x', cancelamento: { estado } }).cancelamento?.estado).toBe(estado)
  })

  it('leitura tolerante de podeAprovar, podeDescartar e do id do cancelamento (ausente = false)', () => {
    const vazio = lerDetalhe({ id: 'x', estado: 'RASCUNHO' })
    expect([vazio.podeAprovar, vazio.podeDescartar]).toEqual([false, false])
    const cheio = lerDetalhe({ id: 'x', estado: 'RASCUNHO', podeAprovar: true, podeDescartar: true, cancelamento: { id: 'c1', estado: 'RASCUNHO', podeAprovar: true, podeDescartar: false } })
    expect([cheio.podeAprovar, cheio.podeDescartar]).toEqual([true, true])
    expect(cheio.cancelamento).toMatchObject({ id: 'c1', podeAprovar: true, podeDescartar: false })
    expect(lerDetalhe({ id: 'x', cancelamento: { estado: 'RASCUNHO' } }).cancelamento).toMatchObject({ id: undefined, podeAprovar: false, podeDescartar: false })
  })

  it('cancelamento_nao_delegado usa a mensagem do back, com padrao quando vazia', () => {
    expect(mensagemErroNota(new ApiError(403, 'cancelamento_nao_delegado', 'Peça ao administrador.'))).toBe('Peça ao administrador.')
    expect(mensagemErroNota(new ApiError(403, 'cancelamento_nao_delegado', ''))).toBe('Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')
  })

  it('sessao: podeCancelarNfse explicito vence; ausente deriva do papel (administrador e plataforma sim)', () => {
    const base = { usuario: { id: 'u', nome: 'A', email: 'a@x.com' }, empresa: null, plataformaAdmin: false }
    expect(podeCancelarNfse(null)).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: 'OPERADOR', podeCancelarNfse: true })).toBe(true)
    expect(podeCancelarNfse({ ...base, papel: 'ADMINISTRADOR', podeCancelarNfse: false })).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: 'ADMINISTRADOR' })).toBe(true)
    expect(podeCancelarNfse({ ...base, papel: 'OPERADOR' })).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: 'CONSULTA' })).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: null, plataformaAdmin: true })).toBe(true)
  })
})

describe('tributacao da nota: contas e padroes (mesmas regras do back)', () => {
  it('base do ISS = valor - desconto, nunca negativa (a conta do governo)', () => {
    expect(baseIssPadrao(1000, 50)).toBe(950)
    expect(baseIssPadrao(333.33)).toBe(333.33)
    expect(baseIssPadrao(10, 20)).toBe(0)
    expect(baseIssPadrao(undefined, 5)).toBeUndefined()
  })

  it('ISS retido = base x aliquota, meio para cima em centavos', () => {
    expect(issRetidoPadrao(950, 5)).toBe(47.5)
    expect(issRetidoPadrao(0.3, 5)).toBe(0.02)
    expect(issRetidoPadrao(100.1, 2.5)).toBe(2.5)
    expect(issRetidoPadrao(125, 2.5)).toBe(3.13)
    expect(issRetidoPadrao(100, undefined)).toBeUndefined()
  })

  it('contas em cinza: ISS retido so com ISS retido ligado; base federal = valor cheio', () => {
    expect(contasDaNota({ valorServicos: 1000, valorDescontoIncondicionado: 50, issRetido: true, aliquotaIss: 5 })).toEqual({ baseIss: 950, issRetido: 47.5, baseFederal: 1000 })
    expect(contasDaNota({ valorServicos: 1000, aliquotaIss: 5 })).toEqual({ baseIss: 1000, issRetido: undefined, baseFederal: 1000 })
    expect(contasDaNota({ valorServicos: 1000 }).baseIss).toBeUndefined() // sem dado de ISS, sem base
    expect(contasDaNota({ valorServicos: 1000, aliquotaIss: 5 }).issRetido).toBeUndefined()
  })

  it('padroes do servico: retencao, aliquota e federais; base e ISS nunca vem do servico', () => {
    const s = { discriminacaoPadrao: 'x', valorPadrao: 200, issRetido: true, aliquotaIss: 5, cstPisCofins: '01', tipoRetencao: 3, aliquotaPis: 0.65, aliquotaCofins: 3 }
    expect(tributosDoServico(s)).toMatchObject({ issRetido: true, aliquotaIss: 5, valorIssRetido: undefined, cstPisCofins: '01', tipoRetencao: 3 })
  })

  it('o corpo leva SEMPRE todos os tributos (em branco = null) e o detalhe volta para o formulario', () => {
    const v = { pessoa: { id: 'p' }, servico: { id: 's' }, valorServicos: 1000, discriminacao: ' d ', issRetido: true, aliquotaIss: 5, valorBaseCalculoIss: 999, valorIssRetido: 49.95, cstPisCofins: '01', valorInss: 110 } as NotaValues
    const c = corpoDaNota(v)
    expect(c.tributos).toMatchObject({ issRetido: true, aliquotaIss: 5, cstPisCofins: '01', valorInss: 110, valorBaseCalculoIss: null, valorIssRetido: null, tipoRetencao: null, valorDescontoIncondicionado: null })
    const d = lerDetalhe({ id: 'n', estado: 'REJEITADA', pessoa: { id: 'p', nome: 'P' }, servico: { id: 's', nome: 'S' }, valorServicos: 1000, discriminacao: 'd',
      tributos: { issRetido: true, aliquotaIss: 5, valorBaseCalculoIss: 950, cstPisCofins: '01', tipoRetencao: 2, valorInss: 110, valorDescontoIncondicionado: 50 } })
    expect(d.tributos).toMatchObject({ issRetido: true, aliquotaIss: 5, valorBaseCalculoIss: 950, cstPisCofins: '01', tipoRetencao: 2, valorInss: 110, valorDescontoIncondicionado: 50 })
    expect(valoresDaNota(d)).toMatchObject({ issRetido: true, valorBaseCalculoIss: 950, cstPisCofins: '01', valorDescontoIncondicionado: 50 })
    expect(lerDetalhe({ id: 'n' }).tributos).toEqual(expect.objectContaining({ issRetido: false, aliquotaIss: undefined }))
  })
})
