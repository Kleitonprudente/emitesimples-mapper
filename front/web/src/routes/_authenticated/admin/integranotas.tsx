import { createFileRoute } from '@tanstack/react-router'
import { IntegraNotasAdmin } from '@/features/admin/IntegraNotasAdmin'
import { PlatformOnly } from '@/features/admin/PlatformOnly'

export const Route = createFileRoute('/_authenticated/admin/integranotas')({
  component: () => (
    <PlatformOnly>
      <IntegraNotasAdmin />
    </PlatformOnly>
  ),
})
