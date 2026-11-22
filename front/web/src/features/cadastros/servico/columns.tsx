// GERADO por frontend/screenColumns a partir de GestaoServicos. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import type { ServicoRow } from '@/gen/api/servico'
const EditarServicoIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoServicosColumnActions = {
  editarServico?: (row: ServicoRow) => void
}

export function gestaoServicosColumns(actions: GestaoServicosColumnActions = {}): ColumnDef<ServicoRow>[] {
  return [
    { id: 'nome', accessorKey: 'nome', header: 'Nome do Serviço' },
    { id: 'codigoServico', accessorKey: 'codigoServico', header: 'Código do Serviço' },
    { id: 'codigoNbs', accessorKey: 'codigoNbs', header: 'Código NBS' },
    { id: 'percentualTributosSimples', accessorKey: 'percentualTributosSimples', header: 'Tributos Aproximados do Simples (%)' },
    { id: 'ativo', accessorKey: 'ativo', header: 'Ativo', cell: (c) => (c.getValue() ? 'Sim' : 'Não') },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarServico && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarServico?.(row.original)}>
              <EditarServicoIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
