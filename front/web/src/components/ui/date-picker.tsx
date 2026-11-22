import * as React from 'react'
import { format } from 'date-fns'
import { CalendarIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'

export type DatePickerProps = {
  value?: Date
  onChange?: (date: Date | undefined) => void
  placeholder?: string
  disabled?: boolean
  className?: string
  id?: string
  'aria-invalid'?: boolean
}

/** Seletor de data (Popover + Calendar), exibe dd/MM/yyyy. */
export function DatePicker({ value, onChange, placeholder = 'dd/mm/aaaa', disabled, className, id, ...props }: DatePickerProps) {
  const [open, setOpen] = React.useState(false)

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          disabled={disabled}
          aria-invalid={props['aria-invalid']}
          data-slot="date-picker"
          className={cn('w-full justify-start font-normal aria-invalid:border-destructive', !value && 'text-muted-foreground', className)}
        >
          <CalendarIcon />
          {value ? format(value, 'dd/MM/yyyy') : placeholder}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-auto p-0">
        <Calendar
          mode="single"
          selected={value}
          defaultMonth={value}
          onSelect={(d) => {
            onChange?.(d)
            setOpen(false)
          }}
        />
      </PopoverContent>
    </Popover>
  )
}
