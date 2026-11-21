import { describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { ApiError } from '@/lib/api'

const schema = z.object({
  nome: z.string().min(1, 'Informe o nome.'),
  documento: z.string().refine((s) => s.length === 11, 'CPF deve ter 11 dígitos.'),
  tipo: z.enum(['A', 'B']),
  cidade: z.object({ id: z.string() }).optional(),
})
type V = z.infer<typeof schema>

const FIELDS: FieldLayout = [
  { name: 'nome', label: 'Nome', required: true },
  [
    { name: 'documento', label: 'CPF', mask: 'cpf', required: true },
    { name: 'tipo', label: 'Tipo', options: [{ value: 'A', label: 'Opção A' }, { value: 'B', label: 'Opção B' }] },
  ],
]

function wrap(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>)
}

describe('AutoForm', () => {
  it('mostra erro por campo (vazio = obrigatorio; regra do schema quando preenchido) e nao envia', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    wrap(<AutoForm schema={schema} fields={FIELDS} defaults={{ nome: '', documento: '', tipo: 'A' } as V} onSubmit={onSubmit} />)
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findAllByText('Campo obrigatório')).toHaveLength(2) // nome e CPF vazios
    expect(screen.getByLabelText(/Nome/)).toHaveAttribute('aria-invalid', 'true')
    await user.type(screen.getByLabelText(/CPF/), '123')
    expect(await screen.findByText('CPF deve ter 11 dígitos.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('campo obrigatorio ausente mostra "Campo obrigatório" (nunca texto tecnico do zod)', async () => {
    const s = z.object({ nome: z.string() })
    const user = userEvent.setup()
    wrap(<AutoForm schema={s} fields={[{ name: 'nome', label: 'Nome', required: true }]} defaults={{ nome: '' }} onSubmit={vi.fn()} />)
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('Campo obrigatório')).toBeInTheDocument()
  })

  it('create: mascara so na apresentacao, o valor enviado e cru (digitos)', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    wrap(<AutoForm schema={schema} fields={FIELDS} defaults={{ nome: '', documento: '', tipo: 'A' } as V} onSubmit={onSubmit} />)
    await user.type(screen.getByLabelText(/Nome/), 'Ana Paula')
    const cpf = screen.getByLabelText(/CPF/)
    await user.type(cpf, '12345678909')
    expect(cpf).toHaveValue('123.456.789-09')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0]).toMatchObject({ nome: 'Ana Paula', documento: '12345678909', tipo: 'A' })
  })

  it('edit: preenche com values e usa o rotulo de edicao', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    wrap(
      <AutoForm
        schema={schema}
        fields={FIELDS}
        defaults={{ nome: '', documento: '', tipo: 'A' } as V}
        values={{ nome: 'Carlos', documento: '98765432100', tipo: 'B' }}
        onSubmit={onSubmit}
      />,
    )
    expect(screen.getByLabelText(/Nome/)).toHaveValue('Carlos')
    expect(screen.getByLabelText(/CPF/)).toHaveValue('987.654.321-00')
    await user.clear(screen.getByLabelText(/Nome/))
    await user.type(screen.getByLabelText(/Nome/), 'Carlos Eduardo')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0]).toMatchObject({ nome: 'Carlos Eduardo', documento: '98765432100', tipo: 'B' })
  })

  it('FK: envia SO {id}, mesmo que o valor carregado tenha o objeto completo', async () => {
    const onSubmit = vi.fn()
    const user = userEvent.setup()
    const fk = { key: 'M', search: async () => [], labelOf: (v: Record<string, unknown>) => String(v.nome) }
    wrap(
      <AutoForm
        schema={schema}
        fields={[{ name: 'nome', label: 'Nome' }, { name: 'documento', label: 'CPF', mask: 'cpf' }, { name: 'cidade', label: 'Cidade', fk }]}
        defaults={{ nome: '', documento: '', tipo: 'A' } as V}
        values={{ nome: 'X', documento: '12345678909', tipo: 'A', cidade: { id: 'mun-1', nome: 'Curitiba', uf: 'PR' } as never }}
        onSubmit={onSubmit}
      />,
    )
    expect(screen.getByRole('combobox')).toHaveTextContent('Curitiba')
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0].cidade).toEqual({ id: 'mun-1' })
  })

  it('exibe o erro do servidor (mensagem e codigo) sem texto tecnico', () => {
    wrap(
      <AutoForm
        schema={schema}
        fields={FIELDS}
        defaults={{ nome: '', documento: '', tipo: 'A' } as V}
        onSubmit={vi.fn()}
        serverError={new ApiError(409, 'documento_duplicado', 'Já existe uma pessoa com este documento.')}
      />,
    )
    expect(screen.getByRole('alert')).toHaveTextContent('Já existe uma pessoa com este documento.')
    expect(screen.getByRole('alert')).toHaveTextContent('documento_duplicado')
  })
})
