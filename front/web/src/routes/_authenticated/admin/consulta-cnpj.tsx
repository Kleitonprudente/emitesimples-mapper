import { createFileRoute } from '@tanstack/react-router'
import { ConsultaCnpjAdmin } from '@/features/admin/ConsultaCnpjAdmin'
import { PlatformOnly } from '@/features/admin/PlatformOnly'

export const Route = createFileRoute('/_authenticated/admin/consulta-cnpj')({
  component: () => (
    <PlatformOnly>
      <ConsultaCnpjAdmin />
    </PlatformOnly>
  ),
})
