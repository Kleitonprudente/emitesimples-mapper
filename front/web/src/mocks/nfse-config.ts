import type { Papel } from '@/lib/api'
import { erro, json } from '@/mocks/http'

/* ---------------------------------------------------------------------------
 * Mock de Configuracao > NFS-e da empresa da sessao (contrato em features/empresa-config/nfseApi.ts):
 *   GET /configuracao/nfse   (qualquer papel da empresa; plataforma sem empresa: 403)
 *   PUT /configuracao/nfse {habilitada, serie?, ultimoNumero?} (so administrador) -> 200 com o corpo do GET
 * Estado em memoria. Regras: habilitar exige serie ^[0-9]{1,5}$ e ultimoNumero 0..999999999 (400 dados_invalidos); sem integracao
 * -> 409 integranotas_nao_configurada; ultimoNumero menor que o atual -> 409 numeracao_invalida. Series de teste ao habilitar
 * (simulam a IntegraNotas): 422 cadastro_incompleto, 502 integranotas_recusou, 503 integranotas_token_recusado,
 * 505 integranotas_resposta_invalida, 504 integranotas_indisponivel. O token do emitente nunca sai.
 * ------------------------------------------------------------------------- */

interface Estado {
  habilitada: boolean
  serie: string | null
  ultimoNumero: number
  ambiente: 'homologacao' | 'producao'
  statusCadastro: 'nao_cadastrado' | 'cadastrado' | 'erro'
  dataCadastroGateway: string | null
  emitenteTokenConfigurado: boolean
  certificadoSituacao: 'nao_enviado' | 'valido' | 'vencido' | 'invalido' | 'nao_pertence'
  validadeCertificado: string | null
  integracaoDisponivel: boolean
}

const inicial = (): Estado => ({
  habilitada: false,
  serie: null,
  ultimoNumero: 0,
  ambiente: 'homologacao',
  statusCadastro: 'nao_cadastrado',
  dataCadastroGateway: null,
  emitenteTokenConfigurado: false,
  certificadoSituacao: 'nao_enviado',
  validadeCertificado: null,
  integracaoDisponivel: true,
})

let estado = inicial()

export function resetNfseConfig(): void {
  estado = inicial()
  certificadoDe = certificadoPadrao
}

/** Para testes e cenarios: ajusta o estado em memoria. */
export function ajustarNfseConfig(parcial: Partial<Estado>): void {
  if ('certificadoSituacao' in parcial) certificadoDe = null
  estado = { ...estado, ...parcial }
}

/** Para outros mocks (ex.: certificado da empresa, que so segue ao gateway com a NFS-e habilitada). */
export const nfseHabilitadaMock = (): boolean => estado.habilitada

/** O certificado vive no mock de Empresa (src/mocks/empresa-config.ts); ele se vincula aqui para o GET refletir o que foi enviado. */
type ProvedorCertificado = (() => { situacao: Estado['certificadoSituacao']; validade: string | null }) | null
let certificadoDe: ProvedorCertificado = null
let certificadoPadrao: ProvedorCertificado = null
export function vincularCertificadoNfse(fn: ProvedorCertificado): void {
  certificadoDe = fn
  certificadoPadrao = fn
}

/** Para o mock de notas: situacao atual da configuracao e alocacao do proximo numero da serie (incrementa o ultimo numero). */
export const nfseConfigMock = () => ({ ...estado, ...(certificadoDe ? { certificadoSituacao: certificadoDe().situacao } : {}) })
export function alocarNumeroNfseMock(): { serie: string; numero: number } {
  estado = { ...estado, ultimoNumero: estado.ultimoNumero + 1 }
  return { serie: estado.serie ?? '1', numero: estado.ultimoNumero }
}

const corpo = () => {
  const cert = certificadoDe?.()
  return { ...estado, ...(cert ? { certificadoSituacao: cert.situacao, validadeCertificado: cert.validade } : {}), proximoNumero: estado.ultimoNumero + 1 }
}

const FALHAS_GATEWAY: Record<string, () => Response> = {
  '422': () =>
    erro(422, 'cadastro_incompleto', 'Cadastro da empresa incompleto.', {
      campos: [
        { campo: 'inscricaoMunicipal', mensagem: 'Informe a inscrição municipal.' },
        { campo: 'codigoMunicipio', mensagem: 'Informe o município.' },
      ],
    }),
  '502': () => erro(502, 'integranotas_recusou', 'CNPJ já cadastrado em outra softhouse.'),
  '503': () => erro(502, 'integranotas_token_recusado', 'Token recusado pela IntegraNotas.'),
  '505': () => erro(502, 'integranotas_resposta_invalida', 'Resposta inválida da IntegraNotas.'),
  '504': () => erro(504, 'integranotas_indisponivel', 'A IntegraNotas não respondeu.'),
}

export function nfseConfigRoutes(req: { method: string; path: string; body?: unknown }, ctx: { papel: Papel | null; empresaId: string | null }): Response | null {
  if (req.path !== '/configuracao/nfse') return null
  if (!ctx.empresaId || !ctx.papel) return erro(403, 'acesso_negado', 'Acesso negado.')
  if (req.method === 'GET') return json(200, corpo())
  if (req.method !== 'PUT') return null
  if (ctx.papel !== 'administrador') return erro(403, 'acesso_negado', 'Apenas o administrador da empresa altera a configuração da NFS-e.')

  const b = (req.body ?? {}) as { habilitada?: unknown; serie?: unknown; ultimoNumero?: unknown }
  if (typeof b.habilitada !== 'boolean') return erro(400, 'dados_invalidos', 'Informe se a emissão fica habilitada.')
  const serie = b.serie === undefined ? undefined : b.serie
  const ultimo = b.ultimoNumero === undefined ? undefined : b.ultimoNumero
  if (serie !== undefined && (typeof serie !== 'string' || !/^[0-9]{1,5}$/.test(serie))) return erro(400, 'dados_invalidos', 'A série deve ter de 1 a 5 dígitos.')
  if (ultimo !== undefined && (typeof ultimo !== 'number' || !Number.isInteger(ultimo) || ultimo < 0 || ultimo > 999999999)) return erro(400, 'dados_invalidos', 'O último número deve ser um inteiro entre 0 e 999999999.')
  if (b.habilitada && ((serie ?? estado.serie) == null || (ultimo ?? estado.ultimoNumero) == null)) return erro(400, 'dados_invalidos', 'Informe série e último número para habilitar.')
  if (b.habilitada && !estado.integracaoDisponivel) return erro(409, 'integranotas_nao_configurada', 'A integração com a IntegraNotas não está configurada.')
  if (ultimo !== undefined && ultimo < estado.ultimoNumero) return erro(409, 'numeracao_invalida', 'A numeração conflita com notas já emitidas.')

  const serieFinal = (serie as string | undefined) ?? estado.serie
  if (b.habilitada && !estado.habilitada && estado.statusCadastro !== 'cadastrado') {
    const falha = serieFinal ? FALHAS_GATEWAY[serieFinal] : undefined
    if (falha) {
      estado = { ...estado, statusCadastro: serieFinal === '422' ? 'nao_cadastrado' : 'erro' }
      return falha()
    }
    estado = { ...estado, statusCadastro: 'cadastrado', dataCadastroGateway: new Date().toISOString().slice(0, 19), emitenteTokenConfigurado: true }
  }
  estado = { ...estado, habilitada: b.habilitada, serie: serieFinal ?? null, ultimoNumero: (ultimo as number | undefined) ?? estado.ultimoNumero }
  return json(200, corpo())
}
