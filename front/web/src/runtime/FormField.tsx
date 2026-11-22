import { useId } from 'react'
import { Loader2, Search } from 'lucide-react'
import { Controller, type Control, type FieldValues, type Path } from 'react-hook-form'
import { Input } from '@/components/ui/input'
import { InputGroup, InputGroupButton, InputGroupInput } from '@/components/ui/input-group'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { FieldLayout } from '@/runtime/AutoForm'
import { Combobox, type FkSource, type FkValue } from '@/runtime/Combobox'
import { MASKS, applyMask, onlyDigits, type MaskKind } from '@/runtime/format'
import { SEARCH_BUTTON_CLASS } from '@/runtime/SearchInput'

export type FieldKind = 'text' | 'email' | 'password' | 'textarea' | 'number' | 'currency' | 'boolean' | 'date' | 'enum' | 'fk' | 'list'

/** Tamanho de um campo na grade de 12 colunas. */
export type FieldSize = 'xs' | 's' | 'm' | 'l' | 'xl' | 'full'

/** Colunas (de 12) de cada tamanho. */
export const SIZE_COLS: Record<FieldSize, number> = { xs: 2, s: 3, m: 4, l: 6, xl: 8, full: 12 }

/** Classes LITERAIS (o Tailwind so gera o que aparece escrito): 12 no mobile, 6 entre sm e md, N a partir de md. */
export const SIZE_CLASS: Record<FieldSize, string> = {
  xs: 'col-span-12 sm:col-span-6 md:col-span-2',
  s: 'col-span-12 sm:col-span-6 md:col-span-3',
  m: 'col-span-12 sm:col-span-6 md:col-span-4',
  l: 'col-span-12 sm:col-span-6 md:col-span-6',
  xl: 'col-span-12 sm:col-span-6 md:col-span-8',
  full: 'col-span-12',
}

/** Campo de lista (detalhes 1-N dentro do formulario): o valor do campo e um array de objetos `{ id?, ...camposVisiveis }`. */
export interface ListSpec {
  /** Campos de cada item (mesma grade de 12 colunas; sem secoes). Os nomes sao relativos ao item (`nome`, nao `contatos.0.nome`). */
  item: FieldLayout
  /** Nome do item no singular, usado no titulo ("Contato 1") e nos rotulos de acessibilidade. */
  itemLabel: string
  /** Texto do botao de adicionar (ex.: "Adicionar contato"). */
  addLabel: string
  /** Texto do estado vazio. Padrao: "Nenhum {itemLabel em minusculas} adicionado." */
  emptyText?: string
  /** Limite de itens; ao atingir, o botao de adicionar fica desabilitado. */
  maxItems?: number
  /** Valores iniciais de um item novo. */
  defaultItem?: Record<string, unknown>
}

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
   * `'cnpj'`: consulta `GET /consulta-cnpj/{cnpj}` (14 digitos validos) e preenche, por nome, razaoSocial (ou `nome`, se nao
   * houver razaoSocial), tipoPessoa='juridica', consumidorFinal=false, nomeFantasia, email, telefone, cep, logradouro, numeroEndereco, complemento, bairro, municipio (FK),
   * regimeTributario e rgInscricaoEstadual. Use junto com `mask: 'documento'` ou `'cnpj'`.
   */
  lookup?: 'cep' | 'cnpj'
  readOnly?: boolean
  /** `value` numerico (enum numerico do back) volta como numero em `onChange`: o select so trafega texto. */
  options?: { value: string | number; label: string }[]
  fk?: FkSource
  /**
   * Tamanho na grade de 12 colunas (md+): xs=2, s=3, m=4, l=6, xl=8, full=12. Se omitido, utoSize decide pelo tipo e
   * pelo maxLength.
   */
  size?: FieldSize
  /** @deprecated Use size. Peso de proporcao dentro da linha (padrao 1); so vale em linhas sem nenhum size. */
  width?: number
  maxLength?: number
  /** Configuracao do campo `kind: 'list'`; o `name` e o do array no formulario (ex.: `contatos`). */
  list?: ListSpec
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
  /** Tipo da busca: define os textos do botao e do estado de carregando. */
  kind?: 'cep' | 'cnpj'
  /** Busca explicita (botao de busca ou Enter): consulta mesmo que o CEP nao tenha mudado; com menos de 8 digitos avisa no campo. */
  onSearch: () => void
}

export function resolveKind(spec: FieldSpec): FieldKind {
  if (spec.kind) return spec.kind
  if (spec.list) return 'list'
  if (spec.options) return 'enum'
  if (spec.fk) return 'fk'
  return 'text'
}

/** Tamanho automatico quando size e omitido: pelo tipo do campo e, no texto, pelo maxLength. */
export function autoSize(spec: FieldSpec): FieldSize {
  const kind = resolveKind(spec)
  if (kind === 'textarea' || kind === 'list') return 'full'
  if (kind === 'boolean' || kind === 'date' || kind === 'number' || kind === 'currency') return 's'
  if (kind === 'fk') return 'l'
  if (kind === 'enum') return 'm'
  if (spec.mask) return 's'
  const n = spec.maxLength
  if (n === undefined) return 'm'
  if (n <= 5) return 'xs'
  if (n <= 15) return 's'
  if (n <= 30) return 'm'
  if (n <= 100) return 'l'
  if (n <= 255) return 'xl'
  return 'full'
}

export const resolveSize = (spec: FieldSpec): FieldSize => spec.size ?? autoSize(spec)

/** Linhas do textarea por maxLength. */
export const textareaRows = (maxLength?: number): number => (maxLength === undefined ? 4 : maxLength <= 200 ? 3 : maxLength <= 2000 ? 6 : 8)

/** Um campo: tipo -> controle (camada 3). Trocar este mapa troca o design system sem tocar em tela. */
export function FormField<T extends FieldValues>({
  spec,
  control,
  disabled,
  lookupState,
  grid,
}: {
  spec: FieldSpec
  control: Control<T>
  disabled?: boolean
  lookupState?: LookupState
  /** Dentro da grade de 12 colunas: o Switch desce para alinhar com a caixa dos inputs vizinhos. */
  grid?: boolean
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
              <Textarea {...common} rows={textareaRows(spec.maxLength)} placeholder={spec.placeholder} maxLength={spec.maxLength} value={field.value ?? ''} onChange={field.onChange} onBlur={field.onBlur} ref={field.ref} />
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
            // Campo com busca (CEP): padrao B do guia, InputGroup com botao de busca em destaque colado a direita. Enter e o botao consultam; sair do campo tambem.
            if (lookupState) {
              node = (
              <>
                <InputGroup>
                  <InputGroupInput
                    {...common}
                    aria-busy={loading || undefined}
                    type="text"
                    inputMode={mask ? MASKS[mask].inputMode : undefined}
                    placeholder={spec.placeholder}
                    maxLength={mask ? undefined : spec.maxLength}
                    value={mask ? applyMask(mask, field.value) : (field.value ?? '')}
                    onChange={(e) => field.onChange(mask ? onlyDigits(e.target.value).slice(0, MASKS[mask].maxDigits) : e.target.value)}
                    onFocus={lookupState.onFocus}
                    onBlur={() => {
                      field.onBlur()
                      lookupState.onBlur()
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        lookupState.onSearch()
                      }
                    }}
                    ref={field.ref}
                  />
                  <InputGroupButton type="button" variant="default" aria-label={lookupState.kind === 'cnpj' ? 'Consultar CNPJ' : 'Buscar endereço pelo CEP'} disabled={isDisabled || loading} className={SEARCH_BUTTON_CLASS} onClick={lookupState.onSearch}>
                    {loading ? <Loader2 className="animate-spin" aria-hidden /> : <Search aria-hidden />}
                  </InputGroupButton>
                </InputGroup>
                {loading && (
                  <span className="sr-only" role="status">
                    {lookupState.kind === 'cnpj' ? 'Consultando CNPJ…' : 'Buscando endereço…'}
                  </span>
                )}
              </>
              )
              break
            }
            node = (
              <Input
                {...common}
                type={kind === 'password' ? 'password' : kind === 'email' ? 'email' : 'text'}
                inputMode={mask ? MASKS[mask].inputMode : undefined}
                placeholder={spec.placeholder}
                maxLength={mask ? undefined : spec.maxLength}
                value={mask ? applyMask(mask, field.value) : (field.value ?? '')}
                onChange={(e) => field.onChange(mask ? onlyDigits(e.target.value).slice(0, MASKS[mask].maxDigits) : e.target.value)}
                onBlur={field.onBlur}
                ref={field.ref}
              />
            )
          }
        }
        return (
          <div className="flex flex-col gap-1.5">
            {kind === 'boolean' ? (
              <div className={grid ? 'flex min-h-9 items-center gap-3 sm:mt-5' : 'flex items-center gap-3'}>
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
            {kind === 'textarea' && spec.maxLength !== undefined && (
              <p aria-hidden className="text-right text-xs text-muted-foreground">
                {String(field.value ?? '').length}/{spec.maxLength}
              </p>
            )}
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
