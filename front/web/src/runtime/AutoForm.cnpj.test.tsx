import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { z } from 'zod'
import { ApiError } from '@/lib/api'
import { AVISO_CNPJ, AVISO_CPF, AVISO_MUNICIPIO, AutoForm, type FieldLayout } from '@/runtime/AutoForm'

const apiMock = vi.hoisted(() => vi.fn())
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))

const schema = z.object({
  documento: z.string().optional(),
  razaoSocial: z.string().optional(),
  nomeFantasia: z.string().optional(),
  email: z.string().optional(),
  telefone: z.string().optional(),
  cep: z.string().optional(),
  logradouro: z.string().optional(),
  numeroEndereco: z.string().optional(),
  complemento: z.string().optional(),
  bairro: z.string().optional(),
  municipio: z.object({ id: z.string() }).optional(),
  regimeTributario: z.number().optional(),
  rgInscricaoEstadual: z.string().optional(),
  nome: z.string().optional(),
  tipoPessoa: z.enum(['FISICA', 'JURIDICA']).optional(),
  consumidorFinal: z.boolean().optional(),
})
type V = z.infer<typeof schema>

const fk = { key: 'Municipio', search: async () => [], labelOf: (v: Record<string, unknown>) => (v.nome ? `${v.nome} - ${v.uf}` : undefined) }

const FIELDS: FieldLayout = [
  { name: 'documento', label: 'CNPJ', mask: 'documento', lookup: 'cnpj' },
  { name: 'razaoSocial', label: 'Razão social' },
  { name: 'nomeFantasia', label: 'Nome fantasia' },
  { name: 'email', label: 'E-mail' },
  { name: 'telefone', label: 'Telefone', mask: 'phone' },
  { name: 'cep', label: 'CEP', mask: 'postalCode', lookup: 'cep' },
  { name: 'logradouro', label: 'Logradouro' },
  { name: 'numeroEndereco', label: 'Número' },
  { name: 'complemento', label: 'Complemento' },
  { name: 'bairro', label: 'Bairro' },
  { name: 'municipio', label: 'Município', fk },
  { name: 'regimeTributario', label: 'Regime tributário', options: [{ value: 1, label: 'Simples Nacional' }, { value: 3, label: 'Lucro Presumido' }] },
  { name: 'rgInscricaoEstadual', label: 'Inscrição estadual' },
]

/** Layout no estilo Cliente/Fornecedor: sem razaoSocial; com nome, tipoPessoa e consumidorFinal. */
const FIELDS_PESSOA: FieldLayout = [
  { name: 'documento', label: 'CNPJ', mask: 'documento', lookup: 'cnpj' },
  { name: 'tipoPessoa', label: 'Tipo de pessoa', options: [{ value: 'FISICA', label: 'Pessoa física' }, { value: 'JURIDICA', label: 'Pessoa jurídica' }] },
  { name: 'consumidorFinal', label: 'Consumidor final', kind: 'boolean' },
  { name: 'nome', label: 'Nome' },
  { name: 'nomeFantasia', label: 'Nome fantasia' },
]

const CPF = '52998224725'
const CNPJ = '27865757000102'
const GUAIRA = { id: 'mun-4108809', nome: 'Guaíra', uf: 'PR', codigoIbge: '4108809' }
const RESPOSTA = {
  cnpj: CNPJ,
  razaoSocial: 'EXEMPLO TECNOLOGIA E SISTEMAS LTDA',
  nomeFantasia: 'Exemplo Tech',
  email: 'contato@exemplotech.com.br',
  telefone: '4436421000',
  cep: '85980000',
  logradouro: 'Rua Marechal Deodoro',
  numeroEndereco: '1250',
  complemento: 'Sala 3',
  bairro: 'Centro',
  municipio: GUAIRA,
  uf: 'PR',
  codigoIbge: '4108809',
  regimeTributario: 1,
  inscricaoEstadual: '9012345678',
  situacaoCadastral: 'Ativa',
  dataInicioAtividade: '2017-03-14',
}
const DEFAULTS = { documento: '' } as V

function renderForm(fields: FieldLayout = FIELDS, onSubmit = vi.fn()) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <AutoForm schema={schema} fields={fields} defaults={DEFAULTS} onSubmit={onSubmit} />
    </QueryClientProvider>,
  )
  return onSubmit
}

beforeEach(() => apiMock.mockReset())

describe('AutoForm: lookup de CNPJ', () => {
  it('ao sair do campo com 14 digitos consulta e preenche os irmaos (municipio FK e regime inclusos)', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    expect(screen.getByLabelText('CNPJ')).toHaveValue('27.865.757/0001-02')
    expect(apiMock).not.toHaveBeenCalled()
    await user.tab()
    await waitFor(() => expect(screen.getByLabelText('Razão social')).toHaveValue('EXEMPLO TECNOLOGIA E SISTEMAS LTDA'))
    expect(apiMock).toHaveBeenCalledTimes(1)
    expect(apiMock).toHaveBeenCalledWith(`/consulta-cnpj/${CNPJ}`)
    expect(screen.getByLabelText('Nome fantasia')).toHaveValue('Exemplo Tech')
    expect(screen.getByLabelText('E-mail')).toHaveValue('contato@exemplotech.com.br')
    expect(screen.getByLabelText('Telefone')).toHaveValue('(44) 3642-1000')
    expect(screen.getByLabelText('CEP')).toHaveValue('85980-000')
    expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua Marechal Deodoro')
    expect(screen.getByLabelText('Número')).toHaveValue('1250')
    expect(screen.getByLabelText('Complemento')).toHaveValue('Sala 3')
    expect(screen.getByLabelText('Bairro')).toHaveValue('Centro')
    expect(screen.getByLabelText('Inscrição estadual')).toHaveValue('9012345678')
    expect(screen.getByRole('combobox', { name: /Município/ })).toHaveTextContent('Guaíra - PR')
    expect(screen.getByRole('combobox', { name: /Regime tributário/ })).toHaveTextContent('Simples Nacional')
    expect(screen.getByText(AVISO_CNPJ)).toBeInTheDocument()
    expect(apiMock).not.toHaveBeenCalledWith('/cep/85980000') // o endereco ja veio: nao dispara o CEP
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0]).toMatchObject({ documento: CNPJ, regimeTributario: 1, municipio: { id: 'mun-4108809' }, cep: '85980000', telefone: '4436421000' })
  })

  it('mostra o estado de carregando enquanto consulta', async () => {
    let release!: (v: unknown) => void
    apiMock.mockReturnValue(new Promise((r) => (release = r)))
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    expect(await screen.findByText('Consultando CNPJ…')).toBeInTheDocument()
    expect(screen.getByLabelText('CNPJ')).toHaveAttribute('aria-busy', 'true')
    release(RESPOSTA)
    await waitFor(() => expect(screen.queryByText('Consultando CNPJ…')).not.toBeInTheDocument())
  })

  it('botao Consultar CNPJ consulta sem sair do campo e repete mesmo sem mudanca', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.click(screen.getByRole('button', { name: 'Consultar CNPJ' }))
    await waitFor(() => expect(screen.getByLabelText('Bairro')).toHaveValue('Centro'))
    expect(apiMock).toHaveBeenCalledTimes(1)
    await user.click(screen.getByRole('button', { name: 'Consultar CNPJ' }))
    await waitFor(() => expect(apiMock).toHaveBeenCalledTimes(2))
  })

  it('Enter no campo consulta e nao envia o formulario', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await user.type(screen.getByLabelText('CNPJ'), `${CNPJ}{Enter}`)
    await waitFor(() => expect(screen.getByLabelText('Bairro')).toHaveValue('Centro'))
    expect(apiMock).toHaveBeenCalledWith(`/consulta-cnpj/${CNPJ}`)
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('incompleto ou CPF ao sair do campo nao chama a API nem mostra erro', async () => {
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CNPJ'), '278657')
    await user.tab()
    await user.clear(screen.getByLabelText('CNPJ'))
    await user.type(screen.getByLabelText('CNPJ'), '52998224725')
    await user.tab()
    expect(apiMock).not.toHaveBeenCalled()
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('botao com documento incompleto avisa no campo e nao chama a API', async () => {
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CNPJ'), '278657')
    await user.click(screen.getByRole('button', { name: 'Consultar CNPJ' }))
    expect(await screen.findByText('Informe os 14 dígitos do CNPJ.')).toBeInTheDocument()
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('digito verificador invalido: botao e Enter mostram "CNPJ inválido." sem chamar a API', async () => {
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CNPJ'), '27865757000103')
    await user.tab()
    expect(screen.queryByText('CNPJ inválido.')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Consultar CNPJ' }))
    expect(await screen.findByText('CNPJ inválido.')).toBeInTheDocument()
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('valor ausente na resposta nao apaga o digitado; complemento so se vazio', async () => {
    apiMock.mockResolvedValue({ cnpj: CNPJ, razaoSocial: 'NOVA RAZAO LTDA', nomeFantasia: null, logradouro: '', complemento: 'do back', municipio: GUAIRA, regimeTributario: null })
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('Nome fantasia'), 'Meu nome')
    await user.type(screen.getByLabelText('Logradouro'), 'Rua que eu digitei')
    await user.type(screen.getByLabelText('Complemento'), 'ap 3')
    await user.type(screen.getByLabelText('Razão social'), 'antiga')
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    await waitFor(() => expect(screen.getByLabelText('Razão social')).toHaveValue('NOVA RAZAO LTDA')) // presente sobrescreve
    expect(screen.getByLabelText('Nome fantasia')).toHaveValue('Meu nome')
    expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua que eu digitei')
    expect(screen.getByLabelText('Complemento')).toHaveValue('ap 3')
    expect(screen.getByRole('combobox', { name: /Regime tributário/ })).toHaveTextContent('Selecione')
  })

  it('municipio null: avisa junto com o aviso do CNPJ.ws e deixa a escolha manual', async () => {
    apiMock.mockResolvedValue({ ...RESPOSTA, municipio: null })
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    expect(await screen.findByText(`${AVISO_CNPJ} ${AVISO_MUNICIPIO}`)).toBeInTheDocument()
    expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua Marechal Deodoro')
    expect(screen.getByRole('combobox', { name: /Município/ })).toHaveTextContent('Selecione')
  })

  it.each([
    [new ApiError(404, 'cnpj_nao_encontrado', 'x'), 'CNPJ não encontrado no CNPJ.ws.'],
    [new ApiError(409, 'token_nao_configurado', 'x'), 'Configure o token do CNPJ.ws em Administração > Consulta de CNPJ.'],
    [new ApiError(502, 'token_recusado', 'x'), 'O CNPJ.ws recusou o token. Revise-o em Administração > Consulta de CNPJ.'],
    [new ApiError(429, 'limite_consulta_excedido', 'x'), 'Limite de consultas do CNPJ.ws excedido. Tente novamente mais tarde.'],
    [new ApiError(429, 'muitas_tentativas', 'x', undefined, 30), 'Muitas tentativas. Aguarde 30 segundos e tente novamente.'],
    [new ApiError(504, 'servico_cnpj_indisponivel', 'x'), 'Serviço de consulta de CNPJ indisponível. Tente novamente em instantes.'],
    [new ApiError(500, 'outro_erro', 'Mensagem do back.'), 'Mensagem do back.'],
  ])('erro %#: mensagem amigavel no campo, sem travar o formulario', async (erro, texto) => {
    apiMock.mockRejectedValueOnce(erro)
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    const alerta = await screen.findByText((t) => t.startsWith(texto.slice(0, 20)))
    expect(alerta).toHaveAttribute('role', 'alert')
    expect(alerta.textContent).toBe(texto)
    expect(screen.getByLabelText('CNPJ')).toHaveAttribute('aria-invalid', 'true')
    await user.type(screen.getByLabelText('Logradouro'), 'Rua X')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
  })

  it('formulario sem campos irmaos ignora o resultado', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm([{ name: 'documento', label: 'CNPJ', mask: 'documento', lookup: 'cnpj' }])
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    expect(await screen.findByText(AVISO_CNPJ)).toBeInTheDocument()
    expect(screen.queryByText(AVISO_MUNICIPIO)).not.toBeInTheDocument()
  })
})

describe('AutoForm: convencao de tipo por documento (CPF/CNPJ)', () => {
  const tipo = () => screen.getByRole('combobox', { name: /Tipo de pessoa/ })
  const consumidor = () => screen.getByRole('switch', { name: /Consumidor final/ })

  it('CPF valido ao sair do campo preenche fisica e consumidor final, sem API', async () => {
    const user = userEvent.setup()
    renderForm(FIELDS_PESSOA)
    await user.type(screen.getByLabelText('CNPJ'), CPF)
    await user.tab()
    await waitFor(() => expect(tipo()).toHaveTextContent('Pessoa física'))
    expect(consumidor()).toBeChecked()
    expect(screen.getByText(AVISO_CPF)).toBeInTheDocument()
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('CPF valido pelo botao e pelo Enter preenche sem API', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm(FIELDS_PESSOA)
    await user.type(screen.getByLabelText('CNPJ'), CPF)
    await user.click(screen.getByRole('button', { name: 'Consultar CNPJ' }))
    await waitFor(() => expect(consumidor()).toBeChecked())
    expect(tipo()).toHaveTextContent('Pessoa física')
    await user.click(consumidor()) // desmarca; Enter reaplica
    expect(consumidor()).not.toBeChecked()
    await user.type(screen.getByLabelText('CNPJ'), '{Enter}')
    await waitFor(() => expect(consumidor()).toBeChecked())
    expect(apiMock).not.toHaveBeenCalled()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('CPF invalido: ao sair nada; botao mostra "CPF inválido."', async () => {
    const user = userEvent.setup()
    renderForm(FIELDS_PESSOA)
    await user.type(screen.getByLabelText('CNPJ'), '52998224724')
    await user.tab()
    expect(screen.queryByText('CPF inválido.')).not.toBeInTheDocument()
    expect(tipo()).toHaveTextContent('Selecione')
    expect(consumidor()).not.toBeChecked()
    await user.click(screen.getByRole('button', { name: 'Consultar CNPJ' }))
    expect(await screen.findByText('CPF inválido.')).toBeInTheDocument()
    expect(tipo()).toHaveTextContent('Selecione')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('CNPJ com sucesso define juridica e nao consumidor final; razao social vai para nome', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm(FIELDS_PESSOA)
    await user.click(consumidor())
    expect(consumidor()).toBeChecked()
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    await waitFor(() => expect(screen.getByLabelText('Nome')).toHaveValue('EXEMPLO TECNOLOGIA E SISTEMAS LTDA'))
    expect(screen.getByLabelText('Nome fantasia')).toHaveValue('Exemplo Tech')
    expect(tipo()).toHaveTextContent('Pessoa jurídica')
    expect(consumidor()).not.toBeChecked()
    expect(apiMock).toHaveBeenCalledWith(`/consulta-cnpj/${CNPJ}`)
    expect(screen.getByText(AVISO_CNPJ)).toBeInTheDocument()
  })

  it('CNPJ com falha na consulta nao altera tipo nem consumidor final', async () => {
    apiMock.mockRejectedValueOnce(new ApiError(404, 'cnpj_nao_encontrado', 'x'))
    const user = userEvent.setup()
    renderForm(FIELDS_PESSOA)
    await user.click(consumidor())
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    expect(await screen.findByText('CNPJ não encontrado no CNPJ.ws.')).toBeInTheDocument()
    expect(tipo()).toHaveTextContent('Selecione')
    expect(consumidor()).toBeChecked()
  })

  it('layout sem tipoPessoa/consumidorFinal ignora a convencao (CPF sem aviso; CNPJ so preenche o resto)', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CNPJ'), CPF)
    await user.tab()
    expect(screen.queryByText(AVISO_CPF)).not.toBeInTheDocument()
    await user.clear(screen.getByLabelText('CNPJ'))
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
    await waitFor(() => expect(screen.getByLabelText('Razão social')).toHaveValue('EXEMPLO TECNOLOGIA E SISTEMAS LTDA'))
    expect(screen.getByText(AVISO_CNPJ)).toBeInTheDocument()
  })
})
