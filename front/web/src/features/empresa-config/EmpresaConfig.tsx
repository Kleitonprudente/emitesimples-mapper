import { useMemo, useRef, useState } from 'react'
import { Info, TriangleAlert } from 'lucide-react'
import { z } from 'zod'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge, type BadgeTone } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Spinner } from '@/components/ui/spinner'
import {
  useEmpresaConfig,
  useEnviarCertificado,
  useSalvarEmpresaConfig,
  type CertificadoSituacao,
  type CertificadoStatus,
  type EmpresaConfig as Empresa,
  type SalvarEmpresa,
} from '@/features/empresa-config/empresaApi'
import { regimeIbsCbsLabels, regimeIbsCbsValues, regimeTributarioLabels, regimeTributarioValues } from '@/gen/schemas/empresa'
import { fkSource } from '@/lib/fk-sources'
import { ApiError } from '@/lib/api'
import { canAct, useSession } from '@/lib/auth'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { notifySuccess } from '@/runtime/feedback'
import { formatDate, isValidCnpj, onlyDigits } from '@/runtime/format'
import { ErrorState, LoadingState } from '@/runtime/states'

/* ---------------------------------------------------------------------------
 * Configuracao > Empresa da EMPRESA da sessao (so administrador). Escrito a mao com o runtime: tela de configuracao
 * singleton, fora do padrao lista+formulario (decisao explicita do usuario). Duas partes: dados da empresa (AutoForm) e
 * certificado digital e-CNPJ A1 (upload multipart). O arquivo e a senha do certificado nunca ficam guardados no front.
 * ------------------------------------------------------------------------- */

const TAMANHO_MAXIMO = 256 * 1024
const SENHA_VALIDA = /^[A-Za-z0-9]{1,60}$/
const MSG_SENHA = 'A senha deve ter só letras e números, sem acentos.'
const MSG_GRANDE = 'Arquivo muito grande.'
const MSG_EXTENSAO = 'Envie um arquivo .pfx (ou .p12).'

/* ===================== Parte 1: dados ===================== */

const opcional = (max: number) => z.string().max(max).optional()

function criarSchema(documentoEditavel: boolean) {
  return z.object({
    razaoSocial: z.string().max(60),
    nomeFantasia: z.string().max(60),
    documento: documentoEditavel
      ? z.string().max(14).refine((v) => isValidCnpj(v), 'CNPJ inválido.')
      : z.string().max(14),
    inscricaoMunicipal: opcional(20),
    rgInscricaoEstadual: opcional(20),
    email: z.email('E-mail inválido.').max(60).optional(),
    whatsapp: opcional(20),
    telefone: opcional(20),
    cep: z.string().regex(/^[0-9]{8}$/, 'O CEP deve ter 8 dígitos.'),
    logradouro: z.string().max(60),
    numeroEndereco: z.string().max(60),
    complemento: opcional(60),
    bairro: z.string().max(60),
    municipio: z.looseObject({ id: z.string() }),
    regimeTributario: z.number().int().refine((v) => (regimeTributarioValues as readonly number[]).includes(v)),
    regimeIbsCbs: z.number().int().refine((v) => (regimeIbsCbsValues as readonly number[]).includes(v)).optional(),
  })
}

type Valores = z.infer<ReturnType<typeof criarSchema>>

const REGIME_TRIBUTARIO = regimeTributarioValues.map((value) => ({ value, label: regimeTributarioLabels[String(value)]! }))
const REGIME_IBS_CBS = regimeIbsCbsValues.map((value) => ({ value, label: regimeIbsCbsLabels[String(value)]! }))

function layout(c: Empresa): FieldLayout {
  return [
    { section: 'Identificação' },
    [
      { name: 'razaoSocial', label: 'Razão social', maxLength: 60, required: true, size: 'l' },
      { name: 'nomeFantasia', label: 'Nome fantasia', maxLength: 60, required: true, size: 'l' },
    ],
    [
      {
        name: 'documento',
        label: 'CNPJ',
        maxLength: 14,
        mask: 'documento',
        size: 'm',
        required: true,
        readOnly: !c.documentoEditavel,
        hint: c.documentoEditavel ? undefined : 'O CNPJ não pode ser alterado depois do cadastro na IntegraNotas.',
      },
      { name: 'inscricaoMunicipal', label: 'Inscrição municipal', maxLength: 20, size: 'm' },
      { name: 'rgInscricaoEstadual', label: 'Inscrição estadual', maxLength: 20, size: 'm' },
    ],
    { section: 'Contato' },
    [
      { name: 'email', label: 'E-mail', maxLength: 60, size: 'l' },
      { name: 'whatsapp', label: 'WhatsApp', maxLength: 20, mask: 'phone', size: 's' },
      { name: 'telefone', label: 'Telefone', maxLength: 20, mask: 'phone', size: 's' },
    ],
    { section: 'Endereço' },
    [
      { name: 'cep', label: 'CEP', maxLength: 8, mask: 'postalCode', size: 's', required: true, hint: 'Ao sair do campo, o endereço é preenchido.', lookup: 'cep' },
      { name: 'logradouro', label: 'Logradouro', maxLength: 60, required: true, size: 'l' },
      { name: 'numeroEndereco', label: 'Número do endereço', maxLength: 60, required: true, size: 's' },
    ],
    [
      { name: 'complemento', label: 'Complemento', maxLength: 60, size: 'm' },
      { name: 'bairro', label: 'Bairro', maxLength: 60, required: true, size: 'm' },
      { name: 'municipio', label: 'Município', fk: fkSource('Municipio'), required: true, size: 'm' },
    ],
    { section: 'Tributação' },
    [
      { name: 'regimeTributario', label: 'Regime tributário', options: REGIME_TRIBUTARIO, required: true, size: 'l' },
      { name: 'regimeIbsCbs', label: 'Regime do IBS e da CBS', options: REGIME_IBS_CBS, size: 'l' },
    ],
  ]
}

/** Valores do formulario a partir da leitura (nulo vira ausente; mascaras trabalham com digitos). */
function paraValores(c: Empresa): Valores {
  return {
    razaoSocial: c.razaoSocial,
    nomeFantasia: c.nomeFantasia,
    documento: onlyDigits(c.documento),
    inscricaoMunicipal: c.inscricaoMunicipal,
    rgInscricaoEstadual: c.rgInscricaoEstadual,
    email: c.email,
    whatsapp: c.whatsapp ? onlyDigits(c.whatsapp) : undefined,
    telefone: c.telefone ? onlyDigits(c.telefone) : undefined,
    cep: onlyDigits(c.cep),
    logradouro: c.logradouro,
    numeroEndereco: c.numeroEndereco,
    complemento: c.complemento,
    bairro: c.bairro,
    municipio: c.municipio as Valores['municipio'],
    regimeTributario: c.regimeTributario,
    regimeIbsCbs: c.regimeIbsCbs,
  }
}

function corpo(v: Valores, documentoEditavel: boolean): SalvarEmpresa {
  const vazioNulo = (s: string | undefined) => (s && s.trim() !== '' ? s.trim() : null)
  const body: SalvarEmpresa = {
    razaoSocial: v.razaoSocial.trim(),
    nomeFantasia: v.nomeFantasia.trim(),
    inscricaoMunicipal: vazioNulo(v.inscricaoMunicipal),
    rgInscricaoEstadual: vazioNulo(v.rgInscricaoEstadual),
    email: vazioNulo(v.email),
    whatsapp: vazioNulo(v.whatsapp),
    telefone: vazioNulo(v.telefone),
    cep: v.cep,
    logradouro: v.logradouro.trim(),
    numeroEndereco: v.numeroEndereco.trim(),
    complemento: vazioNulo(v.complemento),
    bairro: v.bairro.trim(),
    municipio: { id: v.municipio.id },
    regimeTributario: v.regimeTributario,
    regimeIbsCbs: v.regimeIbsCbs ?? null,
  }
  if (documentoEditavel) body.documento = onlyDigits(v.documento)
  return body
}

const CAMPOS: Record<string, string> = {
  razaoSocial: 'razão social',
  nomeFantasia: 'nome fantasia',
  documento: 'CNPJ',
  inscricaoMunicipal: 'inscrição municipal',
  rgInscricaoEstadual: 'inscrição estadual',
  email: 'e-mail',
  whatsapp: 'WhatsApp',
  telefone: 'telefone',
  cep: 'CEP',
  logradouro: 'logradouro',
  numeroEndereco: 'número do endereço',
  complemento: 'complemento',
  bairro: 'bairro',
  municipio: 'município',
  regimeTributario: 'regime tributário',
  regimeIbsCbs: 'regime do IBS e da CBS',
}

/** Erro do salvamento em linguagem amigavel; vai para o topo do formulario (serverError). */
function erroDeSalvar(e: unknown): unknown {
  if (!(e instanceof ApiError)) return e
  const com = (m: string) => new ApiError(e.status, e.codigo, m, e.detalhes, e.retryAfter)
  switch (e.codigo) {
    case 'documento_bloqueado':
      return com('O CNPJ não pode ser alterado depois do cadastro na IntegraNotas.')
    case 'documento_duplicado':
      return com('Já existe uma empresa cadastrada com este CNPJ.')
    case 'requisicao_invalida': {
      const campos = Array.isArray(e.detalhes?.campos) ? (e.detalhes.campos as { campo?: string; mensagem?: string }[]) : []
      const itens = campos.map((c) => {
        const nome = c.campo ? (CAMPOS[c.campo] ?? c.campo) : ''
        return nome && c.mensagem ? `${nome}: ${c.mensagem}` : (c.mensagem ?? nome)
      }).filter((t) => t !== '')
      return itens.length > 0 ? com(`Corrija os dados: ${itens.join('; ')}`) : e
    }
    default:
      return e
  }
}

function DadosEmpresa({ config }: { config: Empresa }) {
  const salvar = useSalvarEmpresaConfig()
  const schema = useMemo(() => criarSchema(config.documentoEditavel), [config.documentoEditavel])
  const fields = useMemo(() => layout(config), [config])
  // So os dados entram na chave: enviar o certificado (que muda `config.certificado`) nao apaga o que esta sendo editado.
  const dados = JSON.stringify([paraValores(config), config.documentoEditavel])
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const values = useMemo(() => paraValores(config), [dados])

  return (
    <AutoForm
      schema={schema}
      fields={fields}
      defaults={{}}
      values={values}
      isSubmitting={salvar.isPending}
      serverError={salvar.isError ? erroDeSalvar(salvar.error) : undefined}
      submitLabel="Salvar"
      onSubmit={(v) =>
        salvar.mutateAsync(corpo(v, config.documentoEditavel)).then(
          () => notifySuccess('Dados da empresa salvos.'),
          () => {},
        )
      }
    />
  )
}

/* ===================== Parte 2: certificado ===================== */

const SITUACAO: Record<CertificadoSituacao, { rotulo: string; tone: BadgeTone }> = {
  NAO_ENVIADO: { rotulo: 'Certificado não enviado', tone: 'neutral' },
  VALIDO: { rotulo: 'Certificado válido', tone: 'success' },
  VENCIDO: { rotulo: 'Certificado vencido', tone: 'danger' },
  INVALIDO: { rotulo: 'Certificado inválido', tone: 'danger' },
  NAO_PERTENCE: { rotulo: 'Certificado de outro CNPJ', tone: 'danger' },
}

const AVISOS: Record<string, string> = {
  nao_foi_possivel_conferir_cnpj: 'Não foi possível conferir o CNPJ do certificado.',
}

/** Mensagem amigavel por codigo de erro; codigo desconhecido cai na mensagem do back. */
function mensagemCertificado(e: unknown): string {
  if (!(e instanceof ApiError)) return 'Algo deu errado. Tente novamente em instantes.'
  switch (e.codigo) {
    case 'certificado_senha_invalida':
      return MSG_SENHA
    case 'certificado_senha_incorreta':
      return 'A senha do certificado está incorreta.'
    case 'certificado_invalido':
      return 'O arquivo não é um certificado válido (.pfx).'
    case 'certificado_vencido':
      return 'O certificado está vencido.'
    case 'certificado_nao_pertence':
      return 'O certificado pertence a outro CNPJ.'
    case 'certificado_invalido_gateway':
      return 'A IntegraNotas não conseguiu abrir o certificado (arquivo corrompido ou senha incorreta).'
    case 'arquivo_grande':
      return MSG_GRANDE
    case 'muitas_tentativas':
      return 'Muitas tentativas. Aguarde um pouco.'
    case 'integranotas_token_recusado':
      return 'A IntegraNotas recusou o token da integração. Fale com o suporte.'
    case 'integranotas_indisponivel':
      return 'A IntegraNotas não respondeu a tempo. Tente de novo.'
    case 'integranotas_resposta_invalida':
      return 'A IntegraNotas devolveu uma resposta inesperada. Tente de novo.'
    default:
      return e.mensagem
  }
}

function StatusCertificado({ c }: { c: CertificadoStatus }) {
  const s = SITUACAO[c.situacao]
  const vence = c.situacao !== 'VENCIDO' && c.venceEmDias !== undefined && c.venceEmDias >= 0 && c.venceEmDias <= 30 ? c.venceEmDias : undefined
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <Badge tone={s.tone}>{s.rotulo}</Badge>
        {c.validade && <Badge tone="outline">{`${c.situacao === 'VENCIDO' ? 'Venceu em' : 'Válido até'} ${formatDate(c.validade)}`}</Badge>}
        {c.pendenteDeEnvio && <Badge tone="warning">Aguardando habilitação da NFS-e</Badge>}
      </div>
      {c.pendenteDeEnvio && (
        <p className="text-sm text-muted-foreground">O certificado está guardado e será enviado à IntegraNotas quando você habilitar a emissão em Configuração &gt; NFS-e.</p>
      )}
      {vence !== undefined && (
        <Alert variant="warning">
          <TriangleAlert aria-hidden />
          <AlertDescription>{vence === 0 ? 'O certificado vence hoje.' : `O certificado vence em ${vence} ${vence === 1 ? 'dia' : 'dias'}.`}</AlertDescription>
        </Alert>
      )}
    </div>
  )
}

function CertificadoDigital({ certificado }: { certificado: CertificadoStatus }) {
  const enviar = useEnviarCertificado()
  const inputRef = useRef<HTMLInputElement>(null)
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [senha, setSenha] = useState('')
  const [erroArquivo, setErroArquivo] = useState<string | null>(null)
  const [erroSenha, setErroSenha] = useState<string | null>(null)
  const [avisos, setAvisos] = useState<string[]>([])

  const limparArquivo = () => {
    setArquivo(null)
    if (inputRef.current) inputRef.current.value = ''
  }

  function escolher(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0] ?? null
    setErroArquivo(null)
    if (f && !/\.(pfx|p12)$/i.test(f.name)) {
      setErroArquivo(MSG_EXTENSAO)
      limparArquivo()
    } else if (f && f.size > TAMANHO_MAXIMO) {
      setErroArquivo(MSG_GRANDE)
      limparArquivo()
    } else setArquivo(f)
  }

  function submeter(e: React.FormEvent) {
    e.preventDefault()
    if (!arquivo || senha === '' || enviar.isPending) return
    if (!SENHA_VALIDA.test(senha)) {
      setErroSenha(MSG_SENHA)
      return
    }
    setErroSenha(null)
    setAvisos([])
    const arq = arquivo
    const entrada = { arquivo: arq, senha }
    setSenha('') // a senha nunca fica no estado depois de enviada (se errar, digita de novo)
    enviar.mutate(entrada, {
      onSuccess: (r) => {
        limparArquivo()
        setAvisos(r.avisos.filter((a) => a in AVISOS))
        notifySuccess(r.pendenteDeEnvio ? 'Certificado guardado. Será enviado quando a NFS-e for habilitada.' : 'Certificado enviado.')
      },
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle role="heading" aria-level={2}>
          Certificado digital e-CNPJ A1
        </CardTitle>
        <CardDescription>Envie o certificado digital e-CNPJ A1 (arquivo .pfx). A senha deve ter só letras e números, sem acentos, até 60 caracteres.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <StatusCertificado c={certificado} />
        {avisos.map((a) => (
          <Alert key={a} variant="info">
            <Info aria-hidden />
            <AlertDescription>{AVISOS[a]}</AlertDescription>
          </Alert>
        ))}
        {enviar.isError && (
          <Alert variant="destructive">
            <TriangleAlert aria-hidden />
            <AlertDescription>{mensagemCertificado(enviar.error)}</AlertDescription>
          </Alert>
        )}
        <form onSubmit={submeter} noValidate className="grid grid-cols-12 gap-4" aria-busy={enviar.isPending || undefined}>
          <div className="col-span-12 flex flex-col gap-1.5 md:col-span-6">
            <Label htmlFor="certificado-arquivo">Arquivo do certificado (.pfx)</Label>
            <Input
              id="certificado-arquivo"
              ref={inputRef}
              type="file"
              accept=".pfx,.p12"
              disabled={enviar.isPending}
              aria-invalid={erroArquivo ? true : undefined}
              aria-describedby={erroArquivo ? 'certificado-arquivo-err' : undefined}
              onChange={escolher}
            />
            {erroArquivo && (
              <p id="certificado-arquivo-err" role="alert" className="text-xs text-destructive">
                {erroArquivo}
              </p>
            )}
          </div>
          <div className="col-span-12 flex flex-col gap-1.5 md:col-span-6">
            <Label htmlFor="certificado-senha">Senha do certificado</Label>
            <Input
              id="certificado-senha"
              type="password"
              autoComplete="off"
              maxLength={60}
              value={senha}
              disabled={enviar.isPending}
              aria-invalid={erroSenha ? true : undefined}
              aria-describedby={erroSenha ? 'certificado-senha-err' : undefined}
              onChange={(e) => {
                setSenha(e.target.value)
                setErroSenha(null)
              }}
            />
            {erroSenha && (
              <p id="certificado-senha-err" role="alert" className="text-xs text-destructive">
                {erroSenha}
              </p>
            )}
          </div>
          <div className="col-span-12 flex items-center gap-3">
            <Button type="submit" disabled={!arquivo || senha === '' || enviar.isPending}>
              Enviar certificado
            </Button>
            {enviar.isPending && (
              <p role="status" className="flex items-center gap-2 text-sm text-muted-foreground">
                <Spinner /> Enviando…
              </p>
            )}
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

/* ===================== tela ===================== */

function Conteudo() {
  const q = useEmpresaConfig()
  if (q.isLoading) return <LoadingState label="Carregando dados da empresa…" />
  if (q.isError || !q.data) return <ErrorState error={q.error} title="Não foi possível carregar os dados da empresa." onRetry={() => void q.refetch()} />
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold">Empresa</h1>
        <p className="text-sm text-muted-foreground">Dados cadastrais e certificado digital usados na emissão de NFS-e.</p>
      </div>
      <DadosEmpresa config={q.data} />
      <CertificadoDigital certificado={q.data.certificado} />
    </div>
  )
}

export function EmpresaConfig() {
  const session = useSession()
  // A tela e da empresa da sessao: administrador da plataforma sem empresa (sem papel) tambem fica de fora. So UX: o back decide.
  if (!canAct(session, 'ADMINISTRADOR')) return <ErrorState title="Acesso restrito ao administrador da empresa." />
  return <Conteudo />
}
