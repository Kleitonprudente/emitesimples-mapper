export interface Violacao {
  regra: string
  arquivo: string
  linha: number
  mensagem: string
}
export const TELAS_A_MAO: Set<string>
export const COMPONENTES_CATALOGADOS: Set<string>
export function verificar(raizSrc: string): Violacao[]
export function verificarDetalhado(raizSrc: string): { violacoes: Violacao[]; total: number }
