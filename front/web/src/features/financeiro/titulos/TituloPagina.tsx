import { useState } from 'react'
import { useNavigate, useSearch } from '@tanstack/react-router'
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
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button, buttonVariants } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { canAct, useSession } from '@/lib/auth'
import { AutoForm } from '@/runtime/AutoForm'
import { mutationFeedback } from '@/runtime/feedback'
import { formatCurrency, formatDate, formatDateTime } from '@/runtime/format'
import { ErrorState, LoadingState } from '@/runtime/states'
import {
  ESTADO_COBRANCA,
  FORMA,
  SITUACAO,
  TIPO_COBRANCA,
  TIPO,
  TIPO_MOVIMENTO,
  baixaSchema,
  camposBaixa,
  camposEditar,
  camposLancar,
  editarSchema,
  hojeIso,
  lancarSchema,
  sugestaoDeBaixa,
  useAprovarTitulo,
  useAtualizarCobranca,
  useCobrancasDoTitulo,
  useCobrancasEnviadas,
  useDescartarCobranca,
  useEnviarCobranca,
  useGerarCobranca,
  useAtualizarTitulo,
  useBaixarTitulo,
  useBaixasDoTitulo,
  useCancelarTitulo,
  useEstornar,
  useLancarTitulo,
  useSugestaoInicialDeBaixa,
  useSugestoes,
  useTitulo,
  type BaixaValues,
  type EditarValues,
  type LancarValues,
  type Titulo,
  type TipoCobranca,
  type TipoTitulo,
} from '@/features/financeiro/financeiroApi'

/* ---------------------------------------------------------------------------
 * Financeiro > Titulo (window.custom TituloFinanceiro, rota /financeiro/titulos/$id; docs/financeiro/README.md, D2 e §11).
 * id 'novo' (com ?tipo=RECEBER|PAGAR) lanca; qualquer outro id mostra o detalhe: dados, baixas (com estorno), editar, baixar
 * (juros e multa sugeridos e editaveis, total ao vivo), cancelar e aprovar. Acoes criticas pedem confirmacao (AlertDialog).
 * Papeis so escondem botoes; o back decide (B2, C4).
 * ------------------------------------------------------------------------- */

const semNulos = <T extends object>(o: T) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== null && v !== undefined && v !== ''))

export function TituloScreen({ id }: { id: string }) {
  const search = useSearch({ strict: false }) as { tipo?: string }
  const tipo: TipoTitulo = search.tipo === 'PAGAR' ? 'PAGAR' : 'RECEBER'
  return id === 'novo' ? <TituloNovo tipo={tipo} /> : <TituloDetalhe id={id} />
}

function TituloNovo({ tipo }: { tipo: TipoTitulo }) {
  const navigate = useNavigate()
  const sugestoes = useSugestoes(tipo)
  const lancar = useLancarTitulo()
  const voltar = () => void navigate({ to: TIPO[tipo].rotaLista })
  const fb = mutationFeedback({ success: 'Título lançado.', onDone: voltar })
  if (sugestoes.isLoading) return <LoadingState />
  const s = sugestoes.data
  // Os padroes da configuracao so pre-preenchem (§4.4): a pessoa pode apagar.
  const defaults: Partial<LancarValues> = semNulos({
    vencimento: s?.vencimento ?? undefined,
    observacao: s?.observacao ?? undefined,
    formaPrevista: s?.formaPrevista ?? undefined,
    categoria: s?.categoria ?? undefined,
    parcelas: 1,
    intervaloDias: 30,
  })
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">{TIPO[tipo].novo}</h1>
      <AutoForm
        schema={lancarSchema}
        fields={camposLancar(tipo)}
        defaults={defaults}
        submitLabel="Lançar"
        isSubmitting={lancar.isPending}
        serverError={lancar.error}
        onCancel={voltar}
        onSubmit={(v) =>
          lancar
            .mutateAsync({
              tipo,
              idPessoa: v.pessoa?.id,
              descricao: v.descricao,
              observacao: v.observacao,
              valor: v.valor,
              vencimento: v.vencimento,
              idCategoria: v.categoria?.id,
              formaPrevista: v.formaPrevista,
              parcelas: v.repetirMeses ? undefined : v.parcelas,
              intervaloDias: v.intervaloDias,
              repetirMeses: v.repetirMeses,
              multaPercentual: v.multaPercentual,
              jurosMensalPercentual: v.jurosMensalPercentual,
            })
            .then(fb.onSuccess, () => undefined)
        }
      />
    </div>
  )
}

function Dado({ rotulo, children }: { rotulo: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1">
      <span className="text-xs text-muted-foreground">{rotulo}</span>
      <span>{children}</span>
    </div>
  )
}

function Confirmar({
  aberto,
  aoMudar,
  titulo,
  texto,
  acao,
  destrutiva,
  aoConfirmar,
}: {
  aberto: boolean
  aoMudar: (v: boolean) => void
  titulo: string
  texto: string
  acao: string
  destrutiva?: boolean
  aoConfirmar: () => void
}) {
  return (
    <AlertDialog open={aberto} onOpenChange={aoMudar}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{titulo}</AlertDialogTitle>
          <AlertDialogDescription>{texto}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Voltar</AlertDialogCancel>
          <AlertDialogAction className={destrutiva ? buttonVariants({ variant: 'destructive' }) : undefined} onClick={aoConfirmar}>
            {acao}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}

function TituloDetalhe({ id }: { id: string }) {
  const session = useSession()
  const navigate = useNavigate()
  const titulo = useTitulo(id)
  const baixas = useBaixasDoTitulo(id)
  const [editando, setEditando] = useState(false)
  const [baixando, setBaixando] = useState(false)
  const [confirmar, setConfirmar] = useState<null | 'cancelar' | 'aprovar' | { estornar: string }>(null)
  const atualizar = useAtualizarTitulo(id)
  const cancelar = useCancelarTitulo(id)
  const aprovar = useAprovarTitulo(id)
  const estornar = useEstornar()
  const operador = canAct(session, 'OPERADOR')
  const administrador = canAct(session, 'ADMINISTRADOR')

  if (titulo.isLoading) return <LoadingState />
  if (titulo.isError || !titulo.data) return <ErrorState error={titulo.error} onRetry={() => void titulo.refetch()} />
  const t = titulo.data
  const aberto = t.situacao === 'ABERTO' || t.situacao === 'PARCIAL'
  const valorEditavel = t.origemTipo === 'MANUAL' && t.quitado === 0 && t.situacao === 'ABERTO'
  const fbAcao = mutationFeedback({ success: 'Registro salvo.', onDone: () => setConfirmar(null) })

  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-semibold">{t.descricao}</h1>
          <div className="flex flex-wrap gap-1">
            <Badge tone="outline">{t.tipo === 'RECEBER' ? 'A receber' : 'A pagar'}</Badge>
            <Badge tone={SITUACAO[t.situacao].tone}>{SITUACAO[t.situacao].rotulo}</Badge>
            {t.vencido && <Badge tone="danger">Vencido</Badge>}
            {!t.aprovado && <Badge tone="outline">Aguardando aprovação</Badge>}
            {t.paraRevisao && <Badge tone="warning">Para revisão</Badge>}
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => void navigate({ to: TIPO[t.tipo].rotaLista })}>
            Voltar
          </Button>
          {operador && aberto && t.aprovado && <Button onClick={() => setBaixando(true)}>{t.tipo === 'RECEBER' ? 'Receber' : 'Pagar'}</Button>}
          {operador && aberto && !editando && (
            <Button variant="outline" onClick={() => setEditando(true)}>
              Editar
            </Button>
          )}
          {administrador && !t.aprovado && t.situacao !== 'CANCELADO' && (
            <Button variant="outline" onClick={() => setConfirmar('aprovar')}>
              Aprovar
            </Button>
          )}
          {administrador && t.situacao !== 'CANCELADO' && (
            <Button variant="destructive" onClick={() => setConfirmar('cancelar')}>
              Cancelar título
            </Button>
          )}
        </div>
      </div>
      {t.paraRevisao && (
        <Alert variant="warning">
          <AlertDescription>O documento de origem foi cancelado com o título já baixado: decida entre estornar a baixa ou registrar a devolução.</AlertDescription>
        </Alert>
      )}
      {editando ? (
        <Card>
          <CardHeader>
            <CardTitle>Editar título</CardTitle>
          </CardHeader>
          <CardContent>
            <AutoForm
              schema={editarSchema}
              fields={camposEditar(t.tipo, valorEditavel)}
              defaults={{}}
              values={semNulos({
                descricao: t.descricao,
                valor: t.valor,
                vencimento: t.vencimento,
                categoria: t.categoria ?? undefined,
                formaPrevista: t.formaPrevista ?? undefined,
                multaPercentual: t.multaPercentual ?? undefined,
                jurosMensalPercentual: t.jurosMensalPercentual ?? undefined,
                observacao: t.observacao ?? undefined,
              }) as EditarValues}
              isSubmitting={atualizar.isPending}
              serverError={atualizar.error}
              onCancel={() => setEditando(false)}
              onSubmit={(v) =>
                atualizar
                  .mutateAsync({
                    descricao: v.descricao,
                    observacao: v.observacao,
                    valor: valorEditavel ? v.valor : undefined,
                    vencimento: v.vencimento,
                    idCategoria: v.categoria?.id,
                    formaPrevista: v.formaPrevista,
                    multaPercentual: v.multaPercentual,
                    jurosMensalPercentual: v.jurosMensalPercentual,
                  })
                  .then(mutationFeedback({ success: 'Registro salvo.', onDone: () => setEditando(false) }).onSuccess, () => undefined)
              }
            />
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardContent className="grid gap-4 pt-6 sm:grid-cols-2 md:grid-cols-4">
            <Dado rotulo={TIPO[t.tipo].pessoa}>{t.nomePessoa ?? '—'}</Dado>
            <Dado rotulo="Vencimento">{formatDate(t.vencimento)}</Dado>
            <Dado rotulo="Parcela">{`${t.parcela}/${t.totalParcelas}`}</Dado>
            <Dado rotulo="Forma prevista">{t.formaPrevista ? FORMA[t.formaPrevista] : '—'}</Dado>
            <Dado rotulo="Valor">{formatCurrency(t.valor)}</Dado>
            <Dado rotulo="Quitado">{formatCurrency(t.quitado)}</Dado>
            <Dado rotulo="Saldo">{formatCurrency(t.saldo)}</Dado>
            <Dado rotulo="Categoria">{t.categoria?.nome ?? '—'}</Dado>
            {t.observacao && <Dado rotulo="Observação">{t.observacao}</Dado>}
            {t.origemNumero && <Dado rotulo="Documento de origem">{t.origemNumero}</Dado>}
          </CardContent>
        </Card>
      )}
      <Card>
        <CardHeader>
          <CardTitle>Baixas</CardTitle>
        </CardHeader>
        <CardContent>
          {baixas.isLoading ? (
            <LoadingState />
          ) : baixas.isError ? (
            <ErrorState error={baixas.error} onRetry={() => void baixas.refetch()} />
          ) : !baixas.data?.length ? (
            <p className="text-sm text-muted-foreground">Nenhuma baixa ainda.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Data</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Forma</TableHead>
                  <TableHead>Quitado</TableHead>
                  <TableHead>Juros + multa</TableHead>
                  <TableHead>Desconto</TableHead>
                  <TableHead>Valor</TableHead>
                  <TableHead>
                    <span className="sr-only">Ações</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {baixas.data.map(({ movimento: m, estornado }) => (
                  <TableRow key={m.id}>
                    <TableCell>{formatDate(m.data)}</TableCell>
                    <TableCell>
                      {TIPO_MOVIMENTO[m.tipo] ?? m.tipo} {estornado && <Badge tone="neutral">Estornada</Badge>}
                    </TableCell>
                    <TableCell>{m.forma ? FORMA[m.forma] : '—'}</TableCell>
                    <TableCell>{formatCurrency(m.quitado)}</TableCell>
                    <TableCell>{formatCurrency(m.juros + m.multa)}</TableCell>
                    <TableCell>{formatCurrency(m.desconto)}</TableCell>
                    <TableCell>{formatCurrency(m.valor)}</TableCell>
                    <TableCell className="text-right">
                      {administrador && m.tipo !== 'ESTORNO' && !estornado && (
                        <Button variant="outline" size="sm" onClick={() => setConfirmar({ estornar: m.id })}>
                          Estornar
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
      {t.tipo === 'RECEBER' && <QuadroDeCobranca titulo={t} administrador={administrador} operador={operador} />}
      {t.tipo === 'RECEBER' && <QuadroDeEmail titulo={t} administrador={administrador} />}
      {baixando && <BaixaDialog titulo={t} aoFechar={() => setBaixando(false)} />}
      <Confirmar
        aberto={confirmar === 'cancelar'}
        aoMudar={(v) => !v && setConfirmar(null)}
        titulo="Cancelar este título?"
        texto="O título deixa de ser cobrado e não aceita mais baixa. Um título com baixa precisa ter a baixa estornada antes."
        acao="Cancelar título"
        destrutiva
        aoConfirmar={() => void cancelar.mutateAsync(undefined).then(fbAcao.onSuccess, fbAcao.onError)}
      />
      <Confirmar
        aberto={confirmar === 'aprovar'}
        aoMudar={(v) => !v && setConfirmar(null)}
        titulo="Aprovar este título?"
        texto="Depois de aprovado, o título aceita baixa e entra nos lembretes de cobrança."
        acao="Aprovar"
        aoConfirmar={() => void aprovar.mutateAsync(undefined).then(fbAcao.onSuccess, fbAcao.onError)}
      />
      <Confirmar
        aberto={typeof confirmar === 'object' && confirmar !== null}
        aoMudar={(v) => !v && setConfirmar(null)}
        titulo="Estornar esta baixa?"
        texto="O estorno lança uma linha de sentido inverso no extrato e devolve o valor ao saldo do título. Não se desfaz."
        acao="Estornar"
        destrutiva
        aoConfirmar={() =>
          typeof confirmar === 'object' && confirmar !== null && void estornar.mutateAsync(confirmar.estornar).then(fbAcao.onSuccess, fbAcao.onError)
        }
      />
    </div>
  )
}

/** Modal de baixa (§11.4): conta, data (hoje), forma, quitado (saldo), juros e multa sugeridos e editaveis, desconto, total ao vivo. */
function BaixaDialog({ titulo: t, aoFechar }: { titulo: Titulo; aoFechar: () => void }) {
  const receber = t.tipo === 'RECEBER'
  const [hoje] = useState(hojeIso)
  // a receber abre ja com a sugestao para hoje e o saldo; sem ela (erro), abre zerado e a pessoa informa
  const inicial = useSugestaoInicialDeBaixa(t.id, receber, hoje, t.saldo)
  if (receber && inicial.isLoading) {
    return (
      <Dialog open onOpenChange={(v) => !v && aoFechar()}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Receber: {t.descricao}</DialogTitle>
            <DialogDescription>Calculando juros e multa…</DialogDescription>
          </DialogHeader>
          <LoadingState />
        </DialogContent>
      </Dialog>
    )
  }
  return <BaixaForm titulo={t} hoje={hoje} juros={inicial.data?.juros ?? 0} multa={inicial.data?.multa ?? 0} aoFechar={aoFechar} />
}

function BaixaForm({ titulo: t, hoje, juros, multa, aoFechar }: { titulo: Titulo; hoje: string; juros: number; multa: number; aoFechar: () => void }) {
  const baixar = useBaixarTitulo(t.id)
  const [total, setTotal] = useState<number>(t.saldo + juros + multa)
  const fb = mutationFeedback({ success: t.tipo === 'RECEBER' ? 'Recebimento registrado.' : 'Pagamento registrado.', onDone: aoFechar })
  const calcular = (v: Partial<BaixaValues>) => setTotal((v.quitado ?? 0) + (v.juros ?? 0) + (v.multa ?? 0) - (v.desconto ?? 0))
  return (
    <Dialog open onOpenChange={(v) => !v && aoFechar()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t.tipo === 'RECEBER' ? 'Receber' : 'Pagar'}: {t.descricao}</DialogTitle>
          <DialogDescription>
            Saldo do título: {formatCurrency(t.saldo)}.{t.tipo === 'RECEBER' ? ' Juros e multa são sugeridos pela configuração; você pode mudar.' : ''}
          </DialogDescription>
        </DialogHeader>
        <AutoForm
          schema={baixaSchema}
          fields={camposBaixa}
          defaults={{ data: hoje, forma: t.formaPrevista ?? undefined, quitado: t.saldo, juros, multa, desconto: 0 }}
          submitLabel="Registrar baixa"
          isSubmitting={baixar.isPending}
          serverError={baixar.error}
          onCancel={aoFechar}
          onValuesChange={(v, { name, setValue }) => {
            calcular(v)
            // a sugestao (§6.2) so vale para titulo a receber e muda com a data e o valor quitado
            if (t.tipo === 'RECEBER' && (name === 'data' || name === 'quitado' || name === undefined) && v.data && v.data.length === 10) {
              void sugestaoDeBaixa(t.id, v.data, v.quitado).then(
                (s) => {
                  setValue('juros', s.juros)
                  setValue('multa', s.multa)
                },
                () => undefined,
              )
            }
          }}
          onSubmit={(v) =>
            baixar
              .mutateAsync({
                idConta: v.conta?.id,
                data: v.data,
                forma: v.forma,
                quitado: v.quitado,
                juros: v.juros ?? 0,
                multa: v.multa ?? 0,
                desconto: v.desconto ?? 0,
                observacao: v.observacao,
              })
              .then(fb.onSuccess, () => undefined)
          }
        />
        <p className="text-right text-sm">
          Total da baixa: <span className="text-base font-semibold">{formatCurrency(total)}</span>
        </p>
      </DialogContent>
    </Dialog>
  )
}

/** Quadro da cobranca no gateway (Fase 7, README 9.5): a viva com os dados de pagamento, o historico, gerar, atualizar e descartar. */
function QuadroDeCobranca({ titulo: t, administrador, operador }: { titulo: Titulo; administrador: boolean; operador: boolean }) {
  const lista = useCobrancasDoTitulo(t.id)
  const gerar = useGerarCobranca(t.id)
  const atualizar = useAtualizarCobranca()
  const descartar = useDescartarCobranca()
  const [tipo, setTipo] = useState<TipoCobranca>(t.formaPrevista === 'BOLETO' ? 'BOLETO' : 'PIX')
  const [confirmar, setConfirmar] = useState(false)
  const fb = mutationFeedback({ success: 'Cobrança gerada.', onDone: () => setConfirmar(false) })
  const fbAcao = mutationFeedback({ success: 'Registro salvo.' })
  const aberto = (t.situacao === 'ABERTO' || t.situacao === 'PARCIAL') && t.aprovado && !t.paraRevisao
  const viva = lista.data?.find((c) => c.viva)
  return (
    <Card>
      <CardHeader>
        <CardTitle>Cobrança (boleto, Pix ou link)</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {lista.isLoading ? (
          <LoadingState />
        ) : lista.isError ? (
          <ErrorState error={lista.error} onRetry={() => void lista.refetch()} />
        ) : !lista.data?.length ? (
          <p className="text-sm text-muted-foreground">Nenhuma cobrança gerada.</p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tipo</TableHead>
                <TableHead>Situação</TableHead>
                <TableHead>Valor</TableHead>
                <TableHead>Vencimento</TableHead>
                <TableHead>Como pagar</TableHead>
                <TableHead>
                  <span className="sr-only">Ações</span>
                </TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {lista.data.map((c) => (
                <TableRow key={c.id}>
                  <TableCell>{TIPO_COBRANCA[c.tipo]}</TableCell>
                  <TableCell>
                    <Badge tone={ESTADO_COBRANCA[c.estado].tone}>{ESTADO_COBRANCA[c.estado].rotulo}</Badge>
                    {c.motivo && <p className="text-xs text-muted-foreground">{c.motivo}</p>}
                  </TableCell>
                  <TableCell>{formatCurrency(c.valor)}</TableCell>
                  <TableCell>{formatDate(c.vencimento)}</TableCell>
                  <TableCell className="max-w-xs break-all text-xs">
                    {c.linhaDigitavel && <p>Linha digitável: {c.linhaDigitavel}</p>}
                    {c.pixCopiaECola && <p>Pix copia e cola: {c.pixCopiaECola}</p>}
                    {[c.urlDoBoleto, c.urlDoPix, c.urlDoLink]
                      .filter((u): u is string => !!u && u.startsWith('https://'))
                      .map((u) => (
                        <a key={u} href={u} target="_blank" rel="noopener noreferrer" className="block underline">
                          Abrir no provedor
                        </a>
                      ))}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-2">
                      {operador && c.viva && (
                        <Button variant="outline" size="sm" disabled={atualizar.isPending} onClick={() => void atualizar.mutateAsync(c.id).then(fbAcao.onSuccess, fbAcao.onError)}>
                          Atualizar situação
                        </Button>
                      )}
                      {administrador && (c.estado === 'INCERTA' || c.estado === 'RECUSADA') && (
                        <Button variant="outline" size="sm" disabled={descartar.isPending} onClick={() => void descartar.mutateAsync(c.id).then(fbAcao.onSuccess, fbAcao.onError)}>
                          Descartar
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
        {viva?.estado === 'INCERTA' && (
          <Alert variant="warning">
            <AlertDescription>O provedor não respondeu. Confira no portal do provedor antes de gerar outra cobrança; se não existir lá, descarte esta.</AlertDescription>
          </Alert>
        )}
        {administrador && aberto && !viva && (
          <div className="flex flex-wrap items-end gap-2">
            <div className="flex flex-col gap-1">
              <span className="text-xs text-muted-foreground">Tipo da cobrança</span>
              <Select value={tipo} onValueChange={(v) => setTipo(v as TipoCobranca)}>
                <SelectTrigger className="w-48" aria-label="Tipo da cobrança">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(Object.keys(TIPO_COBRANCA) as TipoCobranca[]).map((k) => (
                    <SelectItem key={k} value={k}>
                      {TIPO_COBRANCA[k]}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <Button onClick={() => setConfirmar(true)}>Gerar cobrança</Button>
          </div>
        )}
        <Confirmar
          aberto={confirmar}
          aoMudar={(v) => !v && setConfirmar(false)}
          titulo="Gerar esta cobrança?"
          texto={`O provedor gera ${TIPO_COBRANCA[tipo].toLowerCase()} de ${formatCurrency(t.saldo)} para o cliente. Se o título for cancelado, cancele também no portal do provedor.`}
          acao="Gerar cobrança"
          aoConfirmar={() => void gerar.mutateAsync({ tipo }).then(fb.onSuccess, fb.onError)}
        />
      </CardContent>
    </Card>
  )
}

/** Cobranca por e-mail (Fase 6, README 9.3): enviar (so administrador, com confirmacao) e o historico do titulo. */
function QuadroDeEmail({ titulo: t, administrador }: { titulo: Titulo; administrador: boolean }) {
  const enviadas = useCobrancasEnviadas(t.id)
  const enviar = useEnviarCobranca(t.id)
  const [confirmar, setConfirmar] = useState(false)
  const fb = mutationFeedback({ success: 'Cobrança enviada por e-mail.', onDone: () => setConfirmar(false) })
  const aberto = (t.situacao === 'ABERTO' || t.situacao === 'PARCIAL') && t.aprovado && !t.paraRevisao
  return (
    <Card>
      <CardHeader>
        <CardTitle>Cobrança por e-mail</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {enviadas.isLoading ? (
          <LoadingState />
        ) : !enviadas.data?.length ? (
          <p className="text-sm text-muted-foreground">Nenhuma cobrança enviada.</p>
        ) : (
          <ul className="flex flex-col gap-1 text-sm">
            {enviadas.data.map((e) => (
              <li key={e.id}>
                {formatDateTime(e.enviadoEm)} · {e.origem === 'MANUAL' ? 'Enviada' : 'Lembrete'} para {e.email}
              </li>
            ))}
          </ul>
        )}
        {administrador && aberto && (
          <div>
            <Button variant="outline" onClick={() => setConfirmar(true)}>
              Enviar cobrança por e-mail
            </Button>
          </div>
        )}
        <Confirmar
          aberto={confirmar}
          aoMudar={(v) => !v && setConfirmar(false)}
          titulo="Enviar a cobrança ao cliente?"
          texto="O cliente recebe um e-mail com o valor, o vencimento e como pagar. No máximo uma cobrança por cliente por dia."
          acao="Enviar"
          aoConfirmar={() => void enviar.mutateAsync(undefined).then(fb.onSuccess, fb.onError)}
        />
      </CardContent>
    </Card>
  )
}

