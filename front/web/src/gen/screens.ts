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
  { name: 'AgentesDeAcesso', title: 'Agentes', stereo: 'custom', route: '/agentes', icon: 'user-cog', group: 'Administração', order: 80, role: 'ADMINISTRADOR' },
  { name: 'GestaoEmpresas', title: 'Gestão de Empresas', stereo: 'list', route: '/empresas', icon: 'settings', group: 'Administração', order: 78, platformOnly: true },
  { name: 'FormularioEmpresa', title: 'Cadastro de empresa', stereo: 'editor', route: '/empresas/$id', icon: 'settings', platformOnly: true },
  { name: 'GestaoContasFinanceiras', title: 'Contas', stereo: 'list', route: '/financeiro/contas', icon: 'wallet', group: 'Financeiro', order: 10, role: 'ADMINISTRADOR' },
  { name: 'FormularioContaFinanceira', title: 'Cadastro de conta', stereo: 'editor', route: '/financeiro/contas/$id', icon: 'wallet', role: 'ADMINISTRADOR' },
  { name: 'GestaoCategoriasFinanceiras', title: 'Categorias', stereo: 'list', route: '/financeiro/categorias', icon: 'tags', group: 'Financeiro', order: 20, role: 'ADMINISTRADOR' },
  { name: 'FormularioCategoriaFinanceira', title: 'Cadastro de categoria', stereo: 'editor', route: '/financeiro/categorias/$id', icon: 'tags', role: 'ADMINISTRADOR' },
  { name: 'GestaoCondicoesPagamento', title: 'Condições de pagamento', stereo: 'list', route: '/financeiro/condicoes', icon: 'calendar-clock', group: 'Financeiro', order: 30, role: 'ADMINISTRADOR' },
  { name: 'FormularioCondicaoPagamento', title: 'Cadastro de condição de pagamento', stereo: 'editor', route: '/financeiro/condicoes/$id', icon: 'calendar-clock', role: 'ADMINISTRADOR' },
  { name: 'GestaoBancos', title: 'Bancos', stereo: 'list', route: '/bancos', icon: 'landmark', group: 'Administração', order: 78, platformOnly: true },
  { name: 'FormularioBanco', title: 'Cadastro de banco', stereo: 'editor', route: '/bancos/$id', icon: 'landmark', platformOnly: true },
  { name: 'FormularioConfiguracaoFinanceira', title: 'Configuração do financeiro', stereo: 'editor', route: '/financeiro/configuracao', icon: 'settings', group: 'Financeiro', order: 90, role: 'ADMINISTRADOR' },
  { name: 'ContasAReceber', title: 'Contas a receber', stereo: 'custom', route: '/financeiro/receber', icon: 'arrow-down-circle', group: 'Financeiro', order: 1, role: 'CONSULTA' },
  { name: 'ContasAPagar', title: 'Contas a pagar', stereo: 'custom', route: '/financeiro/pagar', icon: 'arrow-up-circle', group: 'Financeiro', order: 2, role: 'CONSULTA' },
  { name: 'TituloFinanceiro', title: 'Título', stereo: 'custom', route: '/financeiro/titulos/$id', role: 'CONSULTA' },
  { name: 'ExtratoFinanceiro', title: 'Extrato', stereo: 'custom', route: '/financeiro/extrato', icon: 'list', group: 'Financeiro', order: 3, role: 'CONSULTA' },
  { name: 'ImportarExportarCsv', title: 'Importar e exportar', stereo: 'custom', route: '/financeiro/csv', icon: 'upload', group: 'Financeiro', order: 95, role: 'CONSULTA' },
  { name: 'IntegracaoCobranca', title: 'Integração de cobrança', stereo: 'custom', route: '/financeiro/cobranca', icon: 'landmark', group: 'Financeiro', order: 96, role: 'ADMINISTRADOR' },
]
