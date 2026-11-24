import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'
import { VinculosDaEmpresa } from '@/features/admin/VinculosDaEmpresa'

const apiMock = vi.hoisted(() => vi.fn())
vi.mock('@/lib/api', async (orig) => ({ ...(await orig<typeof import('@/lib/api')>()), api: apiMock }))
vi.mock('@tanstack/react-router', () => ({ Link: ({ to, children }: { to: string; children: ReactNode }) => <a href={to}>{children}</a> }))

const URL_V = '/admin/empresas/emp-1/vinculos'
const ANA = { id: 'v1', usuario: { id: 'u1', nome: 'Ana Ribeiro', email: 'ana@x.com', ativo: true }, papel: 'administrador', ativo: true, dataCriacao: '2026-01-10T09:00:00' }
const BRUNO = { id: 'v2', usuario: { id: 'u2', nome: 'Bruno Operador', email: 'bruno@x.com', ativo: true }, papel: 'operador', ativo: false, dataCriacao: '2026-02-03T14:30:00' }
const CARLA = { id: 'u3', nome: 'Carla Consulta', email: 'carla@x.com', ativo: true }

type Opts = { method?: string; body?: unknown }
let lista: unknown[]
let aoEnviar: (p: string, o: Opts) => unknown

function prepara(inicial: unknown[]) {
  lista = inicial
  aoEnviar = () => undefined
  apiMock.mockImplementation(async (p: string, o: Opts = {}) => {
    if (p === URL_V && !o.method) return lista
    if (p.startsWith('/admin/usuarios/')) return [CARLA]
    return aoEnviar(p, o)
  })
}
const chamadas = (m: string) => apiMock.mock.calls.filter(([, o]) => o?.method === m)

function renderTela() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={qc}>
      <VinculosDaEmpresa empresaId="emp-1" />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  apiMock.mockReset()
})

describe('VinculosDaEmpresa', () => {
  it('lista os vinculos com usuario, papel, situacao e data', async () => {
    prepara([ANA, BRUNO])
    renderTela()
    expect(await screen.findByText('Ana Ribeiro')).toBeInTheDocument()
    expect(screen.getByText('bruno@x.com')).toBeInTheDocument()
    expect(screen.getByRole('switch', { name: /Acesso de Ana Ribeiro/ })).toBeChecked()
    expect(screen.getByRole('switch', { name: /Acesso de Bruno Operador/ })).not.toBeChecked()
    expect(screen.getByText('10/01/2026')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Criar novo usuário' })).toHaveAttribute('href', '/admin/usuarios')
    expect(screen.getByText('O usuário precisa existir antes de ser vinculado.')).toBeInTheDocument()
    expect(screen.getByText('Administradores sempre podem; operadores só com esta permissão.')).toBeInTheDocument()
  })

  it('mostra o estado vazio', async () => {
    prepara([])
    renderTela()
    expect(await screen.findByText('Nenhum usuário vinculado a esta empresa.')).toBeInTheDocument()
  })

  it('mostra carregando e depois erro com nova tentativa', async () => {
    apiMock.mockImplementation(async () => {
      throw new ApiError(500, "erro_interno", "Falha no servidor.")
    })
    renderTela()
    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(await screen.findByRole('alert')).toHaveTextContent('Falha no servidor.')
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
  })

  it('mudar o papel envia PATCH so com o papel', async () => {
    prepara([ANA])
    renderTela()
    const user = userEvent.setup()
    await user.click(await screen.findByRole('combobox', { name: 'Papel de Ana Ribeiro' }))
    await user.click(await screen.findByRole('option', { name: 'Consulta' }))
    await waitFor(() => expect(chamadas('PATCH')).toHaveLength(1))
    expect(chamadas('PATCH')[0]).toEqual([`${URL_V}/v1`, { method: 'PATCH', body: { papel: 'consulta', ativo: undefined } }])
  })

  it('desativar pede confirmacao e so envia ao confirmar', async () => {
    prepara([ANA])
    renderTela()
    const user = userEvent.setup()
    await user.click(await screen.findByRole('switch', { name: /Acesso de Ana Ribeiro/ }))
    expect(await screen.findByText('Desativar o acesso de Ana Ribeiro a esta empresa?')).toBeInTheDocument()
    expect(chamadas('PATCH')).toHaveLength(0)
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(chamadas('PATCH')).toHaveLength(0)
    await user.click(screen.getByRole('switch', { name: /Acesso de Ana Ribeiro/ }))
    await user.click(await screen.findByRole('button', { name: 'Desativar' }))
    await waitFor(() => expect(chamadas('PATCH')).toHaveLength(1))
    expect(chamadas('PATCH')[0]![1].body).toEqual({ papel: undefined, ativo: false })
  })

  it('reativar e direto, sem confirmacao', async () => {
    prepara([BRUNO])
    renderTela()
    const user = userEvent.setup()
    await user.click(await screen.findByRole('switch', { name: /Acesso de Bruno Operador/ }))
    await waitFor(() => expect(chamadas('PATCH')).toHaveLength(1))
    expect(chamadas('PATCH')[0]![1].body).toEqual({ papel: undefined, ativo: true })
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  async function escolheCarla(user: ReturnType<typeof userEvent.setup>) {
    await screen.findByText('Ana Ribeiro')
    await user.click(screen.getByRole('combobox', { name: 'Usuário' }))
    await user.click(await screen.findByRole('option', { name: /Carla Consulta/ }))
  }

  it('vincular fica desabilitado sem usuario e envia POST com usuario e papel padrao', async () => {
    prepara([ANA])
    aoEnviar = () => ({})
    renderTela()
    const user = userEvent.setup()
    await screen.findByText('Ana Ribeiro')
    expect(screen.getByRole('button', { name: 'Vincular' })).toBeDisabled()
    await escolheCarla(user)
    await user.click(screen.getByRole('button', { name: 'Vincular' }))
    await waitFor(() => expect(chamadas('POST')).toHaveLength(1))
    expect(chamadas('POST')[0]).toEqual([URL_V, { method: 'POST', body: { usuarioId: 'u3', papel: 'operador', podeCancelarNfse: false } }])
  })

  it('coluna Pode cancelar: administrador sempre (marcado e travado), operador pela delegacao, e a dica', async () => {
    prepara([ANA, { ...BRUNO, ativo: true, podeCancelarNfse: false }, { ...BRUNO, id: 'v4', usuario: { ...BRUNO.usuario, id: 'u4', nome: 'Davi Delegado' }, podeCancelarNfse: true }])
    renderTela()
    await screen.findByText('Ana Ribeiro')
    expect(screen.getByRole('columnheader', { name: 'Pode cancelar' })).toBeInTheDocument()
    const ana = screen.getByRole('switch', { name: 'Pode cancelar NFS-e: Ana Ribeiro' })
    expect(ana).toBeChecked()
    expect(ana).toBeDisabled()
    expect(screen.getByRole('switch', { name: 'Pode cancelar NFS-e: Bruno Operador' })).not.toBeChecked()
    expect(screen.getByRole('switch', { name: 'Pode cancelar NFS-e: Davi Delegado' })).toBeChecked()
    expect(screen.getByText('Administradores sempre podem; operadores só com esta permissão.')).toBeInTheDocument()
  })

  it('delegar e remover a delegacao enviam PATCH com podeCancelarNfse', async () => {
    prepara([{ ...BRUNO, ativo: true, podeCancelarNfse: false }])
    renderTela()
    const user = userEvent.setup()
    await user.click(await screen.findByRole('switch', { name: 'Pode cancelar NFS-e: Bruno Operador' }))
    await waitFor(() => expect(chamadas('PATCH')).toHaveLength(1))
    expect(chamadas('PATCH')[0]).toEqual([`${URL_V}/v2`, { method: 'PATCH', body: { papel: undefined, ativo: undefined, podeCancelarNfse: true } }])
  })

  it('o formulario de vincular tem o interruptor de delegacao e envia no POST', async () => {
    prepara([ANA])
    aoEnviar = () => ({})
    renderTela()
    const user = userEvent.setup()
    await escolheCarla(user)
    const delegar = screen.getByRole('switch', { name: 'Pode cancelar NFS-e (delegar ao operador)' })
    expect(delegar).not.toBeChecked()
    await user.click(delegar)
    await user.click(screen.getByRole('button', { name: 'Vincular' }))
    await waitFor(() => expect(chamadas('POST')).toHaveLength(1))
    expect(chamadas('POST')[0]![1].body).toEqual({ usuarioId: 'u3', papel: 'operador', podeCancelarNfse: true })
  })

  it('409 mostra a mensagem de usuario ja vinculado', async () => {
    prepara([ANA])
    aoEnviar = async () => {
      throw new ApiError(409, "vinculo_ja_existe", "Já existe.")
    }
    renderTela()
    const user = userEvent.setup()
    await escolheCarla(user)
    await user.click(screen.getByRole('button', { name: 'Vincular' }))
    expect(await screen.findByText('Este usuário já está vinculado.')).toBeInTheDocument()
  })
})
