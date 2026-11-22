import type { ReactNode } from 'react'

/** Secao da vitrine de componentes: titulo (h2) e grade responsiva de blocos. */
export function SecaoComponentes({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold tracking-tight">{titulo}</h2>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">{children}</div>
    </section>
  )
}

/** Bloco rotulado com o nome do componente (h3) e um exemplo compacto. */
export function BlocoComponente({ nome, largo = false, children }: { nome: string; largo?: boolean; children: ReactNode }) {
  return (
    <div className={`min-w-0 space-y-2 rounded-lg border bg-card p-4 text-card-foreground ${largo ? 'md:col-span-2 xl:col-span-3' : ''}`}>
      <h3 className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{nome}</h3>
      <div className="min-w-0">{children}</div>
    </div>
  )
}
