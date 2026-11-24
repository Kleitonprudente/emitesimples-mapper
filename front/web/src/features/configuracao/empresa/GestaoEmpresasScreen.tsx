// GERADO por frontend/screenPage a partir de GestaoEmpresas. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useEmpresas } from '@/gen/api/empresa'
import { situacaoValues, situacaoLabels } from '@/gen/schemas/empresa'
import { gestaoEmpresasColumns } from './columns'

const CriarEmpresaIcon = resolveIcon('plus')

const FACETS: Facet[] = [
  { id: 'situacao', title: 'Situação da Conta', options: situacaoValues.map((value) => ({ value, label: situacaoLabels[value] })) },
]

export function GestaoEmpresasScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoEmpresasColumns({
        editarEmpresa: canAct(session, 'CONSULTA', true) ? (row) => void navigate({ to: '/empresas/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'razaoSocial' })
  const result = useEmpresas(state, 'razaoSocial')
  const canCriar = canAct(session, 'CONSULTA', true)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/empresas/$id', params: { id: 'novo' } })}>
      <CriarEmpresaIcon /> Nova empresa
    </Button>
  ) : null
  const empty = { title: 'Nenhuma empresa cadastrada ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Gestão de Empresas</h1>
      <DataTable
        columns={columns}
        result={result}
        state={state}
        onStateChange={update}
        getRowId={(r) => r.id}
        searchPlaceholder="Buscar por razão social…"
        facets={FACETS}
        toolbarActions={novo}
        empty={empty}
      />
    </div>
  )
}
