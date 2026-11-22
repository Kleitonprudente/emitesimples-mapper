// GERADO por frontend/screenColumns a partir de GestaoEmpresas. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import { badgeTone } from '@/runtime/format'
import { situacaoLabels, regimeTributarioLabels } from '@/gen/schemas/empresa'
import type { EmpresaRow } from '@/gen/api/empresa'
const EditarEmpresaIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoEmpresasColumnActions = {
  editarEmpresa?: (row: EmpresaRow) => void
}

export function gestaoEmpresasColumns(actions: GestaoEmpresasColumnActions = {}): ColumnDef<EmpresaRow>[] {
  return [
    { id: 'documento', accessorKey: 'documento', header: 'CNPJ / CPF' },
    { id: 'razaoSocial', accessorKey: 'razaoSocial', header: 'Razão Social' },
    { id: 'nomeFantasia', accessorKey: 'nomeFantasia', header: 'Nome Fantasia' },
    { id: 'situacao', accessorKey: 'situacao', header: 'Situação da Conta', cell: (c) => <Badge tone={badgeTone(String(c.getValue() ?? ''))}>{ situacaoLabels[String(c.getValue())] ?? '' }</Badge> },
    { id: 'regimeTributario', accessorKey: 'regimeTributario', header: 'Regime Tributário', cell: (c) => regimeTributarioLabels[String(c.getValue())] ?? '' },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarEmpresa && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarEmpresa?.(row.original)}>
              <EditarEmpresaIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
