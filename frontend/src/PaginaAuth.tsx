import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { login, registrar } from './api'
import { useAuth } from './auth'

/**
 * Tela dedicada de autenticação. É UM único componente reusado nas duas rotas
 * (/login e /registro): como as telas são praticamente iguais, a prop `modo`
 * decide só os textos e qual endpoint chamar. Ao entrar/cadastrar com sucesso,
 * salva a sessão e volta para o jogo.
 */
export default function PaginaAuth({ modo }: { modo: 'login' | 'registro' }) {
  const navigate = useNavigate()
  const { definir } = useAuth()
  // No login, `usuario` guarda o identificador (e-mail OU apelido); no cadastro,
  // é o apelido, e o e-mail vai no campo próprio.
  const [usuario, setUsuario] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  const ehLogin = modo === 'login'

  const autenticar = useMutation({
    mutationFn: () =>
      ehLogin ? login(usuario.trim(), senha) : registrar(usuario.trim(), email.trim(), senha),
    onSuccess: (a) => {
      definir(a)
      navigate('/') // sessão criada: de volta ao jogo
    },
    onError: (e) => setErro(e instanceof Error ? e.message : 'Falha na autenticação.'),
  })

  function enviar() {
    // Validação adiantada (espelha a do backend) para feedback imediato.
    if (ehLogin) {
      if (!usuario.trim() || senha.length < 4) {
        setErro('Informe seu e-mail ou usuário e a senha.')
        return
      }
    } else {
      if (usuario.trim().length < 3) {
        setErro('O usuário deve ter ao menos 3 caracteres.')
        return
      }
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
        setErro('Informe um e-mail válido.')
        return
      }
      if (senha.length < 4) {
        setErro('A senha deve ter ao menos 4 caracteres.')
        return
      }
    }
    setErro(null)
    autenticar.mutate()
  }

  return (
    <div className="auth-page">
      <Link to="/" className="voltar-auth">
        ← Voltar ao jogo
      </Link>

      <div className="auth-box">
        <div className="auth-marca">♞</div>
        <h2>{ehLogin ? 'Entrar' : 'Criar conta'}</h2>
        <p className="auth-sub">
          {ehLogin
            ? 'Acesse para acumular Elo nas partidas online.'
            : 'Crie sua conta e comece com Elo 1200.'}
        </p>

        <input
          placeholder={ehLogin ? 'E-mail ou usuário' : 'Usuário'}
          value={usuario}
          autoFocus
          onChange={(e) => setUsuario(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && enviar()}
        />
        {!ehLogin && (
          <input
            type="email"
            placeholder="E-mail"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && enviar()}
          />
        )}
        <input
          type="password"
          placeholder="Senha"
          value={senha}
          onChange={(e) => setSenha(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && enviar()}
        />

        <button className="primario" onClick={enviar} disabled={autenticar.isPending}>
          {autenticar.isPending ? '…' : ehLogin ? 'Entrar' : 'Criar conta'}
        </button>

        {erro && <p className="status alerta">{erro}</p>}

        {/* Link para a outra tela (as duas se apontam mutuamente). */}
        <p className="auth-troca">
          {ehLogin ? (
            <>
              Não tem conta? <Link to="/registro">Criar conta</Link>
            </>
          ) : (
            <>
              Já tem conta? <Link to="/login">Entrar</Link>
            </>
          )}
        </p>
      </div>
    </div>
  )
}
