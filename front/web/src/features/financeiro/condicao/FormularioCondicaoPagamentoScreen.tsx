// GERADO por frontend/screenEditorById a partir de FormularioCondicaoPagamento. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useAtualizarCondicaoPagamento, useCriarCondicaoPagamento, useCondicaoPagamento } from '@/gen/api/condicaoPagamento'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioCondicaoPagamentoForm, FormularioCondicaoPagamentoFieldNames, type FormularioCondicaoPagamentoValues } from './FormularioCondicaoPagamento'

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioCondicaoPagamentoScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioCondicaoPagamentoNovo /> : <FormularioCondicaoPagamentoEditar id={id} />
}

function FormularioCondicaoPagamentoNovo() {
  const criar = useCriarCondicaoPagamento()
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/financeiro/condicoes' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Nova condição</h1>
      <FormularioCondicaoPagamentoForm
        isSubmitting={criar.isPending}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={(v) => criar.mutateAsync(v).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}

function FormularioCondicaoPagamentoEditar({ id }: { id: string }) {
  const loaded = useCondicaoPagamento(id)
  const salvar = useAtualizarCondicaoPagamento(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/financeiro/condicoes' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioCondicaoPagamentoValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioCondicaoPagamentoFieldNames.includes(k)))
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de condição de pagamento</h1>
      <FormularioCondicaoPagamentoForm
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
