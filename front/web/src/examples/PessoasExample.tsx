import { useMemo, useState } from 'react'
import type { ColumnDef } from '@tanstack/react-table'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { hasRole, useSession } from '@/lib/auth'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { DataTable } from '@/runtime/DataTable'
import { mutationFeedback } from '@/runtime/feedback'
import { badgeTone, formatDate, maskDocumento, maskPhone } from '@/runtime/format'
import { LoadingState } from '@/runtime/states'
import { useTableState } from '@/runtime/table-state'
import { TIPOS_PESSOA, municipioFk, pessoaSchema, useRemoverPessoa, usePessoa, usePessoas, useSalvarPessoa, type PessoaInput, type PessoaRow } from '@/examples/pessoaApi'

/* TELA DE EXEMPLO ESCRITA A MAO: usa so o runtime (DataTable + AutoForm). E o molde do que o gerador devera emitir. */

const FIELDS: FieldLayout = [
  [
    { name: 'tipoPessoa', label: 'Tipo', required: true, options: TIPOS_PESSOA, width: 40 },
    { name: 'documento', label: 'CPF/CNPJ', required: true, mask: 'documento', width: 60 },
  ],
  { name: 'nome', label: 'Nome / razão social', required: true, maxLength: 200 },
  [
    { name: 'email', label: 'E-mail', kind: 'email' },
    { name: 'telefone', label: 'Telefone', mask: 'phone' },
  ],
  [
    { name: 'cep', label: 'CEP', mask: 'postalCode', width: 35 },
    { name: 'cidade', label: 'Cidade', fk: municipioFk, width: 65 },
  ],
]

const DEFAULTS: Partial<PessoaInput> = { tipoPessoa: 'fisica', documento: '', nome: '' }

function toFormValues(p: PessoaRow): PessoaInput {
  return { tipoPessoa: p.tipoPessoa, documento: p.documento, nome: p.nome, email: p.email, telefone: p.telefone, cep: p.cep, cidade: p.cidade }
}

function PessoaDialog({ id, open, onClose }: { id: string | null; open: boolean; onClose: () => void }) {
  const editing = id !== null
  const loaded = usePessoa(id)
  const save = useSalvarPessoa(id)
  const fb = mutationFeedback({ success: editing ? 'Pessoa atualizada.' : 'Pessoa cadastrada.', onDone: onClose })

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{editing ? 'Editar pessoa' : 'Nova pessoa'}</DialogTitle>
          <DialogDescription>Cadastro de clientes e fornecedores.</DialogDescription>
        </DialogHeader>
        {editing && loaded.isLoading ? (
          <LoadingState />
        ) : (
          <AutoForm
            schema={pessoaSchema}
            fields={FIELDS}
            defaults={DEFAULTS as PessoaInput}
            values={loaded.data ? toFormValues(loaded.data) : undefined}
            isSubmitting={save.isPending}
            serverError={save.error}
            onCancel={onClose}
            onSubmit={(v) => save.mutateAsync(v).then(fb.onSuccess, () => {})}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

export function PessoasExample() {
  const session = useSession()
  const canWrite = hasRole(session, 'operador')
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = usePessoas(state)
  const [dialog, setDialog] = useState<{ id: string | null } | null>(null)
  const [removing, setRemoving] = useState<PessoaRow | null>(null)
  const remove = useRemoverPessoa()

  const columns = useMemo<ColumnDef<PessoaRow>[]>(
    () => [
      { id: 'nome', accessorKey: 'nome', header: 'Nome' },
      { id: 'documento', accessorKey: 'documento', header: 'CPF/CNPJ', cell: (c) => maskDocumento(c.getValue()) },
      { id: 'tipoPessoa', accessorKey: 'tipoPessoa', header: 'Tipo', cell: (c) => <Badge tone={badgeTone(c.getValue() as string)}>{TIPOS_PESSOA.find((t) => t.value === c.getValue())?.label}</Badge> },
      { id: 'telefone', accessorKey: 'telefone', header: 'Telefone', cell: (c) => maskPhone(c.getValue()) },
      { id: 'dataCriacao', accessorKey: 'dataCriacao', header: 'Cadastro', cell: (c) => formatDate(c.getValue() as string) },
      {
        id: 'actions',
        header: () => <span className="sr-only">Ações</span>,
        enableSorting: false,
        cell: ({ row }) =>
          canWrite ? (
            <div className="flex justify-end gap-1">
              <Button variant="ghost" size="icon" aria-label={`Editar ${row.original.nome}`} onClick={() => setDialog({ id: row.original.id })}>
                <Pencil />
              </Button>
              <Button variant="ghost" size="icon" aria-label={`Excluir ${row.original.nome}`} onClick={() => setRemoving(row.original)}>
                <Trash2 />
              </Button>
            </div>
          ) : null,
      },
    ],
    [canWrite],
  )

  const novo = canWrite && (
    <Button onClick={() => setDialog({ id: null })}>
      <Plus /> Nova pessoa
    </Button>
  )

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold">Pessoas (exemplo)</h1>
        <p className="text-sm text-muted-foreground">Tela escrita à mão com o runtime, como molde para as telas geradas.</p>
      </div>
      <DataTable
        columns={columns}
        result={result}
        state={state}
        onStateChange={update}
        getRowId={(r) => r.id}
        searchPlaceholder="Buscar por nome…"
        facets={[{ id: 'tipoPessoa', title: 'Tipo', options: TIPOS_PESSOA }]}
        toolbarActions={novo}
        empty={{ title: 'Nenhuma pessoa cadastrada ainda.', description: 'Cadastre clientes e fornecedores para emitir notas.', action: novo || undefined }}
      />
      {dialog && <PessoaDialog id={dialog.id} open onClose={() => setDialog(null)} />}
      <Dialog open={!!removing} onOpenChange={(o) => !o && setRemoving(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Excluir pessoa?</DialogTitle>
            <DialogDescription>{removing ? `${removing.nome} será removida. Esta ação não pode ser desfeita.` : ''}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRemoving(null)}>
              Cancelar
            </Button>
            <Button
              variant="destructive"
              disabled={remove.isPending}
              onClick={() => removing && remove.mutate(removing.id, mutationFeedback({ success: 'Pessoa excluída.', error: 'Não foi possível excluir', onDone: () => setRemoving(null) }))}
            >
              Excluir
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
