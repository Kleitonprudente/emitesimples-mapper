import { describe, expect, it } from 'vitest'
import type { SessionData } from '@/lib/api'
import { canAct } from '@/lib/auth'

const sessao = (papel: SessionData['papel'], plataformaAdmin: boolean): SessionData => ({
  usuario: { id: 'u', nome: 'U', email: 'u@x.com' },
  empresa: papel ? { id: 'e', razaoSocial: 'E', nomeFantasia: null } : null,
  papel,
  plataformaAdmin,
})

describe('canAct', () => {
  it('tela de empresa: vale o papel minimo, a plataforma sem papel nao age', () => {
    expect(canAct(sessao('operador', false), 'operador')).toBe(true)
    expect(canAct(sessao('consulta', false), 'operador')).toBe(false)
    expect(canAct(sessao(null, true), 'consulta')).toBe(false)
    expect(canAct(null, 'consulta')).toBe(false)
  })
  it('tela so da plataforma: vale o administrador da plataforma e mais ninguem', () => {
    expect(canAct(sessao(null, true), 'consulta', true)).toBe(true)
    expect(canAct(sessao('administrador', false), 'consulta', true)).toBe(false)
    expect(canAct(null, 'consulta', true)).toBe(false)
  })
})
