import type { Papel, SessionData } from '@/lib/api'
import { hasRole } from '@/lib/auth'
import { screens } from '@/gen/screens'

/**
 * Manifesto de menu: FONTE UNICA da navegacao do shell.
 * As telas do mapa entram por `gen/screens.ts` (gerado); aqui ficam so os itens escritos a mao.
 */
export interface MenuItem {
  name: string
  route: string
  title: string
  /** Nome lucide (ver runtime/icons.tsx). */
  icon?: string
  group?: string
  order?: number
  /** Papel MINIMO (administrador > operador > consulta). So UX: o back decide. */
  role?: Papel
  /** Visivel so para administrador da plataforma. */
  platformOnly?: boolean
}

const doMapa: MenuItem[] = screens
  .filter((s) => s.route && !s.route.includes('$'))
  .map((s) => ({ name: s.name, route: s.route as string, title: s.title, icon: s.icon, group: s.group, order: s.order, role: s.role, platformOnly: s.platformOnly }))

export const menu: MenuItem[] = [
  { name: 'Inicio', route: '/', title: 'Início', icon: 'home', order: 0 },
  ...doMapa,
  { name: 'NotasNfse', route: '/notas', title: 'NFS-e', icon: 'file-text', group: 'Notas Fiscais', order: 30, role: 'consulta' },
  { name: 'ImportarMunicipios', route: '/admin/municipios', title: 'Importar municípios', icon: 'upload', group: 'Administração', order: 80, platformOnly: true },
  { name: 'GuiaDeEstilo', route: '/admin/theme', title: 'Guia de estilo', icon: 'palette', group: 'Administração', order: 82, platformOnly: true },
  { name: 'UsuariosAdmin', route: '/admin/usuarios', title: 'Usuários', icon: 'user-cog', group: 'Administração', order: 81, platformOnly: true },
  { name: 'ConsultaCnpjAdmin', route: '/admin/consulta-cnpj', title: 'Consulta de CNPJ', icon: 'settings', group: 'Administração', order: 83, platformOnly: true },
  { name: 'IntegraNotasAdmin', route: '/admin/integranotas', title: 'Integração IntegraNotas', icon: 'settings', group: 'Administração', order: 84, platformOnly: true },
  { name: 'NfseConfig', route: '/configuracao/nfse', title: 'NFS-e', icon: 'file-text', group: 'Configuração', order: 70, role: 'administrador' },
  { name: 'EmpresaConfig', route: '/configuracao/empresa', title: 'Empresa', icon: 'settings', group: 'Configuração', order: 71, role: 'administrador' },
  { name: 'Perfil', route: '/perfil', title: 'Meu perfil', icon: 'user', group: 'Conta', order: 90 },
]

export function canSee(item: MenuItem, session: SessionData | null): boolean {
  if (!session) return false
  if (item.platformOnly) return session.plataformaAdmin
  if (!item.role) return true
  return hasRole(session, item.role)
}

export interface MenuGroup {
  label: string | null
  items: MenuItem[]
}

/** Itens visiveis, agrupados e ordenados (grupos na ordem do primeiro item). */
export function visibleMenu(session: SessionData | null, items: MenuItem[] = menu): MenuGroup[] {
  const sorted = items.filter((i) => canSee(i, session)).sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
  const groups: MenuGroup[] = []
  for (const item of sorted) {
    const label = item.group ?? null
    let g = groups.find((x) => x.label === label)
    if (!g) groups.push((g = { label, items: [] }))
    g.items.push(item)
  }
  return groups
}
