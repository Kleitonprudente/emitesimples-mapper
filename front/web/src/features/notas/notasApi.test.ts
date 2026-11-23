import { describe, expect, it } from 'vitest'
import { cancelarSchema, dentroDoMesVigente, lerDetalhe, mensagemErroNota } from '@/features/notas/notasApi'
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
