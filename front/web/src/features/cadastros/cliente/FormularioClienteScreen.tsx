// GERADO por frontend/screenEditorById a partir de FormularioCliente. Nao edite: a saida e sobrescrita por inteiro.
import { useNavigate } from '@tanstack/react-router'
import { formatDateTime } from '@/runtime/format'
import { useState } from 'react'
import { toast } from 'sonner'
import { useAtualizarPessoa, useCriarPessoa, usePessoa, useContatosDePessoa, useSalvarContatosDePessoa } from '@/gen/api/pessoa'
import { errorMessage, mutationFeedback } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'
import { FormularioClienteForm, FormularioClienteFieldNames, type FormularioClienteValues } from './FormularioCliente'

/** Valores fixos deste cadastro: entram so na CRIACAO. Na edicao o PUT preserva o que o registro ja tem (ex.: a pessoa pode ser cliente e fornecedor). */
const FIXED_VALUES = { cliente: true }

// Rota unica: o id literal 'novo' cria; qualquer outro id edita. Dois componentes para os hooks nao dependerem do id.
export function FormularioClienteScreen({ id }: { id: string }) {
  return id === 'novo' ? <FormularioClienteNovo /> : <FormularioClienteEditar id={id} />
}

function FormularioClienteNovo() {
  const criar = useCriarPessoa()
  const salvarContatos = useSalvarContatosDePessoa()
  const [salvando, setSalvando] = useState(false)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/clientes' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  const salvarTudo = async (v: FormularioClienteValues) => {
    const { contatos, ...dados } = v
    const salvo = await criar.mutateAsync({ ...dados, ...FIXED_VALUES }).catch(() => undefined)
    if (!salvo) return
    setSalvando(true)
    try {
      // O pai ja esta salvo; a lista de detalhes vai inteira num PUT atomico (tudo ou nada). Falha nao desfaz o pai.
      await salvarContatos.mutateAsync({ id: salvo.id, itens: contatos })
    } catch (e) {
      toast.error(`Cliente salvo, mas os contatos não foram salvos: ${errorMessage(e)}. Revise na edição.`)
      void navigate({ to: '/clientes/$id', params: { id: salvo.id } })
      return
    } finally {
      setSalvando(false)
    }
    fb.onSuccess(salvo)
  }
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Novo cliente</h1>
      <FormularioClienteForm
        isSubmitting={criar.isPending || salvando}
        serverError={criar.error}
        onCancel={voltar}
        onSubmit={salvarTudo}
      />
    </div>
  )
}

function FormularioClienteEditar({ id }: { id: string }) {
  const loaded = usePessoa(id)
  const salvar = useAtualizarPessoa(id)
  const contatosLoaded = useContatosDePessoa(id)
  const salvarContatos = useSalvarContatosDePessoa()
  const [salvando, setSalvando] = useState(false)
  const navigate = useNavigate()
  const voltar = () => void navigate({ to: '/clientes' })
  const fb = mutationFeedback({ success: 'Registro salvo.', onDone: voltar })
  if (loaded.isLoading || contatosLoaded.isLoading) return <LoadingState />
  if (loaded.isError || !loaded.data) return <ErrorState error={loaded.error} onRetry={() => void loaded.refetch()} />
  if (contatosLoaded.isError || !contatosLoaded.data) return <ErrorState error={contatosLoaded.error} onRetry={() => void contatosLoaded.refetch()} />
  // O back devolve null nos opcionais ausentes; o schema do formulario espera ausente (undefined).
  const contatosAtuais = contatosLoaded.data.map((c) => Object.fromEntries(Object.entries(c).filter(([, v]) => v !== null))) as unknown as FormularioClienteValues['contatos']
  const values = { ...Object.fromEntries(Object.entries(loaded.data).filter(([, v]) => v !== null)), contatos: contatosAtuais } as FormularioClienteValues
  // O PUT substitui o registro inteiro: o que o formulario nao edita (ex.: referencias de integracao) segue como veio.
  const preservar = Object.fromEntries(Object.entries(values).filter(([k]) => !FormularioClienteFieldNames.includes(k)))
  const salvarTudo = async (v: FormularioClienteValues) => {
    const { contatos, ...dados } = v
    // A API responde 204 (sem corpo) ao atualizar: o sucesso vem da promessa, nao do retorno.
    const salvou = await salvar.mutateAsync({ ...preservar, ...dados } as typeof dados).then(() => true, () => false)
    if (!salvou) return
    setSalvando(true)
    try {
      // O pai ja esta salvo; a lista de detalhes vai inteira num PUT atomico (tudo ou nada). Falha nao desfaz o pai.
      await salvarContatos.mutateAsync({ id, itens: contatos })
    } catch (e) {
      toast.error(`Cliente salvo, mas os contatos não foram salvos: ${errorMessage(e)}. Revise na edição.`)
      return
    } finally {
      setSalvando(false)
    }
    fb.onSuccess(undefined)
  }
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Cadastro de cliente</h1>
      <FormularioClienteForm
        values={values}
        isSubmitting={salvar.isPending || salvando}
        serverError={salvar.error}
        onCancel={voltar}
        onSubmit={salvarTudo}
      />
      {loaded.data.dataCriacao && (
        <p className="text-xs text-muted-foreground">
          Criado em {formatDateTime(loaded.data.dataCriacao)}{loaded.data.dataAtualizacao ? <> &middot; Atualizado em {formatDateTime(loaded.data.dataAtualizacao)}</> : null}
        </p>
      )}
    </div>
  )
}
