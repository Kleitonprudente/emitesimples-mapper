// GERADO por frontend/screenColumns a partir de GestaoMunicipios. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import type { MunicipioRow } from '@/gen/api/municipio'
const EditarMunicipioIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoMunicipiosColumnActions = {
  editarMunicipio?: (row: MunicipioRow) => void
}

export function gestaoMunicipiosColumns(actions: GestaoMunicipiosColumnActions = {}): ColumnDef<MunicipioRow>[] {
  return [
    { id: 'codigoIbge', accessorKey: 'codigoIbge', header: 'Código IBGE' },
    { id: 'nome', accessorKey: 'nome', header: 'Nome' },
    { id: 'uf', accessorKey: 'uf', header: 'UF' },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarMunicipio && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarMunicipio?.(row.original)}>
              <EditarMunicipioIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
