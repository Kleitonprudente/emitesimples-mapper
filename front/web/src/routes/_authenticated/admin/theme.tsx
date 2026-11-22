import { createFileRoute } from '@tanstack/react-router'
import { GuiaDeEstilo } from '@/features/admin/GuiaDeEstilo'
import { PlatformOnly } from '@/features/admin/PlatformOnly'

export const Route = createFileRoute('/_authenticated/admin/theme')({
  component: () => (
    <PlatformOnly>
      <GuiaDeEstilo />
    </PlatformOnly>
  ),
})
