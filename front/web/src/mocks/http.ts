/* Helpers de Response do modo mock (compartilhados por registry.ts e admin.ts). */

export const json = (status: number, body: unknown, headers: Record<string, string> = {}) =>
  new Response(body === undefined ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', ...headers } })

export const erro = (status: number, codigo: string, mensagem: string, detalhes?: unknown, headers?: Record<string, string>) =>
  json(status, { codigo, mensagem, ...(detalhes ? { detalhes } : {}) }, headers)
