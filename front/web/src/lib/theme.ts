import { useSyncExternalStore } from 'react'

/* Tema claro/escuro: classe `dark` no <html>. A escolha (nao e PII) fica em localStorage; o script de index.html aplica antes de pintar. */

export type Theme = 'light' | 'dark'
const listeners = new Set<() => void>()

export function getTheme(): Theme {
  return document.documentElement.classList.contains('dark') ? 'dark' : 'light'
}

export function setTheme(t: Theme): void {
  document.documentElement.classList.toggle('dark', t === 'dark')
  try {
    localStorage.setItem('tema', t)
  } catch {
    /* storage indisponivel: vale so nesta sessao */
  }
  listeners.forEach((l) => l())
}

export function useTheme(): [Theme, () => void] {
  const theme = useSyncExternalStore(
    (cb) => {
      listeners.add(cb)
      return () => listeners.delete(cb)
    },
    getTheme,
    () => 'light' as Theme,
  )
  return [theme, () => setTheme(theme === 'dark' ? 'light' : 'dark')]
}
