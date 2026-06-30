/// <reference types="vite/client" />

// Tipagem das nossas variáveis de ambiente (boa prática de TypeScript).
interface ImportMetaEnv {
  /**
   * URL base do backend em PRODUÇÃO (ex.: https://xadrez.up.railway.app).
   * No desenvolvimento fica indefinida e usamos o proxy do Vite (caminho "/partidas").
   */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
