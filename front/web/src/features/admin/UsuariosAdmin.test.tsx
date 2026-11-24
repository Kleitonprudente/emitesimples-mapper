import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { UsuariosAdmin } from '@/features/admin/UsuariosAdmin'

const apiMock = vi.hoisted(() => vi.fn())
const sessao = vi.hoisted(() => ({ atual: { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true } as unknown }))
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@/lib/auth', async (orig) => ({ ...(await orig<typeof import('@/lib/auth')>()), useSession: () => sessao.atual }))

const USUARIOS = [
  { id: 'u1', nome: 'Ana Ribeiro', email: 'ana@exemplo.com.br', whatsapp: '11980001234', ativo: true },
  { id: 'u2', nome: 'Bruno Lima', email: 'bruno@exemplo.com.br', ativo: false },
]
const VINCULOS = [{ id: 'v1', empresaId: 'emp-1', empresa: { razaoSocial: 'Horizonte Contábil Ltda' }, papel: 'OPERADOR', ativo: true }]
const EMPRESAS = [{ id: 'emp-2', razaoSocial: 'Studio Bella ME' }]

function rotas(extra: (path: string, opts: Record<string, unknown>) => unknown = () => undefined) {
  apiMock.mockImplementation(async (path: string, opts: Record<string, unknown> = {}) => {
    const r = extra(path, opts)
    if (r !== undefined) return r
    if (path === '/admin/usuarios/count') return { count: USUARIOS.length }
    if (/^\/admin\/usuarios\/\d+\/\d+$/.test(path)) return USUARIOS
    if (path === '/admin/usuarios/u1/vinculos' && !opts.method) return VINCULOS
    if (path === '/Empresa') return EMPRESAS
    return {}
  })
}

const chamadas = (metodo: string, path: string) => apiMock.mock.calls.filter(([p, o]) => p === path && (o?.method ?? 'GET') === metodo)

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <UsuariosAdmin />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  apiMock.mockReset()
  sessao.atual = { usuario: { id: 'u', nome: 'Eva', email: 'e@x.com' }, empresa: null, papel: null, plataformaAdmin: true }
})

describe('UsuariosAdmin', () => {
  it('quem nao e administrador da plataforma ve acesso restrito e nada e chamado', () => {
    sessao.atual = { ...(sessao.atual as object), plataformaAdmin: false }
    rotas()
    renderTela()
    expect(screen.getByRole('alert')).toHaveTextContent('Acesso restrito')
    expect(apiMock).not.toHaveBeenCalled()
  })

  it('lista paginada no servidor (from/to + count) com situacao e WhatsApp mascarado', async () => {
    rotas()
    renderTela()
    expect(await screen.findByText('Ana Ribeiro')).toBeInTheDocument()
    expect(apiMock).toHaveBeenCalledWith('/admin/usuarios/0/9', { query: { busca: undefined, sortBy: 'nome', sortDirection: 'asc' } })
    expect(apiMock).toHaveBeenCalledWith('/admin/usuarios/count', { query: { busca: undefined } })
    expect(screen.getByText('(11) 98000-1234')).toBeInTheDocument()
    expect(screen.getByText('Ativo')).toBeInTheDocument()
    expect(screen.getByText('Inativo')).toBeInTheDocument()
  })

  it('busca por nome/e-mail envia o parametro `busca`', async () => {
    rotas()
    const user = userEvent.setup()
    renderTela()
    await screen.findByText('Ana Ribeiro')
    await user.type(screen.getByRole('searchbox', { name: 'Buscar' }), 'bruno')
    await waitFor(() => expect(apiMock).toHaveBeenCalledWith('/admin/usuarios/0/9', { query: expect.objectContaining({ busca: 'bruno' }) }))
    expect(apiMock).toHaveBeenCalledWith('/admin/usuarios/count', { query: { busca: 'bruno' } })
  })

  it('erro ao carregar mostra "tentar novamente"', async () => {
    apiMock.mockRejectedValue(new ApiError(500, 'erro_interno', 'Falha no servidor.'))
    renderTela()
    expect(await screen.findByText('Falha no servidor.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
  })

  it('criar: valida (obrigatorios, senha curta) e so entao envia; envia a senha uma vez', async () => {
    rotas()
    const user = userEvent.setup()
    renderTela()
    await screen.findByText('Ana Ribeiro')
    await user.click(screen.getByRole('button', { name: 'Novo usuário' }))
    const dlg = await screen.findByRole('dialog', { name: 'Novo usuário' })
    await user.click(within(dlg).getByRole('button', { name: 'Criar usuário' }))
    expect((await within(dlg).findAllByText('Campo obrigatório')).length).toBeGreaterThanOrEqual(3)
    await user.type(within(dlg).getByLabelText(/^Nome/), 'Carla Nova')
    await user.type(within(dlg).getByLabelText(/^E-mail/), 'carla@exemplo.com.br')
    await user.type(within(dlg).getByLabelText(/^Senha temporária/), 'curta')
    await user.click(within(dlg).getByRole('button', { name: 'Criar usuário' }))
    expect(await within(dlg).findByText('A senha deve ter pelo menos 8 caracteres.')).toBeInTheDocument()
    expect(chamadas('POST', '/admin/usuarios')).toHaveLength(0)
    await user.clear(within(dlg).getByLabelText(/^Senha temporária/))
    await user.type(within(dlg).getByLabelText(/^Senha temporária/), 'temporaria1')
    await user.click(within(dlg).getByRole('button', { name: 'Criar usuário' }))
    await waitFor(() => expect(chamadas('POST', '/admin/usuarios')).toHaveLength(1))
    expect(chamadas('POST', '/admin/usuarios')[0]![1].body).toEqual({ nome: 'Carla Nova', email: 'carla@exemplo.com.br', whatsapp: undefined, senha: 'temporaria1', ativo: true, vinculos: undefined })
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
  })

  it('criar: erro do servidor aparece no dialogo e o campo de senha e limpo', async () => {
    rotas((path, opts) => {
      if (path === '/admin/usuarios' && opts.method === 'POST') throw new ApiError(409, 'email_duplicado', 'Já existe um usuário com este e-mail.')
    })
    const user = userEvent.setup()
    renderTela()
    await screen.findByText('Ana Ribeiro')
    await user.click(screen.getByRole('button', { name: 'Novo usuário' }))
    const dlg = await screen.findByRole('dialog', { name: 'Novo usuário' })
    await user.type(within(dlg).getByLabelText(/^Nome/), 'Ana Dois')
    await user.type(within(dlg).getByLabelText(/^E-mail/), 'ana@exemplo.com.br')
    await user.type(within(dlg).getByLabelText(/^Senha temporária/), 'temporaria1')
    await user.click(within(dlg).getByRole('button', { name: 'Criar usuário' }))
    expect(await within(dlg).findByText('Já existe um usuário com este e-mail.')).toBeInTheDocument()
    expect(within(dlg).getByText(/email_duplicado/)).toBeInTheDocument()
    await waitFor(() => expect(within(dlg).getByLabelText(/^Senha temporária/)).toHaveValue(''))
    expect(within(dlg).getByLabelText(/^Nome/)).toHaveValue('Ana Dois')
  })

  it('criar com vinculo inicial: empresa sem papel e rejeitado; com os dois envia vinculos', async () => {
    rotas()
    const user = userEvent.setup()
    renderTela()
    await screen.findByText('Ana Ribeiro')
    await user.click(screen.getByRole('button', { name: 'Novo usuário' }))
    const dlg = await screen.findByRole('dialog', { name: 'Novo usuário' })
    await user.type(within(dlg).getByLabelText(/^Nome/), 'Davi Novo')
    await user.type(within(dlg).getByLabelText(/^E-mail/), 'davi@exemplo.com.br')
    await user.type(within(dlg).getByLabelText(/^Senha temporária/), 'temporaria1')
    await user.click(within(dlg).getByRole('combobox', { name: /Empresa/ }))
    await user.click(await screen.findByRole('option', { name: 'Studio Bella ME' }))
    await user.click(within(dlg).getByRole('button', { name: 'Criar usuário' }))
    expect(await within(dlg).findByText(/empresa e o papel juntos/)).toBeInTheDocument()
    await user.click(within(dlg).getByRole('combobox', { name: /Papel na empresa/ }))
    await user.click(await screen.findByRole('option', { name: 'Operador' }))
    await user.click(within(dlg).getByRole('button', { name: 'Criar usuário' }))
    await waitFor(() => expect(chamadas('POST', '/admin/usuarios')).toHaveLength(1))
    expect(chamadas('POST', '/admin/usuarios')[0]![1].body).toMatchObject({ vinculos: [{ empresaId: 'emp-2', papel: 'OPERADOR' }] })
  })

  it('editar: preenche, valida e envia PATCH {nome, whatsapp, ativo}', async () => {
    rotas()
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByRole('button', { name: 'Editar Ana Ribeiro' }))
    const dlg = await screen.findByRole('dialog', { name: 'Editar usuário' })
    expect(within(dlg).getByLabelText(/^Nome/)).toHaveValue('Ana Ribeiro')
    expect(within(dlg).getByLabelText('WhatsApp')).toHaveValue('(11) 98000-1234')
    await user.clear(within(dlg).getByLabelText(/^Nome/))
    await user.click(within(dlg).getByRole('button', { name: 'Salvar alterações' }))
    expect(await within(dlg).findByText('Campo obrigatório')).toBeInTheDocument()
    await user.type(within(dlg).getByLabelText(/^Nome/), 'Ana R. Silva')
    await user.click(within(dlg).getByRole('switch', { name: 'Usuário ativo' }))
    await user.click(within(dlg).getByRole('button', { name: 'Salvar alterações' }))
    await waitFor(() => expect(chamadas('PATCH', '/admin/usuarios/u1')).toHaveLength(1))
    expect(chamadas('PATCH', '/admin/usuarios/u1')[0]![1].body).toEqual({ nome: 'Ana R. Silva', whatsapp: '11980001234', ativo: false })
  })

  it('redefinir senha: confere a confirmacao, envia so {senha} e nao deixa a senha no campo', async () => {
    rotas((path, opts) => {
      if (path === '/admin/usuarios/u2/senha' && opts.method === 'POST') throw new ApiError(400, 'senha_invalida', 'Senha recusada pelo servidor.')
    })
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByRole('button', { name: 'Redefinir senha de Bruno Lima' }))
    const dlg = await screen.findByRole('dialog', { name: 'Redefinir senha' })
    await user.type(within(dlg).getByLabelText(/^Nova senha/), 'novasenha123')
    await user.type(within(dlg).getByLabelText(/^Repita/), 'diferente123')
    await user.click(within(dlg).getByRole('button', { name: 'Redefinir senha' }))
    expect(await within(dlg).findByText('As senhas não conferem.')).toBeInTheDocument()
    expect(chamadas('POST', '/admin/usuarios/u2/senha')).toHaveLength(0)
    await user.clear(within(dlg).getByLabelText(/^Repita/))
    await user.type(within(dlg).getByLabelText(/^Repita/), 'novasenha123')
    await user.click(within(dlg).getByRole('button', { name: 'Redefinir senha' }))
    expect(await within(dlg).findByText('Senha recusada pelo servidor.')).toBeInTheDocument()
    expect(chamadas('POST', '/admin/usuarios/u2/senha')[0]![1].body).toEqual({ senha: 'novasenha123' })
    await waitFor(() => expect(within(dlg).getByLabelText(/^Nova senha/)).toHaveValue(''))
    expect(within(dlg).getByLabelText(/^Repita/)).toHaveValue('')
  })

  it('vinculos: lista, muda o papel, desativa (nunca exclui) e adiciona', async () => {
    rotas()
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByRole('button', { name: 'Vínculos de Ana Ribeiro' }))
    const dlg = await screen.findByRole('dialog', { name: /Vínculos de Ana Ribeiro/ })
    expect(await within(dlg).findByText('Horizonte Contábil Ltda')).toBeInTheDocument()
    expect(within(dlg).queryByRole('button', { name: /excluir|remover/i })).not.toBeInTheDocument()

    await user.click(within(dlg).getByRole('combobox', { name: 'Papel em Horizonte Contábil Ltda' }))
    await user.click(await screen.findByRole('option', { name: 'Consulta' }))
    await waitFor(() => expect(chamadas('PATCH', '/admin/usuarios/u1/vinculos/v1')).toHaveLength(1))
    expect(chamadas('PATCH', '/admin/usuarios/u1/vinculos/v1')[0]![1].body).toEqual({ papel: 'CONSULTA', ativo: true })

    await user.click(within(dlg).getByRole('button', { name: /Desativar/ }))
    await waitFor(() => expect(chamadas('PATCH', '/admin/usuarios/u1/vinculos/v1')).toHaveLength(2))
    expect(chamadas('PATCH', '/admin/usuarios/u1/vinculos/v1')[1]![1].body).toEqual({ papel: 'OPERADOR', ativo: false })

    await user.click(within(dlg).getByRole('button', { name: 'Adicionar vínculo' })) // vazio: valida
    expect((await within(dlg).findAllByText('Campo obrigatório')).length).toBeGreaterThanOrEqual(1)
    await user.click(within(dlg).getByRole('combobox', { name: /^Empresa/ }))
    await user.click(await screen.findByRole('option', { name: 'Studio Bella ME' }))
    await user.click(within(dlg).getByRole('combobox', { name: /^Papel\*?$/ }))
    await user.click(await screen.findByRole('option', { name: 'Administrador' }))
    await user.click(within(dlg).getByRole('button', { name: 'Adicionar vínculo' }))
    await waitFor(() => expect(chamadas('POST', '/admin/usuarios/u1/vinculos')).toHaveLength(1))
    expect(chamadas('POST', '/admin/usuarios/u1/vinculos')[0]![1].body).toEqual({ empresaId: 'emp-2', papel: 'ADMINISTRADOR', podeCancelarNfse: false })
  })

  it('vinculos: coluna Pode cancelar delega ao operador (PATCH) e o formulario de adicionar leva o campo com a dica', async () => {
    rotas()
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByRole('button', { name: 'Vínculos de Ana Ribeiro' }))
    const dlg = await screen.findByRole('dialog', { name: /Vínculos de Ana Ribeiro/ })
    expect(await within(dlg).findByRole('columnheader', { name: 'Pode cancelar' })).toBeInTheDocument()
    const interruptor = within(dlg).getByRole('switch', { name: 'Pode cancelar NFS-e em Horizonte Contábil Ltda' })
    expect(interruptor).not.toBeChecked()
    await user.click(interruptor)
    await waitFor(() => expect(chamadas('PATCH', '/admin/usuarios/u1/vinculos/v1')).toHaveLength(1))
    expect(chamadas('PATCH', '/admin/usuarios/u1/vinculos/v1')[0]![1].body).toEqual({ papel: 'OPERADOR', ativo: true, podeCancelarNfse: true })

    expect(within(dlg).getByText('Administradores sempre podem; operadores só com esta permissão.')).toBeInTheDocument()
    await user.click(within(dlg).getByRole('combobox', { name: /^Empresa/ }))
    await user.click(await screen.findByRole('option', { name: 'Studio Bella ME' }))
    await user.click(within(dlg).getByRole('combobox', { name: /^Papel\*?$/ }))
    await user.click(await screen.findByRole('option', { name: 'Operador' }))
    await user.click(within(dlg).getByRole('switch', { name: /Pode cancelar NFS-e \(delegar ao operador\)/ }))
    await user.click(within(dlg).getByRole('button', { name: 'Adicionar vínculo' }))
    await waitFor(() => expect(chamadas('POST', '/admin/usuarios/u1/vinculos')).toHaveLength(1))
    expect(chamadas('POST', '/admin/usuarios/u1/vinculos')[0]![1].body).toEqual({ empresaId: 'emp-2', papel: 'OPERADOR', podeCancelarNfse: true })
  })

  it('vinculos: administrador sempre pode (interruptor marcado e travado)', async () => {
    rotas((path, opts) => {
      if (path === '/admin/usuarios/u1/vinculos' && !opts.method) return [{ ...VINCULOS[0], papel: 'ADMINISTRADOR' }]
    })
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByRole('button', { name: 'Vínculos de Ana Ribeiro' }))
    const dlg = await screen.findByRole('dialog', { name: /Vínculos de Ana Ribeiro/ })
    const interruptor = await within(dlg).findByRole('switch', { name: 'Pode cancelar NFS-e em Horizonte Contábil Ltda' })
    expect(interruptor).toBeChecked()
    expect(interruptor).toBeDisabled()
  })

  it('vinculos: erro do servidor ao adicionar aparece no dialogo', async () => {
    rotas((path, opts) => {
      if (path === '/admin/usuarios/u1/vinculos' && opts.method === 'POST') throw new ApiError(409, 'vinculo_duplicado', 'O usuário já tem vínculo com esta empresa.')
    })
    const user = userEvent.setup()
    renderTela()
    await user.click(await screen.findByRole('button', { name: 'Vínculos de Ana Ribeiro' }))
    const dlg = await screen.findByRole('dialog', { name: /Vínculos de Ana Ribeiro/ })
    await user.click(within(dlg).getByRole('combobox', { name: /^Empresa/ }))
    await user.click(await screen.findByRole('option', { name: 'Studio Bella ME' }))
    await user.click(within(dlg).getByRole('combobox', { name: /^Papel\*?$/ }))
    await user.click(await screen.findByRole('option', { name: 'Operador' }))
    await user.click(within(dlg).getByRole('button', { name: 'Adicionar vínculo' }))
    expect(await within(dlg).findByText('O usuário já tem vínculo com esta empresa.')).toBeInTheDocument()
  })
})
