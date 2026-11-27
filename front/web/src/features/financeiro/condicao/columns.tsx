// GERADO por frontend/screenColumns a partir de GestaoCondicoesPagamento. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import { formaLabels } from '@/gen/schemas/condicaoPagamento'
import type { CondicaoPagamentoRow } from '@/gen/api/condicaoPagamento'
const EditarCondicaoPagamentoIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoCondicoesPagamentoColumnActions = {
  editarCondicaoPagamento?: (row: CondicaoPagamentoRow) => void
}

export function gestaoCondicoesPagamentoColumns(actions: GestaoCondicoesPagamentoColumnActions = {}): ColumnDef<CondicaoPagamentoRow>[] {
  return [
    { id: 'nome', accessorKey: 'nome', header: 'Nome' },
    { id: 'forma', accessorKey: 'forma', header: 'Forma de Pagamento', cell: (c) => formaLabels[String(c.getValue())] ?? '' },
    { id: 'parcelas', accessorKey: 'parcelas', header: 'Parcelas' },
    { id: 'ativo', accessorKey: 'ativo', header: 'Ativa', cell: (c) => (c.getValue() ? 'Sim' : 'Não') },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarCondicaoPagamento && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarCondicaoPagamento?.(row.original)}>
              <EditarCondicaoPagamentoIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
