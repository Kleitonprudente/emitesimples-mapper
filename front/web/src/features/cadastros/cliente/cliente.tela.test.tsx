import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { FormularioClienteScreen } from './FormularioClienteScreen'

/**
 * Tela de edicao/criacao de Cliente INTEIRA contra um `api` que se comporta como o back real:
 * PUT/POST de Pessoa; PUT de Pessoa responde 204 (sem corpo, `undefined`); contatos pelo endpoint do pai.
 */
const apiMock = vi.hoisted(() => vi.fn())
const navigateMock = vi.hoisted(() => vi.fn())
const toastMock = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@tanstack/react-router', () => ({ useNavigate: () => navigateMock }))
vi.mock('sonner', () => ({ toast: toastMock, Toaster: () => null }))

const PESSOA = {
  id: 'p1', version: '2026-11-22T10:00:00', tipoPessoa: 'JURIDICA', documento: '11444777000161', nome: 'ACME LTDA', consumidorFinal: false,
  cliente: true, fornecedor: true, logradouro: 'Rua A', numeroEndereco: '10', bairro: 'Centro', referenciaQualquer: 'preservar-me',
  dataCriacao: '2026-11-01T10:00:00', dataAtualizacao: '2026-11-02T11:00:00',
}
const CONTATOS = [{ id: 'c1', tipoContato: 'FINANCEIRO', nome: 'Ana Souza', whatsapp: '44999990000', email: 'ana@acme.test' }]

function prepara(opts: { contatosPut?: () => Promise<unknown> } = {}) {
  apiMock.mockImplementation(async (path: string, o?: { method?: string; body?: unknown }) => {
    const method = o?.method ?? 'GET'
    if (path === '/Pessoa/id/p1' && method === 'GET') return PESSOA
    if (path === '/Pessoa/p1/contatos' && method === 'GET') return CONTATOS
    if (path === '/Pessoa/id/p1' && method === 'PUT') return undefined // 204 do back real
    if (path === '/Pessoa/p1/contatos' && method === 'PUT') return opts.contatosPut ? opts.contatosPut() : o?.body
    if (path === '/Pessoa' && method === 'POST') return { ...PESSOA, id: 'novo-1' }
    if (path === '/Pessoa/novo-1/contatos' && method === 'PUT') return opts.contatosPut ? opts.contatosPut() : o?.body
    throw new Error(`chamada inesperada: ${method} ${path}`)
  })
}
const chamadas = (method: string, path: string) => apiMock.mock.calls.filter(([p, o]) => p === path && (o?.method ?? 'GET') === method)

function renderTela(id: string) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <FormularioClienteScreen id={id} />
    </QueryClientProvider>,
  )
}

describe('tela de Cliente: salvar pessoa + contatos', () => {
  beforeEach(() => {
    apiMock.mockReset()
    navigateMock.mockReset()
    toastMock.success.mockReset()
    toastMock.error.mockReset()
  })

  it('EDICAO: o PUT da Pessoa responde 204 (sem corpo) e mesmo assim salva os contatos e volta a lista', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela('p1')
    await screen.findByDisplayValue('ACME LTDA')
    expect(screen.getByDisplayValue('Ana Souza')).toBeInTheDocument() // contatos carregados do back
    const nome = screen.getByLabelText(/Nome \/ Razão Social/)
    await user.clear(nome)
    await user.type(nome, 'ACME NOVA LTDA')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(chamadas('PUT', '/Pessoa/p1/contatos')).toHaveLength(1))
    const pessoaPut = chamadas('PUT', '/Pessoa/id/p1')[0]![1].body as Record<string, unknown>
    expect(pessoaPut.nome).toBe('ACME NOVA LTDA')
    expect(pessoaPut.referenciaQualquer).toBe('preservar-me') // o que o formulario nao mostra segue como veio
    expect(pessoaPut.cliente).toBe(true)
    expect(pessoaPut.fornecedor).toBe(true) // edicao nao mexe nos papeis
    expect(pessoaPut).not.toHaveProperty('contatos') // a lista vai ao endpoint do pai, nao no corpo da Pessoa
    const itens = (chamadas('PUT', '/Pessoa/p1/contatos')[0]![1].body as { id: string; nome: string }[])
    expect(itens).toEqual([expect.objectContaining({ id: 'c1', nome: 'Ana Souza', tipoContato: 'FINANCEIRO' })])
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith({ to: '/clientes' }))
    expect(toastMock.error).not.toHaveBeenCalled()
  })

  it('EDICAO: falha nos contatos mantem na pagina e avisa que a pessoa foi salva', async () => {
    prepara({ contatosPut: async () => { throw new ApiError(400, 'requisicao_invalida', 'contatos[0].email: e-mail inválido') } })
    const user = userEvent.setup()
    renderTela('p1')
    await screen.findByDisplayValue('ACME LTDA')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(toastMock.error).toHaveBeenCalled())
    expect(String(toastMock.error.mock.calls[0]![0])).toMatch(/Cliente salvo, mas os contatos não foram salvos/)
    expect(chamadas('PUT', '/Pessoa/id/p1')).toHaveLength(1)
    expect(navigateMock).not.toHaveBeenCalledWith({ to: '/clientes' })
  })

  it('CRIACAO: cria a Pessoa com cliente=true, salva os contatos no id criado e volta a lista', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela('novo')
    // CPF valido: ao sair do campo a regra do documento define pessoa fisica e consumidor final (sem chamar a API)
    await user.type(await screen.findByLabelText(/CNPJ \/ CPF/), '52998224725')
    await user.tab()
    await user.type(screen.getByLabelText(/Nome \/ Razão Social/), 'NOVA LTDA')
    await user.type(screen.getByLabelText(/^Logradouro/), 'Rua B')
    await user.type(screen.getByLabelText(/^Número do Endereço/), '5')
    await user.type(screen.getByLabelText(/^Bairro/), 'Centro')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(chamadas('POST', '/Pessoa')).toHaveLength(1))
    expect(chamadas('POST', '/Pessoa')[0]![1].body).toMatchObject({ nome: 'NOVA LTDA', cliente: true })
    await waitFor(() => expect(chamadas('PUT', '/Pessoa/novo-1/contatos')).toHaveLength(1))
    expect(chamadas('PUT', '/Pessoa/novo-1/contatos')[0]![1].body).toEqual([])
    await waitFor(() => expect(navigateMock).toHaveBeenCalledWith({ to: '/clientes' }))
  })
})
