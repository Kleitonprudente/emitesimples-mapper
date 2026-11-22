import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fkSource } from '@/lib/fk-sources'

const apiMock = vi.hoisted(() => vi.fn())
vi.mock('@/lib/api', () => ({ api: apiMock }))

beforeEach(() => apiMock.mockReset())

describe('fkSource', () => {
  it('Municipio: busca GET /Municipio?nome=*termo*&sortBy=nome e rotula "Nome - UF"', async () => {
    apiMock.mockResolvedValue([{ id: 'mun-4108809', nome: 'Guaíra', uf: 'PR' }])
    const s = fkSource('Municipio')
    expect(await s.search('gua')).toEqual([{ id: 'mun-4108809', label: 'Guaíra - PR' }])
    expect(apiMock).toHaveBeenCalledWith('/Municipio', { query: { nome: '*gua*', sortBy: 'nome' } })
    await s.search('  ')
    expect(apiMock).toHaveBeenLastCalledWith('/Municipio', { query: { nome: undefined, sortBy: 'nome' } })
  })

  it('labelOf rotula um valor ja carregado e devolve undefined sem nome', () => {
    const s = fkSource('Municipio')
    expect(s.labelOf!({ id: 'x', nome: 'Guaíra', uf: 'PR' })).toBe('Guaíra - PR')
    expect(s.labelOf!({ id: 'x', nome: 'Guaíra' })).toBe('Guaíra')
    expect(s.labelOf!({ id: 'x' })).toBeUndefined()
  })

  it('devolve a mesma instancia a cada chamada (estavel para o Query)', () => {
    expect(fkSource('Municipio')).toBe(fkSource('Municipio'))
    expect(fkSource('Outra')).toBe(fkSource('Outra'))
  })

  it('entidade sem registro usa a busca generica por nome', async () => {
    apiMock.mockResolvedValue([{ id: 'c1', nome: 'Serviços' }])
    const s = fkSource('Categoria')
    expect(s.key).toBe('Categoria')
    expect(await s.search('ser')).toEqual([{ id: 'c1', label: 'Serviços' }])
    expect(apiMock).toHaveBeenCalledWith('/Categoria', { query: { nome: '*ser*', sortBy: 'nome' } })
  })
})