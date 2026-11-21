import type { Papel, SessionData } from '@/lib/api'
import { hasRole } from '@/lib/auth'

/**
 * Manifesto de menu: FONTE UNICA da navegacao do shell.
 * Quando o gerador de manifesto existir, este arquivo sera substituido por `gen/screens.ts`
 * (mesma forma: name, route, title, icon, group, order, role). So o shell le este modulo.
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

export const menu: MenuItem[] = [
  { name: 'Inicio', route: '/', title: 'Início', icon: 'home', order: 0 },
  { name: 'PessoasExemplo', route: '/exemplos/pessoas', title: 'Pessoas (exemplo)', icon: 'users', group: 'Exemplos', order: 10, role: 'consulta' },
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
