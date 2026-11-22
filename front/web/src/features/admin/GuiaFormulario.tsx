import { z } from 'zod'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { AutoForm, type FieldLayout } from '@/runtime/AutoForm'
import type { FieldSize, FieldSpec } from '@/runtime/FormField'
import { SIZE_COLS } from '@/runtime/FormField'
import type { FkSource } from '@/runtime/Combobox'
import { notifySuccess } from '@/runtime/feedback'

const DEMO_CIDADES = [
  { id: '1', label: 'Guaíra - PR' },
  { id: '2', label: 'São Paulo - SP' },
  { id: '3', label: 'Curitiba - PR' },
]
const DEMO_MUNICIPIOS: FkSource = {
  key: 'demo-municipio-form',
  search: async (term) => DEMO_CIDADES.filter((c) => c.label.toLowerCase().includes(term.toLowerCase())),
  labelOf: (v) => DEMO_CIDADES.find((c) => c.id === v.id)?.label,
}

const TAMANHOS: { size: FieldSize; uso: string }[] = [
  { size: 'xs', uso: 'Texto de até 5 caracteres (UF, sigla)' },
  { size: 's', uso: 'Data, número, valor, máscara (CPF, CEP, telefone), Switch, texto de até 15' },
  { size: 'm', uso: 'Lista de opções (enum), texto de até 30' },
  { size: 'l', uso: 'Chave estrangeira (Combobox), texto de até 100' },
  { size: 'xl', uso: 'Texto de até 255' },
  { size: 'full', uso: 'Textarea e texto longo (linha inteira)' },
]

const opcoes = (...rotulos: string[]) => rotulos.map((label, i) => ({ value: String(i + 1), label }))

const pessoaSchema = z.looseObject({
  documento: z.string().min(1, 'Informe o documento.'),
  razaoSocial: z.string().min(1, 'Informe a razão social.'),
  logradouro: z.string().min(1, 'Informe o logradouro.'),
  numero: z.string().min(1, 'Informe o número.'),
  contatos: z.array(z.looseObject({ nome: z.string().min(1, 'Informe o nome.') })).optional(),
})

const TIPOS_CONTATO = [
  { value: 'socio', label: 'Sócio' },
  { value: 'financeiro', label: 'Financeiro' },
  { value: 'comercial', label: 'Comercial' },
  { value: 'fiscal', label: 'Fiscal' },
  { value: 'outro', label: 'Outro' },
]

/** Campo de lista (detalhe 1-N): cada item usa as mesmas linhas da grade de 12 colunas. */
const CONTATOS: FieldSpec = {
  name: 'contatos',
  label: 'Contatos',
  kind: 'list',
  size: 'full',
  list: {
    itemLabel: 'Contato',
    addLabel: 'Adicionar contato',
    emptyText: 'Nenhum contato adicionado.',
    maxItems: 10,
    defaultItem: { tipoContato: 'outro' },
    item: [
      [
        { name: 'tipoContato', label: 'Tipo', options: TIPOS_CONTATO, size: 's', required: true },
        { name: 'nome', label: 'Nome', maxLength: 100, size: 'l', required: true },
      ],
      [
        { name: 'whatsapp', label: 'WhatsApp', mask: 'phone', size: 's' },
        { name: 'email', label: 'E-mail', kind: 'email', maxLength: 100, size: 'l' },
      ],
    ],
  },
}


const CONTATOS_INICIAIS = [
  { tipoContato: 'financeiro', nome: 'Marta Lima', whatsapp: '11987654321', email: 'marta@exemplo.com.br' },
  { tipoContato: 'socio', nome: 'Paulo Nunes' },
]

const PESSOA: FieldLayout = [
  { section: 'Identificação' },
  [
    { name: 'tipo', label: 'Tipo', options: opcoes('Jurídica', 'Física'), size: 's', required: true },
    { name: 'documento', label: 'CNPJ/CPF', mask: 'documento', size: 's', required: true },
    { name: 'inscricaoMunicipal', label: 'Inscrição Municipal', maxLength: 15, size: 's' },
    { name: 'rgIe', label: 'RG/IE', maxLength: 15, size: 's' },
  ],
  [
    { name: 'razaoSocial', label: 'Razão Social', maxLength: 100, size: 'l', required: true },
    { name: 'nomeFantasia', label: 'Nome Fantasia', maxLength: 100, size: 'l' },
  ],
  [
    { name: 'tipoDestinatario', label: 'Tipo Destinatário', options: opcoes('Contribuinte', 'Isento', 'Não contribuinte'), size: 'l' },
    { name: 'consumidorFinal', label: 'Consumidor final', kind: 'boolean', size: 'l' },
  ],
  { section: 'Contato' },
  [
    { name: 'email', label: 'E-mail', kind: 'email', maxLength: 100, size: 'l' },
    { name: 'whatsapp', label: 'WhatsApp', mask: 'phone', size: 's' },
    { name: 'telefone', label: 'Telefone', mask: 'phone', size: 's' },
  ],
  { section: 'Endereço', description: 'Nas telas de cadastro, o campo CEP usa lookup: "cep" e preenche o restante ao sair do campo.' },
  [
    { name: 'cep', label: 'CEP', mask: 'postalCode', size: 's', hint: 'Ao sair do campo, o endereço é preenchido.' },
    { name: 'logradouro', label: 'Logradouro', maxLength: 100, size: 'l', required: true },
    { name: 'numero', label: 'Número', maxLength: 10, size: 's', required: true },
  ],
  [
    { name: 'complemento', label: 'Complemento', maxLength: 60, size: 'm' },
    { name: 'bairro', label: 'Bairro', maxLength: 60, size: 'm' },
    { name: 'cidade', label: 'Município', fk: DEMO_MUNICIPIOS, size: 'm' },
  ],
  { section: 'Contatos', description: 'Várias pessoas de contato (sócio, financeiro, comercial...). Com a consulta de CNPJ, os sócios entram aqui como contatos do tipo Sócio.' },
  [CONTATOS],
  { section: 'Pessoa estrangeira', description: 'Seção rara: recolhida por padrão.', collapsible: true },
  [
    { name: 'pais', label: 'País', maxLength: 60, size: 'm' },
    { name: 'documentoEstrangeiro', label: 'Documento estrangeiro', maxLength: 30, size: 'm' },
    { name: 'cidadeExterior', label: 'Cidade no exterior', maxLength: 60, size: 'm' },
  ],
]

const SERVICO: FieldLayout = [
  { section: 'Identificação' },
  [
    { name: 'codigo', label: 'Código', maxLength: 15, size: 's', required: true },
    { name: 'descricao', label: 'Descrição', maxLength: 255, size: 'xl', required: true },
    { name: 'ativo', label: 'Ativo', kind: 'boolean', size: 'xs' },
  ],
  { section: 'Tributação' },
  [
    { name: 'valor', label: 'Valor do serviço', kind: 'currency', size: 's' },
    { name: 'deducoes', label: 'Deduções', kind: 'currency', size: 's' },
    { name: 'desconto', label: 'Desconto', kind: 'currency', size: 's' },
    { name: 'baseCalculo', label: 'Base de cálculo', kind: 'currency', size: 's' },
  ],
  [
    { name: 'aliquotaIss', label: 'Alíquota ISS (%)', kind: 'number', size: 's' },
    { name: 'issRetido', label: 'ISS retido', kind: 'boolean', size: 's' },
    { name: 'codigoTributacao', label: 'Código de tributação municipal', maxLength: 30, size: 'l' },
  ],
  { section: 'Discriminação' },
  [{ name: 'discriminacao', label: 'Discriminação do serviço', kind: 'textarea', maxLength: 2000, size: 'full' }],
]

const SEIS_SECOES: FieldLayout = [
  { section: 'Dados gerais' },
  [
    { name: 'nome', label: 'Nome', maxLength: 100, size: 'l' },
    { name: 'sigla', label: 'Sigla', maxLength: 5, size: 'xs' },
    { name: 'tipo', label: 'Tipo', options: opcoes('A', 'B'), size: 'm' },
  ],
  { section: 'Fiscal' },
  [
    { name: 'regime', label: 'Regime', options: opcoes('Simples', 'Presumido', 'Real'), size: 'm' },
    { name: 'cnae', label: 'CNAE', maxLength: 15, size: 's' },
  ],
  { section: 'Endereço' },
  [
    { name: 'cep', label: 'CEP', mask: 'postalCode', size: 's' },
    { name: 'rua', label: 'Rua', maxLength: 100, size: 'l' },
    { name: 'num', label: 'Nº', maxLength: 10, size: 's' },
  ],
  { section: 'Contato' },
  [
    { name: 'mail', label: 'E-mail', kind: 'email', maxLength: 100, size: 'l' },
    { name: 'fone', label: 'Telefone', mask: 'phone', size: 's' },
  ],
  { section: 'Certificado' },
  [
    { name: 'validade', label: 'Validade', kind: 'date', size: 's' },
    { name: 'senha', label: 'Senha', kind: 'password', maxLength: 30, size: 'm' },
  ],
  { section: 'Observações' },
  [{ name: 'obs', label: 'Observações', kind: 'textarea', maxLength: 500, size: 'full' }],
]

const demoSchema = z.looseObject({})

function Exemplo({ titulo, legenda, children }: { titulo: string; legenda: string; children: React.ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{titulo}</CardTitle>
        <CardDescription>{legenda}</CardDescription>
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

/** Exemplos ao vivo do padrão de formulário (grade de 12 colunas, seções, Tabs). Só compõe AutoForm. */
export function GuiaFormulario() {
  return (
    <div className="flex flex-col gap-6">
      <Exemplo titulo="Tamanhos" legenda="Cada campo ocupa N de 12 colunas (a partir de md). Abaixo disso: 12 no celular e 6 entre sm e md. A linha soma 12; linhas menores ficam alinhadas à esquerda.">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>size</TableHead>
              <TableHead>Colunas</TableHead>
              <TableHead>Quando é automático</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {TAMANHOS.map((t) => (
              <TableRow key={t.size}>
                <TableCell>
                  <code>{t.size}</code>
                </TableCell>
                <TableCell>{SIZE_COLS[t.size]}</TableCell>
                <TableCell>{t.uso}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Exemplo>

      <Exemplo
        titulo="Pessoa"
        legenda="Seções em Card, barra de ações fixa, aviso de alterações não salvas. Identificação 3|3|3|3 e 6|6; Contato 6|3|3; Endereço 3|6|3 e 4|4|4; Contatos é um campo de lista (2 itens iniciais; cada item usa a mesma grade 12: Tipo 3 + Nome 6, WhatsApp 3 + E-mail 6, com remover e adicionar); Pessoa estrangeira é collapsible (rara)."
      >
        <AutoForm schema={pessoaSchema} fields={PESSOA} defaults={{ consumidorFinal: false, contatos: CONTATOS_INICIAIS }} submitLabel="Salvar pessoa" onSubmit={() => notifySuccess('Pessoa válida.')} onCancel={() => {}} />
      </Exemplo>

      <Exemplo titulo="Serviço" legenda="Valores em linhas de 4 campos de 3 colunas. A discriminação (textarea, 2000 caracteres) ocupa a linha inteira, com 6 linhas e contador.">
        <AutoForm schema={demoSchema} fields={SERVICO} defaults={{ ativo: true }} submitLabel="Salvar serviço" onSubmit={() => notifySuccess('Serviço válido.')} />
      </Exemplo>

      <Exemplo titulo="Mais de 5 seções: Tabs" legenda="Com 6 ou mais seções cada uma vira uma aba. A aba com campo inválido ganha um ponto vermelho depois de tentar enviar.">
        <AutoForm
          schema={z.looseObject({ nome: z.string().min(1, 'Informe o nome.'), obs: z.string().min(1, 'Informe as observações.') })}
          fields={SEIS_SECOES.map((it) => (Array.isArray(it) ? it.map((f) => (f.name === 'nome' || f.name === 'obs' ? { ...f, required: true } : f)) : it))}
          defaults={{}}
          submitLabel="Salvar"
          onSubmit={() => notifySuccess('Formulário válido.')}
        />
      </Exemplo>

      <Exemplo titulo="Exemplo errado (não renderizado)" legenda="Uma linha com Razão Social em xl (8) e Nome Fantasia em l (6) soma 14 colunas: o segundo campo quebra para a linha de baixo.">
        <p className="text-sm text-muted-foreground">
          Em desenvolvimento o AutoForm avisa no console: <code>a linha [razaoSocial, nomeFantasia] soma 14 colunas (o esperado e 12)</code>. Corrija usando l (6) e l (6), ou xl (8) e m (4). Linhas
          que somam menos de 12 são aceitas e ficam alinhadas à esquerda (veja Contato no exemplo de Pessoa).
        </p>
      </Exemplo>
    </div>
  )
}
