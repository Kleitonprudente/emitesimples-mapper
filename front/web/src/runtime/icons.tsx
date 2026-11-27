import { CalendarClock, Circle, CircleArrowDown, CircleArrowUp, FileText, Home, Landmark, List, Package, Palette, Pencil, Plus, Settings, ShoppingCart, Tags, Upload, User, UserCog, UserPen, Users, Wallet, type LucideIcon } from 'lucide-react'

/* Nome lucide (vindo do manifesto de menu / mapa de tela) -> componente. Lista curada: importar `icons`
 * inteiro de lucide-react poria todos os icones no bundle. Acrescente aqui conforme as telas pedirem. */
const ICONS: Record<string, LucideIcon> = {
  home: Home,
  users: Users,
  user: User,
  'user-cog': UserCog,
  upload: Upload,
  'user-pen': UserPen,
  pencil: Pencil,
  plus: Plus,
  package: Package,
  'file-text': FileText,
  settings: Settings,
  palette: Palette,
  'shopping-cart': ShoppingCart,
  // financeiro (docs/financeiro/README.md, secao 5)
  wallet: Wallet,
  tags: Tags,
  'calendar-clock': CalendarClock,
  landmark: Landmark,
  'arrow-down-circle': CircleArrowDown,
  'arrow-up-circle': CircleArrowUp,
  list: List,
}

export function resolveIcon(name?: string): LucideIcon {
  return (name && ICONS[name]) || Circle
}
