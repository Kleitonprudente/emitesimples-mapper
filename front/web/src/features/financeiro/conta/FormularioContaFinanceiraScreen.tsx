// GERADO por frontend/screenEditorById a partir de FormularioContaFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useAtualizarContaFinanceira, useCriarContaFinanceira, useContaFinanceira } from '@/gen/api/contaFinanceira'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioContaFinanceiraForm, FormularioContaFinanceiraFieldNames, type FormularioContaFinanceiraValues } from './FormularioContaFinanceira'

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioContaFinanceiraScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioContaFinanceiraNovo /> : <FormularioContaFinanceiraEditar id={id} />
}

function FormularioContaFinanceiraNovo() {
  const criar = useCriarContaFinanceira()
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/financeiro/contas' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Nova conta</h1>
      <FormularioContaFinanceiraForm
        isSubmitting={criar.isPending}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={(v) => criar.mutateAsync(v).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}

function FormularioContaFinanceiraEditar({ id }: { id: string }) {
  const loaded = useContaFinanceira(id)
  const salvar = useAtualizarContaFinanceira(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/financeiro/contas' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioContaFinanceiraValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioContaFinanceiraFieldNames.includes(k)))
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de conta</h1>
      <FormularioContaFinanceiraForm
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
