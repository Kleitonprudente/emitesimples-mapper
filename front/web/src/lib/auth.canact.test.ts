import { describe, expect, it } from 'vitest'
import type { SessionData } from '@/lib/api'
import { canAct, hasRole, podeCancelarNfse } from '@/lib/auth'

const sessao = (papel: SessionData['papel'], plataformaAdmin: boolean): SessionData => ({
  usuario: { id: 'u', nome: 'U', email: 'u@x.com' },
  empresa: papel ? { id: 'e', razaoSocial: 'E', nomeFantasia: null } : null,
  papel,
  plataformaAdmin,
})

describe('canAct', () => {
  it('tela de empresa: vale o papel minimo, a plataforma sem papel nao age', () => {
    expect(canAct(sessao('OPERADOR', false), 'OPERADOR')).toBe(true)
    expect(canAct(sessao('CONSULTA', false), 'OPERADOR')).toBe(false)
    expect(canAct(sessao(null, true), 'CONSULTA')).toBe(false)
    expect(canAct(null, 'CONSULTA')).toBe(false)
  })
  it('tela so da plataforma: vale o administrador da plataforma e mais ninguem', () => {
    expect(canAct(sessao(null, true), 'CONSULTA', true)).toBe(true)
    expect(canAct(sessao('ADMINISTRADOR', false), 'CONSULTA', true)).toBe(false)
    expect(canAct(null, 'CONSULTA', true)).toBe(false)
  })
})

describe('papel maiusculo (formato 9.7)', () => {
  it('a sessao traz o papel maiusculo e a hierarquia ADMINISTRADOR > OPERADOR > CONSULTA vale', () => {
    expect(hasRole(sessao('ADMINISTRADOR', false), 'OPERADOR')).toBe(true)
    expect(hasRole(sessao('OPERADOR', false), 'ADMINISTRADOR')).toBe(false)
    expect(hasRole(sessao('CONSULTA', false), 'CONSULTA')).toBe(true)
    expect(hasRole(sessao('CONSULTA', false), 'OPERADOR')).toBe(false)
    expect(hasRole(sessao(null, true), 'CONSULTA')).toBe(false)
  })
  it('canAct usa o papel minimo em maiusculo (o que as telas geradas emitem)', () => {
    expect(canAct(sessao('OPERADOR', false), 'OPERADOR')).toBe(true)
    expect(canAct(sessao('CONSULTA', false), 'OPERADOR')).toBe(false)
  })
  it('podeCancelarNfse: sem o campo, so ADMINISTRADOR e plataforma', () => {
    expect(podeCancelarNfse(sessao('ADMINISTRADOR', false))).toBe(true)
    expect(podeCancelarNfse(sessao('OPERADOR', false))).toBe(false)
    expect(podeCancelarNfse(sessao('OPERADOR', false))).toBe(false)
  })
})
