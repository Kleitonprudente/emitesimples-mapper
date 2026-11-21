import { describe, expect, it } from 'vitest'
import { visibleMenu, type MenuItem } from '@/config/menu'
import type { SessionData } from '@/lib/api'

const base: SessionData = { usuario: { id: '1', nome: 'A', email: 'a@x.com' }, empresa: null, papel: 'consulta', plataformaAdmin: false }
const items: MenuItem[] = [
  { name: 'Inicio', route: '/', title: 'Início', order: 0 },
  { name: 'Notas', route: '/notas', title: 'Notas', group: 'Fiscal', order: 20, role: 'operador' },
  { name: 'Config', route: '/config', title: 'Config', group: 'Fiscal', order: 10, role: 'administrador' },
  { name: 'Painel', route: '/admin', title: 'Painel', group: 'Plataforma', platformOnly: true },
]
const names = (s: SessionData | null) => visibleMenu(s, items).flatMap((g) => g.items.map((i) => i.name))

describe('menu por papel (so UX)', () => {
  it('sem sessao nao mostra nada', () => expect(names(null)).toEqual([]))
  it('consulta ve so o que nao exige papel', () => expect(names(base)).toEqual(['Inicio']))
  it('operador ve operador e abaixo, ordenado', () => expect(names({ ...base, papel: 'operador' })).toEqual(['Inicio', 'Notas']))
  it('administrador ve tudo da empresa, em ordem', () => expect(names({ ...base, papel: 'administrador' })).toEqual(['Inicio', 'Config', 'Notas']))
  it('plataformaAdmin e flag separada do papel', () => {
    expect(names({ ...base, papel: null, plataformaAdmin: true })).toEqual(['Inicio', 'Painel'])
  })
})
