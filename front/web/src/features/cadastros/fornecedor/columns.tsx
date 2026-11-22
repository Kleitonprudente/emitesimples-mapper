// GERADO por frontend/screenColumns a partir de GestaoFornecedores. Nao edite: a saida e sobrescrita por inteiro.
import type { ColumnDef } from '@tanstack/react-table'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { resolveIcon } from '@/runtime/icons'
import { applyMask, badgeTone } from '@/runtime/format'
import { tipoPessoaLabels } from '@/gen/schemas/pessoa'
import type { PessoaRow } from '@/gen/api/pessoa'
const EditarFornecedorIcon = resolveIcon('pencil')

/** Acoes de linha do mapa: a tela passa o callback quando o usuario pode executa-la; sem callback o botao some. */
export type GestaoFornecedoresColumnActions = {
  editarFornecedor?: (row: PessoaRow) => void
}

export function gestaoFornecedoresColumns(actions: GestaoFornecedoresColumnActions = {}): ColumnDef<PessoaRow>[] {
  return [
    { id: 'documento', accessorKey: 'documento', header: 'CNPJ / CPF', cell: (c) => applyMask('documento', c.getValue()) },
    { id: 'nome', accessorKey: 'nome', header: 'Nome / Razão Social' },
    { id: 'tipoPessoa', accessorKey: 'tipoPessoa', header: 'Tipo de Pessoa', cell: (c) => <Badge tone={badgeTone(String(c.getValue() ?? ''))}>{ tipoPessoaLabels[String(c.getValue())] ?? '' }</Badge> },
    { id: 'email', accessorKey: 'email', header: 'E-mail' },
    { id: 'telefone', accessorKey: 'telefone', header: 'Telefone', cell: (c) => applyMask('phone', c.getValue()) },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          {actions.editarFornecedor && (
            <Button variant="ghost" size="icon" aria-label="Editar" title="Editar" onClick={() => actions.editarFornecedor?.(row.original)}>
              <EditarFornecedorIcon />
            </Button>
          )}
        </div>
      ),
    },
  ]
}
