import { computedSource } from '@/lib/computed-sources'
import { Skeleton } from '@/components/ui/skeleton'

/**
 * Celula de COLUNA CALCULADA (catalogo: front/docs/catalogo-componentes.md; mapa: `[e] computed`). Mostra o valor que a fonte registrada
 * em lib/computed-sources.ts tem para o id da linha; sem valor, "—". A fonte e fixa por coluna, entao o hook e sempre o mesmo.
 */
export function ComputedCell({ source, id }: { source: string; id: string }) {
  const usar = computedSource(source)
  const { valores, isLoading } = usar()
  if (isLoading) return <Skeleton className="h-4 w-20" aria-label="Carregando" />
  return <span>{valores.get(id) ?? '—'}</span>
}
