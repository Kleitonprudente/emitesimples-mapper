import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/lib/api'

/** Sem retry em 401/403 (nao adianta repetir); demais erros: 1 nova tentativa. */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) return false
  return failureCount < 1
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      gcTime: 60_000,
      retry: shouldRetry,
      refetchOnWindowFocus: false,
    },
  },
})
