// GERADO por frontend/screenColumns a partir de GestaoCategoriasFinanceiras. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import { tipoLabels } from '@/gen/schemas/categoriaFinanceira'
import type { CategoriaFinanceiraRow } from '@/gen/api/categoriaFinanceira'
const EditarCategoriaFinanceiraIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoCategoriasFinanceirasColumnActions = {
  editarCategoriaFinanceira?: (row: CategoriaFinanceiraRow) => void
}

export function gestaoCategoriasFinanceirasColumns(actions: GestaoCategoriasFinanceirasColumnActions = {}): ColumnDef<CategoriaFinanceiraRow>[] {
  return [
    { id: 'nome', accessorKey: 'nome', header: 'Nome' },
    { id: 'tipo', accessorKey: 'tipo', header: 'Tipo', cell: (c) => tipoLabels[String(c.getValue())] ?? '' },
    { id: 'ativo', accessorKey: 'ativo', header: 'Ativa', cell: (c) => (c.getValue() ? 'Sim' : 'Não') },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarCategoriaFinanceira && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarCategoriaFinanceira?.(row.original)}>
              <EditarCategoriaFinanceiraIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
