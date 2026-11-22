import { useState } from 'react'
import { AlertTriangleIcon, CheckCircle2Icon, FileXIcon, InfoIcon, XCircleIcon } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from '@/components/ui/empty'
import { Progress } from '@/components/ui/progress'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { Spinner } from '@/components/ui/spinner'
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from '@/components/ui/resizable'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
} from '@/components/ui/sidebar'
import { BlocoComponente, SecaoComponentes } from './GuiaComponentesBloco'

export function GuiaFeedback() {
  const [progresso, setProgresso] = useState(40)
  return (
    <SecaoComponentes titulo="Feedback e estado">
      <BlocoComponente nome="Alert" largo>
        <div className="grid gap-3 md:grid-cols-2">
          <Alert>
            <InfoIcon />
            <AlertTitle>Certificado digital</AlertTitle>
            <AlertDescription>Envie o certificado A1 para emitir notas.</AlertDescription>
          </Alert>
          <Alert variant="success">
            <CheckCircle2Icon />
            <AlertTitle>Nota autorizada</AlertTitle>
            <AlertDescription>A prefeitura autorizou a nota 1234.</AlertDescription>
          </Alert>
          <Alert variant="warning">
            <AlertTriangleIcon />
            <AlertTitle>Certificado vence em 7 dias</AlertTitle>
            <AlertDescription>Renove para não interromper a emissão.</AlertDescription>
          </Alert>
          <Alert variant="destructive">
            <XCircleIcon />
            <AlertTitle>Nota rejeitada</AlertTitle>
            <AlertDescription>O município rejeitou a nota: CNAE inválido.</AlertDescription>
          </Alert>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Progress">
        <div className="space-y-2">
          <Progress value={progresso} aria-label="Importação de notas" />
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Importando notas: {progresso}%</span>
            <Button size="sm" variant="outline" onClick={() => setProgresso((p) => (p >= 100 ? 0 : p + 20))}>
              Avançar
            </Button>
          </div>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Spinner">
        <p className="flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner /> Consultando a prefeitura...
        </p>
      </BlocoComponente>

      <BlocoComponente nome="Skeleton">
        <div className="flex items-center gap-3">
          <Skeleton className="size-10 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Empty">
        <Empty className="border border-dashed">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <FileXIcon />
            </EmptyMedia>
            <EmptyTitle>Nenhuma nota emitida</EmptyTitle>
            <EmptyDescription>As notas emitidas neste período aparecem aqui.</EmptyDescription>
          </EmptyHeader>
          <EmptyContent>
            <Button size="sm">Emitir primeira nota</Button>
          </EmptyContent>
        </Empty>
      </BlocoComponente>
    </SecaoComponentes>
  )
}

const LINHAS = Array.from({ length: 12 }, (_, i) => `Nota ${1230 + i}`)

export function GuiaLayout() {
  return (
    <SecaoComponentes titulo="Layout">
      <BlocoComponente nome="Separator">
        <div className="space-y-2 text-sm">
          <p>Dados da empresa</p>
          <Separator />
          <div className="flex h-5 items-center gap-3">
            <span>Notas</span>
            <Separator orientation="vertical" />
            <span>Impostos</span>
            <Separator orientation="vertical" />
            <span>Relatórios</span>
          </div>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="ScrollArea">
        <ScrollArea className="h-32 rounded-md border">
          <ul className="space-y-1 p-3 text-sm">
            {LINHAS.map((l) => (
              <li key={l}>{l}</li>
            ))}
          </ul>
        </ScrollArea>
      </BlocoComponente>

      <BlocoComponente nome="Resizable">
        <div className="h-32 overflow-hidden rounded-md border">
          <ResizablePanelGroup orientation="horizontal">
            <ResizablePanel defaultSize={40} minSize={20}>
              <div className="flex h-full items-center justify-center text-sm">Lista</div>
            </ResizablePanel>
            <ResizableHandle withHandle />
            <ResizablePanel defaultSize={60} minSize={20}>
              <div className="flex h-full items-center justify-center text-sm">Detalhe</div>
            </ResizablePanel>
          </ResizablePanelGroup>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Sidebar" largo>
        <div className="h-64 overflow-hidden rounded-md border">
          {/* Provider isolado; collapsible="none" mantém a barra dentro da caixa (a barra real usa posição fixa). */}
          <SidebarProvider className="min-h-0 h-full">
            <Sidebar collapsible="none" className="border-r">
              <SidebarHeader className="px-3 py-2 text-sm font-semibold">Emite Simples</SidebarHeader>
              <SidebarContent>
                <SidebarGroup>
                  <SidebarGroupLabel>Operação</SidebarGroupLabel>
                  <SidebarGroupContent>
                    <SidebarMenu>
                      <SidebarMenuItem>
                        <SidebarMenuButton isActive>Notas</SidebarMenuButton>
                        <SidebarMenuBadge>12</SidebarMenuBadge>
                      </SidebarMenuItem>
                      <SidebarMenuItem>
                        <SidebarMenuButton>Pessoas</SidebarMenuButton>
                      </SidebarMenuItem>
                      <SidebarMenuItem>
                        <SidebarMenuButton>Empresas</SidebarMenuButton>
                      </SidebarMenuItem>
                    </SidebarMenu>
                  </SidebarGroupContent>
                </SidebarGroup>
              </SidebarContent>
            </Sidebar>
            <SidebarInset className="p-4 text-sm text-muted-foreground">Conteúdo da página ao lado do menu lateral.</SidebarInset>
          </SidebarProvider>
        </div>
      </BlocoComponente>
    </SecaoComponentes>
  )
}
