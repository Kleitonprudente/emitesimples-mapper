// GERADO por frontend/screenPage a partir de GestaoMunicipios. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useMunicipios } from '@/gen/api/municipio'
import { gestaoMunicipiosColumns } from './columns'

const CriarMunicipioIcon = resolveIcon('plus')
const ImportarCsvIbgeIcon = resolveIcon('upload')

const FACETS: Facet[] = [
]

export function GestaoMunicipiosScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoMunicipiosColumns({
        editarMunicipio: canAct(session, 'CONSULTA', true) ? (row) => void navigate({ to: '/municipios/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useMunicipios(state, 'nome')
  const canCriar = canAct(session, 'CONSULTA', true)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/municipios/$id', params: { id: 'novo' } })}>
      <CriarMunicipioIcon /> Novo município
    </Button>
  ) : null

  const links = (
    <>
      <Button variant="outline" asChild>
        <Link to="/admin/municipios">
          <ImportarCsvIbgeIcon /> Importar CSV do IBGE
        </Link>
      </Button>
    </>
  )
  const empty = { title: 'Nenhum município cadastrado ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Gestão de Municípios</h1>
      <DataTable
        columns={columns}
        result={result}
        state={state}
        onStateChange={update}
        getRowId={(r) => r.id}
        searchPlaceholder="Buscar por nome…"
        facets={FACETS}
        toolbarActions={
          <>
            {novo}
            {links}
          </>
        }
        empty={empty}
      />
    </div>
  )
}
