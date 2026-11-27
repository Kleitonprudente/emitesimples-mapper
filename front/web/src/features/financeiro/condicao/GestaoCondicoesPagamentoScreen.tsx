// GERADO por frontend/screenPage a partir de GestaoCondicoesPagamento. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useCondicaoPagamentos } from '@/gen/api/condicaoPagamento'
import { formaValues, formaLabels } from '@/gen/schemas/condicaoPagamento'
import { gestaoCondicoesPagamentoColumns } from './columns'

const CriarCondicaoPagamentoIcon = resolveIcon('plus')
const ImportarExportarCsvIcon = resolveIcon('upload')

const FACETS: Facet[] = [
  { id: 'forma', title: 'Forma de Pagamento', options: formaValues.map((value) => ({ value, label: formaLabels[value] })) },
  { id: 'ativo', title: 'Ativa', options: [{ value: 'true', label: 'Sim' }, { value: 'false', label: 'Não' }] },
]

export function GestaoCondicoesPagamentoScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoCondicoesPagamentoColumns({
        editarCondicaoPagamento: canAct(session, 'ADMINISTRADOR', false) ? (row) => void navigate({ to: '/financeiro/condicoes/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useCondicaoPagamentos(state, 'nome')
  const canCriar = canAct(session, 'ADMINISTRADOR', false)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/financeiro/condicoes/$id', params: { id: 'novo' } })}>
      <CriarCondicaoPagamentoIcon /> Nova condição
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
  const empty = { title: 'Nenhuma condição de pagamento cadastrada ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Condições de pagamento</h1>
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
