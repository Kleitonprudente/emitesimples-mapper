import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { z } from 'zod'
import { ApiError } from '@/lib/api'
import { AVISO_MUNICIPIO, AutoForm, type FieldLayout } from '@/runtime/AutoForm'

const apiMock = vi.hoisted(() => vi.fn())
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))

const schema = z.object({
  cep: z.string().optional(),
  logradouro: z.string().optional(),
  numero: z.string().optional(),
  complemento: z.string().optional(),
  bairro: z.string().optional(),
  cidade: z.object({ id: z.string() }).optional(),
})
type V = z.infer<typeof schema>

const fk = { key: 'Municipio', search: async () => [], labelOf: (v: Record<string, unknown>) => (v.nome ? `${v.nome} - ${v.uf}` : undefined) }

const FIELDS: FieldLayout = [
  [
    { name: 'cep', label: 'CEP', mask: 'postalCode', lookup: 'cep', hint: 'Ao sair do campo, o endereço é preenchido.', width: 30 },
    { name: 'logradouro', label: 'Logradouro', width: 55 },
    { name: 'numero', label: 'Número', width: 15 },
  ],
  [
    { name: 'complemento', label: 'Complemento' },
    { name: 'bairro', label: 'Bairro' },
    { name: 'cidade', label: 'Município', fk },
  ],
]

const GUAIRA = { id: 'mun-4108809', nome: 'Guaíra', uf: 'PR', codigoIbge: '4108809' }
const RESPOSTA = { cep: '85980000', logradouro: 'Rua Quinze de Novembro', complemento: 'lado par', bairro: 'Centro', uf: 'PR', codigoIbge: '4108809', municipio: GUAIRA }
const DEFAULTS = { cep: '', logradouro: '', numero: '', complemento: '', bairro: '' } as V

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

describe('AutoForm: lookup de CEP', () => {
  it('mostra a dica ligada ao campo por aria-describedby', () => {
    renderForm()
    const cep = screen.getByLabelText('CEP')
    const hint = screen.getByText('Ao sair do campo, o endereço é preenchido.')
    expect(cep.getAttribute('aria-describedby')).toContain(hint.id)
  })

  it('sucesso: ao sair do campo chama GET /cep/{cep} e preenche logradouro, bairro, complemento e municipio', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await user.type(screen.getByLabelText('CEP'), '85980000')
    expect(screen.getByLabelText('CEP')).toHaveValue('85980-000')
    expect(apiMock).not.toHaveBeenCalled() // so ao sair do campo
    await user.tab()
    await waitFor(() => expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua Quinze de Novembro'))
    expect(apiMock).toHaveBeenCalledWith('/cep/85980000')
    expect(screen.getByLabelText('Bairro')).toHaveValue('Centro')
    expect(screen.getByLabelText('Complemento')).toHaveValue('lado par')
    expect(screen.getByRole('combobox', { name: /Município/ })).toHaveTextContent('Guaíra - PR')
    expect(screen.getByRole('button', { name: 'Limpar seleção' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0]).toMatchObject({ cep: '85980000', logradouro: 'Rua Quinze de Novembro', cidade: { id: 'mun-4108809' } })
    expect(onSubmit.mock.calls[0]![0].cidade).toEqual({ id: 'mun-4108809' }) // FK envia so {id}
  })

  it('mostra o estado de carregando no campo CEP enquanto busca', async () => {
    let release!: (v: unknown) => void
    apiMock.mockReturnValue(new Promise((r) => (release = r)))
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CEP'), '85980000')
    await user.tab()
    expect(await screen.findByText('Buscando endereço…')).toBeInTheDocument()
    expect(screen.getByLabelText('CEP')).toHaveAttribute('aria-busy', 'true')
    release(RESPOSTA)
    await waitFor(() => expect(screen.queryByText('Buscando endereço…')).not.toBeInTheDocument())
  })

  it('municipio null: aviso discreto e o campo fica para escolha manual', async () => {
    apiMock.mockResolvedValue({ ...RESPOSTA, municipio: null })
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CEP'), '85980000')
    await user.tab()
    expect(await screen.findByText(AVISO_MUNICIPIO)).toBeInTheDocument()
    expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua Quinze de Novembro')
    expect(screen.getByRole('combobox', { name: /Município/ })).toHaveTextContent('Selecione')
  })

  it('404: erro vira mensagem do campo CEP e o formulario continua utilizavel', async () => {
    apiMock.mockRejectedValueOnce(new ApiError(404, 'cep_nao_encontrado', 'CEP não encontrado.'))
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await user.type(screen.getByLabelText('CEP'), '00000000')
    await user.tab()
    const alerta = await screen.findByText('CEP não encontrado.')
    expect(alerta).toHaveAttribute('role', 'alert')
    expect(screen.getByLabelText('CEP')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('CEP').getAttribute('aria-describedby')).toContain(alerta.id)
    // continua digitando: nada travado
    await user.type(screen.getByLabelText('Logradouro'), 'Rua X')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(apiMock).toHaveBeenCalledTimes(1)
  })

  it('502 e 429 tambem aparecem no campo (429 com tempo de espera)', async () => {
    apiMock.mockRejectedValueOnce(new ApiError(502, 'servico_cep_indisponivel', 'Serviço de CEP indisponível. Tente novamente.'))
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CEP'), '85980000')
    await user.tab()
    expect(await screen.findByText('Serviço de CEP indisponível. Tente novamente.')).toBeInTheDocument()
    // voltar ao campo e sair de novo (sem mudar o CEP) repete a busca, agora 429
    apiMock.mockRejectedValueOnce(new ApiError(429, 'muitas_tentativas', 'Muitas tentativas.', undefined, 30))
    await user.click(screen.getByLabelText('CEP'))
    await user.tab()
    expect(await screen.findByText(/Aguarde 30/)).toBeInTheDocument()
    expect(apiMock).toHaveBeenCalledTimes(2)
  })

  it('nao sobrescreve o digitado quando o CEP devolve logradouro/bairro vazios; complemento so se vazio', async () => {
    apiMock.mockResolvedValue({ ...RESPOSTA, logradouro: '', bairro: '', complemento: 'do back' })
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('Logradouro'), 'Rua que eu digitei')
    await user.type(screen.getByLabelText('Bairro'), 'Meu bairro')
    await user.type(screen.getByLabelText('Complemento'), 'ap 3')
    await user.type(screen.getByLabelText('CEP'), '85980000')
    await user.tab()
    await waitFor(() => expect(screen.getByRole('combobox', { name: /Município/ })).toHaveTextContent('Guaíra - PR'))
    expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua que eu digitei')
    expect(screen.getByLabelText('Bairro')).toHaveValue('Meu bairro')
    expect(screen.getByLabelText('Complemento')).toHaveValue('ap 3')
  })

  it('CEP incompleto nao chama a API; sem mudanca ao sair nao repete a busca', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm()
    await user.type(screen.getByLabelText('CEP'), '8598')
    await user.tab()
    expect(apiMock).not.toHaveBeenCalled()
    await user.click(screen.getByLabelText('CEP'))
    await user.type(screen.getByLabelText('CEP'), '0000')
    await user.tab()
    await waitFor(() => expect(apiMock).toHaveBeenCalledTimes(1))
    await user.click(screen.getByLabelText('CEP'))
    await user.tab()
    expect(apiMock).toHaveBeenCalledTimes(1)
  })

  it('preenche o FK de municipio mesmo quando o campo se chama `municipio` (Empresa), nao so `cidade`', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    const s2 = z.object({ cep: z.string().optional(), logradouro: z.string().optional(), municipio: z.object({ id: z.string() }).optional() })
    const onSubmit = vi.fn()
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(
      <QueryClientProvider client={qc}>
        <AutoForm
          schema={s2}
          fields={[
            { name: 'cep', label: 'CEP', mask: 'postalCode', lookup: 'cep' },
            { name: 'logradouro', label: 'Logradouro' },
            { name: 'municipio', label: 'Município', fk },
          ]}
          defaults={{ cep: '', logradouro: '' }}
          onSubmit={onSubmit}
        />
      </QueryClientProvider>,
    )
    await user.type(screen.getByLabelText('CEP'), '85980000')
    await user.tab()
    await waitFor(() => expect(screen.getByRole('combobox', { name: /Município/ })).toHaveTextContent('Guaíra - PR'))
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0].municipio).toEqual({ id: 'mun-4108809' })
  })

  it('campos irmaos ausentes do layout sao ignorados', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm([{ name: 'cep', label: 'CEP', mask: 'postalCode', lookup: 'cep' }, { name: 'logradouro', label: 'Logradouro' }])
    await user.type(screen.getByLabelText('CEP'), '85980000')
    await user.tab()
    await waitFor(() => expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua Quinze de Novembro'))
    expect(screen.queryByText(AVISO_MUNICIPIO)).not.toBeInTheDocument()
  })
})

describe('AutoForm: clearAfterSubmit', () => {
  it('limpa os campos indicados depois do envio', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    const s = z.object({ nome: z.string(), senha: z.string() })
    const qc = new QueryClient()
    render(
      <QueryClientProvider client={qc}>
        <AutoForm schema={s} fields={[{ name: 'nome', label: 'Nome' }, { name: 'senha', label: 'Senha', kind: 'password' }]} defaults={{ nome: '', senha: '' }} onSubmit={onSubmit} clearAfterSubmit={['senha']} />
      </QueryClientProvider>,
    )
    await user.type(screen.getByLabelText('Nome'), 'Ana')
    await user.type(screen.getByLabelText('Senha'), 'segredo123')
    await user.click(screen.getByRole('button', { name: 'Salvar' }))
    await waitFor(() => expect(onSubmit).toHaveBeenCalled())
    await waitFor(() => expect(screen.getByLabelText('Senha')).toHaveValue(''))
    expect(screen.getByLabelText('Nome')).toHaveValue('Ana')
  })
})
