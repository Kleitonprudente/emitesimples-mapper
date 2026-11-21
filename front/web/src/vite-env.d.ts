/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_ENV?: 'mock' | 'dev' | 'staging' | 'prod'
  readonly VITE_API_BASE?: string
  readonly VITE_API_PROXY_TARGET?: string
}
interface ImportMeta {
  readonly env: ImportMetaEnv
}
