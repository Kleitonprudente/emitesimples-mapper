import { useCallback, useMemo, useRef, useState, type CSSProperties } from 'react'
import { useForm, type DefaultValues, type FieldValues, type Path, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ApiError, api } from '@/lib/api'
import { errorMessage } from '@/runtime/feedback'
import { FormField, resolveKind, type FieldSpec, type LookupState } from '@/runtime/FormField'
import { onlyDigits } from '@/runtime/format'

// Mensagens de validacao do zod em portugues.
z.config(z.locales.pt())

/** Layout: cada item e um campo (linha inteira) ou um array de campos (mesma linha, com `width` como peso). */
export type FieldLayout = (FieldSpec | FieldSpec[])[]

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

const flatten = (layout: FieldLayout): FieldSpec[] => layout.flatMap((r) => (Array.isArray(r) ? r : [r]))

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
    return {
      loading: !!lookups[name]?.loading,
      error: lookups[name]?.error,
      notice: lookups[name]?.notice,
      onFocus: () => {
        focusRef.current[name] = cepNow()
        // Ao voltar ao campo some a mensagem anterior; `erroredRef` lembra que a ultima busca falhou (deixa repetir).
        setLookups((s) => (s[name]?.error || s[name]?.notice ? { ...s, [name]: { loading: false } } : s))
      },
      onBlur: () => {
        const cep = cepNow()
        if (cep.length !== 8) return
        if (cep === focusRef.current[name] && !erroredRef.current.has(name)) return
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
      },
    }
  }

  const submit = form.handleSubmit(async (parsed) => {
    // FK: o back recebe SO {id}.
    const out = { ...parsed } as Record<string, unknown>
    for (const n of fkNames) {
      const v = out[n] as { id?: string } | null | undefined
      out[n] = v?.id ? { id: v.id } : undefined
    }
    try {
      await onSubmit(out as T)
    } finally {
      for (const n of clearAfterSubmit ?? []) form.resetField(n as Path<T>)
    }
  })

  const busy = isSubmitting || form.formState.isSubmitting

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-4" aria-busy={busy || undefined}>
      {fields.map((row, i) => {
        const cells = Array.isArray(row) ? row : [row]
        return (
          <div key={i} className="flex flex-col gap-4 md:flex-row">
            {cells.map((spec) => (
              <div key={spec.name} className="min-w-0 md:flex-[var(--w)]" style={{ '--w': spec.width ?? 1 } as CSSProperties}>
                <FormField spec={spec} control={form.control} disabled={busy} lookupState={lookupFor(spec)} />
              </div>
            ))}
          </div>
        )
      })}
      {serverError !== undefined && serverError !== null && (
        <div role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {errorMessage(serverError)}
          {serverError instanceof ApiError && serverError.status !== 0 && <span className="block text-xs opacity-80">Código: {serverError.codigo}</span>}
        </div>
      )}
      <div className="flex justify-end gap-2">
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
