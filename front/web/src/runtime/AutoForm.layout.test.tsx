import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { z } from 'zod'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import { autoSize, type FieldSpec } from '@/runtime/FormField'

const loose = z.looseObject({})

function form(fields: FieldLayout, schema: z.ZodType = loose, defaults: Record<string, unknown> = {}) {
  return render(<AutoForm schema={schema as never} fields={fields} defaults={defaults as never} onSubmit={() => {}} />)
}

/** Div da grade que contem o controle do campo. */
const cell = (label: RegExp | string) => screen.getByLabelText(label).closest('div[class*="col-span-12"]') as HTMLElement

afterEach(() => vi.restoreAllMocks())

describe('tamanho automatico', () => {
  const cases: [string, FieldSpec, string][] = [
    ['textarea -> full', { name: 'a', label: 'A', kind: 'textarea' }, 'col-span-12'],
    ['boolean -> s', { name: 'a', label: 'A', kind: 'boolean' }, 'md:col-span-3'],
    ['date -> s', { name: 'a', label: 'A', kind: 'date' }, 'md:col-span-3'],
    ['number -> s', { name: 'a', label: 'A', kind: 'number' }, 'md:col-span-3'],
    ['mascara -> s', { name: 'a', label: 'A', mask: 'cpf' }, 'md:col-span-3'],
    ['enum -> m', { name: 'a', label: 'A', options: [{ value: 'x', label: 'X' }] }, 'md:col-span-4'],
    ['texto <=5 -> xs', { name: 'a', label: 'A', maxLength: 5 }, 'md:col-span-2'],
    ['texto <=15 -> s', { name: 'a', label: 'A', maxLength: 15 }, 'md:col-span-3'],
    ['texto <=30 -> m', { name: 'a', label: 'A', maxLength: 30 }, 'md:col-span-4'],
    ['texto <=100 -> l', { name: 'a', label: 'A', maxLength: 100 }, 'md:col-span-6'],
    ['texto <=255 -> xl', { name: 'a', label: 'A', maxLength: 255 }, 'md:col-span-8'],
    ['texto >255 -> full', { name: 'a', label: 'A', maxLength: 1000 }, 'col-span-12'],
    ['texto sem maxLength -> m', { name: 'a', label: 'A' }, 'md:col-span-4'],
  ]
  it.each(cases)('%s', (_n, spec, cls) => {
    form([{ section: 'S' }, [spec]])
    expect(cell('A').className).toContain(cls)
  })

  it('fk -> l', () => {
    expect(autoSize({ name: 'a', label: 'A', fk: { key: 'k', search: async () => [], labelOf: () => undefined } })).toBe('l')
  })

  it('size explicito vence o automatico', () => {
    form([{ section: 'S' }, [{ name: 'a', label: 'A', maxLength: 3, size: 'xl' }]])
    expect(cell('A').className).toContain('md:col-span-8')
    expect(cell('A').className).not.toContain('md:col-span-2')
  })
})

describe('soma da linha', () => {
  it('avisa em dev quando a linha nao soma 12', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    form([{ section: 'S' }, [{ name: 'x', label: 'X', size: 'xl' }, { name: 'y', label: 'Y', size: 'l' }]])
    expect(warn).toHaveBeenCalledTimes(1)
    expect(warn.mock.calls[0][0]).toContain('x, y')
    expect(warn.mock.calls[0][0]).toContain('14')
  })

  it('nao avisa quando soma 12', () => {
    const warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
    form([{ section: 'S' }, [{ name: 'x', label: 'X', size: 'l' }, { name: 'y', label: 'Y', size: 'l' }]])
    expect(warn).not.toHaveBeenCalled()
  })

  it('linha com menos de 12 colunas fica alinhada a esquerda (sem centralizar)', () => {
    form([{ section: 'S' }, [{ name: 'p', label: 'P', size: 's' }, { name: 'q', label: 'Q', size: 'm' }]])
    const linha = screen.getByLabelText('P').closest('.grid') as HTMLElement
    expect(linha.className).not.toContain('md:mx-auto')
    expect(linha.className).toContain('grid-cols-12')
  })
})

describe('secoes', () => {
  it('viram Cards com titulo ligado por aria-labelledby', () => {
    form([{ section: 'Identificação', description: 'Dados básicos' }, [{ name: 'a', label: 'A', size: 'full' }], { section: 'Contato' }, [{ name: 'b', label: 'B', size: 'full' }]])
    const group = screen.getByRole('group', { name: 'Identificação' })
    expect(group).toHaveAttribute('data-slot', 'card')
    expect(group).toContainElement(screen.getByLabelText('A'))
    expect(screen.getByText('Dados básicos')).toBeInTheDocument()
    expect(screen.getByRole('group', { name: 'Contato' })).toBeInTheDocument()
  })

  it('layout sem secao nao usa Card nem barra fixa', () => {
    const { container } = form([{ name: 'a', label: 'A' }])
    expect(container.querySelector('[data-slot="card"]')).toBeNull()
    expect(container.querySelector('.sticky')).toBeNull()
  })

  it('collapsible comeca recolhida', () => {
    form([{ section: 'Rara', collapsible: true }, [{ name: 'a', label: 'Campo A', size: 'full' }]])
    expect(screen.queryByLabelText('Campo A')).toBeNull()
  })

  it('collapsible abre com valor preenchido', () => {
    form([{ section: 'Rara', collapsible: true }, [{ name: 'a', label: 'Campo A', size: 'full' }]], loose, { a: 'x' })
    expect(screen.getByLabelText('Campo A')).toBeInTheDocument()
  })

  it('collapsible com defaultOpen nasce aberta', () => {
    form([{ section: 'Rara', collapsible: true, defaultOpen: true }, [{ name: 'a', label: 'Campo A', size: 'full' }]])
    expect(screen.getByLabelText('Campo A')).toBeInTheDocument()
  })

  it('collapsible abre ao clicar no titulo', async () => {
    form([{ section: 'Rara', collapsible: true }, [{ name: 'a', label: 'Campo A', size: 'full' }]])
    await userEvent.setup().click(screen.getByRole('button', { name: /Rara/ }))
    expect(screen.getByLabelText('Campo A')).toBeInTheDocument()
  })

  it('collapsible abre quando algum campo dela tem erro', async () => {
    const schema = z.object({ a: z.string().min(1), b: z.string().optional() })
    form(
      [{ section: 'Normal' }, [{ name: 'b', label: 'B', size: 'full' }], { section: 'Rara', collapsible: true }, [{ name: 'a', label: 'Campo A', size: 'full', required: true }]],
      schema,
    )
    expect(screen.queryByLabelText(/Campo A/)).toBeNull()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByLabelText(/Campo A/)).toHaveAttribute('aria-invalid', 'true')
  })

  it('mais de 5 secoes viram Tabs, com indicador de erro na aba invalida', async () => {
    const layout: FieldLayout = []
    for (let i = 1; i <= 6; i++) layout.push({ section: `Parte ${i}` }, [{ name: `c${i}`, label: `Campo ${i}`, size: 'full', required: i === 4 }])
    const schema = z.object({ c4: z.string().min(1) }).loose()
    form(layout, schema)
    expect(screen.getAllByRole('tab')).toHaveLength(6)
    expect(screen.queryByRole('group', { name: 'Parte 1' })).toBeNull()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Salvar' }))
    expect(await screen.findByRole('tab', { name: /Parte 4/ })).toHaveTextContent('(com erro)')
    expect(screen.getByRole('tab', { name: /Parte 4/ })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: /Parte 1/ })).not.toHaveTextContent('(com erro)')
  })

  it('ate 5 secoes ficam empilhadas (sem Tabs)', () => {
    const layout: FieldLayout = []
    for (let i = 1; i <= 5; i++) layout.push({ section: `Parte ${i}` }, [{ name: `c${i}`, label: `Campo ${i}`, size: 'full' }])
    form(layout)
    expect(screen.queryAllByRole('tab')).toHaveLength(0)
    expect(screen.getAllByRole('group')).toHaveLength(5)
  })
})

describe('textarea', () => {
  it.each([
    [200, 3],
    [2000, 6],
    [5000, 8],
  ])('maxLength %i -> %i linhas, com contador', async (max, rows) => {
    form([{ name: 'obs', label: 'Obs', kind: 'textarea', maxLength: max }])
    const ta = screen.getByLabelText('Obs')
    expect(ta).toHaveAttribute('rows', String(rows))
    expect(screen.getByText(`0/${max}`)).toBeInTheDocument()
    await userEvent.setup().type(ta, 'abc')
    expect(screen.getByText(`3/${max}`)).toBeInTheDocument()
  })
})

describe('barra de acoes e alteracoes nao salvas', () => {
  it('mostra o aviso ao editar e a barra e fixa', async () => {
    const { container } = form([{ section: 'S' }, [{ name: 'a', label: 'A', size: 'full' }]])
    expect(screen.queryByText('Alterações não salvas')).toBeNull()
    await userEvent.setup().type(screen.getByLabelText('A'), 'x')
    expect(screen.getByText('Alterações não salvas')).toBeInTheDocument()
    expect(container.querySelector('.sticky.bottom-0')).not.toBeNull()
  })
})

describe('width antigo', () => {
  it('linha so com width continua no layout flex', () => {
    const { container } = form([[{ name: 'a', label: 'A', width: 30 }, { name: 'b', label: 'B', width: 70 }]])
    expect(container.querySelector('.grid-cols-12')).toBeNull()
    const wrap = screen.getByLabelText('A').closest('[style]') as HTMLElement
    expect(wrap.style.getPropertyValue('--w')).toBe('30')
  })

  it('width continua valendo mesmo em layout com secoes', () => {
    const { container } = form([{ section: 'S' }, [{ name: 'a', label: 'A', width: 30 }, { name: 'b', label: 'B', width: 70 }]])
    expect(container.querySelector('.grid-cols-12')).toBeNull()
  })
})
