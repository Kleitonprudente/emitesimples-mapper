import type { PapelValor } from '@/lib/api'
import { erro, json } from '@/mocks/http'
import { buildPessoas } from '@/mocks/fixtures'
import { alocarNumeroNfseMock, nfseConfigMock } from '@/mocks/nfse-config'

/* ---------------------------------------------------------------------------
 * Mock de Notas Fiscais > NFS-e (contrato em features/notas/notasApi.ts). Todas as rotas exigem empresa na sessao (senao 403).
 *   GET  /nfse/notas/{from}/{to}?estado=&busca=   (qualquer papel) mais recentes primeiro, ate 100
 *   GET  /nfse/notas/count?estado=&busca=         -> {count}
 *   GET  /nfse/notas/{id}                         -> detalhe
 *   POST /nfse/notas                              (operador+) {pessoaId, servicoId, valorServicos, informacoesComplementares?, discriminacao?}
 *   POST /nfse/notas/{id}/enviar                  (operador+) corpo opcional; reenvia mantendo o numero
 *   POST /nfse/notas/{id}/atualizar               (operador+) consulta o gateway agora (tambem conclui um cancelamento processando na 2a chamada)
 *   POST /nfse/notas/{id}/cancelar                (operador+) {codigoMotivo: 1|2|9, motivo 15..255 apos trim} -> detalhe. 400 requisicao_invalida;
 *        409 nota_nao_autorizada | cancelamento_em_andamento | cancelamento_fora_do_mes (mes da emissao != mes atual, fuso America/Sao_Paulo).
 *        Sucesso: estado cancelada com `cancelamento` concluido; valorServicos 888.88 fica pendente (cancelamento.estado processando).
 *   GET  /nfse/notas/{id}/pdf                     -> application/pdf (404 pdf_indisponivel)
 * Delegacao: cancelar (e aprovar cancelamento) exige `ctx.podeCancelarNfse` (administrador sempre; operador so delegado): 403 cancelamento_nao_delegado.
 * Assistente (a UI nao tem botao de preparar; os testes chamam estas rotas):
 *   POST /nfse/notas/preparar (operador+) mesmo corpo de criar -> detalhe `rascunho` (SEM numero/serie/dataEmissao: so a aprovacao aloca; NAO envia).
 *   POST /nfse/notas/{id}/aprovar (operador+) corpo opcional como /enviar -> processando (ou rejeitada/em_conflito pelos valores magicos). 409 nota_nao_editavel se nao for rascunho.
 *   POST /nfse/notas/{id}/descartar (operador+) -> descartada. 409 nota_nao_editavel se nao for rascunho.
 *   POST /nfse/notas/{id}/cancelamentos/preparar (operador+) {codigoMotivo, motivo} -> detalhe com cancelamento.estado 'RASCUNHO'; mesmas regras de cancelar
 *        (400, 409 nota_nao_autorizada | cancelamento_em_andamento | cancelamento_fora_do_mes), sem exigir delegacao.
 *   POST /nfse/cancelamentos/{id}/aprovar (operador+, delegado) -> reavalia o mes vigente (409 cancelamento_fora_do_mes) e envia; /descartar -> 'DESCARTADO'.
 * Estados novos: `falha_envio` (reenviavel como rejeitada, mesmo numero; seed nota-0007) e `rascunho` (so do assistente; seed nota-0006).
 * E, para as FKs da tela, o cadastro de Servico (so leitura): GET /Servico, /Servico/{from}/{to}, /Servico/count, /Servico/id/{id}.
 *
 * Estado em memoria. Pre-condicoes do POST/enviar (409): nfse_nao_habilitada, emitente_nao_cadastrado, certificado_nao_valido,
 * servico_inativo; 422 tomador_incompleto quando o cliente nao tem `documento`. O numero vem da serie da configuracao da NFS-e
 * (src/mocks/nfse-config.ts). Cenarios por valor magico: valorServicos 666.66 -> rejeitada (com errosRetorno); 777.77 -> em_conflito.
 * Demais notas ficam `processando` e viram `autorizada` (com PDF) apos TEMPO_AUTORIZACAO_MS ou na 2a chamada de /atualizar.
 * ------------------------------------------------------------------------- */

export const TEMPO_AUTORIZACAO_MS = 8000
export const VALOR_REJEICAO = 666.66
export const VALOR_CONFLITO = 777.77
export const VALOR_CANCELAMENTO_PENDENTE = 888.88

export type EstadoNota = 'RASCUNHO' | 'FALHA_ENVIO' | 'PROCESSANDO' | 'EMITIDA_OFFLINE' | 'AUTORIZADA' | 'REJEITADA' | 'EM_CONFLITO' | 'CANCELADA' | 'DESCARTADA'

interface CancelamentoMock {
  id: string
  estado: 'RASCUNHO' | 'PROCESSANDO' | 'CONCLUIDO' | 'RECUSADO' | 'EXPIRADO' | 'DESCARTADO'
  codigoMotivo: number
  motivo: string
  codigoRetorno: string | null
  mensagemRetorno: string | null
  dataHoraEvento: string | null
  solicitadoEm: string
}

interface Nota {
  id: string
  numero: number
  serie: string
  estado: EstadoNota
  dataEmissao: string
  valorServicos: number
  pessoa: { id: string; nome: string; documento: string }
  servico: { id: string; nome: string }
  chave: string | null
  numeroNfse: string | null
  codigoRetorno: string | null
  mensagemRetorno: string | null
  temPdf: boolean
  dataCriacao: string
  informacoesComplementares: string | null
  discriminacao: string
  descricao: string | null
  codigoServico: string
  errosRetorno: { campo: string; erro: string; descricao: string }[] | null
  linkPdf: string | null
  codigoVerificacao: string | null
  dataHoraEvento: string | null
  cancelamento: CancelamentoMock | null
  /** Interno: instante (ms) em que foi enviada e quantas consultas ja fez. */
  enviadaEm: number
  consultas: number
}

export interface ServicoMock {
  id: string
  nome: string
  ativo: boolean
  codigoServico: string
  discriminacaoPadrao: string | null
}

const SERVICOS = (): ServicoMock[] => [
  { id: 'srv-0001', nome: 'Consultoria contábil', ativo: true, codigoServico: '170101', discriminacaoPadrao: 'Serviços de consultoria contábil prestados no período.' },
  { id: 'srv-0002', nome: 'Desenvolvimento de software', ativo: true, codigoServico: '010101', discriminacaoPadrao: 'Desenvolvimento e manutenção de software sob demanda.' },
  { id: 'srv-0003', nome: 'Treinamento corporativo', ativo: true, codigoServico: '080201', discriminacaoPadrao: null },
  { id: 'srv-0004', nome: 'Serviço descontinuado', ativo: false, codigoServico: '990101', discriminacaoPadrao: 'Serviço que não é mais prestado.' },
]

const agoraIso = () => new Date().toISOString().slice(0, 19)

function base(i: number, parcial: Partial<Nota>): Nota {
  const p = buildPessoas()[i]!
  const s = SERVICOS()[i % 2]!
  return {
    id: `nota-${String(i + 1).padStart(4, '0')}`,
    numero: i + 1,
    serie: '9',
    estado: 'AUTORIZADA',
    dataEmissao: `2026-11-${String(10 + i).padStart(2, '0')}T14:00:00`,
    valorServicos: 150 * (i + 1),
    pessoa: { id: p.id, nome: p.nome, documento: p.documento },
    servico: { id: s.id, nome: s.nome },
    chave: null,
    numeroNfse: null,
    codigoRetorno: null,
    mensagemRetorno: null,
    temPdf: false,
    dataCriacao: `2026-11-${String(10 + i).padStart(2, '0')}T13:58:00`,
    informacoesComplementares: null,
    discriminacao: s.discriminacaoPadrao ?? 'Serviço prestado.',
    descricao: null,
    codigoServico: s.codigoServico,
    errosRetorno: null,
    linkPdf: null,
    codigoVerificacao: null,
    dataHoraEvento: null,
    cancelamento: null,
    enviadaEm: 0,
    consultas: 0,
    ...parcial,
  }
}

const autorizadaCom = (n: number): Partial<Nota> => ({
  estado: 'AUTORIZADA',
  numeroNfse: String(2026000 + n),
  chave: `4108809${String(n).padStart(37, '0')}`,
  codigoVerificacao: `ABCD${1000 + n}`,
  temPdf: true,
  dataHoraEvento: '2026-11-10T14:00:30',
})

function mesAnterior(): string {
  const d = new Date()
  d.setDate(1)
  d.setMonth(d.getMonth() - 1)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-15T14:00:00`
}

const sementes = (): Nota[] => [
  base(0, autorizadaCom(1)),
  // Emitida no mes anterior: serve para ver o aviso "fora do mes vigente" no cancelamento.
  base(1, { ...autorizadaCom(2), dataEmissao: mesAnterior() }),
  base(2, {
    estado: 'REJEITADA',
    codigoRetorno: 'E160',
    mensagemRetorno: 'A prefeitura rejeitou a nota.',
    errosRetorno: [{ campo: 'tomador.endereco.cep', erro: 'E160', descricao: 'CEP do tomador inválido para o município.' }],
  }),
  base(3, { estado: 'EM_CONFLITO', codigoRetorno: 'E999', mensagemRetorno: 'Já existe uma nota com este número na prefeitura.' }),
  base(4, { estado: 'CANCELADA', numeroNfse: '2026005', temPdf: true }),
  base(5, { estado: 'RASCUNHO' }),
  base(6, { estado: 'FALHA_ENVIO', mensagemRetorno: 'Falha de comunicação com o gateway. Reenvie a nota.' }),
]

let notas: Nota[] = sementes()
let seq = notas.length
let seqCancelamento = 0

export function resetNotas(): void {
  notas = sementes()
  seq = notas.length
  seqCancelamento = 0
}

/* ----- PDF minimo e valido (uma pagina com texto) ----- */

export function pdfMinimo(texto: string): Uint8Array {
  const conteudo = `BT /F1 18 Tf 72 720 Td (${texto.replace(/[()\\]/g, '')}) Tj ET`
  const objs = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
    `<< /Length ${conteudo.length} >>\nstream\n${conteudo}\nendstream`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
  ]
  let out = '%PDF-1.4\n'
  const offsets: number[] = []
  objs.forEach((o, i) => {
    offsets.push(out.length)
    out += `${i + 1} 0 obj\n${o}\nendobj\n`
  })
  const xref = out.length
  out += `xref\n0 ${objs.length + 1}\n0000000000 65535 f \n${offsets.map((o) => `${String(o).padStart(10, '0')} 00000 n \n`).join('')}`
  out += `trailer\n<< /Size ${objs.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  return new TextEncoder().encode(out)
}

/* ----- regras ----- */

const normaliza = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

function autorizar(n: Nota): void {
  Object.assign(n, autorizadaCom(n.numero), { dataHoraEvento: agoraIso(), mensagemRetorno: null, codigoRetorno: null, errosRetorno: null })
}

/** Simula o webhook da prefeitura: passado o tempo, a nota processando e autorizada. */
function progredir(n: Nota): void {
  if (n.estado === 'PROCESSANDO' && n.enviadaEm > 0 && Date.now() - n.enviadaEm >= TEMPO_AUTORIZACAO_MS) autorizar(n)
}

const cancelando = (n: Nota) => n.cancelamento?.estado === 'PROCESSANDO'
const cancelamentoRascunho = (n: Nota) => n.cancelamento?.estado === 'RASCUNHO'
const podeAtualizar = (n: Nota) => n.estado === 'PROCESSANDO' || n.estado === 'EMITIDA_OFFLINE' || n.estado === 'EM_CONFLITO' || cancelando(n)
const anoMes = (d: Date) => {
  const p = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo', year: 'numeric', month: '2-digit' }).formatToParts(d)
  return `${p.find((x) => x.type === 'year')?.value}-${p.find((x) => x.type === 'month')?.value}`
}
/** dataEmissao sem fuso e horario local de Sao Paulo. */
const noMesVigente = (n: Nota) => n.dataEmissao.slice(0, 7) === anoMes(new Date())
const podeCancelar = (n: Nota, delegado: boolean) => delegado && n.estado === 'AUTORIZADA' && !cancelando(n) && !cancelamentoRascunho(n) && noMesVigente(n)
function concluirCancelamento(n: Nota): void {
  if (!n.cancelamento) return
  n.estado = 'CANCELADA'
  n.cancelamento = { ...n.cancelamento, estado: 'CONCLUIDO', codigoRetorno: '101', mensagemRetorno: 'Cancelamento homologado pela prefeitura.', dataHoraEvento: agoraIso() }
}

const podeReenviar = (n: Nota) => n.estado === 'FALHA_ENVIO' || n.estado === 'REJEITADA'

function item(n: Nota) {
  return {
    id: n.id,
    numero: n.numero,
    serie: n.serie,
    estado: n.estado,
    dataEmissao: n.dataEmissao,
    valorServicos: n.valorServicos,
    pessoa: n.pessoa,
    servico: n.servico,
    chave: n.chave,
    numeroNfse: n.numeroNfse,
    codigoRetorno: n.codigoRetorno,
    mensagemRetorno: n.mensagemRetorno,
    temPdf: n.temPdf,
    dataCriacao: n.dataCriacao,
  }
}

interface Perfil {
  /** operador ou administrador */
  escreve: boolean
  delegado: boolean
}
const SEM_PERFIL: Perfil = { escreve: true, delegado: true }

function detalhe(n: Nota, perfil: Perfil = SEM_PERFIL) {
  const c = n.cancelamento
  return {
    ...item(n),
    informacoesComplementares: n.informacoesComplementares,
    discriminacao: n.discriminacao,
    descricao: n.descricao,
    codigoServico: n.codigoServico,
    errosRetorno: n.errosRetorno,
    linkPdf: n.linkPdf,
    codigoVerificacao: n.codigoVerificacao,
    dataHoraEvento: n.dataHoraEvento,
    editavel: podeReenviar(n),
    podeReenviar: podeReenviar(n),
    podeAtualizar: podeAtualizar(n),
    cancelamento: c ? { ...c, podeAprovar: c.estado === 'RASCUNHO' && perfil.escreve && perfil.delegado, podeDescartar: c.estado === 'RASCUNHO' && perfil.escreve } : null,
    podeCancelar: perfil.escreve && podeCancelar(n, perfil.delegado),
    podeAprovar: n.estado === 'RASCUNHO' && perfil.escreve,
    podeDescartar: n.estado === 'RASCUNHO' && perfil.escreve,
  }
}

type Q = Record<string, string | number | boolean | null | undefined>

function filtradas(q: Q): Nota[] {
  notas.forEach(progredir)
  const estado = q.estado ? String(q.estado) : ''
  const busca = q.busca ? normaliza(String(q.busca).trim()) : ''
  return notas
    .filter((n) => (!estado || n.estado === estado) && (!busca || `${n.serie}-${n.numero}`.includes(busca) || String(n.numero) === busca || normaliza(n.pessoa.nome).includes(busca)))
    .sort((a, b) => b.dataCriacao.localeCompare(a.dataCriacao) || b.id.localeCompare(a.id))
}

interface Ctx {
  papel: PapelValor | null
  empresaId: string | null
  /** Permissao efetiva de cancelar (administrador sempre; operador so delegado). Ausente = so administrador. */
  podeCancelarNfse?: boolean
  pessoa: (id: string) => { id: string; nome: string; documento?: string } | undefined
}

interface Corpo {
  pessoaId?: unknown
  servicoId?: unknown
  valorServicos?: unknown
  informacoesComplementares?: unknown
  discriminacao?: unknown
}

const texto = (v: unknown): string | undefined => (typeof v === 'string' && v.trim() !== '' ? v.trim() : undefined)

/** Valida e aplica o corpo (criar/enviar) sobre `alvo`; devolve uma Response de erro ou null. */
function aplicar(alvo: Nota, b: Corpo, ctx: Ctx, criando: boolean): Response | null {
  const campos: Record<string, string> = {}
  if (criando || b.pessoaId !== undefined) if (typeof b.pessoaId !== 'string' || !b.pessoaId) campos.pessoaId = 'Informe o cliente.'
  if (criando || b.servicoId !== undefined) if (typeof b.servicoId !== 'string' || !b.servicoId) campos.servicoId = 'Informe o serviço.'
  if (criando || b.valorServicos !== undefined) if (typeof b.valorServicos !== 'number' || !Number.isFinite(b.valorServicos) || b.valorServicos < 0.01) campos.valorServicos = 'O valor deve ser de pelo menos R$ 0,01.'
  if (b.discriminacao !== undefined && b.discriminacao !== null && typeof b.discriminacao === 'string' && b.discriminacao.length > 2000) campos.discriminacao = 'No máximo 2000 caracteres.'
  if (Object.keys(campos).length > 0) return erro(400, 'requisicao_invalida', 'Dados inválidos.', { campos })

  // pre-condicoes da empresa
  const cfg = nfseConfigMock()
  if (!cfg.habilitada) return erro(409, 'nfse_nao_habilitada', 'A emissão de NFS-e não está habilitada.')
  if (cfg.statusCadastro !== 'CADASTRADO') return erro(409, 'emitente_nao_cadastrado', 'A empresa não está cadastrada como emitente.')
  if (cfg.certificadoSituacao !== 'VALIDO') return erro(409, 'certificado_nao_valido', 'O certificado digital não está válido.')

  const pessoaId = typeof b.pessoaId === 'string' ? b.pessoaId : alvo.pessoa.id
  if (pessoaId !== alvo.pessoa.id || criando) {
    const p = ctx.pessoa(pessoaId)
    if (!p) return erro(404, 'nao_encontrado', 'Cliente não encontrado.')
    alvo.pessoa = { id: p.id, nome: p.nome, documento: p.documento ?? '' }
  }
  const servicoId = typeof b.servicoId === 'string' ? b.servicoId : alvo.servico.id
  if (servicoId !== alvo.servico.id || criando) {
    const s = SERVICOS().find((x) => x.id === servicoId)
    if (!s) return erro(404, 'nao_encontrado', 'Serviço não encontrado.')
    if (!s.ativo) return erro(409, 'servico_inativo', 'O serviço está inativo.')
    alvo.servico = { id: s.id, nome: s.nome }
    alvo.codigoServico = s.codigoServico
    if (criando && !texto(b.discriminacao)) alvo.discriminacao = s.discriminacaoPadrao ?? ''
  }
  if (typeof b.valorServicos === 'number') alvo.valorServicos = b.valorServicos
  if (b.discriminacao !== undefined) alvo.discriminacao = texto(b.discriminacao) ?? ''
  if (b.informacoesComplementares !== undefined) alvo.informacoesComplementares = texto(b.informacoesComplementares) ?? null
  if (!alvo.discriminacao) return erro(400, 'discriminacao_obrigatoria', 'Informe a discriminação do serviço.')
  if (!alvo.pessoa.documento) {
    return erro(422, 'tomador_incompleto', 'O cadastro do cliente está incompleto.', { campos: [{ campo: 'documento', mensagem: 'Informe o CPF ou CNPJ do cliente.' }] })
  }
  return null
}

/** Envia ao "gateway": define o resultado conforme o valor. */
function enviar(n: Nota): void {
  n.enviadaEm = Date.now()
  n.consultas = 0
  n.dataEmissao = agoraIso()
  n.errosRetorno = null
  n.codigoRetorno = null
  n.mensagemRetorno = null
  if (n.valorServicos === VALOR_REJEICAO) {
    n.estado = 'REJEITADA'
    n.codigoRetorno = 'E160'
    n.mensagemRetorno = 'A prefeitura rejeitou a nota.'
    n.errosRetorno = [
      { campo: 'valorServicos', erro: 'E160', descricao: 'O valor informado não é aceito para este serviço.' },
      { campo: 'tomador.endereco.cep', erro: 'E161', descricao: 'CEP do tomador inválido para o município.' },
    ]
  } else if (n.valorServicos === VALOR_CONFLITO) {
    n.estado = 'EM_CONFLITO'
    n.codigoRetorno = 'E999'
    n.mensagemRetorno = 'Já existe uma nota com este número na prefeitura.'
  } else {
    n.estado = 'PROCESSANDO'
  }
}

export function notasRoutes(req: { method: string; path: string; query?: Q; body?: unknown }, ctx: Ctx): Response | null {
  const { method, path } = req
  const q = req.query ?? {}

  // Cadastro de Servico (so leitura), para o Combobox da nota.
  if (path === '/Servico' || path === '/Servico/count' || /^\/Servico\/\d+\/\d+$/.test(path)) {
    if (method !== 'GET') return null
    let rows = SERVICOS()
    if (q.ativo !== undefined && q.ativo !== '') rows = rows.filter((s) => String(s.ativo) === String(q.ativo))
    if (q.nome) {
      const re = new RegExp(`^${normaliza(String(q.nome)).replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*')}$`)
      rows = rows.filter((s) => re.test(normaliza(s.nome)))
    }
    rows.sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
    if (path === '/Servico/count') return json(200, { count: rows.length })
    const r = /^\/Servico\/(\d+)\/(\d+)$/.exec(path)
    return json(200, r ? rows.slice(Number(r[1]), Number(r[2]) + 1) : rows.slice(0, 100))
  }
  const servicoPorId = /^\/Servico\/id\/([^/]+)$/.exec(path)
  if (servicoPorId && method === 'GET') {
    const s = SERVICOS().find((x) => x.id === servicoPorId[1])
    return s ? json(200, s) : erro(404, 'nao_encontrado', 'Registro não encontrado.')
  }

  if (!path.startsWith('/nfse/notas') && !path.startsWith('/nfse/cancelamentos')) return null
  if (!ctx.empresaId || !ctx.papel) return erro(403, 'acesso_negado', 'Acesso negado.')
  const escreve = ctx.papel === 'OPERADOR' || ctx.papel === 'ADMINISTRADOR'
  const delegado = ctx.papel === 'ADMINISTRADOR' || ctx.podeCancelarNfse === true
  const perfil: Perfil = { escreve, delegado }
  const naoDelegado = () => erro(403, 'cancelamento_nao_delegado', 'Seu usuário não tem permissão para cancelar notas. Peça ao administrador.')
  const negado = () => erro(403, 'acesso_negado', 'Você não tem permissão para emitir ou reenviar notas.')

  if (path === '/nfse/notas' && method === 'POST') {
    if (!escreve) return negado()
    const nova: Nota = base(0, { id: `nota-${String(++seq).padStart(4, '0')}`, estado: 'RASCUNHO', pessoa: { id: '', nome: '', documento: '' }, servico: { id: '', nome: '' }, discriminacao: '' })
    const falha = aplicar(nova, (req.body ?? {}) as Corpo, ctx, true)
    if (falha) {
      seq -= 1
      return falha
    }
    const { serie, numero } = alocarNumeroNfseMock()
    Object.assign(nova, { serie, numero, dataCriacao: agoraIso() })
    enviar(nova)
    notas = [nova, ...notas]
    return json(200, detalhe(nova, perfil))
  }

  // Assistente: prepara o rascunho (valida; NAO aloca numero nem envia). Quem aprova e uma pessoa.
  if (path === '/nfse/notas/preparar' && method === 'POST') {
    if (!escreve) return negado()
    const nova: Nota = base(0, { id: `nota-${String(++seq).padStart(4, '0')}`, estado: 'RASCUNHO', pessoa: { id: '', nome: '', documento: '' }, servico: { id: '', nome: '' }, discriminacao: '' })
    const falha = aplicar(nova, (req.body ?? {}) as Corpo, ctx, true)
    if (falha) {
      seq -= 1
      return falha
    }
    Object.assign(nova, { serie: '', numero: 0, dataCriacao: agoraIso() })
    notas = [nova, ...notas]
    return json(200, detalhe(nova, perfil))
  }

  // Cancelamento preparado pelo assistente: aprovar/descartar pelo id do CANCELAMENTO.
  const rotaCanc = /^\/nfse\/cancelamentos\/([^/]+)\/(aprovar|descartar)$/.exec(path)
  if (rotaCanc && method === 'POST') {
    if (!escreve) return negado()
    const dona = notas.find((n) => n.cancelamento?.id === rotaCanc[1])
    if (!dona?.cancelamento) return erro(404, 'nao_encontrado', 'Cancelamento não encontrado.')
    if (dona.cancelamento.estado !== 'RASCUNHO') return erro(409, 'cancelamento_nao_editavel', 'Só é possível aprovar ou descartar um cancelamento em rascunho.')
    if (rotaCanc[2] === 'descartar') {
      dona.cancelamento = { ...dona.cancelamento, estado: 'DESCARTADO' }
      return json(200, detalhe(dona, perfil))
    }
    if (!delegado) return naoDelegado()
    if (dona.estado !== 'AUTORIZADA') return erro(409, 'nota_nao_autorizada', 'Só é possível cancelar uma nota autorizada.')
    if (!noMesVigente(dona)) return erro(409, 'cancelamento_fora_do_mes', 'O cancelamento só é possível dentro do mês vigente da emissão da nota.')
    dona.cancelamento = { ...dona.cancelamento, estado: 'PROCESSANDO', solicitadoEm: agoraIso() }
    dona.consultas = 0
    if (dona.valorServicos !== VALOR_CANCELAMENTO_PENDENTE) concluirCancelamento(dona)
    return json(200, detalhe(dona, perfil))
  }
  if (!path.startsWith('/nfse/notas')) return null

  if (path === '/nfse/notas/count' && method === 'GET') return json(200, { count: filtradas(q).length })
  const faixa = /^\/nfse\/notas\/(\d+)\/(\d+)$/.exec(path)
  if (faixa && method === 'GET') {
    const from = Number(faixa[1])
    const to = Number(faixa[2])
    if (from > to) return erro(400, 'intervalo_invalido', 'Intervalo inválido.')
    if (to - from + 1 > 100) return erro(400, 'intervalo_invalido', 'No máximo 100 registros por consulta.')
    return json(200, filtradas(q).slice(from, to + 1).map(item))
  }

  const rota = /^\/nfse\/notas\/([^/]+)(?:\/(enviar|atualizar|cancelar|aprovar|descartar|pdf)|\/(cancelamentos\/preparar))?$/.exec(path)
  if (!rota) return null
  const nota = notas.find((n) => n.id === rota[1])
  if (!nota) return erro(404, 'nao_encontrado', 'Nota não encontrada.')
  progredir(nota)
  const acao = rota[2] ?? rota[3]

  if (!acao && method === 'GET') return json(200, detalhe(nota, perfil))

  if (acao === 'pdf' && method === 'GET') {
    if (!nota.temPdf) return erro(404, 'pdf_indisponivel', 'O PDF desta nota ainda não está disponível.')
    return new Response(pdfMinimo(`NFS-e ${nota.serie}-${nota.numero} (mock)`) as BlobPart, { status: 200, headers: { 'Content-Type': 'application/pdf' } })
  }

  if (acao === 'atualizar' && method === 'POST') {
    if (!escreve) return negado()
    if (cancelando(nota)) {
      nota.consultas += 1
      if (nota.consultas >= 2) concluirCancelamento(nota)
    } else if (podeAtualizar(nota) && nota.estado !== 'EM_CONFLITO') {
      nota.consultas += 1
      if (nota.consultas >= 2) autorizar(nota)
    }
    return json(200, detalhe(nota, perfil))
  }

  if ((acao === 'cancelar' || acao === 'cancelamentos/preparar') && method === 'POST') {
    const preparando = acao === 'cancelamentos/preparar'
    if (!escreve) return negado()
    if (!preparando && !delegado) return naoDelegado()
    const b = (req.body ?? {}) as { codigoMotivo?: unknown; motivo?: unknown }
    const campos: Record<string, string> = {}
    if (b.codigoMotivo !== 1 && b.codigoMotivo !== 2 && b.codigoMotivo !== 9) campos.codigoMotivo = 'Escolha o tipo do motivo.'
    const motivo = typeof b.motivo === 'string' ? b.motivo.trim() : ''
    if (motivo.length < 15 || motivo.length > 255) campos.motivo = 'A justificativa deve ter de 15 a 255 caracteres.'
    if (Object.keys(campos).length > 0) return erro(400, 'requisicao_invalida', 'Dados inválidos.', { campos })
    if (cancelando(nota) || cancelamentoRascunho(nota)) return erro(409, 'cancelamento_em_andamento', 'Já existe um cancelamento em andamento para esta nota.')
    if (nota.estado !== 'AUTORIZADA') return erro(409, 'nota_nao_autorizada', 'Só é possível cancelar uma nota autorizada.')
    if (!noMesVigente(nota)) return erro(409, 'cancelamento_fora_do_mes', 'O cancelamento só é possível dentro do mês vigente da emissão da nota.')
    nota.cancelamento = { id: `canc-${String(++seqCancelamento).padStart(4, '0')}`, estado: preparando ? 'RASCUNHO' : 'PROCESSANDO', codigoMotivo: b.codigoMotivo as number, motivo, codigoRetorno: null, mensagemRetorno: null, dataHoraEvento: null, solicitadoEm: agoraIso() }
    if (preparando) return json(200, detalhe(nota, perfil))
    nota.consultas = 0
    if (nota.valorServicos !== VALOR_CANCELAMENTO_PENDENTE) concluirCancelamento(nota)
    return json(200, detalhe(nota, perfil))
  }

  if (acao === 'descartar' && method === 'POST') {
    if (!escreve) return negado()
    if (nota.estado !== 'RASCUNHO') return erro(409, 'nota_nao_editavel', 'Só é possível descartar um rascunho.')
    nota.estado = 'DESCARTADA'
    return json(200, detalhe(nota, perfil))
  }

  if ((acao === 'enviar' || acao === 'aprovar') && method === 'POST') {
    if (!escreve) return negado()
    if (acao === 'aprovar' && nota.estado !== 'RASCUNHO') return erro(409, 'nota_nao_editavel', 'Só é possível aprovar um rascunho.')
    if (nota.estado === 'PROCESSANDO' || nota.estado === 'EMITIDA_OFFLINE') return erro(409, 'nota_em_processamento', 'A nota está em processamento.')
    if (nota.estado === 'EM_CONFLITO') return erro(409, 'nota_em_conflito', 'A nota está em conflito com a prefeitura.')
    if (acao === 'enviar' && !podeReenviar(nota)) return erro(409, 'nota_nao_editavel', 'Esta nota não pode mais ser editada.')
    const copia = { ...nota }
    const falha = aplicar(nota, (req.body ?? {}) as Corpo, ctx, false)
    if (falha) {
      Object.assign(nota, copia)
      return falha
    }
    if (acao === 'aprovar' && !nota.numero) {
      // a aprovacao e quem aloca numero, serie e data de emissao (o preparo do assistente nao os tem)
      const { serie, numero } = alocarNumeroNfseMock()
      Object.assign(nota, { serie, numero, dataEmissao: agoraIso() })
    }
    enviar(nota)
    return json(200, detalhe(nota, perfil))
  }
  return null
}
