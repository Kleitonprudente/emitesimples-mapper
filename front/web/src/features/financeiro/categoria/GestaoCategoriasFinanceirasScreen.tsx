// GERADO por frontend/screenPage a partir de GestaoCategoriasFinanceiras. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useCategoriaFinanceiras } from '@/gen/api/categoriaFinanceira'
import { tipoValues, tipoLabels } from '@/gen/schemas/categoriaFinanceira'
import { gestaoCategoriasFinanceirasColumns } from './columns'

const CriarCategoriaFinanceiraIcon = resolveIcon('plus')
const ImportarExportarCsvIcon = resolveIcon('upload')

const FACETS: Facet[] = [
  { id: 'tipo', title: 'Tipo', options: tipoValues.map((value) => ({ value, label: tipoLabels[value] })) },
  { id: 'ativo', title: 'Ativa', options: [{ value: 'true', label: 'Sim' }, { value: 'false', label: 'Não' }] },
]

export function GestaoCategoriasFinanceirasScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoCategoriasFinanceirasColumns({
        editarCategoriaFinanceira: canAct(session, 'ADMINISTRADOR', false) ? (row) => void navigate({ to: '/financeiro/categorias/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useCategoriaFinanceiras(state, 'nome')
  const canCriar = canAct(session, 'ADMINISTRADOR', false)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/financeiro/categorias/$id', params: { id: 'novo' } })}>
      <CriarCategoriaFinanceiraIcon /> Nova categoria
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
  const empty = { title: 'Nenhuma categoria cadastrada ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Categorias</h1>
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
