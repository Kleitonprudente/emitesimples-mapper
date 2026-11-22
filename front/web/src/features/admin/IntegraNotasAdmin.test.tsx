import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { IntegraNotasAdmin } from '@/features/admin/IntegraNotasAdmin'

const apiMock = vi.hoisted(() => vi.fn())
const sessao = vi.hoisted(() => ({ atual: { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true } as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))

const URL_CFG = '/admin/configuracoes/integranotas'
const BASE = {
  ambienteAtivo: 'homologacao',
  urlHomologacao: 'https://api.homologacao.integranotas.com.br',
  urlProducao: 'https://api.integranotas.com.br',
  urlPublicaWebhook: null,
  tokenHomologacaoConfigurado: false,
  tokenProducaoConfigurado: false,
  dataAtualizacao: null,
  atualizadoPor: null,
  producaoPermitida: false,
}
const COM_TOKENS = { ...BASE, tokenHomologacaoConfigurado: true, tokenProducaoConfigurado: true, urlPublicaWebhook: 'https://api.empresa.com.br', dataAtualizacao: '2026-11-01T10:30:00', atualizadoPor: { id: 'u1', nome: 'Eva Plataforma' } }

/** GET devolve `config`; PUT usa `put` (resolve por padrao). */
function prepara(config: unknown, put: () => Promise<unknown> = async () => undefined) {
  apiMock.mockImplementation(async (_p: string, opts?: { method?: string }) => (opts?.method === 'PUT' ? put() : config))
}
const puts = () => apiMock.mock.calls.filter(([, o]) => o?.method === 'PUT')
const leituras = () => apiMock.mock.calls.filter(([, o]) => !o?.method)

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <IntegraNotasAdmin />
    </QueryClientProvider>,
  )
}

const urlHom = () => screen.findByLabelText(/URL de homologação/)
const tokenHom = () => screen.getByLabelText('Token de homologação', { selector: 'input' })
const tokenProd = () => screen.getByLabelText('Token de produção', { selector: 'input' })

beforeEach(() => {
  apiMock.mockReset()
  sessao.atual = { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true }
})

describe('IntegraNotasAdmin', () => {
  it('quem nao e administrador da plataforma ve acesso restrito', () => {
    sessao.atual = { ...(sessao.atual as object), plataformaAdmin: false }
    renderTela()
    expect(screen.getByRole('alert')).toHaveTextContent('Acesso restrito')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('carrega a configuracao: aviso, secoes, URLs, webhook e ultima atualizacao', async () => {
    prepara(COM_TOKENS)
    renderTela()
    expect(await urlHom()).toHaveValue('https://api.homologacao.integranotas.com.br')
    expect(apiMock).toHaveBeenCalledWith(URL_CFG)
    expect(screen.getByText(/Homologação é o ambiente de testes da IntegraNotas/)).toBeInTheDocument()
    for (const s of ['Ambiente', 'Endereços da API', 'Webhook', 'Tokens da softhouse']) expect(screen.getByText(s)).toBeInTheDocument()
    expect(screen.getByLabelText(/URL de produção/)).toHaveValue('https://api.integranotas.com.br')
    expect(screen.getByLabelText(/URL pública do webhook/)).toHaveValue('https://api.empresa.com.br')
    expect(screen.getByText(/Deixe vazio para não registrar webhook/)).toBeInTheDocument()
    expect(screen.getByText(/Última atualização por Eva Plataforma em/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /testar/i })).not.toBeInTheDocument()
  })

  it('selos, hints e opcao de remover conforme os tokens configurados', async () => {
    prepara({ ...BASE, tokenHomologacaoConfigurado: true })
    renderTela()
    await urlHom()
    expect(screen.getAllByText('Configurado')).toHaveLength(1)
    expect(screen.getAllByText('Sem token')).toHaveLength(1)
    expect(screen.getByText('Token configurado. Deixe em branco para manter.')).toBeInTheDocument()
    expect(screen.getByText('Cole o token da softhouse neste ambiente.')).toBeInTheDocument()
    expect(screen.getByLabelText('Remover o token de homologação configurado')).toBeInTheDocument()
    expect(screen.queryByLabelText('Remover o token de produção configurado')).not.toBeInTheDocument()
    expect(screen.queryByText(/Última atualização/)).not.toBeInTheDocument()
  })

  it('producao bloqueada mostra o aviso; liberada nao', async () => {
    prepara(BASE)
    renderTela()
    expect(await screen.findByText('Produção está bloqueada neste servidor: só homologação.')).toBeInTheDocument()
  })

  it('producao permitida nao mostra o aviso', async () => {
    prepara({ ...BASE, producaoPermitida: true })
    renderTela()
    await urlHom()
    expect(screen.queryByText(/Produção está bloqueada/)).not.toBeInTheDocument()
  })

  it('mostra erro de carregamento', async () => {
    apiMock.mockRejectedValue(new ApiError(500, 'erro', 'Falha.'))
    renderTela()
    expect(await screen.findByText('Não foi possível carregar a configuração.')).toBeInTheDocument()
  })

  it('salvar envia so os campos alterados e refaz a leitura', async () => {
    prepara(COM_TOKENS)
    const user = userEvent.setup()
    renderTela()
    const url = await urlHom()
    await user.clear(url)
    await user.type(url, 'https://nova.integranotas.com.br')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]).toEqual([URL_CFG, { method: 'PUT', body: { urlHomologacao: 'https://nova.integranotas.com.br' } }])
    await waitFor(() => expect(leituras().length).toBeGreaterThanOrEqual(2))
  })

  it('limpar o webhook envia string vazia', async () => {
    prepara(COM_TOKENS)
    const user = userEvent.setup()
    renderTela()
    await urlHom()
    await user.clear(screen.getByLabelText(/URL pública do webhook/))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).toEqual({ urlPublicaWebhook: '' })
  })

  it('token novo e enviado e o campo fica vazio depois de salvar', async () => {
    prepara(BASE)
    const user = userEvent.setup()
    renderTela()
    await urlHom()
    await user.type(tokenHom(), 'segredo123')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).toEqual({ tokenHomologacao: 'segredo123' })
    await waitFor(() => expect(tokenHom()).toHaveValue(''))
    expect(tokenProd()).toHaveValue('')
  })

  it('"Remover o token configurado" envia token vazio so daquele ambiente', async () => {
    prepara(COM_TOKENS)
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByLabelText('Remover o token de produção configurado'))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).toEqual({ tokenProducao: '' })
  })

  it('token preenchido junto com Remover e barrado, sem enviar', async () => {
    prepara(COM_TOKENS)
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByLabelText('Remover o token de homologação configurado'))
    await user.type(tokenHom(), 'novo')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText(/Desmarque "Remover o token de homologação configurado"/)).toBeInTheDocument()
    expect(puts()).toHaveLength(0)
  })

  it('URL sem https:// e barrada no formulario, sem enviar', async () => {
    prepara(COM_TOKENS)
    const user = userEvent.setup()
    renderTela()
    const url = await urlHom()
    await user.clear(url)
    await user.type(url, 'http://inseguro.com')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('A URL deve começar com https://')).toBeInTheDocument()
    expect(puts()).toHaveLength(0)
  })

  it('erro url_invalida (400) do back aparece com mensagem e codigo', async () => {
    prepara(COM_TOKENS, async () => {
      throw new ApiError(400, 'url_invalida', 'A URL informada é inválida.')
    })
    const user = userEvent.setup()
    renderTela()
    const url = await urlHom()
    await user.type(url, 'x')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('A URL informada é inválida.')).toBeInTheDocument()
    expect(screen.getByText(/url_invalida/)).toBeInTheDocument()
  })

  it('erro producao_bloqueada (409) aparece com a mensagem do back', async () => {
    prepara({ ...COM_TOKENS, ambienteAtivo: 'producao' }, async () => {
      throw new ApiError(409, 'producao_bloqueada', 'Produção está bloqueada neste servidor.')
    })
    const user = userEvent.setup()
    renderTela()
    await user.type(await urlHom(), 'x')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('Produção está bloqueada neste servidor.')).toBeInTheDocument()
    expect(screen.getByText(/producao_bloqueada/)).toBeInTheDocument()
  })
})
