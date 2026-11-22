// GERADO por frontend/screenPage a partir de GestaoMunicipios. Nao edite: a saida e sobrescrita por inteiro.
import { useMemo, useState } from 'react'
import { Link, useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { hasRole, useSession } from '@/lib/auth'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { mutationFeedback } from '@/runtime/feedback'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import { useMunicipios, useCriarMunicipio } from '@/gen/api/municipio'
import { gestaoMunicipiosColumns } from './columns'
import { FormularioMunicipioForm } from '@/features/cadastros/municipio/FormularioMunicipio'

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
        editarMunicipio: hasRole(session, 'consulta') ? (row) => void navigate({ to: '/municipios/$id', params: { id: row.id } }) : undefined,
      }),
    [session, navigate],
  )
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useMunicipios(state, 'nome')
  const [dialogOpen, setDialogOpen] = useState(false)
  const criar = useCriarMunicipio()
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: () => setDialogOpen(false) })
  const abrir = () => {
    criar.reset()
    setDialogOpen(true)
  }
  const canCriar = hasRole(session, 'consulta')
  const novo = canCriar ? (
    <Button onClick={abrir}>
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
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>Novo município</DialogTitle>
          </DialogHeader>
          <FormularioMunicipioForm
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
