import type { PapelValor } from '@/lib/api'
import { erro, json } from '@/mocks/http'
import { getMunicipios } from '@/mocks/admin'
import { nfseHabilitadaMock, vincularCertificadoNfse } from '@/mocks/nfse-config'
import { isValidCnpj } from '@/runtime/format'

/* ---------------------------------------------------------------------------
 * Mock de Configuracao > Empresa da empresa da sessao (contrato em features/empresa-config/empresaApi.ts):
 *   GET  /configuracao/empresa              (qualquer papel da empresa; plataforma sem empresa: 403)
 *   PUT  /configuracao/empresa              (so administrador) -> 200 com o corpo do GET
 *   POST /configuracao/empresa/certificado  (so administrador; multipart `arquivo` + `senha`) -> 200 {situacao, validade, pendenteDeEnvio, venceEmDias, avisos}
 * Estado em memoria. PUT: 400 requisicao_invalida (detalhes.campos); documento so muda com documentoEditavel (senao 409
 * documento_bloqueado; o documento 11444777000161 simula CNPJ ja cadastrado: 409 documento_duplicado). documentoEditavel = NFS-e
 * ainda nao habilitada (e nao bloqueado por `ajustarEmpresaConfig`).
 * Certificado: valida senha (^[A-Za-z0-9]{1,60}$), extensao .pfx/.p12 e tamanho (256 KB); o NOME do arquivo simula a resposta:
 *   vencido -> 400 certificado_vencido; outro -> 400 certificado_nao_pertence; senhaerrada -> 400 certificado_senha_incorreta;
 *   invalido -> 400 certificado_invalido; gateway -> 400 certificado_invalido_gateway; grande -> 413 arquivo_grande;
 *   muitas -> 429 muitas_tentativas; token -> 409 integranotas_token_recusado; indisponivel -> 504 integranotas_indisponivel;
 *   respostainvalida -> 502 integranotas_resposta_invalida; semcnpj -> sucesso com aviso nao_foi_possivel_conferir_cnpj;
 *   vence15 -> sucesso com validade em 15 dias. Sem a NFS-e habilitada (src/mocks/nfse-config.ts) o certificado fica guardado
 *   (pendenteDeEnvio:true); ao habilitar, o proximo GET o mostra valido. O arquivo e a senha nunca voltam.
 * ------------------------------------------------------------------------- */

export const DOCUMENTO_DUPLICADO_MOCK = '11444777000161'
const TAMANHO_MAXIMO = 256 * 1024

type Situacao = 'NAO_ENVIADO' | 'VALIDO' | 'VENCIDO' | 'INVALIDO' | 'NAO_PERTENCE'
interface Municipio {
  id: string
  nome: string
  uf: string
  codigoIbge: string
}

interface Estado {
  razaoSocial: string
  nomeFantasia: string
  documento: string
  inscricaoMunicipal: string | null
  rgInscricaoEstadual: string | null
  email: string | null
  whatsapp: string | null
  telefone: string | null
  cep: string
  logradouro: string
  numeroEndereco: string
  complemento: string | null
  bairro: string
  municipio: Municipio | null
  regimeTributario: number
  regimeIbsCbs: number | null
  documentoBloqueado: boolean
  certificado: { situacao: Situacao; validade: string | null; pendenteDeEnvio: boolean }
}

const inicial = (): Estado => ({
  razaoSocial: 'Horizonte Serviços Contábeis Ltda',
  nomeFantasia: 'Horizonte Contábil',
  documento: '11222333000181',
  inscricaoMunicipal: '12345',
  rgInscricaoEstadual: null,
  email: 'contato@horizonte.com.br',
  whatsapp: '11987654321',
  telefone: '1133334444',
  cep: '85980000',
  logradouro: 'Rua Quinze de Novembro',
  numeroEndereco: '100',
  complemento: null,
  bairro: 'Centro',
  municipio: getMunicipios().find((m) => m.codigoIbge === '4108809') ?? getMunicipios()[0] ?? null,
  regimeTributario: 1,
  regimeIbsCbs: null,
  documentoBloqueado: false,
  certificado: { situacao: 'NAO_ENVIADO', validade: null, pendenteDeEnvio: false },
})

let estado = inicial()

export function resetEmpresaConfig(): void {
  estado = inicial()
}

/** Para testes e cenarios: ajusta o estado em memoria. */
export function ajustarEmpresaConfig(parcial: Partial<Estado>): void {
  estado = { ...estado, ...parcial }
}

/* ----- datas (UTC, so a parte da data: sem depender de fuso nem de hora) ----- */

const hojeUtc = () => {
  const n = new Date()
  return Date.UTC(n.getFullYear(), n.getMonth(), n.getDate())
}
const DIA = 86_400_000
const emDias = (d: number) => new Date(hojeUtc() + d * DIA).toISOString().slice(0, 10)
const diasAte = (iso: string) => Math.round((Date.parse(`${iso}T00:00:00Z`) - hojeUtc()) / DIA)

function statusCertificado() {
  // Pendente + NFS-e habilitada: o certificado "segue" ao gateway e passa a valer.
  if (estado.certificado.pendenteDeEnvio && nfseHabilitadaMock()) estado = { ...estado, certificado: { ...estado.certificado, situacao: 'VALIDO', pendenteDeEnvio: false } }
  const { situacao, validade, pendenteDeEnvio } = estado.certificado
  return { situacao, validade, pendenteDeEnvio, venceEmDias: validade ? diasAte(validade) : null }
}

vincularCertificadoNfse(() => {
  const { situacao, validade } = statusCertificado()
  return { situacao, validade }
})

function corpo() {
  const { documentoBloqueado, ...dados } = estado
  const { certificado: _c, ...resto } = dados
  return { ...resto, documentoEditavel: !documentoBloqueado && !nfseHabilitadaMock(), certificado: statusCertificado() }
}

/* ----- PUT ----- */

const texto = (v: unknown): string | undefined => (typeof v === 'string' ? v.trim() : undefined)
const opcional = (v: unknown): string | null | undefined => (v === null || v === undefined || v === '' ? null : typeof v === 'string' ? v.trim() : undefined)

function salvar(b: Record<string, unknown>): Response {
  const campos: { campo: string; mensagem: string }[] = []
  const falha = (campo: string, mensagem: string) => void campos.push({ campo, mensagem })

  const obrigatorio = (campo: string, max: number): string => {
    const v = texto(b[campo])
    if (!v) falha(campo, 'Campo obrigatório.')
    else if (v.length > max) falha(campo, `Máximo de ${max} caracteres.`)
    return v ?? ''
  }
  const livre = (campo: string, max: number): string | null => {
    const v = opcional(b[campo])
    if (v === undefined) falha(campo, 'Valor inválido.')
    else if (v && v.length > max) falha(campo, `Máximo de ${max} caracteres.`)
    return v ?? null
  }

  const razaoSocial = obrigatorio('razaoSocial', 60)
  const nomeFantasia = obrigatorio('nomeFantasia', 60)
  const logradouro = obrigatorio('logradouro', 60)
  const numeroEndereco = obrigatorio('numeroEndereco', 60)
  const bairro = obrigatorio('bairro', 60)
  const inscricaoMunicipal = livre('inscricaoMunicipal', 20)
  const rgInscricaoEstadual = livre('rgInscricaoEstadual', 20)
  const complemento = livre('complemento', 60)
  const whatsapp = livre('whatsapp', 20)
  const telefone = livre('telefone', 20)
  const email = livre('email', 60)
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) falha('email', 'E-mail inválido.')
  const cep = texto(b.cep) ?? ''
  if (!/^[0-9]{8}$/.test(cep)) falha('cep', 'O CEP deve ter 8 dígitos.')

  const refMunicipio = (b.municipio ?? null) as { id?: unknown } | null
  const municipio = typeof refMunicipio?.id === 'string' ? (getMunicipios().find((m) => m.id === refMunicipio.id) ?? null) : null
  if (!municipio) falha('municipio', refMunicipio ? 'Município não encontrado.' : 'Campo obrigatório.')

  const regime = b.regimeTributario
  if (typeof regime !== 'number' || ![1, 2, 3, 4].includes(regime)) falha('regimeTributario', 'Regime tributário inválido.')
  const ibs = b.regimeIbsCbs
  if (ibs !== undefined && ibs !== null && (typeof ibs !== 'number' || ![0, 1, 2, 3, 4].includes(ibs))) falha('regimeIbsCbs', 'Regime do IBS e da CBS inválido.')

  let documento = estado.documento
  if (b.documento !== undefined) {
    const d = texto(b.documento)?.replace(/\D/g, '') ?? ''
    if (d !== estado.documento) {
      if (estado.documentoBloqueado || nfseHabilitadaMock()) return erro(409, 'documento_bloqueado', 'O CNPJ não pode ser alterado depois do cadastro na IntegraNotas.')
      if (!isValidCnpj(d)) falha('documento', 'CNPJ inválido.')
      else if (d === DOCUMENTO_DUPLICADO_MOCK) return erro(409, 'documento_duplicado', 'Já existe uma empresa com este CNPJ.')
      documento = d
    }
  }

  if (campos.length > 0) return erro(400, 'requisicao_invalida', 'Dados inválidos.', { campos })

  estado = {
    ...estado,
    razaoSocial, nomeFantasia, documento, inscricaoMunicipal, rgInscricaoEstadual, email, whatsapp, telefone, cep, logradouro, numeroEndereco,
    complemento, bairro, municipio, regimeTributario: regime as number, regimeIbsCbs: typeof ibs === 'number' ? ibs : null,
  }
  return json(200, corpo())
}

/* ----- certificado ----- */

const FALHAS: Record<string, () => Response> = {
  vencido: () => erro(400, 'certificado_vencido', 'O certificado está vencido.'),
  outro: () => erro(400, 'certificado_nao_pertence', 'O certificado pertence a outro CNPJ.'),
  senhaerrada: () => erro(400, 'certificado_senha_incorreta', 'A senha do certificado está incorreta.'),
  invalido: () => erro(400, 'certificado_invalido', 'O arquivo não é um certificado válido.'),
  gateway: () => erro(400, 'certificado_invalido_gateway', 'A IntegraNotas não conseguiu abrir o certificado.'),
  grande: () => erro(413, 'arquivo_grande', 'O arquivo excede o tamanho máximo.'),
  muitas: () => erro(429, 'muitas_tentativas', 'Muitas tentativas. Aguarde um pouco.'),
  token: () => erro(409, 'integranotas_token_recusado', 'Token recusado pela IntegraNotas.'),
  indisponivel: () => erro(504, 'integranotas_indisponivel', 'A IntegraNotas não respondeu.'),
  respostainvalida: () => erro(502, 'integranotas_resposta_invalida', 'Resposta inválida da IntegraNotas.'),
}

function enviarCertificado(body: unknown): Response {
  const form = body instanceof FormData ? body : null
  const arquivo = form?.get('arquivo')
  const senha = form?.get('senha')
  if (!arquivo || typeof arquivo === 'string') return erro(400, 'certificado_invalido', 'Envie o certificado no campo "arquivo".')
  if (typeof senha !== 'string' || !/^[A-Za-z0-9]{1,60}$/.test(senha)) return erro(400, 'certificado_senha_invalida', 'A senha deve ter só letras e números, sem acentos, até 60 caracteres.')
  if (arquivo.size > TAMANHO_MAXIMO) return erro(413, 'arquivo_grande', 'O arquivo excede o tamanho máximo.')
  const m = /^(.*)\.(pfx|p12)$/i.exec(arquivo.name)
  if (!m) return erro(400, 'certificado_invalido', 'O arquivo não é um certificado válido (.pfx).')
  const nome = m[1]!.toLowerCase()
  const falha = FALHAS[nome]
  if (falha) return falha()

  const validade = emDias(nome === 'vence15' ? 15 : 365)
  const pendenteDeEnvio = !nfseHabilitadaMock()
  estado = { ...estado, certificado: { situacao: pendenteDeEnvio ? 'NAO_ENVIADO' : 'VALIDO', validade, pendenteDeEnvio } }
  const { situacao, pendenteDeEnvio: pend, venceEmDias } = statusCertificado()
  return json(200, { situacao, validade, pendenteDeEnvio: pend, venceEmDias, avisos: nome === 'semcnpj' ? ['nao_foi_possivel_conferir_cnpj'] : [] })
}

export function empresaConfigRoutes(
  req: { method: string; path: string; body?: unknown },
  ctx: { papel: PapelValor | null; empresaId: string | null },
): Response | null {
  const certificado = req.path === '/configuracao/empresa/certificado'
  if (req.path !== '/configuracao/empresa' && !certificado) return null
  if (!ctx.empresaId || !ctx.papel) return erro(403, 'acesso_negado', 'Acesso negado.')
  if (!certificado && req.method === 'GET') return json(200, corpo())
  const esperado = certificado ? 'POST' : 'PUT'
  if (req.method !== esperado) return null
  if (ctx.papel !== 'ADMINISTRADOR') return erro(403, 'acesso_negado', 'Apenas o administrador da empresa altera estes dados.')
  return certificado ? enviarCertificado(req.body) : salvar((req.body ?? {}) as Record<string, unknown>)
}
