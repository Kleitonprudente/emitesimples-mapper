import * as React from 'react'
import { Loader2Icon } from 'lucide-react'
import { cn } from '@/lib/utils'

export function Spinner({ className, ...props }: React.ComponentProps<'svg'>) {
  return <Loader2Icon role="status" aria-label="Carregando" className={cn('size-4 animate-spin', className)} {...props} />
}
