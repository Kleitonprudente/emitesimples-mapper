// GERADO por frontend/screenColumns a partir de GestaoContasFinanceiras. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import { tipoLabels } from '@/gen/schemas/contaFinanceira'
import type { ContaFinanceiraRow } from '@/gen/api/contaFinanceira'
import { ComputedCell } from '@/runtime/ComputedCell'
const EditarContaFinanceiraIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoContasFinanceirasColumnActions = {
  editarContaFinanceira?: (row: ContaFinanceiraRow) => void
}

export function gestaoContasFinanceirasColumns(actions: GestaoContasFinanceirasColumnActions = {}): ColumnDef<ContaFinanceiraRow>[] {
  return [
    { id: 'nome', accessorKey: 'nome', header: 'Nome' },
    { id: 'tipo', accessorKey: 'tipo', header: 'Tipo', cell: (c) => tipoLabels[String(c.getValue())] ?? '' },
    { id: 'codigoBanco', accessorKey: 'codigoBanco', header: 'Código do Banco' },
    { id: 'saldo', header: 'Saldo atual', enableSorting: false, cell: ({ row }) => <ComputedCell source="saldoDaConta" id={row.original.id} /> },
    { id: 'ativo', accessorKey: 'ativo', header: 'Ativa', cell: (c) => (c.getValue() ? 'Sim' : 'Não') },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarContaFinanceira && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarContaFinanceira?.(row.original)}>
              <EditarContaFinanceiraIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
