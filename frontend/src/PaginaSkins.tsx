import { Link } from 'react-router-dom'
import { useAuth } from './auth'
import { useSkin } from './skin'
import { PecaSvg } from './skin'
import { TEMAS, desbloqueado } from './skins'

// INVENTÁRIO de skins (estilo CS2/Valorant): uma vitrine com todos os temas de
// peças. O jogador equipa o que quiser entre os DESBLOQUEADOS (o rank libera
// novos); os bloqueados aparecem esmaecidos com o requisito. "Clássico" é o
// padrão — equipá-lo é o mesmo que "jogar sem skin".

// Peças mostradas na prévia: rei e peão de cada lado (mostra o contraste claro/escuro).
const PREVIA = ['wr', 'wp', 'bp', 'br']

export default function PaginaSkins() {
  const { auth } = useAuth()
  const { tema: equipado, equipar } = useSkin()
  const meuElo = auth?.elo ?? 0

  return (
    <div className="skins-page">
      <Link to="/" className="voltar-auth">
        ← Voltar ao jogo
      </Link>

      <div className="skins-cabecalho">
        <h2>🎨 Skins de peças</h2>
        <p className="auth-sub">
          Equipe o visual que quiser. Novas skins são liberadas conforme você sobe de rank
          {auth ? ` (seu Elo: ${meuElo}).` : '. Faça login para desbloquear pelas suas vitórias.'}
        </p>
      </div>

      <div className="skins-grid">
        {TEMAS.map((t) => {
          const liberado = desbloqueado(t, meuElo)
          const ativo = t.id === equipado.id
          return (
            <div key={t.id} className={`skin-card${ativo ? ' equipado' : ''}${liberado ? '' : ' bloqueado'}`}>
              <div className="skin-previa">
                {PREVIA.map((code) => (
                  <PecaSvg key={code} code={code} tema={t} className="skin-previa-peca" />
                ))}
              </div>

              <div className="skin-info">
                <strong>{t.nome}</strong>
                <span className="skin-req">{t.eloMin === 0 ? 'Padrão' : `${t.rankNome} · Elo ${t.eloMin}`}</span>
              </div>

              {liberado ? (
                <button
                  className={ativo ? 'skin-btn ativo' : 'skin-btn primario'}
                  disabled={ativo}
                  onClick={() => equipar(t.id)}
                >
                  {ativo ? '✓ Equipado' : 'Equipar'}
                </button>
              ) : (
                <span className="skin-cadeado">🔒 {t.rankNome}</span>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
