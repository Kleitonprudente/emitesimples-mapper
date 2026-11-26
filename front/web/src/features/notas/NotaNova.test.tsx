import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { NotaNova } from '@/features/notas/NotaNova'

const apiMock = vi.hoisted(() => vi.fn())
const navegar = vi.hoisted(() => vi.fn())
const toastMock = vi.hoisted(() => ({ success: vi.fn(), error: vi.fn() }))
const sessao = vi.hoisted(() => ({ atual: null as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))
vi.mock('@tanstack/react-router', async (orig) => ({ ...(await orig<typeof import('@tanstack/react-router')>()), useNavigate: () => navegar }))
vi.mock('sonner', () => ({ toast: toastMock }))

const CFG_OK = {
  habilitada: true, serie: '1', ultimoNumero: 9, proximoNumero: 10, ambiente: 'HOMOLOGACAO', statusCadastro: 'CADASTRADO',
  emitenteTokenConfigurado: true, certificadoSituacao: 'VALIDO', integracaoDisponivel: true,
}
const NOTA = {
  id: 'n-novo', numero: 10, serie: '1', estado: 'PROCESSANDO', valorServicos: 100, pessoa: { id: 'p1', nome: 'Ana', documento: '12345678909' },
  servico: { id: 's1', nome: 'Consultoria' }, temPdf: false, discriminacao: 'x', errosRetorno: null, editavel: false, podeReenviar: false, podeAtualizar: true,
}
const PADRAO = 'Texto padrão do serviço.'

function prepara(cfg: object = CFG_OK, post: () => Promise<unknown> = async () => NOTA) {
  apiMock.mockImplementation(async (path: string, opts?: { method?: string }) => {
    if (path === '/configuracao/nfse') return cfg
    if (path === '/Pessoa/0/49') return [{ id: 'p1', nome: 'Ana Paula', documento: '12345678909' }]
    if (path === '/Servico/0/49') return [{ id: 's1', nome: 'Consultoria' }, { id: 's2', nome: 'Software' }]
    if (path.startsWith('/Servico/id/')) return { id: path.slice(-2), discriminacaoPadrao: path.endsWith('s1') ? PADRAO : 'Outro texto.' }
    if (path === '/nfse/notas' && opts?.method === 'POST') return post()
    return undefined
  })
}
const posts = () => apiMock.mock.calls.filter(([p, o]) => p === '/nfse/notas' && o?.method === 'POST')

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <NotaNova />
    </QueryClientProvider>,
  )
}
const com = (papel: string) => ({ usuario: { id: 'u', nome: 'Ana', email: 'a@x.com' }, empresa: { id: 'e', razaoSocial: 'H' }, papel, plataformaAdmin: false })

const enviar = () => screen.getByRole('button', { name: 'Enviar' })
const descricao = () => screen.getByLabelText(/Descrição do serviço/) as HTMLTextAreaElement

async function escolher(user: ReturnType<typeof userEvent.setup>, rotulo: RegExp, opcao: string) {
  await user.click(await screen.findByRole('combobox', { name: rotulo }))
  await user.click(await screen.findByRole('option', { name: new RegExp(opcao) }))
}
async function preenche(user: ReturnType<typeof userEvent.setup>, valor = '100') {
  await escolher(user, /Cliente/, 'Ana Paula')
  await escolher(user, /Serviço/, 'Consultoria')
  fireEvent.change(screen.getByLabelText(/Valor dos serviços/), { target: { value: valor } })
  await waitFor(() => expect(descricao().value).toBe(PADRAO))
}

beforeEach(() => {
  apiMock.mockReset()
  navegar.mockReset()
  toastMock.success.mockReset()
  sessao.atual = com('OPERADOR')
})

describe('NotaNova', () => {
  it('consulta ve acesso restrito e nao chama a API', () => {
    sessao.atual = com('CONSULTA')
    renderTela()
    expect(screen.getByRole('alert')).toHaveTextContent('Acesso restrito')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('mostra o formulario com a secao Nota, o ambiente Homologacao e Enviar habilitado', async () => {
    prepara()
    renderTela()
    expect(await screen.findByText('Nota')).toBeInTheDocument()
    expect(screen.getByText('Homologação')).toBeInTheDocument()
    expect(enviar()).toBeEnabled()
    expect(screen.queryByText(/não está habilitada/)).not.toBeInTheDocument()
  })

  it('NFS-e nao habilitada: alerta e Enviar desabilitado', async () => {
    prepara({ ...CFG_OK, habilitada: false })
    renderTela()
    expect(await screen.findByText('A emissão de NFS-e não está habilitada. Habilite em Configuração > NFS-e.')).toBeInTheDocument()
    expect(enviar()).toBeDisabled()
  })

  it('certificado nao valido: alerta e Enviar desabilitado', async () => {
    prepara({ ...CFG_OK, certificadoSituacao: 'VENCIDO' })
    renderTela()
    expect(await screen.findByText('O certificado digital não está válido. Envie em Configuração > Empresa.')).toBeInTheDocument()
    expect(enviar()).toBeDisabled()
  })

  it('emitente nao cadastrado e integracao indisponivel tambem bloqueiam', async () => {
    prepara({ ...CFG_OK, statusCadastro: 'ERRO', integracaoDisponivel: false })
    renderTela()
    expect(await screen.findByText(/ainda não está cadastrada como emitente/)).toBeInTheDocument()
    expect(screen.getByText(/integração com a IntegraNotas não está configurada/)).toBeInTheDocument()
    expect(enviar()).toBeDisabled()
  })

  it('ambiente de producao aparece como Producao', async () => {
    prepara({ ...CFG_OK, ambiente: 'PRODUCAO' })
    renderTela()
    expect(await screen.findByText('Produção')).toBeInTheDocument()
  })

  it('formulario vazio: campos obrigatorios barrados, sem enviar', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela()
    await screen.findByText('Nota')
    await user.click(enviar())
    expect((await screen.findAllByText('Campo obrigatório')).length).toBeGreaterThanOrEqual(4)
    expect(posts()).toHaveLength(0)
  })

  it('valor menor que R$ 0,01 e barrado', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela()
    await preenche(user, '0')
    await user.click(enviar())
    expect(await screen.findByText('O valor deve ser de pelo menos R$ 0,01.')).toBeInTheDocument()
    expect(posts()).toHaveLength(0)
  })

  it('escolher o servico preenche a descricao; a troca de servico reaplica enquanto nao foi editada', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela()
    await preenche(user)
    await escolher(user, /Serviço/, 'Software')
    await waitFor(() => expect(descricao().value).toBe('Outro texto.'))
  })

  it('descricao editada pela pessoa nao e sobrescrita ao trocar o servico', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela()
    await preenche(user)
    fireEvent.change(descricao(), { target: { value: 'Minha descrição' } })
    await escolher(user, /Serviço/, 'Software')
    await waitFor(() => expect(apiMock).toHaveBeenCalledWith('/Servico/id/s2'))
    expect(descricao().value).toBe('Minha descrição')
  })

  it('envia o corpo certo, avisa e navega para o detalhe da nota devolvida', async () => {
    prepara()
    const user = userEvent.setup()
    renderTela()
    await preenche(user, '150.5')
    fireEvent.change(screen.getByLabelText(/Informações complementares/), { target: { value: ' Pedido 7 ' } })
    await user.click(enviar())
    await waitFor(() => expect(posts()).toHaveLength(1))
    expect(posts()[0]![1].body).toEqual({ pessoaId: 'p1', servicoId: 's1', valorServicos: 150.5, discriminacao: PADRAO, informacoesComplementares: 'Pedido 7', tributos: { issRetido: false, cstPisCofins: null, aliquotaIss: null, valorBaseCalculoIss: null, valorIssRetido: null, tipoRetencao: null, valorBaseCalculo: null, aliquotaPis: null, aliquotaCofins: null, valorPis: null, valorCofins: null, valorInss: null, valorIr: null, valorCsll: null, valorDescontoIncondicionado: null } })
    await waitFor(() => expect(navegar).toHaveBeenCalledWith({ to: '/notas/$id', params: { id: 'n-novo' } }))
    expect(toastMock.success).toHaveBeenCalledWith('Nota enviada.')
  })

  it('durante o envio mostra Enviando a IntegraNotas e desabilita Enviar', async () => {
    let solta: (v: unknown) => void = () => {}
    prepara(CFG_OK, () => new Promise((r) => (solta = r)))
    const user = userEvent.setup()
    renderTela()
    await preenche(user)
    await user.click(enviar())
    expect(await screen.findByText('Enviando à IntegraNotas…')).toBeInTheDocument()
    expect(enviar()).toBeDisabled()
    solta(NOTA)
    await waitFor(() => expect(screen.queryByText('Enviando à IntegraNotas…')).not.toBeInTheDocument())
  })

  describe('erros do back', () => {
    async function falha(e: ApiError) {
      prepara(CFG_OK, async () => {
        throw e
      })
      const user = userEvent.setup()
      renderTela()
      await preenche(user)
      await user.click(enviar())
    }
    it.each([
      ['nfse_nao_habilitada', 409, 'A emissão de NFS-e não está habilitada. Habilite em Configuração > NFS-e.'],
      ['emitente_nao_cadastrado', 409, 'A empresa ainda não está cadastrada como emitente na IntegraNotas. Conclua em Configuração > NFS-e.'],
      ['emitente_de_outro_ambiente', 409, 'O cadastro do emitente é de outro ambiente (homologação ou produção). Refaça o cadastro em Configuração > NFS-e.'],
      ['certificado_nao_valido', 409, 'O certificado digital não está válido. Envie em Configuração > Empresa.'],
      ['servico_inativo', 409, 'O serviço escolhido está inativo. Escolha outro serviço.'],
      ['discriminacao_obrigatoria', 400, 'Informe a descrição do serviço.'],
    ])('%s mostra a mensagem em portugues', async (codigo, status, texto) => {
      await falha(new ApiError(status, codigo, 'texto do back'))
      expect(await screen.findByText(texto)).toBeInTheDocument()
      expect(navegar).not.toHaveBeenCalled()
    })

    it('tomador_incompleto lista os campos faltantes e orienta completar o cadastro', async () => {
      await falha(new ApiError(422, 'tomador_incompleto', 'x', { campos: [{ campo: 'documento', mensagem: 'a' }, { campo: 'cep', mensagem: 'b' }, 'municipio'] }))
      expect(await screen.findByText('Faltam dados do cliente: CPF/CNPJ, CEP, município. Complete o cadastro do cliente.')).toBeInTheDocument()
    })

    it('requisicao_invalida lista os campos; codigo desconhecido cai na mensagem do back', async () => {
      await falha(new ApiError(400, 'requisicao_invalida', 'x', { campos: { valorServicos: 'm' } }))
      expect(await screen.findByText('Revise os campos: valor dos serviços.')).toBeInTheDocument()
    })

    it('429 e codigo desconhecido usam a mensagem do back', async () => {
      await falha(new ApiError(500, 'algo_novo', 'Falha inesperada no gateway.'))
      expect(await screen.findByText('Falha inesperada no gateway.')).toBeInTheDocument()
    })
  })
})
