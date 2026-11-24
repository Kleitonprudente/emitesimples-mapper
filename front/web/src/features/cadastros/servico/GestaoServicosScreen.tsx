// GERADO por frontend/screenPage a partir de GestaoServicos. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useServicos } from '@/gen/api/servico'
import { gestaoServicosColumns } from './columns'

const CriarServicoIcon = resolveIcon('plus')

const FACETS: Facet[] = [
]

export function GestaoServicosScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoServicosColumns({
        editarServico: canAct(session, 'OPERADOR', false) ? (row) => void navigate({ to: '/servicos/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useServicos(state, 'nome')
  const canCriar = canAct(session, 'OPERADOR', false)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/servicos/$id', params: { id: 'novo' } })}>
      <CriarServicoIcon /> Novo serviço
    </Button>
  ) : null
  const empty = { title: 'Nenhum serviço cadastrado ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Serviços</h1>
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
