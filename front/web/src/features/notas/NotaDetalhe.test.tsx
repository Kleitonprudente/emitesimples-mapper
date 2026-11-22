import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { NotaDetalhe } from '@/features/notas/NotaDetalhe'

const apiMock = vi.hoisted(() => vi.fn())
const pdfMock = vi.hoisted(() => vi.fn())
const navegar = vi.hoisted(() => vi.fn())
const toastMock = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))
const sessao = vi.hoisted(() => ({ atual: null as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock, abrirPdf: pdfMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))
vi.mock('@tanstack/react-router', async (orig) => ({ ...(await orig<typeof import('@tanstack/react-router')>()), useNavigate: () => navegar }))
vi.mock('sonner', () => ({ toast: toastMock }))

const BASE = {
  id: 'n1', numero: 100, serie: '1', estado: 'autorizada', dataEmissao: '2026-11-10T14:30:00', valorServicos: 250,
  pessoa: { id: 'p1', nome: 'Ana Paula', documento: '12345678909' }, servico: { id: 's1', nome: 'Consultoria' },
  chave: null, numeroNfse: null, codigoRetorno: null, mensagemRetorno: null, temPdf: false, dataCriacao: '2026-11-10T14:00:00',
  informacoesComplementares: null, discriminacao: 'Serviço prestado.', descricao: null, codigoServico: '170101', errosRetorno: null,
  linkPdf: null, codigoVerificacao: null, dataHoraEvento: null, editavel: false, podeReenviar: false, podeAtualizar: false,
}
const nota = (o: object = {}) => ({ ...BASE, ...o })
const REJEITADA = nota({
  estado: 'rejeitada', mensagemRetorno: 'A prefeitura rejeitou a nota.', editavel: true, podeReenviar: true,
  errosRetorno: [{ campo: 'tomador.cep', erro: 'E1', descricao: 'CEP inválido.' }, { campo: 'valorServicos', erro: 'E2', descricao: 'Valor não aceito.' }],
})

let atual: unknown
let aoPost: (path: string, body: unknown) => Promise<unknown> = async () => atual
function prepara(n: unknown) {
  atual = n
  aoPost = async () => atual
  apiMock.mockImplementation(async (path: string, opts?: { method?: string; body?: unknown }) => {
    if (opts?.method === 'POST') return aoPost(path, opts.body)
    return atual
  })
}
const posts = () => apiMock.mock.calls.filter(([, o]) => o?.method === 'POST')
const gets = () => apiMock.mock.calls.filter(([, o]) => !o?.method)

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <NotaDetalhe id="n1" />
    </QueryClientProvider>,
  )
}
const com = (papel: string) => ({ usuario: { id: 'u', nome: 'Ana', email: 'a@x.com' }, empresa: { id: 'e', razaoSocial: 'H' }, papel, plataformaAdmin: false })

beforeEach(() => {
  apiMock.mockReset()
  pdfMock.mockReset()
  navegar.mockReset()
  toastMock.success.mockReset()
  sessao.atual = com('operador')
})
afterEach(() => vi.useRealTimers())

describe('NotaDetalhe', () => {
  it('mostra carregando e depois o cabecalho Nota serie-numero com a situacao e o card Dados', async () => {
    prepara(nota({ numeroNfse: '2026100', codigoVerificacao: 'ABCD1', chave: '41088' }))
    renderTela()
    expect(screen.getByText('Carregando nota…')).toBeInTheDocument()
    expect(await screen.findByRole('heading', { name: 'Nota 1-100' })).toBeInTheDocument()
    expect(screen.getByText('Autorizada')).toBeInTheDocument()
    expect(screen.getByText('Dados')).toBeInTheDocument()
    expect(screen.getByText('Ana Paula - 123.456.789-09')).toBeInTheDocument()
    expect(screen.getByText('Consultoria')).toBeInTheDocument()
    expect(screen.getByText(/250,00/)).toBeInTheDocument()
    expect(screen.getByText('2026100')).toBeInTheDocument()
    expect(screen.getByText('ABCD1')).toBeInTheDocument()
    expect(screen.getByText('41088')).toBeInTheDocument()
    expect(apiMock).toHaveBeenCalledWith('/nfse/notas/n1')
  })

  it('erro ao carregar mostra ErrorState com Tentar novamente', async () => {
    apiMock.mockRejectedValue(new ApiError(404, 'nao_encontrado', 'Nota não encontrada.'))
    renderTela()
    expect(await screen.findByText('Não foi possível carregar a nota.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
  })

  describe('processando', () => {
    it('Alert de espera, Atualizar agora chama POST /atualizar e atualiza a tela', async () => {
      prepara(nota({ estado: 'processando', podeAtualizar: true }))
      aoPost = async () => {
        atual = nota({ estado: 'autorizada' })
        return atual
      }
      renderTela()
      expect(await screen.findByText('Aguardando a autorização da prefeitura…')).toBeInTheDocument()
      expect(screen.getByText('Processando')).toBeInTheDocument()
      await userEvent.setup().click(screen.getByRole('button', { name: 'Atualizar agora' }))
      await waitFor(() => expect(posts()[0]![0]).toBe('/nfse/notas/n1/atualizar'))
      expect(await screen.findByText('NFS-e autorizada.')).toBeInTheDocument()
    })

    it('consulta a cada 3 s e para quando a nota e autorizada', async () => {
      vi.useFakeTimers({ shouldAdvanceTime: true })
      prepara(nota({ estado: 'processando', podeAtualizar: true }))
      renderTela()
      await screen.findByText('Aguardando a autorização da prefeitura…')
      const antes = gets().length
      await act(async () => {
        await vi.advanceTimersByTimeAsync(3_200)
      })
      expect(gets().length).toBeGreaterThan(antes)
      atual = nota({ estado: 'autorizada' })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(3_200)
      })
      expect(await screen.findByText('NFS-e autorizada.')).toBeInTheDocument()
      const depois = gets().length
      await act(async () => {
        await vi.advanceTimersByTimeAsync(10_000)
      })
      expect(gets().length).toBe(depois)
    })

    it('Atualizar agora so aparece com podeAtualizar e para operador', async () => {
      prepara(nota({ estado: 'processando', podeAtualizar: false }))
      renderTela()
      await screen.findByText('Aguardando a autorização da prefeitura…')
      expect(screen.queryByRole('button', { name: 'Atualizar agora' })).not.toBeInTheDocument()
    })

    it('consulta nao ve Atualizar agora', async () => {
      sessao.atual = com('consulta')
      prepara(nota({ estado: 'processando', podeAtualizar: true }))
      renderTela()
      await screen.findByText('Aguardando a autorização da prefeitura…')
      expect(screen.queryByRole('button', { name: 'Atualizar agora' })).not.toBeInTheDocument()
    })

    it('emitida_offline tambem aguarda a prefeitura', async () => {
      prepara(nota({ estado: 'emitida_offline', podeAtualizar: true }))
      renderTela()
      expect(await screen.findByText('Aguardando a autorização da prefeitura…')).toBeInTheDocument()
      expect(screen.getByText('Emitida offline')).toBeInTheDocument()
    })

    it('erro ao atualizar aparece no Alert com mensagem amigavel', async () => {
      prepara(nota({ estado: 'processando', podeAtualizar: true }))
      aoPost = async () => {
        throw new ApiError(409, 'nota_nao_editavel', 'x')
      }
      renderTela()
      await userEvent.setup().click(await screen.findByRole('button', { name: 'Atualizar agora' }))
      expect(await screen.findByText('Esta nota não pode mais ser editada.')).toBeInTheDocument()
    })
  })

  describe('autorizada', () => {
    it('Alert de sucesso e Visualizar PDF pelo helper autenticado quando ha temPdf', async () => {
      prepara(nota({ temPdf: true }))
      pdfMock.mockResolvedValue(undefined)
      renderTela()
      expect(await screen.findByText('NFS-e autorizada.')).toBeInTheDocument()
      await userEvent.setup().click(screen.getByRole('button', { name: 'Visualizar PDF' }))
      expect(pdfMock).toHaveBeenCalledWith('/nfse/notas/n1/pdf')
    })

    it('sem temPdf mas com linkPdf mostra um link para nova aba', async () => {
      prepara(nota({ temPdf: false, linkPdf: 'https://prefeitura.exemplo/nota.pdf' }))
      renderTela()
      const link = await screen.findByRole('link', { name: 'Visualizar PDF' })
      expect(link).toHaveAttribute('href', 'https://prefeitura.exemplo/nota.pdf')
      expect(link).toHaveAttribute('target', '_blank')
      expect(link).toHaveAttribute('rel', 'noreferrer')
    })

    it('Enviar por e-mail aparece desabilitado com Tooltip "Em breve."', async () => {
      prepara(nota({ temPdf: true }))
      renderTela()
      const botao = await screen.findByRole('button', { name: 'Enviar por e-mail' })
      expect(botao).toBeDisabled()
      await userEvent.setup().hover(botao.parentElement!)
      expect((await screen.findAllByText('Em breve.')).length).toBeGreaterThan(0)
    })

    it('falha ao abrir o PDF avisa por toast de erro', async () => {
      prepara(nota({ temPdf: true }))
      pdfMock.mockRejectedValue(new ApiError(404, 'pdf_indisponivel', 'PDF indisponível.'))
      renderTela()
      await userEvent.setup().click(await screen.findByRole('button', { name: 'Visualizar PDF' }))
      await waitFor(() => expect(toastMock.error).toHaveBeenCalled())
    })
  })

  describe('rejeitada', () => {
    it('lista a mensagem e os erros de retorno e mostra o formulario preenchido', async () => {
      prepara(REJEITADA)
      renderTela()
      expect(await screen.findByText('A prefeitura rejeitou a nota.')).toBeInTheDocument()
      expect(screen.getByText('tomador.cep: CEP inválido.')).toBeInTheDocument()
      expect(screen.getByText('valorServicos: Valor não aceito.')).toBeInTheDocument()
      expect(screen.getByRole('combobox', { name: /Cliente/ })).toHaveTextContent('Ana Paula - 123.456.789-09')
      expect(screen.getByRole('combobox', { name: /Serviço/ })).toHaveTextContent('Consultoria')
      expect(screen.getByLabelText(/Valor dos serviços/)).toHaveValue(250)
      expect(screen.getByLabelText(/Descrição do serviço/)).toHaveValue('Serviço prestado.')
      expect(screen.getByText('O número da nota é mantido no reenvio.')).toBeInTheDocument()
    })

    it('Corrigir e reenviar envia o corpo editado ao mesmo id e avisa', async () => {
      prepara(REJEITADA)
      aoPost = async () => nota({ estado: 'processando', podeAtualizar: true, valorServicos: 300 })
      const user = userEvent.setup()
      renderTela()
      const valor = await screen.findByLabelText(/Valor dos serviços/)
      await user.clear(valor)
      await user.type(valor, '300')
      await user.click(screen.getByRole('button', { name: 'Corrigir e reenviar' }))
      await waitFor(() => expect(posts()).toHaveLength(1))
      expect(posts()[0]).toEqual(['/nfse/notas/n1/enviar', { method: 'POST', body: { pessoaId: 'p1', servicoId: 's1', valorServicos: 300, discriminacao: 'Serviço prestado.' } }])
      await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Nota reenviada.'))
      expect(await screen.findByRole('heading', { name: 'Nota 1-100' })).toBeInTheDocument()
      expect(await screen.findByText('Aguardando a autorização da prefeitura…')).toBeInTheDocument()
    })

    it('erro do back no reenvio aparece em portugues', async () => {
      prepara(REJEITADA)
      aoPost = async () => {
        throw new ApiError(409, 'certificado_nao_valido', 'x')
      }
      await (async () => {
        renderTela()
        await userEvent.setup().click(await screen.findByRole('button', { name: 'Corrigir e reenviar' }))
      })()
      expect(await screen.findByText('O certificado digital não está válido. Envie em Configuração > Empresa.')).toBeInTheDocument()
    })

    it('consulta ve os erros mas nao o formulario de reenvio', async () => {
      sessao.atual = com('consulta')
      prepara(REJEITADA)
      renderTela()
      expect(await screen.findByText('tomador.cep: CEP inválido.')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
    })

    it('rejeitada sem podeReenviar fica somente leitura', async () => {
      prepara({ ...REJEITADA, podeReenviar: false })
      renderTela()
      await screen.findByText('A prefeitura rejeitou a nota.')
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
    })
  })

  describe('rascunho', () => {
    it('com podeReenviar mostra aviso de nao enviada e o formulario', async () => {
      prepara(nota({ estado: 'rascunho', podeReenviar: true, editavel: true }))
      renderTela()
      expect(await screen.findByText('Esta nota ainda não foi enviada.')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Corrigir e reenviar' })).toBeInTheDocument()
    })

    it('sem podeReenviar e leitura', async () => {
      prepara(nota({ estado: 'rascunho' }))
      renderTela()
      await screen.findByText('Rascunho')
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
    })
  })

  describe('em conflito', () => {
    it('avisa para nao reenviar, oferece Atualizar agora e nunca o formulario de reenvio', async () => {
      prepara(nota({ estado: 'em_conflito', podeAtualizar: true, podeReenviar: true }))
      renderTela()
      expect(await screen.findByText('A prefeitura indicou conflito. Não reenvie: use Atualizar agora.')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Atualizar agora' })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
    })
  })

  describe('cancelada e descartada', () => {
    it.each([['cancelada', 'Cancelada'], ['descartada', 'Descartada']])('%s e somente leitura', async (estado, rotulo) => {
      prepara(nota({ estado }))
      renderTela()
      await screen.findByText(rotulo)
      expect(screen.getByText('Serviço prestado.')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Atualizar agora|Corrigir e reenviar|Visualizar PDF/ })).not.toBeInTheDocument()
    })
  })

  it('Voltar a lista navega para /notas', async () => {
    prepara(nota())
    renderTela()
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Voltar à lista' }))
    expect(navegar).toHaveBeenCalledWith({ to: '/notas' })
  })
})
