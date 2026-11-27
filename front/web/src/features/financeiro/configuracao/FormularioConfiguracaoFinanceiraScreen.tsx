// GERADO por frontend/screenEditorById a partir de FormularioConfiguracaoFinanceira. Nao edite: a saida e sobrescrita por inteiro.
import { formatDateTime } from '@/runtime/format'
import { initialTableState } from '@/runtime/table-state'
import { useAtualizarConfiguracaoFinanceira, useConfiguracaoFinanceira, useConfiguracaoFinanceiras } from '@/gen/api/configuracaoFinanceira'
import { mutationFeedback } from '@/runtime/feedback'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'
import { FormularioConfiguracaoFinanceiraForm, FormularioConfiguracaoFinanceiraFieldNames, type FormularioConfiguracaoFinanceiraValues } from './FormularioConfiguracaoFinanceira'

// Registro UNICO (singleton): a lista gerada, escopada pelo back, traz no maximo um registro; ele e editado aqui.
// Nao ha criar nem voltar (front/mapas/README.md, "Editor singleton").
const PRIMEIRO = initialTableState({ pageSize: 1 })

export function FormularioConfiguracaoFinanceiraScreen() {
  const lista = useConfiguracaoFinanceiras(PRIMEIRO)
  if (lista.isLoading) return <LoadingState />
  if (lista.isError) return <ErrorState error={lista.error} onRetry={lista.refetch} />
  const id = lista.data?.[0]?.id
  if (!id) return <EmptyState title="Configuração do financeiro" description="A configuração ainda não foi criada. Abra Contas a receber: os padrões iniciais do financeiro são criados no primeiro acesso." />
  return <FormularioConfiguracaoFinanceiraEditar id={id} />
}

function FormularioConfiguracaoFinanceiraEditar({ id }: { id: string }) {
  const loaded = useConfiguracaoFinanceira(id)
  const salvar = useAtualizarConfiguracaoFinanceira(id)
  const fb = mutationFeedback({ success: 'Registro salvo.' })
  if (loaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const values = Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)) as FormularioConfiguracaoFinanceiraValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioConfiguracaoFinanceiraFieldNames.includes(k)))
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Configuração do financeiro</h1>
      <FormularioConfiguracaoFinanceiraForm
        values={values}
        isSubmitting={salvar.isPending}
        serverError={salvar.error}
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
