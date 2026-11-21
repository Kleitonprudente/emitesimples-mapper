import { useState, type ReactNode } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Building2, ChevronDown, LogOut, Menu, User, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'
import { EnvBadge } from '@/components/EnvBadge'
import { ThemeToggle } from '@/components/ThemeToggle'
import { visibleMenu } from '@/config/menu'
import { PAPEL_LABEL, logout, useSession } from '@/lib/auth'
import { cn } from '@/lib/utils'
import { resolveIcon } from '@/runtime/icons'

function Nav({ onNavigate }: { onNavigate?: () => void }) {
  const session = useSession()
  const groups = visibleMenu(session)
  return (
    <nav aria-label="Menu principal" className="flex flex-col gap-4 p-3">
      {groups.map((g) => (
        <div key={g.label ?? '_'} className="flex flex-col gap-1">
          {g.label && <p className="px-3 pb-1 text-xs font-medium tracking-wide text-muted-foreground uppercase">{g.label}</p>}
          {g.items.map((item) => {
            const Icon = resolveIcon(item.icon)
            return (
              <Link
                key={item.name}
                to={item.route as never}
                onClick={onNavigate}
                activeOptions={{ exact: item.route === '/' }}
                className={cn('flex items-center gap-2 rounded-md px-3 py-2 text-sm text-sidebar-foreground transition-colors outline-none hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus-visible:ring-[3px] focus-visible:ring-ring/50')}
                activeProps={{ className: 'bg-sidebar-accent font-medium text-sidebar-accent-foreground', 'aria-current': 'page' }}
              >
                <Icon className="size-4" aria-hidden />
                {item.title}
              </Link>
            )
          })}
        </div>
      ))}
    </nav>
  )
}

export function AppShell({ children }: { children: ReactNode }) {
  const session = useSession()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  async function sair() {
    await logout()
    await navigate({ to: '/login', search: { redirect: undefined } })
  }

  const empresaNome = session?.empresa ? (session.empresa.nomeFantasia ?? session.empresa.razaoSocial) : null

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-60 shrink-0 border-r border-sidebar-border bg-sidebar md:block">
        <div className="flex h-14 items-center border-b border-sidebar-border px-5 font-semibold">Emite Simples</div>
        <Nav />
      </aside>

      {open && (
        <div className="fixed inset-0 z-40 md:hidden">
          <button type="button" aria-label="Fechar menu" className="absolute inset-0 bg-foreground/50" onClick={() => setOpen(false)} />
          <aside className="relative h-full w-64 border-r border-sidebar-border bg-sidebar">
            <div className="flex h-14 items-center justify-between border-b border-sidebar-border px-5 font-semibold">
              Emite Simples
              <Button variant="ghost" size="icon" aria-label="Fechar menu" onClick={() => setOpen(false)}>
                <X />
              </Button>
            </div>
            <Nav onNavigate={() => setOpen(false)} />
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center gap-2 border-b bg-card px-4">
          <Button variant="ghost" size="icon" className="md:hidden" aria-label="Abrir menu" onClick={() => setOpen(true)}>
            <Menu />
          </Button>
          <EnvBadge />
          {empresaNome && (
            <span className="flex min-w-0 items-center gap-1.5 text-sm text-muted-foreground">
              <Building2 className="size-4 shrink-0" aria-hidden />
              <span className="truncate">{empresaNome}</span>
            </span>
          )}
          <div className="ml-auto flex items-center gap-1">
            <ThemeToggle />
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="gap-2">
                  <User aria-hidden />
                  <span className="hidden max-w-32 truncate sm:inline">{session?.usuario.nome}</span>
                  <ChevronDown className="size-4 opacity-60" aria-hidden />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>
                  <span className="block">{session?.usuario.nome}</span>
                  <span className="block text-xs font-normal text-muted-foreground">
                    {session?.papel ? PAPEL_LABEL[session.papel] : session?.plataformaAdmin ? 'Administrador da plataforma' : ''}
                  </span>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem onSelect={() => navigate({ to: '/perfil' })}>
                  <User /> Meu perfil
                </DropdownMenuItem>
                <DropdownMenuItem onSelect={sair}>
                  <LogOut /> Sair
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>
        <main className="flex-1 p-4 md:p-6">{children}</main>
      </div>
    </div>
  )
}
