import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { ConsultaCnpjAdmin } from '@/features/admin/ConsultaCnpjAdmin'

const apiMock = vi.hoisted(() => vi.fn())
const sessao = vi.hoisted(() => ({ atual: { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true } as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))

const URL_CFG = '/admin/configuracoes/consulta-cnpj'
const COM_TOKEN = { urlBase: 'https://comercial.cnpj.ws/cnpj', tokenConfigurado: true, dataAtualizacao: '2026-11-01T10:30:00', atualizadoPor: { id: 'u1', nome: 'Eva Plataforma' } }
const SEM_TOKEN = { urlBase: 'https://comercial.cnpj.ws/cnpj', tokenConfigurado: false, dataAtualizacao: null, atualizadoPor: null }

/** GET devolve `config`; PUT usa `put` (resolve por padrao). */
function prepara(config: unknown, put: () => Promise<unknown> = async () => undefined, testar: () => Promise<unknown> = async () => RESUMO) {
  apiMock.mockImplementation(async (p: string, opts?: { method?: string }) => {
    if (opts?.method === 'PUT') return put()
    if (opts?.method === 'POST' && p === URL_TESTAR) return testar()
    return config
  })
}
const puts = () => apiMock.mock.calls.filter(([, o]) => o?.method === 'PUT')
const testes = () => apiMock.mock.calls.filter(([p]) => p === URL_TESTAR)

const URL_TESTAR = '/admin/configuracoes/consulta-cnpj/testar'
const RESUMO = {
  cnpj: '27865757000102',
  razaoSocial: 'EXEMPLO TECNOLOGIA E SISTEMAS LTDA',
  nomeFantasia: null,
  situacaoCadastral: 'ATIVA',
  dataInicioAtividade: '2017-03-14',
  porte: 'Microempresa',
  naturezaJuridica: 'Sociedade Empresária Limitada',
  atividadePrincipal: { codigo: '6201-5/01', descricao: 'Desenvolvimento de programas de computador sob encomenda' },
  capitalSocial: 50000,
  endereco: { logradouro: 'Rua Marechal Deodoro', numero: '1250', complemento: 'Sala 3', bairro: 'Centro', cep: '85980000', municipio: 'Guaíra', uf: 'PR' },
  email: null,
  telefone: '4436421000',
  simples: true,
  mei: null,
  quantidadeSocios: 2,
  duracaoMs: 412,
  urlConsultada: 'https://comercial.cnpj.ws/cnpj/27865757000102',
}
const CNPJ_OK = '27.865.757/0001-02'

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <ConsultaCnpjAdmin />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  apiMock.mockReset()
  sessao.atual = { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true }
})

describe('ConsultaCnpjAdmin', () => {
  it('quem nao e administrador da plataforma ve acesso restrito', () => {
    sessao.atual = { ...(sessao.atual as object), plataformaAdmin: false }
    renderTela()
    expect(screen.getByRole('alert')).toHaveTextContent('Acesso restrito')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('carrega a configuracao: URL, badge, hint e ultima atualizacao', async () => {
    prepara(COM_TOKEN)
    renderTela()
    expect(await screen.findByLabelText(/URL \/ endpoint/)).toHaveValue('https://comercial.cnpj.ws/cnpj')
    expect(apiMock).toHaveBeenCalledWith(URL_CFG)
    expect(screen.getByText('Token configurado')).toBeInTheDocument()
    expect(screen.getByText('Token configurado. Deixe em branco para manter.')).toBeInTheDocument()
    expect(screen.getByLabelText('Remover o token configurado')).toBeInTheDocument()
    expect(screen.getByLabelText('Token', { selector: 'input' })).toHaveValue('')
    expect(screen.getByText(/Última atualização por Eva Plataforma em/)).toBeInTheDocument()
  })

  it('sem token: badge neutro, hint de colar e sem opcao de remover', async () => {
    prepara(SEM_TOKEN)
    renderTela()
    await screen.findByLabelText(/URL \/ endpoint/)
    expect(screen.getByText('Sem token')).toBeInTheDocument()
    expect(screen.getByText('Cole o token do plano comercial do CNPJ.ws.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Remover o token configurado')).not.toBeInTheDocument()
    expect(screen.queryByText(/Última atualização/)).not.toBeInTheDocument()
  })

  it('mostra erro de carregamento', async () => {
    apiMock.mockRejectedValue(new ApiError(500, 'erro', 'Falha.'))
    renderTela()
    expect(await screen.findByText('Não foi possível carregar a configuração.')).toBeInTheDocument()
  })

  it('salvar sem token envia so urlBase, mostra toast e refaz a leitura', async () => {
    prepara(COM_TOKEN)
    const user = userEvent.setup()
    renderTela()
    const url = await screen.findByLabelText(/URL \/ endpoint/)
    await user.clear(url)
    await user.type(url, 'https://novo.cnpj.ws/cnpj')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]).toEqual([URL_CFG, { method: 'PUT', body: { urlBase: 'https://novo.cnpj.ws/cnpj' } }])
    await waitFor(() => expect(apiMock.mock.calls.filter(([, o]) => !o?.method).length).toBeGreaterThanOrEqual(2))
  })

  it('com token envia o token e o campo fica vazio depois de salvar', async () => {
    prepara(SEM_TOKEN)
    const user = userEvent.setup()
    renderTela()
    const token = await screen.findByLabelText('Token', { selector: 'input' })
    await user.type(token, 'segredo123')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).toEqual({ urlBase: 'https://comercial.cnpj.ws/cnpj', token: 'segredo123' })
    await waitFor(() => expect(screen.getByLabelText('Token', { selector: 'input' })).toHaveValue(''))
  })

  it('"Remover o token configurado" envia token vazio', async () => {
    prepara(COM_TOKEN)
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByLabelText('Remover o token configurado'))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(puts()).toHaveLength(1))
    expect(puts()[0]![1].body).toEqual({ urlBase: 'https://comercial.cnpj.ws/cnpj', token: '' })
  })

  it('URL sem https:// e barrada no formulario, sem enviar', async () => {
    prepara(COM_TOKEN)
    const user = userEvent.setup()
    renderTela()
    const url = await screen.findByLabelText(/URL \/ endpoint/)
    await user.clear(url)
    await user.type(url, 'http://inseguro.com')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('A URL deve começar com https://')).toBeInTheDocument()
    expect(puts()).toHaveLength(0)
  })

  it('erro url_invalida do back aparece com mensagem e codigo', async () => {
    prepara(COM_TOKEN, async () => {
      throw new ApiError(400, 'url_invalida', 'A URL informada é inválida.')
    })
    const user = userEvent.setup()
    renderTela()
    await screen.findByLabelText(/URL \/ endpoint/)
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByText('A URL informada é inválida.')).toBeInTheDocument()
    expect(screen.getByText(/url_invalida/)).toBeInTheDocument()
  })

  describe('Testar consulta', () => {
    const campo = () => screen.findByRole('searchbox', { name: 'Consultar CNPJ' })
    const consultar = async (user: ReturnType<typeof userEvent.setup>, cnpj: string) => {
      await user.type(await campo(), cnpj)
      await user.click(screen.getAllByRole('button', { name: 'Consultar CNPJ' })[0]!)
    }

    it('a secao aparece com o texto de apoio e o campo habilitado', async () => {
      prepara(COM_TOKEN)
      renderTela()
      expect(await screen.findByRole('heading', { name: 'Testar consulta' })).toBeInTheDocument()
      expect(screen.getByText(/usa a configuração já salva/)).toBeInTheDocument()
      expect(await campo()).toBeEnabled()
      expect(await campo()).toHaveAttribute('placeholder', '00.000.000/0000-00')
    })

    it('sem token a secao fica desabilitada e avisa', async () => {
      prepara(SEM_TOKEN)
      renderTela()
      expect(await screen.findByText('Configure e salve o token para testar.')).toBeInTheDocument()
      expect(await campo()).toBeDisabled()
    })

    it('CNPJ invalido e barrado no cliente, sem chamar o back', async () => {
      prepara(COM_TOKEN)
      const user = userEvent.setup()
      renderTela()
      await consultar(user, '11.111.111/1111-11')
      expect(await screen.findByText('CNPJ inválido.')).toBeInTheDocument()
      expect(testes()).toHaveLength(0)
    })

    it('sucesso: envia so digitos e mostra razao social, badge, endereco e rodape', async () => {
      prepara(COM_TOKEN)
      const user = userEvent.setup()
      renderTela()
      await consultar(user, CNPJ_OK)
      expect(await screen.findByText('Resultado da consulta')).toBeInTheDocument()
      expect(testes()).toEqual([[URL_TESTAR, { method: 'POST', body: { cnpj: '27865757000102' } }]])
      expect(screen.getByText('EXEMPLO TECNOLOGIA E SISTEMAS LTDA')).toBeInTheDocument()
      expect(screen.getByText('27.865.757/0001-02')).toBeInTheDocument()
      expect(screen.getByText('ATIVA').className).toContain('text-success')
      expect(screen.getByText('Rua Marechal Deodoro, 1250 - Sala 3 - Centro - Guaíra/PR - 85980-000')).toBeInTheDocument()
      expect(screen.getByText('6201-5/01 - Desenvolvimento de programas de computador sob encomenda')).toBeInTheDocument()
      expect(screen.getByText('(44) 3642-1000')).toBeInTheDocument()
      expect(screen.getByText('14/03/2017')).toBeInTheDocument()
      expect(screen.getByText('Sim')).toBeInTheDocument()
      expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(3)
      expect(screen.getByText(/Consulta em 412 ms · https:\/\/comercial\.cnpj\.ws\/cnpj\/27865757000102/)).toBeInTheDocument()
    })

    it('situacao diferente de Ativa usa badge de aviso', async () => {
      prepara(COM_TOKEN, undefined, async () => ({ ...RESUMO, situacaoCadastral: 'Baixada' }))
      const user = userEvent.setup()
      renderTela()
      await consultar(user, CNPJ_OK)
      expect((await screen.findByText('Baixada')).className).toContain('bg-warning')
    })

    it.each([
      ['token_recusado', 502, 'O CNPJ.ws recusou o token. Confira o token salvo.'],
      ['limite_consulta_excedido', 429, 'Limite de consultas do CNPJ.ws atingido. Aguarde e tente de novo.'],
      ['servico_cnpj_indisponivel', 504, 'O CNPJ.ws não respondeu a tempo.'],
      ['cnpj_nao_encontrado', 404, 'CNPJ não encontrado no CNPJ.ws.'],
      ['codigo_novo', 500, 'Mensagem do back.'],
    ])('erro %s mostra mensagem em portugues', async (codigo, status, esperado) => {
      prepara(COM_TOKEN, undefined, async () => {
        throw new ApiError(status, codigo, 'Mensagem do back.')
      })
      const user = userEvent.setup()
      renderTela()
      await consultar(user, CNPJ_OK)
      expect(await screen.findByText(esperado)).toBeInTheDocument()
      expect(screen.queryByText('Resultado da consulta')).not.toBeInTheDocument()
    })

    it('alteracoes nao salvas no formulario mostram o alerta', async () => {
      prepara(COM_TOKEN)
      const user = userEvent.setup()
      renderTela()
      expect(screen.queryByText(/Há alterações não salvas/)).not.toBeInTheDocument()
      const url = await screen.findByLabelText(/URL \/ endpoint/)
      await user.type(url, 'x')
      expect(await screen.findByText('Há alterações não salvas: a consulta usa a configuração já salva.')).toBeInTheDocument()
      await user.type(url, '{Backspace}')
      await waitFor(() => expect(screen.queryByText(/Há alterações não salvas/)).not.toBeInTheDocument())
    })

    it('limpar o campo remove o resultado', async () => {
      prepara(COM_TOKEN)
      const user = userEvent.setup()
      renderTela()
      await consultar(user, CNPJ_OK)
      expect(await screen.findByText('Resultado da consulta')).toBeInTheDocument()
      await user.click(screen.getByRole('button', { name: 'Limpar busca' }))
      expect(screen.queryByText('Resultado da consulta')).not.toBeInTheDocument()
    })
  })
})
