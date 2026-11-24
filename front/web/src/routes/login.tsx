import { useEffect, useState } from 'react'
import { createFileRoute, redirect, useRouter } from '@tanstack/react-router'
import { z } from 'zod'
import { Building2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { EnvBadge } from '@/components/EnvBadge'
import { ThemeToggle } from '@/components/ThemeToggle'
import { ApiError, type PapelValor } from '@/lib/api'
import { PAPEL_LABEL, login, restoreSession } from '@/lib/auth'
import { AutoForm } from '@/runtime/AutoForm'
import type { FieldSpec } from '@/runtime/FormField'
import { formatDuration } from '@/runtime/format'

/** So aceita caminho interno (evita open redirect). */
function safeRedirect(v: unknown): string | undefined {
  return typeof v === 'string' && v.startsWith('/') && !v.startsWith('//') && !v.startsWith('/login') ? v : undefined
}

export const Route = createFileRoute('/login')({
  validateSearch: (s: Record<string, unknown>) => ({ redirect: safeRedirect(s.redirect) }),
  beforeLoad: async ({ search }) => {
    // Quem ja tem sessao nao precisa ver o login.
    if (await restoreSession()) throw redirect({ to: (search.redirect ?? '/') as never })
  },
  component: LoginPage,
})

const schema = z.object({
  email: z.email('Informe um e-mail válido.'),
  senha: z.string().min(1, 'Informe a senha.'),
})
type Credenciais = z.infer<typeof schema>

const FIELDS: FieldSpec[] = [
  { name: 'email', label: 'E-mail', kind: 'email', required: true, placeholder: 'voce@empresa.com.br' },
  { name: 'senha', label: 'Senha', kind: 'password', required: true },
]

interface EmpresaOpcao {
  id: string
  razaoSocial: string
  papel: PapelValor
}

/** Contagem regressiva em segundos: start(n) inicia; `left` cai 1 por segundo ate 0. */
function useCountdown() {
  const [left, setLeft] = useState(0)
  useEffect(() => {
    if (left <= 0) return
    const t = setTimeout(() => setLeft((l) => l - 1), 1000)
    return () => clearTimeout(t)
  }, [left])
  return [left, setLeft] as const
}

function LoginPage() {
  const router = useRouter()
  const { redirect: destino } = Route.useSearch()
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [empresas, setEmpresas] = useState<EmpresaOpcao[] | null>(null)
  const [creds, setCreds] = useState<Credenciais | null>(null)
  const [left, setLeft] = useCountdown()

  async function entrar(c: Credenciais, empresaId?: string) {
    setPending(true)
    setError(null)
    try {
      await login({ ...c, empresaId })
      setCreds(null)
      router.history.push(destino ?? '/')
    } catch (e) {
      if (e instanceof ApiError && e.status === 409 && e.codigo === 'empresa_obrigatoria') {
        setCreds(c)
        setEmpresas((e.detalhes?.empresas as EmpresaOpcao[] | undefined) ?? [])
      } else {
        setError(e)
        if (e instanceof ApiError && e.status === 429) setLeft(e.retryAfter ?? 60)
      }
    } finally {
      setPending(false)
    }
  }

  const bloqueado = left > 0

  return (
    <div className="flex min-h-screen items-center justify-center bg-background p-4">
      <div className="absolute top-4 right-4 flex items-center gap-2">
        <EnvBadge />
        <ThemeToggle />
      </div>
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">{empresas ? 'Escolha a empresa' : 'Entrar'}</CardTitle>
          <CardDescription>{empresas ? 'Seu usuário tem acesso a mais de uma empresa.' : 'Acesse sua conta do Emite Simples.'}</CardDescription>
        </CardHeader>
        <CardContent>
          {empresas && creds ? (
            <div className="flex flex-col gap-2">
              {empresas.map((e) => (
                <Button key={e.id} variant="outline" className="h-auto justify-start gap-3 py-3 text-left whitespace-normal" disabled={pending} onClick={() => entrar(creds, e.id)}>
                  <Building2 aria-hidden />
                  <span className="flex flex-col">
                    <span>{e.razaoSocial}</span>
                    <span className="text-xs font-normal text-muted-foreground">{PAPEL_LABEL[e.papel] ?? e.papel}</span>
                  </span>
                </Button>
              ))}
              {error !== null && (
                <p role="alert" className="text-sm text-destructive">
                  {error instanceof ApiError ? error.mensagem : 'Não foi possível entrar. Tente novamente.'}
                </p>
              )}
              <Button
                variant="ghost"
                onClick={() => {
                  setEmpresas(null)
                  setCreds(null)
                  setError(null)
                }}
              >
                Voltar
              </Button>
            </div>
          ) : (
            <>
              <AutoForm
                schema={schema}
                fields={FIELDS}
                defaults={{ email: '', senha: '' }}
                onSubmit={(v) => entrar(v)}
                isSubmitting={pending || bloqueado}
                submitLabel={bloqueado ? `Aguarde ${formatDuration(left)}` : 'Entrar'}
                serverError={error && !(error instanceof ApiError && error.status === 429) ? error : undefined}
              />
              {bloqueado && (
                <p role="alert" className="mt-3 text-sm text-destructive">
                  Muitas tentativas. Aguarde {formatDuration(left)} para tentar novamente.
                </p>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
