import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { SearchInput } from './SearchInput'

describe('SearchInput', () => {
  it('variante botao: clique e Enter confirmam o texto; X limpa e confirma vazio', async () => {
    const onSearch = vi.fn()
    const u = userEvent.setup()
    render(<SearchInput variant="button" onSearch={onSearch} />)
    await u.type(screen.getByRole('searchbox', { name: 'Buscar' }), 'acme')
    expect(onSearch).not.toHaveBeenCalled()
    await u.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(onSearch).toHaveBeenLastCalledWith('acme')
    await u.type(screen.getByRole('searchbox'), '{Enter}')
    expect(onSearch).toHaveBeenCalledTimes(2)
    await u.click(screen.getByRole('button', { name: 'Limpar busca' }))
    expect(onSearch).toHaveBeenLastCalledWith('')
    expect(screen.getByRole('searchbox')).toHaveValue('')
  })

  it('variante icone: so Enter busca e nao ha botao de busca', async () => {
    const onSearch = vi.fn()
    const u = userEvent.setup()
    render(<SearchInput variant="icon" onSearch={onSearch} clearable={false} />)
    expect(screen.queryByRole('button')).toBeNull()
    await u.type(screen.getByRole('searchbox'), 'x{Enter}')
    expect(onSearch).toHaveBeenCalledWith('x')
  })
})
