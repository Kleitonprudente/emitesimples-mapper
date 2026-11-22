// GERADO por frontend/screenEditorById a partir de FormularioPessoa. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { useAtualizarPessoa, usePessoa } from '@/gen/api/pessoa'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioPessoaForm, type FormularioPessoaValues } from './FormularioPessoa'

export function FormularioPessoaScreen({ id }: { id: string }) {
  const loaded = usePessoa(id)
  const salvar = useAtualizarPessoa(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/pessoas' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioPessoaValues
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de pessoa</h1>
      <FormularioPessoaForm
        values={values}
        isSubmitting={salvar.isPending}
        serverError={salvar.error}
        onCancel={voltar}
        onSubmit={(v) => salvar.mutateAsync(v).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}
