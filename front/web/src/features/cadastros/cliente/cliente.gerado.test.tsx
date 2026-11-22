import { describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { getCoreRowModel, useReactTable, flexRender } from '@tanstack/react-table'
import { FormularioClienteForm } from './FormularioCliente'
import { gestaoClientesColumns } from './columns'
import { screens } from '@/gen/screens'
import type { PessoaRow } from '@/gen/api/pessoa'

const wrap = (ui: React.ReactElement) =>
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{ui}</QueryClientProvider>)

describe('telas geradas de Cliente (fumaca de renderizacao)', () => {
  it('formulario unico mostra os campos do mapa e barra o envio vazio', async () => {
    const onSubmit = vi.fn()
    wrap(<FormularioClienteForm onSubmit={onSubmit} />)
    for (const rotulo of [/Nome \/ Razão Social/, /CNPJ \/ CPF/, /^CEP/, /Logradouro/, /Bairro/, /^Município$/]) {
      expect(screen.getByLabelText(rotulo)).toBeInTheDocument()
    }
    await userEvent.setup().click(screen.getByRole('button', { name: 'Salvar' }))
    expect((await screen.findAllByText('Campo obrigatório')).length).toBeGreaterThan(0)
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('colunas renderizam o rotulo longo do enum e o botao Editar so com callback', () => {
    const row = { id: 'p1', tipoPessoa: 'juridica', nome: 'ACME', documento: '11222333000181' } as PessoaRow
    function Tabela({ editar }: { editar?: (r: PessoaRow) => void }) {
      const t = useReactTable({ data: [row], columns: gestaoClientesColumns({ editarCliente: editar }), getCoreRowModel: getCoreRowModel() })
      return (
        <table>
          <tbody>
            {t.getRowModel().rows.map((r) => (
              <tr key={r.id}>{r.getVisibleCells().map((c) => <td key={c.id}>{flexRender(c.column.columnDef.cell, c.getContext())}</td>)}</tr>
            ))}
          </tbody>
        </table>
      )
    }
    const { rerender } = render(<Tabela />)
    expect(screen.getByText('Pessoa jurídica (documento CNPJ, 14 dígitos)')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar' })).toBeNull()
    const editar = vi.fn()
    rerender(<Tabela editar={editar} />)
    within(screen.getByRole('table')).getByRole('button', { name: 'Editar' }).click()
    expect(editar).toHaveBeenCalledWith(row)
  })

  it('manifesto traz as telas do mapa e so as de rota sem $ viram menu', () => {
    expect(screens.map((s) => s.name)).toEqual(['GestaoClientes', 'FormularioCliente', 'GestaoFornecedores', 'FormularioFornecedor', 'GestaoMunicipios', 'FormularioMunicipio', 'GestaoEmpresas', 'FormularioEmpresa'])
  })
})
