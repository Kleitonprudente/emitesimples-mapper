import { useState } from 'react'
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
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { canAct, useSession } from '@/lib/auth'
import { AutoForm } from '@/runtime/AutoForm'
import { mutationFeedback } from '@/runtime/feedback'
import { formatCurrency, formatDate } from '@/runtime/format'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'
import {
  POR_PAGINA_EXTRATO,
  TIPO_MOVIMENTO,
  avulsoSchema,
  camposAvulso,
  camposTransferencia,
  hojeIso,
  transferenciaSchema,
  useEstornar,
  useExtrato,
  useFluxo,
  useLancarAvulso,
  useSaldos,
  useTransferir,
} from '@/features/financeiro/financeiroApi'

/* ---------------------------------------------------------------------------
 * Financeiro > Extrato (window.custom ExtratoFinanceiro; docs/financeiro/README.md, D2, F3-H7 e §11.5).
 * Saldo das contas; extrato da conta no periodo com saldo corrente (calculado no banco) e totais por forma, 15 por pagina;
 * estorno por linha; lancamento avulso e transferencia (so ADMINISTRADOR, C4); fluxo de caixa previsto x realizado por mes (C1).
 * ------------------------------------------------------------------------- */

const primeiroDoMes = () => `${hojeIso().slice(0, 8)}01`

export function ExtratoScreen() {
  const session = useSession()
  const administrador = canAct(session, 'ADMINISTRADOR')
  const saldos = useSaldos()
  const [dialogo, setDialogo] = useState<null | 'avulso' | 'transferencia'>(null)
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-2xl font-semibold">Extrato</h1>
        {administrador && (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => setDialogo('avulso')}>
              Lançamento avulso
            </Button>
            <Button variant="outline" onClick={() => setDialogo('transferencia')}>
              Transferência
            </Button>
          </div>
        )}
      </div>
      {saldos.isLoading ? (
        <LoadingState />
      ) : saldos.isError || !saldos.data ? (
        <ErrorState error={saldos.error} onRetry={() => void saldos.refetch()} />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {saldos.data.contas.map((c) => (
            <Card key={c.idConta}>
              <CardHeader>
                <CardDescription>{c.nome}</CardDescription>
                <CardTitle>{formatCurrency(c.saldo)}</CardTitle>
              </CardHeader>
            </Card>
          ))}
          <Card>
            <CardHeader>
              <CardDescription>Total das contas ativas</CardDescription>
              <CardTitle>{formatCurrency(saldos.data.total)}</CardTitle>
            </CardHeader>
          </Card>
        </div>
      )}
      <Tabs defaultValue="extrato">
        <TabsList>
          <TabsTrigger value="extrato">Movimentação</TabsTrigger>
          <TabsTrigger value="fluxo">Fluxo de caixa</TabsTrigger>
        </TabsList>
        <TabsContent value="extrato">
          <Movimentacao contas={saldos.data?.contas.map((c) => ({ id: c.idConta, nome: c.nome })) ?? []} administrador={administrador} />
        </TabsContent>
        <TabsContent value="fluxo">
          <FluxoDeCaixa />
        </TabsContent>
      </Tabs>
      {dialogo === 'avulso' && <AvulsoDialog aoFechar={() => setDialogo(null)} />}
      {dialogo === 'transferencia' && <TransferenciaDialog aoFechar={() => setDialogo(null)} />}
    </div>
  )
}

function Movimentacao({ contas, administrador }: { contas: { id: string; nome: string }[]; administrador: boolean }) {
  const [idConta, setIdConta] = useState<string | undefined>(undefined)
  const [de, setDe] = useState(primeiroDoMes())
  const [ate, setAte] = useState(hojeIso())
  const [pagina, setPagina] = useState(1)
  const [estornar, setEstornar] = useState<string | null>(null)
  const conta = idConta ?? contas[0]?.id
  const extrato = useExtrato(conta, de || undefined, ate || undefined, pagina)
  const estorno = useEstornar()
  const fb = mutationFeedback({ success: 'Estorno lançado.', onDone: () => setEstornar(null) })
  if (!contas.length) return <EmptyState title="Nenhuma conta ativa." description="Cadastre uma conta em Financeiro > Contas." />
  const paginas = extrato.data ? Math.max(1, Math.ceil(extrato.data.total / POR_PAGINA_EXTRATO)) : 1
  return (
    <div className="flex flex-col gap-4 pt-2">
      <Card>
        <CardContent className="grid gap-4 pt-6 md:grid-cols-12">
          <div className="flex flex-col gap-2 md:col-span-6">
            <Label htmlFor="ext-conta">Conta</Label>
            <Select
              value={conta}
              onValueChange={(v) => {
                setIdConta(v)
                setPagina(1)
              }}
            >
              <SelectTrigger id="ext-conta">
                <SelectValue placeholder="Selecione…" />
              </SelectTrigger>
              <SelectContent>
                {contas.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.nome}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="ext-de">De</Label>
            <Input id="ext-de" type="date" value={de} onChange={(e) => (setDe(e.target.value), setPagina(1))} />
          </div>
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="ext-ate">Até</Label>
            <Input id="ext-ate" type="date" value={ate} onChange={(e) => (setAte(e.target.value), setPagina(1))} />
          </div>
        </CardContent>
      </Card>
      {extrato.isLoading ? (
        <LoadingState />
      ) : extrato.isError || !extrato.data ? (
        <ErrorState error={extrato.error} onRetry={() => void extrato.refetch()} />
      ) : (
        <>
          <p className="text-sm text-muted-foreground">
            Saldo anterior: {formatCurrency(extrato.data.saldoAnterior)} · Saldo no fim do período:{' '}
            <span className="font-medium text-foreground">{formatCurrency(extrato.data.saldoFinal)}</span>
          </p>
          {extrato.data.linhas.length === 0 ? (
            <EmptyState title="Nenhum movimento neste período." />
          ) : (
            <div className="overflow-x-auto rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Data</TableHead>
                    <TableHead>Descrição</TableHead>
                    <TableHead>Tipo</TableHead>
                    <TableHead className="text-right">Entrada</TableHead>
                    <TableHead className="text-right">Saída</TableHead>
                    <TableHead className="text-right">Saldo</TableHead>
                    <TableHead>
                      <span className="sr-only">Ações</span>
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {extrato.data.linhas.map((l) => (
                    <TableRow key={l.id}>
                      <TableCell>{formatDate(l.data)}</TableCell>
                      <TableCell>{l.descricao ?? '—'}</TableCell>
                      <TableCell>
                        {TIPO_MOVIMENTO[l.tipo] ?? l.tipo} {l.estornado && <Badge tone="neutral">Estornado</Badge>}
                      </TableCell>
                      <TableCell className="text-right">{l.sentido === 'ENTRADA' ? formatCurrency(l.valor) : ''}</TableCell>
                      <TableCell className="text-right">{l.sentido === 'SAIDA' ? formatCurrency(l.valor) : ''}</TableCell>
                      <TableCell className="text-right">{formatCurrency(l.saldoCorrente)}</TableCell>
                      <TableCell className="text-right">
                        {administrador && l.tipo !== 'ESTORNO' && !l.estornado && (
                          <Button variant="outline" size="sm" onClick={() => setEstornar(l.id)}>
                            Estornar
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
          <div className="flex items-center justify-end gap-2 text-sm">
            <span className="text-muted-foreground">
              Página {pagina} de {paginas}
            </span>
            <Button variant="outline" size="sm" disabled={pagina <= 1} onClick={() => setPagina((p) => p - 1)}>
              Anterior
            </Button>
            <Button variant="outline" size="sm" disabled={pagina >= paginas} onClick={() => setPagina((p) => p + 1)}>
              Próxima
            </Button>
          </div>
          {extrato.data.totaisPorForma.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Totais por forma no período</CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Forma</TableHead>
                      <TableHead className="text-right">Entradas</TableHead>
                      <TableHead className="text-right">Saídas</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {extrato.data.totaisPorForma.map((t) => (
                      <TableRow key={t.forma}>
                        <TableCell>{t.rotulo}</TableCell>
                        <TableCell className="text-right">{formatCurrency(t.entradas)}</TableCell>
                        <TableCell className="text-right">{formatCurrency(t.saidas)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          )}
        </>
      )}
      <AlertDialog open={estornar !== null} onOpenChange={(v) => !v && setEstornar(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Estornar este movimento?</AlertDialogTitle>
            <AlertDialogDescription>
              O estorno lança uma linha de sentido inverso; numa transferência, as duas pernas. Se for baixa, o título volta a ter saldo. Não se desfaz.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Voltar</AlertDialogCancel>
            <AlertDialogAction
              className={buttonVariants({ variant: 'destructive' })}
              onClick={() => estornar && void estorno.mutateAsync(estornar).then(fb.onSuccess, fb.onError)}
            >
              Estornar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

function FluxoDeCaixa() {
  const [de, setDe] = useState(primeiroDoMes())
  const [ate, setAte] = useState(() => {
    const d = new Date(`${primeiroDoMes()}T12:00:00`)
    d.setMonth(d.getMonth() + 3, 0)
    return d.toISOString().slice(0, 10)
  })
  const fluxo = useFluxo(de, ate)
  return (
    <div className="flex flex-col gap-4 pt-2">
      <Card>
        <CardContent className="grid gap-4 pt-6 md:grid-cols-12">
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="flx-de">De</Label>
            <Input id="flx-de" type="date" value={de} onChange={(e) => setDe(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="flx-ate">Até</Label>
            <Input id="flx-ate" type="date" value={ate} onChange={(e) => setAte(e.target.value)} />
          </div>
          <p className="text-sm text-muted-foreground md:col-span-6 md:self-end">
            Previsto: títulos pelo vencimento. Realizado: o extrato, sem as transferências entre contas.
          </p>
        </CardContent>
      </Card>
      {fluxo.isLoading ? (
        <LoadingState />
      ) : fluxo.isError || !fluxo.data ? (
        <ErrorState error={fluxo.error} onRetry={() => void fluxo.refetch()} />
      ) : (
        <div className="overflow-x-auto rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Mês</TableHead>
                <TableHead className="text-right">Entradas previstas</TableHead>
                <TableHead className="text-right">Saídas previstas</TableHead>
                <TableHead className="text-right">Saldo previsto</TableHead>
                <TableHead className="text-right">Entradas realizadas</TableHead>
                <TableHead className="text-right">Saídas realizadas</TableHead>
                <TableHead className="text-right">Saldo realizado</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {fluxo.data.map((m) => (
                <TableRow key={m.mes}>
                  <TableCell>{`${m.mes.slice(5, 7)}/${m.mes.slice(0, 4)}`}</TableCell>
                  <TableCell className="text-right">{formatCurrency(m.entradasPrevistas)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(m.saidasPrevistas)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(m.saldoPrevisto)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(m.entradasRealizadas)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(m.saidasRealizadas)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(m.saldoRealizado)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  )
}

function AvulsoDialog({ aoFechar }: { aoFechar: () => void }) {
  const lancar = useLancarAvulso()
  const [sentido, setSentido] = useState<'ENTRADA' | 'SAIDA' | undefined>('ENTRADA')
  const fb = mutationFeedback({ success: 'Lançamento registrado.', onDone: aoFechar })
  return (
    <Dialog open onOpenChange={(v) => !v && aoFechar()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Lançamento avulso</DialogTitle>
          <DialogDescription>Dinheiro que entra ou sai sem título (ex.: aporte do sócio, tarifa bancária).</DialogDescription>
        </DialogHeader>
        <AutoForm
          schema={avulsoSchema}
          fields={camposAvulso(sentido)}
          defaults={{ sentido: 'ENTRADA', data: hojeIso() }}
          submitLabel="Lançar"
          isSubmitting={lancar.isPending}
          serverError={lancar.error}
          onCancel={aoFechar}
          onValuesChange={(v, { name, setValue }) => {
            if (name === 'sentido' && v.sentido !== sentido) {
              setSentido(v.sentido)
              setValue('categoria', undefined) // a categoria troca de tipo com o sentido
            }
          }}
          onSubmit={(v) =>
            lancar
              .mutateAsync({ idConta: v.conta?.id, sentido: v.sentido, valor: v.valor, data: v.data, forma: v.forma, idCategoria: v.categoria?.id, descricao: v.descricao })
              .then(fb.onSuccess, () => undefined)
          }
        />
      </DialogContent>
    </Dialog>
  )
}

function TransferenciaDialog({ aoFechar }: { aoFechar: () => void }) {
  const transferir = useTransferir()
  const fb = mutationFeedback({ success: 'Transferência registrada.', onDone: aoFechar })
  return (
    <Dialog open onOpenChange={(v) => !v && aoFechar()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>Transferência entre contas</DialogTitle>
          <DialogDescription>Gera uma saída na conta de origem e uma entrada na de destino. Não conta como receita nem despesa.</DialogDescription>
        </DialogHeader>
        <AutoForm
          schema={transferenciaSchema}
          fields={camposTransferencia}
          defaults={{ data: hojeIso() }}
          submitLabel="Transferir"
          isSubmitting={transferir.isPending}
          serverError={transferir.error}
          onCancel={aoFechar}
          onSubmit={(v) =>
            transferir
              .mutateAsync({ idContaOrigem: v.origem?.id, idContaDestino: v.destino?.id, valor: v.valor, data: v.data, descricao: v.descricao })
              .then(fb.onSuccess, () => undefined)
          }
        />
      </DialogContent>
    </Dialog>
  )
}
