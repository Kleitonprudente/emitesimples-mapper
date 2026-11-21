import { createFileRoute } from '@tanstack/react-router'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useSession } from '@/lib/auth'

export const Route = createFileRoute('/_authenticated/')({
  component: Inicio,
})

function Inicio() {
  const session = useSession()
  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Olá, {session?.usuario.nome.split(' ')[0]}</h1>
      <Card>
        <CardHeader>
          <CardTitle>Bem-vindo ao Emite Simples</CardTitle>
          <CardDescription>As telas do emissor de NFS-e aparecem no menu à medida que forem construídas.</CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">Use o menu lateral para navegar.</CardContent>
      </Card>
    </div>
  )
}
