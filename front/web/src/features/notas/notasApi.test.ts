import { describe, expect, it } from 'vitest'
import { CANCELAMENTO, ESTADO, ESTADOS, cancelarSchema, dentroDoMesVigente, lerDetalhe, lerNota, mensagemErroNota } from '@/features/notas/notasApi'
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
    const n = lerDetalhe({ id: 'x', estado: 'autorizada' })
    expect(n.cancelamento).toBeNull()
    expect(n.podeCancelar).toBeUndefined()
    expect(lerDetalhe({ id: 'x', podeCancelar: false, cancelamento: { estado: 'processando', codigoMotivo: 2, motivo: 'm' } })).toMatchObject({ podeCancelar: false, cancelamento: { estado: 'processando', codigoMotivo: 2 } })
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
    expect(ESTADO.falha_envio).toEqual({ rotulo: 'Falha no envio', tone: 'warning' })
    expect(ESTADO.rascunho.rotulo).toBe('Rascunho (assistente)')
    expect(ESTADO.descartada.rotulo).toBe('Descartada')
    expect(ESTADOS).toEqual(expect.arrayContaining(['falha_envio', 'rascunho', 'descartada']))
    expect(lerNota({ id: 'x', estado: 'falha_envio' }).estado).toBe('falha_envio')
  })

  it('rotulos do cancelamento: rascunho, expirado e descartado', () => {
    expect(CANCELAMENTO.expirado.rotulo).toBe('Expirado: sem confirmação')
    expect(CANCELAMENTO.descartado.rotulo).toBe('Descartado')
    expect(CANCELAMENTO.rascunho.rotulo).toBe('Rascunho (assistente)')
    for (const estado of ['rascunho', 'processando', 'concluido', 'recusado', 'expirado', 'descartado']) expect(lerDetalhe({ id: 'x', cancelamento: { estado } }).cancelamento?.estado).toBe(estado)
  })

  it('leitura tolerante de podeAprovar, podeDescartar e do id do cancelamento (ausente = false)', () => {
    const vazio = lerDetalhe({ id: 'x', estado: 'rascunho' })
    expect([vazio.podeAprovar, vazio.podeDescartar]).toEqual([false, false])
    const cheio = lerDetalhe({ id: 'x', estado: 'rascunho', podeAprovar: true, podeDescartar: true, cancelamento: { id: 'c1', estado: 'rascunho', podeAprovar: true, podeDescartar: false } })
    expect([cheio.podeAprovar, cheio.podeDescartar]).toEqual([true, true])
    expect(cheio.cancelamento).toMatchObject({ id: 'c1', podeAprovar: true, podeDescartar: false })
    expect(lerDetalhe({ id: 'x', cancelamento: { estado: 'rascunho' } }).cancelamento).toMatchObject({ id: undefined, podeAprovar: false, podeDescartar: false })
  })

  it('cancelamento_nao_delegado usa a mensagem do back, com padrao quando vazia', () => {
    expect(mensagemErroNota(new ApiError(403, 'cancelamento_nao_delegado', 'Peça ao administrador.'))).toBe('Peça ao administrador.')
    expect(mensagemErroNota(new ApiError(403, 'cancelamento_nao_delegado', ''))).toBe('Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')
  })

  it('sessao: podeCancelarNfse explicito vence; ausente deriva do papel (administrador e plataforma sim)', () => {
    const base = { usuario: { id: 'u', nome: 'A', email: 'a@x.com' }, empresa: null, plataformaAdmin: false }
    expect(podeCancelarNfse(null)).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: 'operador', podeCancelarNfse: true })).toBe(true)
    expect(podeCancelarNfse({ ...base, papel: 'administrador', podeCancelarNfse: false })).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: 'administrador' })).toBe(true)
    expect(podeCancelarNfse({ ...base, papel: 'operador' })).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: 'consulta' })).toBe(false)
    expect(podeCancelarNfse({ ...base, papel: null, plataformaAdmin: true })).toBe(true)
  })
})
