import type { Papel } from '@/lib/api'

/* Dados de exemplo realistas e deterministicos. CPF/CNPJ com digitos verificadores VALIDOS (fictícios). */

function cpfFrom(base9: string): string {
  const calc = (digits: string, weightStart: number) => {
    const sum = digits.split('').reduce((s, d, i) => s + Number(d) * (weightStart - i), 0)
    const r = (sum * 10) % 11
    return r === 10 ? 0 : r
  }
  const d1 = calc(base9, 10)
  const d2 = calc(base9 + d1, 11)
  return `${base9}${d1}${d2}`
}

function cnpjFrom(base12: string): string {
  const calc = (digits: string) => {
    const w = digits.length === 12 ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]
    const sum = digits.split('').reduce((s, d, i) => s + Number(d) * w[i]!, 0)
    const r = sum % 11
    return r < 2 ? 0 : 11 - r
  }
  const d1 = calc(base12)
  const d2 = calc(base12 + d1)
  return `${base12}${d1}${d2}`
}

export const MUNICIPIOS = [
  { id: 'mun-3550308', nome: 'São Paulo', uf: 'SP', codigoIbge: '3550308' },
  { id: 'mun-3304557', nome: 'Rio de Janeiro', uf: 'RJ', codigoIbge: '3304557' },
  { id: 'mun-3106200', nome: 'Belo Horizonte', uf: 'MG', codigoIbge: '3106200' },
  { id: 'mun-4106902', nome: 'Curitiba', uf: 'PR', codigoIbge: '4106902' },
  { id: 'mun-4314902', nome: 'Porto Alegre', uf: 'RS', codigoIbge: '4314902' },
  { id: 'mun-2927408', nome: 'Salvador', uf: 'BA', codigoIbge: '2927408' },
  { id: 'mun-2304400', nome: 'Fortaleza', uf: 'CE', codigoIbge: '2304400' },
  { id: 'mun-5300108', nome: 'Brasília', uf: 'DF', codigoIbge: '5300108' },
  { id: 'mun-4205407', nome: 'Florianópolis', uf: 'SC', codigoIbge: '4205407' },
  { id: 'mun-3509502', nome: 'Campinas', uf: 'SP', codigoIbge: '3509502' },
]

export type Municipio = (typeof MUNICIPIOS)[number]

export interface Pessoa {
  id: string
  tipoPessoa: 'fisica' | 'juridica'
  documento: string
  nome: string
  nomeFantasia?: string
  email?: string
  telefone?: string
  cep?: string
  logradouro?: string
  numeroEndereco?: string
  bairro?: string
  cidade?: { id: string; nome?: string; uf?: string; codigoIbge?: string }
  dataCriacao: string
  dataAtualizacao: string
}

const NOMES = ['Ana Paula Ribeiro', 'Carlos Eduardo Lima', 'Mariana Souza Alves', 'João Pedro Martins', 'Fernanda Costa Dias', 'Ricardo Nogueira', 'Patrícia Gomes', 'Lucas Andrade', 'Beatriz Carvalho', 'Rafael Teixeira', 'Camila Barbosa', 'Gustavo Pereira']
const EMPRESAS = ['Padaria Pão Quente', 'Oficina Mecânica Silva', 'Clínica Sorriso Ltda', 'Studio Bella Estética', 'Contabilidade Horizonte', 'Mercadinho Bom Preço', 'Escola Aprender Mais', 'Transportes Rápido Sul', 'Design Criativo ME', 'Farmácia Vida Saudável']
const RUAS = ['Rua das Flores', 'Avenida Brasil', 'Rua XV de Novembro', 'Rua Sete de Setembro', 'Avenida Paulista', 'Rua da Paz']
const BAIRROS = ['Centro', 'Jardim América', 'Vila Nova', 'Boa Vista', 'Santa Cruz']

export function buildPessoas(): Pessoa[] {
  const out: Pessoa[] = []
  for (let i = 0; i < 37; i++) {
    const fisica = i % 3 !== 0
    const nome = fisica ? NOMES[i % NOMES.length]! : EMPRESAS[i % EMPRESAS.length]!
    const slug = nome.toLowerCase().normalize('NFD').replace(/[^a-z ]/g, '').split(' ')[0]
    const base = String(123456000 + i * 7919).slice(0, 9)
    const cidade = MUNICIPIOS[i % MUNICIPIOS.length]!
    const dia = String((i % 27) + 1).padStart(2, '0')
    out.push({
      id: `pes-${String(i + 1).padStart(4, '0')}`,
      tipoPessoa: fisica ? 'fisica' : 'juridica',
      documento: fisica ? cpfFrom(base) : cnpjFrom(`${String(10234567 + i * 31).slice(0, 8)}0001`),
      nome: fisica ? nome : `${nome} ${i % 2 ? 'Ltda' : 'ME'}`,
      nomeFantasia: fisica ? undefined : nome,
      email: `${slug}${i}@exemplo.com.br`,
      telefone: `119${String(80000000 + i * 12345).slice(0, 8)}`,
      cep: String(1000000 + i * 4321).padStart(8, '0'),
      logradouro: RUAS[i % RUAS.length],
      numeroEndereco: String(10 + i * 3),
      bairro: BAIRROS[i % BAIRROS.length],
      cidade: { ...cidade },
      dataCriacao: `2026-0${(i % 9) + 1}-${dia}T10:${String(i % 60).padStart(2, '0')}:00`,
      dataAtualizacao: `2026-10-${dia}T15:30:00`,
    })
  }
  return out
}

/* ----- usuarios de teste do modo mock (senha unica: senha1234) ----- */

export const MOCK_SENHA = 'senha1234'

export interface MockEmpresa {
  id: string
  razaoSocial: string
  nomeFantasia: string | null
}

export const EMPRESAS_MOCK: Record<string, MockEmpresa> = {
  'emp-1': { id: 'emp-1', razaoSocial: 'Horizonte Serviços Contábeis Ltda', nomeFantasia: 'Horizonte Contábil' },
  'emp-2': { id: 'emp-2', razaoSocial: 'Studio Bella Estética ME', nomeFantasia: 'Studio Bella' },
}

export interface MockUser {
  id: string
  nome: string
  email: string
  vinculos: { empresaId: string; papel: Papel }[]
  plataformaAdmin?: boolean
}

export const MOCK_USERS: MockUser[] = [
  { id: 'usu-1', nome: 'Ana Ribeiro', email: 'admin@exemplo.com.br', vinculos: [{ empresaId: 'emp-1', papel: 'administrador' }] },
  { id: 'usu-2', nome: 'Bruno Operador', email: 'operador@exemplo.com.br', vinculos: [{ empresaId: 'emp-1', papel: 'operador' }] },
  { id: 'usu-3', nome: 'Carla Consulta', email: 'consulta@exemplo.com.br', vinculos: [{ empresaId: 'emp-1', papel: 'consulta' }] },
  {
    id: 'usu-4',
    nome: 'Diego Multiempresa',
    email: 'multi@exemplo.com.br',
    vinculos: [
      { empresaId: 'emp-1', papel: 'administrador' },
      { empresaId: 'emp-2', papel: 'operador' },
    ],
  },
  { id: 'usu-5', nome: 'Eva Plataforma', email: 'plataforma@exemplo.com.br', vinculos: [], plataformaAdmin: true },
]

/** Este e-mail sempre responde 429 (para ver a mensagem de espera). */
export const MOCK_EMAIL_BLOQUEADO = 'bloqueado@exemplo.com.br'
