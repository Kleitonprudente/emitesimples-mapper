import { useEffect, useRef, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Spinner } from '@/components/ui/spinner'
import { configurarSenhaRecente } from '@/lib/api'

/* ---------------------------------------------------------------------------
 * Senha recente (docs/financeiro/README.md, 9.2, A7). Montado uma vez no __root: quando o back pede a senha (403 senha_recente_exigida,
 * no estorno e na alteracao de conta financeira), a api abre este dialogo; com a senha certa, a operacao e repetida e, por 5 minutos,
 * nao se pergunta de novo. A senha so vai para POST /auth/reautenticar e nunca fica guardada.
 * ------------------------------------------------------------------------- */

interface Pedido {
  confirmar: (senha: string) => Promise<string | null>
  resolver: (ok: boolean) => void
}

export function SenhaRecente() {
  const [pedido, setPedido] = useState<Pedido | null>(null)
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const atual = useRef<Pedido | null>(null)

  useEffect(() => {
    configurarSenhaRecente(
      (confirmar) =>
        new Promise<boolean>((resolver) => {
          atual.current?.resolver(false)
          const p = { confirmar, resolver }
          atual.current = p
          setSenha('')
          setErro(null)
          setPedido(p)
        }),
    )
    return () => configurarSenhaRecente(null)
  }, [])

  const fechar = (ok: boolean) => {
    atual.current?.resolver(ok)
    atual.current = null
    setPedido(null)
    setSenha('')
  }

  const enviar = async () => {
    if (!pedido || !senha) return
    setEnviando(true)
    try {
      const msg = await pedido.confirmar(senha)
      if (msg) setErro(msg)
      else fechar(true)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Dialog open={pedido !== null} onOpenChange={(v) => !v && fechar(false)}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Confirme sua senha</DialogTitle>
          <DialogDescription>Esta operação mexe em dinheiro. Por segurança, confirme sua senha; ela vale por 5 minutos.</DialogDescription>
        </DialogHeader>
        <form
          className="flex flex-col gap-4"
          onSubmit={(e) => {
            e.preventDefault()
            void enviar()
          }}
        >
          <Field data-invalid={erro ? true : undefined}>
            <FieldLabel htmlFor="senha-recente">Senha</FieldLabel>
            <Input
              id="senha-recente"
              type="password"
              autoComplete="current-password"
              autoFocus
              value={senha}
              aria-invalid={erro ? true : undefined}
              onChange={(e) => {
                setSenha(e.target.value)
                setErro(null)
              }}
            />
            {erro && <FieldError>{erro}</FieldError>}
          </Field>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => fechar(false)}>
              Voltar
            </Button>
            <Button type="submit" disabled={!senha || enviando}>
              {enviando && <Spinner />} Confirmar
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
