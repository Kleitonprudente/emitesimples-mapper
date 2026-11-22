// GERADO por frontend/screenEditorById a partir de FormularioMunicipio. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useAtualizarMunicipio, useCriarMunicipio, useMunicipio } from '@/gen/api/municipio'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioMunicipioForm, FormularioMunicipioFieldNames, type FormularioMunicipioValues } from './FormularioMunicipio'

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioMunicipioScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioMunicipioNovo /> : <FormularioMunicipioEditar id={id} />
}

function FormularioMunicipioNovo() {
  const criar = useCriarMunicipio()
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/municipios' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Novo município</h1>
      <FormularioMunicipioForm
        isSubmitting={criar.isPending}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={(v) => criar.mutateAsync(v).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}

function FormularioMunicipioEditar({ id }: { id: string }) {
  const loaded = useMunicipio(id)
  const salvar = useAtualizarMunicipio(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/municipios' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioMunicipioValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioMunicipioFieldNames.includes(k)))
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de município</h1>
      <FormularioMunicipioForm
        values={values}
        isSubmitting={salvar.isPending}
        serverError={salvar.error}
        onCancel={voltar}
        onSubmit={(v) => salvar.mutateAsync({ ...preservar, ...v } as typeof v).then(fb.onSuccess, () => {})}
      />
      {loaded.data.dataCriacao && (
        <p className="text-xs text-muted-foreground">
          Criado em {formatDateTime(loaded.data.dataCriacao)}{loaded.data.dataAtualizacao ? <> &middot; Atualizado em {formatDateTime(loaded.data.dataAtualizacao)}</> : null}
        </p>
      )}
    </div>
  )
}
