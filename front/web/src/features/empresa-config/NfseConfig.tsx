import { useMemo } from 'react'
import { Info, TriangleAlert } from 'lucide-react'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge, type BadgeTone } from '@/components/ui/badge'
import { Spinner } from '@/components/ui/spinner'
import { useConfigNfse, useSalvarConfigNfse, type CertificadoSituacao, type ConfigNfse, type SalvarNfse, type StatusCadastroNfse } from '@/features/empresa-config/nfseApi'
import { ApiError } from '@/lib/api'
import { canAct, useSession } from '@/lib/auth'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { notifySuccess } from '@/runtime/feedback'
import { formatDate, formatDateTime } from '@/runtime/format'
import { ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Configuracao > NFS-e da EMPRESA da sessao (so administrador). Escrito a mao com o runtime: tela de configuracao
 * singleton, fora do padrao lista+formulario (decisao explicita do usuario). Ao habilitar, o back cadastra a empresa
 * como emitente na IntegraNotas; o token do emitente nunca chega aqui.
 * ------------------------------------------------------------------------- */

const schema = z
  .object({
    habilitada: z.boolean(),
    serie: z.string().optional(),
    ultimoNumero: z.number().optional(),
  })
  .superRefine((v, ctx) => {
    const serie = (v.serie ?? '').trim()
    if (serie === '' && v.habilitada) ctx.addIssue({ code: 'custom', path: ['serie'], message: 'Informe a série para habilitar a emissão.' })
    else if (serie !== '' && !/^[0-9]{1,5}$/.test(serie)) ctx.addIssue({ code: 'custom', path: ['serie'], message: 'A série deve ter de 1 a 5 dígitos.' })
    if (v.ultimoNumero === undefined) {
      if (v.habilitada) ctx.addIssue({ code: 'custom', path: ['ultimoNumero'], message: 'Informe o último número (0 se nunca emitiu).' })
    } else if (!Number.isInteger(v.ultimoNumero) || v.ultimoNumero < 0 || v.ultimoNumero > 999999999) {
      ctx.addIssue({ code: 'custom', path: ['ultimoNumero'], message: 'O último número deve ser um inteiro entre 0 e 999999999.' })
    }
  })

type Valores = z.infer<typeof schema>

const STATUS_CADASTRO: Record<StatusCadastroNfse, { rotulo: string; tone: BadgeTone }> = {
  CADASTRADO: { rotulo: 'cadastrado', tone: 'success' },
  NAO_CADASTRADO: { rotulo: 'não cadastrado', tone: 'neutral' },
  ERRO: { rotulo: 'com erro', tone: 'danger' },
}

const CERTIFICADO: Record<CertificadoSituacao, string> = {
  NAO_ENVIADO: 'não enviado',
  VALIDO: 'válido',
  VENCIDO: 'vencido',
  INVALIDO: 'inválido',
  NAO_PERTENCE: 'de outra empresa',
}

const CAMPOS: Record<string, string> = {
  cnpj: 'CNPJ',
  razaoSocial: 'razão social',
  nomeFantasia: 'nome fantasia',
  inscricaoMunicipal: 'inscrição municipal',
  regimeTributario: 'regime tributário',
  cep: 'CEP',
  logradouro: 'logradouro',
  numeroEndereco: 'número do endereço',
  bairro: 'bairro',
  municipio: 'município',
  codigoMunicipio: 'município',
  uf: 'UF',
  email: 'e-mail',
  telefone: 'telefone',
}

/** Mensagem amigavel por codigo de erro; codigo desconhecido cai na mensagem do back. */
function mensagemErro(e: unknown): string {
  if (!(e instanceof ApiError)) return 'Algo deu errado. Tente novamente em instantes.'
  switch (e.codigo) {
    case 'cadastro_incompleto': {
      const campos = Array.isArray(e.detalhes?.campos) ? (e.detalhes.campos as { campo?: string; mensagem?: string }[]) : []
      const nomes = campos.map((c) => (c.campo && CAMPOS[c.campo]) || c.mensagem || c.campo).filter((n): n is string => !!n)
      return nomes.length > 0 ? `Faltam dados da empresa: ${[...new Set(nomes)].join(', ')}.` : e.mensagem
    }
    case 'integranotas_recusou':
      return `A IntegraNotas recusou o cadastro: ${e.mensagem}`
    case 'integranotas_token_recusado':
      return 'A IntegraNotas recusou o token da integração. Fale com o suporte.'
    case 'integranotas_indisponivel':
      return 'A IntegraNotas não respondeu a tempo. Tente de novo.'
    case 'numeracao_invalida':
      return 'A numeração informada conflita com notas já emitidas.'
    case 'integranotas_nao_configurada':
      return 'A integração com a IntegraNotas não está configurada neste servidor. Fale com o suporte.'
    default:
      return e.mensagem
  }
}

function layout(c: ConfigNfse): FieldLayout {
  return [
    { section: 'Emissão de NFS-e' },
    [
      { name: 'habilitada', label: 'Habilitar a emissão de NFS-e', kind: 'boolean', size: 'l', readOnly: !c.integracaoDisponivel && !c.habilitada },
      { name: 'serie', label: 'Série', size: 's', maxLength: 5, hint: 'Série da DPS, 1 a 5 dígitos.' },
      { name: 'ultimoNumero', label: 'Último número', kind: 'number', size: 's', hint: 'Último número de NFS-e já utilizado (0 se nunca emitiu). A próxima nota será o número seguinte.' },
    ],
  ]
}

function corpo(v: Valores): SalvarNfse {
  const body: SalvarNfse = { habilitada: v.habilitada }
  const serie = (v.serie ?? '').trim()
  if (serie !== '') body.serie = serie
  if (v.ultimoNumero !== undefined) body.ultimoNumero = v.ultimoNumero
  return body
}

function Formulario({ config }: { config: ConfigNfse }) {
  const salvar = useSalvarConfigNfse()
  const fields = useMemo(() => layout(config), [config])
  const values = useMemo<Valores>(() => ({ habilitada: config.habilitada, serie: config.serie, ultimoNumero: config.ultimoNumero }), [config])
  const primeiraHabilitacao = !config.habilitada && config.statusCadastro !== 'CADASTRADO'
  const cadastrando = salvar.isPending && salvar.variables?.habilitada === true && !config.habilitada

  return (
    <div className="flex flex-col gap-4">
      {salvar.isError && (
        <Alert variant="destructive">
          <TriangleAlert aria-hidden />
          <AlertDescription>{mensagemErro(salvar.error)}</AlertDescription>
        </Alert>
      )}
      {cadastrando && (
        <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner /> Cadastrando na IntegraNotas…
        </p>
      )}
      <p className="text-sm text-muted-foreground">Próximo número: {config.proximoNumero}</p>
      <AutoForm
        schema={schema}
        fields={fields}
        defaults={{ habilitada: false, serie: '', ultimoNumero: 0 }}
        values={values}
        isSubmitting={salvar.isPending}
        submitLabel={primeiraHabilitacao ? 'Salvar e habilitar' : 'Salvar'}
        onSubmit={(v) =>
          salvar.mutateAsync(corpo(v)).then(
            () => notifySuccess(v.habilitada && !config.habilitada ? 'NFS-e habilitada.' : 'Configuração da NFS-e salva.'),
            () => {},
          )
        }
      />
    </div>
  )
}

function Conteudo() {
  const q = useConfigNfse()
  if (q.isLoading) return <LoadingState label="Carregando configuração…" />
  if (q.isError || !q.data) return <ErrorState error={q.error} title="Não foi possível carregar a configuração." onRetry={() => void q.refetch()} />
  const c = q.data
  const cadastro = STATUS_CADASTRO[c.statusCadastro]
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold">NFS-e</h1>
        <p className="text-sm text-muted-foreground">Emissão de notas fiscais de serviço da sua empresa pela IntegraNotas.</p>
      </div>
      <Alert variant="info">
        <Info aria-hidden />
        <AlertDescription>Ao habilitar a emissão, o sistema cadastra a sua empresa como emitente na IntegraNotas. Antes, confira os dados em Configuração &gt; Empresa.</AlertDescription>
      </Alert>
      {!c.integracaoDisponivel && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription>A integração com a IntegraNotas não está configurada neste servidor. Fale com o suporte.</AlertDescription>
        </Alert>
      )}
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <Badge tone={c.habilitada ? 'success' : 'neutral'}>{c.habilitada ? 'Emissão habilitada' : 'Emissão desabilitada'}</Badge>
        <Badge tone={cadastro.tone}>
          Cadastro no gateway: {cadastro.rotulo}
          {c.dataCadastroGateway ? ` em ${formatDateTime(c.dataCadastroGateway)}` : ''}
        </Badge>
        <Badge tone={c.certificadoSituacao === 'VALIDO' ? 'success' : c.certificadoSituacao === 'NAO_ENVIADO' ? 'neutral' : 'danger'}>
          Certificado: {CERTIFICADO[c.certificadoSituacao]}
          {c.validadeCertificado ? ` (validade ${formatDate(c.validadeCertificado)})` : ''}
        </Badge>
        <Badge tone={c.ambiente === 'PRODUCAO' ? 'info' : 'warning'}>{c.ambiente === 'PRODUCAO' ? 'Produção' : 'Homologação'}</Badge>
      </div>
      <Formulario config={c} />
    </div>
  )
}

export function NfseConfig() {
  const session = useSession()
  // A tela e da empresa da sessao: administrador da plataforma sem empresa (sem papel) tambem fica de fora. So UX: o back decide.
  if (!canAct(session, 'ADMINISTRADOR')) return <ErrorState title="Acesso restrito ao administrador da empresa." />
  return <Conteudo />
}
