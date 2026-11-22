import { useState } from 'react'
import { Search, X } from 'lucide-react'
import { cn } from '@/lib/utils'
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput } from '@/components/ui/input-group'

/** Botao de busca em destaque (padrao B): usado pelo SearchInput e pelo campo CEP. */
export const SEARCH_BUTTON_CLASS = 'w-10 bg-brand-highlight text-brand-highlight-foreground hover:bg-brand-highlight/90'

/**
 * Campo de busca com botao embutido (usa o InputGroup).
 * - `variant="icon"`: lupa decorativa a direita; a busca dispara com Enter.
 * - `variant="button"`: botao de busca colado a direita, na cor de destaque da marca (`brand-highlight`; clique ou Enter).
 * `onSearch` recebe o texto confirmado. Com `clearable`, aparece um X para limpar (dispara `onSearch('')`).
 */
export function SearchInput({
  value: controlled,
  onSearch,
  placeholder = 'Buscar…',
  variant = 'button',
  clearable = true,
  label = 'Buscar',
  className,
}: {
  value?: string
  onSearch: (term: string) => void
  placeholder?: string
  variant?: 'icon' | 'button'
  clearable?: boolean
  label?: string
  className?: string
}) {
  const [text, setText] = useState(controlled ?? '')
  const [seen, setSeen] = useState(controlled)
  // Valor controlado que muda por fora (ex.: "Limpar filtros"): sincroniza durante a renderizacao, sem efeito.
  if (controlled !== undefined && controlled !== seen) {
    setSeen(controlled)
    setText(controlled)
  }
  const limpar = () => {
    setText('')
    onSearch('')
  }
  return (
    <form
      role="search"
      className={cn('w-full sm:max-w-xs', className)}
      onSubmit={(e) => {
        e.preventDefault()
        onSearch(text.trim())
      }}
    >
      <InputGroup>
        <InputGroupInput type="search" aria-label={label} placeholder={placeholder} value={text} onChange={(e) => setText(e.target.value)} />
        {clearable && text && (
          <InputGroupButton type="button" size="icon" aria-label="Limpar busca" className="w-8 text-muted-foreground" onClick={limpar}>
            <X />
          </InputGroupButton>
        )}
        {variant === 'icon' ? (
          <InputGroupAddon>
            <Search aria-hidden />
          </InputGroupAddon>
        ) : (
          <InputGroupButton type="submit" variant="default" aria-label={label} className={SEARCH_BUTTON_CLASS}>
            <Search />
          </InputGroupButton>
        )}
      </InputGroup>
    </form>
  )
}
