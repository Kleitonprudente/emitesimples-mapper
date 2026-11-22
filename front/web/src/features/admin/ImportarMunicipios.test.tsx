import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { ImportarMunicipios } from '@/features/admin/ImportarMunicipios'

const apiMock = vi.hoisted(() => vi.fn())
const sessao = vi.hoisted(() => ({ atual: { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true } as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))

const HISTORICO = [{ id: 'i0', dataHora: '2026-10-02T14:20:00', arquivo: 'ibge-2025.csv', usuario: 'Eva Plataforma', linhasLidas: 5571, criados: 5571, atualizados: 0, inalterados: 0, rejeitados: 0 }]

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <ImportarMunicipios />
    </QueryClientProvider>,
  )
}

const arquivo = () => new File(['codigoIbge;nome;uf\n4108809;Guaíra;PR'], 'municipios.csv', { type: 'text/csv' })

beforeEach(() => {
  apiMock.mockReset()
  sessao.atual = { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true }
})

describe('ImportarMunicipios', () => {
  it('quem nao e administrador da plataforma ve acesso restrito', () => {
    sessao.atual = { ...(sessao.atual as object), plataformaAdmin: false }
    renderTela()
    expect(screen.getByRole('alert')).toHaveTextContent('Acesso restrito')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('renderiza o formulario e o historico (GET /admin/municipios/importacoes)', async () => {
    apiMock.mockResolvedValue(HISTORICO)
    renderTela()
    expect(screen.getByLabelText('Arquivo de municípios')).toHaveAttribute('type', 'file')
    expect(screen.getByRole('button', { name: 'Importar' })).toBeInTheDocument()
    expect(await screen.findByText('ibge-2025.csv')).toBeInTheDocument()
    expect(screen.getByText('5.571', { selector: 'td:nth-of-type(4)' })).toBeInTheDocument()
    expect(apiMock).toHaveBeenCalledWith('/admin/municipios/importacoes')
  })

  it('historico vazio e com erro', async () => {
    apiMock.mockResolvedValueOnce([])
    renderTela()
    expect(await screen.findByText('Nenhuma importação realizada ainda.')).toBeInTheDocument()
  })

  it('sem arquivo: valida e nao envia', async () => {
    apiMock.mockResolvedValue([])
    const user = userEvent.setup()
    renderTela()
    await user.click(screen.getByRole('button', { name: 'Importar' }))
    expect(await screen.findByText('Escolha um arquivo para importar.')).toHaveAttribute('role', 'alert')
    expect(screen.getByLabelText('Arquivo de municípios')).toHaveAttribute('aria-invalid', 'true')
    expect(apiMock.mock.calls.filter(([, o]) => o?.method === 'POST')).toHaveLength(0)
  })

  it('envia multipart (campo `arquivo`) e mostra o resumo com as linhas rejeitadas', async () => {
    apiMock.mockImplementation(async (_p: string, opts?: { method?: string }) => {
      if (opts?.method === 'POST') {
        return { id: 'i1', linhasLidas: 10, criados: 6, atualizados: 2, inalterados: 1, rejeitados: [{ linha: 4, motivo: 'Código IBGE inválido.' }] }
      }
      return HISTORICO
    })
    const user = userEvent.setup()
    renderTela()
    await user.upload(screen.getByLabelText('Arquivo de municípios'), arquivo())
    await user.click(screen.getByRole('button', { name: 'Importar' }))
    const resumo = await screen.findByRole('heading', { name: 'Resumo da importação' })
    const card = resumo.closest('[data-slot="card"]') as HTMLElement
    for (const [rotulo, valor] of [['Linhas lidas', '10'], ['Criados', '6'], ['Atualizados', '2'], ['Inalterados', '1'], ['Rejeitados', '1']]) {
      expect(within(card).getByText(rotulo!).nextElementSibling).toHaveTextContent(valor!)
    }
    expect(within(card).getByText('Código IBGE inválido.')).toBeInTheDocument()
    expect(within(card).getByText('4')).toBeInTheDocument()

    const post = apiMock.mock.calls.find(([, o]) => o?.method === 'POST')!
    expect(post[0]).toBe('/admin/municipios/importacao')
    const form = post[1].body as FormData
    expect(form).toBeInstanceOf(FormData)
    expect((form.get('arquivo') as File).name).toBe('municipios.csv')
    expect(screen.getByLabelText('Arquivo de municípios')).toHaveValue('') // campo limpo
    // historico recarregado apos importar
    await waitFor(() => expect(apiMock.mock.calls.filter(([p]) => p === '/admin/municipios/importacoes').length).toBeGreaterThanOrEqual(2))
  })

  it('le a resposta de forma tolerante (campos ausentes sao omitidos; rejeitados como numero)', async () => {
    apiMock.mockImplementation(async (_p: string, opts?: { method?: string }) => (opts?.method === 'POST' ? { id: 'i2', linhasLidas: 3, rejeitados: 0 } : []))
    const user = userEvent.setup()
    renderTela()
    await user.upload(screen.getByLabelText('Arquivo de municípios'), arquivo())
    await user.click(screen.getByRole('button', { name: 'Importar' }))
    const card = (await screen.findByRole('heading', { name: 'Resumo da importação' })).closest('[data-slot="card"]') as HTMLElement
    expect(within(card).getByText('Linhas lidas')).toBeInTheDocument()
    expect(within(card).queryByText('Criados')).not.toBeInTheDocument()
    expect(within(card).queryByText('Linhas rejeitadas')).not.toBeInTheDocument()
  })

  it('erro do servidor aparece com mensagem e codigo, sem resumo', async () => {
    apiMock.mockImplementation(async (_p: string, opts?: { method?: string }) => {
      if (opts?.method === 'POST') throw new ApiError(400, 'arquivo_invalido', 'O arquivo está vazio ou sem linhas de dados.')
      return []
    })
    const user = userEvent.setup()
    renderTela()
    await user.upload(screen.getByLabelText('Arquivo de municípios'), arquivo())
    await user.click(screen.getByRole('button', { name: 'Importar' }))
    const alerta = await screen.findByText('O arquivo está vazio ou sem linhas de dados.')
    expect(alerta.closest('[role="alert"]')).toHaveTextContent('arquivo_invalido')
    expect(screen.queryByRole('heading', { name: 'Resumo da importação' })).not.toBeInTheDocument()
  })
})
