// GERADO por frontend/screenColumns a partir de GestaoBancos. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import type { BancoRow } from '@/gen/api/banco'
const EditarBancoIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoBancosColumnActions = {
  editarBanco?: (row: BancoRow) => void
}

export function gestaoBancosColumns(actions: GestaoBancosColumnActions = {}): ColumnDef<BancoRow>[] {
  return [
    { id: 'codigo', accessorKey: 'codigo', header: 'Código' },
    { id: 'nome', accessorKey: 'nome', header: 'Nome' },
    { id: 'integradoGateway', accessorKey: 'integradoGateway', header: 'Aceita Cobrança Integrada', cell: (c) => (c.getValue() ? 'Sim' : 'Não') },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarBanco && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarBanco?.(row.original)}>
              <EditarBancoIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
