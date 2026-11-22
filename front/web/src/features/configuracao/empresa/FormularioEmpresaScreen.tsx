// GERADO por frontend/screenEditorById a partir de FormularioEmpresa. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { useAtualizarEmpresa, useEmpresa } from '@/gen/api/empresa'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioEmpresaForm, FormularioEmpresaFieldNames, type FormularioEmpresaValues } from './FormularioEmpresa'

export function FormularioEmpresaScreen({ id }: { id: string }) {
  const loaded = useEmpresa(id)
  const salvar = useAtualizarEmpresa(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/empresas' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioEmpresaValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioEmpresaFieldNames.includes(k)))
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de empresa</h1>
      <FormularioEmpresaForm
        values={values}
        isSubmitting={salvar.isPending}
        serverError={salvar.error}
        onCancel={voltar}
        onSubmit={(v) => salvar.mutateAsync({ ...preservar, ...v } as typeof v).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}
