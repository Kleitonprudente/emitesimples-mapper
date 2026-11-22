import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { verificar } from '../../scripts/verificar-padrao-ui.mjs'

const RAIZ_SRC = path.resolve(__dirname, '..')

describe('padrao de UI', () => {
  it('o repositorio atual nao tem violacoes', () => {
    expect(verificar(RAIZ_SRC)).toEqual([])
  })
})

describe('regras do verificador (fumaca)', () => {
  let tmp: string
  beforeEach(() => {
    tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'padrao-ui-'))
  })
  afterEach(() => {
    fs.rmSync(tmp, { recursive: true, force: true })
  })

  function escrever(rel: string, conteudo: string) {
    const p = path.join(tmp, rel)
    fs.mkdirSync(path.dirname(p), { recursive: true })
    fs.writeFileSync(p, conteudo)
  }
  const regras = () => verificar(tmp).map((v) => v.regra)

  it('R1: componente novo em features', () => {
    escrever('features/x/Novo.tsx', "export function Novo() {\n  return <div>oi</div>\n}\n")
    const v = verificar(tmp)
    expect(v).toHaveLength(1)
    expect(v[0]).toMatchObject({ regra: 'R1', arquivo: 'features/x/Novo.tsx' })
    expect(v[0]!.mensagem).toContain('Componente visual novo é proibido')
  })

  it('R1: .tsx novo em runtime', () => {
    escrever('runtime/Novo.tsx', "export const Novo = () => <div />\n")
    expect(regras()).toContain('R1')
  })

  it('R1: arquivo gerado nao e violacao', () => {
    escrever('features/x/Tela.tsx', "// GERADO por x\nexport function Tela() {\n  return <div />\n}\n")
    expect(verificar(tmp)).toEqual([])
  })

  it('R2: import proibido', () => {
    escrever('routes/x.tsx', "import { Dialog } from 'radix-ui'\nimport { a } from 'axios'\nexport const Route = {}\n")
    const v = verificar(tmp).filter((x) => x.regra === 'R2')
    expect(v).toHaveLength(2)
    expect(v[0]!.linha).toBe(1)
  })

  it('R3: cor fixa', () => {
    escrever('lib/x.tsx', "export const c = 'bg-red-500 text-white'\nexport const d = <p style={{ color: '#ff0000' }} />\n")
    const v = verificar(tmp).filter((x) => x.regra === 'R3')
    expect(v.map((x) => x.linha)).toEqual([1, 2])
  })

  it('R4: linha que soma 14 e campo sem size', () => {
    escrever(
      'features/x/FormularioX.tsx',
      `// GERADO por x
const FIELDS: FieldLayout = [
  { section: 'Dados' },
  [
    { name: 'a', label: 'A', size: 'l' },
    { name: 'b', label: 'B', size: 'xl' },
  ],
  [
    { name: 'c', label: 'C' },
  ],
  { section: '' },
]
`,
    )
    const msgs = verificar(tmp)
      .filter((x) => x.regra === 'R4')
      .map((x) => x.mensagem)
    expect(msgs).toEqual(expect.arrayContaining(['Linha do formulário soma 14, deve somar 12', "Campo 'c' sem size explícito", 'Seção sem título']))
  })

  it('R5: Gestao com Dialog, useState e useCriar', () => {
    escrever(
      'features/x/GestaoXScreen.tsx',
      "// GERADO por x\nimport { Dialog } from '@/components/ui/dialog'\nimport { useState } from 'react'\nimport { useCriarX } from '@/gen/api/x'\n",
    )
    const v = verificar(tmp).filter((x) => x.regra === 'R5')
    expect(v.length).toBeGreaterThanOrEqual(3)
  })

  it('R5: Criar*.tsx em features', () => {
    escrever('features/x/CriarX.tsx', "// GERADO por x\nexport const a = 1\n")
    expect(regras()).toContain('R5')
  })

  it('R6: botao so com icone sem aria-label', () => {
    escrever(
      'features/x/Tela.tsx',
      "// GERADO por x\nexport const T = () => (\n  <div>\n    <Button size=\"icon\" onClick={() => go()}><X /></Button>\n    <Button size=\"icon\" aria-label=\"Fechar\"><X /></Button>\n  </div>\n)\n",
    )
    const v = verificar(tmp).filter((x) => x.regra === 'R6')
    expect(v).toHaveLength(1)
    expect(v[0]!.linha).toBe(4)
  })
})
