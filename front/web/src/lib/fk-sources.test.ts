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

  it('Cliente: lista Pessoa com cliente=true e rotula "Nome - documento mascarado"', async () => {
    apiMock.mockResolvedValue([{ id: 'p1', nome: 'Ana Paula', documento: '12345678909' }, { id: 'p2', nome: 'Sem Doc' }])
    const s = fkSource('Cliente')
    expect(await s.search('ana')).toEqual([{ id: 'p1', label: 'Ana Paula - 123.456.789-09' }, { id: 'p2', label: 'Sem Doc' }])
    expect(apiMock).toHaveBeenCalledWith('/Pessoa/0/49', { query: { cliente: true, nome: '*ana*', sortBy: 'nome' } })
    await s.search(' ')
    expect(apiMock).toHaveBeenLastCalledWith('/Pessoa/0/49', { query: { cliente: true, nome: undefined, sortBy: 'nome' } })
    expect(s.labelOf!({ id: 'p1', nome: 'Acme', documento: '11222333000181' })).toBe('Acme - 11.222.333/0001-81')
    expect(s.labelOf!({ id: 'p1' })).toBeUndefined()
    expect(fkSource('Cliente')).toBe(fkSource('Cliente'))
  })

  it('Servico: lista so os ativos e rotula pelo nome', async () => {
    apiMock.mockResolvedValue([{ id: 's1', nome: 'Consultoria' }])
    const s = fkSource('Servico')
    expect(await s.search('con')).toEqual([{ id: 's1', label: 'Consultoria' }])
    expect(apiMock).toHaveBeenCalledWith('/Servico/0/49', { query: { ativo: true, nome: '*con*', sortBy: 'nome' } })
    expect(s.labelOf!({ id: 's1', nome: 'Consultoria' })).toBe('Consultoria')
    expect(s.labelOf!({ id: 's1' })).toBeUndefined()
  })

  it('entidade sem registro usa a busca generica por nome', async () => {
    apiMock.mockResolvedValue([{ id: 'c1', nome: 'Serviços' }])
    const s = fkSource('Categoria')
    expect(s.key).toBe('Categoria')
    expect(await s.search('ser')).toEqual([{ id: 'c1', label: 'Serviços' }])
    expect(apiMock).toHaveBeenCalledWith('/Categoria', { query: { nome: '*ser*', sortBy: 'nome' } })
  })
})