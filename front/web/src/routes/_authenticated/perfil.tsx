import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useMutation } from '@tanstack/react-query'
import { z } from 'zod'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PAPEL_LABEL, logout, trocarSenha, useSession } from '@/lib/auth'
import { AutoForm } from '@/runtime/AutoForm'
import { mutationFeedback } from '@/runtime/feedback'
import type { FieldSpec } from '@/runtime/FormField'

export const Route = createFileRoute('/_authenticated/perfil')({
  component: Perfil,
})

const bytes = (s: string) => new TextEncoder().encode(s).length

/** 8 a 72 BYTES (limite do bcrypt no back): caracteres acentuados ocupam mais de 1 byte. */
export const senhaSchema = z
  .object({
    senhaAtual: z.string().min(1, 'Informe a senha atual.'),
    novaSenha: z
      .string()
      .refine((s) => bytes(s) >= 8, 'A nova senha deve ter pelo menos 8 caracteres.')
      .refine((s) => bytes(s) <= 72, 'A nova senha é longa demais (máximo de 72 bytes).'),
    confirmacao: z.string().min(1, 'Repita a nova senha.'),
  })
  .refine((v) => v.novaSenha === v.confirmacao, { path: ['confirmacao'], message: 'As senhas não conferem.' })

const FIELDS: FieldSpec[] = [
  { name: 'senhaAtual', label: 'Senha atual', kind: 'password', required: true },
  { name: 'novaSenha', label: 'Nova senha', kind: 'password', required: true, description: 'De 8 a 72 caracteres.' },
  { name: 'confirmacao', label: 'Repita a nova senha', kind: 'password', required: true },
]

function Linha({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5 sm:flex-row sm:items-center sm:gap-4">
      <dt className="w-40 shrink-0 text-sm text-muted-foreground">{label}</dt>
      <dd className="min-w-0 text-sm">{children}</dd>
    </div>
  )
}

function Perfil() {
  const session = useSession()
  const navigate = useNavigate()
  const [error, setError] = useState<unknown>(null)

  const mutation = useMutation({
    mutationFn: (v: { senhaAtual: string; novaSenha: string }) => trocarSenha(v),
    onSuccess: mutationFeedback({
      success: 'Senha alterada. Entre novamente com a nova senha.',
      // O back revoga todos os tokens de renovacao ao trocar a senha: encerramos a sessao local.
      onDone: async () => {
        await logout()
        await navigate({ to: '/login', search: { redirect: undefined } })
      },
    }).onSuccess,
    onError: (e) => setError(e),
  })

  if (!session) return null
  const { usuario, empresa, papel, plataformaAdmin } = session

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Meu perfil</h1>
      <Card>
        <CardHeader>
          <CardTitle>Dados da sessão</CardTitle>
          <CardDescription>Informações da conta com a qual você entrou.</CardDescription>
        </CardHeader>
        <CardContent>
          <dl className="flex flex-col gap-3">
            <Linha label="Nome">{usuario.nome}</Linha>
            <Linha label="E-mail">{usuario.email}</Linha>
            <Linha label="Empresa">{empresa ? empresa.razaoSocial : <span className="text-muted-foreground">Sem empresa</span>}</Linha>
            <Linha label="Papel">{papel ? PAPEL_LABEL[papel] : <span className="text-muted-foreground">Sem papel</span>}</Linha>
            {plataformaAdmin && (
              <Linha label="Plataforma">
                <Badge tone="info">Administrador da plataforma</Badge>
              </Linha>
            )}
          </dl>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Trocar senha</CardTitle>
          <CardDescription>Ao trocar a senha, as outras sessões são encerradas e você precisará entrar de novo.</CardDescription>
        </CardHeader>
        <CardContent>
          <AutoForm
            schema={senhaSchema}
            fields={FIELDS}
            defaults={{ senhaAtual: '', novaSenha: '', confirmacao: '' }}
            onSubmit={(v) => {
              setError(null)
              return mutation.mutateAsync({ senhaAtual: v.senhaAtual, novaSenha: v.novaSenha }).catch(() => {})
            }}
            isSubmitting={mutation.isPending}
            submitLabel="Alterar senha"
            serverError={error ?? undefined}
          />
        </CardContent>
      </Card>
    </div>
  )
}
