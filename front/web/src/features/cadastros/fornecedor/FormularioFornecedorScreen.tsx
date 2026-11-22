// GERADO por frontend/screenEditorById a partir de FormularioFornecedor. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useAtualizarPessoa, useCriarPessoa, usePessoa } from '@/gen/api/pessoa'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioFornecedorForm, FormularioFornecedorFieldNames, type FormularioFornecedorValues } from './FormularioFornecedor'

/** Valores fixos deste cadastro: entram so na CRIACAO. Na edicao o PUT preserva o que o registro ja tem (ex.: a pessoa pode ser cliente e fornecedor). */
const FIXED_VALUES = { fornecedor: true }

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioFornecedorScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioFornecedorNovo /> : <FormularioFornecedorEditar id={id} />
}

function FormularioFornecedorNovo() {
  const criar = useCriarPessoa()
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/fornecedores' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Novo fornecedor</h1>
      <FormularioFornecedorForm
        isSubmitting={criar.isPending}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={(v) => criar.mutateAsync({ ...v, ...FIXED_VALUES }).then(fb.onSuccess, () => {})}
      />
    </div>
  )
}

function FormularioFornecedorEditar({ id }: { id: string }) {
  const loaded = usePessoa(id)
  const salvar = useAtualizarPessoa(id)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/fornecedores' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioFornecedorValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioFornecedorFieldNames.includes(k)))
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de fornecedor</h1>
      <FormularioFornecedorForm
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
