import { ENV_BADGE } from '@/config/env'
import { Badge } from '@/components/ui/badge'

/** Selo de ambiente (MOCK/DEV/STAGING). Nao aparece em producao. */
export function EnvBadge({ className }: { className?: string }) {
  if (!ENV_BADGE) return null
  return (
    <Badge tone="warning" className={className} title="Ambiente de não produção">
      {ENV_BADGE}
    </Badge>
  )
}
