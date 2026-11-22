import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { NotasLista } from '@/features/notas/NotasLista'

const apiMock = vi.hoisted(() => vi.fn())
const pdfMock = vi.hoisted(() => vi.fn())
const navegar = vi.hoisted(() => vi.fn())
const sessao = vi.hoisted(() => ({ atual: null as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock, abrirPdf: pdfMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))
vi.mock('@tanstack/react-router', async (orig) => ({ ...(await orig<typeof import('@tanstack/react-router')>()), useNavigate: () => navegar }))
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }))

const nota = (o: object = {}) => ({
  id: 'n1', numero: 100, serie: '1', estado: 'autorizada', dataEmissao: '2026-11-10T14:30:00', valorServicos: 1500.5,
  pessoa: { id: 'p1', nome: 'Ana Paula', documento: '12345678909' }, servico: { id: 's1', nome: 'Consultoria' },
  chave: null, numeroNfse: null, codigoRetorno: null, mensagemRetorno: null, temPdf: true, dataCriacao: '2026-11-10T14:00:00', ...o,
})

let notas: unknown[] = []
function prepara(lista: unknown[]) {
  notas = lista
  apiMock.mockImplementation(async (path: string) => (path === '/nfse/notas/count' ? { count: notas.length } : notas))
}
const listaChamadas = () => apiMock.mock.calls.filter(([p]) => /^\/nfse\/notas\/\d+\/\d+$/.test(p))

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <NotasLista />
    </QueryClientProvider>,
  )
}
const com = (papel: string) => ({ usuario: { id: 'u', nome: 'Ana', email: 'a@x.com' }, empresa: { id: 'e', razaoSocial: 'H' }, papel, plataformaAdmin: false })

beforeEach(() => {
  apiMock.mockReset()
  pdfMock.mockReset()
  navegar.mockReset()
  sessao.atual = com('operador')
})
afterEach(() => vi.useRealTimers())

describe('NotasLista', () => {
  it('mostra as colunas: numero serie-numero, cliente com documento mascarado, servico, valor, emissao e situacao', async () => {
    prepara([nota()])
    renderTela()
    expect(await screen.findByText('1-100')).toBeInTheDocument()
    expect(screen.getByText('Ana Paula')).toBeInTheDocument()
    expect(screen.getByText('123.456.789-09')).toBeInTheDocument()
    expect(screen.getByText('Consultoria')).toBeInTheDocument()
    expect(screen.getByText(/1\.500,50/)).toBeInTheDocument()
    expect(screen.getByText(/10\/11\/2026/)).toBeInTheDocument()
    for (const h of ['Número', 'Cliente', 'Serviço', 'Valor', 'Emissão', 'Situação']) expect(screen.getByText(h)).toBeInTheDocument()
    expect(apiMock).toHaveBeenCalledWith('/nfse/notas/0/9', { query: { estado: undefined, busca: undefined } })
  })

  it.each([
    ['autorizada', 'Autorizada'],
    ['processando', 'Processando'],
    ['emitida_offline', 'Emitida offline'],
    ['rejeitada', 'Rejeitada'],
    ['em_conflito', 'Em conflito'],
    ['rascunho', 'Rascunho'],
    ['cancelada', 'Cancelada'],
    ['descartada', 'Descartada'],
  ])('badge de situacao %s mostra %s', async (estado, rotulo) => {
    prepara([nota({ estado })])
    renderTela()
    const row = (await screen.findByText('1-100')).closest('tr')!
    expect(within(row).getByText(rotulo)).toBeInTheDocument()
  })

  it('estado vazio com o botao Nova nota para operador', async () => {
    prepara([])
    renderTela()
    expect(await screen.findByText('Nenhuma nota emitida ainda.')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /Nova nota/ }).length).toBeGreaterThan(0)
  })

  it('botao Nova nota so para operador e acima; navega para /notas/novo', async () => {
    prepara([nota()])
    renderTela()
    await userEvent.setup().click(await screen.findByRole('button', { name: /Nova nota/ }))
    expect(navegar).toHaveBeenCalledWith({ to: '/notas/novo' })
  })

  it('consulta nao ve o botao Nova nota', async () => {
    sessao.atual = com('consulta')
    prepara([nota()])
    renderTela()
    await screen.findByText('1-100')
    expect(screen.queryByRole('button', { name: /Nova nota/ })).not.toBeInTheDocument()
  })

  it('Abrir navega para o detalhe', async () => {
    prepara([nota({ id: 'abc' })])
    renderTela()
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Abrir' }))
    expect(navegar).toHaveBeenCalledWith({ to: '/notas/$id', params: { id: 'abc' } })
  })

  it('Visualizar PDF so aparece com temPdf e abre pelo helper autenticado', async () => {
    prepara([nota({ id: 'a', temPdf: true }), nota({ id: 'b', numero: 101, temPdf: false })])
    pdfMock.mockResolvedValue(undefined)
    renderTela()
    await screen.findByText('1-101')
    const botoes = screen.getAllByRole('button', { name: 'Visualizar PDF' })
    expect(botoes).toHaveLength(1)
    await userEvent.setup().click(botoes[0]!)
    expect(pdfMock).toHaveBeenCalledWith('/nfse/notas/a/pdf')
  })

  it('filtro de situacao e busca vao na consulta (estado e busca)', async () => {
    prepara([nota()])
    const user = userEvent.setup()
    renderTela()
    await screen.findByText('1-100')
    await user.click(screen.getByRole('combobox', { name: 'Situação' }))
    await user.click(await screen.findByRole('option', { name: 'Rejeitada' }))
    await waitFor(() => expect(apiMock).toHaveBeenCalledWith('/nfse/notas/0/9', { query: { estado: 'rejeitada', busca: undefined } }))
    await user.type(screen.getByRole('searchbox', { name: 'Buscar' }), 'ana')
    await waitFor(() => expect(apiMock).toHaveBeenCalledWith('/nfse/notas/0/9', { query: { estado: 'rejeitada', busca: 'ana' } }))
  })

  it('mostra erro de carregamento com Tentar novamente', async () => {
    apiMock.mockRejectedValue(new Error('x'))
    renderTela()
    expect(await screen.findByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
  })

  it('recarrega a cada 10 s enquanto houver nota processando', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    prepara([nota({ estado: 'processando' })])
    renderTela()
    await screen.findByText('1-100')
    const antes = listaChamadas().length
    await act(async () => {
      await vi.advanceTimersByTimeAsync(10_500)
    })
    expect(listaChamadas().length).toBeGreaterThan(antes)
  })

  it('para de recarregar quando nenhuma nota esta processando', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    prepara([nota({ estado: 'autorizada' })])
    renderTela()
    await screen.findByText('1-100')
    const antes = listaChamadas().length
    await act(async () => {
      await vi.advanceTimersByTimeAsync(35_000)
    })
    expect(listaChamadas().length).toBe(antes)
  })
})
