import { useNavigate } from '@tanstack/react-router'
import { CircleCheck, Info, TriangleAlert } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Spinner } from '@/components/ui/spinner'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { FormularioNota } from '@/features/notas/NotaNova'
import {
  ESTADO,
  aguardaPrefeitura,
  corpoDaNota,
  erroAmigavel,
  mensagemErroNota,
  urlPdf,
  useAtualizarNota,
  useNota,
  useReenviarNota,
  valoresDaNota,
  type NotaDetalhe as Nota,
} from '@/features/notas/notasApi'
import { ApiError, abrirPdf } from '@/lib/api'
import { canAct, useSession } from '@/lib/auth'
import { errorMessage, notifyError, notifySuccess } from '@/runtime/feedback'
import { formatCurrency, formatDateTime, maskDocumento } from '@/runtime/format'
import { ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Notas Fiscais > NFS-e > detalhe e acompanhamento. Escrito a mao com o runtime: as telas de nota tem fluxo proprio de envio e
 * acompanhamento (decisao explicita do usuario). Consulta a cada 3 s enquanto aguarda a prefeitura; rejeitada/rascunho reenvia
 * mantendo o numero. Em conflito nunca reenvia: so Atualizar agora.
 * ------------------------------------------------------------------------- */

function Linha({ rotulo, valor }: { rotulo: string; valor?: string | null }) {
  if (!valor) return null
  return (
    <div className="flex flex-col gap-0.5">
      <dt className="text-xs text-muted-foreground">{rotulo}</dt>
      <dd className="text-sm break-all whitespace-pre-line">{valor}</dd>
    </div>
  )
}

function Dados({ n, somenteLeitura }: { n: Nota; somenteLeitura: boolean }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Dados</CardTitle>
      </CardHeader>
      <CardContent>
        <dl className="grid gap-4 sm:grid-cols-2">
          <Linha rotulo="Cliente" valor={`${n.pessoa.nome}${n.pessoa.documento ? ` - ${maskDocumento(n.pessoa.documento)}` : ''}`} />
          <Linha rotulo="Serviço" valor={n.servico.nome} />
          <Linha rotulo="Valor" valor={formatCurrency(n.valorServicos)} />
          <Linha rotulo="Emissão" valor={formatDateTime(n.dataEmissao)} />
          <Linha rotulo="Número da NFS-e" valor={n.numeroNfse} />
          <Linha rotulo="Código de verificação" valor={n.codigoVerificacao} />
          <Linha rotulo="Chave de acesso" valor={n.chave} />
          {somenteLeitura && <Linha rotulo="Descrição do serviço" valor={n.discriminacao} />}
          {somenteLeitura && <Linha rotulo="Informações complementares" valor={n.informacoesComplementares} />}
        </dl>
      </CardContent>
    </Card>
  )
}

function Conteudo({ id }: { id: string }) {
  const navigate = useNavigate()
  const session = useSession()
  const q = useNota(id)
  const atualizar = useAtualizarNota(id)
  const reenviar = useReenviarNota(id)
  if (q.isLoading) return <LoadingState label="Carregando nota…" />
  if (q.isError || !q.data) return <ErrorState error={q.error} title="Não foi possível carregar a nota." onRetry={() => void q.refetch()} />
  const n = q.data
  const operador = canAct(session, 'operador')
  const estado = ESTADO[n.estado]
  const aguardando = aguardaPrefeitura(n.estado)
  const conflito = n.estado === 'em_conflito'
  const reenvio = operador && n.podeReenviar && (n.estado === 'rejeitada' || n.estado === 'rascunho')
  const botaoAtualizar =
    operador && n.podeAtualizar && (aguardando || conflito) ? (
      <Button variant="outline" size="sm" disabled={atualizar.isPending} onClick={() => atualizar.mutate(undefined, { onError: (e) => notifyError(e, 'Não foi possível atualizar a nota') })}>
        {atualizar.isPending && <Spinner />} Atualizar agora
      </Button>
    ) : null
  const abrir = () => void abrirPdf(urlPdf(n.id)).catch((e) => notifyError(e, 'Não foi possível abrir o PDF'))
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4">
      <div className="flex flex-wrap items-center gap-2">
        <h1 className="text-2xl font-semibold">
          Nota {n.serie}-{n.numero}
        </h1>
        <Badge tone={estado.tone}>{estado.rotulo}</Badge>
        <Button variant="ghost" size="sm" className="ml-auto" onClick={() => void navigate({ to: '/notas' })}>
          Voltar à lista
        </Button>
      </div>

      {aguardando && (
        <Alert variant="info">
          <Info aria-hidden />
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span className="flex items-center gap-2">
              <Spinner /> Aguardando a autorização da prefeitura…
            </span>
            {botaoAtualizar}
          </AlertDescription>
        </Alert>
      )}
      {conflito && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription className="flex flex-wrap items-center gap-3">
            <span>A prefeitura indicou conflito. Não reenvie: use Atualizar agora.</span>
            {botaoAtualizar}
          </AlertDescription>
        </Alert>
      )}
      {atualizar.isError && (
        <Alert variant="destructive">
          <TriangleAlert aria-hidden />
          <AlertDescription>{atualizar.error instanceof ApiError ? mensagemErroNota(atualizar.error) : errorMessage(atualizar.error)}</AlertDescription>
        </Alert>
      )}
      {n.estado === 'autorizada' && (
        <Alert variant="success">
          <CircleCheck aria-hidden />
          <AlertDescription>NFS-e autorizada.</AlertDescription>
        </Alert>
      )}
      {(n.estado === 'rejeitada' || (n.estado === 'rascunho' && (n.mensagemRetorno || n.errosRetorno.length > 0))) && (
        <Alert variant="destructive">
          <TriangleAlert aria-hidden />
          <AlertTitle>{n.mensagemRetorno ?? 'A nota não foi aceita.'}</AlertTitle>
          {n.errosRetorno.length > 0 && (
            <AlertDescription>
              <ul className="list-disc pl-5">
                {n.errosRetorno.map((e, i) => (
                  <li key={`${e.campo}-${i}`}>
                    {e.campo ? `${e.campo}: ` : ''}
                    {e.descricao || e.erro}
                  </li>
                ))}
              </ul>
            </AlertDescription>
          )}
        </Alert>
      )}
      {n.estado === 'rascunho' && !n.mensagemRetorno && n.errosRetorno.length === 0 && (
        <Alert variant="info">
          <Info aria-hidden />
          <AlertDescription>Esta nota ainda não foi enviada.</AlertDescription>
        </Alert>
      )}

      <Dados n={n} somenteLeitura={!reenvio} />

      {n.estado === 'autorizada' && (
        <div className="flex flex-wrap items-center gap-2">
          {n.temPdf ? (
            <Button onClick={abrir}>Visualizar PDF</Button>
          ) : n.linkPdf ? (
            <Button asChild>
              <a href={n.linkPdf} target="_blank" rel="noreferrer">
                Visualizar PDF
              </a>
            </Button>
          ) : null}
          <Tooltip>
            <TooltipTrigger asChild>
              <span tabIndex={0}>
                <Button variant="outline" disabled>
                  Enviar por e-mail
                </Button>
              </span>
            </TooltipTrigger>
            <TooltipContent>Em breve.</TooltipContent>
          </Tooltip>
        </div>
      )}

      {reenvio && (
        <div className="flex flex-col gap-2">
          <FormularioNota
            values={valoresDaNota(n)}
            submitLabel="Corrigir e reenviar"
            isSubmitting={reenviar.isPending}
            serverError={reenviar.error ? erroAmigavel(reenviar.error) : undefined}
            onSubmit={(v) =>
              reenviar.mutateAsync(corpoDaNota(v)).then(
                () => notifySuccess('Nota reenviada.'),
                () => {},
              )
            }
          />
          <p className="text-xs text-muted-foreground">O número da nota é mantido no reenvio.</p>
        </div>
      )}
    </div>
  )
}

export function NotaDetalhe({ id }: { id: string }) {
  return <Conteudo id={id} />
}
