import type { ReactNode } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState, ErrorState } from '@/runtime/states'
import type { TableResult } from '@/runtime/DataTable'

export interface CardField<T> {
  label: string
  value: (row: T) => ReactNode
}

/** Alternativa visual a tabela: um cartao por registro (bom em telas estreitas). */
export function CardList<T>({
  result,
  getKey,
  title,
  fields,
  actions,
  empty,
}: {
  result: TableResult<T>
  getKey: (row: T) => string
  title: (row: T) => ReactNode
  fields: CardField<T>[]
  actions?: (row: T) => ReactNode
  empty?: { title?: string; description?: string; action?: ReactNode }
}) {
  if (result.isLoading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" data-testid="card-skeleton">
        {Array.from({ length: 3 }, (_, i) => (
          <Card key={i}>
            <CardHeader>
              <Skeleton className="h-5 w-2/3" />
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-4/5" />
            </CardContent>
          </Card>
        ))}
      </div>
    )
  }
  if (result.isError) return <ErrorState error={result.error} onRetry={result.refetch} />
  const rows = result.data ?? []
  if (rows.length === 0) return <EmptyState title={empty?.title ?? 'Nenhum registro ainda.'} description={empty?.description} action={empty?.action} />

  return (
    <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {rows.map((row) => (
        <li key={getKey(row)}>
          <Card className="h-full">
            <CardHeader>
              <CardTitle>{title(row)}</CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm">
                {fields.map((f) => (
                  <div key={f.label} className="contents">
                    <dt className="text-muted-foreground">{f.label}</dt>
                    <dd className="min-w-0 truncate">{f.value(row)}</dd>
                  </div>
                ))}
              </dl>
              {actions && <div className="mt-3 flex justify-end gap-2">{actions(row)}</div>}
            </CardContent>
          </Card>
        </li>
      ))}
    </ul>
  )
}
