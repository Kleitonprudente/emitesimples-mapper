// GERADO por frontend/screenPage a partir de GestaoEmpresas. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { hasRole, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { mutationFeedback } from '@/runtime/feedback'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useEmpresas, useCriarEmpresa } from '@/gen/api/empresa'
import { situacaoValues, situacaoLabels } from '@/gen/schemas/empresa'
import { gestaoEmpresasColumns } from './columns'
import { FormularioEmpresaForm } from '@/features/configuracao/empresa/FormularioEmpresa'

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
        editarEmpresa: hasRole(session, 'consulta') ? (row) => void navigate({ to: '/empresas/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'razaoSocial' })
  const result = useEmpresas(state, 'razaoSocial')
  const [dialogOpen, setDialogOpen] = useState(false)
  const criar = useCriarEmpresa()
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: () => setDialogOpen(false) })
  const abrir = () => {
    criar.reset()
    setDialogOpen(true)
  }
  const canCriar = hasRole(session, 'consulta')
  const novo = canCriar ? (
    <Button onClick={abrir}>
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
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>Nova empresa</DialogTitle>
          </DialogHeader>
          <FormularioEmpresaForm
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
