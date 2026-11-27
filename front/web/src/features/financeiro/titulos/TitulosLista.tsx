import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import type { ColumnDef } from '@tanstack/react-table'
import { Eye } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { fkSource } from '@/lib/fk-sources'
import { canAct, useSession } from '@/lib/auth'
import { AutoForm } from '@/runtime/AutoForm'
import { Combobox, type FkValue } from '@/runtime/Combobox'
import { DataTable, type Facet } from '@/runtime/DataTable'
import { mutationFeedback } from '@/runtime/feedback'
import { formatCurrency, formatDate } from '@/runtime/format'
import { resolveIcon } from '@/runtime/icons'
import { useTableState } from '@/runtime/table-state'
import {
  FILTROS,
  FORMA,
  SITUACAO,
  TIPO,
  camposLote,
  garantirPadroes,
  hojeIso,
  loteSchema,
  useBaixarEmLote,
  useResumo,
  useTitulos,
  type Titulo,
  type TipoTitulo,
} from '@/features/financeiro/financeiroApi'

/* ---------------------------------------------------------------------------
 * Financeiro > Contas a receber / Contas a pagar (window.custom ContasAReceber e ContasAPagar; docs/financeiro/README.md, D2).
 * A mesma lista, o tipo vem da rota (§11). Escrita a mao com o catalogo e o runtime: lista sobre GET /financeiro/titulos (nao e CRUD),
 * com os filtros da §7, periodo, pessoa e categoria, totais do filtro, selecao multipla e baixa em lote. O back decide tudo.
 * ------------------------------------------------------------------------- */

const NovoIcon = resolveIcon('plus')
const FACETS: Facet[] = [{ id: 'filtro', title: 'Situação', options: FILTROS.map((f) => ({ value: f.value, label: f.label })) }]

export function TitulosReceberScreen() {
  return <TitulosLista tipo="RECEBER" />
}

export function TitulosPagarScreen() {
  return <TitulosLista tipo="PAGAR" />
}

function colunas(abrir: (t: Titulo) => void, selecionados: Set<string>, alternar: (t: Titulo) => void, comSelecao: boolean, rotuloPessoa: string) {
  const cols: ColumnDef<Titulo>[] = []
  if (comSelecao) {
    cols.push({
      id: 'sel',
      header: () => <span className="sr-only">Selecionar</span>,
      enableSorting: false,
      cell: ({ row }) =>
        ['ABERTO', 'PARCIAL'].includes(row.original.situacao) && row.original.aprovado ? (
          <Checkbox
            checked={selecionados.has(row.original.id)}
            onCheckedChange={() => alternar(row.original)}
            aria-label={`Selecionar ${row.original.descricao}`}
          />
        ) : null,
    })
  }
  cols.push(
    {
      id: 'vencimento',
      header: 'Vencimento',
      enableSorting: false,
      cell: ({ row }) => (
        <span className={row.original.vencido ? 'font-medium text-destructive' : undefined}>{formatDate(row.original.vencimento)}</span>
      ),
    },
    { id: 'pessoa', header: rotuloPessoa, enableSorting: false, cell: ({ row }) => row.original.nomePessoa ?? '—' },
    { id: 'descricao', header: 'Descrição', enableSorting: false, cell: ({ row }) => row.original.descricao },
    { id: 'parcela', header: 'Parcela', enableSorting: false, cell: ({ row }) => `${row.original.parcela}/${row.original.totalParcelas}` },
    { id: 'valor', header: 'Valor', enableSorting: false, cell: ({ row }) => formatCurrency(row.original.valor) },
    { id: 'saldo', header: 'Saldo', enableSorting: false, cell: ({ row }) => formatCurrency(row.original.saldo) },
    {
      id: 'situacao',
      header: 'Situação',
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex flex-wrap gap-1">
          <Badge tone={SITUACAO[row.original.situacao].tone}>{SITUACAO[row.original.situacao].rotulo}</Badge>
          {row.original.vencido && <Badge tone="danger">Vencido</Badge>}
          {!row.original.aprovado && <Badge tone="outline">Aguardando aprovação</Badge>}
          {row.original.paraRevisao && <Badge tone="warning">Para revisão</Badge>}
        </div>
      ),
    },
    { id: 'forma', header: 'Forma prevista', enableSorting: false, cell: ({ row }) => (row.original.formaPrevista ? FORMA[row.original.formaPrevista] : '—') },
    {
      id: 'actions',
      header: () => <span className="sr-only">Ações</span>,
      enableSorting: false,
      cell: ({ row }) => (
        <div className="flex justify-end">
          <Tooltip>
            <TooltipTrigger asChild>
              <Button variant="ghost" size="icon" aria-label="Abrir" onClick={() => abrir(row.original)}>
                <Eye />
              </Button>
            </TooltipTrigger>
            <TooltipContent>Abrir</TooltipContent>
          </Tooltip>
        </div>
      ),
    },
  )
  return cols
}

export function TitulosLista({ tipo }: { tipo: TipoTitulo }) {
  const session = useSession()
  const navigate = useNavigate()
  const pode = canAct(session, 'OPERADOR')
  const t = TIPO[tipo]
  const [state, update] = useTableState({ filters: { filtro: 'EM_ABERTO' } })
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const [pessoa, setPessoa] = useState<FkValue | null>(null)
  const [categoria, setCategoria] = useState<FkValue | null>(null)
  const [selecionados, setSelecionados] = useState<Set<string>>(new Set())
  const [loteAberto, setLoteAberto] = useState(false)
  const result = useTitulos(tipo, state, { de: de || undefined, ate: ate || undefined, idPessoa: pessoa?.id, idCategoria: categoria?.id })
  const resumo = useResumo()
  const lote = useBaixarEmLote()

  // Primeiro acesso ao modulo: cria os padroes (conta Caixa, categorias, condicoes, configuracao). Idempotente; CONSULTA nao pode criar.
  useEffect(() => {
    if (pode) void garantirPadroes().catch(() => undefined)
  }, [pode])

  const alternar = useCallback(
    (x: Titulo) =>
      setSelecionados((s) => {
        const n = new Set(s)
        if (n.has(x.id)) n.delete(x.id)
        else n.add(x.id)
        return n
      }),
    [],
  )
  const columns = useMemo(
    () => colunas((x) => void navigate({ to: '/financeiro/titulos/$id', params: { id: x.id } }), selecionados, alternar, pode, t.pessoa),
    [navigate, selecionados, alternar, pode, t.pessoa],
  )
  const novo = pode ? (
    <Button onClick={() => void navigate({ to: '/financeiro/titulos/$id', params: { id: 'novo' }, search: { tipo } as never })}>
      <NovoIcon /> {t.novo}
    </Button>
  ) : null
  const acoes = (
    <div className="flex flex-wrap items-center gap-2">
      {pode && selecionados.size > 0 && (
        <Button variant="outline" onClick={() => setLoteAberto(true)}>
          Baixar selecionados ({selecionados.size})
        </Button>
      )}
      {novo}
    </div>
  )
  const r = resumo.data?.[tipo === 'RECEBER' ? 'receber' : 'pagar']
  const fb = mutationFeedback({
    success: 'Baixa em lote registrada.',
    onDone: () => {
      setLoteAberto(false)
      setSelecionados(new Set())
    },
  })

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold">{t.titulo}</h1>
      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader>
            <CardDescription>Em aberto</CardDescription>
            <CardTitle>{r ? formatCurrency(r.emAberto) : '—'}</CardTitle>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardDescription>Vencido{r ? ` (${r.quantidadeVencidos} título${r.quantidadeVencidos === 1 ? '' : 's'})` : ''}</CardDescription>
            <CardTitle>{r ? formatCurrency(r.vencido) : '—'}</CardTitle>
          </CardHeader>
        </Card>
      </div>
      <Card>
        <CardContent className="grid gap-4 pt-6 md:grid-cols-12">
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="fin-de">Vencimento de</Label>
            <Input id="fin-de" type="date" value={de} onChange={(e) => setDe(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="fin-ate">até</Label>
            <Input id="fin-ate" type="date" value={ate} onChange={(e) => setAte(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="fin-pessoa">{t.pessoa}</Label>
            <Combobox id="fin-pessoa" source={fkSource(tipo === 'RECEBER' ? 'Cliente' : 'Fornecedor')} value={pessoa} onChange={setPessoa} placeholder="Todos" />
          </div>
          <div className="flex flex-col gap-2 md:col-span-3">
            <Label htmlFor="fin-categoria">Categoria</Label>
            <Combobox
              id="fin-categoria"
              source={fkSource('CategoriaFinanceira')}
              value={categoria}
              onChange={setCategoria}
              placeholder="Todas"
            />
          </div>
        </CardContent>
      </Card>
      <DataTable
        columns={columns}
        result={result}
        state={state}
        onStateChange={update}
        getRowId={(x) => x.id}
        searchPlaceholder="Buscar pela descrição…"
        facets={FACETS}
        toolbarActions={acoes}
        empty={{ title: tipo === 'RECEBER' ? 'Nenhuma conta a receber neste filtro.' : 'Nenhuma conta a pagar neste filtro.', action: novo || undefined }}
      />
      {result.totais && (
        <p className="text-sm text-muted-foreground">
          Totais do filtro: valor {formatCurrency(result.totais.valor)} · quitado {formatCurrency(result.totais.quitado)} · saldo{' '}
          <span className="font-medium text-foreground">{formatCurrency(result.totais.saldo)}</span>
        </p>
      )}
      <Dialog open={loteAberto} onOpenChange={setLoteAberto}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Baixar {selecionados.size} título{selecionados.size === 1 ? '' : 's'}</DialogTitle>
            <DialogDescription>
              Cada título quita o saldo inteiro, na mesma conta e data.{tipo === 'RECEBER' ? ' Os vencidos levam a multa e os juros sugeridos.' : ''}
            </DialogDescription>
          </DialogHeader>
          <AutoForm
            schema={loteSchema}
            fields={camposLote}
            defaults={{ data: hojeIso() }}
            submitLabel="Baixar"
            isSubmitting={lote.isPending}
            serverError={lote.error}
            onCancel={() => setLoteAberto(false)}
            onSubmit={(v) =>
              lote
                .mutateAsync({ idsTitulos: [...selecionados], idConta: v.conta?.id, data: v.data, forma: v.forma })
                .then(fb.onSuccess, () => undefined)
            }
          />
        </DialogContent>
      </Dialog>
    </div>
  )
}
