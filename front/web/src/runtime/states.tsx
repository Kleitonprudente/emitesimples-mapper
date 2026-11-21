import type { ReactNode } from 'react'
import { AlertTriangle, Inbox, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ApiError } from '@/lib/api'
import { cn } from '@/lib/utils'
import { errorMessage } from '@/runtime/feedback'

export function EmptyState({
  title = 'Nada por aqui ainda.',
  description,
  action,
  icon,
  className,
}: {
  title?: string
  description?: string
  action?: ReactNode
  icon?: ReactNode
  className?: string
}) {
  return (
    <div role="status" className={cn('flex flex-col items-center justify-center gap-3 px-4 py-12 text-center', className)}>
      <div className="text-muted-foreground">{icon ?? <Inbox className="size-10" aria-hidden />}</div>
      <p className="font-medium">{title}</p>
      {description && <p className="max-w-sm text-sm text-muted-foreground">{description}</p>}
      {action}
    </div>
  )
}

export function ErrorState({
  error,
  title = 'Não foi possível carregar os dados.',
  onRetry,
  className,
}: {
  error?: unknown
  title?: string
  onRetry?: () => void
  className?: string
}) {
  return (
    <div role="alert" className={cn('flex flex-col items-center justify-center gap-3 px-4 py-12 text-center', className)}>
      <AlertTriangle className="size-10 text-destructive" aria-hidden />
      <p className="font-medium">{title}</p>
      {error !== undefined && <p className="max-w-sm text-sm text-muted-foreground">{errorMessage(error)}</p>}
      {error instanceof ApiError && error.status !== 0 && (
        <p className="text-xs text-muted-foreground">Código: {error.codigo}</p>
      )}
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  )
}

export function LoadingState({ label = 'Carregando…', className }: { label?: string; className?: string }) {
  return (
    <div role="status" aria-live="polite" className={cn('flex items-center justify-center gap-2 px-4 py-12 text-sm text-muted-foreground', className)}>
      <Loader2 className="size-4 animate-spin" aria-hidden />
      {label}
    </div>
  )
}
