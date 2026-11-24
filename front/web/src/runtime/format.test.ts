import { describe, expect, it } from 'vitest'
import { badgeTone, isValidCnpj, isValidCpf } from '@/runtime/format'

describe('isValidCpf', () => {
  it('aceita CPF com digitos verificadores corretos', () => {
    expect(isValidCpf('52998224725')).toBe(true)
    expect(isValidCpf('11144477735')).toBe(true)
  })
  it('recusa digito errado, tamanho errado, repetidos e nao numerico', () => {
    expect(isValidCpf('52998224724')).toBe(false)
    expect(isValidCpf('5299822472')).toBe(false)
    expect(isValidCpf('529982247255')).toBe(false)
    expect(isValidCpf('11111111111')).toBe(false)
    expect(isValidCpf('529.982.247-25')).toBe(false)
    expect(isValidCpf('')).toBe(false)
  })
})

describe('isValidCnpj', () => {
  it('valida CNPJ', () => {
    expect(isValidCnpj('27865757000102')).toBe(true)
    expect(isValidCnpj('27865757000103')).toBe(false)
  })
})

describe('badgeTone com valores tecnicos maiusculos (9.7)', () => {
  it.each([
    ['AUTORIZADA', 'success'],
    ['EM_CONFLITO', 'warning'],
    ['FALHA_ENVIO', 'warning'],
    ['EMITIDA_OFFLINE', 'info'],
    ['DESCARTADA', 'neutral'],
    ['PENDENTE_CONFIRMACAO', 'warning'],
    ['DESATIVADA', 'neutral'],
    ['CONCLUIDA_COM_REJEICOES', 'warning'],
    ['FALHOU', 'danger'],
  ])('%s -> %s', (valor, tom) => {
    expect(badgeTone(valor)).toBe(tom)
  })
})
