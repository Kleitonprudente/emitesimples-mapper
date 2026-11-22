import { Fragment, useCallback, useEffect, useId, useMemo, useRef, useState, type CSSProperties } from 'react'
import { useForm, type DefaultValues, type FieldValues, type Path, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { ApiError, api } from '@/lib/api'
import { errorMessage } from '@/runtime/feedback'
import { FormField, SIZE_CLASS, SIZE_COLS, resolveKind, resolveSize, type FieldSpec, type LookupState } from '@/runtime/FormField'
import { onlyDigits } from '@/runtime/format'

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

interface LookupUi {
  loading: boolean
  error?: string
  notice?: string
}

export const AVISO_MUNICIPIO = 'Município não encontrado na tabela; escolha manualmente'

const flatten = (layout: FieldLayout): FieldSpec[] => layout.flatMap((r) => (isSection(r) ? [] : cellsOf(r)))

const isFilled = (v: unknown): boolean => v !== undefined && v !== null && v !== '' && v !== false

/** Campo vazio (ou FK limpa pelo X do Combobox, que vira null) vira "ausente" (undefined), para o zod tratar obrigatorio x opcional. */
function clean(values: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(values).map(([k, v]) => [k, v === '' || v === null ? undefined : v]))
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
}: AutoFormProps<T>) {
  const specs = useMemo(() => flatten(fields), [fields])
  const fkNames = useMemo(() => specs.filter((s) => resolveKind(s) === 'fk').map((s) => s.name), [specs])
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
      return result
    }
  }, [schema])

  const form = useForm<T>({
    resolver,
    defaultValues: defaults,
    values,
    mode: 'onTouched',
  })

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

  const lookupFor = (spec: FieldSpec): LookupState | undefined => {
    if (spec.lookup !== 'cep') return undefined
    const name = spec.name
    const cepNow = () => onlyDigits(String(form.getValues(name as Path<T>) ?? ''))
    // forcar=false (sair do campo): so consulta CEP completo e diferente do que havia ao entrar. forcar=true (botao ou Enter): consulta sempre.
    const buscar = (forcar: boolean) => {
      const cep = cepNow()
      if (cep.length !== 8) {
        if (forcar) setLookup(name, { loading: false, error: 'Informe os 8 dígitos do CEP.' })
        return
      }
      if (!forcar && cep === focusRef.current[name] && !erroredRef.current.has(name)) return
      const n = (seqRef.current[name] = (seqRef.current[name] ?? 0) + 1)
      erroredRef.current.delete(name)
      setLookup(name, { loading: true })
      api<CepResult>(`/cep/${cep}`).then(
        (r) => {
          if (seqRef.current[name] !== n) return // resposta de uma busca antiga
          setLookup(name, { loading: false, notice: applyCep(r) })
        },
        (e: unknown) => {
          if (seqRef.current[name] !== n) return
          erroredRef.current.add(name)
          setLookup(name, { loading: false, error: errorMessage(e) })
        },
      )
    }
    return {
      loading: !!lookups[name]?.loading,
      error: lookups[name]?.error,
      notice: lookups[name]?.notice,
      onFocus: () => {
        focusRef.current[name] = cepNow()
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
      const name = first ? namesOf(first).find((n) => !!bad[n]) : undefined
      if (first && name) {
        if (asTabs) setTab(first.key)
        setTimeout(() => form.setFocus(name as Path<T>), 0)
      }
    },
  )

  const busy = isSubmitting || form.formState.isSubmitting

  const renderRow = (row: FieldRow, i: number) => {
    const cells = cellsOf(row)
    if (!isGridRow(cells, hasSections)) {
      return (
        <div key={i} className="flex flex-col gap-4 md:flex-row">
          {cells.map((spec) => (
            <div key={spec.name} className="min-w-0 md:flex-[var(--w)]" style={{ '--w': spec.width ?? 1 } as CSSProperties}>
              <FormField spec={spec} control={form.control} disabled={busy} lookupState={lookupFor(spec)} />
            </div>
          ))}
        </div>
      )
    }
    return (
      <div key={i} className="grid grid-cols-12 items-start gap-4">
        {cells.map((spec) => (
          <div key={spec.name} className={cn('min-w-0', SIZE_CLASS[resolveSize(spec)])}>
            <FormField spec={spec} control={form.control} disabled={busy} lookupState={lookupFor(spec)} grid />
          </div>
        ))}
      </div>
    )
  }

  const renderGroup = (g: Group) => {
    const rows = g.rows.map(renderRow)
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
              <CardContent className="flex flex-col gap-4">{g.rows.map(renderRow)}</CardContent>
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
