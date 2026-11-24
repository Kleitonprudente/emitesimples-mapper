import { useMemo } from 'react'
import { useNavigate } from '@tanstack/react-router'
import type { ColumnDef } from '@tanstack/react-table'
import { Eye, FileText } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { ESTADO, ESTADOS, urlPdf, useNotas, type NotaItem } from '@/features/notas/notasApi'
import { abrirPdf } from '@/lib/api'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { notifyError } from '@/runtime/feedback'
import { formatCurrency, formatDateTime, maskDocumento } from '@/runtime/format'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'

/* ---------------------------------------------------------------------------
 * Notas Fiscais > NFS-e: lista. Escrito a mao com o runtime: as telas de nota tem fluxo proprio de envio e acompanhamento
 * (decisao explicita do usuario). Recarrega sozinha a cada 10 s enquanto houver nota processando.
 * ------------------------------------------------------------------------- */

const NovaNotaIcon = resolveIcon('plus')

const FACETS: Facet[] = [{ id: 'estado', title: 'Situação', options: ESTADOS.map((e) => ({ value: e, label: ESTADO[e].rotulo })) }]

function AcaoIcone({ rotulo, onClick, children }: { rotulo: string; onClick: () => void; children: React.ReactNode }) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Button variant="ghost" size="icon" aria-label={rotulo} onClick={onClick}>
          {children}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{rotulo}</TooltipContent>
    </Tooltip>
  )
}

function colunas(abrir: (n: NotaItem) => void): ColumnDef<NotaItem>[] {
  return [
    { id: 'numero', header: 'Número', enableSorting: false, cell: ({ row }) => (row.original.numero ? `${row.original.serie}-${row.original.numero}` : '—') },
    {
      id: 'cliente',
      header: 'Cliente',
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex flex-col">
          <span>{row.original.pessoa.nome}</span>
          {row.original.pessoa.documento && <span className="text-xs text-muted-foreground">{maskDocumento(row.original.pessoa.documento)}</span>}
        </div>
      ),
    },
    { id: 'servico', header: 'Serviço', enableSorting: false, cell: ({ row }) => row.original.servico.nome },
    { id: 'valor', header: 'Valor', enableSorting: false, cell: ({ row }) => formatCurrency(row.original.valorServicos) },
    { id: 'emissao', header: 'Emissão', enableSorting: false, cell: ({ row }) => formatDateTime(row.original.dataEmissao) },
    { id: 'situacao', header: 'Situação', enableSorting: false, cell: ({ row }) => <Badge tone={ESTADO[row.original.estado].tone}>{ESTADO[row.original.estado].rotulo}</Badge> },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          <AcaoIcone rotulo="Abrir" onClick={() => abrir(row.original)}>
            <Eye />
          </AcaoIcone>
          {row.original.temPdf && (
            <AcaoIcone rotulo="Visualizar PDF" onClick={() => void abrirPdf(urlPdf(row.original.id)).catch((e) => notifyError(e, 'Não foi possível abrir o PDF'))}>
              <FileText />
            </AcaoIcone>
          )}
        </div>
      ),
    },
  ]
}

export function NotasLista() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(() => colunas((n) => void navigate({ to: '/notas/$id', params: { id: n.id } })), [navigate])
  const [state, update] = useTableState()
  const result = useNotas(state)
  const novo = canAct(session, 'operador') ? (
    <Button onClick={() => void navigate({ to: '/notas/novo' })}>
      <NovaNotaIcon /> Nova nota
    </Button>
  ) : null
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">NFS-e</h1>
      <DataTable
        columns={columns}
        result={result}
        state={state}
        onStateChange={update}
        getRowId={(r) => r.id}
        searchPlaceholder="Buscar por número ou cliente…"
        facets={FACETS}
        toolbarActions={novo}
        empty={{ title: 'Nenhuma nota emitida ainda.', action: novo || undefined }}
      />
    </div>
  )
}
