import { createFileRoute } from '@tanstack/react-router'
import { NfseConfig } from '@/features/empresa-config/NfseConfig'

export const Route = createFileRoute('/_authenticated/configuracao/nfse')({
  component: NfseConfig,
})
