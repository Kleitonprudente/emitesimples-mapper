import { useMemo, type CSSProperties } from 'react'
import { useForm, type DefaultValues, type FieldValues, type Resolver } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ApiError } from '@/lib/api'
import { errorMessage } from '@/runtime/feedback'
import { FormField, resolveKind, type FieldSpec } from '@/runtime/FormField'

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
}

const flatten = (layout: FieldLayout): FieldSpec[] => layout.flatMap((r) => (Array.isArray(r) ? r : [r]))

/** Campo vazio vira "ausente" (undefined), para o zod tratar obrigatorio x opcional. */
function clean(values: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(Object.entries(values).map(([k, v]) => [k, v === '' ? undefined : v]))
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

  const submit = form.handleSubmit(async (parsed) => {
    // FK: o back recebe SO {id}.
    const out = { ...parsed } as Record<string, unknown>
    for (const n of fkNames) {
      const v = out[n] as { id?: string } | null | undefined
      out[n] = v?.id ? { id: v.id } : undefined
    }
    await onSubmit(out as T)
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
                <FormField spec={spec} control={form.control} disabled={busy} />
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
