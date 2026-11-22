import type { Papel } from '@/lib/api'
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
 *   POST /nfse/notas/{id}/atualizar               (operador+) consulta o gateway agora
 *   GET  /nfse/notas/{id}/pdf                     -> application/pdf (404 pdf_indisponivel)
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

export type EstadoNota = 'rascunho' | 'processando' | 'emitida_offline' | 'autorizada' | 'rejeitada' | 'em_conflito' | 'cancelada' | 'descartada'

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
    estado: 'autorizada',
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
    enviadaEm: 0,
    consultas: 0,
    ...parcial,
  }
}

const autorizadaCom = (n: number): Partial<Nota> => ({
  estado: 'autorizada',
  numeroNfse: String(2026000 + n),
  chave: `4108809${String(n).padStart(37, '0')}`,
  codigoVerificacao: `ABCD${1000 + n}`,
  temPdf: true,
  dataHoraEvento: '2026-11-10T14:00:30',
})

const sementes = (): Nota[] => [
  base(0, autorizadaCom(1)),
  base(1, autorizadaCom(2)),
  base(2, {
    estado: 'rejeitada',
    codigoRetorno: 'E160',
    mensagemRetorno: 'A prefeitura rejeitou a nota.',
    errosRetorno: [{ campo: 'tomador.endereco.cep', erro: 'E160', descricao: 'CEP do tomador inválido para o município.' }],
  }),
  base(3, { estado: 'em_conflito', codigoRetorno: 'E999', mensagemRetorno: 'Já existe uma nota com este número na prefeitura.' }),
  base(4, { estado: 'cancelada', numeroNfse: '2026005', temPdf: true }),
  base(5, { estado: 'rascunho' }),
]

let notas: Nota[] = sementes()
let seq = notas.length

export function resetNotas(): void {
  notas = sementes()
  seq = notas.length
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
  if (n.estado === 'processando' && n.enviadaEm > 0 && Date.now() - n.enviadaEm >= TEMPO_AUTORIZACAO_MS) autorizar(n)
}

const podeAtualizar = (n: Nota) => n.estado === 'processando' || n.estado === 'emitida_offline' || n.estado === 'em_conflito'
const podeReenviar = (n: Nota) => n.estado === 'rascunho' || n.estado === 'rejeitada'

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

function detalhe(n: Nota) {
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
  papel: Papel | null
  empresaId: string | null
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
  if (cfg.statusCadastro !== 'cadastrado') return erro(409, 'emitente_nao_cadastrado', 'A empresa não está cadastrada como emitente.')
  if (cfg.certificadoSituacao !== 'valido') return erro(409, 'certificado_nao_valido', 'O certificado digital não está válido.')

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
    n.estado = 'rejeitada'
    n.codigoRetorno = 'E160'
    n.mensagemRetorno = 'A prefeitura rejeitou a nota.'
    n.errosRetorno = [
      { campo: 'valorServicos', erro: 'E160', descricao: 'O valor informado não é aceito para este serviço.' },
      { campo: 'tomador.endereco.cep', erro: 'E161', descricao: 'CEP do tomador inválido para o município.' },
    ]
  } else if (n.valorServicos === VALOR_CONFLITO) {
    n.estado = 'em_conflito'
    n.codigoRetorno = 'E999'
    n.mensagemRetorno = 'Já existe uma nota com este número na prefeitura.'
  } else {
    n.estado = 'processando'
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

  if (!path.startsWith('/nfse/notas')) return null
  if (!ctx.empresaId || !ctx.papel) return erro(403, 'acesso_negado', 'Acesso negado.')
  const escreve = ctx.papel === 'operador' || ctx.papel === 'administrador'
  const negado = () => erro(403, 'acesso_negado', 'Você não tem permissão para emitir ou reenviar notas.')

  if (path === '/nfse/notas' && method === 'POST') {
    if (!escreve) return negado()
    const nova: Nota = base(0, { id: `nota-${String(++seq).padStart(4, '0')}`, estado: 'rascunho', pessoa: { id: '', nome: '', documento: '' }, servico: { id: '', nome: '' }, discriminacao: '' })
    const falha = aplicar(nova, (req.body ?? {}) as Corpo, ctx, true)
    if (falha) {
      seq -= 1
      return falha
    }
    const { serie, numero } = alocarNumeroNfseMock()
    Object.assign(nova, { serie, numero, dataCriacao: agoraIso() })
    enviar(nova)
    notas = [nova, ...notas]
    return json(200, detalhe(nova))
  }

  if (path === '/nfse/notas/count' && method === 'GET') return json(200, { count: filtradas(q).length })
  const faixa = /^\/nfse\/notas\/(\d+)\/(\d+)$/.exec(path)
  if (faixa && method === 'GET') {
    const from = Number(faixa[1])
    const to = Number(faixa[2])
    if (from > to) return erro(400, 'intervalo_invalido', 'Intervalo inválido.')
    if (to - from + 1 > 100) return erro(400, 'intervalo_invalido', 'No máximo 100 registros por consulta.')
    return json(200, filtradas(q).slice(from, to + 1).map(item))
  }

  const rota = /^\/nfse\/notas\/([^/]+)(?:\/(enviar|atualizar|pdf))?$/.exec(path)
  if (!rota) return null
  const nota = notas.find((n) => n.id === rota[1])
  if (!nota) return erro(404, 'nao_encontrado', 'Nota não encontrada.')
  progredir(nota)
  const acao = rota[2]

  if (!acao && method === 'GET') return json(200, detalhe(nota))

  if (acao === 'pdf' && method === 'GET') {
    if (!nota.temPdf) return erro(404, 'pdf_indisponivel', 'O PDF desta nota ainda não está disponível.')
    return new Response(pdfMinimo(`NFS-e ${nota.serie}-${nota.numero} (mock)`) as BlobPart, { status: 200, headers: { 'Content-Type': 'application/pdf' } })
  }

  if (acao === 'atualizar' && method === 'POST') {
    if (!escreve) return negado()
    if (podeAtualizar(nota) && nota.estado !== 'em_conflito') {
      nota.consultas += 1
      if (nota.consultas >= 2) autorizar(nota)
    }
    return json(200, detalhe(nota))
  }

  if (acao === 'enviar' && method === 'POST') {
    if (!escreve) return negado()
    if (nota.estado === 'processando' || nota.estado === 'emitida_offline') return erro(409, 'nota_em_processamento', 'A nota está em processamento.')
    if (nota.estado === 'em_conflito') return erro(409, 'nota_em_conflito', 'A nota está em conflito com a prefeitura.')
    if (!podeReenviar(nota)) return erro(409, 'nota_nao_editavel', 'Esta nota não pode mais ser editada.')
    const copia = { ...nota }
    const falha = aplicar(nota, (req.body ?? {}) as Corpo, ctx, false)
    if (falha) {
      Object.assign(nota, copia)
      return falha
    }
    enviar(nota)
    return json(200, detalhe(nota))
  }
  return null
}
