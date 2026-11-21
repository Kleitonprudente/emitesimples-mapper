import { useCallback, useState } from 'react'

/** Estado de uma tabela paginada no servidor. */
export interface TableState {
  /** Pagina atual, base 0. */
  page: number
  pageSize: number
  sortBy?: string
  sortDirection: 'asc' | 'desc'
  /** Texto da busca (a tela decide em qual(is) campo(s) aplicar). */
  search: string
  /** Filtros facetados: campo -> valor escolhido ('' = todos). */
  filters: Record<string, string>
}

export const DEFAULT_PAGE_SIZE = 10

export function initialTableState(overrides: Partial<TableState> = {}): TableState {
  return { page: 0, pageSize: DEFAULT_PAGE_SIZE, sortDirection: 'asc', search: '', filters: {}, ...overrides }
}

export function useTableState(overrides?: Partial<TableState>) {
  const [state, setState] = useState<TableState>(() => initialTableState(overrides))
  const update = useCallback((patch: Partial<TableState>) => {
    // Mudou busca, filtro, ordem ou tamanho: volta para a 1a pagina.
    setState((s) => ({ ...s, page: 0, ...patch }))
  }, [])
  return [state, update] as const
}

/**
 * Traduz o estado para o contrato do back:
 *  - lista:  GET /api/<Entidade>/{from}/{to}   (intervalo inclusivo, maximo 100)
 *  - total:  GET /api/<Entidade>/count         (devolve {count})
 *  - filtros e ordenacao em query: sortBy, sortDirection, <campo>=<valor>; curinga '*'
 */
export function toListRequest(state: TableState, searchField?: string) {
  const from = state.page * state.pageSize
  const to = from + state.pageSize - 1
  const filters: Record<string, string> = {}
  for (const [k, v] of Object.entries(state.filters)) if (v) filters[k] = v
  if (searchField && state.search.trim()) filters[searchField] = `*${state.search.trim()}*`
  return {
    rangePath: `/${from}/${to}`,
    listQuery: { sortBy: state.sortBy, sortDirection: state.sortBy ? state.sortDirection : undefined, ...filters },
    countQuery: filters,
  }
}
