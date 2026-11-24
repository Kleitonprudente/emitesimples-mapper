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
  { name: 'GestaoPessoas', title: 'Pessoas', stereo: 'list', route: '/pessoas', icon: 'users', group: 'Cadastros', order: 9, role: 'OPERADOR' },
  { name: 'FormularioPessoa', title: 'Cadastro de pessoa', stereo: 'editor', route: '/pessoas/$id', icon: 'user-pen', role: 'OPERADOR' },
  { name: 'GestaoClientes', title: 'Clientes', stereo: 'list', route: '/clientes', icon: 'users', group: 'Cadastros', order: 10, role: 'OPERADOR' },
  { name: 'FormularioCliente', title: 'Cadastro de cliente', stereo: 'editor', route: '/clientes/$id', icon: 'user-pen', role: 'OPERADOR' },
  { name: 'GestaoFornecedores', title: 'Fornecedores', stereo: 'list', route: '/fornecedores', icon: 'package', group: 'Cadastros', order: 11, role: 'OPERADOR' },
  { name: 'FormularioFornecedor', title: 'Cadastro de fornecedor', stereo: 'editor', route: '/fornecedores/$id', icon: 'user-pen', role: 'OPERADOR' },
  { name: 'GestaoMunicipios', title: 'Gestão de Municípios', stereo: 'list', route: '/municipios', icon: 'settings', group: 'Administração', order: 79, platformOnly: true },
  { name: 'FormularioMunicipio', title: 'Cadastro de município', stereo: 'editor', route: '/municipios/$id', icon: 'settings', platformOnly: true },
  { name: 'GestaoServicos', title: 'Serviços', stereo: 'list', route: '/servicos', icon: 'package', group: 'Cadastros', order: 20, role: 'OPERADOR' },
  { name: 'FormularioServico', title: 'Cadastro de serviço', stereo: 'editor', route: '/servicos/$id', icon: 'package', role: 'OPERADOR' },
  { name: 'GestaoEmpresas', title: 'Gestão de Empresas', stereo: 'list', route: '/empresas', icon: 'settings', group: 'Administração', order: 78, platformOnly: true },
  { name: 'FormularioEmpresa', title: 'Cadastro de empresa', stereo: 'editor', route: '/empresas/$id', icon: 'settings', platformOnly: true },
]
