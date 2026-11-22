import { describe, expect, it } from 'vitest'
import { menu, visibleMenu, type MenuItem } from '@/config/menu'
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

describe('menu de administracao da plataforma', () => {
  const admins = ['/admin/municipios', '/admin/usuarios', '/admin/consulta-cnpj', '/admin/integranotas']
  const rotas = (s: SessionData) => visibleMenu(s, menu).flatMap((g) => g.items.map((i) => i.route))

  it('itens do grupo Administracao sao platformOnly', () => {
    const itens = menu.filter((i) => admins.includes(i.route))
    expect(itens.map((i) => [i.group, i.platformOnly])).toEqual([['Administração', true], ['Administração', true], ['Administração', true], ['Administração', true]])
  })
  it('so o administrador da plataforma os ve', () => {
    expect(rotas({ ...base, papel: 'administrador' })).not.toEqual(expect.arrayContaining(admins))
    expect(rotas({ ...base, papel: null, plataformaAdmin: true })).toEqual(expect.arrayContaining(admins))
  })
})
describe('menu Configuracao > NFS-e da empresa', () => {
  const rotas = (s: SessionData) => visibleMenu(s, menu).flatMap((g) => g.items.map((i) => i.route))

  it('item no grupo Configuracao, ordem 70, papel administrador, nao platformOnly', () => {
    const item = menu.find((i) => i.route === '/configuracao/nfse')
    expect(item).toMatchObject({ title: 'NFS-e', group: 'Configuração', order: 70, role: 'administrador' })
    expect(item?.platformOnly).toBeUndefined()
  })
  it('so o administrador da empresa ve; operador, consulta e plataforma sem empresa nao', () => {
    expect(rotas({ ...base, papel: 'administrador' })).toContain('/configuracao/nfse')
    expect(rotas({ ...base, papel: 'operador' })).not.toContain('/configuracao/nfse')
    expect(rotas({ ...base, papel: 'consulta' })).not.toContain('/configuracao/nfse')
    expect(rotas({ ...base, papel: null, plataformaAdmin: true })).not.toContain('/configuracao/nfse')
  })
})
