// GERADO por frontend/screenPage a partir de GestaoContasFinanceiras. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useContaFinanceiras } from '@/gen/api/contaFinanceira'
import { tipoValues, tipoLabels } from '@/gen/schemas/contaFinanceira'
import { gestaoContasFinanceirasColumns } from './columns'

const CriarContaFinanceiraIcon = resolveIcon('plus')
const ImportarExportarCsvIcon = resolveIcon('upload')

const FACETS: Facet[] = [
  { id: 'tipo', title: 'Tipo', options: tipoValues.map((value) => ({ value, label: tipoLabels[value] })) },
  { id: 'ativo', title: 'Ativa', options: [{ value: 'true', label: 'Sim' }, { value: 'false', label: 'Não' }] },
]

export function GestaoContasFinanceirasScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoContasFinanceirasColumns({
        editarContaFinanceira: canAct(session, 'ADMINISTRADOR', false) ? (row) => void navigate({ to: '/financeiro/contas/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useContaFinanceiras(state, 'nome')
  const canCriar = canAct(session, 'ADMINISTRADOR', false)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/financeiro/contas/$id', params: { id: 'novo' } })}>
      <CriarContaFinanceiraIcon /> Nova conta
    </Button>
  ) : null

  const links = (
    <>
      <Button variant="outline" asChild>
        <Link to="/financeiro/csv">
          <ImportarExportarCsvIcon /> Importar ou exportar CSV
        </Link>
      </Button>
    </>
  )
  const empty = { title: 'Nenhuma conta cadastrada ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Contas</h1>
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
