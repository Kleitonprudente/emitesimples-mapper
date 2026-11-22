import { useState } from 'react'
import { FileTextIcon, HomeIcon, SettingsIcon, UsersIcon } from 'lucide-react'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Breadcrumb, BreadcrumbEllipsis, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from '@/components/ui/breadcrumb'
import { Button } from '@/components/ui/button'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { Command, CommandDialog, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator, CommandShortcut } from '@/components/ui/command'
import { Kbd, KbdGroup } from '@/components/ui/kbd'
import { Menubar, MenubarContent, MenubarItem, MenubarMenu, MenubarSeparator, MenubarShortcut, MenubarTrigger } from '@/components/ui/menubar'
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
} from '@/components/ui/navigation-menu'
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from '@/components/ui/pagination'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { BlocoComponente, SecaoComponentes } from './GuiaComponentesBloco'

function ComandosEmLista() {
  return (
    <>
      <CommandInput placeholder="Buscar ação..." />
      <CommandList>
        <CommandEmpty>Nenhum comando encontrado.</CommandEmpty>
        <CommandGroup heading="Notas">
          <CommandItem>
            <FileTextIcon /> Emitir nota
            <CommandShortcut>Ctrl+N</CommandShortcut>
          </CommandItem>
          <CommandItem>
            <FileTextIcon /> Cancelar nota
          </CommandItem>
        </CommandGroup>
        <CommandSeparator />
        <CommandGroup heading="Cadastros">
          <CommandItem>
            <UsersIcon /> Nova pessoa
          </CommandItem>
          <CommandItem>
            <SettingsIcon /> Configurar empresa
          </CommandItem>
        </CommandGroup>
      </CommandList>
    </>
  )
}

export function GuiaNavegacao() {
  const [paleta, setPaleta] = useState(false)
  const [pagina, setPagina] = useState(2)
  return (
    <SecaoComponentes titulo="Navegação">
      <BlocoComponente nome="Tabs">
        <Tabs defaultValue="dados">
          <TabsList>
            <TabsTrigger value="dados">Dados</TabsTrigger>
            <TabsTrigger value="notas">Notas</TabsTrigger>
            <TabsTrigger value="config">Configuração</TabsTrigger>
          </TabsList>
          <TabsContent value="dados" className="text-sm">
            Dados cadastrais da empresa.
          </TabsContent>
          <TabsContent value="notas" className="text-sm">
            Notas emitidas no período.
          </TabsContent>
          <TabsContent value="config" className="text-sm">
            Série e numeração do RPS.
          </TabsContent>
        </Tabs>
      </BlocoComponente>

      <BlocoComponente nome="Accordion">
        <Accordion type="single" collapsible defaultValue="a">
          <AccordionItem value="a">
            <AccordionTrigger>O que é uma NFS-e?</AccordionTrigger>
            <AccordionContent>Nota Fiscal de Serviço eletrônica, emitida pelo prestador.</AccordionContent>
          </AccordionItem>
          <AccordionItem value="b">
            <AccordionTrigger>Posso cancelar uma nota?</AccordionTrigger>
            <AccordionContent>Sim, dentro do prazo definido pelo município.</AccordionContent>
          </AccordionItem>
        </Accordion>
      </BlocoComponente>

      <BlocoComponente nome="Collapsible">
        <Collapsible className="space-y-2">
          <CollapsibleTrigger asChild>
            <Button variant="outline" size="sm">
              Mostrar detalhes do tomador
            </Button>
          </CollapsibleTrigger>
          <CollapsibleContent className="rounded-md border p-3 text-sm">Cliente Exemplo Ltda. - CNPJ 00.000.000/0001-00</CollapsibleContent>
        </Collapsible>
      </BlocoComponente>

      <BlocoComponente nome="Breadcrumb">
        <Breadcrumb>
          <BreadcrumbList>
            <BreadcrumbItem>
              <BreadcrumbLink href="#">
                <HomeIcon className="size-4" aria-label="Início" />
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbEllipsis />
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbLink href="#">Empresas</BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage>Editar</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>
      </BlocoComponente>

      <BlocoComponente nome="Pagination">
        <Pagination>
          <PaginationContent>
            <PaginationItem>
              <PaginationPrevious href="#" onClick={(e) => (e.preventDefault(), setPagina((p) => Math.max(1, p - 1)))} />
            </PaginationItem>
            {[1, 2, 3].map((n) => (
              <PaginationItem key={n}>
                <PaginationLink href="#" isActive={pagina === n} onClick={(e) => (e.preventDefault(), setPagina(n))}>
                  {n}
                </PaginationLink>
              </PaginationItem>
            ))}
            <PaginationItem>
              <PaginationEllipsis />
            </PaginationItem>
            <PaginationItem>
              <PaginationNext href="#" onClick={(e) => (e.preventDefault(), setPagina((p) => Math.min(3, p + 1)))} />
            </PaginationItem>
          </PaginationContent>
        </Pagination>
      </BlocoComponente>

      <BlocoComponente nome="Kbd">
        <p className="flex flex-wrap items-center gap-2 text-sm">
          Abrir a paleta:
          <KbdGroup>
            <Kbd>Ctrl</Kbd>
            <Kbd>K</Kbd>
          </KbdGroup>
        </p>
      </BlocoComponente>

      <BlocoComponente nome="Menubar">
        <Menubar>
          <MenubarMenu>
            <MenubarTrigger>Notas</MenubarTrigger>
            <MenubarContent>
              <MenubarItem>
                Nova nota <MenubarShortcut>Ctrl+N</MenubarShortcut>
              </MenubarItem>
              <MenubarSeparator />
              <MenubarItem>Importar lote</MenubarItem>
            </MenubarContent>
          </MenubarMenu>
          <MenubarMenu>
            <MenubarTrigger>Relatórios</MenubarTrigger>
            <MenubarContent>
              <MenubarItem>Notas do mês</MenubarItem>
              <MenubarItem>Impostos retidos</MenubarItem>
            </MenubarContent>
          </MenubarMenu>
        </Menubar>
      </BlocoComponente>

      <BlocoComponente nome="NavigationMenu">
        <NavigationMenu viewport={false}>
          <NavigationMenuList>
            <NavigationMenuItem>
              <NavigationMenuTrigger>Cadastros</NavigationMenuTrigger>
              <NavigationMenuContent>
                <ul className="grid w-56 gap-1 p-1">
                  <li>
                    <NavigationMenuLink href="#">Pessoas</NavigationMenuLink>
                  </li>
                  <li>
                    <NavigationMenuLink href="#">Municípios</NavigationMenuLink>
                  </li>
                </ul>
              </NavigationMenuContent>
            </NavigationMenuItem>
            <NavigationMenuItem>
              <NavigationMenuLink href="#" className="px-3 py-2 text-sm">
                Notas
              </NavigationMenuLink>
            </NavigationMenuItem>
          </NavigationMenuList>
        </NavigationMenu>
      </BlocoComponente>

      <BlocoComponente nome="Command (inline)">
        <Command className="rounded-md border">
          <ComandosEmLista />
        </Command>
      </BlocoComponente>

      <BlocoComponente nome="CommandDialog">
        <Button variant="outline" onClick={() => setPaleta(true)}>
          Abrir paleta de comandos
        </Button>
        <CommandDialog open={paleta} onOpenChange={setPaleta}>
          <ComandosEmLista />
        </CommandDialog>
      </BlocoComponente>
    </SecaoComponentes>
  )
}
