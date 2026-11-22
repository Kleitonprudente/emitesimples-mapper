// Verificador de conformidade do padrao de UI (front/docs/padroes-de-ui.md, secao "Verificacao automatica do padrao").
// Node puro, sem dependencias. Uso: `node scripts/verificar-padrao-ui.mjs` (ou `npm run verificar:ui`).
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

/**
 * Telas escritas a mao (nao geradas) que ja existiam quando a verificacao entrou. Caminhos relativos a src/.
 * Entrada nova aqui so por DECISAO EXPLICITA da pessoa responsavel, com justificativa no comentario.
 */
export const TELAS_A_MAO = new Set([
  // Vitrine de estilo e catalogo de componentes (/admin/theme): mostram o catalogo de proposito.
  'features/admin/GuiaDeEstilo.tsx',
  'features/admin/GuiaFormulario.tsx',
  'features/admin/GuiaComponentes.tsx',
  'features/admin/GuiaComponentesBloco.tsx',
  'features/admin/GuiaComponentesDados.tsx',
  'features/admin/GuiaComponentesFeedback.tsx',
  'features/admin/GuiaComponentesFormulario.tsx',
  'features/admin/GuiaComponentesNavegacao.tsx',
  'features/admin/GuiaComponentesSobreposicoes.tsx',
  // Administracao da plataforma (fora do CRUD gerado).
  'features/admin/ImportarMunicipios.tsx',
  'features/admin/PlatformOnly.tsx',
  'features/admin/UsuariosAdmin.tsx',
  // Consulta de CNPJ: tela de configuracao singleton da plataforma, fora do padrao lista+formulario (decisao explicita do usuario).
  'features/admin/ConsultaCnpjAdmin.tsx',
  // Integracao IntegraNotas: tela de configuracao singleton da plataforma, fora do padrao lista+formulario (decisao explicita do usuario).
  'features/admin/IntegraNotasAdmin.tsx',
  // Configuracao > NFS-e da empresa: tela de configuracao singleton da empresa, fora do padrao lista+formulario (decisao explicita do usuario).
  'features/empresa-config/NfseConfig.tsx',
  // componente à mão montado como aba extra do cadastro de Empresa (gerador extraTabs); decisão explícita do usuário
  'features/admin/VinculosDaEmpresa.tsx',
  // Exemplo de referencia da lista com DataTable.
  'examples/PessoasExample.tsx',
])

/** Componentes de components/ (fora de ui) e runtime/ que ja existiam e estao no catalogo. Um .tsx novo ali falha. */
export const COMPONENTES_CATALOGADOS = new Set([
  'components/AppShell.tsx',
  'components/EnvBadge.tsx',
  'components/ThemeToggle.tsx',
  'runtime/AutoForm.tsx',
  'runtime/CardList.tsx',
  'runtime/Combobox.tsx',
  'runtime/DataTable.tsx',
  'runtime/FormField.tsx',
  'runtime/SearchInput.tsx',
  'runtime/icons.tsx',
  'runtime/states.tsx',
])

const MSG_COMPONENTE_NOVO =
  'Componente visual novo é proibido: use o catálogo (front/docs/catalogo-componentes.md) ou peça decisão'

const TAMANHOS = { xs: 2, s: 3, m: 4, l: 6, xl: 8, full: 12 }

const PALETAS =
  'red|blue|green|slate|gray|zinc|neutral|stone|amber|orange|yellow|lime|emerald|teal|cyan|sky|indigo|violet|purple|fuchsia|pink|rose'
const RE_COR_PALETA = new RegExp(
  `(?<![\\w-])(?:[\\w-]+:)*(?:bg|text|border|ring|fill|stroke|from|to|via)-(?:${PALETAS})-\\d{2,3}(?![\\w-])`,
)
const RE_COR_BASICA = /(?<![\w-])(?:[\w-]+:)*(?:text-white|bg-white|bg-black|text-black)(?![\w-])/
const RE_HEX = /#[0-9a-fA-F]{3,8}(?![\w-])/
const RE_FUNC_COR = /\b(?:rgba?|hsla?)\(/

const PACOTES_PERMITIDOS = [
  'react',
  'react-dom',
  'react-hook-form',
  '@hookform/resolvers',
  'zod',
  'lucide-react',
  'sonner',
  'date-fns',
]
const ALIAS_PERMITIDOS = [
  /^@\/components\/ui\/[\w.-]+$/,
  /^@\/runtime\/[\w.-]+$/,
  /^@\/lib\/[\w./-]+$/,
  /^@\/gen\/[\w./-]+$/,
  /^@\/features\/[\w./-]+$/,
  /^@\/config\/[\w./-]+$/,
  /^@\/examples\/[\w.-]+$/, // exemplo de referencia (linha de base)
  /^@\/hooks\/[\w./-]+$/,
  /^@\/components\/(AppShell|EnvBadge|ThemeToggle)$/,
]

function ehCodigoVerificavel(rel) {
  if (!/\.(tsx|ts)$/.test(rel) || rel.endsWith('.d.ts')) return false
  if (/\.test\.[a-z]+$/.test(rel)) return false
  if (rel === 'routeTree.gen.ts') return false
  if (rel.startsWith('components/ui/') || rel.startsWith('test/') || rel.startsWith('mocks/')) return false
  return true
}

function listar(dir, base = dir, acc = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) listar(p, base, acc)
    else acc.push(path.relative(base, p).split(path.sep).join('/'))
  }
  return acc.sort()
}

const linhaDe = (texto, idx) => texto.slice(0, idx).split('\n').length

/** Troca comentarios por espacos (preserva posicoes/linhas) para nao gerar falso positivo em texto de comentario. */
function semComentarios(src) {
  return src
    .replace(/\/\*[\s\S]*?\*\//g, (m) => m.replace(/[^\n]/g, ' '))
    .replace(/(^|[^:\\'"`])\/\/[^\n]*/g, (m, p) => p + ' '.repeat(m.length - p.length))
}

function ehGerado(src) {
  return src.startsWith('// GERADO por')
}

function emTela(rel) {
  return (
    rel.startsWith('features/') || rel.startsWith('routes/') || rel.startsWith('gen/') || rel.startsWith('examples/')
  )
}

// ---------------------------------------------------------------- R1
const RE_DECL_COMPONENTE =
  /(?:^|\n)\s*(?:export\s+)?(?:default\s+)?(?:(?:async\s+)?function\s+([A-Z]\w*)\s*[<(]|const\s+([A-Z]\w*)\s*(?::[^=\n]+)?=\s*(?:\(|async\b|function\b|(?:React\.)?(?:memo|forwardRef)\s*[<(]|<[^>]*>\s*\())/g
const RE_JSX = /<\/[A-Za-z]|\/>|<[A-Z][\w.]*[\s>]/

function regraR1(rel, src, out) {
  const ehTsx = rel.endsWith('.tsx')
  if (!ehTsx) return
  const noCatalogo = rel.startsWith('components/') || rel.startsWith('runtime/')
  if (noCatalogo) {
    if (!COMPONENTES_CATALOGADOS.has(rel))
      out.push({ regra: 'R1', arquivo: rel, linha: 1, mensagem: MSG_COMPONENTE_NOVO })
    return
  }
  if (rel === 'main.tsx' || rel.startsWith('routes/') || ehGerado(src) || TELAS_A_MAO.has(rel)) return
  const limpo = semComentarios(src)
  if (!RE_JSX.test(limpo)) return
  RE_DECL_COMPONENTE.lastIndex = 0
  const m = RE_DECL_COMPONENTE.exec(limpo)
  if (m) {
    const nome = m[1] ?? m[2]
    out.push({
      regra: 'R1',
      arquivo: rel,
      linha: linhaDe(limpo, m.index + m[0].indexOf(nome)),
      mensagem: `${MSG_COMPONENTE_NOVO} (declara ${nome})`,
    })
  }
}

// ---------------------------------------------------------------- R2
function importsDe(limpo) {
  const res = []
  const re = /(?:\bimport|\bexport)\s+(?:type\s+)?(?:[\w*${},\s]+?\s+from\s+)?['"]([^'"]+)['"]|\bimport\(\s*['"]([^'"]+)['"]\s*\)/g
  let m
  while ((m = re.exec(limpo))) {
    const spec = m[1] ?? m[2]
    if (m[0].startsWith('export') && !/\sfrom\s/.test(m[0])) continue
    res.push({ spec, idx: m.index })
  }
  return res
}

function importPermitido(spec, rel) {
  if (spec.startsWith('./') || spec.startsWith('../') || spec === '.' || spec === '..') return true
  if (spec.startsWith('@/')) return ALIAS_PERMITIDOS.some((r) => r.test(spec))
  if (/^@tanstack\/[\w-]+(\/[\w./-]+)?$/.test(spec)) return true
  if (/^recharts(\/.*)?$/.test(spec)) return rel.startsWith('features/admin/')
  return PACOTES_PERMITIDOS.some((p) => spec === p || spec.startsWith(`${p}/`))
}

function regraR2(rel, limpo, out) {
  if (!emTela(rel)) return
  for (const { spec, idx } of importsDe(limpo)) {
    if (!importPermitido(spec, rel)) {
      const dica = /^radix-ui|^@radix-ui/.test(spec) ? ' (radix direto é proibido nas telas: use @/components/ui)' : ''
      out.push({ regra: 'R2', arquivo: rel, linha: linhaDe(limpo, idx), mensagem: `Import não permitido: '${spec}'${dica}` })
    }
  }
}

// ---------------------------------------------------------------- R3
const EXCECAO_COR = /^features\/admin\/(GuiaDeEstilo|GuiaComponentes)[\w]*\.tsx$|^features\/admin\/GuiaFormulario\.tsx$/

function regraR3(rel, limpo, out) {
  if (EXCECAO_COR.test(rel)) return
  const linhas = limpo.split('\n')
  linhas.forEach((l, i) => {
    let msg = null
    const mp = RE_COR_PALETA.exec(l)
    if (mp) msg = `Cor fixa da paleta do Tailwind: '${mp[0].trim()}' (use os tokens do tema)`
    else {
      const mb = RE_COR_BASICA.exec(l)
      if (mb) msg = `Cor fixa: '${mb[0].trim()}' (use os tokens do tema)`
      else if (/className|style|\bcn\(|clsx\(|-\[/.test(l)) {
        if (RE_HEX.test(l) || RE_FUNC_COR.test(l)) msg = 'Cor fixa (#hex, rgb() ou hsl()) em classe ou estilo (use os tokens do tema)'
      }
    }
    if (msg) out.push({ regra: 'R3', arquivo: rel, linha: i + 1, mensagem: msg })
  })
}

// ---------------------------------------------------------------- R4
/** Devolve o texto entre o colchete que abre em `ini` e o que fecha (respeitando strings e aninhamento). */
function blocoBalanceado(s, ini) {
  const abre = s[ini]
  const fecha = abre === '[' ? ']' : abre === '{' ? '}' : ')'
  let prof = 0
  let aspas = null
  for (let i = ini; i < s.length; i++) {
    const c = s[i]
    if (aspas) {
      if (c === '\\') i++
      else if (c === aspas) aspas = null
      continue
    }
    if (c === "'" || c === '"' || c === '`') aspas = c
    else if (c === abre) prof++
    else if (c === fecha && --prof === 0) return { fim: i, texto: s.slice(ini + 1, i) }
  }
  return null
}

/** Itens de primeiro nivel (objetos `{}` ou arrays `[]`) de um texto de lista. */
function itensDeNivelUm(texto) {
  const itens = []
  let i = 0
  while (i < texto.length) {
    const c = texto[i]
    if (c === '{' || c === '[') {
      const b = blocoBalanceado(texto, i)
      if (!b) break
      itens.push({ tipo: c, texto: b.texto, ini: i })
      i = b.fim + 1
    } else i++
  }
  return itens
}

function regraR4(rel, src, limpo, out) {
  if (!rel.startsWith('features/') || !ehGerado(src)) return
  const base = path.posix.basename(rel)
  if (!/^Formulario.*\.tsx$/.test(base) || base.endsWith('Screen.tsx')) return
  const m = /const\s+FIELDS\s*(?::\s*[\w<>[\]]+)?\s*=\s*\[/.exec(limpo)
  if (!m) {
    out.push({ regra: 'R4', arquivo: rel, linha: 1, mensagem: 'Formulário gerado sem `const FIELDS`' })
    return
  }
  const ini = m.index + m[0].length - 1
  const lista = blocoBalanceado(limpo, ini)
  if (!lista) return
  for (const item of itensDeNivelUm(lista.texto)) {
    const linha = linhaDe(limpo, ini + 1 + item.ini)
    if (item.tipo === '{') {
      const sec = /\bsection\s*:\s*(['"`])(.*?)\1/.exec(item.texto)
      if (sec) {
        if (!sec[2].trim()) out.push({ regra: 'R4', arquivo: rel, linha, mensagem: 'Seção sem título' })
        continue
      }
      if (/\bsection\b/.test(item.texto)) {
        out.push({ regra: 'R4', arquivo: rel, linha, mensagem: 'Seção sem título' })
        continue
      }
      // campo solto: trata como linha de um campo
      somarLinha(`{${item.texto}}`, rel, linha, out)
    } else somarLinha(item.texto, rel, linha, out)
  }
}

function somarLinha(texto, rel, linha, out) {
  const campos = itensDeNivelUm(texto).filter((x) => x.tipo === '{')
  let soma = 0
  let ok = true
  for (const campo of campos) {
    const nome = /\bname\s*:\s*['"`]([^'"`]+)/.exec(campo.texto)?.[1] ?? '?'
    // size de primeiro nivel do campo: remove blocos aninhados antes de procurar
    const plano = achatar(campo.texto)
    const sz = /\bsize\s*:\s*['"`](\w+)['"`]/.exec(plano)?.[1]
    if (!sz) {
      out.push({ regra: 'R4', arquivo: rel, linha, mensagem: `Campo '${nome}' sem size explícito` })
      ok = false
    } else if (!(sz in TAMANHOS)) {
      out.push({ regra: 'R4', arquivo: rel, linha, mensagem: `Campo '${nome}' com size inválido '${sz}'` })
      ok = false
    } else soma += TAMANHOS[sz]
  }
  if (ok && soma !== 12)
    out.push({ regra: 'R4', arquivo: rel, linha, mensagem: `Linha do formulário soma ${soma}, deve somar 12` })
}

/** Remove conteudo de parenteses/colchetes/chaves aninhados e strings, mantendo `size: 'x'` de primeiro nivel. */
function achatar(texto) {
  let res = ''
  let prof = 0
  let aspas = null
  let atual = ''
  for (let i = 0; i < texto.length; i++) {
    const c = texto[i]
    if (aspas) {
      atual += c
      if (c === '\\') atual += texto[++i] ?? ''
      else if (c === aspas) {
        aspas = null
        if (prof === 0) res += atual
        atual = ''
      }
      continue
    }
    if (c === "'" || c === '"' || c === '`') {
      aspas = c
      atual = c
    } else if ('([{'.includes(c)) prof++
    else if (')]}'.includes(c)) prof--
    else if (prof === 0) res += c
  }
  return res
}

// ---------------------------------------------------------------- R5
function regraR5(rel, src, limpo, out) {
  if (!rel.startsWith('features/')) return
  const base = path.posix.basename(rel)
  if (/^(Criar|Editar)\w*\.tsx$/.test(base))
    out.push({
      regra: 'R5',
      arquivo: rel,
      linha: 1,
      mensagem: 'Cadastro sem criação/edição separada: use a rota /x/$id (novo cria) com o formulário único',
    })
  if (!ehGerado(src) || !/^Gestao\w*Screen\.tsx$/.test(base)) return
  const proibidos = [
    [/\bDialog\b/, 'Gestão não usa Dialog: a criação é pela rota /x/novo'],
    [/\buseState\b/, 'Gestão não usa useState de diálogo de criação: a criação é pela rota /x/novo'],
    [/\buseCriar\w*/, 'Gestão não usa useCriar*: a criação é pela rota /x/novo'],
  ]
  limpo.split('\n').forEach((l, i) => {
    for (const [re, mensagem] of proibidos) if (re.test(l)) out.push({ regra: 'R5', arquivo: rel, linha: i + 1, mensagem })
  })
}

// ---------------------------------------------------------------- R6
function regraR6(rel, limpo, out) {
  if (!rel.endsWith('.tsx')) return
  const re = /<([A-Z][\w.]*)(?=[\s/>])/g
  let m
  while ((m = re.exec(limpo))) {
    const ini = m.index
    let i = ini + m[0].length
    let prof = 0
    let aspas = null
    for (; i < limpo.length; i++) {
      const c = limpo[i]
      if (aspas) {
        if (c === '\\') i++
        else if (c === aspas) aspas = null
        continue
      }
      if (prof > 0 && (c === "'" || c === '"' || c === '`')) aspas = c
      else if (prof === 0 && (c === '"' || c === "'")) aspas = c
      else if (c === '{') prof++
      else if (c === '}') prof--
      else if (c === '>' && prof === 0) break
    }
    const tag = limpo.slice(ini, i + 1)
    if (/\bsize=(?:"icon(?:-[\w]+)?"|'icon(?:-[\w]+)?'|\{\s*['"]icon(?:-[\w]+)?['"]\s*\})/.test(tag) && !/\baria-label(?:ledby)?\s*=/.test(tag))
      out.push({
        regra: 'R6',
        arquivo: rel,
        linha: linhaDe(limpo, ini),
        mensagem: `Botão só com ícone (<${m[1]} size="icon">) sem aria-label`,
      })
  }
}

// ---------------------------------------------------------------- API
/** Verifica todos os arquivos de `raizSrc`. Devolve `{ violacoes, total }`. */
export function verificarDetalhado(raizSrc) {
  const violacoes = []
  let total = 0
  for (const rel of listar(raizSrc)) {
    if (!ehCodigoVerificavel(rel)) continue
    total++
    const src = fs.readFileSync(path.join(raizSrc, rel), 'utf8').replace(/^﻿/, '').replace(/\r\n/g, '\n')
    const limpo = semComentarios(src)
    regraR1(rel, src, violacoes)
    regraR2(rel, limpo, violacoes)
    regraR3(rel, limpo, violacoes)
    regraR4(rel, src, limpo, violacoes)
    regraR5(rel, src, limpo, violacoes)
    regraR6(rel, limpo, violacoes)
  }
  return { violacoes, total }
}

/** Lista de violacoes `{regra, arquivo, linha, mensagem}`. */
export function verificar(raizSrc) {
  return verificarDetalhado(raizSrc).violacoes
}

function principal() {
  const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../src')
  const { violacoes, total } = verificarDetalhado(raiz)
  if (violacoes.length === 0) {
    console.log(`Padrão de UI: OK (${total} arquivos verificados)`)
    return 0
  }
  const porRegra = new Map()
  for (const v of violacoes) porRegra.set(v.regra, [...(porRegra.get(v.regra) ?? []), v])
  for (const regra of [...porRegra.keys()].sort()) {
    console.error(`\n${regra} (${porRegra.get(regra).length})`)
    for (const v of porRegra.get(regra)) console.error(`  src/${v.arquivo}:${v.linha}  ${v.mensagem}`)
  }
  console.error(`\nPadrão de UI: ${violacoes.length} violação(ões) em ${total} arquivos. Veja front/docs/padroes-de-ui.md.`)
  return 1
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  process.exitCode = principal()
}
