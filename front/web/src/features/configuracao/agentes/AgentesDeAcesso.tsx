import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { api } from '@/lib/api'
import { errorMessage, mutationFeedback } from '@/runtime/feedback'
import { formatDateTime } from '@/runtime/format'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Administracao > Agentes (window.custom AgentesDeAcesso, rota /agentes; docs/financeiro/README.md 9.4, B3). Credenciais de maquina
 * dos agentes do proprio usuario: criar (o segredo aparece UMA vez), ver o ultimo uso e revogar. O agente so consulta e prepara
 * rascunhos; nunca aprova, baixa, estorna, cancela nem envia cobranca.
 * ------------------------------------------------------------------------- */

export interface Agente {
  id: string
  nome: string
  clientId: string
  escopos: string[]
  ativo: boolean
  dataUltimoUso?: string | null
  dataExpiracao?: string | null
  nomeDoUsuario?: string | null
  dataCriacao: string
}

const ESCOPOS: Record<string, string> = { consultar: 'Consultar', preparar: 'Preparar rascunhos (títulos e notas)' }
const CHAVE = ['agentes']

export function AgentesDeAcessoScreen() {
  const qc = useQueryClient()
  const lista = useQuery({ queryKey: CHAVE, queryFn: () => api<Agente[]>('/agentes') })
  const criar = useMutation({
    mutationFn: (corpo: { nome: string; escopos: string[] }) => api<{ agente: Agente; segredo: string }>('/agentes', { method: 'POST', body: corpo }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: CHAVE }),
  })
  const revogar = useMutation({
    mutationFn: (id: string) => api<Agente>(`/agentes/${id}/revogar`, { method: 'POST' }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: CHAVE }),
  })
  const [nome, setNome] = useState('')
  const [escopos, setEscopos] = useState<string[]>(['consultar'])
  const [criado, setCriado] = useState<{ clientId: string; segredo: string } | null>(null)
  const [revogando, setRevogando] = useState<Agente | null>(null)
  const fbRevogar = mutationFeedback({ success: 'Agente revogado.', onDone: () => setRevogando(null) })

  const alternar = (e: string, v: boolean) => setEscopos((atual) => (v ? [...new Set([...atual, e])] : atual.filter((x) => x !== e)))

  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Agentes</h1>
      <Card>
        <CardHeader>
          <CardTitle>Novo agente</CardTitle>
          <CardDescription>O agente age por você, só com o que você marcar. O que mexe em dinheiro sempre fica com uma pessoa.</CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Field>
            <FieldLabel htmlFor="agente-nome">Nome</FieldLabel>
            <Input id="agente-nome" maxLength={120} placeholder="Ex.: Agente de conciliação" value={nome} onChange={(e) => setNome(e.target.value)} />
          </Field>
          <div className="flex flex-col gap-2">
            {Object.entries(ESCOPOS).map(([k, v]) => (
              <Field key={k} orientation="horizontal">
                <Checkbox id={`escopo-${k}`} checked={escopos.includes(k)} onCheckedChange={(c) => alternar(k, c === true)} />
                <FieldLabel htmlFor={`escopo-${k}`}>{v}</FieldLabel>
              </Field>
            ))}
          </div>
          {criar.isError && (
            <Alert variant="destructive">
              <AlertTitle>Não foi possível criar</AlertTitle>
              <AlertDescription>{errorMessage(criar.error)}</AlertDescription>
            </Alert>
          )}
          {criado && (
            <Alert variant="success">
              <AlertTitle>Guarde o segredo agora: ele não aparece de novo</AlertTitle>
              <AlertDescription className="break-all">
                Identificador: {criado.clientId}
                <br />
                Segredo: {criado.segredo}
              </AlertDescription>
            </Alert>
          )}
          <div>
            <Button
              disabled={!nome.trim() || !escopos.length || criar.isPending}
              onClick={() =>
                void criar.mutateAsync({ nome: nome.trim(), escopos }).then((r) => {
                  setCriado({ clientId: r.agente.clientId, segredo: r.segredo })
                  setNome('')
                }, () => undefined)
              }
            >
              {criar.isPending ? 'Criando…' : 'Criar agente'}
            </Button>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Agentes da empresa</CardTitle>
        </CardHeader>
        <CardContent>
          {lista.isLoading ? (
            <LoadingState />
          ) : lista.isError ? (
            <ErrorState error={lista.error} onRetry={() => void lista.refetch()} />
          ) : !lista.data?.length ? (
            <EmptyState title="Nenhum agente." description="Crie um agente acima para que ele prepare rascunhos por você." />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>Identificador</TableHead>
                  <TableHead>Pode</TableHead>
                  <TableHead>Dono</TableHead>
                  <TableHead>Último uso</TableHead>
                  <TableHead>Situação</TableHead>
                  <TableHead>
                    <span className="sr-only">Ações</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {lista.data.map((a) => (
                  <TableRow key={a.id}>
                    <TableCell>{a.nome}</TableCell>
                    <TableCell className="font-mono text-xs">{a.clientId}</TableCell>
                    <TableCell>{a.escopos.map((e) => ESCOPOS[e] ?? e).join(', ')}</TableCell>
                    <TableCell>{a.nomeDoUsuario ?? '—'}</TableCell>
                    <TableCell>{a.dataUltimoUso ? formatDateTime(a.dataUltimoUso) : 'Nunca'}</TableCell>
                    <TableCell>
                      <Badge tone={a.ativo ? 'success' : 'neutral'}>{a.ativo ? 'Ativo' : 'Revogado'}</Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      {a.ativo && (
                        <Button variant="outline" size="sm" onClick={() => setRevogando(a)}>
                          Revogar
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      <AlertDialog open={revogando !== null} onOpenChange={(v) => !v && setRevogando(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Revogar o agente {revogando?.nome}?</AlertDialogTitle>
            <AlertDialogDescription>O agente deixa de entrar na hora. Não se desfaz: para voltar, crie outro.</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Voltar</AlertDialogCancel>
            <AlertDialogAction
              className={buttonVariants({ variant: 'destructive' })}
              onClick={() => revogando && void revogar.mutateAsync(revogando.id).then(fbRevogar.onSuccess, fbRevogar.onError)}
            >
              Revogar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
