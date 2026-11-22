import { useRef, useState } from 'react'
import { Loader2, Upload } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { PlatformOnly } from '@/features/admin/PlatformOnly'
import { useImportacoes, useImportarMunicipios, type Importacao } from '@/features/admin/adminApi'
import { ApiError } from '@/lib/api'
import { errorMessage, notifySuccess } from '@/runtime/feedback'
import { formatDateTime } from '@/runtime/format'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'

const MAX_REJEITADOS_EXIBIDOS = 200
const n = (v: number | undefined) => (v === undefined ? null : v.toLocaleString('pt-BR'))

function Numero({ label, value }: { label: string; value: number | undefined }) {
  if (value === undefined) return null
  return (
    <div className="flex flex-col rounded-md border px-3 py-2">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-lg font-semibold tabular-nums">{n(value)}</dd>
    </div>
  )
}

function Resumo({ r }: { r: Importacao }) {
  const exibidos = r.rejeitados.slice(0, MAX_REJEITADOS_EXIBIDOS)
  return (
    <Card aria-labelledby="resumo-titulo">
      <CardHeader>
        <CardTitle id="resumo-titulo" role="heading" aria-level={2}>
          Resumo da importação
        </CardTitle>
        {r.arquivo && <CardDescription>Arquivo: {r.arquivo}</CardDescription>}
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <dl className="grid grid-cols-2 gap-3 sm:grid-cols-5">
          <Numero label="Linhas lidas" value={r.linhasLidas} />
          <Numero label="Criados" value={r.criados} />
          <Numero label="Atualizados" value={r.atualizados} />
          <Numero label="Inalterados" value={r.inalterados} />
          <Numero label="Rejeitados" value={r.totalRejeitados} />
        </dl>
        {exibidos.length > 0 && (
          <div className="flex flex-col gap-2">
            <h3 className="text-sm font-medium">Linhas rejeitadas</h3>
            <div className="max-h-72 overflow-y-auto rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-24">Linha</TableHead>
                    <TableHead>Motivo</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {exibidos.map((x, i) => (
                    <TableRow key={`${x.linha}-${i}`}>
                      <TableCell className="tabular-nums">{x.linha ?? '-'}</TableCell>
                      <TableCell>{x.motivo}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            {r.rejeitados.length > exibidos.length && (
              <p className="text-xs text-muted-foreground">Mostrando as primeiras {exibidos.length} de {r.rejeitados.length} linhas rejeitadas.</p>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function Historico() {
  const q = useImportacoes()
  if (q.isLoading) return <LoadingState label="Carregando histórico…" />
  if (q.isError) return <ErrorState error={q.error} title="Não foi possível carregar o histórico." onRetry={() => void q.refetch()} />
  if (!q.data?.length) return <EmptyState title="Nenhuma importação realizada ainda." />
  const mostra = (f: (i: Importacao) => unknown) => q.data!.some((i) => f(i) !== undefined)
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Data</TableHead>
          {mostra((i) => i.arquivo) && <TableHead>Arquivo</TableHead>}
          {mostra((i) => i.usuario) && <TableHead>Usuário</TableHead>}
          <TableHead className="text-right">Lidas</TableHead>
          <TableHead className="text-right">Criados</TableHead>
          <TableHead className="text-right">Atualizados</TableHead>
          <TableHead className="text-right">Inalterados</TableHead>
          <TableHead className="text-right">Rejeitados</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {q.data.map((i) => (
          <TableRow key={i.id}>
            <TableCell>{i.dataHora ? formatDateTime(i.dataHora) : '-'}</TableCell>
            {mostra((x) => x.arquivo) && <TableCell>{i.arquivo ?? '-'}</TableCell>}
            {mostra((x) => x.usuario) && <TableCell>{i.usuario ?? '-'}</TableCell>}
            <TableCell className="text-right tabular-nums">{n(i.linhasLidas) ?? '-'}</TableCell>
            <TableCell className="text-right tabular-nums">{n(i.criados) ?? '-'}</TableCell>
            <TableCell className="text-right tabular-nums">{n(i.atualizados) ?? '-'}</TableCell>
            <TableCell className="text-right tabular-nums">{n(i.inalterados) ?? '-'}</TableCell>
            <TableCell className="text-right tabular-nums">{n(i.totalRejeitados) ?? '-'}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

function Conteudo() {
  const inputRef = useRef<HTMLInputElement>(null)
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [erroLocal, setErroLocal] = useState<string | null>(null)
  const [resumo, setResumo] = useState<Importacao | null>(null)
  const importar = useImportarMunicipios()

  function enviar(e: React.FormEvent) {
    e.preventDefault()
    if (!arquivo) {
      setErroLocal('Escolha um arquivo para importar.')
      inputRef.current?.focus()
      return
    }
    setErroLocal(null)
    setResumo(null)
    importar.mutate(arquivo, {
      onSuccess: (r) => {
        setResumo(r)
        setArquivo(null)
        if (inputRef.current) inputRef.current.value = ''
        notifySuccess('Importação concluída.')
      },
    })
  }

  const erro = importar.error
  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-4">
      <div>
        <h1 className="text-2xl font-semibold">Importar municípios</h1>
        <p className="text-sm text-muted-foreground">Envie o arquivo da tabela de municípios (IBGE). Municípios novos são criados e os existentes, atualizados.</p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Arquivo</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={enviar} noValidate className="flex flex-col gap-3" aria-busy={importar.isPending || undefined}>
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="arquivo-municipios">Arquivo de municípios</Label>
              <Input
                id="arquivo-municipios"
                ref={inputRef}
                type="file"
                disabled={importar.isPending}
                aria-invalid={erroLocal ? true : undefined}
                aria-describedby={erroLocal ? 'arquivo-municipios-err' : undefined}
                onChange={(e) => {
                  setArquivo(e.target.files?.[0] ?? null)
                  setErroLocal(null)
                }}
              />
              {erroLocal && (
                <p id="arquivo-municipios-err" role="alert" className="text-xs text-destructive">
                  {erroLocal}
                </p>
              )}
            </div>
            {erro && (
              <div role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
                {errorMessage(erro)}
                {erro instanceof ApiError && erro.status !== 0 && <span className="block text-xs opacity-80">Código: {erro.codigo}</span>}
              </div>
            )}
            <div>
              <Button type="submit" disabled={importar.isPending}>
                {importar.isPending ? <Loader2 className="animate-spin" aria-hidden /> : <Upload aria-hidden />}
                {importar.isPending ? 'Importando…' : 'Importar'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
      {resumo && <Resumo r={resumo} />}
      <Card>
        <CardHeader>
          <CardTitle role="heading" aria-level={2}>
            Histórico de importações
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Historico />
        </CardContent>
      </Card>
    </div>
  )
}

export function ImportarMunicipios() {
  return (
    <PlatformOnly>
      <Conteudo />
    </PlatformOnly>
  )
}
