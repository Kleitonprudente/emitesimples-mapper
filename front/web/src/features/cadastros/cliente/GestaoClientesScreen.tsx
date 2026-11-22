// GERADO por frontend/screenPage a partir de GestaoClientes. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { usePessoas } from '@/gen/api/pessoa'
import { tipoPessoaValues, tipoPessoaLabels } from '@/gen/schemas/pessoa'
import { gestaoClientesColumns } from './columns'

const CriarClienteIcon = resolveIcon('plus')

const FACETS: Facet[] = [
  { id: 'tipoPessoa', title: 'Tipo de Pessoa', options: tipoPessoaValues.map((value) => ({ value, label: tipoPessoaLabels[value] })) },
]

export function GestaoClientesScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoClientesColumns({
        editarCliente: canAct(session, 'operador', false) ? (row) => void navigate({ to: '/clientes/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = usePessoas(state, 'nome', { cliente: true })
  const canCriar = canAct(session, 'operador', false)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/clientes/$id', params: { id: 'novo' } })}>
      <CriarClienteIcon /> Novo cliente
    </Button>
  ) : null
  const empty = { title: 'Nenhum cliente cadastrado ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Clientes</h1>
      <DataTable
        columns={columns}
        result={result}
        state={state}
        onStateChange={update}
        getRowId={(r) => r.id}
        searchPlaceholder="Buscar por nome…"
        facets={FACETS}
        toolbarActions={novo}
        empty={empty}
      />
    </div>
  )
}
