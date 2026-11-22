import { api } from '@/lib/api'
import type { FkSource } from '@/runtime/Combobox'

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

const REGISTRO: Record<string, FkSource> = {
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
