import { useMemo } from 'react'
import { Info, TriangleAlert } from 'lucide-react'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { PlatformOnly } from '@/features/admin/PlatformOnly'
import { useConfigIntegraNotas, useSalvarConfigIntegraNotas, type ConfigIntegraNotas, type SalvarIntegraNotas } from '@/features/admin/adminApi'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { notifySuccess } from '@/runtime/feedback'
import { formatDateTime } from '@/runtime/format'
import { ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Integracao IntegraNotas: ambiente ativo, enderecos das APIs, webhook e tokens da softhouse. Escrito a mao com o
 * runtime (tela de configuracao singleton da plataforma). Os tokens NUNCA voltam do back: os campos ficam sempre
 * vazios e so sao enviados se preenchidos. Semantica no PUT: campo ausente = mantem; texto = grava; '' = remove
 * (tokens e webhook). So os campos alterados sao enviados.
 * ------------------------------------------------------------------------- */

const urlHttps = (msg: string) => z.string().trim().min(1, 'Informe a URL.').max(200, 'A URL deve ter no máximo 200 caracteres.').regex(/^https:\/\/\S+$/i, msg)

const schema = z
  .object({
    ambienteAtivo: z.enum(['HOMOLOGACAO', 'PRODUCAO']),
    urlHomologacao: urlHttps('A URL deve começar com https://'),
    urlProducao: urlHttps('A URL deve começar com https://'),
    urlPublicaWebhook: z
      .string()
      .trim()
      .max(200, 'A URL deve ter no máximo 200 caracteres.')
      .refine((v) => v === '' || /^https:\/\/\S+$/i.test(v), 'A URL deve começar com https://')
      .optional(),
    tokenHomologacao: z.string().max(500).optional(),
    removerTokenHomologacao: z.boolean().optional(),
    tokenProducao: z.string().max(500).optional(),
    removerTokenProducao: z.boolean().optional(),
  })
  .refine((v) => !(v.removerTokenHomologacao && v.tokenHomologacao), { path: ['tokenHomologacao'], message: 'Desmarque "Remover o token de homologação configurado" para informar um novo token.' })
  .refine((v) => !(v.removerTokenProducao && v.tokenProducao), { path: ['tokenProducao'], message: 'Desmarque "Remover o token de produção configurado" para informar um novo token.' })

type Valores = z.infer<typeof schema>

const AMBIENTES = [
  { value: 'HOMOLOGACAO', label: 'Homologação' },
  { value: 'PRODUCAO', label: 'Produção' },
]

const hintToken = (configurado: boolean) => (configurado ? 'Token configurado. Deixe em branco para manter.' : 'Cole o token da softhouse neste ambiente.')

function layout(c: ConfigIntegraNotas): FieldLayout {
  return [
    { section: 'Ambiente' },
    [{ name: 'ambienteAtivo', label: 'Ambiente ativo', options: AMBIENTES, size: 'l', required: true }],
    { section: 'Endereços da API' },
    [
      { name: 'urlHomologacao', label: 'URL de homologação', maxLength: 200, size: 'l', required: true },
      { name: 'urlProducao', label: 'URL de produção', maxLength: 200, size: 'l', required: true },
    ],
    { section: 'Webhook' },
    [
      {
        name: 'urlPublicaWebhook',
        label: 'URL pública do webhook',
        maxLength: 200,
        size: 'full',
        hint: 'URL pública deste back, ex.: https://api.suaempresa.com.br. Deixe vazio para não registrar webhook; a emissão usará consulta periódica.',
      },
    ],
    { section: 'Tokens da softhouse' },
    [
      { name: 'tokenHomologacao', label: 'Token de homologação', kind: 'password', maxLength: 500, size: 'l', hint: hintToken(c.tokenHomologacaoConfigurado) },
      ...(c.tokenHomologacaoConfigurado ? [{ name: 'removerTokenHomologacao', label: 'Remover o token de homologação configurado', kind: 'boolean' as const, size: 'l' as const }] : []),
    ],
    [
      { name: 'tokenProducao', label: 'Token de produção', kind: 'password', maxLength: 500, size: 'l', hint: hintToken(c.tokenProducaoConfigurado) },
      ...(c.tokenProducaoConfigurado ? [{ name: 'removerTokenProducao', label: 'Remover o token de produção configurado', kind: 'boolean' as const, size: 'l' as const }] : []),
    ],
  ]
}

/** Corpo do PUT so com o que mudou em relacao ao salvo. */
function corpo(v: Valores, c: ConfigIntegraNotas): SalvarIntegraNotas {
  const body: SalvarIntegraNotas = {}
  if (v.ambienteAtivo !== c.ambienteAtivo) body.ambienteAtivo = v.ambienteAtivo
  if (v.urlHomologacao !== c.urlHomologacao) body.urlHomologacao = v.urlHomologacao
  if (v.urlProducao !== c.urlProducao) body.urlProducao = v.urlProducao
  const webhook = (v.urlPublicaWebhook ?? '').trim()
  if (webhook !== c.urlPublicaWebhook) body.urlPublicaWebhook = webhook
  const tokenH = v.removerTokenHomologacao ? '' : v.tokenHomologacao ? v.tokenHomologacao : undefined
  if (tokenH !== undefined) body.tokenHomologacao = tokenH
  const tokenP = v.removerTokenProducao ? '' : v.tokenProducao ? v.tokenProducao : undefined
  if (tokenP !== undefined) body.tokenProducao = tokenP
  return body
}

function Formulario({ config }: { config: ConfigIntegraNotas }) {
  const salvar = useSalvarConfigIntegraNotas()
  const fields = useMemo(() => layout(config), [config])
  const values = useMemo<Valores>(
    () => ({
      ambienteAtivo: config.ambienteAtivo,
      urlHomologacao: config.urlHomologacao,
      urlProducao: config.urlProducao,
      urlPublicaWebhook: config.urlPublicaWebhook,
      tokenHomologacao: '',
      removerTokenHomologacao: false,
      tokenProducao: '',
      removerTokenProducao: false,
    }),
    [config],
  )

  return (
    <AutoForm
      schema={schema}
      fields={fields}
      defaults={{ ambienteAtivo: 'HOMOLOGACAO', urlHomologacao: '', urlProducao: '', urlPublicaWebhook: '', tokenHomologacao: '', removerTokenHomologacao: false, tokenProducao: '', removerTokenProducao: false }}
      values={values}
      clearAfterSubmit={['tokenHomologacao', 'tokenProducao', 'removerTokenHomologacao', 'removerTokenProducao']}
      isSubmitting={salvar.isPending}
      serverError={salvar.error}
      submitLabel="Salvar"
      onSubmit={(v) =>
        salvar.mutateAsync(corpo(v, config)).then(
          () => notifySuccess('Configuração salva.'),
          () => {},
        )
      }
    />
  )
}

function SeloToken({ rotulo, configurado }: { rotulo: string; configurado: boolean }) {
  return (
    <div className="flex items-center gap-2">
      <span>{rotulo}:</span>
      {configurado ? <Badge tone="success">Configurado</Badge> : <Badge tone="neutral">Sem token</Badge>}
    </div>
  )
}

function Conteudo() {
  const q = useConfigIntegraNotas()
  if (q.isLoading) return <LoadingState label="Carregando configuração…" />
  if (q.isError || !q.data) return <ErrorState error={q.error} title="Não foi possível carregar a configuração." onRetry={() => void q.refetch()} />
  const c = q.data
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold">Integração IntegraNotas</h1>
        <p className="text-sm text-muted-foreground">Ambiente, endereços, webhook e tokens usados pela plataforma para emitir notas pela IntegraNotas.</p>
      </div>
      <Alert variant="info">
        <Info aria-hidden />
        <AlertDescription>Homologação é o ambiente de testes da IntegraNotas. O token da softhouse cadastra os emitentes (empresas) e fica guardado cifrado no servidor.</AlertDescription>
      </Alert>
      {!c.producaoPermitida && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription>Produção está bloqueada neste servidor: só homologação.</AlertDescription>
        </Alert>
      )}
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
        <SeloToken rotulo="Token de homologação" configurado={c.tokenHomologacaoConfigurado} />
        <SeloToken rotulo="Token de produção" configurado={c.tokenProducaoConfigurado} />
      </div>
      <Formulario config={c} />
      {c.dataAtualizacao && (
        <p className="text-xs text-muted-foreground">
          Última atualização{c.atualizadoPor ? ` por ${c.atualizadoPor.nome}` : ''} em {formatDateTime(c.dataAtualizacao)}
        </p>
      )}
    </div>
  )
}

export function IntegraNotasAdmin() {
  return (
    <PlatformOnly>
      <Conteudo />
    </PlatformOnly>
  )
}
