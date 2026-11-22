import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'
import { toast } from 'sonner'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { EnvBadge } from '@/components/EnvBadge'
import { ThemeToggle } from '@/components/ThemeToggle'
import { AutoForm } from '@/runtime/AutoForm'
import { CardList } from '@/runtime/CardList'
import { DataTable, type TableResult } from '@/runtime/DataTable'
import { notifyError, notifySuccess } from '@/runtime/feedback'
import { formatCurrency, formatDate, formatDateTime, maskCNPJ, maskCPF, maskCEP, maskPhone } from '@/runtime/format'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'
import { useTableState } from '@/runtime/table-state'
import { ApiError } from '@/lib/api'
import type { ColumnDef } from '@tanstack/react-table'

export const Route = createFileRoute('/theme')({
  component: ThemePage,
})

const PALETTE = ['neutral-0', 'neutral-50', 'neutral-100', 'neutral-200', 'neutral-300', 'neutral-500', 'neutral-700', 'neutral-800', 'neutral-900', 'neutral-950', 'blue-300', 'blue-500', 'blue-600', 'teal-400', 'teal-500', 'green-400', 'green-600', 'amber-400', 'amber-500', 'red-400', 'red-600', 'sky-400', 'sky-600']

const SEMANTIC = [
  ['background', 'bg-background text-foreground'],
  ['card', 'bg-card text-card-foreground'],
  ['primary', 'bg-primary text-primary-foreground'],
  ['secondary', 'bg-secondary text-secondary-foreground'],
  ['muted', 'bg-muted text-muted-foreground'],
  ['accent', 'bg-accent text-accent-foreground'],
  ['destructive', 'bg-destructive text-destructive-foreground'],
  ['success', 'bg-success text-success-foreground'],
  ['warning', 'bg-warning text-warning-foreground'],
  ['info', 'bg-info text-info-foreground'],
  ['sidebar', 'bg-sidebar text-sidebar-foreground'],
] as const

interface Demo {
  id: string
  nome: string
  valor: number
  criado: string
}
const DEMO: Demo[] = [
  { id: '1', nome: 'Consultoria mensal', valor: 1500, criado: '2026-10-03' },
  { id: '2', nome: 'Desenvolvimento de site', valor: 4800.5, criado: '2026-10-18' },
  { id: '3', nome: 'Manutenção de sistema', valor: 890, criado: '2026-11-02' },
]
const COLS: ColumnDef<Demo>[] = [
  { id: 'nome', accessorKey: 'nome', header: 'Serviço' },
  { id: 'valor', accessorKey: 'valor', header: 'Valor', cell: (c) => formatCurrency(c.getValue() as number) },
  { id: 'criado', accessorKey: 'criado', header: 'Criado em', cell: (c) => formatDate(c.getValue() as string) },
]

const demoSchema = z.object({ nome: z.string().min(1, 'Informe o nome.'), tipo: z.enum(['A', 'B']), ativo: z.boolean() })

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="text-lg font-semibold">{title}</h2>
      {children}
    </section>
  )
}

function ThemePage() {
  const [mode, setMode] = useState<'ok' | 'loading' | 'empty' | 'error'>('ok')
  const [state, update] = useTableState({ pageSize: 10 })
  const result: TableResult<Demo> = {
    data: mode === 'ok' ? DEMO : [],
    count: mode === 'ok' ? DEMO.length : 0,
    isLoading: mode === 'loading',
    isError: mode === 'error',
    error: new ApiError(500, 'erro_interno', 'O servidor não conseguiu responder agora.'),
    refetch: () => setMode('ok'),
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-8 p-4 md:p-8">
      <header className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-semibold">Guia de estilo</h1>
        <EnvBadge />
        <div className="ml-auto">
          <ThemeToggle />
        </div>
      </header>
      <p className="text-sm text-muted-foreground">
        Tokens em 3 tiers em <code>src/index.css</code>. Para re-tematizar, edite só o bloco “Tier 2: slots de marca”.
      </p>

      <Section title="Paleta bruta (Tier 1)">
        <div className="grid grid-cols-3 gap-2 sm:grid-cols-6 lg:grid-cols-8">
          {PALETTE.map((p) => (
            <div key={p} className="flex flex-col gap-1 text-xs">
              <div className="h-10 rounded-md border" style={{ background: `var(--xx-${p})` }} />
              {p}
            </div>
          ))}
        </div>
      </Section>

      <Section title="Cores semânticas (Tier 3)">
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
          {SEMANTIC.map(([name, cls]) => (
            <div key={name} className={`rounded-md border px-3 py-4 text-sm ${cls}`}>
              {name}
            </div>
          ))}
        </div>
      </Section>

      <Section title="Tipografia e raio">
        <div className="flex flex-col gap-1">
          <p className="text-3xl font-semibold">Título grande</p>
          <p className="text-xl font-semibold">Título médio</p>
          <p>Texto de corpo com a fonte da marca.</p>
          <p className="text-sm text-muted-foreground">Texto secundário e legendas.</p>
        </div>
        <div className="flex gap-3">
          {['rounded-sm', 'rounded-md', 'rounded-lg', 'rounded-xl'].map((r) => (
            <div key={r} className={`flex size-16 items-center justify-center border bg-card text-xs ${r}`}>
              {r.replace('rounded-', '')}
            </div>
          ))}
        </div>
      </Section>

      <Section title="Botões e badges">
        <div className="flex flex-wrap gap-2">
          <Button>Primário</Button>
          <Button variant="secondary">Secundário</Button>
          <Button variant="outline">Contorno</Button>
          <Button variant="ghost">Fantasma</Button>
          <Button variant="destructive">Destrutivo</Button>
          <Button variant="link">Link</Button>
          <Button disabled>Desabilitado</Button>
        </div>
        <div className="flex flex-wrap gap-2">
          <Badge>Neutro</Badge>
          <Badge tone="primary">Primário</Badge>
          <Badge tone="success">Sucesso</Badge>
          <Badge tone="warning">Atenção</Badge>
          <Badge tone="danger">Erro</Badge>
          <Badge tone="info">Informação</Badge>
          <Badge tone="outline">Contorno</Badge>
        </div>
      </Section>

      <Section title="Campos e AutoForm">
        <Card>
          <CardContent className="grid gap-6 md:grid-cols-2">
            <AutoForm
              schema={demoSchema}
              fields={[
                { name: 'nome', label: 'Nome', required: true },
                [
                  { name: 'tipo', label: 'Tipo', options: [{ value: 'A', label: 'Opção A' }, { value: 'B', label: 'Opção B' }], required: true },
                  { name: 'ativo', label: 'Ativo', kind: 'boolean' },
                ],
              ]}
              defaults={{ nome: '', ativo: true }}
              onSubmit={() => notifySuccess('Formulário válido.')}
            />
            <div className="flex flex-col gap-2 text-sm">
              <p className="font-medium">Máscaras (só apresentação)</p>
              <Input readOnly aria-label="CPF" value={maskCPF('12345678909')} />
              <Input readOnly aria-label="CNPJ" value={maskCNPJ('11222333000181')} />
              <Input readOnly aria-label="Telefone" value={maskPhone('11987654321')} />
              <Input readOnly aria-label="CEP" value={maskCEP('01310100')} />
              <p className="text-muted-foreground">
                {formatCurrency(1234.5)} · {formatDate('2026-11-21')} · {formatDateTime('2026-11-21T14:30:00')}
              </p>
            </div>
          </CardContent>
        </Card>
      </Section>

      <Section title="DataTable (estados)">
        <div className="flex flex-wrap gap-2">
          {(['ok', 'loading', 'empty', 'error'] as const).map((m) => (
            <Button key={m} size="sm" variant={mode === m ? 'default' : 'outline'} onClick={() => setMode(m)}>
              {{ ok: 'Com dados', loading: 'Carregando', empty: 'Vazio', error: 'Erro' }[m]}
            </Button>
          ))}
        </div>
        <DataTable columns={COLS} result={result} state={state} onStateChange={update} searchPlaceholder="Buscar serviço…" empty={{ title: 'Nenhum serviço ainda.', description: 'Cadastre o primeiro serviço.' }} />
      </Section>

      <Section title="CardList">
        <CardList
          result={result}
          getKey={(r) => r.id}
          title={(r) => r.nome}
          fields={[
            { label: 'Valor', value: (r) => formatCurrency(r.valor) },
            { label: 'Criado', value: (r) => formatDate(r.criado) },
          ]}
        />
      </Section>

      <Section title="Estados">
        <div className="grid gap-3 md:grid-cols-3">
          <Card>
            <CardHeader>
              <CardTitle>Vazio</CardTitle>
              <CardDescription>EmptyState</CardDescription>
            </CardHeader>
            <CardContent>
              <EmptyState title="Nada por aqui." description="Crie o primeiro registro." />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Erro</CardTitle>
              <CardDescription>ErrorState</CardDescription>
            </CardHeader>
            <CardContent>
              <ErrorState error={new ApiError(503, 'indisponivel', 'Serviço temporariamente indisponível.')} onRetry={() => toast.info('Tentando de novo…')} />
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>Carregando</CardTitle>
              <CardDescription>LoadingState e Skeleton</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-col gap-3">
              <LoadingState />
              <Skeleton className="h-4 w-full" />
              <Skeleton className="h-4 w-2/3" />
            </CardContent>
          </Card>
        </div>
      </Section>

      <Section title="Feedback (toast)">
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => notifySuccess('Registro salvo com sucesso.')}>
            Sucesso
          </Button>
          <Button variant="outline" onClick={() => notifyError(new ApiError(409, 'documento_duplicado', 'Já existe uma pessoa com este documento.'))}>
            Erro da API
          </Button>
          <Button variant="outline" onClick={() => notifyError(new Error('boom: NullPointerException'))}>
            Erro inesperado (sem texto técnico)
          </Button>
        </div>
      </Section>
    </div>
  )
}
