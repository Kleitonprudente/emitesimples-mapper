import type { Papel } from '@/lib/api'
import { adminRoutes } from '@/mocks/admin'
import { EMPRESAS_MOCK } from '@/mocks/fixtures'
import { erro, json } from '@/mocks/http'

/* ---------------------------------------------------------------------------
 * Mocks dos vinculos de usuario de uma empresa (so plataforma; contrato em features/admin/vinculosEmpresaApi.ts):
 *   GET|POST /admin/empresas/{empresaId}/vinculos      PATCH /admin/empresas/{empresaId}/vinculos/{vinculoId}
 * Os dados do usuario vem do proprio mock de /admin/usuarios (estado unico), sem editar admin.ts.
 * ------------------------------------------------------------------------- */

interface Row {
  id: string
  usuarioId: string
  papel: Papel
  ativo: boolean
  dataCriacao: string
}
interface UsuarioMin {
  id: string
  nome: string
  email: string
  ativo?: boolean
}

const PAPEIS: Papel[] = ['administrador', 'operador', 'consulta']

const seed = (): Record<string, Row[]> => ({
  'emp-1': [
    { id: 'vemp-1', usuarioId: 'usu-1', papel: 'administrador', ativo: true, dataCriacao: '2026-01-10T09:00:00' },
    { id: 'vemp-2', usuarioId: 'usu-2', papel: 'operador', ativo: true, dataCriacao: '2026-02-03T14:30:00' },
  ],
})

let porEmpresa = seed()
let seq = 100

export function resetVinculosEmpresa(): void {
  porEmpresa = seed()
  seq = 100
}

async function usuarios(): Promise<UsuarioMin[]> {
  const res = await adminRoutes({ method: 'GET', path: '/admin/usuarios/0/99', query: {} }, { plataformaAdmin: true, nome: '' })
  return res ? ((await res.json()) as UsuarioMin[]) : []
}

export async function vinculosEmpresaRoutes(
  req: { method: string; path: string; body?: unknown },
  ctx: { plataformaAdmin: boolean },
): Promise<Response | null> {
  const m = /^\/admin\/empresas\/([^/]+)\/vinculos(?:\/([^/]+))?$/.exec(req.path)
  if (!m) return null
  if (!ctx.plataformaAdmin) return erro(403, 'acesso_negado', 'Acesso restrito ao administrador da plataforma.')
  const empresaId = decodeURIComponent(m[1]!)
  if (!EMPRESAS_MOCK[empresaId]) return erro(404, 'empresa_nao_encontrada', 'Empresa não encontrada.')
  const body = (req.body ?? {}) as Record<string, unknown>
  const lista = (porEmpresa[empresaId] ??= [])
  const todos = await usuarios()
  const completo = (r: Row) => {
    const u = todos.find((x) => x.id === r.usuarioId)
    return { id: r.id, usuario: { id: r.usuarioId, nome: u?.nome ?? r.usuarioId, email: u?.email ?? '', ativo: u?.ativo !== false }, papel: r.papel, ativo: r.ativo, dataCriacao: r.dataCriacao }
  }

  if (!m[2] && req.method === 'GET') return json(200, lista.map(completo))

  if (!m[2] && req.method === 'POST') {
    const usuarioId = String(body.usuarioId ?? '')
    const papel = body.papel as Papel
    if (!todos.some((u) => u.id === usuarioId)) return erro(404, 'usuario_nao_encontrado', 'Usuário não encontrado.')
    if (!PAPEIS.includes(papel)) return erro(400, 'papel_invalido', 'Papel inválido.')
    if (lista.some((r) => r.usuarioId === usuarioId)) return erro(409, 'vinculo_ja_existe', 'Este usuário já está vinculado.')
    const novo: Row = { id: `vemp-${++seq}`, usuarioId, papel, ativo: body.ativo !== false, dataCriacao: new Date().toISOString().slice(0, 19) }
    lista.push(novo)
    return json(201, completo(novo))
  }

  if (m[2] && req.method === 'PATCH') {
    const r = lista.find((x) => x.id === m[2])
    if (!r) return erro(404, 'vinculo_nao_encontrado', 'Vínculo não encontrado.')
    if (body.papel !== undefined) {
      if (!PAPEIS.includes(body.papel as Papel)) return erro(400, 'papel_invalido', 'Papel inválido.')
      r.papel = body.papel as Papel
    }
    if (typeof body.ativo === 'boolean') r.ativo = body.ativo
    return json(200, completo(r))
  }
  return null
}
