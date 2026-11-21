import { Circle, FileText, Home, Package, Palette, Pencil, Plus, Settings, ShoppingCart, User, UserPen, Users, type LucideIcon } from 'lucide-react'

/* Nome lucide (vindo do manifesto de menu / mapa de tela) -> componente. Lista curada: importar `icons`
 * inteiro de lucide-react poria todos os icones no bundle. Acrescente aqui conforme as telas pedirem. */
const ICONS: Record<string, LucideIcon> = {
  home: Home,
  users: Users,
  user: User,
  'user-pen': UserPen,
  pencil: Pencil,
  plus: Plus,
  package: Package,
  'file-text': FileText,
  settings: Settings,
  palette: Palette,
  'shopping-cart': ShoppingCart,
}

export function resolveIcon(name?: string): LucideIcon {
  return (name && ICONS[name]) || Circle
}
