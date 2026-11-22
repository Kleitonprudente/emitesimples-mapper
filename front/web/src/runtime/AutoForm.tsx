import { Fragment, useCallback, useEffect, useId, useMemo, useRef, useState, type CSSProperties, type ReactNode } from 'react'
import { useFieldArray, useForm, useFormState, type Control, type DefaultValues, type FieldValues, type Path, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2, Plus, Trash2 } from 'lucide-react'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { cn } from '@/lib/utils'
import { ApiError, api } from '@/lib/api'
import { errorMessage } from '@/runtime/feedback'
import { FormField, SIZE_CLASS, SIZE_COLS, resolveKind, resolveSize, type FieldSpec, type LookupState } from '@/runtime/FormField'
import { isValidCnpj, isValidCpf, onlyDigits } from '@/runtime/format'

// Mensagens de validacao do zod em portugues.
z.config(z.locales.pt())

/** Linha: um campo sozinho ou um array de campos na mesma linha (grade de 12 colunas, ver `FieldSize`). */
export type FieldRow = FieldSpec | FieldSpec[]

/** Cabecalho de secao: as linhas que o seguem, ate a proxima secao, pertencem a ela. */
export interface FieldSection {
  section: string
  description?: string
  /** Secao rara: recolhida por padrao (abre sozinha se tiver valor ou erro). */
  collapsible?: boolean
  defaultOpen?: boolean
}

/** Layout: linhas e, opcionalmente, cabecalhos de secao. Sem secao, renderiza como um formulario simples. */
export type FieldLayout = (FieldRow | FieldSection)[]

const isSection = (i: FieldRow | FieldSection): i is FieldSection => !Array.isArray(i) && 'section' in i

interface Group {
  key: string
  section?: FieldSection
  rows: FieldRow[]
}

/** Agrupa as linhas por secao; linhas antes da primeira secao formam um grupo sem titulo. */
function groupLayout(layout: FieldLayout): Group[] {
  const groups: Group[] = []
  for (const item of layout) {
    if (isSection(item)) groups.push({ key: `s${groups.length}`, section: item, rows: [] })
    else {
      if (groups.length === 0) groups.push({ key: 'g0', rows: [] })
      groups[groups.length - 1].rows.push(item)
    }
  }
  return groups
}

const cellsOf = (row: FieldRow): FieldSpec[] => (Array.isArray(row) ? row : [row])

/** Linha na grade de 12 colunas, ou no layout flex antigo (so `width`, ou formulario simples sem secoes). */
function isGridRow(cells: FieldSpec[], hasSections: boolean): boolean {
  if (cells.some((c) => c.size)) return true
  return hasSections && !cells.some((c) => c.width !== undefined)
}

const MAX_SECTIONS_STACKED = 5

export interface AutoFormProps<T extends FieldValues> {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  schema: z.ZodType<T, any>
  fields: FieldLayout
  /** Valores iniciais de criacao. */
  defaults: DefaultValues<T>
  /** Se informado, o formulario esta em modo edicao e e preenchido (e re-preenchido) com estes valores. */
  values?: T
  onSubmit: (values: T) => void | Promise<void>
  isSubmitting?: boolean
  submitLabel?: string
  onCancel?: () => void
  /** Erro devolvido pela mutation (ApiError): aparece acima dos botoes. */
  serverError?: unknown
  /** Campos que voltam ao valor inicial logo depois do envio (com sucesso ou nao). Uso: senha nunca fica no formulario. */
  clearAfterSubmit?: string[]
  /** Avisa de cada mudanca de valor (name = campo alterado; indefinido em reset). setValue altera outro campo (ex.: preencher a descricao ao escolher o servico). */
  onValuesChange?: (values: Partial<T>, ctx: { name?: string; setValue: (name: string, value: unknown) => void }) => void
}

/** Resposta de GET /cep/{cep} (o back consulta o ViaCEP). */
export interface CepResult {
  cep?: string
  logradouro?: string
  complemento?: string
  bairro?: string
  uf?: string
  codigoIbge?: string
  municipio?: { id: string; nome?: string; uf?: string; codigoIbge?: string } | null
}

/** Resposta de GET /consulta-cnpj/{cnpj} (o back consulta o CNPJ.ws). Todo campo pode faltar. */
export interface CnpjResult {
  cnpj?: string
  razaoSocial?: string | null
  nomeFantasia?: string | null
  email?: string | null
  telefone?: string | null
  cep?: string | null
  logradouro?: string | null
  numeroEndereco?: string | null
  complemento?: string | null
  bairro?: string | null
  municipio?: { id: string; nome?: string; uf?: string; codigoIbge?: string } | null
  uf?: string | null
  codigoIbge?: string | null
  regimeTributario?: number | null
  inscricaoEstadual?: string | null
  situacaoCadastral?: string | null
  dataInicioAtividade?: string | null
  /** Quadro societario (QSA). Com um campo de lista `contatos` irmao, cada socio vira um contato do tipo 'socio'. */
  socios?: { nome?: string | null; tipo?: string | null; qualificacao?: string | null }[] | null
}

export const AVISO_CNPJ = 'Dados preenchidos pelo CNPJ.ws. Confira antes de salvar.'
export const AVISO_CPF = 'CPF informado: pessoa física e consumidor final preenchidos.'

/** Mensagens amigaveis por codigo de erro da consulta de CNPJ; sem codigo conhecido, vale a mensagem do back. */
const ERRO_CNPJ: Record<string, string> = {
  cnpj_nao_encontrado: 'CNPJ não encontrado no CNPJ.ws.',
  token_nao_configurado: 'Configure o token do CNPJ.ws em Administração > Consulta de CNPJ.',
  token_recusado: 'O CNPJ.ws recusou o token. Revise-o em Administração > Consulta de CNPJ.',
  limite_consulta_excedido: 'Limite de consultas do CNPJ.ws excedido. Tente novamente mais tarde.',
  servico_cnpj_indisponivel: 'Serviço de consulta de CNPJ indisponível. Tente novamente em instantes.',
}

function cnpjErrorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.codigo === 'muitas_tentativas' && !e.retryAfter) return 'Muitas consultas seguidas. Aguarde um instante e tente novamente.'
    if (e.codigo !== 'muitas_tentativas' && ERRO_CNPJ[e.codigo]) return ERRO_CNPJ[e.codigo]!
  }
  return errorMessage(e)
}

interface LookupUi {
  loading: boolean
  error?: string
  notice?: string
}

export const AVISO_MUNICIPIO = 'Município não encontrado na tabela; escolha manualmente'

const flatten = (layout: FieldLayout): FieldSpec[] => layout.flatMap((r) => (isSection(r) ? [] : cellsOf(r)))

const isFilled = (v: unknown): boolean => (Array.isArray(v) ? v.length > 0 : v !== undefined && v !== null && v !== '' && v !== false)

const isPlainObject = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null && !Array.isArray(v) && Object.getPrototypeOf(v) === Object.prototype

/** Campo vazio (ou FK limpa pelo X do Combobox, que vira null) vira "ausente" (undefined), para o zod tratar obrigatorio x opcional. Arrays de objetos (listas) limpam cada item. */
function clean(values: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(values).map(([k, v]) => [k, v === '' || v === null ? undefined : Array.isArray(v) ? v.map((x) => (isPlainObject(x) ? clean(x) : x)) : v]),
  )
}

/** Remove as chaves ausentes e converte FK em `{id}` num item de lista. */
function itemForBack(item: Record<string, unknown>, fkItemNames: string[]): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(clean(item))) {
    if (v === undefined) continue
    out[k] = fkItemNames.includes(k) ? ((v as { id?: string }).id ? { id: (v as { id: string }).id } : undefined) : v
  }
  return out
}

/** Caminho (`contatos.1.nome`) do primeiro erro de validacao dentro de uma arvore de erros do react-hook-form. */
function firstErrorPath(err: unknown, path: string): string | undefined {
  if (!err || typeof err !== 'object') return undefined
  const e = err as Record<string, unknown>
  if (typeof e.type === 'string' && 'ref' in e) return path
  for (const [k, v] of Object.entries(e)) {
    if (k === 'ref' || k === 'root') continue
    const found = firstErrorPath(v, `${path}.${k}`)
    if (found) return found
  }
  return undefined
}

/** Compara nomes sem acento nem caixa (socios do QSA x contatos ja preenchidos). */
const normalizeName = (s: string): string => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim().replace(/\s+/g, ' ')

/**
 * Campo de lista (`kind: 'list'`): um bloco por item com titulo "{itemLabel} {n}", botao de remover so com icone e botao de adicionar.
 * `renderItem` desenha as linhas do item (com os nomes ja prefixados `lista.i.`).
 */
function ListBlock<T extends FieldValues>({ spec, control, disabled, renderItem }: { spec: FieldSpec; control: Control<T>; disabled?: boolean; renderItem: (index: number) => ReactNode }) {
  const cfg = spec.list!
  // keyName proprio: o `id` do item e o do registro no back e nao pode ser sobrescrito pela chave de renderizacao.
  const { fields, append, remove } = useFieldArray({ control, name: spec.name as never, keyName: 'rowKey' as never })
  const { errors } = useFormState({ control, name: spec.name as never })
  const listError = (errors as Record<string, { message?: string; root?: { message?: string } } | undefined>)[spec.name]
  const rootMessage = listError?.root?.message ?? (typeof listError?.message === 'string' ? listError.message : undefined)
  const atMax = cfg.maxItems !== undefined && fields.length >= cfg.maxItems
  const emptyText = cfg.emptyText ?? `Nenhum ${cfg.itemLabel.toLowerCase()} adicionado.`
  return (
    <div role="group" aria-label={spec.label} className="flex flex-col gap-3">
      {fields.length === 0 && <p className="text-sm text-muted-foreground">{emptyText}</p>}
      {fields.map((f, i) => {
        const title = `${cfg.itemLabel} ${i + 1}`
        return (
          <Card key={(f as unknown as { rowKey: string }).rowKey} role="group" aria-label={title} className="gap-3 py-4">
            <CardHeader className="flex flex-row items-center justify-between gap-2">
              <CardTitle className="text-sm">{title}</CardTitle>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button type="button" variant="ghost" size="icon" aria-label={`Remover ${title}`} disabled={disabled} onClick={() => remove(i)}>
                    <Trash2 aria-hidden />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Remover</TooltipContent>
              </Tooltip>
            </CardHeader>
            <CardContent className="flex flex-col gap-4">{renderItem(i)}</CardContent>
          </Card>
        )
      })}
      {rootMessage && (
        <p role="alert" className="text-xs text-destructive">
          {rootMessage}
        </p>
      )}
      <div>
        <Button type="button" variant="outline" disabled={disabled || atMax} onClick={() => append({ ...cfg.defaultItem } as never)}>
          <Plus aria-hidden />
          {cfg.addLabel}
        </Button>
      </div>
    </div>
  )
}

export function AutoForm<T extends FieldValues>({
  schema,
  fields,
  defaults,
  values,
  onSubmit,
  isSubmitting,
  submitLabel,
  onCancel,
  serverError,
  clearAfterSubmit,
  onValuesChange,
}: AutoFormProps<T>) {
  const specs = useMemo(() => flatten(fields), [fields])
  const fkNames = useMemo(() => specs.filter((s) => resolveKind(s) === 'fk').map((s) => s.name), [specs])
  const listSpecs = useMemo(() => specs.filter((s) => resolveKind(s) === 'list' && s.list), [specs])
  const listNames = useMemo(() => listSpecs.map((s) => s.name), [listSpecs])
  const isEdit = values !== undefined

  const resolver = useMemo<Resolver<T>>(() => {
    const base = zodResolver(schema as never) as unknown as Resolver<T>
    return async (vals, ctx, opts) => {
      const cleaned = clean(vals as Record<string, unknown>)
      const result = await base(cleaned as T, ctx, opts)
      // "recebido undefined" e a forma tecnica de dizer "campo obrigatorio".
      for (const [name, err] of Object.entries(result.errors as Record<string, { type?: string; message?: string } | undefined>)) {
        if (err && err.type === 'invalid_type' && cleaned[name] === undefined) err.message = 'Campo obrigatório'
      }
      // Itens de lista: idem, por campo de cada item.
      for (const n of listNames) {
        const itemErrs = (result.errors as Record<string, unknown>)[n]
        const items = cleaned[n]
        if (!Array.isArray(itemErrs) || !Array.isArray(items)) continue
        itemErrs.forEach((ie, idx) => {
          if (!ie || typeof ie !== 'object') return
          for (const [field, fe] of Object.entries(ie as Record<string, { type?: string; message?: string } | undefined>)) {
            if (fe && fe.type === 'invalid_type' && (items[idx] as Record<string, unknown> | undefined)?.[field] === undefined) fe.message = 'Campo obrigatório'
          }
        })
      }
      return result
    }
  }, [schema, listNames])

  const form = useForm<T>({
    resolver,
    defaultValues: defaults,
    values,
    mode: 'onTouched',
  })

  const onValuesChangeRef = useRef(onValuesChange)
  useEffect(() => {
    onValuesChangeRef.current = onValuesChange
  })
  useEffect(() => {
    const sub = form.watch((vals, info) => onValuesChangeRef.current?.(vals as Partial<T>, { name: info.name, setValue: (n, v) => form.setValue(n as Path<T>, v as never, { shouldDirty: true, shouldValidate: true }) }))
    return () => sub.unsubscribe()
  }, [form])

  /* ----- lookup de CEP: ao sair do campo com 8 digitos, preenche os irmaos por convencao de nome ----- */
  const names = useMemo(() => new Set(specs.map((s) => s.name)), [specs])
  // Campo FK de municipio irmao: o que aponta para a fonte 'Municipio' (`cidade` em Pessoa, `municipio` em Empresa);
  // sem fk declarado, cai no nome `cidade` por compatibilidade.
  const municipioName = useMemo(
    () => specs.find((s) => s.fk?.key === 'Municipio')?.name ?? (names.has('cidade') ? 'cidade' : undefined),
    [specs, names],
  )
  const [lookups, setLookups] = useState<Record<string, LookupUi>>({})
  const seqRef = useRef<Record<string, number>>({})
  const focusRef = useRef<Record<string, string>>({})
  const erroredRef = useRef<Set<string>>(new Set())
  const setLookup = useCallback((name: string, ui: LookupUi) => setLookups((s) => ({ ...s, [name]: ui })), [])

  /** Preenche os campos irmaos e devolve o aviso (municipio fora da tabela), se houver. */
  const applyCep = useCallback(
    (r: CepResult): string | undefined => {
      const opts = { shouldDirty: true, shouldValidate: true }
      const put = (n: string, v: unknown) => names.has(n) && form.setValue(n as Path<T>, v as never, opts)
      // Campo vazio vindo do back nao apaga o que a pessoa digitou.
      if (r.logradouro) put('logradouro', r.logradouro)
      if (r.bairro) put('bairro', r.bairro)
      if (r.complemento && !form.getValues('complemento' as Path<T>)) put('complemento', r.complemento)
      if (municipioName) {
        if (r.municipio?.id) put(municipioName, r.municipio)
        else return AVISO_MUNICIPIO
      }
      return undefined
    },
    [form, names, municipioName],
  )

  /** Preenche os campos irmaos com o resultado da consulta de CNPJ; devolve o aviso exibido sob o campo. */
  const applyCnpj = useCallback(
    (r: CnpjResult): string | undefined => {
      const opts = { shouldDirty: true, shouldValidate: true }
      const put = (n: string, v: unknown) => names.has(n) && form.setValue(n as Path<T>, v as never, opts)
      // Valor ausente/vazio da resposta nao apaga o digitado; valor presente sobrescreve.
      const text = (n: string, v: string | null | undefined) => {
        if (v) put(n, v)
      }
      // Sem campo `razaoSocial` no layout (ex.: Cliente/Fornecedor), a razao social e o fantasia vao para `nome`/`nomeFantasia`.
      text(names.has('razaoSocial') ? 'razaoSocial' : 'nome', r.razaoSocial)
      text('nomeFantasia', r.nomeFantasia)
      // Convencao: CNPJ consultado com sucesso e pessoa juridica, nao consumidor final.
      put('tipoPessoa', 'juridica')
      put('consumidorFinal', false)
      text('email', r.email)
      text('telefone', onlyDigits(r.telefone))
      text('cep', onlyDigits(r.cep))
      text('logradouro', r.logradouro)
      text('numeroEndereco', r.numeroEndereco)
      text('bairro', r.bairro)
      text('rgInscricaoEstadual', r.inscricaoEstadual)
      if (r.complemento && !form.getValues('complemento' as Path<T>)) put('complemento', r.complemento)
      if (r.regimeTributario !== null && r.regimeTributario !== undefined) put('regimeTributario', r.regimeTributario)
      let semMunicipio = false
      if (municipioName) {
        if (r.municipio?.id) put(municipioName, r.municipio)
        else semMunicipio = true
      }
      // Socios do QSA viram contatos do tipo 'socio' (sem duplicar nome ja presente; nunca apaga o que a pessoa preencheu).
      let socios = 0
      const contatos = listSpecs.find((s) => s.name === 'contatos')
      if (contatos?.list && r.socios?.length && flatten(contatos.list.item).some((s) => s.name === 'tipoContato')) {
        const atuais = (form.getValues('contatos' as Path<T>) as Record<string, unknown>[] | undefined) ?? []
        const vistos = new Set(atuais.filter((c) => c?.tipoContato === 'socio').map((c) => normalizeName(String(c.nome ?? ''))))
        const novos: Record<string, unknown>[] = []
        for (const s of r.socios) {
          const nome = s.nome?.trim()
          if (!nome || vistos.has(normalizeName(nome))) continue
          vistos.add(normalizeName(nome))
          novos.push({ ...contatos.list.defaultItem, tipoContato: 'socio', nome })
        }
        if (novos.length) {
          form.setValue('contatos' as Path<T>, [...atuais, ...novos] as never, { shouldDirty: true })
          socios = novos.length
        }
      }
      return [AVISO_CNPJ, semMunicipio ? AVISO_MUNICIPIO : '', socios ? `${socios} sócio(s) adicionado(s) em Contatos.` : ''].filter(Boolean).join(' ')
    },
    [form, names, municipioName, listSpecs],
  )

  /** CPF valido: pessoa fisica e consumidor final, sem API. Devolve o aviso, ou undefined se o layout nao tem os campos irmaos. */
  const applyCpf = useCallback((): string | undefined => {
    if (!names.has('tipoPessoa') && !names.has('consumidorFinal')) return undefined
    const opts = { shouldDirty: true, shouldValidate: true }
    if (names.has('tipoPessoa')) form.setValue('tipoPessoa' as Path<T>, 'fisica' as never, opts)
    if (names.has('consumidorFinal')) form.setValue('consumidorFinal' as Path<T>, true as never, opts)
    return AVISO_CPF
  }, [form, names])

  const lookupFor = (spec: FieldSpec): LookupState | undefined => {
    const kind = spec.lookup
    if (kind !== 'cep' && kind !== 'cnpj') return undefined
    const name = spec.name
    const digitsNow = () => onlyDigits(String(form.getValues(name as Path<T>) ?? ''))
    const tamanho = kind === 'cep' ? 8 : 14
    // forcar=false (sair do campo): so consulta documento completo e diferente do que havia ao entrar. forcar=true (botao ou Enter): consulta sempre.
    const buscar = (forcar: boolean) => {
      const digits = digitsNow()
      if (kind === 'cnpj' && digits.length === 11) {
        // CPF num campo de documento: nao ha consulta; se valido, so aplica a convencao de tipo.
        if (!isValidCpf(digits)) {
          if (forcar) setLookup(name, { loading: false, error: 'CPF inválido.' })
          return
        }
        if (!forcar && digits === focusRef.current[name]) return
        seqRef.current[name] = (seqRef.current[name] ?? 0) + 1 // descarta consulta de CNPJ anterior em andamento
        erroredRef.current.delete(name)
        setLookup(name, { loading: false, notice: applyCpf() })
        return
      }
      if (digits.length !== tamanho) {
        if (forcar) setLookup(name, { loading: false, error: kind === 'cep' ? 'Informe os 8 dígitos do CEP.' : 'Informe os 14 dígitos do CNPJ.' })
        return
      }
      if (kind === 'cnpj' && !isValidCnpj(digits)) {
        if (forcar) setLookup(name, { loading: false, error: 'CNPJ inválido.' })
        return
      }
      if (!forcar && digits === focusRef.current[name] && !erroredRef.current.has(name)) return
      const n = (seqRef.current[name] = (seqRef.current[name] ?? 0) + 1)
      erroredRef.current.delete(name)
      setLookup(name, { loading: true })
      const request: Promise<{ notice?: string }> =
        kind === 'cep'
          ? api<CepResult>(`/cep/${digits}`).then((r) => ({ notice: applyCep(r) }))
          : api<CnpjResult>(`/consulta-cnpj/${digits}`).then((r) => ({ notice: applyCnpj(r) }))
      request.then(
        ({ notice }) => {
          if (seqRef.current[name] !== n) return // resposta de uma busca antiga
          setLookup(name, { loading: false, notice })
        },
        (e: unknown) => {
          if (seqRef.current[name] !== n) return
          erroredRef.current.add(name)
          setLookup(name, { loading: false, error: kind === 'cep' ? errorMessage(e) : cnpjErrorMessage(e) })
        },
      )
    }
    return {
      kind,
      loading: !!lookups[name]?.loading,
      error: lookups[name]?.error,
      notice: lookups[name]?.notice,
      onFocus: () => {
        focusRef.current[name] = digitsNow()
        // Ao voltar ao campo some a mensagem anterior; `erroredRef` lembra que a ultima busca falhou (deixa repetir).
        setLookups((s) => (s[name]?.error || s[name]?.notice ? { ...s, [name]: { loading: false } } : s))
      },
      onBlur: () => buscar(false),
      onSearch: () => buscar(true),
    }
  }

  const groups = useMemo(() => groupLayout(fields), [fields])
  const sections = useMemo(() => groups.filter((g) => g.section), [groups])
  const hasSections = sections.length > 0
  const asTabs = sections.length > MAX_SECTIONS_STACKED
  const [tab, setTab] = useState<string | undefined>()
  const [manualOpen, setManualOpen] = useState<Record<string, boolean>>({})
  const uid = useId()

  const namesOf = (g: Group): string[] => g.rows.flatMap(cellsOf).map((s) => s.name)
  const errorsNow = form.formState.errors as Record<string, unknown>
  const groupHasError = (g: Group) => namesOf(g).some((n) => !!errorsNow[n])

  // Aviso em desenvolvimento: linha da grade que nao soma 12 colunas (menos que 12 e aceito; mais quebra linha).
  useEffect(() => {
    if (!import.meta.env.DEV) return
    for (const item of fields) {
      if (isSection(item)) continue
      const cells = cellsOf(item)
      if (!isGridRow(cells, hasSections)) continue
      const sum = cells.reduce((acc, c) => acc + SIZE_COLS[resolveSize(c)], 0)
      if (sum !== 12) console.warn(`AutoForm: a linha [${cells.map((c) => c.name).join(', ')}] soma ${sum} colunas (o esperado e 12).`)
    }
  }, [fields, hasSections])

  // Alteracoes nao salvas: avisa o navegador ao fechar/recarregar enquanto o formulario esta sujo.
  const isDirty = form.formState.isDirty
  useEffect(() => {
    if (!isDirty) return
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [isDirty])

  const submit = form.handleSubmit(
    async (parsed) => {
      // FK: o back recebe SO {id}.
      const out = { ...parsed } as Record<string, unknown>
      for (const n of fkNames) {
        const v = out[n] as { id?: string } | null | undefined
        out[n] = v?.id ? { id: v.id } : undefined
      }
      // Listas: itens limpos, FK como {id} e o `id` do registro preservado mesmo sem campo visivel (o zod pode te-lo removido).
      for (const s of listSpecs) {
        const fkItemNames = flatten(s.list!.item).filter((f) => resolveKind(f) === 'fk').map((f) => f.name)
        const atuais = (form.getValues(s.name as Path<T>) as Record<string, unknown>[] | undefined) ?? []
        const items = (out[s.name] as Record<string, unknown>[] | undefined) ?? []
        out[s.name] = items.map((it, i) => {
          const id = atuais[i]?.id
          return itemForBack(typeof id === 'string' && id ? { ...it, id } : it, fkItemNames)
        })
      }
      try {
        await onSubmit(out as T)
        // Salvou: o estado atual vira a nova referencia (some o aviso de alteracoes nao salvas e o beforeunload).
        const next = form.getValues() as Record<string, unknown>
        const initial = (form.formState.defaultValues ?? {}) as Record<string, unknown>
        for (const n of clearAfterSubmit ?? []) next[n] = initial[n]
        form.reset(next as DefaultValues<T>)
      } finally {
        for (const n of clearAfterSubmit ?? []) form.resetField(n as Path<T>)
      }
    },
    (errs) => {
      // Abas: sobe a primeira que tem campo invalido e leva o foco ate o campo.
      const bad = errs as Record<string, unknown>
      const first = sections.find((g) => namesOf(g).some((n) => !!bad[n]))
      // Secoes recolhidas que contem erro abrem (mesmo que a pessoa as tenha fechado antes).
      const comErro = sections.filter((g) => g.section?.collapsible && namesOf(g).some((n) => !!bad[n]))
      if (comErro.length) setManualOpen((m) => ({ ...m, ...Object.fromEntries(comErro.map((g) => [g.key, true])) }))
      const name = first ? namesOf(first).find((n) => !!bad[n]) : undefined
      if (first && name) {
        if (asTabs) setTab(first.key)
        const target = listNames.includes(name) ? (firstErrorPath(bad[name], name) ?? name) : name
        setTimeout(() => form.setFocus(target as Path<T>), 0)
      } else if (!first) {
        // Sem secoes: foca o primeiro campo de lista com erro.
        const listName = listNames.find((n) => !!bad[n])
        const target = listName && firstErrorPath(bad[listName], listName)
        if (target) setTimeout(() => form.setFocus(target as Path<T>), 0)
      }
    },
  )

  const busy = isSubmitting || form.formState.isSubmitting

  /** Um campo: lista vira ListBlock (itens renderizam as linhas de `list.item` com nomes `lista.i.campo`); os demais, FormField. */
  const renderCell = (spec: FieldSpec, grid: boolean): ReactNode => {
    if (resolveKind(spec) === 'list' && spec.list) {
      const item = spec.list.item
      return (
        <ListBlock
          spec={spec}
          control={form.control}
          disabled={busy}
          renderItem={(idx) => item.filter((r) => !isSection(r)).map((r, j) => renderRow(r as FieldRow, j, `${spec.name}.${idx}.`))}
        />
      )
    }
    return <FormField spec={spec} control={form.control} disabled={busy} lookupState={lookupFor(spec)} grid={grid || undefined} />
  }
  const renderRow = (row: FieldRow, i: number, prefix?: string) => {
    const cells = cellsOf(row).map((c) => (prefix ? { ...c, name: `${prefix}${c.name}` } : c))
    if (!prefix && !isGridRow(cells, hasSections)) {
      return (
        <div key={i} className="flex flex-col gap-4 md:flex-row">
          {cells.map((spec) => (
            <div key={spec.name} className="min-w-0 md:flex-[var(--w)]" style={{ '--w': spec.width ?? 1 } as CSSProperties}>
              {renderCell(spec, false)}
            </div>
          ))}
        </div>
      )
    }
    return (
      <div key={i} className="grid grid-cols-12 items-start gap-4">
        {cells.map((spec) => (
          <div key={spec.name} className={cn('min-w-0', SIZE_CLASS[resolveSize(spec)])}>
            {renderCell(spec, true)}
          </div>
        ))}
      </div>
    )
  }

  const renderGroup = (g: Group) => {
    const rows = g.rows.map((r, i) => renderRow(r, i))
    const s = g.section
    if (!s) return <Fragment key={g.key}>{rows}</Fragment>
    const titleId = `${uid}-${g.key}-title`
    if (asTabs) return null
    if (!s.collapsible) {
      return (
        <Card key={g.key} role="group" aria-labelledby={titleId}>
          <CardHeader>
            <CardTitle id={titleId}>{s.section}</CardTitle>
            {s.description && <CardDescription>{s.description}</CardDescription>}
          </CardHeader>
          <CardContent className="flex flex-col gap-4">{rows}</CardContent>
        </Card>
      )
    }
    const current = form.getValues() as Record<string, unknown>
    const auto = !!s.defaultOpen || namesOf(g).some((n) => isFilled(current[n])) || groupHasError(g)
    const open = manualOpen[g.key] ?? auto
    return (
      <Card key={g.key} role="group" aria-labelledby={titleId}>
        <Accordion type="single" collapsible value={open ? 'aberta' : ''} onValueChange={(v) => setManualOpen((m) => ({ ...m, [g.key]: v === 'aberta' }))}>
          <AccordionItem value="aberta" className="border-b-0">
            <CardHeader>
              <AccordionTrigger className="py-0">
                <span id={titleId} className="leading-none font-semibold">
                  {s.section}
                  {groupHasError(g) && <span className="ml-2 text-xs font-normal text-destructive">(há campos com erro)</span>}
                </span>
              </AccordionTrigger>
              {s.description && <CardDescription>{s.description}</CardDescription>}
            </CardHeader>
            <AccordionContent className="pt-4 pb-0">
              <CardContent className="flex flex-col gap-4">{rows}</CardContent>
            </AccordionContent>
          </AccordionItem>
        </Accordion>
      </Card>
    )
  }

  const body = asTabs ? (
    <>
      {groups.filter((g) => !g.section).map(renderGroup)}
      <Tabs value={tab ?? sections[0].key} onValueChange={setTab}>
        <TabsList className="h-auto w-full flex-wrap justify-start">
          {sections.map((g) => (
            <TabsTrigger key={g.key} value={g.key} className="flex-none">
              {g.section?.section}
              {groupHasError(g) && (
                <>
                  <span aria-hidden className="size-2 rounded-full bg-destructive" />
                  <span className="sr-only">(com erro)</span>
                </>
              )}
            </TabsTrigger>
          ))}
        </TabsList>
        {sections.map((g) => (
          <TabsContent key={g.key} value={g.key} forceMount>
            <Card>
              {g.section?.description && (
                <CardHeader>
                  <CardDescription>{g.section.description}</CardDescription>
                </CardHeader>
              )}
              <CardContent className="flex flex-col gap-4">{g.rows.map((r, i) => renderRow(r, i))}</CardContent>
            </Card>
          </TabsContent>
        ))}
      </Tabs>
    </>
  ) : (
    groups.map(renderGroup)
  )

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4" aria-busy={busy || undefined}>
      {body}
      {serverError !== undefined && serverError !== null && (
        <div role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {errorMessage(serverError)}
          {serverError instanceof ApiError && serverError.status !== 0 && <span className="block text-xs opacity-80">Código: {serverError.codigo}</span>}
        </div>
      )}
      <div className={hasSections ? 'sticky bottom-0 z-10 flex items-center justify-end gap-2 border-t bg-background py-3' : 'flex justify-end gap-2'}>
        {hasSections && isDirty && (
          <p role="status" className="mr-auto text-sm text-muted-foreground">
            Alterações não salvas
          </p>
        )}
        {onCancel && (
          <Button type="button" variant="outline" onClick={onCancel} disabled={busy}>
            Cancelar
          </Button>
        )}
        <Button type="submit" disabled={busy}>
          {busy && <Loader2 className="animate-spin" aria-hidden />}
          {submitLabel ?? (isEdit ? 'Salvar alterações' : 'Salvar')}
        </Button>
      </div>
    </form>
  )
}
