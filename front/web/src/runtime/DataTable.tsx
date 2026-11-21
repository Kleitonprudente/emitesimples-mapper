import { useEffect, useState, type ReactNode } from 'react'
import { flexRender, getCoreRowModel, useReactTable, type ColumnDef } from '@tanstack/react-table'
import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight, ChevronsUpDown, Search, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { EmptyState, ErrorState } from '@/runtime/states'
import type { TableState } from '@/runtime/table-state'

export interface Facet {
  /** Campo do filtro (chave em state.filters). */
  id: string
  title: string
  options: { value: string; label: string }[]
}

/** Resultado de uma consulta (a forma de um hook do TanStack Query). */
export interface TableResult<T> {
  data?: T[]
  /** Total no servidor (GET /count). Se ausente, a paginacao so sabe se ha "proxima" pela pagina cheia. */
  count?: number
  isLoading: boolean
  isFetching?: boolean
  isError: boolean
  error?: unknown
  refetch?: () => void
}

export interface DataTableProps<T> {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  columns: ColumnDef<T, any>[]
  result: TableResult<T>
  state: TableState
  onStateChange: (patch: Partial<TableState>) => void
  facets?: Facet[]
  searchPlaceholder?: string
  /** Estado vazio de verdade (sem busca/filtro ativos), normalmente com a acao de criar. */
  empty?: { title?: string; description?: string; action?: ReactNode }
  /** Acoes do cabecalho da tabela (ex.: botao "Novo"). */
  toolbarActions?: ReactNode
  pageSizeOptions?: number[]
  getRowId?: (row: T) => string
}

const ALL = '__all__'

function SearchBox({ value, placeholder, onCommit }: { value: string; placeholder?: string; onCommit: (v: string) => void }) {
  const [text, setText] = useState(value)
  // Busca limpa por fora (ex.: "Limpar filtros"): sincroniza o texto durante a renderizacao, sem efeito.
  const [seen, setSeen] = useState(value)
  if (seen !== value) {
    setSeen(value)
    setText(value)
  }
  // Debounce: so consulta o servidor 300ms depois de parar de digitar.
  useEffect(() => {
    if (text === value) return
    const t = setTimeout(() => onCommit(text), 300)
    return () => clearTimeout(t)
  }, [text, value, onCommit])
  return (
    <div className="relative w-full sm:max-w-xs">
      <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
      <Input type="search" aria-label="Buscar" placeholder={placeholder ?? 'Buscar…'} className="pl-9" value={text} onChange={(e) => setText(e.target.value)} />
    </div>
  )
}

export function DataTable<T>({
  columns,
  result,
  state,
  onStateChange,
  facets = [],
  searchPlaceholder,
  empty,
  toolbarActions,
  pageSizeOptions = [10, 25, 50],
  getRowId,
}: DataTableProps<T>) {
  const rows = result.data ?? []
  const table = useReactTable({
    data: rows,
    columns,
    getCoreRowModel: getCoreRowModel(),
    manualSorting: true,
    manualPagination: true,
    manualFiltering: true,
    getRowId: getRowId ? (r) => getRowId(r) : undefined,
  })

  const hasFilter = !!state.search || Object.values(state.filters).some(Boolean)
  const from = state.page * state.pageSize
  const total = result.count
  const hasNext = total !== undefined ? from + rows.length < total : rows.length === state.pageSize
  const lastPage = total !== undefined ? Math.max(0, Math.ceil(total / state.pageSize) - 1) : undefined

  function toggleSort(id: string) {
    if (state.sortBy !== id) onStateChange({ sortBy: id, sortDirection: 'asc' })
    else if (state.sortDirection === 'asc') onStateChange({ sortBy: id, sortDirection: 'desc' })
    else onStateChange({ sortBy: undefined, sortDirection: 'asc' })
  }

  const showSkeleton = result.isLoading
  const showError = !showSkeleton && result.isError
  const showEmpty = !showSkeleton && !showError && rows.length === 0

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-center">
        <SearchBox value={state.search} placeholder={searchPlaceholder} onCommit={(v) => onStateChange({ search: v })} />
        {facets.map((f) => (
          <Select key={f.id} value={state.filters[f.id] || ALL} onValueChange={(v) => onStateChange({ filters: { ...state.filters, [f.id]: v === ALL ? '' : v } })}>
            <SelectTrigger aria-label={f.title} className="sm:w-48">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>{f.title}: todos</SelectItem>
              {f.options.map((o) => (
                <SelectItem key={o.value} value={o.value}>
                  {o.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        ))}
        {hasFilter && (
          <Button variant="ghost" size="sm" onClick={() => onStateChange({ search: '', filters: {} })}>
            <X /> Limpar filtros
          </Button>
        )}
        {toolbarActions && <div className="sm:ml-auto">{toolbarActions}</div>}
      </div>

      <div className="rounded-lg border bg-card" aria-busy={result.isFetching || undefined}>
        <Table>
          <TableHeader>
            {table.getHeaderGroups().map((hg) => (
              <TableRow key={hg.id} className="hover:bg-transparent">
                {hg.headers.map((h) => {
                  const sortable = h.column.columnDef.enableSorting !== false && h.column.id !== 'actions'
                  const active = state.sortBy === h.column.id
                  return (
                    <TableHead key={h.id} aria-sort={active ? (state.sortDirection === 'asc' ? 'ascending' : 'descending') : undefined}>
                      {h.isPlaceholder ? null : sortable ? (
                        <button type="button" onClick={() => toggleSort(h.column.id)} className="-ml-1 inline-flex items-center gap-1 rounded-sm px-1 py-0.5 hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none">
                          {flexRender(h.column.columnDef.header, h.getContext())}
                          {active ? state.sortDirection === 'asc' ? <ArrowUp className="size-3.5" aria-hidden /> : <ArrowDown className="size-3.5" aria-hidden /> : <ChevronsUpDown className="size-3.5 opacity-40" aria-hidden />}
                        </button>
                      ) : (
                        flexRender(h.column.columnDef.header, h.getContext())
                      )}
                    </TableHead>
                  )
                })}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {showSkeleton &&
              Array.from({ length: Math.min(state.pageSize, 5) }, (_, i) => (
                <TableRow key={i} data-testid="skeleton-row" className="hover:bg-transparent">
                  {table.getVisibleLeafColumns().map((c) => (
                    <TableCell key={c.id}>
                      <Skeleton className="h-4 w-full max-w-40" />
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            {!showSkeleton &&
              !showError &&
              table.getRowModel().rows.map((row) => (
                <TableRow key={row.id}>
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</TableCell>
                  ))}
                </TableRow>
              ))}
          </TableBody>
        </Table>
        {showError && <ErrorState error={result.error} onRetry={result.refetch} />}
        {showEmpty &&
          (hasFilter ? (
            <EmptyState title="Nenhum resultado encontrado." description="Ajuste a busca ou os filtros." action={<Button variant="outline" size="sm" onClick={() => onStateChange({ search: '', filters: {} })}>Limpar filtros</Button>} />
          ) : (
            <EmptyState title={empty?.title ?? 'Nenhum registro ainda.'} description={empty?.description} action={empty?.action} />
          ))}
      </div>

      <div className="flex flex-col items-center justify-between gap-2 text-sm text-muted-foreground sm:flex-row">
        <span aria-live="polite">
          {total !== undefined
            ? total === 0
              ? '0 registros'
              : `${from + 1}–${from + rows.length} de ${total}`
            : rows.length > 0
              ? `${from + 1}–${from + rows.length}`
              : ''}
        </span>
        <div className="flex items-center gap-2">
          <span>Linhas</span>
          <Select value={String(state.pageSize)} onValueChange={(v) => onStateChange({ pageSize: Number(v) })}>
            <SelectTrigger aria-label="Linhas por página" className="h-8 w-20">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {pageSizeOptions.map((n) => (
                <SelectItem key={n} value={String(n)}>
                  {n}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button variant="outline" size="icon" aria-label="Página anterior" disabled={state.page === 0 || showSkeleton} onClick={() => onStateChange({ page: state.page - 1 })}>
            <ChevronLeft />
          </Button>
          <span className="min-w-16 text-center">{lastPage !== undefined ? `${state.page + 1} / ${lastPage + 1}` : `Página ${state.page + 1}`}</span>
          <Button variant="outline" size="icon" aria-label="Próxima página" disabled={!hasNext || showSkeleton} onClick={() => onStateChange({ page: state.page + 1 })}>
            <ChevronRight />
          </Button>
        </div>
      </div>
    </div>
  )
}
