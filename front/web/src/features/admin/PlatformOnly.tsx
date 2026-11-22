import type { ReactNode } from 'react'
import { useSession } from '@/lib/auth'
import { ErrorState } from '@/runtime/states'

/** So UX: esconde as telas de admin de quem nao e administrador da plataforma (o back e quem decide, com 403). */
export function PlatformOnly({ children }: { children: ReactNode }) {
  const session = useSession()
  if (!session?.plataformaAdmin) return <ErrorState title="Acesso restrito ao administrador da plataforma." />
  return <>{children}</>
}
