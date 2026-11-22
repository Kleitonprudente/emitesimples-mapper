import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { NfseConfig } from '@/features/empresa-config/NfseConfig'

const apiMock = vi.hoisted(() => vi.fn())
const toastMock = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))
const sessao = vi.hoisted(() => ({ atual: null as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))
vi.mock('sonner', () => ({ toast: toastMock }))

const URL_CFG = '/configuracao/nfse'
const DESABILITADA = {
  habilitada: false,
  serie: null,
  ultimoNumero: 0,
  proximoNumero: 1,
  ambiente: 'homologacao',
  statusCadastro: 'nao_cadastrado',
  dataCadastroGateway: null,
  emitenteTokenConfigurado: false,
  certificadoSituacao: 'nao_enviado',
  validadeCertificado: null,
  integracaoDisponivel: true,
}
const HABILITADA = {
  ...DESABILITADA,
  habilitada: true,
  serie: '1',
  ultimoNumero: 41,
  proximoNumero: 42,
  statusCadastro: 'cadastrado',
  dataCadastroGateway: '2026-11-01T10:30:00',
  emitenteTokenConfigurado: true,
  certificadoSituacao: 'valido',
  validadeCertificado: '2027-05-20',
}

function prepara(config: unknown, put: () => Promise<unknown> = async () => config) {
  apiMock.mockImplementation(async (_p: string, opts?: { method?: string }) => (opts?.method === 'PUT' ? put() : config))
}
const puts = () => apiMock.mock.calls.filter(([, o]) => o?.method === 'PUT')

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <NfseConfig />
    </QueryClientProvider>,
  )
}

const empresa = { id: 'emp-1', razaoSocial: 'Horizonte' }
const comPapel = (papel: string | null, plataformaAdmin = false) => ({ usuario: { id: 'u', nome: 'Ana', email: 'a@x.com' }, empresa: papel ? empresa : null, papel, plataformaAdmin })

const serie = () => screen.findByLabelText(/Série/)
const ultimo = () => screen.getByLabelText(/Último número/)
const chave = () => screen.getByRole('switch', { name: 'Habilitar a emissão de NFS-e' })
const salvar = (nome = 'Salvar') => screen.getByRole('button', { name: nome })

async function preencheEHabilita(user: ReturnType<typeof userEvent.setup>, s: string, n: string) {
  const campoSerie = await serie()
  await user.click(chave())
  await user.type(campoSerie, s)
  fireEvent.change(ultimo(), { target: { value: n } })
}

beforeEach(() => {
  apiMock.mockReset()
  toastMock.success.mockReset()
  sessao.atual = comPapel('administrador')
})

describe('NfseConfig', () => {
  it.each([
    ['operador', comPapel('operador')],
    ['consulta', comPapel('consulta')],
    ['plataforma sem empresa', comPapel(null, true)],
  ])('%s ve acesso restrito e nao chama a API', (_n, s) => {
    sessao.atual = s
    renderTela()
    expect(screen.getByRole('alert')).toHaveTextContent('Acesso restrito')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('carrega e mostra aviso, status em Badges, ambiente, numeracao e secao', async () => {
    prepara(HABILITADA)
    renderTela()
    expect(await serie()).toHaveValue('1')
    expect(apiMock).toHaveBeenCalledWith(URL_CFG)
    expect(screen.getByText(/Ao habilitar a emissão, o sistema cadastra a sua empresa como emitente na IntegraNotas/)).toBeInTheDocument()
    expect(screen.getByText('Emissão habilitada')).toBeInTheDocument()
    expect(screen.getByText(/Cadastro no gateway: cadastrado em/)).toBeInTheDocument()
    expect(screen.getByText(/Certificado: válido \(validade 20\/05\/2027\)/)).toBeInTheDocument()
    expect(screen.getByText('Homologação')).toBeInTheDocument()
    expect(screen.getByText('Emissão de NFS-e')).toBeInTheDocument()
    expect(ultimo()).toHaveValue(41)
    expect(screen.getByText('Próximo número: 42')).toBeInTheDocument()
    expect(screen.getByText('Série da DPS, 1 a 5 dígitos.')).toBeInTheDocument()
    expect(chave()).toBeChecked()
    expect(screen.queryByText(/não está configurada neste servidor/)).not.toBeInTheDocument()
  })

  it('desabilitada sem cadastro: badges neutros e ambiente producao como Producao', async () => {
    prepara({ ...DESABILITADA, ambiente: 'producao' })
    renderTela()
    await serie()
    expect(screen.getByText('Emissão desabilitada')).toBeInTheDocument()
    expect(screen.getByText('Cadastro no gateway: não cadastrado')).toBeInTheDocument()
    expect(screen.getByText('Certificado: não enviado')).toBeInTheDocument()
    expect(screen.getByText('Produção')).toBeInTheDocument()
  })

  it('certificado vencido e cadastro com erro aparecem como tal', async () => {
    prepara({ ...DESABILITADA, statusCadastro: 'erro', certificadoSituacao: 'vencido', validadeCertificado: '2026-01-10' })
    renderTela()
    await serie()
    expect(screen.getByText('Cadastro no gateway: com erro')).toBeInTheDocument()
    expect(screen.getByText(/Certificado: vencido \(validade 10\/01\/2026\)/)).toBeInTheDocument()
  })

  it('mostra erro de carregamento', async () => {
    apiMock.mockRejectedValue(new ApiError(500, 'erro', 'Falha.'))
    renderTela()
    expect(await screen.findByText('Não foi possível carregar a configuração.')).toBeInTheDocument()
  })

  it('habilitar envia o payload certo, avisa e relê', async () => {
    prepara(DESABILITADA)
    const user = userEvent.setup()
    renderTela()
    await preencheEHabilita(user, '1', '10')
    await user.click(salvar('Salvar e habilitar'))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]).toEqual([URL_CFG, { method: 'PUT', body: { habilitada: true, serie: '1', ultimoNumero: 10 } }])
    await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('NFS-e habilitada.'))
    await waitFor(() => expect(apiMock.mock.calls.filter(([, o]) => !o?.method).length).toBeGreaterThanOrEqual(2))
  })

  it('durante o envio mostra Cadastrando na IntegraNotas', async () => {
    let solta: (v: unknown) => void = () => {}
    prepara(DESABILITADA, () => new Promise((r) => (solta = r)))
    const user = userEvent.setup()
    renderTela()
    await preencheEHabilita(user, '1', '0')
    await user.click(salvar('Salvar e habilitar'))
    expect(await screen.findByText('Cadastrando na IntegraNotas…')).toBeInTheDocument()
    expect(salvar('Salvar e habilitar')).toBeDisabled()
    solta(HABILITADA)
    await waitFor(() => expect(screen.queryByText('Cadastrando na IntegraNotas…')).not.toBeInTheDocument())
  })

  it('"Salvar e habilitar" so na primeira vez', async () => {
    prepara(DESABILITADA)
    renderTela()
    await serie()
    expect(salvar('Salvar e habilitar')).toBeInTheDocument()
  })

  it('ja cadastrada e desabilitada, ou habilitada: botao Salvar e toast generico', async () => {
    prepara({ ...HABILITADA, habilitada: false })
    const user = userEvent.setup()
    renderTela()
    await serie()
    expect(screen.queryByRole('button', { name: 'Salvar e habilitar' })).not.toBeInTheDocument()
    await user.click(chave())
    await user.click(salvar())
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).toEqual({ habilitada: true, serie: '1', ultimoNumero: 41 })
    await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('NFS-e habilitada.'))
  })

  it('serie vazia ao habilitar e barrada, sem enviar', async () => {
    prepara(DESABILITADA)
    const user = userEvent.setup()
    renderTela()
    await serie()
    await user.click(chave())
    await user.click(salvar('Salvar e habilitar'))
    expect(await screen.findByText('Informe a série para habilitar a emissão.')).toBeInTheDocument()
    expect(puts()).toHaveLength(0)
  })

  it('serie com letras e barrada; numero vazio ou negativo tambem', async () => {
    prepara(DESABILITADA)
    const user = userEvent.setup()
    renderTela()
    await preencheEHabilita(user, 'ab', '')
    await user.click(salvar('Salvar e habilitar'))
    expect(await screen.findByText('A série deve ter de 1 a 5 dígitos.')).toBeInTheDocument()
    expect(screen.getByText('Informe o último número (0 se nunca emitiu).')).toBeInTheDocument()
    fireEvent.change(ultimo(), { target: { value: '-3' } })
    await user.click(salvar('Salvar e habilitar'))
    expect(await screen.findByText('O último número deve ser um inteiro entre 0 e 999999999.')).toBeInTheDocument()
    expect(puts()).toHaveLength(0)
  })

  it('desabilitar envia habilitada false com a numeracao salva', async () => {
    prepara(HABILITADA, async () => ({ ...HABILITADA, habilitada: false }))
    const user = userEvent.setup()
    renderTela()
    await serie()
    await user.click(chave())
    await user.click(salvar())
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).toEqual({ habilitada: false, serie: '1', ultimoNumero: 41 })
    await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith('Configuração da NFS-e salva.'))
  })

  it('integracao indisponivel: aviso, nao deixa habilitar; desabilitar continua', async () => {
    prepara({ ...DESABILITADA, integracaoDisponivel: false })
    renderTela()
    await serie()
    expect(screen.getByText('A integração com a IntegraNotas não está configurada neste servidor. Fale com o suporte.')).toBeInTheDocument()
    expect(chave()).toBeDisabled()
  })

  it('integracao indisponivel mas ja habilitada: pode desabilitar', async () => {
    prepara({ ...HABILITADA, integracaoDisponivel: false })
    const user = userEvent.setup()
    renderTela()
    await serie()
    expect(chave()).toBeEnabled()
    await user.click(chave())
    await user.click(salvar())
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body.habilitada).toBe(false)
  })

  describe('erros do back', () => {
    async function falha(e: ApiError) {
      prepara(DESABILITADA, async () => {
        throw e
      })
      const user = userEvent.setup()
      renderTela()
      await preencheEHabilita(user, '1', '0')
      await user.click(salvar('Salvar e habilitar'))
    }
    const alerta = async (texto: string) => expect(await screen.findByText(texto)).toBeInTheDocument()

    it('422 cadastro_incompleto lista os campos em portugues', async () => {
      await falha(new ApiError(422, 'cadastro_incompleto', 'Cadastro incompleto.', { campos: [{ campo: 'inscricaoMunicipal', mensagem: 'x' }, { campo: 'cep', mensagem: 'y' }] }))
      await alerta('Faltam dados da empresa: inscrição municipal, CEP.')
    })
    it('502 integranotas_recusou inclui a mensagem do back', async () => {
      await falha(new ApiError(502, 'integranotas_recusou', 'CNPJ já cadastrado.'))
      await alerta('A IntegraNotas recusou o cadastro: CNPJ já cadastrado.')
    })
    it('502 integranotas_token_recusado', async () => {
      await falha(new ApiError(502, 'integranotas_token_recusado', 'x'))
      await alerta('A IntegraNotas recusou o token da integração. Fale com o suporte.')
    })
    it('504 integranotas_indisponivel', async () => {
      await falha(new ApiError(504, 'integranotas_indisponivel', 'x'))
      await alerta('A IntegraNotas não respondeu a tempo. Tente de novo.')
    })
    it('409 numeracao_invalida', async () => {
      await falha(new ApiError(409, 'numeracao_invalida', 'x'))
      await alerta('A numeração informada conflita com notas já emitidas.')
    })
    it('409 integranotas_nao_configurada', async () => {
      await falha(new ApiError(409, 'integranotas_nao_configurada', 'x'))
      await alerta('A integração com a IntegraNotas não está configurada neste servidor. Fale com o suporte.')
    })
    it('codigo desconhecido cai na mensagem do back; erro nao gera toast de sucesso', async () => {
      await falha(new ApiError(403, 'acesso_negado', 'Sem permissão para isto.'))
      await alerta('Sem permissão para isto.')
      expect(toastMock.success).not.toHaveBeenCalled()
    })
  })
})
