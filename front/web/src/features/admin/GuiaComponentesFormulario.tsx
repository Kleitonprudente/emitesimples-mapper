import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { BoldIcon, ItalicIcon, UnderlineIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ButtonGroup, ButtonGroupSeparator, ButtonGroupText } from '@/components/ui/button-group'
import { Calendar } from '@/components/ui/calendar'
import { Checkbox } from '@/components/ui/checkbox'
import { Combobox } from '@/components/ui/combobox'
import { DatePicker } from '@/components/ui/date-picker'
import { Field, FieldContent, FieldDescription, FieldError, FieldGroup, FieldLabel, FieldLegend, FieldSet } from '@/components/ui/field'
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { InputOTP, InputOTPGroup, InputOTPSeparator, InputOTPSlot } from '@/components/ui/input-otp'
import { Label } from '@/components/ui/label'
import { NativeSelect, NativeSelectOptGroup, NativeSelectOption } from '@/components/ui/native-select'
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Slider } from '@/components/ui/slider'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { Toggle } from '@/components/ui/toggle'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { BlocoComponente, SecaoComponentes } from './GuiaComponentesBloco'

const REGIMES = [
  { value: 'mei', label: 'MEI' },
  { value: 'simples', label: 'Simples Nacional' },
  { value: 'presumido', label: 'Lucro Presumido' },
  { value: 'real', label: 'Lucro Real' },
]

const empresaSchema = z.object({
  razaoSocial: z.string().min(3, 'Informe a razão social (mínimo 3 letras).'),
})
type EmpresaForm = z.infer<typeof empresaSchema>

function FormExemplo() {
  const form = useForm<EmpresaForm>({ resolver: zodResolver(empresaSchema), defaultValues: { razaoSocial: '' } })
  const [enviado, setEnviado] = useState<string | null>(null)
  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((v) => setEnviado(v.razaoSocial))} className="space-y-3" noValidate>
        <FormField
          control={form.control}
          name="razaoSocial"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Razão social</FormLabel>
              <FormControl>
                <Input placeholder="Minha Empresa Ltda." {...field} />
              </FormControl>
              <FormDescription>Como consta no cartão CNPJ.</FormDescription>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" size="sm">
          Salvar
        </Button>
        {enviado && <p className="text-sm text-success">Salvo: {enviado}</p>}
      </form>
    </Form>
  )
}

export function GuiaFormulario() {
  const [concordo, setConcordo] = useState(false)
  const [tipo, setTipo] = useState('servico')
  const [notificar, setNotificar] = useState(true)
  const [aliquota, setAliquota] = useState([5])
  const [codigo, setCodigo] = useState('')
  const [data, setData] = useState<Date | undefined>(new Date(2026, 10, 22))
  const [dataCal, setDataCal] = useState<Date | undefined>(new Date(2026, 10, 22))
  const [regime, setRegime] = useState('simples')
  const [regimeBusca, setRegimeBusca] = useState<string>()
  const [formato, setFormato] = useState<string[]>(['bold'])
  const [alinhamento, setAlinhamento] = useState('esquerda')
  const [negrito, setNegrito] = useState(false)
  // O exemplo de Field mostra o estado de erro de forma fixa.
  const campoInvalido = true
  return (
    <SecaoComponentes titulo="Formulário e entrada">
      <BlocoComponente nome="Input e Label">
        <div className="space-y-1.5">
          <Label htmlFor="g-razao">Nome fantasia</Label>
          <Input id="g-razao" placeholder="Padaria do Bairro" />
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Textarea">
        <div className="space-y-1.5">
          <Label htmlFor="g-discr">Discriminação do serviço</Label>
          <Textarea id="g-discr" rows={3} placeholder="Descreva o serviço prestado..." />
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Select">
        <Select value={regime} onValueChange={setRegime}>
          <SelectTrigger className="w-full" aria-label="Regime tributário">
            <SelectValue placeholder="Regime tributário" />
          </SelectTrigger>
          <SelectContent>
            {REGIMES.map((r) => (
              <SelectItem key={r.value} value={r.value}>
                {r.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </BlocoComponente>

      <BlocoComponente nome="NativeSelect">
        <NativeSelect aria-label="Município" defaultValue="sp">
          <NativeSelectOptGroup label="Sudeste">
            <NativeSelectOption value="sp">São Paulo</NativeSelectOption>
            <NativeSelectOption value="rj">Rio de Janeiro</NativeSelectOption>
          </NativeSelectOptGroup>
          <NativeSelectOptGroup label="Sul">
            <NativeSelectOption value="cwb">Curitiba</NativeSelectOption>
          </NativeSelectOptGroup>
        </NativeSelect>
      </BlocoComponente>

      <BlocoComponente nome="Combobox (lista estática)">
        <Combobox options={REGIMES} value={regimeBusca} onValueChange={setRegimeBusca} placeholder="Buscar regime..." />
      </BlocoComponente>

      <BlocoComponente nome="Checkbox">
        <div className="flex items-center gap-2">
          <Checkbox id="g-aceite" checked={concordo} onCheckedChange={(v) => setConcordo(v === true)} />
          <Label htmlFor="g-aceite">Confirmo os dados do tomador</Label>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="RadioGroup">
        <RadioGroup value={tipo} onValueChange={setTipo}>
          <div className="flex items-center gap-2">
            <RadioGroupItem value="servico" id="g-r1" />
            <Label htmlFor="g-r1">Serviço prestado</Label>
          </div>
          <div className="flex items-center gap-2">
            <RadioGroupItem value="tomado" id="g-r2" />
            <Label htmlFor="g-r2">Serviço tomado</Label>
          </div>
        </RadioGroup>
      </BlocoComponente>

      <BlocoComponente nome="Switch">
        <div className="flex items-center gap-2">
          <Switch id="g-notificar" checked={notificar} onCheckedChange={setNotificar} />
          <Label htmlFor="g-notificar">Enviar nota por e-mail</Label>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Slider">
        <div className="space-y-2">
          <Label>Alíquota do ISS: {aliquota[0]}%</Label>
          <Slider value={aliquota} onValueChange={setAliquota} min={0} max={5} step={0.5} aria-label="Alíquota do ISS" />
        </div>
      </BlocoComponente>

      <BlocoComponente nome="Toggle">
        <Toggle variant="outline" aria-label="Negrito" pressed={negrito} onPressedChange={setNegrito}>
          <BoldIcon /> Destacar
        </Toggle>
      </BlocoComponente>

      <BlocoComponente nome="ToggleGroup">
        <div className="space-y-2">
          <ToggleGroup type="multiple" variant="outline" value={formato} onValueChange={setFormato} aria-label="Formatação">
            <ToggleGroupItem value="bold" aria-label="Negrito">
              <BoldIcon />
            </ToggleGroupItem>
            <ToggleGroupItem value="italic" aria-label="Itálico">
              <ItalicIcon />
            </ToggleGroupItem>
            <ToggleGroupItem value="underline" aria-label="Sublinhado">
              <UnderlineIcon />
            </ToggleGroupItem>
          </ToggleGroup>
          <ToggleGroup type="single" variant="outline" value={alinhamento} onValueChange={(v) => v && setAlinhamento(v)} aria-label="Período">
            <ToggleGroupItem value="esquerda">Mês</ToggleGroupItem>
            <ToggleGroupItem value="centro">Trimestre</ToggleGroupItem>
            <ToggleGroupItem value="direita">Ano</ToggleGroupItem>
          </ToggleGroup>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="ButtonGroup">
        <ButtonGroup>
          <ButtonGroupText>Notas</ButtonGroupText>
          <Button variant="outline">Anterior</Button>
          <ButtonGroupSeparator />
          <Button variant="outline">Próxima</Button>
        </ButtonGroup>
      </BlocoComponente>

      <BlocoComponente nome="InputOTP">
        <div className="space-y-2">
          <Label>Código de verificação (6 dígitos)</Label>
          <InputOTP maxLength={6} value={codigo} onChange={setCodigo} aria-label="Código de verificação">
            <InputOTPGroup>
              <InputOTPSlot index={0} />
              <InputOTPSlot index={1} />
              <InputOTPSlot index={2} />
            </InputOTPGroup>
            <InputOTPSeparator />
            <InputOTPGroup>
              <InputOTPSlot index={3} />
              <InputOTPSlot index={4} />
              <InputOTPSlot index={5} />
            </InputOTPGroup>
          </InputOTP>
        </div>
      </BlocoComponente>

      <BlocoComponente nome="DatePicker">
        <DatePicker value={data} onChange={setData} placeholder="Data de emissão" />
      </BlocoComponente>

      <BlocoComponente nome="Calendar">
        <Calendar mode="single" selected={dataCal} onSelect={setDataCal} defaultMonth={dataCal} className="rounded-md border" />
      </BlocoComponente>

      <BlocoComponente nome="Field">
        <FieldSet>
          <FieldLegend>Tomador</FieldLegend>
          <FieldGroup>
            <Field data-invalid={campoInvalido}>
              <FieldLabel htmlFor="g-doc">CPF ou CNPJ</FieldLabel>
              <Input id="g-doc" aria-invalid={campoInvalido} placeholder="000.000.000-00" />
              <FieldDescription>Somente números.</FieldDescription>
              <FieldError errors={[{ message: 'Documento obrigatório.' }]} />
            </Field>
            <Field orientation="horizontal">
              <Checkbox id="g-f-iss" />
              <FieldContent>
                <FieldLabel htmlFor="g-f-iss">ISS retido</FieldLabel>
                <FieldDescription>O tomador recolhe o imposto.</FieldDescription>
              </FieldContent>
            </Field>
          </FieldGroup>
        </FieldSet>
      </BlocoComponente>

      <BlocoComponente nome="Form (react-hook-form + zod)">
        <FormExemplo />
      </BlocoComponente>
    </SecaoComponentes>
  )
}
