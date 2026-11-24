import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type Papel } from '@/lib/api'
import type { FkSource } from '@/runtime/Combobox'
import type { TableState } from '@/runtime/table-state'

/* ---------------------------------------------------------------------------
 * adminApi.ts: TODAS as chamadas das telas de administrador da plataforma ficam AQUI.
 * Contrato CONFIRMADO com o back (back/api/README.md, secao admin): ajuste so este arquivo quando ele mudar.
 *
 *   Usuarios
 *     GET  /admin/usuarios/{from}/{to}?busca&sortBy&sortDirection  -> [{id, nome, email, whatsapp?, ativo?, dataCriacao?}]
 *     GET  /admin/usuarios/count?busca                             -> {count}
 *     POST /admin/usuarios {nome, email, whatsapp?, senha, ativo, vinculos?:[{empresaId, papel}]}
 *     PATCH /admin/usuarios/{id} {nome?, whatsapp?, ativo?}        (whatsapp '' apaga; 400 auto_desativacao)
 *     POST /admin/usuarios/{id}/senha {senha}                       -> 204
 *     GET  /admin/usuarios/{id}/vinculos                           -> [{id, empresaId, empresa?:{razaoSocial}, papel, ativo, podeCancelarNfse}]
 *     POST /admin/usuarios/{id}/vinculos {empresaId, papel, podeCancelarNfse?}
 *     PATCH /admin/usuarios/{id}/vinculos/{vinculoId} {papel?, ativo?, podeCancelarNfse?}   (nunca exclui: so desativa)
 *     GET  /Empresa?razaoSocial=*termo*&sortBy=razaoSocial         -> [{id, razaoSocial}]   (Combobox de empresa)
 *   Municipios
 *     POST /admin/municipios/importacao  multipart, campo `arquivo` -> {id, linhasLidas, criados, atualizados, inalterados, rejeitados:[{linha, motivo}], totalRejeitados, nomeArquivo, estado, dataHoraInicio}
 *     GET  /admin/municipios/importacoes?limite=20                 -> [{id, nomeArquivo, dataHoraInicio, usuario:{nome}, linhasLidas, ..., totalRejeitados}]
 *   Consulta de CNPJ (CNPJ.ws)
 *     GET /admin/configuracoes/consulta-cnpj -> {urlBase, tokenConfigurado, dataAtualizacao, atualizadoPor:{id,nome}|null}  (o token NUNCA volta)
 *     PUT /admin/configuracoes/consulta-cnpj {urlBase, token?} -> 204  (token ausente = mantem; texto = grava; '' = remove; 400 url_invalida|token_invalido)
 *     POST /admin/configuracoes/consulta-cnpj/testar {cnpj} -> resumo da consulta com a configuracao SALVA (ResumoConsultaCnpj)
 *       erros: 400 cnpj_invalido, 409 token_nao_configurado, 404 cnpj_nao_encontrado, 502 token_recusado|resposta_invalida,
 *              429 limite_consulta_excedido|muitas_tentativas, 504 servico_cnpj_indisponivel
 *   Integracao IntegraNotas
 *     GET /admin/configuracoes/integranotas -> {ambienteAtivo, urlHomologacao, urlProducao, urlPublicaWebhook|null, tokenHomologacaoConfigurado, tokenProducaoConfigurado,
 *                                               dataAtualizacao, atualizadoPor:{id,nome}|null, producaoPermitida}  (os tokens NUNCA voltam)
 *     PUT /admin/configuracoes/integranotas {ambienteAtivo?, urlHomologacao?, urlProducao?, urlPublicaWebhook?, tokenHomologacao?, tokenProducao?} -> 204
 *       (campo ausente = mantem; texto = grava; '' = remove token/webhook; 400 url_invalida|token_invalido|ambiente_invalido; 409 producao_bloqueada)
 *
 * As respostas sao lidas de forma TOLERANTE (campo ausente = omitido na tela).
 * Senhas: so trafegam no corpo do envio; nunca sao guardadas nem exibidas.
 * ------------------------------------------------------------------------- */

const rec = (v: unknown): Record<string, unknown> => (v && typeof v === 'object' ? (v as Record<string, unknown>) : {})
const str = (v: unknown): string | undefined => (typeof v === 'string' && v !== '' ? v : v == null ? undefined : String(v))
const num = (v: unknown): number | undefined => (typeof v === 'number' && Number.isFinite(v) ? v : undefined)

/* ============================== usuarios ============================== */

export interface Usuario {
  id: string
  nome: string
  email: string
  whatsapp?: string
  /** Ausente na resposta = ativo. */
  ativo: boolean
  dataCriacao?: string
}

export function lerUsuario(raw: unknown): Usuario {
  const r = rec(raw)
  return {
    id: String(r.id ?? ''),
    nome: str(r.nome) ?? '',
    email: str(r.email) ?? '',
    whatsapp: str(r.whatsapp),
    ativo: r.ativo !== false,
    dataCriacao: str(r.dataCriacao),
  }
}

export interface Vinculo {
  id: string
  empresaId: string
  empresaNome: string
  papel: Papel
  ativo: boolean
  /** Delegacao de cancelar NFS-e ao operador (ausente = false). Administrador sempre pode. */
  podeCancelarNfse: boolean
}

export function lerVinculo(raw: unknown): Vinculo {
  const r = rec(raw)
  const empresa = rec(r.empresa)
  const empresaId = str(r.empresaId) ?? str(empresa.id) ?? ''
  return {
    id: String(r.id ?? ''),
    empresaId,
    empresaNome: str(empresa.razaoSocial) ?? str(r.empresaRazaoSocial) ?? str(r.razaoSocial) ?? empresaId,
    papel: (str(r.papel) ?? 'consulta') as Papel,
    ativo: r.ativo !== false,
    podeCancelarNfse: r.podeCancelarNfse === true,
  }
}

export const PAPEIS: { value: Papel; label: string }[] = [
  { value: 'administrador', label: 'Administrador' },
  { value: 'operador', label: 'Operador' },
  { value: 'consulta', label: 'Consulta' },
]

export const adminKeys = {
  usuarios: ['admin', 'usuarios'] as const,
  lista: (s: TableState) => ['admin', 'usuarios', 'lista', s] as const,
  vinculos: (id: string) => ['admin', 'usuarios', id, 'vinculos'] as const,
  importacoes: ['admin', 'municipios', 'importacoes'] as const,
  consultaCnpj: ['admin', 'configuracoes', 'consulta-cnpj'] as const,
  integraNotas: ['admin', 'configuracoes', 'integranotas'] as const,
}

/** Lista paginada no servidor: GET /admin/usuarios/{from}/{to} + /count, com `busca` por nome/e-mail. */
export function useUsuarios(state: TableState) {
  const from = state.page * state.pageSize
  const to = from + state.pageSize - 1
  const busca = state.search.trim() || undefined
  const q = useQuery({
    queryKey: adminKeys.lista(state),
    queryFn: async () => {
      const [rows, total] = await Promise.all([
        api<unknown[]>(`/admin/usuarios/${from}/${to}`, { query: { busca, sortBy: state.sortBy, sortDirection: state.sortBy ? state.sortDirection : undefined } }),
        api<{ count: number }>('/admin/usuarios/count', { query: { busca } }),
      ])
      return { data: (rows ?? []).map(lerUsuario), count: total.count }
    },
    placeholderData: keepPreviousData,
  })
  return { data: q.data?.data, count: q.data?.count, isLoading: q.isLoading, isFetching: q.isFetching, isError: q.isError, error: q.error, refetch: () => void q.refetch() }
}

export interface NovoUsuario {
  nome: string
  email: string
  whatsapp?: string
  senha: string
  ativo: boolean
  vinculos?: { empresaId: string; papel: Papel }[]
}

export function useCriarUsuario() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: NovoUsuario) => api<unknown>('/admin/usuarios', { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.usuarios }),
  })
}

export function useEditarUsuario(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { nome: string; whatsapp?: string; ativo: boolean }) =>
      api<unknown>(`/admin/usuarios/${id}`, { method: 'PATCH', body: { nome: v.nome, whatsapp: v.whatsapp ?? '', ativo: v.ativo } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.usuarios }),
  })
}

export function useRedefinirSenha(id: string) {
  return useMutation({
    mutationFn: (novaSenha: string) => api<void>(`/admin/usuarios/${id}/senha`, { method: 'POST', body: { senha: novaSenha } }),
  })
}

export function useVinculos(usuarioId: string) {
  return useQuery({
    queryKey: adminKeys.vinculos(usuarioId),
    queryFn: async () => ((await api<unknown[]>(`/admin/usuarios/${usuarioId}/vinculos`)) ?? []).map(lerVinculo),
  })
}

export function useAdicionarVinculo(usuarioId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { empresaId: string; papel: Papel; podeCancelarNfse?: boolean }) => api<unknown>(`/admin/usuarios/${usuarioId}/vinculos`, { method: 'POST', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.vinculos(usuarioId) }),
  })
}

export function useAtualizarVinculo(usuarioId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { vinculoId: string; papel: Papel; ativo: boolean; podeCancelarNfse?: boolean }) =>
      api<unknown>(`/admin/usuarios/${usuarioId}/vinculos/${v.vinculoId}`, { method: 'PATCH', body: { papel: v.papel, ativo: v.ativo, podeCancelarNfse: v.podeCancelarNfse } }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.vinculos(usuarioId) }),
  })
}

/** FK de Empresa (Combobox dos vinculos). */
export const empresaFk: FkSource = {
  key: 'Empresa',
  search: async (term) => {
    const rows = await api<{ id: string; razaoSocial: string }[]>('/Empresa', { query: { razaoSocial: term.trim() ? `*${term.trim()}*` : undefined, sortBy: 'razaoSocial' } })
    return rows.map((e) => ({ id: e.id, label: e.razaoSocial }))
  },
  labelOf: (v) => str(v.razaoSocial),
}

/* ======================= importacao de municipios ======================= */

export interface Rejeitado {
  linha?: number
  motivo: string
}

export interface Importacao {
  id: string
  dataHora?: string
  arquivo?: string
  usuario?: string
  linhasLidas?: number
  criados?: number
  atualizados?: number
  inalterados?: number
  /** Quantidade de linhas rejeitadas (do total devolvido, mesmo se a lista nao vier). */
  totalRejeitados?: number
  /** Detalhe (linha e motivo); so a resposta da importacao traz. */
  rejeitados: Rejeitado[]
}

/** Aceita `rejeitados` como lista ou como contagem, e campos ausentes. */
export function lerImportacao(raw: unknown): Importacao {
  const r = rec(raw)
  const lista = Array.isArray(r.rejeitados) ? r.rejeitados.map((x) => ({ linha: num(rec(x).linha), motivo: str(rec(x).motivo) ?? 'Linha rejeitada.' })) : []
  return {
    id: String(r.id ?? ''),
    dataHora: str(r.dataHoraInicio) ?? str(r.dataHora) ?? str(r.criadoEm) ?? str(r.dataCriacao) ?? str(r.data),
    arquivo: str(r.arquivo) ?? str(r.nomeArquivo),
    usuario: str(rec(r.usuario).nome) ?? str(r.usuario) ?? str(r.usuarioNome),
    linhasLidas: num(r.linhasLidas),
    criados: num(r.criados),
    atualizados: num(r.atualizados),
    inalterados: num(r.inalterados),
    totalRejeitados: num(r.totalRejeitados) ?? (Array.isArray(r.rejeitados) ? lista.length : num(r.rejeitados)),
    rejeitados: lista,
  }
}

export function useImportacoes() {
  return useQuery({
    queryKey: adminKeys.importacoes,
    queryFn: async () => ((await api<unknown[]>('/admin/municipios/importacoes')) ?? []).map(lerImportacao),
  })
}

export function useImportarMunicipios() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (arquivo: File) => {
      const form = new FormData()
      form.append('arquivo', arquivo)
      return lerImportacao(await api<unknown>('/admin/municipios/importacao', { method: 'POST', body: form }))
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.importacoes }),
  })
}

/* ===================== configuracao da consulta de CNPJ ===================== */

export interface ConfigConsultaCnpj {
  urlBase: string
  tokenConfigurado: boolean
  dataAtualizacao?: string
  atualizadoPor?: { id: string; nome: string }
}

export function lerConfigConsultaCnpj(raw: unknown): ConfigConsultaCnpj {
  const r = rec(raw)
  const por = rec(r.atualizadoPor)
  const nome = str(por.nome)
  return {
    urlBase: str(r.urlBase) ?? '',
    tokenConfigurado: r.tokenConfigurado === true,
    dataAtualizacao: str(r.dataAtualizacao),
    atualizadoPor: nome ? { id: String(por.id ?? ''), nome } : undefined,
  }
}

export function useConfigConsultaCnpj() {
  return useQuery({
    queryKey: adminKeys.consultaCnpj,
    queryFn: async () => lerConfigConsultaCnpj(await api<unknown>('/admin/configuracoes/consulta-cnpj')),
  })
}

/** `token`: ausente = mantem; texto = grava/substitui; '' = remove. */
export function useSalvarConfigConsultaCnpj() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: { urlBase: string; token?: string }) => api<void>('/admin/configuracoes/consulta-cnpj', { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.consultaCnpj }),
  })
}

/* ===================== configuracao da integracao IntegraNotas ===================== */

export type AmbienteIntegraNotas = 'homologacao' | 'producao'

export interface ConfigIntegraNotas {
  ambienteAtivo: AmbienteIntegraNotas
  urlHomologacao: string
  urlProducao: string
  urlPublicaWebhook: string
  tokenHomologacaoConfigurado: boolean
  tokenProducaoConfigurado: boolean
  producaoPermitida: boolean
  dataAtualizacao?: string
  atualizadoPor?: { id: string; nome: string }
}

export function lerConfigIntegraNotas(raw: unknown): ConfigIntegraNotas {
  const r = rec(raw)
  const por = rec(r.atualizadoPor)
  const nome = str(por.nome)
  return {
    ambienteAtivo: r.ambienteAtivo === 'producao' ? 'producao' : 'homologacao',
    urlHomologacao: str(r.urlHomologacao) ?? '',
    urlProducao: str(r.urlProducao) ?? '',
    urlPublicaWebhook: str(r.urlPublicaWebhook) ?? '',
    tokenHomologacaoConfigurado: r.tokenHomologacaoConfigurado === true,
    tokenProducaoConfigurado: r.tokenProducaoConfigurado === true,
    producaoPermitida: r.producaoPermitida === true,
    dataAtualizacao: str(r.dataAtualizacao),
    atualizadoPor: nome ? { id: String(por.id ?? ''), nome } : undefined,
  }
}

export function useConfigIntegraNotas() {
  return useQuery({
    queryKey: adminKeys.integraNotas,
    queryFn: async () => lerConfigIntegraNotas(await api<unknown>('/admin/configuracoes/integranotas')),
  })
}

/** Campo ausente = mantem; tokens e urlPublicaWebhook: texto = grava/substitui; '' = remove. */
export interface SalvarIntegraNotas {
  ambienteAtivo?: AmbienteIntegraNotas
  urlHomologacao?: string
  urlProducao?: string
  urlPublicaWebhook?: string
  tokenHomologacao?: string
  tokenProducao?: string
}

export function useSalvarConfigIntegraNotas() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (v: SalvarIntegraNotas) => api<void>('/admin/configuracoes/integranotas', { method: 'PUT', body: v }),
    onSuccess: () => qc.invalidateQueries({ queryKey: adminKeys.integraNotas }),
  })
}

/* ===================== teste da consulta de CNPJ ===================== */

export interface ResumoConsultaCnpj {
  cnpj: string
  razaoSocial: string
  nomeFantasia?: string
  situacaoCadastral?: string
  dataInicioAtividade?: string
  porte?: string
  naturezaJuridica?: string
  atividadePrincipal?: { codigo?: string; descricao?: string }
  capitalSocial?: string
  endereco?: { logradouro?: string; numero?: string; complemento?: string; bairro?: string; cep?: string; municipio?: string; uf?: string }
  email?: string
  telefone?: string
  simples?: boolean
  mei?: boolean
  quantidadeSocios?: number
  duracaoMs?: number
  urlConsultada?: string
}

const bool = (v: unknown): boolean | undefined => (typeof v === 'boolean' ? v : undefined)

export function lerResumoConsultaCnpj(raw: unknown): ResumoConsultaCnpj {
  const r = rec(raw)
  const at = rec(r.atividadePrincipal)
  const en = rec(r.endereco)
  return {
    cnpj: str(r.cnpj) ?? '',
    razaoSocial: str(r.razaoSocial) ?? '',
    nomeFantasia: str(r.nomeFantasia),
    situacaoCadastral: str(r.situacaoCadastral),
    dataInicioAtividade: str(r.dataInicioAtividade),
    porte: str(r.porte),
    naturezaJuridica: str(r.naturezaJuridica),
    atividadePrincipal: r.atividadePrincipal ? { codigo: str(at.codigo), descricao: str(at.descricao) } : undefined,
    capitalSocial: str(r.capitalSocial),
    endereco: r.endereco
      ? { logradouro: str(en.logradouro), numero: str(en.numero), complemento: str(en.complemento), bairro: str(en.bairro), cep: str(en.cep), municipio: str(en.municipio), uf: str(en.uf) }
      : undefined,
    email: str(r.email),
    telefone: str(r.telefone),
    simples: bool(r.simples),
    mei: bool(r.mei),
    quantidadeSocios: num(r.quantidadeSocios),
    duracaoMs: num(r.duracaoMs),
    urlConsultada: str(r.urlConsultada),
  }
}

/** Consulta de teste com a configuracao SALVA (nao invalida queries). `cnpj`: so digitos. */
export function useTestarConsultaCnpj() {
  return useMutation({
    mutationFn: async (cnpj: string) => lerResumoConsultaCnpj(await api<unknown>('/admin/configuracoes/consulta-cnpj/testar', { method: 'POST', body: { cnpj } })),
  })
}
