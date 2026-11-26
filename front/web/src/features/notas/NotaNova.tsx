import { useCallback, useMemo, useRef, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Info, TriangleAlert } from 'lucide-react'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Spinner } from '@/components/ui/spinner'
import { useConfigNfse } from '@/features/empresa-config/nfseApi'
import {
  buscarPadroesDoServico,
  contasDaNota,
  corpoDaNota,
  erroAmigavel,
  layoutDaNota,
  notaSchema,
  tributosDoServico,
  useCriarNota,
  type ContasDaNota,
  type NotaValues,
} from '@/features/notas/notasApi'
import { canAct, useSession } from '@/lib/auth'
import { AutoForm } from '@/runtime/AutoForm'
import { notifySuccess } from '@/runtime/feedback'
import { ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Notas Fiscais > NFS-e > Nova nota. Escrito a mao com o runtime: as telas de nota tem fluxo proprio de envio e acompanhamento
 * (decisao explicita do usuario). O botao Enviar e a aprovacao da pessoa: nada emite nota sozinho.
 * ------------------------------------------------------------------------- */

const DEFAULTS = { valorServicos: undefined, discriminacao: '', informacoesComplementares: '', issRetido: false } as unknown as NotaValues

const mesmasContas = (a: ContasDaNota, b: ContasDaNota) => a.baseIss === b.baseIss && a.issRetido === b.issRetido && a.baseFederal === b.baseFederal

/**
 * Formulario da nota (nova e reenvio). Ao escolher o servico: preenche a descricao com o texto padrao (se a pessoa ainda nao a
 * editou), o valor com o valor padrao (se estiver vazio) e os quadros de tributacao com os padroes do servico. Os campos de base e
 * de ISS retido em branco mostram em cinza a conta que o sistema faz.
 */
export function FormularioNota({
  values,
  submitLabel,
  isSubmitting,
  serverError,
  onSubmit,
  onCancel,
}: {
  values?: NotaValues
  submitLabel: string
  isSubmitting: boolean
  serverError?: unknown
  onSubmit: (v: NotaValues) => void | Promise<void>
  onCancel?: () => void
}) {
  const editada = useRef(!!values?.discriminacao)
  const automatica = useRef('')
  const sequencia = useRef(0)
  const [contas, setContas] = useState<ContasDaNota>(() => contasDaNota(values ?? {}))
  const layout = useMemo(() => layoutDaNota(contas), [contas])
  const aoMudar = useCallback((v: Partial<NotaValues>, ctx: { name?: string; setValue: (n: string, val: unknown) => void }) => {
    const novas = contasDaNota(v)
    setContas((atuais) => (mesmasContas(atuais, novas) ? atuais : novas))
    // base e ISS retido sao so calculados: a tela mostra o mesmo numero que o back e o governo vao usar
    if (ctx.name !== 'valorBaseCalculoIss' && ctx.name !== 'valorIssRetido') {
      if (v.valorBaseCalculoIss !== novas.baseIss) ctx.setValue('valorBaseCalculoIss', novas.baseIss)
      if (v.valorIssRetido !== novas.issRetido) ctx.setValue('valorIssRetido', novas.issRetido)
    }
    if (ctx.name === 'discriminacao') {
      const d = v.discriminacao ?? ''
      editada.current = d.trim() !== '' && d !== automatica.current
      return
    }
    if (ctx.name !== 'servico' || !v.servico?.id) return
    const minha = ++sequencia.current
    const valorAtual = v.valorServicos
    void buscarPadroesDoServico(v.servico.id).then(
      (s) => {
        if (minha !== sequencia.current) return
        if (valorAtual === undefined && s.valorPadrao !== undefined) ctx.setValue('valorServicos', s.valorPadrao)
        for (const [campo, valor] of Object.entries(tributosDoServico(s))) ctx.setValue(campo, valor)
        if (!s.discriminacaoPadrao || editada.current) return
        automatica.current = s.discriminacaoPadrao
        ctx.setValue('discriminacao', s.discriminacaoPadrao)
      },
      () => {},
    )
  }, [])
  return (
    <div className="flex flex-col gap-4">
      {isSubmitting && (
        <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner /> Enviando à IntegraNotas…
        </p>
      )}
      <AutoForm
        schema={notaSchema}
        fields={layout}
        defaults={DEFAULTS}
        values={values}
        isSubmitting={isSubmitting}
        submitLabel={submitLabel}
        serverError={serverError}
        onSubmit={onSubmit}
        onCancel={onCancel}
        onValuesChange={aoMudar}
      />
    </div>
  )
}

function Conteudo() {
  const navigate = useNavigate()
  const cfg = useConfigNfse()
  const criar = useCriarNota()
  if (cfg.isLoading) return <LoadingState label="Carregando…" />
  if (cfg.isError || !cfg.data) return <ErrorState error={cfg.error} title="Não foi possível carregar a configuração da NFS-e." onRetry={() => void cfg.refetch()} />
  const c = cfg.data
  const semCertificado = c.certificadoSituacao !== 'VALIDO'
  const semEmitente = c.habilitada && c.statusCadastro !== 'CADASTRADO'
  const bloqueado = !c.habilitada || semCertificado || semEmitente || !c.integracaoDisponivel
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4">
      <div className="flex flex-wrap items-center gap-2">
        <h1 className="text-2xl font-semibold">Nova nota</h1>
        <Badge tone={c.ambiente === 'PRODUCAO' ? 'info' : 'warning'}>{c.ambiente === 'PRODUCAO' ? 'Produção' : 'Homologação'}</Badge>
      </div>
      {!c.habilitada && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription>A emissão de NFS-e não está habilitada. Habilite em Configuração &gt; NFS-e.</AlertDescription>
        </Alert>
      )}
      {semEmitente && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription>A empresa ainda não está cadastrada como emitente na IntegraNotas. Confira em Configuração &gt; NFS-e.</AlertDescription>
        </Alert>
      )}
      {semCertificado && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription>O certificado digital não está válido. Envie em Configuração &gt; Empresa.</AlertDescription>
        </Alert>
      )}
      {!c.integracaoDisponivel && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription>A integração com a IntegraNotas não está configurada neste servidor. Fale com o suporte.</AlertDescription>
        </Alert>
      )}
      {!bloqueado && (
        <Alert variant="info">
          <Info aria-hidden />
          <AlertDescription>Ao enviar, a nota é emitida e segue para a prefeitura. Confira os dados antes.</AlertDescription>
        </Alert>
      )}
      <fieldset disabled={bloqueado} className="min-w-0 border-0 p-0">
        <FormularioNota
          submitLabel="Enviar"
          isSubmitting={criar.isPending}
          serverError={criar.error ? erroAmigavel(criar.error) : undefined}
          onCancel={() => void navigate({ to: '/notas' })}
          onSubmit={(v) =>
            criar.mutateAsync(corpoDaNota(v)).then(
              (nota) => {
                notifySuccess('Nota enviada.')
                void navigate({ to: '/notas/$id', params: { id: nota.id } })
              },
              () => {},
            )
          }
        />
      </fieldset>
    </div>
  )
}

export function NotaNova() {
  const session = useSession()
  if (!canAct(session, 'OPERADOR')) return <ErrorState title="Acesso restrito a quem pode emitir notas." />
  return <Conteudo />
}
