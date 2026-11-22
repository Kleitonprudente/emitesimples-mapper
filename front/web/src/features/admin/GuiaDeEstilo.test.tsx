import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { GuiaDeEstilo } from './GuiaDeEstilo'

describe('GuiaDeEstilo (/admin/theme)', () => {
  it('renderiza a vitrine com as secoes de referencia', () => {
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <GuiaDeEstilo />
      </QueryClientProvider>,
    )
    expect(screen.getByRole('heading', { name: 'Guia de estilo' })).toBeInTheDocument()
    for (const titulo of ['Botões e badges', 'DataTable (estados)', 'Diálogo, menu e confirmação', 'Combobox (chave estrangeira)', /Padrão de formulário/, 'Feedback (toast)']) {
      expect(screen.getByRole('heading', { name: titulo })).toBeInTheDocument()
    }
    expect(screen.getAllByLabelText('CEP').length).toBeGreaterThanOrEqual(2)
    expect(screen.getByLabelText(/^Logradouro/)).toBeInTheDocument()
  })

  it('inclui o catalogo completo de componentes', () => {
    render(
      <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
        <GuiaDeEstilo />
      </QueryClientProvider>,
    )
    expect(screen.getByRole('heading', { name: 'Catálogo completo de componentes' })).toBeInTheDocument()
    for (const secao of ['Navegação', 'Formulário e entrada', 'Sobreposições', 'Exibição de dados', 'Feedback e estado', 'Layout']) {
      expect(screen.getByRole('heading', { name: secao })).toBeInTheDocument()
    }
    for (const nome of ['Tabs', 'Accordion', 'Calendar', 'Chart', 'AlertDialog', 'Sidebar', 'Resizable']) {
      expect(screen.getByRole('heading', { name: nome })).toBeInTheDocument()
    }
  })
})
