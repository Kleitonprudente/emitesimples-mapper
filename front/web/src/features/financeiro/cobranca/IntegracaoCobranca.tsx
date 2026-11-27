import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldDescription, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Switch } from '@/components/ui/switch'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { api } from '@/lib/api'
import { errorMessage, notifySuccess } from '@/runtime/feedback'
import { EmptyState, ErrorState, LoadingState } from '@/runtime/states'
import { useConfigurarCredencial, useCredenciaisDeCobranca } from '@/features/financeiro/financeiroApi'

/* ---------------------------------------------------------------------------
 * Financeiro > Integracao de cobranca (window.custom IntegracaoCobranca, rota /financeiro/cobranca; docs/financeiro/README.md 9.5).
 * Liga uma conta a um provedor de cobranca. O token e o segredo do webhook vao para o cofre de segredos e nunca voltam a tela; a URL
 * do webhook aparece UMA vez, quando a integracao e criada. Por ora so o provedor de teste (FALSO).
 * ------------------------------------------------------------------------- */

const PROVEDORES: Record<string, string> = { FALSO: 'Provedor de teste (sem dinheiro de verdade)' }

export function IntegracaoCobrancaScreen() {
  const credenciais = useCredenciaisDeCobranca()
  const contas = useQuery({
    queryKey: ['ContaFinanceira', 'ativas-para-cobranca'],
    queryFn: () => api<{ id: string; nome: string; ativo?: boolean }[]>('/ContaFinanceira'),
  })
  const configurar = useConfigurarCredencial()
  const [idConta, setIdConta] = useState('')
  const [provedor, setProvedor] = useState('FALSO')
  const [sellerId, setSellerId] = useState('')
  const [endpoint, setEndpoint] = useState('')
  const [token, setToken] = useState('')
  const [ativo, setAtivo] = useState(true)
  const [webhook, setWebhook] = useState<string | null>(null)

  const escolher = (id: string) => {
    setIdConta(id)
    const c = credenciais.data?.find((x) => x.idConta === id)
    setProvedor(c?.provedor ?? 'FALSO')
    setSellerId(c?.sellerId ?? '')
    setEndpoint(c?.endpoint ?? '')
    setAtivo(c?.ativo ?? true)
    setToken('')
    setWebhook(null)
  }

  const salvar = () =>
    void configurar
      .mutateAsync({ idConta, provedor, sellerId, endpoint: endpoint || undefined, token: token || undefined, ativo })
      .then((r) => {
        setToken('')
        setWebhook(r.urlDoWebhook ? `${window.location.origin}${r.urlDoWebhook}` : null)
        notifySuccess('Integração salva.')
      }, () => undefined)

  if (credenciais.isLoading || contas.isLoading) return <LoadingState />
  if (credenciais.isError) return <ErrorState error={credenciais.error} onRetry={() => void credenciais.refetch()} />
  const ativas = (contas.data ?? []).filter((c) => c.ativo !== false)

  return (
    <div className="flex max-w-5xl flex-col gap-4">
      <h1 className="text-2xl font-semibold">Integração de cobrança</h1>
      <Card>
        <CardHeader>
          <CardTitle>Contas integradas</CardTitle>
          <CardDescription>Cada conta pode ter uma integração. O dinheiro das cobranças cai na conta integrada.</CardDescription>
        </CardHeader>
        <CardContent>
          {!credenciais.data?.length ? (
            <EmptyState title="Nenhuma conta integrada." description="Escolha uma conta abaixo e informe os dados do provedor." />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Conta</TableHead>
                  <TableHead>Provedor</TableHead>
                  <TableHead>Identificação</TableHead>
                  <TableHead>Token</TableHead>
                  <TableHead>Situação</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {credenciais.data.map((c) => (
                  <TableRow key={c.id}>
                    <TableCell>{c.nomeConta}</TableCell>
                    <TableCell>{PROVEDORES[c.provedor] ?? c.provedor}</TableCell>
                    <TableCell>{c.sellerId}</TableCell>
                    <TableCell>{c.temToken ? 'Guardado' : 'Não informado'}</TableCell>
                    <TableCell>
                      <Badge tone={c.ativo ? 'success' : 'neutral'}>{c.ativo ? 'Ativa' : 'Desligada'}</Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>Configurar integração</CardTitle>
          <CardDescription>O token não volta para a tela depois de salvo. Em branco, o token guardado continua valendo.</CardDescription>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-12">
          <Field className="md:col-span-6">
            <FieldLabel htmlFor="cob-conta">Conta</FieldLabel>
            <Select value={idConta} onValueChange={escolher}>
              <SelectTrigger id="cob-conta">
                <SelectValue placeholder="Escolha a conta" />
              </SelectTrigger>
              <SelectContent>
                {ativas.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.nome}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field className="md:col-span-6">
            <FieldLabel htmlFor="cob-provedor">Provedor</FieldLabel>
            <Select value={provedor} onValueChange={setProvedor}>
              <SelectTrigger id="cob-provedor">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {Object.entries(PROVEDORES).map(([k, v]) => (
                  <SelectItem key={k} value={k}>
                    {v}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </Field>
          <Field className="md:col-span-6">
            <FieldLabel htmlFor="cob-seller">Identificação no provedor</FieldLabel>
            <Input id="cob-seller" maxLength={100} value={sellerId} onChange={(e) => setSellerId(e.target.value)} />
            <FieldDescription>A carteira ou conta no provedor. Única no sistema: o dinheiro nunca cai na conta errada.</FieldDescription>
          </Field>
          <Field className="md:col-span-6">
            <FieldLabel htmlFor="cob-endpoint">Endereço da API (opcional)</FieldLabel>
            <Input id="cob-endpoint" maxLength={255} placeholder="https://" value={endpoint} onChange={(e) => setEndpoint(e.target.value)} />
          </Field>
          <Field className="md:col-span-8">
            <FieldLabel htmlFor="cob-token">Token da API</FieldLabel>
            <Input id="cob-token" type="password" autoComplete="off" value={token} onChange={(e) => setToken(e.target.value)} />
          </Field>
          <Field orientation="horizontal" className="md:col-span-4 md:self-end">
            <Switch id="cob-ativo" checked={ativo} onCheckedChange={setAtivo} />
            <FieldLabel htmlFor="cob-ativo">Integração ligada</FieldLabel>
          </Field>
          {configurar.isError && (
            <Alert variant="destructive" className="md:col-span-12">
              <AlertTitle>Não foi possível salvar</AlertTitle>
              <AlertDescription>{errorMessage(configurar.error)}</AlertDescription>
            </Alert>
          )}
          {webhook && (
            <Alert variant="success" className="md:col-span-12">
              <AlertTitle>Endereço do webhook (aparece só agora)</AlertTitle>
              <AlertDescription className="break-all">Cadastre no portal do provedor: {webhook}</AlertDescription>
            </Alert>
          )}
          <div className="md:col-span-12">
            <Button disabled={!idConta || !sellerId.trim() || configurar.isPending} onClick={salvar}>
              {configurar.isPending ? 'Salvando…' : 'Salvar integração'}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
