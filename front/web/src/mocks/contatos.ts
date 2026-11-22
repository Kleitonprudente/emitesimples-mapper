import type { Papel } from '@/lib/api'
import { erro, json } from '@/mocks/http'

/* ---------------------------------------------------------------------------
 * Mock dos contatos de uma Pessoa (contrato atomico, escrito a mao no back; convencao `/{Entidade}/{id}/{relacao}`):
 *   GET /Pessoa/{id}/contatos  -> [{ id, tipoContato, nome, whatsapp|null, email|null }] ordenado por nome (qualquer papel)
 *   PUT /Pessoa/{id}/contatos  -> corpo = lista DESEJADA completa [{ id?, tipoContato, nome, whatsapp?, email? }];
 *                                 200 com a lista salva (com ids), tudo ou nada.
 * Erros: 400 requisicao_invalida com detalhes.campos { 'contatos[2].nome': 'mensagem' }; 403 para `consulta` no PUT;
 * 404 se a pessoa nao existe.
 * ------------------------------------------------------------------------- */

export const TIPOS_CONTATO = ['socio', 'financeiro', 'comercial', 'fiscal', 'outro'] as const
export type TipoContato = (typeof TIPOS_CONTATO)[number]

export interface Contato {
  id: string
  tipoContato: TipoContato
  nome: string
  whatsapp: string | null
  email: string | null
}

/** Duas sementes para a primeira pessoa mock. */
const seed = (): Record<string, Contato[]> => ({
  'pes-0001': [
    { id: 'con-0001', tipoContato: 'financeiro', nome: 'Marta Lima', whatsapp: '11987654321', email: 'marta.lima@exemplo.com.br' },
    { id: 'con-0002', tipoContato: 'comercial', nome: 'Paulo Nunes', whatsapp: '11912345678', email: 'paulo.nunes@exemplo.com.br' },
  ],
})

let porPessoa = seed()
let seq = 2

export function resetContatos(): void {
  porPessoa = seed()
  seq = 2
}

const porNome = (a: Contato, b: Contato) => a.nome.localeCompare(b.nome, 'pt-BR')
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Devolve `null` se o caminho nao for de contatos de pessoa. */
export function contatosRoutes(
  req: { method: string; path: string; body?: unknown },
  ctx: { papel: Papel | null; plataformaAdmin: boolean; pessoaExiste: (id: string) => boolean },
): Response | null {
  const m = /^\/Pessoa\/([^/]+)\/contatos$/.exec(req.path)
  if (!m) return null
  const pessoaId = decodeURIComponent(m[1]!)

  if (req.method === 'PUT' && !ctx.plataformaAdmin && ctx.papel !== 'administrador' && ctx.papel !== 'operador') {
    return erro(403, 'acesso_negado', 'Seu papel não permite alterar contatos.')
  }
  if (!ctx.pessoaExiste(pessoaId)) return erro(404, 'nao_encontrado', 'Registro não encontrado.')

  if (req.method === 'GET') return json(200, [...(porPessoa[pessoaId] ?? [])].sort(porNome))

  if (req.method === 'PUT') {
    if (!Array.isArray(req.body)) return erro(400, 'requisicao_invalida', 'Corpo inválido: envie a lista de contatos.', { campos: { contatos: 'Envie a lista de contatos.' } })
    const atuais = porPessoa[pessoaId] ?? []
    const campos: Record<string, string> = {}
    req.body.forEach((raw, i) => {
      const c = (raw ?? {}) as Partial<Contato>
      const k = `contatos[${i}]`
      if (!c.tipoContato || !(TIPOS_CONTATO as readonly string[]).includes(c.tipoContato)) campos[`${k}.tipoContato`] = 'Tipo de contato inválido.'
      if (!c.nome || !String(c.nome).trim()) campos[`${k}.nome`] = 'Informe o nome.'
      if (c.email && !EMAIL.test(c.email)) campos[`${k}.email`] = 'E-mail inválido.'
      if (c.id && !atuais.some((a) => a.id === c.id)) campos[`${k}.id`] = 'Contato não encontrado nesta pessoa.'
    })
    if (Object.keys(campos).length) return erro(400, 'requisicao_invalida', 'Há campos inválidos.', { campos })
    const salva: Contato[] = (req.body as Partial<Contato>[]).map((c) => ({
      id: c.id ?? `con-${String(++seq).padStart(4, '0')}`,
      tipoContato: c.tipoContato!,
      nome: String(c.nome).trim(),
      whatsapp: c.whatsapp || null,
      email: c.email || null,
    }))
    porPessoa[pessoaId] = salva
    return json(200, [...salva].sort(porNome))
  }
  return null
}
