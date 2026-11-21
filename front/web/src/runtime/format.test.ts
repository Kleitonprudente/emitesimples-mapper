import { describe, expect, it } from 'vitest'
import { applyMask, badgeTone, formatCurrency, formatDate, formatDateTime, formatDuration, maskCEP, maskCNPJ, maskCPF, maskDocumento, maskPhone, onlyDigits } from '@/runtime/format'

const nbsp = / /g

describe('format', () => {
  it('mascara CPF, parcial e completo', () => {
    expect(maskCPF('12345678909')).toBe('123.456.789-09')
    expect(maskCPF('1234')).toBe('123.4')
    expect(maskCPF('123456789091234')).toBe('123.456.789-09')
  })
  it('mascara CNPJ', () => {
    expect(maskCNPJ('11222333000181')).toBe('11.222.333/0001-81')
    expect(maskCNPJ('112223')).toBe('11.222.3')
  })
  it('mascara telefone fixo e celular', () => {
    expect(maskPhone('1134567890')).toBe('(11) 3456-7890')
    expect(maskPhone('11987654321')).toBe('(11) 98765-4321')
    expect(maskPhone('11')).toBe('(11')
    expect(maskPhone('')).toBe('')
  })
  it('mascara CEP', () => {
    expect(maskCEP('01310100')).toBe('01310-100')
  })
  it('documento escolhe CPF ou CNPJ pelo tamanho', () => {
    expect(maskDocumento('12345678909')).toBe('123.456.789-09')
    expect(maskDocumento('11222333000181')).toBe('11.222.333/0001-81')
  })
  it('applyMask e so apresentacao: sem mascara devolve o texto; nulo vira vazio', () => {
    expect(applyMask(undefined, 'abc')).toBe('abc')
    expect(applyMask('cpf', null)).toBe('')
    expect(onlyDigits('123.456.789-09')).toBe('12345678909')
  })
  it('moeda em pt-BR', () => {
    expect(formatCurrency(1234.5).replace(nbsp, ' ')).toBe('R$ 1.234,50')
    expect(formatCurrency(null)).toBe('')
    expect(formatCurrency('abc')).toBe('')
  })
  it('data sem deslocar o dia e data-hora', () => {
    expect(formatDate('2026-11-21')).toBe('21/11/2026')
    expect(formatDate('2026-01-01')).toBe('01/01/2026')
    expect(formatDate(null)).toBe('')
    expect(formatDate('lixo')).toBe('')
    expect(formatDateTime('2026-11-21T14:30:00').replace(nbsp, ' ')).toMatch(/^21\/11\/2026,? 14:30$/)
  })
  it('tom de badge por valor de enum', () => {
    expect(badgeTone('ATIVO')).toBe('success')
    expect(badgeTone('cancelada')).toBe('danger')
    expect(badgeTone('QUALQUER')).toBe('neutral')
    expect(badgeTone(null)).toBe('neutral')
  })
  it('duracao em portugues', () => {
    expect(formatDuration(1)).toBe('1 segundo')
    expect(formatDuration(30)).toBe('30 segundos')
    expect(formatDuration(90)).toBe('2 minutos')
    expect(formatDuration(60)).toBe('1 minuto')
  })
})
