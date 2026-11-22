import { Bar, BarChart, CartesianGrid, XAxis } from 'recharts'
import { BuildingIcon, FileTextIcon } from 'lucide-react'
import { AspectRatio } from '@/components/ui/aspect-ratio'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Badge } from '@/components/ui/badge'
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from '@/components/ui/carousel'
import { type ChartConfig, ChartContainer, ChartLegend, ChartLegendContent, ChartTooltip, ChartTooltipContent } from '@/components/ui/chart'
import { Item, ItemActions, ItemContent, ItemDescription, ItemGroup, ItemMedia, ItemSeparator, ItemTitle } from '@/components/ui/item'
import { BlocoComponente, SecaoComponentes } from './GuiaComponentesBloco'

const chartConfig = {
  emitidas: { label: 'Emitidas', color: 'var(--primary)' },
  pagas: { label: 'Pagas', color: 'var(--success)' },
  pendentes: { label: 'Pendentes', color: 'var(--info)' },
} satisfies ChartConfig

const chartData = [
  { mes: 'Jul', emitidas: 18, pagas: 14, pendentes: 4 },
  { mes: 'Ago', emitidas: 24, pagas: 20, pendentes: 4 },
  { mes: 'Set', emitidas: 21, pagas: 16, pendentes: 5 },
  { mes: 'Out', emitidas: 30, pagas: 26, pendentes: 4 },
]

const NOTAS = [
  { numero: '1234', tomador: 'Cliente Exemplo Ltda.', valor: 'R$ 1.500,00' },
  { numero: '1235', tomador: 'Padaria do Bairro ME', valor: 'R$ 890,00' },
  { numero: '1236', tomador: 'Oficina Central', valor: 'R$ 4.800,50' },
]

export function GuiaDados() {
  return (
    <SecaoComponentes titulo="Exibição de dados">
      <BlocoComponente nome="Avatar">
        <div className="flex items-center gap-3">
          <Avatar>
            <AvatarImage src="/imagem-inexistente.png" alt="Maria Souza" />
            <AvatarFallback>MS</AvatarFallback>
          </Avatar>
          <Avatar>
            <AvatarFallback>
              <BuildingIcon className="size-4" />
            </AvatarFallback>
          </Avatar>
          <span className="text-sm text-muted-foreground">Imagem com fallback de iniciais</span>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="AspectRatio">
        <AspectRatio ratio={16 / 9} className="flex items-center justify-center rounded-md bg-muted text-sm text-muted-foreground">
          Logotipo da empresa (16:9)
        </AspectRatio>
      </BlocoComponente>

      <BlocoComponente nome="Item">
        <ItemGroup className="rounded-md border">
          {NOTAS.slice(0, 2).map((n, i) => (
            <div key={n.numero}>
              {i > 0 && <ItemSeparator />}
              <Item size="sm">
                <ItemMedia variant="icon">
                  <FileTextIcon />
                </ItemMedia>
                <ItemContent>
                  <ItemTitle>Nota {n.numero}</ItemTitle>
                  <ItemDescription>{n.tomador}</ItemDescription>
                </ItemContent>
                <ItemActions>
                  <Badge tone="success">{n.valor}</Badge>
                </ItemActions>
              </Item>
            </div>
          ))}
        </ItemGroup>
      </BlocoComponente>

      <BlocoComponente nome="Carousel">
        <div className="px-10">
          <Carousel opts={{ align: 'start' }}>
            <CarouselContent>
              {NOTAS.map((n) => (
                <CarouselItem key={n.numero}>
                  <div className="space-y-1 rounded-md border p-4 text-sm">
                    <p className="font-medium">Nota {n.numero}</p>
                    <p className="text-muted-foreground">{n.tomador}</p>
                    <p>{n.valor}</p>
                  </div>
                </CarouselItem>
              ))}
            </CarouselContent>
            <CarouselPrevious />
            <CarouselNext />
          </Carousel>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Chart" largo>
        <ChartContainer config={chartConfig} className="h-56 w-full">
          <BarChart data={chartData} accessibilityLayer>
            <CartesianGrid vertical={false} />
            <XAxis dataKey="mes" tickLine={false} axisLine={false} />
            <ChartTooltip content={<ChartTooltipContent />} />
            <ChartLegend content={<ChartLegendContent />} />
            <Bar dataKey="emitidas" fill="var(--color-emitidas)" radius={4} />
            <Bar dataKey="pagas" fill="var(--color-pagas)" radius={4} />
            <Bar dataKey="pendentes" fill="var(--color-pendentes)" radius={4} />
          </BarChart>
        </ChartContainer>
      </BlocoComponente>

    </SecaoComponentes>
  )
}
