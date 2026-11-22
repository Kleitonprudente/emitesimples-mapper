// GERADO por frontend/screenEditorById a partir de FormularioEmpresa. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useAtualizarEmpresa, useCriarEmpresa, useEmpresa } from '@/gen/api/empresa'
import { mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioEmpresaForm, FormularioEmpresaFieldNames, type FormularioEmpresaValues } from './FormularioEmpresa'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { VinculosDaEmpresa } from '@/features/admin/VinculosDaEmpresa'

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioEmpresaScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioEmpresaNovo /> : <FormularioEmpresaEditar id={id} />
}

function FormularioEmpresaNovo() {
  const criar = useCriarEmpresa()
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/empresas' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Nova empresa</h1>
      <Tabs defaultValue="dados">
        <TabsList>
          <TabsTrigger value="dados">Dados</TabsTrigger>
          <TabsTrigger value="vinculosDaEmpresa" disabled>Usuários</TabsTrigger>
        </TabsList>
        <p className="text-sm text-muted-foreground">Salve a empresa para continuar.</p>
        <TabsContent value="dados">
      <FormularioEmpresaForm
        isSubmitting={criar.isPending}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={(v) => criar.mutateAsync(v).then(fb.onSuccess, () => {})}
      />
        </TabsContent>
      </Tabs>
    </div>
  )
}

function FormularioEmpresaEditar({ id }: { id: string }) {
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
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de empresa</h1>
      <Tabs defaultValue="dados">
        <TabsList>
          <TabsTrigger value="dados">Dados</TabsTrigger>
          <TabsTrigger value="vinculosDaEmpresa">Usuários</TabsTrigger>
        </TabsList>
        <TabsContent value="dados">
      <FormularioEmpresaForm
        values={values}
        isSubmitting={salvar.isPending}
        serverError={salvar.error}
        onCancel={voltar}
        onSubmit={(v) => salvar.mutateAsync({ ...preservar, ...v } as typeof v).then(fb.onSuccess, () => {})}
      />
        </TabsContent>
        <TabsContent value="vinculosDaEmpresa">
          <VinculosDaEmpresa empresaId={id} />
        </TabsContent>
      </Tabs>
      {loaded.data.dataCriacao && (
        <p className="text-xs text-muted-foreground">
          Criado em {formatDateTime(loaded.data.dataCriacao)}{loaded.data.dataAtualizacao ? <> &middot; Atualizado em {formatDateTime(loaded.data.dataAtualizacao)}</> : null}
        </p>
      )}
    </div>
  )
}
