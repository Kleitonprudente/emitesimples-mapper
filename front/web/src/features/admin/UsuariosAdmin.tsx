import { useMemo, useState } from 'react'
import type { ColumnDef } from '@tanstack/react-table'
import { KeyRound, Link2, Pencil, Plus } from 'lucide-react'
import { z } from 'zod'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PlatformOnly } from '@/features/admin/PlatformOnly'
import { DICA_PODE_CANCELAR, ROTULO_PODE_CANCELAR } from '@/features/admin/vinculosEmpresaApi'
import {
  PAPEIS,
  empresaFk,
  useAdicionarVinculo,
  useAtualizarVinculo,
  useCriarUsuario,
  useEditarUsuario,
  useRedefinirSenha,
  useUsuarios,
  useVinculos,
  type Usuario,
  type Vinculo,
} from '@/features/admin/adminApi'
import type { Papel } from '@/lib/api'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { DataTable } from '@/runtime/DataTable'
import { mutationFeedback, notifyError, notifySuccess } from '@/runtime/feedback'
import { maskPhone } from '@/runtime/format'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'
import { useTableState } from '@/runtime/table-state'

/* ---------------------------------------------------------------------------
 * Usuarios da plataforma (so administrador da plataforma). Escrito a mao com o runtime;
 * todas as chamadas estao em adminApi.ts. Senha: so no envio; o campo e limpo logo apos enviar.
 * ------------------------------------------------------------------------- */

const bytes = (s: string) => new TextEncoder().encode(s).length
const senhaTemporaria = z
  .string()
  .refine((s) => bytes(s) >= 8, 'A senha deve ter pelo menos 8 caracteres.')
  .refine((s) => bytes(s) <= 72, 'A senha é longa demais (máximo de 72 bytes).')
const whatsapp = z.string().regex(/^\d{10,11}$/, 'WhatsApp com DDD (10 ou 11 dígitos).').optional()
const papel = z.enum(['administrador', 'operador', 'consulta'], { error: 'Escolha o papel.' })

export const novoUsuarioSchema = z
  .object({
    nome: z.string().min(1, 'Informe o nome.').max(200, 'Máximo de 200 caracteres.'),
    email: z.email('Informe um e-mail válido.'),
    whatsapp,
    senha: senhaTemporaria,
    ativo: z.boolean(),
    empresa: z.object({ id: z.string() }).optional(),
    papel: papel.optional(),
  })
  .refine((v) => !!v.empresa === !!v.papel, { path: ['papel'], message: 'Escolha a empresa e o papel juntos (ou deixe os dois em branco).' })
type NovoUsuarioForm = z.infer<typeof novoUsuarioSchema>

export const editarUsuarioSchema = z.object({
  nome: z.string().min(1, 'Informe o nome.').max(200, 'Máximo de 200 caracteres.'),
  whatsapp,
  ativo: z.boolean(),
})
type EditarUsuarioForm = z.infer<typeof editarUsuarioSchema>

export const senhaSchema = z
  .object({ novaSenha: senhaTemporaria, confirmacao: z.string().min(1, 'Repita a nova senha.') })
  .refine((v) => v.novaSenha === v.confirmacao, { path: ['confirmacao'], message: 'As senhas não conferem.' })

const vinculoSchema = z.object({ empresa: z.object({ id: z.string() }), papel, podeCancelarNfse: z.boolean() })

const PAPEL_OPTIONS = PAPEIS.map((p) => ({ value: p.value, label: p.label }))

const CAMPOS_NOVO: FieldLayout = [
  [
    { name: 'nome', label: 'Nome', required: true, maxLength: 200 },
    { name: 'email', label: 'E-mail', kind: 'email', required: true },
  ],
  [
    { name: 'whatsapp', label: 'WhatsApp', mask: 'phone', description: 'Opcional, com DDD.' },
    { name: 'senha', label: 'Senha temporária', kind: 'password', required: true, description: 'De 8 a 72 caracteres.' },
  ],
  [
    { name: 'empresa', label: 'Empresa (opcional)', fk: empresaFk, width: 60 },
    { name: 'papel', label: 'Papel na empresa', options: PAPEL_OPTIONS, width: 40 },
  ],
  { name: 'ativo', label: 'Usuário ativo', kind: 'boolean' },
]

const CAMPOS_EDITAR: FieldLayout = [
  { name: 'nome', label: 'Nome', required: true, maxLength: 200 },
  { name: 'whatsapp', label: 'WhatsApp', mask: 'phone', description: 'Opcional, com DDD.' },
  { name: 'ativo', label: 'Usuário ativo', kind: 'boolean' },
]

const CAMPOS_SENHA: FieldLayout = [
  { name: 'novaSenha', label: 'Nova senha', kind: 'password', required: true, description: 'De 8 a 72 caracteres. O usuário deverá trocá-la no primeiro acesso.' },
  { name: 'confirmacao', label: 'Repita a nova senha', kind: 'password', required: true },
]

const CAMPOS_VINCULO: FieldLayout = [
  [
    { name: 'empresa', label: 'Empresa', fk: empresaFk, required: true, width: 60 },
    { name: 'papel', label: 'Papel', options: PAPEL_OPTIONS, required: true, width: 40 },
  ],
  { name: 'podeCancelarNfse', label: ROTULO_PODE_CANCELAR, kind: 'boolean', hint: DICA_PODE_CANCELAR },
]

/* ------------------------------- dialogs ------------------------------- */

function NovoUsuarioDialog({ onClose }: { onClose: () => void }) {
  const criar = useCriarUsuario()
  const fb = mutationFeedback({ success: 'Usuário criado.', onDone: onClose })
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Novo usuário</DialogTitle>
          <DialogDescription>Crie o acesso e, se quiser, já vincule a uma empresa.</DialogDescription>
        </DialogHeader>
        <AutoForm
          schema={novoUsuarioSchema}
          fields={CAMPOS_NOVO}
          defaults={{ nome: '', email: '', whatsapp: '', senha: '', ativo: true } as NovoUsuarioForm}
          clearAfterSubmit={['senha']}
          isSubmitting={criar.isPending}
          serverError={criar.error}
          onCancel={onClose}
          submitLabel="Criar usuário"
          onSubmit={(v) =>
            criar
              .mutateAsync({
                nome: v.nome,
                email: v.email,
                whatsapp: v.whatsapp,
                senha: v.senha,
                ativo: v.ativo,
                vinculos: v.empresa && v.papel ? [{ empresaId: v.empresa.id, papel: v.papel }] : undefined,
              })
              .then(fb.onSuccess, () => {})
          }
        />
      </DialogContent>
    </Dialog>
  )
}

function EditarUsuarioDialog({ usuario, onClose }: { usuario: Usuario; onClose: () => void }) {
  const editar = useEditarUsuario(usuario.id)
  const fb = mutationFeedback({ success: 'Usuário atualizado.', onDone: onClose })
  const values = useMemo<EditarUsuarioForm>(() => ({ nome: usuario.nome, whatsapp: usuario.whatsapp, ativo: usuario.ativo }), [usuario])
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Editar usuário</DialogTitle>
          <DialogDescription>{usuario.email}</DialogDescription>
        </DialogHeader>
        <AutoForm
          schema={editarUsuarioSchema}
          fields={CAMPOS_EDITAR}
          defaults={{ nome: '', whatsapp: '', ativo: true } as EditarUsuarioForm}
          values={values}
          isSubmitting={editar.isPending}
          serverError={editar.error}
          onCancel={onClose}
          onSubmit={(v) => editar.mutateAsync(v).then(fb.onSuccess, () => {})}
        />
      </DialogContent>
    </Dialog>
  )
}

function SenhaDialog({ usuario, onClose }: { usuario: Usuario; onClose: () => void }) {
  const redefinir = useRedefinirSenha(usuario.id)
  const fb = mutationFeedback({ success: 'Senha redefinida.', onDone: onClose })
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Redefinir senha</DialogTitle>
          <DialogDescription>{usuario.nome} ({usuario.email})</DialogDescription>
        </DialogHeader>
        <AutoForm
          schema={senhaSchema}
          fields={CAMPOS_SENHA}
          defaults={{ novaSenha: '', confirmacao: '' }}
          clearAfterSubmit={['novaSenha', 'confirmacao']}
          isSubmitting={redefinir.isPending}
          serverError={redefinir.error}
          submitLabel="Redefinir senha"
          onCancel={onClose}
          onSubmit={(v) => redefinir.mutateAsync(v.novaSenha).then(fb.onSuccess, () => {})}
        />
      </DialogContent>
    </Dialog>
  )
}

function VinculoLinha({ vinculo, usuarioId }: { vinculo: Vinculo; usuarioId: string }) {
  const atualizar = useAtualizarVinculo(usuarioId)
  const salvar = (novoPapel: Papel, ativo: boolean, msg: string, podeCancelarNfse?: boolean) =>
    atualizar.mutate({ vinculoId: vinculo.id, papel: novoPapel, ativo, podeCancelarNfse }, { onSuccess: () => notifySuccess(msg), onError: (e) => notifyError(e, 'Não foi possível atualizar o vínculo') })
  return (
    <TableRow>
      <TableCell className="font-medium">{vinculo.empresaNome}</TableCell>
      <TableCell>
        <Select value={vinculo.papel} disabled={atualizar.isPending} onValueChange={(v) => salvar(v as Papel, vinculo.ativo, 'Papel atualizado.')}>
          <SelectTrigger aria-label={`Papel em ${vinculo.empresaNome}`} className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {PAPEIS.map((p) => (
              <SelectItem key={p.value} value={p.value}>
                {p.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </TableCell>
      <TableCell>
        <Switch
          checked={vinculo.papel === 'administrador' || vinculo.podeCancelarNfse}
          disabled={atualizar.isPending || vinculo.papel === 'administrador'}
          aria-label={`Pode cancelar NFS-e em ${vinculo.empresaNome}`}
          onCheckedChange={(on) => salvar(vinculo.papel, vinculo.ativo, on ? 'Cancelamento delegado.' : 'Delegação de cancelamento removida.', on)}
        />
      </TableCell>
      <TableCell>
        <Badge tone={vinculo.ativo ? 'success' : 'neutral'}>{vinculo.ativo ? 'Ativo' : 'Inativo'}</Badge>
      </TableCell>
      <TableCell className="text-right">
        <Button
          variant="outline"
          size="sm"
          disabled={atualizar.isPending}
          onClick={() => salvar(vinculo.papel, !vinculo.ativo, vinculo.ativo ? 'Vínculo desativado.' : 'Vínculo reativado.')}
        >
          {vinculo.ativo ? 'Desativar' : 'Reativar'}
          <span className="sr-only"> vínculo com {vinculo.empresaNome}</span>
        </Button>
      </TableCell>
    </TableRow>
  )
}

function VinculosDialog({ usuario, onClose }: { usuario: Usuario; onClose: () => void }) {
  const q = useVinculos(usuario.id)
  const adicionar = useAdicionarVinculo(usuario.id)
  const [formKey, setFormKey] = useState(0)
  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Vínculos de {usuario.nome}</DialogTitle>
          <DialogDescription>Empresas e papéis. Vínculos não são excluídos: apenas desativados.</DialogDescription>
        </DialogHeader>
        {q.isLoading ? (
          <LoadingState />
        ) : q.isError ? (
          <ErrorState error={q.error} onRetry={() => void q.refetch()} />
        ) : !q.data?.length ? (
          <EmptyState title="Este usuário não tem vínculos." description="Adicione uma empresa abaixo." className="py-6" />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Empresa</TableHead>
                <TableHead>Papel</TableHead>
                <TableHead>Pode cancelar</TableHead>
                <TableHead>Situação</TableHead>
                <TableHead className="text-right">
                  <span className="sr-only">Ações</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {q.data.map((v) => (
                <VinculoLinha key={v.id} vinculo={v} usuarioId={usuario.id} />
              ))}
            </TableBody>
          </Table>
        )}
        <div className="flex flex-col gap-3 border-t pt-4">
          <h3 className="text-sm font-medium">Adicionar vínculo</h3>
          <AutoForm
            key={formKey}
            schema={vinculoSchema}
            fields={CAMPOS_VINCULO}
            defaults={{ podeCancelarNfse: false } as z.infer<typeof vinculoSchema>}
            submitLabel="Adicionar vínculo"
            isSubmitting={adicionar.isPending}
            serverError={adicionar.error}
            onSubmit={(v) =>
              adicionar.mutateAsync({ empresaId: v.empresa.id, papel: v.papel, podeCancelarNfse: v.podeCancelarNfse }).then(
                () => {
                  notifySuccess('Vínculo adicionado.')
                  setFormKey((k) => k + 1)
                },
                () => {},
              )
            }
          />
        </div>
      </DialogContent>
    </Dialog>
  )
}

/* -------------------------------- tela -------------------------------- */

type DialogState = { tipo: 'novo' } | { tipo: 'editar' | 'senha' | 'vinculos'; usuario: Usuario }

function Conteudo() {
  const [state, update] = useTableState({ sortBy: 'nome' })
  const result = useUsuarios(state)
  const [dialog, setDialog] = useState<DialogState | null>(null)
  const fechar = () => setDialog(null)

  const columns = useMemo<ColumnDef<Usuario>[]>(
    () => [
      { id: 'nome', accessorKey: 'nome', header: 'Nome' },
      { id: 'email', accessorKey: 'email', header: 'E-mail' },
      { id: 'whatsapp', accessorKey: 'whatsapp', header: 'WhatsApp', enableSorting: false, cell: (c) => (c.getValue() ? maskPhone(c.getValue()) : '-') },
      {
        id: 'ativo',
        accessorKey: 'ativo',
        header: 'Situação',
        enableSorting: false,
        cell: (c) => <Badge tone={c.getValue() ? 'success' : 'neutral'}>{c.getValue() ? 'Ativo' : 'Inativo'}</Badge>,
      },
      {
        id: 'actions',
        header: () => <span className="sr-only">Ações</span>,
        enableSorting: false,
        cell: ({ row }) => (
          <div className="flex justify-end gap-1">
            <Button variant="ghost" size="icon" aria-label={`Editar ${row.original.nome}`} onClick={() => setDialog({ tipo: 'editar', usuario: row.original })}>
              <Pencil />
            </Button>
            <Button variant="ghost" size="icon" aria-label={`Vínculos de ${row.original.nome}`} onClick={() => setDialog({ tipo: 'vinculos', usuario: row.original })}>
              <Link2 />
            </Button>
            <Button variant="ghost" size="icon" aria-label={`Redefinir senha de ${row.original.nome}`} onClick={() => setDialog({ tipo: 'senha', usuario: row.original })}>
              <KeyRound />
            </Button>
          </div>
        ),
      },
    ],
    [],
  )

  const novo = (
    <Button onClick={() => setDialog({ tipo: 'novo' })}>
      <Plus /> Novo usuário
    </Button>
  )

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold">Usuários</h1>
        <p className="text-sm text-muted-foreground">Acessos da plataforma, seus vínculos com empresas e papéis.</p>
      </div>
      <DataTable
        columns={columns}
        result={result}
        state={state}
        onStateChange={update}
        getRowId={(r) => r.id}
        searchPlaceholder="Buscar por nome ou e-mail…"
        toolbarActions={novo}
        empty={{ title: 'Nenhum usuário cadastrado.', action: novo }}
      />
      {dialog?.tipo === 'novo' && <NovoUsuarioDialog onClose={fechar} />}
      {dialog?.tipo === 'editar' && <EditarUsuarioDialog usuario={dialog.usuario} onClose={fechar} />}
      {dialog?.tipo === 'senha' && <SenhaDialog usuario={dialog.usuario} onClose={fechar} />}
      {dialog?.tipo === 'vinculos' && <VinculosDialog usuario={dialog.usuario} onClose={fechar} />}
    </div>
  )
}

export function UsuariosAdmin() {
  return (
    <PlatformOnly>
      <Conteudo />
    </PlatformOnly>
  )
}
