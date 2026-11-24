import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
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
  id: 'n1', numero: 100, serie: '1', estado: 'AUTORIZADA', dataEmissao: '2026-11-10T14:30:00', valorServicos: 250,
  pessoa: { id: 'p1', nome: 'Ana Paula', documento: '12345678909' }, servico: { id: 's1', nome: 'Consultoria' },
  chave: null, numeroNfse: null, codigoRetorno: null, mensagemRetorno: null, temPdf: false, dataCriacao: '2026-11-10T14:00:00',
  informacoesComplementares: null, discriminacao: 'Serviço prestado.', descricao: null, codigoServico: '170101', errosRetorno: null,
  linkPdf: null, codigoVerificacao: null, dataHoraEvento: null, editavel: false, podeReenviar: false, podeAtualizar: false, cancelamento: null,
}
const nota = (o: object = {}) => ({ ...BASE, ...o })
const REJEITADA = nota({
  estado: 'REJEITADA', mensagemRetorno: 'A prefeitura rejeitou a nota.', editavel: true, podeReenviar: true,
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
const com = (papel: string, podeCancelarNfse?: boolean) => ({ usuario: { id: 'u', nome: 'Ana', email: 'a@x.com' }, empresa: { id: 'e', razaoSocial: 'H' }, papel, plataformaAdmin: false, ...(podeCancelarNfse === undefined ? {} : { podeCancelarNfse }) })

beforeEach(() => {
  apiMock.mockReset()
  pdfMock.mockReset()
  navegar.mockReset()
  toastMock.success.mockReset()
  sessao.atual = com('OPERADOR')
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
      prepara(nota({ estado: 'PROCESSANDO', podeAtualizar: true }))
      aoPost = async () => {
        atual = nota({ estado: 'AUTORIZADA' })
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
      prepara(nota({ estado: 'PROCESSANDO', podeAtualizar: true }))
      renderTela()
      await screen.findByText('Aguardando a autorização da prefeitura…')
      const antes = gets().length
      await act(async () => {
        await vi.advanceTimersByTimeAsync(3_200)
      })
      expect(gets().length).toBeGreaterThan(antes)
      atual = nota({ estado: 'AUTORIZADA' })
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
      prepara(nota({ estado: 'PROCESSANDO', podeAtualizar: false }))
      renderTela()
      await screen.findByText('Aguardando a autorização da prefeitura…')
      expect(screen.queryByRole('button', { name: 'Atualizar agora' })).not.toBeInTheDocument()
    })

    it('consulta nao ve Atualizar agora', async () => {
      sessao.atual = com('CONSULTA')
      prepara(nota({ estado: 'PROCESSANDO', podeAtualizar: true }))
      renderTela()
      await screen.findByText('Aguardando a autorização da prefeitura…')
      expect(screen.queryByRole('button', { name: 'Atualizar agora' })).not.toBeInTheDocument()
    })

    it('emitida_offline tambem aguarda a prefeitura', async () => {
      prepara(nota({ estado: 'EMITIDA_OFFLINE', podeAtualizar: true }))
      renderTela()
      expect(await screen.findByText('Aguardando a autorização da prefeitura…')).toBeInTheDocument()
      expect(screen.getByText('Emitida offline')).toBeInTheDocument()
    })

    it('erro ao atualizar aparece no Alert com mensagem amigavel', async () => {
      prepara(nota({ estado: 'PROCESSANDO', podeAtualizar: true }))
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
      aoPost = async () => nota({ estado: 'PROCESSANDO', podeAtualizar: true, valorServicos: 300 })
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
      sessao.atual = com('CONSULTA')
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

  describe('falha no envio', () => {
    const FALHA = nota({ estado: 'FALHA_ENVIO', mensagemRetorno: 'Falha de comunicação com o gateway.', podeReenviar: true, editavel: true })

    it('badge de aviso, explica a falha e reenvia como rejeitada (mesmo numero)', async () => {
      prepara(FALHA)
      aoPost = async () => nota({ estado: 'PROCESSANDO', podeAtualizar: true })
      renderTela()
      expect(await screen.findByText('Falha no envio')).toBeInTheDocument()
      expect(screen.getByText('Falha de comunicação com o gateway.')).toBeInTheDocument()
      expect(screen.getByText('A nota não chegou à prefeitura por uma falha no envio. Reenvie para tentar de novo.')).toBeInTheDocument()
      expect(screen.getByText('O número da nota é mantido no reenvio.')).toBeInTheDocument()
      await userEvent.setup().click(screen.getByRole('button', { name: 'Corrigir e reenviar' }))
      await waitFor(() => expect(posts()[0]![0]).toBe('/nfse/notas/n1/enviar'))
      await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Nota reenviada.'))
    })

    it('sem podeReenviar fica somente leitura', async () => {
      prepara({ ...FALHA, podeReenviar: false })
      renderTela()
      await screen.findByText('A nota não chegou à prefeitura por uma falha no envio. Reenvie para tentar de novo.')
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
    })

    it('consulta nao ve o formulario de reenvio', async () => {
      sessao.atual = com('CONSULTA')
      prepara(FALHA)
      renderTela()
      await screen.findByText('Falha de comunicação com o gateway.')
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
    })
  })

  describe('rascunho do assistente', () => {
    const RASCUNHO = nota({ estado: 'RASCUNHO', podeAprovar: true, podeDescartar: true })
    const APROVADA = nota({ estado: 'PROCESSANDO', podeAtualizar: true })

    it('mostra o aviso e o badge, e nunca o formulario de reenvio nem o antigo "ainda nao foi enviada"', async () => {
      prepara(nota({ estado: 'RASCUNHO', podeReenviar: true, editavel: true, podeAprovar: true, podeDescartar: true }))
      renderTela()
      expect(await screen.findByText('Preparada pelo assistente. Revise e aprove para emitir.')).toBeInTheDocument()
      expect(screen.getByText('Rascunho (assistente)')).toBeInTheDocument()
      expect(screen.queryByText('Esta nota ainda não foi enviada.')).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Aprovar e emitir' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Descartar' })).toBeInTheDocument()
    })

    it('os botoes seguem podeAprovar e podeDescartar', async () => {
      prepara(nota({ estado: 'RASCUNHO', podeAprovar: false, podeDescartar: true }))
      renderTela()
      await screen.findByText('Preparada pelo assistente. Revise e aprove para emitir.')
      expect(screen.queryByRole('button', { name: 'Aprovar e emitir' })).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Descartar' })).toBeInTheDocument()
    })

    it('sem as permissoes nao ha botoes (leitura)', async () => {
      prepara(nota({ estado: 'RASCUNHO' }))
      renderTela()
      await screen.findByText('Rascunho (assistente)')
      expect(screen.queryByRole('button', { name: /Aprovar|Descartar/ })).not.toBeInTheDocument()
    })

    it('Aprovar e emitir pede confirmacao, so chama /aprovar ao confirmar e a nota vai a processando', async () => {
      prepara(RASCUNHO)
      aoPost = async () => {
        atual = APROVADA
        return atual
      }
      const user = userEvent.setup()
      renderTela()
      await user.click(await screen.findByRole('button', { name: 'Aprovar e emitir' }))
      const dlg = await screen.findByRole('alertdialog')
      expect(within(dlg).getByText('Aprovar e emitir esta nota?')).toBeInTheDocument()
      expect(posts()).toHaveLength(0)
      await user.click(within(dlg).getByRole('button', { name: 'Voltar' }))
      await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument())
      expect(posts()).toHaveLength(0)
      await user.click(screen.getByRole('button', { name: 'Aprovar e emitir' }))
      await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Confirmar' }))
      await waitFor(() => expect(posts()).toHaveLength(1))
      expect(posts()[0]).toEqual(['/nfse/notas/n1/aprovar', { method: 'POST' }])
      expect(await screen.findByText('Aguardando a autorização da prefeitura…')).toBeInTheDocument()
      expect(toastMock.success).toHaveBeenCalledWith('Nota aprovada e enviada à prefeitura.')
      expect(screen.queryByRole('button', { name: 'Aprovar e emitir' })).not.toBeInTheDocument()
    })

    it('Descartar pede confirmacao, chama /descartar e a nota vira Descartada', async () => {
      prepara(RASCUNHO)
      aoPost = async () => {
        atual = nota({ estado: 'DESCARTADA' })
        return atual
      }
      const user = userEvent.setup()
      renderTela()
      await user.click(await screen.findByRole('button', { name: 'Descartar' }))
      const dlg = await screen.findByRole('alertdialog')
      expect(within(dlg).getByText('Descartar este rascunho?')).toBeInTheDocument()
      expect(posts()).toHaveLength(0)
      await user.click(within(dlg).getByRole('button', { name: 'Confirmar' }))
      await waitFor(() => expect(posts()[0]).toEqual(['/nfse/notas/n1/descartar', { method: 'POST' }]))
      expect(await screen.findByText('Descartada')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Descartar' })).not.toBeInTheDocument()
      expect(toastMock.success).toHaveBeenCalledWith('Rascunho descartado.')
    })

    it('erro do back ao aprovar aparece legivel e os botoes continuam', async () => {
      prepara(RASCUNHO)
      aoPost = async () => {
        throw new ApiError(409, 'certificado_nao_valido', 'x')
      }
      const user = userEvent.setup()
      renderTela()
      await user.click(await screen.findByRole('button', { name: 'Aprovar e emitir' }))
      await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Confirmar' }))
      expect(await screen.findByText('O certificado digital não está válido. Envie em Configuração > Empresa.')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Aprovar e emitir' })).toBeEnabled()
    })

    it('nao ha botao para preparar nota pelo assistente na tela', async () => {
      prepara(RASCUNHO)
      renderTela()
      await screen.findByText('Rascunho (assistente)')
      expect(screen.queryByRole('button', { name: /Preparar|Nova nota/i })).not.toBeInTheDocument()
    })
  })

  describe('cancelamento preparado pelo assistente', () => {
    const MOTIVO = 'Valor emitido errado, nota refeita.'
    const COM_RASCUNHO = (extra: object = {}) =>
      nota({ cancelamento: { id: 'c1', estado: 'RASCUNHO', codigoMotivo: 1, motivo: MOTIVO, solicitadoEm: '2026-11-23T11:00:00', podeAprovar: true, podeDescartar: true, ...extra } })

    beforeEach(() => {
      vi.useFakeTimers({ toFake: ['Date'] })
      vi.setSystemTime(new Date('2026-11-23T15:00:00Z'))
    })

    it('mostra o bloco com os botoes, a situacao e esconde Cancelar NFS-e', async () => {
      sessao.atual = com('ADMINISTRADOR')
      prepara(COM_RASCUNHO())
      renderTela()
      expect(await screen.findByText('Cancelamento preparado pelo assistente')).toBeInTheDocument()
      expect(screen.getByText(MOTIVO)).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Aprovar cancelamento' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Descartar' })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Cancelar NFS-e' })).not.toBeInTheDocument()
    })

    it('os botoes seguem cancelamento.podeAprovar e podeDescartar', async () => {
      prepara(COM_RASCUNHO({ podeAprovar: false, podeDescartar: false }))
      renderTela()
      await screen.findByText('Cancelamento preparado pelo assistente')
      expect(screen.queryByRole('button', { name: 'Aprovar cancelamento' })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Descartar' })).not.toBeInTheDocument()
    })

    it('Aprovar cancelamento confirma e chama /nfse/cancelamentos/{id}/aprovar', async () => {
      prepara(COM_RASCUNHO())
      aoPost = async () => {
        atual = nota({ estado: 'CANCELADA', cancelamento: { id: 'c1', estado: 'CONCLUIDO', codigoMotivo: 1, motivo: MOTIVO } })
        return atual
      }
      const user = userEvent.setup()
      renderTela()
      await user.click(await screen.findByRole('button', { name: 'Aprovar cancelamento' }))
      const dlg = await screen.findByRole('alertdialog')
      expect(within(dlg).getByText('Aprovar o cancelamento da nota?')).toBeInTheDocument()
      expect(posts()).toHaveLength(0)
      await user.click(within(dlg).getByRole('button', { name: 'Confirmar' }))
      await waitFor(() => expect(posts()[0]).toEqual(['/nfse/cancelamentos/c1/aprovar', { method: 'POST' }]))
      expect(await screen.findByText('Cancelada')).toBeInTheDocument()
      expect(screen.queryByText('Cancelamento preparado pelo assistente')).not.toBeInTheDocument()
    })

    it('Descartar o cancelamento chama /descartar e a nota volta a poder cancelar', async () => {
      sessao.atual = com('ADMINISTRADOR')
      prepara(COM_RASCUNHO())
      aoPost = async () => {
        atual = nota({ cancelamento: { id: 'c1', estado: 'DESCARTADO', codigoMotivo: 1, motivo: MOTIVO } })
        return atual
      }
      const user = userEvent.setup()
      renderTela()
      await user.click(await screen.findByRole('button', { name: 'Descartar' }))
      await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Confirmar' }))
      await waitFor(() => expect(posts()[0]).toEqual(['/nfse/cancelamentos/c1/descartar', { method: 'POST' }]))
      expect(await screen.findByText('Descartado')).toBeInTheDocument()
      expect(screen.queryByText('Cancelamento preparado pelo assistente')).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Cancelar NFS-e' })).toBeInTheDocument()
    })

    it('erro do back (sem delegacao) aparece em mensagem legivel', async () => {
      prepara(COM_RASCUNHO())
      aoPost = async () => {
        throw new ApiError(403, 'cancelamento_nao_delegado', 'Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')
      }
      const user = userEvent.setup()
      renderTela()
      await user.click(await screen.findByRole('button', { name: 'Aprovar cancelamento' }))
      await user.click(within(await screen.findByRole('alertdialog')).getByRole('button', { name: 'Confirmar' }))
      expect(await screen.findByText('Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')).toBeInTheDocument()
    })

    it.each([
      ['EXPIRADO', 'Expirado: sem confirmação'],
      ['DESCARTADO', 'Descartado'],
    ])('cancelamento %s mostra o rotulo %s e nenhum botao do assistente', async (estado, rotulo) => {
      prepara(nota({ cancelamento: { id: 'c9', estado, codigoMotivo: 1, motivo: MOTIVO } }))
      renderTela()
      expect(await screen.findByText(rotulo)).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Aprovar cancelamento/ })).not.toBeInTheDocument()
    })
  })

  describe('em conflito', () => {
    it('avisa para nao reenviar, oferece Atualizar agora e nunca o formulario de reenvio', async () => {
      prepara(nota({ estado: 'EM_CONFLITO', podeAtualizar: true, podeReenviar: true }))
      renderTela()
      expect(await screen.findByText('A prefeitura indicou conflito. Não reenvie: use Atualizar agora.')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Atualizar agora' })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Corrigir e reenviar' })).not.toBeInTheDocument()
    })
  })

  describe('cancelada e descartada', () => {
    it.each([['CANCELADA', 'Cancelada'], ['DESCARTADA', 'Descartada']])('%s e somente leitura', async (estado, rotulo) => {
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

  describe('cancelar NFS-e', () => {
    const JUSTIFICATIVA = 'Valor emitido errado, nota refeita.'
    const CANCELADA = nota({
      estado: 'CANCELADA',
      cancelamento: { estado: 'CONCLUIDO', codigoMotivo: 1, motivo: JUSTIFICATIVA, mensagemRetorno: 'Cancelamento homologado.', solicitadoEm: '2026-11-23T11:00:00', dataHoraEvento: '2026-11-23T11:00:05' },
    })
    const PENDENTE = nota({ cancelamento: { estado: 'PROCESSANDO', codigoMotivo: 1, motivo: JUSTIFICATIVA, solicitadoEm: '2026-11-23T11:00:00' }, podeAtualizar: true })
    const botaoCancelar = () => screen.queryByRole('button', { name: 'Cancelar NFS-e' })
    const abrirModal = async () => {
      await userEvent.setup().click(await screen.findByRole('button', { name: 'Cancelar NFS-e' }))
      return await screen.findByRole('dialog')
    }
    const digitar = (dialogo: HTMLElement, valor: string) => fireEvent.change(within(dialogo).getByLabelText('Justificativa'), { target: { value: valor } })
    const confirmar = (dialogo: HTMLElement) => within(dialogo).getByRole('button', { name: /Cancelar nota|Cancelando/ })

    beforeEach(() => {
      vi.useFakeTimers({ toFake: ['Date'] })
      vi.setSystemTime(new Date('2026-11-23T15:00:00Z'))
      sessao.atual = com('OPERADOR', true)
    })

    it('operador delegado ve o botao em nota autorizada do mes vigente', async () => {
      prepara(nota())
      renderTela()
      expect(await screen.findByRole('button', { name: 'Cancelar NFS-e' })).toBeInTheDocument()
    })

    it.each([
      ['CONSULTA', 'AUTORIZADA', {}],
      ['OPERADOR', 'REJEITADA', {}],
      ['OPERADOR', 'PROCESSANDO', {}],
      ['OPERADOR', 'CANCELADA', {}],
      ['OPERADOR', 'AUTORIZADA', { podeCancelar: false }],
      ['OPERADOR', 'AUTORIZADA', { cancelamento: { estado: 'PROCESSANDO' } }],
    ])('%s com nota %s %j: sem botao Cancelar', async (papel, estado, extra) => {
      sessao.atual = com(papel, true)
      prepara(nota({ estado, ...extra }))
      renderTela()
      await screen.findByRole('heading', { name: 'Nota 1-100' })
      expect(botaoCancelar()).not.toBeInTheDocument()
    })

    it('operador SEM delegacao nao ve o botao, mas ve o texto discreto no mes vigente', async () => {
      sessao.atual = com('OPERADOR', false)
      prepara(nota({ podeCancelar: false }))
      renderTela()
      expect(await screen.findByText('Cancelamento depende de permissão delegada pelo administrador.')).toBeInTheDocument()
      expect(botaoCancelar()).not.toBeInTheDocument()
    })

    it('sessao sem o campo podeCancelarNfse (leitura tolerante): operador nao cancela', async () => {
      sessao.atual = com('OPERADOR')
      prepara(nota())
      renderTela()
      expect(await screen.findByText('Cancelamento depende de permissão delegada pelo administrador.')).toBeInTheDocument()
      expect(botaoCancelar()).not.toBeInTheDocument()
    })

    it('administrador sem o campo na sessao ve o botao e nao ve o texto', async () => {
      sessao.atual = com('ADMINISTRADOR')
      prepara(nota())
      renderTela()
      expect(await screen.findByRole('button', { name: 'Cancelar NFS-e' })).toBeInTheDocument()
      expect(screen.queryByText('Cancelamento depende de permissão delegada pelo administrador.')).not.toBeInTheDocument()
    })

    it('sem delegacao e fora do mes vigente nao mostra o texto (nada a delegar)', async () => {
      sessao.atual = com('OPERADOR', false)
      prepara(nota({ dataEmissao: '2026-10-20T14:00:00', podeCancelar: false }))
      renderTela()
      await screen.findByRole('heading', { name: 'Nota 1-100' })
      expect(screen.queryByText('Cancelamento depende de permissão delegada pelo administrador.')).not.toBeInTheDocument()
      expect(botaoCancelar()).not.toBeInTheDocument()
    })

    it('403 cancelamento_nao_delegado do back aparece no modal com a mensagem do back', async () => {
      prepara(nota())
      aoPost = async () => {
        throw new ApiError(403, 'cancelamento_nao_delegado', 'Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')
      }
      renderTela()
      const d = await abrirModal()
      digitar(d, JUSTIFICATIVA)
      await userEvent.setup().click(confirmar(d))
      expect(await within(d).findByText('Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')).toBeInTheDocument()
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    it('fora do mes vigente avisa na pagina e nao abre o modal nem chama a API', async () => {
      prepara(nota({ dataEmissao: '2026-10-20T14:00:00', podeCancelar: false }))
      renderTela()
      await userEvent.setup().click(await screen.findByRole('button', { name: 'Cancelar NFS-e' }))
      expect(screen.getByText('O cancelamento só é possível dentro do mês vigente da emissão da nota.')).toBeInTheDocument()
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      expect(posts()).toHaveLength(0)
    })

    it('fuso de Sao Paulo: emissao em UTC no dia 1 de dezembro 01h ainda e novembro', async () => {
      vi.setSystemTime(new Date('2026-11-30T20:00:00Z'))
      prepara(nota({ dataEmissao: '2026-12-01T01:00:00Z' }))
      renderTela()
      await abrirModal()
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })

    it('o modal so habilita Cancelar nota com 15 a 255 caracteres apos trim e mostra o contador', async () => {
      prepara(nota())
      renderTela()
      const d = await abrirModal()
      expect(within(d).getByText('0/255')).toBeInTheDocument()
      expect(confirmar(d)).toBeDisabled()
      digitar(d, '   ' + 'a'.repeat(14) + '   ')
      expect(within(d).getByText('14/255')).toBeInTheDocument()
      expect(confirmar(d)).toBeDisabled()
      digitar(d, 'a'.repeat(15))
      expect(confirmar(d)).toBeEnabled()
      digitar(d, 'a'.repeat(255))
      expect(within(d).getByText('255/255')).toBeInTheDocument()
      expect(confirmar(d)).toBeEnabled()
      digitar(d, 'a'.repeat(256))
      expect(confirmar(d)).toBeDisabled()
      expect(within(d).getByText('A justificativa deve ter de 15 a 255 caracteres.')).toBeInTheDocument()
      expect(posts()).toHaveLength(0)
    })

    it('Voltar fecha o modal sem chamar a API', async () => {
      prepara(nota())
      renderTela()
      const d = await abrirModal()
      await userEvent.setup().click(within(d).getByRole('button', { name: 'Voltar' }))
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(posts()).toHaveLength(0)
    })

    it('sucesso: envia o corpo, a nota vira Cancelada, mostra o bloco e avisa por toast', async () => {
      prepara(nota())
      aoPost = async () => {
        atual = CANCELADA
        return atual
      }
      renderTela()
      const d = await abrirModal()
      digitar(d, `  ${JUSTIFICATIVA}  `)
      await userEvent.setup().click(confirmar(d))
      await waitFor(() => expect(posts()).toHaveLength(1))
      expect(posts()[0]).toEqual(['/nfse/notas/n1/cancelar', { method: 'POST', body: { codigoMotivo: 1, motivo: JUSTIFICATIVA } }])
      await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Nota cancelada.'))
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
      expect(screen.getByText('Cancelada')).toBeInTheDocument()
      expect(screen.getByText('Cancelamento')).toBeInTheDocument()
      expect(screen.getByText('Erro de emissão')).toBeInTheDocument()
      expect(screen.getByText(JUSTIFICATIVA)).toBeInTheDocument()
      expect(screen.getByText('Cancelamento homologado.')).toBeInTheDocument()
      expect(botaoCancelar()).not.toBeInTheDocument()
    })

    it('envia o tipo escolhido no Select', async () => {
      prepara(nota())
      aoPost = async () => CANCELADA
      renderTela()
      const d = await abrirModal()
      const user = userEvent.setup()
      await user.click(within(d).getByRole('combobox', { name: 'Tipo do motivo' }))
      await user.click(await screen.findByRole('option', { name: 'Outros' }))
      digitar(d, JUSTIFICATIVA)
      await user.click(confirmar(d))
      await waitFor(() => expect(posts()).toHaveLength(1))
      expect(posts()[0]![1]).toEqual({ method: 'POST', body: { codigoMotivo: 9, motivo: JUSTIFICATIVA } })
    })

    it('durante o envio desabilita e mostra Cancelando…', async () => {
      prepara(nota())
      let liberar: (v: unknown) => void = () => {}
      aoPost = () => new Promise((r) => (liberar = r))
      renderTela()
      const d = await abrirModal()
      digitar(d, JUSTIFICATIVA)
      await userEvent.setup().click(confirmar(d))
      const botao = await within(d).findByRole('button', { name: /Cancelando…/ })
      expect(botao).toBeDisabled()
      expect(within(d).getByRole('button', { name: 'Voltar' })).toBeDisabled()
      liberar(CANCELADA)
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    })

    it('erro 409 do back vira mensagem legivel no modal, que continua aberto', async () => {
      prepara(nota())
      aoPost = async () => {
        throw new ApiError(409, 'cancelamento_fora_do_mes', 'x')
      }
      renderTela()
      const d = await abrirModal()
      digitar(d, JUSTIFICATIVA)
      await userEvent.setup().click(confirmar(d))
      expect(await within(d).findByText('O cancelamento só é possível dentro do mês vigente da emissão da nota.')).toBeInTheDocument()
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(confirmar(d)).toBeEnabled()
      expect(toastMock.success).not.toHaveBeenCalled()
    })

    it('recusa do gateway mostra a mensagem do gateway no modal', async () => {
      prepara(nota())
      aoPost = async () => {
        throw new ApiError(409, 'cancelamento_recusado', 'Prazo de cancelamento expirado na prefeitura.')
      }
      renderTela()
      const d = await abrirModal()
      digitar(d, JUSTIFICATIVA)
      await userEvent.setup().click(confirmar(d))
      expect(await within(d).findByText('Prazo de cancelamento expirado na prefeitura.')).toBeInTheDocument()
    })

    it('cancelamento pendente: mostra "Cancelamento em processamento" e Atualizar agora consulta de novo', async () => {
      prepara(nota())
      aoPost = async (path) => {
        if (path.endsWith('/cancelar')) {
          atual = PENDENTE
          return atual
        }
        atual = CANCELADA
        return atual
      }
      renderTela()
      const d = await abrirModal()
      digitar(d, JUSTIFICATIVA)
      await userEvent.setup().click(confirmar(d))
      expect(await screen.findByText('Cancelamento em processamento')).toBeInTheDocument()
      await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Cancelamento solicitado. Aguardando a confirmação da prefeitura.'))
      expect(botaoCancelar()).not.toBeInTheDocument()
      expect(screen.queryByText('NFS-e autorizada.')).not.toBeInTheDocument()
      await userEvent.setup().click(screen.getByRole('button', { name: 'Atualizar agora' }))
      await waitFor(() => expect(posts()[1]![0]).toBe('/nfse/notas/n1/atualizar'))
      expect(await screen.findByText('Cancelada')).toBeInTheDocument()
      expect(screen.queryByText('Cancelamento em processamento')).not.toBeInTheDocument()
    })

    it('cancelamento recusado aparece no bloco e a nota autorizada pode tentar de novo', async () => {
      prepara(nota({ cancelamento: { estado: 'RECUSADO', codigoMotivo: 2, motivo: JUSTIFICATIVA, mensagemRetorno: 'Prazo expirado.' } }))
      renderTela()
      expect(await screen.findByText('Recusado')).toBeInTheDocument()
      expect(screen.getByText('Serviço não concluído')).toBeInTheDocument()
      expect(screen.getByText('Prazo expirado.')).toBeInTheDocument()
      expect(botaoCancelar()).toBeInTheDocument()
    })
  })
})
