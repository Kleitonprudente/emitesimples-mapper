import { api } from '@/lib/api'
import type { FkSource } from '@/runtime/Combobox'
import { maskDocumento } from '@/runtime/format'

/* ---------------------------------------------------------------------------
 * Registro de fontes de chave estrangeira (Combobox com busca assincrona).
 * O gerador de telas emite `fk: fkSource('Municipio')`. Entidade sem registro usa a busca generica
 * (GET /<Entidade>?nome=*termo*&sortBy=nome, rotulo = `nome`).
 * ------------------------------------------------------------------------- */

interface Linha {
  id: string
  nome?: string
  uf?: string
  [k: string]: unknown
}

function buscaPorCampo(entidade: string, campo: string, rotulo: (r: Linha) => string): FkSource['search'] {
  return async (term) => {
    const rows = await api<Linha[]>(`/${entidade}`, { query: { [campo]: term.trim() ? `*${term.trim()}*` : undefined, sortBy: campo } })
    return rows.map((r) => ({ id: r.id, label: rotulo(r) }))
  }
}

const rotuloMunicipio = (v: { nome?: unknown; uf?: unknown }): string | undefined =>
  v.nome ? `${v.nome}${v.uf ? ` - ${v.uf}` : ''}` : undefined

/** Busca com filtro fixo na lista paginada (`/{Entidade}/0/49`): para fontes que so oferecem parte dos registros. */
function buscaFiltrada(entidade: string, filtro: Record<string, string | boolean>, rotulo: (r: Linha) => string): FkSource['search'] {
  return async (term) => {
    const rows = await api<Linha[]>(`/${entidade}/0/49`, { query: { ...filtro, nome: term.trim() ? `*${term.trim()}*` : undefined, sortBy: 'nome' } })
    return rows.map((r) => ({ id: r.id, label: rotulo(r) }))
  }
}

const rotuloCliente = (v: { nome?: unknown; documento?: unknown }): string | undefined =>
  v.nome ? `${v.nome}${v.documento ? ` - ${maskDocumento(v.documento)}` : ''}` : undefined

const semAcento = (s: unknown): string => String(s ?? '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

const nomeDoPai = (c: Record<string, unknown>): string | undefined => {
  const p = c.categoriaPai as { nome?: unknown } | null | undefined
  return p?.nome ? String(p.nome) : undefined
}

/** 'Pai › Filha' (ou so o nome, na categoria de topo). */
const rotuloCategoria = (v: Record<string, unknown>): string | undefined =>
  v.nome ? (nomeDoPai(v) ? `${nomeDoPai(v)} › ${v.nome}` : String(v.nome)) : undefined

/** As ativas do tipo que nao sao pai de nenhuma, agrupadas pelo pai; a busca casa o nome da filha ou do pai (sem acento). */
function categoriasQueAceitamLancamento(tipo: 'RECEITA' | 'DESPESA'): FkSource['search'] {
  return async (term) => {
    const rows = await api<Linha[]>('/CategoriaFinanceira/0/99', { query: { ativo: true, tipo, sortBy: 'nome' } })
    const pais = new Set(rows.map((r) => (r.categoriaPai as { id?: string } | null | undefined)?.id).filter(Boolean))
    const t = semAcento(term.trim())
    return rows
      .filter((r) => !pais.has(r.id))
      .filter((r) => !t || semAcento(r.nome).includes(t) || semAcento(nomeDoPai(r)).includes(t))
      .map((r) => ({ id: r.id, label: String(r.nome ?? r.id), group: nomeDoPai(r) }))
      .sort((a, b) => (a.group ?? '').localeCompare(b.group ?? '', 'pt-BR') || a.label.localeCompare(b.label, 'pt-BR'))
  }
}

const REGISTRO: Record<string, FkSource> = {
  /** Cliente: Pessoa com cliente=true; rotulo 'Nome - documento mascarado'. */
  Cliente: {
    key: 'Cliente',
    search: buscaFiltrada('Pessoa', { cliente: true }, (p) => rotuloCliente(p) ?? p.id),
    labelOf: rotuloCliente,
  },
  /** Servico: so os ativos; rotulo 'nome'. */
  Servico: {
    key: 'Servico',
    search: buscaFiltrada('Servico', { ativo: true }, (s) => String(s.nome ?? s.id)),
    labelOf: (v) => (v.nome ? String(v.nome) : undefined),
  },
  /** CategoriaFinanceira (pai da categoria, financeiro): so as ativas; o back confere tipo e nivel. Rotulo 'nome'. */
  CategoriaFinanceira: {
    key: 'CategoriaFinanceira',
    search: buscaFiltrada('CategoriaFinanceira', { ativo: true }, (c) => String(c.nome ?? c.id)),
    labelOf: (v) => (v.nome ? String(v.nome) : undefined),
  },
  /** Fornecedor: Pessoa com fornecedor=true (titulo a pagar); rotulo 'Nome - documento mascarado'. */
  Fornecedor: {
    key: 'Fornecedor',
    search: buscaFiltrada('Pessoa', { fornecedor: true }, (p) => rotuloCliente(p) ?? p.id),
    labelOf: rotuloCliente,
  },
  /**
   * Categorias de receita e de despesa que ACEITAM LANCAMENTO (titulo e lancamento avulso; docs/financeiro/README.md, F4-C1): as filhas,
   * agrupadas pelo nome do pai, e as de topo sem filhas. O pai so agrupa e nao se escolhe; o back confere de novo.
   */
  CategoriaReceita: { key: 'CategoriaReceita', search: categoriasQueAceitamLancamento('RECEITA'), labelOf: rotuloCategoria },
  CategoriaDespesa: { key: 'CategoriaDespesa', search: categoriasQueAceitamLancamento('DESPESA'), labelOf: rotuloCategoria },
  /** ContaFinanceira: so as ativas (baixa, avulso, transferencia). */
  ContaFinanceira: {
    key: 'ContaFinanceira',
    search: buscaFiltrada('ContaFinanceira', { ativo: true }, (c) => String(c.nome ?? c.id)),
    labelOf: (v) => (v.nome ? String(v.nome) : undefined),
  },
  /** CondicaoPagamento (nota, docs/financeiro/README.md 9.1 E1): so as ativas; rotulo 'nome'. O financeiro confere de novo. */
  CondicaoPagamento: {
    key: 'CondicaoPagamento',
    search: buscaFiltrada('CondicaoPagamento', { ativo: true }, (c) => String(c.nome ?? c.id)),
    labelOf: (v) => (v.nome ? String(v.nome) : undefined),
  },
  Municipio: {
    key: 'Municipio',
    search: buscaPorCampo('Municipio', 'nome', (m) => rotuloMunicipio(m) ?? m.id),
    labelOf: rotuloMunicipio,
  },
}

const cache = new Map<string, FkSource>()

/** Fonte de FK da entidade. A mesma instancia e devolvida a cada chamada (estavel para React/Query). */
export function fkSource(entidade: string): FkSource {
  const registrada = REGISTRO[entidade]
  if (registrada) return registrada
  let s = cache.get(entidade)
  if (!s) {
    s = {
      key: entidade,
      search: buscaPorCampo(entidade, 'nome', (r) => String(r.nome ?? r.id)),
      labelOf: (v) => (v.nome ? String(v.nome) : undefined),
    }
    cache.set(entidade, s)
  }
  return s
}
