import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { googleFinalizar } from './api'
import { useAuth } from './auth'

/**
 * 1º acesso via Google: o usuário escolhe o apelido (público e único) antes de
 * entrar. Recebe o `credential` do Google pela navegação (state). Se alguém
 * abrir /apelido direto (sem credential), volta para o login.
 */
export default function PaginaApelido() {
  const navigate = useNavigate()
  const location = useLocation()
  const { definir } = useAuth()
  const estado = location.state as { credential?: string; sugestao?: string } | null

  const [apelido, setApelido] = useState(estado?.sugestao ?? '')
  const [erro, setErro] = useState<string | null>(null)

  const credential = estado?.credential

  const finalizar = useMutation({
    mutationFn: () => googleFinalizar(credential!, apelido.trim()),
    onSuccess: (a) => {
      definir(a)
      navigate('/')
    },
    onError: (e) => setErro(e instanceof Error ? e.message : 'Falha ao finalizar o cadastro.'),
  })

  // Sem credential (acesso direto/refresh): não há o que finalizar.
  if (!credential) return <Navigate to="/login" replace />

  function enviar() {
    if (apelido.trim().length < 3) {
      setErro('O apelido deve ter ao menos 3 caracteres.')
      return
    }
    setErro(null)
    finalizar.mutate()
  }

  return (
    <div className="auth-page">
      <div className="auth-box">
        <div className="auth-marca">♞</div>
        <h2>Escolha seu apelido</h2>
        <p className="auth-sub">É o nome público que aparece no ranking. Dá para trocar depois.</p>

        <input
          placeholder="Apelido"
          value={apelido}
          autoFocus
          onChange={(e) => setApelido(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && enviar()}
        />

        <button className="primario" onClick={enviar} disabled={finalizar.isPending}>
          {finalizar.isPending ? '…' : 'Entrar'}
        </button>

        {erro && <p className="status alerta">{erro}</p>}
      </div>
    </div>
  )
}
