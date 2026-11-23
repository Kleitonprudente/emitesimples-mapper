// GERADO por frontend/screenPage a partir de GestaoPessoas. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { canAct, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { usePessoas } from '@/gen/api/pessoa'
import { tipoPessoaValues, tipoPessoaLabels } from '@/gen/schemas/pessoa'
import { gestaoPessoasColumns } from './columns'

const CriarPessoaIcon = resolveIcon('plus')

const FACETS: Facet[] = [
  { id: 'tipoPessoa', title: 'Tipo de Pessoa', options: tipoPessoaValues.map((value) => ({ value, label: tipoPessoaLabels[value] })) },
  { id: 'cliente', title: 'Cliente', options: [{ value: 'true', label: 'Sim' }, { value: 'false', label: 'Não' }] },
  { id: 'fornecedor', title: 'Fornecedor', options: [{ value: 'true', label: 'Sim' }, { value: 'false', label: 'Não' }] },
]

export function GestaoPessoasScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoPessoasColumns({
        editarPessoa: canAct(session, 'operador', false) ? (row) => void navigate({ to: '/pessoas/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = usePessoas(state, 'nome')
  const canCriar = canAct(session, 'operador', false)
  const novo = canCriar ? (
    <Button onClick={() => void navigate({ to: '/pessoas/$id', params: { id: 'novo' } })}>
      <CriarPessoaIcon /> Nova pessoa
    </Button>
  ) : null
  const empty = { title: 'Nenhuma pessoa cadastrada ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Pessoas</h1>
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
