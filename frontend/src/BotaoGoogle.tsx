import { useEffect, useRef } from 'react'
import { GOOGLE_CLIENT_ID } from './config'

// Tipos mínimos da API global do Google Identity Services (window.google),
// carregada pelo <script> em index.html.
interface GoogleId {
  initialize: (cfg: { client_id: string; callback: (r: { credential: string }) => void }) => void
  renderButton: (el: HTMLElement, opts: Record<string, unknown>) => void
}
declare global {
  interface Window {
    google?: { accounts: { id: GoogleId } }
  }
}

/**
 * Botão "Entrar com Google" (GIS). Ao autenticar, o Google chama de volta com o
 * `credential` (ID token), que repassamos para o backend validar. O script do
 * GIS é async: esperamos ele carregar antes de renderizar o botão.
 */
export default function BotaoGoogle({ onCredential }: { onCredential: (credential: string) => void }) {
  const ref = useRef<HTMLDivElement>(null)
  // Mantém o callback mais recente sem re-inicializar o GIS.
  const cb = useRef(onCredential)
  cb.current = onCredential

  useEffect(() => {
    let cancelado = false
    function tentar() {
      if (cancelado) return
      const id = window.google?.accounts?.id
      if (!id || !ref.current) {
        setTimeout(tentar, 200) // script ainda carregando
        return
      }
      id.initialize({ client_id: GOOGLE_CLIENT_ID, callback: (r) => cb.current(r.credential) })
      ref.current.replaceChildren() // evita botão duplicado no StrictMode
      id.renderButton(ref.current, {
        theme: 'filled_black',
        size: 'large',
        text: 'continue_with',
        shape: 'pill',
        width: 300,
      })
    }
    tentar()
    return () => {
      cancelado = true
    }
  }, [])

  return <div className="botao-google" ref={ref} />
}
