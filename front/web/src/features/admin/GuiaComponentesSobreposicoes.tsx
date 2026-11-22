import { InfoIcon } from 'lucide-react'
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import {
  ContextMenu,
  ContextMenuCheckboxItem,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuLabel,
  ContextMenuSeparator,
  ContextMenuShortcut,
  ContextMenuSub,
  ContextMenuSubContent,
  ContextMenuSubTrigger,
  ContextMenuTrigger,
} from '@/components/ui/context-menu'
import { Drawer, DrawerClose, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle, DrawerTrigger } from '@/components/ui/drawer'
import { HoverCard, HoverCardContent, HoverCardTrigger } from '@/components/ui/hover-card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle, SheetTrigger } from '@/components/ui/sheet'
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip'
import { notifySuccess } from '@/runtime/feedback'
import { BlocoComponente, SecaoComponentes } from './GuiaComponentesBloco'

export function GuiaSobreposicoes() {
  return (
    <SecaoComponentes titulo="Sobreposições">
      <BlocoComponente nome="AlertDialog">
        <AlertDialog>
          <AlertDialogTrigger asChild>
            <Button variant="destructive">Cancelar nota</Button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Cancelar a nota 1234?</AlertDialogTitle>
              <AlertDialogDescription>O cancelamento é irreversível e será informado à prefeitura.</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel>Voltar</AlertDialogCancel>
              <AlertDialogAction onClick={() => notifySuccess('Nota cancelada (exemplo).')}>Cancelar nota</AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </BlocoComponente>

      <BlocoComponente nome="Sheet">
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="outline">Abrir filtros</Button>
          </SheetTrigger>
          <SheetContent side="right">
            <SheetHeader>
              <SheetTitle>Filtrar notas</SheetTitle>
              <SheetDescription>Refine a lista por período e situação.</SheetDescription>
            </SheetHeader>
            <div className="space-y-1.5 px-4">
              <Label htmlFor="g-sheet-tomador">Tomador</Label>
              <Input id="g-sheet-tomador" placeholder="Nome ou documento" />
            </div>
            <SheetFooter>
              <Button>Aplicar</Button>
            </SheetFooter>
          </SheetContent>
        </Sheet>
      </BlocoComponente>

      <BlocoComponente nome="Drawer">
        <Drawer>
          <DrawerTrigger asChild>
            <Button variant="outline">Abrir resumo da nota</Button>
          </DrawerTrigger>
          <DrawerContent>
            <DrawerHeader>
              <DrawerTitle>Resumo da nota</DrawerTitle>
              <DrawerDescription>Serviço de consultoria, valor de R$ 1.500,00.</DrawerDescription>
            </DrawerHeader>
            <DrawerFooter>
              <Button>Emitir</Button>
              <DrawerClose asChild>
                <Button variant="outline">Fechar</Button>
              </DrawerClose>
            </DrawerFooter>
          </DrawerContent>
        </Drawer>
      </BlocoComponente>

      <BlocoComponente nome="Popover">
        <Popover>
          <PopoverTrigger asChild>
            <Button variant="outline">Alterar série</Button>
          </PopoverTrigger>
          <PopoverContent className="w-60 space-y-2">
            <Label htmlFor="g-serie">Série do RPS</Label>
            <Input id="g-serie" defaultValue="1" />
          </PopoverContent>
        </Popover>
      </BlocoComponente>

      <BlocoComponente nome="Tooltip">
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="outline" size="icon" aria-label="Ajuda sobre o ISS">
              <InfoIcon />
            </Button>
          </TooltipTrigger>
          <TooltipContent>Imposto Sobre Serviços, recolhido ao município.</TooltipContent>
        </Tooltip>
      </BlocoComponente>

      <BlocoComponente nome="HoverCard">
        <HoverCard>
          <HoverCardTrigger asChild>
            <Button variant="link" className="px-0">
              Cliente Exemplo Ltda.
            </Button>
          </HoverCardTrigger>
          <HoverCardContent className="space-y-1 text-sm">
            <p className="font-medium">Cliente Exemplo Ltda.</p>
            <p className="text-muted-foreground">CNPJ 00.000.000/0001-00 - 12 notas emitidas.</p>
          </HoverCardContent>
        </HoverCard>
      </BlocoComponente>

      <BlocoComponente nome="ContextMenu">
        <ContextMenu>
          <ContextMenuTrigger className="flex h-24 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
            Clique com o botão direito aqui
          </ContextMenuTrigger>
          <ContextMenuContent className="w-56">
            <ContextMenuLabel>Nota 1234</ContextMenuLabel>
            <ContextMenuItem>
              Duplicar <ContextMenuShortcut>Ctrl+D</ContextMenuShortcut>
            </ContextMenuItem>
            <ContextMenuSub>
              <ContextMenuSubTrigger>Enviar para</ContextMenuSubTrigger>
              <ContextMenuSubContent>
                <ContextMenuItem>E-mail do tomador</ContextMenuItem>
                <ContextMenuItem>Contador</ContextMenuItem>
              </ContextMenuSubContent>
            </ContextMenuSub>
            <ContextMenuSeparator />
            <ContextMenuCheckboxItem checked>Marcar como favorita</ContextMenuCheckboxItem>
          </ContextMenuContent>
        </ContextMenu>
      </BlocoComponente>
    </SecaoComponentes>
  )
}
