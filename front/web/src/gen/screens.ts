// GERADO por frontend/screenManifest. Nao edite: a saida e sobrescrita por inteiro.
// Fonte unica da navegacao: so as entradas com route sem $ viram item de menu (dialogs nao tem rota).
import type { Papel } from '@/lib/api'

export interface ScreenEntry {
  name: string
  route?: string
  title: string
  stereo: string
  icon?: string
  group?: string
  order?: number
  role?: Papel
  platformOnly?: boolean
}

export const screens: ScreenEntry[] = [
  { name: 'GestaoPessoas', title: 'Gestão de Pessoas', stereo: 'list', route: '/pessoas', icon: 'users', group: 'Cadastros', order: 10, role: 'operador' },
  { name: 'FormularioPessoa', title: 'Cadastro de pessoa', stereo: 'editor', route: '/pessoas/$id', icon: 'user-pen', role: 'operador' },
  { name: 'GestaoMunicipios', title: 'Gestão de Municípios', stereo: 'list', route: '/municipios', icon: 'settings', group: 'Administração', order: 79, platformOnly: true },
  { name: 'FormularioMunicipio', title: 'Cadastro de município', stereo: 'editor', route: '/municipios/$id', icon: 'settings', platformOnly: true },
]
