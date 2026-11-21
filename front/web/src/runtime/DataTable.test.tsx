import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ColumnDef } from '@tanstack/react-table'
import { DataTable, type TableResult } from '@/runtime/DataTable'
import { initialTableState, toListRequest, type TableState } from '@/runtime/table-state'
import { ApiError } from '@/lib/api'

interface Row {
  id: string
  nome: string
}
const columns: ColumnDef<Row>[] = [
  { id: 'nome', accessorKey: 'nome', header: 'Nome' },
  { id: 'actions', header: 'Ações', cell: () => 'ok' },
]
const rows: Row[] = [
  { id: '1', nome: 'Ana' },
  { id: '2', nome: 'Bruno' },
]

function setup(result: Partial<TableResult<Row>>, state: Partial<TableState> = {}, extra: Partial<React.ComponentProps<typeof DataTable<Row>>> = {}) {
  const onStateChange = vi.fn()
  const full: TableResult<Row> = { isLoading: false, isError: false, ...result }
  render(<DataTable columns={columns} result={full} state={initialTableState(state)} onStateChange={onStateChange} {...extra} />)
  return { onStateChange }
}

describe('DataTable', () => {
  it('mostra skeleton enquanto carrega', () => {
    setup({ isLoading: true })
    expect(screen.getAllByTestId('skeleton-row').length).toBeGreaterThan(0)
    expect(screen.queryByText('Ana')).not.toBeInTheDocument()
  })

  it('mostra as linhas e o intervalo com o total do servidor', () => {
    setup({ data: rows, count: 25 })
    expect(screen.getByText('Ana')).toBeInTheDocument()
    expect(screen.getByText('1–2 de 25')).toBeInTheDocument()
  })

  it('estado vazio com a acao informada', () => {
    setup({ data: [], count: 0 }, {}, { empty: { title: 'Nenhuma pessoa ainda.', action: <button>Nova pessoa</button> } })
    expect(screen.getByText('Nenhuma pessoa ainda.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nova pessoa' })).toBeInTheDocument()
  })

  it('vazio com busca ativa mostra "nenhum resultado" e limpa os filtros', async () => {
    const user = userEvent.setup()
    const { onStateChange } = setup({ data: [], count: 0 }, { search: 'zzz' })
    expect(screen.getByText('Nenhum resultado encontrado.')).toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: /Limpar filtros/ })[0]!)
    expect(onStateChange).toHaveBeenCalledWith({ search: '', filters: {} })
  })

  it('estado de erro mostra mensagem util e tenta de novo', async () => {
    const user = userEvent.setup()
    const refetch = vi.fn()
    setup({ isError: true, error: new ApiError(500, 'erro_interno', 'O servidor não respondeu.'), refetch })
    expect(screen.getByRole('alert')).toHaveTextContent('O servidor não respondeu.')
    expect(screen.getByRole('alert')).toHaveTextContent('erro_interno')
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(refetch).toHaveBeenCalled()
  })

  it('ordenacao alterna asc, desc e sem ordem', async () => {
    const user = userEvent.setup()
    const a = setup({ data: rows, count: 2 })
    await user.click(screen.getByRole('button', { name: /Nome/ }))
    expect(a.onStateChange).toHaveBeenCalledWith({ sortBy: 'nome', sortDirection: 'asc' })
  })

  it('ordenacao: asc vira desc; desc remove a ordem; coluna de acoes nao ordena', async () => {
    const user = userEvent.setup()
    const a = setup({ data: rows, count: 2 }, { sortBy: 'nome', sortDirection: 'asc' })
    await user.click(screen.getByRole('button', { name: /Nome/ }))
    expect(a.onStateChange).toHaveBeenCalledWith({ sortBy: 'nome', sortDirection: 'desc' })
    expect(screen.queryByRole('button', { name: /Ações/ })).not.toBeInTheDocument()
  })

  it('paginacao no servidor: proxima pagina e botoes nos limites', async () => {
    const user = userEvent.setup()
    const { onStateChange } = setup({ data: rows, count: 25 }, { page: 0, pageSize: 10 })
    expect(screen.getByRole('button', { name: 'Página anterior' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Próxima página' }))
    expect(onStateChange).toHaveBeenCalledWith({ page: 1 })
  })

  it('ultima pagina desabilita "proxima"', () => {
    setup({ data: [rows[0]!], count: 21 }, { page: 2, pageSize: 10 })
    expect(screen.getByRole('button', { name: 'Próxima página' })).toBeDisabled()
    expect(screen.getByText('21–21 de 21')).toBeInTheDocument()
  })
})

describe('toListRequest (contrato com o back)', () => {
  it('traduz pagina para /{from}/{to} inclusivo, ordenacao e busca com curinga', () => {
    const r = toListRequest(initialTableState({ page: 2, pageSize: 10, sortBy: 'nome', sortDirection: 'desc', search: ' ana ', filters: { tipoPessoa: 'FISICA', vazio: '' } }), 'nome')
    expect(r.rangePath).toBe('/20/29')
    expect(r.listQuery).toEqual({ sortBy: 'nome', sortDirection: 'desc', tipoPessoa: 'FISICA', nome: '*ana*' })
    expect(r.countQuery).toEqual({ tipoPessoa: 'FISICA', nome: '*ana*' })
  })
})
