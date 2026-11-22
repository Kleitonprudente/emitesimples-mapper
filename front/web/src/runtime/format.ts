import type { BadgeTone } from '@/components/ui/badge'

/* Formatacao pt-BR. Mascaras sao SOMENTE de apresentacao: o dado trafega cru (so digitos). */

/** CNPJ com 14 digitos e digitos verificadores corretos (sem todos iguais). Recebe so digitos. */
export function isValidCnpj(c: string): boolean {
  if (!/^\d{14}$/.test(c) || /^(\d)\1{13}$/.test(c)) return false
  const dv = (n: number) => {
    let soma = 0
    let peso = n - 7
    for (let i = 0; i < n; i++) {
      soma += Number(c[i]) * peso--
      if (peso < 2) peso = 9
    }
    const r = soma % 11
    return r < 2 ? 0 : 11 - r
  }
  return dv(12) === Number(c[12]) && dv(13) === Number(c[13])
}

/** CPF com 11 digitos e digitos verificadores corretos (sem todos iguais). Recebe so digitos. */
export function isValidCpf(c: string): boolean {
  if (!/^\d{11}$/.test(c) || /^(\d)\1{10}$/.test(c)) return false
  const dv = (n: number) => {
    let soma = 0
    for (let i = 0; i < n; i++) soma += Number(c[i]) * (n + 1 - i)
    const r = (soma * 10) % 11
    return r === 10 ? 0 : r
  }
  return dv(9) === Number(c[9]) && dv(10) === Number(c[10])
}

export const onlyDigits =(v: unknown): string => String(v ?? '').replace(/\D/g, '')

export type MaskKind = 'cpf' | 'cnpj' | 'documento' | 'phone' | 'postalCode'

export function maskCPF(v: unknown): string {
  const d = onlyDigits(v).slice(0, 11)
  return d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3-$4')
}

export function maskCNPJ(v: unknown): string {
  const d = onlyDigits(v).slice(0, 14)
  return d
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3/$4')
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, '$1.$2.$3/$4-$5')
}

/** Fixo (10 digitos) ou celular (11): (11) 98765-4321. */
export function maskPhone(v: unknown): string {
  const d = onlyDigits(v).slice(0, 11)
  if (d.length === 0) return ''
  if (d.length <= 2) return `(${d}`
  if (d.length <= 6) return d.replace(/^(\d{2})(\d+)/, '($1) $2')
  if (d.length <= 10) return d.replace(/^(\d{2})(\d{4})(\d+)/, '($1) $2-$3')
  return d.replace(/^(\d{2})(\d{5})(\d+)/, '($1) $2-$3')
}

export function maskCEP(v: unknown): string {
  return onlyDigits(v)
    .slice(0, 8)
    .replace(/^(\d{5})(\d)/, '$1-$2')
}

export const MASKS: Record<MaskKind, { apply: (v: unknown) => string; maxDigits: number; inputMode: 'numeric' | 'tel' }> = {
  cpf: { apply: maskCPF, maxDigits: 11, inputMode: 'numeric' },
  cnpj: { apply: maskCNPJ, maxDigits: 14, inputMode: 'numeric' },
  documento: { apply: (v) => maskDocumento(v), maxDigits: 14, inputMode: 'numeric' },
  phone: { apply: maskPhone, maxDigits: 11, inputMode: 'tel' },
  postalCode: { apply: maskCEP, maxDigits: 8, inputMode: 'numeric' },
}

export function applyMask(kind: MaskKind | undefined, v: unknown): string {
  if (v === null || v === undefined) return ''
  return kind ? MASKS[kind].apply(v) : String(v)
}

/** CPF (11 digitos) ou CNPJ (14) automaticamente, para colunas de documento. */
export function maskDocumento(v: unknown): string {
  const d = onlyDigits(v)
  return d.length > 11 ? maskCNPJ(d) : maskCPF(d)
}

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
export function formatCurrency(v: number | string | null | undefined): string {
  if (v === null || v === undefined || v === '') return ''
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? brl.format(n) : ''
}

/** Aceita 'YYYY-MM-DD' (sem fuso: nao desloca o dia) ou ISO completo. */
function toDate(v: string | Date): Date | null {
  if (v instanceof Date) return Number.isNaN(v.getTime()) ? null : v
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(v)
  const d = m ? new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3])) : new Date(v)
  return Number.isNaN(d.getTime()) ? null : d
}

const dateFmt = new Intl.DateTimeFormat('pt-BR')
const dateTimeFmt = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

export function formatDate(v: string | Date | null | undefined): string {
  if (!v) return ''
  const d = toDate(v)
  return d ? dateFmt.format(d) : ''
}

export function formatDateTime(v: string | Date | null | undefined): string {
  if (!v) return ''
  const d = toDate(v)
  return d ? dateTimeFmt.format(d) : ''
}

/** Tom de badge por valor de enum (mapa explicito + heuristica por palavra). */
const TONES: Record<string, BadgeTone> = {
  ATIVO: 'success',
  ATIVA: 'success',
  EMITIDA: 'success',
  AUTORIZADA: 'success',
  PAGO: 'success',
  PENDENTE: 'warning',
  PROCESSANDO: 'info',
  RASCUNHO: 'neutral',
  CANCELADA: 'danger',
  CANCELADO: 'danger',
  REJEITADA: 'danger',
  ERRO: 'danger',
  INATIVO: 'neutral',
  INATIVA: 'neutral',
}

export function badgeTone(value: string | null | undefined): BadgeTone {
  if (!value) return 'neutral'
  return TONES[value.toUpperCase()] ?? 'neutral'
}

/** Segundos -> "2 minutos", "30 segundos". */
export function formatDuration(seconds: number): string {
  if (seconds < 60) return `${seconds} ${seconds === 1 ? 'segundo' : 'segundos'}`
  const m = Math.ceil(seconds / 60)
  return `${m} ${m === 1 ? 'minuto' : 'minutos'}`
}
