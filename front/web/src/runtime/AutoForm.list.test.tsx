import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { z } from 'zod'
import { AVISO_CNPJ, AutoForm, type FieldLayout } from '@/runtime/AutoForm'

const apiMock = vi.hoisted(() => vi.fn())
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))

const TIPOS = [
  { value: 'SOCIO', label: 'Sócio' },
  { value: 'FINANCEIRO', label: 'Financeiro' },
  { value: 'OUTRO', label: 'Outro' },
]

const contatoSchema = z.object({
  id: z.string().optional(),
  tipoContato: z.enum(['SOCIO', 'FINANCEIRO', 'COMERCIAL', 'FISCAL', 'OUTRO']),
  nome: z.string().min(1, 'Informe o nome.'),
  whatsapp: z.string().optional(),
  email: z.email('E-mail inválido.').optional(),
})
const schema = z.object({
  documento: z.string().optional(),
  nome: z.string().min(1, 'Informe o nome.'),
  contatos: z.array(contatoSchema),
})
type V = z.infer<typeof schema>

const ITEM: FieldLayout = [
  [
    { name: 'tipoContato', label: 'Tipo', options: TIPOS, size: 's', required: true },
    { name: 'nome', label: 'Nome', size: 'l', required: true },
  ],
  [
    { name: 'whatsapp', label: 'WhatsApp', mask: 'phone', size: 's', hint: 'Com DDD.' },
    { name: 'email', label: 'E-mail', kind: 'email', size: 'l' },
  ],
]
const LISTA = { item: ITEM, itemLabel: 'Contato', addLabel: 'Adicionar contato', maxItems: 3, defaultItem: { tipoContato: 'OUTRO' } }

const SIMPLES: FieldLayout = [
  { name: 'nome', label: 'Nome da pessoa', required: true },
  { name: 'contatos', label: 'Contatos', kind: 'list', list: LISTA, size: 'full' },
]
const COM_SECOES: FieldLayout = [
  { section: 'Identificação' },
  [{ name: 'documento', label: 'CNPJ', mask: 'documento', lookup: 'cnpj', size: 's' }, { name: 'nome', label: 'Nome da pessoa', size: 'l' }],
  { section: 'Contatos', description: 'Quem fala pela empresa.', collapsible: true },
  [{ name: 'contatos', label: 'Contatos', kind: 'list', list: LISTA, size: 'full' }],
]

const CNPJ = '27865757000102'

function renderForm(fields: FieldLayout = SIMPLES, extra: { values?: V; defaults?: V; onSubmit?: (v: V) => void } = {}) {
  const onSubmit = extra.onSubmit ?? vi.fn()
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <AutoForm schema={schema} fields={fields} defaults={extra.defaults ?? ({ contatos: [] } as unknown as V)} values={extra.values} onSubmit={onSubmit} />
    </QueryClientProvider>,
  )
  return onSubmit as ReturnType<typeof vi.fn>
}

const add = (user: ReturnType<typeof userEvent.setup>) => user.click(screen.getByRole('button', { name: 'Adicionar contato' }))
const salvar = (user: ReturnType<typeof userEvent.setup>) => user.click(screen.getByRole('button', { name: /^Salvar/ }))

beforeEach(() => apiMock.mockReset())

describe('AutoForm: campo de lista', () => {
  it('estado vazio mostra o texto padrao e o botao de adicionar', () => {
    renderForm()
    expect(screen.getByText('Nenhum contato adicionado.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Adicionar contato' })).toBeEnabled()
    expect(screen.queryByRole('group', { name: 'Contato 1' })).not.toBeInTheDocument()
  })

  it('emptyText personalizado', () => {
    renderForm([{ name: 'contatos', label: 'Contatos', kind: 'list', list: { ...LISTA, emptyText: 'Sem contatos ainda.' } }])
    expect(screen.getByText('Sem contatos ainda.')).toBeInTheDocument()
  })

  it('adiciona itens numerados, com campos aninhados, hint e mascara', async () => {
    const user = userEvent.setup()
    renderForm()
    await add(user)
    await add(user)
    expect(screen.queryByText('Nenhum contato adicionado.')).not.toBeInTheDocument()
    const c1 = screen.getByRole('group', { name: 'Contato 1' })
    const c2 = screen.getByRole('group', { name: 'Contato 2' })
    expect(within(c1).getByText('Contato 1')).toBeInTheDocument()
    await user.type(within(c1).getByLabelText(/^Nome/), 'Ana')
    await user.type(within(c2).getByLabelText(/^Nome/), 'Beto')
    expect(within(c1).getByLabelText(/^Nome/)).toHaveValue('Ana')
    expect(within(c2).getByLabelText(/^Nome/)).toHaveValue('Beto')
    await user.type(within(c1).getByLabelText('WhatsApp'), '11987654321')
    expect(within(c1).getByLabelText('WhatsApp')).toHaveValue('(11) 98765-4321')
    expect(within(c1).getByLabelText('WhatsApp')).toHaveAccessibleDescription('Com DDD.')
    // defaultItem aplicado ao enum
    expect(within(c1).getByRole('combobox', { name: /Tipo/ })).toHaveTextContent('Outro')
  })

  it('usa a mesma grade de 12 colunas e os mesmos tamanhos dentro do item', async () => {
    const user = userEvent.setup()
    renderForm()
    await add(user)
    const cell = (l: RegExp | string) => within(screen.getByRole('group', { name: 'Contato 1' })).getByLabelText(l).closest('div[class*="col-span-12"]') as HTMLElement
    expect(cell(/^Nome/).className).toContain('md:col-span-6')
    expect(cell('WhatsApp').className).toContain('md:col-span-3')
    expect(cell(/^Nome/).parentElement!.className).toContain('grid-cols-12')
  })

  it('remover: botao so com icone, aria-label e renumeracao dos itens', async () => {
    const user = userEvent.setup()
    renderForm()
    await add(user)
    await add(user)
    await user.type(within(screen.getByRole('group', { name: 'Contato 1' })).getByLabelText(/^Nome/), 'Ana')
    await user.type(within(screen.getByRole('group', { name: 'Contato 2' })).getByLabelText(/^Nome/), 'Beto')
    const remover = screen.getByRole('button', { name: 'Remover Contato 1' })
    expect(remover).toHaveTextContent('')
    await user.click(remover)
    expect(screen.queryByRole('group', { name: 'Contato 2' })).not.toBeInTheDocument()
    expect(within(screen.getByRole('group', { name: 'Contato 1' })).getByLabelText(/^Nome/)).toHaveValue('Beto')
    await user.click(screen.getByRole('button', { name: 'Remover Contato 1' }))
    expect(screen.getByText('Nenhum contato adicionado.')).toBeInTheDocument()
  })

  it('maxItems desabilita o botao de adicionar; remover reabilita', async () => {
    const user = userEvent.setup()
    renderForm()
    await add(user)
    await add(user)
    await add(user)
    expect(screen.getByRole('button', { name: 'Adicionar contato' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Remover Contato 3' }))
    expect(screen.getByRole('button', { name: 'Adicionar contato' })).toBeEnabled()
  })

  it('valida por item: erro sob o campo, nao envia, e "Campo obrigatorio" no nome vazio', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm(SIMPLES, { defaults: { contatos: [] } as unknown as V })
    await user.type(screen.getByLabelText(/^Nome da pessoa/), 'Empresa X')
    await add(user)
    await add(user)
    const c1 = screen.getByRole('group', { name: 'Contato 1' })
    const c2 = screen.getByRole('group', { name: 'Contato 2' })
    await user.type(within(c2).getByLabelText(/^Nome/), 'Beto')
    await user.type(within(c2).getByLabelText('E-mail'), 'invalido')
    await salvar(user)
    expect(await within(c1).findByText('Campo obrigatório')).toBeInTheDocument()
    expect(within(c1).getByLabelText(/^Nome/)).toHaveAttribute('aria-invalid', 'true')
    expect(await within(c2).findByText('E-mail inválido.')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
    // corrige e envia
    await user.type(within(c1).getByLabelText(/^Nome/), 'Ana')
    await user.clear(within(c2).getByLabelText('E-mail'))
    await salvar(user)
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
  })

  it('item sem tipo escolhido nao envia e marca o select como invalido', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm([{ name: 'contatos', label: 'Contatos', kind: 'list', list: { ...LISTA, defaultItem: undefined } }])
    await add(user)
    await user.type(screen.getByLabelText(/^Nome/), 'Ana')
    await salvar(user)
    await waitFor(() => expect(screen.getByRole('combobox', { name: /Tipo/ })).toHaveAttribute('aria-invalid', 'true'))
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submit envia contatos como array de objetos limpos (sem vazios)', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await user.type(screen.getByLabelText(/^Nome da pessoa/), 'Empresa X')
    await add(user)
    const c1 = screen.getByRole('group', { name: 'Contato 1' })
    await user.type(within(c1).getByLabelText(/^Nome/), 'Ana')
    await user.type(within(c1).getByLabelText('WhatsApp'), '11987654321')
    await salvar(user)
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    const enviado = onSubmit.mock.calls[0]![0] as { contatos: Record<string, unknown>[] }
    expect(enviado.contatos).toEqual([{ tipoContato: 'OUTRO', nome: 'Ana', whatsapp: '11987654321' }])
    expect(Object.keys(enviado.contatos[0]!)).not.toContain('email')
  })

  it('lista vazia e enviada como []', async () => {
    const user = userEvent.setup()
    const onSubmit = renderForm()
    await user.type(screen.getByLabelText(/^Nome da pessoa/), 'Empresa X')
    await salvar(user)
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0]).toMatchObject({ nome: 'Empresa X', contatos: [] })
  })

  describe('modo edicao', () => {
    const values: V = {
      nome: 'Empresa X',
      contatos: [
        { id: 'con-1', tipoContato: 'FINANCEIRO', nome: 'Marta', whatsapp: '11911112222', email: 'marta@x.com' },
        { id: 'con-2', tipoContato: 'SOCIO', nome: 'Paulo' },
      ],
    } as V

    it('preenche a lista com os valores carregados', () => {
      renderForm(SIMPLES, { values })
      expect(within(screen.getByRole('group', { name: 'Contato 1' })).getByLabelText(/^Nome/)).toHaveValue('Marta')
      expect(within(screen.getByRole('group', { name: 'Contato 1' })).getByLabelText('WhatsApp')).toHaveValue('(11) 91111-2222')
      expect(within(screen.getByRole('group', { name: 'Contato 2' })).getByRole('combobox', { name: /Tipo/ })).toHaveTextContent('Sócio')
    })

    it('preserva o id dos itens no submit, inclusive apos editar, remover e adicionar', async () => {
      const user = userEvent.setup()
      const onSubmit = renderForm(SIMPLES, { values })
      await user.type(within(screen.getByRole('group', { name: 'Contato 2' })).getByLabelText(/^Nome/), ' Nunes')
      await add(user)
      await user.type(within(screen.getByRole('group', { name: 'Contato 3' })).getByLabelText(/^Nome/), 'Novo')
      await user.click(screen.getByRole('button', { name: 'Remover Contato 1' }))
      await salvar(user)
      await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
      expect(onSubmit.mock.calls[0]![0].contatos).toEqual([
        { id: 'con-2', tipoContato: 'SOCIO', nome: 'Paulo Nunes' },
        { tipoContato: 'OUTRO', nome: 'Novo' },
      ])
    })

    it('envia o id mesmo quando o schema nao o declara (zod remove chaves desconhecidas)', async () => {
      const semId = z.object({ nome: z.string(), contatos: z.array(z.object({ nome: z.string().min(1), tipoContato: z.string() })) })
      const onSubmit = vi.fn()
      const user = userEvent.setup()
      const qc = new QueryClient()
      render(
        <QueryClientProvider client={qc}>
          <AutoForm schema={semId as never} fields={SIMPLES} defaults={{ contatos: [] } as never} values={values as never} onSubmit={onSubmit} />
        </QueryClientProvider>,
      )
      await salvar(user)
      await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
      expect(onSubmit.mock.calls[0]![0].contatos.map((c: { id: string }) => c.id)).toEqual(['con-1', 'con-2'])
    })
  })

  it('dirty: a lista suja o formulario e liga o beforeunload; salvar limpa', async () => {
    const addSpy = vi.spyOn(window, 'addEventListener')
    const user = userEvent.setup()
    renderForm(COM_SECOES, { defaults: { contatos: [] } as unknown as V })
    expect(screen.queryByText('Alterações não salvas')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Contatos/ })) // abre a secao recolhida
    await add(user)
    expect(await screen.findByText('Alterações não salvas')).toBeInTheDocument()
    expect(addSpy.mock.calls.some(([t]) => t === 'beforeunload')).toBe(true)
    addSpy.mockRestore()
  })

  it('layout com secoes: a lista e um item de linha; o titulo vem da secao (sem rotulo duplicado)', async () => {
    const user = userEvent.setup()
    renderForm(COM_SECOES, { defaults: { contatos: [] } as unknown as V })
    await user.click(screen.getByRole('button', { name: /Contatos/ }))
    expect(screen.getByText('Quem fala pela empresa.')).toBeInTheDocument()
    expect(screen.getAllByText('Contatos')).toHaveLength(1) // so o titulo da secao; o grupo usa aria-label
    expect(screen.getAllByRole('group', { name: 'Contatos' })).toHaveLength(2) // Card da secao + grupo da lista
  })

  it('erro num item abre a secao recolhida e leva o foco ao campo invalido', async () => {
    const user = userEvent.setup()
    renderForm(COM_SECOES, { defaults: { nome: 'X', contatos: [{ tipoContato: 'OUTRO', nome: '' }] } as unknown as V })
    // secao nasce aberta por ter itens; fecha manualmente e tenta salvar
    const gatilho = screen.getByRole('button', { name: /Contatos/ })
    await user.click(gatilho)
    expect(gatilho).toHaveAttribute('aria-expanded', 'false')
    await salvar(user)
    await waitFor(() => expect(screen.getByRole('button', { name: /Contatos/ })).toHaveAttribute('aria-expanded', 'true'))
    expect(screen.getByText('(há campos com erro)')).toBeInTheDocument()
    expect(await screen.findByText('Campo obrigatório')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText(/^Nome\*?$/, { selector: 'input' })).toHaveFocus())
  })

  it('layout sem lista nao muda: nao ha grupo, botao de adicionar nem tooltip de remover', () => {
    renderForm([{ name: 'nome', label: 'Nome da pessoa' }])
    expect(screen.queryByRole('button', { name: /Adicionar/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('group')).not.toBeInTheDocument()
  })
})

describe('AutoForm: socios do CNPJ viram contatos', () => {
  const RESPOSTA = {
    cnpj: CNPJ,
    razaoSocial: 'EXEMPLO LTDA',
    socios: [
      { nome: 'Joana D’Arc Silva', tipo: 'Pessoa Física', qualificacao: 'Sócio-Administrador' },
      { nome: 'JOÃO PEDRO MARTINS', tipo: 'Pessoa Física', qualificacao: 'Sócio' },
      { nome: '  ', qualificacao: 'Sócio' },
    ],
  }

  async function consultar(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText('CNPJ'), CNPJ)
    await user.tab()
  }

  it('adiciona um contato tipo socio por socio e menciona no aviso', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    const onSubmit = renderForm(COM_SECOES, { defaults: { contatos: [] } as unknown as V })
    await consultar(user)
    expect(await screen.findByText(`${AVISO_CNPJ} 2 sócio(s) adicionado(s) em Contatos.`)).toBeInTheDocument()
    const c1 = screen.getByRole('group', { name: 'Contato 1' })
    expect(within(c1).getByLabelText(/^Nome/)).toHaveValue('Joana D’Arc Silva')
    expect(within(c1).getByRole('combobox', { name: /Tipo/ })).toHaveTextContent('Sócio')
    expect(within(screen.getByRole('group', { name: 'Contato 2' })).getByLabelText(/^Nome/)).toHaveValue('JOÃO PEDRO MARTINS')
    expect(screen.queryByRole('group', { name: 'Contato 3' })).not.toBeInTheDocument()
    await salvar(user)
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1))
    expect(onSubmit.mock.calls[0]![0].contatos).toEqual([
      { tipoContato: 'SOCIO', nome: 'Joana D’Arc Silva' },
      { tipoContato: 'SOCIO', nome: 'JOÃO PEDRO MARTINS' },
    ])
  })

  it('nao duplica (sem acento/caixa), so compara com o tipo socio e nunca apaga contatos existentes', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm(COM_SECOES, {
      defaults: {
        contatos: [
          { id: 'con-9', tipoContato: 'SOCIO', nome: 'joao pedro martins' },
          { id: 'con-8', tipoContato: 'FINANCEIRO', nome: 'Joana D’Arc Silva' },
        ],
      } as unknown as V,
    })
    await consultar(user)
    expect(await screen.findByText(`${AVISO_CNPJ} 1 sócio(s) adicionado(s) em Contatos.`)).toBeInTheDocument()
    expect(screen.getAllByRole('group', { name: /^Contato \d$/ })).toHaveLength(3)
    expect(within(screen.getByRole('group', { name: 'Contato 1' })).getByLabelText(/^Nome/)).toHaveValue('joao pedro martins')
    expect(within(screen.getByRole('group', { name: 'Contato 3' })).getByLabelText(/^Nome/)).toHaveValue('Joana D’Arc Silva')
  })

  it('consultar de novo nao adiciona de novo; sem novos socios o aviso nao menciona contatos', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    renderForm(COM_SECOES, { defaults: { contatos: [] } as unknown as V })
    await consultar(user)
    await screen.findByText(/2 sócio\(s\)/)
    await user.click(screen.getByRole('button', { name: 'Consultar CNPJ' }))
    expect(await screen.findByText(AVISO_CNPJ)).toBeInTheDocument()
    expect(screen.getAllByRole('group', { name: /^Contato \d$/ })).toHaveLength(2)
  })

  it('sem campo tipoContato no item, ou sem lista contatos, ignora os socios', async () => {
    apiMock.mockResolvedValue(RESPOSTA)
    const user = userEvent.setup()
    const semTipo: FieldLayout = [
      { name: 'documento', label: 'CNPJ', mask: 'documento', lookup: 'cnpj' },
      { name: 'contatos', label: 'Contatos', kind: 'list', list: { ...LISTA, item: [[{ name: 'nome', label: 'Nome', size: 'full' }]] } },
    ]
    renderForm(semTipo, { defaults: { contatos: [] } as unknown as V })
    await consultar(user)
    expect(await screen.findByText(AVISO_CNPJ)).toBeInTheDocument()
    expect(screen.queryByRole('group', { name: 'Contato 1' })).not.toBeInTheDocument()
  })

  it('socios ausentes na resposta mantem o aviso original', async () => {
    apiMock.mockResolvedValue({ cnpj: CNPJ, razaoSocial: 'X' })
    const user = userEvent.setup()
    renderForm(COM_SECOES, { defaults: { contatos: [] } as unknown as V })
    await consultar(user)
    expect(await screen.findByText(AVISO_CNPJ)).toBeInTheDocument()
  })
})
