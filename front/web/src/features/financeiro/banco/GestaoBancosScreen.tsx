// GERADO por frontend/screenPage a partir de GestaoBancos. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useBancos } from '@/gen/api/banco'
import { gestaoBancosColumns } from './columns'

const CriarBancoIcon = resolveIcon('plus')
const ImportarCsvBancoCentralIcon = resolveIcon('upload')

const FACETS: Facet[] = [
  { id: 'integradoGateway', title: 'Aceita Cobrança Integrada', options: [{ value: 'true', label: 'Sim' }, { value: 'false', label: 'Não' }] },
]

export function GestaoBancosScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoBancosColumns({
        editarBanco: canAct(session, 'CONSULTA', true) ? (row) => void navigate({ to: '/bancos/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useBancos(state, 'nome')
  const canCriar = canAct(session, 'CONSULTA', true)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/bancos/$id', params: { id: 'novo' } })}>
      <CriarBancoIcon /> Novo banco
    </Button>
  ) : null

  const links = (
    <>
      <Button variant="outline" asChild>
        <Link to="/financeiro/csv">
          <ImportarCsvBancoCentralIcon /> Importar CSV do Banco Central
        </Link>
      </Button>
    </>
  )
  const empty = { title: 'Nenhum banco cadastrado ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Bancos</h1>
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
