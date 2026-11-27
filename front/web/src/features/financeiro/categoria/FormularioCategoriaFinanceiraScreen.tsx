// GERADO por frontend/screenEditorById a partir de FormularioCategoriaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useAtualizarCategoriaFinanceira, useCriarCategoriaFinanceira, useCategoriaFinanceira } from '@/gen/api/categoriaFinanceira'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioCategoriaFinanceiraForm, FormularioCategoriaFinanceiraFieldNames, type FormularioCategoriaFinanceiraValues } from './FormularioCategoriaFinanceira'

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioCategoriaFinanceiraScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioCategoriaFinanceiraNovo /> : <FormularioCategoriaFinanceiraEditar id={id} />
}

function FormularioCategoriaFinanceiraNovo() {
  const criar = useCriarCategoriaFinanceira()
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/financeiro/categorias' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Nova categoria</h1>
      <FormularioCategoriaFinanceiraForm
        isSubmitting={criar.isPending}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={(v) => criar.mutateAsync(v).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}

function FormularioCategoriaFinanceiraEditar({ id }: { id: string }) {
  const loaded = useCategoriaFinanceira(id)
  const salvar = useAtualizarCategoriaFinanceira(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/financeiro/categorias' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioCategoriaFinanceiraValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioCategoriaFinanceiraFieldNames.includes(k)))
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de categoria</h1>
      <FormularioCategoriaFinanceiraForm
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
