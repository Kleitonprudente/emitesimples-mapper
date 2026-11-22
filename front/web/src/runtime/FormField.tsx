import { useId } from 'react'
import { Loader2 } from 'lucide-react'
import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Combobox, type FkSource, type FkValue } from '@/runtime/Combobox'
import { MASKS, applyMask, onlyDigits, type MaskKind } from '@/runtime/format'

export type FieldKind = 'text' | 'email' | 'password' | 'textarea' | 'number' | 'currency' | 'boolean' | 'date' | 'enum' | 'fk'

export interface FieldSpec {
  name: string
  label: string
  /** Se omitido: 'enum' quando ha options, 'fk' quando ha fk, 'text' nos demais. */
  kind?: FieldKind
  /** Mascara so de apresentacao: o valor do formulario e sempre cru (digitos). */
  mask?: MaskKind
  required?: boolean
  placeholder?: string
  description?: string
  /** Texto de ajuda fixo sob o campo, ligado ao controle por aria-describedby (ex.: "Ao sair do campo, o endereco e preenchido."). */
  hint?: string
  /**
   * Busca de endereco por CEP. Ao SAIR do campo com 8 digitos o AutoForm chama `GET /cep/{cep}` e preenche, por
   * convencao de nome, os campos irmaos que existirem no layout: `logradouro`, `bairro`, `complemento` (so se vazio)
   * e `cidade` (objeto `municipio` devolvido). Use junto com `mask: 'postalCode'`.
   */
  lookup?: 'cep'
  readOnly?: boolean
  /** `value` numerico (enum numerico do back) volta como numero em `onChange`: o select so trafega texto. */
  options?: { value: string | number; label: string }[]
  fk?: FkSource
  /** Peso de proporcao dentro da linha (padrao 1). */
  width?: number
  maxLength?: number
}

/** Estado de uma busca (lookup) ligada ao campo; quem o controla e o AutoForm. */
export interface LookupState {
  loading: boolean
  /** Erro da busca (404/502/429...): aparece como mensagem do campo, sem invalidar nem travar o formulario. */
  error?: string
  /** Aviso discreto (ex.: municipio fora da tabela). */
  notice?: string
  onFocus: () => void
  onBlur: () => void
}

export function resolveKind(spec: FieldSpec): FieldKind {
  if (spec.kind) return spec.kind
  if (spec.options) return 'enum'
  if (spec.fk) return 'fk'
  return 'text'
}

/** Um campo: tipo -> controle (camada 3). Trocar este mapa troca o design system sem tocar em tela. */
export function FormField<T extends FieldValues>({
  spec,
  control,
  disabled,
  lookupState,
}: {
  spec: FieldSpec
  control: Control<T>
  disabled?: boolean
  lookupState?: LookupState
}) {
  const uid = useId()
  const id = `${uid}-${spec.name}`
  const errId = `${id}-err`
  const hintId = `${id}-hint`
  const noticeId = `${id}-notice`
  const kind = resolveKind(spec)
  const isDisabled = disabled || spec.readOnly

  return (
    <Controller
      control={control}
      name={spec.name as Path<T>}
      render={({ field, fieldState }) => {
        const lookupError = lookupState?.error
        const invalid = !!fieldState.error
        const hasError = invalid || !!lookupError
        const errorMessage = fieldState.error?.message ?? lookupError
        const notice = !hasError ? lookupState?.notice : undefined
        const describedBy =
          [spec.hint ? hintId : null, hasError ? errId : null, notice ? noticeId : null].filter(Boolean).join(' ') || undefined
        const common = {
          id,
          disabled: isDisabled,
          'aria-invalid': hasError || undefined,
          'aria-describedby': describedBy,
        }
        let node: React.ReactNode
        switch (kind) {
          case 'textarea':
            node = (
              <Textarea {...common} placeholder={spec.placeholder} maxLength={spec.maxLength} value={field.value ?? ''} onChange={field.onChange} onBlur={field.onBlur} ref={field.ref} />
            )
            break
          case 'number':
          case 'currency':
            node = (
              <div className="relative">
                {kind === 'currency' && <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-sm text-muted-foreground">R$</span>}
                <Input
                  {...common}
                  type="number"
                  inputMode="decimal"
                  step={kind === 'currency' ? '0.01' : 'any'}
                  className={kind === 'currency' ? 'pl-9' : undefined}
                  placeholder={spec.placeholder}
                  value={field.value ?? ''}
                  onChange={(e) => field.onChange(e.target.value === '' ? undefined : Number(e.target.value))}
                  onBlur={field.onBlur}
                  ref={field.ref}
                />
              </div>
            )
            break
          case 'boolean':
            node = <Switch {...common} checked={!!field.value} onCheckedChange={field.onChange} onBlur={field.onBlur} ref={field.ref} />
            break
          case 'date':
            node = <Input {...common} type="date" value={field.value ?? ''} onChange={field.onChange} onBlur={field.onBlur} ref={field.ref} />
            break
          case 'enum':
            node = (
              <Select
                value={field.value == null ? '' : String(field.value)}
                onValueChange={(v) => field.onChange((spec.options ?? []).find((o) => String(o.value) === v)?.value ?? v)}
                disabled={isDisabled}
              >
                <SelectTrigger id={id} aria-invalid={hasError || undefined} aria-describedby={describedBy} onBlur={field.onBlur} ref={field.ref}>
                  <SelectValue placeholder={spec.placeholder ?? 'Selecione…'} />
                </SelectTrigger>
                <SelectContent>
                  {(spec.options ?? []).map((o) => (
                    <SelectItem key={o.value} value={String(o.value)}>
                      {o.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )
            break
          case 'fk':
            node = spec.fk ? (
              <Combobox id={id} source={spec.fk} value={field.value as FkValue | null} onChange={field.onChange} placeholder={spec.placeholder} disabled={isDisabled} invalid={hasError} describedBy={describedBy} />
            ) : null
            break
          default: {
            const mask = spec.mask
            const loading = !!lookupState?.loading
            node = (
              <div className="relative">
                <Input
                  {...common}
                  aria-busy={loading || undefined}
                  className={loading ? 'pr-9' : undefined}
                  type={kind === 'password' ? 'password' : kind === 'email' ? 'email' : 'text'}
                  inputMode={mask ? MASKS[mask].inputMode : undefined}
                  placeholder={spec.placeholder}
                  maxLength={mask ? undefined : spec.maxLength}
                  value={mask ? applyMask(mask, field.value) : (field.value ?? '')}
                  onChange={(e) => field.onChange(mask ? onlyDigits(e.target.value).slice(0, MASKS[mask].maxDigits) : e.target.value)}
                  onFocus={lookupState?.onFocus}
                  onBlur={() => {
                    field.onBlur()
                    lookupState?.onBlur()
                  }}
                  ref={field.ref}
                />
                {loading && (
                  <span className="pointer-events-none absolute inset-y-0 right-3 flex items-center text-muted-foreground">
                    <Loader2 className="size-4 animate-spin" aria-hidden />
                    <span className="sr-only" role="status">
                      Buscando endereço…
                    </span>
                  </span>
                )}
              </div>
            )
          }
        }
        return (
          <div className="flex flex-col gap-1.5">
            {kind === 'boolean' ? (
              <div className="flex items-center gap-3">
                {node}
                <Label htmlFor={id}>{spec.label}</Label>
              </div>
            ) : (
              <Label htmlFor={id}>
                {spec.label}
                {spec.required && (
                  <span aria-hidden className="text-destructive">
                    *
                  </span>
                )}
              </Label>
            )}
            {kind !== 'boolean' && node}
            {spec.description && !hasError && <p className="text-xs text-muted-foreground">{spec.description}</p>}
            {spec.hint && (
              <p id={hintId} className="text-xs text-muted-foreground">
                {spec.hint}
              </p>
            )}
            {notice && (
              <p id={noticeId} role="status" className="text-xs text-muted-foreground">
                {notice}
              </p>
            )}
            {hasError && (
              <p id={errId} role="alert" className="text-xs text-destructive">
                {errorMessage}
              </p>
            )}
          </div>
        )
      }}
    />
  )
}
