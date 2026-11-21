import { useEffect, useId, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Check, ChevronsUpDown, Loader2, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

export interface FkOption {
  id: string
  label: string
}

/** Fonte de uma chave estrangeira: busca assincrona + como rotular um valor ja carregado. */
export interface FkSource {
  /** Identifica a entidade no cache do Query. */
  key: string
  search: (term: string) => Promise<FkOption[]>
  /** Rotulo de um valor que veio da API (objeto completo ou so {id}). */
  labelOf?: (value: Record<string, unknown>) => string | undefined
}

export interface FkValue {
  id: string
  [k: string]: unknown
}

function useDebounced<T>(value: T, ms: number): T {
  const [v, setV] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setV(value), ms)
    return () => clearTimeout(t)
  }, [value, ms])
  return v
}

/** Combobox com busca assincrona. O valor do formulario e {id, ...}; o AutoForm envia SO {id}. */
export function Combobox({
  id,
  source,
  value,
  onChange,
  placeholder = 'Selecione…',
  disabled,
  invalid,
}: {
  id?: string
  source: FkSource
  value: FkValue | null | undefined
  onChange: (v: FkValue | null) => void
  placeholder?: string
  disabled?: boolean
  invalid?: boolean
}) {
  const [open, setOpen] = useState(false)
  const [term, setTerm] = useState('')
  const [active, setActive] = useState(0)
  const [pickedLabel, setPickedLabel] = useState<string | null>(null)
  const debounced = useDebounced(term, 300)
  const listId = useId()

  const query = useQuery({
    queryKey: ['fk', source.key, debounced],
    queryFn: () => source.search(debounced),
    enabled: open,
  })
  const options = query.data ?? []

  const label = value ? (pickedLabel ?? source.labelOf?.(value) ?? value.id) : null

  function pick(o: FkOption) {
    setPickedLabel(o.label)
    onChange({ id: o.id })
    setOpen(false)
    setTerm('')
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <div className="flex gap-1">
        <PopoverTrigger asChild>
          <Button
            id={id}
            type="button"
            variant="outline"
            role="combobox"
            aria-expanded={open}
            aria-controls={listId}
            aria-invalid={invalid || undefined}
            disabled={disabled}
            className={cn('w-full justify-between font-normal aria-invalid:border-destructive', !label && 'text-muted-foreground')}
          >
            <span className="truncate">{label ?? placeholder}</span>
            <ChevronsUpDown className="size-4 opacity-50" aria-hidden />
          </Button>
        </PopoverTrigger>
        {value && !disabled && (
          <Button type="button" variant="ghost" size="icon" aria-label="Limpar seleção" onClick={() => { setPickedLabel(null); onChange(null) }}>
            <X className="size-4" />
          </Button>
        )}
      </div>
      <PopoverContent className="w-[var(--radix-popover-trigger-width)] p-2">
        <Input
          autoFocus
          value={term}
          placeholder="Buscar…"
          aria-label="Buscar"
          onChange={(e) => {
            setTerm(e.target.value)
            setActive(0)
          }}
          onKeyDown={(e) => {
            if (e.key === 'ArrowDown') {
              e.preventDefault()
              setActive((a) => Math.min(a + 1, options.length - 1))
            } else if (e.key === 'ArrowUp') {
              e.preventDefault()
              setActive((a) => Math.max(a - 1, 0))
            } else if (e.key === 'Enter') {
              e.preventDefault()
              const o = options[active]
              if (o) pick(o)
            }
          }}
        />
        <ul id={listId} role="listbox" className="mt-2 max-h-56 overflow-y-auto">
          {query.isFetching && (
            <li className="flex items-center gap-2 px-2 py-2 text-sm text-muted-foreground">
              <Loader2 className="size-4 animate-spin" aria-hidden /> Buscando…
            </li>
          )}
          {!query.isFetching && query.isError && <li className="px-2 py-2 text-sm text-destructive">Não foi possível buscar.</li>}
          {!query.isFetching && !query.isError && options.length === 0 && (
            <li className="px-2 py-2 text-sm text-muted-foreground">Nenhum resultado.</li>
          )}
          {!query.isFetching &&
            options.map((o, i) => (
              <li
                key={o.id}
                role="option"
                aria-selected={value?.id === o.id}
                onMouseEnter={() => setActive(i)}
                onClick={() => pick(o)}
                className={cn('flex cursor-pointer items-center justify-between rounded-sm px-2 py-1.5 text-sm', i === active && 'bg-accent text-accent-foreground')}
              >
                {o.label}
                {value?.id === o.id && <Check className="size-4" aria-hidden />}
              </li>
            ))}
        </ul>
      </PopoverContent>
    </Popover>
  )
}
