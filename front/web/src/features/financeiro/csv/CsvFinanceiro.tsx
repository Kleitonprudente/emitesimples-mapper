import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { canAct, useSession } from '@/lib/auth'
import { errorMessage, notifyError, notifySuccess } from '@/runtime/feedback'
import { CSV, exportarCsv, useImportarBancos, useImportarCsv, type EntidadeCsv } from '@/features/financeiro/financeiroApi'

/* ---------------------------------------------------------------------------
 * Financeiro > Importar e exportar (window.custom ImportarExportarCsv, rota /financeiro/csv; docs/financeiro/README.md, F4-C3 e F4-C4).
 * Exportar: qualquer papel. Importar: so ADMINISTRADOR (critico): so cria, nome existente e ignorado, tudo ou nada.
 * Bancos do Banco Central: so o administrador da plataforma (cria ou atualiza pelo codigo; nunca apaga nem desmarca).
 * ------------------------------------------------------------------------- */

const ENTIDADES: EntidadeCsv[] = ['contas', 'categorias', 'condicoes']
const CHAVE_DA_LISTA: Record<EntidadeCsv, string> = { contas: 'ContaFinanceira', categorias: 'CategoriaFinanceira', condicoes: 'CondicaoPagamento' }

export function CsvFinanceiroScreen() {
  const session = useSession()
  const plataforma = !!session?.plataformaAdmin
  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Importar e exportar</h1>
      {!plataforma &&
        ENTIDADES.map((e) => <SecaoDaEntidade key={e} entidade={e} podeImportar={canAct(session, 'ADMINISTRADOR')} />)}
      {plataforma && <SecaoDosBancos />}
    </div>
  )
}

function SecaoDaEntidade({ entidade, podeImportar }: { entidade: EntidadeCsv; podeImportar: boolean }) {
  const qc = useQueryClient()
  const importar = useImportarCsv()
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [resultado, setResultado] = useState<{ criados: number; ignorados: { linha: number; nome: string; motivo: string }[] } | null>(null)
  const id = `csv-${entidade}`
  return (
    <Card>
      <CardHeader>
        <CardTitle>{CSV[entidade].titulo}</CardTitle>
        <CardDescription>
          CSV em UTF-8, separado por ponto e vírgula, com o cabeçalho: <code className="break-all">{CSV[entidade].cabecalho}</code>
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div>
          <Button variant="outline" onClick={() => void exportarCsv(entidade).catch((e) => notifyError(e, 'Não foi possível exportar'))}>
            Exportar CSV
          </Button>
        </div>
        {podeImportar && (
          <div className="flex flex-col gap-2">
            <Label htmlFor={id}>Importar arquivo</Label>
            <div className="flex flex-wrap items-center gap-2">
              <Input
                id={id}
                type="file"
                accept=".csv,text/csv"
                className="max-w-sm"
                onChange={(ev) => {
                  setArquivo(ev.target.files?.[0] ?? null)
                  setResultado(null)
                }}
              />
              <Button
                disabled={!arquivo || importar.isPending}
                onClick={() =>
                  arquivo &&
                  void importar.mutateAsync({ entidade, arquivo }).then(
                    (r) => {
                      setResultado(r)
                      void qc.invalidateQueries({ queryKey: [CHAVE_DA_LISTA[entidade]] })
                      notifySuccess(`${r.criados} registro(s) criado(s).`)
                    },
                    () => undefined,
                  )
                }
              >
                {importar.isPending ? 'Importando…' : 'Importar'}
              </Button>
            </div>
            <p className="text-sm text-muted-foreground">
              A importação só cria: nome que já existe é ignorado. Um erro em qualquer linha recusa o arquivo inteiro, sem gravar nada.
            </p>
            {importar.isError && (
              <Alert variant="destructive">
                <AlertTitle>Arquivo recusado</AlertTitle>
                <AlertDescription>{errorMessage(importar.error)}</AlertDescription>
              </Alert>
            )}
            {resultado && (
              <Alert variant="success">
                <AlertTitle>{resultado.criados} criado(s)</AlertTitle>
                {resultado.ignorados.length > 0 && (
                  <AlertDescription>
                    <ul className="list-disc pl-4">
                      {resultado.ignorados.map((i) => (
                        <li key={i.linha}>
                          Linha {i.linha} ({i.nome}): {i.motivo}
                        </li>
                      ))}
                    </ul>
                  </AlertDescription>
                )}
              </Alert>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function SecaoDosBancos() {
  const qc = useQueryClient()
  const importar = useImportarBancos()
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [resultado, setResultado] = useState<{ criados: number; atualizados: number; ignorados: number } | null>(null)
  return (
    <Card>
      <CardHeader>
        <CardTitle>Bancos (Banco Central)</CardTitle>
        <CardDescription>
          Arquivo público de participantes do Banco Central. Cria ou atualiza pelo código; nunca apaga nem desmarca a cobrança integrada.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-2">
        <Label htmlFor="csv-bancos">Arquivo do Banco Central</Label>
        <div className="flex flex-wrap items-center gap-2">
          <Input
            id="csv-bancos"
            type="file"
            accept=".csv,text/csv"
            className="max-w-sm"
            onChange={(ev) => {
              setArquivo(ev.target.files?.[0] ?? null)
              setResultado(null)
            }}
          />
          <Button
            disabled={!arquivo || importar.isPending}
            onClick={() =>
              arquivo &&
              void importar.mutateAsync(arquivo).then(
                (r) => {
                  setResultado(r)
                  void qc.invalidateQueries({ queryKey: ['Banco'] })
                  notifySuccess('Bancos atualizados.')
                },
                () => undefined,
              )
            }
          >
            {importar.isPending ? 'Importando…' : 'Importar'}
          </Button>
        </div>
        {importar.isError && (
          <Alert variant="destructive">
            <AlertTitle>Arquivo recusado</AlertTitle>
            <AlertDescription>{errorMessage(importar.error)}</AlertDescription>
          </Alert>
        )}
        {resultado && (
          <Alert variant="success">
            <AlertTitle>Importação concluída</AlertTitle>
            <AlertDescription>
              {resultado.criados} criado(s), {resultado.atualizados} atualizado(s), {resultado.ignorados} linha(s) sem código ignorada(s).
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
    </Card>
  )
}
