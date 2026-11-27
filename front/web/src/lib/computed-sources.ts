import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api'
import { formatCurrency } from '@/runtime/format'

/* ---------------------------------------------------------------------------
 * Registro de fontes de COLUNA CALCULADA (coluna `[e] computed` do mapa de janela; front/mapas/README.md, "Coluna calculada").
 * O gerador emite `<ComputedCell source="saldoDaConta" id={row.original.id} />`; a fonte devolve o mapa id -> texto ja formatado.
 * Cada fonte e um hook (usa o cache do TanStack Query: varias celulas da mesma tela fazem UMA chamada).
 * ------------------------------------------------------------------------- */

export interface ComputedResult {
  valores: Map<string, string>
  isLoading: boolean
}

export type ComputedSource = () => ComputedResult

/** Saldo atual de cada conta ativa (GET /financeiro/saldos; docs/financeiro/README.md, F4-C2). Mesma chave de cache do financeiroApi. */
function useSaldoDaConta(): ComputedResult {
  const q = useQuery({
    queryKey: ['financeiro', 'livro', 'saldos'],
    queryFn: () => api<{ contas: { idConta: string; saldo: number }[]; total: number }>('/financeiro/saldos'),
  })
  return { valores: new Map((q.data?.contas ?? []).map((c) => [c.idConta, formatCurrency(c.saldo)])), isLoading: q.isLoading }
}

/** Fonte de demonstracao da vitrine (/admin/theme). */
function useExemplo(): ComputedResult {
  return { valores: new Map([['1', 'R$ 1.234,56'], ['2', 'R$ 0,00']]), isLoading: false }
}

const REGISTRO: Record<string, ComputedSource> = {
  saldoDaConta: useSaldoDaConta,
  exemplo: useExemplo,
}

const vazio: ComputedSource = () => ({ valores: new Map(), isLoading: false })

/** Fonte registrada pelo nome; nome desconhecido devolve uma fonte vazia (a celula mostra "—"). */
export function computedSource(nome: string): ComputedSource {
  return REGISTRO[nome] ?? vazio
}
