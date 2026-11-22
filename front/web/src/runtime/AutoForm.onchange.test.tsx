import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { z } from 'zod'
import { AutoForm } from '@/runtime/AutoForm'

const schema = z.object({ a: z.string().optional(), b: z.string().optional() })
const fields = [[{ name: 'a', label: 'Campo A', size: 'l' as const }, { name: 'b', label: 'Campo B', size: 'l' as const }]]

describe('AutoForm onValuesChange', () => {
  it('avisa o campo alterado com os valores e permite preencher outro campo via setValue', () => {
    const aoMudar = vi.fn((_v: unknown, ctx: { name?: string; setValue: (n: string, v: unknown) => void }) => {
      if (ctx.name === 'a') ctx.setValue('b', 'derivado')
    })
    render(<AutoForm schema={schema} fields={fields} defaults={{ a: '', b: '' }} onSubmit={() => {}} onValuesChange={aoMudar} />)
    fireEvent.change(screen.getByLabelText('Campo A'), { target: { value: 'x' } })
    expect(aoMudar).toHaveBeenCalledWith(expect.objectContaining({ a: 'x' }), expect.objectContaining({ name: 'a' }))
    expect(screen.getByLabelText('Campo B')).toHaveValue('derivado')
  })

  it('sem o callback nada muda no comportamento', () => {
    render(<AutoForm schema={schema} fields={fields} defaults={{ a: '', b: '' }} onSubmit={() => {}} />)
    fireEvent.change(screen.getByLabelText('Campo A'), { target: { value: 'x' } })
    expect(screen.getByLabelText('Campo B')).toHaveValue('')
  })
})
