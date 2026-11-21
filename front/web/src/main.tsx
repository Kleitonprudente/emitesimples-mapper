import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { createRouter, RouterProvider } from '@tanstack/react-router'
import '@/index.css'
import { configureApi } from '@/lib/api'
import { queryClient } from '@/lib/query-client'
import '@/lib/auth'
import { routeTree } from '@/routeTree.gen'

const router = createRouter({
  routeTree,
  context: { queryClient },
  defaultPreload: 'intent',
})

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}

// Sessao expirada no meio do uso: lib/api.ts limpa o estado e chama isto.
configureApi({
  onUnauthorized: (redirect) => {
    void router.navigate({ to: '/login', search: { redirect: redirect.startsWith('/login') ? undefined : redirect } })
  },
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
