// GERADO por frontend/screenRoute a partir de ImportarExportarCsv. Nao edite: a saida e sobrescrita por inteiro.
// Arquivo de destino: src/routes/_authenticated/financeiro/csv/index.tsx
import { createFileRoute } from '@tanstack/react-router'
import { CsvFinanceiroScreen } from '@/features/financeiro/csv/CsvFinanceiro'

// window.custom: a tela e declarada no mapa e o componente e escrito a mao (front/mapas/README.md, "window.custom").
export const Route = createFileRoute('/_authenticated/financeiro/csv/')({
  component: CsvFinanceiroScreen,
})
