// GERADO por frontend/screenPage a partir de GestaoPessoas. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { hasRole, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { mutationFeedback } from '@/runtime/feedback'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { usePessoas, useCriarPessoa } from '@/gen/api/pessoa'
import { tipoPessoaValues, tipoPessoaLabels } from '@/gen/schemas/pessoa'
import { gestaoPessoasColumns } from './columns'
import { FormularioPessoaForm } from '@/features/cadastros/pessoa/FormularioPessoa'

const CriarPessoaIcon = resolveIcon('plus')

const FACETS: Facet[] = [
  { id: 'tipoPessoa', title: 'Tipo de Pessoa', options: tipoPessoaValues.map((value) => ({ value, label: tipoPessoaLabels[value] })) },
]

export function GestaoPessoasScreen() {
  const session = useSession()
  const navigate = useNavigate()
  const columns = useMemo(
    () =>
      gestaoPessoasColumns({
        editarPessoa: hasRole(session, 'operador') ? (row) => void navigate({ to: '/pessoas/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = usePessoas(state, 'nome')
  const [dialogOpen, setDialogOpen] = useState(false)
  const criar = useCriarPessoa()
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: () => setDialogOpen(false) })
  const abrir = () => {
    criar.reset()
    setDialogOpen(true)
  }
  const canCriar = hasRole(session, 'operador')
  const novo = canCriar ? (
    <Button onClick={abrir}>
      <CriarPessoaIcon /> Nova pessoa
    </Button>
  ) : null
  const empty = { title: 'Nenhuma pessoa cadastrada ainda.', action: novo || undefined }
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Gestão de Pessoas</h1>
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
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>Nova pessoa</DialogTitle>
          </DialogHeader>
          <FormularioPessoaForm
            isSubmitting={criar.isPending}
            serverError={criar.error}
            onCancel={() => setDialogOpen(false)}
            onSubmit={(v) => criar.mutateAsync(v).then(fb.onSuccess, () => {})}
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}
