// GERADO por frontend/screenEditorById a partir de FormularioServico. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useAtualizarServico, useCriarServico, useServico } from '@/gen/api/servico'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioServicoForm, FormularioServicoFieldNames, type FormularioServicoValues } from './FormularioServico'

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioServicoScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioServicoNovo /> : <FormularioServicoEditar id={id} />
}

function FormularioServicoNovo() {
  const criar = useCriarServico()
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/servicos' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Novo serviço</h1>
      <FormularioServicoForm
        isSubmitting={criar.isPending}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={(v) => criar.mutateAsync(v).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}

function FormularioServicoEditar({ id }: { id: string }) {
  const loaded = useServico(id)
  const salvar = useAtualizarServico(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/servicos' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioServicoValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioServicoFieldNames.includes(k)))
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de serviço</h1>
      <FormularioServicoForm
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
