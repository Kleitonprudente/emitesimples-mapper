import { useEffect, useMemo, useRef, useState } from 'react'
import { Info, TriangleAlert } from 'lucide-react'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge, type BadgeTone } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Spinner } from '@/components/ui/spinner'
import { PlatformOnly } from '@/features/admin/PlatformOnly'
import { ApiError } from '@/lib/api'
import { useConfigConsultaCnpj, useSalvarConfigConsultaCnpj, useTestarConsultaCnpj, type ConfigConsultaCnpj, type ResumoConsultaCnpj } from '@/features/admin/adminApi'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { notifySuccess } from '@/runtime/feedback'
import { formatCurrency, formatDate, formatDateTime, maskCEP, maskCNPJ, maskPhone, onlyDigits } from '@/runtime/format'
import { SearchInput } from '@/runtime/SearchInput'
import { ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Consulta de CNPJ (CNPJ.ws): URL/endpoint e token da plataforma. Escrito a mao com o runtime (tela de
 * configuracao singleton). O token NUNCA volta do back: o campo fica sempre vazio e so e enviado se preenchido.
 * Semantica do token no PUT: ausente = mantem; texto = grava/substitui; '' = remove.
 * ------------------------------------------------------------------------- */

const EXEMPLO = 'https://comercial.cnpj.ws/cnpj'

const schema = z.object({
  urlBase: z
    .string()
    .trim()
    .min(1, 'Informe a URL.')
    .max(200, 'A URL deve ter no máximo 200 caracteres.')
    .regex(/^https:\/\/\S+$/i, 'A URL deve começar com https://'),
  token: z.string().max(500).optional(),
  removerToken: z.boolean().optional(),
}).refine((v) => !(v.removerToken && v.token), { path: ['token'], message: 'Desmarque "Remover o token configurado" para informar um novo token.' })

type Valores = z.infer<typeof schema>

function layout(tokenConfigurado: boolean): FieldLayout {
  return [
    { section: 'Endpoint' },
    [{ name: 'urlBase', label: 'URL / endpoint', maxLength: 200, size: 'full', required: true, hint: `Exemplo: ${EXEMPLO}` }],
    { section: 'Token' },
    [
      {
        name: 'token',
        label: 'Token',
        kind: 'password',
        maxLength: 500,
        size: 'l',
        hint: tokenConfigurado ? 'Token configurado. Deixe em branco para manter.' : 'Cole o token do plano comercial do CNPJ.ws.',
      },
      ...(tokenConfigurado ? [{ name: 'removerToken', label: 'Remover o token configurado', kind: 'boolean' as const, size: 'l' as const }] : []),
    ],
  ]
}

/** CNPJ com os dois digitos verificadores (rejeita sequencias repetidas). */
function cnpjValido(digitos: string): boolean {
  if (!/^\d{14}$/.test(digitos) || /^(\d)\1{13}$/.test(digitos)) return false
  const dv = (n: number) => {
    let soma = 0
    let peso = n - 7
    for (let i = 0; i < n; i++) {
      soma += Number(digitos[i]) * peso--
      if (peso < 2) peso = 9
    }
    const r = soma % 11
    return r < 2 ? 0 : 11 - r
  }
  return dv(12) === Number(digitos[12]) && dv(13) === Number(digitos[13])
}

const MENSAGENS_TESTE: Record<string, string> = {
  cnpj_invalido: 'CNPJ inválido.',
  token_nao_configurado: 'Configure e salve o token para testar.',
  cnpj_nao_encontrado: 'CNPJ não encontrado no CNPJ.ws.',
  token_recusado: 'O CNPJ.ws recusou o token. Confira o token salvo.',
  limite_consulta_excedido: 'Limite de consultas do CNPJ.ws atingido. Aguarde e tente de novo.',
  muitas_tentativas: 'Muitas tentativas seguidas. Aguarde um pouco e tente de novo.',
  servico_cnpj_indisponivel: 'O CNPJ.ws não respondeu a tempo.',
  resposta_invalida: 'O CNPJ.ws devolveu uma resposta inesperada.',
}

function mensagemTeste(e: unknown): string {
  if (e instanceof ApiError) return MENSAGENS_TESTE[e.codigo] ?? e.message
  return 'Não foi possível consultar o CNPJ.'
}

const ND = '—'
const ou = (v: string | undefined | null): string => (v ? v : ND)
const simNao = (v: boolean | undefined): { texto: string; tone: BadgeTone } | undefined => (v === undefined ? undefined : { texto: v ? 'Sim' : 'Não', tone: v ? 'success' : 'neutral' })

interface LinhaResumo {
  rotulo: string
  texto?: string
  badge?: { texto: string; tone: BadgeTone }
}

/** Dados do resultado em rotulo/valor (so dados; a marcacao fica na tela). */
function linhasResumo(r: ResumoConsultaCnpj): LinhaResumo[] {
  const en = r.endereco
  const rua = en?.logradouro ? `${en.logradouro}${en.numero ? `, ${en.numero}` : ''}` : en?.numero
  const local = en?.municipio ? `${en.municipio}${en.uf ? `/${en.uf}` : ''}` : en?.uf
  const endereco = [rua, en?.complemento, en?.bairro, local, en?.cep ? maskCEP(en.cep) : undefined].filter(Boolean).join(' - ')
  const at = r.atividadePrincipal
  const atividade = [at?.codigo, at?.descricao].filter(Boolean).join(' - ')
  const capital = r.capitalSocial === undefined ? '' : formatCurrency(r.capitalSocial) || r.capitalSocial
  const sit = r.situacaoCadastral
  const sitTone: BadgeTone = sit?.trim().toLowerCase() === 'ativa' ? 'success' : 'warning'
  const simples = simNao(r.simples)
  const mei = simNao(r.mei)
  return [
    { rotulo: 'CNPJ', texto: ou(maskCNPJ(r.cnpj)) },
    { rotulo: 'Razão social', texto: ou(r.razaoSocial) },
    { rotulo: 'Nome fantasia', texto: ou(r.nomeFantasia) },
    sit ? { rotulo: 'Situação cadastral', badge: { texto: sit, tone: sitTone } } : { rotulo: 'Situação cadastral', texto: ND },
    { rotulo: 'Início da atividade', texto: ou(formatDate(r.dataInicioAtividade)) },
    { rotulo: 'Porte', texto: ou(r.porte) },
    { rotulo: 'Natureza jurídica', texto: ou(r.naturezaJuridica) },
    { rotulo: 'Atividade principal', texto: ou(atividade) },
    { rotulo: 'Capital social', texto: ou(capital) },
    { rotulo: 'Endereço', texto: ou(endereco) },
    { rotulo: 'E-mail', texto: ou(r.email) },
    { rotulo: 'Telefone', texto: ou(r.telefone ? maskPhone(r.telefone) : undefined) },
    simples ? { rotulo: 'Optante pelo Simples', badge: simples } : { rotulo: 'Optante pelo Simples', texto: ND },
    mei ? { rotulo: 'MEI', badge: mei } : { rotulo: 'MEI', texto: ND },
    { rotulo: 'Sócios', texto: r.quantidadeSocios === undefined ? ND : String(r.quantidadeSocios) },
  ]
}

/** O formulario de configuracao esta sujo? Le o DOM (o AutoForm nao expoe "sujo"): URL diferente da salva, token digitado ou remocao marcada. */
function formularioSujo(raiz: HTMLElement | null, urlSalva: string): boolean {
  if (!raiz) return false
  const url = raiz.querySelector<HTMLInputElement>('input:not([type=password]):not([type=checkbox])')
  const token = raiz.querySelector<HTMLInputElement>('input[type=password]')
  const remover = raiz.querySelector('[role=checkbox],[role=switch]')
  return (!!url && url.value.trim() !== urlSalva) || !!token?.value || remover?.getAttribute('aria-checked') === 'true'
}

function Formulario({ config, aoSalvar }: { config: ConfigConsultaCnpj; aoSalvar: () => void }) {
  const salvar = useSalvarConfigConsultaCnpj()
  const fields = useMemo(() => layout(config.tokenConfigurado), [config.tokenConfigurado])
  const values = useMemo<Valores>(() => ({ urlBase: config.urlBase, token: '', removerToken: false }), [config.urlBase])

  return (
    <AutoForm
      schema={schema}
      fields={fields}
      defaults={{ urlBase: '', token: '', removerToken: false }}
      values={values}
      clearAfterSubmit={['token', 'removerToken']}
      isSubmitting={salvar.isPending}
      serverError={salvar.error}
      submitLabel="Salvar"
      onSubmit={(v) => {
        const token = v.removerToken ? '' : v.token ? v.token : undefined
        return salvar.mutateAsync({ urlBase: v.urlBase, ...(token === undefined ? {} : { token }) }).then(
          () => {
            notifySuccess('Configuração salva.')
            aoSalvar()
          },
          () => {},
        )
      }}
    />
  )
}

function Conteudo() {
  const q = useConfigConsultaCnpj()
  const testar = useTestarConsultaCnpj()
  const raizForm = useRef<HTMLDivElement>(null)
  const seqConsulta = useRef(0)
  const [sujo, setSujo] = useState(false)
  const [resultado, setResultado] = useState<ResumoConsultaCnpj | null>(null)
  const [erroTeste, setErroTeste] = useState<string | null>(null)
  const urlSalva = q.data?.urlBase ?? ''
  const recalcular = () => setSujo(formularioSujo(raizForm.current, urlSalva))
  // Depois de salvar/recarregar a configuracao, o formulario volta ao valor salvo: refaz a leitura.
  useEffect(() => {
    const t = setTimeout(() => setSujo(formularioSujo(raizForm.current, urlSalva)), 0)
    return () => clearTimeout(t)
  // oxlint-disable-next-line react/exhaustive-effect-dependencies -- dataAtualizacao/tokenConfigurado so disparam a releitura
  }, [urlSalva, q.data?.dataAtualizacao, q.data?.tokenConfigurado])

  const consultar = (termo: string) => {
    if (testar.isPending) return
    const minha = ++seqConsulta.current
    setResultado(null)
    setErroTeste(null)
    if (!termo) return
    const digitos = onlyDigits(termo)
    if (!cnpjValido(digitos)) return setErroTeste('CNPJ inválido.')
    testar.mutate(digitos, {
      onSuccess: (r) => {
        if (minha === seqConsulta.current) setResultado(r)
      },
      onError: (e) => {
        if (minha === seqConsulta.current) setErroTeste(mensagemTeste(e))
      },
    })
  }

  if (q.isLoading) return <LoadingState label="Carregando configuração…" />
  if (q.isError || !q.data) return <ErrorState error={q.error} title="Não foi possível carregar a configuração." onRetry={() => void q.refetch()} />
  const c = q.data
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold">Consulta de CNPJ</h1>
        <p className="text-sm text-muted-foreground">Endereço e token usados pela plataforma para consultar CNPJ no CNPJ.ws.</p>
      </div>
      <Alert variant="info">
        <Info aria-hidden />
        <AlertDescription>
          A consulta usa o CNPJ.ws: GET {'{URL}'}/{'{CNPJ sem pontuação}'}, com o token enviado como parâmetro token. Exemplo de endpoint: {EXEMPLO}
        </AlertDescription>
      </Alert>
      <div className="flex items-center gap-2 text-sm">
        <span>Situação do token:</span>
        {c.tokenConfigurado ? <Badge tone="success">Token configurado</Badge> : <Badge tone="neutral">Sem token</Badge>}
      </div>
      <div ref={raizForm} onInput={recalcular} onClick={() => setTimeout(recalcular, 0)}>
        <Formulario config={c} aoSalvar={() => setTimeout(recalcular, 0)} />
      </div>
      {c.dataAtualizacao && (
        <p className="text-xs text-muted-foreground">
          Última atualização{c.atualizadoPor ? ` por ${c.atualizadoPor.nome}` : ''} em {formatDateTime(c.dataAtualizacao)}
        </p>
      )}
      <section aria-labelledby="testar-consulta" className="flex flex-col gap-3">
        <div>
          <h2 id="testar-consulta" className="text-lg font-semibold">
            Testar consulta
          </h2>
          <p className="text-sm text-muted-foreground">A consulta de teste usa a configuração já salva (URL e token), não o que está digitado no formulário acima.</p>
        </div>
        {!c.tokenConfigurado && (
          <Alert variant="warning">
            <TriangleAlert aria-hidden />
            <AlertDescription>Configure e salve o token para testar.</AlertDescription>
          </Alert>
        )}
        {c.tokenConfigurado && sujo && (
          <Alert variant="warning">
            <TriangleAlert aria-hidden />
            <AlertDescription>Há alterações não salvas: a consulta usa a configuração já salva.</AlertDescription>
          </Alert>
        )}
        <fieldset disabled={!c.tokenConfigurado || testar.isPending} className="m-0 min-w-0 border-0 p-0">
          <SearchInput variant="button" label="Consultar CNPJ" placeholder="00.000.000/0000-00" onSearch={consultar} className="sm:max-w-sm" />
        </fieldset>
        {testar.isPending && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Spinner />
            <span>Consultando no CNPJ.ws…</span>
          </div>
        )}
        {erroTeste && (
          <Alert variant="destructive">
            <AlertDescription>{erroTeste}</AlertDescription>
          </Alert>
        )}
        {resultado && (
          <Card>
            <CardHeader>
              <CardTitle>Resultado da consulta</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">
              <dl className="grid grid-cols-1 gap-x-6 gap-y-3 text-sm sm:grid-cols-[12rem_1fr]">
                {linhasResumo(resultado).map((l) => (
                  <div key={l.rotulo} className="contents">
                    <dt className="text-muted-foreground">{l.rotulo}</dt>
                    <dd className="m-0 break-words">{l.badge ? <Badge tone={l.badge.tone}>{l.badge.texto}</Badge> : l.texto}</dd>
                  </div>
                ))}
              </dl>
              <p className="text-xs text-muted-foreground">
                Consulta em {resultado.duracaoMs ?? ND} ms · {resultado.urlConsultada ?? ND}
              </p>
            </CardContent>
          </Card>
        )}
      </section>
    </div>
  )
}

export function ConsultaCnpjAdmin() {
  return (
    <PlatformOnly>
      <Conteudo />
    </PlatformOnly>
  )
}
