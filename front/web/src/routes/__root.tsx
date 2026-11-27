import { createRootRouteWithContext, Link, Outlet } from '@tanstack/react-router'
import type { QueryClient } from '@tanstack/react-query'
import { Toaster } from '@/components/ui/sonner'
import { Button } from '@/components/ui/button'
import { EmptyState } from '@/runtime/states'
import { SenhaRecente } from '@/runtime/SenhaRecente'

export interface RouterContext {
  queryClient: QueryClient
}

export const Route = createRootRouteWithContext<RouterContext>()({
  component: () => (
    <>
      <Outlet />
      <Toaster position="top-right" richColors closeButton />
      <SenhaRecente />
    </>
  ),
  notFoundComponent: () => (
    <EmptyState
      className="min-h-screen"
      title="Página não encontrada."
      description="O endereço não existe ou foi movido."
      action={
        <Button asChild variant="outline">
          <Link to="/">Ir para o início</Link>
        </Button>
      }
    />
  ),
})
