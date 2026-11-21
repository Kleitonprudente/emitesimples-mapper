import { toast } from 'sonner'
import { ApiError } from '@/lib/api'
import { formatDuration } from '@/runtime/format'

const GENERICA = 'Algo deu errado. Tente novamente em instantes.'

/** Mensagem segura para o usuario: usa a mensagem do back (ja em pt-BR); nunca texto tecnico de excecao. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 429 && error.retryAfter) {
      return `Muitas tentativas. Aguarde ${formatDuration(error.retryAfter)} e tente novamente.`
    }
    return error.mensagem
  }
  return GENERICA
}

export function notifySuccess(message: string): void {
  toast.success(message)
}

/** Toast de erro com a mensagem util e, quando e erro da API, o codigo para suporte. */
export function notifyError(error: unknown, fallbackTitle = 'Não foi possível concluir a operação'): void {
  if (error instanceof ApiError && error.status !== 0) {
    toast.error(errorMessage(error), { description: `${fallbackTitle} (código: ${error.codigo})` })
    return
  }
  toast.error(fallbackTitle, { description: errorMessage(error) })
}

/** Callbacks prontos para useMutation: toast de sucesso/erro padronizado. */
export function mutationFeedback<TData = unknown>(opts: { success: string; error?: string; onDone?: (data: TData) => void }) {
  return {
    onSuccess: (data: TData) => {
      notifySuccess(opts.success)
      opts.onDone?.(data)
    },
    onError: (e: unknown) => notifyError(e, opts.error),
  }
}
