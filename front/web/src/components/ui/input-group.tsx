import * as React from 'react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'

/**
 * Grupo de input (estilo shadcn): um contorno unico em volta de campo + adornos (icone, botao).
 * O foco e do grupo (focus-within), nao do campo interno.
 */
export function InputGroup({ className, ...props }: React.ComponentProps<'div'>) {
  return (
    <div
      role="group"
      data-slot="input-group"
      className={cn(
        'flex h-9 w-full min-w-0 items-center overflow-hidden rounded-md border border-input bg-transparent shadow-xs transition-colors',
        'focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50',
        'has-[[aria-invalid=true]]:border-destructive has-[[aria-invalid=true]]:ring-destructive/20',
        className,
      )}
      {...props}
    />
  )
}

/** Campo sem contorno proprio, para dentro do InputGroup. */
export function InputGroupInput({ className, type, ...props }: React.ComponentProps<'input'>) {
  return (
    <input
      type={type}
      data-slot="input-group-control"
      className={cn(
        'h-full min-w-0 flex-1 bg-transparent px-3 py-1 text-sm outline-none placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50',
        className,
      )}
      {...props}
    />
  )
}

/** Adorno (icone ou texto) antes ou depois do campo. */
export function InputGroupAddon({ className, align = 'end', ...props }: React.ComponentProps<'div'> & { align?: 'start' | 'end' }) {
  return (
    <div
      data-slot="input-group-addon"
      data-align={align}
      className={cn('flex h-full shrink-0 items-center gap-1 text-muted-foreground [&_svg]:size-4', align === 'start' ? 'pl-3' : 'pr-3', className)}
      {...props}
    />
  )
}

/** Botao embutido no grupo: ocupa a altura toda e cola na borda (sem raio proprio). */
export function InputGroupButton({ className, variant = 'ghost', size = 'icon', ...props }: React.ComponentProps<typeof Button>) {
  return <Button data-slot="input-group-button" variant={variant} size={size} className={cn('h-full rounded-none', className)} {...props} />
}
