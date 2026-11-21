export type AppEnv = 'mock' | 'dev' | 'staging' | 'prod'

const ENVS: readonly AppEnv[] = ['mock', 'dev', 'staging', 'prod']
const raw = import.meta.env.VITE_APP_ENV

/** Ambiente da aplicacao. Valor invalido cai em 'prod' (o mais restritivo, sem selo e sem mock). */
export const APP_ENV: AppEnv = ENVS.includes(raw as AppEnv) ? (raw as AppEnv) : 'prod'
export const IS_MOCK = APP_ENV === 'mock'

/** Base da API: mesma origem. Nenhuma URL absoluta no codigo. */
export const API_BASE: string = import.meta.env.VITE_API_BASE || '/api'

const BADGES: Record<AppEnv, string | null> = { mock: 'MOCK', dev: 'DEV', staging: 'STAGING', prod: null }

/** Selo de ambiente exibido no cabecalho. Prod nao exibe. */
export const ENV_BADGE: string | null = BADGES[APP_ENV]
