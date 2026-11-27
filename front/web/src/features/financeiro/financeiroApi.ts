import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { z } from 'zod'
import type { BadgeTone } from '@/components/ui/badge'
import { api, apiBlob } from '@/lib/api'
import { fkSource } from '@/lib/fk-sources'
import type { FieldLayout } from '@/runtime/AutoForm'
import type { TableState } from '@/runtime/table-state'

/* ---------------------------------------------------------------------------
 * financeiroApi.ts: chamadas, tipos, formularios e rotulos das telas window.custom do financeiro (docs/financeiro/README.md, D2).
 * Contrato (back pronto e testado; todos exigem empresa; erros {codigo, mensagem, detalhes?.campos}):
 *   POST /financeiro/padroes                         -> {configuracaoId, criados[]}      (ADMINISTRADOR, OPERADOR; idempotente)
 *   GET  /financeiro/titulos?tipo=&filtro=&de=&ate=&idPessoa=&idCategoria=&busca=&pagina=&tamanho= -> {itens, total, pagina, tamanho, totais}
 *   GET  /financeiro/titulos/sugestoes?tipo=         -> {vencimento, observacao, formaPrevista, categoria}
 *   GET  /financeiro/titulos/resumo                  -> {receber, pagar}: {emAberto, vencido, quantidadeVencidos}
 *   GET  /financeiro/titulos/{id}                    -> TituloDto;  GET .../{id}/baixas -> [{movimento, estornado}]
 *   GET  /financeiro/titulos/{id}/sugestao-baixa?data=&quitado= -> {diasAtraso, quitado, multa, juros, total}
 *   POST /financeiro/titulos (lancar: parcelado OU recorrente) -> TituloDto[]          (ADMINISTRADOR, OPERADOR)
 *   PUT  /financeiro/titulos/{id}                    -> TituloDto                       (ADMINISTRADOR, OPERADOR)
 *   POST /financeiro/titulos/{id}/cancelar | /aprovar -> TituloDto                      (so ADMINISTRADOR)
 *   POST /financeiro/titulos/{id}/baixar             -> {movimento, titulo}             (ADMINISTRADOR, OPERADOR)
 *   POST /financeiro/titulos/baixar-em-lote          -> [{movimento, titulo}]           (ADMINISTRADOR, OPERADOR)
 *   POST /financeiro/movimentos/{id}/estornar | /avulso | /transferencia               (so ADMINISTRADOR)
 *   GET  /financeiro/extrato?idConta=&de=&ate=&pagina= | /saldos | /fluxo-de-caixa?de=&ate=   (qualquer papel)
 * Dinheiro trafega como numero com 2 casas; datas como aaaa-mm-dd. O back decide: a tela so adianta a UX.
 * ------------------------------------------------------------------------- */

export type TipoTitulo = 'RECEBER' | 'PAGAR'
export type Situacao = 'ABERTO' | 'PARCIAL' | 'PAGO' | 'CANCELADO'
export type Forma = 'DINHEIRO' | 'PIX' | 'BOLETO' | 'CARTAO' | 'TRANSFERENCIA'

export const SITUACAO: Record<Situacao, { rotulo: string; tone: BadgeTone }> = {
  ABERTO: { rotulo: 'Aberto', tone: 'info' },
  PARCIAL: { rotulo: 'Parcial', tone: 'warning' },
  PAGO: { rotulo: 'Pago', tone: 'success' },
  CANCELADO: { rotulo: 'Cancelado', tone: 'neutral' },
}

export const FORMAS: readonly Forma[] = ['DINHEIRO', 'PIX', 'BOLETO', 'CARTAO', 'TRANSFERENCIA']
export const FORMA: Record<Forma, string> = { DINHEIRO: 'Dinheiro', PIX: 'Pix', BOLETO: 'Boleto', CARTAO: 'Cartão', TRANSFERENCIA: 'Transferência' }
const OPCOES_FORMA = FORMAS.map((f) => ({ value: f, label: FORMA[f] }))

/** Filtros da referencia (§7): a mesma definicao na lista e nos totais. */
export const FILTROS = [
  { value: 'EM_ABERTO', label: 'Em aberto' },
  { value: 'PARCIAIS', label: 'Parciais' },
  { value: 'VENCIDOS', label: 'Vencidos' },
  { value: 'PAGOS', label: 'Pagos' },
  { value: 'CANCELADOS', label: 'Cancelados' },
  { value: 'PARA_REVISAO', label: 'Para revisão' },
] as const

export const TIPO: Record<TipoTitulo, { titulo: string; novo: string; pessoa: string; rotaLista: '/financeiro/receber' | '/financeiro/pagar' }> = {
  RECEBER: { titulo: 'Contas a receber', novo: 'Nova conta a receber', pessoa: 'Cliente', rotaLista: '/financeiro/receber' },
  PAGAR: { titulo: 'Contas a pagar', novo: 'Nova conta a pagar', pessoa: 'Fornecedor', rotaLista: '/financeiro/pagar' },
}

export const TIPO_MOVIMENTO: Record<string, string> = {
  RECEBIMENTO: 'Recebimento',
  PAGAMENTO: 'Pagamento',
  ENTRADA_AVULSA: 'Entrada avulsa',
  SAIDA_AVULSA: 'Saída avulsa',
  TRANSFERENCIA_ENTRADA: 'Transferência recebida',
  TRANSFERENCIA_SAIDA: 'Transferência enviada',
  ESTORNO: 'Estorno',
}

export interface CategoriaResumo {
  id: string
  nome: string
}

export interface Titulo {
  id: string
  tipo: TipoTitulo
  idPessoa?: string | null
  nomePessoa?: string | null
  descricao: string
  observacao?: string | null
  valor: number
  quitado: number
  saldo: number
  vencimento: string
  vencido: boolean
  formaPrevista?: Forma | null
  situacao: Situacao
  parcela: number
  totalParcelas: number
  idLancamento?: string | null
  origemTipo: string
  origemNumero?: string | null
  multaPercentual?: number | null
  jurosMensalPercentual?: number | null
  paraRevisao: boolean
  aprovado: boolean
  categoria?: CategoriaResumo | null
}

export interface Totais {
  valor: number
  quitado: number
  saldo: number
}

interface PaginaDeTitulos {
  itens: Titulo[]
  total: number
  totais: Totais
}

export interface Movimento {
  id: string
  tipo: string
  sentido: 'ENTRADA' | 'SAIDA'
  data: string
  descricao?: string | null
  forma?: Forma | null
  valor: number
  quitado: number
  juros: number
  multa: number
  desconto: number
  idConta?: string
  idTitulo?: string | null
  idEstornoDe?: string | null
}

export interface LinhaDoTitulo {
  movimento: Movimento
  estornado: boolean
}

export interface SugestaoDeBaixa {
  diasAtraso: number
  quitado: number
  multa: number
  juros: number
  total: number
}

export interface LinhaDoExtrato {
  id: string
  data: string
  tipo: string
  sentido: 'ENTRADA' | 'SAIDA'
  descricao?: string | null
  forma?: string | null
  valor: number
  saldoCorrente: number
  idTitulo?: string | null
  idEstornoDe?: string | null
  estornado: boolean
}

export interface Extrato {
  idConta: string
  nomeConta: string
  de: string
  ate: string
  saldoAnterior: number
  saldoFinal: number
  linhas: LinhaDoExtrato[]
  total: number
  pagina: number
  totaisPorForma: { forma: string; rotulo: string; entradas: number; saidas: number }[]
}

export interface SaldoDaConta {
  idConta: string
  nome: string
  tipo: string
  saldo: number
}

export interface MesDoFluxo {
  mes: string
  entradasPrevistas: number
  saidasPrevistas: number
  entradasRealizadas: number
  saidasRealizadas: number
  saldoPrevisto: number
  saldoRealizado: number
}

export const POR_PAGINA_EXTRATO = 15

const chaves = {
  titulos: ['financeiro', 'titulos'] as const,
  livro: ['financeiro', 'livro'] as const,
}

/** Filtros fora da tabela (periodo pelo vencimento, pessoa, categoria). */
export interface FiltrosExtras {
  de?: string
  ate?: string
  idPessoa?: string
  idCategoria?: string
}

/** Lista paginada no servidor, na forma que o DataTable espera ({data, count, ...}); `totais` sao os do filtro inteiro. */
export function useTitulos(tipo: TipoTitulo, state: TableState, extras: FiltrosExtras) {
  const filtro = state.filters.filtro || undefined
  const q = useQuery({
    queryKey: [...chaves.titulos, 'lista', tipo, state, extras],
    queryFn: () =>
      api<PaginaDeTitulos>('/financeiro/titulos', {
        query: { tipo, filtro, busca: state.search || undefined, pagina: state.page + 1, tamanho: state.pageSize, ...extras },
      }),
    placeholderData: keepPreviousData,
  })
  return {
    data: q.data?.itens,
    count: q.data?.total,
    totais: q.data?.totais,
    isLoading: q.isLoading,
    isFetching: q.isFetching,
    isError: q.isError,
    error: q.error,
    refetch: () => void q.refetch(),
  }
}

export function useResumo() {
  return useQuery({
    queryKey: [...chaves.titulos, 'resumo'],
    queryFn: () => api<Record<'receber' | 'pagar', { emAberto: number; vencido: number; quantidadeVencidos: number }>>('/financeiro/titulos/resumo'),
  })
}

export function useTitulo(id: string) {
  return useQuery({ queryKey: [...chaves.titulos, 'um', id], queryFn: () => api<Titulo>(`/financeiro/titulos/${id}`) })
}

export function useBaixasDoTitulo(id: string) {
  return useQuery({ queryKey: [...chaves.titulos, 'baixas', id], queryFn: () => api<LinhaDoTitulo[]>(`/financeiro/titulos/${id}/baixas`) })
}

export function useSugestoes(tipo: TipoTitulo) {
  return useQuery({
    queryKey: [...chaves.titulos, 'sugestoes', tipo],
    queryFn: () =>
      api<{ vencimento?: string | null; observacao?: string | null; formaPrevista?: Forma | null; categoria?: CategoriaResumo | null }>(
        '/financeiro/titulos/sugestoes',
        { query: { tipo } },
      ),
  })
}

export function sugestaoDeBaixa(id: string, data: string, quitado?: number) {
  return api<SugestaoDeBaixa>(`/financeiro/titulos/${id}/sugestao-baixa`, { query: { data, quitado } })
}

/** Sugestao para a abertura da baixa (F3-H2, §11.4): a receber ja abre com juros e multa; a pagar nao busca (devolve zero). */
export function useSugestaoInicialDeBaixa(id: string, receber: boolean, data: string, quitado: number) {
  return useQuery({
    queryKey: [...chaves.titulos, id, 'sugestao-baixa', data, quitado],
    queryFn: () => sugestaoDeBaixa(id, data, quitado),
    enabled: receber,
    staleTime: 0,
  })
}

/** Garante os padroes iniciais (conta Caixa, categorias, condicoes, configuracao). Idempotente. */
export function garantirPadroes() {
  return api<{ configuracaoId: string; criados: string[] }>('/financeiro/padroes', { method: 'POST' })
}

/** Toda escrita do financeiro invalida titulos e livro (saldos, extrato, fluxo). */
function useEscrita<TArg, TRes>(fn: (a: TArg) => Promise<TRes>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: chaves.titulos })
      void qc.invalidateQueries({ queryKey: chaves.livro })
    },
  })
}

export const useLancarTitulo = () => useEscrita((corpo: Record<string, unknown>) => api<Titulo[]>('/financeiro/titulos', { method: 'POST', body: corpo }))
export const useAtualizarTitulo = (id: string) =>
  useEscrita((corpo: Record<string, unknown>) => api<Titulo>(`/financeiro/titulos/${id}`, { method: 'PUT', body: corpo }))
export const useCancelarTitulo = (id: string) => useEscrita(() => api<Titulo>(`/financeiro/titulos/${id}/cancelar`, { method: 'POST' }))
export const useAprovarTitulo = (id: string) => useEscrita(() => api<Titulo>(`/financeiro/titulos/${id}/aprovar`, { method: 'POST' }))
export const useBaixarTitulo = (id: string) =>
  useEscrita((corpo: Record<string, unknown>) => api<{ movimento: Movimento; titulo: Titulo }>(`/financeiro/titulos/${id}/baixar`, { method: 'POST', body: corpo }))
export const useBaixarEmLote = () =>
  useEscrita((corpo: Record<string, unknown>) => api<unknown[]>('/financeiro/titulos/baixar-em-lote', { method: 'POST', body: corpo }))
/* ----- Fase 6: cobranca por e-mail (docs/financeiro/README.md 9.3) ----- */

export interface CobrancaEnviada {
  id: string
  idTitulo: string
  origem: 'MANUAL' | 'LEMBRETE'
  dia: string
  email: string
  enviadoEm: string
}

export function useCobrancasEnviadas(id: string) {
  return useQuery({
    queryKey: [...chaves.titulos, 'cobrancas-enviadas', id],
    queryFn: () => api<CobrancaEnviada[]>(`/financeiro/titulos/${id}/cobrancas-enviadas`),
  })
}
export const useEnviarCobranca = (id: string) =>
  useEscrita(() => api<CobrancaEnviada>(`/financeiro/titulos/${id}/enviar-cobranca`, { method: 'POST' }))

/* ----- Fase 7: cobranca por gateway (docs/financeiro/README.md 9.5) ----- */

export type TipoCobranca = 'BOLETO' | 'PIX' | 'LINK'
export type EstadoCobranca =
  | 'CRIANDO' | 'AGUARDANDO' | 'INCERTA' | 'PAGA' | 'PAGA_PARA_REVISAR' | 'RECUSADA' | 'CANCELADA' | 'EXPIRADA' | 'DESCARTADA'

export const TIPO_COBRANCA: Record<TipoCobranca, string> = { BOLETO: 'Boleto', PIX: 'Pix', LINK: 'Link de pagamento' }
export const ESTADO_COBRANCA: Record<EstadoCobranca, { rotulo: string; tone: BadgeTone }> = {
  CRIANDO: { rotulo: 'Gerando', tone: 'neutral' },
  AGUARDANDO: { rotulo: 'Aguardando pagamento', tone: 'info' },
  INCERTA: { rotulo: 'Confira no portal', tone: 'warning' },
  PAGA: { rotulo: 'Paga', tone: 'success' },
  PAGA_PARA_REVISAR: { rotulo: 'Paga: revisar', tone: 'warning' },
  RECUSADA: { rotulo: 'Recusada', tone: 'danger' },
  CANCELADA: { rotulo: 'Cancelada', tone: 'neutral' },
  EXPIRADA: { rotulo: 'Expirada', tone: 'neutral' },
  DESCARTADA: { rotulo: 'Descartada', tone: 'neutral' },
}

export interface Cobranca {
  id: string
  idTitulo: string
  idConta: string
  tipo: TipoCobranca
  estado: EstadoCobranca
  valor: number
  vencimento: string
  linhaDigitavel?: string | null
  codigoDeBarras?: string | null
  urlDoBoleto?: string | null
  pixCopiaECola?: string | null
  urlDoPix?: string | null
  urlDoLink?: string | null
  pagoEm?: string | null
  valorPago?: number | null
  tarifa?: number | null
  motivo?: string | null
  respostaDoProvedor?: string | null
  ultimaConsultaEm?: string | null
  dataCriacao: string
  viva: boolean
}

export function useCobrancasDoTitulo(id: string) {
  return useQuery({ queryKey: [...chaves.titulos, 'cobrancas', id], queryFn: () => api<Cobranca[]>(`/financeiro/titulos/${id}/cobrancas`) })
}
export const useGerarCobranca = (id: string) =>
  useEscrita((corpo: { tipo: TipoCobranca; vencimento?: string }) => api<Cobranca>(`/financeiro/titulos/${id}/cobrancas`, { method: 'POST', body: corpo }))
export const useAtualizarCobranca = () => useEscrita((id: string) => api<Cobranca>(`/financeiro/cobrancas/${id}/atualizar`, { method: 'POST' }))
export const useDescartarCobranca = () => useEscrita((id: string) => api<Cobranca>(`/financeiro/cobrancas/${id}/descartar`, { method: 'POST' }))

export interface CredencialCobranca {
  id: string
  idConta: string
  nomeConta: string
  provedor: string
  endpoint?: string | null
  sellerId: string
  temToken: boolean
  ativo: boolean
}

export function useCredenciaisDeCobranca() {
  return useQuery({ queryKey: ['financeiro', 'cobranca', 'credenciais'], queryFn: () => api<CredencialCobranca[]>('/financeiro/cobranca/credenciais') })
}
export function useConfigurarCredencial() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ idConta, ...corpo }: { idConta: string; provedor: string; endpoint?: string; sellerId: string; token?: string; ativo: boolean }) =>
      api<{ credencial: CredencialCobranca; urlDoWebhook?: string | null }>(`/financeiro/cobranca/credenciais/${idConta}`, { method: 'PUT', body: corpo }),
    onSuccess: () => void qc.invalidateQueries({ queryKey: ['financeiro', 'cobranca', 'credenciais'] }),
  })
}

export const useEstornar = () => useEscrita((id: string) => api<Movimento[]>(`/financeiro/movimentos/${id}/estornar`, { method: 'POST' }))
export const useLancarAvulso = () => useEscrita((corpo: Record<string, unknown>) => api<Movimento>('/financeiro/movimentos/avulso', { method: 'POST', body: corpo }))
export const useTransferir = () =>
  useEscrita((corpo: Record<string, unknown>) => api<Movimento[]>('/financeiro/movimentos/transferencia', { method: 'POST', body: corpo }))

export function useSaldos() {
  return useQuery({ queryKey: [...chaves.livro, 'saldos'], queryFn: () => api<{ contas: SaldoDaConta[]; total: number }>('/financeiro/saldos') })
}

export function useExtrato(idConta: string | undefined, de: string | undefined, ate: string | undefined, pagina: number) {
  return useQuery({
    queryKey: [...chaves.livro, 'extrato', idConta, de, ate, pagina],
    queryFn: () => api<Extrato>('/financeiro/extrato', { query: { idConta, de, ate, pagina } }),
    enabled: !!idConta,
    placeholderData: keepPreviousData,
  })
}

export function useFluxo(de: string, ate: string) {
  return useQuery({
    queryKey: [...chaves.livro, 'fluxo', de, ate],
    queryFn: () => api<MesDoFluxo[]>('/financeiro/fluxo-de-caixa', { query: { de, ate } }),
  })
}

/* ------------------------------------------------------------------------- CSV (docs/financeiro/README.md, F4-C3 e F4-C4) */

export type EntidadeCsv = 'contas' | 'categorias' | 'condicoes'

export const CSV: Record<EntidadeCsv, { titulo: string; cabecalho: string }> = {
  contas: { titulo: 'Contas', cabecalho: 'nome;tipo;codigo_banco;agencia;dados_do_banco;saldo_inicial;data_saldo_inicial;ativo' },
  categorias: { titulo: 'Categorias', cabecalho: 'nome;tipo;categoria_pai;dicas;ativo' },
  condicoes: { titulo: 'Condições de pagamento', cabecalho: 'nome;forma;parcelas;dias_primeiro_vencimento;intervalo_dias;receber_na_emissao;ativo' },
}

/** Baixa o CSV exportado (qualquer papel). */
export async function exportarCsv(entidade: EntidadeCsv): Promise<void> {
  const blob = await apiBlob(`/financeiro/csv/${entidade}`, 'text/csv')
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${entidade}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function comArquivo(arquivo: File) {
  const fd = new FormData()
  fd.append('arquivo', arquivo)
  return fd
}

export const useImportarCsv = () =>
  useEscrita(({ entidade, arquivo }: { entidade: EntidadeCsv; arquivo: File }) =>
    api<{ criados: number; ignorados: { linha: number; nome: string; motivo: string }[] }>(`/financeiro/csv/${entidade}`, {
      method: 'POST',
      body: comArquivo(arquivo),
    }),
  )

export const useImportarBancos = () =>
  useEscrita((arquivo: File) =>
    api<{ criados: number; atualizados: number; ignorados: number }>('/financeiro/csv/bancos', { method: 'POST', body: comArquivo(arquivo) }),
  )

/* ------------------------------------------------------------------------- formularios (AutoForm; o back valida de novo) */

const ref = z.looseObject({ id: z.string() })
const valor = z.number({ error: 'Informe o valor.' }).min(0.01, 'O valor deve ser de R$ 0,01 a R$ 999.999.999,99.').max(999999999.99, 'O valor deve ser de R$ 0,01 a R$ 999.999.999,99.')
const dataObrigatoria = z.string({ error: 'Informe a data.' }).min(10, 'Informe a data.')
const percentual = z.number().min(0, 'De 0 a 999,99.').max(999.99, 'De 0 a 999,99.').optional()
const formaOpcional = z.enum(FORMAS as unknown as [Forma, ...Forma[]]).optional()

/** Campos comuns a lancar e editar. */
const tituloBase = {
  descricao: z.string({ error: 'Informe a descrição.' }).trim().min(1, 'Informe a descrição.').max(255, 'No máximo 255 caracteres.'),
  valor,
  vencimento: dataObrigatoria,
  categoria: ref.nullish(),
  formaPrevista: formaOpcional,
  multaPercentual: percentual,
  jurosMensalPercentual: percentual,
  observacao: z.string().max(255, 'No máximo 255 caracteres.').optional(),
}

export const lancarSchema = z
  .object({
    ...tituloBase,
    pessoa: ref.nullish(),
    parcelas: z.number().int('Número inteiro.').min(1, 'De 1 a 48.').max(48, 'De 1 a 48.').optional(),
    intervaloDias: z.number().int('Número inteiro.').min(0, 'De 0 a 365.').max(365, 'De 0 a 365.').optional(),
    repetirMeses: z.number().int('Número inteiro.').min(1, 'De 1 a 24.').max(24, 'De 1 a 24.').optional(),
  })
  .refine((v) => !(v.repetirMeses && (v.parcelas ?? 1) > 1), { message: 'Escolha parcelado ou recorrente, não os dois.', path: ['repetirMeses'] })
export type LancarValues = z.infer<typeof lancarSchema>

export const editarSchema = z.object(tituloBase)
export type EditarValues = z.infer<typeof editarSchema>

const categoriaDo = (tipo: TipoTitulo) => fkSource(tipo === 'RECEBER' ? 'CategoriaReceita' : 'CategoriaDespesa')

function camposComuns(tipo: TipoTitulo, valorLabel: string): FieldLayout {
  return [
    [
      { name: 'descricao', label: 'Descrição', required: true, size: 'xl', maxLength: 255 },
      { name: 'valor', label: valorLabel, kind: 'currency', required: true, size: 'm' },
    ],
    [
      { name: 'vencimento', label: 'Vencimento', kind: 'date', required: true, size: 's' },
      { name: 'formaPrevista', label: 'Forma prevista', options: OPCOES_FORMA, size: 'm' },
      { name: 'categoria', label: 'Categoria', fk: categoriaDo(tipo), size: 'm', hint: 'Só categorias sem filhas.' },
    ],
  ]
}

export function camposLancar(tipo: TipoTitulo): FieldLayout {
  return [
    { section: 'Título' },
    [{ name: 'pessoa', label: TIPO[tipo].pessoa, fk: fkSource(tipo === 'RECEBER' ? 'Cliente' : 'Fornecedor'), size: 'full' }],
    ...camposComuns(tipo, 'Valor total'),
    { section: 'Parcelado ou recorrente', description: 'Parcelado divide o valor total; recorrente repete o mesmo valor todo mês. Escolha um dos dois.' },
    [
      { name: 'parcelas', label: 'Parcelas', kind: 'number', size: 'm', hint: 'De 1 a 48. O resto dos centavos vai na última.' },
      { name: 'intervaloDias', label: 'Dias entre parcelas', kind: 'number', size: 'm', hint: 'Padrão 30.' },
      { name: 'repetirMeses', label: 'Repetir por meses', kind: 'number', size: 'm', hint: 'Recorrente: de 1 a 24.' },
    ],
    { section: 'Multa, juros e observação', collapsible: true },
    [
      { name: 'multaPercentual', label: 'Multa própria (%)', kind: 'number', size: 'm', hint: 'Vazio = a da configuração.' },
      { name: 'jurosMensalPercentual', label: 'Juros ao mês próprios (%)', kind: 'number', size: 'm', hint: 'Vazio = os da configuração.' },
      { name: 'observacao', label: 'Observação', size: 'm', maxLength: 255, hint: 'Instruções de pagamento.' },
    ],
  ]
}

export function camposEditar(tipo: TipoTitulo, valorEditavel: boolean): FieldLayout {
  const comuns = camposComuns(tipo, 'Valor')
  const linha0 = comuns[0] as { name: string; readOnly?: boolean; hint?: string }[]
  linha0[1] = { ...linha0[1], readOnly: !valorEditavel, hint: valorEditavel ? undefined : 'O valor só muda em título lançado à mão e sem baixa.' }
  return [
    { section: 'Título' },
    ...comuns,
    [
      { name: 'multaPercentual', label: 'Multa própria (%)', kind: 'number', size: 'm' },
      { name: 'jurosMensalPercentual', label: 'Juros ao mês próprios (%)', kind: 'number', size: 'm' },
      { name: 'observacao', label: 'Observação', size: 'm', maxLength: 255 },
    ],
  ]
}

export const baixaSchema = z.object({
  conta: ref.nullish().refine((v) => !!v, 'Escolha a conta.'),
  data: dataObrigatoria,
  forma: z.enum(FORMAS as unknown as [Forma, ...Forma[]], { error: 'Escolha a forma.' }),
  quitado: valor,
  juros: z.number().min(0, 'Não pode ser negativo.').optional(),
  multa: z.number().min(0, 'Não pode ser negativo.').optional(),
  desconto: z.number().min(0, 'Não pode ser negativo.').optional(),
  observacao: z.string().max(255, 'No máximo 255 caracteres.').optional(),
})
export type BaixaValues = z.infer<typeof baixaSchema>

export const camposBaixa: FieldLayout = [
  [
    { name: 'conta', label: 'Conta', fk: fkSource('ContaFinanceira'), required: true, size: 'l' },
    { name: 'data', label: 'Data', kind: 'date', required: true, size: 's' },
    { name: 'forma', label: 'Forma', options: OPCOES_FORMA, required: true, size: 's' },
  ],
  [
    { name: 'quitado', label: 'Valor quitado', kind: 'currency', required: true, size: 's' },
    { name: 'juros', label: 'Juros', kind: 'currency', size: 's' },
    { name: 'multa', label: 'Multa', kind: 'currency', size: 's' },
    { name: 'desconto', label: 'Desconto', kind: 'currency', size: 's' },
  ],
  [{ name: 'observacao', label: 'Observação', size: 'full', maxLength: 255, hint: 'Vira a descrição da linha no extrato.' }],
]

export const loteSchema = z.object({
  conta: ref.nullish().refine((v) => !!v, 'Escolha a conta.'),
  data: dataObrigatoria,
  forma: z.enum(FORMAS as unknown as [Forma, ...Forma[]], { error: 'Escolha a forma.' }),
})
export type LoteValues = z.infer<typeof loteSchema>

export const camposLote: FieldLayout = [
  [
    { name: 'conta', label: 'Conta', fk: fkSource('ContaFinanceira'), required: true, size: 'l' },
    { name: 'data', label: 'Data', kind: 'date', required: true, size: 's' },
    { name: 'forma', label: 'Forma', options: OPCOES_FORMA, required: true, size: 's' },
  ],
]

export const avulsoSchema = z.object({
  conta: ref.nullish().refine((v) => !!v, 'Escolha a conta.'),
  sentido: z.enum(['ENTRADA', 'SAIDA'], { error: 'Escolha entrada ou saída.' }),
  valor,
  data: dataObrigatoria,
  forma: formaOpcional,
  categoria: ref.nullish(),
  descricao: z.string().max(255, 'No máximo 255 caracteres.').optional(),
})
export type AvulsoValues = z.infer<typeof avulsoSchema>

export function camposAvulso(sentido: 'ENTRADA' | 'SAIDA' | undefined): FieldLayout {
  return [
    [
      { name: 'conta', label: 'Conta', fk: fkSource('ContaFinanceira'), required: true, size: 'l' },
      { name: 'sentido', label: 'Entrada ou saída', options: [{ value: 'ENTRADA', label: 'Entrada' }, { value: 'SAIDA', label: 'Saída' }], required: true, size: 'l' },
    ],
    [
      { name: 'valor', label: 'Valor', kind: 'currency', required: true, size: 'm' },
      { name: 'data', label: 'Data', kind: 'date', required: true, size: 'm' },
      { name: 'forma', label: 'Forma', options: OPCOES_FORMA, size: 'm' },
    ],
    [
      { name: 'categoria', label: 'Categoria', fk: fkSource(sentido === 'SAIDA' ? 'CategoriaDespesa' : 'CategoriaReceita'), size: 'l' },
      { name: 'descricao', label: 'Descrição', size: 'l', maxLength: 255 },
    ],
  ]
}

export const transferenciaSchema = z
  .object({
    origem: ref.nullish().refine((v) => !!v, 'Escolha a conta de origem.'),
    destino: ref.nullish().refine((v) => !!v, 'Escolha a conta de destino.'),
    valor,
    data: dataObrigatoria,
    descricao: z.string().max(255, 'No máximo 255 caracteres.').optional(),
  })
  .refine((v) => !v.origem || !v.destino || v.origem.id !== v.destino.id, {
    message: 'A conta de destino precisa ser diferente da de origem.',
    path: ['destino'],
  })
export type TransferenciaValues = z.infer<typeof transferenciaSchema>

export const camposTransferencia: FieldLayout = [
  [
    { name: 'origem', label: 'Conta de origem', fk: fkSource('ContaFinanceira'), required: true, size: 'l' },
    { name: 'destino', label: 'Conta de destino', fk: fkSource('ContaFinanceira'), required: true, size: 'l' },
  ],
  [
    { name: 'valor', label: 'Valor', kind: 'currency', required: true, size: 's' },
    { name: 'data', label: 'Data', kind: 'date', required: true, size: 's' },
    { name: 'descricao', label: 'Descrição', size: 'l', maxLength: 255 },
  ],
]

/** Hoje no fuso do modulo (America/Sao_Paulo), aaaa-mm-dd. */
export function hojeIso(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date())
}
