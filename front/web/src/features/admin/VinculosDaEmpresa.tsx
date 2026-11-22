import { useMemo, useState } from 'react'
import { Link } from '@tanstack/react-router'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PAPEIS } from '@/features/admin/adminApi'
import { usuarioAtivoFk, useAtualizarVinculoDaEmpresa, useVincularUsuario, useVinculosDaEmpresa, type VinculoDaEmpresa } from '@/features/admin/vinculosEmpresaApi'
import { ApiError, type Papel } from '@/lib/api'
import { Combobox, type FkValue } from '@/runtime/Combobox'
import { errorMessage, notifyError, notifySuccess } from '@/runtime/feedback'
import { formatDate } from '@/runtime/format'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Aba "Usuarios" do cadastro de Empresa (so em edicao, so plataforma): vinculos de usuario com a empresa.
 * Escrito a mao (montado pelo gerador via extraTabs). Vinculos nunca sao excluidos: so desativados.
 * ------------------------------------------------------------------------- */

export function VinculosDaEmpresa({ empresaId }: { empresaId: string }) {
  const lista = useVinculosDaEmpresa(empresaId)
  const vincular = useVincularUsuario(empresaId)
  const atualizar = useAtualizarVinculoDaEmpresa(empresaId)
  const [desativar, setDesativar] = useState<VinculoDaEmpresa | null>(null)
  const [usuario, setUsuario] = useState<FkValue | null>(null)
  const [papelNovo, setPapelNovo] = useState<Papel>('operador')
  const [erroVincular, setErroVincular] = useState<string | null>(null)

  const vinculados = useMemo(() => (lista.data ?? []).map((v) => v.usuario.id).sort(), [lista.data])
  const fonte = useMemo(() => usuarioAtivoFk(empresaId, vinculados), [empresaId, vinculados])

  const salvar = (v: VinculoDaEmpresa, mudanca: { papel?: Papel; ativo?: boolean }, msg: string) =>
    atualizar.mutate({ vinculoId: v.id, ...mudanca }, { onSuccess: () => notifySuccess(msg), onError: (e) => notifyError(e, 'Não foi possível atualizar o vínculo') })

  const enviarVinculo = () => {
    if (!usuario) return
    setErroVincular(null)
    vincular.mutate(
      { usuarioId: usuario.id, papel: papelNovo },
      {
        onSuccess: () => {
          notifySuccess('Usuário vinculado.')
          setUsuario(null)
          setPapelNovo('operador')
        },
        onError: (e) => setErroVincular(e instanceof ApiError && e.status === 409 ? 'Este usuário já está vinculado.' : errorMessage(e)),
      },
    )
  }

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <CardTitle>Usuários da empresa</CardTitle>
        </CardHeader>
        <CardContent>
          {lista.isLoading ? (
            <LoadingState />
          ) : lista.isError ? (
            <ErrorState error={lista.error} onRetry={() => void lista.refetch()} />
          ) : !lista.data?.length ? (
            <EmptyState title="Nenhum usuário vinculado a esta empresa." className="py-6" />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Usuário</TableHead>
                  <TableHead>Papel</TableHead>
                  <TableHead>Situação</TableHead>
                  <TableHead>Desde</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lista.data.map((v) => (
                  <TableRow key={v.id}>
                    <TableCell>
                      <div className="font-medium">{v.usuario.nome}</div>
                      <div className="text-sm text-muted-foreground">{v.usuario.email}</div>
                    </TableCell>
                    <TableCell>
                      <Select value={v.papel} disabled={atualizar.isPending} onValueChange={(p) => salvar(v, { papel: p as Papel }, 'Papel atualizado.')}>
                        <SelectTrigger aria-label={`Papel de ${v.usuario.nome}`} className="w-40">
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
                      <div className="flex items-center gap-2">
                        <Switch
                          checked={v.ativo}
                          disabled={atualizar.isPending}
                          aria-label={`Acesso de ${v.usuario.nome} a esta empresa`}
                          onCheckedChange={(on) => (on ? salvar(v, { ativo: true }, 'Vínculo reativado.') : setDesativar(v))}
                        />
                        <Badge tone={v.ativo ? 'success' : 'neutral'}>{v.ativo ? 'Ativo' : 'Inativo'}</Badge>
                      </div>
                    </TableCell>
                    <TableCell>{formatDate(v.dataCriacao)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Vincular usuário</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <div className="flex flex-wrap items-end gap-3">
            <div className="flex min-w-64 flex-1 flex-col gap-1.5">
              <Label htmlFor="vinculo-usuario">Usuário</Label>
              <Combobox id="vinculo-usuario" source={fonte} value={usuario} onChange={setUsuario} placeholder="Buscar por nome ou e-mail…" disabled={vincular.isPending} />
            </div>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="vinculo-papel">Papel</Label>
              <Select value={papelNovo} onValueChange={(p) => setPapelNovo(p as Papel)} disabled={vincular.isPending}>
                <SelectTrigger id="vinculo-papel" className="w-40">
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
            </div>
            <Button type="button" disabled={!usuario || vincular.isPending} onClick={enviarVinculo}>
              Vincular
            </Button>
            <Button type="button" variant="outline" asChild>
              <Link to="/admin/usuarios">Criar novo usuário</Link>
            </Button>
          </div>
          <p className="text-sm text-muted-foreground">O usuário precisa existir antes de ser vinculado.</p>
          {erroVincular && (
            <Alert variant="destructive">
              <AlertDescription>{erroVincular}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      <AlertDialog open={!!desativar} onOpenChange={(o) => !o && setDesativar(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Desativar o acesso de {desativar?.usuario.nome} a esta empresa?</AlertDialogTitle>
            <AlertDialogDescription>O vínculo não é excluído: você pode reativá-lo depois.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction onClick={() => desativar && salvar(desativar, { ativo: false }, 'Vínculo desativado.')}>Desativar</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
