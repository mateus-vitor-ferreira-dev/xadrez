import { useEffect, useRef } from 'react'
import { GOOGLE_CLIENT_ID } from '../lib/config'

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
      // O visual deste botão não importa: ele fica invisível (opacity 0) por cima
      // do nosso botão estilizado. Só precisa cobrir a mesma área p/ receber o clique.
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

  return (
    <div className="botao-google">
      {/* Botão VISÍVEL, estilizado à mão para combinar com o tema do site.
          É puramente decorativo (pointer-events: none): quem recebe o clique é
          o botão real do Google, renderizado invisível logo abaixo. */}
      <div className="botao-google-visual" aria-hidden="true">
        <LogoGoogle />
        <span>Continuar com o Google</span>
      </div>
      {/* Botão REAL do GIS: fica por cima, invisível, mas ainda clicável.
          É ele que dispara o fluxo de login e devolve o `credential`. */}
      <div className="botao-google-gis" ref={ref} />
    </div>
  )
}

// Logo "G" oficial do Google (4 cores), em SVG inline. Sem o quadrado branco
// atrás: aqui ele aparece direto sobre o botão escuro, combinando com o site.
function LogoGoogle() {
  return (
    <svg width="18" height="18" viewBox="0 0 48 48" aria-hidden="true">
      <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z" />
      <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z" />
      <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z" />
      <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z" />
    </svg>
  )
}
