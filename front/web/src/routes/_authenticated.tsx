import { createFileRoute, Outlet, redirect } from '@tanstack/react-router'
import { AppShell } from '@/components/AppShell'
import { restoreSession } from '@/lib/auth'

export const Route = createFileRoute('/_authenticated')({
  // Valida a sessao ANTES de montar qualquer tela protegida. Sem sessao -> /login?redirect=
  beforeLoad: async ({ location }) => {
    const session = await restoreSession()
    if (!session) {
      throw redirect({ to: '/login', search: { redirect: location.href } })
    }
  },
  component: () => (
    <AppShell>
      <Outlet />
    </AppShell>
  ),
})
