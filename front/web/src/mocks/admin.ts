import type { Papel } from '@/lib/api'
import { EMPRESAS_MOCK, MOCK_USERS, MUNICIPIOS, type Municipio } from '@/mocks/fixtures'
import { erro, json } from '@/mocks/http'

/* ---------------------------------------------------------------------------
 * Mocks das rotas de ADMINISTRADOR DA PLATAFORMA (contrato provisorio; ver features/admin/adminApi.ts):
 *   GET  /admin/usuarios/count?busca            GET /admin/usuarios/{from}/{to}?busca&sortBy&sortDirection
 *   POST /admin/usuarios                         PATCH /admin/usuarios/{id}
 *   POST /admin/usuarios/{id}/senha              GET|POST /admin/usuarios/{id}/vinculos
 *   PATCH /admin/usuarios/{id}/vinculos/{vid}
 *   POST /admin/municipios/importacao (multipart, campo `arquivo`)   GET /admin/municipios/importacoes
 * Login de teste: plataforma@exemplo.com.br / senha1234.
 * ------------------------------------------------------------------------- */

interface UsuarioRow {
  id: string
  nome: string
  email: string
  whatsapp: string | null
  ativo: boolean
  dataCriacao: string
}
interface VinculoRow {
  id: string
  empresaId: string
  papel: Papel
  ativo: boolean
}
interface ImportacaoRow {
  id: string
  dataHora: string
  arquivo: string
  usuario: string
  linhasLidas: number
  criados: number
  atualizados: number
  inalterados: number
  rejeitados: number
}

const PAPEIS: Papel[] = ['administrador', 'operador', 'consulta']
const NOMES_EXTRA = ['Fabio Contador', 'Gisele Fiscal', 'Hugo Financeiro', 'Isabela Suporte', 'Julio Atendimento', 'Karina Vendas', 'Leonardo Analista', 'Marta Gestora']

function seedUsuarios(): { usuarios: UsuarioRow[]; vinculos: Record<string, VinculoRow[]> } {
  const usuarios: UsuarioRow[] = []
  const vinculos: Record<string, VinculoRow[]> = {}
  MOCK_USERS.forEach((u, i) => {
    usuarios.push({ id: u.id, nome: u.nome, email: u.email, whatsapp: i % 2 ? `1198000${String(1000 + i)}` : null, ativo: true, dataCriacao: '2026-01-10T09:00:00' })
    vinculos[u.id] = u.vinculos.map((v, j) => ({ id: `vin-${u.id}-${j + 1}`, empresaId: v.empresaId, papel: v.papel, ativo: true }))
  })
  for (let i = 0; i < 24; i++) {
    const id = `usu-${i + 6}`
    const nome = `${NOMES_EXTRA[i % NOMES_EXTRA.length]} ${i + 1}`
    usuarios.push({ id, nome, email: `usuario${i + 1}@exemplo.com.br`, whatsapp: i % 3 ? null : `119${String(70000000 + i * 1111)}`, ativo: i % 7 !== 0, dataCriacao: `2026-02-${String((i % 27) + 1).padStart(2, '0')}T10:00:00` })
    vinculos[id] = i % 4 === 0 ? [] : [{ id: `vin-${id}-1`, empresaId: i % 2 ? 'emp-1' : 'emp-2', papel: PAPEIS[i % 3]!, ativo: true }]
  }
  return { usuarios, vinculos }
}

let { usuarios, vinculos } = seedUsuarios()
let municipios: Municipio[] = MUNICIPIOS.map((m) => ({ ...m }))
let importacoes: ImportacaoRow[] = [
  { id: 'imp-0', dataHora: '2026-10-02T14:20:00', arquivo: 'municipios-ibge-2025.csv', usuario: 'Eva Plataforma', linhasLidas: 5571, criados: 5571, atualizados: 0, inalterados: 0, rejeitados: 0 },
]
let seq = 100

export const getMunicipios = (): Municipio[] => municipios

export function resetAdmin(): void {
  ;({ usuarios, vinculos } = seedUsuarios())
  municipios = MUNICIPIOS.map((m) => ({ ...m }))
  importacoes = importacoes.slice(0, 1)
  seq = 100
}

const bytes = (s: string) => new TextEncoder().encode(s).length

function filtrar(q: Record<string, string | number | boolean | null | undefined>): UsuarioRow[] {
  const busca = String(q.busca ?? '').trim().toLowerCase()
  let rows = busca ? usuarios.filter((u) => u.nome.toLowerCase().includes(busca) || u.email.toLowerCase().includes(busca)) : [...usuarios]
  const by = (q.sortBy ? String(q.sortBy) : 'nome') as keyof UsuarioRow
  const dir = String(q.sortDirection ?? 'asc').toLowerCase() === 'desc' ? -1 : 1
  rows = rows.sort((a, b) => String(a[by] ?? '').localeCompare(String(b[by] ?? ''), 'pt-BR', { numeric: true }) * dir)
  return rows
}

const comEmpresa = (v: VinculoRow) => ({ ...v, empresa: { id: v.empresaId, razaoSocial: EMPRESAS_MOCK[v.empresaId]?.razaoSocial ?? v.empresaId } })

async function importar(body: unknown, quem: string): Promise<Response> {
  const arquivo = body instanceof FormData ? body.get('arquivo') : null
  if (!arquivo || typeof arquivo === 'string') return erro(400, 'arquivo_obrigatorio', 'Envie o arquivo no campo "arquivo".')
  const texto = (await arquivo.text()).replace(/^﻿/, '')
  const linhas = texto.split(/\r?\n/)
  while (linhas.length && !linhas[linhas.length - 1]!.trim()) linhas.pop()
  if (linhas.length < 2) return erro(400, 'arquivo_invalido', 'O arquivo está vazio ou sem linhas de dados.')
  const sep = linhas[0]!.includes(';') ? ';' : ','
  let criados = 0
  let atualizados = 0
  let inalterados = 0
  const rejeitados: { linha: number; motivo: string }[] = []
  // Colunas: codigoIbge, nome, uf (1a linha = cabecalho).
  linhas.slice(1).forEach((conteudo, i) => {
    const linha = i + 2
    const [ibge = '', nome = '', uf = ''] = conteudo.split(sep).map((c) => c.trim())
    if (!/^\d{7}$/.test(ibge)) return void rejeitados.push({ linha, motivo: 'Código IBGE inválido (7 dígitos).' })
    if (!nome) return void rejeitados.push({ linha, motivo: 'Nome do município vazio.' })
    if (!/^[A-Za-z]{2}$/.test(uf)) return void rejeitados.push({ linha, motivo: 'UF inválida.' })
    const existente = municipios.find((m) => m.codigoIbge === ibge)
    if (!existente) {
      municipios.push({ id: `mun-${ibge}`, nome, uf: uf.toUpperCase(), codigoIbge: ibge })
      criados++
    } else if (existente.nome === nome && existente.uf === uf.toUpperCase()) inalterados++
    else {
      existente.nome = nome
      existente.uf = uf.toUpperCase()
      atualizados++
    }
  })
  const id = `imp-${++seq}`
  const linhasLidas = linhas.length - 1
  importacoes = [{ id, dataHora: new Date().toISOString().slice(0, 19), arquivo: (arquivo as File).name || 'arquivo', usuario: quem, linhasLidas, criados, atualizados, inalterados, rejeitados: rejeitados.length }, ...importacoes]
  return json(200, { id, nomeArquivo: (arquivo as File).name || 'arquivo', estado: rejeitados.length ? 'concluida_com_rejeicoes' : 'concluida', linhasLidas, criados, atualizados, inalterados, totalRejeitados: rejeitados.length, rejeitados })
}

/** Rotas /admin/**. Devolve `null` se o caminho nao for de admin. Exige administrador da plataforma (403 senao). */
export async function adminRoutes(req: { method: string; path: string; query?: Record<string, string | number | boolean | null | undefined>; body?: unknown }, ctx: { plataformaAdmin: boolean; nome: string }): Promise<Response | null> {
  const { method, path } = req
  if (!path.startsWith('/admin/')) return null
  if (!ctx.plataformaAdmin) return erro(403, 'acesso_negado', 'Acesso restrito ao administrador da plataforma.')
  const q = req.query ?? {}
  const body = (req.body ?? {}) as Record<string, unknown>

  if (path === '/admin/municipios/importacao' && method === 'POST') return importar(req.body, ctx.nome)
  if (path === '/admin/municipios/importacoes' && method === 'GET') return json(200, importacoes)

  if (path === '/admin/usuarios/count' && method === 'GET') return json(200, { count: filtrar(q).length })
  const faixa = /^\/admin\/usuarios\/(\d+)\/(\d+)$/.exec(path)
  if (faixa && method === 'GET') {
    const from = Number(faixa[1])
    const to = Number(faixa[2])
    if (to < from || to - from + 1 > 100) return erro(400, 'intervalo_invalido', 'Intervalo inválido (máximo de 100 registros).')
    return json(200, filtrar(q).slice(from, to + 1))
  }

  if (path === '/admin/usuarios' && method === 'POST') {
    const nome = String(body.nome ?? '').trim()
    const email = String(body.email ?? '').trim().toLowerCase()
    const senha = String(body.senha ?? '')
    if (!nome || !/^\S+@\S+\.\S+$/.test(email)) return erro(400, 'dados_invalidos', 'Informe nome e e-mail válidos.')
    if (bytes(senha) < 8 || bytes(senha) > 72) return erro(400, 'senha_invalida', 'A senha deve ter de 8 a 72 caracteres.')
    if (usuarios.some((u) => u.email === email)) return erro(409, 'email_duplicado', 'Já existe um usuário com este e-mail.')
    const novosVinculos = (Array.isArray(body.vinculos) ? body.vinculos : []) as { empresaId?: string; papel?: Papel }[]
    for (const v of novosVinculos) {
      if (!v.empresaId || !EMPRESAS_MOCK[v.empresaId]) return erro(404, 'empresa_nao_encontrada', 'Empresa não encontrada.')
      if (!v.papel || !PAPEIS.includes(v.papel)) return erro(400, 'papel_invalido', 'Papel inválido.')
    }
    const id = `usu-${++seq}`
    const novo: UsuarioRow = { id, nome, email, whatsapp: body.whatsapp ? String(body.whatsapp) : null, ativo: body.ativo !== false, dataCriacao: new Date().toISOString().slice(0, 19) }
    usuarios = [novo, ...usuarios]
    vinculos[id] = novosVinculos.map((v, j) => ({ id: `vin-${id}-${j + 1}`, empresaId: v.empresaId!, papel: v.papel!, ativo: true }))
    return json(200, novo)
  }

  const usuario = /^\/admin\/usuarios\/([^/]+)$/.exec(path)
  if (usuario && method === 'PATCH') {
    const u = usuarios.find((x) => x.id === usuario[1])
    if (!u) return erro(404, 'usuario_nao_encontrado', 'Usuário não encontrado.')
    if (!String(body.nome ?? '').trim()) return erro(400, 'dados_invalidos', 'Informe o nome.')
    Object.assign(u, { nome: String(body.nome).trim(), whatsapp: body.whatsapp ? String(body.whatsapp) : null, ativo: body.ativo !== false })
    return json(200, u)
  }

  const senha = /^\/admin\/usuarios\/([^/]+)\/senha$/.exec(path)
  if (senha && method === 'POST') {
    if (!usuarios.some((x) => x.id === senha[1])) return erro(404, 'usuario_nao_encontrado', 'Usuário não encontrado.')
    const nova = String(body.senha ?? '')
    if (bytes(nova) < 8 || bytes(nova) > 72) return erro(400, 'senha_invalida', 'A senha deve ter de 8 a 72 caracteres.')
    return new Response(null, { status: 204 })
  }

  const vins = /^\/admin\/usuarios\/([^/]+)\/vinculos(?:\/([^/]+))?$/.exec(path)
  if (vins) {
    const uid = vins[1]!
    if (!usuarios.some((x) => x.id === uid)) return erro(404, 'usuario_nao_encontrado', 'Usuário não encontrado.')
    const lista = (vinculos[uid] ??= [])
    if (!vins[2] && method === 'GET') return json(200, lista.map(comEmpresa))
    if (!vins[2] && method === 'POST') {
      const empresaId = String(body.empresaId ?? '')
      const papel = body.papel as Papel
      if (!EMPRESAS_MOCK[empresaId]) return erro(404, 'empresa_nao_encontrada', 'Empresa não encontrada.')
      if (!PAPEIS.includes(papel)) return erro(400, 'papel_invalido', 'Papel inválido.')
      if (lista.some((v) => v.empresaId === empresaId)) return erro(409, 'vinculo_duplicado', 'O usuário já tem vínculo com esta empresa.')
      const novo: VinculoRow = { id: `vin-${uid}-${++seq}`, empresaId, papel, ativo: true }
      lista.push(novo)
      return json(200, comEmpresa(novo))
    }
    if (vins[2] && method === 'PATCH') {
      const v = lista.find((x) => x.id === vins[2])
      if (!v) return erro(404, 'vinculo_nao_encontrado', 'Vínculo não encontrado.')
      const papel = body.papel as Papel
      if (!PAPEIS.includes(papel)) return erro(400, 'papel_invalido', 'Papel inválido.')
      v.papel = papel
      v.ativo = body.ativo !== false
      return json(200, comEmpresa(v))
    }
  }
  return null
}
