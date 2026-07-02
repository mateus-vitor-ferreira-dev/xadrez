import { Link } from 'react-router-dom'
import PerfilUsuario from '../components/PerfilUsuario'
import { useAuth } from '../contexts/auth'

// Barra fixa no topo das telas: identidade do usuário (ou entrar/criar conta) à
// esquerda e os atalhos globais (skins, tema claro/escuro, som) à direita. Estava
// inline no App; virou um layout reutilizável. O estado de tema/som mora em quem
// usa a barra (o App também precisa do "mudo" para os sons), e chega por props.

interface BarraTopoProps {
  /** Tema visual atual — decide o ícone ☀️/🌙. */
  tema: 'escuro' | 'claro'
  /** Alterna o tema claro/escuro. */
  onToggleTema: () => void
  /** Se o som está mudo — decide o ícone 🔇/🔊. */
  mudo: boolean
  /** Alterna o som. */
  onToggleMudo: () => void
}

export default function BarraTopo({ tema, onToggleTema, mudo, onToggleMudo }: BarraTopoProps) {
  const { auth, sair } = useAuth()

  return (
    <div className="barra-topo">
      <div className="usuario-area">
        {auth ? (
          <>
            <PerfilUsuario usuario={auth.usuario} elo={auth.elo} />
            <button className="toggle" onClick={sair}>
              Sair
            </button>
          </>
        ) : (
          <>
            <Link className="toggle" to="/login">
              Entrar
            </Link>
            <Link className="toggle" to="/registro">
              Criar conta
            </Link>
          </>
        )}
      </div>
      <div className="toggles">
        <Link className="toggle" to="/como-jogar" title="Como jogar">
          ❔
        </Link>
        <Link className="toggle" to="/skins" title="Skins das peças">
          🎨
        </Link>
        <button className="toggle" title="Tema" onClick={onToggleTema}>
          {tema === 'escuro' ? '☀️' : '🌙'}
        </button>
        <button className="toggle" title="Som" onClick={onToggleMudo}>
          {mudo ? '🔇' : '🔊'}
        </button>
      </div>
    </div>
  )
}
